package arducam;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

import arducam.ArduCamSDK.ArduCamCfg;
import arducam.ArduCamSDK.ArduCamOutData;
import sun.awt.image.ShortBandedRaster;
import sun.security.provider.VerificationProvider;

public class StreamingDemo extends JPanel implements ActionListener {
	// constants
	private static final String OPEN_FILE = "Open CFG file";
	private static final String START_CAMERA = "Setup and Start camera";
	private static final String STOP_CAMERA = "Stop camera";
	private static final String REG_SET = "Set";
	private static final String REG_GET = "Get";
	private static final String SLIDER_CHANGED = "slider";
	private static final String DECODE_CHANGED = "decoding";
	private static final String DECODING_BW = "decode as raw B&W";
	private static final String DECODING_SUBPIX = "decode by sub-pixels";
	private static final String DECODING_RAW = "decode as bayer RAW";
	private static final String[] DECODING_OPT = {DECODING_BW, DECODING_SUBPIX, DECODING_RAW};
	// GUI elements
	private JLabel jlImage;
	private JLabel jlInfo;
	private JFileChooser fileChooser;
	private JTextField jtfRegAddr;
	private JTextField jtfRegValue;
	private JTextField jlRegValueMin;
	private JTextField jlRegValueMax;
	private JSlider jsRegValueSlider;
	private JComboBox<String> jcbDecoding;
	
	private ArduCamCfgFileParameters cfgParameters;
	private static ArduCamSDK arduCamSDKlib;
	private static IntByReference useHandle;
	private static Thread captureImageThread;
	private static Thread readImageThread;
	private static AtomicBoolean running = new AtomicBoolean(false);
	private static DecodingType decodingType = DecodingType.DECODING_BW;
	private ArduCamSDK.ArduCamCfg.ByReference useCfg;
	
	// will change during config.
	public static int HEIGHT = 964;
	public static int WIDTH  = 1280;
	private static int[] pixList = new int[3];
	
	public StreamingDemo(){
		initGUI();		
	}
	
