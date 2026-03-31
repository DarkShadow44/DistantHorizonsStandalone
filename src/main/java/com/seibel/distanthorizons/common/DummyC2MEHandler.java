package com.seibel.distanthorizons.common;

import com.seibel.distanthorizons.core.wrapperInterfaces.modAccessor.IC2meAccessor;

public class DummyC2MEHandler implements IC2meAccessor {

    @Override
    public String getModName() {
        return "Dummy C2ME";
    }
}
