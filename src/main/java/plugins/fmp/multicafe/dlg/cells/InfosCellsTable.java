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
import plugins.fmp.multicafe.experiment.cells.Cell;
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
	private List<Cell> cageArrayCopy = null;

	// -------------------------

	public void initialize(MultiCAFE parent0, List<Cell> cageCopy) {
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
					for (Cell cell : exp.cells.cellList) {
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
					for (Cell cellFrom : cageArrayCopy) {
						cellFrom.valid = false;
						for (Cell cellTo : exp.cells.cellList) {
							if (!cellFrom.cellRoi2D.getName().equals(cellTo.cellRoi2D.getName()))
								continue;
							cellFrom.valid = true;
							cellTo.cellNFlies = cellFrom.cellNFlies;
							cellTo.cellAge = cellFrom.cellAge;
							cellTo.cellComment = cellFrom.cellComment;
							cellTo.cellSex = cellFrom.cellSex;
							cellTo.cellStrain = cellFrom.cellStrain;
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
						Cell cell0 = exp.cells.cellList.get(rowIndex);
						for (Cell cell : exp.cells.cellList) {
							if (cell.cellRoi2D.getName().equals(cell0.cellRoi2D.getName()))
								continue;
							switch (columnIndex) {
							case 1:
								cell.cellNFlies = cell0.cellNFlies;
								break;
							case 2:
								cell.cellStrain = cell0.cellStrain;
								break;
							case 3:
								cell.cellSex = cell0.cellSex;
								break;
							case 4:
								cell.cellAge = cell0.cellAge;
								break;
							case 5:
								cell.cellComment = cell0.cellComment;
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
					exp.cells.setFirstAndLastCellToZeroFly();
					cageTableModel.fireTableDataChanged();
				}
			}
		});

	}

	void close() {
		dialogFrame.close();
		Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
		if (exp != null) {
			exp.cells.transferNFliesFromCellsToCapillaries(exp.capillaries.capillariesList);
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
