package plugins.fmp.multicafe.fmp_tools.chart;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Logger;

import org.jfree.chart.ChartColor;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import plugins.fmp.multicafe.fmp_experiment.Experiment;
import plugins.fmp.multicafe.fmp_experiment.cages.Cage;
import plugins.fmp.multicafe.fmp_experiment.cages.CageProperties;
import plugins.fmp.multicafe.fmp_experiment.capillaries.Capillary;
import plugins.fmp.multicafe.fmp_experiment.capillaries.CapillaryMeasure;
//import plugins.fmp.multicafe.fmp_experiment.capillaries.CapillaryProperties;
import plugins.fmp.multicafe.fmp_experiment.spots.Spot;
import plugins.fmp.multicafe.fmp_experiment.spots.SpotMeasure;
import plugins.fmp.multicafe.fmp_tools.results.EnumResults;
import plugins.fmp.multicafe.fmp_tools.results.ResultsOptions;

/**
 * Utility class for creating and managing cage charts. This class provides
 * functionality to build XY plots from cage data, including data extraction,
 * plot configuration, and rendering setup.
 * 
 * <p>
 * ChartCage handles the conversion of cage data into chart-ready formats,
 * manages global min/max values for axis scaling, and configures plot
 * appearance based on data characteristics.
 * </p>
 * 
 * <p>
 * Usage example:
 * 
 * <pre>
 * ChartCageSpots chartBuilder = new ChartCageSpots();
 * chartBuilder.initMaxMin();
 * 
 * XYSeriesCollection data = chartBuilder.combineResults(cage, resultsArray1, resultsArray2);
 * NumberAxis xAxis = new NumberAxis("Time");
 * NumberAxis yAxis = new NumberAxis("Value");
 * XYPlot plot = chartBuilder.buildXYPlot(data, xAxis, yAxis);
 * </pre>
 * 
 * @author MultiSPOTS96
 * @see org.jfree.chart.plot.XYPlot
 * @see org.jfree.data.xy.XYSeriesCollection
 * @see plugins.fmp.multiSPOTS96.experiment.cages.Cage
 */
public class ChartCageBuild {

	/** Logger for this class */
	private static final Logger LOGGER = Logger.getLogger(ChartCageBuild.class.getName());

	/** Default stroke width for chart lines */
	private static final float DEFAULT_STROKE_WIDTH = 0.5f;

	/** Default dash pattern for secondary data series */
	private static final float[] DASH_PATTERN = { 2.0f, 4.0f };

	/** Default dash phase for secondary data series */
	private static final float DASH_PHASE = 0.0f;

	/** Background color for charts with data */
	private static final Color BACKGROUND_WITH_DATA = Color.WHITE;

	/** Background color for charts without data */
	private static final Color BACKGROUND_WITHOUT_DATA = Color.LIGHT_GRAY;

	/** Grid color for charts with data */
	private static final Color GRID_WITH_DATA = Color.GRAY;

	/** Grid color for charts without data */
	private static final Color GRID_WITHOUT_DATA = Color.WHITE;

	/** Token used to mark secondary data series */
	private static final String SECONDARY_DATA_TOKEN = "*";

	/** Delimiter used in series descriptions */
	private static final String DESCRIPTION_DELIMITER = ":";

	/** Flag indicating if global min/max values have been set */
	private static boolean flagMaxMinSet = false;

	/** Global maximum Y value across all series */
	private static double globalYMax = 0;

	/** Global minimum Y value across all series */
	private static double globalYMin = 0;

	/** Global maximum X value across all series */
	private static double globalXMax = 0;

	/** Current maximum Y value for the current series */
	private static double ymax = 0;

	/** Current minimum Y value for the current series */
	private static double ymin = 0;

	/** Current maximum X value for the current series */
	private static double xmax = 0;

	/**
	 * Initializes the global min/max tracking variables. This method should be
	 * called before processing new data to reset the global extrema tracking.
	 */
	static public void initMaxMin() {
		ymax = Double.NaN;
		ymin = Double.NaN;
		xmax = 0;
		flagMaxMinSet = false;
		globalYMax = 0;
		globalYMin = 0;
		globalXMax = 0;

		// LOGGER.fine("Initialized max/min tracking variables");
	}

