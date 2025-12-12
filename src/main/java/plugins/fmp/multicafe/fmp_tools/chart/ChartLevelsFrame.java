package plugins.fmp.multicafe.fmp_tools.chart;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

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
import icy.roi.ROI2D;
import plugins.fmp.multicafe.MultiCAFE;
import plugins.fmp.multicafe.fmp_experiment.Experiment;
import plugins.fmp.multicafe.fmp_experiment.capillaries.Capillary;
import plugins.fmp.multicafe.fmp_tools.results.EnumResults;
import plugins.fmp.multicafe.fmp_tools.results.Results;
import plugins.fmp.multicafe.fmp_tools.results.ResultsArray;
import plugins.fmp.multicafe.fmp_tools.results.ResultsFromCapillaries;

/**
 * Enhanced chart display class for capillary level data visualization. This
 * class creates and manages charts displaying capillary measurements over time,
 * with improved error handling, logging, and interactive functionality.
 * 
 * <p>
 * ChartLevelsFrame extends the original ChartLevels functionality with:
 * <ul>
 * <li>Comprehensive error handling and validation</li>
 * <li>Logging support for debugging</li>
 * <li>Enhanced mouse interaction with ROI selection</li>
 * <li>Better code organization and documentation</li>
 * <li>Improved chart identification system</li>
 * </ul>
 * </p>
 * 
 * <p>
 * Usage example:
 * 
 * <pre>
 * ChartLevelsFrame chartFrame = new ChartLevelsFrame();
 * chartFrame.createChartPanel(parent, "Capillary Levels", rectv);
 * chartFrame.displayData(exp, EnumXLSExport.TOPLEVEL, "Top Level", false);
 * </pre>
 * 
 * @author MultiCAFE
 * @see plugins.fmp.multicafe.fmp_experiment.capillaries.Capillary
 * @see plugins.fmp.multicafe.fmp_experiment.Experiment
 */
public class ChartLevelsFrame extends IcyFrame {

	/** Logger for this class */
	private static final Logger LOGGER = Logger.getLogger(ChartLevelsFrame.class.getName());

	/** Default chart width in pixels */
	private static final int DEFAULT_CHART_WIDTH = 800;

	/** Default chart height in pixels */
	private static final int DEFAULT_CHART_HEIGHT = 300;

	/** Default minimum chart width in pixels */
	private static final int MIN_CHART_WIDTH = 800;

	/** Default minimum chart height in pixels */
	private static final int MIN_CHART_HEIGHT = 300;

	/** Default maximum chart width in pixels */
	private static final int MAX_CHART_WIDTH = 800;

	/** Default maximum chart height in pixels */
	private static final int MAX_CHART_HEIGHT = 500;

	/** Default frame width in pixels */
	private static final int DEFAULT_FRAME_WIDTH = 300;

	/** Default frame height in pixels */
	private static final int DEFAULT_FRAME_HEIGHT = 70;

	/** Mouse button for left click */
	private static final int LEFT_MOUSE_BUTTON = MouseEvent.BUTTON1;

	/** Main chart panel containing all charts */
	public JPanel mainChartPanel = null;

	/** Main chart frame */
	private IcyFrame mainChartFrame = null;

	/** Parent MultiCAFE instance */
	private MultiCAFE parent0 = null;

	/** Chart location */
	private Rectangle chartRectv = null;

	/** Chart location point */
	private Point graphLocation = new Point(0, 0);

	/** Flag indicating if global max/min values have been set */
	private boolean flagMaxMinSet = false;

	/** Global maximum Y value across all charts */
	private double globalYMax = 0;

	/** Global minimum Y value across all charts */
	private double globalYMin = 0;

	/** Global maximum X value across all charts */
	private double globalXMax = 0;

	/** Current maximum Y value */
	private double ymax = 0;

	/** Current minimum Y value */
	private double ymin = 0;

	/** Current maximum X value */
	private double xmax = 0;

	/** List of charts created */
	private List<JFreeChart> xyChartList = new ArrayList<JFreeChart>();

	/** Chart title */
	private String title;

