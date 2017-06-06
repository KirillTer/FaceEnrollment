package com.intel.perc.cameras;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.view.accessibility.AccessibilityManagerCompat;
import android.util.Log;
import android.util.Size;
import android.widget.Toast;

public abstract class AbstractCamera implements Camera {
    protected final Size mImageDimension;
    protected final String mCameraHandlerThreadName;
    protected final int mCameraIndex;

    protected CameraDevice mCameraDevice;
    protected CameraCaptureSession mCameraCaptureSessions;//TODO may be remove from here
//    protected CaptureRequest.Builder mCaptureRequestBuilder;//TODO may be remove from here
    protected Handler mCameraHandler;
    protected HandlerThread mCameraThread;
    protected ImageAvailableListener mImageAvailableListener;
    protected Context mContext;
    protected Activity mActivity;

    private static String TAG = "AbstractCamera";
    public AbstractCamera(Context context, Size imageDimension, int cameraIndex, String cameraHandlerThreadName)
    {
        mContext = context;
        mActivity = (Activity) context;
        mCameraHandlerThreadName = cameraHandlerThreadName;
        mImageDimension = imageDimension;
        mCameraIndex = cameraIndex;
        mCameraDevice = null;
        // Create a new thread for the camera2 callbacks to arrive on
        startCameraThread();
//        openCamera(); TODO uncomment
    }

//    protected abstract void createCameraPreview();

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            mCameraDevice = camera;
        }
        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            Log.e(TAG, "CAMERA DISCONNECTED");
            if (mCameraDevice != null) {
                mCameraDevice.close();
            }

            mCameraDevice = null;
        }
        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            if (mCameraDevice != null) {
                mCameraDevice.close();
            }
            mCameraDevice = null;
        }
    };


    @Override
    public void setListener(ImageAvailableListener listener) {//TODO may be move abstract class
        mImageAvailableListener = listener;
    }

    protected void startCameraThread() {
        mCameraThread = new HandlerThread(mCameraHandlerThreadName);
        mCameraThread.start();
        mCameraHandler = new Handler(mCameraThread.getLooper());
    }

    protected void stopCameraThread() {
        mCameraThread.quitSafely();
        try {
            mCameraThread.join();
            mCameraThread = null;
            mCameraThread = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    protected void openCamera() {
        String cameraId;
        CameraManager manager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        try {
            if (mCameraIndex >=  manager.getCameraIdList().length)
            {
                mCameraDevice = null;
                Log.e(TAG, "failed to find camera number " + Integer.toString(mCameraIndex));
                Toast.makeText(mContext, "Failed to find camera", Toast.LENGTH_LONG).show();
                System.exit(1);
                return;
            }
            cameraId = manager.getCameraIdList()[mCameraIndex];
            if (mContext.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                mActivity.requestPermissions(new String[]{Manifest.permission.CAMERA}, 200);
                return;
            }
            manager.openCamera(cameraId, stateCallback, mCameraHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }
}
