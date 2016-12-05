/*
 * Copyright 2011 Robert Theis
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.sfsu.cs.orange.ocr;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.googlecode.leptonica.android.ReadFile;
import com.googlecode.tesseract.android.ResultIterator;
import com.googlecode.tesseract.android.TessBaseAPI;
import com.googlecode.tesseract.android.TessBaseAPI.PageIteratorLevel;

import org.opencv.core.Core;
import org.opencv.core.Point;

import edu.sfsu.cs.orange.ocr.camera.ImageProccessingService;

import static android.R.id.message;

/**
 * Class to send OCR requests to the OCR engine in a separate thread, send a success/failure message,
 * and dismiss the indeterminate progress dialog box. Used for non-continuous mode OCR only.
 */
final class OcrRecognizeAsyncTask extends AsyncTask<Void, Void, Boolean> {

  //  private static final boolean PERFORM_FISHER_THRESHOLDING = false; 
  //  private static final boolean PERFORM_OTSU_THRESHOLDING = false; 
  //  private static final boolean PERFORM_SOBEL_THRESHOLDING = false; 

  private MainActivity activity;
  private TessBaseAPI baseApi;
  private Bitmap bitmap;
  private int width;
  private int height;
  private OcrResult ocrResult;
  private long timeRequired;

  OcrRecognizeAsyncTask(MainActivity activity, TessBaseAPI baseApi, Bitmap bitmap, int width, int height) {
    this.activity = activity;
    this.baseApi = baseApi;
    this.bitmap = bitmap;
    this.width = width;
    this.height = height;
  }

  /*OcrRecognizeAsyncTaskOld(CaptureActivity activity, TessBaseAPI baseApi, Bitmap bitmap, int width, int height) {
    this.activity = activity;
    this.baseApi = baseApi;
    this.bitmap = bitmap;
    this.width = width;
    this.height = height;
  }*/

