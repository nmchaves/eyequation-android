package edu.sfsu.cs.orange.ocr;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.googlecode.tesseract.android.TessBaseAPI;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Rect;

import java.io.File;
import java.util.List;

import edu.sfsu.cs.orange.ocr.camera.CameraConfigurationManager;
import edu.sfsu.cs.orange.ocr.camera.CameraManager;
import edu.sfsu.cs.orange.ocr.camera.CameraPreviewCallback;
import edu.sfsu.cs.orange.ocr.math.ExpressionParser;

public class MainActivity extends Activity {

    private String TAG = "MainActivity";

    // Whether or not the camera is previewing. If camera is not previewing,
    // then we must be viewing the frame of the equation(s)
    boolean isCameraPreviewing = true;

    // Callback for receiving a preview frame from the camera
    private CameraPreviewCallback cameraPreviewCB;

    public class CameraPreviewCallback implements Camera.PreviewCallback {

        public CameraPreviewCallback() {}

        @Override
        public void onPreviewFrame(byte[] bytes, Camera camera) {
            handlePreviewFrame(bytes, camera);
        }
    }

    private static Camera mCamera;
    private CameraConfigurationManager configManager;
    private FrameLayout previewFrame;
    private CameraPreview mPreview;
    private Button captureButton;
    private ImageView pictureView;

    private Bitmap currentFrame;
    private Bitmap currentFrameRaw;

    private List<Rect> equationRectangles;

    private TessBaseAPI baseApi;

    private ProgressDialog dialog; // for initOcr - language download & unzip
    private ProgressDialog indeterminateDialog; // also for initOcr - init OCR engine
    private boolean isEngineReady;

    private int pageSegmentationMode = TessBaseAPI.PageSegMode.PSM_SINGLE_LINE; //TessBaseAPI.PageSegMode.PSM_AUTO_OSD;
    private int ocrEngineMode = TessBaseAPI.OEM_TESSERACT_ONLY;
    private String characterBlacklist;
    private String characterWhitelist;

    // TODO: clean this up
    private String sourceLanguageCodeOcr = "eng"; // ISO 639-3 language code
    private String sourceLanguageReadable = "English"; // Language name, for example, "English"

    private CameraManager cameraManager;
    CameraManager getCameraManager() {
        return cameraManager;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // Keep the app awake. Helpful for demo purposes
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Attempt to load OpenCV
        if (!OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_11, this, mOvenCVLoaderCallback)) {
            Toast toast = Toast.makeText(getApplicationContext(), "Failed to load OpenCV! Check that you have OpenCV Manager.", Toast.LENGTH_LONG);
            toast.show();
        }

        //sourceLanguageReadable = LanguageCodeHelper.getOcrLanguageName(this, sourceLanguageCodeOcr);
        cameraManager = new CameraManager(getApplication());

        isEngineReady = false;

        // Initialize the OCR engine
        File storageDirectory = getStorageDirectory();
        if (storageDirectory != null) {
            initOcrEngine(storageDirectory, sourceLanguageCodeOcr, sourceLanguageReadable);
        }

