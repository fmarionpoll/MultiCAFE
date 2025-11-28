package plugins.fmp.multicafe.dlg.cages;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import icy.gui.frame.IcyFrame;
import plugins.fmp.multicafe.MultiCAFE;
import plugins.fmp.multicafe.experiment.Experiment;
import plugins.fmp.multicafe.experiment.cages.Cage;
import plugins.fmp.multicafe.tools.JComponents.CageTableModel;

public class InfosCagesTable extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7599620793495187279L;
	IcyFrame dialogFrame = null;
	private JTable tableView = new JTable();
	private CageTableModel cageTableModel = null;
	private JButton copyButton = new JButton("Copy table");
	private JButton pasteButton = new JButton("Paste");
	private JButton duplicateAllButton = new JButton("Duplicate cell to all");
	private JButton noFliesButton = new JButton("Cage 0/9: no flies");
	private MultiCAFE parent0 = null;
	private List<Cage> cageArrayCopy = null;

	// -------------------------

	public void initialize(MultiCAFE parent0, List<Cage> cageCopy) {
		this.parent0 = parent0;
		cageArrayCopy = cageCopy;

		cageTableModel = new CageTableModel(parent0.expListComboLazy);
		tableView.setModel(cageTableModel);
		tableView.setPreferredScrollableViewportSize(new Dimension(500, 400));
		tableView.setFillsViewportHeight(true);
		TableColumnModel columnModel = tableView.getColumnModel();
		for (int i = 0; i < 2; i++)
			setFixedColumnProperties(columnModel.getColumn(i));
		JScrollPane scrollPane = new JScrollPane(tableView);

		JPanel topPanel = new JPanel(new GridLayout(2, 1));
		FlowLayout flowLayout = new FlowLayout(FlowLayout.LEFT);
		JPanel panel1 = new JPanel(flowLayout);
		panel1.add(copyButton);
		panel1.add(pasteButton);
		topPanel.add(panel1);

		JPanel panel2 = new JPanel(flowLayout);
		panel2.add(duplicateAllButton);
		panel2.add(noFliesButton);
		topPanel.add(panel2);

		JPanel tablePanel = new JPanel();
		tablePanel.add(scrollPane);

		dialogFrame = new IcyFrame("Cell properties", true, true);
		dialogFrame.add(topPanel, BorderLayout.NORTH);
		dialogFrame.add(tablePanel, BorderLayout.CENTER);

		dialogFrame.pack();
		dialogFrame.addToDesktopPane();
		dialogFrame.requestFocus();
		dialogFrame.center();
		dialogFrame.setVisible(true);
		defineActionListeners();
		pasteButton.setEnabled(cageArrayCopy.size() > 0);
	}

	private void defineActionListeners() {
		copyButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListComboLazy.getSelectedItem();
				if (exp != null) {
					cageArrayCopy.clear();
					for (Cage cell : exp.getCages().getCageList()) {
						cageArrayCopy.add(cell);
					}
					pasteButton.setEnabled(true);
				}
			}
		});

		pasteButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListComboLazy.getSelectedItem();
				if (exp != null) {
					for (Cage cellFrom : cageArrayCopy) {
						cellFrom.setValid(false);
						for (Cage cellTo : exp.getCages().getCageList()) {
							if (!cellFrom.getCageRoi2D().getName().equals(cellTo.getCageRoi2D().getName()))
								continue;
							cellFrom.setValid(true);
							cellTo.setCageNFlies(cellFrom.getCageNFlies());
							cellTo.setCageAge(cellFrom.getCageAge());
							cellTo.setCageComment(cellFrom.getCageComment());
							cellTo.setCageSex(cellFrom.getCageSex());
							cellTo.setCageStrain(cellFrom.getCageStrain());
						}
					}
					cageTableModel.fireTableDataChanged();
				}
			}
		});

		duplicateAllButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListComboLazy.getSelectedItem();
				if (exp != null) {
					int rowIndex = tableView.getSelectedRow();
					int columnIndex = tableView.getSelectedColumn();
					if (rowIndex >= 0) {
						Cage cell0 = exp.getCages().getCageList().get(rowIndex);
						for (Cage cell : exp.getCages().getCageList()) {
							if (cell.getCageRoi2D().getName().equals(cell0.getCageRoi2D().getName()))
								continue;
							switch (columnIndex) {
							case 1:
								cell.setCageNFlies(cell0.getCageNFlies());
								break;
							case 2:
								cell.setCageStrain(cell0.getCageStrain());
								break;
							case 3:
								cell.setCageSex(cell0.getCageSex());
								break;
							case 4:
								cell.setCageAge(cell0.getCageAge());
								break;
							case 5:
								cell.setCageComment(cell0.getCageComment());
								break;
							default:
								break;
							}
						}
					}
					cageTableModel.fireTableDataChanged();
				}
			}
		});

		noFliesButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListComboLazy.getSelectedItem();
				if (exp != null) {
					exp.getCages().setFirstAndLastCageToZeroFly();
					cageTableModel.fireTableDataChanged();
				}
			}
		});

	}

	void close() {
		dialogFrame.close();
		Experiment exp = (Experiment) parent0.expListComboLazy.getSelectedItem();
		if (exp != null) {
			exp.getCages().transferNFliesFromCagesToCapillaries(exp.getCapillaries().getCapillariesList());
			parent0.paneCapillaries.tabFile.saveCapillaries_file(exp);
		}
	}

	private void setFixedColumnProperties(TableColumn column) {
		column.setResizable(false);
		column.setPreferredWidth(50);
		column.setMaxWidth(50);
		column.setMinWidth(30);
	}

}
