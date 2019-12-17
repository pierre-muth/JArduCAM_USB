package sandbox;

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
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

import arducam.ArduCamCfgFileParameters;
import arducam.ArduCamSDK;
import arducam.ArduCamSDK.ArduCamCfg;
import arducam.ArduCamSDK.ArduCamOutData;
import arducam.Utils;
import cern.jdve.Chart;
import cern.jdve.Style;
import cern.jdve.data.DefaultDataSet;
import cern.jdve.renderer.BarChartRenderer;

public class StreamingAR0134CV extends JPanel implements ActionListener {
	private static final String OPEN_FILE = "Open CFG file";
	private static final String START_CAMERA = "Setup and Start camera";
	private static final String STOP_CAMERA = "Stop camera";
	private static final String SAVEBUFFER = "Save Buffer";
	private static final String REG_SET = "Set";
	private static final String REG_GET = "Get";
	private static final String SLIDER_CHANGED = "slider";
	private static final String DECODE_CHANGED = "decoding";
	private static final String DECODING_BW = "decode as raw B&W";
	private static final String DECODING_SUBPIX = "decode by sub-pixels";
	private static final String DECODING_RAW = "decode as bayer RAW";
	private static final String[] DECODING_OPT = {DECODING_BW, DECODING_SUBPIX, DECODING_RAW};
	private JLabel jlImage;
	private JLabel jlInfo;
	private JFileChooser fileChooser;
	private JTextField jtfRegAddr;
	private JTextField jtfRegValue;
	private JTextField jlRegValueMin;
	private JTextField jlRegValueMax;
	private JSlider jsRegValueSlider;
	private JCheckBox jchbMax;
	private JCheckBox jchSubBuff;
	
	private ArduCamCfgFileParameters cfgParameters;
	private static ArduCamSDK arduCamSDKlib;
	private static IntByReference useHandle;
	private static Thread captureImageThread;
	private static Thread readImageThread;
	private static AtomicBoolean running = new AtomicBoolean(false);
	private static AtomicBoolean saveBuffer = new AtomicBoolean(false);
	private ArduCamSDK.ArduCamCfg.ByReference useCfg;
	
	public static int HEIGHT = 964;
	public static int WIDTH  = 1280;
	
	private static Mat maxMat;
	private static Mat bufMat;
	private static DefaultDataSet histogramDataSet = new DefaultDataSet("histogram");
	