	/**
	 * Builds an XY plot from the given dataset and axes.
	 * 
	 * @param xySeriesCollection the dataset to plot
	 * @param xAxis              the X-axis to use
	 * @param yAxis              the Y-axis to use
	 * @return configured XYPlot ready for chart creation
	 * @throws IllegalArgumentException if any parameter is null
	 */
	static public XYPlot buildXYPlot(XYSeriesCollection xySeriesCollection, NumberAxis xAxis, NumberAxis yAxis) {
		if (xySeriesCollection == null) {
			throw new IllegalArgumentException("XYSeriesCollection cannot be null");
		}
		if (xAxis == null) {
			throw new IllegalArgumentException("X-axis cannot be null");
		}
		if (yAxis == null) {
			throw new IllegalArgumentException("Y-axis cannot be null");
		}

//		//LOGGER.fine("Building XY plot with " + xySeriesCollection.getSeriesCount() + " series");

		if (isLRData(xySeriesCollection)) {
			return buildXYPlotLR(xySeriesCollection, xAxis, yAxis);
		}

		XYLineAndShapeRenderer subPlotRenderer = getSubPlotRenderer(xySeriesCollection);
		XYPlot xyPlot = new XYPlot(xySeriesCollection, xAxis, yAxis, subPlotRenderer);
		updatePlotBackgroundAccordingToNFlies(xySeriesCollection, xyPlot);

		return xyPlot;
	}

	private static boolean isLRData(XYSeriesCollection xySeriesCollection) {
		for (int i = 0; i < xySeriesCollection.getSeriesCount(); i++) {
			String key = (String) xySeriesCollection.getSeriesKey(i);
			if (key.endsWith("_PI") || key.endsWith("_Sum"))
				return true;
		}
		return false;
	}

	private static XYPlot buildXYPlotLR(XYSeriesCollection xySeriesCollection, NumberAxis xAxis, NumberAxis yAxis) {
		XYSeriesCollection sumCollection = new XYSeriesCollection();
		XYSeriesCollection piCollection = new XYSeriesCollection();

		for (int i = 0; i < xySeriesCollection.getSeriesCount(); i++) {
			XYSeries series = xySeriesCollection.getSeries(i);
			String key = (String) series.getKey();
			if (key.endsWith("_PI")) {
				piCollection.addSeries(series);
			} else {
				sumCollection.addSeries(series);
			}
		}

		XYLineAndShapeRenderer sumRenderer = getSubPlotRenderer(sumCollection);
		XYPlot xyPlot = new XYPlot(sumCollection, xAxis, yAxis, sumRenderer);

		XYLineAndShapeRenderer piRenderer = getSubPlotRenderer(piCollection);
		// Right Y-axis: PI. Keep consistent scaling across all charts and remove its title.
		NumberAxis yAxisPI = new NumberAxis("");
		yAxisPI.setAutoRange(false);
		yAxisPI.setRange(-1.0, 1.0);

		xyPlot.setDataset(1, piCollection);
		xyPlot.setRenderer(1, piRenderer);
		xyPlot.setRangeAxis(1, yAxisPI);
		xyPlot.mapDatasetToRangeAxis(1, 1);

		updatePlotBackgroundAccordingToNFlies(sumCollection, xyPlot);

		return xyPlot;
	}

	/**
	 * Updates the plot background and grid colors based on the number of flies in
	 * the data.
	 * 
	 * @param xySeriesCollection the dataset to analyze
	 * @param xyPlot             the plot to update
	 */
	private static void updatePlotBackgroundAccordingToNFlies(XYSeriesCollection xySeriesCollection, XYPlot xyPlot) {
		int nFlies = getNFliesFromxySeriesCollectionDescription(xySeriesCollection);
		setXYPlotBackGroundAccordingToNFlies(xyPlot, nFlies);
	}

	private static int getNFliesFromxySeriesCollectionDescription(XYSeriesCollection xySeriesCollection) {
		int nFlies = -1;
		if (xySeriesCollection == null || xySeriesCollection.getSeriesCount() == 0) {
			LOGGER.warning("Cannot update plot background: dataset is null or empty");
			return nFlies;
		}

		try {
			String[] description = xySeriesCollection.getSeries(0).getDescription().split(DESCRIPTION_DELIMITER);
			if (description.length < 6) {
				LOGGER.warning("Invalid series description format, using default background");
				return nFlies;
			}
			nFlies = Integer.parseInt(description[5]);
		} catch (NumberFormatException e) {
			LOGGER.warning("Could not parse number of flies from description: " + e.getMessage());
		}
		return nFlies;
	}

