package com.daddysoffice.sample.broadcasttest;

import android.app.Activity;
import android.os.Bundle;

public class MainActivity extends Activity {

    private CameraPreview mPreview;

    private MJpegServer mServer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mServer = new MJpegServer();

        mPreview = new CameraPreview(this);
        mPreview.setCaptureEventListener(new CameraPreview.CaptureEventListener() {
            @Override
            public void OnFrame(byte[] frame) {
                mServer.setCurrentFrame(frame);
            }
        });

        setContentView(mPreview);
    }

    @Override
    protected void onResume() {
        super.onResume();

        mPreview.openCamera();

        mServer.start(10080);
    }

    @Override
    protected void onPause() {
        super.onPause();

        mServer.stop();

        mPreview.closeCamera();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
