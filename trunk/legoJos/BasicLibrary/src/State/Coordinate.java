/**
 * Coordinate Spec
 * 
 * This is an abstraction for positions in the CSE office area, which was grided with 50cm \times 50cm
 * grids. These small grids are the basic unit for position estimation and robot navigation
 * 
 */

package State;

import java.io.*;
import java.util.*;

// used for something
public class Coordinate
{
	public int x, y;
	
	// the grid data files
	// these files are loaded to the system
	public static String meta_grid_file = 
		"../data/meta_grid.mgr";        // .mgr defines maps the smallest grids to meta grids
	public static String meta_grid_graph_file = 
		"../data/meta_grid_graph.mgh";  // .mgh defines the relationship between each pairs of meta grids
	
	// map from meta grid to original small coordinate
	public static Map<Integer, Coordinate[]> meta_grid;
	// the graph representing the relationship between each meta grid
	public static Map<Integer, Integer[]> meta_grid_graph;

	// initialize the meta grid information from a meta grid definition
	public static void initMetaGrid()
	{
		// get the data from file
		meta_grid = new HashMap<Integer, Coordinate[]>();
		BufferedReader reader = null;
		System.out.println("loading meta grid spec...");
		try
		{
			reader = new BufferedReader(new FileReader(meta_grid_file));
		}
		catch (FileNotFoundException e)
		{
			System.err.println("error: the file is not found\n" + e.toString());
			return;
		}

		try
		{
			String input = reader.readLine();
			while (input != null)
			{
				input = input.trim();
				if (input.length() == 0 || input.charAt(0) == '%')
				{
					// get to next line
					input = reader.readLine();
				}
				else
				{
					int grid_num = Integer.parseInt(input);
					Coordinate[] coord_list = new Coordinate[2];
					coord_list[0] = new Coordinate(reader.readLine());
					coord_list[1] = new Coordinate(reader.readLine());
					meta_grid.put(grid_num, coord_list);
					input = reader.readLine();
				}
			}
		}
		catch (IOException e)
		{
			System.err.println("error: cannot read the file");
			return;
		}
		System.out.println("meta grid spec loaded");
	}
	
	// load the meta grid graph
	public static void initMetaGridGraph()
	{
		System.out.println("loading meta grid map...");
		meta_grid_graph = new HashMap<Integer, Integer[]>();
		BufferedReader reader = null;
		try
		{
			reader = new BufferedReader(new FileReader(meta_grid_graph_file));
		}
		catch (FileNotFoundException e)
		{
			System.err.println("error: the meta grid graph file is not found\n" + e.toString());
			return;
		}
		 
		try
		{
			String input = reader.readLine();
			while (input != null)
			{
				if (input.length() == 0 || input.charAt(0) == '%')
				{
					// do nothing
				}
				else
				{
					String[] noder = input.split(":");
					String[] child = noder[1].split(",");
					Integer node = Integer.parseInt(noder[0].trim());
					Integer[] children = new Integer[4];
					children[0] = Integer.parseInt(child[0].trim());
					children[1] = Integer.parseInt(child[1].trim());
					children[2] = Integer.parseInt(child[2].trim());
					children[3] = Integer.parseInt(child[3].trim());
					meta_grid_graph.put(node, children);
				}
				input = reader.readLine();
			}
		}
		catch (IOException e)
		{
			System.err.println("error: loading the meta grid graph\n"
					+ e.toString());
		}
		System.out.println("meta grid map loaded");
	}
	
