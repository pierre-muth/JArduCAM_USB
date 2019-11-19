package arducam;

import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;

public class SDKtest {

	public static void main(String[] args) {
		byte[] arg1 = new byte[32];
		int answer;

		ArduCamSDK arduCamSDKlib = ArduCamSDK.INSTANCE;

		answer = arduCamSDKlib.ArduCam_scan(arg1);
		System.out.println("ArduCam_scan returned: "+Integer.toString(answer));
		System.out.println("arg1: "+Utils.bytesToHex( arg1 )+" : "+new String(arg1));
		System.out.println("");
		
		IntByReference useHandle = new IntByReference();
		int usbIdx = 0;
		final ArduCamSDK.ArduCamCfg.ByReference useCfg = new ArduCamSDK.ArduCamCfg.ByReference();
		useCfg.u32CameraType = 0x4D091031;
		useCfg.u16Vid = 0x52CB;                 // Vendor ID for USB
		useCfg.u32Width = 1280;					// Image Width
		useCfg.u32Height = 964;					// Image Height
		useCfg.u8PixelBytes = 2;
		useCfg.u8PixelBits = 12;
		useCfg.u32I2cAddr = 0x20;
		useCfg.u32Size = 1;
		useCfg.usbType = 2;
		useCfg.emI2cMode = 3;
		useCfg.emImageFmtMode = 0;			// image format mode 
		useCfg.u32TransLvl = 64;
		
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		answer = arduCamSDKlib.ArduCam_open(useHandle.getPointer(), useCfg, usbIdx);
		System.out.println("ArduCam_open returned: "+Utils.intToHex(answer));
		System.out.println("useHandle.getValue(): "+Utils.intToHex( useHandle.getValue() ));
		System.out.println("");
		
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		int i2cAddr = 0x20;
		int regAddr = 0x3012;
		
		answer = arduCamSDKlib.ArduCam_writeReg_16_16(useHandle.getValue(), i2cAddr, regAddr, 0x10);
		System.out.println("ArduCam_writeReg_16_16 returned: "+Utils.intToHex(answer));
		System.out.println("");
		
		//#define CONTINUOUS_MODE	0x02
		answer = arduCamSDKlib.ArduCam_setMode(useHandle.getValue(), 0x02);
		System.out.println("ArduCam_setMode returned: "+Utils.intToHex(answer));
		System.out.println("");
		
		IntByReference pval = new IntByReference();
		answer = arduCamSDKlib.ArduCam_readReg_16_16(useHandle.getValue(), i2cAddr, regAddr, pval.getPointer());
		System.out.println("ArduCam_readReg_16_16 returned: "+Utils.intToHex(answer));
		System.out.println("pval.getValue(): "+Utils.intToHex( pval.getValue() ));
		System.out.println("");
		
		answer = arduCamSDKlib.ArduCam_beginCaptureImage(useHandle.getValue());
		System.out.println("ArduCam_beginCaptureImage returned: "+Utils.intToHex(answer));
		System.out.println("");
		

		int i = 30;
		while (i > 0) {
			answer = arduCamSDKlib.ArduCam_captureImage(useHandle.getValue());
			System.out.println("ArduCam_captureImage returned: "+Utils.intToHex(answer));
			System.out.println("");
			
			answer = arduCamSDKlib.ArduCam_availableImage(useHandle.getValue());
			System.out.println(i +" ArduCam_availableImage returned: "+Utils.intToHex(answer));
			System.out.println("");
			
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			
			i--;
		}
		
		
		final ArduCamSDK.ArduCamOutData.ByReference frameData = new ArduCamSDK.ArduCamOutData.ByReference();
		answer = arduCamSDKlib.ArduCam_readImage(useHandle.getValue(), frameData.getPointer());
		System.out.println("ArduCam_readImage returned: "+Utils.intToHex(answer));
		System.out.println("");
		
		answer = arduCamSDKlib.ArduCam_endCaptureImage(useHandle.getValue());
		System.out.println("ArduCam_endCaptureImage returned: "+Utils.intToHex(answer));
		System.out.println("");
		
		answer = arduCamSDKlib.ArduCam_close(useHandle.getValue());
		System.out.println("ArduCam_close returned: "+Utils.intToHex(answer));
		System.out.println("useHandle.getValue(): "+Utils.intToHex( useHandle.getValue() ));
	}

}
