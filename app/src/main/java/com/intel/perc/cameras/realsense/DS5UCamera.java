package com.intel.perc.cameras.realsense;

import android.app.Activity;
import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.MeteringRectangle;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.widget.Toast;

import com.intel.camera2.extensions.calibration.DepthCameraImageReader;
import com.intel.irfaceauthenticator.wrapper.ImageContainer;
import com.intel.perc.cameras.AbstractCamera;
import com.intel.perc.cameras.CaptureType;
import com.intel.perc.cameras.ImageAvailableListener;
import com.intel.perc.cameras.ImageSource;
import com.intel.perc.cameras.platform.PlatformCamera;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

import depth_camera.ICameraHAL;

public class DS5UCamera extends AbstractCamera {

    private final static int DS5_CAMERA_INDEX = 2;

    private static final int DS5_COLOR_FORMAT = DS5Utlils.Ds5ExtendedFormat.Y12I.getValue();
    private static final Size DS5_CAMERA_RESOLUTION = new Size(1920, 1080);
    private static final String CAMERA_HANDLER_NAME_THREAD = "DS5UCamera";
    private static final MeteringRectangle[] AE_RECTANGLES = new MeteringRectangle[] {new MeteringRectangle(690, 270, 1230, 810, MeteringRectangle.METERING_WEIGHT_MAX)};
    private static final String TAG = "DS5Camera";
    private static final int MAX_IMAGES_FOR_READER = 50;
    CaptureType mCaptureTypeWorkaround;//TODO signgle snapshot while streaming not work, this field used for simulation

    private Handler mPublisherHandler;
    private HandlerThread mPublisherThread;
    private boolean mIsPublishing;

    DepthCameraImageReader mReader;
    Surface mSurface;

    public DS5UCamera(Context context)
    {
        super(context, DS5_CAMERA_RESOLUTION, DS5_CAMERA_INDEX, CAMERA_HANDLER_NAME_THREAD);
        mCaptureTypeWorkaround = CaptureType.STREAM;
        mIsPublishing = false;
        try {
            SetAdvancedMode();
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException | RemoteException e) {
            Log.e(TAG, "failed to set advanced mode");
            Log.e(TAG, Log.getStackTraceString(e));
            Toast.makeText(mContext, "failed to set advanced mode", Toast.LENGTH_LONG).show();
        }

        openCamera();
        mPublisherThread = new HandlerThread("publisher thread");
        mPublisherThread.start();
        mPublisherHandler = new Handler(mPublisherThread.getLooper()); //TODO add stop somewere

    }

