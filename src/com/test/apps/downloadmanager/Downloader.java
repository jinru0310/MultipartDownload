/**
Copyright (c) 2011-present - Luu Gia Thuy

Permission is hereby granted, free of charge, to any person
obtaining a copy of this software and associated documentation
files (the "Software"), to deal in the Software without
restriction, including without limitation the rights to use,
copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the
Software is furnished to do so, subject to the following
conditions:

The above copyright notice and this permission notice shall be
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
OTHER DEALINGS IN THE SOFTWARE.
*/

package com.test.apps.downloadmanager;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Observable;

import android.util.Log;
import static com.test.apps.downloadmanager.Constants.TAG;

public abstract class Downloader extends Observable implements Runnable{

    // Member variables
    /** The URL to download the file */
    protected URL mURL;

    /** Output folder for downloaded file */
    protected String mOutputFolder;

    /** Number of connections (threads) to download the file */
    protected int mNumConnections;

    /** The file name, extracted from URL */
    protected String mFileName;

    /** The file full path */
    protected String mFullPath;

    /** Size of the downloaded file (in bytes) */
    protected int mFileSize;

    /** The state of the download */
    protected int mState;

    /** downloaded size of the file (in bytes) */
    protected int mDownloaded;

    /** List of download threads */
    protected ArrayList<DownloadThread> mListDownloadThread;

    // These are the status names.
    public static final String STATUSES[] = {"Downloading",
                    "Paused", "Complete", "Cancelled", "Error"};

    // Contants for download's state
    public static final int DOWNLOADING = 0;
    public static final int PAUSED = 1;
    public static final int COMPLETED = 2;
    public static final int CANCELLED = 3;
    public static final int ERROR = 4;

    /**
     * Constructor
     * @param url
     * @param outputFolder
     * @param numConnections
     */
    protected Downloader(URL url, String outputFolder, int numConnections) {
        mURL = url;
        mOutputFolder = outputFolder;
        mNumConnections = numConnections;

        // Get the file name from url path
        String fileURL = url.getFile();
        mFileName = fileURL.substring(fileURL.lastIndexOf('/') + 1);
        Log.d(TAG, "File name: " + mFileName);
        mFileSize = -1;
        mState = DOWNLOADING;
        mDownloaded = 0;

        mListDownloadThread = new ArrayList<DownloadThread>();
    }

    /**
     * Pause the downloader
     */
    public void pause() {
        setState(PAUSED);
    }

    /**
     * Resume the downloader
     */
    public void resume() {
        setState(DOWNLOADING);
        download();
    }

    /**
     * Cancel the downloader
     */
    public void cancel() {
        setState(CANCELLED);
    }

    /**
     * Get the URL (in String)
     */
    public String getURL() {
        return mURL.toString();
    }

    /**
     * Get the downloaded file's size
     */
    public int getFileSize() {
        return mFileSize;
    }

    /**
     * Get the current progress of the download
     */
    public float getProgress() {
        return ((float)mDownloaded / mFileSize) * 100;
    }

    /**
     * Get current state of the downloader
     */
    public int getState() {
        return mState;
    }

    /**
     * Set the state of the downloader
     */
    synchronized protected void setState(int value) {
        mState = value;
        stateChanged();
    }

    /**
     * Start or resume download
     */
    protected void download() {
        Thread t = new Thread(this);
        t.start();
    }

    /**
     * Increase the downloaded size
     */
    synchronized protected void downloaded(int value) {
        mDownloaded += value;
        stateChanged();
    }

    /**
     * Set the state has changed and notify the observers
     */
    protected void stateChanged() {
        setChanged();
        notifyObservers();
    }
    
    /**
     * Status notify
     */
    protected void onCompleted() {
        // TODO: soft delete
        File file = new File(mFullPath);
        if (file.exists()) {
            Log.w(TAG, "Delete testing file!!");
            file.delete();
        }

    	for(IDownloadStatus callback : MpltipartDownloadManager.getInstance().getCallbackList()) {
    		callback.onCompleted();
    	}
    }

    /**
     * Thread to download part of a file
     */
    protected abstract class DownloadThread implements Runnable {
        protected int mThreadID;
        protected URL mURL;
        protected String mOutputFile;
        protected int mStartByte;
        protected int mEndByte;
        protected boolean mIsFinished;
        protected Thread mThread;

        public DownloadThread(int threadID, URL url, String outputFile, int startByte, int endByte) {
            mThreadID = threadID;
            mURL = url;
            mOutputFile = outputFile;
            mStartByte = startByte;
            mEndByte = endByte;
            mIsFinished = false;

            download();
        }

        /**
             * Get whether the thread is finished download the part of file
             */
        public boolean isFinished() {
            return mIsFinished;
        }

      /**
             * Start or resume the download
             */
        public void download() {
            mThread = new Thread(this);
            mThread.start();
        }
        /**
         * Waiting for the thread to finish
         * @throws InterruptedException
         */
        public void waitFinish() throws InterruptedException {
            mThread.join();
        }
    }
}
