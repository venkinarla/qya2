package GUI;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.PrintWriter;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MenuKeyListener;

//import Data.DataCollector;
import Robot.Tribot;
import Device.PDA;
import FileIO.Logging;

public class GUI_ManualCtrlPanel extends JPanel
{
	//************** data member ***************
	// direction buttons
	private JButton up_button;
	private JButton down_button;
	private JButton left_button;
	private JButton right_button;
	private JButton stop_button;
	
	private JButton downleft_button;
	private JButton downright_button;
	
	private JButton left_reverse_button;
	private JButton right_reverse_button;
	
	// robot connection button
	private JButton connect_button;

	// robot speed control slider
	private JSlider speed_slider;
	
	// direction slider
	private JSlider angle_slider;

	// control variables
	private boolean connected;
	private int speed = 100;
	
	// the robot and mobile device
	Tribot lego;
	
	// the meta grid that this robot is in
	int meta_grid = -1;
	
	// the log file name
	String log_file = "log.txt";

	//************* class method ***************
	GUI_ManualCtrlPanel(Tribot robot)
	{
		// initialize GUI
		initGUI();
		
		// configure the robot
		lego = robot;
		
		// set the Basic of the frame
		connected = robot.isConnected();
	}
	
	public void initGUI()
	{
		//setSize(200, 200);
		setLayout(new BorderLayout(5, 5));

		JPanel ctrl_panel = new JPanel();
		ctrl_panel.setLayout(new GridLayout(3, 3));

		// Add labels and text fields to the frame
		up_button = new JButton("|");
		left_reverse_button = new JButton("to-rev");
		right_reverse_button = new JButton("to-rev");
		down_button = new JButton("|");
		downleft_button = new JButton("/");
		downright_button = new JButton("\\");
		left_button = new JButton("<");
		right_button = new JButton(">");
		stop_button = new JButton("Stop");
		connect_button = new JButton("manual");
		speed_slider = new JSlider(0, 1000);

		speed_slider.setMajorTickSpacing(500);
		speed_slider.setMinorTickSpacing(25);
		speed_slider.setPaintTicks(true);
		speed_slider.setPaintLabels(true);

		// speed = speed_slider.getValue();
		add(connect_button, BorderLayout.NORTH);

		add(ctrl_panel, BorderLayout.CENTER);
		add(speed_slider, BorderLayout.SOUTH);
		ctrl_panel.add(left_reverse_button);
		ctrl_panel.add(up_button);
		ctrl_panel.add(right_reverse_button);
		ctrl_panel.add(left_button);
		ctrl_panel.add(stop_button);
		ctrl_panel.add(right_button);
		ctrl_panel.add(downleft_button);
		ctrl_panel.add(down_button);
		ctrl_panel.add(downright_button);
		ctrl_panel.setBorder(BorderFactory.createTitledBorder(BorderFactory
				.createLoweredBevelBorder(), "NXT Motion/Speed Controller:"));
		
		// reverse the left motor
		left_reverse_button.addActionListener(new ActionListener()
		{
			boolean is_reversed = false;
			
			public void actionPerformed(ActionEvent e)
			{
				Thread novo_thread = new Thread()
				{
					public void run()
					{
						lego.reverseLeft();
						if (is_reversed)
							is_reversed = false;
						else
							is_reversed = true;
						String label_txt = is_reversed ? "rev-ed" : "rev";
						left_reverse_button.setText(label_txt);
					}
				};
				if ( left_reverse_button.isEnabled() )
					novo_thread.run();
			}
		});
		
		// reverse the right motor
		right_reverse_button.addActionListener(new ActionListener()
		{
			boolean is_reversed = false;
			
			public void actionPerformed(ActionEvent e)
			{
				Thread novo_thread = new Thread()
				{
					public void run()
					{
						lego.reverseRight();
						if ( is_reversed )
							is_reversed = false;
						else
							is_reversed = true;
						String label_txt = is_reversed ? "rev-ed" : "rev";
						right_reverse_button.setText(label_txt);
					}
				};
				if ( right_reverse_button.isEnabled() )
					novo_thread.run();
			}
		});
		
		// listen to stop button
		stop_button.addActionListener(new ActionListener()
		{
			public void actionPerformed( ActionEvent e )
			{
				if ( stop_button.isEnabled() )
				{
					lego.stopProgram();
					lego.stop();
					
					Logging.logActionData(new String[]{"STOP"});
					
					// update the state for the robot
					Integer[] robot_reading = { lego.getLeftDist(),
							lego.getRightDist(), lego.getFrontDist(),
							lego.getFrontLight(), lego.getLeftSpeed(),
							lego.getRightSpeed(), lego.getOrientation() };
					Logging.logStateData(log_file, robot_reading, meta_grid);
				}
			}
		});
		
		// move forward
		up_button.addMouseListener(new MouseAdapter()
		{	
			private long time_start;
			
			public void mousePressed( MouseEvent e )
			{	
				Thread novo_thread = new Thread()
				{
					public void run()
					{
						lego.forward(speed);
					}
				};
				
				if ( up_button.isEnabled() )
				{
					// start the timer
					time_start = System.currentTimeMillis();
					novo_thread.run();
				}
			}
			
			public void mouseReleased( MouseEvent e )
			{
				Thread novo_thread = new Thread()
				{	
					public void run()
					{
						lego.stop();
					}
				};
				
				if ( up_button.isEnabled() )
				{
					novo_thread.run();
					// stop the timer
					long elapsed = System.currentTimeMillis() - time_start;
					Logging.logActionData( 
							new String[]{"MOVE " + lego.getLeftSpeed() + " " + elapsed});
				}
			}
		});
		
		// move backward
		down_button.addMouseListener(new MouseAdapter()
		{	
			long time_start;
			
			public void mousePressed( MouseEvent e )
			{
				
				Thread novo_thread = new Thread()
				{
					public void run()
					{
						lego.backward(speed);
					}
				};
				
				if ( down_button.isEnabled() )
				{
					// set the starting time
					time_start = System.currentTimeMillis();
					novo_thread.run();
				}
			}
			
			public void mouseReleased( MouseEvent e )
			{
				Thread novo_thread = new Thread()
				{
					public void run()
					{
						lego.stop();
					}
				};
				
				if ( down_button.isEnabled() )
				{
					novo_thread.run();
					
					// get the finished time
					long elapsed = System.currentTimeMillis() - time_start;
					Logging.logActionData( 
						new String[]{"MOVE -" + Math.abs(lego.getLeftSpeed())
						+ " " + elapsed});
				}
			}
		});
		
		// turn left without shifting
		left_button.addMouseListener(new MouseAdapter()
		{	
			long time_start;
			
			public void mousePressed( MouseEvent e )
			{	
				Thread novo_thread = new Thread()
				{
					public void run()
					{
						lego.turnLeft(speed);
					}
				};
				
				if ( left_button.isEnabled() )
				{
					// set the starting time
					time_start = System.currentTimeMillis();
					novo_thread.run();
				}
			}
			
			public void mouseReleased( MouseEvent e )
			{
				Thread novo_thread = new Thread()
				{
					public void run()
					{
						lego.stop();
					}
				};
				
				if ( left_button.isEnabled() )
				{
					novo_thread.run();
					long elapsed = System.currentTimeMillis() - time_start;
					Logging.logActionData( new String[] { "TURNANGLE "
							+ Math.abs(lego.getLeftSpeed()) * elapsed / 1000 });
				}
			}
		});
		
		// turn right without shifting
		right_button.addMouseListener(new MouseAdapter()
		{	
			long time_start;
			
			public void mousePressed( MouseEvent e )
			{
				Thread novo_thread = new Thread()
				{
					public void run()
					{
						lego.turnRight(speed);
					}
				};
				if ( right_button.isEnabled() )
				{
					// set the starting time
					time_start = System.currentTimeMillis();
					novo_thread.run();
				}
			}
			
			public void mouseReleased( MouseEvent e )
			{
				Thread novo_thread = new Thread()
				{
					public void run()
					{
						lego.stop();
					}
				};
				
				if (right_button.isEnabled())
				{
					novo_thread.run();

					long elapsed = System.currentTimeMillis() - time_start;
					Logging.logActionData(
							new String[] { "TURNANGLE -"
									+ Math.abs(lego.getLeftSpeed()) * elapsed/1000});
				}
			}
		});
		
		speed_slider.addChangeListener(new SpeedChangeListener());
		speed_slider.setValue(300);

		connect_button.addMouseListener(new MouseAdapter()
		{	
			public void mouseClicked( MouseEvent e )
			{
				Thread start_thread = new Thread()
				{
					public void run()
					{
						try
						{
							if ( !lego.isConnected() )
							{
								System.err.println("error: the robot is not connected");
							}
							else
							{
								lego.stopProgram();
								lego.stop();
								// set manual control to be of highest priority
								connect_button.setText("Automatic");
								enableALL();
								connected = true;
							}
						}
						catch (Exception e)
						{
							System.err.println("connection error: " + e.toString());
							connected = false;
							disableALL();
						}
					}
				};
				
				Thread stop_thread = new Thread()
				{
					public void run()
					{
						try
						{
							disableALL();
							// give the control back to the automatic controller
							connect_button.setText("Manual");
							connected = false;
						}
						catch (Exception e)
						{
							System.err.println("disconnection error: " + e.toString());
							connected = true;
							enableALL();
						}
					}
				};

				if (!connected)
				{
					start_thread.run();
				}
				else
				{
					stop_thread.run();
				}	
			}
		});
		disableALL();
	}

