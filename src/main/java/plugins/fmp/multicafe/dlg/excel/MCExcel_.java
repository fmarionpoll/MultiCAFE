package plugins.fmp.multicafe.dlg.excel;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import icy.gui.component.PopupPanel;
import icy.system.thread.ThreadUtil;
import plugins.fmp.multicafe.MultiCAFE;
import plugins.fmp.multicafe.fmp_experiment.Experiment;
import plugins.fmp.multicafe.fmp_tools.JComponents.Dialog;
import plugins.fmp.multicafe.fmp_tools.JComponents.exceptions.FileDialogException;
import plugins.fmp.multicafe.fmp_tools.toExcel.capillaries.XLSExportMeasuresFromCapillary;
import plugins.fmp.multicafe.fmp_tools.toExcel.config.XLSExportOptions;
import plugins.fmp.multicafe.fmp_tools.toExcel.exceptions.ExcelExportException;
import plugins.fmp.multicafe.fmp_tools.toExcel.gulps.XLSExportMeasuresFromGulp;
import plugins.fmp.multicafe.fmp_tools.toExcel.move.XLSExportMeasuresFromFlyPosition;

public class MCExcel_ extends JPanel implements PropertyChangeListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = -4296207607692017074L;
	PopupPanel capPopupPanel = null;
	private JTabbedPane tabsPane = new JTabbedPane();
	public Options tabCommonOptions = new Options();
	private Levels tabLevels = new Levels();
	private Gulps tabGulps = new Gulps();
	private Move tabMove = new Move();
	private MultiCAFE parent0 = null;

	public void init(JPanel mainPanel, String string, MultiCAFE parent0) {
		this.parent0 = parent0;

		capPopupPanel = new PopupPanel(string);
		JPanel capPanel = capPopupPanel.getMainPanel();
		capPanel.setLayout(new BorderLayout());
		capPopupPanel.collapse();
		mainPanel.add(capPopupPanel);
		GridLayout capLayout = new GridLayout(3, 2);

		tabCommonOptions.init(capLayout);
		tabsPane.addTab("Common options", null, tabCommonOptions, "Define common options");
		tabCommonOptions.addPropertyChangeListener(this);

		tabLevels.init(capLayout);
		tabsPane.addTab("Levels", null, tabLevels, "Export capillary levels to file");
		tabLevels.addPropertyChangeListener(this);

		tabGulps.init(capLayout);
		tabsPane.addTab("Gulps", null, tabGulps, "Export gulps to file");
		tabGulps.addPropertyChangeListener(this);

		tabMove.init(capLayout);
		tabsPane.addTab("Move", null, tabMove, "Export fly positions to file");
		tabMove.addPropertyChangeListener(this);

		capPanel.add(tabsPane);
		tabsPane.setSelectedIndex(0);

		capPopupPanel.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				parent0.mainFrame.revalidate();
				parent0.mainFrame.pack();
				parent0.mainFrame.repaint();
			}
		});
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		Experiment exp = (Experiment) parent0.expListComboLazy.getSelectedItem();
		if (exp == null)
			return;

		if (evt.getPropertyName().equals("EXPORT_MOVEDATA")) {
			String file = defineXlsFileName(exp, "_move.xlsx");
			if (file == null)
				return;
			updateParametersCurrentExperiment(exp);
			ThreadUtil.bgRun(new Runnable() {
				@Override
				public void run() {
					XLSExportMeasuresFromFlyPosition xlsExport = new XLSExportMeasuresFromFlyPosition();
					try {
						xlsExport.exportToFile(file, getMoveOptions(exp));
					} catch (ExcelExportException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			});
		} else if (evt.getPropertyName().equals("EXPORT_KYMOSDATA")) {
			String file = defineXlsFileName(exp, "_feeding.xlsx");
			if (file == null)
				return;
			updateParametersCurrentExperiment(exp);
			ThreadUtil.bgRun(new Runnable() {
				@Override
				public void run() {
					XLSExportMeasuresFromCapillary xlsExport2 = new XLSExportMeasuresFromCapillary();
					try {
						xlsExport2.exportToFile(file, getLevelsOptions(exp));
					} catch (ExcelExportException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			});
		} else if (evt.getPropertyName().equals("EXPORT_GULPSDATA")) {
			String file = defineXlsFileName(exp, "_gulps.xlsx");
			if (file == null)
				return;
			updateParametersCurrentExperiment(exp);
			ThreadUtil.bgRun(new Runnable() {
				@Override
				public void run() {
					XLSExportMeasuresFromGulp xlsExport2 = new XLSExportMeasuresFromGulp();
					try {
						xlsExport2.exportToFile(file, getGulpsOptions(exp));
					} catch (ExcelExportException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			});
		}
	}

	private String defineXlsFileName(Experiment exp, String pattern) {
		String filename0 = exp.getSeqCamData().getFileNameFromImageList(0);
		Path directory = Paths.get(filename0).getParent();
		Path subpath = directory.getName(directory.getNameCount() - 1);
		String tentativeName = subpath.toString() + pattern;

		try {
			return Dialog.saveFileAs(tentativeName, directory.getParent().toString(), "xlsx");
		} catch (FileDialogException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return tentativeName;
	}

	private void updateParametersCurrentExperiment(Experiment exp) {
		parent0.paneCapillaries.getDialogCapillariesInfos(exp);
		parent0.paneExperiment.tabInfos.getExperimentInfosFromDialog(exp);
	}

	private XLSExportOptions getMoveOptions(Experiment exp) {
		XLSExportOptions options = new XLSExportOptions();
		options.xyImage = tabMove.xyCenterCheckBox.isSelected();
		options.xyCage = tabMove.xyCageCheckBox.isSelected();
		options.xyCapillaries = tabMove.xyTipCapsCheckBox.isSelected();
		options.distance = tabMove.distanceCheckBox.isSelected();
		options.alive = tabMove.aliveCheckBox.isSelected();
		options.onlyalive = tabMove.deadEmptyCheckBox.isSelected();
		options.sleep = tabMove.sleepCheckBox.isSelected();
		options.ellipseAxes = tabMove.rectSizeCheckBox.isSelected();
		getCommonOptions(options, exp);
		return options;
	}

	private XLSExportOptions getLevelsOptions(Experiment exp) {
		XLSExportOptions options = new XLSExportOptions();
		options.sumGulps = false;
		options.nbGulps = false;

		options.topLevel = tabLevels.topLevelCheckBox.isSelected();
		options.topLevelDelta = tabLevels.topLevelDeltaCheckBox.isSelected();
		options.bottomLevel = tabLevels.bottomLevelCheckBox.isSelected();
		options.sumGulps = false;
		options.lrPI = tabLevels.lrPICheckBox.isSelected();
		options.lrPIThreshold = (double) tabLevels.lrPIThresholdJSpinner.getValue();
		options.sumPerCage = tabLevels.sumPerCageCheckBox.isSelected();
		// options.t0 = tabLevels.t0CheckBox.isSelected();
		options.correctEvaporation = tabLevels.subtractEvaporationCheckBox.isSelected();
		getCommonOptions(options, exp);
		return options;
	}

	private XLSExportOptions getGulpsOptions(Experiment exp) {
		XLSExportOptions options = new XLSExportOptions();
		options.topLevel = false;
		options.topLevelDelta = false;
		options.bottomLevel = false;
		options.derivative = tabGulps.derivativeCheckBox.isSelected();
		options.sumPerCage = false;
//		options.t0 = false;
		options.sumGulps = tabGulps.sumGulpsCheckBox.isSelected();
		options.lrPI = tabGulps.sumCheckBox.isSelected();
		options.nbGulps = tabGulps.nbGulpsCheckBox.isSelected();
		options.amplitudeGulps = tabGulps.amplitudeGulpsCheckBox.isSelected();

		options.markovChain = tabGulps.markovChainCheckBox.isSelected();
		options.autocorrelation = tabGulps.autocorrelationCheckBox.isSelected();
		options.crosscorrelation = tabGulps.crosscorrelationCheckBox.isSelected();
		options.nBinsCorrelation = (int) tabGulps.nbinsJSpinner.getValue();

		options.correctEvaporation = false;
		getCommonOptions(options, exp);
		return options;
	}

	private void getCommonOptions(XLSExportOptions options, Experiment exp) {
		options.transpose = tabCommonOptions.transposeCheckBox.isSelected();
		options.buildExcelStepMs = tabCommonOptions.getExcelBuildStep();
		options.buildExcelUnitMs = tabCommonOptions.binUnit.getMsUnitValue();
		options.fixedIntervals = tabCommonOptions.isFixedFrameButton.isSelected();
		options.startAll_Ms = tabCommonOptions.getStartAllMs();
		options.endAll_Ms = tabCommonOptions.getEndAllMs();

		options.collateSeries = tabCommonOptions.collateSeriesCheckBox.isSelected();
		options.padIntervals = tabCommonOptions.padIntervalsCheckBox.isSelected();
		options.absoluteTime = false; // tabCommonOptions.absoluteTimeCheckBox.isSelected();
		options.onlyalive = tabCommonOptions.onlyAliveCheckBox.isSelected();
		options.exportAllFiles = tabCommonOptions.exportAllFilesCheckBox.isSelected();

		options.expList = parent0.expListComboLazy;
		options.expList.expListBinSubDirectory = exp.getBinSubDirectory();
		if (tabCommonOptions.exportAllFilesCheckBox.isSelected()) {
			options.firstExp = 0;
			int itemCount = options.expList.getItemCount();
			options.lastExp = (itemCount > 0) ? itemCount - 1 : 0;
		} else {
			int selectedIndex = parent0.expListComboLazy.getSelectedIndex();
			options.firstExp = (selectedIndex >= 0) ? selectedIndex : 0;
			options.lastExp = options.firstExp;
		}
		options.experimentIndexFirst = options.firstExp;
		options.experimentIndexLast = options.lastExp;
	}
}
