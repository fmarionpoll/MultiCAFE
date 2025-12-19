package plugins.fmp.multicafe.dlg.levels;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.data.Range;
import org.jfree.data.xy.XYSeriesCollection;

import icy.gui.frame.IcyFrame;
import icy.gui.util.GuiUtil;
import plugins.fmp.multicafe.fmp_experiment.Experiment;
import plugins.fmp.multicafe.fmp_experiment.cages.Cage;
import plugins.fmp.multicafe.fmp_tools.chart.ChartCageBuild;
import plugins.fmp.multicafe.fmp_tools.chart.ChartCagePair;
import plugins.fmp.multicafe.fmp_tools.chart.ChartCagePanel;
import plugins.fmp.multicafe.fmp_tools.chart.builders.CageCapillarySeriesBuilder;
import plugins.fmp.multicafe.fmp_tools.chart.plot.CageChartPlotFactory;
import plugins.fmp.multicafe.fmp_tools.results.EnumResults;
import plugins.fmp.multicafe.fmp_tools.results.ResultsOptions;

/**
 * Chart display class for spot data visualization. This class creates and
 * manages a grid of charts displaying spot measurements for different cages in
 * an experiment. It provides interactive functionality for clicking on chart
 * elements to select corresponding spots and navigate to relevant data views.
 * 
 * <p>
 * ChartSpots creates a grid layout of charts where each chart represents a cage
 * and displays spot measurement data over time. Users can click on chart
 * elements to select spots and navigate to the corresponding data in the main
 * application.
 * </p>
 * 
 * <p>
 * Usage example:
 * 
 * <pre>
 * ChartSpots chartSpots = new ChartSpots();
 * chartSpots.createPanel("Spot Charts", experiment, exportOptions, parent);
 * chartSpots.displayData(experiment, exportOptions);
 * </pre>
 * 
 * @author MultiSPOTS96
 * @see org.jfree.chart.ChartPanel
 * @see plugins.fmp.multiSPOTS96.experiment.Experiment
 * @see plugins.fmp.multiSPOTS96.experiment.cages.Cage
 * @see plugins.fmp.multiSPOTS96.experiment.spots.Spot
 */
public class ChartCageArrayFrame extends IcyFrame {

	/** Logger for this class */
	private static final Logger LOGGER = Logger.getLogger(ChartCageArrayFrame.class.getName());

	/** Default chart width in pixels */
	private static final int DEFAULT_CHART_WIDTH = 200;

	/** Default chart height in pixels */
	private static final int DEFAULT_CHART_HEIGHT = 100;

	/** Default minimum chart width in pixels */
	private static final int MIN_CHART_WIDTH = 50;

	/** Default maximum chart width in pixels */
	private static final int MAX_CHART_WIDTH = 1200;

	/** Default minimum chart height in pixels */
	private static final int MIN_CHART_HEIGHT = 25;

	/** Default maximum chart height in pixels */
	private static final int MAX_CHART_HEIGHT = 600;

	/** Default frame width in pixels */
	private static final int DEFAULT_FRAME_WIDTH = 300;

	/** Default frame height in pixels */
	private static final int DEFAULT_FRAME_HEIGHT = 70;

	/** Default Y-axis range for relative data */
	private static final double RELATIVE_Y_MIN = -0.2;

	/** Default Y-axis range for relative data */
	private static final double RELATIVE_Y_MAX = 1.2;

	/** Main chart panel containing all charts */
	private JPanel mainChartPanel = null;

	/** Main chart frame */
	public IcyFrame mainChartFrame = null;

	/** Y-axis range for charts */
	private Range yRange = null;

	/** X-axis range for charts */
	private Range xRange = null;

	/** Chart location */
	private Point graphLocation = new Point(0, 0);

	/** Number of panels along X axis */
	private int nPanelsAlongX = 1;

	/** Number of panels along Y axis */
	private int nPanelsAlongY = 1;

