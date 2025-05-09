package plugins.fmp.multicafe.tools.JComponents;

import javax.swing.table.AbstractTableModel;

import plugins.fmp.multicafe.experiment.Experiment;
import plugins.fmp.multicafe.experiment.cage.Cell;

public class CellTableModel extends AbstractTableModel {
	/**
	 * 
	 */
	private static final long serialVersionUID = -3501225818220221949L;
	private ExperimentCombo expList = null;

	public CellTableModel(ExperimentCombo expList) {
		super();
		this.expList = expList;
	}

	@Override
	public int getColumnCount() {
		return 6;
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		switch (columnIndex) {
		case 0:
			return String.class;
		case 1:
			return Integer.class;
		case 2:
			return String.class;
		case 3:
			return String.class;
		case 4:
			return Integer.class;
		case 5:
			return String.class;
		}
		return String.class;
	}

	@Override
	public String getColumnName(int column) {
		switch (column) {
		case 0:
			return "Name";
		case 1:
			return "N flies";
		case 2:
			return "Strain";
		case 3:
			return "Sex";
		case 4:
			return "Age";
		case 5:
			return "Comment";
		}
		return "";
	}

	@Override
	public int getRowCount() {
		if (expList != null && expList.getSelectedIndex() >= 0) {
			Experiment exp = (Experiment) expList.getSelectedItem();
			return exp.cageBox.cellList.size();
		}
		return 0;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		Cell cell = null;
		if (expList != null && expList.getSelectedIndex() >= 0) {
			Experiment exp = (Experiment) expList.getSelectedItem();
			cell = exp.cageBox.cellList.get(rowIndex);
		}
		if (cell != null) {
			switch (columnIndex) {
			case 0:
				return cell.cellRoi2D.getName();
			case 1:
				return cell.cellNFlies;
			case 2:
				return cell.strCellStrain;
			case 3:
				return cell.strCellSex;
			case 4:
				return cell.cellAge;
			case 5:
				return cell.strCellComment;
			}
		}
		return null;
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		switch (columnIndex) {
		case 0:
			return false;
		default:
			return true;
		}
	}

	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		Cell cell = null;
		if (expList != null && expList.getSelectedIndex() >= 0) {
			Experiment exp = (Experiment) expList.getSelectedItem();
			cell = exp.cageBox.cellList.get(rowIndex);
		}
		if (cell != null) {
			switch (columnIndex) {
			case 0:
				cell.cellRoi2D.setName(aValue.toString());
				break;
			case 1:
				cell.cellNFlies = (int) aValue;
				break;
			case 2:
				cell.strCellStrain = aValue.toString();
				break;
			case 3:
				cell.strCellSex = aValue.toString();
				break;
			case 4:
				cell.cellAge = (int) aValue;
				break;
			case 5:
				cell.strCellComment = aValue.toString();
				break;
			}
		}
	}

}
