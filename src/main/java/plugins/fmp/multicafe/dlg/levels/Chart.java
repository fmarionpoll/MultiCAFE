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

	private ChartLevelsFrame plotTopAndBottom = null;
	private ChartLevelsFrame plotDelta = null;
	private ChartLevelsFrame plotDerivative = null;
	private ChartLevelsFrame plotSumgulps = null;
	private plugins.fmp.multicafe.dlg.levels.ChartCageArrayFrame chartCageArrayFrame = null;

	private MultiCAFE parent0 = null;

	// Global window positions - shared across all experiments
//	private static Rectangle globalChartTopBottomBounds = null;
	private static Rectangle globalChartDeltaBounds = null;
//	private static Rectangle globalChartDerivativeBounds = null;
	private static Rectangle globalChartSumGulpsBounds = null;

	private EnumResults[] measures = new EnumResults[] { //
			EnumResults.TOPRAW, //
			EnumResults.TOPLEVEL, // ,
			EnumResults.DERIVEDVALUES, //
			EnumResults.TOPLEVEL_LR //
	};
	private JComboBox<EnumResults> exportTypeComboBox = new JComboBox<EnumResults>(measures);

//	private JCheckBox limitsCheckbox = new JCheckBox("top/bottom", false);
//	private JCheckBox derivativeCheckbox = new JCheckBox("derivative", false);
	private JCheckBox consumptionCheckbox = new JCheckBox("sumGulps", false);
	private JCheckBox sumPICheckbox = new JCheckBox("SUM/PI", false);
	private JCheckBox correctEvaporationCheckbox = new JCheckBox("correct evaporation", false);
	private JButton displayResultsButton = new JButton("Display results");
	private JButton axisOptionsButton = new JButton("Axis options");
	private JRadioButton displayAllButton = new JRadioButton("all cages");
	private JRadioButton displaySelectedButton = new JRadioButton("cage selected");

	private AxisOptions graphOptions = null;
//	private EnumXLSExport[] measures = new EnumXLSExport[] { //
//			EnumXLSExport.AREA_SUM, //
//			EnumXLSExport.AREA_SUMCLEAN // ,
//			// EnumXLSExportType.AREA_DIFF
//	};

	void init(GridLayout capLayout, MultiCAFE parent0) {
		setLayout(capLayout);
		this.parent0 = parent0;
		setLayout(capLayout);
		FlowLayout layout = new FlowLayout(FlowLayout.LEFT);
		layout.setVgap(0);

		JPanel panel = new JPanel(layout);
		panel.add(exportTypeComboBox);

//		panel.add(limitsCheckbox);
//		panel.add(derivativeCheckbox);
		panel.add(consumptionCheckbox);
		panel.add(sumPICheckbox);
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

		exportTypeComboBox.setSelectedIndex(1);
		defineActionListeners();
	}

	private void defineActionListeners() {

		exportTypeComboBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListComboLazy.getSelectedItem();
				if (exp != null)
					displayChartPanels(exp);
			}
		});

		displayResultsButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListComboLazy.getSelectedItem();
				if (exp != null)
					displayGraphsPanels(exp);
			}
		});

//		limitsCheckbox.addActionListener(new ActionListener() {
//			@Override
//			public void actionPerformed(final ActionEvent e) {
//				Experiment exp = (Experiment) parent0.expListComboLazy.getSelectedItem();
//				if (exp != null) {
//					displayGraphsPanels(exp);
//				}
//			}
//		});

		consumptionCheckbox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListComboLazy.getSelectedItem();
				if (exp != null) {
					displayGraphsPanels(exp);
				}
			}
		});