	public StreamingAR0134CV(){
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
		
		maxMat = new Mat(HEIGHT, WIDTH, CvType.CV_8UC3);
		bufMat = new Mat(HEIGHT, WIDTH, CvType.CV_8UC3);
		
		jlImage.setPreferredSize(new Dimension(WIDTH, HEIGHT));
		
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
		Thread.sleep(1000);
		
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
	
	// swing stuff
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
		JButton jbSaveBuff = new JButton(SAVEBUFFER);
		jbSaveBuff.setActionCommand(SAVEBUFFER);
		jbSaveBuff.addActionListener(this);
		jchbMax = new JCheckBox("Max'ing");
		jchbMax.setSelected(false);
		jchSubBuff = new JCheckBox("Substract Buffer");
		jchSubBuff.setSelected(false);
		
		jpCameraControl.add(jbOpen);
		jpCameraControl.add(jbStart);
		jpCameraControl.add(jbStop);
		jpCameraControl.add(jbSaveBuff);
		jpCameraControl.add(jchbMax);
		jpCameraControl.add(jchSubBuff);
		
		jtfRegAddr = new JTextField("address");
		jtfRegAddr.setPreferredSize(new Dimension(60,25));
		jtfRegValue = new JTextField("value");
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
		jlRegValueMin = new JTextField("0");
		jlRegValueMin.setPreferredSize(new Dimension(60,25));
		jlRegValueMax = new JTextField("65535");
		jlRegValueMax.setPreferredSize(new Dimension(60,25));
		jsRegValueSlider = new JSlider(0, 0xFFFF);
		jsRegValueSlider.setPreferredSize(new Dimension(100,25));
		jsRegValueSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				jsRegValueSlider.setMinimum(Integer.parseInt(jlRegValueMin.getText()));
				jsRegValueSlider.setMaximum(Integer.parseInt(jlRegValueMax.getText()));
				actionPerformed(new ActionEvent(this, 0, SLIDER_CHANGED) );
			}
		});
		Chart histoChart = new Chart();
		BarChartRenderer barRenderer = new BarChartRenderer();
		barRenderer.setStyle(0, new Style(Color.BLACK, Color.BLACK));
		barRenderer.setDataSet(histogramDataSet);
		barRenderer.setWidthPercent(50);
		histoChart.addRenderer(barRenderer);
		histoChart.setXScaleVisible(false);
		histoChart.setYScaleVisible(false);
		histoChart.getArea().setTopMargin(0);
		histoChart.getYScale().setLogarithmic(2);
		histoChart.setPreferredSize(new Dimension(250, 40));
		JPanel jpRegister = new JPanel(new FlowLayout(FlowLayout.LEADING));
		jpRegister.add(jtfRegAddr);
		jpRegister.add(jtfRegValue);
		jpRegister.add(jbSetRegister);
		jpRegister.add(jbGetRegister);
		jpRegister.add(jlRegValueMin);
		jpRegister.add(jsRegValueSlider);
		jpRegister.add(jlRegValueMax);
		jpRegister.add(histoChart);
		
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
	
	// decoding GUI actions
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
		
		if (actionEvent.getActionCommand().equals(SAVEBUFFER)){
			saveBuffer.set(true);
		}
	}
	
	public static void main(String[] args) {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		//Create and set up the window.
		JFrame frame = new JFrame("ArduCam Streaming Demo");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().add(new StreamingAR0134CV(), BorderLayout.CENTER );
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
			int restTime = 0;

			while(running.get()){
				answer = arduCamSDKlib.ArduCam_availableImage(useHandle.getValue());
				if (answer >0){

					answer = arduCamSDKlib.ArduCam_readImage(useHandle.getValue(), pstFrameData);
					ArduCamOutData arduCamOutData = new ArduCamOutData(pstFrameData.getValue());
					ArduCamCfg arduCamCfg = arduCamOutData.stImagePara;
					byte[] imageRAWByteData = arduCamOutData.pu8ImageData.getPointer().getByteArray(0, arduCamCfg.u32Size);
					Mat colorMat;
					Mat matRAW;
					
					if (cfgParameters.cameraParameters.BIT_WIDTH == 8){
						matRAW =  new Mat(HEIGHT, WIDTH, CvType.CV_8UC1);
						matRAW.put(0, 0, imageRAWByteData);
						colorMat = new Mat(HEIGHT, WIDTH, CvType.CV_8UC3);
						Imgproc.cvtColor(matRAW, colorMat, Imgproc.COLOR_BayerGR2BGR);
					} else {
						ByteBuffer bb = ByteBuffer.wrap(imageRAWByteData);
						bb.order(ByteOrder.LITTLE_ENDIAN);
						ShortBuffer sb = bb.asShortBuffer();
						short[] shorts = new short[sb.capacity()];
						sb.get(shorts);
						matRAW =  new Mat(HEIGHT, WIDTH, CvType.CV_16UC1);
						matRAW.put(0, 0, shorts);
						Core.normalize(matRAW, matRAW, 0, 255, Core.NORM_MINMAX);
						colorMat =  new Mat(HEIGHT, WIDTH, CvType.CV_16UC3);
						Imgproc.cvtColor(matRAW, colorMat, Imgproc.COLOR_BayerGR2BGR);
						colorMat.convertTo(colorMat, CvType.CV_8UC3);
					}
					
					// keep only highest value
					if (jchbMax.isSelected()) {
						Core.max(colorMat, maxMat, maxMat);
						maxMat.copyTo(colorMat);
					} else {
						colorMat.copyTo(maxMat);
					}
					
					// substract the bufferer value
					if (jchSubBuff.isSelected()) {
						Core.subtract(colorMat, bufMat, colorMat);
					}
					
					// save the current frame as a buffer
					if (saveBuffer.get()) {
						colorMat.copyTo(bufMat);
						saveBuffer.set(false);
					}
					

					byte[] imageColorData = new byte[WIDTH * HEIGHT * (int)colorMat.elemSize()];
					colorMat.get(0, 0, imageColorData);
					
					// refresh the displayed image
					javax.swing.SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							BufferedImage bufferedImage = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_3BYTE_BGR);
							WritableRaster raster;
							raster = bufferedImage.getRaster();
							raster.setDataElements(0, 0, WIDTH, HEIGHT, imageColorData);
							bufferedImage.setData(raster);
							jlImage.setIcon(new ImageIcon(bufferedImage));
						}
					});
					
					answer = arduCamSDKlib.ArduCam_del(useHandle.getValue());
					
					// get the histogram from data on frame
//					histogramDataSet.clear();
//					int startIdx = (WIDTH*(HEIGHT-2)) +16;
//					int value;
//					ByteBuffer bb;
//					
//					for (int i = startIdx; i < 128+startIdx; i+=2) {
//						bb = ByteBuffer.allocate(2);
//						bb.order(ByteOrder.BIG_ENDIAN);
//						bb.put(imageRAWByteData[i]);
//						bb.put(imageRAWByteData[i+1]);
//						value = bb.getShort(0);
//						histogramDataSet.add(i, value);
//					}
					
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
			
			while(running.get()){
				answer = arduCamSDKlib.ArduCam_captureImage(useHandle.getValue());
			}

			answer = arduCamSDKlib.ArduCam_endCaptureImage(useHandle.getValue());
			displayInfo(" CaptureImageThread: ArduCam_endCaptureImage returned: "+Utils.intToHex(answer));

		}
	}
	
}
