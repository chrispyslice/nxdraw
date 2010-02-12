/**
 * NXDraw
 * 
 * Main window for nxDraw
 * 
 * @author Chris Granville
 * @version 1.0
 */

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import java.io.*;

public class NXDraw extends JFrame
{
    // Window
    private final int CANVAS_HEIGHT = 800;                                  // Canvas height
    private final int CANVAS_WIDTH = 640;                                   // Canvas width
    private final int CP_WIDTH = 200;                                       // Control panel width
    private final int MA_HEIGHT = 100;                                      // Message area height
    private final String WINDOW_TITLE = "Nexus Drawer";                     // Window title
    
    // Preferences
    private final boolean DEBUG = false;                                     // Do we want to display debugging info?
    
    // Freehand drawing
    int freehandThickness = 1;                                              // Get the value of the thickness slider
    private int freehandPixelsCount = 0;                                    // Make sure we don't go over the limit of the below arrays
    private final int MAX_FREEHAND_PIXELS = 100000;                         // Max. no of freehand squares
    private Color[] freehandColour = new Color[MAX_FREEHAND_PIXELS];        // Hold colour of each square
    private int[][] fxy = new int[MAX_FREEHAND_PIXELS][3];                  // Position and size of each square
    private Color selectedColour = new Color(0.0f, 0.0f, 0.0f);             // Initial colour. Currently black
    private int freehandPixelsLeft = MAX_FREEHAND_PIXELS;                   // How many freehand drawings can we still do?
    
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
    private JMenuBar menuBar;
    
    // --------------------------------------------------------
    
    class Canvas extends JPanel
    {
        public void paintComponent(Graphics gfx)
        {
            super.paintComponent(gfx);
            
            int canvasHeight = getHeight();
            int canvasWidth = getWidth();

            // Small lines
            if( fineCheckBox.isSelected() )
            {
                gfx.setColor(new Color(0.8F, 0.8F, 0.8F));
                for(int i = 0; i < canvasHeight; i =i + 10)
                {
                    gfx.drawLine(0,i, canvasWidth, i);
                }
                for(int i = 0; i < canvasWidth; i =i + 10)
                {
                    gfx.drawLine(i, 0, i, canvasHeight);
                }
            }
            
            // Thick lines
            if( coarseCheckBox.isSelected() )
            {
               gfx.setColor(new Color(0.6F, 0.6F, 0.6F));
               for(int i = 0; i < canvasHeight; i =i + 50)
               {
                   gfx.drawLine(0,i, canvasWidth, i);
                } 
                for(int i = 0; i < canvasWidth; i =i + 50)
                {
                    gfx.drawLine(i, 0, i, canvasHeight);
                } 
            }
            
            // Freehand, for every pixel...
            for ( int i = 0; i < freehandPixelsCount; i++ )
            {
                gfx.setColor(freehandColour[i]);
                int width_height = fxy[i][2];
                int offsetX = fxy[i][0] - (width_height / 2);
                int offsetY = fxy[i][1] - (width_height / 2);
                //gfx.fillOval(fxy[i][0], fxy[i][1], fxy[i][2], fxy[i][2]);
                gfx.fillOval(offsetX, offsetY, width_height, width_height);
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
            if(!updateFreeHandArrays(evt))  messageArea.append("There are no inks left! You must clear the canvas!\n");
            
            // Mouse has moved when dragged
            mouseMoved(evt);
            
            // Status
            if (DEBUG) System.out.println("{[" + evt.getX() + ", " + evt.getY() + "], " + freehandThickness + "} has colour " + selectedColour);
            
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
            ;
        }
        
        public void mouseReleased(MouseEvent evt)
        {
            ;
        }
        
        public void mouseClicked(MouseEvent evt)
        {
            // Put the appropriate data into the arrays
            if(!updateFreeHandArrays(evt)) messageArea.append("There are no inks left! You must clear the canvas!\n");
            
            // Status
            if ( DEBUG ) System.out.println("{[" + evt.getX() + ", " + evt.getY() + "], " + freehandThickness + "} has colour " + selectedColour);
            
            // Repaint the canvas
            canvas.repaint();
        }
        
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
            freehandThickness = freehandSizeSlider.getValue();
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
    
    // Colour changed
    class ColorChooserActionListener implements ActionListener
    {
        public void actionPerformed(ActionEvent evt)
        {
            JColorChooser colourChooser = new JColorChooser(selectedColour);
            Color newColor = colourChooser.showDialog(null, "Select new colour...", selectedColour);
            selectedColour = newColor;
        }
    }
    
    // Clear the canvas
    class ClearCanvasActionListener implements ActionListener
    {
        public void actionPerformed(ActionEvent evt)
        {
            if(DEBUG) System.out.println("Clear canvas requested");
            // Run through the fxy array so everything = 0;
            for(int i = 0; i < freehandPixelsCount; i++)
            {
                fxy[i][0] = 0;
                fxy[i][1] = 0;
                fxy[i][2] = 0;
            }
            freehandPixelsCount = 0;
            freehandPixelsLeft = MAX_FREEHAND_PIXELS;
            messageArea.setText("");
            repaint();
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
        JMenuItem fileLoadMenuItem = new JMenuItem("Load");
        fileMenu.add(fileLoadMenuItem);
        fileMenu.addSeparator();
        JMenuItem fileExitMenuItem = new JMenuItem("Exit");
        fileMenu.add(fileExitMenuItem);
        menuBar.add(fileMenu);
        JMenuItem helpAboutMenuItem = new JMenuItem("About");
        helpMenu.add(helpAboutMenuItem);
        menuBar.add(helpMenu);
        add(menuBar, BorderLayout.PAGE_START);
        
        
        // Control Panel
        controlPanel = new JPanel();
        controlPanel.setBorder(new TitledBorder(new EtchedBorder(), "Control Panel"));
        controlPanel.setPreferredSize(new Dimension(CP_WIDTH, CANVAS_HEIGHT));
        //add(controlPanel, BorderLayout.LINE_START);  // This line instead of the following two to put the control panel in a scroll pane.      
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
            freehandColour[freehandPixelsCount] = selectedColour;
            fxy[freehandPixelsCount][0] = eventX;
            fxy[freehandPixelsCount][1] = eventY;
            fxy[freehandPixelsCount][2] = freehandThickness;
            freehandPixelsCount++;
            
            freehandPixelsLeft--;
            if (freehandPixelsLeft > 0)
            {
                messageArea.append("You have " + freehandPixelsLeft + " inks left.\n");
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
    
    public static void main(String args[])
    {
        NXDraw window = new NXDraw();
    }
}