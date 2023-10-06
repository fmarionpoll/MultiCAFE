package plugins.fmp.multicafe2.tools.chart;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import icy.gui.frame.IcyFrame;
import icy.gui.util.GuiUtil;
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
		mainChartFrame = GuiUtil.generateTitleFrame(title, new JPanel(), new Dimension(300, 70), true, true, true, true);	    
		mainChartPanel = new JPanel(); 
		mainChartPanel.setLayout( new BoxLayout( mainChartPanel, BoxLayout.LINE_AXIS ) );
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

		for (XYSeriesCollection xyDataset : xyDataSetList) 
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
			yTitle = null;
			
			setXAxis(xyChart);
			
			if (option == EnumXLSExportType.TOPLEVEL || option == EnumXLSExportType.BOTTOMLEVEL) 
				xyChart.getXYPlot().getRangeAxis(0).setInverted(true);
			
			xyChartList.add(xyChart);
			ChartPanel xyChartPanel = new ChartPanel(xyChart, width, height, minimumDrawWidth, 100, 
					maximumDrawWidth, maximumDrawHeight, false, false, true, true, true, true);
			mainChartPanel.add(xyChartPanel);

			width = 100;
			height = 200;
			minimumDrawWidth = 50;
			maximumDrawWidth = width;
			maximumDrawHeight = 200;
			displayLabels = false;
		}
		mainChartFrame.pack();
		mainChartFrame.setLocation(pt);
		mainChartFrame.addToDesktopPane ();
		mainChartFrame.setVisible(true);
	}
	
	private void getDataArrays(Experiment exp, EnumXLSExportType exportType, boolean subtractEvaporation, List<XYSeriesCollection> xyList) 
	{
		XLSExport xlsExport = new XLSExport();
		XLSExportOptions options = new XLSExportOptions();
		options.buildExcelStepMs = 60000;
		options.t0 = true;
		options.subtractEvaporation = subtractEvaporation;
		XLSResultsArray resultsList = xlsExport.getCapDataFromOneExperiment(exp, exportType, options);
		
		XLSResultsArray resultsList2 = null;
		if (exportType == EnumXLSExportType.TOPLEVEL) 
			resultsList2 = xlsExport.getCapDataFromOneExperiment(exp, EnumXLSExportType.BOTTOMLEVEL, options);
		
//		String previousName = null;
		XYSeriesCollection xyDataset = null;
		int oldcage = -1;
		for (int iRow = 0; iRow < resultsList.size(); iRow++ ) 
		{
			XLSResults rowXLSResults = resultsList.getRow(iRow);
			
			if (oldcage != rowXLSResults.cageID ) 
			{
				xyDataset = new XYSeriesCollection();
				oldcage = rowXLSResults.cageID; 
				xyList.add(xyDataset);
			} 
			
			XYSeries seriesXY = getXYSeries(rowXLSResults, rowXLSResults.name.substring(4));
			
			if (resultsList2 != null)
			{
				for (int iRow2 = 0; iRow2 < resultsList2.size(); iRow2++ ) 
				{
					XLSResults row2 = resultsList2.getRow(iRow2);
					if (row2.name .equals(rowXLSResults.name)) 
					{
						appendDataToXYSeries(seriesXY, row2);
						break;
					}
				}
			}
			xyDataset.addSeries(seriesXY );
			updateGlobalMaxMin();
		}
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
