package com.seibel.distanthorizons.core.render;

import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.enums.MinecraftTextFormat;
import com.seibel.distanthorizons.core.logging.DhLogger;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.util.ExceptionUtil;
import com.seibel.distanthorizons.core.util.TimerUtil;
import com.seibel.distanthorizons.core.util.objects.RollingAverage;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftRenderWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftSharedWrapper;
import com.seibel.distanthorizons.coreapi.ModInfo;
import org.jetbrains.annotations.Nullable;

import java.text.NumberFormat;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

public class RenderThreadTaskHandler
{
	private static final DhLogger LOGGER = new DhLoggerBuilder()
		.fileLevelConfig(Config.Common.Logging.logRendererEventToFile)
		.build();
	
	private static final DhLogger RATE_LIMITED_LOGGER = new DhLoggerBuilder()
		.fileLevelConfig(Config.Common.Logging.logRendererEventToFile)
		.maxCountPerSecond(4)
		.build();
	
	private static final ConcurrentLinkedQueue<QueuedRunnable> RENDER_THREAD_RUNNABLE_QUEUE = new ConcurrentLinkedQueue<>();
	
	private static final ConcurrentHashMap<String, RollingAverage> AVERAGE_NANO_RUN_TIME_BY_TASK_NAME = new ConcurrentHashMap<>();
	private static final LongAdder COMPLETED_TASK_COUNTER = new LongAdder();
	private static final NumberFormat DECIMAL_NUMBER_FORMAT = NumberFormat.getNumberInstance();
	private static final NumberFormat INT_NUMBER_FORMAT = NumberFormat.getIntegerInstance();
	private static final boolean LOG_SLOW_TASKS = false;
	
	private static final Timer TIMER = TimerUtil.CreateTimer("Cleanup timer");
	private static final long MS_BETWEEN_CLEANUP_TICKS = 1_000L;
	private static final long NANOS_BEFORE_RUN_CLEANUP_TIMER = TimeUnit.NANOSECONDS.convert(1_000L, TimeUnit.MILLISECONDS);
	
	
	public static final RenderThreadTaskHandler INSTANCE = new RenderThreadTaskHandler();
	
	
	private long nanoSinceTasksRun = System.nanoTime();
	private final boolean running; 
	
	private Thread renderThread;
	/** 
	 * the currently running {@link QueuedRunnable}
	 * will be null if nothing is running.
	 */
	private volatile @Nullable QueuedRunnable currentQueuedRunnable;
	
	
	
	//=============//
	// constructor //
	//=============//
	//region
	
	private RenderThreadTaskHandler() 
	{
		// we only want to run this when the client is available
		IMinecraftSharedWrapper mcShared = SingletonInjector.INSTANCE.get(IMinecraftSharedWrapper.class);
		if (!mcShared.isDedicatedServer())
		{
			LOGGER.debug("Starting ["+RenderThreadTaskHandler.class.getSimpleName()+"]...");
			this.running = true;
			TIMER.scheduleAtFixedRate(TimerUtil.createTimerTask(this::manualCleanupTick), MS_BETWEEN_CLEANUP_TICKS, MS_BETWEEN_CLEANUP_TICKS);
		}
		else
		{
			this.running = false;
			LOGGER.debug("Skipping ["+RenderThreadTaskHandler.class.getSimpleName()+"] startup due to running on a dedicated server.");
		}
	}
	
	//endregion
	
	
	
	//==============//
	// task queuing //
	//==============//
	//region
	
	public void queueRunningOnRenderThread(String name, Runnable renderCall)
	{
		// don't queuing tasks if they'll never be run
		if (!this.running)
		{
			return;
		}
		
		
		// don't get the stacktrace on release to reduce GC pressure
		StackTraceElement[] stackTrace = null;
		if (ModInfo.IS_DEV_BUILD)
		{
			stackTrace = Thread.currentThread().getStackTrace();
		}
		
		QueuedRunnable runnable = new QueuedRunnable(name, renderCall, stackTrace);
		RENDER_THREAD_RUNNABLE_QUEUE.add(runnable);
	}
	
	//endregion
	
	
	
	//===========//
	// run tasks //
	//===========//
	//region
	
