package plugins.fmp.multicafe.fmp_experiment;

import java.io.File;
import java.util.List;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import icy.util.XMLUtil;
import plugins.fmp.multicafe.fmp_experiment.sequence.ImageLoader;
import plugins.fmp.multicafe.fmp_tools.toExcel.enums.EnumXLSColumnHeader;

/**
 * Shared LazyExperiment implementation that can be used across different
 * components to provide memory-efficient experiment loading.
 * 
 * <p>
 * This class implements the lazy loading pattern for Experiment objects,
 * allowing components to store lightweight experiment references and only load
 * full data when needed. This dramatically reduces memory usage when handling
 * large numbers of experiments.
 * </p>
 * 
 * <p>
 * <strong>Performance Optimization:</strong> This class now caches experiment
 * properties to avoid repeated XML file reads when retrieving field values for
 * combo boxes.
 * </p>
 * 
 * @author MultiSPOTS96
 * @version 2.0.0
 */
public class LazyExperiment extends Experiment {

	private static final Logger LOGGER = Logger.getLogger(LazyExperiment.class.getName());

	private final ExperimentMetadata metadata;
	private boolean isLoaded = false;
	private boolean experimentPropertiesLoaded = false;
	private ExperimentProperties cachedExperimentProperties = null;

	// XML file constants for properties loading
	private final static String ID_MCEXPERIMENT = "MCexperiment";
	private final static String ID_MS96_experiment_XML = "MCexperiment.xml"; // "MS96_experiment.xml";
//	private final static String ID_MS96_cages_XML = "MS96_cages.xml";

	public LazyExperiment(ExperimentMetadata metadata) {
		this.metadata = metadata;
		this.setResultsDirectory(metadata.getResultsDirectory());
	}

	@Override
	public String toString() {
		return metadata.getCameraDirectory();
	}

	public void loadIfNeeded() {
		if (!isLoaded) {
			try {
				// Load cached properties first if available (for lightweight access)
				loadPropertiesIfNeeded();
				
				ExperimentDirectories expDirectories = new ExperimentDirectories();
				if (expDirectories.getDirectoriesFromExptPath(metadata.getBinDirectory(),
						metadata.getCameraDirectory())) {
					
					// Set up directories using public methods
					setResultsDirectory(expDirectories.getResultsDirectory());
					setImagesDirectory(expDirectories.getCameraImagesDirectory());
					
					// Load XML metadata only (no images, no cages)
					// xmlLoad_MCExperiment() loads from resultsDirectory + MCexperiment.xml
					if (!xmlLoad_MCExperiment()) {
						LOGGER.warning("Failed to load experiment XML for " + metadata.getCameraDirectory());
						return;
					}
					
					// Set up ImageLoader with directory and file names only (NO sequence loading)
					ImageLoader imgLoader = getSeqCamData().getImageLoader();
					imgLoader.setImagesDirectory(expDirectories.getCameraImagesDirectory());
					List<String> imagesList = ExperimentDirectories.getImagesListFromPathV2(
							imgLoader.getImagesDirectory(), "jpg");
					// Use setImagesList instead of loadImageList to avoid loading the sequence
					getSeqCamData().setImagesList(imagesList);
					
					// Calculate file intervals if needed (lightweight operation)
					if (expDirectories.cameraImagesList.size() > 1) {
						getFileIntervalsFromSeqCamData();
					}
					
					// Initialize cages array (empty, will be loaded when needed)
					// Don't load cages here - they will be loaded when experiment is opened
					
					// Copy cached properties to parent's prop object
					if (cachedExperimentProperties != null) {
						getProperties().copyFieldsFrom(cachedExperimentProperties);
					}
					
					this.isLoaded = true;
				}
			} catch (Exception e) {
				LOGGER.warning("Error loading experiment " + metadata.getCameraDirectory() + ": " + e.getMessage());
				e.printStackTrace();
			}
		}
	}

	public boolean loadPropertiesIfNeeded() {
		if (!experimentPropertiesLoaded) {
			try {
				String resultsDir = metadata.getResultsDirectory();
				if (resultsDir == null) {
					resultsDir = metadata.getCameraDirectory() + File.separator + "results";
				}

				String xmlFileName = resultsDir + File.separator + ID_MS96_experiment_XML;
				File xmlFile = new File(xmlFileName);

				if (!xmlFile.exists()) {
					LOGGER.warning("XML file not found: " + xmlFileName);
					return false;
				}

				Document doc = XMLUtil.loadDocument(xmlFileName);
				if (doc == null) {
					LOGGER.warning("Could not load XML document from " + xmlFileName);
					return false;
				}

				Node node = XMLUtil.getElement(XMLUtil.getRootElement(doc), ID_MCEXPERIMENT);
				if (node == null) {
					LOGGER.warning("Could not find MCexperiment node in XML");
					return false;
				}

				cachedExperimentProperties = new ExperimentProperties();
				cachedExperimentProperties.loadXML_Properties(node);
				experimentPropertiesLoaded = true;

				return true;
			} catch (Exception e) {
				LOGGER.warning("Error loading properties for experiment " + metadata.getCameraDirectory() + ": "
						+ e.getMessage());
				return false;
			}
		}
		return true;
	}

	public String getFieldValue(EnumXLSColumnHeader field) {
		if (loadPropertiesIfNeeded() && cachedExperimentProperties != null) {
			return cachedExperimentProperties.getField(field);
		}
		return "..";
	}

	public boolean isLoaded() {
		return isLoaded;
	}

	public boolean isPropertiesLoaded() {
		return experimentPropertiesLoaded;
	}

	public ExperimentMetadata getMetadata() {
		return metadata;
	}

	public ExperimentProperties getCachedProperties() {
		loadPropertiesIfNeeded();
		return cachedExperimentProperties;
	}

	@Override
	public ExperimentProperties getProperties() {
		// Always return the parent's prop object (not cached properties)
		// The parent's prop should be loaded via load_MS96_experiment() or loadIfNeeded()
		// Don't sync cached properties here as they might be stale or from a different experiment
		return super.getProperties();
	}

	/**
	 * Lightweight metadata class for experiment information. Contains only
	 * essential information needed for the dropdown and lazy loading.
	 */
	public static class ExperimentMetadata {
		private final String cameraDirectory;
		private final String resultsDirectory;
		private final String binDirectory;

		public ExperimentMetadata(String cameraDirectory, String resultsDirectory, String binDirectory) {
			this.cameraDirectory = cameraDirectory;
			this.resultsDirectory = resultsDirectory;
			this.binDirectory = binDirectory;
		}

		public String getCameraDirectory() {
			return cameraDirectory;
		}

		public String getResultsDirectory() {
			return resultsDirectory;
		}

		public String getBinDirectory() {
			return binDirectory;
		}

		@Override
		public String toString() {
			return cameraDirectory; // Used for dropdown display
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null || getClass() != obj.getClass())
				return false;
			ExperimentMetadata that = (ExperimentMetadata) obj;
			return cameraDirectory.equals(that.cameraDirectory) && resultsDirectory.equals(that.resultsDirectory)
					&& binDirectory.equals(that.binDirectory);
		}

		@Override
		public int hashCode() {
			int result = cameraDirectory != null ? cameraDirectory.hashCode() : 0;
			result = 31 * result + (resultsDirectory != null ? resultsDirectory.hashCode() : 0);
			result = 31 * result + (binDirectory != null ? binDirectory.hashCode() : 0);
			return result;
		}
	}
}