package com.seibel.distanthorizons.common.util.threading;

import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftSharedWrapper;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;

/**
 * Queues work that must run on the Minecraft server thread. The platform's
 * server-tick callback is responsible for calling {@link #runTasks(long)}.
 */
public class ServerThreadTaskHandler
{
	public static final ServerThreadTaskHandler INSTANCE = new ServerThreadTaskHandler();

	private final ConcurrentLinkedQueue<QueuedTask<?>> deferrableTaskQueue = new ConcurrentLinkedQueue<>();
	private final ConcurrentLinkedQueue<QueuedTask<?>> essentialTaskQueue = new ConcurrentLinkedQueue<>();
	private IMinecraftSharedWrapper mcSharedWrapper = null;
	private volatile boolean isShutdown;



	private ServerThreadTaskHandler() { }



	/**
	 * Queues a task that may be deferred to a later tick while the server thread is unhealthy. <br>
	 * Use this for work that only adds load, like requesting a chunk.
	 */
	public <T> CompletableFuture<T> queueTask(Supplier<T> task)
	{ return addTask(this.deferrableTaskQueue, task); }

	/**
	 * Queues a task that runs even while the server thread is unhealthy. <br>
	 * Use this for work that removes load, like releasing a chunk. Deferring that
	 * work would keep chunks loaded exactly when memory pressure is at its worst.
	 */
	public <T> CompletableFuture<T> queueEssentialTask(Supplier<T> task)
	{
		return addTask(this.essentialTaskQueue, task);
	}

	private <T> CompletableFuture<T> addTask(ConcurrentLinkedQueue<QueuedTask<?>> taskQueue, Supplier<T> task)
	{
		CompletableFuture<T> future = new CompletableFuture<>();
		if (this.isShutdown)
		{
			cancelFuture(future);
			return future;
		}

		QueuedTask<T> queuedTask = new QueuedTask<>(task, future);
		taskQueue.add(queuedTask);

		// Close the race where shutdown drains the queue immediately before this add.
		if (this.isShutdown && taskQueue.remove(queuedTask))
		{
			cancelFuture(future);
		}

		return future;
	}

	/**
	 * Runs queued tasks on the server thread. <br>
	 * Essential tasks run first and are never gated on server health, deferrable
	 * tasks only run once the server thread is healthy. Both draw from the same
	 * time budget, and at least one task is run even when that task alone
	 * exceeds the supplied budget.
	 */
	public void runTasks(long maxRunTimeNano)
	{
		long deadlineNano = System.nanoTime() + maxRunTimeNano;

		// note: if essential tasks keep using up the whole budget then deferrable
		// tasks will starve. That's acceptable, since essential tasks only exist
		// because deferrable ones already ran, so the backlog drains instead of deadlocking.
		if (!runQueueUntilDeadline(this.essentialTaskQueue, deadlineNano))
		{
			return;
		}

		if (this.deferrableTaskQueue.isEmpty())
		{
			return;
		}

		if (!this.deferrableTasksCanRun())
		{
			return;
		}

		runQueueUntilDeadline(this.deferrableTaskQueue, deadlineNano);
	}
	
	private boolean deferrableTasksCanRun()
	{
		if (!Config.Common.WorldGenerator.limitIfServerUnhealthy.get())
		{
			return true;
		}

		return mcSharedWrapper == null || mcSharedWrapper.isServerThreadHealthy();
	}

	/** @return true if the queue was emptied, false if the deadline was reached first. */
	private static boolean runQueueUntilDeadline(ConcurrentLinkedQueue<QueuedTask<?>> taskQueue, long deadlineNano)
	{
		QueuedTask<?> queuedTask;
		while ((queuedTask = taskQueue.poll()) != null)
		{
			queuedTask.run();
			if (System.nanoTime() >= deadlineNano)
			{
				return false;
			}
		}

		return true;
	}

	/** Completes queued tasks exceptionally without running them. */
	public void cancelPendingTasks()
	{
		// Set this before draining so concurrent submissions either get drained or
		// observe shutdown themselves after entering the queue.
		this.isShutdown = true;
		cancelQueuedTasks(this.essentialTaskQueue);
		cancelQueuedTasks(this.deferrableTaskQueue);
	}

	/** Prepares this singleton handler for a new Minecraft server session. */
	public void reset()
	{
		this.isShutdown = false;
		if (mcSharedWrapper == null) {
			mcSharedWrapper = SingletonInjector.INSTANCE.get(IMinecraftSharedWrapper.class);
		}
	}

	private static void cancelQueuedTasks(ConcurrentLinkedQueue<QueuedTask<?>> taskQueue)
	{
		QueuedTask<?> queuedTask;
		while ((queuedTask = taskQueue.poll()) != null)
		{
			cancelFuture(queuedTask.future);
		}
	}
	private static void cancelFuture(CompletableFuture<?> future)
	{
		future.completeExceptionally(
			new CancellationException("The Minecraft server stopped before the queued task could run."));
	}

	private static class QueuedTask<T>
	{
		private final Supplier<T> task;
		private final CompletableFuture<T> future;

		private QueuedTask(Supplier<T> task, CompletableFuture<T> future)
		{
			this.task = task;
			this.future = future;
		}

		private void run()
		{
			try
			{
				this.future.complete(this.task.get());
			}
			catch (Throwable throwable)
			{
				this.future.completeExceptionally(throwable);
			}
		}
	}
}
