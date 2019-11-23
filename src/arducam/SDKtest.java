package arducam;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

import arducam.ArduCamSDK.ArduCamOutData;

public class SDKtest {
	static ArduCamSDK arduCamSDKlib;
	static boolean running = true;
	ArduCamSDK.ArduCamCfg.ByReference useCfg;
	
	public SDKtest() throws InterruptedException {
		byte[] arg1 = new byte[32];
		int answer;

		arduCamSDKlib = ArduCamSDK.INSTANCE;

		answer = arduCamSDKlib.ArduCam_scan(arg1);
		System.out.println("ArduCam_scan returned: "+Utils.intToHex(answer));
		System.out.println("arg1: "+Utils.bytesToHex( arg1 )+" : "+new String(arg1));
		System.out.println("...");
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		IntByReference useHandle = new IntByReference();
		int usbIdx = 0;
		useCfg = new ArduCamSDK.ArduCamCfg.ByReference();
		useCfg.u32CameraType = 0x4D091031;
		useCfg.u16Vid = 0x52CB;                 // Vendor ID for USB
		useCfg.u32Width = 1280;					// Image Width
		useCfg.u32Height = 964;					// Image Height
		useCfg.u8PixelBytes = 1;
		useCfg.u8PixelBits = 8;
		useCfg.u32I2cAddr = 0x20;
		useCfg.u32Size = 1280*964;
		useCfg.usbType = 2;
		useCfg.emI2cMode = 3;
		useCfg.emImageFmtMode = 0;			// image format mode 
		useCfg.u32TransLvl = 64;
		
		answer = arduCamSDKlib.ArduCam_autoopen(useHandle.getPointer(), useCfg);
		System.out.println("ArduCam_autoopen returned: "+Utils.intToHex(answer));
		System.out.println("useHandle.getValue(): "+Utils.intToHex( useHandle.getValue() ));
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("");
		
		IntByReference pu8DevUsbType = new IntByReference();
		IntByReference pu8InfUsbType = new IntByReference();
		answer = arduCamSDKlib.ArduCam_getUsbType(useHandle.getValue(), pu8DevUsbType.getPointer(), pu8InfUsbType.getPointer());
		System.out.println("ArduCam_getUsbType returned: "+Utils.intToHex(answer));
		System.out.println("ArduCam_getUsbType pu8DevUsbType: "+Utils.intToHex(pu8DevUsbType.getValue()));
		System.out.println("ArduCam_getUsbType pu8InfUsbType: "+Utils.intToHex(pu8InfUsbType.getValue()));
		
		System.out.println("");
		
		Pointer pu8Buf1 = new Memory(1 * Native.getNativeSize(Byte.TYPE));
		pu8Buf1.setByte(0, (byte) 0x85);
		answer  = arduCamSDKlib.ArduCam_setboardConfig(useHandle.getValue(), 0xD7, 0x4600, 0x0100, 1, pu8Buf1);
		pu8Buf1.setByte(0, (byte) 0x00);
		answer += arduCamSDKlib.ArduCam_setboardConfig(useHandle.getValue(), 0xD7, 0x4600, 0x0200, 1, pu8Buf1);
		pu8Buf1.setByte(0, (byte) 0xC0);
		answer += arduCamSDKlib.ArduCam_setboardConfig(useHandle.getValue(), 0xD7, 0x4600, 0x0300, 1, pu8Buf1);
		pu8Buf1.setByte(0, (byte) 0x40);
		answer += arduCamSDKlib.ArduCam_setboardConfig(useHandle.getValue(), 0xD7, 0x4600, 0x0300, 1, pu8Buf1);
		pu8Buf1.setByte(0, (byte) 0x00);
		answer += arduCamSDKlib.ArduCam_setboardConfig(useHandle.getValue(), 0xD7, 0x4600, 0x0400, 1, pu8Buf1);
		pu8Buf1.setByte(0, (byte) 0x00);
		answer += arduCamSDKlib.ArduCam_setboardConfig(useHandle.getValue(), 0xD7, 0x4600, 0x0A00, 1, pu8Buf1);
		Pointer pu8Buf3 = new Memory(3 * Native.getNativeSize(Byte.TYPE));
		pu8Buf3.setByte(0, (byte) 0x03);
		pu8Buf3.setByte(1, (byte) 0x04);
		pu8Buf3.setByte(2, (byte) 0x0C);
		answer += arduCamSDKlib.ArduCam_setboardConfig(useHandle.getValue(), 0xF6, 0x0000, 0x0000, 3, pu8Buf3);
		System.out.println("ArduCam_setboardConfig returned: "+Utils.intToHex(answer));
		
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("");
		
		int i2cAddr = 0x20;
		answer  = arduCamSDKlib.ArduCam_writeReg_16_16(useHandle.getValue(), i2cAddr, 0x3028, 0x0010);
		
		answer += arduCamSDKlib.ArduCam_writeReg_16_16(useHandle.getValue(), i2cAddr, 0x302E, 0x0001);
		answer += arduCamSDKlib.ArduCam_writeReg_16_16(useHandle.getValue(), i2cAddr, 0x3030, 0x0022);
		answer += arduCamSDKlib.ArduCam_writeReg_16_16(useHandle.getValue(), i2cAddr, 0x302C, 0x0001);
		answer += arduCamSDKlib.ArduCam_writeReg_16_16(useHandle.getValue(), i2cAddr, 0x302A, 0x0010);
		
		answer += arduCamSDKlib.ArduCam_writeReg_16_16(useHandle.getValue(), i2cAddr, 0x3032, 0x0000);
		answer += arduCamSDKlib.ArduCam_writeReg_16_16(useHandle.getValue(), i2cAddr, 0x30B0, 0x0080);
		
		answer += arduCamSDKlib.ArduCam_writeReg_16_16(useHandle.getValue(), i2cAddr, 0x301A, 0x10DC);
//		answer += arduCamSDKlib.ArduCam_writeReg_16_16(useHandle.getValue(), i2cAddr, 0x301A, 0x1990);
		answer += arduCamSDKlib.ArduCam_writeReg_16_16(useHandle.getValue(), i2cAddr, 0x300C, 0x0672);
		
		answer += arduCamSDKlib.ArduCam_writeReg_16_16(useHandle.getValue(), i2cAddr, 0x3002, 0x0000);
		answer += arduCamSDKlib.ArduCam_writeReg_16_16(useHandle.getValue(), i2cAddr, 0x3004, 0x0000);
		answer += arduCamSDKlib.ArduCam_writeReg_16_16(useHandle.getValue(), i2cAddr, 0x3006, 0x03BF);
		answer += arduCamSDKlib.ArduCam_writeReg_16_16(useHandle.getValue(), i2cAddr, 0x3008, 0x04FF);
		answer += arduCamSDKlib.ArduCam_writeReg_16_16(useHandle.getValue(), i2cAddr, 0x300A, 0x03DE);
		answer += arduCamSDKlib.ArduCam_writeReg_16_16(useHandle.getValue(), i2cAddr, 0x3012, 0xDEAD);
		answer += arduCamSDKlib.ArduCam_writeReg_16_16(useHandle.getValue(), i2cAddr, 0x3014, 0x00C0);
		answer += arduCamSDKlib.ArduCam_writeReg_16_16(useHandle.getValue(), i2cAddr, 0x30A6, 0x0001);
		
		answer += arduCamSDKlib.ArduCam_writeReg_16_16(useHandle.getValue(), i2cAddr, 0x308C, 0x0000);
		answer += arduCamSDKlib.ArduCam_writeReg_16_16(useHandle.getValue(), i2cAddr, 0x308A, 0x0000);
		answer += arduCamSDKlib.ArduCam_writeReg_16_16(useHandle.getValue(), i2cAddr, 0x3090, 0x03BF);
		answer += arduCamSDKlib.ArduCam_writeReg_16_16(useHandle.getValue(), i2cAddr, 0x308E, 0x04FF);
		answer += arduCamSDKlib.ArduCam_writeReg_16_16(useHandle.getValue(), i2cAddr, 0x30AA, 0x03DE);
		answer += arduCamSDKlib.ArduCam_writeReg_16_16(useHandle.getValue(), i2cAddr, 0x3016, 0x00FA);
		answer += arduCamSDKlib.ArduCam_writeReg_16_16(useHandle.getValue(), i2cAddr, 0x3018, 0x00C0);
		answer += arduCamSDKlib.ArduCam_writeReg_16_16(useHandle.getValue(), i2cAddr, 0x30A8, 0x0001);
		
		answer += arduCamSDKlib.ArduCam_writeReg_16_16(useHandle.getValue(), i2cAddr, 0x3040, 0x4000);
		answer += arduCamSDKlib.ArduCam_writeReg_16_16(useHandle.getValue(), i2cAddr, 0x3064, 0x1982);
		answer += arduCamSDKlib.ArduCam_writeReg_16_16(useHandle.getValue(), i2cAddr, 0x31C6, 0x8000);
		
//		answer += arduCamSDKlib.ArduCam_writeReg_16_16(useHandle.getValue(), i2cAddr, 0x3100, 0x0000);
//		answer += arduCamSDKlib.ArduCam_writeReg_16_16(useHandle.getValue(), i2cAddr, 0x305E, 0x00F0);
		
		answer += arduCamSDKlib.ArduCam_writeReg_16_16(useHandle.getValue(), i2cAddr, 0x3056, 30);
		answer += arduCamSDKlib.ArduCam_writeReg_16_16(useHandle.getValue(), i2cAddr, 0x3058, 42);
		answer += arduCamSDKlib.ArduCam_writeReg_16_16(useHandle.getValue(), i2cAddr, 0x305A, 42);
		answer += arduCamSDKlib.ArduCam_writeReg_16_16(useHandle.getValue(), i2cAddr, 0x305C, 30);
		
		System.out.println("ArduCam_writeReg_16_16 returned: "+Utils.intToHex(answer));
		
		int regAddr = 0x3012;
		IntByReference pval = new IntByReference();
		answer = arduCamSDKlib.ArduCam_readReg_16_16(useHandle.getValue(), i2cAddr, regAddr, pval.getPointer());
		System.out.println("ArduCam_readReg_16_16 returned: "+Utils.intToHex(answer));
		System.out.println("regAddr: "+Utils.intToHex(regAddr)+" pval.getValue(): "+Utils.intToHex( pval.getValue() ));
		
		//EXTERNAL_TRIGGER_MODE = 0x01
		//CONTINUOUS_MODE = 0x02
		answer = arduCamSDKlib.ArduCam_setMode(useHandle.getValue(), 0x02);
		System.out.println("ArduCam_setMode returned: "+Utils.intToHex(answer));
		
		System.out.println("..");
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		Thread captureImageThread = new Thread(new CaptureImageThread(useHandle));
		captureImageThread.start();
		
		Thread readImageThread = new Thread(new ReadImageThread(useHandle));
		readImageThread.start();
		
		System.out.println("Running.");
		try {
			Thread.sleep(8000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		running = false; 
		captureImageThread.join();
		readImageThread.join();
		
		System.out.println("Stopped.");
		
		answer = arduCamSDKlib.ArduCam_close(useHandle.getValue());
		System.out.println("ArduCam_close returned: "+Utils.intToHex(answer));
		System.out.println("useHandle.getValue(): "+Utils.intToHex( useHandle.getValue() ));
	}

	private class CaptureImageThread implements Runnable {
		private IntByReference useHandle;
		
		public CaptureImageThread(IntByReference useHandle) {
			this.useHandle = useHandle;
		}

		@Override
		public void run() {
			int answer = arduCamSDKlib.ArduCam_beginCaptureImage(useHandle.getValue());
			System.out.println("CaptureImageThread: ArduCam_beginCaptureImage returned: "+Utils.intToHex(answer));
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			while(running){
				answer = arduCamSDKlib.ArduCam_captureImage(useHandle.getValue());
				System.out.println("CaptureImageThread: ArduCam_captureImage returned: "+Utils.intToHex(answer));
				
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
			answer = arduCamSDKlib.ArduCam_endCaptureImage(useHandle.getValue());
			System.out.println("CaptureImageThread: ArduCam_endCaptureImage returned: "+Utils.intToHex(answer));
					
		}
		
		
	}
	
	private class ReadImageThread implements Runnable {
		private IntByReference useHandle;
		private ArduCamSDK.ArduCamOutData.ByReference arduCamOutData;
		private PointerByReference parduCamOutData;
		
		private byte[] byteDump = new byte[(1280*964*1)+100];
		private Pointer pu8BufDump = new Memory( ((1280*964*1)+100) * Native.getNativeSize(Byte.TYPE) );
		
		public ReadImageThread(IntByReference useHandle) {
			this.useHandle = useHandle;
			arduCamOutData = new ArduCamSDK.ArduCamOutData.ByReference(); 
			arduCamOutData.stImagePara = SDKtest.this.useCfg; 
			parduCamOutData = new PointerByReference(pu8BufDump);
		}

		@Override
		public void run() {
			int answer;
			
			while(running){

				answer = arduCamSDKlib.ArduCam_availableImage(useHandle.getValue());
				System.out.println("ReadImageThread: ArduCam_availableImage returned: "+Utils.intToHex(answer));

				if (answer >0){
					System.out.println("will ArduCam_getSingleFrame");
					answer = arduCamSDKlib.ArduCam_readImage(useHandle.getValue(), byteDump);
					System.out.println("ReadImageThread: ArduCam_readImage returned: "+Utils.intToHex(answer));
					System.out.println("ReadImageThread: 128 byteDump: "+Utils.bytesToHex( byteDump, 128 ));
				}
				

				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		
	}
	
	public static void main(String[] args) throws InterruptedException {
		new SDKtest();
	}
}
