package plugins.fmp.multicafe2.tools;


import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.JToolBar;

import icy.canvas.Canvas2D;
import icy.gui.component.button.IcyButton;
import icy.gui.viewer.Viewer;
import icy.sequence.Sequence;
import icy.resource.icon.IcyIcon;

import plugins.fmp.multicafe2.resource.ResourceUtilFMP;


public class KymosCanvas2D extends Canvas2D
{
    /**
	 * 
	 */
	private static final long serialVersionUID = 8827595503996677250L;
	public 	JComboBox<String> kymographsCombo 	= new JComboBox <String> (new String[] {"none", "transf"});
	
 
    public KymosCanvas2D(Viewer viewer)
    {
        super(viewer);
    }
    
    @Override
    public void customizeToolbar(JToolBar toolBar)
    {
    	toolBar.addSeparator();
        toolBar.add(kymographsCombo);
        
		IcyButton previousButton = new IcyButton(ResourceUtilFMP.ICON_PREVIOUS_IMAGE);
		previousButton.setSelected(false);
		previousButton.setFocusable(false);
		previousButton.setToolTipText("Select previous capillary (to the left or lower index)");
        toolBar.add(previousButton); 
		
        IcyButton nextButton = new IcyButton(ResourceUtilFMP.ICON_NEXT_IMAGE);
        nextButton.setSelected(false);
        nextButton.setFocusable(false);
        nextButton.setToolTipText("Select next capillary (to the right or higher index)");
		toolBar.add(nextButton);
		
		IcyIcon fitY = ResourceUtilFMP.ICON_FIT_YAXIS;
		IcyButton fitYAxisButton = new IcyButton(fitY);
		fitYAxisButton.setSelected(false);
		fitYAxisButton.setFocusable(false);
		fitYAxisButton.setToolTipText("Set image scale ratio to 1:1 and fit Y axis to the window height");
		toolBar.add(fitYAxisButton);
		
		IcyIcon fitX = ResourceUtilFMP.ICON_FIT_XAXIS;
		IcyButton fitXAxisButton = new IcyButton(fitX);
		fitXAxisButton.setSelected(false);
		fitXAxisButton.setFocusable(false);
		fitXAxisButton.setToolTipText("Fit X and Y axis to the window size");
		toolBar.add(fitXAxisButton);
        
		super.customizeToolbar(toolBar);
        
        previousButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                setPositionT( getPositionT()-1);
            }});
        
        nextButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
            	setPositionT( getPositionT()+1);
            }});
        
        fitYAxisButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
            	zoomImage_1_1();
            }});
        
        fitXAxisButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
            	shrinkImage_to_fit() ;
            }});
    }   	        
    
    void zoomImage_1_1() 
	{
		Sequence seqKymograph = getSequence();
		Rectangle rectImage = seqKymograph.getBounds2D();
		Rectangle rectCanvas = getCanvasVisibleRect();
		
		int offsetX = (int) (rectCanvas.width / getScaleX() / 2); 
		double scaleY = rectCanvas.getHeight() / rectImage.getHeight();;  
		double scaleX = scaleY; 
		setMouseImagePos(offsetX, rectImage.height  / 2);
		setScale(scaleX, scaleY, true, true);
	}
    
    void shrinkImage_to_fit() 
	{
		Sequence seqKymograph = getSequence();
		Rectangle rectImage = seqKymograph.getBounds2D();
		Rectangle rectCanvas = getCanvasVisibleRect();
		
		double scaleX = rectCanvas.getWidth() / rectImage.getWidth(); 
		double scaleY = rectCanvas.getHeight() / rectImage.getHeight();
		setMouseImagePos(rectImage.width/2, rectImage.height/ 2);
		setScale(scaleX, scaleY, true, true);
	}
}
