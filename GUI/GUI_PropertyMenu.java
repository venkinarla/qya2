/**
 * set up parameters for the robot
 */

package GUI;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import Robot.Tribot;
import javax.swing.*;


public class GUI_PropertyMenu extends JPanel
{
	//************ data member **************
	public Tribot lego;
	
	// GUI elememnts
    public LabeledGauge front_sonar_gauge;
    public LabeledGauge front_light_gauge;
    public LabeledGauge left_sonar_gauge;
    public LabeledGauge right_sonar_gauge;
    public LabeledGauge left_motor_gauge;
    public LabeledGauge right_motor_gauge;
    public LabeledGauge battery_gauge;
	
	//************ class method *************
	// initialization
	public GUI_PropertyMenu(Tribot robot)
	{
		lego = robot;
		
		initGUI();
		//left_motor_gauge.setVal(400);
		Timer timer = new Timer(1000, new Updater());
		timer.start();
	}

	// initialize GUI
	public void initGUI()
	{
		setLayout(new GridLayout(3, 2));

		// sonar gauges
		front_sonar_gauge = new LabeledGauge("Front Sonar", 200);
		front_light_gauge = new LabeledGauge("Front Light", 100);
		left_sonar_gauge = new LabeledGauge("Left Sonar", 200);
		right_sonar_gauge = new LabeledGauge("Right Sonar", 200);
		
		// speed gauges
		left_motor_gauge = new LabeledGauge("Left Motor", 1000);
		right_motor_gauge = new LabeledGauge("Right Motor", 1000);
		
		// battery gauge
		battery_gauge = new LabeledGauge("Battery Voltage Level", 10000);
		
		add(front_sonar_gauge);
		add(front_light_gauge);
		add(left_sonar_gauge);
		add(right_sonar_gauge);
		add(left_motor_gauge);
		add(right_motor_gauge);
	}
	
	public void setRobot(Tribot robot)
	{
		lego = robot;
	}
	
	private class Updater implements ActionListener
	{
		public void actionPerformed( ActionEvent e )
		{
			Thread novo_thread = new Thread()
			{
				public void run()
				{
					if (!lego.isConnected())
						return;

					left_sonar_gauge.setVal(lego.getLeftDist());
					right_sonar_gauge.setVal(lego.getRightDist());
					front_sonar_gauge.setVal(lego.getFrontDist());

					left_motor_gauge.setVal(lego.getLeftSpeed());
					right_motor_gauge.setVal(lego.getRightSpeed());

					battery_gauge.setVal(lego.getVoltage());
				}
			};
			novo_thread.start();
		}
	}
	
	public static void main(String[] args)
	{
		//Tribot lego = new Tribot();
		
		JFrame myUI = new JFrame();
		myUI.setTitle("Sensor Property Panel");
		myUI.setSize(320, 450);
		myUI.add(new GUI_PropertyMenu(null));
		myUI.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		myUI.show();
	}
	
}
