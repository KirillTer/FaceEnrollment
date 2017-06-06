package com.intel.faceenrollment;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.CamcorderProfile;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import com.intel.irfaceauthenticator.wrapper.AuthenticationStatus;
import com.intel.irfaceauthenticator.wrapper.FaceAuthenticatorDatabaseFactory;
import com.intel.irfaceauthenticator.wrapper.FaceAuthenticatorFactory;
import com.intel.irfaceauthenticator.wrapper.IFaceAuthenticator;
import com.intel.irfaceauthenticator.wrapper.IFaceAuthenticatorDatabase;
import com.intel.irfaceauthenticator.wrapper.ImageContainer;
import com.intel.irfaceauthenticator.wrapper.RegisterUserResult;
import com.intel.irfaceauthenticator.wrapper.RegisterUserStatus;
import com.intel.irfaceauthenticator.wrapper.SaveDatabaseStatus;
import com.intel.irfaceauthenticator.wrapper.UnregisterUserStatus;
import com.intel.perc.cameras.Camera;
import com.intel.perc.cameras.CaptureType;
import com.intel.perc.cameras.ImageAvailableListener;
import com.intel.perc.cameras.ImageSource;
import com.intel.perc.cameras.platform.PlatformCamera;
import com.intel.perc.cameras.realsense.DS5UCamera;
import com.intel.perc.cameras.utils.ImagePoster;

import junit.framework.Assert;


public class CameraActivity extends AppCompatActivity implements ImageAvailableListener {

    private static final String TAG = "FaceInrollment";
    private static final int REQUEST_CAMERA_PERMISSION_RESULT = 0;
    private static final int REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION_RESULT = 1;

    private static final float RECOGNITION_THRESHOLD = 784.6340f;
    private static final float LANDMARKS_THRESHOLD = 0.15f;
    private static String IR_IMAGE_1 = "/storage/emulated/0/Pictures/faceEnrollment/frame_12_ir.png";
    private static String IR_IMAGE_2 = "/storage/emulated/0/Pictures/faceEnrollment/frame_180_ir.png";
    private static final String DATABASE_FILE_PATH = "/storage/emulated/0/Pictures/faceEnrollment/savedDb.db";

    private TextView mCameraTextView;
    private TextureView mTextureView;
    private Camera mCamera;
    private HandlerThread mBackgroundHandlerThread;
    private Handler mBackgroundHandler;
    private State mState;
    private boolean mRegistered = false;
    private int mRegisteredUserId = -1;
    enum State
    {
        REGISTER,
        AUTHENTIFICATE,
        UNREGISTER,
        IDLE
    }

