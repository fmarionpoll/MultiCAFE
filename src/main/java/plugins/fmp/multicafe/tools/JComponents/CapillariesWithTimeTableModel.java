package plugins.fmp.multicafe.tools.JComponents;

import java.util.ArrayList;

import javax.swing.table.AbstractTableModel;

import plugins.fmp.multicafe.experiment.Experiment;
import plugins.fmp.multicafe.experiment.capillaries.Capillaries;

public class CapillariesWithTimeTableModel extends AbstractTableModel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private ExperimentsJComboBox expList = null;
	private final String columnNames[] = { "Starting at frame", "End frame" };
	private ArrayList<Long[]> intervals = null;

	public CapillariesWithTimeTableModel(ExperimentsJComboBox expList) {
		super();
		this.expList = expList;
	}

	@Override
	public int getRowCount() {
		if (expList != null && expList.getSelectedIndex() >= 0) {
			Capillaries capillaries = getCapillariesOfSelectedExperiment();
			intervals = capillaries.getKymoIntervalsFromCapillaries().intervals;
			return intervals.size();
		}
		return 0;
	}

	private Capillaries getCapillariesOfSelectedExperiment() {
		Experiment exp = (Experiment) expList.getSelectedItem();
		return exp.capillaries;
	}

	@Override
	public int getColumnCount() {
		return 1;
//		return columnNames.length;
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
//    	switch (columnIndex) {
//    	case 0: return Integer.class;
//    	case 1: return Integer.class;
//        }
		return Integer.class;
	}

	@Override
	public String getColumnName(int column) {
		return columnNames[column];
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		Long[] interval = intervals.get(rowIndex);
		return interval[columnIndex];
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return true;
	}

	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		Long[] interval = intervals.get(rowIndex);
//		switch (columnIndex) {
//		case 0:  
//		case 1: 
		interval[columnIndex] = (long) aValue;
//			break;
//	    }
	}

}