	/** List of capillaries associated with subplots */
	private List<Capillary> capillaryList = new ArrayList<Capillary>();

	/**
	 * Creates the main chart panel and frame.
	 * 
	 * @param parent  the parent MultiCAFE instance
	 * @param cstitle the title for the chart window
	 * @param rectv   the initial location rectangle for the chart
	 * @throws IllegalArgumentException if parent or title is null/empty
	 */
	public void createChartPanel(MultiCAFE parent, String cstitle, Rectangle rectv) {
		if (parent == null) {
			throw new IllegalArgumentException("Parent MultiCAFE cannot be null");
		}
		if (cstitle == null || cstitle.trim().isEmpty()) {
			throw new IllegalArgumentException("Title cannot be null or empty");
		}

		title = cstitle;
		parent0 = parent;

		mainChartPanel = new JPanel();
		mainChartPanel.setLayout(new BoxLayout(mainChartPanel, BoxLayout.LINE_AXIS));

		mainChartFrame = GuiUtil.generateTitleFrame(title, new JPanel(),
				new Dimension(DEFAULT_FRAME_WIDTH, DEFAULT_FRAME_HEIGHT), true, true, true, true);
		JScrollPane scrollPane = new JScrollPane(mainChartPanel);
		mainChartFrame.add(scrollPane);

		if (chartRectv == null && rectv != null) {
			chartRectv = rectv;
			graphLocation = new Point(rectv.x, rectv.y);
		}

		LOGGER.fine("Created chart panel: " + title);
	}

	/**
	 * Displays capillary level data for the experiment.
	 * 
	 * @param exp                 the experiment containing the data
	 * @param resultType          the export type option
	 * @param title               the chart title
	 * @param correcttEvaporation whether to subtract evaporation
	 * @throws IllegalArgumentException if exp or option is null
	 */
	public void displayData(Experiment exp, EnumResults resultType, String title, boolean correcttEvaporation) {
		if (exp == null) {
			throw new IllegalArgumentException("Experiment cannot be null");
		}
		if (resultType == null) {
			throw new IllegalArgumentException("Export option cannot be null");
		}
		if (title == null || title.trim().isEmpty()) {
			throw new IllegalArgumentException("Title cannot be null or empty");
		}

		xyChartList.clear();
		capillaryList.clear();
		ymax = Double.NaN;
		ymin = Double.NaN;
		flagMaxMinSet = false;

		List<XYSeriesCollection> xyDataSetList = getDataArrays(exp, resultType, correcttEvaporation);

		if (xyDataSetList == null || xyDataSetList.isEmpty()) {
			LOGGER.warning("No data to display for option: " + resultType);
			return;
		}

		createAndDisplayChart(exp, resultType, title, xyDataSetList);
	}

