// ICameraHAL.aidl
package depth_camera;

/**
 * AIDL file for Binder API generating
 *
 */

interface ICameraHAL{
    int setCalibrationData(in byte[] calibData, boolean backupArea);
    int getCalibrationData(out byte[] calibData, boolean backupArea);
    int getCalibrationDataStatus(out int[] calibDataUpdated);
    int getCachedCalibrationData(out byte[] outData, int len);
    int setMaintenance(int cameraId, boolean enable);
    int getMaintenance(int cameraId);
}
