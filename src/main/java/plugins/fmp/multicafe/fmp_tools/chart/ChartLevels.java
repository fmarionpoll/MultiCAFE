package plugins.fmp.multicafe.fmp_tools.chart;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

import org.jfree.chart.ChartColor;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.plot.CombinedRangeXYPlot;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import icy.gui.frame.IcyFrame;
import icy.gui.util.GuiUtil;
import icy.gui.viewer.Viewer;
import plugins.fmp.multicafe.MultiCAFE;
import plugins.fmp.multicafe.fmp_experiment.Experiment;
import plugins.fmp.multicafe.fmp_experiment.capillaries.Capillary;
import plugins.fmp.multicafe.fmp_tools.toExcel.capillaries.XLSExportMeasuresFromCapillary;
import plugins.fmp.multicafe.fmp_tools.toExcel.config.XLSExportOptions;
import plugins.fmp.multicafe.fmp_tools.toExcel.data.XLSResults;
import plugins.fmp.multicafe.fmp_tools.toExcel.data.XLSResultsArray;
import plugins.fmp.multicafe.fmp_tools.toExcel.enums.EnumXLSExport;

public class ChartLevels extends IcyFrame {
	public JPanel mainChartPanel = null;
	public IcyFrame mainChartFrame = null;

	private MultiCAFE parent0 = null;

	private Rectangle chartRectv = null;

	private boolean flagMaxMinSet = false;
	private double globalYMax = 0;
	private double globalYMin = 0;
	private double globalXMax = 0;

	private double ymax = 0;
	private double ymin = 0;
	private double xmax = 0;
	private List<JFreeChart> xyChartList = new ArrayList<JFreeChart>();
	private String title;

	// ----------------------------------------

	public void createChartPanel(MultiCAFE parent, String cstitle, Rectangle rectv) {
		title = cstitle;
		parent0 = parent;

		mainChartPanel = new JPanel();
		mainChartPanel.setLayout(new BoxLayout(mainChartPanel, BoxLayout.LINE_AXIS));

		mainChartFrame = GuiUtil.generateTitleFrame(title, new JPanel(), new Dimension(300, 70), true, true, true,
				true);
		mainChartFrame.add(mainChartPanel);

		if (chartRectv == null) {
			chartRectv = rectv;
		}
	}

	public void displayData(Experiment exp, EnumXLSExport option, String title, boolean subtractEvaporation) {
		xyChartList.clear();
		ymax = 0;
		ymin = 0;
		flagMaxMinSet = false;
		List<XYSeriesCollection> xyDataSetList = getDataArrays(exp, option, subtractEvaporation);
		;

		final NumberAxis yAxis = new NumberAxis("volume (µl)");
		yAxis.setAutoRangeIncludesZero(false);
		yAxis.setInverted(true);
		final CombinedRangeXYPlot combinedXYPlot = new CombinedRangeXYPlot(yAxis);
		Paint[] color = ChartColor.createDefaultPaintArray();

		for (XYSeriesCollection xySeriesCollection : xyDataSetList) {
			String[] description = xySeriesCollection.getSeries(0).getDescription().split("_");

			NumberAxis xAxis = new NumberAxis("Time (minutes)");
			XYLineAndShapeRenderer subPlotRenderer = new XYLineAndShapeRenderer(true, false);
			final XYPlot subplot = new XYPlot(xySeriesCollection, xAxis, null, subPlotRenderer);
			int icolor = 0;
			for (int i = 0; i < xySeriesCollection.getSeriesCount(); i++, icolor++) {
				if (icolor > color.length)
					icolor = 0;
				subPlotRenderer.setSeriesPaint(i, color[icolor]);
			}

			int nflies = Integer.valueOf(description[1]);
			if (nflies == 0) {
				subplot.setBackgroundPaint(Color.LIGHT_GRAY);
				subplot.setDomainGridlinePaint(Color.WHITE);
				subplot.setRangeGridlinePaint(Color.WHITE);
			} else if (nflies < 0) {
				subplot.setBackgroundPaint(Color.DARK_GRAY);
				subplot.setDomainGridlinePaint(Color.WHITE);
				subplot.setRangeGridlinePaint(Color.WHITE);
			} else if (nflies > 1) {
				subplot.setBackgroundPaint(new Color(173, 216, 230)); // r: 173, g: 216, b: 230
				subplot.setDomainGridlinePaint(Color.WHITE);
				subplot.setRangeGridlinePaint(Color.WHITE);
			} else {
				subplot.setBackgroundPaint(Color.WHITE);
				subplot.setDomainGridlinePaint(Color.GRAY);
				subplot.setRangeGridlinePaint(Color.GRAY);
			}

			combinedXYPlot.add(subplot);
		}

		JFreeChart chart = new JFreeChart(title, null, combinedXYPlot, true);

		int width = 800;
		int height = 300;
		int minimumDrawWidth = width;
		int minimumDrawHeight = 300;
		int maximumDrawWidth = 800;
		int maximumDrawHeight = 500;
		boolean useBuffer = true;

		final ChartPanel panel = new ChartPanel(chart, width, height, minimumDrawWidth, minimumDrawHeight,
				maximumDrawWidth, maximumDrawHeight, useBuffer, true, true, true, false, true); // boolean properties,
																								// boolean save, boolean
																								// print, boolean zoom,
																								// boolean tooltips)
		panel.addChartMouseListener(new ChartMouseListener() {
			public void chartMouseClicked(ChartMouseEvent e) {
				selectKymoImage(getSelectedCurve(e));
			}

			public void chartMouseMoved(ChartMouseEvent e) {
			}
		});

		mainChartPanel.add(panel);
		mainChartFrame.pack();
		mainChartFrame.setLocation(chartRectv.getLocation());
		mainChartFrame.addToDesktopPane();
		mainChartFrame.setVisible(true);
	}

