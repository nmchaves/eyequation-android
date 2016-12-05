package edu.sfsu.cs.orange.ocr;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;

import com.googlecode.tesseract.android.TessBaseAPI;

import org.opencv.core.Rect;

import java.util.List;

import edu.sfsu.cs.orange.ocr.camera.ImageProccessingService;

/**
 * Created by nicochaves on 12/4/16.
 */

public class FindEqnRectsAsyncTask extends AsyncTask {

    private String TAG = "FindEqnRectsAsyncTask";

    private MainActivity activity;
    private TessBaseAPI baseApi;
    private Bitmap bitmap;
    private int width;
    private int height;
    private OcrResult ocrResult;
    private long timeRequired;

    FindEqnRectsAsyncTask(MainActivity activity, TessBaseAPI baseApi, Bitmap bitmap, int width, int height) {
        this.activity = activity;
        this.baseApi = baseApi;
        this.bitmap = bitmap;
        this.width = width;
        this.height = height;
    }

    @Override
    protected Object doInBackground(Object[] objects) {

        Integer ulx, uly, brx, bry;

        long start = System.currentTimeMillis();
        //Bitmap bitmap = activity.getCameraManager().buildLuminanceSource(data, width, height).renderCroppedGreyscaleBitmap();


        Bitmap gray = ImageProccessingService.getInstance().convertToGrayScle(this.bitmap);
        final List<Rect> rectList = ImageProccessingService.getInstance().detectObjects(gray);

        if (rectList.size() == 0) {
            // TODO: handle this case better by calling some fail function in MainActivity
            Log.d(TAG, "Failed to detect rectangles");
            return false;
        } else {

            drawEquationRectangles(rectList);
            DoOcrOnEquations(rectList);

            // Then process each rect equation by passing off each one to an ocrRecogAsyncTask
            // use rect.hashCode() to identify each rectangle
        }


        return null;
    }

    private void drawEquationRectangles(final List<Rect> rectList) {

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                // update UI with the rectangles
                activity.drawEquationRects(rectList);

            }
        });

    }

    private void DoOcrOnEquations(List<Rect> rectList) {

        for(Rect rect : rectList) {

        }

        Rect rect = rectList.get(0);

        Integer ulx = (int) rect.tl().x;
        Integer uly = (int) rect.tl().y;
        Integer brx = (int) rect.br().x;
        Integer bry = (int) rect.br().y;

        //Core.rectangle(bitmap, rect.br(), rect.tl(), CONTOUR_COLOR);

        // crop by bounding box, but leave some padding space
        Log.d("Cropping at", ulx.toString() + " " +uly.toString());
        Bitmap cropped = Bitmap.createBitmap(bitmap, Math.max(ulx - 10,0), Math.max(uly - 10,0),
                Math.min(brx + 15,bitmap.getWidth())- ulx , Math.min(bry + 15,bitmap.getHeight())- uly );

        new OcrRecognizeAsyncTask(this.activity, this.baseApi, cropped,
                cropped.getWidth(), cropped.getHeight())
              .execute();
    }


}
