package plugins.fmp.multicafe2.tools;


import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.JToolBar;

import icy.canvas.Canvas2D;
import icy.gui.component.button.IcyButton;
import icy.gui.viewer.Viewer;
import icy.image.IcyBufferedImage;
import icy.sequence.Sequence;
import icy.resource.icon.IcyIcon;

import plugins.fmp.multicafe2.resource.ResourceUtilFMP;
import plugins.fmp.multicafe2.tools.Image.ImageTransformEnums;
import plugins.fmp.multicafe2.tools.Image.ImageTransformInterface;


public class KymosCanvas2D extends Canvas2D
{
    /**
	 * 
	 */
	private static final long serialVersionUID = 8827595503996677250L;
	public ImageTransformEnums[] imageTransform = new ImageTransformEnums[] {ImageTransformEnums.NONE,
			ImageTransformEnums.R_RGB, ImageTransformEnums.G_RGB, ImageTransformEnums.B_RGB 
//			, ImageTransformEnums.R2MINUS_GB, ImageTransformEnums.G2MINUS_RB, ImageTransformEnums.B2MINUS_RG, ImageTransformEnums.RGB
//			, ImageTransformEnums.GBMINUS_2R, ImageTransformEnums.RBMINUS_2G, ImageTransformEnums.RGMINUS_2B, ImageTransformEnums.RGB_DIFFS
//			, ImageTransformEnums.H_HSB, ImageTransformEnums.S_HSB, ImageTransformEnums.B_HSB
			};
	public JComboBox<ImageTransformEnums> imageTransformFunctionsCombo = new JComboBox<ImageTransformEnums> (imageTransform);
	ImageTransformInterface transform = ImageTransformEnums.NONE.getFunction();
 
    public KymosCanvas2D(Viewer viewer)
    {
        super(viewer);
    }
    
    @Override
    public void customizeToolbar(JToolBar toolBar)
    {
    	toolBar.addSeparator();
        toolBar.add(imageTransformFunctionsCombo);
        
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
        
        imageTransformFunctionsCombo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
            	ImageTransformEnums transformEnum = (ImageTransformEnums) imageTransformFunctionsCombo.getSelectedItem();
            	transform = transformEnum.getFunction();
            	refresh();
            }});
        
        imageTransformFunctionsCombo.setSelectedIndex(0);
        refresh();
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
    
    @Override
    public IcyBufferedImage getImage(int t, int z, int c)
    {
    	IcyBufferedImage img = super.getImage(t, z, c);
    	IcyBufferedImage img2 = transform.getTransformedImage (img, null);
        return img2;
    }
    
    public void updateListOfImageTransformFunctions(ImageTransformEnums[] transformArray) 
    {
        // remove listeners
        ActionListener[] listeners = imageTransformFunctionsCombo.getActionListeners();
        for (int i = 0; i < listeners.length; i++)
        	imageTransformFunctionsCombo.removeActionListener(listeners[i]);

        if (imageTransformFunctionsCombo.getItemCount() > 0)
        	imageTransformFunctionsCombo.removeAllItems();

        // add contents
        imageTransformFunctionsCombo.addItem(ImageTransformEnums.NONE);
        for (int i = 0; i < transformArray.length; i++) {
        	imageTransformFunctionsCombo.addItem(transformArray[i]);
        }

        // restore listeners
        for (int i = 0; i < listeners.length; i++)
        	imageTransformFunctionsCombo.addActionListener(listeners[i]);
    }
    
    public void selectImageTransformFunction(int iselected) 
    {
		imageTransformFunctionsCombo.setSelectedIndex(iselected);
    }
}
