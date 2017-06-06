package com.intel.perc.cameras;

public interface Camera {
    void takePicture();
    void startStreaming();
    void stopStreaming();
    void setListener(ImageAvailableListener listener);

}
