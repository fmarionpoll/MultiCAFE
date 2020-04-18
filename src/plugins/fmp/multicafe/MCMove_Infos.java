package plugins.fmp.multicafe;


import java.awt.Dimension;
import java.awt.GridLayout;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import plugins.fmp.multicafeSequence.CageTableModel;




public class MCMove_Infos  extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = -3325915033686366985L;
    private JTable tableView = new JTable();
	private AbstractTableModel viewModel;
	
    
	
	void init(GridLayout capLayout, MultiCAFE parent0) {
//		setLayout(capLayout);
		setLayout(new GridLayout(1,0));		 
		viewModel = new CageTableModel(parent0);
		
	    tableView.setModel(viewModel);
	    tableView.setPreferredScrollableViewportSize(new Dimension(500, 70));
	    tableView.setFillsViewportHeight(true);
	    TableColumnModel columnModel = tableView.getColumnModel();
        setFixedColumnProperties(columnModel.getColumn(0));
        setFixedColumnProperties(columnModel.getColumn(1));

        //Create the scroll pane and add the table to it.
        JScrollPane scrollPane = new JScrollPane(tableView);
        //Add the scroll pane to this panel.
        add(scrollPane);
	}
	
	private void setFixedColumnProperties (TableColumn column) {
        column.setResizable(false);
        column.setPreferredWidth(50);
        column.setMaxWidth(50);
        column.setMinWidth(30);
	}

}
