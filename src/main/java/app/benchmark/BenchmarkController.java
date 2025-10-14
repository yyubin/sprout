package app.benchmark;

import sprout.beans.annotation.Controller;
import sprout.mvc.annotation.GetMapping;
import sprout.mvc.annotation.RequestMapping;
import sprout.mvc.annotation.RequestParam;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

/**
 * Performance benchmark controller for HTTP server testing
 */
@Controller
@RequestMapping("/benchmark")
public class BenchmarkController {

    /**
     * Simple hello world endpoint - baseline performance test
     */
    @GetMapping("/hello")
    public String hello() {
        return "Hello, World!";
    }

    /**
     * JSON response endpoint - test serialization performance
     */
    @GetMapping("/json")
    public String json() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("timestamp", System.currentTimeMillis());
        response.put("data", Map.of(
                "message", "Performance test response",
                "server", "Sprout HTTP Server",
                "version", "1.0"
        ));
        return response.toString();
    }

    /**
     * CPU-intensive endpoint - test server under CPU load
     * Calculates fibonacci number using iterative method
     */
    @GetMapping("/cpu")
    public String cpu(@RequestParam(required = false, defaultValue = "35") String n) {
        int num = Integer.parseInt(n);
        long result = fibonacci(num);
        return "Fibonacci(" + num + ") = " + result;
    }

    /**
     * Heavy CPU-intensive endpoint - prime number calculation
     */
    @GetMapping("/cpu-heavy")
    public String cpuHeavy(@RequestParam(required = false, defaultValue = "10000") String limit) {
        int max = Integer.parseInt(limit);
        int primeCount = countPrimes(max);
        return "Primes up to " + max + ": " + primeCount;
    }

    /**
     * I/O latency simulation endpoint - test async handling
     * Simulates database or external API call delay
     */
    @GetMapping("/latency")
    public String latency(@RequestParam(required = false, defaultValue = "100") String ms) {
        int delay = Integer.parseInt(ms);
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Interrupted";
        }
        return "Delayed response after " + delay + "ms";
    }

    /**
     * Mixed workload - combination of CPU and latency
     */
    @GetMapping("/mixed")
    public String mixed() {
        // Small CPU work
        long fib = fibonacci(20);

        // Small delay
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return "Mixed workload result: " + fib;
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public String health() {
        return "OK";
    }

    // Helper methods

    private long fibonacci(int n) {
        if (n <= 1) return n;

        long prev = 0, curr = 1;
        for (int i = 2; i <= n; i++) {
            long next = prev + curr;
            prev = curr;
            curr = next;
        }
        return curr;
    }

    private int countPrimes(int max) {
        if (max < 2) return 0;

        boolean[] isPrime = new boolean[max + 1];
        for (int i = 2; i <= max; i++) {
            isPrime[i] = true;
        }

        // Sieve of Eratosthenes
        for (int i = 2; i * i <= max; i++) {
            if (isPrime[i]) {
                for (int j = i * i; j <= max; j += i) {
                    isPrime[j] = false;
                }
            }
        }

        int count = 0;
        for (int i = 2; i <= max; i++) {
            if (isPrime[i]) count++;
        }
        return count;
    }
}
