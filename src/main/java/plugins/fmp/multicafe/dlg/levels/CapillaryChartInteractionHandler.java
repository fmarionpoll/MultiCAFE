package plugins.fmp.multicafe.dlg.levels;

import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.logging.Logger;

import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import icy.gui.viewer.Viewer;
import icy.roi.ROI2D;
import plugins.fmp.multicafe.fmp_experiment.Experiment;
import plugins.fmp.multicafe.fmp_experiment.capillaries.Capillary;
import plugins.fmp.multicafe.fmp_experiment.cages.Cage;
import plugins.fmp.multicafe.fmp_tools.chart.ChartCagePair;
import plugins.fmp.multicafe.fmp_tools.results.ResultsOptions;

/**
 * Handler for capillary-related chart interactions. Manages user clicks on
 * capillary charts to select capillaries, navigate to images, and display
 * kymographs.
 * 
 * @author MultiCAFE
 */
public class CapillaryChartInteractionHandler implements ChartInteractionHandler {

	private static final Logger LOGGER = Logger.getLogger(CapillaryChartInteractionHandler.class.getName());

	private static final String CHART_ID_DELIMITER = ":";
	private static final int LEFT_MOUSE_BUTTON = MouseEvent.BUTTON1;

	private final Experiment experiment;
	private final ResultsOptions resultsOptions;
	private final ChartCagePair[][] chartPanelArray;

	/**
	 * Creates a new capillary chart interaction handler.
	 * 
	 * @param experiment      the experiment containing the data
	 * @param resultsOptions   the export options
	 * @param chartPanelArray  the array of chart panel pairs
	 */
	public CapillaryChartInteractionHandler(Experiment experiment, ResultsOptions resultsOptions,
			ChartCagePair[][] chartPanelArray) {
		this.experiment = experiment;
		this.resultsOptions = resultsOptions;
		this.chartPanelArray = chartPanelArray;
	}

	@Override
	public ChartMouseListener createMouseListener() {
		return new CapillaryChartMouseListener();
	}

	/**
	 * Gets the capillary from a clicked chart.
	 * 
	 * @param e the chart mouse event
	 * @return the selected capillary or null if not found
	 */
	private Capillary getCapillaryFromClickedChart(ChartMouseEvent e) {
		if (e == null) {
			LOGGER.warning("Chart mouse event is null");
			return null;
		}

		final MouseEvent trigger = e.getTrigger();
		if (trigger.getButton() != LEFT_MOUSE_BUTTON) {
			return null;
		}

		JFreeChart chart = e.getChart();
		if (chart == null || chart.getID() == null) {
			LOGGER.warning("Chart or chart ID is null");
			return null;
		}

		String[] chartID = chart.getID().split(CHART_ID_DELIMITER);
		if (chartID.length < 4) {
			LOGGER.warning("Invalid chart ID format: " + chart.getID());
			return null;
		}

		try {
			int row = Integer.parseInt(chartID[1]);
			int col = Integer.parseInt(chartID[3]);

			if (row < 0 || row >= chartPanelArray.length || col < 0 || col >= chartPanelArray[0].length) {
				LOGGER.warning("Invalid chart coordinates: row=" + row + ", col=" + col);
				return null;
			}

			Cage cage = chartPanelArray[row][col].getCage();
			if (cage == null) {
				LOGGER.warning("Clicked chart has no associated cage");
				return null;
			}

			ChartPanel panel = chartPanelArray[row][col].getChartPanel();
			if (panel == null) {
				LOGGER.warning("Clicked chart has no associated panel");
				return null;
			}

			XYPlot xyPlot = (XYPlot) chart.getPlot();
			ChartEntity chartEntity = e.getEntity();

			Capillary capillaryFound = null;

			if (chartEntity != null && chartEntity instanceof XYItemEntity) {
				capillaryFound = getCapillaryFromXYItemEntity((XYItemEntity) chartEntity, cage);
			} else {
				// Clicked on empty space - find closest curve
				// Need to get the ChartPanel from the event source
				Object source = e.getTrigger().getSource();
				if (source instanceof ChartPanel) {
					capillaryFound = findClosestCapillaryFromPoint(e, cage, xyPlot, (ChartPanel) source);
				}
			}

			return capillaryFound;

		} catch (NumberFormatException ex) {
			LOGGER.warning("Could not parse chart coordinates: " + ex.getMessage());
			return null;
		}
	}

	/**
	 * Gets the capillary from an XY item entity by parsing the series key.
	 * 
	 * @param xyItemEntity the XY item entity
	 * @param cage         the cage containing the capillaries
	 * @return the selected capillary or null if not found
	 */
	private Capillary getCapillaryFromXYItemEntity(XYItemEntity xyItemEntity, Cage cage) {
		if (xyItemEntity == null) {
			LOGGER.warning("XY item entity is null");
			return null;
		}

		int seriesIndex = xyItemEntity.getSeriesIndex();
		XYDataset xyDataset = xyItemEntity.getDataset();

		if (xyDataset == null) {
			LOGGER.warning("XY dataset is null");
			return null;
		}

		String seriesKey = (String) xyDataset.getSeriesKey(seriesIndex);
		if (seriesKey == null) {
			LOGGER.warning("Series key is null");
			return null;
		}

		return getCapillaryFromSeriesKey(seriesKey, cage);
	}