	protected void finalize()
	{
		// do nothing
	}

	public void disableALL()
	{
		up_button.setEnabled(false);
		left_reverse_button.setEnabled(false);
		right_reverse_button.setEnabled(false);
		down_button.setEnabled(false);
		downleft_button.setEnabled(false);
		downright_button.setEnabled(false);
		left_button.setEnabled(false);
		right_button.setEnabled(false);
		stop_button.setEnabled(false);
		speed_slider.setEnabled(false);
	}

	public void enableALL()
	{
		up_button.setEnabled(true);
		left_reverse_button.setEnabled(true);
		right_reverse_button.setEnabled(true);
		down_button.setEnabled(true);
		left_button.setEnabled(true);
		right_button.setEnabled(true);
		stop_button.setEnabled(true);
		speed_slider.setEnabled(true);
	}
	
	private class SpeedChangeListener implements ChangeListener
	{
		public void stateChanged( ChangeEvent arg0 )
		{
			Thread novo_thread = new Thread()
			{
				public void run()
				{
					speed = speed_slider.getValue();
				}
			};
			novo_thread.start();
		}
	}
	

	public static void main( String[] args )
	{
		Tribot lego = new Tribot();
		
		JFrame myUI = new JFrame();
		myUI.setTitle("Control Panel");
		myUI.setSize(200, 300);
		myUI.add(new GUI_ManualCtrlPanel(lego));
		myUI.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		myUI.show();
	}

}
