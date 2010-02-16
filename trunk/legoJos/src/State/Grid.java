package State;

import java.io.*;
import java.util.*;
import java.math.*;


public class Grid
{	
	//************** data member ***************
	private byte[][] grid;
	int max_x;
	int max_y;
	
	// load the grid definition to the system
	// grid map defines the basic environment which consists arrays of small 50\times 50 grids
	public static String grid_file =  "src/dataset/grid_map.gm";  
 	
	//************** class methods *************
	public Grid()
	{
		File f=new File(".");
		try {
			System.out.println(f.getCanonicalPath());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		grid = new byte[500][];
		for ( int i=0; i<grid.length; ++i )
		{
			grid[i] = new byte[500];
		}
		max_x = 0;
		max_y = 0;
	}
	
	public byte[][] getGridMap()
	{
		return grid;
	}
	
	public void loadGrid()
	{
		Scanner fin = null;
		try
		{
			fin = new Scanner(new FileInputStream(grid_file));
		}
		catch (FileNotFoundException e)
		{
			System.err.println("grid file loading error: cannot open file " + grid_file);
		}
		
		int curr_row = 0;
		int curr_col = 0;
		int row_count = 0;
		while( fin.hasNextLine() )
		{
			String curr_line = fin.nextLine();
			int col_count = 0;
			for ( int i=0; i<curr_line.length(); ++i )
			{
				char ch = curr_line.charAt(i);
				if ( (ch != '0') && (ch != '1') )
					continue;
				++col_count;
				grid[curr_row][curr_col] = (byte)(( ch == '1' ) ? 1 : 0 );
				++curr_col;
			}
			max_x = col_count;
			curr_col = 0;
			++curr_row;
			++row_count;
		}
		max_y = row_count;
		if ( grid.length > 0 )
			System.out.printf("grid map loaded, max_x = %d, max_y = %d\n", 
					max_x, max_y);
		else
		{
			System.err.println("grid file loading error: grid corrupted");
			grid = null;
		}
	}
	
	public void printGrid()
	{
		if ( grid == null )
			return;
		
		int max_x = grid[0].length;
		int max_y = grid.length;
		
		for ( int i=0; i<max_y; ++i )
		{
			for ( int j=0; j<max_x; ++j )
			{
				System.out.print(grid[i][j] + " ");
			}
			System.out.println();
		}
	}
	
	// bfs
	public Vector<Coordinate> search(Coordinate start, Coordinate goal)
	{
		if ( start.equals(goal) )
			return null;
		
		int max_x = grid[0].length;
		int max_y = grid.length;
		//Set<Coordinate> visited = new HashSet<Coordinate>();
		byte[][] visited = new byte[500][];
		for ( int i=0; i<500; ++i )
		{
			visited[i] = new byte[500];
		}
		LinkedList<Coordinate> queue = new LinkedList<Coordinate>();
		//visited.add(start);
		queue.add(start);
		Vector<Integer> parent = new Vector<Integer>(100);
		parent.add(-1);
		int curr_idx = 0;
		
		// search the result
		while ( true )
		{
			if ( curr_idx == queue.size() ) 
			{
				System.out.println("No");
				return null;
			}
			
			Coordinate curr_coord = queue.get(curr_idx);
			
			if ( curr_coord.equals(goal) )
			{
				System.out.println("Euryka");
				break;
			}
			
			int curr_x = curr_coord.x;
			int curr_y = curr_coord.y;
			
			// east
			if ( curr_x + 1 < max_x )
			{
				Coordinate novo_coord = new Coordinate(curr_x+1, curr_y); 
				if ( grid[curr_y][curr_x+1] == 0 && visited[curr_y][curr_x+1]==0 )
				{
					//queue.add(novo_coord);
					enqueue(novo_coord, start, goal, queue);
					parent.add(curr_idx);
					visited[curr_y][curr_x+1] = 1;
				}
			}
			
			// west
			if ( curr_x - 1 >= 0 )
			{
				Coordinate novo_coord = new Coordinate(curr_x-1, curr_y);
				if (grid[curr_y][curr_x - 1] == 0 && visited[curr_y][curr_x-1]==0)
				{
					//queue.add(novo_coord);
					enqueue(novo_coord, start, goal, queue);
					parent.add(curr_idx);
					visited[curr_y][curr_x-1] = 1;
				}
			}
			
			// north
			if ( curr_y - 1 >= 0 )
			{
				Coordinate novo_coord = new Coordinate(curr_x, curr_y-1);
				if (grid[curr_y - 1][curr_x] == 0 && visited[curr_y-1][curr_x]==0);
				{
					//queue.add(novo_coord);
					enqueue(novo_coord, start, goal, queue);
					parent.add(curr_idx);
					visited[curr_y-1][curr_x] = 1;
				}
			}
			
			// south
			if ( curr_y + 1 < max_y )
			{
				Coordinate novo_coord = new Coordinate(curr_x, curr_y+1);
				if (grid[curr_y + 1][curr_x] == 0 && visited[curr_y+1][curr_x]==0);
				{
					//queue.add(novo_coord);
					enqueue(novo_coord, start, goal, queue);
					parent.add(curr_idx);
					visited[curr_y+1][curr_x] = 1;
				}
			}
			
			++curr_idx;
		}
		
		// back track
		LinkedList<Integer> path_idx = new LinkedList<Integer>();
		int this_idx = curr_idx;
		while ( this_idx != -1 )
		{
			path_idx.push(this_idx);
			this_idx = parent.get(this_idx);
		}

		Vector<Coordinate> shortest_path = new Vector<Coordinate>();
		System.out.println("Shortest Path:");
		for ( int i=0; i<path_idx.size(); ++i )
		{
			System.out.println( queue.get(path_idx.get(i)) );
			shortest_path.add(new Coordinate(queue.get(path_idx.get(i))));
		}
		
		return shortest_path;
	}
	
	public static void enqueue(Coordinate coord, Coordinate start, 
			Coordinate goal, LinkedList<Coordinate>queue)
	{
		boolean flag = true;
		for ( int i=0; i<queue.size(); ++i )
		{
			if ( compare(coord, queue.get(i), start, goal) == 1 )
				continue;
			queue.add(i, coord);
			flag = false;
			break;
		}
		if ( flag )
		{
			queue.add(coord);
		}
	}
	
	public static int compare(Coordinate a, Coordinate b, Coordinate start, Coordinate goal)
	{
		int dist_a = Math.abs(a.x - goal.x) + Math.abs(a.y - goal.y)
		+ Math.abs(a.x - start.x) + Math.abs(a.y - start.y);
		int dist_b = Math.abs(b.x - goal.x) + Math.abs(b.y - goal.y) +
		Math.abs(b.x - start.x) + Math.abs(b.y - start.y);
		
		return ( dist_a < dist_b ) ? -1 : 1;
	}
	
	
	public static void main(String[] args)
	{
		Grid mapper = new Grid();
		mapper.loadGrid();	
	}

}