	private int getSelectedCurve(ChartMouseEvent e) {
		final MouseEvent trigger = e.getTrigger();
		if (trigger.getButton() != MouseEvent.BUTTON1)
			return -1;

		JFreeChart chart = e.getChart();
		ChartEntity chartEntity = e.getEntity();
		MouseEvent mouseEvent = e.getTrigger();

		int isel = 0;
		if (chartEntity != null && chartEntity instanceof XYItemEntity) {
			XYItemEntity xyItemEntity = ((XYItemEntity) e.getEntity());
			isel += xyItemEntity.getSeriesIndex();
		}

		CombinedRangeXYPlot combinedXYPlot = (CombinedRangeXYPlot) chart.getPlot();
		@SuppressWarnings("unchecked")
		List<XYPlot> subplots = combinedXYPlot.getSubplots();

		ChartPanel panel = (ChartPanel) mainChartPanel.getComponent(0);
		PlotRenderingInfo plotInfo = panel.getChartRenderingInfo().getPlotInfo();
		Point2D p = panel.translateScreenToJava2D(mouseEvent.getPoint());
		int subplotindex = plotInfo.getSubplotIndex(p);
		for (int i = 0; i < subplotindex; i++)
			isel += subplots.get(i).getSeriesCount();

		return isel;
	}

	private void selectKymoImage(int isel) {
		Experiment exp = (Experiment) parent0.expListComboLazy.getSelectedItem();
		Viewer v = exp.getSeqKymos().getSequence().getFirstViewer();
		if (v != null && isel >= 0)
			v.setPositionT(isel);
	}

	private List<XYSeriesCollection> getDataArrays(Experiment exp, EnumXLSExport exportType,
			boolean subtractEvaporation) {
		XLSExportOptions options = new XLSExportOptions();
		options.buildExcelStepMs = 60000;
		XLSResultsArray resultsArray1 = getDataAsResultsArray(exp, exportType, subtractEvaporation, options);
		XLSResultsArray resultsArray2 = null;
		if (exportType == EnumXLSExport.TOPLEVEL)
			resultsArray2 = getDataAsResultsArray(exp, EnumXLSExport.BOTTOMLEVEL, subtractEvaporation, options);

		XYSeriesCollection xyDataset = null;
		int oldCage = -1;

		List<XYSeriesCollection> xyList = new ArrayList<XYSeriesCollection>();
		for (int iRow = 0; iRow < resultsArray1.size(); iRow++) {
			XLSResults xlsResults = resultsArray1.getRow(iRow);
			if (xlsResults == null) {
				continue;
			}
			if (oldCage != xlsResults.getCageID()) {
				xyDataset = new XYSeriesCollection();
				oldCage = xlsResults.getCageID();
				xyList.add(xyDataset);
			}

			if (xyDataset == null) {
				xyDataset = new XYSeriesCollection();
				xyList.add(xyDataset);
			}

			XYSeries seriesXY = getXYSeries(xlsResults, xlsResults.getName().substring(4), options.buildExcelStepMs);
			seriesXY.setDescription("cell " + xlsResults.getCageID() + "_" + xlsResults.getNflies());
			if (resultsArray2 != null)
				appendDataToXYSeries(seriesXY, resultsArray2.getRow(iRow), options.buildExcelStepMs);

			xyDataset.addSeries(seriesXY);
			updateGlobalMaxMin();
		}
		return xyList;
	}

