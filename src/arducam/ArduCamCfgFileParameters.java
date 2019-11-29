package arducam;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class ArduCamCfgFileParameters {
	public static final String CAMERA_PARAMETER_KEY =		"[camera parameter]";
	public static final String BOARD_USB2_PARAMETER_KEY =	"[board parameter][dev2]";
	public static final String REGISTER_PARAMETER_KEY =		"[register parameter]";
	
	public CameraParameter cameraParameters;
	public ArrayList<BoardParameter> boardUSB2Parameters;
	public ArrayList<RegisterParameter> registerParameters;
	
	public ArduCamCfgFileParameters(File cfgFile) throws IOException{
		String line;
		BufferedReader br = new BufferedReader( new FileReader(cfgFile) );
		Parameter node = Parameter.NONE;
		cameraParameters = new CameraParameter(); 
		boardUSB2Parameters = new ArrayList<>();
		registerParameters = new ArrayList<>();
		
		do {
			line = br.readLine();
			
			if (line == null) break;
			if (line.startsWith(";")) continue;
			if (line.isEmpty()) continue;
			if (line.compareTo(CAMERA_PARAMETER_KEY) == 0) {
				node = Parameter.CAMERA_PARAMETER;
				continue;
			}
			if (line.compareTo(BOARD_USB2_PARAMETER_KEY) == 0) {
				node = Parameter.BOARD_USB2_PARAMETER;
				continue;
			}
			if (line.compareTo( REGISTER_PARAMETER_KEY ) == 0) {
				node = Parameter.REGISTER_PARAMETER;
				continue;
			}
			if (line.startsWith("[")) {
				node = Parameter.NONE;
				continue;
			}
			
			line = line.replaceAll("\\s", "");
			
			switch (node) {
			case CAMERA_PARAMETER:
				if (line.contains(CameraParameter.CFG_MODE_KEY))
					cameraParameters.CFG_MODE = Integer.parseInt( line.split("=")[1] );
				if (line.contains(CameraParameter.TYPE_KEY))
					cameraParameters.TYPE = line.split("=")[1];
				if (line.contains(CameraParameter.SIZE_KEY)) {
					line = line.split("=")[1];
					cameraParameters.SIZE[0] = Integer.parseInt( line.split(",")[0] );
					cameraParameters.SIZE[1] = Integer.parseInt( line.split(",")[1] );
				}
				if (line.contains(CameraParameter.BIT_WIDTH_KEY))
					cameraParameters.BIT_WIDTH = Integer.parseInt( line.split("=")[1] );
				if (line.contains(CameraParameter.FORMAT_KEY)) {
					line = line.split("=")[1];
					cameraParameters.FORMAT[0] = Integer.parseInt( line.split(",")[0] );
					cameraParameters.FORMAT[1] = Integer.parseInt( line.split(",")[1] );
				}
				if (line.contains(CameraParameter.I2C_MODE_KEY))
					cameraParameters.I2C_MODE = Integer.parseInt( line.split("=")[1] );
				if (line.contains(CameraParameter.I2C_ADDR_KEY)){
					line = line.split("=")[1];
					cameraParameters.I2C_ADDR = Integer.parseInt( line.replace("0x", ""), 16 );
				}
				if (line.contains(CameraParameter.TRANS_LVL_KEY))
					cameraParameters.TRANS_LVL = Integer.parseInt( line.split("=")[1] );
				
				break;

			case BOARD_USB2_PARAMETER:
				line = line.replaceAll("VRCMD=", "");
				String[] pieces = line.split(",");
				BoardParameter bp = new BoardParameter();
				bp.P0 = Integer.parseInt( pieces[0].replace("0x", ""), 16 );
				bp.P1 = Integer.parseInt( pieces[1].replace("0x", ""), 16 );
				bp.P2 = Integer.parseInt( pieces[2].replace("0x", ""), 16 );
				bp.P3 = Integer.parseInt( pieces[3] );
				int[] buf = new int[bp.P3];
				for (int i = 0; i < buf.length; i++) {
					buf[i] = Integer.parseInt( pieces[i+4].replace("0x", ""), 16 );
				}
				bp.P4 = buf;
				boardUSB2Parameters.add(bp);
				
				break;
				
			case REGISTER_PARAMETER:
				int offset =  line.indexOf("//");
				if (offset != -1) line = line.substring(0, offset);
				
				if (line.contains(RegisterParameter.DELAY_KEY)) {
					RegisterParameter rp = new RegisterParameter();
					rp.type = RegisterParameter.DELAY;
					registerParameters.add(rp);
				}
				if (line.contains(RegisterParameter.REG_KEY)) {
					RegisterParameter rp = new RegisterParameter();
					rp.type = RegisterParameter.REG;
					line = line.split("=")[1];
					String[] bits = line.split(",");
					rp.register = Integer.parseInt( bits[0].replace("0x", ""), 16 );
					if (bits[1].contains("0x")) rp.value = Integer.parseInt( bits[1].replace("0x", ""), 16 );
					else rp.value = Integer.parseInt( bits[1] );
					registerParameters.add(rp);
				}
				
				break;
				
			case NONE:
				break;
			default:
				break;
			}
			
		} while (line != null);
		
		br.close();
	}
		
	public class CameraParameter{
		public static final String CFG_MODE_KEY = "CFG_MODE";
		public static final String TYPE_KEY = "TYPE";
		public static final String SIZE_KEY = "SIZE";
		public static final String BIT_WIDTH_KEY = "BIT_WIDTH";
		public static final String FORMAT_KEY = "FORMAT";
		public static final String I2C_MODE_KEY = "I2C_MODE";
		public static final String I2C_ADDR_KEY = "I2C_ADDR";
		public static final String TRANS_LVL_KEY = "TRANS_LVL";
		
		public int CFG_MODE = 1;
		public String TYPE  = "AR0134";
		public int[] SIZE = {1280, 964};  
		public int BIT_WIDTH = 8;
		public int[] FORMAT = {0, 2};
		public int I2C_MODE = 3;
		public int I2C_ADDR = 0x20;
		public int TRANS_LVL = 64;
	}

	public class BoardParameter{
		public int P0;
		public int P1;
		public int P2;
		public int P3;
		public int[] P4;
	}
	
	public class RegisterParameter{
		public static final String DELAY_KEY = "DELAY";
		public static final String REG_KEY = "REG";
		
		public static final int REG = 0;
		public static final int DELAY = 1;
		
		public int type;
		public int register;
		public int value;
	}
	
	public enum Parameter {
		CAMERA_PARAMETER,
		BOARD_USB2_PARAMETER,
		REGISTER_PARAMETER,
		NONE
	}
	
	public static void main(String[] args) {
		try {
			new ArduCamCfgFileParameters( new File( "AR0134_RAW_8b_1280x964_31fps.cfg" ) );
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

