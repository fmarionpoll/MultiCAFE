package plugins.fmp.multicafe2.dlg.levels;

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import icy.gui.util.GuiUtil;
import icy.gui.viewer.Viewer;
import icy.sequence.Sequence;
import icy.sequence.SequenceEvent;
import icy.sequence.SequenceListener;
import plugins.fmp.multicafe2.MultiCAFE2;
import plugins.fmp.multicafe2.experiment.Capillaries;
import plugins.fmp.multicafe2.experiment.Capillary;
import plugins.fmp.multicafe2.experiment.Experiment;
import plugins.fmp.multicafe2.tools.chart.ChartLevels;
import plugins.fmp.multicafe2.tools.toExcel.EnumXLSExportType;


public class Graphs extends JPanel implements SequenceListener
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -7079184380174992501L;
	private ChartLevels plotTopAndBottom	= null;
	private ChartLevels plotDelta 			= null;
	private ChartLevels plotDerivative 		= null;
	private ChartLevels plotSumgulps 		= null;
	private MultiCAFE2 	parent0 			= null;
	
	private JCheckBox 	limitsCheckbox 				= new JCheckBox("top/bottom", true);
	private JCheckBox 	derivativeCheckbox 			= new JCheckBox("derivative", false);
	private JCheckBox 	consumptionCheckbox 		= new JCheckBox("sumGulps", false);
	private JCheckBox 	deltaCheckbox 				= new JCheckBox("delta (Vt - Vt-1)", false);
	private JCheckBox 	correctEvaporationCheckbox 	= new JCheckBox("correct evaporation", false);
	private JButton 	displayResultsButton 		= new JButton("Display results");
	
	
	void init(GridLayout capLayout, MultiCAFE2 parent0) 
	{	
		setLayout(capLayout);
		this.parent0 = parent0;
		setLayout(capLayout);
		FlowLayout layout = new FlowLayout(FlowLayout.LEFT);
		layout.setVgap(0);
		
		JPanel panel = new JPanel(layout);
		panel.add(limitsCheckbox);
		panel.add(derivativeCheckbox);
		panel.add(consumptionCheckbox);
		panel.add(deltaCheckbox);
		add(panel);
		JPanel panel1 = new JPanel(layout);
		panel1.add(correctEvaporationCheckbox);
		add(panel1);
		
		add(GuiUtil.besidesPanel(displayResultsButton, new JLabel(" "))); 
		defineActionListeners();
	}
	
	private void defineActionListeners() 
	{
		displayResultsButton.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{ 
				Experiment exp = (Experiment)  parent0.expListCombo.getSelectedItem();
				if (exp != null) 
				{
					exp.seqKymos.validateRois();
					exp.seqKymos.transferKymosRoisToCapillaries_Measures(exp.capillaries);
					displayGraphsPanels(exp);
				}
			}});
		
		correctEvaporationCheckbox.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{ 
				Experiment exp = (Experiment)  parent0.expListCombo.getSelectedItem();
				if (exp != null) 
				{
					displayGraphsPanels(exp);
				}
			}});
	}
	
	public void displayGraphsPanels(Experiment exp) 
	{
		Point ptRelative = new Point(0, 0);
		Rectangle rectv = new Rectangle(50, 500, 10, 10);
		Viewer v = exp.seqCamData.seq.getFirstViewer();
		if (v != null) {
			rectv = v.getBounds();
			ptRelative.y = rectv.height;
		}
		else
		{
			Rectangle rect0 = parent0.mainFrame.getBoundsInternal();
			rectv.setLocation(rect0.x /*+ rect0.width*/, rect0.y + 300); // rect0.height);
		}
			
		int dx = 5;
		int dy = 10; 
		exp.seqKymos.seq.addListener(this);
		
		if (limitsCheckbox.isSelected() && isThereAnyDataToDisplay(exp, EnumXLSExportType.TOPLEVEL)
				&& isThereAnyDataToDisplay(exp, EnumXLSExportType.BOTTOMLEVEL))  
		{
			plotTopAndBottom = plotToChart(exp, "top + bottom levels", 
					EnumXLSExportType.TOPLEVEL, 
					plotTopAndBottom, rectv, ptRelative);
			ptRelative.translate(dx, dy);
		}
		else if (plotTopAndBottom != null) 
			closeChart(plotTopAndBottom);
		
		if (deltaCheckbox.isSelected() && isThereAnyDataToDisplay(exp, EnumXLSExportType.TOPLEVELDELTA))  
		{
			plotDelta = plotToChart(exp, "top delta t -(t-1)", 
					EnumXLSExportType.TOPLEVELDELTA, 
					plotDelta, rectv, ptRelative);
			ptRelative.translate(dx, dy);
		}
		else if (plotDelta != null) 
			closeChart(plotDelta);
		
		if (derivativeCheckbox.isSelected()&& isThereAnyDataToDisplay(exp, EnumXLSExportType.DERIVEDVALUES))   
		{
			plotDerivative = plotToChart(exp, "Derivative", 
					EnumXLSExportType.DERIVEDVALUES, 
					plotDerivative, rectv, ptRelative);
			ptRelative.translate(dx, dy); 
		}
		else if (plotDerivative != null) 
			closeChart(plotDerivative);
		
		if (consumptionCheckbox.isSelected()&& isThereAnyDataToDisplay(exp, EnumXLSExportType.SUMGULPS))  
		{
			plotSumgulps = plotToChart(exp, "Cumulated gulps", 
					EnumXLSExportType.SUMGULPS, 
					plotSumgulps, rectv, ptRelative);
			ptRelative.translate(dx, dy); 
		}
		else if (plotSumgulps != null) 
			closeChart(plotSumgulps);
	}
	
	private ChartLevels plotToChart(Experiment exp, String title, EnumXLSExportType option, 
											ChartLevels iChart, Rectangle rectv, Point ptRelative ) 
	{	
		if (iChart != null) 
			iChart.mainChartFrame.dispose();
		iChart = new ChartLevels();
		iChart.createChartPanel(parent0, title);
		if (ptRelative != null)
			iChart.setLocationRelativeToRectangle(rectv, ptRelative);
		iChart.displayData(exp, option, correctEvaporationCheckbox.isSelected());
		iChart.mainChartFrame.toFront();
		iChart.mainChartFrame.requestFocus();
		return iChart;
	}
	
	public void closeAllCharts() 
	{
		plotTopAndBottom = closeChart (plotTopAndBottom); 
		plotDerivative = closeChart (plotDerivative); 
		plotSumgulps = closeChart (plotSumgulps); 
		plotDelta = closeChart (plotDelta);	
	}
	
	private ChartLevels closeChart(ChartLevels chart) 
	{
		if (chart != null) 
			chart.mainChartFrame.dispose();
		chart = null;
		return chart;
	}

	private boolean isThereAnyDataToDisplay(Experiment exp, EnumXLSExportType option) 
	{
		boolean flag = false;
		Capillaries capillaries = exp.capillaries;
		for (Capillary cap: capillaries.capillariesList) 
		{
			flag = cap.isThereAnyMeasuresDone(option);
			if (flag)
				break;
		}
		return flag;
	}

	@Override
	public void sequenceChanged(SequenceEvent sequenceEvent) 
	{
	}

	@Override
	public void sequenceClosed(Sequence sequence) 
	{
		sequence.removeListener(this);
		closeAllCharts();
	}
}

