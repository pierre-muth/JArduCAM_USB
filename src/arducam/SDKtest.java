package arducam;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.util.Date;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.PointerByReference;

import arducam.ArduCamSDK.ArduCamOutData;
import cern.jdve.Chart;
import cern.jdve.ChartInteractor;
import cern.jdve.data.DefaultDataSet;

public class SDKtest extends JPanel {
	private static ArduCamSDK arduCamSDKlib;
	private static IntByReference useHandle;
	private static Thread captureImageThread;
	private static Thread readImageThread;
	private static boolean running = true;
	private ArduCamSDK.ArduCamCfg.ByReference useCfg;
	private JLabel imageLabel;
	public static final int HEIGHT = 964;
	public static final int WIDTH  = 1280;

	private static int[] pixList = new int[HEIGHT * WIDTH];
	private static int[] rawList = new int[HEIGHT * WIDTH];
	private static int[] histogram = new int[4096];

	private DefaultDataSet histoDataSet = new DefaultDataSet("histogram");

	public SDKtest() throws InterruptedException {
		initGUI();
	}

	private class CaptureImageThread implements Runnable {
		private IntByReference useHandle;

		public CaptureImageThread(IntByReference useHandle) {
			this.useHandle = useHandle;
		}

		@Override
		public void run() {
			int answer = arduCamSDKlib.ArduCam_beginCaptureImage(useHandle.getValue());
			System.out.println(new Date().getTime()+" CaptureImageThread: ArduCam_beginCaptureImage returned: "+Utils.intToHex(answer));
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			System.out.println("launching ArduCam_captureImage");
			while(running){
				answer = arduCamSDKlib.ArduCam_captureImage(useHandle.getValue());
			}

			answer = arduCamSDKlib.ArduCam_endCaptureImage(useHandle.getValue());
			System.out.println(new Date().getTime()+" CaptureImageThread: ArduCam_endCaptureImage returned: "+Utils.intToHex(answer));

		}


	}

	private class ReadImageThread implements Runnable {
		private IntByReference useHandle;
		private ArduCamSDK.ArduCamOutData arduCamOutData;
		private LongByReference ref = new LongByReference();

		
		
		public ReadImageThread(IntByReference useHandle) {
			this.useHandle = useHandle;
		}

		@Override
		public void run() {
			int answer;

			while(running){
				answer = arduCamSDKlib.ArduCam_availableImage(useHandle.getValue());
				if (answer >0){
					System.out.println("");
					answer = arduCamSDKlib.ArduCam_readImage(useHandle.getValue(), ref);
					Pointer pt = new Pointer(ref.getValue());

					byte[] blist = pt.getByteArray(0, 1280*50);
					for (int i = 0; i < blist.length; i++) {
						pixList[i] = blist[i];
					}
					
					javax.swing.SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							BufferedImage monoImage = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_BYTE_GRAY);
							WritableRaster monoRaster;
							monoRaster = monoImage.getRaster();
							monoRaster.setPixels(0, 0, WIDTH, HEIGHT, pixList);
							monoImage.setData(monoRaster);
							imageLabel.setIcon(new ImageIcon(monoImage));
						}
					});

