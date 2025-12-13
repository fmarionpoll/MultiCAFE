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
import plugins.fmp.multicafe.fmp_tools.results.EnumResults;
import plugins.fmp.multicafe.fmp_tools.results.Results;
import plugins.fmp.multicafe.fmp_tools.results.ResultsArray;

public class ChartLevels extends IcyFrame {
	public JPanel mainChartPanel = null;
	private IcyFrame mainChartFrame = null;

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

	public void displayData(Experiment exp, EnumResults option, String title, boolean subtractEvaporation) {
		xyChartList.clear();
		ymax = Double.NaN;
		ymin = Double.NaN;
		flagMaxMinSet = false;
		List<XYSeriesCollection> xyDataSetList = getXYSeriesCollectionList(exp, option, subtractEvaporation);

		final NumberAxis yAxis = new NumberAxis("volume (µl)");
		yAxis.setAutoRangeIncludesZero(false);
		yAxis.setInverted(true);
		final CombinedRangeXYPlot combinedXYPlot = new CombinedRangeXYPlot(yAxis);
		Paint[] color = ChartColor.createDefaultPaintArray();

		for (XYSeriesCollection xySeriesCollection : xyDataSetList) {
			String[] description = xySeriesCollection.getSeries(0).getDescription().split("_");

			NumberAxis xAxis = new NumberAxis(description[0]);
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

	private List<XYSeriesCollection> getXYSeriesCollectionList(Experiment exp, EnumResults resultType,
			boolean subtractEvaporation) {

		exp.getCapillaryMeasures(resultType, subtractEvaporation);
		ResultsArray resultsArray1 = exp.transformDataScalingForOutput(resultType, subtractEvaporation);
		ResultsArray resultsArray2 = null;
		if (resultType == EnumResults.TOPLEVEL) {
			exp.getCapillaryMeasures(EnumResults.BOTTOMLEVEL, subtractEvaporation);
			resultsArray2 = exp.transformDataScalingForOutput(EnumResults.BOTTOMLEVEL, subtractEvaporation);
		}

		XYSeriesCollection xyDataset = null;
		int oldCage = -1;

		List<XYSeriesCollection> xyList = new ArrayList<XYSeriesCollection>();
		for (int iRow = 0; iRow < resultsArray1.size(); iRow++) {
			Results results = resultsArray1.getRow(iRow);
			if (results == null) {
				continue;
			}
			if (oldCage != results.getCageID()) {
				xyDataset = new XYSeriesCollection();
				oldCage = results.getCageID();
				xyList.add(xyDataset);
			}

			if (xyDataset != null) {
				XYSeries seriesXY = getXYSeries(results, results.getName().substring(4), exp, resultType);
				seriesXY.setDescription("cage " + results.getCageID() + "_" + results.getNflies());
				if (resultsArray2 != null)
					appendDataToXYSeries(seriesXY, resultsArray2.getRow(iRow), exp, EnumResults.BOTTOMLEVEL);

				xyDataset.addSeries(seriesXY);
				updateGlobalMaxMin();
			}
		}
		return xyList;
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

	private XYSeries getXYSeries(Results results, String name, Experiment exp, EnumResults resultType) {
		XYSeries seriesXY = new XYSeries(name, false);
		if (results.getValuesOut() != null && results.getValuesOut().length > 0) {
			xmax = results.getValuesOut().length;

			// Find first valid (non-NaN) value for initial ymax/ymin
			double firstValue = Double.NaN;
			for (int i = 0; i < results.getValuesOut().length; i++) {
				double val = results.getValuesOut()[i];
				if (!Double.isNaN(val)) {
					firstValue = val;
					break;
				}
			}
			if (!Double.isNaN(firstValue)) {
				ymax = firstValue;
				ymin = firstValue;
			}
			addPointsAndUpdateExtrema(seriesXY, results, 0, exp, resultType);
		}
		return seriesXY;
	}

	private void appendDataToXYSeries(XYSeries seriesXY, Results results, Experiment exp, EnumResults resultType) {
		if (results.getValuesOut() != null && results.getValuesOut().length > 0) {
			seriesXY.add(Double.NaN, Double.NaN);
			addPointsAndUpdateExtrema(seriesXY, results, 0, exp, resultType);
		}
	}

	private void addPointsAndUpdateExtrema(XYSeries seriesXY, Results results, int startFrame, Experiment exp,
			EnumResults resultType) {

		int x = 0;
		int npoints = results.getValuesOut().length;
		for (int j = 0; j < npoints; j++) {
			double y = results.getValuesOut()[j];
			if (Double.isNaN(y)) {
				seriesXY.add(x + startFrame, Double.NaN);
			} else {
				seriesXY.add(x + startFrame, y);
				if (Double.isNaN(ymax) || ymax < y)
					ymax = y;
				if (Double.isNaN(ymin) || ymin > y)
					ymin = y;
			}
			x++;
		}
	}

	/**
	 * Gets the main chart frame.
	 * 
	 * @return the main chart frame
	 */
	public IcyFrame getMainChartFrame() {
		return mainChartFrame;
	}

}
