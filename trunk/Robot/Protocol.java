package Robot;

import java.util.Vector;

/**
 * The Class of Client Protocol.
 * Designed by previous FYP group. 
 * Most of the things in here is useless after we change the structure.
 * Only processInput,processCommand, RobCommand and some commands are still in use.
 */
public class Protocol
{
	private static final int MAXCOMMANDSIZE = 10;
	// action
	public static final int STOP = 0;
	public static final int MOVE = 1;
	public static final int TURN = 2;
	public static final int TURNANGLE = 3;
	public static final int GETAP = 4;
	public static final int ISOBSTACLE = 5;
	//public static final int HELP = 6;
	public static final int MOVEDIST = 7;
	public static final int MANUAL = 8;

	// Status
	public static final int WAITING = 0;
	public static final int MOVING = 1;
	public static final int DISCONNECTED = 2;
	public static final int DUMMY3 = 3;
	public static final int DUMMY4 = 4;

	// Respond
	private static final int OK = 0;
	private static final int DONE = 1;
	private static final int WARNING = 2;
	private static final int ERROR = 3;
	private static final int FINISHED = 4;
	private static final int DUMMY6 = 5;

	// ERROR
	private static final int INVALID_INPUT = 0;
	private static final int STILL_MOVING = 0;
	private static final int COMMANDLIST_FULL = 0;
	private static final int DUMMY9 = 0;
	private static final int DUMMY10 = 0;
	private static final int DUMMY11 = 0;

	private int RobotStatus;
	private boolean readed;
	private RobCommand CurrentCommand;

	private Vector<RobCommand> CommandList;

	public static final String[] action = { "STOP", // STOP "DURATION"
			"MOVE", // MOVE "SPEED" "DURATION" (only move forward and backward)
			"TURN", // TURN "SPEED_LEFT" "SPEED_RIGHT" "DURATION"
			"TURNANGLE", // TURNANGLE "ANGLE" "SPEED" (rotate an angle without
			// shifting position)
			"GETAP", // GETAP "Number of sample" "period" (Robot will scan and
			// return signal strength data)
			"ISOBSTACLE", // Detect if there is any obstacle on the road
			"HELP", // Print out the Help data
			"MOVEIDST", // move a certain distance
			"MANUAL" // switch to manual control
	};

	public static final String[] status = { "WAITING", "MOVING",
			"DISCONNECTED", "DUMMY3", "FINISHED", };

	public static final String[] respon = { "OK", "DONE", "WARNING", "ERROR",
			"FINISHED", "DUMMY6" };

	public static final String[] errorMess = { "INVALID_INPUT", "STILL_MOVING",
			"COMMANDLIST_FULL", "DUMMY9", "DUMMY10", "DUMMY11" };
	// *************** class method ********************
	// initialization
	/**
	 * Instantiates a new protocol.
	 */
	public Protocol()
	{
		readed = true;
		CurrentCommand = null;
		RobotStatus = WAITING;
		CommandList = new Vector<RobCommand>();
	}

	/**
	 * Sets the status.
	 * 
	 * @param Instatus the instatus
	 * @return the int
	 */
	public int setStatus( int Instatus )
	{
		if (Instatus > 0 && Instatus < status.length)
			RobotStatus = Instatus;
		else
			return -1;
		return 0;
	}

	/**
	 * Reply.
	 * 
	 * @param OutStatus the out status
	 * @param Outrespon the outrespon
	 * @param Outvalue the outvalue
	 * @param Outduration the outduration
	 * @param OutError the out error
	 * @return the string
	 */
	public String reply( int OutStatus, int Outrespon, int Outvalue,
			int Outduration, int OutError )
	{
		String output = "Unknown Command";
		switch (Outrespon)
			{
			case OK:
			case DONE:
			case FINISHED:
				output = status[Outrespon];
				break;
			case ERROR:
				if (OutError < errorMess.length && OutError >= 0)
					output = status[ERROR] + " : " + errorMess[OutError];
				else
					output = status[ERROR];
				break;
			case WARNING:
				break;
			case DUMMY6:
				break;
			default:
				;
			}

		return output;
	}

