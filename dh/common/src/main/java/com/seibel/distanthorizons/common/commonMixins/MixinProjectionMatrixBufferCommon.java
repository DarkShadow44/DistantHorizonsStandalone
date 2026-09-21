package com.seibel.distanthorizons.common.commonMixins;

#if MC_VER <= MC_26_2_0

public class MixinProjectionMatrixBufferCommon {}

#else

import com.seibel.distanthorizons.common.wrappers.McObjectConverter;
import com.seibel.distanthorizons.core.api.internal.ClientApi;
import com.seibel.distanthorizons.core.util.math.DhMat4f;
import org.joml.Matrix4f;

public class MixinProjectionMatrixBufferCommon
{
	public static boolean inWorldRenderPass = false;
	
	
	public static void onMatrixWrite(Matrix4f matrix)
	{
		// ignore writes if we aren't in the world render pass
		if (!inWorldRenderPass)
		{
			return;
		}
		
		
		DhMat4f dhMatrix = McObjectConverter.convert(matrix);
		if (dhMatrix.equals(DhMat4f.IDENTITY))
		{
			// it's possible Mojang may pass in the identity matrix
			// to clear the old one,
			// we don't want that
			return;
		}
		
		ClientApi.RENDER_STATE.mcProjectionMatrix = dhMatrix;
	}
	
}
#endif
