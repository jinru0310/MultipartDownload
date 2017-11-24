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

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MpltipartDownloadManager {
	public List<IDownloadStatus> mCallbackList = new ArrayList<IDownloadStatus>();

    // The unique instance of this class
    private static MpltipartDownloadManager sInstance = null;

    // Constant variables
    public static final String DEFAULT_OUTPUT_FOLDER = "";

    // Member variables
    private int mNumConnPerDownload;
    private ArrayList<Downloader> mDownloadList;

    /** Protected constructor */
    protected MpltipartDownloadManager() {
        mNumConnPerDownload = Constants.DEFAULT_NUM_CONN_PER_DOWNLOAD;
        mDownloadList = new ArrayList<Downloader>();
    }
    
    /** Callback */
    public List<IDownloadStatus> getCallbackList() {
    	return mCallbackList;
    }
    
    public void addDownloadStatusListener(IDownloadStatus listener) {
    	if (mCallbackList != null) {
            mCallbackList.add(listener);
        }
    }
    
    public void removeDownloadStatusListener(IDownloadStatus listener) {
    	if (listener != null) {
            mCallbackList.remove(listener);
        }
    }

    /**
     * Get the max. number of connections per download
     */
    public int getNumConnPerDownload() {
        return mNumConnPerDownload;
    }

    /**
     * Set the max number of connections per download
     */
    public void setNumConnPerDownload(int value) {
        mNumConnPerDownload = value;
    }

    /**
     * Get the downloader object in the list
     * @param index
     * @return
     */
    public Downloader getDownload(int index) {
        return mDownloadList.get(index);
    }

    public void removeDownload(int index) {
        mDownloadList.remove(index);
    }

    /**
     * Get the download list
     * @return
     */
    public ArrayList<Downloader> getDownloadList() {
        return mDownloadList;
    }


    public Downloader createDownload(URL verifiedURL, String outputFolder) {
        HttpDownloader fd = new HttpDownloader(verifiedURL, outputFolder, mNumConnPerDownload);
        mDownloadList.add(fd);

        return fd;
    }

    /**
     * Get the unique instance of this class
     * @return the instance of this class
     */
    public static MpltipartDownloadManager getInstance() {
        if (sInstance == null)
            sInstance = new MpltipartDownloadManager();

        return sInstance;
    }

    /**
     * Verify whether an URL is valid
     * @param fileURL
     * @return the verified URL, null if invalid
     */
    public static URL verifyURL(String fileURL) {
        // Only allow HTTP URLs.
        if (!fileURL.toLowerCase().startsWith("http://"))
            return null;
        
        // Verify format of URL.
        URL verifiedUrl = null;
        try {
            verifiedUrl = new URL(fileURL);
        } catch (Exception e) {
            return null;
        }
        
        // Make sure URL specifies a file.
        if (verifiedUrl.getFile().length() < 2)
            return null;
        
        return verifiedUrl;
    }

}
