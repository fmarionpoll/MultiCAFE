package plugins.fmp.multicafe2.dlg.experiment;

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


import plugins.fmp.multicafe2.MultiCAFE2;
import plugins.fmp.multicafe2.dlg.JComponents.ExperimentCombo;
import plugins.fmp.multicafe2.experiment.Experiment;
import plugins.fmp.multicafe2.tools.toExcel.EnumXLSColumnHeader;

public class Edit   extends JPanel 
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 2190848825783418962L;

	private JComboBox<EnumXLSColumnHeader>	headersCombo = new JComboBox<EnumXLSColumnHeader>
			(new EnumXLSColumnHeader[] {
					EnumXLSColumnHeader.EXPT
					, EnumXLSColumnHeader.BOXID
					, EnumXLSColumnHeader.COMMENT1
					, EnumXLSColumnHeader.COMMENT2
					, EnumXLSColumnHeader.STRAIN
					, EnumXLSColumnHeader.SEX
//					, EnumXLSColumnHeader.CAPSTIM
//					, EnumXLSColumnHeader.CAPCONC
					});
	
	private JComboBox<String>	descriptorsCombo	= new JComboBox<String>();
	private JTextField			newValue 			= new JTextField (10);
	private JButton				applyButton 		= new JButton("Apply");
	private MultiCAFE2 			parent0 			= null;
			boolean 			disableChangeFile 	= false;
			ExperimentCombo 	editExpList 		= new ExperimentCombo();
	
	
	void init(GridLayout capLayout, MultiCAFE2 parent0) 
	{
		this.parent0 = parent0;
		setLayout(capLayout);
			
		FlowLayout flowlayout = new FlowLayout(FlowLayout.LEFT);
		flowlayout.setVgap(1);
		
		int bWidth = 100;
		int bHeight = 21;
		
		JPanel panel0 = new JPanel (flowlayout);
		panel0.add(new JLabel("Infos field "));
		panel0.add(headersCombo);
		headersCombo.setPreferredSize(new Dimension(bWidth, bHeight));
		add(panel0);
				
		JPanel panel1 = new JPanel(flowlayout);
		panel1.add(new JLabel("Field value "));
		panel1.add(descriptorsCombo);
		descriptorsCombo.setPreferredSize(new Dimension(bWidth, bHeight));
		panel1.add(new JLabel(" replace with "));
		panel1.add(newValue);
		panel1.add(applyButton);
		add (panel1);
	
		defineActionListeners();
	}
	
	public void initEditCombos() 
	{
		editExpList.setExperimentsFromList(parent0.expListCombo.getExperimentsAsList()); 
		editExpList.getFieldValuesToCombo(descriptorsCombo, (EnumXLSColumnHeader) headersCombo.getSelectedItem());
	}
	
	
	private void defineActionListeners() 
	{
		applyButton.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{ 
				applyChange();
				newValue.setText("");
				initEditCombos();
			}});
		
		headersCombo.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{ 
				editExpList.getFieldValuesToCombo(descriptorsCombo, (EnumXLSColumnHeader) headersCombo.getSelectedItem());
			}});
	}
	
	void applyChange() 
	{
		int nitems = editExpList.getItemCount();
		EnumXLSColumnHeader headerItem = (EnumXLSColumnHeader) headersCombo.getSelectedItem();
		String filter = (String) descriptorsCombo.getSelectedItem();
		
		for (int i = 0; i < nitems; i++)
		{
			Experiment exp = editExpList.getItemAt(i);
			String pattern = exp.getField(headerItem);
			if (pattern .equals(filter)) 
			{
				 exp.setField(headerItem, newValue.getText());
				 exp.xmlSaveMCExperiment();
			}
		}
		
	}
	


}