	private static void setXYPlotBackGroundAccordingToNFlies(XYPlot xyPlot, int nFlies) {
		if (nFlies > 0) {
			xyPlot.setBackgroundPaint(BACKGROUND_WITH_DATA);
			xyPlot.setDomainGridlinePaint(GRID_WITH_DATA);
			xyPlot.setRangeGridlinePaint(GRID_WITH_DATA);
			// LOGGER.fine("Set background for chart with " + nflies + " flies");
		} else {
			xyPlot.setBackgroundPaint(BACKGROUND_WITHOUT_DATA);
			xyPlot.setDomainGridlinePaint(GRID_WITHOUT_DATA);
			xyPlot.setRangeGridlinePaint(GRID_WITHOUT_DATA);
			// LOGGER.fine("Set background for chart with no flies");
		}
	}

	/**
	 * Extracts spot data from one cage in the results array.
	 * 
	 * @param experiment the stack of images and assoc items
	 * @param cage       the cage to get data for
	 * @param options    list of options
	 * @return XYSeriesCollection containing the cage's data
	 */
	static XYSeriesCollection getSpotDataDirectlyFromOneCage(Experiment exp, Cage cage, ResultsOptions resultsOptions) {
		if (cage == null || cage.spotsArray == null || cage.spotsArray.getSpotsCount() < 1) {
			LOGGER.warning("Cannot get spot data: spot array is empty or cage is null");
			return new XYSeriesCollection();
		}

		XYSeriesCollection xySeriesCollection = null;

		for (Spot spot : cage.spotsArray.getSpotsList()) {
			if (xySeriesCollection == null) {
				xySeriesCollection = new XYSeriesCollection();
			}

			XYSeries seriesXY = createXYSeriesFromSpotMeasure(exp, spot, resultsOptions);
			if (seriesXY != null) {
				seriesXY.setDescription(buildSeriesDescriptionFromCageAndSpot(cage, spot));
				xySeriesCollection.addSeries(seriesXY);
				updateGlobalMaxMin();
			}
		}

		// LOGGER.fine("Extracted " + seriesCount + " series for cage ID: " +
		// cage.getProperties().getCageID());
		return xySeriesCollection;
	}

	/**
	 * Extracts spot data from one cage in the results array.
	 * 
	 * @param experiment the stack of images and assoc items
	 * @param cage       the cage to get data for
	 * @param options    list of options
	 * @return XYSeriesCollection containing the cage's data
	 */
	public static XYSeriesCollection getCapillaryDataDirectlyFromOneCage(Experiment exp, Cage cage,
			ResultsOptions resultsOptions) {
		if (cage == null || cage.getCapillaries() == null || cage.getCapillaries().getList().size() < 1) {
//			LOGGER.warning("Cannot get capillary data: capillaries array is empty or cage is null");
			return new XYSeriesCollection();
		}

		if (isLRType(resultsOptions.resultType)) {
			return getLRDataFromOneCage(exp, cage, resultsOptions);
		}

		XYSeriesCollection xySeriesCollection = null;
		int i = 0;
		for (Capillary cap : cage.getCapillaries().getList()) {
			if (xySeriesCollection == null) {
				xySeriesCollection = new XYSeriesCollection();
			}

			XYSeries seriesXY = createXYSeriesFromCapillaryMeasure(exp, cap, resultsOptions);
			if (seriesXY != null) {
				seriesXY.setDescription(buildSeriesDescriptionFromCageAndCapillary(cage, cap, i));
				xySeriesCollection.addSeries(seriesXY);
				updateGlobalMaxMin();
			}
			i++;
		}
		return xySeriesCollection;
	}

	public static boolean isLRType(EnumResults resultType) {
		return resultType == EnumResults.TOPLEVEL_LR || resultType == EnumResults.TOPLEVELDELTA_LR
				|| resultType == EnumResults.SUMGULPS_LR;
	}

