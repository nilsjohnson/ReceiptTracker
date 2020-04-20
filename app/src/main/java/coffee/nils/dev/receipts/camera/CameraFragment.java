package coffee.nils.dev.receipts.camera;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.MeteringRectangle;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;

import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;


import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import coffee.nils.dev.receipts.R;
import coffee.nils.dev.receipts.data.Receipt;
import coffee.nils.dev.receipts.data.ReceiptDAO;
import coffee.nils.dev.receipts.util.ImageUtil;

import static coffee.nils.dev.receipts.camera.CameraState.*;
import static coffee.nils.dev.receipts.util.ImageUtil.*;

public class CameraFragment extends Fragment implements View.OnClickListener, ActivityCompat.OnRequestPermissionsResultCallback
{
    static
    {
        System.loadLibrary("native-lib");
        System.loadLibrary("opencv_java4");
    }

    public native long autoCrop(long addr);

    // Conversion from screen rotation to JPEG orientationn
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private static final String FRAGMENT_DIALOG = "dialog";

    private ProgressBar progressBar;

    static
    {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private ReceiptDAO receiptDAO = ReceiptDAO.get(getContext());
    private UUID receiptID;
    private static final String ARG_RECEIPT_ID = "Receipt_ID";
    private static final String ARG_REVIEW_EVENT_HANDLER = "Review_Event_Handler";

    private static final String TAG = "CameraFragment";


     //Max preview height and width that is guaranteed by Camera2 API
    private static final int MAX_PREVIEW_WIDTH = 1920;
    private static final int MAX_PREVIEW_HEIGHT = 1080;

    /**
     * {@link TextureView.SurfaceTextureListener} handles several lifecycle events on a
     * {@link TextureView}.
     */
    private final TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener()
    {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height)
        {
            openCamera(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height)
        {
            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) { return true; }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture) { }
    };

    /** ID of the current {@link CameraDevice} */
    private String cameraId;

    /** An {@link AutoFitTextureView} for camera preview.*/
    private AutoFitTextureView textureView;

    /** A {@link CameraCaptureSession } for camera preview.*/
    private CameraCaptureSession captureSession;

    /** A reference to the opened {@link CameraDevice}.*/
    private CameraDevice cameraDevice;

    /** The {@link android.util.Size} of camera preview.*/
    private Size previewSize;

