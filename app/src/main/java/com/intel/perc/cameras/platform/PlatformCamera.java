package com.intel.perc.cameras.platform;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.widget.Toast;

import com.intel.perc.cameras.AbstractCamera;
import com.intel.perc.cameras.CaptureType;
import com.intel.perc.cameras.ImageAvailableListener;
import com.intel.perc.cameras.ImageSource;


import java.util.Arrays;


public class PlatformCamera extends AbstractCamera {
    private static final int PLATFORM_FACE_CAMERA_INDEX = 1;
    private static final Size PLATFORM_CAMERA_RESOLUTION = new Size(1920, 1080);
    private static final String CAMERA_HANDLER_NAME_THREAD = "DS5 Thread";

    private static final int MAX_IMAGES_FOR_READER = 5;
    private static final int PLATFORM_IMAGE_FORMAT = ImageFormat.JPEG;
    private static final String TAG = "PlatformCamera";

    CaptureType mCaptureTypeWorkaround;//TODO signgle snapshot while streaming not work, this field used for simulation

    public PlatformCamera(Context context)
    {
        super(context, PLATFORM_CAMERA_RESOLUTION, PLATFORM_FACE_CAMERA_INDEX, CAMERA_HANDLER_NAME_THREAD);
        mCaptureTypeWorkaround = CaptureType.STREAM;
        openCamera();
    }

/*    CaptureRequest.Builder BuildCaptureRequest()
    {
        try {
            CaptureRequest.Builder captureRequestBuilder = null;
            ImageReader reader = ImageReader.newInstance(mImageDimension.getWidth(), mImageDimension.getHeight(),  PLATFORM_IMAGE_FORMAT, MAX_IMAGES_FOR_READER);
            Surface surface = reader.getSurface();
            captureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            //for ds5 here should be AE ROI

            ImageReader.OnImageAvailableListener readerListener = new  ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader imageReader) {
                    Image image = null;
                    image = imageReader.acquireLatestImage();
                    if (image != null) {
                        Image.Plane[] planes = image.getPlanes();
                        if (planes != null && planes[0] != null) {
                            try {
                                if (mImageAvailableListener != null) {
                                    mImageAvailableListener.onImageAvailable(ImageSource.DS5U, CaptureType.STREAM, image);
                                }
                                else {
                                    Log.e(TAG, "mImageAvailableListener is null");
                                }
                            } catch (Exception se) {
                                Log.e(TAG, se.getMessage(), se);
                                //closeCamera();
                            }
                        }
                        //image will be closed when next image is acquired
                    }
                }


            };
            reader.setOnImageAvailableListener(readerListener, mCameraHandler);
    }*/

    class OnImageAvailibleListener implements ImageReader.OnImageAvailableListener { //TODO may be rename to adapter????
        private CaptureType mCaptureType;
        private ImageAvailableListener mImageAvailableListener;

        public OnImageAvailibleListener(CaptureType captureType, ImageAvailableListener imageAvailableListener) {
            mCaptureType = captureType;
            mImageAvailableListener = imageAvailableListener;
        }

        @Override
        public void onImageAvailable(ImageReader imageReader) {
            Image image = imageReader.acquireLatestImage();
            if (image != null) {
                Image.Plane[] planes = image.getPlanes();
                if (planes != null && planes[0] != null) {
                    try {
                        if (mImageAvailableListener != null) {
                            //mImageAvailableListener.onImageAvailable(ImageSource.PCAM, mCaptureType, image);//TODO uncomment

                            mImageAvailableListener.onImageAvailable(ImageSource.PCAM, mCaptureTypeWorkaround, image);//TODO remove only workaround
                            mCaptureTypeWorkaround = CaptureType.STREAM; // restore regular value
                        } else {
                            Log.e(TAG, "mImageAvailableListener is null dropping image");
                            image.close();
                        }
                    } catch (Exception se) {
                        Log.e(TAG, se.getMessage(), se);
                        //closeCamera();
                    }
                }
                //image will be closed when next image is acquired
            }
        }
    }



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
                    mRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
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
            Toast.makeText(mActivity, "onConfigureFailed", Toast.LENGTH_SHORT).show();
        }
    }

    protected void startCapture(CaptureType captureType) {
        try {
            for (int i = 0; i < 100 && mCameraDevice == null; ++i)
            {//TODO remove only for debug
                Log.d(TAG, "waiting for camera device initialization");
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            ImageReader reader = ImageReader.newInstance(mImageDimension.getWidth(), mImageDimension.getHeight(),  PLATFORM_IMAGE_FORMAT, MAX_IMAGES_FOR_READER);
            Surface surface = reader.getSurface();
            CaptureRequest.Builder captureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);//TODO check if TEMPLATE_PREVIEW is OK
            captureRequestBuilder.addTarget(surface);
            //for ds5 here should be AE ROI

            ImageReader.OnImageAvailableListener readerListener = new OnImageAvailibleListener(captureType, mImageAvailableListener);
            reader.setOnImageAvailableListener(readerListener, mCameraHandler);

            captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, 270);//TODO try dont use hardcoderd
            mCameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSessionStateCallback(captureRequestBuilder, captureType), null);
        } catch (CameraAccessException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }


    protected void stopCapture() {
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


    @Override
    public void takePicture() {
        mCaptureTypeWorkaround = CaptureType.SNAPSHOT;//TODO remove - ugly workaround
        //startCapture(CaptureType.SNAPSHOT);//TODO uncomment
    }

    @Override
    public void startStreaming() {
        startCapture(CaptureType.STREAM);
    }

    @Override
    public void stopStreaming() {
        stopCapture();
    }

/*    @Override
    protected void createCameraPreview() {
        try {
            ImageReader reader = ImageReader.newInstance(mImageDimension.getWidth(), mImageDimension.getHeight(),  PLATFORM_IMAGE_FORMAT, MAX_IMAGES_FOR_READER);
            Surface surface = reader.getSurface();
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mCaptureRequestBuilder.addTarget(surface);
            //for ds5 here should be AE ROI

            ImageReader.OnImageAvailableListener readerListener = new  ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader imageReader) {
                    Image image = null;
                    image = imageReader.acquireLatestImage();
                    if (image != null) {
                        Image.Plane[] planes = image.getPlanes();
                        if (planes != null && planes[0] != null) {
                            try {
                                if (mImageAvailableListener != null) {
                                    mImageAvailableListener.onImageAvailable(ImageSource.DS5U, CaptureType.STREAM, image);
                                }
                                else {
                                    Log.e(TAG, "mImageAvailableListener is null");
                                }
                            } catch (Exception se) {
                                Log.e(TAG, se.getMessage(), se);
                                //closeCamera();
                            }
                        }
                        //image will be closed when next image is acquired
                    }
                }


            };
            reader.setOnImageAvailableListener(readerListener, mCameraHandler);

            mCameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    if (mCameraDevice == null) {
                        return;
                    }
                    mCameraCaptureSessions = session;
                    mCaptureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                    try {
                        mCameraCaptureSessions.setRepeatingRequest(mCaptureRequestBuilder.build(), null, mCameraHandler);
                    } catch (CameraAccessException e) {
                        Log.e(TAG,  Log.getStackTraceString(e));
                        return;
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Toast.makeText(mActivity, "Configuration change", Toast.LENGTH_SHORT).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }*/

/*    protected void updatePreview() {
        if (mCameraDevice != null) {
            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            try {
                mCameraCaptureSessions.setRepeatingRequest(mCaptureRequestBuilder.build(), null, mCameraHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }*/

}