    @Override
    public void onImageAvailable(ImageSource source, CaptureType captureType, Image rawImage) {
        //Log.d(TAG, "arrived image from: " + source.toString() + " capture type: " + captureType.toString() + " image size " + Integer.toString(rawImage.getWidth()) + "x" + Integer.toString(rawImage.getHeight()));

        switch (captureType)
        {
            case STREAM:
                //ImagePoster will close the image
                if (mBackgroundHandler != null) {
                    //mBackgroundHandler.post(new ImagePoster(rawImage, mTextureView));
                    Thread thr = new Thread(new ImagePoster(rawImage, mTextureView));
                    thr.run();
                    try {
                        thr.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case SNAPSHOT:
                Log.d(TAG, "Snapshot arrived");
                ImageContainer imageContainer = new ImageContainerBuilder().Build(rawImage);

                switch (mState)
                {
                    case REGISTER:
                        // Register user
                        if (mRegistered)
                        {
                            Toast.makeText(getApplicationContext(), ""+RegisterUserStatus.FAILED_ALREADY_REGISTERED, Toast.LENGTH_SHORT).show();
                            break;
                        }
                        RegisterUserResult registerStatus = mFaceAuthenticator.registerUser(mFaceAuthenticatorDatabase, imageContainer);
                        mFaceAuthenticatorDatabase.saveDatabase(DATABASE_FILE_PATH);
                        Toast.makeText(getApplicationContext(), ""+registerStatus.getStatus(), Toast.LENGTH_SHORT).show();
                        if (registerStatus.getStatus() == RegisterUserStatus.SUCCESS)
                        {
                            mRegistered = true;
                            mStillImageButton.setClickable(false);
                            mRegisteredUserId = registerStatus.getUserId();
                        }
                        break;
                    case AUTHENTIFICATE:
                        //TODO load database
                        //IFaceAuthenticatorDatabase faceAuthenticatorDatabase = mFaceAuthenticatorDatabase.create(DATABASE_FILE_PATH);
                        AuthenticationStatus statusNew = mFaceAuthenticator.authenticateUser(mFaceAuthenticatorDatabase, imageContainer, mRegisteredUserId);
                        Toast.makeText(getApplicationContext(), ""+statusNew, Toast.LENGTH_SHORT).show();
                        break;
                    case UNREGISTER:
                        UnregisterUserStatus status = mFaceAuthenticator.unregisterUser(mFaceAuthenticatorDatabase, mRegisteredUserId);
                        Toast.makeText(getApplicationContext(), ""+status, Toast.LENGTH_SHORT).show();
                        if (status == UnregisterUserStatus.UNREGISTER_USER_SUCCESS)
                        {
                            mRegistered = false;
                            mStillImageButton.setClickable(true);
                        }
                        break;
                    case IDLE:
                        //do nothing
                        break;
                }

                String imageFilePath = "/storage/emulated/0/Pictures/faceEnrollment/image.bin";
                try {
                    FileOutputStream fos = new FileOutputStream(imageFilePath);
                    fos.write(imageContainer.buffer);
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                rawImage.close(); //TODO uncomment
                //ImageSaver will close the image
/*                Log.d(TAG, "saving image");
                try {
                    createImageFileName();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Thread thread = new Thread(new ImageSaver(rawImage));
                thread.start();
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }*/
                //mBackgroundHandler.post(new ImageSaver(rawImage));// TODO uncomment - check why onPaused called - stops worker thread
                break;
        }

    }

    private class ImageSaver implements Runnable {

        private final Image mImage;

        public ImageSaver(Image image) {
            mImage = image;
        }

        @Override
        public void run() {
            ByteBuffer byteBuffer = mImage.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(bytes);

            Log.d(TAG, "saving file to: " + mImageFileName.toString());
            FileOutputStream fileOutputStream = null;
            try {
                fileOutputStream = new FileOutputStream(mImageFileName);
                fileOutputStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mImage.close();

                Intent mediaStoreUpdateIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                mediaStoreUpdateIntent.setData(Uri.fromFile(new File(mImageFileName)));
                sendBroadcast(mediaStoreUpdateIntent);

                if(fileOutputStream != null) {
                    try {
                        fileOutputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

        }
    }

    private ImageButton mStillImageButton;
    private ImageButton mSaveImageButton;
    private Button mRecognizeButton;
    private Button mUnRegisterButton;
    private ImageView mRingImage;
    private ImageView mCornerImage;

    private File mImageFolder;
    private File currentImageFolder;
    private String mImageFileName;
    private File mSavedImageFileName;
    private String prepend;
    private File imageFile;

    private IFaceAuthenticator mFaceAuthenticator;
    private IFaceAuthenticatorDatabase mFaceAuthenticatorDatabase;

    private static SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 0);
        ORIENTATIONS.append(Surface.ROTATION_90, 90);
        ORIENTATIONS.append(Surface.ROTATION_180, 180);
        ORIENTATIONS.append(Surface.ROTATION_270, 270);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mState = State.IDLE;
        RequestReadWritePermissions();
        // Create database
        final FaceAuthenticatorDatabaseFactory faceAuthenticatorDatabaseFactory = new FaceAuthenticatorDatabaseFactory();
        mFaceAuthenticatorDatabase = faceAuthenticatorDatabaseFactory.create();

        // Create authenticator
        FaceAuthenticatorFactory faceAuthenticatorFactory = new FaceAuthenticatorFactory();

        Context testContext = getApplicationContext();
        mFaceAuthenticator = faceAuthenticatorFactory.create(testContext);

        setContentView(R.layout.activity_camera);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //createVideoFolder();
        createImageFolder();

        mTextureView = (TextureView) findViewById(R.id.textureView);
        mCameraTextView = (TextView) findViewById(R.id.cameraTextView);

        mRingImage = (ImageView) findViewById(R.id.imageRing);
        mCornerImage = (ImageView) findViewById(R.id.imageCorner);
        mRecognizeButton = (Button) findViewById(R.id.recognizeButton);
        mUnRegisterButton = (Button) findViewById(R.id.UnRegisterButton);
        mSaveImageButton = (ImageButton) findViewById(R.id.saveImageButton);
        mRecognizeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            //AuthenticateUser
//            IR_IMAGE_1 = imageFile.getParent();

            mState = State.AUTHENTIFICATE;
            mCamera.takePicture();
            }
        });
        mUnRegisterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //AuthenticateUser
//            IR_IMAGE_1 = imageFile.getParent();

                mState = State.UNREGISTER;
                mCamera.takePicture();
            }
        });
        mSaveImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CameraActivity.this, FileName.class);
                intent.putExtra("oldFileName",imageFile);
                intent.putExtra("imageFolder",mImageFolder);
                startActivity(intent);
            }
        });
        mRecognizeButton.setVisibility(View.GONE);
        mRecognizeButton.setVisibility(View.VISIBLE);
        mSaveImageButton.setVisibility(View.GONE);
        mStillImageButton = (ImageButton) findViewById(R.id.cameraImageButton);
        mStillImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
