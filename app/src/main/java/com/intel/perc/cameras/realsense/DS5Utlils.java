package com.intel.perc.cameras.realsense;

import android.graphics.Bitmap;
import android.media.Image;
import android.util.Log;
import android.util.Size;

import java.nio.ByteBuffer;

public class DS5Utlils {
    public enum Ds5ExtendedFormat
    {
        Y8I(0x121),
        Y12I(0x122),
        DEPTH(0x44363159);

        public int getValue() {
            return mValue;
        }
        Ds5ExtendedFormat(int value)
        {
            mValue = value;
        }
        private  int mValue;
    }

//    private native convert(byte[] buffer);

    public  static class ImageBufferPair
    {
        public ImageBufferPair(byte[] mLeft, byte[] mRight) {
            this.mLeft = mLeft;
            this.mRight = mRight;
        }

        private static final String TAG = "DS5Utils";

        public byte[] getLeft() {
            return mLeft;
        }

        public void setLeft(byte[] left) {
            this.mLeft = left;
        }

        private byte [] mLeft;

        public byte[] getRight() {
            return mRight;
        }

        public void setRight(byte[] right) {
            this.mRight = right;
        }

        private byte [] mRight;
    }

    public ImageBufferPair splitY8Iimage(Image image)
    {
        if (image.getFormat() != Ds5ExtendedFormat.Y8I.getValue())
        {
            throw new RuntimeException("image format should be Y8I");
        }

        ByteBuffer ds5buffer = image.getPlanes()[0].getBuffer();
        ds5buffer.rewind();
        byte[] y8iInterlaced = new byte[ds5buffer.capacity()];
        byte[] leftImageBytes = new byte [y8iInterlaced.length/2];
        byte[] rightImageBytes = new byte [y8iInterlaced.length/2];
        ds5buffer.get(y8iInterlaced);
        int absCount = 0;

        long timeBegin = System.currentTimeMillis();
        for (int i = 0; i < leftImageBytes.length; ++i)
        {
            leftImageBytes[i] = y8iInterlaced[absCount++];
            rightImageBytes[i] = y8iInterlaced[absCount++];
        }
        long timeEnd = System.currentTimeMillis();
//        Log.d("===stas", "deinterleave processing time:  " + Double.toString((timeEnd - timeBegin)));
        long time = timeEnd - timeBegin;
        return new ImageBufferPair(leftImageBytes, rightImageBytes);
    }

    Bitmap createBitmapFromFromY8IImage(Image image) {
        if (image.getFormat() != Ds5ExtendedFormat.Y8I.getValue())
        {
            throw new RuntimeException("createBitmapFromFromY8IImage works only with Y8I format");
        }
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        buffer.rewind();
        byte[] arr = new byte[buffer.remaining()];
        buffer.get(arr);
        return createBitmapFromFromY8IBuffer(new Size(image.getWidth(), image.getHeight()), arr);
    }

    public Bitmap createBitmapFromFromY8IBuffer(Size imageSize, byte[] interleavedBuffer) {
        if (imageSize.getHeight() * imageSize.getWidth() * 2 != interleavedBuffer.length)
        {
            throw new RuntimeException("bad buffer size");
        }

        int[] rgb = new int[imageSize.getWidth() * imageSize.getHeight()];

        long timeBegin = System.currentTimeMillis();
        deinterleave(rgb, interleavedBuffer, imageSize.getWidth(), imageSize.getHeight());
        long timeEnd = System.currentTimeMillis();
//        Log.d("===stas", "deinterleave processing time:  " + Double.toString((timeEnd - timeBegin)));
        Bitmap bitmap = Bitmap.createBitmap(rgb, imageSize.getWidth(), imageSize.getHeight(), Bitmap.Config.ARGB_8888);
        return bitmap;
    }


    public void deinterleave(int[] rgb, byte[] input, int width, int height) {
        int y;
        int offset = 0;
        int offset2 = 0;
        for (int i = 0; i < height; i++)
            for (int j = 0; j < width; j++) {
                y = input[offset++];
                if (y < 0)
                    y = 0;
                if (y > 255)
                    y = 255;
                offset++;
                rgb[offset2] = (0xff000000) | (y << 16) | (y << 8) | y;
                offset2++;

            }
    }

    public byte findMin(byte [] buffer)
    {
        byte min = (byte)0xFF;
        for (int i = 0; i < buffer.length; ++i)
        {
            if (buffer[i] < min)
            {
                min = buffer[i];
            }
        }
        return min;
    }

    public byte findMax(byte [] buffer)
    {
        byte max = 0;
        for (int i = 0; i < buffer.length; ++i)
        {
            if (buffer[i] > max)
            {
                max = buffer[i];
            }
        }
        return max;
    }
}
