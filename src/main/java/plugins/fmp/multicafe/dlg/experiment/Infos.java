package plugins.fmp.multicafe.dlg.experiment;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
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

	private JComboBox<String> stim1Combo = new JComboBox<String>(new SortedComboBoxModel());
	private JComboBox<String> stim2Combo = new JComboBox<String>(new SortedComboBoxModel());
	private JComboBox<String> boxIDCombo = new JComboBox<String>(new SortedComboBoxModel());
	private JComboBox<String> exptCombo = new JComboBox<String>(new SortedComboBoxModel());
	private JComboBox<String> strainCombo = new JComboBox<String>(new SortedComboBoxModel());
	private JComboBox<String> sexCombo = new JComboBox<String>(new SortedComboBoxModel());
	private JComboBox<String> conc1Combo = new JComboBox<String>(new SortedComboBoxModel());
	private JComboBox<String> conc2Combo = new JComboBox<String>(new SortedComboBoxModel());

	private JLabel experimentLabel = new JLabel(EnumXLSColumnHeader.EXP_EXPT.toString());
	private JLabel boxIDLabel = new JLabel(EnumXLSColumnHeader.EXP_BOXID.toString());
	private JLabel stim1Label = new JLabel(EnumXLSColumnHeader.EXP_STIM.toString());
	private JLabel conc1Label = new JLabel(EnumXLSColumnHeader.EXP_CONC.toString());
	private JLabel strainLabel = new JLabel(EnumXLSColumnHeader.EXP_STRAIN.toString());
	private JLabel sexLabel = new JLabel(EnumXLSColumnHeader.EXP_SEX.toString());
	private JLabel stim2Label = new JLabel(EnumXLSColumnHeader.EXP_COND1.toString());
	private JLabel conc2Label = new JLabel(EnumXLSColumnHeader.EXP_COND2.toString());

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

		c.gridy = 0;
		addLineOfElements(c, experimentLabel, exptCombo, boxIDLabel, boxIDCombo, openButton);
		c.gridy = 1;
		addLineOfElements(c, strainLabel, strainCombo, sexLabel, sexCombo, saveButton);
		c.gridy = 2;
		addLineOfElements(c, stim1Label, stim1Combo, conc1Label, stim2Combo, duplicateButton);
		c.gridy = 3;
		addLineOfElements(c, stim2Label, conc1Combo, conc2Label, conc2Combo, zoomButton);

		zoomButton.setEnabled(false);
		boxIDCombo.setEditable(true);
		exptCombo.setEditable(true);
		stim1Combo.setEditable(true);
		stim2Combo.setEditable(true);
		strainCombo.setEditable(true);
		sexCombo.setEditable(true);
		conc1Combo.setEditable(true);
		conc2Combo.setEditable(true);

		defineActionListeners();
	}

	void addLineOfElements(GridBagConstraints c, JComponent element1, JComponent element2, JComponent element3,
			JComponent element4, JComponent element5) {
		c.gridx = 0;
		int delta1 = 1;
		int delta2 = 3;
		if (element1 != null)
			add(element1, c);
		c.gridx += delta1;
		if (element2 != null)
			add(element2, c);
		c.gridx += delta2;
		if (element3 != null)
			add(element3, c);
		c.gridx += delta1;
		if (element4 != null)
			add(element4, c);
		c.gridx += delta2;
		if (element5 != null)
			add(element5, c);
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
		setInfoCombo(exp_destination, exp_source, stim1Combo, EnumXLSColumnHeader.EXP_STIM);
		setInfoCombo(exp_destination, exp_source, stim2Combo, EnumXLSColumnHeader.EXP_CONC);
		setInfoCombo(exp_destination, exp_source, strainCombo, EnumXLSColumnHeader.EXP_STRAIN);
		setInfoCombo(exp_destination, exp_source, sexCombo, EnumXLSColumnHeader.EXP_SEX);
		setInfoCombo(exp_destination, exp_source, conc1Combo, EnumXLSColumnHeader.EXP_COND1);
		setInfoCombo(exp_destination, exp_source, conc2Combo, EnumXLSColumnHeader.EXP_COND2);
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
		exp.setExperimentFieldNoTest(EnumXLSColumnHeader.EXP_STIM, (String) stim1Combo.getSelectedItem());
		exp.setExperimentFieldNoTest(EnumXLSColumnHeader.EXP_CONC, (String) stim2Combo.getSelectedItem());
		exp.setExperimentFieldNoTest(EnumXLSColumnHeader.EXP_STRAIN, (String) strainCombo.getSelectedItem());
		exp.setExperimentFieldNoTest(EnumXLSColumnHeader.EXP_SEX, (String) sexCombo.getSelectedItem());
		exp.setExperimentFieldNoTest(EnumXLSColumnHeader.EXP_COND1, (String) conc1Combo.getSelectedItem());
		exp.setExperimentFieldNoTest(EnumXLSColumnHeader.EXP_COND2, (String) conc2Combo.getSelectedItem());
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
		parent0.expListCombo.getFieldValuesToCombo(stim1Combo, EnumXLSColumnHeader.EXP_STIM);
		parent0.expListCombo.getFieldValuesToCombo(stim2Combo, EnumXLSColumnHeader.EXP_CONC);
		parent0.expListCombo.getFieldValuesToCombo(boxIDCombo, EnumXLSColumnHeader.EXP_BOXID);
		parent0.expListCombo.getFieldValuesToCombo(strainCombo, EnumXLSColumnHeader.EXP_STRAIN);
		parent0.expListCombo.getFieldValuesToCombo(sexCombo, EnumXLSColumnHeader.EXP_SEX);
		parent0.expListCombo.getFieldValuesToCombo(conc1Combo, EnumXLSColumnHeader.EXP_COND1);
		parent0.expListCombo.getFieldValuesToCombo(conc2Combo, EnumXLSColumnHeader.EXP_COND2);
		Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
		if (exp != null)
			transferPreviousExperimentInfosToDialog(exp, exp);
	}

	public void clearCombos() {
		exptCombo.removeAllItems();
		stim1Combo.removeAllItems();
		stim2Combo.removeAllItems();
		boxIDCombo.removeAllItems();
		strainCombo.removeAllItems();
		sexCombo.removeAllItems();
		conc1Combo.removeAllItems();
		conc2Combo.removeAllItems();
	}

	void duplicatePreviousDescriptors() {
		int iprevious = parent0.expListCombo.getSelectedIndex() - 1;
		if (iprevious < 0)
			return;

		Experiment exp0 = (Experiment) parent0.expListCombo.getItemAt(iprevious);
		Experiment exp = (Experiment) parent0.expListCombo.getItemAt(iprevious + 1);
		transferPreviousExperimentInfosToDialog(exp0, exp);
		transferPreviousExperimentCapillariesInfos(exp0, exp);
	}

	void transferPreviousExperimentCapillariesInfos(Experiment exp0, Experiment exp) {
		exp.capillaries.capillariesDescription.grouping = exp0.capillaries.capillariesDescription.grouping;
		parent0.paneCapillaries.tabCreate.setGroupedBy2(exp0.capillaries.capillariesDescription.grouping == 2);
		exp.capillaries.capillariesDescription.volume = exp0.capillaries.capillariesDescription.volume;
		parent0.paneCapillaries.tabInfos.setAllDescriptors(exp0.capillaries);
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
