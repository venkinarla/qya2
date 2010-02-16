/**
 * log the necessary data including
 * 		1. Actions performed by the robot
 * 		2. Current Coordinate of the robot
 * 		3. All the sensor readings of the robot
 * 		4. Estimated orientation of the robot
 */

package FileIO;

import java.util.*;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import Robot.Tribot;
import State.Coordinate;

public class Logging
{
	public static String log_file = "../data/log.lgr";
	
	public static void logStateData(String file_name, Integer[] robot_reading, Integer curr_metagrid)
	{
		Vector<String> reading = new Vector<String>();
		
		if (robot_reading.length != 7)
		{
			System.err.println("error: we don't have enough sensor readings");
		}
		else
		{
			// sensor data
			reading.add("left wing sonar: " + robot_reading[0]);
			reading.add("right wing sonar: " + robot_reading[1]);
			reading.add("front sonar: " + robot_reading[2]);
			reading.add("front light sensor: " + robot_reading[3]);
			
			// motor data
			int left_speed = robot_reading[4];
			reading.add("left motor: " + ((left_speed > 0)? "running" : "stop")
					+ ((left_speed > 0)? ", speed: " + left_speed : ""));
			int right_speed = robot_reading[5];
			reading.add("right motor: " + ((right_speed > 0) ? "running" : "stop") 
					+((right_speed > 0)? ", speed: " + right_speed : ""));
			
			// the orientation of the robot
			reading.add("robot orientation: " + robot_reading[6]);
		}
		
		// record which meta grid this coordinate is in
		reading.add("current meta grid: " + curr_metagrid);
		
		
		// log them
		for ( int i=0; i<reading.size(); ++i )
		{
			appendLog( reading.elementAt(i) );
		}
		// time stamp and ending mark
		appendLog( Calendar.getInstance().getTime().toString()+"\nEND\n" );
	}
	
	public static void logActionData( String[] action )
	{
		for ( int i=0; i<action.length; ++i )
		{
			if ( action[i] != null )
				appendLog(action[i]);
		}
		appendLog(Calendar.getInstance().getTime().toString()+"\nEND\n");
	}
	
	// write the string to a local log entry
	public static void appendLog( String entry )
	{
		try
		{
			// must set the appending mode to be true
			FileOutputStream file = new FileOutputStream(log_file, true);
			FileChannel fc = file.getChannel();
			
			long length = fc.size();
			//System.out.printf("file size: %d\n", length);
			fc.position(length+1);
			
			ByteBuffer buff_write = ByteBuffer.wrap((entry+"\n").getBytes());
			
			fc.write(buff_write);
			fc.close();
			file.close();
		}
		catch (Exception e)
		{
			System.err.println(e.toString());
		}
	}
	
	public static void main(String[] args)
	{
		//appendLog( "file_random_access.txt", "so this is it" );
	}
}
