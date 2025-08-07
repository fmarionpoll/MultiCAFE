package plugins.fmp.multicafe.dlg.cells;

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
import plugins.fmp.multicafe.tools.JComponents.CellTableModel;

public class InfosCellsTable extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7599620793495187279L;
	IcyFrame dialogFrame = null;
	private JTable tableView = new JTable();
	private CellTableModel cageTableModel = null;
	private JButton copyButton = new JButton("Copy table");
	private JButton pasteButton = new JButton("Paste");
	private JButton duplicateAllButton = new JButton("Duplicate cell to all");
	private JButton noFliesButton = new JButton("Cell 0/9: no flies");
	private MultiCAFE parent0 = null;
	private List<Cage> cageArrayCopy = null;

	// -------------------------

	public void initialize(MultiCAFE parent0, List<Cage> cageCopy) {
		this.parent0 = parent0;
		cageArrayCopy = cageCopy;

		cageTableModel = new CellTableModel(parent0.expListCombo);
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
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null) {
					cageArrayCopy.clear();
					for (Cage cell : exp.cells.cageList) {
						cageArrayCopy.add(cell);
					}
					pasteButton.setEnabled(true);
				}
			}
		});

		pasteButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null) {
					for (Cage cellFrom : cageArrayCopy) {
						cellFrom.valid = false;
						for (Cage cellTo : exp.cells.cageList) {
							if (!cellFrom.cageRoi2D.getName().equals(cellTo.cageRoi2D.getName()))
								continue;
							cellFrom.valid = true;
							cellTo.cageNFlies = cellFrom.cageNFlies;
							cellTo.cageAge = cellFrom.cageAge;
							cellTo.cageComment = cellFrom.cageComment;
							cellTo.cageSex = cellFrom.cageSex;
							cellTo.cageStrain = cellFrom.cageStrain;
						}
					}
					cageTableModel.fireTableDataChanged();
				}
			}
		});

		duplicateAllButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null) {
					int rowIndex = tableView.getSelectedRow();
					int columnIndex = tableView.getSelectedColumn();
					if (rowIndex >= 0) {
						Cage cell0 = exp.cells.cageList.get(rowIndex);
						for (Cage cell : exp.cells.cageList) {
							if (cell.cageRoi2D.getName().equals(cell0.cageRoi2D.getName()))
								continue;
							switch (columnIndex) {
							case 1:
								cell.cageNFlies = cell0.cageNFlies;
								break;
							case 2:
								cell.cageStrain = cell0.cageStrain;
								break;
							case 3:
								cell.cageSex = cell0.cageSex;
								break;
							case 4:
								cell.cageAge = cell0.cageAge;
								break;
							case 5:
								cell.cageComment = cell0.cageComment;
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
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null) {
					exp.cells.setFirstAndLastCageToZeroFly();
					cageTableModel.fireTableDataChanged();
				}
			}
		});

	}

	void close() {
		dialogFrame.close();
		Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
		if (exp != null) {
			exp.cells.transferNFliesFromCagesToCapillaries(exp.capillaries.capillariesList);
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
