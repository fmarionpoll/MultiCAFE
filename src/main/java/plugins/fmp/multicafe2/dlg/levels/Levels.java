package plugins.fmp.multicafe2.dlg.levels;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.geom.Rectangle2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;


import icy.util.StringUtil;
import plugins.fmp.multicafe2.MultiCAFE2;
import plugins.fmp.multicafe2.experiment.Capillary;
import plugins.fmp.multicafe2.experiment.Experiment;
import plugins.fmp.multicafe2.experiment.SequenceKymos;
import plugins.fmp.multicafe2.series.BuildSeriesOptions;
import plugins.fmp.multicafe2.series.DetectLevels;
import plugins.fmp.multicafe2.tools.ROI2DUtilities;
import plugins.fmp.multicafe2.tools.Image.ImageTransformEnums;
import plugins.kernel.roi.roi2d.ROI2DRectangle;



public class Levels extends JPanel implements PropertyChangeListener 
{
	private static final long serialVersionUID 	= -6329863521455897561L;
	JSpinner			startSpinner			= new JSpinner(new SpinnerNumberModel(0, 0, 10000, 1));
	JSpinner			endSpinner				= new JSpinner(new SpinnerNumberModel(3, 1, 10000, 1));
	
	private JCheckBox	pass1CheckBox 			= new JCheckBox ("pass1", true);
	private JComboBox<String> direction1ComboBox= new JComboBox<String> (new String[] {" threshold >", " threshold <" });
	private JSpinner 	threshold1Spinner 		= new JSpinner(new SpinnerNumberModel(35, 1, 255, 1));
	JComboBox<ImageTransformEnums> transform01ComboBox = new JComboBox<ImageTransformEnums> (
		new ImageTransformEnums[] {
			ImageTransformEnums.R_RGB, ImageTransformEnums.G_RGB, ImageTransformEnums.B_RGB, 
			ImageTransformEnums.R2MINUS_GB, ImageTransformEnums.G2MINUS_RB, ImageTransformEnums.B2MINUS_RG, ImageTransformEnums.RGB,
			ImageTransformEnums.GBMINUS_2R, ImageTransformEnums.RBMINUS_2G, ImageTransformEnums.RGMINUS_2B, ImageTransformEnums.RGB_DIFFS,
			ImageTransformEnums.H_HSB, ImageTransformEnums.S_HSB, ImageTransformEnums.B_HSB
			
		});
	private JButton		displayTransform1Button	= new JButton("Display");
	
	private JCheckBox	pass2CheckBox 			= new JCheckBox ("pass2", false);
	private JComboBox<String> direction2ComboBox= new JComboBox<String> (new String[] {" threshold >", " threshold <" });
	private JSpinner 	threshold2Spinner 		= new JSpinner(new SpinnerNumberModel(40, 1, 255, 1));
	JComboBox<ImageTransformEnums> transform02ComboBox = new JComboBox<ImageTransformEnums> (new ImageTransformEnums[] {
			ImageTransformEnums.YDIFFN, ImageTransformEnums.YDIFFN2,
			ImageTransformEnums.DERICHE, ImageTransformEnums.DERICHE_COLOR,
			ImageTransformEnums.MINUSHORIZAVG,
			ImageTransformEnums.COLORDISTANCE_L1_Y, ImageTransformEnums.COLORDISTANCE_L2_Y,
			ImageTransformEnums.SUBTRACT_1RSTCOL, ImageTransformEnums.L1DIST_TO_1RSTCOL,
			});
	private JButton		displayTransform2Button	= new JButton("Display");
	
	private JCheckBox	allKymosCheckBox 		= new JCheckBox ("all kymographs", true);
	private JSpinner	spanTopSpinner			= new JSpinner(new SpinnerNumberModel(3, 1, 100, 1));
	private String 		detectString 			= "        Detect     ";
	private JButton 	detectButton 			= new JButton(detectString);
	private JCheckBox	fromCheckBox 			= new JCheckBox (" from (pixel)", false);
	private JCheckBox 	allSeriesCheckBox 		= new JCheckBox("ALL (current to last)", false);
	private JCheckBox	leftCheckBox 			= new JCheckBox ("L", true);
	private JCheckBox	rightCheckBox 			= new JCheckBox ("R", true);
	private JCheckBox	runBackwardsCheckBox 	= new JCheckBox ("run backwards", false);
	
