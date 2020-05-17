package plugins.fmp.multicafe;

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;

import icy.gui.util.GuiUtil;
import plugins.fmp.multicafeSequence.Experiment;


public class MCSequence_Intervals extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5739112045358747277L;
	JSpinner 	startFrameJSpinner	= new JSpinner(new SpinnerNumberModel(0, 0, 10000, 1)); 
	JSpinner 	endFrameJSpinner	= new JSpinner(new SpinnerNumberModel(99999999, 1, 99999999, 1));
	JSpinner 	stepFrameJSpinner	= new JSpinner(new SpinnerNumberModel(1, 1, 1000, 1));
	JRadioButton  isFixedFrame		= new JRadioButton("keep the same intervals for all experiment", false);
	JRadioButton  isFloatingFrame	= new JRadioButton("analyze complete experiments", true);
		
	
	void init(GridLayout capLayout) {
		setLayout(capLayout);	

		FlowLayout layout1 = new FlowLayout(FlowLayout.LEFT);
		layout1.setVgap(0);
		
		JPanel panel1 = new JPanel(layout1);
		panel1.add(new JLabel("Analyze from ", SwingConstants.RIGHT));
		panel1.add(startFrameJSpinner);
		panel1.add(new JLabel(" to "));
		panel1.add(endFrameJSpinner);
		panel1.add(new JLabel(" step "));
		panel1.add(stepFrameJSpinner );
		add(GuiUtil.besidesPanel(panel1));
		
		FlowLayout layout2 = new FlowLayout(FlowLayout.LEFT);
		layout2.setVgap(0);
		JPanel panel2 = new JPanel(layout2);
		panel2.add(isFixedFrame);
		panel2.add(isFloatingFrame);
		ButtonGroup group = new ButtonGroup();
		group.add(isFloatingFrame);
		group.add(isFixedFrame);
		
		add(GuiUtil.besidesPanel(panel2));
		
		defineActionListeners();
		startFrameJSpinner.setEnabled(false); 
		endFrameJSpinner.setEnabled(false);
	}
	
	
	private void defineActionListeners() {
		isFixedFrame.addActionListener(new ActionListener () { @Override public void actionPerformed( final ActionEvent e ) {
				startFrameJSpinner.setEnabled(true); 
				endFrameJSpinner.setEnabled(true);
			}});
		isFloatingFrame.addActionListener(new ActionListener () { @Override public void actionPerformed( final ActionEvent e ) {
				startFrameJSpinner.setEnabled(false); 
				endFrameJSpinner.setEnabled(false);
			}});
	}
		
	public void setAnalyzeFrameToDialog (Experiment exp) {
		startFrameJSpinner.setValue((int) exp.getStartFrame());
		endFrameJSpinner.setValue((int) exp.getSeqCamSizeT());
		int step = exp.checkStepFrame();
		if (step <= 0 )
			exp.setStepFrame(1);
		stepFrameJSpinner.setValue(exp.getStepFrame());
	}
	
	void getAnalyzeFrameFromDialog (Experiment exp) {		
		exp.setStartFrame ((int) startFrameJSpinner.getValue());
		exp.setEndFrame ( (int) endFrameJSpinner.getValue());
		exp.setStepFrame ( (int) stepFrameJSpinner.getValue());
	}
	
	public void setEndFrameToDialog (int end) {
		endFrameJSpinner.setValue(end);		
	}
	
	int getStepFrame() {
		return (int) stepFrameJSpinner.getValue();
	}
	
	boolean getIsFixedFrame() {
		return isFixedFrame.isSelected();
	}
	
	int getStartFrame() {
		return (int) startFrameJSpinner.getValue();
	}
	
	int getEndFrame() {
		return (int) endFrameJSpinner.getValue();
	}
}
