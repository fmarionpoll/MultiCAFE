package plugins.fmp.multicafe.dlg.experiment;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;

import plugins.fmp.multicafe.MultiCAFE;
import plugins.fmp.multicafe.experiment.Experiment;
import plugins.fmp.multicafe.tools1.JComponents.JComboBoxExperimentLazy;
import plugins.fmp.multicafe.tools.JComponents.SortedComboBoxModel;
import plugins.fmp.multicafe.tools.toExcel.EnumXLSColumnHeader;

public class Filter extends JPanel {
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
//	private JComboBox<String> comboStimCheck = new JComboBox<String>(new SortedComboBoxModel());
//	private JComboBox<String> comboConcCheck = new JComboBox<String>(new SortedComboBoxModel());

	private JCheckBox experimentCheck = new JCheckBox(EnumXLSColumnHeader.EXP_EXPT.toString());
	private JCheckBox boxIDCheck = new JCheckBox(EnumXLSColumnHeader.EXP_BOXID.toString());
	private JCheckBox stim1Check = new JCheckBox(EnumXLSColumnHeader.EXP_STIM.toString());
	private JCheckBox conc1Check = new JCheckBox(EnumXLSColumnHeader.EXP_CONC.toString());
	private JCheckBox strainCheck = new JCheckBox(EnumXLSColumnHeader.EXP_STRAIN.toString());
	private JCheckBox sexCheck = new JCheckBox(EnumXLSColumnHeader.EXP_SEX.toString());
	private JCheckBox stim2Check = new JCheckBox(EnumXLSColumnHeader.EXP_COND1.toString());
	private JCheckBox conc2Check = new JCheckBox(EnumXLSColumnHeader.EXP_COND2.toString());
//	private JCheckBox capStimCheck = new JCheckBox(EnumXLSColumnHeader.CAP_STIM.toString());
//	private JCheckBox capConcCheck = new JCheckBox(EnumXLSColumnHeader.CAP_CONC.toString());

	private JButton applyButton = new JButton("Apply");
	private JButton clearButton = new JButton("Clear");

	private MultiCAFE parent0 = null;
	boolean disableChangeFile = false;
	public JComboBoxExperimentLazy filterExpList = new JComboBoxExperimentLazy();

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
		addLineOfElements(c, experimentCheck, exptCombo, boxIDCheck, boxIDCombo, applyButton);
		c.gridy = 1;
		addLineOfElements(c, strainCheck, strainCombo, sexCheck, sexCombo, clearButton);
		c.gridy = 2;
		addLineOfElements(c, stim1Check, stim1Combo, conc1Check, stim2Combo, null);
		c.gridy = 3;
		addLineOfElements(c, stim2Check, conc1Combo, conc2Check, conc2Combo, null);

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

	public void initFilterCombos() {
		if (!parent0.paneBrowse.panelLoadSave.filteredCheck.isSelected())
			filterExpList.setExperimentsFromList(parent0.expListComboLazy.getExperimentsAsList());
		filterExpList.getFieldValuesToComboLightweight(exptCombo, EnumXLSColumnHeader.EXP_EXPT);
		filterExpList.getFieldValuesToComboLightweight(stim1Combo, EnumXLSColumnHeader.EXP_STIM);
		filterExpList.getFieldValuesToComboLightweight(stim2Combo, EnumXLSColumnHeader.EXP_CONC);
		filterExpList.getFieldValuesToComboLightweight(boxIDCombo, EnumXLSColumnHeader.EXP_BOXID);
		filterExpList.getFieldValuesToComboLightweight(sexCombo, EnumXLSColumnHeader.EXP_SEX);
		filterExpList.getFieldValuesToComboLightweight(strainCombo, EnumXLSColumnHeader.EXP_STRAIN);
		filterExpList.getFieldValuesToComboLightweight(conc1Combo, EnumXLSColumnHeader.EXP_COND1);
		filterExpList.getFieldValuesToComboLightweight(conc2Combo, EnumXLSColumnHeader.EXP_COND2);
	}

	private void defineActionListeners() {
		applyButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				filterExperimentList(true);
				parent0.paneExperiment.tabsPane.setSelectedIndex(0);
			}
		});

		clearButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				filterExperimentList(false);
			}
		});
	}

	public void filterExperimentList(boolean setFilter) {
		if (setFilter) {
			parent0.expListComboLazy.setExperimentsFromList(filterAllItems());
		} else {
			clearAllCheckBoxes();
			parent0.expListComboLazy.setExperimentsFromList(filterExpList.getExperimentsAsList());
		}

		if (parent0.expListComboLazy.getItemCount() > 0)
			parent0.expListComboLazy.setSelectedIndex(0);
		if (setFilter != parent0.paneBrowse.panelLoadSave.filteredCheck.isSelected())
			parent0.paneBrowse.panelLoadSave.filteredCheck.setSelected(setFilter);
	}

	public void clearAllCheckBoxes() {
		boolean select = false;
		experimentCheck.setSelected(select);
		boxIDCheck.setSelected(select);
		stim1Check.setSelected(select);
		conc1Check.setSelected(select);
		strainCheck.setSelected(select);
		sexCheck.setSelected(select);
		stim2Check.setSelected(select);
		conc2Check.setSelected(select);
	}

	private List<Experiment> filterAllItems() {
		List<Experiment> filteredList = new ArrayList<Experiment>(filterExpList.getExperimentsAsList());
		if (experimentCheck.isSelected())
			filterItem(filteredList, EnumXLSColumnHeader.EXP_EXPT, (String) exptCombo.getSelectedItem());
		if (boxIDCheck.isSelected())
			filterItem(filteredList, EnumXLSColumnHeader.EXP_BOXID, (String) boxIDCombo.getSelectedItem());
		if (stim1Check.isSelected())
			filterItem(filteredList, EnumXLSColumnHeader.EXP_STIM, (String) stim1Combo.getSelectedItem());
		if (conc1Check.isSelected())
			filterItem(filteredList, EnumXLSColumnHeader.EXP_CONC, (String) stim2Combo.getSelectedItem());
		if (sexCheck.isSelected())
			filterItem(filteredList, EnumXLSColumnHeader.EXP_SEX, (String) sexCombo.getSelectedItem());
		if (strainCheck.isSelected())
			filterItem(filteredList, EnumXLSColumnHeader.EXP_STRAIN, (String) strainCombo.getSelectedItem());
		if (stim2Check.isSelected())
			filterItem(filteredList, EnumXLSColumnHeader.EXP_COND1, (String) conc1Combo.getSelectedItem());
		if (conc2Check.isSelected())
			filterItem(filteredList, EnumXLSColumnHeader.EXP_COND2, (String) conc2Combo.getSelectedItem());
		return filteredList;
	}

	void filterItem(List<Experiment> filteredList, EnumXLSColumnHeader header, String filter) {
		Iterator<Experiment> iterator = filteredList.iterator();
		while (iterator.hasNext()) {
			Experiment exp = iterator.next();
			int compare = exp.getProperties().getExperimentField(header).compareTo(filter);
			if (compare != 0)
				iterator.remove();
		}
	}

}