	private MultiCAFE2 	parent0 				= null;
	private DetectLevels threadDetectLevels 	= null;
	
	// -----------------------------------------------------
		
	void init(GridLayout capLayout, MultiCAFE2 parent0) 
	{
		setLayout(capLayout);
		this.parent0 = parent0;
		
		FlowLayout layoutLeft = new FlowLayout(FlowLayout.LEFT); 
		
		JPanel panel0 = new JPanel(layoutLeft);
		((FlowLayout)panel0.getLayout()).setVgap(0);
		panel0.add(detectButton);
		panel0.add(allSeriesCheckBox);
		panel0.add(allKymosCheckBox);
		panel0.add(leftCheckBox);
		panel0.add(rightCheckBox);
		add(panel0);
		
		JPanel panel01 = new JPanel(layoutLeft);
		panel01.add(pass1CheckBox);
		panel01.add(direction1ComboBox);
		((JLabel) direction1ComboBox.getRenderer()).setHorizontalAlignment(JLabel.RIGHT);
		panel01.add(threshold1Spinner);
		panel01.add(transform01ComboBox);
		panel01.add(displayTransform1Button);
		add (panel01);
		
		JPanel panel02 = new JPanel(layoutLeft);
		panel02.add(pass2CheckBox);
		panel02.add(direction2ComboBox);
		((JLabel) direction2ComboBox.getRenderer()).setHorizontalAlignment(JLabel.RIGHT);
		panel02.add(threshold2Spinner);
		panel02.add(transform02ComboBox);
		panel02.add(displayTransform2Button);
		add (panel02);
		
		JPanel panel03 = new JPanel(layoutLeft);
		panel03.add(fromCheckBox);
		panel03.add(startSpinner);
		panel03.add(new JLabel("to"));
		panel03.add(endSpinner);
		panel03.add(runBackwardsCheckBox);
		add( panel03);
		
		defineActionListeners();
		allowItemsAccordingToSelection();
	}
	