	/**
	 * Doesn't do any thread/GL Context validation.
	 * Running this outside of the render thread may cause crashes or other issues. 
	 */
	public void runRenderThreadTasks()
	{
		IMinecraftRenderWrapper MC_RENDER = SingletonInjector.INSTANCE.get(IMinecraftRenderWrapper.class);
		
		// https://fpstoms.com/
		int frameLimit = MC_RENDER.getFrameLimit();
		if (frameLimit <= 1)
		{
			frameLimit = 240;
		}
		
		
		int msPerFrame = 1000 / frameLimit;
		long nanoPerFrame = msPerFrame * 1_000_000L;
		nanoPerFrame /= 2; // divide the time in half so we can only impact half of the framerate at worst
		this.runRenderThreadTasks(nanoPerFrame);
	}
	private void runRenderThreadTasks(long nanoMaxRunTime)
	{
		long loopStartTimeNano = System.nanoTime();
		this.nanoSinceTasksRun = loopStartTimeNano;
		
		
		if (this.renderThread == null)
		{
			this.renderThread = Thread.currentThread();
		}
		
		
		QueuedRunnable runnable = RENDER_THREAD_RUNNABLE_QUEUE.poll();
		while(runnable != null)
		{
			long taskStartNano = System.nanoTime();
			
			this.currentQueuedRunnable = runnable;
			runnable.run();
			this.currentQueuedRunnable = null;
			
			// only try running for a limited amount of time to prevent lag spikes
			long taskNano = System.nanoTime() - taskStartNano;
			long totalLoopNano = System.nanoTime() - loopStartTimeNano;
			
			// stat tracking
			if (ModInfo.IS_DEV_BUILD)
			{
				if (!AVERAGE_NANO_RUN_TIME_BY_TASK_NAME.containsKey(runnable.name))
				{
					AVERAGE_NANO_RUN_TIME_BY_TASK_NAME.put(runnable.name, new RollingAverage(1_000));
				}
				AVERAGE_NANO_RUN_TIME_BY_TASK_NAME.get(runnable.name).add(totalLoopNano);
				
				COMPLETED_TASK_COUNTER.increment();
			}
			
			
			// estimate when our ending nano-time would be once the next task is run
			long expectedNextTaskNano = totalLoopNano
				// doubling this task's time gives a rough over-estimate of how long the next task should take	
				+ (taskNano * 2);
			// If the next task would push us over the max run time, stop now.
			// This prevents stuttering at the cost of lower throughput. 
			if (expectedNextTaskNano >= nanoMaxRunTime)
			{
				if (LOG_SLOW_TASKS 
					&& totalLoopNano > nanoMaxRunTime)
				{
					// this task took longer than what we wanted
					RATE_LIMITED_LOGGER.warn("["+runnable.name+"] slow, actual ["+totalLoopNano+"], allowed ["+nanoMaxRunTime+"].");
				}
				
				break;
			}
			
			runnable = RENDER_THREAD_RUNNABLE_QUEUE.poll();
		}
	}
	
	/**
	 * Should only be called if our render code isn't being hit for some reason.
	 * Normally this only happens if there's a mod that limits MC's framerate to 0.
	 */
	private void manualCleanupTick()
	{
		long nowNano = System.nanoTime();
		long nanoSinceLast = nowNano - this.nanoSinceTasksRun;
		if (nanoSinceLast < NANOS_BEFORE_RUN_CLEANUP_TIMER)
		{
			return;
		}
		
		// We haven't gotten a frame for a while,
		// this means we could have GL jobs building up.
		// Run the queued tasks on MC's executor (hopefully this should always run,
		// even if DH's render code isn't being hit).
		IMinecraftClientWrapper mcClient = SingletonInjector.INSTANCE.get(IMinecraftClientWrapper.class);
		if (mcClient != null)
		{
			mcClient.executeOnRenderThread(() -> this.runRenderThreadTasks(500 * 1_000_000L));
		}
		else
		{
			// shouldn't happen, but just in case
			
			// somehow the timer started when there wasn't a client wrapper
			// this probably means the timer was started on a dedicated server
			RATE_LIMITED_LOGGER.warn("["+RenderThreadTaskHandler.class.getSimpleName()+"] timer started when ["+IMinecraftClientWrapper.class.getSimpleName()+"] is null. This shouldn't happen but can likely be ignored.");
		}
	}
	
	//endregion
	
	
	
	//===========//
	// debugging //
	//===========//
	//region
	
