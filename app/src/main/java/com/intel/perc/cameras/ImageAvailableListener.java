package com.intel.perc.cameras;

import android.media.Image;

public interface ImageAvailableListener {
    void onImageAvailable(ImageSource source, CaptureType captureType, Image rawImage);
}
