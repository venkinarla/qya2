package Server;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;

import java.util.*;

import javax.swing.JPanel;

import State.*;


public class MapPanel extends JPanel
{
	public static final int GraphHeight = 652; 			//Height of the [Map.gif] in dataset
	public static final int GraphWidth = 954;			//Width of [Map.gif] in dataset
	public static final int MapHeight = 28;
	public static final int MapWidth = 41;				//Number of grid used in the map
	private Image image = null;
	private int startx, starty, endx, endy, Robx, Roby;	//x,y value of 3 colour dot
	private int PointSize, ZoomFactor;
	/*
	 	PointSize 		: the size of the colour dot display on the map
	 	ZoomFactor		: the width/heigth of a grid
	 */

	//private Vector<Coordinate> shortest_path;
	private byte[][] map;

	public MapPanel(Image inputMap, double inputZoom)
	{
		image = inputMap;
		setPreferredSize(new Dimension(GraphWidth, GraphHeight));
		startx = -1;
		starty = -1;
		endx = -1;
		endy = -1;
		Robx = -1;
		Roby = -1;
		PointSize = 15;
		map = null;
		ZoomFactor = (int) inputZoom;
	}

	public void setImage( Image inputMap )
	{
		image = inputMap;
	}

	public Image getImage()
	{
		return image;
	}

	public void setPointSize( int size )
	{
		PointSize = size;
	}

	public int getPointSize()
	{
		return PointSize;
	}

	public void setSTxy( int x, int y )
	{
		startx = x;
		starty = y;
	}

	public void setENDxy( int x, int y )
	{
		endx = x;
		endy = y;
	}

	public void setRobxy( int x, int y )
	{
		Robx = x;
		Roby = y;
	}

	public void setMap( byte[][] InMap )
	{
		map = InMap;
	}

	// paint the panel
	public void paintComponent( Graphics g )
	{
		super.paintComponent(g);
		if (image != null)
		{
			g.drawImage(image, 0, 0, this);
			int correctStartx = startx / 4 + 5;
			int correctStarty = starty / 4 + 5;
			int correctEndx = endx / 4 + 5;
			int correctEndy = endy / 4 + 5;
			int correctRobx = Robx / 4 + 3;
			int correctRoby = Roby / 4 + 3;			
			// display the shortest path on the map
			/*g.setColor(Color.yellow);
			if ( shortest_path != null )
			{
				for ( int i=0; i<shortest_path.size(); ++i )
				{
					g.fillOval((shortest_path.get(i).x + Xoffset)* ZoomFactor, 
							(shortest_path.get(i).y + Yoffset) * ZoomFactor, 
							PointSize, PointSize);
				}
			}*/
			if (startx != -1 && starty != -1)
			{
				// Draw the starting location
				g.setColor(Color.green);
				g.fillOval(startx * ZoomFactor + correctStartx, starty * ZoomFactor + correctStarty, PointSize,
						PointSize);
			}

			if (endx != -1 && endy != -1)
			{
				// Draw the Ending location
				g.setColor(Color.blue);
				g.fillOval(endx * ZoomFactor + correctEndx, endy * ZoomFactor + correctEndy, PointSize,
						PointSize);
			}

			if (Robx != -1 && Roby != -1)
			{
				// Draw the Robot
				g.setColor(Color.red);
				g.fillOval(Robx * ZoomFactor + correctRobx, Roby * ZoomFactor + correctRoby, PointSize,
						PointSize);
			}

		}
		else
		{
			System.out.println("No image can be Loaded");
		}
	}

}