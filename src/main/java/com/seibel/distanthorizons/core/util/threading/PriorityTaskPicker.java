package com.seibel.distanthorizons.core.util.threading;

import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.core.config.listeners.IConfigListener;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.util.objects.RollingAverage;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

/**
 * This handles dividing work DH needs to do across
 * DH's thread pool.
 */
public class PriorityTaskPicker
{
	private static final Logger LOGGER = DhLoggerBuilder.getLogger();
	
	
	/** the list of currently registered executors */
	private final ArrayList<Executor> executors = new ArrayList<>();
	
	/** Lock to ensure task picking logic is thread-safe */
	private final ReentrantLock taskPickerLock = new ReentrantLock();
	/** Tracks the number of active threads */
	private final AtomicInteger occupiedThreadsRef = new AtomicInteger(0);
	
	private final AtomicBoolean isShutDownRef = new AtomicBoolean(false);
	
	
	
	//==========//
	// executor //
	//==========//
	
	public Executor createExecutor(String name)
	{
		Executor executor = new Executor(this, name);
		this.executors.add(executor);
		return executor;
	}
	
	/**
	 * Tries to start the next queued task
	 * for one of the available executors.
	 */
	private void tryStartNextTask()
	{
		// only let one thread start the next task to prevent concurrency errors
		if (!this.taskPickerLock.tryLock())
		{
			return;
		}
		
		
		try
		{
			// Limit how many tasks can be queued for a given pool before moving to the next pool.
			// This allows the picker to spread out the work a little more vs having the threads
			// only work on a single executor's queue at a time
			int maxQueuedBeforeOverflow = Math.max(1, Config.Common.MultiThreading.numberOfThreads.get() / 2);
			
			// fill up executors that have run for less time first,
			// this prevents long-running tasks from taking up all the CPU time
			Iterator<Executor> iterator = this.getExecutorIteratorSortedByShortestTotalRunTime();
			while (iterator.hasNext())
			{
				Executor executor = iterator.next();
				int queuedTaskCount = 0;
				
				TrackedRunnable task;
				
				// start tasks until we're running as many threads as acceptable by the config,
				// or until this executor is empty,
				// or until we should move on to the next executor
				while (this.occupiedThreadsRef.get() < Config.Common.MultiThreading.numberOfThreads.get()
						&& queuedTaskCount <= maxQueuedBeforeOverflow
						&& (task = executor.taskQueue.poll()) != null)
				{
					queuedTaskCount++;
					
					try
					{
						executor.runTask(task);
						this.occupiedThreadsRef.getAndIncrement();
					}
					catch (RejectedExecutionException e)
					{
						if (this.isShutDownRef.get())
						{
							// Clear this executor's tasks since we no longer expect anything to execute.
							executor.taskQueue.clear();
						}
						else
						{
							throw e;
						}
					}
				}
			}
		}
		finally
		{
			this.taskPickerLock.unlock();
		}
	}
	private Iterator<Executor> getExecutorIteratorSortedByShortestTotalRunTime()
	{
		Stream<Executor> stream = this.executors.stream();
		// returns smaller numbers first
		stream = stream.sorted(Comparator.comparingLong((executor) -> executor.totalRuntimeNanos.get()));
		return stream.iterator();
	}
	