/*                mCameraTextView.setText("Face capture done");
                mSaveImageButton.setVisibility(View.VISIBLE);
                mRecognizeButton.setVisibility(View.VISIBLE);
                mRingImage.setVisibility(View.GONE);
                mCornerImage.setVisibility(View.GONE);
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)mStillImageButton.getLayoutParams();
                DisplayMetrics metrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(metrics);
                float logicalDensity = metrics.density;
                int px = (int) Math.ceil(80 * logicalDensity);
                params.setMargins(0, 0, px, 0); //substitute parameters for left, top, right, bottom
                mStillImageButton.setLayoutParams(params);
                checkWriteStoragePermission();
                try {
                    createImageFileName();
                } catch (IOException e) {
                    Log.e(TAG, Log.getStackTraceString(e));
                }*/
                mState = State.REGISTER;
                mCamera.takePicture();
            }
        });

        Intent intent = getIntent();
        prepend = (String) intent.getSerializableExtra("newFileName");
        mSavedImageFileName  = (File) intent.getSerializableExtra("oldFileName");
        currentImageFolder = (File) intent.getSerializableExtra("imageFolder");
/*        if (prepend != null) {
            //Toast.makeText(getApplicationContext(), prepend, Toast.LENGTH_SHORT).show();
//111
            File to = new File(currentImageFolder, prepend + ".png");
            boolean success = mSavedImageFileName.renameTo(to);
            mSavedImageFileName.delete();
            IR_IMAGE_1 = (currentImageFolder + prepend + ".png");

            // Register user
            RegisterUserResult registerStatus = mFaceAuthenticator.registerUser(mFaceAuthenticatorDatabase, IR_IMAGE_1);

            mFaceAuthenticatorDatabase.saveDatabase(DATABASE_FILE_PATH);

            Toast.makeText(getApplicationContext(), ""+registerStatus.getStatus(), Toast.LENGTH_SHORT).show();
        }
*/
        mCamera = new DS5UCamera(this);
        //mCamera = new PlatformCamera(this);
        mCamera.setListener(this);
    }

    private void RequestReadWritePermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        startBackgroundThread();
        mCamera.startStreaming();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length == 0)
        {
            return;
        }
        if(requestCode == REQUEST_CAMERA_PERMISSION_RESULT) {
            if(grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getApplicationContext(),
                        "Application will not run without camera services", Toast.LENGTH_SHORT).show();
            }
            if(grantResults[1] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getApplicationContext(),
                        "Application will not have audio on record", Toast.LENGTH_SHORT).show();
            }
        }
        if(requestCode == REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION_RESULT) {
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this,
                        "Permission successfully granted!", Toast.LENGTH_SHORT).show();
            } else {
                //Toast.makeText(this, "App needs to save video to run", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCamera.stopStreaming(); //TODO uncomment
        //closeCamera();
        //TODO stop streaming
        stopBackgroundThread();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocas) {
        super.onWindowFocusChanged(hasFocas);
        View decorView = getWindow().getDecorView();
        if(hasFocas) {
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    }

    private void startBackgroundThread() {
        mBackgroundHandlerThread = new HandlerThread("FaceEnrollment");
        mBackgroundHandlerThread.start();
        mBackgroundHandler = new Handler(mBackgroundHandlerThread.getLooper());
    }

    private void stopBackgroundThread() {
        mBackgroundHandlerThread.quitSafely();
        try {
            mBackgroundHandlerThread.join();
            mBackgroundHandlerThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void createImageFolder() {
        File imageFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        mImageFolder = new File(imageFile, "faceEnrollment");
        if(!mImageFolder.exists()) {
            mImageFolder.mkdirs();
        }
    }
//111
    private File createImageFileName() throws IOException {
        prepend = "111";
        imageFile = File.createTempFile(prepend, ".jpg", mImageFolder);
        mImageFileName = imageFile.getAbsolutePath();
        return imageFile;
    }

    private void checkWriteStoragePermission() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {

                if(shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    Toast.makeText(this, "app needs to be able to save videos", Toast.LENGTH_SHORT).show();
                }
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION_RESULT);
            }
        }
    }

}
