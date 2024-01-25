package plugins.fmp.multicafe2.dlg.levels;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JToggleButton;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;

import icy.util.StringUtil;
import plugins.fmp.multicafe2.MultiCAFE2;
import plugins.fmp.multicafe2.experiment.Capillary;
import plugins.fmp.multicafe2.experiment.Experiment;
import plugins.fmp.multicafe2.series.DetectGulps;
import plugins.fmp.multicafe2.tools.KymosCanvas2D;
import plugins.fmp.multicafe2.tools.Image.ImageTransformEnums;
import plugins.fmp.multicafe2.series.BuildSeriesOptions;




public class LevelsToGulps extends JPanel  implements PropertyChangeListener 
{
	/**
	 * 
	 */
	private static final long 	serialVersionUID 		= -5590697762090397890L;
	
	JCheckBox				allKymosCheckBox 			= new JCheckBox ("all kymographs", true);
	ImageTransformEnums[] 	gulpTransforms = new ImageTransformEnums[] {
			ImageTransformEnums.XDIFFN , ImageTransformEnums.YDIFFN, 
			ImageTransformEnums.YDIFFN2, ImageTransformEnums.XYDIFFN
	};
	
	JComboBox<ImageTransformEnums> gulpTransformsComboBox 
						= new JComboBox<ImageTransformEnums> (gulpTransforms);
	JSpinner				startSpinner				= new JSpinner(new SpinnerNumberModel(0, 0, 100000, 1));
	JSpinner				endSpinner					= new JSpinner(new SpinnerNumberModel(3, 1, 100000, 1));
	JCheckBox				buildDerivativeCheckBox 	= new JCheckBox ("derivative", true);
	JCheckBox				detectGulpsCheckBox 		= new JCheckBox ("gulps", true);
	
	private JCheckBox		partCheckBox 				= new JCheckBox ("from (pixel)", false);
	private JToggleButton	gulpTransformDisplayButton	= new JToggleButton("Display");
	private JSpinner		spanTransf2Spinner			= new JSpinner(new SpinnerNumberModel(3, 0, 500, 1));
	private JSpinner 		detectGulpsThresholdSpinner	= new JSpinner(new SpinnerNumberModel(.5, 0., 500., .1));
	private String 			detectString 				= "        Detect     ";
	private JButton 		detectButton 				= new JButton(detectString);
	private JCheckBox 		allCheckBox 				= new JCheckBox("ALL (current to last)", false);
	private DetectGulps 	threadDetectGulps 			= null;
	private MultiCAFE2 		parent0						= null;
	
	
	void init(GridLayout capLayout, MultiCAFE2 parent0) 
	{
		setLayout(capLayout);
		this.parent0 = parent0;
		
		FlowLayout layoutLeft = new FlowLayout(FlowLayout.LEFT); 
		layoutLeft.setVgap(0);
		
		JPanel panel0 = new JPanel(layoutLeft);
		panel0.add( detectButton);
		panel0.add( allCheckBox);
		panel0.add(allKymosCheckBox);
		panel0.add(buildDerivativeCheckBox);
		panel0.add(detectGulpsCheckBox);
		add(panel0 );
		
		JPanel panel01 = new JPanel(layoutLeft);
		panel01.add(new JLabel("threshold", SwingConstants.RIGHT));
		panel01.add(detectGulpsThresholdSpinner);
		panel01.add(gulpTransformsComboBox);
		panel01.add(gulpTransformDisplayButton);
		add (panel01);
		
		JPanel panel1 = new JPanel(layoutLeft);
		panel1.add(partCheckBox);
		panel1.add(startSpinner);
		panel1.add(new JLabel("to"));
		panel1.add(endSpinner);
		add( panel1);

		gulpTransformsComboBox.setSelectedItem(ImageTransformEnums.XDIFFN);
		defineActionListeners();
	}
	