	/** Array of chart panel pairs */
	public ChartCagePair[][] chartPanelArray = null;

	/** Current experiment */
	private Experiment experiment = null;

	/** Chart interaction handler */
	private ChartInteractionHandler interactionHandler = null;

	/** Current results options */
	private ResultsOptions currentOptions = null;

	/** Base title for the chart window */
	private String baseTitle = null;

	/** ComboBox for selecting measurement type */
	private JComboBox<EnumResults> resultTypeComboBox = null;

	/** Reference to parent combobox in Chart.java for synchronization */
	private JComboBox<EnumResults> parentComboBox = null;

	/** Bottom panel containing legend */
	private JPanel bottomPanel = null;

	/**
	 * Available measurement types - will be populated from parent combobox if
	 * available
	 */
	private EnumResults[] measurementTypes = null;

//	/** Parent MultiCAFE/MultiSPOTS96 instance */
//	private MultiCAFE parent = null;

	/**
	 * Creates the main chart panel and frame.
	 * 
	 * @param title   the title for the chart window
	 * @param exp     the experiment containing the data
	 * @param options the export options for data processing
	 * @param parent0 the parent MultiSPOTS96 instance
	 * @throws IllegalArgumentException if any required parameter is null
	 */
	public void createMainChartPanel(String title, Experiment exp, ResultsOptions options) {
		if (exp == null) {
			throw new IllegalArgumentException("Experiment cannot be null");
		}
		if (options == null) {
			throw new IllegalArgumentException("Export options cannot be null");
		}
		if (title == null || title.trim().isEmpty()) {
			throw new IllegalArgumentException("Title cannot be null or empty");
		}

		this.experiment = exp;
		this.currentOptions = options;
		this.baseTitle = title;

		mainChartPanel = new JPanel();
		boolean flag = (options.cageIndexFirst == options.cageIndexLast);
		nPanelsAlongX = flag ? 1 : exp.getCages().nCagesAlongX;
		nPanelsAlongY = flag ? 1 : exp.getCages().nCagesAlongY;

		mainChartPanel.setLayout(new GridLayout(nPanelsAlongY, nPanelsAlongX));
		String finalTitle = title + ": " + options.resultType.toString();

		// Reuse existing frame if it's still valid (has a parent or is visible),
		// otherwise create a new one
		if (mainChartFrame != null && (mainChartFrame.getParent() != null || mainChartFrame.isVisible())) {
			mainChartFrame.setTitle(finalTitle);
			mainChartFrame.removeAll();
		} else {
			mainChartFrame = GuiUtil.generateTitleFrame(finalTitle, new JPanel(),
					new Dimension(DEFAULT_FRAME_WIDTH, DEFAULT_FRAME_HEIGHT), true, true, true, true);
		}

		mainChartFrame.setLayout(new BorderLayout());

		JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

		// Get measurement types from parent combobox if available, otherwise use
		// default
		EnumResults[] typesToUse = getMeasurementTypes();
		resultTypeComboBox = new JComboBox<EnumResults>(typesToUse);
		resultTypeComboBox.setSelectedItem(options.resultType);
		resultTypeComboBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				EnumResults selectedType = (EnumResults) resultTypeComboBox.getSelectedItem();
				if (selectedType != null && currentOptions != null) {
					currentOptions.resultType = selectedType;
					updateFrameTitle();
					updateLegendPanel();
					displayData(experiment, currentOptions);

					// Synchronize with parent combobox if it exists
					if (parentComboBox != null && parentComboBox.getSelectedItem() != selectedType) {
						// Temporarily remove action listeners to avoid recursive updates
						ActionListener[] listeners = parentComboBox.getActionListeners();
						for (ActionListener listener : listeners) {
							parentComboBox.removeActionListener(listener);
						}
						parentComboBox.setSelectedItem(selectedType);
						// Re-add the listeners
						for (ActionListener listener : listeners) {
							parentComboBox.addActionListener(listener);
						}
					}
				}
			}
		});
		topPanel.add(resultTypeComboBox);
		mainChartFrame.add(topPanel, BorderLayout.NORTH);

		JScrollPane scrollPane = new JScrollPane(mainChartPanel);
		mainChartFrame.add(scrollPane, BorderLayout.CENTER);

		bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		updateLegendPanel();
		mainChartFrame.add(bottomPanel, BorderLayout.SOUTH);

		mainChartFrame.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				savePreferences();
			}

			@Override
			public void componentMoved(ComponentEvent e) {
				savePreferences();
			}
		});

		chartPanelArray = new ChartCagePair[nPanelsAlongY][nPanelsAlongX];

