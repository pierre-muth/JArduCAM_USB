package arducam;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.util.Date;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

import arducam.ArduCamSDK.ArduCamCfg;
import arducam.ArduCamSDK.ArduCamOutData;

public class StreamingDemo extends JPanel implements ActionListener {
	private static final String OPEN_FILE = "Open CFG file";
	private static final String START_CAMERA = "Setup and Start camera";
	private static final String STOP_CAMERA = "Stop camera";
	private JLabel jlImage;
	private JFileChooser fileChooser;
	private ArduCamCfgFileParameters cfgParameters;
	private static ArduCamSDK arduCamSDKlib;
	private static IntByReference useHandle;
	private static Thread captureImageThread;
	private static Thread readImageThread;
	private static boolean running = false;
	private ArduCamSDK.ArduCamCfg.ByReference useCfg;
	private static int[] pixList;
	public static int HEIGHT = 964;
	public static int WIDTH  = 1280;
	
	public StreamingDemo(){
		initGUI();		
	}
	
	private void setupAndStartCamera() throws InterruptedException{
		if (cfgParameters == null) return;
		if (running) return;
		
		int answer;
		byte[] arg1 = new byte[32];
		
		pixList = new int[1280*964];
		jlImage.setPreferredSize(new Dimension(1280, 964));
		
		arduCamSDKlib = ArduCamSDK.INSTANCE;
		
		answer = arduCamSDKlib.ArduCam_scan(arg1);
		System.out.println("ArduCam_scan returned: "+Utils.intToHex(answer));
		System.out.println("arg1: "+Utils.bytesToHex( arg1 )+" : "+new String(arg1));
		System.out.println("...");
		Thread.sleep(2000);
		
		useHandle = new IntByReference();
		useCfg = new ArduCamSDK.ArduCamCfg.ByReference();
//		useCfg.u32CameraType = 0x4D091031;
//		useCfg.u16Vid = 0x52CB;                 // Vendor ID for USB
//		useCfg.u32Width = cfgParameters.cameraParameters.SIZE[0];					// Image Width
//		useCfg.u32Height = cfgParameters.cameraParameters.SIZE[1];					// Image Height
//		useCfg.u8PixelBytes = 1;
//		useCfg.u8PixelBits = (byte) cfgParameters.cameraParameters.BIT_WIDTH;
//		useCfg.u32I2cAddr = cfgParameters.cameraParameters.I2C_ADDR;
//		useCfg.u32Size = useCfg.u32Width * useCfg.u32Height;
//		useCfg.usbType = 2;
//		useCfg.emI2cMode = cfgParameters.cameraParameters.I2C_MODE;
//		useCfg.emImageFmtMode = 0;			// image format mode 
//		useCfg.u32TransLvl = cfgParameters.cameraParameters.TRANS_LVL;
		useCfg.u32CameraType = 0x4D091031;
		useCfg.u16Vid = 0x52CB;                 // Vendor ID for USB
		useCfg.u32Width = 1280;					// Image Width
		useCfg.u32Height = 964;					// Image Height
		useCfg.u8PixelBytes = 1;
		useCfg.u8PixelBits = 8;
		useCfg.u32I2cAddr = 0x20;
		useCfg.u32Size = 0xDEAD;
		useCfg.usbType = 2;
		useCfg.emI2cMode = 3;
		useCfg.emImageFmtMode = 0;			// image format mode 
		useCfg.u32TransLvl = 64;
		
		answer = arduCamSDKlib.ArduCam_autoopen(useHandle.getPointer(), useCfg);
		System.out.println("ArduCam_autoopen returned: "+Utils.intToHex(answer));
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
			System.out.println( "ArduCam_setboardConfig returned: "+Utils.intToHex(answer));
			if (answer != 0) return;
		}
		
		Thread.sleep(1000);
		
		int i2cAddr = cfgParameters.cameraParameters.I2C_ADDR;
		for (ArduCamCfgFileParameters.RegisterParameter registerParameter : cfgParameters.registerParameters) {
			if (registerParameter.type == ArduCamCfgFileParameters.RegisterParameter.DELAY) {
				Thread.sleep(100);
			} else {
				answer  = arduCamSDKlib.ArduCam_writeReg_16_16(useHandle.getValue(), i2cAddr, registerParameter.register, registerParameter.value);
				System.out.println( "ArduCam_writeReg_16_16 returned: "+Utils.intToHex(answer));
			}
		}

		//EXTERNAL_TRIGGER_MODE = 0x01
		//CONTINUOUS_MODE = 0x02
		answer = arduCamSDKlib.ArduCam_setMode(useHandle.getValue(), 0x02);
		System.out.println( "ArduCam_setMode returned: "+Utils.intToHex(answer));
		if (answer != 0) return;
		
		running = true;
		Thread.sleep(1000);
		
