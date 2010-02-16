package Data;

public class SignalStrength
{
	public String Mac_address;
	public String Name; // may not be unquie, like smobilenet
	public int Signal_Strength;
	public int x, y; // may by double for higher accuracy

	public SignalStrength()
	{
		Mac_address = "";
		Name = "";
		Signal_Strength = (int)-7e21;
		x = -1;
		y = -1;
	}
	
	public SignalStrength(String InMac_address, String InName,
			int InSignal_strength, int Inx, int Iny)
	{
		Mac_address = InMac_address;
		Name = InName;
		Signal_Strength = InSignal_strength;
		x = Inx;
		y = Iny;
	}

	public void SetValue( String InMac_address, String InName,
			int InSignal_strength, int Inx, int Iny )
	{
		Mac_address = InMac_address;
		Name = InName;
		Signal_Strength = InSignal_strength;
		x = Inx;
		y = Iny;
	}
	
	public String toString()
	{
		String encoded_str = "mac_addr " + Mac_address + ", " + 
					         "ssid " + Name + ", " + 
					         "rssi " + Signal_Strength + ", " + 
					         "x " + x + ", " + 
					         "y " + y;
		
		return encoded_str;
	}
	
	/**
	 * decode the signal strength data from an encoding string
	 * 
	 * @param encoded_str
	 * @return the decoding if the string is a valid encoding otherwise null
	 */
	public static SignalStrength decode(String encoded_str)
	{
		String[] elements = encoded_str.trim().split(", ");
		int size = elements.length;
		SignalStrength decoded_sig = new SignalStrength();
		
		if ( size != 5 )
		{
			System.err.println("signal strength decoding error: corrupted encoding");
			System.out.println(encoded_str);
			return null;
		}
		
		decoded_sig.Mac_address = elements[0].substring(9);
		decoded_sig.Name = elements[1].substring(5);
		decoded_sig.Signal_Strength = Integer.decode(elements[2].substring(5));
		decoded_sig.x = Integer.decode(elements[3].substring(2));
		decoded_sig.y = Integer.decode(elements[4].substring(2));
		
		return decoded_sig;
	}

	public static void main(String[] args)
	{
		String test_str = "mac_addr 00:11:92:f8:87:b1, ssid sMobileNet, rssi -81, x 101, y 38";
		String result_str = SignalStrength.decode(test_str).toString();
		System.out.println(test_str);
		System.out.println(SignalStrength.decode(test_str).toString());
		System.out.println("are they the same: " + (test_str.compareTo(result_str)==0));
		System.out.println("test finished");
	}
	
}
