package Robot;

import java.io.*;
import java.util.*;

import lejos.pc.comm.*;
import lejos.nxt.LightSensor;
import lejos.nxt.Motor;
import lejos.nxt.UltrasonicSensor;
import lejos.nxt.remote.NXTCommand;
import lejos.nxt.remote.RemoteMotor;
import lejos.nxt.remote.RemoteMotorPort;
import lejos.nxt.remote.RemoteSensorPort;

// we want to get more localization support
import lejos.localization.*;

import FileIO.Logging;
import Robot.Protocol;
import State.Coordinate;

// implement the traditional Lego tribot
public class Tribot
{
	// ************** data member *****************
	protected String name;
	protected String dev_addr;
	protected boolean connected = false;
	protected NXTCommand cmd;
	protected int type;
	
	// remote ports
	protected RemoteSensorPort port1;
	protected RemoteSensorPort port2;
	protected RemoteSensorPort port3;
	protected RemoteSensorPort port4;
	protected static int portA = 0;
	protected static int portB = 1;
	protected static int portC = 2;
	
	// motors
	protected RemoteMotor left_motor;
	protected RemoteMotor right_motor;
	protected RemoteMotor aux_motor;
	protected int left_reversed = 1;
	protected int right_reversed = 1;
	protected int aux_reversed = 1;
	
	// the guiding angle
	int guiding_angle = 0;
	
	// sensors
	protected UltrasonicSensor right_sonar = null;
	protected UltrasonicSensor left_sonar = null;
	protected UltrasonicSensor front_sonar = null;
	protected LightSensor front_light = null;
	
	// remote I/O
	protected InputStream bin = null;
	protected OutputStream bout = null;
	
	// legacy PDA simulation
	protected static Protocol comm_protocol = new Protocol();
	
	// the action logging file
	public static String log_file = "log.lgr";
	
	//****************** parameters ****************
	// direction, initially zero facing "north"
	protected int orientation = 0;
	
	// artificial bag
	protected int safe_front_dist;
	protected int safe_left_dist;
	protected int safe_right_dist;
	
	// threshold for sonar readings
	protected static int sonar_thresh = 200;

	// wheel diameter, check the wheel peripheral for detail
	protected float wheel_diam;
	
	// track width, the length of the axis between wheels
	protected float track_width;
	
	// connection types
	protected static int BLUETOOTH = NXTCommFactory.BLUETOOTH;
	protected static int USB = NXTCommFactory.USB;

	private int totalforward = 0;
	// ************** class method ****************
	// initialize
	public Tribot()
	{
		type = -1;
		connected = false;
	}
	
	
	public Tribot(NXTInfo dev_info, int conn_mode)
	{
		NXTComm comm = null;
		try
		{
			comm = NXTCommFactory.createNXTComm(conn_mode);
			connected = comm.open(dev_info);
			if ( !connected )
			{
				System.err.println("error: cannot connect to the device");
				System.exit(1);
			}
			System.out.println("connected");
			cmd.setNXTComm(comm);
			name = dev_info.name;
		}
		catch (Exception e)
		{
			System.err.println("error: cannot create tribot");
			System.exit(1);
		}
	}
	
	// customized configuration
	// specify the sensors and motors
	public void configure()
	{
		name = "NXT";
		setBT();
		
		if (!connected)
			connect(name);
		if (connected)
		{
			wheel_diam = 16.0f;
			track_width = 5.6f;

			left_motor = new RemoteMotor(cmd, portA);
			right_motor = new RemoteMotor(cmd, portB);
			aux_motor = new RemoteMotor(cmd, portC);
		
			left_motor.setSpeed(400); 					//MAX speed = 720
			right_motor.setSpeed(400);
			//aux_motor.setSpeed(300);

			left_sonar = new UltrasonicSensor(port1);
			right_sonar = new UltrasonicSensor(port2);
			front_sonar = new UltrasonicSensor(port3);
			front_light = new LightSensor(port4);
		}
	}
	
