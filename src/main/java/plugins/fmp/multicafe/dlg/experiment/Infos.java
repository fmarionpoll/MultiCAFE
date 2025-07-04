package plugins.fmp.multicafe.dlg.experiment;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import icy.canvas.Canvas2D;
import icy.gui.viewer.Viewer;
import icy.sequence.Sequence;
import plugins.fmp.multicafe.MultiCAFE;
import plugins.fmp.multicafe.experiment.Experiment;
import plugins.fmp.multicafe.tools.JComponents.SortedComboBoxModel;
import plugins.fmp.multicafe.tools.toExcel.EnumXLSColumnHeader;

public class Infos extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2190848825783418962L;

	private JComboBox<String> cmt1Combo = new JComboBox<String>(new SortedComboBoxModel());
	private JComboBox<String> comt2Combo = new JComboBox<String>(new SortedComboBoxModel());
	private JComboBox<String> boxIDCombo = new JComboBox<String>(new SortedComboBoxModel());
	private JComboBox<String> exptCombo = new JComboBox<String>(new SortedComboBoxModel());
	private JComboBox<String> strainCombo = new JComboBox<String>(new SortedComboBoxModel());
	private JComboBox<String> sexCombo = new JComboBox<String>(new SortedComboBoxModel());
	private JComboBox<String> cond1Combo = new JComboBox<String>(new SortedComboBoxModel());
	private JComboBox<String> cond2Combo = new JComboBox<String>(new SortedComboBoxModel());

	private JLabel experimentCheck = new JLabel(EnumXLSColumnHeader.EXP_EXPT.toString());
	private JLabel boxIDCheck = new JLabel(EnumXLSColumnHeader.EXP_BOXID.toString());
	private JLabel comment1Check = new JLabel(EnumXLSColumnHeader.EXP_STIM.toString());
	private JLabel comment2Check = new JLabel(EnumXLSColumnHeader.EXP_CONC.toString());
	private JLabel strainCheck = new JLabel(EnumXLSColumnHeader.EXP_STRAIN.toString());
	private JLabel sexCheck = new JLabel(EnumXLSColumnHeader.EXP_SEX.toString());
	private JLabel cond1Check = new JLabel(EnumXLSColumnHeader.EXP_COND1.toString());
	private JLabel cond2Check = new JLabel(EnumXLSColumnHeader.EXP_COND2.toString());

	private JButton openButton = new JButton("Load...");
	private JButton saveButton = new JButton("Save...");
	private JButton duplicateButton = new JButton("Get previous");
	private JButton zoomButton = new JButton("zoom top");

	private MultiCAFE parent0 = null;
	public boolean disableChangeFile = false;

	void init(GridLayout capLayout, MultiCAFE parent0) {
		this.parent0 = parent0;
		GridBagLayout layoutThis = new GridBagLayout();
		setLayout(layoutThis);

		GridBagConstraints c = new GridBagConstraints();
		c.gridwidth = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.BASELINE;
		c.ipadx = 0;
		c.ipady = 0;
		c.insets = new Insets(1, 2, 1, 2);
		int delta1 = 1;
		int delta2 = 3;

		// line 0
		c.gridx = 0;
		c.gridy = 0;
		add(experimentCheck, c);
		c.gridx += delta1;
		add(exptCombo, c);
		c.gridx += delta2;
		add(boxIDCheck, c);
		c.gridx += delta1;
		add(boxIDCombo, c);
		c.gridx += delta2;
		add(openButton, c);
		// line 1
		c.gridy = 1;
		c.gridx = 0;
		add(comment1Check, c);
		c.gridx += delta1;
		add(cmt1Combo, c);
		c.gridx += delta2;
		add(comment2Check, c);
		c.gridx += delta1;
		add(comt2Combo, c);
		c.gridx += delta2;
		add(saveButton, c);
		// line 2
		c.gridy = 2;
		c.gridx = 0;
		add(strainCheck, c);
		c.gridx += delta1;
		add(strainCombo, c);
		c.gridx += delta2;
		add(sexCheck, c);
		c.gridx += delta1;
		add(sexCombo, c);
		c.gridx += delta2;
		add(duplicateButton, c);
		// line 3
		c.gridy = 3;
		c.gridx = 0;
		add(cond1Check, c);
		c.gridx += delta1;
		add(cond1Combo, c);
		c.gridx += delta2;
		add(cond2Check, c);
		c.gridx += delta1;
		add(cond2Combo, c);
		c.gridx += delta2;
		add(zoomButton, c);
		zoomButton.setEnabled(false);

		boxIDCombo.setEditable(true);
		exptCombo.setEditable(true);
		cmt1Combo.setEditable(true);
		comt2Combo.setEditable(true);
		strainCombo.setEditable(true);
		sexCombo.setEditable(true);
		cond1Combo.setEditable(true);
		cond2Combo.setEditable(true);

		defineActionListeners();
	}

	private void defineActionListeners() {
		openButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null) {
					exp.xmlLoad_MCExperiment();
					transferPreviousExperimentInfosToDialog(exp, exp);
				}
			}
		});

		saveButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null) {
					getExperimentInfosFromDialog(exp);
					exp.xmlSave_MCExperiment();
				}
			}
		});

		duplicateButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				duplicatePreviousDescriptors();
			}
		});

		zoomButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null)
					zoomToUpperCorner(exp);
			}
		});
	}

	// set/ get

	public void transferPreviousExperimentInfosToDialog(Experiment exp_source, Experiment exp_destination) {
		setInfoCombo(exp_destination, exp_source, boxIDCombo, EnumXLSColumnHeader.EXP_BOXID);
		setInfoCombo(exp_destination, exp_source, exptCombo, EnumXLSColumnHeader.EXP_EXPT);
		setInfoCombo(exp_destination, exp_source, cmt1Combo, EnumXLSColumnHeader.EXP_STIM);
		setInfoCombo(exp_destination, exp_source, comt2Combo, EnumXLSColumnHeader.EXP_CONC);
		setInfoCombo(exp_destination, exp_source, strainCombo, EnumXLSColumnHeader.EXP_STRAIN);
		setInfoCombo(exp_destination, exp_source, sexCombo, EnumXLSColumnHeader.EXP_SEX);
		setInfoCombo(exp_destination, exp_source, cond1Combo, EnumXLSColumnHeader.EXP_COND1);
		setInfoCombo(exp_destination, exp_source, cond2Combo, EnumXLSColumnHeader.EXP_COND2);
	}

	private void setInfoCombo(Experiment exp_dest, Experiment exp_source, JComboBox<String> combo,
			EnumXLSColumnHeader field) {
		String altText = exp_source.getExperimentField(field);
		String text = exp_dest.getExperimentField(field);
		if (text.equals(".."))
			exp_dest.setExperimentFieldNoTest(field, altText);
		text = exp_dest.getExperimentField(field);
		addItemToComboIfNew(text, combo);
		combo.setSelectedItem(text);
	}

	public void getExperimentInfosFromDialog(Experiment exp) {
		exp.setExperimentFieldNoTest(EnumXLSColumnHeader.EXP_BOXID, (String) boxIDCombo.getSelectedItem());
		exp.setExperimentFieldNoTest(EnumXLSColumnHeader.EXP_EXPT, (String) exptCombo.getSelectedItem());
		exp.setExperimentFieldNoTest(EnumXLSColumnHeader.EXP_STIM, (String) cmt1Combo.getSelectedItem());
		exp.setExperimentFieldNoTest(EnumXLSColumnHeader.EXP_CONC, (String) comt2Combo.getSelectedItem());
		exp.setExperimentFieldNoTest(EnumXLSColumnHeader.EXP_STRAIN, (String) strainCombo.getSelectedItem());
		exp.setExperimentFieldNoTest(EnumXLSColumnHeader.EXP_SEX, (String) sexCombo.getSelectedItem());
		exp.setExperimentFieldNoTest(EnumXLSColumnHeader.EXP_COND1, (String) cond1Combo.getSelectedItem());
		exp.setExperimentFieldNoTest(EnumXLSColumnHeader.EXP_COND2, (String) cond2Combo.getSelectedItem());
	}

	private void addItemToComboIfNew(String toAdd, JComboBox<String> combo) {
		if (toAdd == null)
			return;
		SortedComboBoxModel model = (SortedComboBoxModel) combo.getModel();
		if (model.getIndexOf(toAdd) == -1)
			model.addElement(toAdd);
	}

	public void initInfosCombos() {
		parent0.expListCombo.getFieldValuesToCombo(exptCombo, EnumXLSColumnHeader.EXP_EXPT);
		parent0.expListCombo.getFieldValuesToCombo(cmt1Combo, EnumXLSColumnHeader.EXP_STIM);
		parent0.expListCombo.getFieldValuesToCombo(comt2Combo, EnumXLSColumnHeader.EXP_CONC);
		parent0.expListCombo.getFieldValuesToCombo(boxIDCombo, EnumXLSColumnHeader.EXP_BOXID);
		parent0.expListCombo.getFieldValuesToCombo(strainCombo, EnumXLSColumnHeader.EXP_STRAIN);
		parent0.expListCombo.getFieldValuesToCombo(sexCombo, EnumXLSColumnHeader.EXP_SEX);
		parent0.expListCombo.getFieldValuesToCombo(cond1Combo, EnumXLSColumnHeader.EXP_COND1);
		parent0.expListCombo.getFieldValuesToCombo(cond2Combo, EnumXLSColumnHeader.EXP_COND2);
		Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
		if (exp != null)
			transferPreviousExperimentInfosToDialog(exp, exp);
	}

	public void clearCombos() {
		exptCombo.removeAllItems();
		cmt1Combo.removeAllItems();
		comt2Combo.removeAllItems();
		boxIDCombo.removeAllItems();
		strainCombo.removeAllItems();
		sexCombo.removeAllItems();
		cond1Combo.removeAllItems();
		cond2Combo.removeAllItems();
	}

	void duplicatePreviousDescriptors() {
		int iprevious = parent0.expListCombo.getSelectedIndex() - 1;
		if (iprevious < 0)
			return;

		Experiment exp0 = (Experiment) parent0.expListCombo.getItemAt(iprevious);
		Experiment exp = (Experiment) parent0.expListCombo.getItemAt(iprevious + 1);
		transferPreviousExperimentInfosToDialog(exp0, exp);
		parent0.paneCapillaries.transferPreviousExperimentCapillariesInfos(exp0, exp);
	}

	void zoomToUpperCorner(Experiment exp) {
		Sequence seq = exp.seqCamData.seq;
		Viewer v = seq.getFirstViewer();
		if (v != null) {
			Canvas2D canvas = (Canvas2D) v.getCanvas();
			canvas.setScale(2., 2., true);
			canvas.setOffset(0, 0, true);
		}

	}

}
