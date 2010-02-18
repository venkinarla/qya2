package Data;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Scanner;


public class meta_grid_graph_generation {
	//************** data member ***************
	private byte[][] grid;
	int max_x;
	int max_y;
	public static int[][] meta_grid;
	public static Map<Integer, Integer[]> meta_grid_graph;
	
	// load the grid definition to the system
	// grid map defines the basic environment which consists arrays of small 50\times 50 grids
	public static String grid_file =  "src/dataset/grid_map.gm";  
 	
	//************** class methods *************
	public meta_grid_graph_generation()
	{
		File f=new File(".");
		try {
			System.out.println(f.getCanonicalPath());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		grid = new byte[500][];
		meta_grid = new int[500][];
		for ( int i=0; i<grid.length; ++i )
		{
			grid[i] = new byte[500];
			meta_grid[i] = new int[500];
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
		
		int curr_row = 1;
		int curr_col = 1;
		int row_count = 0;
		int num_grid = 0;
		while( fin.hasNextLine() )
		{
			String curr_line = fin.nextLine();
			int col_count = 0;
			for ( int i=0; i<curr_line.length(); ++i )
			{
				char ch = curr_line.charAt(i);
				System.out.println(ch);
				if ( (ch != '0') && (ch != '1') )
					continue;
				++col_count;
				grid[curr_row][curr_col] = (byte)(( ch == '1' ) ? 1 : 2 );
				if( ch == '0' ) 
					num_grid++;
				++curr_col;
			}
			max_x = col_count;
			curr_col = 1;
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
		System.out.println("Num grid = "+num_grid);
	}
	
	public void BuildMetaGraph(){
		FileOutputStream fos; 
		DataOutputStream dos;
		Integer grid_count=0;
	    try {
	    	File file= new File("src/dataset/meta_grid_graph_test");
	    	fos = new FileOutputStream(file);
		    dos=new DataOutputStream(fos);
		    for( int col=max_x+1; col>0; col-- ){
		    	for( int rol=1; rol<max_y+1; rol++ ){
		    		if( grid[rol][col]==(byte)2 ){
		    			grid_count++;
		    			meta_grid[rol][col] = grid_count;
		    		}
		    	}
		    }
		    System.out.println(grid_count);
		    System.out.println(max_x +" "+max_y);
		    for( int col=max_x+1; col>0; col-- ){
		    	for( int rol=1; rol<max_y+1; rol++ ){
		    		if( meta_grid[rol][col]==0 )
		    			continue;
		    		int currGrid = meta_grid[rol][col];
		    		int[] neighbor = new int[4];
		    		neighbor[0] = meta_grid[rol][col+1];
		    		neighbor[1] = meta_grid[rol+1][col];
		    		neighbor[2] = meta_grid[rol][col-1];
		    		neighbor[3] = meta_grid[rol-1][col];
		    		for( int i=0; i<4; i++ ){
		    			if( neighbor[i] == 0 )
		    				neighbor[i] = -1;
		    		}
		    		dos.writeBytes(currGrid+": "+neighbor[0]+", "
		    						+neighbor[1]+", "+neighbor[2]+", "+neighbor[3]+"\n");
		    	}
		    }

		    } catch (IOException e) {
		      e.printStackTrace();
		    }

		
	}
	
	//For Testing
	//Printing the grid map read from the grid_map.gm on command line
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
	
	//For Testing
	//Printing the grid map read from the grid_map.gm to another file
	//named grid_test
	public void printGridToFile()
	{
		FileOutputStream fos; 
		DataOutputStream dos;
	    try {
	    	File file= new File("src/dataset/grid_test");
	    	fos = new FileOutputStream(file);
		    dos=new DataOutputStream(fos);
		    
			if ( grid == null )
				return;
			
			int max_x = this.max_x;
			int max_y = this.max_y;
			
			for ( int i=1; i<max_y+1; ++i )
			{
				for ( int j=1; j<max_x+1; ++j )
				{
					dos.writeBytes(grid[i][j]+"");
				}
				dos.writeBytes("\n");
			}
	    }
	    catch (IOException e) {
		      e.printStackTrace();
	    }
	}
	
	public void printMetaGridToFile()
	{
		FileOutputStream fos; 
		DataOutputStream dos;
	    try {
	    	File file= new File("src/dataset/meta_grid_test");
	    	fos = new FileOutputStream(file);
		    dos=new DataOutputStream(fos);
		    
			if ( grid == null )
				return;
			
			int max_x = this.max_x;
			int max_y = this.max_y;
			
			for ( int i=1; i<max_y+1; ++i )
			{
				for ( int j=1; j<max_x+1; ++j )
				{
					dos.writeBytes(String.format("%03d",meta_grid[i][j])+" ");
				}
				dos.writeBytes("\n");
			}
	    }
	    catch (IOException e) {
		      e.printStackTrace();
	    }
	}
	
	public static void main(String[] args){
		meta_grid_graph_generation mggg = new meta_grid_graph_generation();
		mggg.loadGrid();
		mggg.printGridToFile();
		mggg.BuildMetaGraph();
		mggg.printMetaGridToFile();
	}
}
