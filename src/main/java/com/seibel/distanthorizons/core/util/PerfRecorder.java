package com.seibel.distanthorizons.core.util;

import com.seibel.distanthorizons.core.logging.DhLogger;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

/**
 * Can be used to track relative performance when attempting to optimize specific methods. <br>
 * Example: <br>
 * <code>
 * PerfRecorder.Timer upload = this.filePerfRecorder.start("upload");  <br>
 * this.uploadToGpuAsync(lodQuadBuilder);  <br>
 * upload.end();  <br>
 * </code>
 * <br>
 * Note: there is some overhead to the timers starting/ending 
 * so times will not be exact, especially in very fast methods. <br>
 * In those contexts a dedicated profiler will be better.
 */
public class PerfRecorder
{
	private static final int TOTAL_WIDTH = 7;
	/**
	 * Examples: <br>
	 * <code>
	 * "123,456" <br>
	 * "  3,456" <br>
	 * "    456" <br>
	 * "      6" <br>
	 * </code>
	 */
	private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("###,###");
	
	
	/** 
	 * different loggers are needed for each recorder
	 * to prevent them from all sharing the same
	 * {@link DhLogger#canLog()} limit.
	 */
	private final DhLogger logger;
	
	public final String name;
	
	public final ConcurrentHashMap<String, LongAdder> nanoPerId = new ConcurrentHashMap<>();
	public String lastPerfLog = "";
	
	
	
	//=============//
	// constructor //
	//=============//
	
	public PerfRecorder(String name)
	{ 
		this.name = name;
		
		this.logger = new DhLoggerBuilder()
				.name(PerfRecorder.class.getSimpleName()+"-"+this.name)
				.maxCountPerSecond(1)
				.build();
	}
	
	
	
	//=====================//
	// performance logging //
	//=====================//
	
	public Timer start(String id) { return new Timer(id); }
	
	public void clear() { this.nanoPerId.clear(); }
	
	/**
	 * Will log if the message changed
	 * and enough time has passed as defined by
	 * the parent {@link DhLogger#canLog}
	 */
	public void tryLog()
	{
		if (this.logger.canLog())
		{
			String perfTime = this.toString();
			if (!perfTime.equals(this.lastPerfLog))
			{
				this.lastPerfLog = perfTime;
				this.logger.info(perfTime);
			}
		}
	}
	
	
	
	//===================//
	// default overrides //
	//===================//
	
	@Override
	public String toString()
	{
		// sort keys
		// (done so we can easily and consistently compare different runs)
		ArrayList<String> sortedKeys = new ArrayList<>();
		Enumeration<String> keyEnumerator = this.nanoPerId.keys();
		while (keyEnumerator.hasMoreElements())
		{
			sortedKeys.add(keyEnumerator.nextElement());
		}
		sortedKeys.sort(Comparator.naturalOrder());
		
		
		
		// build the string
		StringBuilder builder = new StringBuilder();
		long totalNanoTime = 0;
		
		for(String id : sortedKeys)
		{
			// convert nanoseconds to milliseconds for easier reading
			LongAdder nanoTimeAdder = this.nanoPerId.get(id);
			long nsTime = nanoTimeAdder.sum();
			long msTime = TimeUnit.MILLISECONDS.convert(nsTime, TimeUnit.NANOSECONDS);
			totalNanoTime += nsTime;
			
			String line = id+"["+formatNumber(msTime)+"] ";
			builder.append(line);
		}
		
		// add the total time
		long totalMsTime = TimeUnit.MILLISECONDS.convert(totalNanoTime, TimeUnit.NANOSECONDS);
		return "Total["+formatNumber(totalMsTime)+"] " + builder.toString();
	}
	private static String formatNumber(long number) 
	{
		String formattedNumber = DECIMAL_FORMAT.format(number);
		return String.format("%" + TOTAL_WIDTH + "s", formattedNumber);
	}
	
	
	
	//==============//
	// helper class //
	//==============//
	
	public class Timer
	{
		protected final String id;
		protected final long startTime;
		
		
		
		//=============//
		// constructor //
		//=============//
		
		private Timer(String id)
		{
			this.id = id;
			this.startTime = System.nanoTime();
		}
		
		
		//===============//
		// timer methods //
		//===============//
		
		public void end()
		{
			long endTime = System.nanoTime();
			long totalNano = endTime - this.startTime;
			
			LongAdder nsAdder = PerfRecorder.this.nanoPerId.get(this.id);
			if (nsAdder != null)
			{
				nsAdder.add(totalNano);
				return;
			}
			
			nsAdder = PerfRecorder.this.nanoPerId.computeIfAbsent(this.id, (String id) -> new LongAdder());
			nsAdder.add(totalNano);
		}
	}
	
	
	
}
