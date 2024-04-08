package plugins.fmp.multicafe.dlg.capillaries;


import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import icy.gui.viewer.Viewer;
import icy.util.StringUtil;
import plugins.fmp.multicafe.MultiCAFE;
import plugins.fmp.multicafe.experiment.Experiment;
import plugins.fmp.multicafe.experiment.SequenceCamData;
import plugins.fmp.multicafe.tools.EnumStatusComputation;
import plugins.fmp.multicafe.tools.Sequence.SequenceTransform;
import plugins.fmp.multicafe.tools.Sequence.SequenceTransformEnums;
import plugins.fmp.multicafe.tools.Sequence.SequenceTransformOptions;




public class FilterImage extends JPanel implements PropertyChangeListener 
{
	private static final long serialVersionUID = 1L;

	private JComboBox<String> directionComboBox = new JComboBox<String> (new String[] {" threshold >", " threshold <" });
	private JSpinner thresholdSpinner = new JSpinner(new SpinnerNumberModel(35, 1, 255, 1));
	JComboBox<SequenceTransformEnums> transformComboBox = new JComboBox<SequenceTransformEnums> (
		new SequenceTransformEnums[] {
			SequenceTransformEnums.HSB,  
			SequenceTransformEnums.H_HSB, 
			SequenceTransformEnums.S_HSB, 
			SequenceTransformEnums.B_HSB
		});
	
	private String 		calculateString 			= "Calculate";
	private JButton		displayTransformButton	= new JButton(calculateString);	
	private JSpinner	spanTopSpinner			= new JSpinner(new SpinnerNumberModel(3, 1, 100, 1));
	private String 		detectString 			= "        run Otsu     ";
	private JButton 	detectButton 			= new JButton(detectString);
	
	private MultiCAFE 	parent0 				= null;
	EnumStatusComputation sComputation 			= EnumStatusComputation.START_COMPUTATION; 
	private SequenceTransform threadBuildFiltered = null;
	
	// -----------------------------------------------------
		
	void init(GridLayout capLayout, MultiCAFE parent0) 
	{
		setLayout(capLayout);
		this.parent0 = parent0;
		
		FlowLayout layoutLeft = new FlowLayout(FlowLayout.LEFT); 
		
		JPanel panel0 = new JPanel(layoutLeft);
		((FlowLayout)panel0.getLayout()).setVgap(0);
		panel0.add(detectButton);

		add(panel0);
		
		JPanel panel01 = new JPanel(layoutLeft);
		panel01.add(directionComboBox);
		((JLabel) directionComboBox.getRenderer()).setHorizontalAlignment(JLabel.RIGHT);
		panel01.add(thresholdSpinner);
		panel01.add(transformComboBox);
		panel01.add(displayTransformButton);
		add (panel01);
		
		JPanel panel02 = new JPanel(layoutLeft);
		add (panel02);

		detectButton.setEnabled(false);
		directionComboBox.setEnabled(false);
		thresholdSpinner.setEnabled(false);
		transformComboBox.setSelectedIndex(2);
		
		defineActionListeners();
	}
	
	private void defineActionListeners() 
	{	
		transformComboBox.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{ 
				displayFilteredImage();
			}});
		
		displayTransformButton.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{ 
				displayFilteredImage();
			}});
	}
	
	// -------------------------------------------------
	
	int getSpanDiffTop() 
	{
		return (int) spanTopSpinner.getValue() ;
	}
		
	void displayFilteredImage() 
	{
		Experiment exp =(Experiment)  parent0.expListCombo.getSelectedItem();
		if (exp == null || exp.seqCamData == null)
			return;
		
		SequenceCamData seqCamData = exp.seqCamData;
		if (seqCamData == null)
			return;

		if (displayTransformButton.getText() .equals(calculateString))
		{
			initBuildParameters();
			startComputation();
		} else {
			stopComputation();
		}
	}
	
	private SequenceTransformOptions initBuildParameters() 
	{	
		SequenceTransformOptions options = new SequenceTransformOptions();
		// other parameters
		options.transform01 = (SequenceTransformEnums) transformComboBox.getSelectedItem();
		Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
		options.seq = exp.seqCamData.seq;
		return options;
	}
	
	void startComputation() 
	{
		sComputation = EnumStatusComputation.STOP_COMPUTATION;
		
		threadBuildFiltered = new SequenceTransform();	
		threadBuildFiltered.options = initBuildParameters();
		
		threadBuildFiltered.addPropertyChangeListener(this);
		threadBuildFiltered.execute();
		displayTransformButton.setText("STOP");
	}

	private void stopComputation() 
	{	
		if (threadBuildFiltered != null && !threadBuildFiltered.stopFlag) 
			threadBuildFiltered.stopFlag = true;
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) 
	{
		 if (StringUtil.equals("thread_ended", evt.getPropertyName())) 
		 {
			stopComputation();
			displayTransformButton.setText(calculateString);
			int zChannelDestination = 1;
			Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
			Viewer viewer = exp.seqCamData.seq.getFirstViewer();
			viewer.getCanvas().setPositionZ(zChannelDestination);
		 }
	}

}