					answer = arduCamSDKlib.ArduCam_del(useHandle.getValue());

				} else System.out.print(".");

				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

	}

	private void initCam() throws InterruptedException{
		byte[] arg1 = new byte[32];
		int answer;

		arduCamSDKlib = ArduCamSDK.INSTANCE;

		answer = arduCamSDKlib.ArduCam_scan(arg1);
		System.out.println("ArduCam_scan returned: "+Utils.intToHex(answer));
		System.out.println("arg1: "+Utils.bytesToHex( arg1 )+" : "+new String(arg1));
		System.out.println("...");
		Thread.sleep(2000);

		useHandle = new IntByReference();
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
		System.out.println(new Date().getTime());
		System.out.println("ArduCam_autoopen returned: "+Utils.intToHex(answer));
		System.out.println("useHandle.getValue(): "+Utils.intToHex( useHandle.getValue() ));
		Thread.sleep(2000);
		System.out.println("");
		if (answer != 0) return;

		IntByReference pu8DevUsbType = new IntByReference();
		IntByReference pu8InfUsbType = new IntByReference();
		answer = arduCamSDKlib.ArduCam_getUsbType(useHandle.getValue(), pu8DevUsbType.getPointer(), pu8InfUsbType.getPointer());
		System.out.println(new Date().getTime());
		System.out.println("ArduCam_getUsbType returned: "+Utils.intToHex(answer));
		System.out.println("ArduCam_getUsbType pu8DevUsbType: "+Utils.intToHex(pu8DevUsbType.getValue()));
		System.out.println("ArduCam_getUsbType pu8InfUsbType: "+Utils.intToHex(pu8InfUsbType.getValue()));

		System.out.println("");

		Pointer pu8Buf1 = new Memory(1 * Native.getNativeSize(Byte.TYPE));
		pu8Buf1.setByte(0, (byte) 0x05);
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
		System.out.println(new Date().getTime());
		System.out.println("ArduCam_setboardConfig returned: "+Utils.intToHex(answer));
		if (answer != 0) return;
		
		Thread.sleep(1000);
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
		answer += arduCamSDKlib.ArduCam_writeReg_16_16(useHandle.getValue(), i2cAddr, 0x3012, 0x0800);
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

		System.out.println(new Date().getTime());
		System.out.println("ArduCam_writeReg_16_16 returned: "+Utils.intToHex(answer));
		if (answer != 0) return;

		int regAddr = 0x3012;
		IntByReference pval = new IntByReference();
		answer = arduCamSDKlib.ArduCam_readReg_16_16(useHandle.getValue(), i2cAddr, regAddr, pval.getPointer());
		System.out.println("ArduCam_readReg_16_16 returned: "+Utils.intToHex(answer));
		System.out.println("regAddr: "+Utils.intToHex(regAddr)+" pval.getValue(): "+Utils.intToHex( pval.getValue() ));

		//EXTERNAL_TRIGGER_MODE = 0x01
		//CONTINUOUS_MODE = 0x02
		answer = arduCamSDKlib.ArduCam_setMode(useHandle.getValue(), 0x02);
		System.out.println(new Date().getTime());
		System.out.println("ArduCam_setMode returned: "+Utils.intToHex(answer));
		if (answer != 0) return;

		System.out.println("..");
		Thread.sleep(1000);

		captureImageThread = new Thread(new CaptureImageThread(useHandle));
		captureImageThread.start();

		readImageThread = new Thread(new ReadImageThread(useHandle));
		readImageThread.start();

		System.out.println(new Date().getTime());
		System.out.println("Running.");
		//		Thread.sleep(8000);

	}

	private void stopCam(){

		running = false; 
		try {
			captureImageThread.join();
			readImageThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		System.out.println("Stopped.");

		int answer = arduCamSDKlib.ArduCam_close(useHandle.getValue());
		System.out.println(new Date().getTime());
		System.out.println("ArduCam_close returned: "+Utils.intToHex(answer));
		System.out.println("useHandle.getValue(): "+Utils.intToHex( useHandle.getValue() ));

	}

	private void initGUI(){
		setLayout(new BorderLayout());
		JTabbedPane jtp = new JTabbedPane();
		imageLabel = new JLabel();
		imageLabel.setPreferredSize(new Dimension(WIDTH, HEIGHT));
		imageLabel.setOpaque(true);
		jtp.add(imageLabel, "image");

		Chart histogramChart = new Chart();
		histogramChart.setDataSet(histoDataSet);
		histogramChart.addInteractor(ChartInteractor.ZOOM);

		jtp.add(histogramChart, "histogram");

		add(jtp, BorderLayout.CENTER);

		JButton jbStart = new JButton("Start");
		jbStart.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					initCam();
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
		});

		add(jbStart, BorderLayout.NORTH);

		JButton jbStop = new JButton("Stop");
		jbStop.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				stopCam();
			}
		});

		add(jbStop, BorderLayout.SOUTH);

	}

	public static void main(String[] args) throws InterruptedException {

		SDKtest test = new SDKtest();

		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				//Create and set up the window.
				JFrame frame = new JFrame("FrameDemo");
				frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				frame.getContentPane().add(test, BorderLayout.CENTER );
				//Display the window.
				frame.pack();
				frame.setVisible(true);
			}
		});
	}
}