        setupCameraPreview();

    }


    /** Finds the proper location on the SD card where we can save files. */
    private File getStorageDirectory() {

        String state = null;
        try {
            state = Environment.getExternalStorageState();
        } catch (RuntimeException e) {
            Log.e(TAG, "Is the SD card visible?", e);
            showErrorMessage("Error", "Required external storage (such as an SD card) is unavailable.");
        }

        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {

            // We can read and write the media
            //    	if (Integer.valueOf(android.os.Build.VERSION.SDK_INT) > 7) {
            // For Android 2.2 and above

            try {
                return getExternalFilesDir(Environment.MEDIA_MOUNTED);
            } catch (NullPointerException e) {
                // We get an error here if the SD card is visible, but full
                Log.e(TAG, "External storage is unavailable");
                showErrorMessage("Error", "Required external storage (such as an SD card) is full or unavailable.");
            }

        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            // We can only read the media
            Log.e(TAG, "External storage is read-only");
            showErrorMessage("Error", "Required external storage (such as an SD card) is unavailable for data storage.");
        } else {
            // Something else is wrong. It may be one of many other states, but all we need
            // to know is we can neither read nor write
            Log.e(TAG, "External storage is unavailable");
            showErrorMessage("Error", "Required external storage (such as an SD card) is unavailable or corrupted.");
        }
        return null;
    }

    /**
     * Displays an error message dialog box to the user on the UI thread.
     *
     * @param title The title for the dialog box
     * @param message The error message to be displayed
     */
    void showErrorMessage(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setOnCancelListener(new FinishListener(this))
                .setPositiveButton( "Done", new FinishListener(this))
                .show();
    }


    // Callback for loading OpenCV using OpenCV Manager
    private BaseLoaderCallback mOvenCVLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    Log.i(TAG, "OpenCV loaded successfully");
                    break;
                default:
                    // Handle error after callback
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance

            // Set camera parameters
            Camera.Parameters params = c.getParameters();

            // Continously try to focus the camera
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);

            // Use NV21 picture format to conform with expected input from PlanarYUVLuminanceSource
            params.setPreviewFormat(ImageFormat.NV21);
            //params.setPictureFormat(ImageFormat.NV21);

            c.setParameters(params);
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    private void setupCameraPreview() {

        // Create an instance of Camera
        mCamera = getCameraInstance();
        cameraPreviewCB = new CameraPreviewCallback();

        // Camera configuration
        configManager = new CameraConfigurationManager(this);
        configManager.initFromCameraParameters(mCamera);
        configManager.setDesiredCameraParameters(mCamera);

        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera);
        previewFrame = (FrameLayout) findViewById(R.id.camera_preview);
        previewFrame.addView(mPreview);

        pictureView = (ImageView) findViewById(R.id.picture_view);

        // Add a listener to the Capture button
        captureButton = (Button) findViewById(R.id.button_capture);

        captureButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // get an image from the camera
                        //mCamera.takePicture(null, null, mPictureCB);

                        // Request a frame from the camera
                        mCamera.setOneShotPreviewCallback(cameraPreviewCB);

                    }
                }
        );
    }

    // Perform all processing steps starting from a raw byte array representing
    // the image in NV21 format to the final solved equation(s)
    public void handlePreviewFrame(byte[] data, Camera camera) {

        // Hide the camera preview so we can overlay the single frame
        hideCameraPreview();

        Point cameraResolution = configManager.getCameraResolution();
        int width = cameraResolution.x;
        int height = cameraResolution.y;

        currentFrame = NV21BytesToGrayScaleBitmap(data, width, height);

        pictureView.setImageBitmap(currentFrame);
        pictureView.setVisibility(View.VISIBLE);

        // Perform the rest of the pipeline
        processImage(currentFrame, width, height);
    }

    /*private Camera.PictureCallback mPictureCB = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            int width = camera.getParameters().getPreviewSize().width;
            int height = camera.getParameters().getPreviewSize().height;

            currentFrameRaw = BitmapFactory.decodeByteArray(data, 0, data.length);
            currentFrame = currentFrameRaw;
            //PlanarYUVLuminanceSource lum = new PlanarYUVLuminanceSource(data, imgBitmap.getWidth(), imgBitmap.getHeight(),
             //       0, 0,
              //      imgBitmap.getWidth(), imgBitmap.getHeight(), false);


            pictureView.setImageBitmap(currentFrame);
            pictureView.setVisibility(View.VISIBLE);

            hideCameraPreview();

            Point cameraResolution = configManager.getCameraResolution();
            width = cameraResolution.x;
            height = cameraResolution.y;

            // TODO: just try converting NV21 to jpg and back

            // Start processing the image
            //processImage(data, currentFrame);
            currentFrame = NV21BytesToBitmap(data, width, height);
            processImage(currentFrame, width, height);

        }
    };*/

    ProgressDialog getProgressDialog() {
        return indeterminateDialog;
    }

    private CaptureActivityHandler handler;

    Handler getHandler() {
        return handler;
    }

    /**
     * Convert an NV21 formatted array to a grayscale bitmap of the image.
     */
    private Bitmap NV21BytesToGrayScaleBitmap(byte[] data, int width, int height) {
        PlanarYUVLuminanceSource lum = new PlanarYUVLuminanceSource(data, width, height,
                0, 0, width, height, false);
        return lum.renderCroppedGreyscaleBitmap();
    }

    private void processImage(Bitmap bitmap, int width, int height) {
        new FindEqnRectsAsyncTask(this, baseApi, bitmap, width, height)
                .execute();
    }

    public void handleEquationResult(EquationResult equationResult) {

        // Redraw this equation's rectangle with a color that indicates whether
        // a solution was found (green) or not (red)
        int rectColor;
        boolean success = equationResult.isSuccess();
        if(success) {
            rectColor = Color.GREEN;
        }
        else {
            rectColor = Color.RED;
        }
        redrawEquationRect(null, equationResult.getEquationNumber(), rectColor);

        if(success) {
            // Toast the solution and draw it into the image
            double solution = equationResult.getSolution();
            Toast toast = Toast.makeText(this, "Result: " + String.valueOf(solution), Toast.LENGTH_LONG);
            toast.show();

            drawEquationResult(solution, equationResult.getEquationNumber());
        }
        else {
            String errorMessage = equationResult.getErrorMessage();
            Toast toast = Toast.makeText(this, errorMessage, Toast.LENGTH_LONG);
            toast.show();
        }
    }

    private void hideCameraPreview() {
        previewFrame.setVisibility(View.GONE);
        captureButton.setVisibility(View.GONE);
        isCameraPreviewing = false;
    }

    private void showCameraPreview() {
        previewFrame.setVisibility(View.VISIBLE);
        captureButton.setVisibility(View.VISIBLE);
        isCameraPreviewing = true;
    }

    @Override
    public void onBackPressed() {
        if( isCameraPreviewing ) {
            super.onBackPressed();
        }
        else {
            // Return to the camera preview
            pictureView.setVisibility(View.GONE);
            mCamera.startPreview();
            showCameraPreview();
        }

    }

    /**
     * Requests initialization of the OCR engine with the given parameters.
     *
     * @param storageRoot Path to location of the tessdata directory to use
     * @param languageCode Three-letter ISO 639-3 language code for OCR
     * @param languageName Name of the language for OCR, for example, "English"
     */
    private void initOcrEngine(File storageRoot, String languageCode, String languageName) {
        isEngineReady = false;

        // Set up the dialog box for the thermometer-style download progress indicator
        if (dialog != null) {
            dialog.dismiss();
        }
        dialog = new ProgressDialog(this);


        // Display the name of the OCR engine we're initializing in the indeterminate progress dialog box
        indeterminateDialog = new ProgressDialog(this);
        indeterminateDialog.setTitle("Please wait");
        String ocrEngineModeName = "Tesseract";
        indeterminateDialog.setMessage("Initializing " + ocrEngineModeName + " OCR engine for " + languageName + "...");
        indeterminateDialog.setCancelable(false);
        indeterminateDialog.show();

        // Start AsyncTask to install language data and init OCR
        baseApi = new TessBaseAPI();
        new OcrInitAsyncTask(this, baseApi, dialog, indeterminateDialog, languageCode, languageName, ocrEngineMode)
                .execute(storageRoot.toString());
    }

    public void handleOcrResult(boolean success, OcrResult result) {

        // Test whether the result is null
        if (!success || result.getText() == null || result.getText().equals("")) {
            Toast toast = Toast.makeText(this, "OCR failed. Please try again.", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.TOP, 0, 0);
            toast.show();
            return;
        }

        // Try to evaluate the mathematical expression
        String ocrText = result.getText();
        Log.d(TAG, "OCR Text: " + ocrText);
        Double solution = null;
        try {
            solution = ExpressionParser.parse(ocrText);
        } catch (Exception e) {
            Toast toast = Toast.makeText(this, "Unable to parse expression.", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.TOP, 0, 0);
            toast.show();
            return;
        }

        Toast toast = Toast.makeText(this, "Result: " + String.valueOf(solution), Toast.LENGTH_LONG);
        toast.show();

    }

    public void drawEquationResult(Double result, int equationNumber) {

        //Create a new image bitmap and attach a brand new canvas to it
        Bitmap newBitmap = Bitmap.createBitmap(currentFrame.getWidth(), currentFrame.getHeight(), Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(newBitmap);

        //Draw the image bitmap into the canvas
        canvas.drawBitmap(currentFrame, 0, 0, null);

        // Set up the paint to be drawn on the canvas
        Paint paint = new Paint();
        paint.setColor(Color.GREEN);
        paint.setStrokeWidth(22); // text size

        // Write the text near the top right of the rectangle
        // TODO: if rect fills the entire bitmap, put the result inside of the rect
        // or toast the result. use the displayResultInsideRect
        Rect rect = equationRectangles.get(equationNumber);
        String resultStr = String.valueOf(result);
        int offsetRight = getOffsetRight(resultStr, rect);
        canvas.drawText(resultStr, rect.x + rect.width - offsetRight, rect.y, paint);

        // Display the image with rectangles on it
        pictureView.setImageDrawable(new BitmapDrawable(getResources(), newBitmap));
        currentFrame = newBitmap;

    }

    public int getOffsetRight(String result, Rect rect) {
        return 30;
    }
    // TODO.
    public boolean displayResultInsideRect(Rect rect) {
        return false;
    }

    public void redrawEquationRect(Rect rect, int equationNumber, int color) {

        if(rect == null) {
            rect = equationRectangles.get(equationNumber);
        }

        //Create a new image bitmap and attach a brand new canvas to it
        Bitmap newBitmap = Bitmap.createBitmap(currentFrame.getWidth(), currentFrame.getHeight(), Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(newBitmap);

        //Draw the image bitmap into the canvas
        canvas.drawBitmap(currentFrame, 0, 0, null);

        // Set up the paint to be drawn on the canvas
        Paint paint = new Paint();
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);

        // Redraw the rectangle
        android.graphics.Rect rectGraphic = new android.graphics.Rect((int) rect.tl().x, (int) rect.tl().y,
                (int) rect.br().x, (int) rect.br().y);
        canvas.drawRect(rectGraphic, paint);

        // Display the image with rectangles on it
        pictureView.setImageDrawable(new BitmapDrawable(getResources(), newBitmap));
        currentFrame = newBitmap;
    }

    public void drawEquationRects(List<Rect> rectList) {

        equationRectangles = rectList;

        //Create a new image bitmap and attach a brand new canvas to it
        Bitmap newBitmap = Bitmap.createBitmap(currentFrame.getWidth(), currentFrame.getHeight(), Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(newBitmap);

        //Draw the image bitmap into the canvas
        canvas.drawBitmap(currentFrame, 0, 0, null);

        // Set up the paint to be drawn on the canvas
        Paint paint = new Paint();
        paint.setColor(Color.YELLOW);
        paint.setStyle(Paint.Style.STROKE);

        // Draw each rectangle
        for(Rect rect : rectList) {
            android.graphics.Rect rectGraphic = new android.graphics.Rect((int) rect.tl().x, (int) rect.tl().y,
                    (int) rect.br().x, (int) rect.br().y);
            canvas.drawRect(rectGraphic, paint);
        }

        // Display the image with rectangles on it
        pictureView.setImageDrawable(new BitmapDrawable(getResources(), newBitmap));
        currentFrame = newBitmap;
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause()");

        // Stop using the camera, to avoid conflicting with other camera-based apps
        mCamera.release();
        //cameraManager.closeDriver();

        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();

        //retrievePreferences();

        // Set up the camera preview surface.
        /*surfaceView = (SurfaceView) findViewById(R.id.preview_view);
        surfaceHolder = surfaceView.getHolder();
        if (!hasSurface) {
            surfaceHolder.addCallback(this);
            //surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }*/

        // Do OCR engine initialization, if necessary
        //boolean doNewInit = (baseApi == null) || !sourceLanguageCodeOcr.equals(previousSourceLanguageCodeOcr) ||
        //        ocrEngineMode != previousOcrEngineMode;
        if (baseApi == null) {
            // Initialize the OCR engine
            File storageDirectory = getStorageDirectory();
            if (storageDirectory != null) {
                initOcrEngine(storageDirectory, sourceLanguageCodeOcr, sourceLanguageReadable);
            }
        } else {
            // We already have the engine initialized, so just start the camera.
            resumeOCR();
        }
    }



    /**
     * Method to start or restart recognition after the OCR engine has been initialized,
     * or after the app regains focus. Sets state related settings and OCR engine parameters,
     * and requests camera initialization.
     */
    void resumeOCR() {
        Log.d(TAG, "resumeOCR()");

        //setButtonVisibility(true);
        //shutterButton.setClickable(true);

        // This method is called when Tesseract has already been successfully initialized, so set
        // isEngineReady = true here.
        isEngineReady = true;

        /*if (handler != null) {
            handler.resetState();
        }*/
        if (baseApi != null) {
            baseApi.setPageSegMode(pageSegmentationMode);
            //baseApi.setVariable(TessBaseAPI.VAR_CHAR_BLACKLIST, characterBlacklist);
            //baseApi.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, characterWhitelist);
        }

        /*if (hasSurface) {
            // The activity was paused but not stopped, so the surface still exists. Therefore
            // surfaceCreated() won't be called, so init the camera here.
            initCamera(surfaceHolder);
        }*/
    }


    private void processImageOld(byte[] data, Bitmap imgBitmap) {

        Bitmap newBitmap = imgBitmap; // = imgBitmap;
        /*
        Mat mat = new Mat(); //Mat.zeros(imgBitmap.getHeight(), imgBitmap.getWidth(), CvType.CV_8UC4);
        Utils.bitmapToMat(imgBitmap, mat);
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY);

        Mat matBW = new Mat(); //Mat.zeros(imgBitmap.getHeight(), imgBitmap.getWidth(), CvType.CV_8UC1);
        Imgproc.adaptiveThreshold(mat, matBW, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 25, 40);
        Utils.matToBitmap(matBW, newBitmap);
        */
        /*
        CameraUtils
                .buildLuminanceSource(data, imgBitmap.getWidth(), imgBitmap.getHeight())
                .renderCroppedGreyscaleBitmap();*/

        /*
        Mat mat = new Mat(); //Mat.zeros(imgBitmap.getHeight(), imgBitmap.getWidth(), CvType.CV_8UC4);
        Utils.bitmapToMat(imgBitmap, mat);
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY);

        Mat matBW = new Mat(); //Mat.zeros(imgBitmap.getHeight(), imgBitmap.getWidth(), CvType.CV_8UC1);
        Imgproc.adaptiveThreshold(mat, matBW, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 15, 40);
        Utils.matToBitmap(matBW, imgBitmap);
        */
        PlanarYUVLuminanceSource lum = new PlanarYUVLuminanceSource(data, imgBitmap.getWidth(), imgBitmap.getHeight(),
                0, 0,
                imgBitmap.getWidth(), imgBitmap.getHeight(), false);

        Bitmap bitmap2 = lum.renderCroppedGreyscaleBitmap(); //getCameraManager().buildLuminanceSource(data, width, height).renderCroppedGreyscaleBitmap();

        new FindEqnRectsAsyncTask(this, baseApi, bitmap2, imgBitmap.getWidth(), imgBitmap.getHeight())
                .execute();
    }

}