	/**
	 * Maps a series key to a Capillary object. Handles both individual capillaries
	 * ("cageID_L", "cageID_R") and LR types ("cageID_Sum", "cageID_PI").
	 * 
	 * @param seriesKey the series key (e.g., "0_L", "0_R", "0_Sum", "0_PI")
	 * @param cage      the cage containing the capillaries
	 * @return the corresponding capillary or null if not found
	 */
	private Capillary getCapillaryFromSeriesKey(String seriesKey, Cage cage) {
		if (seriesKey == null || cage == null) {
			return null;
		}

		String[] parts = seriesKey.split("_");
		if (parts.length < 2) {
			LOGGER.warning("Invalid series key format: " + seriesKey);
			return null;
		}

		String sideOrType = parts[1];

		// Handle LR types: Sum and PI
		if ("Sum".equals(sideOrType) || "PI".equals(sideOrType)) {
			// For Sum/PI, default to first L capillary for Sum, first R for PI
			// or find based on user preference
			List<Capillary> capillaries = cage.getCapillaries().getList();
			if (capillaries.isEmpty()) {
				return null;
			}

			// Try to find L capillary for Sum, R for PI
			for (Capillary cap : capillaries) {
				String capSide = cap.getCapillarySide();
				if ("Sum".equals(sideOrType) && ("L".equals(capSide) || "1".equals(capSide))) {
					return cap;
				} else if ("PI".equals(sideOrType) && ("R".equals(capSide) || "2".equals(capSide))) {
					return cap;
				}
			}

			// Fallback to first capillary
			return capillaries.get(0);
		}

		// Handle individual capillaries: L, R, 1, 2, etc.
		List<Capillary> capillaries = cage.getCapillaries().getList();
		for (Capillary cap : capillaries) {
			String capSide = cap.getCapillarySide();
			if (sideOrType.equals(capSide) || sideOrType.equals("1") && ("L".equals(capSide) || "1".equals(capSide))
					|| sideOrType.equals("2") && ("R".equals(capSide) || "2".equals(capSide))) {
				return cap;
			}
		}

		LOGGER.warning("Could not find capillary for series key: " + seriesKey);
		return null;
	}

