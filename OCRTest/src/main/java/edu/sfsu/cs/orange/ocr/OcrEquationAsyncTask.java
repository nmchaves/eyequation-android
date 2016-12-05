package edu.sfsu.cs.orange.ocr;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import com.googlecode.leptonica.android.ReadFile;
import com.googlecode.tesseract.android.ResultIterator;
import com.googlecode.tesseract.android.TessBaseAPI;

import org.opencv.core.Rect;

import java.util.ArrayList;
import java.util.List;

import edu.sfsu.cs.orange.ocr.camera.ImageProccessingService;
import edu.sfsu.cs.orange.ocr.math.ExpressionParser;

/**
 * Created by nicochaves on 12/4/16.
 */

public class OcrEquationAsyncTask extends AsyncTask {

    // The activity we need to communicate with
    MainActivity activity;

    // Bitmap of the equation
    Bitmap bitmap;

    private int equationNumber;

    private int width;
    private int height;

    // Tesseract API for OCR
    private TessBaseAPI baseApi;

    private long timeRequired;
    private OcrResult ocrResult;

    public OcrEquationAsyncTask(MainActivity activity, TessBaseAPI baseApi,
                                int equationNumber, Bitmap bitmap, int width, int height) {
        this.activity = activity;
        this.baseApi = baseApi;
        this.bitmap = bitmap;
        this.width = width;
        this.height = height;
        this.equationNumber = equationNumber;
    }

    @Override
    protected Object doInBackground(Object[] objects) {

        Integer ulx, uly, brx, bry;

        long start = System.currentTimeMillis();

        Bitmap gray = ImageProccessingService.getInstance().convertToGrayScle(this.bitmap);

        String textResult;

        try {
            baseApi.setImage(ReadFile.readBitmap(bitmap));
            textResult = baseApi.getUTF8Text();
            timeRequired = System.currentTimeMillis() - start;

            // Check for failure to recognize text
            if (textResult == null || textResult.equals("")) {
                return false;
            }
            Log.d("OcrEquationAsyncTask", "OCR text: " + textResult);

            Double result = null;
            EquationResult eqnResult;
            try {
                result = ExpressionParser.parse(textResult);
            } catch (Exception e) {
                /*Toast toast = Toast.makeText(activity, "Unable to parse math expression.", Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.TOP, 0, 0);
                toast.show();*/
                eqnResult = new EquationResult(equationNumber, null, false, "Unable to parse expression.");
                sendEquationResult(eqnResult);
                return false;
            }

            eqnResult = new EquationResult(equationNumber, result, true, null);
            //activity.handleEquationResult(eqnResult);
            sendEquationResult(eqnResult);
            return true;

            /*
            ocrResult = new OcrResult();
            ocrResult.setWordConfidences(baseApi.wordConfidences());
            ocrResult.setMeanConfidence( baseApi.meanConfidence());
            ocrResult.setRegionBoundingBoxes(baseApi.getRegions().getBoxRects());
            ocrResult.setTextlineBoundingBoxes(baseApi.getTextlines().getBoxRects());
            ocrResult.setWordBoundingBoxes(baseApi.getWords().getBoxRects());
            ocrResult.setStripBoundingBoxes(baseApi.getStrips().getBoxRects());

            // Iterate through the results.
            final ResultIterator iterator = baseApi.getResultIterator();
            int[] lastBoundingBox;
            ArrayList<android.graphics.Rect> charBoxes = new ArrayList<android.graphics.Rect>();
            iterator.begin();
            do {
                lastBoundingBox = iterator.getBoundingBox(TessBaseAPI.PageIteratorLevel.RIL_SYMBOL);
                android.graphics.Rect lastRectBox = new android.graphics.Rect(lastBoundingBox[0], lastBoundingBox[1],
                        lastBoundingBox[2], lastBoundingBox[3]);
                charBoxes.add(lastRectBox);
            } while (iterator.next(TessBaseAPI.PageIteratorLevel.RIL_SYMBOL));
            iterator.delete();
            ocrResult.setCharacterBoundingBoxes(charBoxes);
            */


        } catch (RuntimeException e) {
            Log.e("OcrRecognizeAsyncTask", "Caught RuntimeException in request to Tesseract. Setting state to CONTINUOUS_STOPPED.");
            e.printStackTrace();
            try {
                baseApi.clear();
            } catch (NullPointerException e1) {
                // Continue
            }
            return false;
        }
        /*timeRequired = System.currentTimeMillis() - start;
        ocrResult.setBitmap(bitmap);
        ocrResult.setText(textResult);
        ocrResult.setRecognitionTimeRequired(timeRequired);
        return true;*/

    }

    private void sendEquationResult(final EquationResult equationResult) {

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                // update UI with the new result
                activity.handleEquationResult(equationResult);

            }
        });

    }
}
