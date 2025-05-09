package plugins.fmp.multicafe.dlg.browse;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import icy.gui.viewer.Viewer;
import icy.sequence.Sequence;
import icy.sequence.SequenceEvent;
import icy.sequence.SequenceListener;
import icy.sequence.SequenceEvent.SequenceEventSourceType;
import plugins.fmp.multicafe.MultiCAFE;
import plugins.fmp.multicafe.experiment.Experiment;
import plugins.fmp.multicafe.experiment.ExperimentDirectories;
import plugins.fmp.multicafe.tools.JComponents.SequenceNameListRenderer;
import icy.gui.frame.progress.ProgressFrame;

public class LoadSaveExperiment extends JPanel implements PropertyChangeListener, ItemListener, SequenceListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = -690874563607080412L;

	private JButton createButton = new JButton("Create...");
	private JButton openButton = new JButton("Open...");
	private JButton searchButton = new JButton("Search...");
	private JButton closeButton = new JButton("Close");
	public JCheckBox filteredCheck = new JCheckBox("List filtered");

	public List<String> selectedNames = new ArrayList<String>();
	private SelectFiles1 dialogSelect = null;

	private JButton previousButton = new JButton("<");
	private JButton nextButton = new JButton(">");

	private MultiCAFE parent0 = null;

	public JPanel initPanel(MultiCAFE parent0) {
		this.parent0 = parent0;

		SequenceNameListRenderer renderer = new SequenceNameListRenderer();
		parent0.expListCombo.setRenderer(renderer);
		int bWidth = 30;
		int height = 20;
		previousButton.setPreferredSize(new Dimension(bWidth, height));
		nextButton.setPreferredSize(new Dimension(bWidth, height));

		JPanel sequencePanel0 = new JPanel(new BorderLayout());
		sequencePanel0.add(previousButton, BorderLayout.LINE_START);
		sequencePanel0.add(parent0.expListCombo, BorderLayout.CENTER);
		sequencePanel0.add(nextButton, BorderLayout.LINE_END);

		JPanel sequencePanel = new JPanel(new BorderLayout());
		FlowLayout layout = new FlowLayout(FlowLayout.LEFT);
		layout.setVgap(1);
		JPanel subPanel = new JPanel(layout);
		subPanel.add(openButton);
		subPanel.add(createButton);
		subPanel.add(searchButton);
		subPanel.add(closeButton);
		subPanel.add(filteredCheck);
		sequencePanel.add(subPanel, BorderLayout.LINE_START);

		defineActionListeners();
		parent0.expListCombo.addItemListener(this);

		JPanel twoLinesPanel = new JPanel(new GridLayout(2, 1));
		twoLinesPanel.add(sequencePanel0);
		twoLinesPanel.add(sequencePanel);

		return twoLinesPanel;
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if (evt.getPropertyName().equals("SELECT1_CLOSED")) {
			parent0.paneExperiment.tabInfos.disableChangeFile = true;
			if (selectedNames.size() < 1)
				return;

			ExperimentDirectories experimentDirectories = new ExperimentDirectories();
			if (experimentDirectories.getDirectoriesFromExptPath(parent0.expListCombo, selectedNames.get(0), null)) {
				final int item = addExperimentFrom3NamesAnd2Lists(experimentDirectories);
				final String binSubDirectory = parent0.expListCombo.expListBinSubDirectory;

				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						parent0.paneExperiment.tabInfos.disableChangeFile = false;
						for (int i = 1; i < selectedNames.size(); i++) {
							ExperimentDirectories expDirectories = new ExperimentDirectories();
							if (expDirectories.getDirectoriesFromExptPath(parent0.expListCombo, selectedNames.get(i),
									binSubDirectory))
								addExperimentFrom3NamesAnd2Lists(expDirectories);
						}
						selectedNames.clear();
						updateBrowseInterface();
						parent0.paneExperiment.tabInfos.disableChangeFile = true;
						parent0.paneExperiment.tabInfos.initInfosCombos();
						parent0.expListCombo.setSelectedIndex(item);
						Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
						if (exp != null)
							parent0.paneExperiment.tabInfos.transferPreviousExperimentInfosToDialog(exp, exp);
					}
				});
			}
		}
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getStateChange() == ItemEvent.SELECTED) {
			final Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
			if (exp != null)
				openSelecteExperiment(exp);
		} else if (e.getStateChange() == ItemEvent.DESELECTED) {
			Experiment exp = (Experiment) e.getItem();
			closeViewsForCurrentExperiment(exp);
		}
	}

	void closeAllExperiments() {
		closeCurrentExperiment();
		parent0.expListCombo.removeAllItems();
		parent0.paneExperiment.tabFilter.clearAllCheckBoxes();
		parent0.paneExperiment.tabFilter.filterExpList.removeAllItems();
		parent0.paneExperiment.tabInfos.clearCombos();
		filteredCheck.setSelected(false);
	}

	public void closeViewsForCurrentExperiment(Experiment exp) {
		if (exp != null) {
			if (exp.seqCamData != null) {
				exp.xmlSave_MCExperiment();
				exp.saveCapillariesMeasures(exp.getKymosBinFullDirectory());
			}
			exp.closeSequences();
		}
	}

	public void closeCurrentExperiment() {
		if (parent0.expListCombo.getSelectedIndex() < 0)
			return;
		Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
		if (exp != null)
			closeViewsForCurrentExperiment(exp);
	}

	void updateBrowseInterface() {
		int isel = parent0.expListCombo.getSelectedIndex();
		boolean flag1 = (isel == 0 ? false : true);
		boolean flag2 = (isel == (parent0.expListCombo.getItemCount() - 1) ? false : true);
		previousButton.setEnabled(flag1);
		nextButton.setEnabled(flag2);
	}

	boolean openSelecteExperiment(Experiment exp) {
		ProgressFrame progressFrame = new ProgressFrame("Load Data");
		exp.xmlLoad_MCExperiment();

		boolean flag = true;
		progressFrame.setMessage("Load image");

		exp.loadCamDataImages();
		parent0.paneExperiment.updateViewerForSequenceCam(exp);

		exp.seqCamData.seq.addListener(this);
		if (exp.seqCamData != null) {
			exp.loadCamDataCapillaries();

			parent0.paneKymos.tabLoadSave.loadDefaultKymos(exp);

			if (exp.seqKymos != null) {
				parent0.paneLevels.tabFileLevels.dlg_levels_loadCapillaries_Measures(exp);
				if (parent0.paneExperiment.tabOptions.graphsCheckBox.isSelected())
					parent0.paneLevels.tabGraphs.displayGraphsPanels(exp);
			}

			exp.loadCageMeasures();
			exp.updateROIsAt(0);
			progressFrame.setMessage("Load data: update dialogs");

			parent0.paneExperiment.updateDialogs(exp);
			parent0.paneKymos.updateDialogs(exp);
			parent0.paneCapillaries.updateDialogs(exp);
		} else {
			flag = false;
			System.out.println(
					"LoadSaveExperiments:openSelectedExperiment() Error: no jpg files found for this experiment\n");
		}
		parent0.paneExperiment.tabInfos.transferPreviousExperimentInfosToDialog(exp, exp);
		progressFrame.close();

		return flag;
	}

	// ------------------------

	private void defineActionListeners() {
		parent0.expListCombo.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				updateBrowseInterface();
			}
		});

		nextButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				parent0.expListCombo.setSelectedIndex(parent0.expListCombo.getSelectedIndex() + 1);
				updateBrowseInterface();
			}
		});

		previousButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				parent0.expListCombo.setSelectedIndex(parent0.expListCombo.getSelectedIndex() - 1);
				updateBrowseInterface();
			}
		});

		searchButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				selectedNames = new ArrayList<String>();
				dialogSelect = new SelectFiles1();
				dialogSelect.initialize(parent0);
			}
		});

		createButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				ExperimentDirectories eDAF = new ExperimentDirectories();
				if (eDAF.getDirectoriesFromDialog(parent0.expListCombo, null, true)) {
					int item = addExperimentFrom3NamesAnd2Lists(eDAF);
					parent0.paneExperiment.tabInfos.initInfosCombos();
					parent0.expListCombo.setSelectedIndex(item);
				}
			}
		});

		openButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				ExperimentDirectories eDAF = new ExperimentDirectories();
				if (eDAF.getDirectoriesFromDialog(parent0.expListCombo, null, false)) {
					int item = addExperimentFrom3NamesAnd2Lists(eDAF);
					parent0.paneExperiment.tabInfos.initInfosCombos();
					parent0.expListCombo.setSelectedIndex(item);
				}
			}
		});

		closeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				closeAllExperiments();
				parent0.paneExperiment.tabsPane.setSelectedIndex(0);
				parent0.expListCombo.removeAllItems();
				parent0.expListCombo.updateUI();
			}
		});

		filteredCheck.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				parent0.paneExperiment.tabFilter.filterExperimentList(filteredCheck.isSelected());
			}
		});
	}

	private int addExperimentFrom3NamesAnd2Lists(ExperimentDirectories eDAF) {
		Experiment exp = new Experiment(eDAF);
		int item = parent0.expListCombo.addExperiment(exp, false);
		return item;
	}

	@Override
	public void sequenceChanged(SequenceEvent sequenceEvent) {
		if (sequenceEvent.getSourceType() == SequenceEventSourceType.SEQUENCE_DATA) {
			Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
			if (exp != null) {
				if (exp.seqCamData.seq != null && sequenceEvent.getSequence() == exp.seqCamData.seq) {
					Viewer v = exp.seqCamData.seq.getFirstViewer();
					int t = v.getPositionT();
					v.setTitle(exp.seqCamData.getDecoratedImageName(t));
				}
			}
		}
	}

	@Override
	public void sequenceClosed(Sequence sequence) {
		sequence.removeListener(this);
	}

}
