package com.asbresearch.common;

import com.sun.management.HotSpotDiagnosticMXBean;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import javax.management.MBeanServer;
import lombok.experimental.UtilityClass;

@UtilityClass
public class JvmUtil {

    public static final String HOT_SPOT_DIAGNOSTIC_BEAN = "com.sun.management:type=HotSpotDiagnostic";

    public static void dumpHeap(String filePath, boolean live) throws IOException {
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        HotSpotDiagnosticMXBean mxBean = ManagementFactory.newPlatformMXBeanProxy(server, HOT_SPOT_DIAGNOSTIC_BEAN, HotSpotDiagnosticMXBean.class);
        mxBean.dumpHeap(filePath, live);
    }
}