	// describe the change of angle
	public static int transAngle( int init_angle, int goal_angle )
	{	
		int[][] trans_table = 
		{
				{0, 90, 180, -90},
				{-90, 0, 90, -180},
				{-180, -90, 0, 90},
				{90, 180, -90, 0}
		};
		
		int x = -1, y = -1;
		switch ( init_angle )
		{
		case 0:
			x = 0;
			break;
		case 90:
			x = 1;
			break;
		case 180:
			x = 2;
			break;
		case -90:
			x = 3;
			break;
		default:
			break;
		}
		
		switch ( goal_angle )
		{
		case 0:
			y = 0;
			break;
		case 90:
			y = 1;
			break;
		case 180:
			y = 2;
			break;
		case -90:
			y = 3;
			break;
		default:
			break;
		}
		
		return trans_table[x][y];
	}
	
	
	// search the meta grid graph for path planning
	// return the path from source to destination
	public static Integer[] search(Integer start, Integer goal)
	{
		//Integer[] path = new Integer[100];
		if ( meta_grid_graph == null )
			initMetaGridGraph();
		
		//Map<Integer, Boolean> expanded = new HashMap<Integer>
		LinkedList<Integer> visited = new LinkedList<Integer>();
		LinkedList<Integer> parent = new LinkedList<Integer>();
		visited.add(start);
		parent.add(-1);
		int curr_idx = 0;
		
		while ( true )
		{
			if ( curr_idx == visited.size() )
			{
				System.out.println("No");
				return null;
			}
			Integer node = visited.get(curr_idx);
			if ( node == goal )
				break;
			
			Integer[] children = meta_grid_graph.get(node);
			Integer vater = ((curr_idx == 0)? start : visited.get(parent.get(curr_idx)));
			for ( int i=0; i<4; ++i )
			{
				if ( children[i] != -1 && children[i] != vater )
				{
					visited.add(children[i]);
					parent.add(curr_idx);
				}
			}
			++curr_idx;
		}
		
		Integer curr_vater = null;
		LinkedList<Integer> path_finder = new LinkedList<Integer>();
		while ( curr_idx != -1 )
		{
			curr_vater = visited.get(curr_idx);
			path_finder.push(curr_vater);
			curr_idx = parent.get(curr_idx);
		}
		
		Integer[] path = new Integer[path_finder.size()];
		for ( int i=0; i<path_finder.size(); ++i )
		{
			path[i] = path_finder.get(i);
			System.out.print(path_finder.get(i) + " ");
		}
		System.out.println();
		
		return path;
	}
	
	/**
	 * get the meta grid number for a small grid specified by the coord
	 * 
	 * @param coord
	 * @return the meta grid number
	 */
	public static int getGridNum( Coordinate coord )
	{
		if (meta_grid == null)
			initMetaGrid();

		for (int grid_num : meta_grid.keySet())
		{
			Coordinate[] boundary = meta_grid.get(grid_num);
			if (coord.inRect(boundary[0], boundary[1]))
			{
				return grid_num;
			}
		}
		return -1;
	}

	// test whether this coordinate is inside the rectangle created by
	// the two input coordinate, return true if the answer is yes
	public boolean inRect( Coordinate coord1, Coordinate coord2 )
	{
		if (x >= coord1.x && y >= coord1.y && x <= coord2.x && y <= coord2.y)
			return true;
		else
			return false;
	}
	
	// get an random small grid from within a meta grid
	public static Coordinate getRandCoord( Integer meta_num )
	{
		Coordinate coord = new Coordinate();
		Coordinate[] boundary = meta_grid.get(meta_num);
		int x_dist = boundary[1].x - boundary[0].x;
		int y_dist = boundary[1].y - boundary[0].y;
		
		coord.x = boundary[0].x + (int)(x_dist * Math.random());
		coord.y = boundary[0].y + (int)(y_dist * Math.random());
  		
		return coord;
	}

	public Coordinate()
	{
		x = 0;
		y = 0;
	}

	public Coordinate(int inX, int inY)
	{
		x = inX;
		y = inY;
	}

	public Coordinate(Coordinate other)
	{
		x = other.x;
		y = other.y;
	}

	public Coordinate(String coord)
	{
		String[] xy = coord.split(",");
		x = Integer.parseInt(xy[0].substring(4));
		y = Integer.parseInt(xy[1].substring(5));
	}

	public static int getDirection( Vector<Coordinate> coords )
	{
		return 1;
	}

	public void setXY( int inX, int inY )
	{
		x = inX;
		y = inY;
	}

	// return the distance in meters
	public static double distance( Coordinate a, Coordinate b )
	{
		double dist_x = Math.abs(a.x - b.x);
		double dist_y = Math.abs(a.y - b.y);
		return Math.sqrt((dist_x * dist_x + dist_y * dist_y)) * 0.5;
	}

	public boolean equals( Coordinate other )
	{
		return (this.x == other.x && this.y == other.y);
	}

	public String toString()
	{
		return "x = " + x + ", y = " + y;
	}

	public static void main( String[] args )
	{
		String coord_str = "x = 18, y = 32";
		Coordinate coord = new Coordinate(coord_str);

		System.out.println(coord_str);
		System.out.println(coord.toString());
		System.out.println(coord.x + " " + coord.y);

		Coordinate.initMetaGrid();
		
		Coordinate.search(9, 14);
		System.out.println(Coordinate.getGridNum(coord));
	}
}
