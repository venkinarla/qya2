package GUI;

import java.awt.BorderLayout;
import java.awt.GridLayout;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import Robot.Tribot;

public class UI_PDA extends JFrame
{
	//************ data member *************
	Tribot lego;
	
	//private GUI_ManualCtrlPanel manual_panel;
	private GUI_AutoCtrlPanel auto_panel;
	//private GUI_APCollectorPanel ap_panel;
	
	//private JTabbedPane tabbed_pane;

	//************ class method ************
	// initialization
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

	public static void main( String[] args )
	{
		UI_PDA myUI = new UI_PDA(new Tribot());
		myUI.setTitle("Robot Guiding System");
		myUI.setSize(400, 520);
		myUI.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		myUI.show();
	}

}