	private static EnumResults getBaseType(EnumResults resultType) {
		switch (resultType) {
		case TOPLEVEL_LR:
			return EnumResults.TOPLEVEL;
		case TOPLEVELDELTA_LR:
			return EnumResults.TOPLEVELDELTA;
		case SUMGULPS_LR:
			return EnumResults.SUMGULPS;
		default:
			return resultType;
		}
	}

	private static XYSeriesCollection getLRDataFromOneCage(Experiment exp, Cage cage, ResultsOptions resultsOptions) {
		XYSeriesCollection xySeriesCollection = new XYSeriesCollection();
		EnumResults baseType = getBaseType(resultsOptions.resultType);

		ResultsOptions baseOptions = new ResultsOptions();
		baseOptions.copy(resultsOptions);
		baseOptions.resultType = baseType;

		XYSeriesCollection parts = getCapillaryDataDirectlyFromOneCage(exp, cage, baseOptions);
		if (parts == null || parts.getSeriesCount() == 0)
			return xySeriesCollection;

		XYSeriesCollection sumAndPISeries = buildSumAndPISeries(cage, parts);
		for (int i = 0; i < sumAndPISeries.getSeriesCount(); i++) {
			xySeriesCollection.addSeries(sumAndPISeries.getSeries(i));
		}

		// Update global min/max
		for (int i = 0; i < xySeriesCollection.getSeriesCount(); i++) {
			XYSeries series = xySeriesCollection.getSeries(i);
			for (int j = 0; j < series.getItemCount(); j++) {
				double y = series.getY(j).doubleValue();
				double x = series.getX(j).doubleValue();
				if (!Double.isNaN(y)) {
					if (Double.isNaN(ymax) || y > ymax)
						ymax = y;
					if (Double.isNaN(ymin) || y < ymin)
						ymin = y;
				}
				if (x > xmax)
					xmax = x;
			}
		}
		updateGlobalMaxMin();

		return xySeriesCollection;
	}

	static XYSeriesCollection buildSumAndPISeries(Cage cage, XYSeriesCollection parts) {
		XYSeriesCollection result = new XYSeriesCollection();
		if (parts.getSeriesCount() == 0)
			return result;

		List<XYSeries> listL = new ArrayList<>();
		List<XYSeries> listR = new ArrayList<>();

		for (int i = 0; i < parts.getSeriesCount(); i++) {
			XYSeries series = parts.getSeries(i);
			String key = (String) series.getKey();
			if (key.endsWith("L") || key.endsWith("1")) {
				listL.add(series);
			} else if (key.endsWith("R") || key.endsWith("2")) {
				listR.add(series);
			}
		}

		SortedSet<Double> allX = new TreeSet<>();
		for (int i = 0; i < parts.getSeriesCount(); i++) {
			XYSeries series = parts.getSeries(i);
			for (int j = 0; j < series.getItemCount(); j++) {
				allX.add(series.getX(j).doubleValue());
			}
		}

		XYSeries seriesSum = new XYSeries(cage.getCageID() + "_Sum", false);
		XYSeries seriesPI = new XYSeries(cage.getCageID() + "_PI", false);

		seriesSum.setDescription(buildSeriesDescription(cage, "L"));
		seriesPI.setDescription(buildSeriesDescription(cage, "R"));

		for (Double x : allX) {
			double sumL = 0;
			double sumR = 0;
			boolean hasL = false;
			boolean hasR = false;

			for (XYSeries s : listL) {
				int idx = s.indexOf(x);
				if (idx >= 0) {
					sumL += s.getY(idx).doubleValue();
					hasL = true;
				}
			}
			for (XYSeries s : listR) {
				int idx = s.indexOf(x);
				if (idx >= 0) {
					sumR += s.getY(idx).doubleValue();
					hasR = true;
				}
			}

			if (hasL || hasR) {
				double sum = sumL + sumR;
				double pi = (sum != 0) ? (sumL - sumR) / sum : 0;

				seriesSum.add(x.doubleValue(), sum);
				seriesPI.add(x.doubleValue(), pi);
			}
		}

		result.addSeries(seriesSum);
		result.addSeries(seriesPI);

		return result;
	}

