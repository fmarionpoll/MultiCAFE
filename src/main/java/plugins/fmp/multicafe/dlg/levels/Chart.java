package plugins.fmp.multicafe.dlg.levels;

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import icy.gui.viewer.Viewer;
import icy.sequence.Sequence;
import icy.sequence.SequenceEvent;
import icy.sequence.SequenceListener;
import plugins.fmp.multicafe.MultiCAFE;
import plugins.fmp.multicafe.fmp_experiment.Experiment;
import plugins.fmp.multicafe.fmp_experiment.cages.Cage;
import plugins.fmp.multicafe.fmp_experiment.cages.CageString;
import plugins.fmp.multicafe.fmp_experiment.capillaries.Capillaries;
import plugins.fmp.multicafe.fmp_experiment.capillaries.Capillary;
import plugins.fmp.multicafe.fmp_tools.chart.ChartLevelsFrame;
import plugins.fmp.multicafe.fmp_tools.results.EnumResults;
import plugins.fmp.multicafe.fmp_tools.results.ResultsOptions;
import plugins.fmp.multicafe.fmp_tools.results.ResultsOptionsBuilder;

public class Chart extends JPanel implements SequenceListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = -7079184380174992501L;

	private ChartLevelsFrame activeChart = null;
	private EnumResults currentResultType = null;
	private ChartCageArrayFrame chartCageArrayFrame = null;
	private MultiCAFE parent0 = null;

	// Global window positions - shared across all experiments
	private static Rectangle globalChartBounds = null;

	private EnumResults[] measures = new EnumResults[] { //
			EnumResults.TOPRAW, //
			EnumResults.TOPLEVEL, //
			EnumResults.BOTTOMLEVEL, //
			EnumResults.DERIVEDVALUES, //
			EnumResults.TOPLEVEL_LR, //
			EnumResults.SUMGULPS };
	private JComboBox<EnumResults> resultTypeComboBox = new JComboBox<EnumResults>(measures);

	private JCheckBox correctEvaporationCheckbox = new JCheckBox("correct evaporation", false);
	private JButton displayResultsButton = new JButton("Display results");
	private JButton axisOptionsButton = new JButton("Axis options");
	private JRadioButton displayAllButton = new JRadioButton("all cages");
	private JRadioButton displaySelectedButton = new JRadioButton("cage selected");

	private AxisOptions graphOptions = null;

	void init(GridLayout capLayout, MultiCAFE parent0) {
		setLayout(capLayout);
		this.parent0 = parent0;
		setLayout(capLayout);
		FlowLayout layout = new FlowLayout(FlowLayout.LEFT);
		layout.setVgap(0);

		JPanel panel = new JPanel(layout);
		panel.add(resultTypeComboBox);
		add(panel);

		JPanel panel1 = new JPanel(layout);
		panel1.add(correctEvaporationCheckbox);
		panel1.add(displayAllButton);
		panel1.add(displaySelectedButton);
		add(panel1);

		JPanel panel04 = new JPanel(layout);
		panel04.add(displayResultsButton);
		panel04.add(axisOptionsButton);
		add(panel04);

		ButtonGroup group1 = new ButtonGroup();
		group1.add(displayAllButton);
		group1.add(displaySelectedButton);
		displayAllButton.setSelected(true);

		resultTypeComboBox.setSelectedIndex(1);
		defineActionListeners();
	}

	private void defineActionListeners() {

		resultTypeComboBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListComboLazy.getSelectedItem();
				if (exp != null) {
					displayChartPanels(exp); // displayGraphsPanels(exp);
				}
			}
		});

		displayResultsButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListComboLazy.getSelectedItem();
				if (exp != null) {
					displayChartPanels(exp); // displayGraphsPanels(exp);
				}
			}
		});

		correctEvaporationCheckbox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListComboLazy.getSelectedItem();
				if (exp != null) {
					displayChartPanels(exp); // displayGraphsPanels(exp);
				}
			}
		});

		axisOptionsButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListComboLazy.getSelectedItem();
				if (exp != null) {
					if (graphOptions != null) {
						graphOptions.close();
					}
					graphOptions = new AxisOptions();
					graphOptions.requestFocus();
				}
			}
		});
	}

	// ------------------------------------------------ OPTION 2

	public void displayChartPanels(Experiment exp) {
		exp.getSeqCamData().getSequence().removeListener(this);
		EnumResults exportType = (EnumResults) resultTypeComboBox.getSelectedItem();
		if (isThereAnyDataToDisplay(exp, exportType))
			chartCageArrayFrame = plotCapillaryMeasuresToChart(exp, exportType, chartCageArrayFrame);
		exp.getSeqCamData().getSequence().addListener(this);
	}

	private ChartCageArrayFrame plotCapillaryMeasuresToChart(Experiment exp, EnumResults resultType,
			ChartCageArrayFrame iChart) {
		if (iChart != null)
			iChart.getMainChartFrame().dispose();

		int first = 0;
		int last = exp.getCages().getCageList().size() - 1;
		if (!displayAllButton.isSelected()) {
			Cage cageFound = exp.getCages().findFirstCageWithSelectedSpot();
			if (cageFound == null)
				cageFound = exp.getCages().findFirstSelectedCage();
			if (cageFound == null)
				return null;
			exp.getSeqCamData().centerDisplayOnRoi(cageFound.getRoi());
			String cageNumber = CageString.getCageNumberFromCageRoiName(cageFound.getRoi().getName());
			first = Integer.parseInt(cageNumber);
			last = first;
		}

		ResultsOptions options = ResultsOptionsBuilder.forChart() //
				.withBuildExcelStepMs(60000) //
				.withSubtractT0(true) //
				.withResultType(resultType) //
				.withCageRange(first, last) //
				.build();

		iChart = new ChartCageArrayFrame();
		iChart.createMainChartPanel("Capillary level measures", exp, options);
		iChart.setChartSpotUpperLeftLocation(getInitialUpperLeftPosition(exp));
		iChart.displayData(exp, options);
		iChart.getMainChartFrame().toFront();
		iChart.getMainChartFrame().requestFocus();
		return iChart;
	}
	// ------------------------------------------------

	private Rectangle getInitialUpperLeftPosition(Experiment exp) {
		Rectangle rectv = new Rectangle(50, 500, 10, 10);
		if (exp.getSeqCamData() != null && exp.getSeqCamData().getSequence() != null) {
			Viewer v = exp.getSeqCamData().getSequence().getFirstViewer();
			if (v != null) {
				rectv = v.getBounds();
			} else {
				rectv = parent0.mainFrame.getBounds();
				rectv.translate(0, 150);
			}
		} else {
			if (parent0 != null && parent0.mainFrame != null) {
				rectv = parent0.mainFrame.getBounds();
				rectv.translate(0, 150);
			}
		}
		return rectv;
	}

	// ------------------------------------------------ OPTION 1

