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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import icy.gui.frame.progress.ProgressFrame;
import icy.gui.viewer.Viewer;
import icy.sequence.Sequence;
import icy.sequence.SequenceEvent;
import icy.sequence.SequenceEvent.SequenceEventSourceType;
import icy.sequence.SequenceListener;
import plugins.fmp.multicafe.MultiCAFE;
import plugins.fmp.multicafe.fmp_experiment.Experiment;
import plugins.fmp.multicafe.fmp_experiment.ExperimentDirectories;
import plugins.fmp.multicafe.fmp_experiment.LazyExperiment;
import plugins.fmp.multicafe.fmp_experiment.LazyExperiment.ExperimentMetadata;
import plugins.fmp.multicafe.fmp_tools.DescriptorsIO;
import plugins.fmp.multicafe.fmp_tools.Directories;
import plugins.fmp.multicafe.fmp_tools.JComponents.SequenceNameListRenderer;

public class LoadSaveExperiment extends JPanel implements PropertyChangeListener, ItemListener, SequenceListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = -690874563607080412L;
	private static final Logger LOGGER = Logger.getLogger(LoadSaveExperiment.class.getName());

	// Performance constants for metadata-only processing
	private static final int METADATA_BATCH_SIZE = 20; // Process 20 experiments at a time
	private static final int PROGRESS_UPDATE_INTERVAL = 10; // Update progress every 10 experiments

	// UI Components
	private JButton openButton = new JButton("Open...");
	private JButton createButton = new JButton("Create...");
	private JButton searchButton = new JButton("Search...");
	private JButton closeButton = new JButton("Close");
	public JCheckBox filteredCheck = new JCheckBox("List filtered");

	// Data structures
	public List<String> selectedNames = new ArrayList<String>();
	private SelectFiles1 dialogSelect = null;

	// Navigation buttons
	private JButton previousButton = new JButton("<");
	private JButton nextButton = new JButton(">");

	// Metadata storage - lightweight experiment information
	private List<ExperimentMetadata> experimentMetadataList = new ArrayList<>();
	private volatile boolean isProcessing = false;
	private final AtomicInteger processingCount = new AtomicInteger(0);

	// Track currently loading experiment to prevent concurrent loads
	private volatile Experiment currentlyLoadingExperiment = null;
	private volatile int currentlyLoadingIndex = -1;

	// Track active async workers for cancellation
	private volatile SwingWorker<Boolean, Void> activeLoadWorker = null;
	private volatile CompletableFuture<Boolean> activeSaveFuture = null;
	// Track the load ID to ensure only the most recent load applies its data
	private volatile long currentLoadId = 0;

	// Parent reference
	private MultiCAFE parent0 = null;

	// -----------------------------------------
	public LoadSaveExperiment() {
	}

	public JPanel initPanel(MultiCAFE parent0) {
		this.parent0 = parent0;
		setLayout(new BorderLayout());
		setPreferredSize(new Dimension(400, 200));

		JPanel group2Panel = initUI();
		defineActionListeners();
		parent0.expListComboLazy.addItemListener(this);

		return group2Panel;
	}

	private JPanel initUI() {
		JPanel group2Panel = new JPanel(new GridLayout(2, 1));
		JPanel navPanel = initNavigationPanel();
		JPanel buttonPanel = initButtonPanel();
		group2Panel.add(navPanel);
		group2Panel.add(buttonPanel);
		return group2Panel;
	}

	private JPanel initNavigationPanel() {
		JPanel navPanel = new JPanel(new BorderLayout());
		SequenceNameListRenderer renderer = new SequenceNameListRenderer();
		parent0.expListComboLazy.setRenderer(renderer);
		int bWidth = 30;
		int height = 20;
		previousButton.setPreferredSize(new Dimension(bWidth, height));
		nextButton.setPreferredSize(new Dimension(bWidth, height));

		navPanel.add(previousButton, BorderLayout.LINE_START);
		navPanel.add(parent0.expListComboLazy, BorderLayout.CENTER);
		navPanel.add(nextButton, BorderLayout.LINE_END);
		return navPanel;
	}

	private JPanel initButtonPanel() {
		JPanel buttonPanel = new JPanel(new BorderLayout());
		FlowLayout layout = new FlowLayout(FlowLayout.LEFT);
		layout.setVgap(1);
		JPanel subPanel = new JPanel(layout);
		subPanel.add(openButton);
		subPanel.add(createButton);
		subPanel.add(searchButton);
		subPanel.add(closeButton);
		subPanel.add(filteredCheck);
		buttonPanel.add(subPanel, BorderLayout.LINE_START);
		return buttonPanel;
	}

	private void defineActionListeners() {
		openButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				handleOpenButton();
			}
		});

		searchButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				handleSearchButton();
			}
		});

		closeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				handleCloseButton();
			}
		});

		previousButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				handlePreviousButton();
			}
		});

		nextButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				handleNextButton();
			}
		});

		parent0.expListComboLazy.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				updateBrowseInterface();
			}
		});

		filteredCheck.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				parent0.paneExperiment.tabFilter.filterExperimentList(filteredCheck.isSelected());
			}
		});

		createButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				ExperimentDirectories eDAF = new ExperimentDirectories();
				if (eDAF.getDirectoriesFromDialog(parent0.expListComboLazy, null, true)) {
					int item = addExperimentFrom3NamesAnd2Lists(eDAF);
					parent0.paneExperiment.tabInfos.initCombos();
					parent0.expListComboLazy.setSelectedIndex(item);
				}
			}
		});

	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if (evt.getPropertyName().equals("SELECT1_CLOSED")) {
			if (selectedNames.size() < 1) {
				return;
			}

			if (isProcessing) {
				LOGGER.warning("File processing already in progress, ignoring new request");
				return;
			}

			processSelectedFilesMetadataOnly();
		}
	}

	private void processSelectedFilesMetadataOnly() {
		isProcessing = true;
		processingCount.set(0);
		experimentMetadataList.clear();
		long startTime = System.nanoTime();

		ProgressFrame progressFrame = new ProgressFrame("Processing Experiment Metadata");
		progressFrame.setMessage("Scanning " + selectedNames.size() + " experiment directories...");

		SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
			@Override
			protected Void doInBackground() throws Exception {
				processMetadataOnly(progressFrame);
				return null;
			}

			@Override
			protected void done() {
				isProcessing = false;
				progressFrame.close();
				long endTime = System.nanoTime();
				System.out.println(
						"LoadExperiment: processSelectedFilesMetadataOnly took " + (endTime - startTime) / 1e6 + " ms");
				SwingUtilities.invokeLater(() -> {
					updateBrowseInterface();
				});
			}
		};

		worker.execute();
	}

	private void processMetadataOnly(ProgressFrame progressFrame) {
		final String subDir = parent0.expListComboLazy.expListBinSubDirectory;
		final int totalFiles = selectedNames.size();

		try {
			// Process files in batches for metadata only
			for (int i = 0; i < totalFiles; i += METADATA_BATCH_SIZE) {
				int endIndex = Math.min(i + METADATA_BATCH_SIZE, totalFiles);

				// Update progress
				final int currentBatch = i;
				final int currentEndIndex = endIndex;
				SwingUtilities.invokeLater(() -> {
					progressFrame.setMessage(String.format("Scanning experiments %d-%d of %d", currentBatch + 1,
							currentEndIndex, totalFiles));
					progressFrame.setPosition((double) currentBatch / totalFiles);
				});

				// Process batch for metadata only
				for (int j = i; j < endIndex; j++) {
					final String fileName = selectedNames.get(j);
					final int fileIndex = j;
					processSingleFileMetadataOnly(fileName, subDir, fileIndex);
					processingCount.incrementAndGet();

					// Update progress periodically
					if (j % PROGRESS_UPDATE_INTERVAL == 0) {
						final int currentProgress = j;
						SwingUtilities.invokeLater(() -> {
							progressFrame.setMessage(String.format("Found %d experiments...", currentProgress + 1));
						});
					}

					// Minimal delay to prevent UI freezing
					try {
						Thread.sleep(1); // Very small delay
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						break;
					}
				}
			}

			// Add metadata to UI
			SwingUtilities.invokeLater(() -> {
				addMetadataToUI();
			});

			// Clear selected names after processing
			selectedNames.clear();

		} catch (Exception e) {
			LOGGER.severe("Error processing experiment metadata: " + e.getMessage());
			SwingUtilities.invokeLater(() -> {
				progressFrame.setMessage("Error: " + e.getMessage());
			});
		}
	}

	private void processSingleFileMetadataOnly(String fileName, String subDir, int fileIndex) {
		try {
			// Create lightweight ExperimentDirectories for metadata scanning only
			ExperimentDirectories expDirectories = new ExperimentDirectories();

			// Only check if the experiment directory exists and is valid
			if (expDirectories.getDirectoriesFromExptPath(subDir, fileName)) {
				String camDataImagesDirectory = expDirectories.getCameraImagesDirectory();
				String resultsDirectory = expDirectories.getResultsDirectory();
				ExperimentMetadata metadata = new ExperimentMetadata(camDataImagesDirectory, resultsDirectory, subDir);
				experimentMetadataList.add(metadata);
			}

		} catch (Exception e) {
			LOGGER.warning("Failed to process metadata for file [" + fileIndex + "] " + fileName + ": " + e.getMessage());
		}
	}

	private void addMetadataToUI() {
		try {
			List<LazyExperiment> lazyExperiments = new ArrayList<>();
			for (ExperimentMetadata metadata : experimentMetadataList) {
				LazyExperiment lazyExp = new LazyExperiment(metadata);
				lazyExperiments.add(lazyExp);
			}

			parent0.expListComboLazy.addLazyExperimentsBulk(lazyExperiments);
			parent0.paneExperiment.tabInfos.initCombos();

			// Kick off background descriptor preloading for fast filters/infos
			parent0.descriptorIndex.preloadFromCombo(parent0.expListComboLazy, new Runnable() {
				@Override
				public void run() {
					// Once preloaded, refresh Infos and Filter combos if tabs are visited
					parent0.paneExperiment.tabInfos.initCombos();
					parent0.paneExperiment.tabFilter.initCombos();
				}
			});

			// Also generate descriptors files in background for any experiment missing it
			new SwingWorker<Void, Void>() {
				@Override
				protected Void doInBackground() throws Exception {
					for (int i = 0; i < parent0.expListComboLazy.getItemCount(); i++) {
						Experiment exp = parent0.expListComboLazy.getItemAtNoLoad(i);
						String path = DescriptorsIO.getDescriptorsFullName(exp.getResultsDirectory());
						java.io.File f = new java.io.File(path);
						if (!f.exists()) {
							DescriptorsIO.buildFromExperiment(exp);
						}
					}
					return null;
				}
			}.execute();

		} catch (Exception e) {
			LOGGER.warning("Error adding metadata to UI: " + e.getMessage());
		}
	}

	boolean openSelecteExperiment(Experiment exp) {
		final long startTime = System.nanoTime();
		int expIndex = parent0.expListComboLazy.getSelectedIndex();
		
		// Check if this experiment is still the selected one
		if (parent0.expListComboLazy.getSelectedItem() != exp) {
			LOGGER.info("Skipping load for experiment [" + expIndex + "] - no longer selected");
			return false;
		}
		
		ProgressFrame progressFrame = new ProgressFrame("Load Experiment Data");

		// Don't start loading if saving is in progress (wait for save to complete)
		if (exp.isSaving()) {
			LOGGER.warning("Cannot load experiment [" + expIndex + "] - save operation in progress: " + exp.toString());
			progressFrame.close();
			return false;
		}

		// Set loading flag to prevent saving while loading
		exp.setLoading(true);
		
		// Track this as the currently loading experiment
		currentlyLoadingExperiment = exp;
		currentlyLoadingIndex = expIndex;

		try {
			// Check if still loading the correct experiment
			if (parent0.expListComboLazy.getSelectedItem() != exp) {
				LOGGER.info("Aborting load for experiment [" + expIndex + "] - different experiment selected");
				exp.setLoading(false);
				currentlyLoadingExperiment = null;
				currentlyLoadingIndex = -1;
				progressFrame.close();
				return false;
			}
			
			// If it's a LazyExperiment, load the data first
			if (exp instanceof LazyExperiment) {
				progressFrame.setMessage("Loading experiment data...");
				((LazyExperiment) exp).loadIfNeeded();
			}

			// Check again after lazy load
			if (parent0.expListComboLazy.getSelectedItem() != exp) {
				LOGGER.info("Aborting load for experiment [" + expIndex + "] - different experiment selected after lazy load");
				exp.setLoading(false);
				currentlyLoadingExperiment = null;
				currentlyLoadingIndex = -1;
				progressFrame.close();
				return false;
			}

			exp.xmlLoad_MCExperiment();

			boolean flag = true;
			progressFrame.setMessage("Load image");

			// Step 1: Load seqCamData images
			exp.getSeqCamData().loadImages();
			
			// Check if still loading the correct experiment before updating viewer
			if (parent0.expListComboLazy.getSelectedItem() != exp) {
				LOGGER.info("Aborting load for experiment [" + expIndex + "] - different experiment selected after loading images");
				exp.setLoading(false);
				if (currentlyLoadingExperiment == exp) {
					currentlyLoadingExperiment = null;
					currentlyLoadingIndex = -1;
				}
				progressFrame.close();
				return false;
			}
			
			parent0.paneExperiment.updateViewerForSequenceCam(exp);

			// Check if still loading the correct experiment
			if (parent0.expListComboLazy.getSelectedItem() != exp) {
				LOGGER.info("Aborting load for experiment [" + expIndex + "] - different experiment selected during load");
				exp.setLoading(false);
				if (currentlyLoadingExperiment == exp) {
					currentlyLoadingExperiment = null;
					currentlyLoadingIndex = -1;
				}
				progressFrame.close();
				return false;
			}

			if (exp.getSeqCamData() == null) {
				flag = false;
				LOGGER.severe(
						"LoadSaveExperiments:openSelectedExperiment() [" + expIndex + "] Error: no jpg files found for this experiment\n");
				progressFrame.close();
				// Clear loading flag - loading failed early
				exp.setLoading(false);
				if (currentlyLoadingExperiment == exp) {
					currentlyLoadingExperiment = null;
					currentlyLoadingIndex = -1;
				}
				return flag;
			}

			if (exp.getSeqCamData().getSequence() != null)
				exp.getSeqCamData().getSequence().addListener(this);

			// Step 1 (continued): Check if MCcapillaries.xml exists and load capillaries +
			// display on seqCamData
			progressFrame.setMessage("Load capillaries");
			if (exp.loadCamDataCapillaries()) {
				// Capillaries loaded and displayed on seqCamData images
			}

			// Step 2: Identify and select bin directory (bin_60, bin_xx)
			progressFrame.setMessage("Select bin directory");
			String selectedBinDir = selectBinDirectory(exp);
			if (selectedBinDir != null) {
				exp.setBinSubDirectory(selectedBinDir);
				parent0.expListComboLazy.expListBinSubDirectory = selectedBinDir;
			}

			// Step 3: Load kymographs from selected bin directory and display in another
			// window
			progressFrame.setMessage("Load kymographs");
			boolean kymosLoaded = false;
			if (selectedBinDir != null) {
				kymosLoaded = parent0.paneKymos.tabLoadSave.loadDefaultKymos(exp);
				if (kymosLoaded && exp.getSeqKymos() != null) {
					parent0.paneKymos.tabDisplay.displayUpdateOnSwingThread();
				}
			}

			// Step 4: Load CapillaryMeasures.csv from bin directory and display measures
			progressFrame.setMessage("Load capillary measures");
			if (selectedBinDir != null && exp.getBinSubDirectory() != null) {
				// Ensure we're loading from the correct bin directory
				String binFullDir = exp.getKymosBinFullDirectory();
				if (binFullDir != null) {
					exp.loadCapillaries();
					if (exp.getSeqKymos() != null && exp.getSeqKymos().getSequence() != null) {
						exp.getSeqKymos().transferCapillariesMeasuresToKymos(exp.getCapillaries());
					}
				}
			}

			// Step 5: If kymographs are present, transfer measures to kymographs
			if (kymosLoaded && exp.getSeqKymos() != null && exp.getSeqKymos().getSequence() != null) {
				exp.getSeqKymos().transferCapillariesMeasuresToKymos(exp.getCapillaries());
			}

			if (parent0.paneExperiment.tabOptions.graphsCheckBox.isSelected())
				parent0.paneLevels.tabGraphs.displayChartPanels(exp); // displayGraphsPanels(exp);

			// Check if still loading the correct experiment before starting async load
			if (parent0.expListComboLazy.getSelectedItem() != exp) {
				LOGGER.info("Aborting load for experiment [" + expIndex + "] - different experiment selected before cage load");
				exp.setLoading(false);
				if (currentlyLoadingExperiment == exp) {
					currentlyLoadingExperiment = null;
					currentlyLoadingIndex = -1;
				}
				progressFrame.close();
				return false;
			}

			// Prepare for async load - move CSV file if needed
			String pathToMeasures = exp.getResultsDirectory() + java.io.File.separator + "CagesMeasures.csv";
			java.io.File f = new java.io.File(pathToMeasures);
			if (!f.exists()) {
				// Try to move from bin directory (synchronous but quick)
				String pathToOldCsv = exp.getKymosBinFullDirectory() + java.io.File.separator + "CagesMeasures.csv";
				java.io.File fileToMove = new java.io.File(pathToOldCsv);
				if (fileToMove.exists()) {
					fileToMove.renameTo(f);
				}
			}
			
			// Load cage measures asynchronously
			progressFrame.setMessage("Load cage measures...");
			final Experiment finalExp = exp;
			final int finalExpIndex = expIndex;
			final ProgressFrame finalProgressFrame = progressFrame;
			
			// Cancel any previous load worker
			if (activeLoadWorker != null && !activeLoadWorker.isDone()) {
				activeLoadWorker.cancel(true);
			}
			
			// Generate a new load ID for this load operation
			final long thisLoadId = System.nanoTime();
			currentLoadId = thisLoadId;
			
			activeLoadWorker = exp.getCages().getPersistence().loadCagesAsync(
				exp.getCages(),
				exp.getResultsDirectory(),
				exp,
				new Runnable() {
					@Override
					public void run() {
						// This runs on EDT after async load completes
						// Check if this is still the most recent load (prevent stale loads from applying data)
						if (currentLoadId != thisLoadId) {
							LOGGER.info("Skipping UI update for experiment [" + finalExpIndex + "] - newer load in progress");
							return;
						}
						
						// Check if this experiment is still the selected one
						if (parent0.expListComboLazy.getSelectedItem() != finalExp) {
							LOGGER.info("Skipping UI update for experiment [" + finalExpIndex + "] - no longer selected");
							return;
						}
						
						// Check if sequence is still valid
						if (finalExp.getSeqCamData() != null && finalExp.getSeqCamData().getSequence() != null) {
							// Log cage counts before cagesFromROIs
							int cagesWithFlyPosBefore = 0;
							for (plugins.fmp.multicafe.fmp_experiment.cages.Cage cage : finalExp.getCages().getCageList()) {
								if (cage.flyPositions != null && cage.flyPositions.flyPositionList != null 
										&& !cage.flyPositions.flyPositionList.isEmpty()) {
									cagesWithFlyPosBefore++;
								}
							}
							
							finalExp.getCages().cagesToROIs(finalExp.getSeqCamData());
							finalExp.getCages().cagesFromROIs(finalExp.getSeqCamData());
							
							// Log cage counts after cagesFromROIs to detect if fly positions are lost
							int cagesWithFlyPosAfter = 0;
							for (plugins.fmp.multicafe.fmp_experiment.cages.Cage cage : finalExp.getCages().getCageList()) {
								if (cage.flyPositions != null && cage.flyPositions.flyPositionList != null 
										&& !cage.flyPositions.flyPositionList.isEmpty()) {
									cagesWithFlyPosAfter++;
								}
							}
							
							if (cagesWithFlyPosBefore > cagesWithFlyPosAfter) {
								LOGGER.warning("LoadExperiment [" + finalExpIndex + "] Lost fly positions: before=" + cagesWithFlyPosBefore + ", after=" + cagesWithFlyPosAfter);
							}
						}
						
						finalExp.updateROIsAt(0);
						finalProgressFrame.setMessage("Load data: update dialogs");

						parent0.paneExperiment.updateDialogs(finalExp);
						parent0.paneKymos.updateDialogs(finalExp);
						parent0.paneCapillaries.updateDialogs(finalExp);

						parent0.paneExperiment.tabInfos.transferPreviousExperimentInfosToDialog(finalExp, finalExp);
						finalProgressFrame.close();

						// Log completion timing and cage counts
						long callbackEndTime = System.nanoTime();
						int cageCount = finalExp.getCages().getCageList().size();
						int cagesWithFlyPositions = 0;
						int totalFlyPositions = 0;
						for (plugins.fmp.multicafe.fmp_experiment.cages.Cage cage : finalExp.getCages().getCageList()) {
							if (cage.flyPositions != null && cage.flyPositions.flyPositionList != null 
									&& !cage.flyPositions.flyPositionList.isEmpty()) {
								cagesWithFlyPositions++;
								totalFlyPositions += cage.flyPositions.flyPositionList.size();
							}
						}
						System.out.println("LoadExperiment: openSelecteExperiment [" + finalExpIndex + "] async load completed, total time: " + (callbackEndTime - startTime) / 1e6 + " ms, cages: " + cageCount + ", with fly positions: " + cagesWithFlyPositions + ", total fly positions: " + totalFlyPositions);

						// Only clear flags if this is still the currently loading experiment
						if (currentlyLoadingExperiment == finalExp) {
							finalExp.setLoading(false);
							currentlyLoadingExperiment = null;
							currentlyLoadingIndex = -1;
							activeLoadWorker = null;
						} else {
							// This load completed but experiment changed - make sure loading flag is cleared
							finalExp.setLoading(false);
						}
					}
				});
			
			activeLoadWorker.execute();
			
			// Log timing for async load start
			long endTime = System.nanoTime();
			System.out.println("LoadExperiment: openSelecteExperiment [" + expIndex + "] started async load, took " + (endTime - startTime) / 1e6 + " ms");
			
			// Return true to indicate load started (even though it's async)
			// The actual completion will be handled in the callback
			return true;
		} catch (Exception e) {
			LOGGER.severe("Error opening experiment [" + expIndex + "]: "
					+ (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
			LOGGER.severe("Exception details [" + expIndex + "]: " + e.toString());
			e.printStackTrace();
			progressFrame.close();

			// Clear loading flag - loading failed but we're done trying
			exp.setLoading(false);
			if (currentlyLoadingExperiment == exp) {
				currentlyLoadingExperiment = null;
				currentlyLoadingIndex = -1;
			}

			long endTime = System.nanoTime();
			System.out.println(
					"LoadExperiment: openSelecteExperiment [" + expIndex + "] failed, took " + (endTime - startTime) / 1e6 + " ms");
			return false;
		}
	}

	/**
	 * Detects and selects bin directory (bin_60, bin_xx) according to rules: - If
	 * loading single experiment and multiple bins exist, ask user - If loading
	 * series and not first file, keep previously selected bin - If directory not
	 * found, ask user what to do
	 */
	private String selectBinDirectory(Experiment exp) {
		String resultsDir = exp.getResultsDirectory();
		if (resultsDir == null) {
			return null;
		}

		List<String> binDirs = Directories.getSortedListOfSubDirectoriesWithTIFF(resultsDir);
		if (binDirs == null || binDirs.isEmpty()) {
			// No bin directories found - ask user what to do
			int response = JOptionPane.showConfirmDialog(null,
					"No bin directories found in " + resultsDir + "\nDo you want to continue without kymographs?",
					"No Bin Directory Found", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
			if (response == JOptionPane.YES_OPTION) {
				return null;
			} else {
				// User can manually select a directory
				return selectBinDirectoryDialog(binDirs);
			}
		}

		// Check if we're loading a single experiment or a series
		boolean isFirstExperiment = (parent0.expListComboLazy.getSelectedIndex() == 0);
		boolean isSingleExperiment = (parent0.expListComboLazy.getItemCount() == 1);

		// If we have a previously selected bin directory and it exists, use it (for
		// series)
		String previousBinDir = parent0.expListComboLazy.expListBinSubDirectory;
		if (!isFirstExperiment && previousBinDir != null && binDirs.contains(previousBinDir)) {
			return previousBinDir;
		}

		// If single experiment or first in series
		if (isSingleExperiment || isFirstExperiment) {
			if (binDirs.size() > 1) {
				// Multiple bin directories - ask user which one
				return selectBinDirectoryDialog(binDirs);
			} else if (binDirs.size() == 1) {
				// Only one bin directory - use it
				return binDirs.get(0);
			}
		}

		// Default: use first available or ask
		if (binDirs.size() > 0) {
			return binDirs.get(0);
		}

		return null;
	}

	/**
	 * Shows a dialog to let user select a bin directory from the list
	 */
	private String selectBinDirectoryDialog(List<String> binDirs) {
		if (binDirs == null || binDirs.isEmpty()) {
			return null;
		}

		Object[] array = binDirs.toArray();
		JComboBox<Object> jcb = new JComboBox<Object>(array);
		jcb.setEditable(false);
		JOptionPane.showMessageDialog(null, jcb, "Select bin directory", JOptionPane.QUESTION_MESSAGE);
		Object selected = jcb.getSelectedItem();
		return (selected != null) ? selected.toString() : null;
	}

	private void handleOpenButton() {
		ExperimentDirectories eDAF = new ExperimentDirectories();
		final String binDirectory = parent0.expListComboLazy.expListBinSubDirectory;
		if (eDAF.getDirectoriesFromDialog(binDirectory, null, false)) {
			String camDataImagesDirectory = eDAF.getCameraImagesDirectory();
			String resultsDirectory = eDAF.getResultsDirectory();
			ExperimentMetadata metadata = new ExperimentMetadata(camDataImagesDirectory, resultsDirectory,
					binDirectory);

			LazyExperiment lazyExp = new LazyExperiment(metadata);
			int selectedIndex = parent0.expListComboLazy.addLazyExperiment(lazyExp);
			parent0.paneExperiment.tabInfos.initCombos();
			parent0.expListComboLazy.setSelectedIndex(selectedIndex);
		}
	}

	private void handleSearchButton() {
		selectedNames = new ArrayList<String>();
		dialogSelect = new SelectFiles1();
		dialogSelect.initialize(parent0, selectedNames);
	}

	private void handleCloseButton() {
		closeAllExperiments();
		parent0.expListComboLazy.removeAllItems();
		parent0.expListComboLazy.updateUI();
	}

	private void handlePreviousButton() {
		parent0.expListComboLazy.setSelectedIndex(parent0.expListComboLazy.getSelectedIndex() - 1);
		updateBrowseInterface();
	}

	private void handleNextButton() {
		parent0.expListComboLazy.setSelectedIndex(parent0.expListComboLazy.getSelectedIndex() + 1);
		updateBrowseInterface();
	}

	// ----------------------------

	@Override
	public void sequenceChanged(SequenceEvent sequenceEvent) {
		if (sequenceEvent.getSourceType() == SequenceEventSourceType.SEQUENCE_DATA) {
			Experiment exp = (Experiment) parent0.expListComboLazy.getSelectedItem();
			if (exp != null) {
				if (exp.getSeqCamData().getSequence() != null
						&& sequenceEvent.getSequence() == exp.getSeqCamData().getSequence()) {
					Viewer v = exp.getSeqCamData().getSequence().getFirstViewer();
					int t = v.getPositionT();
					v.setTitle(exp.getSeqCamData().getDecoratedImageName(t));
				}
			}
		}
	}

	@Override
	public void sequenceClosed(Sequence sequence) {
		sequence.removeListener(this);
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getStateChange() == ItemEvent.SELECTED) {
			final Experiment exp = (Experiment) parent0.expListComboLazy.getSelectedItem();
			if (exp != null) {
			// Cancel any ongoing load for a different experiment
			if (currentlyLoadingExperiment != null && currentlyLoadingExperiment != exp) {
				LOGGER.info("Cancelling load for experiment [" + currentlyLoadingIndex + "] - new experiment selected");
				// Cancel the active load worker
				if (activeLoadWorker != null && !activeLoadWorker.isDone()) {
					activeLoadWorker.cancel(true);
					activeLoadWorker = null;
				}
				// Clear loading flag for the previous experiment
				if (currentlyLoadingExperiment != null) {
					currentlyLoadingExperiment.setLoading(false);
				}
				currentlyLoadingExperiment = null;
				currentlyLoadingIndex = -1;
			}
			openSelecteExperiment(exp);
			}
		} else if (e.getStateChange() == ItemEvent.DESELECTED) {
			Experiment exp = (Experiment) e.getItem();
			if (exp != null)
				closeViewsForCurrentExperiment(exp);
			else
				System.out.println("experiment = null");
		}
	}

	void closeAllExperiments() {
		closeCurrentExperiment();
		parent0.expListComboLazy.removeAllItems();
		parent0.paneExperiment.tabFilter.clearAllCheckBoxes();
		parent0.paneExperiment.tabFilter.filterExpList.removeAllItems();
		parent0.paneExperiment.tabInfos.clearCombos();
		filteredCheck.setSelected(false);
	}

	public void closeViewsForCurrentExperiment(Experiment exp) {
		if (exp != null) {
			// Don't save if loading is still in progress (prevents race condition)
			if (exp.isLoading()) {
				LOGGER.warning("Skipping save for experiment - loading still in progress: " + exp.toString());
				return;
			}

			// Don't start a new save if one is already in progress (prevents concurrent saves)
			if (exp.isSaving()) {
				LOGGER.warning("Skipping save for experiment - save operation already in progress: " + exp.toString());
				return;
			}

			// Set saving flag to prevent concurrent saves and loads
			exp.setSaving(true);

			try {
				if (exp.getSeqCamData() != null) {
					exp.xmlSave_MCExperiment();
					exp.saveCapillariesMeasures(exp.getKymosBinFullDirectory());
					
					// Save cage measures asynchronously
					exp.getCages().cagesFromROIs(exp.getSeqCamData());
					
					// Cancel any previous save future
					if (activeSaveFuture != null && !activeSaveFuture.isDone()) {
						activeSaveFuture.cancel(true);
					}
					
					// Start async save
					final Experiment finalExp = exp;
					activeSaveFuture = exp.getCages().getPersistence().saveCagesAsync(
						exp.getCages(),
						exp.getResultsDirectory(),
						exp
					);
					
					// Handle save completion
					activeSaveFuture.whenComplete((result, throwable) -> {
						if (throwable != null) {
							LOGGER.severe("Error saving cage measures asynchronously: " + throwable.getMessage());
						}
						// Save MS96_descriptors.xml (synchronous, but quick)
						if (finalExp.getSeqCamData() != null) {
							DescriptorsIO.buildFromExperiment(finalExp);
						}
						// Clear saving flag
						finalExp.setSaving(false);
						activeSaveFuture = null;
					});
					
					// Close sequences immediately (don't wait for async save)
					exp.closeSequences();
				} else {
					// No sequence data, just clear the flag
					exp.setSaving(false);
				}
			} catch (Exception e) {
				LOGGER.severe("Error in closeViewsForCurrentExperiment: " + e.getMessage());
				// Always clear saving flag, even if save fails
				exp.setSaving(false);
				activeSaveFuture = null;
			}
		}
	}

	public void closeCurrentExperiment() {
		if (parent0.expListComboLazy.getSelectedIndex() < 0)
			return;
		Experiment exp = (Experiment) parent0.expListComboLazy.getSelectedItem();
		if (exp != null)
			closeViewsForCurrentExperiment(exp);
	}

	void updateBrowseInterface() {
		int isel = parent0.expListComboLazy.getSelectedIndex();
		boolean flag1 = (isel == 0 ? false : true);
		boolean flag2 = (isel == (parent0.expListComboLazy.getItemCount() - 1) ? false : true);
		previousButton.setEnabled(flag1);
		nextButton.setEnabled(flag2);
	}

	/**
	 * Gets memory usage statistics for monitoring.
	 * 
	 * @return Memory usage information
	 */
	public String getMemoryUsageInfo() {
		Runtime runtime = Runtime.getRuntime();
		long totalMemory = runtime.totalMemory();
		long freeMemory = runtime.freeMemory();
		long usedMemory = totalMemory - freeMemory;

		return String.format("Memory: %dMB used, %dMB total, %d experiments loaded", usedMemory / 1024 / 1024,
				totalMemory / 1024 / 1024, experimentMetadataList.size());
	}

	// ------------------------

	private int addExperimentFrom3NamesAnd2Lists(ExperimentDirectories eDAF) {
		Experiment exp = new Experiment(eDAF);
		int item = parent0.expListComboLazy.addExperiment(exp);
		return item;
	}

}
