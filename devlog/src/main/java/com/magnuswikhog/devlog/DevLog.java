package com.magnuswikhog.devlog;

import android.content.Context;
import android.util.Log;

import org.jetbrains.annotations.NonNls;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

public class DevLog {
    public static boolean loggingEnabled = false;
    public static boolean remoteLoggingEnabled = false;
    public static String tagPrefix = "";


    public static void i(@NonNls String tag, @NonNls String string, boolean alwaysLog) {
        if (loggingEnabled || alwaysLog ) {
            Log.i(tagPrefix+tag, string);
            if( remoteLoggingEnabled ){
                RemoteLogger.i(tagPrefix+tag, string);
            }
        }
    }
    public static void e(@NonNls String tag, @NonNls String string, boolean alwaysLog) {
        if (loggingEnabled || alwaysLog ) {
            Log.e(tagPrefix+tag, string);
            if( remoteLoggingEnabled ){
                RemoteLogger.e(tagPrefix+tag, string);
            }
        }
    }
    public static void d(@NonNls String tag, @NonNls String string, boolean alwaysLog) {
        if (loggingEnabled || alwaysLog ) {
            Log.d(tagPrefix+tag, string);
            if( remoteLoggingEnabled ){
                RemoteLogger.d(tagPrefix+tag, string);
            }
        }
    }
    public static void v(@NonNls String tag, @NonNls String string, boolean alwaysLog) {
        if (loggingEnabled || alwaysLog ) {
            Log.v(tagPrefix+tag, string);
            if( remoteLoggingEnabled ){
                RemoteLogger.v(tagPrefix+tag, string);
            }
        }
    }
    public static void w(@NonNls String tag, @NonNls String string, boolean alwaysLog) {
        if (loggingEnabled || alwaysLog ) {
            Log.w(tagPrefix+tag, string);
            if( remoteLoggingEnabled ){
                RemoteLogger.w(tagPrefix+tag, string);
            }
        }
    }


    public static void i(@NonNls String tag, @NonNls String string) {
        i(tag, string, false);
    }
    public static void e(@NonNls String tag, @NonNls String string) {
        e(tag, string, false);
    }
    public static void d(@NonNls String tag, @NonNls String string) {
        d(tag, string, false);
    }
    public static void v(@NonNls String tag, @NonNls String string) {
        v(tag, string, false);
    }
    public static void w(@NonNls String tag, @NonNls String string) {
        w(tag, string, false);
    }



    public static void printStackTrace(Exception e) {
        if (loggingEnabled){
            e.printStackTrace();
            if( remoteLoggingEnabled ){
                try {
                    Writer writer = new StringWriter();
                    e.printStackTrace(new PrintWriter(writer));
                    RemoteLogger.e(tagPrefix+"DevLogStackTrace", writer.toString());
                }
                catch(Exception ignored){}
            }
        }
    }



    public static synchronized  void storeRemoteLog(Context context){
        if( remoteLoggingEnabled ) {
            RemoteLogger.storeLog(context);
        }
    }
}