//	public void displayGraphsPanels(Experiment exp) {
//		if (exp == null)
//			return;
//
//		if (exp.getSeqKymos() != null && exp.getSeqKymos().getSequence() != null)
//			exp.getSeqKymos().getSequence().addListener(this);
//
//		EnumResults selectedOption = (EnumResults) resultTypeComboBox.getSelectedItem();
//
//		if (activeChart != null && currentResultType != null) {
//			if (activeChart.getMainChartFrame() != null)
//				globalChartBounds = activeChart.getMainChartFrame().getBounds();
//			activeChart.getMainChartFrame().dispose();
//			activeChart = null;
//		}
//
//		if (isThereAnyDataToDisplay(exp, selectedOption)) {
//			Rectangle rectv = getInitialUpperLeftPosition(exp);
//			Rectangle pos = (globalChartBounds != null) ? globalChartBounds : rectv;
//
//			String title = selectedOption.toTitle();
//			if (selectedOption == EnumResults.TOPLEVEL && !correctEvaporationCheckbox.isSelected()) {
//				title = EnumResults.TOPRAW.toTitle();
//			}
//			activeChart = plotToChart(exp, title, selectedOption, pos);
//			currentResultType = selectedOption;
//		}
//	}
//
//	private ChartLevelsFrame plotToChart(Experiment exp, String title, EnumResults resultType, Rectangle rectv) {
//		ChartLevelsFrame iChart = new ChartLevelsFrame();
//		iChart.createChartPanel(parent0, "Capillary levels measurement", rectv);
//		iChart.displayData(exp, resultType, title, correctEvaporationCheckbox.isSelected());
//		if (iChart.getMainChartFrame() != null) {
//			iChart.getMainChartFrame().toFront();
//			iChart.getMainChartFrame().requestFocus();
//		}
//		return iChart;
//	}

	// ------------------------------------------------

	public void closeAllCharts() {
		if (activeChart != null) {
			if (activeChart.getMainChartFrame() != null) {
				globalChartBounds = activeChart.getMainChartFrame().getBounds();
				activeChart.getMainChartFrame().dispose();
			}
			activeChart = null;
		}
	}

	private boolean isThereAnyDataToDisplay(Experiment exp, EnumResults resultType) {
		boolean flag = false;
		Capillaries capillaries = exp.getCapillaries();
		for (Capillary cap : capillaries.getList()) {
			flag = cap.isThereAnyMeasuresDone(resultType);
			if (flag)
				break;
		}
		return flag;
	}

	@Override
	public void sequenceChanged(SequenceEvent sequenceEvent) {
	}

	@Override
	public void sequenceClosed(Sequence sequence) {
		sequence.removeListener(this);

		// Save window positions before closing (global positions, shared across all
		// experiments)
		saveChartPositions();

		closeAllCharts();
	}

	private void saveChartPositions() {
		if (activeChart != null && activeChart.getMainChartFrame() != null) {
			globalChartBounds = activeChart.getMainChartFrame().getBounds();
		}
	}
}
