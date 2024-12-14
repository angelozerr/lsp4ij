package com.redhat.devtools.lsp4ij.debugger;

import org.eclipse.lsp4j.debug.Capabilities;
import org.jetbrains.annotations.Nullable;

public class DAPCapabilitiesUtils {

    public static boolean isSupportsTerminateRequest(@Nullable Capabilities capabilities) {
        return capabilities != null && Boolean.TRUE.equals(capabilities.getSupportsTerminateRequest());
    }

}
