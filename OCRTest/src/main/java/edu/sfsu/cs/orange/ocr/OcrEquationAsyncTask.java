package edu.sfsu.cs.orange.ocr;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;

import com.googlecode.leptonica.android.ReadFile;
import com.googlecode.tesseract.android.ResultIterator;
import com.googlecode.tesseract.android.TessBaseAPI;

import edu.sfsu.cs.orange.ocr.camera.ImageProccessingService;
import edu.sfsu.cs.orange.ocr.math.ExpressionParser;

/**
 * Created by nicochaves on 12/4/16.
 */

public class OcrEquationAsyncTask extends AsyncTask {

    // The activity we need to communicate with
    MainActivity activity;

    // Bitmap of the equation in black and white
    Bitmap bitmapBW;

    // Equation number (used to identify the equation)
    private int equationNumber;

    private int width;
    private int height;

    // Tesseract API for OCR
    private TessBaseAPI baseApi;

    private long timeRequired;

    public OcrEquationAsyncTask(MainActivity activity, TessBaseAPI baseApi,
                                int equationNumber, Bitmap bitmapBW, int width, int height) {
        this.activity = activity;
        this.baseApi = baseApi;
        this.bitmapBW = bitmapBW;
        this.width = width;
        this.height = height;
        this.equationNumber = equationNumber;
    }

    @Override
    protected Object doInBackground(Object[] objects) {

        long start = System.currentTimeMillis();

        //Bitmap gray = ImageProccessingService.getInstance().convertToGrayScle(this.bitmap);

        String ocrText;

        try {
            baseApi.setImage(ReadFile.readBitmap(bitmapBW));
            ocrText = baseApi.getUTF8Text();
            timeRequired = System.currentTimeMillis() - start;

            // Check for failure to recognize text
            if (ocrText == null || ocrText.equals("")) {
                return false;
            }
            Log.d("OcrEquationAsyncTask", "OCR text: " + ocrText);

            Double result = null;
            EquationResult eqnResult;
            try {
                result = ExpressionParser.parse(ocrText);
            } catch (Exception e) {
                eqnResult = new EquationResult(equationNumber, ocrText, null, false, "Unable to parse expression: " + ocrText);
                sendEquationResult(eqnResult);
                return false;
            }

            eqnResult = new EquationResult(equationNumber, ocrText, result, true, null);
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
