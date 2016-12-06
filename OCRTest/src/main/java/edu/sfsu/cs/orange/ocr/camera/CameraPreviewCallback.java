package edu.sfsu.cs.orange.ocr.camera;

import android.hardware.Camera;
import android.util.Log;

import edu.sfsu.cs.orange.ocr.MainActivity;

/**
 * Created by nicochaves on 12/6/16.
 */

public class CameraPreviewCallback implements Camera.PreviewCallback {

    MainActivity activity;

    public CameraPreviewCallback(MainActivity activity) {
        this.activity = activity;
    }

    @Override
    public void onPreviewFrame(byte[] bytes, Camera camera) {
        Log.d("onPreviewFrame", String.valueOf(bytes.length));
        activity.handlePreviewFrame(bytes, camera);
    }
}
