package Data;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Scanner;

public class MetaGridMgrGeneration {
	// ************** data member ***************
	private byte[][] grid;
	int max_x;
	int max_y;
	public static int[][] meta_grid;
	public static Map<Integer, Integer[]> meta_grid_graph;
	public static String grid_file = "src/dataset/grid_map.gm";

	// ************** class methods *************
	public MetaGridMgrGeneration() {
		File f = new File(".");
		try {
			System.out.println(f.getCanonicalPath());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		grid = new byte[500][];
		meta_grid = new int[500][];
		for (int i = 0; i < grid.length; ++i) {
			grid[i] = new byte[500];
			meta_grid[i] = new int[500];
		}
		max_x = 0;
		max_y = 0;
	}

	public byte[][] getGridMap() {
		return grid;
	}

	public void loadGrid() {
		Scanner fin = null;
		try {
			fin = new Scanner(new FileInputStream(grid_file));
		} catch (FileNotFoundException e) {
			System.err.println("grid file loading error: cannot open file "
					+ grid_file);
		}

		int curr_row = 1;
		int curr_col = 1;
		int row_count = 0;
		int num_grid = 0;
		while (fin.hasNextLine()) {
			String curr_line = fin.nextLine();
			int col_count = 0;
			for (int i = 0; i < curr_line.length(); ++i) {
				char ch = curr_line.charAt(i);
				if ((ch != '0') && (ch != '1'))
					continue;
				++col_count;
				grid[curr_row][curr_col] = (byte) ((ch == '1') ? 1 : 2);
				if (ch == '0')
					num_grid++;
				++curr_col;
			}
			max_x = col_count;
			curr_col = 1;
			++curr_row;
			++row_count;
		}
		max_y = row_count;
		if (grid.length > 0)
			System.out.printf("grid map loaded, max_x = %d, max_y = %d\n",
					max_x, max_y);
		else {
			System.err.println("grid file loading error: grid corrupted");
			grid = null;
		}
		System.out.println("Num grid = " + num_grid);
	}

	// Output the meta_grid_mgr file
	// based on the grid_map_gm file
	public void BuildMetaMgrGraph() {
		FileOutputStream fos;
		DataOutputStream dos;
		Integer grid_count = 0;
		try {
			File file = new File("src/dataset/meta_grid.mgr");
			fos = new FileOutputStream(file);
			dos = new DataOutputStream(fos);
			dos.writeBytes("%This meta file indicate the mapping between the grids on map and coordinates\n");
			dos.writeBytes("%Format:\n");
			dos.writeBytes("%\t\tGrid Number\n%\t\tx = _, y = _\n\n");
			for (int col = max_x + 1; col > 0; col--) {
				for (int rol = 1; rol < max_y + 1; rol++) {
					if (grid[rol][col] == (byte) 2) {
						grid_count++;
						dos.writeBytes(grid_count + "\n" + "x = " + (col-1)
								+ ", y = " + (rol-1) + "\n\n");
					}
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static void main(String[] args) {
		MetaGridMgrGeneration mgmg = new MetaGridMgrGeneration();
		mgmg.loadGrid();
		mgmg.BuildMetaMgrGraph();
	}
}
