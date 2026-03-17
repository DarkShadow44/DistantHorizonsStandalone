package com.seibel.distanthorizons.core.render;

import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.logging.DhLogger;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.util.TimerUtil;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftRenderWrapper;

import java.util.Timer;
import java.util.concurrent.ConcurrentLinkedQueue;

public class RenderThreadTaskHandler
{
	public static final DhLogger LOGGER = new DhLoggerBuilder()
		.fileLevelConfig(Config.Common.Logging.logRendererEventToFile)
		.build();
	
	private static final ConcurrentLinkedQueue<Runnable> RENDER_THREAD_RUNNABLE_QUEUE = new ConcurrentLinkedQueue<>();
	
	private static final Timer TIMER = TimerUtil.CreateTimer("Cleanup timer");
	private static final long MS_BETWEEN_CLEANUP_TICKS = 1_000L;
	private static final long MS_BEFORE_RUN_CLEANUP_TIMER = 1_000L;
	
	
	public static final RenderThreadTaskHandler INSTANCE = new RenderThreadTaskHandler();
	
	
	private long msSinceGlTasksRun = System.currentTimeMillis();
	
	
	
	//=============//
	// constructor //
	//=============//
	//region
	
	private RenderThreadTaskHandler() { TIMER.scheduleAtFixedRate(TimerUtil.createTimerTask(this::manualCleanupTick), MS_BETWEEN_CLEANUP_TICKS, MS_BETWEEN_CLEANUP_TICKS); }
	
	//endregion
	
	
	
	//==============//
	// task queuing //
	//==============//
	//region
	
	public void queueRunningOnRenderThread(Runnable renderCall)
	{
		StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
		RENDER_THREAD_RUNNABLE_QUEUE.add(() -> this.createRenderThreadRunnable(renderCall, stackTrace));
	}
	private void createRenderThreadRunnable(Runnable renderCall, StackTraceElement[] stackTrace)
	{
		try
		{
			renderCall.run();
		}
		catch (Exception e)
		{
			RuntimeException error = new RuntimeException("Uncaught Exception during GL call execution:", e);
			error.setStackTrace(stackTrace);
			LOGGER.error("[" + Thread.currentThread().getName() + "] ran into an unexpected error running a GL call, Error: ["+ e.getMessage() +"].", error);
		}
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
		
		int frameLimit = MC_RENDER.getFrameLimit();
		if (frameLimit <= 1)
		{
			frameLimit = 4; // 240 FPS
		}
		
		// https://fpstoms.com/
		int msPerFrame = 1000 / frameLimit;
		this.runRenderThreadTasks(msPerFrame);
	}
	private void runRenderThreadTasks(long msMaxRunTime)
	{
		long startTimeMs = System.currentTimeMillis();
		this.msSinceGlTasksRun = startTimeMs;
		
		Runnable runnable = RENDER_THREAD_RUNNABLE_QUEUE.poll();
		while(runnable != null)
		{
			runnable.run();
			
			// only try running for a limited amount of time to prevent lag spikes
			long currentTimeMs = System.currentTimeMillis();
			long runDuration = currentTimeMs - startTimeMs;
			if (runDuration > msMaxRunTime)
			{
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
		long nowMs = System.currentTimeMillis();
		long msSinceLast = nowMs - this.msSinceGlTasksRun;
		if (msSinceLast > MS_BEFORE_RUN_CLEANUP_TIMER)
		{
			return;
		}
		
		// We haven't gotten a frame for a while,
		// this means we could have GL jobs building up.
		// Run the queued tasks on MC's executor (hopefully this should always run,
		// even if DH's render code isn't being hit).
		IMinecraftClientWrapper MC = SingletonInjector.INSTANCE.get(IMinecraftClientWrapper.class);
		MC.executeOnRenderThread(() -> this.runRenderThreadTasks(1_000));
	}
	
	//end region
	
	
	
}