	/**
	 * Creates and displays the chart with the provided data.
	 * 
	 * @param exp           the experiment
	 * @param resultType    the export option
	 * @param title         the chart title
	 * @param xyDataSetList the list of XY series collections
	 */
	private void createAndDisplayChart(Experiment exp, EnumResults resultType, String title,
			List<XYSeriesCollection> xyDataSetList) {
		final NumberAxis yAxis = new NumberAxis("volume (µl)");
		yAxis.setAutoRangeIncludesZero(false);
		yAxis.setInverted(true);
		final CombinedRangeXYPlot combinedXYPlot = new CombinedRangeXYPlot(yAxis);
		Paint[] color = ChartColor.createDefaultPaintArray();

		int subplotIndex = 0;
		for (XYSeriesCollection xySeriesCollection : xyDataSetList) {
			if (xySeriesCollection == null || xySeriesCollection.getSeriesCount() == 0) {
				LOGGER.warning("Skipping empty XY series collection at index " + subplotIndex);
				subplotIndex++;
				continue;
			}

			try {
				String[] description = xySeriesCollection.getSeries(0).getDescription().split("_");
				if (description.length < 2) {
					LOGGER.warning("Invalid description format: " + xySeriesCollection.getSeries(0).getDescription());
					subplotIndex++;
					continue;
				}

				NumberAxis xAxis = new NumberAxis(description[0]);
				XYLineAndShapeRenderer subPlotRenderer = new XYLineAndShapeRenderer(true, false);
				final XYPlot subplot = new XYPlot(xySeriesCollection, xAxis, null, subPlotRenderer);

				int icolor = 0;
				for (int i = 0; i < xySeriesCollection.getSeriesCount(); i++, icolor++) {
					if (icolor >= color.length)
						icolor = 0;
					subPlotRenderer.setSeriesPaint(i, color[icolor]);
				}

				int nflies = Integer.parseInt(description[1]);
				applyBackgroundColor(subplot, nflies);

				combinedXYPlot.add(subplot);
				subplotIndex++;
			} catch (NumberFormatException e) {
				LOGGER.warning("Could not parse fly count from description: " + e.getMessage());
			} catch (Exception e) {
				LOGGER.warning("Error processing subplot at index " + subplotIndex + ": " + e.getMessage());
			}
		}

		JFreeChart chart = new JFreeChart(title, null, combinedXYPlot, true);
		chart.setID("capillaryLevels:" + resultType.toString());

		final ChartPanel panel = new ChartPanel(chart, DEFAULT_CHART_WIDTH, DEFAULT_CHART_HEIGHT, MIN_CHART_WIDTH,
				MIN_CHART_HEIGHT, MAX_CHART_WIDTH, MAX_CHART_HEIGHT, true, true, true, true, false, true);

		panel.addChartMouseListener(new CapillaryChartMouseListener(exp, resultType));

		mainChartPanel.removeAll();
		mainChartPanel.add(panel);
		mainChartFrame.pack();
		mainChartFrame.setLocation(graphLocation);
		mainChartFrame.addToDesktopPane();
		mainChartFrame.setVisible(true);

		xyChartList.add(chart);
		LOGGER.fine("Displayed chart with " + subplotIndex + " subplots");
	}

	/**
	 * Applies background color to a subplot based on fly count.
	 * 
	 * @param subplot the XY plot to style
	 * @param nflies  the number of flies
	 */
	private void applyBackgroundColor(XYPlot subplot, int nflies) {
		if (nflies == 0) {
			subplot.setBackgroundPaint(Color.LIGHT_GRAY);
			subplot.setDomainGridlinePaint(Color.WHITE);
			subplot.setRangeGridlinePaint(Color.WHITE);
		} else if (nflies < 0) {
			subplot.setBackgroundPaint(Color.DARK_GRAY);
			subplot.setDomainGridlinePaint(Color.WHITE);
			subplot.setRangeGridlinePaint(Color.WHITE);
		} else if (nflies > 1) {
			subplot.setBackgroundPaint(new Color(173, 216, 230));
			subplot.setDomainGridlinePaint(Color.WHITE);
			subplot.setRangeGridlinePaint(Color.WHITE);
		} else {
			subplot.setBackgroundPaint(Color.WHITE);
			subplot.setDomainGridlinePaint(Color.GRAY);
			subplot.setRangeGridlinePaint(Color.GRAY);
		}
	}

