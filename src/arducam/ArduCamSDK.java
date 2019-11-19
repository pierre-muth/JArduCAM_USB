package arducam;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;
import com.sun.jna.ptr.IntByReference;

public interface ArduCamSDK extends Library {
	ArduCamSDK INSTANCE = (ArduCamSDK)Native.load("ArduCamLib", ArduCamSDK.class);
	
	@FieldOrder({ "u32CameraType", "u16Vid", "u32Width", "u32Height", "u8PixelBytes", "u8PixelBits", "u32I2cAddr", "u32Size", "usbType", "emI2cMode", "emImageFmtMode", "u32TransLvl" })
	public static class ArduCamCfg extends Structure {
		public static class ByReference extends ArduCamCfg implements Structure.ByReference { }
		
		public int	u32CameraType;				// Camera Type
		public int  u16Vid;                     // Vendor ID for USB
		public int	u32Width;					// Image Width
		public int	u32Height;					// Image Height
		public int	u8PixelBytes;
		public int  u8PixelBits;
		public int  u32I2cAddr;
		public int  u32Size;
		public int  usbType;
		public int  emI2cMode;
		public int  emImageFmtMode;			// image format mode 
		public int	u32TransLvl;
		
	}
	
	@FieldOrder({ "stImagePara", "u64Time", "pu8ImageData" })
	public static class ArduCamOutData extends Structure {
		public static class ByReference extends ArduCamOutData implements Structure.ByReference { }
		
		public ArduCamCfg stImagePara;				
		public int u64Time;      
		public Pointer pu8ImageData;      
		
	}

	int ArduCam_autoopen(IntByReference useHandle, ArduCamCfg.ByReference useCfg);
	
	int ArduCam_scan( Object... args );
	int ArduCam_open( Pointer useHandle, ArduCamCfg.ByReference useCfg, int usbIdx );
	int ArduCam_close( int useHandle );
	
	int ArduCam_beginCaptureImage( int useHandle );
	int ArduCam_captureImage( int useHandle );
	int ArduCam_endCaptureImage( int useHandle );
	
	int ArduCam_availableImage( int useHandle );
	int ArduCam_readImage( int useHandle, Pointer pstFrameData );	
	
	int ArduCam_del( int useHandle );							
	int ArduCam_flush( int useHandle );

	int ArduCam_writeReg_16_16( int useHandle, int i2cAddr, int regAddr, int pval );
	int ArduCam_readReg_16_16( int useHandle, int i2cAddr, int regAddr, Pointer pval );
	
	int ArduCam_setboardConfig( int useHandle, int u8Command, int u16Value, int u16Index, int u32BufSize, Pointer pu8Buf );

	int ArduCam_isFrameReady(int useHandle);
	int ArduCam_getSingleFrame(int useHandle, Pointer pstFrameData, int time_out);
	int ArduCam_setMode(int useHandle, int mode);
}