	private XLSResultsArray getDataAsResultsArray(Experiment exp, EnumXLSExport exportType,
			boolean subtractEvaporation, XLSExportOptions optionsParam) {
		if (exp == null || exp.getCapillaries() == null || exp.getCapillaries().getCapillariesList() == null) {
			return new XLSResultsArray();
		}

		XLSExportOptions options = new XLSExportOptions();
		options.buildExcelStepMs = optionsParam.buildExcelStepMs;
		options.relativeToT0 = true;
		options.subtractEvaporation = subtractEvaporation;
		options.exportType = exportType;

		XLSExportMeasuresFromCapillary xlsExport = new XLSExportMeasuresFromCapillary();

		XLSResultsArray resultsArray = new XLSResultsArray();
		double scalingFactorToPhysicalUnits = exp.getCapillaries().getScalingFactorToPhysicalUnits(exportType);

		for (Capillary capillary : exp.getCapillaries().getCapillariesList()) {
			XLSResults xlsResults = xlsExport.getXLSResultsDataValuesFromCapillaryMeasures(exp, capillary, options, false);
			if (xlsResults != null) {
				xlsResults.transferDataValuesToValuesOut(scalingFactorToPhysicalUnits, exportType);
				resultsArray.add(xlsResults);
			}
		}

		return resultsArray;
	}

	private void updateGlobalMaxMin() {
		if (!flagMaxMinSet) {
			globalYMax = ymax;
			globalYMin = ymin;
			globalXMax = xmax;
			flagMaxMinSet = true;
		} else {
			if (globalYMax < ymax)
				globalYMax = ymax;
			if (globalYMin >= ymin)
				globalYMin = ymin;
			if (globalXMax < xmax)
				globalXMax = xmax;
		}
	}

	private XYSeries getXYSeries(XLSResults results, String name, long buildExcelStepMs) {
		XYSeries seriesXY = new XYSeries(name, false);
		if (results.getValuesOut() != null && results.getValuesOut().length > 0) {
			xmax = (results.getValuesOut().length - 1) * (buildExcelStepMs / 60000.0);
			ymax = results.getValuesOut()[0];
			ymin = ymax;
			addPointsAndUpdateExtrema(seriesXY, results, 0, buildExcelStepMs);
		}
		return seriesXY;
	}

	private void appendDataToXYSeries(XYSeries seriesXY, XLSResults results, long buildExcelStepMs) {
		if (results.getValuesOut() != null && results.getValuesOut().length > 0) {
			int currentLength = seriesXY.getItemCount();
			double timeMinutes = currentLength * (buildExcelStepMs / 60000.0);
			seriesXY.add(timeMinutes, Double.NaN);
			addPointsAndUpdateExtrema(seriesXY, results, currentLength, buildExcelStepMs);
		}
	}

	private void addPointsAndUpdateExtrema(XYSeries seriesXY, XLSResults results, int startIndex, long buildExcelStepMs) {
		int npoints = results.getValuesOut().length;
		double timeStepMinutes = buildExcelStepMs / 60000.0; // Convert ms to minutes
		for (int j = 0; j < npoints; j++) {
			double timeMinutes = (startIndex + j) * timeStepMinutes;
			double y = results.getValuesOut()[j];
			seriesXY.add(timeMinutes, y);
			if (ymax < y)
				ymax = y;
			if (ymin > y)
				ymin = y;
		}
		if ((startIndex + npoints - 1) * timeStepMinutes > xmax)
			xmax = (startIndex + npoints - 1) * timeStepMinutes;
	}

}
