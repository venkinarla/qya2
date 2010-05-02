package Robot;

import java.io.*;
import lejos.pc.comm.*;
import lejos.nxt.LightSensor;
import lejos.nxt.UltrasonicSensor;
import lejos.nxt.remote.NXTCommand;
import lejos.nxt.remote.RemoteMotor;
import lejos.nxt.remote.RemoteSensorPort;

import Robot.Protocol;
import State.Coordinate;

/**
 * The Class Tribot that control the robot.
 * Some of the movement function were not in used.
 */
public class Tribot
{
	protected String name;						// The name of the robot
	//protected String dev_addr;
	protected boolean connected = false;		// The boolean to tell if the robot is connected to the client
	protected NXTCommand cmd;					// The NXTCommand used to connect the robot
	protected int type;							// The type of connection
	
	protected RemoteSensorPort port1;			// The sensor port in port 1
	protected RemoteSensorPort port2;			// The sensor port in port 2
	protected RemoteSensorPort port3;			// The sensor port in port 3
	protected RemoteSensorPort port4;			// The sensor port in port 4
	protected static int portA = 0;				// The int value used for the connected motor in port A
	protected static int portB = 1;				// The int value used for the connected motor in port B
	protected static int portC = 2;				// The int value used for the connected motor in port C
	
	protected RemoteMotor left_motor;			// The connected motor in port A
	protected RemoteMotor right_motor;			// The connected motor in port b
	protected RemoteMotor aux_motor;			// The connected motor in port C
	protected int left_reversed = 1;			// The value to tell if the motors are reversed
	protected int right_reversed = 1;
	protected int aux_reversed = 1;
	
	protected UltrasonicSensor right_sonar = null;	// The sonar sensor on three sides
	protected UltrasonicSensor left_sonar = null;
	protected UltrasonicSensor front_sonar = null;
	protected LightSensor front_light = null;		// The light sensor at the front
	
	protected InputStream bin = null;				// The input and output stream of the client
	protected OutputStream bout = null;
	
	protected static Protocol comm_protocol = new Protocol(); // The protocol used to communicate with the client
	
	//public static String log_file = "log.lgr";
	
	protected int orientation = 0;					// Robot direction. "0" means North
	
	protected int safe_front_dist;					// The minimum distance at the front. Used for obstacle detection
	protected int safe_left_dist;					// The minimum distance at the sides. Used for correction
	protected int safe_right_dist;

	//protected static int sonar_thresh = 200;
	//protected float wheel_diam;
	//protected float track_width;
	
	protected static int BLUETOOTH = NXTCommFactory.BLUETOOTH; // The connection types of robot
	protected static int USB = NXTCommFactory.USB;

	private int totalforward = 0;					// The number of grid that the robot move without turning. Used for correction
	
	
	/**
	 * Instantiates a new tribot.
	 */
	public Tribot()
	{
		type = -1;
		connected = false;
	}

	/**
	 * Instantiates a new tribot.
	 * 
	 * @param dev_info the NXTInfo of the robot
	 * @param conn_mode the connection mode
	 */
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
	
	/**
	 * Configure the sensors and motors
	 * 
	 * @return true, if successful
	 */
	public boolean configure()
	{
		name = "NXT";						// Set the name of the robot. Used in bluetooth search
		setBT();
		
		if (!connected)
			connect(name);					// Connect the robot if not connected
		if (connected)
		{
			//wheel_diam = 16.0f;
			//track_width = 5.6f;

			left_motor = new RemoteMotor(cmd, portA);	// Setup all three motors
			right_motor = new RemoteMotor(cmd, portB);
			aux_motor = new RemoteMotor(cmd, portC);
		
			left_motor.setSpeed(430); 					// Setup the speed of the motors. MAX speed = 720
			right_motor.setSpeed(430);					
			//aux_motor.setSpeed(300);

			left_sonar = new UltrasonicSensor(port1);	// Setup all the sensors
			right_sonar = new UltrasonicSensor(port2);
			front_sonar = new UltrasonicSensor(port3);
			//front_light = new LightSensor(port4);
		}
		return connected;
	}
	