	private void defineActionListeners() 
	{	
		transform01ComboBox.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{ 
				Experiment exp =(Experiment)  parent0.expListCombo.getSelectedItem();
				if (exp != null && exp.seqCamData != null) 
				{
					kymosDisplayFiltered01(exp);
					firePropertyChange("KYMO_DISPLAY_FILTERED1", false, true);
				}
			}});
		
		transform02ComboBox.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{ 
				allowItemsAccordingToSelection();
				Experiment exp =(Experiment)  parent0.expListCombo.getSelectedItem();
				if (exp != null && exp.seqCamData != null) 
				{
					kymosDisplayFiltered02(exp);
					firePropertyChange("KYMO_DISPLAY_FILTERED2", false, true);
				}
			}});
	
		detectButton.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{ 
				if (detectButton.getText().equals(detectString))
					startComputation();
				else 
					stopComputation();
			}});	
		
		displayTransform1Button.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{ 
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null) 
				{ 
					kymosDisplayFiltered01(exp);
					firePropertyChange("KYMO_DISPLAY_FILTERED1", false, true);
				}
			}});
		
		displayTransform2Button.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{ 
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null) 
				{ 
					kymosDisplayFiltered02(exp);
					firePropertyChange("KYMO_DISPLAY_FILTERED2", false, true);
				}
			}});
		
		allSeriesCheckBox.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{
				Color color = Color.BLACK;
				if (allSeriesCheckBox.isSelected()) 
					color = Color.RED;
				allSeriesCheckBox.setForeground(color);
				detectButton.setForeground(color);
		}});
		
		fromCheckBox.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp == null)
						return;
				if (fromCheckBox.isSelected()) 
				{
					Rectangle2D searchRectangle = exp.seqKymos.seq.getBounds2D() ;
					ROI2DRectangle searchRectangleROI2D = new ROI2DRectangle(searchRectangle);
					final String dummyname = "search_rectangle";
					searchRectangleROI2D.setName(dummyname);
					searchRectangleROI2D.setColor(Color.ORANGE);
					
					exp.seqKymos.seq.addROI(searchRectangleROI2D);
					exp.seqKymos.seq.setSelectedROI(searchRectangleROI2D);
				}
				else
				{
					ROI2DUtilities.removeRoisContainingString(-1, "search_rectangle", exp.seqKymos.seq);
				}
			}});
		
	}
	
	// -------------------------------------------------
	
	int getSpanDiffTop() 
	{
		return (int) spanTopSpinner.getValue() ;
	}
		
	void kymosDisplayFiltered01(Experiment exp) 
	{
		SequenceKymos seqKymos = exp.seqKymos;
		if (seqKymos == null)
			return;
		ImageTransformEnums transform01 = (ImageTransformEnums) transform01ComboBox.getSelectedItem();
		
		List<Capillary> capList = exp.capillaries.capillariesList;
		for (int t=0; t < exp.seqKymos.seq.getSizeT(); t++) 
			getInfosFromDialog(capList.get(t));		
		
		int zChannelDestination = 1;
		exp.kymosBuildFiltered01(0, zChannelDestination, transform01, getSpanDiffTop());
		seqKymos.seq.getFirstViewer().getCanvas().setPositionZ(zChannelDestination);
	}
	
	void kymosDisplayFiltered02(Experiment exp) 
	{
		SequenceKymos seqKymos = exp.seqKymos;
		if (seqKymos == null)
			return;
		ImageTransformEnums transform02 = (ImageTransformEnums) transform02ComboBox.getSelectedItem();
		
		List<Capillary> capList = exp.capillaries.capillariesList;
		for (int t=0; t < exp.seqKymos.seq.getSizeT(); t++) 
			getInfosFromDialog(capList.get(t));		
		
		int zChannelDestination = 1;
		exp.kymosBuildFiltered01(0, zChannelDestination, transform02, getSpanDiffTop());
		seqKymos.seq.getFirstViewer().getCanvas().setPositionZ(zChannelDestination);
	}
	
	void allowItemsAccordingToSelection() 
	{
		boolean flag = false;
		switch ((ImageTransformEnums) transform02ComboBox.getSelectedItem())
		{
		case SUBTRACT_1RSTCOL:
		case L1DIST_TO_1RSTCOL:
			flag = true;
			break;

		default:
			break;
		}
		transform2EnableInputs(flag);
	}
	
	void transform2EnableInputs(boolean enable) 
	{
//		direction2ComboBox.setEnabled(enable);
		threshold2Spinner.setEnabled(enable);
	}
	
	void setInfosToDialog(Capillary cap) 
	{
		BuildSeriesOptions options = cap.limitsOptions;
		
		pass1CheckBox.setSelected(options.pass1);
		pass2CheckBox.setSelected(options.pass2);
		
		transform01ComboBox.setSelectedItem(options.transform01);
		int index = options.directionUp1 ? 0:1;
		direction1ComboBox.setSelectedIndex(index);
		threshold1Spinner.setValue(options.detectLevel1Threshold);
		
		transform02ComboBox.setSelectedItem(options.transform02);
		index = options.directionUp2 ? 0:1;
		direction2ComboBox.setSelectedIndex(index);
		threshold2Spinner.setValue(options.detectLevel2Threshold);
		
		allKymosCheckBox.setSelected(options.detectAllKymos);
		leftCheckBox.setSelected(options.detectL);
		rightCheckBox.setSelected(options.detectR);
	}
	
	void getInfosFromDialog(Capillary cap) 
	{
		BuildSeriesOptions capOptions 		= cap.limitsOptions;
		capOptions.pass1 					= pass1CheckBox.isSelected();
		capOptions.pass2 					= pass2CheckBox.isSelected();
		capOptions.transform01 				= (ImageTransformEnums) transform01ComboBox.getSelectedItem();
		capOptions.transform02 				= (ImageTransformEnums) transform02ComboBox.getSelectedItem();
		capOptions.directionUp1 			= (direction1ComboBox.getSelectedIndex() == 0) ;
		capOptions.detectLevel1Threshold 	= (int) threshold1Spinner.getValue();
		capOptions.directionUp2 			= (direction2ComboBox.getSelectedIndex() == 0) ;
		capOptions.detectLevel2Threshold 	= (int) threshold2Spinner.getValue();
		capOptions.detectAllKymos 			= allKymosCheckBox.isSelected();
		capOptions.detectL 					= leftCheckBox.isSelected();
		capOptions.detectR 					= rightCheckBox.isSelected();
	}
	
	private BuildSeriesOptions initBuildParameters(Experiment exp) 
	{	
		BuildSeriesOptions options = new BuildSeriesOptions();
		// list of stack experiments
		options.expList = parent0.expListCombo; 
		options.expList.index0 = parent0.expListCombo.getSelectedIndex();
		if (allSeriesCheckBox.isSelected()) 
			options.expList.index1 = options.expList.getItemCount()-1;
		else
			options.expList.index1 = parent0.expListCombo.getSelectedIndex();
		// list of kymographs
		options.detectAllKymos = allKymosCheckBox.isSelected();
		parent0.paneKymos.tabDisplay.indexImagesCombo = parent0.paneKymos.tabDisplay.kymographsCombo.getSelectedIndex();
		if (!allKymosCheckBox.isSelected()) 
		{
			options.kymoFirst = parent0.paneKymos.tabDisplay.indexImagesCombo;
			options.kymoLast = options.kymoFirst;
		}
		else
		{
			options.kymoFirst = 0;
			options.kymoLast = parent0.paneKymos.tabDisplay.kymographsCombo.getItemCount()-1;
		}
		// other parameters
		options.pass1 				= pass1CheckBox.isSelected();
		options.transform01 		= (ImageTransformEnums) transform01ComboBox.getSelectedItem();
		options.directionUp1 		= (direction1ComboBox.getSelectedIndex() == 0);
		options.detectLevel1Threshold= (int) threshold1Spinner.getValue();
		
		options.pass2 				= pass2CheckBox.isSelected();
		options.transform02			= (ImageTransformEnums) transform02ComboBox.getSelectedItem();
		options.directionUp2 		= (direction2ComboBox.getSelectedIndex() == 0);
		options.detectLevel2Threshold= (int) threshold2Spinner.getValue();
		
		options.analyzePartOnly		= fromCheckBox.isSelected();
		options.searchArea.x 		= (int) startSpinner.getValue();
		options.searchArea.width 	= (int) endSpinner.getValue()+ (int) startSpinner.getValue(); 
		options.spanDiffTop			= getSpanDiffTop();
		options.detectL 			= leftCheckBox.isSelected();
		options.detectR				= rightCheckBox.isSelected();
		options.parent0Rect 		= parent0.mainFrame.getBoundsInternal();
		options.binSubDirectory 	= parent0.expListCombo.expListBinSubDirectory ;
		options.runBackwards		= runBackwardsCheckBox.isSelected();
		return options;
	}
	
	void startComputation() 
	{
		Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();	
		if (exp != null)
		{
			threadDetectLevels = new DetectLevels();
			threadDetectLevels.options = initBuildParameters(exp);
			
			threadDetectLevels.addPropertyChangeListener(this);
			threadDetectLevels.execute();
			detectButton.setText("STOP");
		}
	}

	private void stopComputation() 
	{	
		if (threadDetectLevels != null && !threadDetectLevels.stopFlag) 
			threadDetectLevels.stopFlag = true;
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) 
	{
		 if (StringUtil.equals("thread_ended", evt.getPropertyName())) 
		 {
			detectButton.setText(detectString);
			parent0.paneKymos.tabDisplay.selectKymographImage(parent0.paneKymos.tabDisplay.indexImagesCombo);
			parent0.paneKymos.tabDisplay.indexImagesCombo = -1;
			
			startSpinner.setValue(threadDetectLevels.options.searchArea.x); 
			endSpinner.setValue(threadDetectLevels.options.searchArea.width+ threadDetectLevels.options.searchArea.x); 
		 }
	}

}