	/** 
	 * if tasks are currently queued the debug
	 * stats may not be zero after this method has been called.
	 */
	public void clearDebugStats()
	{
		AVERAGE_NANO_RUN_TIME_BY_TASK_NAME.clear();
		COMPLETED_TASK_COUNTER.reset();
	}
	
	public void addDebugMenuStringsToList(List<String> messageList)
	{
		if (!ModInfo.IS_DEV_BUILD)
		{
			return;
		}
		
		
		String o = MinecraftTextFormat.ORANGE;
		String g = MinecraftTextFormat.GREEN;
		String b = MinecraftTextFormat.DARK_BLUE;
		String y = MinecraftTextFormat.YELLOW;
		String cf = MinecraftTextFormat.CLEAR_FORMATTING;
		
		
		
		String queueSize = DECIMAL_NUMBER_FORMAT.format(RENDER_THREAD_RUNNABLE_QUEUE.size());
		String completedCount = DECIMAL_NUMBER_FORMAT.format(COMPLETED_TASK_COUNTER.sum());
		
		String messageHeader = "Render Tasks, Queue: "+o+queueSize+cf+", Done: "+g+completedCount+cf;
		messageList.add(messageHeader);
		
		AVERAGE_NANO_RUN_TIME_BY_TASK_NAME.forEach((name, rollingAverage) -> 
		{
			// thread runtime
			String runTimeAvgStr;
			double runTimeAvgInNano = rollingAverage.getAverage();
			if (!Double.isNaN(runTimeAvgInNano))
			{
				double runTimeAvgInMs = runTimeAvgInNano / 1_000_000.0;
				runTimeAvgStr = DECIMAL_NUMBER_FORMAT.format(runTimeAvgInMs);
			}
			else
			{
				runTimeAvgStr = "<0";
			}
			
			String lifetimeCount = INT_NUMBER_FORMAT.format(rollingAverage.getLifetimeCount());
			
			String message = name+" Avg: "+b+runTimeAvgStr+"ms"+cf+" #: "+y+lifetimeCount+cf;
			messageList.add(message);
		});
	}
	
	
	/** Returns true if the currently running thread is being run by this handler */
	public boolean isCurrentThread() 
	{
		if (this.renderThread != null)
		{
			return Thread.currentThread() == this.renderThread;
		}
		
		// shouldn't normally be needed, but can be used if this
		// handler hasn't been run yet
		return Thread.currentThread().getName().equals("Render thread");
	}
	
	/** 
	 * Only recommended to be used by the task that's currently being run.
	 * Use {@link RenderThreadTaskHandler#isCurrentThread()} to check. <br>
	 * Can be used to get stack traces for render thread tasks while they're being run. 
	 */
	public @Nullable QueuedRunnable getCurrentlyRunningTask() { return this.currentQueuedRunnable; }
	
	//endregion
	
	
	
	//================//
	// helper classes //
	//================//
	//region
	
	public static class QueuedRunnable implements Runnable
	{
		/** used to easily track what's being done on the render thread */
		public final String name;
		public final Runnable renderCall;
		/** will be null on release build to reduce GC pressure */
		@Nullable
		public final StackTraceElement[] stackTrace;
		
		
		
		//=============//
		// constructor //
		//=============//
		//region
		
		public QueuedRunnable(String name, Runnable renderCall, @Nullable StackTraceElement[] stackTrace)
		{
			this.name = name;
			this.renderCall = renderCall;
			this.stackTrace = stackTrace;
		}
		
		//endregion
		
		
		
		//=========//
		// running //
		//=========//
		//region
		
		@Override
		public void run()
		{
			try
			{
				this.renderCall.run();
			}
			catch (Exception e)
			{
				if (ExceptionUtil.isShutdownException(e))
				{
					return;
				}
				
				RuntimeException error = new RuntimeException("Uncaught Exception during GL call execution. StackTrace: ["+(this.stackTrace != null ? "Present" : "Missing")+"] Error: ["+e.getMessage()+"]", e);
				if (this.stackTrace != null)
				{
					error.setStackTrace(this.stackTrace);
				}
				LOGGER.error("[" + Thread.currentThread().getName() + "] ran into an unexpected error running a GL call, Error: ["+ e.getMessage() +"].", error);
			}
		}
		
		//endregion
		
		
		
		//================//
		// base overrides //
		//================//
		//region
		
		@Override
		public String toString() { return this.name; }
		
		//endregion
		
	}
	
	//endregion
	
	
	
}
