package arducam;

import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;
import com.sun.jna.ptr.ByteByReference;
import com.sun.jna.ptr.PointerByReference;

public interface ArduCamSDK extends Library {
	ArduCamSDK INSTANCE = (ArduCamSDK)Native.load("ArduCamLib", ArduCamSDK.class);
	
	@FieldOrder({ "u32CameraType", "u16Vid", "u32Width", "u32Height", "u8PixelBytes", "u8PixelBits", "u32I2cAddr", "u32Size", "usbType", "emI2cMode", "emImageFmtMode", "u32TransLvl" })
	public static class ArduCamCfg extends Structure {
		public static class ByReference extends ArduCamCfg implements Structure.ByReference { }
		
		public ArduCamCfg(){
			super();
		}
		public ArduCamCfg(Pointer p){
			super(p);
		}
		
		public int	u32CameraType;				// Camera Type
		public short  u16Vid;                     // Vendor ID for USB
		public int	u32Width;					// Image Width
		public int	u32Height;					// Image Height
		public byte	u8PixelBytes;
		public byte  u8PixelBits;
		public int  u32I2cAddr;
		public int  u32Size;
		public byte  usbType;
		public int  emI2cMode;
		public int  emImageFmtMode;			// image format mode 
		public int	u32TransLvl;
		
	}
	
	@FieldOrder({ "stImagePara", "u64Time", "pu8ImageData" })
	public static class ArduCamOutData extends Structure {
		public ArduCamOutData(Pointer p){
			super(p);
			autoRead();
		}
		
		public ArduCamCfg stImagePara;				
		public long u64Time;      
		public PointerByReference pu8ImageData;
		
	}

	int ArduCam_autoopen(Pointer useHandle, ArduCamCfg.ByReference useCfg);
	
	int ArduCam_scan( Object... args );
	int ArduCam_open( Pointer useHandle, ArduCamCfg.ByReference useCfg, int usbIdx );
	int ArduCam_close( int useHandle );
	
	int ArduCam_beginCaptureImage( int useHandle );
	int ArduCam_captureImage( int useHandle );
	int ArduCam_endCaptureImage( int useHandle );
	
	int ArduCam_availableImage( int useHandle );
//	int ArduCam_readImage( int useHandle, ArduCamOutData.ByReference pstFrameData );
	int ArduCam_readImage( int useHandle, PointerByReference pstFrameData );	
	
	int ArduCam_writeSensorReg( int useHandle, int regAddr, int val );
	
	int ArduCam_del( int useHandle );							
	int ArduCam_flush( int useHandle );

	int ArduCam_writeReg_16_16( int useHandle, int i2cAddr, int regAddr, int pval );
	int ArduCam_readReg_16_16( int useHandle, int i2cAddr, int regAddr, Pointer pval );
	
	int ArduCam_getUsbType(int useHandle, Pointer pu8DevUsbType, Pointer pu8InfUsbType);
	
	int ArduCam_setboardConfig( int useHandle, int u8Command, int u16Value, int u16Index, int u32BufSize, Pointer pu8Buf );

	int ArduCam_softTrigger(int useHandle);
	int ArduCam_isFrameReady(int useHandle);
	int ArduCam_getSingleFrame(int useHandle, Object pDump, int timeOut);
	int ArduCam_setMode(int useHandle, int mode);
}
