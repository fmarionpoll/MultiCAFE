package plugins.fmp.multicafe2.tools.chart;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import icy.gui.frame.IcyFrame;
import icy.gui.util.GuiUtil;
import icy.gui.viewer.Viewer;
import plugins.fmp.multicafe2.MultiCAFE2;
import plugins.fmp.multicafe2.experiment.Experiment;

import plugins.fmp.multicafe2.tools.toExcel.EnumXLSExportType;
import plugins.fmp.multicafe2.tools.toExcel.XLSExport;
import plugins.fmp.multicafe2.tools.toExcel.XLSExportOptions;
import plugins.fmp.multicafe2.tools.toExcel.XLSResults;
import plugins.fmp.multicafe2.tools.toExcel.XLSResultsArray;

public class ChartLevels extends IcyFrame  
{
	public JPanel 	mainChartPanel 	= null;
	public IcyFrame mainChartFrame 	= null;
	private MultiCAFE2 	parent0 	= null;
	
	private Point pt = new Point (0,0);
	private boolean flagMaxMinSet = false;
	private double globalYMax = 0;
	private double globalYMin = 0;
	private double globalXMin = 0;
	private double globalXMax = 0;

	private double ymax = 0;
	private double ymin = 0;
	private double xmax = 0;
	private List<JFreeChart> xyChartList  = new ArrayList <JFreeChart>();
	private String title;

	//----------------------------------------
	
	public void createChartPanel(MultiCAFE2 parent, String cstitle) 
	{
		title = cstitle;
		parent0 = parent;
		
		mainChartPanel = new JPanel(); 
		mainChartPanel.setLayout( new BoxLayout( mainChartPanel, BoxLayout.LINE_AXIS ) );
		
		mainChartFrame = GuiUtil.generateTitleFrame(title, new JPanel(), new Dimension(300, 70), true, true, true, true);	    
		mainChartFrame.add(mainChartPanel);
	}

	public void setLocationRelativeToRectangle(Rectangle rectv, Point deltapt) 
	{
		pt = new Point(rectv.x + deltapt.x, rectv.y + deltapt.y);
	}
	
	public void displayData(Experiment exp, EnumXLSExportType option, boolean subtractEvaporation) 
	{
		xyChartList.clear();

		ymax = 0;
		ymin = 0;
		List<XYSeriesCollection> xyDataSetList = new ArrayList <XYSeriesCollection>();
		flagMaxMinSet = false;
		getDataArrays(exp, option, subtractEvaporation, xyDataSetList);
		
		// display charts
		int width = 140; 
		int minimumDrawWidth = 100;
		int maximumDrawWidth = width;
		int height = 200;
		int maximumDrawHeight = height;
		
		boolean displayLabels = true;
		String yTitle = option.toUnit();
		int ichart = 0;

		for (XYSeriesCollection xySeriesCollection : xyDataSetList) 
		{
			JFreeChart xyChart = buildChartFromSeries( xySeriesCollection,  yTitle, displayLabels, option); 
			yTitle = null;  			// used only once
			displayLabels = false;		// used only once
			xyChart.setID (Integer.toString(ichart));
						
			xyChartList.add(xyChart);
			ChartPanel xyChartPanel = new ChartPanel(xyChart, width, height, minimumDrawWidth, 100, 
					maximumDrawWidth, maximumDrawHeight, false, false, true, true, true, true);
			xyChartPanel.addChartMouseListener(new ChartMouseListener() {
			    public void chartMouseClicked(ChartMouseEvent e) {selectKymoImage(getSelectedCurve(e)); }
			    public void chartMouseMoved(ChartMouseEvent e) {}
			});
			mainChartPanel.add(xyChartPanel);
			ichart += xySeriesCollection.getSeriesCount();

			width = 100;
			height = 200;
			minimumDrawWidth = 50;
			maximumDrawWidth = width;
			maximumDrawHeight = 200;

		}
		mainChartFrame.pack();
		mainChartFrame.setLocation(pt);
		mainChartFrame.addToDesktopPane ();
		mainChartFrame.setVisible(true);
	}
	
	
	private int getSelectedCurve(ChartMouseEvent e) 
	{
		final MouseEvent trigger = e.getTrigger();
        if (trigger.getButton() != MouseEvent.BUTTON1)
        	return -1;
        		
		JFreeChart chart = e.getChart();
        int isel = Integer.valueOf(chart.getID());
        
		ChartEntity chartEntity = e.getEntity();
		if (chartEntity != null && chartEntity instanceof XYItemEntity) {
            XYItemEntity ent = (XYItemEntity) chartEntity;
            isel += ent.getSeriesIndex();
		}
		return isel;
	}
	
	private void selectKymoImage(int isel)
	{
		Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
        Viewer v = exp.seqKymos.seq.getFirstViewer();
        v.setPositionT(isel);
	}
	
