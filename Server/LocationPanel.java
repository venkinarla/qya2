package Server;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
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
				parentPanel.OffEndMonitor();
				parentPanel.OnStartMonitor();
			}
		});
		buttonEnd = new JButton("Set Destination");
		buttonEnd.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked( MouseEvent e )
			{
				parentPanel.OffStartMonitor();
				parentPanel.OnEndMonitor();
			}
		});
		buttonGo = new JButton("Go");
		buttonGo.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked( MouseEvent e )
			{
				out.println("BYPASS");
				parentPanel.setMovePath(new Coordinate(startx, starty),
						new Coordinate(endx, endy));
				parentPanel.setMove(true);
			}
		});

		setSize(200, 400);
		setLayout(new BorderLayout());
		// A temp panel that store the buttons and textfields
		JPanel tempPanel = new JPanel();
		tempPanel.setLayout(new GridLayout(5, 1, 10, 10));
		tempPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLoweredBevelBorder(), "Start / End"));
		
		tempPanel.add(buttonStart);
		textStart.setColumns(5);
		tempPanel.add(textStart);
		
		tempPanel.add(buttonEnd);
		textEnd.setColumns(5);
		tempPanel.add(textEnd);

		tempPanel.add(buttonGo);

		//Add the LabelPanel and tempPanel to the layout
		add(new LabelPanel(), BorderLayout.NORTH);
		add(tempPanel, BorderLayout.CENTER);
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
		myUI.setSize(150, 300);
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
		}));
		myUI.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		myUI.show();
	}

	public void setSTxy( int x, int y ) //Display the X and Y value of the starting point
	{
		startx = x;
		starty = y;
		textStart.setText("X:" + String.valueOf(startx) + "," + "Y:" + String.valueOf(starty));
	}

	public void setENDxy( int x, int y ) //Display the X and Y value of the starting point
	{
		endx = x;
		endy = y;
		textEnd.setText("X:" + String.valueOf(endx) + "," + "Y:" + String.valueOf(endy));
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
			setLayout(new GridLayout(3, 1));
			setSize(150, 100);
			add(new JLabel("          NXT Location"));
			add(new JLabel("          Start Location"));
			add(new JLabel("          End Location"));
		}
		public void paintComponent( Graphics g )
		{
			super.paintComponent(g);
			g.setColor(Color.RED);
			g.fillOval(10, 23, PointSize, PointSize);
			g.setColor(Color.green);
			g.fillOval(10, 39, PointSize, PointSize);
			g.setColor(Color.blue);
			g.fillOval(10, 55, PointSize, PointSize);
		}
	}
}