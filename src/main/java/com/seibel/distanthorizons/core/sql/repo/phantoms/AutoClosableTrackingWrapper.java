package com.seibel.distanthorizons.core.sql.repo.phantoms;

import com.seibel.distanthorizons.coreapi.ModInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Set;

/**
 * This is used to detect leaks
 * with our JDBC implementation, specifically
 * to make sure all {@link AutoCloseable} objects
 * are inside try-finally resources.
 */
public class AutoClosableTrackingWrapper implements InvocationHandler
{
	//private static final Logger LOGGER = DhLoggerBuilder.getLogger();
	
	/** 
	 * should be enabled during development to
	 * notify if any resources are being leaked.
	 */
	public static final boolean TRACK_WRAPPERS = ModInfo.IS_DEV_BUILD;
	
	@NotNull
	public final AutoCloseable wrappedClosable;
	@NotNull
	public final Set<AutoClosableTrackingWrapper> parentTrackingSet;
	
	
	
	//==============//
	// constructors //
	//==============//
	
	@Nullable
	public static <TStatic extends AutoCloseable> TStatic wrap(
			@NotNull Class<?> clazz,
			@Nullable TStatic wrappedClosable,
			@NotNull Set<AutoClosableTrackingWrapper> trackingSet)
	{
		if (!TRACK_WRAPPERS)
		{
			return wrappedClosable;
		}
		
		// done to prevent null pointers
		if (wrappedClosable == null)
		{
			return null;
		}
		
		return (TStatic) Proxy.newProxyInstance(
				wrappedClosable.getClass().getClassLoader(),
				new Class[]{ clazz },
				new AutoClosableTrackingWrapper(wrappedClosable, trackingSet)
		);
	}
	
	private AutoClosableTrackingWrapper(@NotNull AutoCloseable wrappedClosable, @NotNull Set<AutoClosableTrackingWrapper> trackingSet)
	{
		this.wrappedClosable = wrappedClosable;
		this.parentTrackingSet = trackingSet;
		this.parentTrackingSet.add(this);
	}
	
	
	
	//============//
	// reflection //
	//============//
	
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
	{
		// Track the close() method
		if ("close".equals(method.getName()))
		{
			this.wrappedClosable.close();
			this.parentTrackingSet.remove(this);
			return null;
		}
		
		try
		{
			// Delegate all other methods to the wrapped object
			return method.invoke(this.wrappedClosable, args);	
		}
		catch (InvocationTargetException e)
		{
			// get the target so we can filter the exception correctly up-stream
			throw e.getTargetException();
		}
	}
	
	
	
}
