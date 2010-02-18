package GUI;

import java.awt.*;
import javax.swing.*;
import Robot.Tribot;

public class GUI_Parameter extends JPanel
{
	//************ data member *************
	Tribot lego;
	
	//************ class method ************
	// initialization
	public GUI_Parameter( Tribot robot )
	{
		lego = robot;
	}
	
	public void initGUI()
	{
		setLayout(new BorderLayout(5, 5));
		
	}
}
