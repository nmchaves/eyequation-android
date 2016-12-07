package edu.sfsu.cs.orange.ocr;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.googlecode.tesseract.android.TessBaseAPI;

import org.opencv.core.Rect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import edu.sfsu.cs.orange.ocr.camera.ImageProccessingService;

import static android.R.attr.max;

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

            //mergeRectangles(rectList);

            drawEquationRectangles(rectList);
            DoOcrOnEquations(rectList);
        }

        return null;
    }

    /*private void mergeRectangles(List<Rect> rectList) {
        // Sort rectangles by their horizontal position

        // Find horizontally connected (or almost connected) rectangles rectangles that need to be merged.
        List<ArrayList<Integer>> connectedComps = new ArrayList<ArrayList<Integer>>();


        {
        List<List<Integer>> connectedComps = new List<List<Integer>>() {
            @Override
            public void add(int i, List<Integer> integers) {

            }

            @Override
            public boolean add(List<Integer> integers) {
                return false;
            }

            @Override
            public boolean addAll(int i, Collection<? extends List<Integer>> collection) {
                return false;
            }

            @Override
            public boolean addAll(Collection<? extends List<Integer>> collection) {
                return false;
            }

            @Override
            public void clear() {

            }

            @Override
            public boolean contains(Object o) {
                return false;
            }

            @Override
            public boolean containsAll(Collection<?> collection) {
                return false;
            }

            @Override
            public List<Integer> get(int i) {
                return null;
            }

            @Override
            public int indexOf(Object o) {
                return 0;
            }

            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public Iterator<List<Integer>> iterator() {
                return null;
            }

            @Override
            public int lastIndexOf(Object o) {
                return 0;
            }

            @Override
            public ListIterator<List<Integer>> listIterator() {
                return null;
            }

            @Override
            public ListIterator<List<Integer>> listIterator(int i) {
                return null;
            }

            @Override
            public List<Integer> remove(int i) {
                return null;
            }

            @Override
            public boolean remove(Object o) {
                return false;
            }

            @Override
            public boolean removeAll(Collection<?> collection) {
                return false;
            }

            @Override
            public boolean retainAll(Collection<?> collection) {
                return false;
            }

            @Override
            public List<Integer> set(int i, List<Integer> integers) {
                return null;
            }

            @Override
            public int size() {
                return 0;
            }

            @Override
            public List<List<Integer>> subList(int i, int i1) {
                return null;
            }

            @Override
            public Object[] toArray() {
                return new Object[0];
            }

            @Override
            public <T> T[] toArray(T[] ts) {
                return null;
            }
        }

        for(Rect rect: rectList) {
            if(tlx < 0) {
                tlx = rect.tl().x;
                tly = rect.tl().y;
                brx = rect.br().x;
                bry = rect.br().y;
            }
            else {

            }

            if(rect.tl().x < tlx) {
                tlx = rect.tl().x;
            }

        }
    }

    private Rect doMerge(List<Rect> rectList) {

        double tlx = -1;
        double tly = -1;
        double brx = -1;
        double bry = -1;

        for(Rect rect: rectList) {
            if(tlx < 0) {
                tlx = rect.tl().x;
                tly = rect.tl().y;
                brx = rect.br().x;
                bry = rect.br().y;
            }
            else {

            }

            if(rect.tl().x < tlx) {
                tlx = rect.tl().x;
            }

        }

    }*/

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
