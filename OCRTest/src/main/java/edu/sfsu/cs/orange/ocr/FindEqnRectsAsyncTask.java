package edu.sfsu.cs.orange.ocr;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

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
    private Bitmap bitmapBW;
    private int width;
    private int height;
    private OcrResult ocrResult;
    private long timeRequired;

    FindEqnRectsAsyncTask(MainActivity activity, TessBaseAPI baseApi, Bitmap bitmap, Bitmap bitmapBW, int width, int height) {
        this.activity = activity;
        this.baseApi = baseApi;
        this.bitmap = bitmap;
        this.bitmapBW = bitmapBW;
        this.width = width;
        this.height = height;
    }

    @Override
    protected Object doInBackground(Object[] objects) {

        Integer ulx, uly, brx, bry;

        long start = System.currentTimeMillis();

        Bitmap gray = ImageProccessingService.getInstance().convertToGrayScle(this.bitmap);
        final List<Rect> rectList = ImageProccessingService.getInstance().detectObjects(gray);

        if (rectList.size() == 0) {
            displayNoRectMessage();
            Log.d(TAG, "Failed to detect rectangles");
            return false;
        } else {
            drawEquationRectangles(rectList);
            DoOcrOnEquations(rectList);
        }

        return null;
    }

    private void displayNoRectMessage() {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast toast = Toast.makeText(activity, "Could not find any equations", Toast.LENGTH_LONG);
                toast.show();
            }
        });
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

        int equationNumber = 0;
        for(Rect rect : rectList) {
            Integer ulx = (int) rect.tl().x;
            Integer uly = (int) rect.tl().y;
            Integer brx = (int) rect.br().x;
            Integer bry = (int) rect.br().y;

            // crop by bounding box, but leave some padding space
            Log.d("Cropping at", ulx.toString() + " " +uly.toString());

            Bitmap cropped = Bitmap.createBitmap(bitmapBW, Math.max(ulx,0), Math.max(uly,0),
                    Math.min(brx , bitmapBW.getWidth())- ulx , Math.min(bry,bitmapBW.getHeight())- uly );


            //Bitmap cropped = Bitmap.createBitmap(bitmapBW, Math.max(ulx - paddingHoriz,0), Math.max(uly - paddingVert,0),
             //       Math.min(brx + paddingHoriz,bitmapBW.getWidth())- ulx , Math.min(bry + paddingVert,bitmapBW.getHeight())- uly );

            //Bitmap cropped = Bitmap.createBitmap(bitmap, Math.max(ulx - paddingHoriz,0), Math.max(uly - paddingVert,0),
             //       Math.min(brx + paddingHoriz,bitmap.getWidth())- ulx , Math.min(bry + paddingVert,bitmap.getHeight())- uly );

            // Start an async task to recognize this equation
            new OcrEquationAsyncTask(this.activity, this.baseApi, equationNumber, cropped,
                    cropped.getWidth(), cropped.getHeight())
                    .execute();

            equationNumber++;
        }

    }

}
