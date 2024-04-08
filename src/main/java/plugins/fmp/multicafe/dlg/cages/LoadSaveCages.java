package plugins.fmp.multicafe.dlg.cages;

import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import icy.gui.frame.progress.ProgressFrame;
import icy.gui.util.FontUtil;
import plugins.fmp.multicafe.MultiCAFE;
import plugins.fmp.multicafe.experiment.Experiment;



public class LoadSaveCages extends JPanel 
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -5257698990389571518L;
	private JButton		openCagesButton			= new JButton("Load...");
	private JButton		saveCagesButton			= new JButton("Save...");
	private MultiCAFE 	parent0					= null;
	
	void init(GridLayout capLayout, MultiCAFE parent0) 
	{
		setLayout(capLayout);
		this.parent0 = parent0;

		FlowLayout flowLayout = new FlowLayout(FlowLayout.RIGHT);
		flowLayout.setVgap(0);
		JPanel panel1 = new JPanel(flowLayout);
		JLabel loadsaveText = new JLabel ("-> File (xml) ", SwingConstants.RIGHT);
		loadsaveText.setFont(FontUtil.setStyle(loadsaveText.getFont(), Font.ITALIC));
		panel1.add(loadsaveText);
		panel1.add(openCagesButton);
		panel1.add(saveCagesButton);
		panel1.validate();
		add(panel1);
		
		defineActionListeners();
	}
	
	private void defineActionListeners() 
	{
		openCagesButton.addActionListener(new ActionListener () 
		{
			@Override public void actionPerformed( final ActionEvent e ) 
			{ 
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null)
				{
					loadCages(exp);
					firePropertyChange("LOAD_DATA", false, true);
					parent0.paneCages.tabsPane.setSelectedIndex(3);
				}
			}});
		
		saveCagesButton.addActionListener(new ActionListener () 
		{
			@Override public void actionPerformed( final ActionEvent e ) 
			{ 
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null)
				{
					saveCagesAndMeasures(exp);
					parent0.paneCages.tabsPane.setSelectedIndex(3);
				}
			}});
	}

	public boolean loadCages(Experiment exp) 
	{	
		if (exp == null)
			return false;
		ProgressFrame progress = new ProgressFrame("load fly positions");
		
		boolean flag = exp.loadCagesMeasures();
		if (flag) 
		{
//			parent0.paneCages.tabGraphics.moveCheckbox.setEnabled(true);
//			parent0.paneCages.tabGraphics.displayResultsButton.setEnabled(true);
			exp.updateROIsAt(0);
		}
		progress.close();
		return flag;
	}
	
	public void saveCagesAndMeasures(Experiment exp) 
	{
		if (exp != null) 
		{
			exp.cages.cagesFromROIs(exp.seqCamData);
			exp.saveCagesMeasures();
		}
	}
	
}