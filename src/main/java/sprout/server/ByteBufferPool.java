package sprout.server;

import sprout.beans.InfrastructureBean;
import sprout.beans.annotation.Component;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class ByteBufferPool implements InfrastructureBean {

    private static class PoolConfig {
        final int bufferSize;
        final int maxPoolSize;
        final ConcurrentLinkedQueue<ByteBuffer> pool;
        final AtomicLong acquireCount = new AtomicLong(0);
        final AtomicLong releaseCount = new AtomicLong(0);
        final AtomicLong allocateCount = new AtomicLong(0);

        PoolConfig(int bufferSize, int maxPoolSize) {
            this.bufferSize = bufferSize;
            this.maxPoolSize = maxPoolSize;
            this.pool = new ConcurrentLinkedQueue<>();
        }
    }

    // Predefined buffer sizes
    public static final int SMALL_BUFFER_SIZE = 2048;      // 2KB for protocol detection
    public static final int MEDIUM_BUFFER_SIZE = 8192;     // 8KB for read operations
    public static final int LARGE_BUFFER_SIZE = 32768;     // 32KB for large responses

    private static final int DEFAULT_MAX_POOL_SIZE = 500;

    private final ConcurrentHashMap<Integer, PoolConfig> pools;
    private final boolean useDirect;

    public ByteBufferPool() {
        this(false);
    }

    public ByteBufferPool(boolean useDirect) {
        this.useDirect = useDirect;
        this.pools = new ConcurrentHashMap<>();

        // Initialize default pools
        initializePool(SMALL_BUFFER_SIZE, DEFAULT_MAX_POOL_SIZE);
        initializePool(MEDIUM_BUFFER_SIZE, DEFAULT_MAX_POOL_SIZE);
        initializePool(LARGE_BUFFER_SIZE, DEFAULT_MAX_POOL_SIZE / 5); // Fewer large buffers
    }

    public void initializePool(int bufferSize, int maxPoolSize) {
        pools.put(bufferSize, new PoolConfig(bufferSize, maxPoolSize));
    }

    public ByteBuffer acquire(int size) {
        int poolSize = findPoolSize(size);
        PoolConfig config = pools.get(poolSize);

        if (config == null) {
            // No pool for this size, allocate directly
            return allocateBuffer(size);
        }

        config.acquireCount.incrementAndGet();

        ByteBuffer buffer = config.pool.poll();
        if (buffer != null) {
            // Got buffer from pool, reset it
            buffer.clear();
            return buffer;
        }

        // Pool is empty, allocate new buffer
        config.allocateCount.incrementAndGet();
        return allocateBuffer(poolSize);
    }

    public void release(ByteBuffer buffer) {
        if (buffer == null) {
            return;
        }

        int capacity = buffer.capacity();
        PoolConfig config = pools.get(capacity);

        if (config == null) {
            // Not a pooled size, let it be GC'd
            return;
        }

        config.releaseCount.incrementAndGet();

        // Check if pool is full
        if (config.pool.size() >= config.maxPoolSize) {
            // Pool is full, discard buffer (will be GC'd)
            return;
        }

        // Clear buffer and return to pool
        buffer.clear();
        config.pool.offer(buffer);
    }

    private int findPoolSize(int requestedSize) {
        if (requestedSize <= SMALL_BUFFER_SIZE) {
            return SMALL_BUFFER_SIZE;
        } else if (requestedSize <= MEDIUM_BUFFER_SIZE) {
            return MEDIUM_BUFFER_SIZE;
        } else if (requestedSize <= LARGE_BUFFER_SIZE) {
            return LARGE_BUFFER_SIZE;
        }
        // For very large buffers, return the requested size (no pooling)
        return requestedSize;
    }

    private ByteBuffer allocateBuffer(int size) {
        return useDirect ? ByteBuffer.allocateDirect(size) : ByteBuffer.allocate(size);
    }

    public PoolStats getStats(int bufferSize) {
        PoolConfig config = pools.get(bufferSize);
        if (config == null) {
            return null;
        }

        return new PoolStats(
            bufferSize,
            config.pool.size(),
            config.maxPoolSize,
            config.acquireCount.get(),
            config.releaseCount.get(),
            config.allocateCount.get()
        );
    }

    public static class PoolStats {
        public final int bufferSize;
        public final int currentPoolSize;
        public final int maxPoolSize;
        public final long acquireCount;
        public final long releaseCount;
        public final long allocateCount;

        public PoolStats(int bufferSize, int currentPoolSize, int maxPoolSize,
                         long acquireCount, long releaseCount, long allocateCount) {
            this.bufferSize = bufferSize;
            this.currentPoolSize = currentPoolSize;
            this.maxPoolSize = maxPoolSize;
            this.acquireCount = acquireCount;
            this.releaseCount = releaseCount;
            this.allocateCount = allocateCount;
        }

        public double getHitRate() {
            if (acquireCount == 0) {
                return 0.0;
            }
            long hits = acquireCount - allocateCount;
            return (hits * 100.0) / acquireCount;
        }

        public double getUtilization() {
            if (maxPoolSize == 0) {
                return 0.0;
            }
            return (currentPoolSize * 100.0) / maxPoolSize;
        }

        @Override
        public String toString() {
            return String.format(
                "PoolStats{size=%d, pool=%d/%d, acquires=%d, releases=%d, allocations=%d, hitRate=%.2f%%, utilization=%.2f%%}",
                bufferSize, currentPoolSize, maxPoolSize, acquireCount, releaseCount,
                allocateCount, getHitRate(), getUtilization()
            );
        }
    }

    public void clear() {
        for (PoolConfig config : pools.values()) {
            config.pool.clear();
        }
    }

    public int getTotalBuffersInPool() {
        return pools.values().stream()
                .mapToInt(config -> config.pool.size())
                .sum();
    }
}