  @Override
  protected Boolean doInBackground(Void... arg0) {
    /*Integer ulx;
    Integer uly;
    Integer brx;
    Integer bry;*/
    Integer ulx, uly, brx, bry;

    long start = System.currentTimeMillis();
    //Bitmap bitmap = activity.getCameraManager().buildLuminanceSource(data, width, height).renderCroppedGreyscaleBitmap();

    // TODO: modify the detectObjects function in ImageProcessingService so that it returns
    // the rectangles around each equation. Then process those rectangles here (e.g. pass them
    // to tesseract, draw them, etc.)
    Log.d("ABCDEFGHIJKLM", "before!");
    Bitmap gray = ImageProccessingService.getInstance().convertToGrayScle(this.bitmap);
    //List<List<Point>> rectList = ImageProccessingService.getInstance().detectObjects(gray);
    List<org.opencv.core.Rect> rectList = ImageProccessingService.getInstance().detectObjects(gray);
    // test out slicing image
    if (rectList.size() == 0) {
      // TODO: send proper error message
      Log.d("abcd", "Failed to detect rectangles");
      return false;
    } else {
      for (org.opencv.core.Rect rect : rectList) {
          ulx = (int) rect.tl().x;
          uly = (int) rect.tl().y;
          brx = (int) rect.br().x;
          bry = (int) rect.br().y;

        //Core.rectangle(bitmap, rect.br(), rect.tl(), CONTOUR_COLOR);

        // crop by bounding box, but leave some padding space
        Log.d("Cropping at", ulx.toString() + " " +uly.toString());
        bitmap = Bitmap.createBitmap(bitmap, Math.max(ulx - 10,0), Math.max(uly - 10,0),
                Math.min(brx + 15,bitmap.getWidth())- ulx , Math.min(bry + 15,bitmap.getHeight())- uly );

          // TODO: process all rectangles instead of breaking here
        break;
      }
    }

    /*if (rectList.size() > 0) {
      ulx = (int) rectList.get(0).get(0).x;
      uly = (int) rectList.get(0).get(0).y;
      brx = (int) rectList.get(0).get(1).x;
      bry = (int) rectList.get(0).get(1).y;
      // crop by bounding box, but leave some padding space
      Log.d("Cropping at", ulx.toString() + " " +uly.toString());
      bitmap = Bitmap.createBitmap(bitmap, Math.max(ulx - 10,0), Math.max(uly - 10,0),
              Math.min(brx + 15,bitmap.getWidth())- ulx , Math.min(bry + 15,bitmap.getHeight())- uly );
    }*/
    Log.d("ABCDEFGHIJKLM", "after!");


    String textResult;

    //      if (PERFORM_FISHER_THRESHOLDING) {
    //        Pix thresholdedImage = Thresholder.fisherAdaptiveThreshold(ReadFile.readBitmap(bitmap), 48, 48, 0.1F, 2.5F);
    //        Log.e("OcrRecognizeAsyncTask", "thresholding completed. converting to bmp. size:" + bitmap.getWidth() + "x" + bitmap.getHeight());
    //        bitmap = WriteFile.writeBitmap(thresholdedImage);
    //      }
    //      if (PERFORM_OTSU_THRESHOLDING) {
    //        Pix thresholdedImage = Binarize.otsuAdaptiveThreshold(ReadFile.readBitmap(bitmap), 48, 48, 9, 9, 0.1F);
    //        Log.e("OcrRecognizeAsyncTask", "thresholding completed. converting to bmp. size:" + bitmap.getWidth() + "x" + bitmap.getHeight());
    //        bitmap = WriteFile.writeBitmap(thresholdedImage);
    //      }
    //      if (PERFORM_SOBEL_THRESHOLDING) {
    //        Pix thresholdedImage = Thresholder.sobelEdgeThreshold(ReadFile.readBitmap(bitmap), 64);
    //        Log.e("OcrRecognizeAsyncTask", "thresholding completed. converting to bmp. size:" + bitmap.getWidth() + "x" + bitmap.getHeight());
    //        bitmap = WriteFile.writeBitmap(thresholdedImage);
    //      }

    try {     
      baseApi.setImage(ReadFile.readBitmap(bitmap));
      textResult = baseApi.getUTF8Text();
      timeRequired = System.currentTimeMillis() - start;

      // Check for failure to recognize text
      if (textResult == null || textResult.equals("")) {
        return false;
      }
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
      ArrayList<Rect> charBoxes = new ArrayList<Rect>();
      iterator.begin();
      do {
          lastBoundingBox = iterator.getBoundingBox(PageIteratorLevel.RIL_SYMBOL);
          Rect lastRectBox = new Rect(lastBoundingBox[0], lastBoundingBox[1],
                  lastBoundingBox[2], lastBoundingBox[3]);
          charBoxes.add(lastRectBox);
      } while (iterator.next(PageIteratorLevel.RIL_SYMBOL));
      iterator.delete();
      ocrResult.setCharacterBoundingBoxes(charBoxes);

    } catch (RuntimeException e) {
      Log.e("OcrRecognizeAsyncTask", "Caught RuntimeException in request to Tesseract. Setting state to CONTINUOUS_STOPPED.");
      e.printStackTrace();
      try {
        baseApi.clear();
        //activity.stopHandler();
      } catch (NullPointerException e1) {
        // Continue
      }
      return false;
    }
    timeRequired = System.currentTimeMillis() - start;
    ocrResult.setBitmap(bitmap);
    ocrResult.setText(textResult);
    ocrResult.setRecognitionTimeRequired(timeRequired);
    return true;
  }

  @Override
  protected void onPostExecute(Boolean result) {
    super.onPostExecute(result);

    activity.handleOcrResult(result, ocrResult);

    /*
    Handler handler = activity.getHandler();
    if (handler != null) {
      // Send results for single-shot mode recognition.
      if (result) {
        Message message = Message.obtain(handler, R.id.ocr_decode_succeeded, ocrResult);
        message.sendToTarget();
      } else {
        Message message = Message.obtain(handler, R.id.ocr_decode_failed, ocrResult);
        message.sendToTarget();
      }
      activity.getProgressDialog().dismiss();
    }
    if (baseApi != null) {
      baseApi.clear();
    }*/
  }
}