	// connect to a tribot
	public void connect( String name )
	{
		try
		{
			NXTComm comm = NXTCommFactory.createNXTComm(type);
			NXTInfo details = null;
			
			NXTInfo[] dev_info = comm.search(name, type);

			if (dev_info.length <= 0)
			{
				System.err.println("ERROR: Cannot find any device");
				comm.close();
				return;
			}

			// connect to the first connectable device
			for (int i = 0; i < dev_info.length; ++i)
			{
				connected = comm.open(dev_info[i]);
				if (connected)
				{
					details = dev_info[i];
					System.out.println("Connection established");
					break;
				}
			}
			
			if ( !connected )
			{	
				System.err.println("ERROR: Cannot establish connection");
				comm.close();
				return;
			}
			else 
			{
				// display the device information
				String conn_type = (details.protocol == USB) ? "USB": "BLUETOOTH";
				System.out.println("Connected");
				System.out.println("================================================");
				System.out.println("Connection Type : " + conn_type);
				System.out.println("NXT Name        : " + details.name);
				System.out.println("Bluetooth  Addr : " + details.deviceAddress);
				System.out.println("================================================");

				NXTCommand cmd = new NXTCommand();
				cmd.setNXTComm(comm);
				this.name = name;
				this.cmd = cmd;
				bin = comm.getInputStream();
				bout = comm.getOutputStream();

				port1 = new RemoteSensorPort(cmd, 0);
				port2 = new RemoteSensorPort(cmd, 1);
				port3 = new RemoteSensorPort(cmd, 2);
				port4 = new RemoteSensorPort(cmd, 3);
			}
		}
		catch (Exception e)
		{
			System.err.println("ERROR: " + e.toString());
		}
		
	}
	
	protected void finalize()
	{
		disconnect();
	}
	
	public void disconnect()
	{
		stop();
		try
		{
			if ( connected )
				cmd.close();
			connected = false;
		}
		catch (IOException e)
		{
			System.err.println("ERROR: failed to disconnet");
		}
	}
	
	public void wait( int millis )
	{
		try
		{
			Thread.sleep(millis);
		}
		catch( Exception e )
		{
			// do nothing
		}
	}
	
