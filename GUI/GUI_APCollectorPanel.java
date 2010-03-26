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

public class GUI_APCollectorPanel extends JPanel
{
	//************* data member ***************
	// functional units
	DataCollector collector;
	DataReceiver receiver;
	
	// add the collected data to database
	SignalDatabase database;
	
	// parameters
	int num_sample = 5;
	int meta_grid;
	
	// GUI
	JTextArea text_window;
	JButton scan_button;
	JTextField num_sample_field;
	JTextField meta_grid_field;
	
	//************* class method **************
	// initialization
	public GUI_APCollectorPanel()
	{
		database = new SignalDatabase();
		database.loadDataSet();
		initGUI();
	}
	
	public void initGUI()
	{
		setLayout(new BorderLayout(5, 5));
		
		text_window = new JTextArea();
		PrintStream printer = new PrintStream(new JTextAreaOutputStream(text_window));
		System.setOut(printer);
		System.setErr(printer);
		text_window.setEditable(false);
		JScrollPane text_scroll = new JScrollPane(text_window,
				JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		add(text_scroll, BorderLayout.CENTER);
		
		JPanel param_panel = new JPanel();
		param_panel.setLayout(new GridLayout(3, 1));
		
		param_panel.add(new JLabel("scan no."));
		num_sample_field = new JTextField();
		num_sample_field.setText("4");
		param_panel.add(num_sample_field);;
		
		param_panel.add(new JLabel("grid"));
		meta_grid_field = new JTextField();
		meta_grid_field.setText("-1");
		param_panel.add(meta_grid_field);
		
		param_panel.add(new JLabel("press to scan"));
		scan_button = new JButton();
		scan_button.setText("scan");
		param_panel.add(scan_button);
		
		scan_button.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e) 
			{
				num_sample = Integer.parseInt(num_sample_field.getText());
				meta_grid = Integer.parseInt(meta_grid_field.getText());
				text_window.append("Current meta grid: " + meta_grid + "\n");
				/*Vector<SignalVector> sv = DataCollector.Scan(1, 1000);
				System.out.println(sv);
				if (sv == null)
				{
					try {
						Runtime.getRuntime().exec("devcon disable \"PCI\\VEN_8086&DEV_4220*\"");
					} catch (IOException e3) {
						// TODO Auto-generated catch block
						e3.printStackTrace();
					}
					try {
						Thread.sleep(4000);
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					try {
						Runtime.getRuntime().exec("devcon enable \"PCI\\VEN_8086&DEV_4220*\"");
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					try {
						Thread.sleep(6000);
					} catch (InterruptedException e2) {
						// TODO Auto-generated catch block
						e2.printStackTrace();
					}
					sv = DataCollector.Scan(1, 1000);
				}
				for( int i=0; i<sv.size(); i++ ){
					text_window.append("AP"+(i+1)+"\n");
					for(String mac: sv.elementAt(i).getMacAddr()){
						
						text_window.append(mac+" "+sv.elementAt(i).getRSSI(mac)+"\n");
					}
				}*/
				// add the new data to database
				database.expand(DataCollector.Scan(num_sample, 1000), meta_grid);
				text_window.append("Database Size: " + database.sig_vec_base.size() + "\n");
				// save database to local disk
				database.saveDataSet();
			}		
		});
		
		add(param_panel, BorderLayout.PAGE_START);
	}
	
	protected void finalize()
	{
		// save the dataset in the end
		database.saveDataSet();
	}
	
	
	public static void main(String[] args)
	{
		JFrame myUI = new JFrame();
		myUI.setTitle("AP Collector");
		myUI.setSize(400, 520);
		myUI.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		myUI.add(new GUI_APCollectorPanel());
		myUI.show();
	}
	
	
}
