package com.intel.faceenrollment;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.intel.irfaceauthenticator.wrapper.RegisterUserResult;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

import com.intel.irfaceauthenticator.wrapper.AuthenticationStatus;
import com.intel.irfaceauthenticator.wrapper.FaceAuthenticatorDatabaseFactory;
import com.intel.irfaceauthenticator.wrapper.FaceAuthenticatorFactory;
import com.intel.irfaceauthenticator.wrapper.IFaceAuthenticator;
import com.intel.irfaceauthenticator.wrapper.IFaceAuthenticatorDatabase;
import com.intel.irfaceauthenticator.wrapper.RegisterUserResult;
import com.intel.irfaceauthenticator.wrapper.RegisterUserStatus;
import com.intel.irfaceauthenticator.wrapper.UnregisterUserStatus;


/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void faceAuthenticatorIntegrationTest() throws Exception {
/*        // Create database
        FaceAuthenticatorDatabaseFactory faceAuthenticatorDatabaseFactory = new FaceAuthenticatorDatabaseFactory();
        IFaceAuthenticatorDatabase faceAuthenticatorDatabase = faceAuthenticatorDatabaseFactory.create();

        // Create authenticator
        FaceAuthenticatorFactory faceAuthenticatorFactory = new FaceAuthenticatorFactory();

        Context testContext = InstrumentationRegistry.getTargetContext();
        IFaceAuthenticator faceAuthenticator = faceAuthenticatorFactory.create(testContext, RECOGNITION_THRESHOLD, LANDMARKS_THRESHOLD);

        // Register user
        RegisterUserResult registerStatus = faceAuthenticator.registerUser(faceAuthenticatorDatabase, IR_IMAGE_1);
        Assert.assertEquals(registerStatus.getStatus(), RegisterUserStatus.SUCCESS);

        // Save database
        faceAuthenticatorDatabase.saveDatabase(DATABASE_FILE_PATH);

        // Unregister user in first database
        UnregisterUserStatus unregisterUserStatus = faceAuthenticator.unregisterUser(faceAuthenticatorDatabase, registerStatus.getUserId());
        Assert.assertEquals(unregisterUserStatus, UnregisterUserStatus.UNREGISTER_USER_SUCCESS);

        // Try authenticating user in first database - failure expected
        AuthenticationStatus authenticateStatus = faceAuthenticator.authenticateUser(faceAuthenticatorDatabase, IR_IMAGE_2, registerStatus.getUserId());
        Assert.assertEquals(authenticateStatus, AuthenticationStatus.FAILED_ID_NOT_FOUND);

        // Create database from saved database - contains original registered user
        IFaceAuthenticatorDatabase backupDatabase = faceAuthenticatorDatabaseFactory.create(DATABASE_FILE_PATH);

        // try authenticating user in the backup database - success expected
        authenticateStatus = faceAuthenticator.authenticateUser(backupDatabase, IR_IMAGE_2, registerStatus.getUserId());
        Assert.assertEquals(authenticateStatus, AuthenticationStatus.ALLOWED);*/
    }

    private static final float RECOGNITION_THRESHOLD = 784.6340f;
    private static final float LANDMARKS_THRESHOLD = 0.15f;
    private static final String IR_IMAGE_1 = "/storage/emulated/0/Pictures/faceEnrollment/frame_12_ir.png";
    private static final String IR_IMAGE_2 = "/storage/emulated/0/Pictures/faceEnrollment/frame_180_ir.png";
    private static final String DATABASE_FILE_PATH = "/storage/emulated/0/Pictures/faceEnrollment/savedDb.db";
}