	// going forward
	public void forward(int speed, int duration)
	{
		int time_count = 0;				// Loop counter
		int time_unit = 10;				// The amount of time increased for every loop
		int front_danger_dist = 40;		// The front danger distance for the robot 
		int wing_danger_dist = 40;		// The side danger distance for the robot
		int turn_angle = (90 + Coordinate.correctangle);			// The angle turned when hit something
		int turnto = -1;				// The direction that the robot turn to when hit something. 0 = left, 1 = right.
		int counter = 0;
		//int wing_angle = 35;
		//int forward_deviation_correction_time = 2000;
		//int forward_deviation_correction_angle = 20;
		
		// soft parameters that could be learned by the machine
		//int angle_thresh = 40;
		//double angle_test_thresh = 0.29;
		//double turning_test_thresh = 0.13;
		// recording changing issues
		//int non_changed_time = 0;
		int front_reading = getFrontDist();
		
		setSpeed(speed);				// Setup the speed of both motor
		if ( front_reading >= front_danger_dist )	//Check to see if nothing is in front of the robot
		{
			right_motor.forward();			// Tell the right motor move forward
			left_motor.forward();			// Tell the left motor move forward
		}
											// PS. If in high speed (eg. >400 ) , robot may move to the left because the robot motor move first.
		
		int old_left_reading = -1;		// The previous left wing reading
		int old_right_reading = -1;		// The previous right wing reading
		System.out.println("Orientation = " + orientation);
		while ( time_count < duration )
		{
			if (totalforward >= 2)
			{
				duration = duration - 10;
				totalforward = 0;
			}
																//--------------------------------------
			boolean changed = false;							// This indicate if the robot has change the direction because of obstacle
			
			int left_reading = getLeftDist();					// Get all the distance reading
			int right_reading = getRightDist();
			front_reading = getFrontDist();
			//System.out.println(getLeftSpeed() + " " + getRightSpeed() + " " + time_count + " " + time_unit);
																//--------------------------------------
			if ( front_reading <= front_danger_dist )			// if the robot is about to hit something
			{
				System.out.println("FRONT HIT = " + front_reading + " HIT TIME = " + time_count + " dur = " + duration);
				if (time_count + time_unit*2 >= duration)		// Ignore the last bit of distance if it is about the end of the duration
				{
					System.out.println("Chop end");
					break;										// To bypass some of the grid with the stupid locker. Like Grid 3.
				}
				stop();											// Stop the robot
				if ( left_reading < right_reading )				// if the left side of the robot is nearer to the wall than the right side
				{
						correctTurnangle(turn_angle);			// Turn the robot to the right
						turnto = 1;								// Record the turned direction. 1 = right
						System.out.println("TURN TO RIGHT");
				}
				else											// if the right side of the robot is nearer to the wall than the left side
				{
						correctTurnangle(-turn_angle);			// Turn the robot to the left
						turnto = 0;								// Record the turned direction. 0 = left
						System.out.println("TURN TO LEFT");
				}
				changed = true;									// Mark the robot as turned
			}													//--------------------------------------
			else if ( (old_left_reading != -1) &&				// else if the robot is near to one side of the wall  
					  (old_right_reading != -1)
					)
			{
				if (left_reading < wing_danger_dist)
				{
					if ( (old_left_reading - left_reading) > 3  && 	// if robot moving toward the left side quickly
						 (old_left_reading - left_reading) < 50 &&	// ignore sudden jump due to environment
						 (left_reading < right_reading)				// and if there are more space at the right side
						)
					{
						changespeedforward(400,250);				// Slow down the right motor to correct the movement
						wait(200);
						System.out.println("LEFT CORRECT");
					}
					else if ( (old_left_reading > left_reading) &&	// if robot moving toward the left side slowly
								(left_reading < right_reading)		// and if there are more space at the right side
							) 
					{
						changespeedforward(400,300);				// Slow down the right motor to correct the movement
						wait(200);
						System.out.println("LEFT SMALL CORRECT");
					}
				}
				if (right_reading < wing_danger_dist)
				{
					if ( (old_right_reading - right_reading) > 3 && 	// if robot moving toward the right side quickly 
						 (old_right_reading - right_reading) < 50 &&	// ignore sudden jump due to environment
						 (right_reading < left_reading)					// and if there are more space at the left side
							)
					{
						changespeedforward(250,400);				// Slow down the left motor to correct the movement
						wait(200);
						System.out.println("RIGHT CORRECT");
					}				
					else if ( (old_right_reading > right_reading) &&	// if robot moving toward the right side slowly 
								(right_reading < left_reading)			// and if there are more space at the left side
							)
					{
						changespeedforward(300,400);				// Slow down the left motor to correct the movement
						wait(200);
						System.out.println("RIGHT SAMLL CORRECT");
					}
				}
			}													//--------------------------------------
			System.out.println(old_left_reading +" "+ old_right_reading +" "+ left_reading +" "+ right_reading);
				/*if (left_reading < wing_danger_dist)
				{
					loggedTurnangle(-wing_angle);
					time_count -= time_unit/10;
					changed = true;
				}
				if (right_reading < wing_danger_dist)
				{
					loggedTurnangle(wing_angle);
					time_count -= time_unit/10;
					changed = true;
				}*/	
			//}
			
			// if we are following a line for too long
			// modify the orientation angle 
			/*if ( !changed &&
					non_changed_time > forward_deviation_correction_time )
			{
				if ( left_reading < right_reading && Math.random() > 0.3 )
					orientation += forward_deviation_correction_angle;
				if ( right_reading < left_reading && Math.random() > 0.3 )
					orientation -= forward_deviation_correction_angle;
			}*/
			
	
			// check the angle deviation
			// otherwise it is easily trapped in some area
			/*if ( !changed &&
					Math.abs(orientation - guiding_angle) > angle_thresh && 
					Math.random() > angle_test_thresh )
			{
				//turnangle(-orientation);
				loggedTurnangle(-orientation);
				changed = true;
			}*/
															//--------------------------------------
			if ( changed )									// if the direction of the robot is changed
			{
				/*if ( time_count > 0 )
					Logging.logActionData( 
						new String[]{"MOVE " + speed + " " + non_changed_time });*/
				old_left_reading = getLeftDist();			// Get the left reading just after turned		
				old_right_reading = getRightDist();			// Get the right reading just after turned
				forward(speed);								// Move the robot forward
				//wait(200);									// Allow the robot to move for at least 0.2 second
				left_reading = getLeftDist();				// Get the new left reading after movement
				right_reading = getRightDist();				// Get the new right reading after movement
				front_reading = getFrontDist();				// Get the front reading after movement
				if (turnto == 1)							// if turned to right
				{
					while (left_reading <= 40)				
					{
						//forward(speed);
						/*if (Math.abs(left_reading - old_left_reading) > 3)
						{
							changespeedforward(400,350);
						}*/
						wait(100);
						old_left_reading = left_reading;
						left_reading = getLeftDist();
						front_reading = getFrontDist();
					}
					correctTurnangle(-turn_angle);
					forward(speed);
				}
				else if (turnto == 0)						// if turned to left
				{
					while (right_reading <= 40)
					{
						//forward(speed);
						/*if ( front_reading <= 20 )
						{
							stop();
							turnto = 1;
							loggedTurnangle(-turn_angle*2);
							old_left_reading = getLeftDist();
							forward(speed);
							wait(200);
							left_reading = getLeftDist();
							while (left_reading <= 40)				//   
							{
								if (Math.abs(left_reading - old_left_reading) > 3)
								{
									changespeedforward(400,380);
								}
								wait(200);
								old_left_reading = left_reading;
								left_reading = getLeftDist();
								front_reading = getFrontDist();
							}
							break;
						}						
						else*//* if (Math.abs(right_reading - old_right_reading) > 3)
						{
							changespeedforward(350,400);
						}*/
						wait(100);
						old_right_reading = right_reading;
						right_reading = getRightDist();
						front_reading = getFrontDist();
						counter++;
						System.out.println(counter);
					}
					if (turnto == 0)
						correctTurnangle(turn_angle);
					else
						correctTurnangle(-turn_angle);
					forward(speed);
					
				}
				changed = false;
				turnto = -1;
				old_left_reading = -1;
				old_right_reading = -1;
				//non_changed_time = 0;
			}
			else
			{
				forward(speed);
				old_left_reading = left_reading;
				old_right_reading = right_reading;
				wait(time_unit);
				//non_changed_time += time_unit;
			}
			time_count += time_unit;
		}
		totalforward = totalforward + 1;
		stop();
	}
	
