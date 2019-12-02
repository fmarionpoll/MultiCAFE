package plugins.fmp.multicafe;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import icy.gui.util.GuiUtil;
import plugins.fmp.multicafeSequence.Experiment;
import plugins.fmp.multicafeSequence.SequenceCamData;
import plugins.fmp.multicafeSequence.SequenceKymos;


public class MCSequence_Close  extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = -7576474358794296471L;
	private JButton		closeAllButton			= new JButton("Close views");
	private MultiCAFE 	parent0 				= null;
	
	
	void init(GridLayout capLayout, MultiCAFE parent0) {
		setLayout(capLayout);
		this.parent0  = parent0;
		add( GuiUtil.besidesPanel(closeAllButton, new JLabel(" ")));
		closeAllButton.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
				closeAll();
				firePropertyChange("SEQ_CLOSE", false, true);
			}});
	}
	
	void closeAll() {
		if (parent0.currentExperimentIndex < 0)
			return;
		Experiment exp = parent0.expList.getExperiment(parent0.currentExperimentIndex);
		closeExp(exp);
	}
	
	void closeExp(Experiment exp) {
		if (exp != null) {
			parent0.sequencePane.infosTab.getExperimentInfosFromDialog(exp);
			parent0.sequencePane.intervalsTab.getAnalyzeFrameFromDialog (exp);
			parent0.kymosPane.createTab.getBuildKymosParametersFromDialog(exp);
			exp.xmlSaveExperiment();

			SequenceKymos seqKymos = exp.seqKymos;
			if (seqKymos != null && seqKymos.seq != null) {
				seqKymos.seq.removeAllROI();
				seqKymos.seq.close();
			}
			SequenceCamData seqCamData = exp.seqCamData;
			if (seqCamData != null && seqCamData.seq != null) {
				seqCamData.seq.removeAllROI();
				seqCamData.seq.close();
			}
		}
		
		parent0.movePane.graphicsTab.closeAll();
		parent0.levelsPane.graphsTab.closeAll();
		parent0.kymosPane.displayTab.kymographNamesComboBox.removeAllItems();
	}

}
