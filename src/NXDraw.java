import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.JColorChooser;
import javax.swing.JOptionPane;
import javax.swing.Timer;
import javax.swing.UnsupportedLookAndFeelException;

import java.io.*;

/**
 * Implementation of NDraw
 * 
 * @author Chris Granville 
 * @version 3.1
 */

public class NXDraw extends JFrame {
    private static final boolean NIMBUS = true;
	private static final long serialVersionUID = 1L;
	// Window
    private final int CANVAS_HEIGHT = 800;								// Canvas height
    private final int CANVAS_WIDTH = 640;								// Canvas width
    private final int CP_WIDTH = 200;									// Control panel width
    private final int MA_HEIGHT = 100;									// Message area height
    private final String WINDOW_TITLE = "NDraw";						// Window title
   
    // Preferences
    private final boolean DEBUG = false;								// Do we want to display debugging info?
    private final int COARSE_LINES_WIDTH = 50;							// Coarse line width
	private final int FINE_LINE_WIDTH = 10;								// Fine line width

	// Freehand drawing
	int fhand_thickness = 1;											// Get the value of the thickness slider
	private int fhand_count = 0;										// Make sure we don't go over the limit of the below arrays
	private final int MAX_FHAND = 100000;								// Max. no of freehand squares
	private Color[] fhand_color = new Color[MAX_FHAND];					// Hold color of each square
	private int[][] fhand_xy = new int[MAX_FHAND][3];					// Position and size of each square
	private Color selected_color = new Color(0.0f, 0.0f, 0.0f);			// Initial color. Currently black
	private int fh_left = MAX_FHAND;									// How many freehand drawings can we still do?

	// Drawing tools - rectangle, ovals and lines
	private final int MAX_RECTS = 10;									// Maximum number of rectangles
	private final int MAX_OVALS = 10;									// Maximum number of ovals
	private final int MAX_LINES = 10;									// Maximum number of lines

	private int[][] rect_xy = new int[MAX_RECTS][4];					// x1, y1, x2 and y2 for rectangles
	private Color[] rect_color = new Color[MAX_RECTS];					// Line color of rectangles
	private int rect_count = 0;											// Number of rectangles so far

	private int[][] oval_xy = new int[MAX_OVALS][4];					// x1, y1, x2 and y2 for ovals
	private Color[] oval_color = new Color[MAX_OVALS];					// Line color of ovals
	private int oval_count = 0;											// Number of ovals so far
	
	private int[][] line_xy = new int[MAX_LINES][4];					// x1, y1, x2 and y2 for lines
	private Color[] line_color = new Color[MAX_LINES];					// Line color of lines
	private int line_count = 0;											// Number of lines so far

	private char curr_dtool_mode = 'l';									// Current mode for the drawing tool
	
	// Instance declarations
	private Canvas canvas;
	private Cursor canvasCursor;
	private JPanel controlPanel;
	private JLabel coordsLabel;
	private JRadioButton lineRadioButton, ovalRadioButton, rectangleRadioButton, freehandRadioButton;
	private JSlider freehandSizeSlider;
	private JCheckBox fineCheckBox, coarseCheckBox;
	private JButton colourButton, clearButton, animateButton;
	private JTextArea messageArea;
	private ObjectOutputStream objectOut;
	private ObjectInputStream objectIn;
	private JFileChooser fileChooser = new JFileChooser();
	private File file;
	
	// -------------------------------------------------------------------------------------------------------------------------------------
	