	/** Blocking, shuts down the thread pool immediately, stopping all tasks. */
	public void shutdownNow()
	{
		LOGGER.info("Shutting down PriorityTaskPicker thread pool...");
		this.isShutDownRef.set(true);
		
		try
		{
			for (int i = 0; i < this.executors.size(); i++)
			{
				Executor executor = this.executors.get(i);
				if (executor != null)
				{
					executor.shutdown();
					if (!executor.awaitTermination(5, TimeUnit.SECONDS))
					{
						executor.shutdownNow();
					}
				}
			}
		}
		catch (InterruptedException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	
	
	//================//
	// helper classes //
	//================//
	
	/**
	 * Each executor handles a specific type of work that DH needs done.
	 * By separating out task into its own executor it allows for easier performance monitoring
	 * via a tool like Visual VM and fairly spreading out CPU time between tasks.
	 */
	public static class Executor extends AbstractExecutorService implements IConfigListener
	{
		private final PriorityTaskPicker parentTaskPicker;
		private final String name;
		
		private final Queue<TrackedRunnable> taskQueue = new ConcurrentLinkedQueue<>();
		
		private final AtomicInteger runningTasksRef = new AtomicInteger(0);
		private final AtomicInteger completedTasksRef = new AtomicInteger(0);
		
		private final AtomicLong totalRuntimeNanos = new AtomicLong(0);
		/** used for performance logging */
		private final RollingAverage runTimeInMsRollingAverage = new RollingAverage(200);
		
		/** holds the threads this {@link Executor} can run */
		private RateLimitedThreadPoolExecutor threadPoolExecutor;
		
		
		
		//=============//
		// constructor //
		//=============//
		
		public Executor(PriorityTaskPicker parentTaskPicker, String name)
		{
			this.parentTaskPicker = parentTaskPicker;
			this.name = name;
			
			this.threadPoolExecutor = this.createThreadPool();
			
			Config.Common.MultiThreading.numberOfThreads.addListener(this);
		}
		
		private RateLimitedThreadPoolExecutor createThreadPool()
		{
			return new RateLimitedThreadPoolExecutor(
					Config.Common.MultiThreading.numberOfThreads.get(),
					new DhThreadFactory(this.name, Thread.MIN_PRIORITY, false),
					new ArrayBlockingQueue<>(Runtime.getRuntime().availableProcessors())
			);
		}
		
		
		
		//=================//
		// config handling //
		//=================//
		
		@Override
		public void onConfigValueSet() 
		{
			RateLimitedThreadPoolExecutor oldExecutor = this.threadPoolExecutor;
			this.threadPoolExecutor = this.createThreadPool();
			
			// shut down the old executor after replacing it with the new one
			// to make sure no tasks are lost in the transfer
			if (oldExecutor != null)
			{
				oldExecutor.shutdown();
			}
		}
		
		
		
		//=====================//
		// task queue handling //
		//=====================//
		
		@Override
		public void execute(@NotNull Runnable command)
		{
			this.taskQueue.add(new TrackedRunnable(this.parentTaskPicker, this, command));
			
			// Attempt to start the task immediately
			this.parentTaskPicker.tryStartNextTask();
		}
		
		/** The passed in {@link Runnable} must be exactly the same as the one passed into {@link PriorityTaskPicker.Executor#execute(Runnable)} */
		public void remove(@NotNull Runnable command) { this.taskQueue.removeIf(trackedRunnable -> trackedRunnable.command == command); }
		
		
		public void runTask(@NotNull Runnable command)
		{ 
			this.threadPoolExecutor.execute(command);
			this.runningTasksRef.getAndIncrement();
		}
		
		
		public int getQueueSize() { return this.taskQueue.size(); }
		public int getPoolSize() { return Config.Common.MultiThreading.numberOfThreads.get(); }
		
		public int getRunningTaskCount() { return this.runningTasksRef.get(); }
		public int getCompletedTaskCount() { return this.completedTasksRef.get(); }
		/** Will return NaN if nothing has been submitted yet */
		public double getAverageRunTimeInMs() { return this.runTimeInMsRollingAverage.getAverage(); }
		
		
		
		//==========//
		// shutdown //
		//==========//
		
		@Override
		public void shutdown() { this.threadPoolExecutor.shutdown(); }
		
		@Override
		public @NotNull List<Runnable> shutdownNow() { return this.threadPoolExecutor.shutdownNow(); }
		
		@Override
		public boolean isShutdown() { return this.threadPoolExecutor.isShutdown(); }
		@Override
		public boolean isTerminated() { return this.threadPoolExecutor.isTerminated(); }
		
		@Override
		public boolean awaitTermination(long timeout, @NotNull TimeUnit unit) throws InterruptedException 
		{ return this.threadPoolExecutor.awaitTermination(timeout, unit); }
		
	}
	
	/** used so we can {@link PriorityTaskPicker.Executor#remove(Runnable)} using the original {@link Runnable} */
	private static class TrackedRunnable implements Runnable
	{
		private final PriorityTaskPicker parentTaskPicker;
		private final Executor executor;
		
		/** the runnable passed into {@link PriorityTaskPicker.Executor#execute(Runnable)} */
		public final Runnable command;
		
		
		
		//=============//
		// constructor //
		//=============//
		
		public TrackedRunnable(PriorityTaskPicker parentTaskPicker, Executor executor, Runnable command)
		{
			this.parentTaskPicker = parentTaskPicker;
			this.executor = executor;
			this.command = command;
		}
		
		
		
		//=========//
		// running //
		//=========//
		
		@Override
		public void run()
		{
			long startTime = System.nanoTime();
			try
			{
				this.command.run();
			}
			finally
			{
				long timeElapsed = System.nanoTime() - startTime;
				this.executor.runTimeInMsRollingAverage.addValue(TimeUnit.NANOSECONDS.toMillis(timeElapsed));
				
				// Update variables related to task status
				this.parentTaskPicker.occupiedThreadsRef.getAndDecrement();
				this.executor.runningTasksRef.getAndDecrement();
				this.executor.completedTasksRef.getAndIncrement();
				this.executor.totalRuntimeNanos.addAndGet(timeElapsed);
				
				this.parentTaskPicker.tryStartNextTask();
			}
		}
		
	}
	
	
	
}
