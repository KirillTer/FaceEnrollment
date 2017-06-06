package com.intel.faceenrollment;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.media.Image;

import com.intel.irfaceauthenticator.wrapper.ImageContainer;
import com.intel.irfaceauthenticator.wrapper.ImageTypeEnum;
import com.intel.perc.cameras.realsense.DS5Utlils;
import com.intel.realsense.test.Y12IConverter;

import java.nio.ByteBuffer;

public class ImageContainerBuilder {
    ImageContainer Build (Image image)
    {
        ImageContainer imageContainer= null;
        imageContainer = new ImageContainer();
        imageContainer.width = image.getWidth();
        imageContainer.height = image.getHeight();
        if (image.getFormat() == DS5Utlils.Ds5ExtendedFormat.Y8I.getValue())
        {
            imageContainer.imageType = ImageTypeEnum.GrayScale8;
            DS5Utlils.ImageBufferPair pair = new DS5Utlils().splitY8Iimage(image);
            imageContainer.buffer = pair.getLeft();
        } else if (image.getFormat() == ImageFormat.JPEG){
            imageContainer.imageType = ImageTypeEnum.Rgb24;
            //read image bytes
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.capacity()];
            buffer.get(bytes);
            //create bitmap
            Bitmap bitmapImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);
            final int PIXEL_SIZE = 3;
            imageContainer.buffer = new byte[3 * imageContainer.height * imageContainer.width];
            for (int rowNb = 0; rowNb < imageContainer.height; ++rowNb) {
                for (int colNb = 0; colNb < imageContainer.width; ++colNb) {
                    int color = bitmapImage.getPixel(colNb, rowNb);
                    int pixelOffset = (rowNb* imageContainer.width + colNb) * PIXEL_SIZE;
                    imageContainer.buffer[pixelOffset]     = (byte)Color.red(color);
                    imageContainer.buffer[pixelOffset + 1] = (byte)Color.green(color);
                    imageContainer.buffer[pixelOffset + 2] = (byte)Color.blue(color);
                }
            }
        } else if (image.getFormat() == DS5Utlils.Ds5ExtendedFormat.Y12I.getValue())
        {
            return BuildFromYI12(image);
        }
        else {
            throw new RuntimeException("format not supported");
        }

        return imageContainer;
    }

    private ImageContainer BuildFromYI12(Image image)
    {
        int width = image.getWidth();
        int height = image.getHeight();
        ImageContainer imageContainer = new ImageContainer();
        imageContainer.width = width;
        imageContainer.height = height;

        imageContainer.imageType = ImageTypeEnum.IrInterlaced10_24_Bayer;
        ByteBuffer inputBuffer = image.getPlanes()[0].getBuffer();
        imageContainer.buffer = new byte [inputBuffer.capacity()];
        inputBuffer.get(imageContainer.buffer);

/*        imageContainer.buffer = new byte[imageContainer.width * imageContainer.height * 2];
        imageContainer.imageType = ImageTypeEnum.IR10_16;
        ByteBuffer inputBuffer = image.getPlanes()[0].getBuffer();
        ByteBuffer outputBuffer = ByteBuffer.wrap(imageContainer.buffer);*/

//        Y12IConverter converter = new Y12IConverter();
//        imageContainer.buffer = converter.createShortBufferFromY12Image(image);

//        imageContainer.buffer = new byte[inputBuffer.capacity()];
//        inputBuffer.get(imageContainer.buffer);
/*        final int DOUBLE_PIXEL_SIZE = 3; //two pixels one from left, one from right
        byte[] imageBytes = new byte[width * height * DOUBLE_PIXEL_SIZE];
        inputBuffer.get(imageBytes);
        for (int rowNb = 0; rowNb < height; ++rowNb)
        {
            for (int colNb = 0; colNb < width; ++colNb)
            {
                int leftByte  = imageBytes[DOUBLE_PIXEL_SIZE *(rowNb * width + colNb)] & 0xFF;
                int rightByte = imageBytes[DOUBLE_PIXEL_SIZE *(rowNb * width + colNb) + 1] & 0xF;
                int pixelValue = ((leftByte << 8) | rightByte) << 6;
                imageContainer.buffer[2 *(rowNb * width + colNb)]     = (byte)((pixelValue & 0xFF));
                imageContainer.buffer[2 *(rowNb * width + colNb) + 1] = (byte)((pixelValue >> 8) & 0xFF);
//                short currentPixeValue = inputBuffer.getShort(DOUBLE_PIXEL_SIZE *(rowNb * width + colNb));
//                currentPixeValue = (short) (currentPixeValue & 0xFFF);
                //currentPixeValue = (short) (currentPixeValue << 6);
//                outputBuffer.putShort((rowNb * width + colNb) * 2, currentPixeValue);
            }
        }*/

        return  imageContainer;
    }
}
