package edu.sfsu.cs.orange.ocr.camera;

import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.KeyPoint;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.opencv.imgproc.Imgproc.boundingRect;

public class ImageProccessingService {

    private final String TAG = "ImageProccessingService";

    // How much to pad an equation rectangle (as a percentage of the rectangle size)
    double paddingHorizPct = 0.05;
    double paddingVertPct = 0.2;

    private static ImageProccessingService instance;

    public static ImageProccessingService getInstance() {
        if(instance == null) {
            instance = new ImageProccessingService();
        }
        return instance;
    }

    private ImageProccessingService() {

    }

    public Bitmap convertToGrayScle(Bitmap bitmap) {

        Mat mat = Mat.zeros(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC1);
        Utils.bitmapToMat(bitmap, mat);
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGB2GRAY);
        Utils.matToBitmap(mat, bitmap);
        return bitmap;
    }

    public List<Rect> detectObjects(Bitmap bitmap) {
        Mat img = Mat.zeros(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC1);
        Mat mask = Mat.zeros(img.size(), CvType.CV_8UC1);

        Scalar CONTOUR_COLOR = new Scalar(255); //new Scalar(255, 0, 0);
        Utils.bitmapToMat(bitmap, img);

        // Needed to prevent img from being treated as CV_8UC4, which MSER feature detector won't accept
        //Imgproc.cvtColor(img, img, Imgproc.COLOR_BGR5652GRAY);
        Imgproc.cvtColor(img, img, Imgproc.COLOR_RGB2GRAY);

        MatOfKeyPoint mokp = new MatOfKeyPoint();
        FeatureDetector fd = FeatureDetector.create(FeatureDetector.MSER);
        fd.detect(img, mokp);

        Log.i(TAG, "Mat of key points = " + mokp.rows() + "x" + mokp.cols());

        List<KeyPoint> keypointsList = mokp.toList();

        for(KeyPoint keyPoint : keypointsList) {
            int rectanx1 = (int) (keyPoint.pt.x - 0.5 * keyPoint.size);
            int rectany1 = (int) (keyPoint.pt.y - 0.5 * keyPoint.size);
            // if keypoint at negative value (is that even possible?)
            if(rectanx1 <= 0)
                rectanx1 = 1;
            if(rectany1 <= 0)
                rectany1 = 1;
            int rectanw = (int) keyPoint.size;
            int rectanh = (int) keyPoint.size;
            // if keypoint stretches off the image (is that even possible?)
            if ((rectanx1 + rectanw) > img.width())
                rectanw = img.width() - rectanx1;
            if ((rectany1 + rectanh) > img.height())
                rectanh = img.height() - rectany1;

            Rect rectant = new Rect(rectanx1, rectany1, rectanw, rectanh);
            Mat roi = new Mat(mask, rectant);
            roi.setTo(CONTOUR_COLOR);
            Log.i(TAG, "Keypoint at x = " + rectanx1 + ", width = " + rectanw + ", y = " +
                    rectany1 + ", height = " + rectanh);
        }

        Scalar zeos = new Scalar(0, 0, 0);
        List<MatOfPoint> contour2 = new ArrayList<MatOfPoint>();
        //List<List<Point>> rectList = new ArrayList<List<Point>>();
        List<Rect> rectList = new ArrayList<Rect>();

        Mat morbyte = new Mat();
        Mat hierarchy = new Mat();

        Rect rectan2 = new Rect();
        int imgsize = img.height() * img.width();

        // Perform dilation
        Mat se = new Mat(1, 50, CvType.CV_8UC1, Scalar.all(255));
        Imgproc.morphologyEx(mask, morbyte, Imgproc.MORPH_DILATE, se);

        // Find contours
        Imgproc.findContours(morbyte, contour2, hierarchy,
                Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE);

        for (MatOfPoint matOfPoint : contour2) {
            rectList.add(padRectangle(Imgproc.boundingRect(matOfPoint)));
        }

        return rectList;
    }

    private Rect padRectangle(Rect rect) {

        int ulx = (int) rect.tl().x;
        int uly = (int) rect.tl().y;
        int brx = (int) rect.br().x;
        int bry = (int) rect.br().y;

        Integer paddingHoriz = (int) ((brx - ulx) * paddingHorizPct);
        Integer paddingVert = (int) ((bry - uly) * paddingVertPct);

        return new Rect(Math.max(0, ulx - paddingHoriz), Math.max(0, uly - paddingVert),
                2 * paddingHoriz + (brx - ulx), 2 * paddingVert + (bry - uly));

    }
}