		captureImageThread = new Thread(new CaptureImageThread(useHandle));
		captureImageThread.start();

		readImageThread = new Thread(new ReadImageThread(useHandle));
		readImageThread.start();

		System.out.println( "Running.");
		
	}
	
	private void stopCamera(){
		if (!running) return;

		running = false; 
		try {
			captureImageThread.join();
			readImageThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		System.out.println("Stopping");

		int answer = arduCamSDKlib.ArduCam_close(useHandle.getValue());
		System.out.println("ArduCam_close returned: "+Utils.intToHex(answer));
	}
	
	private void initGUI() {
		setLayout(new BorderLayout());
		JPanel jpTop = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JButton jbOpen = new JButton(OPEN_FILE);
		jbOpen.setActionCommand(OPEN_FILE);
		jbOpen.addActionListener(this);
		JButton jbStart = new JButton(START_CAMERA);
		jbStart.setActionCommand(START_CAMERA);
		jbStart.addActionListener(this);
		JButton jbStop = new JButton(STOP_CAMERA);
		jbStop.setActionCommand(STOP_CAMERA);
		jbStop.addActionListener(this);
		jpTop.add(jbOpen);
		jpTop.add(jbStart);
		jpTop.add(jbStop);
		jlImage = new JLabel();
		jlImage.setOpaque(true);
		JScrollPane jspCenter = new JScrollPane(jlImage);
		jspCenter.setPreferredSize(new Dimension(640, 480));
		add(jpTop, BorderLayout.NORTH);
		add(jspCenter, BorderLayout.CENTER);
		
		fileChooser = new JFileChooser();
	    FileNameExtensionFilter filter = new FileNameExtensionFilter(
	        "CFG file", "cfg");
	    fileChooser.setFileFilter(filter);
	}

	@Override
	public void actionPerformed(ActionEvent actionEvent) {

		if (actionEvent.getActionCommand().contains(OPEN_FILE)){
			int returnVal = fileChooser.showOpenDialog(this);
			if(returnVal == JFileChooser.APPROVE_OPTION) {
				
				System.out.println("Openning " + fileChooser.getSelectedFile().getName());

				try {
					cfgParameters = new ArduCamCfgFileParameters(fileChooser.getSelectedFile());
				} catch (IOException e) {
					System.out.println( e.getMessage());
				}
				
				System.out.println("Parameters loaded.");
			}

		}
		
		if (actionEvent.getActionCommand().contains(START_CAMERA)){
			try {
				setupAndStartCamera();
			} catch (InterruptedException e) {
				System.out.println( e.getMessage());
			}
		}
		
		if (actionEvent.getActionCommand().contains(STOP_CAMERA)){
			stopCamera();
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

			while(running){
				answer = arduCamSDKlib.ArduCam_availableImage(useHandle.getValue());
				if (answer >0){

					answer = arduCamSDKlib.ArduCam_readImage(useHandle.getValue(), pstFrameData);
					
					ArduCamOutData arduCamOutData = new ArduCamOutData(pstFrameData.getValue());
					ArduCamCfg arduCamCfg = arduCamOutData.stImagePara;
					
					byte[] imageData = arduCamOutData.pu8ImageData.getPointer().getByteArray(0, arduCamCfg.u32Size);
//					
					for (int i = 0; i < imageData.length; i++) {
						pixList[i] = imageData[i];
					}
					
					javax.swing.SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							BufferedImage bufferedImage = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_BYTE_GRAY);
							WritableRaster raster;
							raster = bufferedImage.getRaster();
							raster.setPixels(0, 0, WIDTH, HEIGHT, pixList);
							bufferedImage.setData(raster);
							
							jlImage.setIcon(new ImageIcon(bufferedImage));
						}
					});

					answer = arduCamSDKlib.ArduCam_del(useHandle.getValue());
					System.out.println(" fps: "+ 1000/( new Date().getTime() - time ));
					time = new Date().getTime();

				} else System.out.print(".");

				try {
					Thread.sleep(5);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

	}

	
	private class CaptureImageThread implements Runnable {
		private IntByReference useHandle;

		public CaptureImageThread(IntByReference useHandle) {
			this.useHandle = useHandle;
		}

		@Override
		public void run() {
			int answer = arduCamSDKlib.ArduCam_beginCaptureImage(useHandle.getValue());
			System.out.println(" CaptureImageThread: ArduCam_beginCaptureImage returned: "+Utils.intToHex(answer));
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			System.out.println( "launching ArduCam_captureImage");
			
			while(running){
				answer = arduCamSDKlib.ArduCam_captureImage(useHandle.getValue());
			}

			answer = arduCamSDKlib.ArduCam_endCaptureImage(useHandle.getValue());
			System.out.println(" CaptureImageThread: ArduCam_endCaptureImage returned: "+Utils.intToHex(answer));

		}


	}
}
