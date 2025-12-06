package plugins.fmp.multicafe.dlg.levels;

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
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
import plugins.fmp.multicafe.fmp_tools.chart.ChartCageArrayFrame;
import plugins.fmp.multicafe.fmp_tools.toExcel.config.XLSExportOptions;
import plugins.fmp.multicafe.fmp_tools.toExcel.config.XLSExportOptionsBuilder;
import plugins.fmp.multicafe.fmp_tools.toExcel.enums.EnumXLSExport;

public class LevelsChart extends JPanel implements SequenceListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = -7079184380174992501L;
//	private ChartLevels plotTopAndBottom = null;
//	private ChartLevels plotDelta = null;
//	private ChartLevels plotDerivative = null;
//	private ChartLevels plotSumgulps = null;
	private ChartCageArrayFrame plotTopAndBottom = null;
	private ChartCageArrayFrame plotDelta = null;
	private ChartCageArrayFrame plotDerivative = null;
	private ChartCageArrayFrame plotSumgulps = null;

	private MultiCAFE parent0 = null;

	// Global window positions - shared across all experiments
	private static Rectangle globalChartTopBottomBounds = null;
	private static Rectangle globalChartDeltaBounds = null;
	private static Rectangle globalChartDerivativeBounds = null;
	private static Rectangle globalChartSumGulpsBounds = null;

	private JCheckBox limitsCheckbox = new JCheckBox("top/bottom", true);
	private JCheckBox derivativeCheckbox = new JCheckBox("derivative", false);
	private JCheckBox consumptionCheckbox = new JCheckBox("sumGulps", false);
	private JCheckBox deltaCheckbox = new JCheckBox("delta (Vt - Vt-1)", false);
	private JCheckBox correctEvaporationCheckbox = new JCheckBox("correct evaporation", false);
	private JButton displayResultsButton = new JButton("Display results");
	private JButton axisOptionsButton = new JButton("Axis options");
	private JRadioButton displayAllButton = new JRadioButton("all cages");
	private JRadioButton displaySelectedButton = new JRadioButton("cage selected");

	void init(GridLayout capLayout, MultiCAFE parent0) {
		setLayout(capLayout);
		this.parent0 = parent0;
		setLayout(capLayout);
		FlowLayout layout = new FlowLayout(FlowLayout.LEFT);
		layout.setVgap(0);

		JPanel panel = new JPanel(layout);
		panel.add(limitsCheckbox);
		panel.add(derivativeCheckbox);
		panel.add(consumptionCheckbox);
		panel.add(deltaCheckbox);
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

		defineActionListeners();
	}

	private void defineActionListeners() {
		displayResultsButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListComboLazy.getSelectedItem();
//				if (exp != null) {
//					exp.getSeqKymos().validateRois();
//					exp.getSeqKymos().transferKymosRoisToCapillaries_Measures(exp.getCapillaries());
//					displayGraphsPanels(exp);
//				}
				if (exp != null)
					displayGraphsPanels(exp);
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

	public void displayGraphsPanels(Experiment exp) {
		if (exp.getSeqKymos() == null || exp.getSeqKymos().getSequence() == null) {
			return; // Cannot display graphs without kymographs sequence
		}
		exp.getSeqKymos().getSequence().addListener(this);
		Rectangle rectv = getInitialUpperLeftPosition(exp);
		int dx = 5;
		int dy = 10;

		if (limitsCheckbox.isSelected() && isThereAnyDataToDisplay(exp, EnumXLSExport.TOPLEVEL)
				&& isThereAnyDataToDisplay(exp, EnumXLSExport.BOTTOMLEVEL)) {
			// Use saved global position if available, otherwise use initial position
			Rectangle savedPos = globalChartTopBottomBounds;
//			Rectangle pos = (savedPos != null) ? savedPos : rectv;
//			plotTopAndBottom = plotToChart(exp, "top + bottom levels", EnumXLSExport.TOPLEVEL, plotTopAndBottom, pos);
			plotMeasuresToChart(exp, EnumXLSExport.TOPLEVEL, plotTopAndBottom);
			if (savedPos == null) {
				rectv.translate(dx, dy);
			}
			plotTopAndBottom.toFront();
		} else if (plotTopAndBottom != null)
			closeChart(plotTopAndBottom);

		if (deltaCheckbox.isSelected() && isThereAnyDataToDisplay(exp, EnumXLSExport.TOPLEVELDELTA)) {
			// Use saved global position if available, otherwise use initial position
			Rectangle savedPos = globalChartDeltaBounds;
//			Rectangle pos = (savedPos != null) ? savedPos : rectv;
//			plotDelta = plotToChart(exp, "top delta t -(t-1)", EnumXLSExport.TOPLEVELDELTA, plotDelta, pos);
			plotMeasuresToChart(exp, EnumXLSExport.TOPLEVELDELTA, plotDelta);
			if (savedPos == null) {
				rectv.translate(dx, dy);
			}
			plotDelta.toFront();
		} else if (plotDelta != null)
			closeChart(plotDelta);

		if (derivativeCheckbox.isSelected() && isThereAnyDataToDisplay(exp, EnumXLSExport.DERIVEDVALUES)) {
			// Use saved global position if available, otherwise use initial position
			Rectangle savedPos = globalChartDerivativeBounds;
//			Rectangle pos = (savedPos != null) ? savedPos : rectv;
			// plotDerivative = plotToChart(exp, "Derivative", EnumXLSExport.DERIVEDVALUES,
			// plotDerivative, pos);
			plotMeasuresToChart(exp, EnumXLSExport.DERIVEDVALUES, plotDerivative);
			if (savedPos == null) {
				rectv.translate(dx, dy);
			}
			plotDerivative.toFront();
		} else if (plotDerivative != null)
			closeChart(plotDerivative);

		if (consumptionCheckbox.isSelected() && isThereAnyDataToDisplay(exp, EnumXLSExport.SUMGULPS)) {
			// Use saved global position if available, otherwise use initial position
			Rectangle savedPos = globalChartSumGulpsBounds;
//			Rectangle pos = (savedPos != null) ? savedPos : rectv;
			// plotSumgulps = plotToChart(exp, "Cumulated gulps", EnumXLSExport.SUMGULPS,
			// plotSumgulps, pos);
			plotMeasuresToChart(exp, EnumXLSExport.SUMGULPS, plotSumgulps);
			if (savedPos == null) {
				rectv.translate(dx, dy);
			}
			plotSumgulps.toFront();
		} else if (plotSumgulps != null)
			closeChart(plotSumgulps);
	}

	private ChartCageArrayFrame plotMeasuresToChart(Experiment exp, EnumXLSExport exportType,
			ChartCageArrayFrame iChart) {
		if (iChart != null)
			iChart.getMainChartFrame().dispose();

		int first = 0;
		int last = exp.getCages().cagesList.size() - 1;
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

		XLSExportOptions options = XLSExportOptionsBuilder.forChart().withBuildExcelStepMs(60000)
//				.withRelativeToT0(relativeToCheckbox.isSelected())
				.withExportType(exportType).withCageRange(first, last).build();

		iChart = new ChartCageArrayFrame();
		iChart.createMainChartPanel("Spots measures", exp, options, parent0);
		iChart.setChartSpotUpperLeftLocation(getInitialUpperLeftPosition(exp));
		iChart.displayData(exp, options);
		iChart.getMainChartFrame().toFront();
		iChart.getMainChartFrame().requestFocus();
		return iChart;
	}

//	private ChartLevels plotToChart(Experiment exp, String title, EnumXLSExport option, ChartLevels iChart,
//			Rectangle rectv) {
//		if (iChart != null)
//			iChart.mainChartFrame.dispose();
//		iChart = new ChartLevels();
//		iChart.createChartPanel(parent0, title, rectv);
//		iChart.displayData(exp, option, title, correctEvaporationCheckbox.isSelected());
//		iChart.mainChartFrame.toFront();
//		iChart.mainChartFrame.requestFocus();
//		return iChart;
//	}

	public void closeAllCharts() {
		closeChart(plotTopAndBottom);
		closeChart(plotDerivative);
		closeChart(plotSumgulps);
		closeChart(plotDelta);
	}

	private void closeChart(ChartCageArrayFrame chart) {
		if (chart != null)
			chart.mainChartFrame.dispose();
		chart = null;
	}

	private boolean isThereAnyDataToDisplay(Experiment exp, EnumXLSExport option) {
		boolean flag = false;
		Capillaries capillaries = exp.getCapillaries();
		for (Capillary cap : capillaries.getCapillariesList()) {
			flag = cap.isThereAnyMeasuresDone(option);
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
		if (plotTopAndBottom != null && plotTopAndBottom.mainChartFrame != null) {
			globalChartTopBottomBounds = plotTopAndBottom.mainChartFrame.getBounds();
		}
		if (plotDelta != null && plotDelta.mainChartFrame != null) {
			globalChartDeltaBounds = plotDelta.mainChartFrame.getBounds();
		}
		if (plotDerivative != null && plotDerivative.mainChartFrame != null) {
			globalChartDerivativeBounds = plotDerivative.mainChartFrame.getBounds();
		}
		if (plotSumgulps != null && plotSumgulps.mainChartFrame != null) {
			globalChartSumGulpsBounds = plotSumgulps.mainChartFrame.getBounds();
		}
	}
}