//		LOGGER.info("Created chart panel with " + nPanelsAlongY + "x" + nPanelsAlongX + " grid");
	}

	/**
	 * Sets up the Y-axis for a chart.
	 * 
	 * @param title          the axis title
	 * @param row            the row index
	 * @param col            the column index
	 * @param resultsOptions the export options
	 * @return configured NumberAxis
	 */
	private NumberAxis setYaxis(String label, int row, int col, ResultsOptions resultsOptions) {
		NumberAxis yAxis = new NumberAxis();
		row = row * experiment.getCages().nRowsPerCage;
		col = col * experiment.getCages().nColumnsPerCage;
		String yLegend = label // + " " + String.valueOf((char) (row + 'A')) + Integer.toString(col) + " "
				+ resultsOptions.resultType.toUnit();
		yAxis.setLabel(yLegend);

		if (resultsOptions.relativeToMaximum || resultsOptions.relativeToMedianT0) {
			yAxis.setAutoRange(false);
			yAxis.setRange(RELATIVE_Y_MIN, RELATIVE_Y_MAX);
		} else {
			yAxis.setAutoRange(true);
			yAxis.setAutoRangeIncludesZero(false);
		}

		return yAxis;
	}

	/**
	 * Sets up the X-axis for a chart.
	 * 
	 * @param title          the axis title
	 * @param resultsOptions the export options
	 * @return configured NumberAxis
	 */
	private NumberAxis setXaxis(String label, ResultsOptions resultsOptions) {
		NumberAxis xAxis = new NumberAxis();
		xAxis.setLabel(label);
		xAxis.setAutoRange(true);
		xAxis.setAutoRangeIncludesZero(false);
		return xAxis;
	}

	/**
	 * Displays spot data for the experiment.
	 * 
	 * @param exp            the experiment containing the data
	 * @param resultsOptions the export options for data processing
	 * @throws IllegalArgumentException if exp or resultsOptions is null
	 */
	public void displayData(Experiment exp, ResultsOptions resultsOptions) {
		if (exp == null) {
			throw new IllegalArgumentException("Experiment cannot be null");
		}
		if (resultsOptions == null) {
			throw new IllegalArgumentException("Export options cannot be null");
		}

		this.experiment = exp;
		this.currentOptions = resultsOptions;

		if (resultTypeComboBox != null && resultsOptions.resultType != null) {
			resultTypeComboBox.setSelectedItem(resultsOptions.resultType);
		}
		// Clear any previously displayed charts so empty datasets never leave stale
		// charts on screen.
		if (mainChartPanel != null) {
			mainChartPanel.removeAll();
		}
		// Ensure derived measures (evaporation-corrected TOPLEVEL, LR SUM/PI, etc.) are
		// computed
		// before we build the datasets used for plotting.
		exp.getCages().prepareComputations(exp, resultsOptions);
		// Initialize appropriate interaction handler
		interactionHandler = createInteractionHandler(exp, resultsOptions);
		createChartPanelArray(resultsOptions);
		arrangePanelsInDisplay(resultsOptions);
		displayChartFrame();

//		LOGGER.info("Displayed spot charts for experiment");
	}

	/**
	 * Creates chart panels for all cages in the experiment.
	 * 
	 * @param resultsOptions the export options
	 */
	private void createChartPanelArray(ResultsOptions resultsOptions) {
		// Update grid dimensions based on current experiment state
		boolean flag = (resultsOptions.cageIndexFirst == resultsOptions.cageIndexLast);
		nPanelsAlongX = flag ? 1 : experiment.getCages().nCagesAlongX;
		nPanelsAlongY = flag ? 1 : experiment.getCages().nCagesAlongY;

		// update layout
		mainChartPanel.setLayout(new GridLayout(nPanelsAlongY, nPanelsAlongX));

		// Reset array to ensure no stale panels from previous experiments/views
		chartPanelArray = new ChartCagePair[nPanelsAlongY][nPanelsAlongX];

		int indexCage = 0;
		ChartCageBuild.initMaxMin();
		Map<Cage, XYSeriesCollection> datasets = new HashMap<Cage, XYSeriesCollection>();

		CageCapillarySeriesBuilder builder = new CageCapillarySeriesBuilder();
		for (int row = 0; row < experiment.getCages().nCagesAlongY; row++) {
			for (int col = 0; col < experiment.getCages().nCagesAlongX; col++, indexCage++) {
				if (indexCage < resultsOptions.cageIndexFirst || indexCage > resultsOptions.cageIndexLast)
					continue;
				Cage cage = experiment.getCages().getCageFromRowColCoordinates(row, col);
				if (cage != null) {
					XYSeriesCollection xyDataSetList = builder.build(experiment, cage, resultsOptions);
					datasets.put(cage, xyDataSetList);
				}
			}
		}

		indexCage = 0;
		for (int row = 0; row < experiment.getCages().nCagesAlongY; row++) {
			for (int col = 0; col < experiment.getCages().nCagesAlongX; col++, indexCage++) {
				if (indexCage < resultsOptions.cageIndexFirst || indexCage > resultsOptions.cageIndexLast)
					continue;

				Cage cage = experiment.getCages().getCageFromRowColCoordinates(row, col);
				if (cage == null) {
					LOGGER.warning("No cage found at row " + row + ", col " + col);
					continue;
				}

				XYSeriesCollection xyDataSetList = datasets.get(cage);
				ChartPanel chartPanel = createChartPanelForCage(cage, row, col, resultsOptions, xyDataSetList);
				int arrayRow = flag ? 0 : row;
				int arrayCol = flag ? 0 : col;
				chartPanelArray[arrayRow][arrayCol] = new ChartCagePair(chartPanel, cage);
			}
		}
	}

	/**
	 * Creates a chart panel for a specific cage.
	 * 
	 * @param chartCage        the chart builder
	 * @param cage             the cage to create chart for
	 * @param xlsResultsArray  the primary data array
	 * @param xlsResultsArray2 the secondary data array
	 * @param row              the row index
	 * @param col              the column index
	 * @param resultsOptions   the export options
	 * @return configured ChartPanel
	 */
	private ChartCagePanel createChartPanelForCage(Cage cage, int row, int col, ResultsOptions resultsOptions,
			XYSeriesCollection xyDataSetList) {

		// If requested result isn't available, show an explicit placeholder rather than
		// leaving stale charts visible.
		if (xyDataSetList == null || xyDataSetList.getSeriesCount() == 0) {
			NumberAxis xAxis = setXaxis("", resultsOptions); // time - removed to gain place
			NumberAxis yAxis = setYaxis("", row, col, resultsOptions);
			XYPlot xyPlot = CageChartPlotFactory.buildXYPlot(new XYSeriesCollection(), xAxis, yAxis);
			JFreeChart chart = new JFreeChart(null, null, xyPlot, false);

			TextTitle title = new TextTitle("Cage " + cage.getProperties().getCageID() + " (no data)",
					new Font("SansSerif", Font.PLAIN, 12));
			title.setPosition(RectangleEdge.BOTTOM);
			chart.addSubtitle(title);
			chart.setID("row:" + row + ":icol:" + col + ":cageID:" + cage.getProperties().getCagePosition());

			ChartCagePanel chartCagePanel = new ChartCagePanel(chart, DEFAULT_CHART_WIDTH, DEFAULT_CHART_HEIGHT,
					MIN_CHART_WIDTH, MIN_CHART_HEIGHT, MAX_CHART_WIDTH, MAX_CHART_HEIGHT, true, true, true, true, false,
					true);
			chartCagePanel.subscribeToCagePropertiesUpdates(cage);
			return chartCagePanel;
		}

		if (cage.getCapillaries().getList().size() < 1) {
//			LOGGER.fine("Skipping cage " + cage.getProperties().getCageID() + " - no capillaries");
			ChartCagePanel chartPanel = new ChartCagePanel(null, // jfreechart
					DEFAULT_CHART_WIDTH, DEFAULT_CHART_HEIGHT, // preferred width, height of the panel
					MIN_CHART_WIDTH, MIN_CHART_HEIGHT, // minimal drawing width, drawing height
					MAX_CHART_WIDTH, MAX_CHART_HEIGHT, // maximumDrawWidth, maximumDrawHeight
					true, // use memory buffer to improve performance.
					true, // chart property editor available via popup menu
					true, // copy option available via popup menu
					true, // print option available via popup menu
					false, // zoom options not added to the popup menu
					true); // tooltips enabled for the chart;

			return chartPanel;
		}

		NumberAxis xAxis = setXaxis("", resultsOptions); // time - removed to gain place
		NumberAxis yAxis = setYaxis("", row, col, resultsOptions);

		if (!resultsOptions.relativeToMaximum && !resultsOptions.relativeToMedianT0) {
			if (ChartCageBuild.isGlobalMaxMinSet()) {
				double min = ChartCageBuild.getGlobalYMin();
				double max = ChartCageBuild.getGlobalYMax();
				double range = max - min;
				if (range == 0)
					range = 1.0;
				yAxis.setRange(min - range * 0.05, max + range * 0.05);
			}
		}

		XYPlot xyPlot = CageChartPlotFactory.buildXYPlot(xyDataSetList, xAxis, yAxis);

		JFreeChart chart = new JFreeChart(null, // title - the chart title (null permitted).
				null, // titleFont - the font for displaying the chart title (null permitted)
				xyPlot, // plot - controller of the visual representation of the data
				false); // createLegend - legend not created for the chart

		TextTitle title = new TextTitle("Cage " + cage.getProperties().getCageID(),
				new Font("SansSerif", Font.PLAIN, 12));
		title.setPosition(RectangleEdge.BOTTOM);
		chart.addSubtitle(title);

		chart.setID("row:" + row + ":icol:" + col + ":cageID:" + cage.getProperties().getCagePosition());

		ChartCagePanel chartCagePanel = new ChartCagePanel(chart, // jfreechart
				DEFAULT_CHART_WIDTH, DEFAULT_CHART_HEIGHT, // preferred width, height of the panel
				MIN_CHART_WIDTH, MIN_CHART_HEIGHT, // minimal drawing width, drawing height
				MAX_CHART_WIDTH, MAX_CHART_HEIGHT, // maximumDrawWidth, maximumDrawHeight
				true, // use memory buffer to improve performance.
				true, // chart property editor available via popup menu
				true, // copy option available via popup menu
				true, // print option available via popup menu
				false, // zoom options not added to the popup menu
				true); // tooltips enabled for the chart

		if (interactionHandler != null) {
			chartCagePanel.addChartMouseListener(interactionHandler.createMouseListener());
		}
		chartCagePanel.subscribeToCagePropertiesUpdates(cage);
		return chartCagePanel;
	}

	private class LegendItem extends JComponent {
		private static final long serialVersionUID = 1L;
		private String text;
		private Color color;

		public LegendItem(String text, Color color) {
			this.text = text;
			this.color = color;
			setPreferredSize(new Dimension(100, 20));
		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			g.setColor(color);
			g.drawLine(0, 10, 20, 10);
			g.setColor(Color.BLACK);
			g.drawString(text, 25, 15);
		}
	}

	/**
	 * Arranges panels in the display based on export options.
	 * 
	 * @param resultsOptions the export options
	 */
	private void arrangePanelsInDisplay(ResultsOptions resultsOptions) {
		// Ensure we never keep previously displayed panels when the new dataset is
		// empty.
		mainChartPanel.removeAll();
		if (resultsOptions.cageIndexFirst == resultsOptions.cageIndexLast) {
			// When displaying a single cage, the array is 1x1 and the panel is always at
			// [0][0]
			if (chartPanelArray != null && chartPanelArray.length > 0 && chartPanelArray[0].length > 0) {
				ChartCagePair pair = chartPanelArray[0][0];
				if (pair != null && pair.getChartPanel() != null) {
					mainChartPanel.add(pair.getChartPanel());
				} else {
					mainChartPanel.add(new JPanel());
				}
			} else {
				mainChartPanel.add(new JPanel());
			}
		} else {
			for (int row = 0; row < nPanelsAlongY; row++) {
				for (int col = 0; col < nPanelsAlongX; col++) {
					ChartPanel chartPanel = null;
					if (row < chartPanelArray.length && col < chartPanelArray[0].length) {
						ChartCagePair pair = chartPanelArray[row][col];
						if (pair != null) {
							chartPanel = pair.getChartPanel();
						}
					}

					if (chartPanel == null) {
						mainChartPanel.add(new JPanel());
					} else {
						mainChartPanel.add(chartPanel);
					}
				}
			}
		}

		mainChartPanel.revalidate();
		mainChartPanel.repaint();
	}

	/**
	 * Displays the chart frame.
	 */
	private void displayChartFrame() {
		if (mainChartFrame == null) {
			LOGGER.warning("Cannot display chart frame: mainChartFrame is null");
			return;
		}

		mainChartFrame.pack();
		loadPreferences();

		// Only add to desktop pane if not already added
		if (mainChartFrame.getParent() == null) {
			mainChartFrame.addToDesktopPane();
		}

		mainChartFrame.setVisible(true);
//		LOGGER.fine("Displayed chart frame at location: " + graphLocation);
	}

	private void loadPreferences() {
		Preferences prefs = Preferences.userNodeForPackage(ChartCageArrayFrame.class);
		int x = prefs.getInt("window_x", graphLocation.x);
		int y = prefs.getInt("window_y", graphLocation.y);
		int w = prefs.getInt("window_w", DEFAULT_FRAME_WIDTH);
		int h = prefs.getInt("window_h", DEFAULT_FRAME_HEIGHT);
		mainChartFrame.setBounds(new Rectangle(x, y, w, h));
	}

	private void savePreferences() {
		Preferences prefs = Preferences.userNodeForPackage(ChartCageArrayFrame.class);
		Rectangle r = mainChartFrame.getBounds();
		prefs.putInt("window_x", r.x);
		prefs.putInt("window_y", r.y);
		prefs.putInt("window_w", r.width);
		prefs.putInt("window_h", r.height);
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
//		LOGGER.fine("Set chart location to: " + graphLocation);
	}

	/**
	 * Updates the frame title to reflect the current measurement type.
	 */
	private void updateFrameTitle() {
		if (mainChartFrame != null && baseTitle != null && currentOptions != null) {
			String finalTitle = baseTitle + ": " + currentOptions.resultType.toString();
			mainChartFrame.setTitle(finalTitle);
		}
	}

	/**
	 * Updates the legend panel based on the current result type.
	 */
	private void updateLegendPanel() {
		if (bottomPanel == null || currentOptions == null) {
			return;
		}

		bottomPanel.removeAll();
		if (ChartCageBuild.isLRType(currentOptions.resultType)) {
			bottomPanel.add(new LegendItem("Sum", Color.BLUE));
			bottomPanel.add(new LegendItem("PI", Color.RED));
		} else {
			bottomPanel.add(new LegendItem("L", Color.BLUE));
			bottomPanel.add(new LegendItem("R", Color.RED));
		}
		bottomPanel.revalidate();
		bottomPanel.repaint();
	}

	/**
	 * Creates the appropriate interaction handler based on the result type.
	 * 
	 * @param exp            the experiment
	 * @param resultsOptions the export options
	 * @return the appropriate interaction handler
	 */
	private ChartInteractionHandler createInteractionHandler(Experiment exp, ResultsOptions resultsOptions) {
		if (isSpotResultType(resultsOptions.resultType)) {
			return new SpotChartInteractionHandler(exp, resultsOptions, chartPanelArray);
		} else {
			// Default to capillary handler for capillary types and others
			return new CapillaryChartInteractionHandler(exp, resultsOptions, chartPanelArray);
		}
	}

	/**
	 * Determines if a result type is for spot measurements.
	 * 
	 * @param resultType the result type to check
	 * @return true if it's a spot type, false otherwise
	 */
	private boolean isSpotResultType(EnumResults resultType) {
		if (resultType == null) {
			return false;
		}
		switch (resultType) {
		case AREA_SUM:
		case AREA_SUMCLEAN:
		case AREA_OUT:
		case AREA_DIFF:
		case AREA_FLYPRESENT:
			return true;
		default:
			return false;
		}
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
	 * Gets the chart panel array.
	 * 
	 * @return the chart panel array
	 */
	public ChartCagePair[][] getChartCagePairArray() {
		return chartPanelArray;
	}

	/**
	 * Gets the number of panels along X axis.
	 * 
	 * @return the number of panels along X
	 */
	public int getPanelsAlongX() {
		return nPanelsAlongX;
	}

	/**
	 * Gets the number of panels along Y axis.
	 * 
	 * @return the number of panels along Y
	 */
	public int getPanelsAlongY() {
		return nPanelsAlongY;
	}

	public Range getXRange() {
		return xRange;
	}

	public void setXRange(Range range) {
		xRange = range;
	}

	public Range getYRange() {
		return yRange;
	}

	public void setYRange(Range range) {
		yRange = range;
	}

	/**
	 * Sets the parent combobox reference for synchronization. When the chart's
	 * combobox changes, it will update the parent combobox to keep them in sync.
	 * Also extracts the measurement types from the parent combobox to keep the
	 * lists synchronized.
	 * 
	 * @param comboBox the parent combobox to synchronize with
	 */
	public void setParentComboBox(JComboBox<EnumResults> comboBox) {
		this.parentComboBox = comboBox;
		// Extract measurement types from parent combobox model
		if (comboBox != null) {
			ComboBoxModel<EnumResults> model = comboBox.getModel();
			int size = model.getSize();
			EnumResults[] types = new EnumResults[size];
			for (int i = 0; i < size; i++) {
				types[i] = model.getElementAt(i);
			}
			this.measurementTypes = types;
		}
	}

	/**
	 * Gets the measurement types to use for the combobox. If a parent combobox is
	 * set, uses its items. Otherwise falls back to a default list.
	 * 
	 * @return array of measurement types
	 */
	private EnumResults[] getMeasurementTypes() {
		if (measurementTypes != null && measurementTypes.length > 0) {
			return measurementTypes;
		}
		// Fallback default list if parent combobox is not set
		return new EnumResults[] { //
				EnumResults.TOPRAW, //
				EnumResults.TOPLEVEL, //
				EnumResults.BOTTOMLEVEL, //
				EnumResults.TOPLEVEL_LR, //
				EnumResults.DERIVEDVALUES, //
				EnumResults.SUMGULPS, //
				EnumResults.SUMGULPS_LR };
	}

}
