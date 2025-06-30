package plugins.fmp.multicafe;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import icy.gui.frame.IcyFrame;
import icy.gui.util.GuiUtil;
import icy.main.Icy;
import icy.plugin.PluginLauncher;
import icy.plugin.PluginLoader;
import icy.plugin.abstract_.PluginActionable;
import icy.preferences.GeneralPreferences;
import plugins.fmp.multicafe.dlg.browse.MCBrowse_;
import plugins.fmp.multicafe.dlg.capillaries.MCCapillaries_;
import plugins.fmp.multicafe.dlg.cells.MCCells_;
import plugins.fmp.multicafe.dlg.excel.MCExcel_;
import plugins.fmp.multicafe.dlg.experiment.MCExperiment_;
import plugins.fmp.multicafe.dlg.kymos.MCKymos_;
import plugins.fmp.multicafe.dlg.levels.MCLevels_;
import plugins.fmp.multicafe.tools.JComponents.ExperimentsJComboBox;
import plugins.fmp.multicafe.workinprogress_gpu.MCSpots_;

public class MultiCAFE extends PluginActionable {
	public IcyFrame mainFrame = new IcyFrame("MultiCAFE June 30, 2025", true, true, true, true);
	public ExperimentsJComboBox expListCombo = new ExperimentsJComboBox();

	public MCBrowse_ paneBrowse = new MCBrowse_();
	public MCExperiment_ paneExperiment = new MCExperiment_();
	public MCCapillaries_ paneCapillaries = new MCCapillaries_();
	public MCKymos_ paneKymos = new MCKymos_();
	public MCLevels_ paneLevels = new MCLevels_();
	public MCSpots_ paneSpots = new MCSpots_();
	public MCCells_ paneCells = new MCCells_();
	public MCExcel_ paneExcel = new MCExcel_();

	public JTabbedPane tabsPane = new JTabbedPane();

	// -------------------------------------------------------------------

	@Override
	public void run() {
		JPanel mainPanel = GuiUtil.generatePanelWithoutBorder();
		paneBrowse.init(mainPanel, "Browse", this);
		paneExperiment.init(mainPanel, "Experiment", this);
		paneCapillaries.init(mainPanel, "Capillaries", this);
		paneKymos.init(mainPanel, "Kymographs", this);
		paneLevels.init(mainPanel, "Levels", this);
		paneCells.init(mainPanel, "Cells", this);
		paneExcel.init(mainPanel, "Export", this);

		mainFrame.setLayout(new BorderLayout());
		mainFrame.add(mainPanel, BorderLayout.WEST);
		mainFrame.pack();
		mainFrame.center();
		mainFrame.setVisible(true);
		mainFrame.addToDesktopPane();
	}

	public static void main(String[] args) {
		Icy.main(args);
		GeneralPreferences.setSequencePersistence(false);
		PluginLauncher.start(PluginLoader.getPlugin(MultiCAFE.class.getName()));
	}

}
