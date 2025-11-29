package plugins.fmp.multicafe.dlg.levels;

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import icy.gui.util.GuiUtil;
import icy.gui.viewer.Viewer;
import icy.sequence.Sequence;
import icy.sequence.SequenceEvent;
import icy.sequence.SequenceListener;
import plugins.fmp.multicafe.MultiCAFE;
import plugins.fmp.multicafe.fmp_experiment.Experiment;
import plugins.fmp.multicafe.fmp_experiment.capillaries.Capillaries;
import plugins.fmp.multicafe.fmp_experiment.capillaries.Capillary;
import plugins.fmp.multicafe.tools0.toExcel.EnumXLSExport;
import plugins.fmp.multicafe.tools1.chart.ChartLevels;

public class LevelsChart extends JPanel implements SequenceListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = -7079184380174992501L;
	private ChartLevels plotTopAndBottom = null;
	private ChartLevels plotDelta = null;
	private ChartLevels plotDerivative = null;
	private ChartLevels plotSumgulps = null;

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
		add(panel1);

		add(GuiUtil.besidesPanel(displayResultsButton, new JLabel(" ")));
		defineActionListeners();
	}

	private void defineActionListeners() {
		displayResultsButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListComboLazy.getSelectedItem();
				if (exp != null) {
					exp.getSeqKymos().validateRois();
					exp.getSeqKymos().transferKymosRoisToCapillaries_Measures(exp.getCapillaries());
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
	}

	private Rectangle getInitialUpperLeftPosition(Experiment exp) {
		Rectangle rectv = new Rectangle(50, 500, 10, 10);
		Viewer v = exp.getSeqCamData().getSequence().getFirstViewer();
		if (v != null) {
			rectv = v.getBounds();
			// rectv.translate(0, rectv.height);
		} else {
			rectv = parent0.mainFrame.getBounds();
//			rectv.translate(rectv.width, rectv.height + 100);
			rectv.translate(0, 150);
		}
		return rectv;
	}

	public void displayGraphsPanels(Experiment exp) {
		exp.getSeqKymos().getSequence().addListener(this);
		Rectangle rectv = getInitialUpperLeftPosition(exp);
		int dx = 5;
		int dy = 10;

		if (limitsCheckbox.isSelected() && isThereAnyDataToDisplay(exp, EnumXLSExport.TOPLEVEL)
				&& isThereAnyDataToDisplay(exp, EnumXLSExport.BOTTOMLEVEL)) {
			// Use saved global position if available, otherwise use initial position
			Rectangle savedPos = globalChartTopBottomBounds;
			Rectangle pos = (savedPos != null) ? savedPos : rectv;
			plotTopAndBottom = plotToChart(exp, "top + bottom levels", EnumXLSExport.TOPLEVEL, plotTopAndBottom, pos);
			if (savedPos == null) {
				rectv.translate(dx, dy);
			}
			plotTopAndBottom.toFront();
		} else if (plotTopAndBottom != null)
			closeChart(plotTopAndBottom);

		if (deltaCheckbox.isSelected() && isThereAnyDataToDisplay(exp, EnumXLSExport.TOPLEVELDELTA)) {
			// Use saved global position if available, otherwise use initial position
			Rectangle savedPos = globalChartDeltaBounds;
			Rectangle pos = (savedPos != null) ? savedPos : rectv;
			plotDelta = plotToChart(exp, "top delta t -(t-1)", EnumXLSExport.TOPLEVELDELTA, plotDelta, pos);
			if (savedPos == null) {
				rectv.translate(dx, dy);
			}
			plotDelta.toFront();
		} else if (plotDelta != null)
			closeChart(plotDelta);

		if (derivativeCheckbox.isSelected() && isThereAnyDataToDisplay(exp, EnumXLSExport.DERIVEDVALUES)) {
			// Use saved global position if available, otherwise use initial position
			Rectangle savedPos = globalChartDerivativeBounds;
			Rectangle pos = (savedPos != null) ? savedPos : rectv;
			plotDerivative = plotToChart(exp, "Derivative", EnumXLSExport.DERIVEDVALUES, plotDerivative, pos);
			if (savedPos == null) {
				rectv.translate(dx, dy);
			}
			plotDerivative.toFront();
		} else if (plotDerivative != null)
			closeChart(plotDerivative);

		if (consumptionCheckbox.isSelected() && isThereAnyDataToDisplay(exp, EnumXLSExport.SUMGULPS)) {
			// Use saved global position if available, otherwise use initial position
			Rectangle savedPos = globalChartSumGulpsBounds;
			Rectangle pos = (savedPos != null) ? savedPos : rectv;
			plotSumgulps = plotToChart(exp, "Cumulated gulps", EnumXLSExport.SUMGULPS, plotSumgulps, pos);
			if (savedPos == null) {
				rectv.translate(dx, dy);
			}
			plotSumgulps.toFront();
		} else if (plotSumgulps != null)
			closeChart(plotSumgulps);
	}

	private ChartLevels plotToChart(Experiment exp, String title, EnumXLSExport option, ChartLevels iChart,
			Rectangle rectv) {
		if (iChart != null)
			iChart.mainChartFrame.dispose();
		iChart = new ChartLevels();
		iChart.createChartPanel(parent0, title, rectv);
		iChart.displayData(exp, option, title, correctEvaporationCheckbox.isSelected());
		iChart.mainChartFrame.toFront();
		iChart.mainChartFrame.requestFocus();
		return iChart;
	}

	public void closeAllCharts() {
		plotTopAndBottom = closeChart(plotTopAndBottom);
		plotDerivative = closeChart(plotDerivative);
		plotSumgulps = closeChart(plotSumgulps);
		plotDelta = closeChart(plotDelta);
	}

	private ChartLevels closeChart(ChartLevels chart) {
		if (chart != null)
			chart.mainChartFrame.dispose();
		chart = null;
		return chart;
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
