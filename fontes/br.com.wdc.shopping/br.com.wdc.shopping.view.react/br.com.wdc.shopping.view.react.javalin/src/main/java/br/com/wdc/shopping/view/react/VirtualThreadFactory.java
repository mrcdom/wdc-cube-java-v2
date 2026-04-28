package br.com.wdc.shopping.view.react;

import java.util.concurrent.ThreadFactory;

/**
 * Factory for creating Virtual Threads (Java 21+).
 * 
 * Virtual Threads are lightweight threads that can significantly improve server throughput for I/O-intensive applications. They have the following advantages:
 * 
 * - Very low memory overhead (kilobytes per thread vs megabytes for platform threads) - Millions can be created without performance degradation - Automatic
 * switching to platform threads when OS I/O is required - Can replace thread pools for many workloads
 * 
 * Use Virtual Threads for: - I/O-bound tasks (HTTP requests, database queries, WebSocket operations) - Background scheduled tasks (cleanup, monitoring) - Tasks
 * that don't require CPU-intensive computations
 * 
 * Use Platform Threads for: - CPU-intensive computations that block (e.g., cryptographic operations) - Tasks that require native OS synchronization primitives
 * - Tasks that explicitly need thread-local storage or specific thread properties
 */
public class VirtualThreadFactory {

	private VirtualThreadFactory() {
		super();
	}

	/**
	 * Creates a ThreadFactory that produces virtual threads. Each virtual thread is daemon and inherits the name prefix with an auto-incremented counter.
	 * 
	 * @param namePrefix the prefix for virtual thread names (e.g., "SessionCleanup")
	 * @return a ThreadFactory producing virtual threads
	 */
	public static ThreadFactory ofVirtual(String namePrefix) {
		return Thread.ofVirtual().name(namePrefix + "-", 0).factory();
	}

	/**
	 * Creates a ThreadFactory that produces named virtual threads with auto-incrementing IDs.
	 * 
	 * @param namePrefix the prefix for thread names
	 * @param startId the starting ID for the counter
	 * @return a ThreadFactory producing virtual threads
	 */
	public static ThreadFactory ofVirtualWithId(String namePrefix, long startId) {
		return Thread.ofVirtual().name(namePrefix + "-", startId).factory();
	}

}
