/**
 * the GUI for collecting AP signal strength data
 */

package GUI;

import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.util.Vector;

import javax.swing.*;

import Data.*;
import Robot.Tribot;

/**
 * The Class GUI_APCollectorPanel.
 */
public class GUI_APCollectorPanel extends JPanel {
	// functional units
	// DataCollector collector;
	// DataReceiver receiver;
	// SignalDatabase database;
	int num_sample = 25;					// The number of time we collect the signals
	//int meta_grid;
	
	JTextArea text_window;					// The text field displaying the message
	JButton scan_button;					// The button for scanning
	JTextField meta_grid_field;				// The cell number
	//private BufferedWriter out = null;
	//private FileWriter fstream = null;
	private int counter = 1;				// The counter for file saving

	/**
	 * Instantiates a new AP collector GUI.
	 */
	public GUI_APCollectorPanel() {
		// database = new SignalDatabase();
		// database.loadDataSet();
		initGUI();
	}

	/**
	 * Inits the gui.
	 */
	public void initGUI() {
		setLayout(new BorderLayout(5, 5));
		
		JPanel param_panel = new JPanel();
		
		scan_button = new JButton("Scan");
		meta_grid_field = new JTextField ("1", 5);
		param_panel.setLayout(new GridBagLayout());
		GridBagConstraints gc = new GridBagConstraints();
		gc.anchor = GridBagConstraints.EAST;
		gc.fill = GridBagConstraints.BOTH;
		gc.gridwidth = GridBagConstraints.LINE_START;
		param_panel.add(scan_button, gc);
		
		gc.gridwidth = GridBagConstraints.LINE_END;
		param_panel.add(meta_grid_field, gc);
		text_window = new JTextArea();
		PrintStream printer = new PrintStream(new JTextAreaOutputStream(
				text_window));
		System.setOut(printer);
		// System.setErr(printer);
		
		text_window.setEditable(false);
		JScrollPane text_scroll = new JScrollPane(text_window,
				JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		add(text_scroll, BorderLayout.CENTER);
		
		scan_button.addActionListener(new ActionListener() 
		{
			public void actionPerformed(ActionEvent e) 
			{
				int grid = Integer.parseInt(meta_grid_field.getText());
				PrintWriter fout = null;
				try {
					fout = new PrintWriter(grid+"[" + counter + "].txt");
				} 
				catch (IOException e3) 
				{
					e3.printStackTrace();
				}
				for (int j = 0; j < num_sample; j++) 							// Repeat the process 25 times
				{
					Vector<SignalVector> sv = DataCollector.Scan(1, 1000);		// Use DataCollector to scan the signals 
					for (int i = 0; i < sv.size(); i++) 
					{
						fout.println(sv.elementAt(i).dim + " " + grid);
						//text_window.append("Number of APs =  " + sv.elementAt(i).dim + "\n");
						for (String mac : sv.elementAt(i).getMacAddr()) 
						{
							fout.println(mac + " " + sv.elementAt(i).getRSSI(mac));
						}
					}
					fout.println("");
				}
				text_window.append("25 sets of data for cell [" + grid + "] are collected" + "\n");		// Output a successfully message
				counter++;
				fout.close();
			}
		});

		add(param_panel, BorderLayout.NORTH);
	}

	/**
	 * The main method.
	 * 
	 * @param args the arguments
	 */
	public static void main(String[] args) {
		JFrame myUI = new JFrame();
		myUI.setTitle("AP Collector");
		myUI.setSize(400, 520);
		myUI.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		myUI.add(new GUI_APCollectorPanel());
		myUI.setVisible(true);
	}

}
