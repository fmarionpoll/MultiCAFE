package plugins.fmp.multicafe.tools.Canvas2D;

import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JToolBar;

import icy.canvas.Canvas2D;
import icy.gui.component.button.IcyButton;
import icy.gui.viewer.Viewer;
import icy.image.IcyBufferedImage;
import icy.resource.icon.IcyIcon;
import icy.sequence.Sequence;
import plugins.fmp.multicafe.resource.ResourceUtilFMP;
import plugins.fmp.multicafe.tools.ImageTransform.ImageTransformEnums;
import plugins.fmp.multicafe.tools.ImageTransform.ImageTransformInterface;
import plugins.fmp.multicafe.tools.ImageTransform.ImageTransformOptions;

public class Canvas2DWithTransforms extends Canvas2D {
	/**
	 * 
	 */
	private static final long serialVersionUID = 8827595503996677250L;

	public ImageTransformEnums[] imageTransformStep1 = new ImageTransformEnums[] { ImageTransformEnums.NONE,
			ImageTransformEnums.R_RGB, ImageTransformEnums.G_RGB, ImageTransformEnums.B_RGB,
			ImageTransformEnums.R2MINUS_GB, ImageTransformEnums.G2MINUS_RB, ImageTransformEnums.B2MINUS_RG,
			ImageTransformEnums.RGB, ImageTransformEnums.GBMINUS_2R, ImageTransformEnums.RBMINUS_2G,
			ImageTransformEnums.RGMINUS_2B, ImageTransformEnums.RGB_DIFFS, ImageTransformEnums.H_HSB,
			ImageTransformEnums.S_HSB, ImageTransformEnums.B_HSB, ImageTransformEnums.SUBTRACT_REF };
	public JComboBox<ImageTransformEnums> transformsCombo1 = new JComboBox<ImageTransformEnums>(imageTransformStep1);
	ImageTransformInterface transformStep1 = ImageTransformEnums.NONE.getFunction();
	ImageTransformOptions optionsStep1 = new ImageTransformOptions();
	ImageTransformOptions optionsStep2 = new ImageTransformOptions();

	public ImageTransformEnums[] imageTransformStep2 = new ImageTransformEnums[] { ImageTransformEnums.NONE,
			ImageTransformEnums.SORT_SUMDIFFCOLS, ImageTransformEnums.SORT_CHAN0COLS };
	public JComboBox<ImageTransformEnums> transformsCombo2 = new JComboBox<ImageTransformEnums>(imageTransformStep2);
	ImageTransformInterface transformStep2 = ImageTransformEnums.NONE.getFunction();

	public Canvas2DWithTransforms(Viewer viewer) {
		super(viewer);
	}

