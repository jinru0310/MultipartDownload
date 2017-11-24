package com.test.apps.downloadmanager;

import android.annotation.TargetApi;
import android.os.Build;
import android.util.Log;

import java.io.File;
import java.io.IOException;

public class Helper {
    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public static String generateSaveFile(String filename, String filePath) {
        File file = new File(filePath + "/" + filename);
        if (file.exists()) {
            Log.w(Constants.TAG, "File alread existed. delete it!!");
            file.delete();
        }
        
        boolean isCreateComplete = false;
        try {
            
            isCreateComplete = file.createNewFile();
            Log.d(Constants.TAG, "isCreateComplete: " + isCreateComplete);
            file.setReadable(true);
            file.setWritable(true);
            isCreateComplete = true;
                        
        } catch (IOException e) {
            Log.e(Constants.TAG, "generateSaveFile fail");
            String errorMsg = e.getMessage();
            Log.w(Constants.TAG, errorMsg, e);
        }

        return file.getPath();
    }

    /*
  * Calculate downloads speed in bits per second.
  */
    public static String calculateBPS(long fileSize, long totalTime) {
        final long bps = (fileSize * 8) / msToSec(totalTime);
        return formatNumber(bps);
    }

    /*
     * Convert millisecond to second.
     */
    public static long msToSec(long millisecond) {
        final long sec = millisecond / 1000L;
        return sec > 1L ? sec : 1L;
    }

    /*
     * Convert millisecond to second in float.
     */
    public static float msToSec2(long millisecond) {
        final float sec = ((float) millisecond) / 1000.f;
        return sec;
    }

    /*
     * Format number from 12345 to 12,345.
     */
    public static String formatNumber(long number) {
        return String.format("%1$,1d", number);
    }

    /*
    * Extract file name from url
     */

    public static String genFileName(String url) {
        return  url.substring(url.lastIndexOf("/") + 1, url.length());
    }
}