	/**
	 * Connect to the lego robot
	 * 
	 * @param name the robot name
	 */
	public void connect( String name )
	{
		try
		{
			NXTComm comm = NXTCommFactory.createNXTComm(type);
			NXTInfo details = null;
			
			NXTInfo dev_info = new NXTInfo(BLUETOOTH, "NXT", "00:16:53:01:21:F9");		// The information of the robot that we are using
			connected = comm.open(dev_info);
			if (connected)
			{
				details = dev_info;
				System.out.println("Robot Connection Established!");
			}
			if ( !connected )
			{	
				System.err.println("ERROR: Cannot establish connection");
				comm.close();
				return;
			}
			else 
			{
				// display the robot information
				String conn_type = (details.protocol == USB) ? "USB": "BLUETOOTH";
				//System.out.println("Connected");
				//System.out.println("================================================");
				System.out.println("Connection Type : " + conn_type);
				System.out.println("NXT Name        : " + details.name);
				System.out.println("Bluetooth  Addr : " + details.deviceAddress);
				//System.out.println("================================================");

				NXTCommand cmd = new NXTCommand();
				cmd.setNXTComm(comm);
				this.name = name;
				this.cmd = cmd;
				bin = comm.getInputStream();					// Setup the robot input & output port for the robot
				bout = comm.getOutputStream();

				port1 = new RemoteSensorPort(cmd, 0);
				port2 = new RemoteSensorPort(cmd, 1);
				port3 = new RemoteSensorPort(cmd, 2);
				port4 = new RemoteSensorPort(cmd, 3);
			}
		}
		catch (Exception e)
		{
			System.err.println("ERROR: " + e.getMessage());
		}
		
	}
	
	/**
	 * Disconnect from the robot.
	 */
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
	
	/**
	 * Wait a while for the robot to travel.
	 * 
	 * @param millis the time millisecond
	 */
	public void wait( int millis )
	{
		try
		{
			Thread.sleep(millis);
		}
		catch( Exception e )
		{
		}
	}

	/**
	 * The main forward function. Handle the obstacle detection and correction.
	 * 
	 * @param speed the motor speed
	 * @param duration the duration of a forward command
	 */
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
		if ( front_reading >= front_danger_dist && front_reading < 255)	//Check to see if nothing is in front of the robot
		{
			left_motor.forward();			// Tell the left motor move forward
			right_motor.forward();			// Tell the right motor move forward
		}
											// PS. If in high speed (eg. >400 ) , robot may move to the left because the robot motor move first.
		
