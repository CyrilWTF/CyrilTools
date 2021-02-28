package me.cyril.cyriltools;

import android.net.TrafficStats;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@SuppressWarnings({"ConstantConditions", "JavaReflectionMemberAccess"})
public class Reflector {
    public static long GetRxBytes(String iface) {
        try {
            Method getRxBytes = TrafficStats.class.getMethod("getRxBytes", String.class);
            return (long) getRxBytes.invoke(null, iface);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static long GetTxBytes(String iface) {
        try {
            Method getTxBytes = TrafficStats.class.getMethod("getTxBytes", String.class);
            return (long) getTxBytes.invoke(null, iface);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return -1;
    }
}
