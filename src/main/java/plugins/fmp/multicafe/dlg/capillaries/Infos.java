package plugins.fmp.multicafe.dlg.capillaries;

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;

import plugins.fmp.multicafe.MultiCAFE;
import plugins.fmp.multicafe.experiment.Experiment;
import plugins.fmp.multicafe.experiment.capillaries.Capillaries;
import plugins.fmp.multicafe.experiment.capillaries.Capillary;

public class Infos extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 4950182090521600937L;

	private JSpinner capillaryVolumeSpinner = new JSpinner(new SpinnerNumberModel(5., 0., 100., 1.));
	private JSpinner capillaryPixelsSpinner = new JSpinner(new SpinnerNumberModel(5, 0, 1000, 1));
	private JButton getCapillaryLengthButton = new JButton("pixels 1rst capillary");
	private JButton editCapillariesButton = new JButton("Edit capillaries infos...");
	private MultiCAFE parent0 = null;
	private InfosCapillaryTable infosCapillaryTable = null;
	private List<Capillary> capillariesArrayCopy = new ArrayList<Capillary>();

	void init(GridLayout capLayout, MultiCAFE parent0) {
		setLayout(capLayout);
		this.parent0 = parent0;

		JPanel panel0 = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 1));
		panel0.add(new JLabel("volume (µl) ", SwingConstants.RIGHT));
		panel0.add(capillaryVolumeSpinner);
		panel0.add(new JLabel("length (pixels) ", SwingConstants.RIGHT));
		panel0.add(capillaryPixelsSpinner);
		panel0.add(getCapillaryLengthButton);
		add(panel0);

		JPanel panel1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 1));
		panel1.add(editCapillariesButton);
		add(panel1);

		defineActionListeners();
	}

	private void defineActionListeners() {
		getCapillaryLengthButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				double npixels = getLengthFirstCapillaryROI();
				capillaryPixelsSpinner.setValue((int) npixels);
			}
		});

		editCapillariesButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null) {
					exp.capillaries.transferDescriptionToCapillaries();
					if (infosCapillaryTable != null) {
						infosCapillaryTable.close();
					}
					infosCapillaryTable = new InfosCapillaryTable();
					infosCapillaryTable.initialize(parent0, capillariesArrayCopy);
					infosCapillaryTable.requestFocus();
				}
			}
		});
	}

	// set/ get

	void setAllDescriptors(Capillaries cap) {
		capillaryVolumeSpinner.setValue(cap.capillariesDescription.volume);
		capillaryPixelsSpinner.setValue(cap.capillariesDescription.pixels);
	}

	void getDescriptors(Capillaries capList) {
		capList.capillariesDescription.volume = (double) capillaryVolumeSpinner.getValue();
		capList.capillariesDescription.pixels = (int) capillaryPixelsSpinner.getValue();
	}

	public int getLengthFirstCapillaryROI() {
		Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
		int npixels = 0;
		if (exp != null) {
			exp.capillaries.updateCapillariesFromSequence(exp.seqCamData.seq);
			if (exp.capillaries.capillariesList.size() > 0) {
				Capillary cap = exp.capillaries.capillariesList.get(0);
				npixels = cap.getCapillaryROILength();
			}
		}
		return npixels;
	}

}