	private static String buildSeriesDescription(Cage cage, String side) {
		CageProperties cageProp = cage.getProperties();
		Color color = Color.BLACK;
		if (side.contains("L") || side.contains("1"))
			color = Color.BLUE;
		else if (side.contains("R") || side.contains("2"))
			color = Color.RED;

		return "ID:" + cageProp.getCageID() + ":Pos:" + cageProp.getCagePosition() + ":nflies:"
				+ cageProp.getCageNFlies() + ":R:" + color.getRed() + ":G:" + color.getGreen() + ":B:"
				+ color.getBlue();
	}

	/**
	 * Builds a description string for a series.
	 * 
	 * @param cage the cage
	 * @param spot the spot
	 * @return formatted description string
	 */
	private static String buildSeriesDescriptionFromCageAndSpot(Cage cage, Spot spot) {
		CageProperties cageProp = cage.getProperties();
		Color color = spot.getProperties().getColor();
		return "ID:" + cageProp.getCageID() + ":Pos:" + cageProp.getCagePosition() + ":nflies:"
				+ cageProp.getCageNFlies() + ":R:" + color.getRed() + ":G:" + color.getGreen() + ":B:"
				+ color.getBlue();
	}

	private static String buildSeriesDescriptionFromCageAndCapillary(Cage cage, Capillary cap, int i) {
		CageProperties cageProp = cage.getProperties();
		String side = cap.getCapillarySide();
		Color[] palette = { Color.BLUE, Color.RED, Color.GREEN, Color.MAGENTA, Color.CYAN, Color.ORANGE, Color.PINK,
				Color.LIGHT_GRAY };
		Color color = palette[i % palette.length];

		if (side.contains("L") || side.contains("1"))
			color = Color.BLUE;
		else if (side.contains("R") || side.contains("2"))
			color = Color.RED;

//		if (cap.getRoi() != null)
//			color = cap.getRoi().getColor();

		return "ID:" + cageProp.getCageID() + ":Pos:" + cageProp.getCagePosition() + ":nflies:"
				+ cageProp.getCageNFlies() + ":R:" + color.getRed() + ":G:" + color.getGreen() + ":B:"
				+ color.getBlue();
	}

	private static XYSeries createXYSeriesFromSpotMeasure(Experiment exp, Spot spot, ResultsOptions resultOptions) {
		XYSeries seriesXY = new XYSeries(spot.getName(), false);

		if (exp.getSeqCamData().getTimeManager().getCamImagesTime_Ms() == null)
			exp.getSeqCamData().build_MsTimesArray_From_FileNamesList();
		double[] camImages_time_min = exp.getSeqCamData().getTimeManager().getCamImagesTime_Minutes();
		SpotMeasure spotMeasure = spot.getMeasurements(resultOptions.resultType);
		double divider = 1.;
		if (resultOptions.relativeToMaximum && resultOptions.resultType != EnumResults.AREA_FLYPRESENT) {
			divider = spotMeasure.getMaximumValue();
		}

		int npoints = spotMeasure.getCount();

		for (int j = 0; j < npoints; j++) {
			double x = camImages_time_min[j];
			double y = spotMeasure.getValueAt(j) / divider;
			seriesXY.add(x, y);

			if (Double.isNaN(ymax) || ymax < y) {
				ymax = y;
			}
			if (Double.isNaN(ymin) || ymin > y) {
				ymin = y;
			}
			x++;
		}
		return seriesXY;
	}

	private static XYSeries createXYSeriesFromCapillaryMeasure(Experiment exp, Capillary cap,
			ResultsOptions resultOptions) {
		XYSeries seriesXY = new XYSeries(cap.getCageID() + "_" + cap.getCapillarySide(), false);

		if (exp.getSeqCamData().getTimeManager().getCamImagesTime_Ms() == null)
			exp.getSeqCamData().build_MsTimesArray_From_FileNamesList();
		double[] camImages_time_min = exp.getSeqCamData().getTimeManager().getCamImagesTime_Minutes();
		CapillaryMeasure capMeasure = cap.getMeasurements(resultOptions.resultType);

		if (capMeasure == null)
			return null;

		int npoints = capMeasure.getNPoints();
		if (npoints > camImages_time_min.length)
			npoints = camImages_time_min.length;

		double scalingFactor = 1.0;
		if (resultOptions.resultType.toUnit().equals("volume (ul)")) {
			if (cap.getPixels() > 0)
				scalingFactor = cap.getVolume() / cap.getPixels();
		}

		for (int j = 0; j < npoints; j++) {
			double x = camImages_time_min[j];
			double y = capMeasure.getValueAt(j) * scalingFactor;
			seriesXY.add(x, y);

			if (Double.isNaN(ymax) || ymax < y) {
				ymax = y;
			}
			if (Double.isNaN(ymin) || ymin > y) {
				ymin = y;
			}
		}
		return seriesXY;
	}

