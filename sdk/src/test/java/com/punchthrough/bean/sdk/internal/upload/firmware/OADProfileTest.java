package com.punchthrough.bean.sdk.internal.upload.firmware;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

import com.punchthrough.bean.sdk.BeanManager;
import com.punchthrough.bean.sdk.internal.ble.GattClient;
import com.punchthrough.bean.sdk.internal.device.DeviceProfile;
import com.punchthrough.bean.sdk.internal.exception.ImageParsingException;
import com.punchthrough.bean.sdk.internal.utility.Constants;
import com.punchthrough.bean.sdk.internal.utility.Convert;
import com.punchthrough.bean.sdk.message.BeanError;
import com.punchthrough.bean.sdk.message.Callback;
import com.punchthrough.bean.sdk.message.UploadProgress;
import com.punchthrough.bean.sdk.upload.FirmwareBundle;
import com.punchthrough.bean.sdk.upload.FirmwareImage;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.List;

import static com.punchthrough.bean.sdk.internal.utility.Convert.intArrayToByteArray;
import static junit.framework.TestCase.fail;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@RunWith(PowerMockRunner.class)
@PrepareForTest(BeanManager.class)
public class OADProfileTest {

    byte[] rawImageA = intArrayToByteArray(new int[]{
            0x2B, 0x65,                // CRC
            0xFF, 0xFF,                // CRC Shadow
            0x64, 0x00,                // Version
            0x00, 0x7C,                // Length
            0x41, 0x41, 0x41, 0x41,    // Image A (ASCII 41)
            0xFF, 0xFF, 0xFF, 0xFF     // Reserved
    });

    byte[] rawImageB = intArrayToByteArray(new int[]{
            0x2B, 0x65,                // CRC
            0xFF, 0xFF,                // CRC Shadow
            0x64, 0x00,                // Version
            0x00, 0x7C,                // Length
            0x42, 0x42, 0x42, 0x42,    // Image B (ASCII 42)
            0xFF, 0xFF, 0xFF, 0xFF     // Reserved
    });

    // Test state
    DeviceProfile.FirmwareVersionCallback fwVersionCallback;
    List<BeanError> testErrors = new ArrayList<>();

    // Mocks
    GattClient mockGattClient;
    BluetoothGattService mockOADService;
    BluetoothGattCharacteristic mockOADIdentify;
    BluetoothGattCharacteristic mockOADBlock;
    BluetoothGattDescriptor mockOADIdentifyDescriptor;
    BluetoothGattDescriptor mockOADBlockDescriptor;
    List<BluetoothGattService> services = new ArrayList<>();
    DeviceProfile mockDeviceProfile;
    BeanManager mockBeanManager;

    // Class under test
    OADProfile oadProfile;

