package GUI;

import java.io.*;

import javax.swing.JTextArea;


public class JTextAreaOutputStream extends OutputStream
{
	private JTextArea text_area;
	
	public JTextAreaOutputStream( JTextArea jtext )
	{
		text_area = jtext;
	}

	@Override
	public void write( int b ) throws IOException
	{
		text_area.append(new String(new byte[]{(byte)b}));
	}
	
	public void flush() throws IOException
	{
		text_area.repaint();
	}
	
}
