package com.seibel.distanthorizons.forge;

import com.seibel.distanthorizons.core.wrapperInterfaces.modAccessor.IIrisAccessor;
import net.coderbot.iris.Iris;
import net.irisshaders.iris.api.v0.IrisApi;

public class IrisAccessor implements IIrisAccessor {
    @Override
    public String getModName()
    {
        return Iris.MODNAME;
    }

    @Override
    public boolean isShaderPackInUse()
    {
        return IrisApi.getInstance().isShaderPackInUse();
    }

    @Override
    public boolean isRenderingShadowPass()
    {
        return IrisApi.getInstance().isRenderingShadowPass();
    }
}
