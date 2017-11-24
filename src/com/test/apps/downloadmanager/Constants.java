package com.test.apps.downloadmanager;

public class Constants {
    public static String gTestUrl = "http://download.thinkbroadband.com/5MB.zip";
    public static int gFileLength = 0;

    public static final String TAG = "MultipartDownload";
    public static final String TEST_FILENAME = "test.bin";
    public static final String TEST_FOLDER = "MyTest";

    /** Contants for block and buffer size */
    public static final int BLOCK_SIZE = 32 * 1024;//4096;
    public static final int BUFFER_SIZE = 32 * 1024;//4096;
    //public static final int BLOCK_SIZE = 512;
    //public static final int BUFFER_SIZE = 512;
    public static final int MIN_DOWNLOAD_SIZE = BLOCK_SIZE * 100;

    public static int DEFAULT_NUM_CONN_PER_DOWNLOAD = 3;
}
