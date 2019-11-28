package arducam;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

public class StreamingDemo extends JPanel implements ActionListener {
	private static final String OPEN = "Open";
	private static final String START = "Start";
	private static final String STOP = "Stop";
	private JTextArea jtaConsole;
	private JLabel jlImage;
	
	public StreamingDemo(){
		// init GUI
		setLayout(new BorderLayout());
		JPanel jpTop = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JButton jbOpen = new JButton(OPEN);
		jbOpen.setActionCommand(OPEN);
		jbOpen.addActionListener(this);
		JButton jbStart = new JButton(START);
		jbStart.setActionCommand(START);
		jbStart.addActionListener(this);
		JButton jbStop = new JButton(STOP);
		jbStop.setActionCommand(STOP);
		jbStop.addActionListener(this);
		jpTop.add(jbOpen);
		jpTop.add(jbStart);
		jpTop.add(jbStop);
		jtaConsole = new JTextArea(">");
		jtaConsole.setEditable(false);
		JScrollPane jspBottom = new JScrollPane(jtaConsole);
		jspBottom.setPreferredSize(new Dimension(640, 150));
		jlImage = new JLabel();
		jlImage.setOpaque(true);
		JScrollPane jspCenter = new JScrollPane(jlImage);
		jspCenter.setPreferredSize(new Dimension(640, 480));
		add(jpTop, BorderLayout.NORTH);
		add(jspCenter, BorderLayout.CENTER);
		add(jspBottom, BorderLayout.SOUTH);
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

	@Override
	public void actionPerformed(ActionEvent actionEvent) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				jtaConsole.append("\n"+actionEvent.getActionCommand());
			}
		});
		
		if (actionEvent.getActionCommand().contains(OPEN)){
			
		}
	}

}