	/**
	 * Updates the global min/max values based on current series extrema.
	 */
	private static void updateGlobalMaxMin() {
		if (!flagMaxMinSet) {
			globalYMax = ymax;
			globalYMin = ymin;
			globalXMax = xmax;
			flagMaxMinSet = true;
			// LOGGER.fine(
//					"Set initial global extrema: Y[" + globalYMin + ", " + globalYMax + "], X[0, " + globalXMax + "]");
		} else {
//			boolean updated = false;
			if (Double.isNaN(globalYMax) || globalYMax < ymax) {
				globalYMax = ymax;
//				updated = true;
			}
			if (Double.isNaN(globalYMin) || globalYMin > ymin) {
				globalYMin = ymin;
//				updated = true;
			}
			if (globalXMax < xmax) {
				globalXMax = xmax;
//				updated = true;
			}

//			if (updated) {
//				LOGGER.fine(
//						"Updated global extrema: Y[" + globalYMin + ", " + globalYMax + "], X[0, " + globalXMax + "]");
//			}
		}
	}

	/**
	 * Creates a renderer for the XY plot with appropriate styling.
	 * 
	 * @param xySeriesCollection the dataset to render
	 * @return configured XYLineAndShapeRenderer
	 */
	private static XYLineAndShapeRenderer getSubPlotRenderer(XYSeriesCollection xySeriesCollection) {
		if (xySeriesCollection == null) {
			LOGGER.warning("Cannot create renderer: dataset is null");
			return null;
		}

		XYLineAndShapeRenderer subPlotRenderer = new XYLineAndShapeRenderer(true, false);
		Stroke stroke = new BasicStroke(DEFAULT_STROKE_WIDTH, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f,
				DASH_PATTERN, DASH_PHASE);

		for (int i = 0; i < xySeriesCollection.getSeriesCount(); i++) {
			try {
				String[] description = xySeriesCollection.getSeries(i).getDescription().split(DESCRIPTION_DELIMITER);
				if (description.length >= 12) {
					int r = Integer.parseInt(description[7]);
					int g = Integer.parseInt(description[9]);
					int b = Integer.parseInt(description[11]);
					subPlotRenderer.setSeriesPaint(i, new ChartColor(r, g, b));

					String key = (String) xySeriesCollection.getSeriesKey(i);
					if (key != null && key.contains(SECONDARY_DATA_TOKEN)) {
						subPlotRenderer.setSeriesStroke(i, stroke);
					}
				} else {
					LOGGER.warning("Invalid description format for series " + i + ", using default color");
					subPlotRenderer.setSeriesPaint(i, ChartColor.BLACK);
				}
			} catch (NumberFormatException e) {
				LOGGER.warning("Could not parse color values for series " + i + ": " + e.getMessage());
				subPlotRenderer.setSeriesPaint(i, ChartColor.BLACK);
			}
		}

		// LOGGER.fine("Created renderer for " + xySeriesCollection.getSeriesCount() + "
		// series");
		return subPlotRenderer;
	}

	/**
	 * Gets the global maximum Y value.
	 * 
	 * @return the global Y maximum
	 */
	public static double getGlobalYMax() {
		return globalYMax;
	}

	/**
	 * Gets the global minimum Y value.
	 * 
	 * @return the global Y minimum
	 */
	public static double getGlobalYMin() {
		return globalYMin;
	}

	/**
	 * Gets the global maximum X value.
	 * 
	 * @return the global X maximum
	 */
	public static double getGlobalXMax() {
		return globalXMax;
	}

	/**
	 * Checks if global min/max values have been set.
	 * 
	 * @return true if global extrema are set, false otherwise
	 */
	public static boolean isGlobalMaxMinSet() {
		return flagMaxMinSet;
	}

}
