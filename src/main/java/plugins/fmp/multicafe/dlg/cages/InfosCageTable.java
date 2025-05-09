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
import plugins.fmp.multicafe.experiment.cages.Cell;
import plugins.fmp.multicafe.tools.JComponents.CageTableModel;

public class InfosCageTable extends JPanel {
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
	private JButton noFliesButton = new JButton("Cages0/9: no flies");
	private MultiCAFE parent0 = null;
	private List<Cell> cageArrayCopy = null;

	// -------------------------

	public void initialize(MultiCAFE parent0, List<Cell> cageCopy) {
		this.parent0 = parent0;
		cageArrayCopy = cageCopy;

		cageTableModel = new CageTableModel(parent0.expListCombo);
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

		dialogFrame = new IcyFrame("Cages properties", true, true);
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
					for (Cell cage : exp.cages.cellList) {
						cageArrayCopy.add(cage);
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
					for (Cell cageFrom : cageArrayCopy) {
						cageFrom.valid = false;
						for (Cell cageTo : exp.cages.cellList) {
							if (!cageFrom.cellRoi2D.getName().equals(cageTo.cellRoi2D.getName()))
								continue;
							cageFrom.valid = true;
							cageTo.cellNFlies = cageFrom.cellNFlies;
							cageTo.cellAge = cageFrom.cellAge;
							cageTo.strCellComment = cageFrom.strCellComment;
							cageTo.strCellSex = cageFrom.strCellSex;
							cageTo.strCellStrain = cageFrom.strCellStrain;
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
						Cell cage0 = exp.cages.cellList.get(rowIndex);
						for (Cell cage : exp.cages.cellList) {
							if (cage.cellRoi2D.getName().equals(cage0.cellRoi2D.getName()))
								continue;
							switch (columnIndex) {
							case 1:
								cage.cellNFlies = cage0.cellNFlies;
								break;
							case 2:
								cage.strCellStrain = cage0.strCellStrain;
								break;
							case 3:
								cage.strCellSex = cage0.strCellSex;
								break;
							case 4:
								cage.cellAge = cage0.cellAge;
								break;
							case 5:
								cage.strCellComment = cage0.strCellComment;
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
					exp.cages.setFirstAndLastCellToZeroFly();
					cageTableModel.fireTableDataChanged();
				}
			}
		});

	}

	void close() {
		dialogFrame.close();
		Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
		if (exp != null) {
			exp.cages.transferNFliesFromCageToCapillaries(exp.capillaries.capillariesList);
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
