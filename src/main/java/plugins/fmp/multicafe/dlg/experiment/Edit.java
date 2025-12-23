package plugins.fmp.multicafe.dlg.experiment;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import plugins.fmp.multicafe.MultiCAFE;
import plugins.fmp.multicafe.fmp_experiment.Experiment;
import plugins.fmp.multicafe.fmp_tools.toExcel.enums.EnumXLSColumnHeader;

public class Edit extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2190848825783418962L;

	private JComboBox<EnumXLSColumnHeader> fieldNamesCombo = new JComboBox<EnumXLSColumnHeader>(
			new EnumXLSColumnHeader[] { EnumXLSColumnHeader.EXP_EXPT, EnumXLSColumnHeader.EXP_BOXID,
					EnumXLSColumnHeader.EXP_STIM1, EnumXLSColumnHeader.EXP_CONC1, EnumXLSColumnHeader.EXP_STRAIN,
					EnumXLSColumnHeader.EXP_SEX, EnumXLSColumnHeader.EXP_STIM2, EnumXLSColumnHeader.EXP_CONC2,
					EnumXLSColumnHeader.CAP_STIM, EnumXLSColumnHeader.CAP_CONC, EnumXLSColumnHeader.CAP_VOLUME });

	private JComboBox<String> fieldOldValuesCombo = new JComboBox<String>();
	private JTextField newValueTextField = new JTextField(10);
	private JButton applyButton = new JButton("Apply");
	private MultiCAFE parent0 = null;
	boolean disableChangeFile = false;
	// JComboBoxExperimentLazy editExpList = new JComboBoxExperimentLazy();

	void init(GridLayout capLayout, MultiCAFE parent0) {
		this.parent0 = parent0;
		setLayout(capLayout);

		FlowLayout flowlayout = new FlowLayout(FlowLayout.LEFT);
		flowlayout.setVgap(1);

		int bWidth = 100;
		int bHeight = 21;

		JPanel panel0 = new JPanel(flowlayout);
		panel0.add(new JLabel("Field name "));
		panel0.add(fieldNamesCombo);
		fieldNamesCombo.setPreferredSize(new Dimension(bWidth, bHeight));
		add(panel0);

		bWidth = 200;
		JPanel panel1 = new JPanel(flowlayout);
		panel1.add(new JLabel("Field value "));
		panel1.add(fieldOldValuesCombo);
		fieldOldValuesCombo.setPreferredSize(new Dimension(bWidth, bHeight));
		add(panel1);

		JPanel panel2 = new JPanel(flowlayout);
		panel2.add(new JLabel("replace with"));
		panel2.add(newValueTextField);
		newValueTextField.setPreferredSize(new Dimension(bWidth, bHeight));
		panel2.add(applyButton);
		add(panel2);

		defineActionListeners();
	}

	public void initEditCombos() {
		parent0.expListComboLazy.setExperimentsFromList(parent0.expListComboLazy.getExperimentsAsList());
		// Use parent0.expListComboLazy to get values from ALL experiments
		parent0.expListComboLazy.getFieldValuesToComboLightweight(fieldOldValuesCombo,
				(EnumXLSColumnHeader) fieldNamesCombo.getSelectedItem());
	}

	private void defineActionListeners() {
		applyButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				applyChange();
				newValueTextField.setText("");
				initEditCombos();
			}
		});

		fieldNamesCombo.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				// Use parent0.expListComboLazy to get values from ALL experiments, not just
				// editExpList
				parent0.expListComboLazy.getFieldValuesToComboLightweight(fieldOldValuesCombo,
						(EnumXLSColumnHeader) fieldNamesCombo.getSelectedItem());
			}
		});
	}

	void applyChange() {
		int nExperiments = parent0.expListComboLazy.getItemCount();
		EnumXLSColumnHeader fieldEnumCode = (EnumXLSColumnHeader) fieldNamesCombo.getSelectedItem();
		String oldValue = (String) fieldOldValuesCombo.getSelectedItem();
		String newValue = newValueTextField.getText();

		for (int i = 0; i < nExperiments; i++) {
			Experiment exp = parent0.expListComboLazy.getItemAt(i);
			exp.load_MS96_experiment();
			exp.load_MS96_cages();

			if (fieldEnumCode == EnumXLSColumnHeader.CAP_STIM || fieldEnumCode == EnumXLSColumnHeader.CAP_CONC
					|| fieldEnumCode == EnumXLSColumnHeader.CAP_VOLUME) {
				exp.loadCapillaries();
				exp.replaceCapillariesFieldIfEqualOldValue(fieldEnumCode, oldValue, newValue);
				exp.saveMCCapillaries_Only();

			} else {
				exp.replaceExperimentFieldIfEqualOldValue(fieldEnumCode, oldValue, newValue);
				exp.save_MS96_experiment();
				exp.save_MS96_cages();
			}
		}

		parent0.expListComboLazy.getFieldValuesToComboLightweight(fieldOldValuesCombo,
				(EnumXLSColumnHeader) fieldNamesCombo.getSelectedItem());
	}

}