		int old_left_reading = -1;		// The previous left wing reading
		int old_right_reading = -1;		// The previous right wing reading
		//System.out.println("Orientation = " + orientation);
		while ( time_count < duration )
		{
			/*if (totalforward >= 3)
			{
				duration = duration - 10;
				totalforward = 0;
			}*/
																//--------------------------------------
			boolean changed = false;							// This indicate if the robot has change the direction because of obstacle
			
			int left_reading = getLeftDist();					// Get all the distance reading
			int right_reading = getRightDist();
			front_reading = getFrontDist();
			//System.out.println(getLeftSpeed() + " " + getRightSpeed() + " " + time_count + " " + time_unit);
																//--------------------------------------
			if ( front_reading <= front_danger_dist)			// if the robot is about to hit something
			{
				//System.out.println("FRONT HIT = " + front_reading + " HIT TIME = " + time_count + " dur = " + duration);
				/*if (time_count + time_unit*2 >= duration)		// Ignore the last bit of distance if it is about the end of the duration
				{
					System.out.println("Chop end");
					break;										// To bypass some of the grid with the stupid locker. Like Grid 3.
				}*/
				stop();											// Stop the robot
				if ( left_reading < right_reading )				// if the left side of the robot is nearer to the wall than the right side
				{
						correctTurnangle(turn_angle);			// Turn the robot to the right
						turnto = 1;								// Record the turned direction. 1 = right
						//System.out.println("TURN TO RIGHT");
				}
				else											// if the right side of the robot is nearer to the wall than the left side
				{
						correctTurnangle(-turn_angle);			// Turn the robot to the left
						turnto = 0;								// Record the turned direction. 0 = left
						//System.out.println("TURN TO LEFT");
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
						//System.out.println("LEFT CORRECT");
					}
					else if ( (old_left_reading > left_reading) &&	// if robot moving toward the left side slowly
								(left_reading < right_reading)		// and if there are more space at the right side
							) 
					{
						changespeedforward(400,300);				// Slow down the right motor to correct the movement
						wait(200);
						//System.out.println("LEFT SMALL CORRECT");
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
						//System.out.println("RIGHT CORRECT");
					}				
					else if ( (old_right_reading > right_reading) &&	// if robot moving toward the right side slowly 
								(right_reading < left_reading)			// and if there are more space at the left side
							)
					{
						changespeedforward(300,400);				// Slow down the left motor to correct the movement
						wait(200);
						//System.out.println("RIGHT SAMLL CORRECT");
					}
				}
			}													//--------------------------------------
			//System.out.println(old_left_reading +" "+ old_right_reading +" "+ left_reading +" "+ right_reading);
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
						if ( front_reading <= 20 )			// if no path is found on the right side
						{
							stop();
							turnto = 0;
							correctTurnangle(turn_angle*2);
							old_right_reading = getRightDist();
							forward(speed);
							wait(200);
							right_reading = getRightDist();
							while (right_reading <= 40)   
							{
									forward(speed);
									wait(100);
									old_right_reading = right_reading;
									right_reading = getRightDist();
									front_reading = getFrontDist();
							}
							break;
						}						
						wait(100);
						old_left_reading = left_reading;
						left_reading = getLeftDist();
						front_reading = getFrontDist();
						counter++;
						//System.out.println(counter);
					}
					if (turnto == 0)
						correctTurnangle(turn_angle);
					else
						correctTurnangle(-turn_angle);
					forward(speed);
				}
				else if (turnto == 0)						// if turned to left
				{
					while (right_reading <= 40)				
					{
						forward(speed);
						if ( front_reading <= 20 )			// if no path is found on the left side
						{
							stop();
							turnto = 1;
							correctTurnangle(-turn_angle*2);
							old_left_reading = getLeftDist();
							forward(speed);
							wait(200);
							left_reading = getLeftDist();
							while (left_reading <= 40)   
							{
									forward(speed);
									wait(100);
									old_left_reading = left_reading;
									left_reading = getLeftDist();
									front_reading = getFrontDist();
							}
							break;
						}						
						wait(100);
						old_right_reading = right_reading;
						right_reading = getRightDist();
						front_reading = getFrontDist();
						counter++;
						//System.out.println(counter);
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
	
	/**
	 * Turn angle for bypass obstacles
	 * 
	 * @param angle the angle
	 */
	public void correctTurnangle( int angle )
	{
		setSpeed(Math.abs(300));
		left_motor.rotate(angle * 2, true);
		right_motor.rotate(-angle * 2, true);
		wait(1000);
	}

	/**
	 * Turn angle for normal movement
	 * 
	 * @param angle the angle
	 */
	public void turnangle( int angle )
	{
		turnangle(300, angle);
		orientation += angle;
	}
	
	/**
	 * The main turn angle function
	 * 
	 * @param speed the speed
	 * @param angle the angle
	 */
	public void turnangle( int speed, int angle )
	{
		setSpeed(Math.abs(speed));
		totalforward = 0;
		left_motor.rotate(angle * 2, true);
		right_motor.rotate(-angle * 2, true);
	}
	
	/**
	 * Stop the robot.
	 */
	public void stop()
	{
		left_motor.stop();
		right_motor.stop();
		//setLeftSpeed(0);
		//setRightSpeed(0);
	}
	
	/**
	 * Reverse the left motor.
	 */
	public void reverseLeft()
	{
		left_reversed = -1 * left_reversed;
	}
	
	/**
	 * Reverse the right motor.
	 */
	public void reverseRight()
	{
		right_reversed = -1 * right_reversed;
	}
	
	/*public void turn( int left_speed, int right_speed, int duration )
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
	}*/
	
	/**
	 * Checks if robot is connected.
	 * 
	 * @return true, if connected
	 */
	public boolean isConnected()
	{
		return connected;
	}
	
	/**
	 * Sets the connection type to bluetooth.
	 */
	public void setBT()
	{
		type = BLUETOOTH;
	}
	
	/**
	 * Sets the connection type to USB.
	 */
	public void setUSB()
	{
		type = USB;
	}

	/**
	 * The basic forward function
	 * 
	 * @param speed the speed.
	 */
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
	
	/**
	 * Correction function. Move the robot forward by using different motor speed
	 * 
	 * @param leftspeed the left motor speed
	 * @param rightspeed the right motor speed
	 */
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
	
	/**
	 * The basic backward function.
	 * 
	 * @param speed the speed
	 */
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
	
	/**
	 * Turn left.
	 * 
	 * @param speed the speed
	 */
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
	
	/**
	 * Turn right.
	 * 
	 * @param speed the speed
	 */
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
	
	/**
	 * Run program stored in the NXT brick.
	 * 
	 * @param prog_name the program name
	 */
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
	
	/**
	 * Stop the program.
	 */
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

	/**
	 * Sets the robot orientation.
	 * 
	 * @param novo_orientation the new orientation
	 */
	public void setOrientation( int novo_orientation )
	{
		orientation = novo_orientation;
	}

	/**
	 * Gets the robot orientation.
	 * 
	 * @return the orientation
	 */
	public int getOrientation()
	{
		return orientation;
	}
	
	/**
	 * Sets the motor speed for both motor.
	 * 
	 * @param novo_speed the new speed
	 */
	public void setSpeed( int novo_speed )
	{
		setLeftSpeed(novo_speed);
		setRightSpeed(novo_speed);
	}
	
	/**
	 * Sets the left motor speed.
	 * 
	 * @param novo_speed the new left motor speed
	 */
	public void setLeftSpeed( int novo_speed )
	{
		left_motor.setSpeed(novo_speed * left_reversed);
		left_motor.setSpeed(novo_speed * left_reversed);
	}
	
	/**
	 * Sets the right motor speed.
	 * 
	 * @param novo_speed the new right motor speed
	 */
	public void setRightSpeed( int novo_speed )
	{
		right_motor.setSpeed(novo_speed * right_reversed);
		right_motor.setSpeed(novo_speed * right_reversed);
	}
	
	/**
	 * Gets the left motor speed.
	 * 
	 * @return the left motor speed
	 */
	public int getLeftSpeed()
	{
		return left_motor.getSpeed();
	}
	
	/**
	 * Gets the right motor speed.
	 * 
	 * @return the right motor speed
	 */
	public int getRightSpeed()
	{
		return right_motor.getSpeed();
	}
	
	/**
	 * Gets the left motor power.
	 * 
	 * @return the left motor power
	 */
	public int getLeftPower()
	{
		return left_motor.getPower();
	}
	
	/**
	 * Gets the right motor power.
	 * 
	 * @return the right motor power
	 */
	public int getRightPower()
	{
		return right_motor.getPower();
	}
	
	/**
	 * Gets the front light sensor reading.
	 * 
	 * @return the front light reading
	 */
	public int getFrontLight()
	{
		return front_light.readNormalizedValue();
	}
	
	/**
	 * Gets the front sonar sensor reading.
	 * 
	 * @return the front distance
	 */
	public int getFrontDist()
	{
		return front_sonar.getDistance();
	}
	
	/**
	 * Gets the left sonar sensor reading.
	 * 
	 * @return the left distance
	 */
	public int getLeftDist()
	{
		return left_sonar.getDistance();
	}
	
	/**
	 * Gets the right sonar sensor reading.
	 * 
	 * @return the right distance
	 */
	public int getRightDist()
	{
		return right_sonar.getDistance();
	}
	
	/**
	 * Gets the voltage.
	 * 
	 * @return the voltage
	 */
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
	
	
	/**
	 * Gets the input stream.
	 * 
	 * @return the input stream
	 */
	public InputStream getInputStream()
	{
		return bin;
	}
	
	
	/**
	 * Gets the output stream.
	 * 
	 * @return the output stream
	 */
	public OutputStream getOutputStream()
	{
		return bout;
	}
	
	
	/**
	 * The main method.
	 * 
	 * @param args the arguments
	 */
	public static void main(String[] args)
	{
		Tribot lego = new Tribot();
		lego.configure();
	}
}
