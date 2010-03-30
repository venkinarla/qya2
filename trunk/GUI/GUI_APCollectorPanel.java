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

public class GUI_APCollectorPanel extends JPanel {
	// ************* data member ***************
	// functional units
	// DataCollector collector;
	// DataReceiver receiver;

	// add the collected data to database
	// SignalDatabase database;

	// parameters
	int num_sample = 5;
	int meta_grid;

	// GUI
	JTextArea text_window;
	JButton scan_button;
	// JTextField num_sample_field;
	// JTextField meta_grid_field;
	private BufferedWriter out = null;
	private FileWriter fstream = null;
	private int counter = 1;

	// ************* class method **************
	// initialization
	public GUI_APCollectorPanel() {
		// database = new SignalDatabase();
		// database.loadDataSet();
		initGUI();
	}

	public void initGUI() {
		setLayout(new BorderLayout(5, 5));

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

		JPanel param_panel = new JPanel();

		scan_button = new JButton();
		scan_button.setText("scan");
		param_panel.add(scan_button);

		scan_button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				PrintWriter fout = null;
				try {
					fout = new PrintWriter(counter + ".txt");
				} catch (IOException e3) {
					// TODO Auto-generated catch block
					e3.printStackTrace();
				}
				for (int j = 0; j < 30; j++) {
					Vector<SignalVector> sv = DataCollector.Scan(1, 1000);
					for (int i = 0; i < sv.size(); i++) {
						fout.println(sv.elementAt(i).dim + "\n");

						text_window.append("Number of APs =  "
								+ sv.elementAt(i).dim + "\n");
						for (String mac : sv.elementAt(i).getMacAddr()) {
							fout.println(mac + " "
									+ sv.elementAt(i).getRSSI(mac) + "\n");
						}
					}
					fout.close();
					text_window.append("\n");
				}
			}
		});

		add(param_panel, BorderLayout.PAGE_START);
	}

	public static void main(String[] args) {
		JFrame myUI = new JFrame();
		myUI.setTitle("AP Collector");
		myUI.setSize(400, 520);
		myUI.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		myUI.add(new GUI_APCollectorPanel());
		myUI.show();
	}

}