	private JFreeChart buildChartFromSeries(XYSeriesCollection xyDataset, String yTitle, boolean displayLabels, EnumXLSExportType option) 
	{
		JFreeChart xyChart = ChartFactory.createXYLineChart(
				null,				// chartname 
				null, 				// xDomain
				yTitle, 			// yDomain
				xyDataset, 			// collection
				PlotOrientation.VERTICAL, 
				true, 				// legend
				false, 				// tooltips
				false);				// url
		xyChart.setAntiAlias( true );
		xyChart.setTextAntiAlias( true );
		
		setYAxis(xyChart, displayLabels);
		setXAxis(xyChart);
		if (option == EnumXLSExportType.TOPLEVEL || option == EnumXLSExportType.BOTTOMLEVEL) 
			xyChart.getXYPlot().getRangeAxis(0).setInverted(true);
		
		return xyChart;
	}

	private void getDataArrays(Experiment exp, EnumXLSExportType exportType, boolean subtractEvaporation, List<XYSeriesCollection> xyList) 
	{
		XLSResultsArray resultsArray1 = getDataAsResultsArray(exp, exportType, subtractEvaporation);
		XLSResultsArray resultsArray2 = null;
		if (exportType == EnumXLSExportType.TOPLEVEL) 
			resultsArray2 = getDataAsResultsArray(exp, EnumXLSExportType.BOTTOMLEVEL, subtractEvaporation);
		
		XYSeriesCollection xyDataset = null;
		int oldcage = -1;
		
		for (int iRow = 0; iRow < resultsArray1.size(); iRow++ ) 
		{
			XLSResults rowXLSResults1 = resultsArray1.getRow(iRow);
			if (oldcage != rowXLSResults1.cageID ) 
			{
				xyDataset = new XYSeriesCollection();
				oldcage = rowXLSResults1.cageID; 
				xyList.add(xyDataset);
			} 	
			
			XYSeries seriesXY = getXYSeries(rowXLSResults1, rowXLSResults1.name.substring(4));
			if (resultsArray2 != null) 
				appendDataToXYSeries(seriesXY, resultsArray2.getRow(iRow));
			
			xyDataset.addSeries(seriesXY );
			updateGlobalMaxMin();
		}
	}
	
	private XLSResultsArray getDataAsResultsArray(Experiment exp, EnumXLSExportType exportType, boolean subtractEvaporation)
	{
		XLSExportOptions options = new XLSExportOptions();
		options.buildExcelStepMs = 60000;
		options.t0 = true;
		options.subtractEvaporation = subtractEvaporation;
		
		XLSExport xlsExport = new XLSExport();
		return xlsExport.getCapDataFromOneExperiment(exp, exportType, options);
	}
	
	private void setXAxis(JFreeChart xyChart) {

	if( parent0.paneExcel.tabCommonOptions.getIsFixedFrame())
	{
		double binMs	= parent0.paneExcel.tabCommonOptions.getBinMs();
		globalXMin 		= parent0.paneExcel.tabCommonOptions.getStartMs()/binMs;
		globalXMax 		= parent0.paneExcel.tabCommonOptions.getEndMs()/binMs;
	}
	ValueAxis xAxis = xyChart.getXYPlot().getDomainAxis(0);
	xAxis.setRange(globalXMin, globalXMax);
	}
	
	private void setYAxis(JFreeChart xyChart, boolean displayLabels) {
		ValueAxis yAxis = xyChart.getXYPlot().getRangeAxis(0);
		if (globalYMin == globalYMax)
			globalYMax = globalYMin +1;
		yAxis.setRange(globalYMin, globalYMax);
		yAxis.setTickLabelsVisible(displayLabels);
	}

	private void updateGlobalMaxMin() 
	{
		if (!flagMaxMinSet) 
		{
			globalYMax = ymax;
			globalYMin = ymin;
			globalXMax = xmax;
			flagMaxMinSet = true;
		}
		else 
		{
			if (globalYMax < ymax) globalYMax = ymax;
			if (globalYMin >= ymin) globalYMin = ymin;
			if (globalXMax < xmax) globalXMax = xmax;
		}
	}
	
	private XYSeries getXYSeries(XLSResults results, String name) 
	{
		XYSeries seriesXY = new XYSeries(name, false);
		if (results.valuesOut != null && results.valuesOut.length > 0) 
		{
			xmax = results.valuesOut.length;
			ymax = results.valuesOut[0];
			ymin = ymax;
			addPointsAndUpdateExtrema(seriesXY, results, 0);	
		}
		return seriesXY;
	}
	
	private void appendDataToXYSeries(XYSeries seriesXY, XLSResults results ) 
	{	
		if (results.valuesOut != null && results.valuesOut.length > 0) 
		{
			seriesXY.add(Double.NaN, Double.NaN);
			addPointsAndUpdateExtrema(seriesXY, results, 0);	
		}
	}
	
	private void addPointsAndUpdateExtrema(XYSeries seriesXY, XLSResults results, int startFrame) 
	{
		int x = 0;
		int npoints = results.valuesOut.length;
		for (int j = 0; j < npoints; j++) 
		{
			double y = results.valuesOut[j];
			seriesXY.add( x+startFrame , y );
			if (ymax < y) ymax = y;
			if (ymin > y) ymin = y;
			x++;
		}
	}
}
