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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import android.util.Log;

import static com.test.apps.downloadmanager.Constants.TAG;

public class HttpDownloader extends Downloader {
    private static final int TIMEOUT = 30000;

    public HttpDownloader(URL url, String outputFolder, int numConnections) {
        super(url, outputFolder, numConnections);
        download();
    }

    @Override
    public void run() {
        Log.d(TAG, "run");
        HttpURLConnection conn = null;
        try {
            // Open connection to URL
            conn = (HttpURLConnection)mURL.openConnection();
            conn.setConnectTimeout(TIMEOUT);
            conn.setReadTimeout(TIMEOUT);

            // Connect
            conn.connect();
            Log.d(TAG, "connect");

            //  Response code should be in the 200 range.
            if (conn.getResponseCode() != 200) {
                setState(ERROR);
                Log.e(TAG, "response code error");
            }
            
            // Check for valid content length.
            int contentLength = conn.getContentLength();
            if (contentLength < 1) {
                Log.e(TAG, "content length error");
                setState(ERROR);
            }

            if (mFileSize == -1) {
                mFileSize = contentLength;
                Constants.gFileLength = mFileSize;
                stateChanged();
                Log.d(TAG, "File size: " + mFileSize);
            }
            
            final int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                processResponseHeaders(conn);
            }
               
            // if the state is DOWNLOADING (no error) -> start downloading
            if (mState == DOWNLOADING) {
                // check whether we have list of download threads or not, if not -> init download
                if (mListDownloadThread.size() == 0) {
                    if (mFileSize > Constants.MIN_DOWNLOAD_SIZE) {
                        // downloading size for each thread
                        int partSize = Math.round(((float)mFileSize / mNumConnections) / Constants.BLOCK_SIZE) * Constants.BLOCK_SIZE;
                        Log.d(TAG, "Part size: " + partSize);

                        // start/end Byte for each thread
                        int startByte = 0;
                        int endByte = partSize;
                        HttpDownloadThread aThread = new HttpDownloadThread(1, mURL, mFullPath, startByte, endByte);
                        mListDownloadThread.add(aThread);
                        int index = Constants.DEFAULT_NUM_CONN_PER_DOWNLOAD;
                        while (endByte < mFileSize) {
                            startByte = endByte + 1;
                            int tmp = endByte + partSize + Constants.BLOCK_SIZE + 1;
                            if (tmp > mFileSize) {
                                endByte = mFileSize;
                            } else {
                                endByte = endByte + partSize + 1;
                            }
                            aThread = new HttpDownloadThread(index, mURL, mFullPath, startByte, endByte);
                            mListDownloadThread.add(aThread);
                            ++index;
                        }
                    } else {
                        HttpDownloadThread aThread = new HttpDownloadThread(1, mURL, mFullPath, 0, mFileSize);
                        mListDownloadThread.add(aThread);
                    }
                } else { // resume all downloading threads
                    for (int i=0; i<mListDownloadThread.size(); ++i) {
                        if (!mListDownloadThread.get(i).isFinished())
                            mListDownloadThread.get(i).download();
                    }
                }

                // waiting for all threads to complete
                for (int i=0; i<mListDownloadThread.size(); ++i) {
                    mListDownloadThread.get(i).waitFinish();
                }

                // check the current state again
                if (mState == DOWNLOADING) {
                	Log.i(TAG, "DownloadComplete!");
                    setState(COMPLETED);
                    onCompleted();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Download thread exception");
            String errorMsg = e.getMessage();
            Log.w(TAG, errorMsg, e);
            setState(ERROR);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
    
    private void processResponseHeaders(HttpURLConnection conn) {
        mFullPath = Helper.generateSaveFile(mFileName, mOutputFolder);
    }

    /**
     * Thread using Http protocol to download a part of file
     */
    private class HttpDownloadThread extends DownloadThread {

        public HttpDownloadThread(int threadID, URL url, String outputFile, int startByte, int endByte) {
            super(threadID, url, outputFile, startByte, endByte);
        }

        @Override
        public void run() {
            BufferedInputStream in = null;
            RandomAccessFile raf = null;
            HttpURLConnection conn = null;

            try {
                // open Http connection to URL
                conn = (HttpURLConnection)mURL.openConnection();

                // set the range of byte to download
                String byteRange = mStartByte + "-" + mEndByte;
                conn.setRequestProperty("Range", "bytes=" + byteRange);
                Log.d(TAG, "bytes=" + byteRange);

                // Connect to server
                conn.connect();

                //  Response code should be in the 200 range.
                if (conn.getResponseCode() / 100 != 2) {
                    Log.e(TAG, "response code is not in the 200 range : " + conn.getResponseCode());
                    setState(ERROR);
                }

                // get the input stream
                in = new BufferedInputStream(conn.getInputStream());

                // open the output file and seek to the start location
                raf = new RandomAccessFile(new File(mOutputFile), "rw");
                raf.seek(mStartByte);

                byte data[] = new byte[Constants.BUFFER_SIZE];
                int numRead;
                while((mState == DOWNLOADING) && ((numRead = in.read(data, 0, Constants.BUFFER_SIZE)) != -1)) { 
                    // write to buffer
                    raf.write(data, 0, numRead);
                    
                    //Log.d(TAG, "( " + mThreadID +" )writing... mStartByte:" + mStartByte + "  numRead:" + numRead);
                    // increase the startByte for resume later
                    mStartByte += numRead;
                    // increase the downloaded size
                    downloaded(numRead);
                }

                if (mState == DOWNLOADING) {
                    mIsFinished = true;
                }
            } catch (IOException e) {
                Log.e(TAG, "HttpURLConnection error");
                setState(ERROR);
                String errorMsg = e.getMessage();
                Log.w(TAG, errorMsg, e);
            } finally {
                if (raf != null) {
                    try {
                        raf.close();
                    } catch (IOException e) {}
                }
                
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {}
                }

                if (conn != null) {
                    conn.disconnect();
                }
            }

            Log.d(TAG, "End thread " + mThreadID);
        }
    }

    /* Testing method */
    private static void writeData(final File file) {
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(file, "rw");
        } catch (FileNotFoundException e) {
            String errorMsg = e.getMessage();
            Log.w("WriteData", errorMsg, e);
        }

        try {
            Thread thread2 = new Thread(new Runnable() {
                public void run() {
                    try {
                        RandomAccessFile raf2 = new RandomAccessFile(file, "rw");
                        try {
                            raf2.seek(5);
                            raf2.writeBytes("This is writing data testing function!");
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    } catch (FileNotFoundException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            });
            thread2.start();
        } finally {
            if (raf != null) {
                try {
                    raf.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

    }
}