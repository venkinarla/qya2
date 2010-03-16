package Server;

import Data.*;

public class UI_Protocol {
	//action
	private static final int STOP = 0;
	private static final int MOVE = 1;
	private static final int TURN = 2;
	private static final int TURNANGLE = 3;
	private static final int AP = 4;
	private static final int OBJ = 5;
	private static final int HELP = 6;
	
	//Status
	private static final int WAITING = 0;
	private static final int MOVING = 1;
	private static final int DISCONNECTED = 2;
	private static final int DUMMY3 = 3;
	private static final int DUMMY4 = 4;

	//Respond
	private static final int OK = 0;
	private static final int DONE = 1;
	private static final int WARNING = 2;
	private static final int ERROR = 3;
	private static final int FINISHED = 4;
	private static final int DUMMY6 = 5;
	
	//ERROR
	private static final int INVALID_INPUT = 0;
	private static final int STILL_MOVING = 0;
	private static final int DUMMY8 = 0;
	private static final int DUMMY9 = 0;
	private static final int DUMMY10 = 0;
	private static final int DUMMY11 = 0;
	
	private int RobotStatus;
	private int commandAction=-1,
		commandValue=-1,
		commandDuration=-1;	
	
	// stores the current ap data
	private SignalStrength CurrentAP;
	
	public static final String[] action = { 	"STOP",				// STOP "DURATION"
												"MOVE",				// MOVE "SPEED"	"DURATION"  (only move forward and backward)
												"TURN",				// TURN "LEFTSPEED" "RIGHTDPEED" "DURATION"
												"TURNANGLE",		// TURNANGLE "ANGLE"		(ROBOT will stop and turn at defined angle)
												"AP:",
												"OBJ:",
												"HELP"
												};
	
	public static final String[] status = { 	"WAITING",
												"MOVING",
												"DISCONNECTED",
												"DUMMY3",
												"FINISHED",
												};
	
	public static final String[] respon = { 	"OK",
					                            "DONE",
					                            "WARNING",
					                            "ERROR",
												"FINISHED",
												"DUMMY6"
											};
	
	public static final String[] errorMess = { 	"INVALID_INPUT",
											        "STILL_MOVING",
											        "DUMMY8",
											        "DUMMY9",
													"DUMMY10",
													"DUMMY11"
												};
	
	
	UI_Protocol()
	{
		RobotStatus=WAITING;
		CurrentAP = new SignalStrength(null,null,0,0,0);
	}
		
	public int setStatus(int Instatus)
	{		
		if (Instatus > 0 && Instatus<status.length)
			RobotStatus = Instatus;
		else return -1;
		return 0;
	}
	
	public String reply(int OutStatus, int Outrespon, int Outvalue, int Outduration, int OutError)
	{
		String output=null;
		switch (Outrespon)
		{
			case OK:
			case DONE:
			case FINISHED:
				output =  status[Outrespon];
				break;
			case ERROR:
				if (OutError < errorMess.length && OutError>=0)
					output =  status[ERROR]+":" + errorMess[OutError];
				else
					output =  status[ERROR];
				break;
			case WARNING:
				break;
			//case DUMMY5:
			//	break;
			case DUMMY6:
				break;
			default:; 
		}
		
		return output;		
	}
	
	public boolean processCommand(String [] command)
	{
		boolean validCommand = true;
		for (int i=0; i<action.length; i++)
		{
			if (command[0].equalsIgnoreCase(action[i])) 
				commandAction=i;
		}
		
		try
		{
			switch (commandAction)
		    {
				case AP:
					if (command.length==4 && !command[1].equalsIgnoreCase("END")) 
					CurrentAP= new SignalStrength(command[1], command[2], Integer.parseInt(command[3]), -1, -1);
					break;
				case OBJ:					
					break;
				case HELP:					
					break;
				default: validCommand = false;			
		    }
		
		}catch (NumberFormatException e)
		{
			//e.printStackTrace();
			System.err.println("error: number format error\n" + e.toString());
			validCommand=false;
		}
		return validCommand;
	}
	
	public String processInput(String InMessage) 
	{
		if (InMessage == null) return "Unknown Command";
		
		String [] command = InMessage.split(" ");	
		//boolean validCommand = true;
		//validCommand = processCommand(command); 		
		//if (!validCommand) return "NA"; 
		
		/*if (command[0].equalsIgnoreCase("AP:"))
		{
			if (command[1].equalsIgnoreCase("END"))
				return "END";
			else
				return "AP";
		}*/
		
		// when a sequence of commands is finished
		// upon hearing this, we give the moving approval
		if ( command[0].equalsIgnoreCase("ERROR") )
		{
			return "ERROR";
		}
		else if (command[0].equalsIgnoreCase("FINISHED"))
		{
			return "Robot Movement Finished";
		}
		else if (command[0].equalsIgnoreCase("OK"))
		{
			return "Robot Orientation Changed";
		}
		else return "Invalid Command";
	}
	
	public SignalStrength GetSignal()
	{
		return CurrentAP;
	}
}