	/**
	 * Gets the selected curve index from a chart mouse event.
	 * 
	 * @param e the chart mouse event
	 * @return the selected curve index, or -1 if invalid
	 */
	private int getSelectedCurve(ChartMouseEvent e) {
		if (e == null) {
			LOGGER.warning("Chart mouse event is null");
			return -1;
		}

		final MouseEvent trigger = e.getTrigger();
		if (trigger == null || trigger.getButton() != LEFT_MOUSE_BUTTON) {
			return -1;
		}

		JFreeChart chart = e.getChart();
		if (chart == null) {
			LOGGER.warning("Chart is null");
			return -1;
		}

		ChartEntity chartEntity = e.getEntity();
		MouseEvent mouseEvent = e.getTrigger();

		int isel = 0;
		if (chartEntity != null && chartEntity instanceof XYItemEntity) {
			XYItemEntity xyItemEntity = (XYItemEntity) chartEntity;
			isel += xyItemEntity.getSeriesIndex();
		}

		if (!(chart.getPlot() instanceof CombinedRangeXYPlot)) {
			LOGGER.warning("Chart plot is not a CombinedRangeXYPlot");
			return -1;
		}

		CombinedRangeXYPlot combinedXYPlot = (CombinedRangeXYPlot) chart.getPlot();
		@SuppressWarnings("unchecked")
		List<XYPlot> subplots = combinedXYPlot.getSubplots();

		if (mainChartPanel == null || mainChartPanel.getComponentCount() == 0) {
			LOGGER.warning("Main chart panel is empty");
			return -1;
		}

		ChartPanel panel = (ChartPanel) mainChartPanel.getComponent(0);
		if (panel == null) {
			LOGGER.warning("Chart panel is null");
			return -1;
		}

		PlotRenderingInfo plotInfo = panel.getChartRenderingInfo().getPlotInfo();
		Point2D p = panel.translateScreenToJava2D(mouseEvent.getPoint());
		int subplotindex = plotInfo.getSubplotIndex(p);

		if (subplotindex < 0 || subplotindex >= subplots.size()) {
			LOGGER.warning("Invalid subplot index: " + subplotindex);
			return -1;
		}

		for (int i = 0; i < subplotindex; i++) {
			isel += subplots.get(i).getSeriesCount();
		}

		return isel;
	}

	/**
	 * Selects the kymograph image at the specified index.
	 * 
	 * @param isel the index to select
	 */
	private void selectKymoImage(int isel) {
		if (parent0 == null) {
			LOGGER.warning("Parent MultiCAFE is null");
			return;
		}

		Experiment exp = (Experiment) parent0.expListComboLazy.getSelectedItem();
		if (exp == null) {
			LOGGER.warning("No experiment selected");
			return;
		}

		if (exp.getSeqKymos() == null || exp.getSeqKymos().getSequence() == null) {
			LOGGER.warning("Kymograph sequence is not available");
			return;
		}

		Viewer v = exp.getSeqKymos().getSequence().getFirstViewer();
		if (v != null && isel >= 0) {
			v.setPositionT(isel);
			LOGGER.fine("Selected kymograph at index: " + isel);
		}
	}

	/**
	 * Selects a capillary ROI in the experiment.
	 * 
	 * @param exp       the experiment
	 * @param capillary the capillary to select
	 */
	private void chartSelectCapillary(Experiment exp, Capillary capillary) {
		if (exp == null || capillary == null) {
			LOGGER.warning("Cannot select capillary: experiment or capillary is null");
			return;
		}

		ROI2D roi = capillary.getRoi();
		if (roi != null) {
			if (exp.getSeqCamData() != null && exp.getSeqCamData().getSequence() != null) {
				exp.getSeqCamData().getSequence().setFocusedROI(roi);
				exp.getSeqCamData().centerDisplayOnRoi(roi);
				exp.getSeqCamData().getSequence().setSelectedROI(roi);
				LOGGER.fine("Selected capillary ROI: " + roi.getName());
			}
		}
	}

	/**
	 * Gets the capillary from a clicked chart.
	 * 
	 * @param e    the chart mouse event
	 * @param exp  the experiment
	 * @param isel the selected curve index
	 * @return the selected capillary or null if not found
	 */
	private Capillary getCapillaryFromClickedChart(ChartMouseEvent e, Experiment exp, int isel) {
		if (exp == null || isel < 0) {
			return null;
		}

		if (exp.getCapillaries() == null) {
			LOGGER.warning("Experiment has no capillaries");
			return null;
		}

		List<Capillary> capillaries = exp.getCapillaries().getList();
		if (capillaries == null || capillaries.isEmpty()) {
			LOGGER.warning("Capillaries list is empty");
			return null;
		}

		if (isel < capillaries.size()) {
			return capillaries.get(isel);
		}

		LOGGER.warning("Selected index " + isel + " is out of bounds for " + capillaries.size() + " capillaries");
		return null;
	}