	class Canvas extends JPanel
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public void paintComponent(Graphics gfx)
		{
			((Graphics2D)gfx).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); 
			super.paintComponent(gfx);
			draw(gfx);
		}
		
		public void draw(Graphics gfx)
		{
			int canvasHeight = getHeight();
			int canvasWidth = getWidth();

			// Small lines
			if( fineCheckBox.isSelected() )
			{
				gfx.setColor(new Color(0.8F, 0.8F, 0.8F));
				for(int i = 0; i < canvasHeight; i += FINE_LINE_WIDTH)
				{
					gfx.drawLine(0,i, canvasWidth, i);
				}
				for(int i = 0; i < canvasWidth; i += FINE_LINE_WIDTH)
				{
					gfx.drawLine(i, 0, i, canvasHeight);
				}
			}
			
			// Thick lines
			if( coarseCheckBox.isSelected() )
			{
			   gfx.setColor(new Color(0.6F, 0.6F, 0.6F));
			   for(int i = 0; i < canvasHeight; i += COARSE_LINES_WIDTH)
			   {
				   gfx.drawLine(0,i, canvasWidth, i);
				} 

				for(int i = 0; i < canvasWidth; i += COARSE_LINES_WIDTH)
				{
					gfx.drawLine(i, 0, i, canvasHeight);
				} 
			}
			
			// Freehand, for every pixel...
			for ( int i = 0; i < fhand_count; i++ )
			{
				gfx.setColor(fhand_color[i]);
				int width_height = fhand_xy[i][2];
				int offsetX = fhand_xy[i][0] - (width_height / 2);
				int offsetY = fhand_xy[i][1] - (width_height / 2);
				gfx.fillOval(offsetX, offsetY, width_height, width_height);
			}
   
			// Drawing tools
			// First, rectanges
			for( int i = 0; i <= rect_count && rect_count < MAX_RECTS; i++ )
			{
				gfx.setColor(rect_color[i]);
				gfx.drawRect(Math.min(rect_xy[i][0],rect_xy[i][2]), Math.min(rect_xy[i][1], rect_xy[i][3]), Math.abs(rect_xy[i][0] - rect_xy[i][2]), Math.abs(rect_xy[i][1] - rect_xy[i][3]));
			}
			
			// Ovals
			for( int i = 0; i <= oval_count && oval_count < MAX_OVALS; i++ )
			{
				gfx.setColor(oval_color[i]);
				gfx.drawOval(Math.min(oval_xy[i][0],oval_xy[i][2]), Math.min(oval_xy[i][1], oval_xy[i][3]), Math.abs(oval_xy[i][0] - oval_xy[i][2]), Math.abs(oval_xy[i][1] - oval_xy[i][3]));
			}
			
			// Lines
			for( int i = 0; i <= line_count && line_count < MAX_LINES; i++ )
			{
				gfx.setColor(line_color[i]);
				gfx.drawLine(line_xy[i][0], line_xy[i][1], line_xy[i][2], line_xy[i][3]);
			}
		}
	}
	
	// --------------------------------------------------------
	
	// For the (x, y) labels and freehand drawing
	class CanvasMouseMotionListener implements MouseMotionListener
	{
		public void mouseMoved(MouseEvent evt)
		{
			coordsLabel.setText(evt.getX() + ", " + evt.getY());
		}
		
		// --------------------------------------------------------
		
		public void mouseDragged(MouseEvent evt)
		{
			// Update array with data for gfx component
			if( curr_dtool_mode == 'f' )
			{
				if(!updateFreeHandArrays(evt)) messageArea.append("There are no inks left! You must clear the canvas!\n");
			}

			// Mouse has moved when dragged
			mouseMoved(evt);
			
			// Status
			if (DEBUG) System.out.println("{[" + evt.getX() + ", " + evt.getY() + "], " + fhand_thickness + "} has colour " + selected_color);
		 
		   
			switch( curr_dtool_mode )
			{
			   case 'l':
					line_xy[line_count][2] = evt.getX();
					line_xy[line_count][3] = evt.getY();
			   break;
			   
			   case 'r':
					rect_xy[rect_count][2] = evt.getX();
					rect_xy[rect_count][3] = evt.getY();
			   break;
			   
			   case 'o':
					oval_xy[oval_count][2] = evt.getX();
					oval_xy[oval_count][3] = evt.getY();
			   break;
			}
			
			// Repaint the canvas
			canvas.repaint();
			
			
		}
	}
	
	// --------------------------------------------------------
	
	// for the freehand drawing
	class CanvasMouseListener implements MouseListener
	{
		public void mousePressed(MouseEvent evt)
		{
			switch( curr_dtool_mode )
			{
				case 'l':
					line_xy[line_count][0] = line_xy[line_count][2] = evt.getX();
					line_xy[line_count][1] = line_xy[line_count][3] = evt.getY();
					line_color[line_count] = selected_color;
				break;
				
				case 'r':
					rect_xy[rect_count][0] = rect_xy[rect_count][2] = evt.getX();
					rect_xy[rect_count][1] = rect_xy[rect_count][3] = evt.getY();
					rect_color[rect_count] = selected_color;
				break;
				
				case 'o':
					oval_xy[oval_count][0] = oval_xy[oval_count][2] = evt.getX();
					oval_xy[oval_count][1] = oval_xy[oval_count][3] = evt.getY();
					oval_color[oval_count] = selected_color;
				break;
			}
		}
		
		public void mouseReleased(MouseEvent evt)
		{
			switch( curr_dtool_mode )
			{
				case 'l': line_count++; break;
				case 'r': rect_count++; break;
				case 'o': oval_count++; break;
			}  
		}
		
		public void mouseClicked(MouseEvent evt)
		{
			// Put the appropriate data into the arrays
			if( curr_dtool_mode == 'f' )
			{
				if(!updateFreeHandArrays(evt)) messageArea.append("There are no inks left! You must clear the canvas!\n");
			}
			
			// Status
			if ( DEBUG ) System.out.println("{[" + evt.getX() + ", " + evt.getY() + "], " + fhand_thickness + "} has colour " + selected_color);
			
			// Repaint the canvas
			canvas.repaint();
		}
		
      @Override
		public void mouseEntered(MouseEvent evt)
		{
			;
		}
		
		public void mouseExited(MouseEvent evt)
		{
			;
		}
	}
	
	// --------------------------------------------------------
	
	// For the colour changer
	class FreehandSliderListener implements ChangeListener
	{
		public void stateChanged(ChangeEvent evt)
		{
			fhand_thickness = freehandSizeSlider.getValue();
			//System.out.println(freehandSizeSlider.getValue());
		}
	}
	
	// --------------------------------------------------------
	
	// Grid
	class GridControlChangeListener implements ChangeListener
	{
		public void stateChanged(ChangeEvent evt)
		{
			repaint();
		}
	}
	
	// Color changed
	class ColorChooserActionListener implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			Color newColor = JColorChooser.showDialog(null, "Select new colour...", selected_color);
			selected_color = newColor;
		}
	}
	
	// Clear the canvas
	class ClearCanvasActionListener implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			if(DEBUG) System.out.println("Clear canvas requested");
			
			// Freehand
			fhand_xy = new int[MAX_FHAND][3];
			fhand_count = 0;
			fh_left = MAX_FHAND;
			
			// Lines
			line_xy = new int[MAX_LINES][4];
			line_count = 0;
			
			// Rectangles
			rect_xy = new int[MAX_RECTS][4];
			rect_count = 0;
			
			// Ovals
			oval_xy = new int[MAX_OVALS][4];
			oval_count = 0;
			
			messageArea.setText("Canvas has been cleared");
			repaint();
		}
	}
	
	class AnimateButtonActionListener implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			AnimatorClass aniClass = new AnimatorClass();
			Timer animationTimer = new Timer(10, aniClass);
			animationTimer.start();
		}
	}
	
	class AnimatorClass implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			//oval_xy[0][0]--;
			oval_xy[0][1]++;
			//oval_xy[0][2]--;
			oval_xy[0][3]++;
			repaint();
		}
	}
	
	// Drawing tool selector
	class DrawingToolActionListener implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			String currentMode = evt.getActionCommand();
			if( currentMode == "Line" )
			{
				curr_dtool_mode = 'l';
			}
			else if ( currentMode == "Rectangle" )
			{
				curr_dtool_mode = 'r';
			}
			else if ( currentMode == "Oval" )
			{
				curr_dtool_mode = 'o';
			}
			else if ( currentMode == "Freehand" )
			{
				curr_dtool_mode = 'f';
			}
		}
	}
	
	class SaveMenuActionListener implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			int returnVal = fileChooser.showSaveDialog(NXDraw.this);
			if (returnVal == JFileChooser.APPROVE_OPTION)
			{
	            file = fileChooser.getSelectedFile();
	            try
	            {
					objectOut = new ObjectOutputStream(new FileOutputStream(file));
					objectOut.writeObject(line_xy);
					objectOut.writeObject(line_color);
					objectOut.writeInt(line_count);
					objectOut.writeObject(oval_xy);
					objectOut.writeObject(oval_color);
					objectOut.writeInt(oval_count);
					objectOut.writeObject(rect_xy);
					objectOut.writeObject(rect_color);
					objectOut.writeInt(rect_count);
					objectOut.writeObject(fhand_xy);
					objectOut.writeObject(fhand_color);
					objectOut.writeInt(fhand_count);
				}
	            catch (FileNotFoundException e)
	            {
					e.printStackTrace();
				}
	            catch (IOException e)
	            {
					e.printStackTrace();
				}
	            finally
	            {
	            	try
	            	{
	                    if (objectOut != null)
	                    {
	                    	objectOut.flush();
	                    	objectOut.close();
	                    }
	                }
	            	catch (IOException ex)
	            	{
	                    ex.printStackTrace();
	                }
	            }
	            messageArea.append("Saved file to " + file);
	        }
			else
			{
	            messageArea.append("Open command cancelled by user.");
	        }
		}
	}
	
	class LoadMenuActionListener implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			int returnVal = fileChooser.showOpenDialog(NXDraw.this);
			if (returnVal == JFileChooser.APPROVE_OPTION)
			{
	            file = fileChooser.getSelectedFile();
	            try
	            {
	            	messageArea.append("Open file " + file);
					objectIn = new ObjectInputStream(new FileInputStream(file));
					line_xy = (int[][])objectIn.readObject();
					line_color = (Color[])objectIn.readObject();
					line_count = (int)objectIn.readInt();
					oval_xy = (int[][])objectIn.readObject();
					oval_color = (Color[])objectIn.readObject();
					oval_count = (int)objectIn.readInt();
					rect_xy = (int[][])objectIn.readObject();
					rect_color = (Color[])objectIn.readObject();
					rect_count = (int)objectIn.readInt();
					fhand_xy = (int[][])objectIn.readObject();
					fhand_color = (Color[])objectIn.readObject();
					fhand_count = (int)objectIn.readInt();
					repaint();
				}
	            catch (FileNotFoundException e)
	            {
					e.printStackTrace();
				}
	            catch (IOException e)
	            {
					e.printStackTrace();
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
				finally
				{
					try
	            	{
	                    if (objectIn != null)
	                    {
	                    	objectIn.close();
	                    }
	                }
	            	catch (IOException ex)
	            	{
	                    ex.printStackTrace();
	                }
				}
			}
			else
			{
				messageArea.append("");
			}
		}
	}
	
	class ExitMenuActionListener implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			System.exit(0);
		}
	}
	
	class AboutMenuActionListener implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			JOptionPane.showMessageDialog(canvas, "NXDraw version 3.2\nCopyright 2009-2010 Chris Granville. All Rights Reserved.");
		}
	}
	
	// --------------------------------------------------------
	
	public NXDraw()
	{   
		setLayout(new BorderLayout());
		setTitle(WINDOW_TITLE);
		
		// Canvas
		canvasCursor = new Cursor(Cursor.CROSSHAIR_CURSOR);
		canvas = new Canvas();
		canvas.setBorder(new TitledBorder(new EtchedBorder(), "Canvas"));
		canvas.setPreferredSize(new Dimension(CANVAS_WIDTH, CANVAS_HEIGHT));
		canvas.setCursor(canvasCursor);
		canvas.addMouseMotionListener(new CanvasMouseMotionListener());
		canvas.addMouseListener(new CanvasMouseListener());
		add(canvas, BorderLayout.CENTER);
		
		// Menu Bar
		JMenuBar menuBar = new JMenuBar();
		JMenu fileMenu = new JMenu("File");
		JMenu helpMenu = new JMenu("Help");
		JMenuItem fileSaveMenuItem = new JMenuItem("Save");
		fileMenu.add(fileSaveMenuItem);
		fileSaveMenuItem.addActionListener(new SaveMenuActionListener());
		JMenuItem fileLoadMenuItem = new JMenuItem("Load");
		fileMenu.add(fileLoadMenuItem);
		fileLoadMenuItem.addActionListener(new LoadMenuActionListener());
		fileMenu.addSeparator();
		JMenuItem fileExitMenuItem = new JMenuItem("Exit");
		fileMenu.add(fileExitMenuItem);
		fileExitMenuItem.addActionListener(new ExitMenuActionListener());
		menuBar.add(fileMenu);
		JMenuItem helpAboutMenuItem = new JMenuItem("About");
		helpMenu.add(helpAboutMenuItem);
		helpAboutMenuItem.addActionListener(new AboutMenuActionListener());
		menuBar.add(helpMenu);
		add(menuBar, BorderLayout.PAGE_START);
		
		
		// Control Panel
		controlPanel = new JPanel();
		controlPanel.setBorder(new TitledBorder(new EtchedBorder(), "Control Panel"));
		controlPanel.setPreferredSize(new Dimension(CP_WIDTH, CANVAS_HEIGHT));
		JScrollPane controlPanelScrollPane = new JScrollPane(controlPanel);
		controlPanelScrollPane.setPreferredSize(new Dimension(CP_WIDTH + 30, CANVAS_HEIGHT));
		add(controlPanelScrollPane, BorderLayout.LINE_START);		
		
		// Control Panel contents
		
		// Coordinates panel
		JPanel coordinatesPanel = new JPanel();
		coordinatesPanel.setBorder(new TitledBorder(new EtchedBorder(), "Drawing Position"));
		coordinatesPanel.setPreferredSize(new Dimension(CP_WIDTH - 20, 60));
		coordsLabel = new JLabel();
		coordinatesPanel.add(coordsLabel);
		controlPanel.add(coordinatesPanel);
		
		// Drawing tools panel
		JPanel drawingToolsPanel = new JPanel();
		drawingToolsPanel.setPreferredSize(new Dimension(CP_WIDTH - 20, 140));
		drawingToolsPanel.setLayout(new GridLayout(0, 1));
		drawingToolsPanel.setBorder(new TitledBorder(new EtchedBorder(), "Drawing Tools"));
		ButtonGroup drawingToolsButtonGroup = new ButtonGroup();
		lineRadioButton = new JRadioButton("Line", true);
		drawingToolsButtonGroup.add(lineRadioButton);
		drawingToolsPanel.add(lineRadioButton);
		rectangleRadioButton = new JRadioButton("Rectangle");
		drawingToolsButtonGroup.add(rectangleRadioButton);
		drawingToolsPanel.add(rectangleRadioButton);
		ovalRadioButton = new JRadioButton("Oval");
		drawingToolsButtonGroup.add(ovalRadioButton);
		drawingToolsPanel.add(ovalRadioButton);
		freehandRadioButton = new JRadioButton("Freehand");
		lineRadioButton.addActionListener(new DrawingToolActionListener());
		rectangleRadioButton.addActionListener(new DrawingToolActionListener());
		ovalRadioButton.addActionListener(new DrawingToolActionListener());
		freehandRadioButton.addActionListener(new DrawingToolActionListener());
		drawingToolsButtonGroup.add(freehandRadioButton);
		drawingToolsPanel.add(freehandRadioButton);
		controlPanel.add(drawingToolsPanel);
		
		// Freehand trace size slider
		JPanel freehandSliderPanel = new JPanel();
		freehandSliderPanel.setPreferredSize(new Dimension(CP_WIDTH - 20, 90));
		drawingToolsPanel.setLayout(new GridLayout(0, 1));
		freehandSliderPanel.setBorder(new TitledBorder(new EtchedBorder(), "Freehand Size"));
		freehandSizeSlider = new JSlider(0, 20, 1);
		freehandSizeSlider.setPreferredSize(new Dimension(CP_WIDTH - 40, 50));
		freehandSizeSlider.setMajorTickSpacing(5);
		freehandSizeSlider.setMinorTickSpacing(1);
		freehandSizeSlider.setPaintTicks(true);
		freehandSizeSlider.setPaintLabels(true);
		freehandSizeSlider.addChangeListener(new FreehandSliderListener());
		freehandSliderPanel.add(freehandSizeSlider);
		controlPanel.add(freehandSliderPanel);

		// Grid Panel
		JPanel gridPanel = new JPanel();
		gridPanel.setPreferredSize(new Dimension(CP_WIDTH - 20, 80));
		gridPanel.setLayout(new GridLayout(0, 1));
		gridPanel.setBorder(new TitledBorder(new EtchedBorder(), "Grid"));
		fineCheckBox = new JCheckBox("Fine");
		fineCheckBox.addChangeListener(new GridControlChangeListener());
		gridPanel.add(fineCheckBox);
		coarseCheckBox = new JCheckBox("Coarse");
		coarseCheckBox.addChangeListener(new GridControlChangeListener());
		gridPanel.add(coarseCheckBox);
		controlPanel.add(gridPanel);
		
		// Colour Panel
		JPanel colourPanel = new JPanel();
		colourPanel.setPreferredSize(new Dimension(CP_WIDTH - 20, 90));
		colourPanel.setBorder(new TitledBorder(new EtchedBorder(), "Colour"));
		colourButton = new JButton();
		colourButton.setBackground(Color.blue);
		colourButton.setPreferredSize(new Dimension(50, 50));
		colourButton.addActionListener(new ColorChooserActionListener());
		colourPanel.add(colourButton);
		controlPanel.add(colourPanel);

		// Clear button
		clearButton = new JButton("Clear Canvas");
		clearButton.setPreferredSize(new Dimension(CP_WIDTH - 20, 50));
		clearButton.addActionListener(new ClearCanvasActionListener());
		controlPanel.add(clearButton);

		// Animate button
		animateButton = new JButton("Animate");
		animateButton.setPreferredSize(new Dimension(CP_WIDTH - 20, 50));
		animateButton.addActionListener(new AnimateButtonActionListener());
		controlPanel.add(animateButton);

		// Message area
		messageArea = new JTextArea();
		messageArea.setEditable(false);
		messageArea.setBackground(canvas.getBackground());
		JScrollPane textAreaScrollPane = new JScrollPane(messageArea);
		textAreaScrollPane.setBorder(new TitledBorder(new EtchedBorder(), "Message Area"));
		textAreaScrollPane.setPreferredSize(new Dimension(CP_WIDTH + CANVAS_WIDTH, MA_HEIGHT));
		add(textAreaScrollPane, BorderLayout.PAGE_END);

		// Misc
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		pack();
		setVisible(true);
	}
	
	private boolean updateFreeHandArrays(MouseEvent evt)
	{
		int eventX = evt.getX();
		int eventY = evt.getY();
		try
		{
			fhand_color[fhand_count] = selected_color;
			fhand_xy[fhand_count][0] = eventX;
			fhand_xy[fhand_count][1] = eventY;
			fhand_xy[fhand_count][2] = fhand_thickness;
			fhand_count++;
			
			fh_left--;
			if (fh_left > 0)
			{
				messageArea.append("You have " + fh_left + " inks left.\n");
			}
			
			return true;
		}
		catch (ArrayIndexOutOfBoundsException e)
		{
			return false;   // Return false instead of updating the messageArea here as two types of mouseEvent can call this,
							// and we may want to return different messages.
		}
	}
	
	// --------------------------------------------------------
	
	public static void main(String args[]) throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException
	{
		if(NIMBUS) UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
		new NXDraw();
	}
	
}