    @Before
    public void setup() {

        // NOTE: It is important to mock out the dependency in the "correct" order. Typically,
        // lower-level objects such as Android builtins and Bluetooth stuff should be mocked first

        // Setup mock OAD Service, Characteristics and Descriptors
        mockOADService = mock(BluetoothGattService.class);
        mockOADIdentify = mock(BluetoothGattCharacteristic.class);
        mockOADBlock = mock(BluetoothGattCharacteristic.class);
        mockOADIdentifyDescriptor = mock(BluetoothGattDescriptor.class);
        mockOADBlockDescriptor = mock(BluetoothGattDescriptor.class);
        when(mockOADService.getCharacteristic(Constants.UUID_OAD_CHAR_IDENTIFY)).thenReturn(mockOADIdentify);
        when(mockOADService.getCharacteristic(Constants.UUID_OAD_CHAR_BLOCK)).thenReturn(mockOADBlock);
        when(mockOADIdentify.getDescriptor(Constants.UUID_CLIENT_CHAR_CONFIG)).thenReturn(mockOADIdentifyDescriptor);
        when(mockOADBlock.getDescriptor(Constants.UUID_CLIENT_CHAR_CONFIG)).thenReturn(mockOADBlockDescriptor);
        when(mockOADBlock.getUuid()).thenReturn(Constants.UUID_OAD_CHAR_BLOCK);
        when(mockOADIdentify.getUuid()).thenReturn(Constants.UUID_OAD_CHAR_IDENTIFY);

        // Setup mock Bean Manager
        mockBeanManager = mock(BeanManager.class);
        PowerMockito.mockStatic(BeanManager.class);
        when(BeanManager.getInstance()).thenReturn(mockBeanManager);

        // Setup mock Device Information Profile
        mockDeviceProfile = mock(DeviceProfile.class);
        doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                fwVersionCallback = (DeviceProfile.FirmwareVersionCallback) args[0];
                return null;
            }
        }).when(mockDeviceProfile).getFirmwareVersion(any(DeviceProfile.FirmwareVersionCallback.class));

        // Setup mock GattClient
        mockGattClient = mock(GattClient.class);
        when(mockGattClient.isConnected()).thenReturn(true);
        when(mockGattClient.getServices()).thenReturn(services);
        when(mockGattClient.getService(Constants.UUID_OAD_SERVICE)).thenReturn(mockOADService);
        when(mockGattClient.setCharacteristicNotification(mockOADIdentify, true)).thenReturn(true);
        when(mockGattClient.setCharacteristicNotification(mockOADBlock, true)).thenReturn(true);
        when(mockGattClient.getDeviceProfile()).thenReturn(mockDeviceProfile);

        // Setup class under test - OADProfile
        oadProfile = new OADProfile(mockGattClient);
        oadProfile.onProfileReady();
        oadProfile.onBeanConnected();
    }

    private FirmwareBundle buildBundle() throws ImageParsingException {
        FirmwareImage imageA = new FirmwareImage(rawImageA, "12345_imageA.bin");
        FirmwareImage imageB = new FirmwareImage(rawImageB, "12345_imageB.bin");
        List<FirmwareImage> images = new ArrayList<>();
        images.add(imageA);
        images.add(imageB);
        return new FirmwareBundle(images);
    }

    private void requestBlock(int blkNo) {
        when(mockOADBlock.getValue()).thenReturn(Convert.intToTwoBytes(blkNo, Constants.CC2540_BYTE_ORDER));
        oadProfile.onCharacteristicChanged(mockGattClient, mockOADBlock);
    }

    private void reconnect() {
        oadProfile.onBeanConnectionFailed();
    }

    private Callback<UploadProgress> onProgress = new Callback<UploadProgress>() {
        @Override
        public void onResult(UploadProgress result) {

        }
    };

    private Runnable onComplete = mock(Runnable.class);

    private Callback<BeanError> onError = new Callback<BeanError>() {
        @Override
        public void onResult(BeanError result) {
            testErrors.add(result);
        }
    };

    private void assertNoErrors() {
        for (BeanError e : testErrors) {
            fail(e.toString());
        }
    }

    private void assertState(OADState state) {
        assertThat(state).isEqualTo(oadProfile.getState());
    }

    @Test
    public void testNoUpdateNeeded() throws ImageParsingException {
        oadProfile.programWithFirmware(buildBundle(), onProgress, onComplete, onError);
        assertState(OADState.CHECKING_FW_VERSION);
        fwVersionCallback.onComplete("12345");  // Same as bundle version
        assertState(OADState.INACTIVE);
        assertNoErrors();
        verify(onComplete).run();
    }

    @Test
    public void testUpdateNeeded() throws ImageParsingException {

        // Start test
        oadProfile.programWithFirmware(buildBundle(), onProgress, onComplete, onError);
        assertState(OADState.CHECKING_FW_VERSION);
        fwVersionCallback.onComplete("1234"); // Less than bundle version
        assertState(OADState.OFFERING_IMAGES);
        requestBlock(0);
        assertState(OADState.RECONNECTING);
        reconnect();
        verify(mockBeanManager).startDiscovery();
        assertNoErrors();
    }
}