//		derivativeCheckbox.addActionListener(new ActionListener() {
//			@Override
//			public void actionPerformed(final ActionEvent e) {
//				Experiment exp = (Experiment) parent0.expListComboLazy.getSelectedItem();
//				if (exp != null) {
//					displayGraphsPanels(exp);
//				}
//			}
//		});

		sumPICheckbox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListComboLazy.getSelectedItem();
				if (exp != null) {
					displayGraphsPanels(exp);
				}
			}
		});

		correctEvaporationCheckbox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListComboLazy.getSelectedItem();
				if (exp != null) {
					displayGraphsPanels(exp);
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

	public void displayChartPanels(Experiment exp) {
		exp.getSeqCamData().getSequence().removeListener(this);
		EnumResults exportType = (EnumResults) exportTypeComboBox.getSelectedItem();
		if (isThereAnyDataToDisplay(exp, exportType))
			chartCageArrayFrame = plotSpotMeasuresToChart(exp, exportType, chartCageArrayFrame);
		exp.getSeqCamData().getSequence().addListener(this);
	}

	private ChartCageArrayFrame plotSpotMeasuresToChart(Experiment exp, EnumResults exportType,
			ChartCageArrayFrame iChart) {
		if (iChart != null)
			iChart.getMainChartFrame().dispose();

		int first = 0;
		int last = exp.getCages().getCageList().size() - 1;
		if (!displayAllButton.isSelected()) {
			plugins.fmp.multicafe.fmp_experiment.cages.Cage cageFound = exp.getCages().findFirstCageWithSelectedSpot();
			if (cageFound == null)
				cageFound = exp.getCages().findFirstSelectedCage();
			if (cageFound == null)
				return null;
			exp.getSeqCamData().centerDisplayOnRoi(cageFound.getRoi());
			String cageNumber = CageString.getCageNumberFromCageRoiName(cageFound.getRoi().getName());
			first = Integer.parseInt(cageNumber);
			last = first;
		}

		ResultsOptions options = ResultsOptionsBuilder.forChart().withBuildExcelStepMs(60000)
				.withRelativeToT0(relativeToCheckbox.isSelected()).withExportType(exportType).withCageRange(first, last)
				.build();

		iChart = new ChartCageArrayFrame();
		iChart.createMainChartPanel("Capillary level measures", exp, options);
		iChart.setChartSpotUpperLeftLocation(getInitialUpperLeftPosition(exp));
		iChart.displayData(exp, options);
		iChart.getMainChartFrame().toFront();
		iChart.getMainChartFrame().requestFocus();
		return iChart;
	}

	public void displayGraphsPanels(Experiment exp) {
		if (exp == null) {
			return;
		}

		if (exp.getSeqKymos() != null && exp.getSeqKymos().getSequence() != null) {
			exp.getSeqKymos().getSequence().addListener(this);
		}

		Rectangle rectv = getInitialUpperLeftPosition(exp);
		int dx = 5;
		int dy = 10;

//		if (limitsCheckbox.isSelected() && isThereAnyDataToDisplay(exp, EnumResults.TOPLEVEL)
//				&& isThereAnyDataToDisplay(exp, EnumResults.BOTTOMLEVEL)) {
//			// Use saved global position if available, otherwise use initial position
//			Rectangle savedPos = globalChartTopBottomBounds;
//			Rectangle pos = (savedPos != null) ? savedPos : rectv;
//			plotTopAndBottom = plotToChart(exp, "top + bottom levels", EnumResults.TOPLEVEL, plotTopAndBottom, pos);
//			if (savedPos == null) {
//				rectv.translate(dx, dy);
//			}
//		} else if (plotTopAndBottom != null)
//			closeChart(plotTopAndBottom);

		if (sumPICheckbox.isSelected() && isThereAnyDataToDisplay(exp, EnumResults.TOPLEVELDELTA)) {
			// Use saved global position if available, otherwise use initial position
			Rectangle savedPos = globalChartDeltaBounds;
			Rectangle pos = (savedPos != null) ? savedPos : rectv;
			// plotDelta = plotToChart(exp, "top delta t -(t-1)", EnumResults.TOPLEVELDELTA,
			// plotDelta, pos);
			plotDelta = plotToChart(exp, "toplevel SUM and PI", EnumResults.TOPLEVEL_LR, plotDelta, pos);
			if (savedPos == null) {
				rectv.translate(dx, dy);
			}
		} else if (plotDelta != null)
			closeChart(plotDelta);

//		if (derivativeCheckbox.isSelected() && isThereAnyDataToDisplay(exp, EnumResults.DERIVEDVALUES)) {
//			// Use saved global position if available, otherwise use initial position
//			Rectangle savedPos = globalChartDerivativeBounds;
//			Rectangle pos = (savedPos != null) ? savedPos : rectv;
//			plotDerivative = plotToChart(exp, "Derivative", EnumResults.DERIVEDVALUES, plotDerivative, pos);
//			if (savedPos == null) {
//				rectv.translate(dx, dy);
//			}
//		} else if (plotDerivative != null)
//			closeChart(plotDerivative);

		if (consumptionCheckbox.isSelected() && isThereAnyDataToDisplay(exp, EnumResults.SUMGULPS)) {
			// Use saved global position if available, otherwise use initial position
			Rectangle savedPos = globalChartSumGulpsBounds;
			Rectangle pos = (savedPos != null) ? savedPos : rectv;
			plotSumgulps = plotToChart(exp, "Cumulated gulps", EnumResults.SUMGULPS, plotSumgulps, pos);
			if (savedPos == null) {
				rectv.translate(dx, dy);
			}
		} else if (plotSumgulps != null)
			closeChart(plotSumgulps);
	}

	private ChartLevelsFrame plotToChart(Experiment exp, String title, EnumResults resultType, ChartLevelsFrame iChart,
			Rectangle rectv) {
		if (iChart != null && iChart.getMainChartFrame() != null)
			iChart.getMainChartFrame().dispose();
		iChart = new ChartLevelsFrame();
		iChart.createChartPanel(parent0, title, rectv);
		iChart.displayData(exp, resultType, title, correctEvaporationCheckbox.isSelected());
		if (iChart.getMainChartFrame() != null) {
			iChart.getMainChartFrame().toFront();
			iChart.getMainChartFrame().requestFocus();
		}
		return iChart;
	}

	public void closeAllCharts() {
		closeChart(plotTopAndBottom);
		closeChart(plotDerivative);
		closeChart(plotSumgulps);
		closeChart(plotDelta);
	}

	private void closeChart(ChartLevelsFrame chart) {
		if (chart != null && chart.getMainChartFrame() != null)
			chart.getMainChartFrame().dispose();
		chart = null;
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
		// Save positions globally - these will be reused for all experiments
		if (plotTopAndBottom != null && plotTopAndBottom.getMainChartFrame() != null) {
			globalChartTopBottomBounds = plotTopAndBottom.getMainChartFrame().getBounds();
		}
		if (plotDelta != null && plotDelta.getMainChartFrame() != null) {
			globalChartDeltaBounds = plotDelta.getMainChartFrame().getBounds();
		}
		if (plotDerivative != null && plotDerivative.getMainChartFrame() != null) {
			globalChartDerivativeBounds = plotDerivative.getMainChartFrame().getBounds();
		}
		if (plotSumgulps != null && plotSumgulps.getMainChartFrame() != null) {
			globalChartSumGulpsBounds = plotSumgulps.getMainChartFrame().getBounds();
		}
	}
}
