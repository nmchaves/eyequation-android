package edu.sfsu.cs.orange.ocr.camera;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.widget.ImageView;
import android.widget.Toast;

import edu.sfsu.cs.orange.ocr.PlanarYUVLuminanceSource;

public class CameraUtils {

    private static CameraUtils instance = null;

    public static CameraUtils getInstance() {
        if(instance == null) {
            instance = new CameraUtils();
        }
        return instance;
    }

    private CameraUtils() {

    }

    public boolean isCameraSupported(Context context) {
        PackageManager packageManager = context.getPackageManager();
        if(packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA) == false){
            Toast.makeText(context, "This device does not have a camera.", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }


    public void convertToGrayScale(Bitmap bitmap, ImageView viewImage) {
        bitmap = ImageProccessingService.getInstance().convertToGrayScle(bitmap);
        viewImage.setImageBitmap(bitmap);
    }

    /**
     * A factory method to build the appropriate LuminanceSource object based on the format
     * of the preview buffers, as described by Camera.Parameters.
     *
     * @param data A preview frame.
     * @param width The width of the image.
     * @param height The height of the image.
     * @return A PlanarYUVLuminanceSource instance.
     */
    public static PlanarYUVLuminanceSource buildLuminanceSource(byte[] data, int width, int height) {

        boolean reverseImage = true;

        // Go ahead and assume it's YUV rather than die.
        return new PlanarYUVLuminanceSource(data, width, height, 0, 0,
                width, height, reverseImage);
    }
}