	private void defineActionListeners() 
	{
		gulpTransformsComboBox.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{ 
				Experiment exp = (Experiment)  parent0.expListCombo.getSelectedItem();
				if (exp != null && exp.seqCamData != null) 
				{
					int index = gulpTransformsComboBox.getSelectedIndex();
					getKymosCanvas(exp).imageTransformFunctionsCombo.setSelectedIndex(index +1);
				}
			}});
		
		detectButton.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{
				if (detectButton.getText() .equals(detectString))
					startComputation(true);
				else 
					stopComputation();
			}});
		
		gulpTransformDisplayButton.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{
				Experiment exp =(Experiment)  parent0.expListCombo.getSelectedItem();
				if (exp != null && exp.seqCamData != null) 
				{
					if (gulpTransformDisplayButton.isSelected()) {
						KymosCanvas2D canvas = getKymosCanvas(exp);
						canvas.updateListOfImageTransformFunctions( gulpTransforms);
						int index = gulpTransformsComboBox.getSelectedIndex();
						canvas.selectImageTransformFunction(index +1);
					}
					else
						getKymosCanvas(exp).imageTransformFunctionsCombo.setSelectedIndex(0);
				}
			}});
		
		allCheckBox.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{
				Color color = Color.BLACK;
				if (allCheckBox.isSelected()) 
					color = Color.RED;
				allCheckBox.setForeground(color);
				detectButton.setForeground(color);
		}});
		
	}

	private BuildSeriesOptions initBuildParameters(Experiment exp ) 
	{	
		BuildSeriesOptions options = threadDetectGulps.options;
		options.expList = parent0.expListCombo; 
		options.expList.index0 = parent0.expListCombo.getSelectedIndex();
		
		if (allCheckBox.isSelected()) 
			options.expList.index1 = options.expList.getItemCount()-1;
		else
			options.expList.index1 = parent0.expListCombo.getSelectedIndex();

		options.detectAllKymos = allKymosCheckBox.isSelected();
		parent0.paneKymos.tabDisplay.indexImagesCombo = parent0.paneKymos.tabDisplay.kymographsCombo.getSelectedIndex();
		if (!allKymosCheckBox.isSelected()) {
			options.kymoFirst 	= parent0.paneKymos.tabDisplay.kymographsCombo.getSelectedIndex();
			options.kymoLast 	= options.kymoFirst;
		}
		else
		{
			options.kymoFirst = 0;
			options.kymoLast 	= parent0.paneKymos.tabDisplay.kymographsCombo.getItemCount()-1;
		}
		options.detectGulpsThreshold_uL = (double) detectGulpsThresholdSpinner.getValue();
		options.transformForGulps = (ImageTransformEnums) gulpTransformsComboBox.getSelectedItem();
		options.detectAllGulps 	= allKymosCheckBox.isSelected();
		options.spanDiff		= (int) spanTransf2Spinner.getValue();
		options.buildGulps		= detectGulpsCheckBox.isSelected();
		options.buildDerivative	= buildDerivativeCheckBox.isSelected();
		options.analyzePartOnly	= partCheckBox.isSelected();
		options.searchArea.x 	= (int) startSpinner.getValue();
		options.searchArea.width= (int) endSpinner.getValue()+ (int) startSpinner.getValue(); 
		options.parent0Rect 	= parent0.mainFrame.getBoundsInternal();
		options.binSubDirectory = (String) parent0.paneKymos.tabDisplay.getBinSubdirectory() ;
		return options;
	}
	
	void startComputation(boolean bDetectGulps) 
	{
		Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
		if (exp != null)
		{
			exp.saveCapillariesMeasures(exp.getKymosBinFullDirectory());
			threadDetectGulps = new DetectGulps();
			threadDetectGulps.options = initBuildParameters(exp);
			if (!bDetectGulps)
				threadDetectGulps.options.buildGulps = false;
			threadDetectGulps.addPropertyChangeListener(this);
			threadDetectGulps.execute();
			detectButton.setText("STOP");
		}
	}

	void setInfos(Capillary cap) 
	{
		BuildSeriesOptions options = cap.getGulpsOptions();
		detectGulpsThresholdSpinner.setValue(options.detectGulpsThreshold_uL);
		gulpTransformsComboBox.setSelectedItem(options.transformForGulps);
		allKymosCheckBox.setSelected(options.detectAllGulps);
	}

	private void stopComputation() 
	{	
		if (threadDetectGulps != null && !threadDetectGulps.stopFlag) {
			threadDetectGulps.stopFlag = true;
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) 
	{
		 if (StringUtil.equals("thread_ended", evt.getPropertyName())) 
		 {
			detectButton.setText(detectString);
			parent0.paneKymos.tabDisplay.selectKymographImage(parent0.paneKymos.tabDisplay.indexImagesCombo);
			parent0.paneKymos.tabDisplay.indexImagesCombo = -1;
			
			startSpinner.setValue(threadDetectGulps.options.searchArea.x); 
			endSpinner.setValue(threadDetectGulps.options.searchArea.width+ threadDetectGulps.options.searchArea.x); 

		 }
	}
	
	protected KymosCanvas2D getKymosCanvas(Experiment exp) 
	{
		KymosCanvas2D canvas = (KymosCanvas2D) exp.seqKymos.seq.getFirstViewer().getCanvas();
		return canvas;
	}
	
	

}