	@Override
	public void customizeToolbar(JToolBar toolBar) {
		for (int i = 3; i >= 0; i--)
			toolBar.remove(i);
		toolBar.addSeparator();
		toolBar.add(new JLabel("step1"));
		toolBar.add(transformsCombo1);
		transformsCombo1.setToolTipText("transform image step 1");
		transformsCombo1.setEditable(true);

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

		fitYAxisButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				zoomImage_1_1();
			}
		});

		fitXAxisButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				shrinkImage_to_fit();
			}
		});

		transformsCombo1.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				ImageTransformEnums transformEnum = (ImageTransformEnums) transformsCombo1.getSelectedItem();
				transformStep1 = transformEnum.getFunction();
				optionsStep1.transformOption = transformEnum;
				refresh();
			}
		});

		transformsCombo1.setSelectedIndex(0);
		refresh();
	}

	void zoomImage_1_1() {
		Sequence seqKymograph = getSequence();
		Rectangle rectImage = seqKymograph.getBounds2D();
		Rectangle rectCanvas = getCanvasVisibleRect();

		int offsetX = (int) (rectCanvas.width / getScaleX() / 2);
		double scaleY = rectCanvas.getHeight() / rectImage.getHeight();
		double scaleX = scaleY;
		setMouseImagePos(offsetX, rectImage.height / 2);
		setScale(scaleX, scaleY, true, true);
	}

	void shrinkImage_to_fit() {
		Sequence seqKymograph = getSequence();
		Rectangle rectImage = seqKymograph.getBounds2D();
		Rectangle rectCanvas = getCanvasVisibleRect();

		double scaleX = rectCanvas.getWidth() / rectImage.getWidth();
		double scaleY = rectCanvas.getHeight() / rectImage.getHeight();
		setMouseImagePos(rectImage.width / 2, rectImage.height / 2);
		setScale(scaleX, scaleY, true, true);
	}

	@Override
	public IcyBufferedImage getImage(int t, int z, int c) {
		IcyBufferedImage img1 = transformStep1.getTransformedImage(super.getImage(t, z, c), optionsStep1);
		if (transformStep2 != null)
			return transformStep2.getTransformedImage(img1, optionsStep2);
		return img1;
	}

	public void updateTransformsComboStep1(ImageTransformEnums[] transformArray) {
		updateTransformsCombo(transformArray, transformsCombo1);
	}

	public void updateTransformsComboStep2(ImageTransformEnums[] transformArray) {
		updateTransformsCombo(transformArray, transformsCombo2);
	}

	protected void updateTransformsCombo(ImageTransformEnums[] transformArray,
			JComboBox<ImageTransformEnums> imageTransformFunctionsCombo) {
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

	public ImageTransformOptions getOptionsStep1() {
		return optionsStep1;
	}

	public void setOptionsStep1(ImageTransformOptions options) {
		optionsStep1 = options;
	}

	public ImageTransformOptions getOptionsStep2() {
		return optionsStep2;
	}

	public void setOptionsStep2(ImageTransformOptions options) {
		optionsStep2 = options;
	}

	public void selectIndexStep1(int iselected, ImageTransformOptions options) {
		transformsCombo1.setSelectedIndex(iselected);
		if (options != null)
			optionsStep1 = options;
	}

	public void selectItemStep1(ImageTransformEnums item, ImageTransformOptions options) {
		transformsCombo1.setSelectedItem(item);
		if (options != null)
			optionsStep1 = options;
	}

	public void selectIndexStep2(int iselected, ImageTransformOptions options) {
		transformsCombo2.setSelectedIndex(iselected);
		if (options != null)
			optionsStep2 = options;
	}

	public void selectItemStep2(ImageTransformEnums item, ImageTransformOptions options) {
		transformsCombo2.setSelectedItem(item);
		if (options != null)
			optionsStep2 = options;
	}

	public void customizeToolbarStep2(JToolBar toolBar) {
		toolBar.addSeparator();

		IcyButton previousButton = new IcyButton(ResourceUtilFMP.ICON_PREVIOUS_IMAGE);
		previousButton.setSelected(false);
		previousButton.setFocusable(false);
		previousButton.setToolTipText("Previous");
		toolBar.add(previousButton, 0);

		IcyButton nextButton = new IcyButton(ResourceUtilFMP.ICON_NEXT_IMAGE);
		nextButton.setSelected(false);
		nextButton.setFocusable(false);
		nextButton.setToolTipText("Next");
		toolBar.add(nextButton, 1);

		super.customizeToolbar(toolBar);

		previousButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				setPositionT(getPositionT() - 1);
			}
		});

		nextButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				setPositionT(getPositionT() + 1);
			}
		});

		toolBar.add(new JLabel("step2"), 6);
		toolBar.add(transformsCombo2, 7);
		transformsCombo2.setToolTipText("transform image step 2");
		transformsCombo2.setEditable(true);

		transformsCombo2.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				ImageTransformEnums transformEnum = (ImageTransformEnums) transformsCombo2.getSelectedItem();
				transformStep2 = transformEnum.getFunction();
				refresh();
			}
		});
	}

}
