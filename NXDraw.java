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
    private final int CANVAS_HEIGHT = 800;
    private final int CANVAS_WIDTH = 640;
    private final int CP_WIDTH = 200;
    private final int MA_HEIGHT = 100;
    private final String WINDOW_TITLE = "Nexus Drawer";
    
    // Freehand drawing
    int freehandThickness = 1;                                              // Get the value of the thickness slider
    private int freehandPixelsCount = 0;                                    // Make sure we don't go over the limit of the below arrays
    private final int MAX_FREEHAND_PIXELS = 100000;                           // Max. no of freehand squares
    private Color[] freehandColour = new Color[MAX_FREEHAND_PIXELS];        // Hold colour of each square
    private int[][] fxy = new int[MAX_FREEHAND_PIXELS][3];                  // Position and size of each square
    
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
                gfx.fillRect(fxy[i][0], fxy[i][1], fxy[i][2], fxy[i][2]);
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
            float randRed = (float) Math.random();
            float randBlue = (float) Math.random();
            float randGreen = (float) Math.random();
            
            freehandColour[freehandPixelsCount] = new Color(randRed, randGreen, randBlue);
            fxy[freehandPixelsCount][0] = evt.getX();
            fxy[freehandPixelsCount][1] = evt.getY();
            fxy[freehandPixelsCount][2] = freehandThickness;
            freehandPixelsCount++;
            
            mouseMoved(evt);
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
            // Generate some random numbers until we implement the colour selector
            float randRed = (float) Math.random();
            float randBlue = (float) Math.random();
            float randGreen = (float) Math.random();
            
            // Put the appropriate data into the arrays
            freehandColour[freehandPixelsCount] = new Color(randRed, randGreen, randBlue);
            fxy[freehandPixelsCount][0] = evt.getX();
            fxy[freehandPixelsCount][1] = evt.getY();
            fxy[freehandPixelsCount][2] = freehandThickness;
            freehandPixelsCount++;
            
            // Repaint the canva
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
    
    class GridControlChangeListener implements ChangeListener
    {
        public void stateChanged(ChangeEvent evt)
        {
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
        colourButton.setPreferredSize(new Dimension(50, 50));
        colourPanel.add(colourButton);
        controlPanel.add(colourPanel);

        // Clear button
        clearButton = new JButton("Clear Canvas");
        clearButton.setPreferredSize(new Dimension(CP_WIDTH - 20, 50));
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
    
    // --------------------------------------------------------
    
    public static void main(String args[])
    {
        NXDraw window = new NXDraw();
    }
}