	/**
	 * Process the command and check to see if it is valid
	 * 
	 * @param command the command
	 * @return true, if valid
	 */
	public boolean processCommand( String[] command )
	{
		int commandAction = -1, commandValue1 = 0, commandValue2 = 0, commandDuration = 0;

		boolean validCommand = true;

		for (int i = 0; i < action.length; i++)
		{
			if (command[0].equalsIgnoreCase(action[i]))
			{
				commandAction = i;
				break;
			}
		}

		try
		{
			switch (commandAction)
				{
				case STOP:
					if (command.length != 2)
						return false;
					commandDuration = Integer.parseInt(command[1]);
					break;
				case MOVE:
					if (command.length != 3)
						return false;
					commandValue1 = Integer.parseInt(command[1]);
					commandDuration = Integer.parseInt(command[2]);
					break;
				case TURNANGLE:
					if (command.length != 2)
						return false;
					commandValue1 = Integer.parseInt(command[1]);
					break;
				case TURN:
					if (command.length != 3)
						return false;
					commandValue1 = Integer.parseInt(command[1]);
					commandValue2 = Integer.parseInt(command[2]);
					break;
				case MOVEDIST:
					if (command.length != 3)
						return false;
					commandValue1 = Integer.parseInt(command[1]);
					commandValue2 = Integer.parseInt(command[2]);
				case GETAP:
					if (command.length != 2)
						return false;
					commandValue1 = Integer.parseInt(command[1]);
					break;
				case ISOBSTACLE:
					return true;
				default:
					validCommand = false;
				}
			if (validCommand)
				CommandList.add(new RobCommand(commandAction, commandValue1,
						commandValue2, commandDuration));
		}
		catch (NumberFormatException e)
		{
			e.printStackTrace();
			validCommand = false;
		}
		return validCommand;
	}

	/**
	 * Process the input message.
	 * 
	 * @param InMessage the in message
	 * @return the string
	 */
	public String processInput( String InMessage )
	{
		if (InMessage == null)
			return "Unknown Command";
		else if (InMessage.compareToIgnoreCase("BYPASS") == 0)			// Used to bypass the readln of server when waiting for a command
			return "USELESS";

		String theInput;
		theInput = InMessage.trim();
		String[] command = theInput.split(" ");
		String theOutput = null;
		boolean validCommand = true;

		if (CommandList != null && CommandList.size() >= MAXCOMMANDSIZE)
		{
			return respon[ERROR] + " : " + errorMess[COMMANDLIST_FULL];
		}

		validCommand = processCommand(command);							// Check to see if the command is valid
		
		if (validCommand)
		{
			// "MOVE [..] [..]"
			if (command[0].substring(0, 4).equalsIgnoreCase("MOVE"))
				return respon[FINISHED];
			// "TURNANGLE [..]"
			else if(command[0].substring(0, 9).equalsIgnoreCase("TURNANGLE"))
				return respon[OK];
		}
		return respon[ERROR] + " : " + errorMess[INVALID_INPUT];
	}

	/**
	 * Gets the command list size.
	 * 
	 * @return the int
	 */
	public int GetCommandListSize()
	{
		return CommandList.size();
	}

	/**
	 * Next command.
	 */
	public void NextCommand()
	{
		CurrentCommand = (RobCommand) CommandList.firstElement();
		CommandList.removeElementAt(0);
		readed = false;
	}

	/**
	 * Gets the action.
	 * 
	 * @return the int
	 */
	public int GetAction()
	{
		readed = true;
		if (CurrentCommand != null)
			return CurrentCommand.commandAction;
		return -1;
	}

	/**
	 * Gets the value1.
	 * 
	 * @return the int
	 */
	public int GetValue1()
	{
		readed = true;
		if (CurrentCommand != null)
			return CurrentCommand.commandValue1;
		return -1;
	}

	/**
	 * Gets the value2.
	 * 
	 * @return the int
	 */
	public int GetValue2()
	{
		readed = true;
		if (CurrentCommand != null)
			return CurrentCommand.commandValue2;
		return -1;
	}

	/**
	 * Gets the duration.
	 * 
	 * @return the int
	 */
	public int GetDuration()
	{
		readed = true;
		if (CurrentCommand != null)
			return CurrentCommand.commandDuration;
		return -1;
	}

	/**
	 * The Class RobCommand.
	 */
	private class RobCommand
	{
		public int commandAction;
		public int commandValue1, commandValue2;
		public int commandDuration;

		/**
		 * Instantiates a new rob command.
		 */
		public RobCommand()
		{
			commandAction = 0;
			commandValue1 = 0;
			commandValue2 = 0;
			commandDuration = 0;
		}

		/**
		 * Instantiates a new rob command.
		 * 
		 * @param inAction the in action
		 * @param inValue1 the in value1
		 * @param inValue2 the in value2
		 * @param inDuration the in duration
		 */
		public RobCommand(int inAction, int inValue1, int inValue2,
				int inDuration)
		{
			commandAction = inAction;
			commandValue1 = inValue1;
			commandValue2 = inValue2;
			commandDuration = inDuration;
		}
	}

	/**
	 * Clear the command.
	 */
	public void clear()
	{
		CommandList.clear();
	}

}
