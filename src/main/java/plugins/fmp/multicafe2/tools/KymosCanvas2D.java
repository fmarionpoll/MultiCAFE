package plugins.fmp.multicafe2.tools;

import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JToolBar;

import icy.canvas.Canvas2D;
import icy.gui.component.button.IcyButton;
import icy.gui.viewer.Viewer;
import icy.sequence.Sequence;
import icy.resource.ResourceUtil;
import icy.resource.icon.IcyIcon;

import plugins.fmp.multicafe2.resource.ResourceUtilFMP;


public class KymosCanvas2D extends Canvas2D
{
    /**
	 * 
	 */
	private static final long serialVersionUID = 8827595503996677250L;
	static final int ICON_SIZE = 20;

    static final Image ICON_PREVIOUS_IMAGE = ResourceUtil.getAlphaIconAsImage("br_prev.png");
    static final Image ICON_NEXT_IMAGE  = ResourceUtil.getAlphaIconAsImage("br_next.png");
    
    
    
    public KymosCanvas2D(Viewer viewer)
    {
        super(viewer);
    }
    
    @Override
    public void customizeToolbar(JToolBar toolBar)
    {
    	 toolBar.addSeparator();
         
		IcyButton previousButton = new IcyButton(new IcyIcon(ICON_PREVIOUS_IMAGE));
		previousButton.setSelected(false);
		previousButton.setFocusable(false);
		previousButton.setToolTipText("Select previous capillary (to the left or lower index)");
        toolBar.add(previousButton); 
		

        IcyButton nextButton = new IcyButton(new IcyIcon(ICON_NEXT_IMAGE));
        nextButton.setSelected(false);
        nextButton.setFocusable(false);
        nextButton.setToolTipText("Select next capillary (to the right or higher index)");
		toolBar.add(nextButton);
		
		IcyButton fitYAxisButton = new IcyButton(ResourceUtilFMP.ICON_FIT_YAXIS);
		fitYAxisButton.setSelected(false);
		fitYAxisButton.setFocusable(false);
		fitYAxisButton.setToolTipText("Set image scale ratio to 1:1 and fit Y axis to the window height");
		toolBar.add(fitYAxisButton);
		
		IcyButton fitXAxisButton = new IcyButton(ResourceUtilFMP.ICON_FIT_XAXIS);
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
