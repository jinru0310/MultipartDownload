package com.test.apps.downloadmanager;

import java.io.File;
import java.net.URL;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.widget.*;
import com.example.multiplepathdownload.R;

import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;

import static com.test.apps.downloadmanager.Constants.TAG;
import static com.test.apps.downloadmanager.Constants.gTestUrl;

public class MainActivity extends Activity {

    private Button mButtonClean;
    private EditText mtvLink;
    private RadioGroup mRadioGroup;
            
    /** Multiple path */
    private long mStartTime1;
    private long mFinishTime1;
    
    private Button mButtonMpDm;
    private TextView mTextMpDm;
    private MpltipartDownloadManager mMultipartDm;
    private MyDownloadStatusListener mCallback = new MyDownloadStatusListener();
    
    /** Android DownloadManager */
    private Button mButtonDm;
    private TextView mTextDm;
    
    private long mStartTime2;
    private long mFinishTime2;
    
    private DownloadManager mDownloadManager;
    private BroadcastReceiver mDownloadManagerReceiver;
    private long enqueue;

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mRadioGroup = (RadioGroup) findViewById(R.id.radioGroup);
        //Log.d(TAG, "Constants.DEFAULT_NUM_CONN_PER_DOWNLOAD: " + Constants.DEFAULT_NUM_CONN_PER_DOWNLOAD);

        mtvLink = (EditText) findViewById(R.id.tvLink);
        Constants.gTestUrl = mtvLink.getText().toString();

        mButtonClean = (Button) findViewById(R.id.btClean);
        mButtonClean.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mtvLink.setText("http://");
            }
        });

        /** Multiple path */
        mTextMpDm = (TextView) findViewById(R.id.tvMpDm);
        mButtonMpDm = (Button) findViewById(R.id.btnMpDm);
        mButtonMpDm.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (doDownload()) {
                    Toast.makeText(v.getContext(), "Multiple path download triggered", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(v.getContext(), "Multiple path download start fail", Toast.LENGTH_SHORT).show();
                }
            }
            
        });

        // Android DownloadManager
        mTextDm = (TextView) findViewById(R.id.tvDm);
        mButtonDm = (Button) findViewById(R.id.btnDm);
        mButtonDm.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
            	mStartTime2 = System.currentTimeMillis();
                mDownloadManager = (android.app.DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(gTestUrl));
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, Constants.TEST_FILENAME);
                
                enqueue = mDownloadManager.enqueue(request);
                Toast.makeText(v.getContext(), "DownloadManager download triggered", Toast.LENGTH_SHORT).show();
            }
        });

        mDownloadManagerReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                    long downloadId = intent.getLongExtra(android.app.DownloadManager.EXTRA_DOWNLOAD_ID, 0);
                    DownloadManager.Query query = new DownloadManager.Query();
                    query.setFilterById(enqueue);
                    Cursor c = mDownloadManager.query(query);
                    if (c.moveToFirst()) {
                        int sizeBytes = c.getInt(c.getColumnIndexOrThrow(android.app.DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                        int columnIndex = c.getColumnIndex(android.app.DownloadManager.COLUMN_STATUS);
                        if (android.app.DownloadManager.STATUS_SUCCESSFUL == c.getInt(columnIndex)) {
                            long downId = intent.getLongExtra(android.app.DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                            mFinishTime2 = System.currentTimeMillis();
                            Toast.makeText(context, intent.getAction() + " id: "+downId, Toast.LENGTH_SHORT).show();
                            long spendTime = mFinishTime2 - mStartTime2;
                            mTextDm.setText(Helper.msToSec2(spendTime) +
                                    " secs (" + Helper.calculateBPS(sizeBytes, spendTime) + " bits/sec)");
                            Log.i(TAG, "Android DM test. Time to spend: " + spendTime + " ms");
                        }
                    }
                }
            }
            
        };
        //  register listener of Android DownloadManager
        registerReceiver(mDownloadManagerReceiver, new IntentFilter(android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    protected void onStop() {
        mMultipartDm.removeDownloadStatusListener(mCallback);
        unregisterReceiver(mDownloadManagerReceiver);
    }
    
    private boolean doDownload() {
        mMultipartDm = MpltipartDownloadManager.getInstance();
        mMultipartDm.setNumConnPerDownload(Integer.valueOf(getThreadCount()));
        mMultipartDm.addDownloadStatusListener(mCallback);
        
        File outputFile = Environment.getExternalStorageDirectory();
        File base = new File(outputFile.getPath() + "/" + Constants.TEST_FOLDER);
        if (!base.isDirectory() && !base.mkdirs()) {
            // for create folder
        }
        
        URL verifiedURL = mMultipartDm.verifyURL(gTestUrl);
        if (verifiedURL != null) {
        	mStartTime1 = System.currentTimeMillis();
            mMultipartDm.createDownload(verifiedURL, base.getPath());
            
            // Log saving path
            mTextMpDm.setText(base.getPath());
            return true;
        }
        return false;
    }
    
    private class MyDownloadStatusListener implements IDownloadStatus {

        @Override
        public void onCompleted() {
            mFinishTime1 = System.currentTimeMillis();
            final long spendTime = mFinishTime1 - mStartTime1;
            Log.i(TAG, "Multiple path test. Time to spend: " + spendTime + " ms");
            
            MainActivity.this.runOnUiThread(new Runnable(){
                public void run(){
                    //mTextView1.setText(mTextView1.getText() + "  " + "Spend: " + spendTime + " ms");
                    Log.d(TAG, "Download content length: " + Constants.gFileLength);
                    mTextMpDm.setText(Helper.msToSec2(spendTime) +
                            " secs (" + Helper.calculateBPS(Constants.gFileLength, spendTime) + " bits/sec), Thread: " +
                            getThreadCount());
                    Toast.makeText(MainActivity.this, "download completed", Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private String getThreadCount() {
        for (int i = 0; i < mRadioGroup.getChildCount(); i++) {
            RadioButton rd = (RadioButton) mRadioGroup.getChildAt(i);
            if (rd.isChecked()) {
                return rd.getText().toString();
            }
        }
        return "1";
    }

}