	//****************** logged actions **********************
	// these functions will be called by the path planner
	public void loggedTurnangle( int angle )
	{
		turnangle(angle);
		wait(1000);
		//Logging.logActionData( new String[] {"TURNANGLE " + angle});
	}
	
	public void correctTurnangle( int angle )
	{
		setSpeed(Math.abs(300));
		left_motor.rotate(angle * 2, true);
		right_motor.rotate(-angle * 2, true);
		wait(1000);
		//Logging.logActionData( new String[] {"TURNANGLE " + angle});
	}
	
	public void loggedForward( int speed, int duration, boolean stop )
	{
		forward(speed);
		wait(duration);
		if ( stop )
			stop();
		Logging.logActionData( 
				new String[] {"MOVE " + speed + " " + duration});
	}
	
	public void loggedBackward( int speed, int duration, boolean stop )
	{
		backward(speed);
		wait(duration);
		if ( stop )
			stop();
		Logging.logActionData( 
				new String[] {"MOVE -" + speed + " " + duration });
	}
	
	public void loggedStop()
	{
		stop();
		Logging.logActionData( 
				new String[] {"STOP"});
	}
	
	
	//******************* basic kinematics ********************
	// the angle turning function called by the device
	public void turnangle( int angle )
	{
		turnangle(300, angle);
		orientation += angle;
	}
	
	
	// turning an angle with a specific speed
	public void turnangle( int speed, int angle )
	{
		setSpeed(Math.abs(speed));
		totalforward = 0;
		left_motor.rotate(angle * 2, true);
		right_motor.rotate(-angle * 2, true);
	}
	
	
	// stop the two motors for good
	public void stop()
	{
		left_motor.stop();
		right_motor.stop();
		//setLeftSpeed(0);
		//setRightSpeed(0);
	}
	
	public void setGuidingAngle(int angle)
	{
		guiding_angle = angle;
	}
	
	// reverse the direction of the motor
	public void reverseLeft()
	{
		left_reversed = -1 * left_reversed;
	}
	
	public void reverseRight()
	{
		right_reversed = -1 * right_reversed;
	}

	public float getRealSpeed( int speed )
	{
		return ((float) 3.141592653589793) * wheel_diam * speed / 360;
	}
	
	// turn around
	public void turn( int left_speed, int right_speed, int duration )
	{	
		setLeftSpeed(left_speed);
		setRightSpeed(right_speed);
		
		if ( left_speed > 0 )
			left_motor.forward();
		else
			left_motor.backward();
		if ( right_speed > 0 )
			right_motor.forward();
		else
			right_motor.backward();
	
		try
		{
			Thread.sleep(duration);
		}
		catch (InterruptedException e)
		{
		}
		this.stop();
	}	
	
	//************ connection settings **************
	public boolean isConnected()
	{
		return connected;
	}
	
	public void setBT()
	{
		type = BLUETOOTH;
	}
	
	public void setUSB()
	{
		type = USB;
	}
	
