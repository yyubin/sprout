package sprout.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ByteBufferPoolTest {

    private ByteBufferPool pool;

    @BeforeEach
    void setUp() {
        pool = new ByteBufferPool();
    }

    @Test
    @DisplayName("Should acquire and release buffer successfully")
    void testBasicAcquireAndRelease() {
        // Acquire a buffer
        ByteBuffer buffer = pool.acquire(ByteBufferPool.SMALL_BUFFER_SIZE);

        assertNotNull(buffer);
        assertEquals(ByteBufferPool.SMALL_BUFFER_SIZE, buffer.capacity());
        assertEquals(0, buffer.position());
        assertEquals(ByteBufferPool.SMALL_BUFFER_SIZE, buffer.limit());

        // Release it back
        pool.release(buffer);

        // Stats should show 1 acquire and 1 release
        ByteBufferPool.PoolStats stats = pool.getStats(ByteBufferPool.SMALL_BUFFER_SIZE);
        assertEquals(1, stats.acquireCount);
        assertEquals(1, stats.releaseCount);
    }

    @Test
    @DisplayName("Should reuse buffers from pool")
    void testBufferReuse() {
        // Acquire and release a buffer
        ByteBuffer buffer1 = pool.acquire(ByteBufferPool.SMALL_BUFFER_SIZE);
        buffer1.putInt(42); // Mark it
        pool.release(buffer1);

        // Acquire again - should get the same buffer (cleared)
        ByteBuffer buffer2 = pool.acquire(ByteBufferPool.SMALL_BUFFER_SIZE);

        // Should be cleared
        assertEquals(0, buffer2.position());
        assertEquals(ByteBufferPool.SMALL_BUFFER_SIZE, buffer2.limit());

        // Stats should show 100% hit rate on second acquire
        ByteBufferPool.PoolStats stats = pool.getStats(ByteBufferPool.SMALL_BUFFER_SIZE);
        assertEquals(2, stats.acquireCount);
        assertEquals(1, stats.allocateCount); // Only 1 allocation
        assertEquals(50.0, stats.getHitRate(), 0.01); // 1 hit out of 2 acquires
    }

    @Test
    @DisplayName("Should allocate correct size based on request")
    void testSizeMapping() {
        // Request small size
        ByteBuffer small = pool.acquire(1024);
        assertEquals(ByteBufferPool.SMALL_BUFFER_SIZE, small.capacity());

        // Request medium size
        ByteBuffer medium = pool.acquire(4096);
        assertEquals(ByteBufferPool.MEDIUM_BUFFER_SIZE, medium.capacity());

        // Request large size
        ByteBuffer large = pool.acquire(16384);
        assertEquals(ByteBufferPool.LARGE_BUFFER_SIZE, large.capacity());

        pool.release(small);
        pool.release(medium);
        pool.release(large);
    }

    @Test
    @DisplayName("Should handle different pool sizes independently")
    void testMultiplePools() {
        ByteBuffer small = pool.acquire(ByteBufferPool.SMALL_BUFFER_SIZE);
        ByteBuffer medium = pool.acquire(ByteBufferPool.MEDIUM_BUFFER_SIZE);
        ByteBuffer large = pool.acquire(ByteBufferPool.LARGE_BUFFER_SIZE);

        pool.release(small);
        pool.release(medium);
        pool.release(large);

        // Each pool should have independent stats
        ByteBufferPool.PoolStats smallStats = pool.getStats(ByteBufferPool.SMALL_BUFFER_SIZE);
        ByteBufferPool.PoolStats mediumStats = pool.getStats(ByteBufferPool.MEDIUM_BUFFER_SIZE);
        ByteBufferPool.PoolStats largeStats = pool.getStats(ByteBufferPool.LARGE_BUFFER_SIZE);

        assertEquals(1, smallStats.acquireCount);
        assertEquals(1, mediumStats.acquireCount);
        assertEquals(1, largeStats.acquireCount);

        assertEquals(1, smallStats.currentPoolSize);
        assertEquals(1, mediumStats.currentPoolSize);
        assertEquals(1, largeStats.currentPoolSize);
    }

    @Test
    @DisplayName("Should respect maximum pool size")
    void testMaxPoolSize() {
        int maxSize = 10;
        pool.initializePool(1024, maxSize);

        // Acquire and release more buffers than max pool size
        List<ByteBuffer> buffers = new ArrayList<>();
        for (int i = 0; i < maxSize + 5; i++) {
            buffers.add(pool.acquire(1024));
        }

        // Release all
        for (ByteBuffer buffer : buffers) {
            pool.release(buffer);
        }

        // Pool should only keep maxSize buffers
        ByteBufferPool.PoolStats stats = pool.getStats(1024);
        assertTrue(stats.currentPoolSize <= maxSize);
    }

    @Test
    @DisplayName("Should handle null buffer release gracefully")
    void testNullRelease() {
        assertDoesNotThrow(() -> pool.release(null));
    }

    @Test
    @DisplayName("Should handle concurrent access safely")
    void testConcurrentAccess() throws InterruptedException, ExecutionException {
        int numThreads = 10;
        int operationsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        List<Future<?>> futures = new ArrayList<>();

        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < numThreads; i++) {
            Future<?> future = executor.submit(() -> {
                for (int j = 0; j < operationsPerThread; j++) {
                    ByteBuffer buffer = pool.acquire(ByteBufferPool.MEDIUM_BUFFER_SIZE);
                    assertNotNull(buffer);

                    // Simulate some work
                    buffer.putInt(j);

                    pool.release(buffer);
                    successCount.incrementAndGet();
                }
            });
            futures.add(future);
        }

        // Wait for all threads to complete
        for (Future<?> future : futures) {
            future.get();
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

        // Verify all operations succeeded
        assertEquals(numThreads * operationsPerThread, successCount.get());

        // Verify stats
        ByteBufferPool.PoolStats stats = pool.getStats(ByteBufferPool.MEDIUM_BUFFER_SIZE);
        assertEquals(numThreads * operationsPerThread, stats.acquireCount);
        assertEquals(numThreads * operationsPerThread, stats.releaseCount);
        assertTrue(stats.getHitRate() > 0, "Hit rate should be greater than 0");
    }

    @Test
    @DisplayName("Should calculate hit rate correctly")
    void testHitRateCalculation() {
        // Allocate 5 buffers
        List<ByteBuffer> buffers = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            buffers.add(pool.acquire(ByteBufferPool.SMALL_BUFFER_SIZE));
        }

        // Release them
        for (ByteBuffer buffer : buffers) {
            pool.release(buffer);
        }

        // Acquire 5 more - should all be hits
        for (int i = 0; i < 5; i++) {
            buffers.add(pool.acquire(ByteBufferPool.SMALL_BUFFER_SIZE));
        }

        ByteBufferPool.PoolStats stats = pool.getStats(ByteBufferPool.SMALL_BUFFER_SIZE);

        // 10 total acquires, 5 allocations = 50% hit rate
        assertEquals(10, stats.acquireCount);
        assertEquals(5, stats.allocateCount);
        assertEquals(50.0, stats.getHitRate(), 0.01);
    }

    @Test
    @DisplayName("Should use direct buffers when configured")
    void testDirectBuffers() {
        ByteBufferPool directPool = new ByteBufferPool(true);
        ByteBuffer buffer = directPool.acquire(ByteBufferPool.SMALL_BUFFER_SIZE);

        assertTrue(buffer.isDirect(), "Buffer should be direct");

        directPool.release(buffer);
    }

    @Test
    @DisplayName("Should use heap buffers by default")
    void testHeapBuffers() {
        ByteBuffer buffer = pool.acquire(ByteBufferPool.SMALL_BUFFER_SIZE);

        assertFalse(buffer.isDirect(), "Buffer should be heap-allocated");

        pool.release(buffer);
    }

    @Test
    @DisplayName("Should clear pool successfully")
    void testClearPool() {
        // Add some buffers to pool
        List<ByteBuffer> buffers = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            buffers.add(pool.acquire(ByteBufferPool.SMALL_BUFFER_SIZE));
        }
        for (ByteBuffer buffer : buffers) {
            pool.release(buffer);
        }

        int beforeClear = pool.getTotalBuffersInPool();
        assertTrue(beforeClear > 0);

        pool.clear();

        int afterClear = pool.getTotalBuffersInPool();
        assertEquals(0, afterClear);
    }

    @Test
    @DisplayName("Should handle very large buffer requests")
    void testVeryLargeBuffers() {
        int veryLargeSize = 1024 * 1024; // 1MB
        ByteBuffer largeBuffer = pool.acquire(veryLargeSize);

        assertNotNull(largeBuffer);
        assertTrue(largeBuffer.capacity() >= veryLargeSize);

        // This won't be pooled
        pool.release(largeBuffer);

        // Should not have stats for this size
        ByteBufferPool.PoolStats stats = pool.getStats(veryLargeSize);
        assertNull(stats);
    }

    @Test
    @DisplayName("Should calculate utilization correctly")
    void testUtilizationCalculation() {
        int maxSize = 100;
        // Use a predefined pool size and reinitialize with custom max size
        pool.initializePool(ByteBufferPool.MEDIUM_BUFFER_SIZE, maxSize);

        // Add 50 buffers to pool
        List<ByteBuffer> buffers = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            buffers.add(pool.acquire(ByteBufferPool.MEDIUM_BUFFER_SIZE));
        }
        for (ByteBuffer buffer : buffers) {
            pool.release(buffer);
        }

        ByteBufferPool.PoolStats stats = pool.getStats(ByteBufferPool.MEDIUM_BUFFER_SIZE);

        // 50 out of 100 = 50% utilization
        assertEquals(50.0, stats.getUtilization(), 0.01);
    }

    @Test
    @DisplayName("Should handle buffer state correctly after release")
    void testBufferStateAfterRelease() {
        ByteBuffer buffer = pool.acquire(ByteBufferPool.MEDIUM_BUFFER_SIZE);

        // Write some data
        buffer.putInt(123);
        buffer.putLong(456L);
        buffer.flip();

        // Release it
        pool.release(buffer);

        // Acquire again
        ByteBuffer reused = pool.acquire(ByteBufferPool.MEDIUM_BUFFER_SIZE);

        // Should be cleared (position=0, limit=capacity)
        assertEquals(0, reused.position());
        assertEquals(ByteBufferPool.MEDIUM_BUFFER_SIZE, reused.limit());
        assertEquals(ByteBufferPool.MEDIUM_BUFFER_SIZE, reused.capacity());
    }

    @Test
    @DisplayName("Should provide accurate stats toString")
    void testStatsToString() {
        ByteBuffer buffer = pool.acquire(ByteBufferPool.SMALL_BUFFER_SIZE);
        pool.release(buffer);

        ByteBufferPool.PoolStats stats = pool.getStats(ByteBufferPool.SMALL_BUFFER_SIZE);
        String statsString = stats.toString();

        assertNotNull(statsString);
        assertTrue(statsString.contains("size=" + ByteBufferPool.SMALL_BUFFER_SIZE));
        assertTrue(statsString.contains("acquires=1"));
        assertTrue(statsString.contains("releases=1"));
    }
}
