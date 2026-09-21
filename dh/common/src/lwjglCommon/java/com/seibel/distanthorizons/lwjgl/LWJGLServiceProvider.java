package com.seibel.distanthorizons.lwjgl;

/**
 * Loads LWJGLService via ServiceLoader, picking highest priority.
 */
public final class LWJGLServiceProvider 
{
    public static final ILWJGLService LWJGL = createInstance();
    public static final int POINTER_SIZE = LWJGL.getPointerSize();
    public static final long NULL = 0L;

	
	
    private LWJGLServiceProvider() {}
	
	
	
	static ILWJGLService constructInstance(String className)
	{
		try
		{
			var clz = Class.forName(className);
			var method = clz.getDeclaredMethod("create");
			return (ILWJGLService) method.invoke(null);
		}
		catch (ReflectiveOperationException e)
		{
			throw new AssertionError(e);
		}
	}

    static ILWJGLService createInstance() 
    {
        try 
        {
            Class.forName("org.lwjgl.opengl.GL11C");
            return constructInstance("com.seibel.distanthorizons.lwjgl.lwjgl3.LWJGL3Service");
        } 
		catch (ClassNotFoundException e) 
		{
            return constructInstance("com.seibel.distanthorizons.lwjgl.lwjgl2.LWJGL2Service");
        }
    }
	
	
	
}