	/**
	 * Gets data arrays for the experiment.
	 * 
	 * @param exp                the experiment
	 * @param resultType         the export type
	 * @param correctEvaporation whether to subtract evaporation
	 * @return list of XY series collections
	 */
	private List<XYSeriesCollection> getDataArrays(Experiment exp, EnumResults resultType, boolean correctEvaporation) {
		if (exp == null || resultType == null) {
			LOGGER.warning("Invalid parameters for getDataArrays");
			return new ArrayList<XYSeriesCollection>();
		}

		ResultsArray resultsArray1 = getDataAsResultsArray(exp, resultType, correctEvaporation);
		ResultsArray resultsArray2 = null;
		if (resultType == EnumResults.TOPLEVEL) {
			resultsArray2 = getDataAsResultsArray(exp, EnumResults.BOTTOMLEVEL, correctEvaporation);
		}

		XYSeriesCollection xyDataset = null;
		int oldCage = -1;

		List<XYSeriesCollection> xyList = new ArrayList<XYSeriesCollection>();
		capillaryList.clear();

		if (resultsArray1 == null || resultsArray1.size() == 0) {
			LOGGER.warning("No results data available");
			return xyList;
		}

		for (int iRow = 0; iRow < resultsArray1.size(); iRow++) {
			Results xlsResults = resultsArray1.getRow(iRow);
			if (xlsResults == null) {
				continue;
			}

			if (oldCage != xlsResults.getCageID()) {
				xyDataset = new XYSeriesCollection();
				oldCage = xlsResults.getCageID();
				xyList.add(xyDataset);

				Capillary capillary = findCapillaryByCageID(exp, oldCage);
				if (capillary != null) {
					capillaryList.add(capillary);
				}
			}

			if (xyDataset != null) {
				try {
					String name = xlsResults.getName();
					if (name == null || name.length() < 4) {
						LOGGER.warning("Invalid name format: " + name);
						continue;
					}

					XYSeries seriesXY = getXYSeries(xlsResults, name.substring(4), exp, resultType);
					seriesXY.setDescription("cage " + xlsResults.getCageID() + "_" + xlsResults.getNflies());

					if (resultsArray2 != null && iRow < resultsArray2.size()) {
						Results bottomResults = resultsArray2.getRow(iRow);
						if (bottomResults != null) {
							appendDataToXYSeries(seriesXY, bottomResults, exp, EnumResults.BOTTOMLEVEL);
						}
					}

					xyDataset.addSeries(seriesXY);
					updateGlobalMaxMin();
				} catch (Exception ex) {
					LOGGER.warning("Error processing row " + iRow + ": " + ex.getMessage());
				}
			}
		}

		return xyList;
	}

	/**
	 * Finds a capillary by cage ID.
	 * 
	 * @param exp    the experiment
	 * @param cageID the cage ID
	 * @return the capillary or null if not found
	 */
	private Capillary findCapillaryByCageID(Experiment exp, int cageID) {
		if (exp == null || exp.getCapillaries() == null) {
			return null;
		}

		List<Capillary> capillaries = exp.getCapillaries().getList();
		if (capillaries == null) {
			return null;
		}

		for (Capillary cap : capillaries) {
			if (cap != null && cap.getCageID() == cageID) {
				return cap;
			}
		}

		return null;
	}

	/**
	 * Gets data as results array.
	 * 
	 * @param exp                the experiment
	 * @param resultType         the export type
	 * @param correctEvaporation whether to subtract evaporation
	 * @return the results array
	 */
	private ResultsArray getDataAsResultsArray(Experiment exp, EnumResults resultType, boolean correctEvaporation) {
		if (exp == null || resultType == null) {
			LOGGER.warning("Invalid parameters for getDataAsResultsArray");
			return new ResultsArray();
		}

		// Note: Computations are now handled inside getMeasuresFromAllCapillaries
		// to ensure consistency between chart display and Excel export
		ResultsFromCapillaries xlsResultsFromCaps = new ResultsFromCapillaries(exp.getCapillaries().getList().size());
		return xlsResultsFromCaps.getMeasuresFromAllCapillaries(exp, resultType, correctEvaporation);
	}