	// ************* moving commands ******************
	public void forward(int speed)
	{
		setLeftSpeed(speed*left_reversed);
		setRightSpeed(speed*right_reversed);
		
		if ( right_reversed == 1 )
			right_motor.forward();
		else
			right_motor.backward();
		
		if ( left_reversed == 1 )
			left_motor.forward();
		else
			left_motor.backward();
	}
	
	public void changespeedforward(int leftspeed, int rightspeed)
	{
		setLeftSpeed(leftspeed*left_reversed);
		setRightSpeed(rightspeed*right_reversed);
		
		if ( right_reversed == 1 )
			right_motor.forward();
		else
			right_motor.backward();
		
		if ( left_reversed == 1 )
			left_motor.forward();
		else
			left_motor.backward();
	}
	
	public void backward(int speed)
	{
		setLeftSpeed(speed*left_reversed);
		setRightSpeed(speed*right_reversed);
		
		if ( left_reversed == -1 )
			left_motor.forward();
		else
			left_motor.backward();
		if ( right_reversed == -1 )
			right_motor.forward();
		else
			right_motor.backward();
	}
	
	public void turnLeft(int speed)
	{
		setLeftSpeed(speed*left_reversed/2);
		setRightSpeed(speed*right_reversed/2);
		
		if ( left_reversed == 1 )
			left_motor.backward();
		else
			left_motor.forward();
		if ( right_reversed == 1 )
			right_motor.forward();
		else
			right_motor.backward();
	}
	
	public void turnRight(int speed)
	{
		setLeftSpeed(speed*left_reversed/2);
		setRightSpeed(speed*right_reversed/2);
		
		if ( left_reversed == -1 )
			left_motor.backward();
		else
			left_motor.forward();
		if ( right_reversed == -1 )
			right_motor.forward();
		else
			right_motor.backward();
	}
	
	//************** parameters getting & setting **************
	public void runProgram( String prog_name )
	{
		try
		{
			cmd.stopProgram();
			cmd.startProgram(prog_name);
		}
		catch (IOException e)
		{
			System.err.println("error: cannot run the program " + prog_name 
					+ "\n" + e.toString());
		}
	}
	
	public void stopProgram(  )
	{
		try
		{
			cmd.stopProgram();
		}
		catch (IOException e)
		{
			System.err.println("error: cannot stop program\n" + e.toString());
		}
	}
	
	// set current orientation 
	public void setOrientation( int novo_orientation )
	{
		orientation = novo_orientation;
	}
	
	// get current orientation
	public int getOrientation()
	{
		return orientation;
	}
	
	public void setSpeed( int novo_speed )
	{
		setLeftSpeed(novo_speed);
		setRightSpeed(novo_speed);
	}
	
	// a weird bug, we have to set it twice
	public void setLeftSpeed( int novo_speed )
	{
		left_motor.setSpeed(novo_speed * left_reversed);
		left_motor.setSpeed(novo_speed * left_reversed);
	}
	
	// a weird bug, we have to set it twice
	public void setRightSpeed( int novo_speed )
	{
		right_motor.setSpeed(novo_speed * right_reversed);
		right_motor.setSpeed(novo_speed * right_reversed);
	}
	
	public int getLeftSpeed()
	{
		return left_motor.getSpeed();
	}
	
	public int getRightSpeed()
	{
		return right_motor.getSpeed();
	}
	
	public int getLeftPower()
	{
		return left_motor.getPower();
	}
	
	public int getRightPower()
	{
		return right_motor.getPower();
	}
	
	// front light sensor reading
	public int getFrontLight()
	{
		return front_light.readNormalizedValue();
	}
	
	// front sonar reading
	public int getFrontDist()
	{
		return front_sonar.getDistance();
	}
	
	// left wing sonar reading
	public int getLeftDist()
	{
		return left_sonar.getDistance();
	}
	
	// right wing sonar reading
	public int getRightDist()
	{
		return right_sonar.getDistance();
	}
	
	
	public int getVoltage()
	{
		try
		{
			return cmd.getBatteryLevel();
		}
		catch (Exception e)
		{
			System.err.println("Error: cannot get voltage " + e.toString());
			return -1;
		}
	}
	
	
	public InputStream getInputStream()
	{
		return bin;
	}
	
	
	public OutputStream getOutputStream()
	{
		return bout;
	}
	
	
	public static void main(String[] args)
	{
		Tribot lego = new Tribot();
		lego.configure();
	}
}