	private void setupAndStartCamera() throws InterruptedException{
		if (cfgParameters == null) {
			displayInfo("No parameters loaded.");
			return;
		}
		if (running.get()) {
			displayInfo("already running.");
			return;
		}
		int answer;
		
		WIDTH = cfgParameters.cameraParameters.SIZE[0];
		HEIGHT = cfgParameters.cameraParameters.SIZE[1];
		jlImage.setPreferredSize(new Dimension(WIDTH, HEIGHT));
		pixList = new int[WIDTH*HEIGHT*3];
		
		arduCamSDKlib = ArduCamSDK.INSTANCE;
		
		useHandle = new IntByReference();
		useCfg = new ArduCamSDK.ArduCamCfg.ByReference();
		useCfg.u32CameraType = 0xAAAAAAAA;
		useCfg.u16Vid = 0x52CB;                 // Vendor ID for USB
		useCfg.u32Width = cfgParameters.cameraParameters.SIZE[0];					// Image Width
		useCfg.u32Height = cfgParameters.cameraParameters.SIZE[1];					// Image Height
		useCfg.u8PixelBytes = (byte) (cfgParameters.cameraParameters.BIT_WIDTH > 8 ? 2 : 1) ;
		useCfg.u8PixelBits = (byte) cfgParameters.cameraParameters.BIT_WIDTH;
		useCfg.u32I2cAddr = cfgParameters.cameraParameters.I2C_ADDR;
		useCfg.u32Size = useCfg.u32Width * useCfg.u32Height;
		useCfg.usbType = 2;
		useCfg.emI2cMode = cfgParameters.cameraParameters.I2C_MODE;
		useCfg.emImageFmtMode = cfgParameters.cameraParameters.FORMAT[0];			// image format mode 
		useCfg.u32TransLvl = cfgParameters.cameraParameters.TRANS_LVL;
		

		answer = arduCamSDKlib.ArduCam_autoopen(useHandle.getPointer(), useCfg);
		displayInfo("ArduCam_autoopen returned: "+Utils.intToHex(answer));
		if (answer != 0) return;

		Thread.sleep(2000);
		
		IntByReference pu8DevUsbType = new IntByReference();
		IntByReference pu8InfUsbType = new IntByReference();
		answer = arduCamSDKlib.ArduCam_getUsbType(useHandle.getValue(), pu8DevUsbType.getPointer(), pu8InfUsbType.getPointer());
		System.out.println(new Date().getTime());
		System.out.println("ArduCam_getUsbType returned: "+Utils.intToHex(answer));
		System.out.println("ArduCam_getUsbType pu8DevUsbType: "+Utils.intToHex(pu8DevUsbType.getValue()));
		System.out.println("ArduCam_getUsbType pu8InfUsbType: "+Utils.intToHex(pu8InfUsbType.getValue()));
		
		Pointer pu8Buf;
		for (ArduCamCfgFileParameters.BoardParameter boardUSB2Parameter : cfgParameters.boardUSB2Parameters) {
			pu8Buf = new Memory(boardUSB2Parameter.P3 * Native.getNativeSize(Byte.TYPE));
			for (int i = 0; i < boardUSB2Parameter.P3; i++) {
				pu8Buf.setByte(i, (byte) boardUSB2Parameter.P4[i]);
			}
			answer  = arduCamSDKlib.ArduCam_setboardConfig(useHandle.getValue(), boardUSB2Parameter.P0, boardUSB2Parameter.P1, boardUSB2Parameter.P2, boardUSB2Parameter.P3, pu8Buf);
			displayInfo( "ArduCam_setboardConfig returned: "+Utils.intToHex(answer));
			if (answer != 0) return;
		}
		
		Thread.sleep(1000);
		
		int i2cAddr = cfgParameters.cameraParameters.I2C_ADDR;
		for (ArduCamCfgFileParameters.RegisterParameter registerParameter : cfgParameters.registerParameters) {
			if (registerParameter.type == ArduCamCfgFileParameters.RegisterParameter.DELAY) {
				Thread.sleep(100);
			} else {
				answer  = arduCamSDKlib.ArduCam_writeReg_16_16(useHandle.getValue(), i2cAddr, registerParameter.register, registerParameter.value);
				displayInfo( "ArduCam_writeReg_16_16 returned: "+Utils.intToHex(answer));
			}
		}

		//EXTERNAL_TRIGGER_MODE = 0x01
		//CONTINUOUS_MODE = 0x02
		answer = arduCamSDKlib.ArduCam_setMode(useHandle.getValue(), 0x02);
		displayInfo( "ArduCam_setMode returned: "+Utils.intToHex(answer));
		if (answer != 0) return;
		
		running.set(true);
		Thread.sleep(500);
		
		captureImageThread = new Thread(new CaptureImageThread(useHandle));
		captureImageThread.start();

		readImageThread = new Thread(new ReadImageThread(useHandle));
		readImageThread.start();

		displayInfo( "Running.");
		
	}
	