    //TODO show toast not works - check why
    private void ShowMessageAndExit(String message)
    {
        Log.e(TAG, message);
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mActivity.getApplicationContext(), message, Toast.LENGTH_SHORT).show();
            }
        });

        try {
            Thread.sleep(20000);
        } catch (InterruptedException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
        mActivity.finishAndRemoveTask();
        System.exit(0);
    }

    private class AsyncPublisher implements Runnable
    {
        CaptureType mCaptureType;
        Image mImage;
        ImageSource mImageSource;
        public AsyncPublisher( ImageSource imageSource, CaptureType captureType, Image image) {
            mCaptureType = captureType;
            mImage = image;
            mImageSource = imageSource;
        }

        @Override
        public void run() {
            mIsPublishing = true;
            if (mImageAvailableListener != null)
            {
                mImageAvailableListener.onImageAvailable(mImageSource, mCaptureType, mImage);
            }
             mIsPublishing = false;
        }
    }
    private void SetAdvancedMode() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, RemoteException {
        String serviceName = "depth_camera";
        ICameraHAL service = null;
        // Find the service and bind to it
        IBinder mBinder;
        Method getService = Class.forName("android.os.ServiceManager").getMethod("getService", String.class);
        mBinder = (IBinder) (getService.invoke(new Object(), serviceName));
        if (mBinder == null) {
            return;
        }
        ICameraHAL mService = ICameraHAL.Stub.asInterface(mBinder);
        mService.setMaintenance(DS5_CAMERA_INDEX, true);

        Log.d(TAG, "Advanced mode status: " + mService.getMaintenance(DS5_CAMERA_INDEX));
    }

    //TODO may be share this code with platform camera class
    class OnImageAvailibleListener implements DepthCameraImageReader.OnDepthCameraImageAvailableListener { //TODO may be rename to adapter????
        private CaptureType mCaptureType;
        private ImageAvailableListener mImageAvailableListener;

        public OnImageAvailibleListener(CaptureType captureType, ImageAvailableListener imageAvailableListener) {
            mCaptureType = captureType;
            mImageAvailableListener = imageAvailableListener;
        }

        @Override
        public void onDepthCameraImageAvailable(DepthCameraImageReader imageReader) {
            Log.d(TAG, "before acquireLatestImage");
            Image image = imageReader.acquireLatestImage();
            Log.d(TAG, "after acquireLatestImage");
            if (mIsPublishing)
            {
                Log.v(TAG, "drop frame - still not processed");
                image.close();
                return;
            }
            if (image != null) {
                Image.Plane[] planes = image.getPlanes();
                if (planes != null && planes[0] != null) {
                    try {
                        if (mImageAvailableListener != null) {
                            //mImageAvailableListener.onImageAvailable(ImageSource.DS5U, mCaptureType, image);//TODO uncomment
                            //mImageAvailableListener.onImageAvailable(ImageSource.DS5U, mCaptureTypeWorkaround, image);//TODO remove only workaround
                            mIsPublishing = true;
                            mPublisherHandler.post(new AsyncPublisher(ImageSource.DS5U, mCaptureTypeWorkaround, image));
                            mCaptureTypeWorkaround = CaptureType.STREAM; // restore regular value
                            //publisher is responsible for closing image
                        } else {
                            image.close();
                            Log.e(TAG, "mImageAvailableListener is null dropping image");
                        }
                    } catch (Exception se) {
                        image.close();
                        Log.e(TAG, se.getMessage(), se);
                        //closeCamera();
                    }
                }
                //image will be closed when next image is acquired
            }
        }
    }

    //TODO may be share this code with platform camera class
    class CameraCaptureSessionStateCallback extends CameraCaptureSession.StateCallback {
        CaptureType mCaptureType;
        CaptureRequest.Builder mRequestBuilder;
        public CameraCaptureSessionStateCallback(CaptureRequest.Builder requestBuilder, CaptureType captureType)
        {
            super();
            mCaptureType = captureType;
            mRequestBuilder = requestBuilder;
        }

        @Override
        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
            if (mCameraDevice == null) {
                return;
            }

            switch (mCaptureType) {
                case STREAM: {
                    mCameraCaptureSessions = cameraCaptureSession;
                    mRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);//TODO why we need this???
                    try {
                        cameraCaptureSession.setRepeatingRequest(mRequestBuilder.build(), null, mCameraHandler);
                    } catch (CameraAccessException e) {
                        Log.e(TAG, Log.getStackTraceString(e));
                        return;
                    }
                    break;
                }
                case SNAPSHOT: {
                    try {
                        cameraCaptureSession.capture(mRequestBuilder.build(), null, mCameraHandler);
                    } catch (CameraAccessException e) {
                        Log.e(TAG, Log.getStackTraceString(e));
                        return;
                    }
                    break;
                }
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
            Log.e(TAG, "onConfigureFailed");
//            Toast.makeText(mActivity, "onConfigureFailed", Toast.LENGTH_SHORT).show();
            ShowMessageAndExit("onConfigureFailed");
        }
    }
    @Override
    public void takePicture() {
        mCaptureTypeWorkaround = CaptureType.SNAPSHOT;//TODO remove - ugly workaround
    }

    @Override
    public void startStreaming() {
        try {
            for (int i = 0; i < 4 && mCameraDevice == null; ++i)
            {//TODO remove only for debug
                Log.d(TAG, "waiting for camera device initialization");
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if (mCameraDevice == null)
            {
                ShowMessageAndExit("failed to connect to camera");
            }

            mReader = DepthCameraImageReader.newInstance(DS5_CAMERA_RESOLUTION.getWidth(), DS5_CAMERA_RESOLUTION.getHeight(),  DS5_COLOR_FORMAT, MAX_IMAGES_FOR_READER);
            mSurface = mReader.getSurface();
            CaptureRequest.Builder captureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);//TODO check if TEMPLATE_PREVIEW is OK
            captureRequestBuilder.addTarget(mSurface);
            captureRequestBuilder.set(CaptureRequest.CONTROL_AE_REGIONS, AE_RECTANGLES);

            DepthCameraImageReader.OnDepthCameraImageAvailableListener readerListener = new DS5UCamera.OnImageAvailibleListener(CaptureType.STREAM, mImageAvailableListener);
            mReader.setOnImageAvailableListener(readerListener, mCameraHandler);

            mCameraDevice.createCaptureSession(Arrays.asList(mSurface), new DS5UCamera.CameraCaptureSessionStateCallback(captureRequestBuilder, CaptureType.STREAM), null);
        } catch (CameraAccessException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    @Override
    public void stopStreaming() {
        if (mCameraCaptureSessions == null)
        {
            return;
        }
        try {
            mCameraCaptureSessions.stopRepeating();
        } catch (CameraAccessException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }
}