	/**
	 * Updates global maximum and minimum values.
	 */
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

	/**
	 * Gets an XY series from results.
	 * 
	 * @param results    the results
	 * @param name       the series name
	 * @param exp        the experiment
	 * @param resultType the export type
	 * @return the XY series
	 */
	private XYSeries getXYSeries(Results results, String name, Experiment exp, EnumResults resultType) {
		XYSeries seriesXY = new XYSeries(name, false);
		if (results == null || results.getValuesOut() == null || results.getValuesOut().length == 0) {
			return seriesXY;
		}

		xmax = results.getValuesOut().length;

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
		return seriesXY;
	}

	/**
	 * Appends data to an XY series.
	 * 
	 * @param seriesXY   the series to append to
	 * @param results    the results to append
	 * @param exp        the experiment
	 * @param resultType the export type
	 */
	private void appendDataToXYSeries(XYSeries seriesXY, Results results, Experiment exp, EnumResults resultType) {
		if (results == null || results.getValuesOut() == null || results.getValuesOut().length == 0) {
			return;
		}

		seriesXY.add(Double.NaN, Double.NaN);
		addPointsAndUpdateExtrema(seriesXY, results, 0, exp, resultType);
	}

	/**
	 * Adds points to a series and updates extrema.
	 * 
	 * @param seriesXY   the series
	 * @param results    the results
	 * @param startFrame the start frame
	 * @param exp        the experiment
	 * @param resultType the export type
	 */
	private void addPointsAndUpdateExtrema(XYSeries seriesXY, Results results, int startFrame, Experiment exp,
			EnumResults resultType) {
		if (results == null || results.getValuesOut() == null) {
			return;
		}

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
	 * Sets the chart location relative to a rectangle.
	 * 
	 * @param rectv the reference rectangle
	 * @throws IllegalArgumentException if rectv is null
	 */
	public void setChartUpperLeftLocation(Rectangle rectv) {
		if (rectv == null) {
			throw new IllegalArgumentException("Reference rectangle cannot be null");
		}

		graphLocation = new Point(rectv.x, rectv.y);
		if (chartRectv == null) {
			chartRectv = rectv;
		}
		LOGGER.fine("Set chart location to: " + graphLocation);
	}

	// Accessors for testing and external use

	/**
	 * Gets the main chart panel.
	 * 
	 * @return the main chart panel
	 */
	public JPanel getMainChartPanel() {
		return mainChartPanel;
	}

	/**
	 * Gets the main chart frame.
	 * 
	 * @return the main chart frame
	 */
	public IcyFrame getMainChartFrame() {
		return mainChartFrame;
	}

	/**
	 * Gets the global Y maximum.
	 * 
	 * @return the global Y maximum
	 */
	public double getGlobalYMax() {
		return globalYMax;
	}

	/**
	 * Gets the global Y minimum.
	 * 
	 * @return the global Y minimum
	 */
	public double getGlobalYMin() {
		return globalYMin;
	}

	/**
	 * Gets the global X maximum.
	 * 
	 * @return the global X maximum
	 */
	public double getGlobalXMax() {
		return globalXMax;
	}

	/**
	 * Inner class for handling chart mouse events.
	 */
	private class CapillaryChartMouseListener implements ChartMouseListener {
		private final Experiment experiment;

		/**
		 * Creates a new mouse listener.
		 * 
		 * @param exp        the experiment
		 * @param resultType the export option (currently unused, kept for future use)
		 */
		public CapillaryChartMouseListener(Experiment exp, @SuppressWarnings("unused") EnumResults resultType) {
			this.experiment = exp;
		}

		@Override
		public void chartMouseClicked(ChartMouseEvent e) {
			int isel = getSelectedCurve(e);
			if (isel >= 0) {
				selectKymoImage(isel);

				Capillary capillary = getCapillaryFromClickedChart(e, experiment, isel);
				if (capillary != null) {
					chartSelectCapillary(experiment, capillary);
				}
			}
		}

		@Override
		public void chartMouseMoved(ChartMouseEvent e) {
			// No action needed for mouse movement
		}
	}
}
