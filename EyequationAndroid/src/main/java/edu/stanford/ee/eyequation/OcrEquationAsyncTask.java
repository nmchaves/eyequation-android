package edu.stanford.ee.eyequation;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;

import com.googlecode.leptonica.android.ReadFile;
import com.googlecode.tesseract.android.TessBaseAPI;

import edu.stanford.ee.eyequation.math.ExpressionParser;


/**
 * An async task for performing OCR on a mathematical equation.
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
