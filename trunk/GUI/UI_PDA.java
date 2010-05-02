package GUI;

import java.awt.BorderLayout;
import java.awt.GridLayout;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import Robot.Tribot;

/**
 * The Class PDA UI.
 */
public class UI_PDA extends JFrame
{
	Tribot lego;								// The lego robot
	
	//private GUI_ManualCtrlPanel manual_panel; // The manual control page that we are not using 
	private GUI_AutoCtrlPanel auto_panel;		// The auto contorl page
	//private GUI_APCollectorPanel ap_panel;	// The AP collector page
	
	//private JTabbedPane tabbed_pane;

	/**
	 * Instantiates a new pda UI.
	 * 
	 * @param robot the robot
	 */
	public UI_PDA(Tribot robot)
	{
		lego = robot;
		
		// GUI
		setLayout(new BorderLayout(10, 10));
		//tabbed_pane = new JTabbedPane();

		auto_panel = new GUI_AutoCtrlPanel(lego);
		//manual_panel = new GUI_ManualCtrlPanel(lego);
		//ap_panel = new GUI_APCollectorPanel();
	
		//tabbed_pane.add("Auto", auto_panel);
		//tabbed_pane.add("Manual", manual_panel);
		//tabbed_pane.add("AP Collector", ap_panel);
		add(auto_panel, BorderLayout.CENTER);
	}

	/**
	 * The main method.
	 * 
	 * @param args the arguments
	 */
	public static void main( String[] args )
	{
		UI_PDA myUI = new UI_PDA(new Tribot());
		myUI.setTitle("Robot Guiding System");
		myUI.setSize(400, 520);
		myUI.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		myUI.setVisible(true);
	}

}
