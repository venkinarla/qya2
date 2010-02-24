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

// implement the traditional Lego tribot
public class Tribot
{
	// ************** data memeber *****************
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
		
		wheel_diam = 16.0f;
		track_width = 5.6f;

		left_motor = new RemoteMotor(cmd, portA);
		right_motor = new RemoteMotor(cmd, portB);
		aux_motor = new RemoteMotor(cmd, portC);
		
		left_motor.setSpeed(720); 					//MAX speed = 720
		right_motor.setSpeed(720);
		//aux_motor.setSpeed(300);

		left_sonar = new UltrasonicSensor(port1);
		right_sonar = new UltrasonicSensor(port2);
		front_sonar = new UltrasonicSensor(port3);
		front_light = new LightSensor(port4);
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
		// loop number checker
		int time_count = 0;
		int time_unit = 10;
		
		// hard parameters that should be tuned by hand
		// we should push these parameters to the extreme
		int front_danger_dist = 26;
		int wing_danger_dist = 16;
		int wing_angle = 35;
		int front_angle = 90;
		int forward_deviation_correction_time = 2000;
		int forward_deviation_correction_angle = 20;
		
		// soft parameters that could be learned by the machine
		int angle_thresh = 40;
		double angle_test_thresh = 0.29;
		double turning_test_thresh = 0.13;
		
		// recording changing issues
		int non_changed_time = 0;
		
		setSpeed(speed);

		left_motor.forward();
		right_motor.forward();
		
		while ( time_count < duration )
		{	
			// the change flag which indicates whether
			// there is any direction changes in this round
			boolean changed = false;
			
			int left_reading = getLeftDist();
			int right_reading = getRightDist();
			int front_reading = getFrontDist();
			System.out.println("LEFT = " + left_reading + " RIGHT = " + right_reading + " FRONT = " + front_reading);
			if ( front_reading < front_danger_dist )
			{
				stop();
				loggedBackward(speed, 1300, true);
				System.out.println("FRONT HIT!");
				if ( left_reading < right_reading )
				{
					if ( Math.random() > turning_test_thresh )
						loggedTurnangle(-front_angle);
					else
						loggedTurnangle(front_angle);
				}
				else
				{
					if ( Math.random() > turning_test_thresh )
						loggedTurnangle(front_angle);
					else
						loggedTurnangle(-front_angle);
				}
				time_count -= time_unit/5;
				changed = true;
			}
			else
			{
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
			}
			
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
			
			// if the speed is modified, fire forward again
			if ( changed )
			{
				if ( time_count > 0 )
					Logging.logActionData( 
						new String[]{"MOVE " + speed + " " + non_changed_time });
				forward(speed);
				wait(time_unit);
				non_changed_time = 0;
			}
			else
			{
				wait(time_unit);
				non_changed_time += time_unit;
			}
			
			time_count += time_unit;
		}
		
		// stop after finishing
		stop();
	}
	
	//****************** logged actions **********************
	// these functions will be called by the path planner
	public void loggedTurnangle( int angle )
	{
		turnangle(angle);
		wait(1000);
		Logging.logActionData( 
				new String[] {"TURNANGLE " + angle});
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
		turnangle(100, angle);
		//wait(1000);
		orientation += angle;
	}
	
	
	// turning an angle with a specific speed
	public void turnangle( int speed, int angle )
	{
		setSpeed(Math.abs(speed));
		
		if ( angle > 0 )
		{
			left_motor.rotate(-angle * 2, true);
			right_motor.rotate(angle * 2, true);
		}
		else
		{
			left_motor.rotate(-angle * 2, true);
			right_motor.rotate(angle * 2, true);
		}
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
		
		if ( left_reversed == 1 )
			left_motor.forward();
		else
			left_motor.backward();
		if ( right_reversed == 1 )
			right_motor.forward();
		else
			right_motor.backward();
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
			System.err.println("[Tribot] error: cannot get voltage " + e.toString());
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
