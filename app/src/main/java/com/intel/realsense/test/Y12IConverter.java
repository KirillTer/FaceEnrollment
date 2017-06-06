package com.intel.realsense.test;

import android.graphics.Bitmap;
import android.media.Image;
import android.os.Handler;

import java.nio.ByteBuffer;

/**
 * Created by liuweina on 5/25/17.
 */

public class Y12IConverter {
    static {
        System.loadLibrary("DepthConverter");
    }

    @SuppressWarnings("JniMissingFunction")
    private native void Y12ItoRGB(int width, int height, ByteBuffer input, ByteBuffer rgb);

    @SuppressWarnings("JniMissingFunction")
    private native void Y12ItoShort(int width, int height, ByteBuffer input, ByteBuffer shortData);

    public Bitmap createBitmapFromFromY12Image(Image image) {

        int width = image.getWidth();
        int height = image.getHeight();
        ByteBuffer rgbBuffer = ByteBuffer.allocateDirect(width * height * 4);
        ByteBuffer inputBuffer = image.getPlanes()[0].getBuffer();
        Y12ItoRGB(image.getWidth(), image.getHeight(), inputBuffer, rgbBuffer);
        rgbBuffer.rewind();
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(rgbBuffer);
        //image.close();
        return bitmap;
    }

    public byte [] createShortBufferFromY12Image(Image image)
    {
        int width = image.getWidth();
        int height = image.getHeight();
        ByteBuffer shortBuffer = ByteBuffer.allocateDirect(width * height * 2);
        ByteBuffer inputBuffer = image.getPlanes()[0].getBuffer();
        Y12ItoRGB(image.getWidth(), image.getHeight(), inputBuffer, shortBuffer);
        byte[] output = new byte[shortBuffer.capacity()];
        shortBuffer.get(output);
        return output;
        //image.close();

    }
}
