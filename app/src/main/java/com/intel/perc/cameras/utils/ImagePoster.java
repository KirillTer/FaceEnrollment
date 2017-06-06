package com.intel.perc.cameras.utils;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.media.Image;
import android.util.Log;
import android.util.Size;
import android.view.TextureView;

import com.intel.perc.cameras.realsense.DS5Utlils;
import com.intel.realsense.test.Y12IConverter;

import java.nio.ByteBuffer;

public class ImagePoster implements Runnable {
    private Image mImage;
    private TextureView mView;
    private static final  String TAG = "ImagePoster";

    public ImagePoster(Image image, TextureView view) {
        mImage = image;
        mView = view;
    }

    @Override
    public void run() {
        Bitmap bitmapImage = null;
        //TODO move this transformations to converter class
        if (mImage.getFormat() == ImageFormat.JPEG)
        {
            //read image bytes
            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.capacity()];
            buffer.get(bytes);
            //create bitmap
            bitmapImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);
        } else if (mImage.getFormat() == DS5Utlils.Ds5ExtendedFormat.Y8I.getValue())
        {
            //read image bytes
            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.capacity()];
            buffer.get(bytes);
            //create bitmap
            bitmapImage = new DS5Utlils().createBitmapFromFromY8IBuffer(new Size(mImage.getWidth(), mImage.getHeight()), bytes);
        }
        else if (mImage.getFormat() == DS5Utlils.Ds5ExtendedFormat.Y12I.getValue())
        {
            Y12IConverter converter = new Y12IConverter();
            bitmapImage = converter.createBitmapFromFromY12Image(mImage);
        }
        else
        {
            mImage.close();
            throw new RuntimeException("format not supported " + mImage.getFormat());
        }
        mImage.close();

        Canvas canvas = mView.lockCanvas();
        //scale image to canvas size
        RectF bitmapRect = new RectF(0, 0, bitmapImage.getWidth(), bitmapImage.getHeight());
        RectF canvasRect = new RectF(0, 0, canvas.getWidth(), canvas.getHeight());
        Matrix scaleMatrix = new Matrix();
        if (!scaleMatrix.setRectToRect(bitmapRect, canvasRect, Matrix.ScaleToFit.CENTER))
        {
            Log.e(TAG, "scaling failed");
        }

        if (canvas == null)
        {
            mImage.close();
            return;
        }
        //draw bitmap
        canvas.drawBitmap(bitmapImage, scaleMatrix, null);
        mView.unlockCanvasAndPost(canvas);
    }
}
