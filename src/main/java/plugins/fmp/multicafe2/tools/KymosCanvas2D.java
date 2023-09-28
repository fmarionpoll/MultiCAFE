package plugins.fmp.multicafe2.tools;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JToolBar;

import icy.canvas.Canvas2D;
import icy.gui.viewer.Viewer;
import icy.sequence.Sequence;



public class KymosCanvas2D extends Canvas2D
{
    /**
	 * 
	 */
	private static final long serialVersionUID = 8827595503996677250L;
	final JButton nextButton 				= new JButton("+");
    final JButton previousButton 			= new JButton("-");
    final JButton zoomImageButton			= new JButton("1:1");
    final JButton shrinkImageButton			= new JButton("all");

    
    public KymosCanvas2D(Viewer viewer)
    {
        super(viewer);
    }
    
    @Override
    public void customizeToolbar(JToolBar toolBar)
    {
//    	zoomFitImageButton = new IcyButton(new IcyIcon(Canvas2D.ICON_FIT_IMAGE));
//        zoomFitImageButton.setFlat(true);
//        zoomFitImageButton.setToolTipText("Fit window to image size");
//        GridBagConstraints gbc_zoomFitImage = new GridBagConstraints();
//        gbc_zoomFitImage.insets = new Insets(0, 0, 0, 5);
//        gbc_zoomFitImage.gridx = 6;
//        gbc_zoomFitImage.gridy = 1;
//        panel.add(zoomFitImageButton, gbc_zoomFitImage);
        
		int bWidth = 30; 
		int height = 25;
		previousButton.setPreferredSize(new Dimension(bWidth, height));
		previousButton.setToolTipText("Select previous capillary (to the left or lower index)");
		GridBagConstraints gbc_previousButton = new GridBagConstraints();
        gbc_previousButton.insets = new Insets(0, 0, 0, 5);
        gbc_previousButton.gridheight = 1;
        gbc_previousButton.gridwidth = 1;
        gbc_previousButton.gridx = 0;
        gbc_previousButton.gridy = 1;
        toolBar.add(previousButton, gbc_previousButton);
		
		nextButton.setPreferredSize(new Dimension(bWidth, height));
		nextButton.setToolTipText("Select next capillary (to the right or higher index)");
		toolBar.add(nextButton);
		
		zoomImageButton.setPreferredSize(new Dimension(bWidth+10, height));
		zoomImageButton.setToolTipText("Set image scale ratio to 1:1 and fit Y axis to the window height");
		toolBar.add(zoomImageButton);
		
		shrinkImageButton.setPreferredSize(new Dimension(bWidth+10, height));
		shrinkImageButton.setToolTipText("Fit X and Y axis to the window size");
		toolBar.add(shrinkImageButton);
        
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
        
        zoomImageButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
            	zoomImage_1_1();
            }});
        
        shrinkImageButton.addActionListener(new ActionListener() {
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