	/**
	 * Finds the closest capillary curve to the clicked point when clicking on empty
	 * chart space.
	 * 
	 * @param e      the chart mouse event
	 * @param cage   the cage containing the capillaries
	 * @param xyPlot the XY plot
	 * @param panel  the chart panel
	 * @return the closest capillary or null if not found
	 */
	private Capillary findClosestCapillaryFromPoint(ChartMouseEvent e, Cage cage, XYPlot xyPlot, ChartPanel panel) {
		if (e == null || cage == null || xyPlot == null || panel == null) {
			return null;
		}

		java.awt.Point screenPoint = e.getTrigger().getPoint();
		Point2D java2DPoint = panel.translateScreenToJava2D(screenPoint);

		Rectangle2D dataArea = panel.getScreenDataArea();
		ValueAxis domainAxis = xyPlot.getDomainAxis();
		ValueAxis rangeAxis = xyPlot.getRangeAxis();

		double clickedX = domainAxis.java2DToValue(java2DPoint.getX(), dataArea, xyPlot.getDomainAxisEdge());
		double clickedY = rangeAxis.java2DToValue(java2DPoint.getY(), dataArea, xyPlot.getRangeAxisEdge());

		XYDataset dataset = xyPlot.getDataset();
		if (!(dataset instanceof XYSeriesCollection)) {
			return null;
		}

		XYSeriesCollection seriesCollection = (XYSeriesCollection) dataset;
		double minDistance = Double.MAX_VALUE;
		Capillary closestCapillary = null;

		// Iterate through all series to find the closest point
		for (int seriesIndex = 0; seriesIndex < seriesCollection.getSeriesCount(); seriesIndex++) {
			XYSeries series = seriesCollection.getSeries(seriesIndex);
			String seriesKey = (String) series.getKey();

			// Find closest point in this series
			for (int itemIndex = 0; itemIndex < series.getItemCount(); itemIndex++) {
				double x = series.getX(itemIndex).doubleValue();
				double y = series.getY(itemIndex).doubleValue();

				// Calculate distance (Euclidean distance in chart coordinates)
				double distance = Math.sqrt(Math.pow(x - clickedX, 2) + Math.pow(y - clickedY, 2));

				if (distance < minDistance) {
					minDistance = distance;
					// Get capillary from series key
					Capillary cap = getCapillaryFromSeriesKey(seriesKey, cage);
					if (cap != null) {
						closestCapillary = cap;
					}
				}
			}
		}

		return closestCapillary;
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
			}
		}
	}

	/**
	 * Selects the time position for a capillary based on the clicked X coordinate.
	 * 
	 * @param exp            the experiment
	 * @param resultsOptions the export options
	 * @param capillary      the capillary
	 * @param timeMinutes    the time in minutes from the clicked X coordinate
	 */
	private void selectTForCapillary(Experiment exp, ResultsOptions resultsOptions, Capillary capillary,
			double timeMinutes) {
		if (exp == null || capillary == null) {
			LOGGER.warning("Cannot select time: experiment or capillary is null");
			return;
		}

		Viewer v = exp.getSeqCamData().getSequence().getFirstViewer();
		if (v == null) {
			return;
		}

		// Convert time (minutes) to frame index
		if (timeMinutes >= 0) {
			// Find nearest frame index
			int frameIndex = exp.getSeqCamData().getTimeManager()
					.findNearestIntervalWithBinarySearch((long) (timeMinutes * 60000), 0,
							exp.getSeqCamData().getImageLoader().getNTotalFrames());
			v.setPositionT(frameIndex);
		}
	}

	/**
	 * Selects the kymograph image for a capillary.
	 * 
	 * @param exp       the experiment
	 * @param capillary the capillary to select kymograph for
	 */
	private void chartSelectKymographForCapillary(Experiment exp, Capillary capillary) {
		if (exp == null || capillary == null) {
			LOGGER.warning("Cannot select kymograph: experiment or capillary is null");
			return;
		}

		if (exp.getSeqKymos() == null || exp.getSeqKymos().getSequence() == null) {
			return;
		}

		Viewer v = exp.getSeqKymos().getSequence().getFirstViewer();
		if (v == null) {
			return;
		}

		// Use kymographIndex if available, otherwise find by capillary position
		int kymographIndex = capillary.kymographIndex;
		if (kymographIndex < 0) {
			// Find capillary index in the list
			List<Capillary> capillaries = exp.getCapillaries().getList();
			kymographIndex = capillaries.indexOf(capillary);
		}

		if (kymographIndex >= 0 && kymographIndex < exp.getSeqKymos().getSequence().getSizeT()) {
			v.setPositionT(kymographIndex);
		}
	}

	/**
	 * Handles the selection of a clicked capillary.
	 * 
	 * @param exp            the experiment
	 * @param resultsOptions the export options
	 * @param clickedCapillary the clicked capillary
	 * @param timeMinutes    the time in minutes from the clicked X coordinate
	 */
	private void chartSelectClickedCapillary(Experiment exp, ResultsOptions resultsOptions,
			Capillary clickedCapillary, double timeMinutes) {
		if (clickedCapillary == null) {
			LOGGER.warning("Clicked capillary is null");
			return;
		}

		chartSelectCapillary(exp, clickedCapillary);
		selectTForCapillary(exp, resultsOptions, clickedCapillary, timeMinutes);
		chartSelectKymographForCapillary(exp, clickedCapillary);

		// Center display on cage ROI
		Cage cage = exp.getCages().getCageFromRowColCoordinates(
				clickedCapillary.getCageID() / exp.getCages().nCagesAlongX,
				clickedCapillary.getCageID() % exp.getCages().nCagesAlongX);
		if (cage != null) {
			ROI2D cageRoi = cage.getRoi();
			if (cageRoi != null) {
				exp.getSeqCamData().centerDisplayOnRoi(cageRoi);
			}
		}
	}

	/**
	 * Gets the time in minutes from a chart mouse event by extracting the X
	 * coordinate.
	 * 
	 * @param e     the chart mouse event
	 * @param panel the chart panel
	 * @param plot  the XY plot
	 * @return the time in minutes or -1 if not found
	 */
	private double getTimeMinutesFromEvent(ChartMouseEvent e, ChartPanel panel, XYPlot plot) {
		if (e == null || panel == null || plot == null) {
			return -1;
		}

		java.awt.Point screenPoint = e.getTrigger().getPoint();
		Point2D java2DPoint = panel.translateScreenToJava2D(screenPoint);
		Rectangle2D dataArea = panel.getScreenDataArea();
		ValueAxis domainAxis = plot.getDomainAxis();

		// Extract X coordinate (time in minutes)
		double timeMinutes = domainAxis.java2DToValue(java2DPoint.getX(), dataArea, plot.getDomainAxisEdge());
		return timeMinutes;
	}

	/**
	 * Inner class for handling chart mouse events for capillaries.
	 */
	private class CapillaryChartMouseListener implements ChartMouseListener {
		@Override
		public void chartMouseClicked(ChartMouseEvent e) {
			Capillary clickedCapillary = getCapillaryFromClickedChart(e);
			if (clickedCapillary != null) {
				JFreeChart chart = e.getChart();
				Object source = e.getTrigger().getSource();
				if (source instanceof ChartPanel) {
					ChartPanel panel = (ChartPanel) source;
					XYPlot plot = (XYPlot) chart.getPlot();
					double timeMinutes = getTimeMinutesFromEvent(e, panel, plot);
					chartSelectClickedCapillary(experiment, resultsOptions, clickedCapillary, timeMinutes);
				}
			}
		}

		@Override
		public void chartMouseMoved(ChartMouseEvent e) {
			// No action needed for mouse movement
		}
	}
}

