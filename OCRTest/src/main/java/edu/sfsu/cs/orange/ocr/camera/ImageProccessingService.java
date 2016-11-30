package edu.sfsu.cs.orange.ocr.camera;

import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.KeyPoint;
import org.opencv.imgproc.Imgproc;

import java.util.List;

/**
 * Created by yaron on 31/12/15.
 */
public class ImageProccessingService {

    private final String TAG = "ImageProccessingService";

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

    public void detectObjects(Bitmap bitmap) {
        Mat img = Mat.zeros(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC1);
        Utils.bitmapToMat(bitmap, img);

        // Needed to prevent img from being treated as CV_8UC4, which MSER feature detector won't accept
        Imgproc.cvtColor(img, img, Imgproc.COLOR_RGB2GRAY);

        MatOfKeyPoint mokp = new MatOfKeyPoint();
        FeatureDetector fd = FeatureDetector.create(FeatureDetector.MSER);
        fd.detect(img, mokp);

        Log.i(TAG, "Mat of key points = " + mokp.rows() + "x" + mokp.cols());

        // TODO: test out converting they keypoints to rectangles
        // See http://yaronvazana.com/2016/02/02/android-text-detection-using-opencv/
        // In particular, look at the onCameraFrame() function in the "Text Detection Implementation"
        List<KeyPoint> keypointsList = mokp.toList();

        for(KeyPoint keyPoint : keypointsList) {
            // TODO: process keypoint (convert it into a rectangle)
            int rectanx1 = (int) (keyPoint.pt.x - 0.5 * keyPoint.size);
            int rectany1 = (int) (keyPoint.pt.y - 0.5 * keyPoint.size);
        }




    }
}