    /** {@link CameraDevice.StateCallback} is called when {@link CameraDevice} changes its state.*/
    private final CameraDevice.StateCallback callbackState = new CameraDevice.StateCallback()
    {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice)
        {
            // This method is called when the camera is opened.  We start camera preview here.
            cameraOpenCloseLock.release();
            CameraFragment.this.cameraDevice = cameraDevice;
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice)
        {
            cameraOpenCloseLock.release();
            cameraDevice.close();
            CameraFragment.this.cameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error)
        {
            cameraOpenCloseLock.release();
            cameraDevice.close();
            CameraFragment.this.cameraDevice = null;
            Activity activity = getActivity();
            if (null != activity)
            {
                activity.finish();
            }
        }

    };

    /** An additional thread for running tasks that shouldn't block the UI.*/
    private HandlerThread backgroundThread;

    /** A {@link Handler} for running tasks in the background.*/
    private Handler backgroundHandler;

    /** An {@link ImageReader} that handles still image capture.*/

    private ImageReader imageReader;
    private Bitmap bitmap;

    /**
     * This a callback object for the {@link ImageReader}. "onImageAvailable" will be called when a
     * still image is ready to be saved.
     */
    private final ImageReader.OnImageAvailableListener onImageAvailableListener = new ImageReader.OnImageAvailableListener()
    {
        @Override
        public void onImageAvailable(ImageReader reader)
        {
            Image image = reader.acquireLatestImage();
            // image to byte array
            ByteBuffer bb = image.getPlanes()[0].getBuffer();
            byte[] data = new byte[bb.remaining()];
            bb.get(data);

            Mat mat = Imgcodecs.imdecode(new MatOfByte(data), Imgcodecs.IMREAD_UNCHANGED);
            long addr = autoCrop(autoCrop(mat.getNativeObjAddr()));
            Mat cropped = new Mat(addr);
            bitmap = ImageUtil.getEmptyBitmap(cropped);
            Utils.matToBitmap(cropped, bitmap);

            Receipt r = receiptDAO.getReceiptById(receiptID);
            backgroundHandler.post(receiptDAO.makeImageSaver(bitmap, r.getFileName()));
        }

    };

    /** {@link CaptureRequest.Builder} for the camera preview */
    private CaptureRequest.Builder previewBuilder;

    /** {@link CaptureRequest} generated by {@link #previewBuilder} */
    private CaptureRequest previewRequest;

    /** The current state of camera state for taking pictures. */
    private CameraState state = STATE_PREVIEW;

    /**A {@link Semaphore} to prevent the app from exiting before closing the camera. */
    private Semaphore cameraOpenCloseLock = new Semaphore(1);

    /** Whether the current camera device supports Flash or not. */
    private boolean flashSupported;

    /** Orientation of the camera sensor */
    private int sensorOrientation;

    /** A {@link CameraCaptureSession.CaptureCallback} that handles events related to JPEG capture. */
    private CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback()
    {
        private void process(CaptureResult result)
        {
            Log.d(TAG, "state @ time of callback: " + state.toString());
            switch (state)
            {
                case STATE_PREVIEW:
                {
                    // We have nothing to do when the camera preview is working normally.
                    break;
                }
                case STATE_WAITING_LOCK:
                {
                    Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                    if (afState == null)
                    {
                        captureStillPicture();
                    }
                    else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState || CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState)
                    {
                        // CONTROL_AE_STATE can be null on some devices
                        Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                        if (aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED)
                        {
                            state = STATE_PICTURE_TAKEN;
                            captureStillPicture();
                        }
                        else
                        {
                            runPrecaptureSequence();
                        }
                    }
                    break;
                }
                case STATE_WAITING_PRECAPTURE:
                {
                    // CONTROL_AE_STATE can be null on some devices
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE || aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED)
                    {
                        state = STATE_WAITING_NON_PRECAPTURE;
                    }
                    break;
                }
                case STATE_WAITING_NON_PRECAPTURE:
                {
                    // CONTROL_AE_STATE can be null on some devices
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE)
                    {
                        state = STATE_PICTURE_TAKEN;
                        captureStillPicture();
                    }
                    break;
                }
            }
        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult)
        {
            process(partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result)
        {
            process(result);
        }
    };


    /**
     * Capture a still picture. This method should be called when we get a response in
     * {@link #captureCallback} from both {@link #lockFocus()}.
     */
    private void captureStillPicture()
    {
        try
        {
            final Activity activity = getActivity();
            if (null == activity || null == cameraDevice)
            {
                return;
            }
            // This is the CaptureRequest.Builder that we use to take a picture.

            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(imageReader.getSurface());

            // Use the same AE and AF modes as the preview.
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            setAutoFlash(captureBuilder);

            // Orientation
            int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getOrientation(rotation));

            CameraCaptureSession.CaptureCallback CaptureCallback = new CameraCaptureSession.CaptureCallback()
            {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result)
                {
                    showToast("Saved");
                    Log.d(TAG, "Saved");
                    unlockFocus();
                    reviewAction.startReview(bitmap, receiptDAO.getReceiptById(receiptID).getFileName());

                }
            };

            captureSession.stopRepeating();
            captureSession.abortCaptures();
            captureSession.capture(captureBuilder.build(), CaptureCallback, null);
        }
        catch (CameraAccessException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Shows a {@link Toast} on the UI thread.
     *
     * @param text The message to show
     */
    private void showToast(final String text)
    {
        final Activity activity = getActivity();
        if (activity != null)
        {
            activity.runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    Toast.makeText(activity, text, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    /**
     * Given {@code choices} of {@code Size}s supported by a camera, choose the smallest one that
     * is at least as large as the respective texture view size, and that is at most as large as the
     * respective max size, and whose aspect ratio matches with the specified value. If such size
     * doesn't exist, choose the largest one that is at most as large as the respective max size,
     * and whose aspect ratio matches with the specified value.
     *
     * @param choices           The list of sizes that the camera supports for the intended output
     *                          class
     * @param textureViewWidth  The width of the texture view relative to sensor coordinate
     * @param textureViewHeight The height of the texture view relative to sensor coordinate
     * @param maxWidth          The maximum width that can be chosen
     * @param maxHeight         The maximum height that can be chosen
     * @param aspectRatio       The aspect ratio
     * @return The optimal {@code Size}, or an arbitrary one if none were big enough
     */
    private static Size chooseOptimalSize(Size[] choices, int textureViewWidth, int textureViewHeight, int maxWidth, int maxHeight, Size aspectRatio)
    {
        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        // Collect the supported resolutions that are smaller than the preview Surface
        List<Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices)
        {
            if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight && option.getHeight() == option.getWidth() * h / w)
            {
                if (option.getWidth() >= textureViewWidth && option.getHeight() >= textureViewHeight)
                {
                    bigEnough.add(option);
                }
                else
                {
                    notBigEnough.add(option);
                }
            }
        }

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        if (bigEnough.size() > 0)
        {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else if (notBigEnough.size() > 0)
        {
            return Collections.max(notBigEnough, new CompareSizesByArea());
        } else
        {
            Log.e(TAG, "Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    public static CameraFragment newInstance(UUID receiptID, CameraActivity.ReviewImage reviewAction)
    {
        Bundle args = new Bundle();
        args.putSerializable(ARG_RECEIPT_ID, receiptID);
        args.putSerializable(ARG_REVIEW_EVENT_HANDLER, reviewAction);
        CameraFragment camFrag = new CameraFragment();
        camFrag.setArguments(args);
        return camFrag;
    }

    private CameraActivity.ReviewImage reviewAction;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if (getArguments() != null)
        {
           receiptID = (UUID) getArguments().getSerializable(ARG_RECEIPT_ID);
           reviewAction = (CameraActivity.ReviewImage) getArguments().getSerializable(ARG_REVIEW_EVENT_HANDLER);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.fragment_camera2_basic, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState)
    {
        view.findViewById(R.id.btn_take_picture).setOnClickListener(this);
        textureView = (AutoFitTextureView) view.findViewById(R.id.texture);
        progressBar = view.findViewById(R.id.progressBar_cropping);
        progressBar.setVisibility(ProgressBar.INVISIBLE);

        textureView.setOnTouchListener(new TextureView.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                int x = (int)event.getX();
                int y = (int)event.getY();
                focusArea(x, y);
                return true;
            }
        });
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        startBackgroundThread();

        // When the screen is turned off and turned back on, the SurfaceTexture is already
        // available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
        // a camera and start preview from here (otherwise, we wait until the surface is ready in
        // the SurfaceTextureListener).
        if (textureView.isAvailable())
        {
            openCamera(textureView.getWidth(), textureView.getHeight());
        }
        else
        {
            textureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    @Override
    public void onPause()
    {
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    private void requestCameraPermission()
    {
        if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA))
        {
            new ConfirmationDialog().show(getChildFragmentManager(), FRAGMENT_DIALOG);
        }
        else
        {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults)
    {
        if (requestCode == REQUEST_CAMERA_PERMISSION)
        {
            if (grantResults.length != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED)
            {
                ErrorDialog.newInstance(getString(R.string.request_permission))
                        .show(getChildFragmentManager(), FRAGMENT_DIALOG);
            }
        }
        else
        {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    /**
     * Sets up member variables related to camera.
     *
     * @param width  The width of available size for camera preview
     * @param height The height of available size for camera preview
     */
    @SuppressWarnings("SuspiciousNameCombination")
    private void setUpCameraOutputs(int width, int height)
    {
        Activity activity = getActivity();
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try
        {
            for (String cameraId : manager.getCameraIdList())
            {
                CameraCharacteristics characteristics
                        = manager.getCameraCharacteristics(cameraId);

                // We don't use a front facing camera in this sample.
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT)
                {
                    continue;
                }

                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map == null)
                {
                    continue;
                }

                // For still image captures, we use the largest available size.
                Size largest = Collections.max(
                        Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                        new CompareSizesByArea());
                imageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(), ImageFormat.JPEG, /*maxImages*/2);
                imageReader.setOnImageAvailableListener(onImageAvailableListener, backgroundHandler);

                // Find out if we need to swap dimension to get the preview size relative to sensor
                // coordinate.
                int displayRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
                //noinspection ConstantConditions
                sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                boolean swappedDimensions = false;
                switch (displayRotation)
                {
                    case Surface.ROTATION_0:
                    case Surface.ROTATION_180:
                        if (sensorOrientation == 90 || sensorOrientation == 270)
                        {
                            swappedDimensions = true;
                        }
                        break;
                    case Surface.ROTATION_90:
                    case Surface.ROTATION_270:
                        if (sensorOrientation == 0 || sensorOrientation == 180)
                        {
                            swappedDimensions = true;
                        }
                        break;
                    default:
                        Log.e(TAG, "Display rotation is invalid: " + displayRotation);
                }

                Point displaySize = new Point();
                activity.getWindowManager().getDefaultDisplay().getSize(displaySize);
                int rotatedPreviewWidth = width;
                int rotatedPreviewHeight = height;
                int maxPreviewWidth = displaySize.x;
                int maxPreviewHeight = displaySize.y;

                if (swappedDimensions)
                {
                    rotatedPreviewWidth = height;
                    rotatedPreviewHeight = width;
                    maxPreviewWidth = displaySize.y;
                    maxPreviewHeight = displaySize.x;
                }

                if (maxPreviewWidth > MAX_PREVIEW_WIDTH)
                {
                    maxPreviewWidth = MAX_PREVIEW_WIDTH;
                }

                if (maxPreviewHeight > MAX_PREVIEW_HEIGHT)
                {
                    maxPreviewHeight = MAX_PREVIEW_HEIGHT;
                }

                // Danger, W.R.! Attempting to use too large a preview size could  exceed the camera
                // bus' bandwidth limitation, resulting in gorgeous previews but the storage of
                // garbage capture data.
                previewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                        rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth,
                        maxPreviewHeight, largest);

                // We fit the aspect ratio of TextureView to the size of preview we picked.
                int orientation = getResources().getConfiguration().orientation;
                if (orientation == Configuration.ORIENTATION_LANDSCAPE)
                {
                    textureView.setAspectRatio(previewSize.getWidth(), previewSize.getHeight());
                }
                else
                {
                    textureView.setAspectRatio(previewSize.getHeight(), previewSize.getWidth());
                }

                // Check if the flash is supported.
                Boolean available = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                flashSupported = available == null ? false : available;

                this.cameraId = cameraId;
                return;
            }
        }
        catch (CameraAccessException e)
        {
            e.printStackTrace();
        }
        catch (NullPointerException e)
        {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            ErrorDialog.newInstance(getString(R.string.camera_error)).show(getChildFragmentManager(), FRAGMENT_DIALOG);
        }
    }

    /**
     * Opens the camera specified by {@link CameraFragment#cameraId}.
     */
    private void openCamera(int width, int height)
    {
        if(ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
        {
            requestCameraPermission();
            return;
        }
        setUpCameraOutputs(width, height);
        configureTransform(width, height);
        Activity activity = getActivity();
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try
        {
            if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS))
            {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            manager.openCamera(cameraId, callbackState, backgroundHandler);
        }
        catch (CameraAccessException e)
        {
            e.printStackTrace();
        }
        catch (InterruptedException e)
        {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }
    }

    /**
     * Closes the current {@link CameraDevice}.
     */
    private void closeCamera()
    {
        try
        {
            cameraOpenCloseLock.acquire();
            if (null != captureSession)
            {
                captureSession.close();
                captureSession = null;
            }
            if (null != cameraDevice)
            {
                cameraDevice.close();
                cameraDevice = null;
            }
            if (null != imageReader)
            {
                imageReader.close();
                imageReader = null;
            }
        }
        catch (InterruptedException e)
        {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        }
        finally
        {
            cameraOpenCloseLock.release();
        }
    }

    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread()
    {
        backgroundThread = new HandlerThread("CameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread()
    {
        backgroundThread.quitSafely();
        try
        {
            backgroundThread.join();
            backgroundThread = null;
            backgroundHandler = null;
        } catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Creates a new {@link CameraCaptureSession} for camera preview.
     */
    private void createCameraPreviewSession()
    {
        try
        {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;

            // We configure the size of default buffer to be the size of camera preview we want.
            texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());

            // This is the output Surface we need to start preview.
            Surface surface = new Surface(texture);

            // We set up a CaptureRequest.Builder with the output Surface.
            previewBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewBuilder.addTarget(surface);

            // Here, we create a CameraCaptureSession for camera preview.
            cameraDevice.createCaptureSession(Arrays.asList(surface, imageReader.getSurface()),
                    new CameraCaptureSession.StateCallback()
                    {

                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession)
                        {
                            // The camera is already closed
                            if (null == cameraDevice)
                            {
                                return;
                            }

                            // When the session is ready, we start displaying the preview.
                            captureSession = cameraCaptureSession;
                            try
                            {
                                // Auto focus should be continuous for camera preview.
                                previewBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                // Flash is automatically enabled when necessary.
                                setAutoFlash(previewBuilder);

                                // Finally, we start displaying the camera preview.
                                previewRequest = previewBuilder.build();
                                captureSession.setRepeatingRequest(previewRequest, captureCallback, backgroundHandler);
                            } catch (CameraAccessException e)
                            {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(
                                @NonNull CameraCaptureSession cameraCaptureSession)
                        {
                            showToast("Failed");
                        }
                    }, null
            );
        } catch (CameraAccessException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Configures the necessary {@link android.graphics.Matrix} transformation to `textureView`.
     * This method should be called after the camera preview size is determined in
     * setUpCameraOutputs and also the size of `textureView` is fixed.
     *
     * @param viewWidth  The width of `textureView`
     * @param viewHeight The height of `textureView`
     */
    private void configureTransform(int viewWidth, int viewHeight)
    {
        Activity activity = getActivity();
        if (null == textureView || null == previewSize || null == activity)
        {
            return;
        }

        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();

        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, previewSize.getHeight(), previewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();

        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation)
        {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / previewSize.getHeight(),
                    (float) viewWidth / previewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        }
        else if (Surface.ROTATION_180 == rotation)
        {
            matrix.postRotate(180, centerX, centerY);
        }
        textureView.setTransform(matrix);
    }

    /**
     * Initiate a still image capture.
     */
    private void takePicture()
    {
        progressBar.setVisibility(ProgressBar.VISIBLE);
        lockFocus();
    }

    private void focusArea(int xCoordinate, int yCoordinate)
    {
        Log.d(TAG, "focus called: state:" + state.toString());
        if (cameraId == null) { return; }


        CameraManager cm = (CameraManager)getActivity().getSystemService(Context.CAMERA_SERVICE);
        CameraCharacteristics cc = null;

        MeteringRectangle focusArea;
        try
        {
            cc = cm.getCameraCharacteristics(cameraId);
            focusArea = new MeteringRectangle(xCoordinate-100,yCoordinate-100,200,200, MeteringRectangle.METERING_WEIGHT_DONT_CARE);

            captureSession.stopRepeating();
            previewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_IDLE);
            previewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
            // capture once to apply settings
            captureSession.capture(previewBuilder.build(), captureCallback, backgroundHandler);
            state = STATE_PREVIEW;

            if (meteringAreaAESupported(cc))
            {
                Log.d(TAG, "AE regions are suppored");
                previewBuilder.set(CaptureRequest.CONTROL_AE_REGIONS, new MeteringRectangle[]{focusArea});
            }
            if (meteringAreaAFSupported(cc))
            {
                Log.d(TAG, "AF regions are suppored");
                previewBuilder.set(CaptureRequest.CONTROL_AF_REGIONS, new MeteringRectangle[]{focusArea});
                previewBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
            }

            // capture again to set focus
            captureSession.capture(previewBuilder.build(), captureCallback, backgroundHandler);

            previewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_IDLE);
            previewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            previewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, null);

            captureSession.setRepeatingRequest(previewBuilder.build(), captureCallback, backgroundHandler);
        }
        catch (CameraAccessException e)
        {
            Log.e(TAG, "Problem setting the touch baseed focused.\n" + e.getMessage());
        }
    }

    /**
     * Lock the focus as the first step for a still image capture.
     */
    private void lockFocus()
    {
        try
        {
            // This is how to tell the camera to lock focus.
            previewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
            // Tell #mCaptureCallback to wait for the lock.
            state = STATE_WAITING_LOCK;
            captureSession.capture(previewBuilder.build(), captureCallback, backgroundHandler);
        } catch (CameraAccessException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Run the precapture sequence for capturing a still image. This method should be called when
     * we get a response in {@link #captureCallback} from {@link #lockFocus()}.
     */
    private void runPrecaptureSequence()
    {
        try
        {
            // This is how to tell the camera to trigger.
            previewBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            // Tell #mCaptureCallback to wait for the precapture sequence to be set.
            state = STATE_WAITING_PRECAPTURE;
            captureSession.capture(previewBuilder.build(), captureCallback, backgroundHandler);
        }
        catch (CameraAccessException e)
        {
            e.printStackTrace();
        }
    }

    private boolean meteringAreaAESupported(CameraCharacteristics cc)
    {
        Integer value = cc.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE);
        if (value != null)
        {
            return value >= 1;
        }

        return false;
    }

    private boolean meteringAreaAFSupported(CameraCharacteristics cc)
    {
        Integer value = cc.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF);
        if (value != null)
        {
            return value >= 1;
        }
        return false;
    }

    /**
     * Retrieves the JPEG orientation from the specified screen rotation.
     *
     * @param rotation The screen rotation.
     * @return The JPEG orientation (one of 0, 90, 270, and 360)
     */
    private int getOrientation(int rotation)
    {
        // Sensor orientation is 90 for most devices, or 270 for some devices (eg. Nexus 5X)
        // We have to take that into account and rotate JPEG properly.
        // For devices with orientation of 90, we simply return our mapping from ORIENTATIONS.
        // For devices with orientation of 270, we need to rotate the JPEG 180 degrees.
        return (ORIENTATIONS.get(rotation) + sensorOrientation + 270) % 360;
    }

    /**
     * Unlock the focus. This method should be called when still image capture sequence is
     * finished.
     */
    private void unlockFocus()
    {
        try
        {
            // Reset the auto-focus trigger
            previewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            setAutoFlash(previewBuilder);
            captureSession.capture(previewBuilder.build(), captureCallback, backgroundHandler);
            // After this, the camera will go back to the normal state of preview.
            state = STATE_PREVIEW;
            captureSession.setRepeatingRequest(previewRequest, captureCallback, backgroundHandler);
        }
        catch (CameraAccessException e)
        {
            Log.e(TAG, e.getMessage());
        }
    }

    @Override
    public void onClick(View view)
    {
        switch (view.getId())
        {
            case R.id.btn_take_picture:
            {
                takePicture();
                break;
            }
            case R.id.info:
            {
                Activity activity = getActivity();
                if (null != activity)
                {
                    new AlertDialog.Builder(activity)
                            .setMessage(R.string.intro_message)
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                }
                break;
            }
        }
    }

    private void setAutoFlash(CaptureRequest.Builder requestBuilder)
    {
        if (flashSupported)
        {
            requestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
        }
    }

    /**
     * Compares two {@code Size}s based on their areas.
     */
    static class CompareSizesByArea implements Comparator<Size>
    {
        @Override
        public int compare(Size lhs, Size rhs)
        {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() - (long) rhs.getWidth() * rhs.getHeight());
        }
    }

    /**
     * Shows an error message dialog.
     */
    public static class ErrorDialog extends DialogFragment
    {
        private static final String ARG_MESSAGE = "message";

        public static ErrorDialog newInstance(String message)
        {
            ErrorDialog dialog = new ErrorDialog();
            Bundle args = new Bundle();
            args.putString(ARG_MESSAGE, message);
            dialog.setArguments(args);
            return dialog;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState)
        {
            final Activity activity = getActivity();
            return new AlertDialog.Builder(activity)
                    .setMessage(getArguments().getString(ARG_MESSAGE))
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i)
                        {
                            activity.finish();
                        }
                    })
                    .create();
        }

    }

    /**
     * Shows OK/Cancel confirmation dialog about camera permission.
     */
    public static class ConfirmationDialog extends DialogFragment
    {
        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState)
        {
            final Fragment parent = getParentFragment();
            return new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.request_permission)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            parent.requestPermissions(new String[]{Manifest.permission.CAMERA},
                                    REQUEST_CAMERA_PERMISSION);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel,
                            new DialogInterface.OnClickListener()
                            {
                                @Override
                                public void onClick(DialogInterface dialog, int which)
                                {
                                    Activity activity = parent.getActivity();
                                    if (activity != null)
                                    {
                                        activity.finish();
                                    }
                                }
                            })
                    .create();
        }
    }
}