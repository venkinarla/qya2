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
	public static final int correctangle = 0;
	public int x, y;
	
	// the grid data files
	// these files are loaded to the system
	public static String meta_grid_file = 
		"src/dataset/meta_grid.mgr";        // .mgr defines maps the smallest grids to meta grids
	public static String meta_grid_graph_file = 
		"src/dataset/meta_grid_graph.mgh";  // .mgh defines the relationship between each pairs of meta grids
	
	// map from meta grid to original small coordinate
	public static Map<Integer, Coordinate> meta_grid;
	// the graph representing the relationship between each meta grid
	public static Map<Integer, Integer[]> meta_grid_graph;

	// initialize the meta grid information from a meta grid definition
	public static void initMetaGrid()
	{
		// get the data from file
		meta_grid = new HashMap<Integer, Coordinate>();
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
					Coordinate coord_list = new Coordinate(reader.readLine());
					//coord_list[0] = new Coordinate(reader.readLine());
					//coord_list[1] = new Coordinate(reader.readLine());
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
					System.out.println(node+":"+ children[0]+":"+ children[1]+ ":"+ children[2]+":"
								+ children[3]);
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
	
	public static int transAngle( int init_angle, int goal_angle )
	// Return the angle that used to turn the robot. 
	{	
		// PS. Extra 7 degree due to the carpet on 3rd floor.
		if (init_angle == 0)
		{
			if (goal_angle == 90)
				return (90+correctangle);
			else if (goal_angle == 180)
				return (180+correctangle*2);
			else if (goal_angle == -90)
				return -(90+correctangle);
		}
		else if (init_angle == 90)
		{
			if (goal_angle == 0)
				return -(90+correctangle);
			else if (goal_angle == 180)
				return (90+correctangle);
			else if (goal_angle == -90)
				return (180+correctangle*2);
		}
		else if (init_angle == 180)
		{
			if (goal_angle == 0)
				return (180+correctangle*2);
			else if (goal_angle == 90)
				return -(90+correctangle);
			else if (goal_angle == -90)
				return (90+correctangle);
		}
		else if (init_angle == -90)
		{
			if (goal_angle == 0)
				return (90+correctangle);
			else if (goal_angle == 90)
				return (180+correctangle*2);
			else if (goal_angle == 180)
				return -(90+correctangle);
		}
		return 999;
	}
	
	
	// search the meta grid graph for path planning
	// return the path from source to destination
	@SuppressWarnings("unchecked")
	public static Integer[] search(Integer start, Integer goal)
	{
		//Integer[] path = new Integer[100];
		if ( meta_grid_graph == null )
			initMetaGridGraph();
		
		
		Integer numGrid = meta_grid_graph.size(); // number of grids on map
		Integer[] color = new Integer[numGrid+1]; // 0:not discoverd; 1:discoverd; 2:finished
		Integer[] pred = new Integer[numGrid+1];
		Integer[] children = meta_grid_graph.get(start);
		Integer[] dTime = new Integer[numGrid+1]; // discovered time
		//Integer[] minStep = new Integer[numGrid+1];
		
		for( int i=0; i<numGrid+1; i++ ){
			color[i] = 0;
			pred[i] = -1;
			dTime[i] = 999;
			//minStep[i] = 999;
		}
		color[start] = 1;
		dTime[start] = 0;
		//minStep[goal] = 0;
		
		//BFS 
		Integer u;
		Boolean found = false;
		Queue q = new LinkedList() ;
		u = start;
		q.add(start);
		while(true){
			if( found ) break;//path found
			
			u = (Integer) q.remove();
			color[u]=1;
			children = meta_grid_graph.get(u);
			for(int v: children ){
				if( v == -1 ) 
					continue;
				if( v == goal ){
					dTime[v] = dTime[u]+1;
					pred[v] = u;
					found = true;
					System.out.println("Reach "+v+" in "+dTime[v]+" steps");
					break;
				}
				if( color[v]==0 ){
					pred[v] = u;
					dTime[v] = dTime[u]+1;
					q.add(v);
				}
			}
		}
		/*for( int i=0; i<children.length; i++ ){
			
			if( children[i] == -1 )
				continue;
			
			pred[children[i]] = start;
			if( color[children[i]] == 0 ){
				RecursiveDFS(children[i], goal, color, pred, dTime, minStep);
				if( minStep[children[i]]+1 < minStep[start])
					minStep[start] = minStep[children[i]]+1;
			}
			else if( color[children[i]]==2 ){
				if( dTime[start]+1 < dTime[children[i]]){
					dTime[children[i]] = 1;
					pred[children[i]] = start;
				}
				if( minStep[children[i]]+1 < minStep[start] ){
					minStep[start] = minStep[children[i]]+1;
				}
			}
			else continue;
			
		}*/
		if( pred[goal]==-1 )
			return null;
		else{
			Integer[] path = new Integer[dTime[goal]+1];
			Integer currNode = goal;
			path[path.length-1] = goal;
			
			for( int i=path.length-2; i>=0; i-- ){
				path[i] = pred[currNode];
				currNode = path[i];
			}
			for( int i=0; i<path.length; i++ )
				System.out.print(path[i]+" ");
			System.out.println();
			return path;
		}
	}
	
/*	public static Integer RecursiveDFS(Integer u, Integer v, Integer[] color, Integer[] pred
										,Integer[] dTime, Integer[] minStep){
		
		color[u] = 1;
		dTime[u] = dTime[pred[u]]+1;
		Integer[] children = meta_grid_graph.get(u);
		for( int i=0; i<children.length; i++ ){
			
			if( children[i]==-1 )
				continue;
			if( children[i]==v ){
				System.out.println("reach pred:"+u+" dTime "+dTime[u]);
				if( dTime[u]+1 < dTime[v] ){
					dTime[v] = dTime[u]+1;
					pred[v] = u;
					minStep[u] = 1;
					return minStep[u];
				}
			}
			if( color[children[i]]==0 ){
				pred[children[i]] = u;
				Integer tempMinStep = RecursiveDFS(children[i], v, color, pred, dTime, minStep);
				if( tempMinStep+1 < minStep[u] )
					minStep[u] = tempMinStep+1;
			}
			else if( color[children[i]]==2 ){
				if( dTime[u]+1 < dTime[children[i]]){
					dTime[children[i]] = dTime[u]+1;
					pred[children[i]] = u;
				}
				if( minStep[children[i]]+1< minStep[u]){
					minStep[u] = minStep[children[i]]+1;
					pred[children[i]] = u;
					dTime[children[i]] = dTime[u]+1;
				}
			}
		}
		color[u] = 2;
		return minStep[u];	
	}
*/
	
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
			Coordinate boundary = meta_grid.get(grid_num);
			/*if (coord.inRect(boundary[0], boundary[1]))
			{
				return grid_num;
			}*/
			if( boundary.x == coord.x && boundary.y == coord.y)
				return grid_num;
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
		Coordinate boundary = meta_grid.get(meta_num);
		//int x_dist = boundary[1].x - boundary[0].x;
		//int y_dist = boundary[1].y - boundary[0].y;
		
		//coord.x = boundary[0].x + (int)(x_dist * Math.random());
		//coord.y = boundary[0].y + (int)(y_dist * Math.random());
  		
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

	//utility
	public static void printPath( Integer[] a, Integer b, Integer step){
		Integer[] c = new Integer[step];
		for( int i=0; i<c.length; i++ )
			c[i] = 0;
		
		c[c.length-1] = b;
		for( int i=c.length-2; i>=0; i-- ){
			c[i] = a[b];
			b = c[i];
		}
		for( int i=0; i<c.length; i++ ){
			System.out.print(c[i]+" ");
		}
		System.out.println();
	}
	
	public static void main( String[] args )
	{
		String coord_str = "x = 18, y = 32";
		Coordinate coord = new Coordinate(coord_str);

		System.out.println(coord_str);
		System.out.println(coord.toString());
		System.out.println(coord.x + " " + coord.y);

		Coordinate.initMetaGrid();
		
		Coordinate.search(5, 15);
		System.out.println("Get Grid Num :"+Coordinate.getGridNum(coord));
	}
}
