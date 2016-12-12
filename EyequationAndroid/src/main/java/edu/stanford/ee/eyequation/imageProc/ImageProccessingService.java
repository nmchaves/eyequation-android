package edu.stanford.ee.eyequation.imageProc;

import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.KeyPoint;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class ImageProccessingService {

    private final String TAG = "ImageProccessingService";

    // How much to pad an equation rectangle (as a percentage of the rectangle size)
    private double paddingHorizPct = 0.05;
    private double paddingVertPct = 0.2;

    private static ImageProccessingService instance;

    public static ImageProccessingService getInstance() {
        if(instance == null) {
            instance = new ImageProccessingService();
        }
        return instance;
    }

    private ImageProccessingService() {}

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

        Scalar CONTOUR_COLOR = new Scalar(255);
        Utils.bitmapToMat(bitmap, img);

        // Needed to prevent img from being treated as CV_8UC4, which MSER feature detector won't accept
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


        List<MatOfPoint> contour2 = new ArrayList<MatOfPoint>();
        List<Rect> rectList = new ArrayList<Rect>();

        Mat morbyte = new Mat();
        Mat hierarchy = new Mat();

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

    public static Bitmap locallyAdaptiveThreshold(Bitmap gray) {
        Bitmap newBitmap = Bitmap.createBitmap(gray.getWidth(), gray.getHeight(), Bitmap.Config.ARGB_8888);

        Mat mat = new Mat(); //Mat.zeros(imgBitmap.getHeight(), imgBitmap.getWidth(), CvType.CV_8UC4);
        Utils.bitmapToMat(gray, mat);
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY);

        Mat matBW = new Mat(); //Mat.zeros(imgBitmap.getHeight(), imgBitmap.getWidth(), CvType.CV_8UC1);

        // Block size for thresholding. MUST BE AN ODD NUMBER or openCV will throw an exception!
        int blockSize = 55;
        Imgproc.adaptiveThreshold(mat, matBW, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, blockSize, 15);
        Utils.matToBitmap(matBW, newBitmap);

        return newBitmap;

    }

    /**
     * Convert an NV21 formatted array to a grayscale bitmap of the image.
     */
    public static Bitmap NV21BytesToGrayScaleBitmap(byte[] data, int width, int height) {
        PlanarYUVLuminanceSource lum = new PlanarYUVLuminanceSource(data, width, height,
                0, 0, width, height, false);
        return lum.renderCroppedGreyscaleBitmap();
    }

    // Adapted from:
    // http://stackoverflow.com/questions/5272388/extract-black-and-white-image-from-android-cameras-nv21-format/12702836#12702836
    /**
     * Converts YUV420 NV21 to RGB8888
     *
     * @param data byte array on YUV420 NV21 format.
     * @param width pixels width
     * @param height pixels height
     * @return a RGB8888 pixels int array. Where each int is a pixels ARGB.
     */
    /*public static int[] convertYUV420_NV21toRGB8888(byte [] data, int width, int height) {
        int size = width*height;
        int offset = size;
        int[] pixels = new int[size];
        int u, v, y1, y2, y3, y4;

        // i percorre os Y and the final pixels
        // k percorre os pixles U e V
        for(int i=0, k=0; i < size; i+=2, k+=2) {
            y1 = data[i  ]&0xff;
            y2 = data[i+1]&0xff;
            y3 = data[width+i  ]&0xff;
            y4 = data[width+i+1]&0xff;

            u = data[offset+k  ]&0xff;
            v = data[offset+k+1]&0xff;
            u = u-128;
            v = v-128;

            pixels[i  ] = convertYUVtoRGB(y1, u, v);
            pixels[i+1] = convertYUVtoRGB(y2, u, v);
            pixels[width+i  ] = convertYUVtoRGB(y3, u, v);
            pixels[width+i+1] = convertYUVtoRGB(y4, u, v);

            if (i!=0 && (i+2)%width==0)
                i+=width;
        }

        return pixels;
    }

    private static int convertYUVtoRGB(int y, int u, int v) {
        int r,g,b;

        r = y + (int)1.402f*v;
        g = y - (int)(0.344f*u +0.714f*v);
        b = y + (int)1.772f*u;
        r = r>255? 255 : r<0 ? 0 : r;
        g = g>255? 255 : g<0 ? 0 : g;
        b = b>255? 255 : b<0 ? 0 : b;
        return 0xff000000 | (b<<16) | (g<<8) | r;
    }*/
}