	private void stopCamera(){
		if (!running.get()) return;

		running.set(false); 
		try {
			captureImageThread.join();
			readImageThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		displayInfo("Stopping");

		int answer = arduCamSDKlib.ArduCam_close(useHandle.getValue());
		displayInfo("ArduCam_close returned: "+Utils.intToHex(answer));
	}
	
	private synchronized int setRegister(int register, int value){
		if (cfgParameters == null) return -1;
		int answer = 0;
		int i2cAddr = cfgParameters.cameraParameters.I2C_ADDR;
		answer  = arduCamSDKlib.ArduCam_writeReg_16_16(useHandle.getValue(), i2cAddr, register, value);
		System.out.println("setRegister i2cAddr: "+i2cAddr+", register: "+register+", value: "+value+", answer: "+answer);
		return answer;
	}
	
	private synchronized int getRegister(int register){
		int answer = 0;
		int i2cAddr = cfgParameters.cameraParameters.I2C_ADDR;
		IntByReference pval = new IntByReference();
		answer = arduCamSDKlib.ArduCam_readReg_16_16(useHandle.getValue(), i2cAddr, register, pval.getPointer());
		if (answer != 0){
			return answer;
		} else {
			return pval.getValue();
		}
	}
	
	// Swing stuff
	private void initGUI() {
		setLayout(new BorderLayout());
		JPanel jpCameraControl = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JButton jbOpen = new JButton(OPEN_FILE);
		jbOpen.setActionCommand(OPEN_FILE);
		jbOpen.addActionListener(this);
		JButton jbStart = new JButton(START_CAMERA);
		jbStart.setActionCommand(START_CAMERA);
		jbStart.addActionListener(this);
		JButton jbStop = new JButton(STOP_CAMERA);
		jbStop.setActionCommand(STOP_CAMERA);
		jbStop.addActionListener(this);
		jcbDecoding = new JComboBox<>(DECODING_OPT);
		jcbDecoding.setActionCommand(DECODE_CHANGED);
		jcbDecoding.addActionListener(this);
		
		jpCameraControl.add(jbOpen);
		jpCameraControl.add(jbStart);
		jpCameraControl.add(jbStop);
		jpCameraControl.add(jcbDecoding);
		
		jtfRegAddr = new JTextField("0x3012");
		jtfRegAddr.setPreferredSize(new Dimension(60,25));
		jtfRegValue = new JTextField("200");
		jtfRegValue.setPreferredSize(new Dimension(60,25));
		JButton jbSetRegister = new JButton(REG_SET);
		jbSetRegister.setMargin(new Insets(2, 2, 2, 2));
		jbSetRegister.setPreferredSize(new Dimension(40,25));
		jbSetRegister.setActionCommand(REG_SET);
		jbSetRegister.addActionListener(this);
		JButton jbGetRegister = new JButton(REG_GET);
		jbGetRegister.setMargin(new Insets(2, 2, 2, 2));
		jbGetRegister.setPreferredSize(new Dimension(40,25));
		jbGetRegister.setActionCommand(REG_GET);
		jbGetRegister.addActionListener(this);
		jlRegValueMin = new JTextField("1");
		jlRegValueMin.setPreferredSize(new Dimension(60,25));
		jlRegValueMax = new JTextField("5000");
		jlRegValueMax.setPreferredSize(new Dimension(60,25));
		jsRegValueSlider = new JSlider(1, 5000);
		jsRegValueSlider.setPreferredSize(new Dimension(100,25));
		jsRegValueSlider.setValue(200);
		jsRegValueSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				jsRegValueSlider.setMinimum(Integer.parseInt(jlRegValueMin.getText()));
				jsRegValueSlider.setMaximum(Integer.parseInt(jlRegValueMax.getText()));
				actionPerformed(new ActionEvent(this, 0, SLIDER_CHANGED) );
			}
		});
		JPanel jpRegister = new JPanel(new FlowLayout(FlowLayout.LEADING));
		jpRegister.add(jtfRegAddr);
		jpRegister.add(jtfRegValue);
		jpRegister.add(jbSetRegister);
		jpRegister.add(jbGetRegister);
		jpRegister.add(jlRegValueMin);
		jpRegister.add(jsRegValueSlider);
		jpRegister.add(jlRegValueMax);
		
		JPanel jpTop = new JPanel(new GridLayout(2, 1));
		jpTop.add(jpCameraControl);
		jpTop.add(jpRegister);
		
		jlImage = new JLabel();
		jlImage.setOpaque(true);
		JScrollPane jspCenter = new JScrollPane(jlImage);
		jspCenter.setPreferredSize(new Dimension(640, 640));
		jlInfo = new JLabel("info here");
		add(jpTop, BorderLayout.NORTH);
		add(jspCenter, BorderLayout.CENTER);
		add(jlInfo, BorderLayout.SOUTH);
		
		fileChooser = new JFileChooser();
	    FileNameExtensionFilter filter = new FileNameExtensionFilter(
	        "CFG file", "cfg");
	    fileChooser.setFileFilter(filter);
	}

	private void displayInfo(String message){
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				jlInfo.setText(message);
			}
		});
	}
	
	// GUI actions
	@Override
	public void actionPerformed(ActionEvent actionEvent) {
		if (actionEvent.getActionCommand().equals(OPEN_FILE)){
			int returnVal = fileChooser.showOpenDialog(this);
			if(returnVal == JFileChooser.APPROVE_OPTION) {
				
				displayInfo("Openning " + fileChooser.getSelectedFile().getName());

				try {
					cfgParameters = new ArduCamCfgFileParameters(fileChooser.getSelectedFile());
				} catch (IOException e) {
					displayInfo( e.getMessage());
				}
				
				displayInfo("Parameters loaded.");
			}

		}

		if (actionEvent.getActionCommand().equals(START_CAMERA)){
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						setupAndStartCamera();
					} catch (InterruptedException e) {
						displayInfo( e.getMessage());
					}
				}
			}).start();
		}

		if (actionEvent.getActionCommand().equals(STOP_CAMERA)){
			stopCamera();
		}
		
		if (actionEvent.getActionCommand().equals(REG_SET)){
			int register = 0;
			int value = 0;
			String toParse =  jtfRegAddr.getText();
			if (toParse.contains("0x")) {
				register = Integer.parseInt(toParse.replace("0x", ""), 16);
			} else {
				register = Integer.parseInt(toParse);
			}
			toParse = jtfRegValue.getText();
			if (toParse.contains("0x")) {
				value = Integer.parseInt(toParse.replace("0x", ""), 16);
			} else {
				value = Integer.parseInt(toParse);
			}
			System.out.println("Reg set: "+register+", "+value);
			
			setRegister(register, value);
		}
		
		if (actionEvent.getActionCommand().equals(REG_GET)){
			int register = 0;
			int value = 0;
			String toParse =  jtfRegAddr.getText();
			if (toParse.contains("0x")) {
				register = Integer.parseInt(toParse.replace("0x", ""), 16);
			} else {
				register = Integer.parseInt(toParse);
			}
			value = getRegister(register);
			jtfRegValue.setText(String.valueOf(value));
		}
		
		if (actionEvent.getActionCommand().equals(SLIDER_CHANGED)){
			jtfRegValue.setText(String.valueOf(jsRegValueSlider.getValue()));
			
			int register = 0;
			int value = jsRegValueSlider.getValue();
			String toParse =  jtfRegAddr.getText();
			if (toParse.contains("0x")) {
				register = Integer.parseInt(toParse.replace("0x", ""), 16);
			} else {
				register = Integer.parseInt(toParse);
			}
			
			setRegister(register, value);
		}
		
		if (actionEvent.getActionCommand().equals(DECODE_CHANGED)){
			if ( ((String)jcbDecoding.getSelectedItem()).equals(DECODING_BW) ) {
				decodingType = DecodingType.DECODING_BW;
			}
			if ( ((String)jcbDecoding.getSelectedItem()).equals(DECODING_SUBPIX) ) {
				decodingType = DecodingType.DECODING_SUBPIX;
			}
			if ( ((String)jcbDecoding.getSelectedItem()).equals(DECODING_RAW) ) {
				decodingType = DecodingType.DECODING_RAW;
			}
			for (int i = 0; i < pixList.length; i++) {
				pixList[i] = 0;
			}
		}
		
	}
	
	public static void main(String[] args) {
		//Create and set up the window.
		JFrame frame = new JFrame("ArduCam Streaming Demo");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().add(new StreamingDemo(), BorderLayout.CENTER );
		//Display the window.
		frame.pack();
		frame.setVisible(true);
	}
	
	/**
	 * Thread reading and decode the image data.
	 */
	private class ReadImageThread implements Runnable {
		private IntByReference useHandle;
		private PointerByReference pstFrameData = new PointerByReference();
		private long time = 0;

		public ReadImageThread(IntByReference useHandle) {
			this.useHandle = useHandle;
		}

		@Override
		public void run() {
			int answer;
			int restTime = 0;

			while(running.get()){
				answer = arduCamSDKlib.ArduCam_availableImage(useHandle.getValue());
				if (answer >0){

					answer = arduCamSDKlib.ArduCam_readImage(useHandle.getValue(), pstFrameData);
					
					ArduCamOutData arduCamOutData = new ArduCamOutData(pstFrameData.getValue());
					ArduCamCfg arduCamCfg = arduCamOutData.stImagePara;
					
					byte[] imageData = arduCamOutData.pu8ImageData.getPointer().getByteArray(0, arduCamCfg.u32Size);
					int imageUnsingedByte;
					
					if (cfgParameters.cameraParameters.BIT_WIDTH == 8){
						// 8 bit per pixel
						// pixels are b&w and not color decoded
						for (int i = 0; i < imageData.length; i++) {
							imageUnsingedByte = Byte.toUnsignedInt(imageData[i]);
							pixList[i*3] = imageUnsingedByte;
							pixList[1+i*3] = imageUnsingedByte;
							pixList[2+i*3] = imageUnsingedByte;
						}
						
						
					} else {
						// 12 bit et pixel
						// pixels are b&w and not color decoded
						ByteBuffer bb = ByteBuffer.wrap(imageData);
						bb.order(ByteOrder.LITTLE_ENDIAN);
						
						for (int i = 0; i < imageData.length/2; i++) {
							pixList[i*3] = bb.getShort();
							pixList[i*3] /= 16;
							pixList[i*3+1] = pixList[i*3];
							pixList[i*3+2] = pixList[i*3];
						}
						
					}
					
					// pixels are not color decoded but in the the color channel corresponging to the bayer matrix
					if (decodingType == DecodingType.DECODING_RAW) {
						for (int i = 0; i < (WIDTH*HEIGHT); i++) {
							imageUnsingedByte = Byte.toUnsignedInt(imageData[i]);
							// blue
							if( (i/WIDTH)%2 == 0 && i%2==1 ) {
								pixList[1+i*3] = 0;
								pixList[2+i*3] = 0;
							}
							// green
							if( (i/WIDTH)%2 == 0 && i%2==0 ) {
								pixList[0+i*3] = 0;
								pixList[2+i*3] = 0;
							}
							if( (i/WIDTH)%2 == 1 && i%2==1 ) {
								pixList[0+i*3] = 0;
								pixList[2+i*3] = 0;
							}
							// red
							if( (i/WIDTH)%2 == 1 && i%2==0 ){
								pixList[0+i*3] = 0;
								pixList[1+i*3] = 0;
							}
						}
					}
					
					// simple color decoding: creating sub-pixel (divides resolution by 4)
					if (decodingType == DecodingType.DECODING_SUBPIX) {
						int i;
						for (int y = 0; y < HEIGHT/2; y++) {
							for (int x = 0; x < WIDTH/2; x++) {
								i = x+y*WIDTH;
								pixList[i*3+1] = pixList[3*((x*2)+(y*2*WIDTH))]; 		//G
								pixList[i*3  ] = pixList[3*((x*2)+(y*2*WIDTH)+1)];		//R
								pixList[i*3+2] = pixList[3*((x*2)+(y*2*WIDTH)+WIDTH)];	//B
								
								
							}
						}
					}
					
					// refresh the displayed image
					javax.swing.SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							BufferedImage bufferedImage = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_3BYTE_BGR);
							WritableRaster raster;
							raster = bufferedImage.getRaster();
							raster.setPixels(0, 0, WIDTH, HEIGHT, pixList);
							bufferedImage.setData(raster);
							
							jlImage.setIcon(new ImageIcon(bufferedImage));
						}
					});

					answer = arduCamSDKlib.ArduCam_del(useHandle.getValue());
					
					displayInfo(1000/( new Date().getTime() - time ) +" fps.  "+restTime*5+" ms slept.");
					time = new Date().getTime();
					restTime=0;

				} else restTime++;

				try {
					Thread.sleep(5);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

	}

	
	/**
	 *  Thread that sends continuously a capture request.
	 */
	private class CaptureImageThread implements Runnable {
		private IntByReference useHandle;

		public CaptureImageThread(IntByReference useHandle) {
			this.useHandle = useHandle;
		}

		@Override
		public void run() {
			int answer = arduCamSDKlib.ArduCam_beginCaptureImage(useHandle.getValue());
			displayInfo(" CaptureImageThread: ArduCam_beginCaptureImage returned: "+Utils.intToHex(answer));
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			displayInfo( "launching ArduCam_captureImage");
			
			// loop requesting a capture, should run fast and continuously
			while(running.get()){
				answer = arduCamSDKlib.ArduCam_captureImage(useHandle.getValue());
			}

			answer = arduCamSDKlib.ArduCam_endCaptureImage(useHandle.getValue());
			displayInfo(" CaptureImageThread: ArduCam_endCaptureImage returned: "+Utils.intToHex(answer));

		}
	}
	
	private enum DecodingType {
		DECODING_BW, DECODING_SUBPIX, DECODING_RAW
	}
}
