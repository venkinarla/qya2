package Server;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.util.Vector;

import javax.swing.JPanel;

import State.*;

/**
 * The Class MapPanel which display the map on the server UI.
 */
public class MapPanel extends JPanel
{
	private static final long serialVersionUID = 1671130736951078438L;
	public static final int GraphHeight = 652; 			//Height of the [Map.gif] in dataset
	public static final int GraphWidth = 954;			//Width of [Map.gif] in dataset
	public static final int MapHeight = 28;
	public static final int MapWidth = 41;				//Number of grid used in the map
	private Image image = null;
	private int startx, starty, endx, endy, Robx, Roby;	//x,y value of 3 colour dot
	private int estx, esty;								//x,y value of the estimated location
	private int PointSize, ZoomFactor;
	/*
	 	PointSize 		: the size of the colour dot display on the map
	 	ZoomFactor		: the width/heigth of a grid
	 */
	private Integer[] ShortestPath = null;								//shortest path for the robot
	private Vector<Integer> PossibleBlock = new Vector<Integer>();		//the cells that might have a blockage
	private byte[][] map;

	/**
	 * Instantiates a new map panel.
	 * 
	 * @param inputMap the map image
	 * @param inputZoom the zoom factor
	 */
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
		estx = -1;
		esty = -1;
		PointSize = 15;
		map = null;
		ZoomFactor = (int) inputZoom;
	}

	/**
	 * Sets the map image.
	 * 
	 * @param inputMap the map image
	 */
	public void setImage( Image inputMap )
	{
		image = inputMap;
	}

	/**
	 * Gets the map image.
	 * 
	 * @return the map image
	 */
	public Image getImage()
	{
		return image;
	}

	/**
	 * Sets the point size.
	 * 
	 * @param size the new point size
	 */
	public void setPointSize( int size )
	{
		PointSize = size;
	}

	/**
	 * Gets the point size.
	 * 
	 * @return the point size
	 */
	public int getPointSize()
	{
		return PointSize;
	}

	/**
	 * Sets the starting position.
	 * 
	 * @param x the x coordinate of starting position
	 * @param y the y coordinate of starting position
	 */
	public void setSTxy( int x, int y )
	{
		startx = x;
		starty = y;
	}

	/**
	 * Sets the destination.
	 * 
	 * @param x the x coordinate of destination
	 * @param y the y coordinate of destination
	 */
	public void setENDxy( int x, int y )
	{
		endx = x;
		endy = y;
	}

	/**
	 * Sets the expected location of the robot
	 * 
	 * @param x the x coordinate of expected location
	 * @param y the y coordinate of expected location
	 */
	public void setRobxy( int x, int y )
	{
		Robx = x;
		Roby = y;
	}
	
	/**
	 * Sets the estimated location of the robot
	 * 
	 * @param x the x coordinate of estimated location
	 * @param y the y coordinate of estimated location
	 */
	public void setESTxy( int x, int y )
	{
		estx = x;
		esty = y;
	}
	

	/**
	 * Sets the grid map.
	 * 
	 * @param InMap the new map
	 */
	public void setMap( byte[][] InMap )
	{
		map = InMap;
	}
	
	/**
	 * Sets the shortest path
	 * 
	 * @param path the shortest path
	 */
	public void setPath( Integer[] path)
	{
		ShortestPath = path;
	}

	/**
	 * Paints the panel
	 * 
	 * @param path the shortest path
	 */
	public void paintComponent( Graphics g )
	{
		//PossibleBlock.add(3);
		super.paintComponent(g);
		if (image != null)
		{
			g.drawImage(image, 0, 0, this);				// Paint the map image
			int correctStartx = startx / 4 + 5;			// The offset of each displayed dot. P
			int correctStarty = starty / 4 + 5;
			int correctEndx = endx / 4 + 5;
			int correctEndy = endy / 4 + 5;
			int correctRobx = Robx / 4 + 3;
			int correctRoby = Roby / 4 + 3;
			int correctESTx = estx / 4 + 10;
			int correctESTy = esty / 4 + 10;
			
			if ( ShortestPath != null )					// display the shortest path on the map
			{
				g.setColor(Color.orange);
				for ( int i=0; i<ShortestPath.length; ++i )
				{
					g.fillOval(Coordinate.getCoord(ShortestPath[i]).x * ZoomFactor + Coordinate.getCoord(ShortestPath[i]).x / 4, 
							Coordinate.getCoord(ShortestPath[i]).y * ZoomFactor + Coordinate.getCoord(ShortestPath[i]).y / 4, 
							PointSize - 5 , PointSize - 5);
					
				}
			}
			if ( !PossibleBlock.isEmpty() )				// display the possible blockage on the map
			{
				g.setColor(Color.RED);
				for ( int i=0; i<PossibleBlock.size(); ++i )
				{
					int blockx = Coordinate.getCoord(PossibleBlock.get(i)).x;
					int blocky = Coordinate.getCoord(PossibleBlock.get(i)).y;
					g.drawOval(blockx * ZoomFactor + blockx / 4 + 3, 
							blocky * ZoomFactor + blocky / 4 + 3, 
							PointSize, PointSize);
					g.drawOval(blockx * ZoomFactor + blockx / 4 + 5, 
							blocky * ZoomFactor + blocky / 4 + 5, 
							PointSize-4, PointSize-4);
					g.drawOval(blockx * ZoomFactor + blockx / 4 + 7, 
							blocky * ZoomFactor + blocky / 4 + 7, 
							PointSize-8, PointSize-8);					
				}
			}			
			if (startx != -1 && starty != -1)
			{
				// Draw the starting location
				g.setColor(Color.green);
				g.fillOval(startx * ZoomFactor + correctStartx, starty * ZoomFactor + correctStarty, PointSize,
						PointSize);
			}

			if (endx != -1 && endy != -1)
			{
				// Draw the destination
				g.setColor(Color.blue);
				g.fillOval(endx * ZoomFactor + correctEndx, endy * ZoomFactor + correctEndy, PointSize,
						PointSize);
			}

			if (Robx != -1 && Roby != -1)
			{
				// Draw the expected location of robot
				g.setColor(Color.red);
				g.fillOval(Robx * ZoomFactor + correctRobx, Roby * ZoomFactor + correctRoby, PointSize,
						PointSize);
			}
			
			if (estx != -1 && esty != -1)
			{
				// Draw the estimated location of robot
				g.setColor(Color.MAGENTA);
				g.fillOval(estx * ZoomFactor + correctESTx, esty * ZoomFactor + correctESTy, PointSize,
						PointSize);
			} 

		}
		else
		{
			System.out.println("No image can be Loaded");
		}
	}

}