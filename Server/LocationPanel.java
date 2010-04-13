package Server;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.PrintWriter;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import State.Coordinate;

public class LocationPanel extends JPanel
{
	private static final long serialVersionUID = -2793372883170917905L;
	private int startx, starty, endx, endy;				// x,y value of the starting point and ending point
	private JTextField textStart, textEnd;				// Textfields that display the x, y value			
	private JButton buttonStart, buttonEnd, buttonGo;	// Buttons
	private MapControl parentPanel;						// The main map panel
	public PrintWriter out;								// Output stream used to bypass readln

	public LocationPanel(MapControl InParent)
	{
		parentPanel = InParent;
		textStart = new JTextField("");
		textStart.setHorizontalAlignment(JTextField.CENTER);
		textEnd = new JTextField("");
		textEnd.setHorizontalAlignment(JTextField.CENTER);
		buttonStart = new JButton("Set Starting Point");
		buttonStart.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked( MouseEvent e )
			{
				if (!buttonStart.isEnabled())
					return ;				
				parentPanel.OffEndMonitor();
				parentPanel.OnStartMonitor();
			}
		});
		buttonEnd = new JButton("Set Destination");
		buttonEnd.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked( MouseEvent e )
			{
				if (!buttonEnd.isEnabled())
					return ;
				parentPanel.OffStartMonitor();
				parentPanel.OnEndMonitor();
			}
		});
		buttonGo = new JButton("Go");
		buttonGo.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked( MouseEvent e )
			{
				if (!buttonGo.isEnabled())
					return ;
				out.println("BYPASS");
				parentPanel.setMovePath(new Coordinate(startx, starty),
						new Coordinate(endx, endy));
				parentPanel.setMove(true);
				parentPanel.started();
				buttonStart.setEnabled(false);
				buttonEnd.setEnabled(false);
				buttonGo.setEnabled(false);
			}
		});

		setSize(200, 400);
		setLayout(new BorderLayout());
		// A temp panel that store the buttons and textfields
		JPanel tempPanel = new JPanel();
		tempPanel.setLayout(new GridLayout(5, 1, 10, 10));
		tempPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLoweredBevelBorder(), "Control"));
		
		tempPanel.add(buttonStart);
		textStart.setColumns(5);
		tempPanel.add(textStart);
		
		tempPanel.add(buttonEnd);
		textEnd.setColumns(5);
		tempPanel.add(textEnd);

		tempPanel.add(buttonGo);

		//Add the LabelPanel and tempPanel to the layout
		add(new LabelPanel(), BorderLayout.NORTH);
		add(new JLabel(" "), BorderLayout.CENTER);
		add(tempPanel, BorderLayout.SOUTH);
	}

	public void disableALL()//Disable all buttons
	{
		buttonStart.setEnabled(false);
		buttonEnd.setEnabled(false);
		buttonGo.setEnabled(false);
		textStart.setEditable(false);
		textEnd.setEditable(false);
	}

	public void enableALL() //Enable all buttons
	{
		buttonStart.setEnabled(true);
		buttonEnd.setEnabled(true);
		buttonGo.setEnabled(true);
		textStart.setEditable(true);
		textEnd.setEditable(true);
	}

	public static void main( String[] args )
	{
		JFrame myUI = new JFrame();
		myUI.setTitle("Location Panel");
		myUI.setSize(170, 370);
		myUI.add(new LocationPanel(new MapControl()
		{
			public void OffEndMonitor()
			{
			}

			public void OffStartMonitor()
			{
			}

			public void OnEndMonitor()
			{
			}

			public void OnStartMonitor()
			{
			}

			public void setMovePath( Coordinate Start, Coordinate End )
			{
			}
			
			public void setMove( boolean flag )
			{
			}

			public void started() {
				
			}
		}));
		myUI.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		myUI.show();
	}

	public void setSTxy( int x, int y ) //Display the X and Y value of the starting point
	{
		startx = x;
		starty = y;
		Coordinate Coor = new Coordinate();
		Coor.x = startx;
		Coor.y = starty;
		int cell = Coordinate.getGridNum(Coor);
		textStart.setText("X:" + String.valueOf(startx) + "," + "Y:" + String.valueOf(starty) + " Cell:" + cell);
	}

	public void setENDxy( int x, int y ) //Display the X and Y value of the starting point
	{
		endx = x;
		endy = y;
		Coordinate Coor = new Coordinate();
		Coor.x = endx;
		Coor.y = endy;
		int cell = Coordinate.getGridNum(Coor);
		textEnd.setText("X:" + String.valueOf(endx) + "," + "Y:" + String.valueOf(endy) + " Cell:" + cell);
	}
	
	public void cleanText() // Remove the displayed value of two textfield.
	{
		textStart.setText("");
		textEnd.setText("");
	}	

	class LabelPanel extends JPanel //Panel including the start, end and go button
	{
		private int PointSize;
		LabelPanel()
		{
			PointSize = 15;
			setBorder(BorderFactory.createTitledBorder(BorderFactory.createLoweredBevelBorder(), "Label"));
			setLayout(new GridLayout(6, 1));
			setSize(170, 100);
			add(new JLabel("        : Estimated Location"));
			add(new JLabel("        : Expected Location"));
			add(new JLabel("        : Starting Location"));
			add(new JLabel("        : Destination"));
			add(new JLabel("        : Possible Blockage"));
			add(new JLabel("        : Traveling Path"));
		}
		public void paintComponent( Graphics g )
		{
			super.paintComponent(g);
			g.setColor(Color.MAGENTA);
			g.fillOval(10, 23, PointSize, PointSize);			
			g.setColor(Color.RED);
			g.fillOval(10, 41, PointSize, PointSize);
			g.setColor(Color.green);
			g.fillOval(10, 59, PointSize, PointSize);
			g.setColor(Color.blue);
			g.fillOval(10, 77, PointSize, PointSize);
			g.setColor(Color.RED);
			g.drawOval(10, 95, PointSize, PointSize);
			g.drawOval(12, 97, PointSize-4, PointSize-4);
			g.drawOval(14, 99, PointSize-8, PointSize-8);
			g.setColor(Color.orange);
			g.fillOval(10, 118, PointSize - 10, PointSize - 10);
			g.fillOval(15, 118, PointSize - 10, PointSize - 10);
			g.fillOval(20, 118, PointSize - 10, PointSize - 10);
		}
	}
}