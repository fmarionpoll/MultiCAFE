package plugins.fmp.multicafe.fmp_experiment;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import icy.image.IcyBufferedImage;
import icy.image.ImageUtil;
import icy.roi.ROI2D;
import icy.sequence.Sequence;
import icy.util.XMLUtil;
import plugins.fmp.multicafe.fmp_experiment.cages.Cage;
import plugins.fmp.multicafe.fmp_experiment.cages.CagesArray;
import plugins.fmp.multicafe.fmp_experiment.cages.CagesSequenceMapper;
import plugins.fmp.multicafe.fmp_experiment.capillaries.Capillaries;
import plugins.fmp.multicafe.fmp_experiment.capillaries.CapillariesDescription;
import plugins.fmp.multicafe.fmp_experiment.capillaries.Capillary;
import plugins.fmp.multicafe.fmp_experiment.capillaries.CapillariesKymosMapper;
import plugins.fmp.multicafe.fmp_experiment.ids.CapillaryID;
import plugins.fmp.multicafe.fmp_experiment.persistence.MigrationDetector;
import plugins.fmp.multicafe.fmp_experiment.persistence.MigrationTool;
import plugins.fmp.multicafe.fmp_experiment.sequence.ImageAdjustmentOptions;
import plugins.fmp.multicafe.fmp_experiment.sequence.ImageFileData;
import plugins.fmp.multicafe.fmp_experiment.sequence.ImageLoader;
import plugins.fmp.multicafe.fmp_experiment.sequence.ImageProcessingResult;
import plugins.fmp.multicafe.fmp_experiment.sequence.KymographInfo;
import plugins.fmp.multicafe.fmp_experiment.sequence.SequenceCamData;
import plugins.fmp.multicafe.fmp_experiment.sequence.SequenceKymos;
import plugins.fmp.multicafe.fmp_experiment.sequence.TimeManager;
import plugins.fmp.multicafe.fmp_experiment.spots.Spot;
import plugins.fmp.multicafe.fmp_experiment.spots.SpotsArray;
import plugins.fmp.multicafe.fmp_service.KymographService;
import plugins.fmp.multicafe.fmp_tools.Directories;
import plugins.fmp.multicafe.fmp_tools.Logger;
import plugins.fmp.multicafe.fmp_tools.ROI2D.ROI2DUtilities;
import plugins.fmp.multicafe.fmp_tools.results.EnumResults;
import plugins.fmp.multicafe.fmp_tools.results.Results;
import plugins.fmp.multicafe.fmp_tools.results.ResultsArray;
import plugins.fmp.multicafe.fmp_tools.results.ResultsArrayFromCapillaries;
import plugins.fmp.multicafe.fmp_tools.results.ResultsOptions;
import plugins.fmp.multicafe.fmp_tools.toExcel.enums.EnumXLSColumnHeader;

public class Experiment {
	public final static String RESULTS = "results";
	public final static String BIN = "bin_";

	private String camDataImagesDirectory = null;
	private String resultsDirectory = null;
	private String binDirectory = null;

	private SequenceCamData seqCamData = null;
	private SequenceKymos seqKymos = null;
	private Sequence seqReference = null;

	private CagesArray cages = new CagesArray();
	private Capillaries capillaries = new Capillaries();
	private SpotsArray spotsArray = new SpotsArray();

	private ExperimentTimeManager timeManager = new ExperimentTimeManager();
	private ExperimentProperties prop = new ExperimentProperties();

	public Experiment chainToPreviousExperiment = null;
	public Experiment chainToNextExperiment = null;
	public long chainImageFirst_ms = 0;
	public int experimentID = 0;
	public int col = -1;

	private String generatorProgram = null;
	private static String staticProgramContext = null;
	// Flags to prevent race conditions between loading and saving
	private volatile boolean isLoading = false;
	private volatile boolean isSaving = false;

	// -----------------------------------------

	public Sequence getSeqReference() {
		return seqReference;
	}

	public void setSeqReference(Sequence seqReference) {
		this.seqReference = seqReference;
	}

	public CagesArray getCages() {
		return cages;
	}

	public void setCages(CagesArray cages) {
		this.cages = cages;
	}

	// __________________________________________________

	public FileTime getFirstImage_FileTime() {
		return timeManager.getFirstImage_FileTime();
	}

	public void setFirstImage_FileTime(FileTime fileTime) {
		timeManager.setFirstImage_FileTime(fileTime);
	}

	public FileTime getLastImage_FileTime() {
		return timeManager.getLastImage_FileTime();
	}

	public void setLastImage_FileTime(FileTime fileTime) {
		timeManager.setLastImage_FileTime(fileTime);
	}

	public long getCamImageFirst_ms() {
		return timeManager.getCamImageFirst_ms();
	}

	public void setCamImageFirst_ms(long ms) {
		timeManager.setCamImageFirst_ms(ms);
	}

	public long getCamImageLast_ms() {
		return timeManager.getCamImageLast_ms();
	}

	public void setCamImageLast_ms(long ms) {
		timeManager.setCamImageLast_ms(ms);
	}

	public long getCamImageBin_ms() {
		return timeManager.getCamImageBin_ms();
	}

	public void setCamImageBin_ms(long ms) {
		timeManager.setCamImageBin_ms(ms);
	}

	public long[] getCamImages_ms() {
		return timeManager.getCamImages_ms();
	}

	public void setCamImages_ms(long[] ms) {
		timeManager.setCamImages_ms(ms);
	}

	public long getBinT0() {
		return timeManager.getBinT0();
	}

	public void setBinT0(long val) {
		timeManager.setBinT0(val);
	}

	public long getKymoFirst_ms() {
		return timeManager.getKymoFirst_ms();
	}

	public void setKymoFirst_ms(long ms) {
		timeManager.setKymoFirst_ms(ms);
	}

	public long getKymoLast_ms() {
		return timeManager.getKymoLast_ms();
	}

	public void setKymoLast_ms(long ms) {
		timeManager.setKymoLast_ms(ms);
	}

	public long getKymoBin_ms() {
		return timeManager.getKymoBin_ms();
	}

	public void setKymoBin_ms(long ms) {
		timeManager.setKymoBin_ms(ms);
	}

	// _________________________________________________

	private final static String ID_VERSION = "version";
	private final static String ID_VERSIONNUM = "1.0.0";
	private final static String ID_FRAMEFIRST = "indexFrameFirst";
	private final static String ID_NFRAMES = "nFrames";
	private final static String ID_FRAMEDELTA = "indexFrameDelta";

	private final static String ID_TIMEFIRSTIMAGEMS = "fileTimeImageFirstMs";
	private final static String ID_TIMELASTIMAGEMS = "fileTimeImageLastMs";
	private final static String ID_FIRSTKYMOCOLMS = "firstKymoColMs";
	private final static String ID_LASTKYMOCOLMS = "lastKymoColMs";
	private final static String ID_BINKYMOCOLMS = "binKymoColMs";

	private final static String ID_IMAGESDIRECTORY = "imagesDirectory";
	private final static String ID_MCEXPERIMENT = "MCexperiment";
	// New v2 format filename
	private final String ID_V2_EXPERIMENT_XML = "v2_Experiment.xml";
	// Legacy filenames (for fallback)
	private final String ID_MS96_experiment_XML = "MCexperiment.xml";
	private final String ID_MCEXPERIMENT_XML_LEGACY = "MS96_experiment.xml";
	private final static String ID_MCDROSOTRACK_XML = "MCdrosotrack.xml";
	private final static String ID_GENERATOR_PROGRAM = "generatorProgram";

	private final static int EXPT_DIRECTORY = 1;
	private final static int IMG_DIRECTORY = 2;
	private final static int BIN_DIRECTORY = 3;
	// ----------------------------------

	public Experiment() {
		seqCamData = SequenceCamData.builder().withStatus(EnumStatus.FILESTACK).build();
	}

	public Experiment(String expDirectory) {
		seqCamData = SequenceCamData.builder().withStatus(EnumStatus.FILESTACK).build();
		this.resultsDirectory = expDirectory;
	}

	public Experiment(SequenceCamData seqCamData) {
		this.seqCamData = seqCamData;
		resultsDirectory = this.seqCamData.getImagesDirectory() + File.separator + RESULTS;
		getFileIntervalsFromSeqCamData();
		load_MS96_experiment();
	}

	public Experiment(ExperimentDirectories eADF) {
		camDataImagesDirectory = eADF.getCameraImagesDirectory();
		resultsDirectory = eADF.getResultsDirectory();
		seqCamData = SequenceCamData.builder().withStatus(EnumStatus.FILESTACK).build();
		load_MS96_experiment();

		ImageLoader imgLoader = seqCamData.getImageLoader();
		imgLoader.setImagesDirectory(eADF.getCameraImagesDirectory());
		List<String> imagesList = ExperimentDirectories.getImagesListFromPathV2(imgLoader.getImagesDirectory(), "jpg");
		seqCamData.loadImageList(imagesList);
		if (eADF.cameraImagesList.size() > 1)
			getFileIntervalsFromSeqCamData();
	}

	// ----------------------------------

	public String getResultsDirectory() {
		return resultsDirectory;
	}

	public boolean isLoading() {
		return isLoading;
	}

	public void setLoading(boolean loading) {
		this.isLoading = loading;
	}

	public boolean isSaving() {
		return isSaving;
	}

	public void setSaving(boolean saving) {
		this.isSaving = saving;
	}

	public String toString() {
		return resultsDirectory;
	}

	public void setResultsDirectory(String fileName) {
		resultsDirectory = ExperimentDirectories.getParentIf(fileName, BIN);
	}

	public void setBinDirectory(String bin) {
		binDirectory = bin;
	}

	public String getBinDirectory() {
		return binDirectory;
	}

	public String getImagesDirectory() {
		return camDataImagesDirectory;
	}

	public void setImagesDirectory(String imagesDirectory) {
		this.camDataImagesDirectory = imagesDirectory;
	}

	// ------------------------------ Legacy Metadata Accessors

	public String getBoxID() {
		return prop.field_boxID;
	}

	public void setBoxID(String boxID) {
		prop.field_boxID = boxID;
	}

	public String getExperiment() {
		return prop.field_experiment;
	}

	public void setExperiment(String experiment) {
		prop.field_experiment = experiment;
	}

	public String getComment1() {
		return prop.field_comment1;
	}

	public void setComment1(String comment1) {
		prop.field_comment1 = comment1;
	}

	public String getComment2() {
		return prop.field_comment2;
	}

	public void setComment2(String comment2) {
		prop.field_comment2 = comment2;
	}

	public String getStrain() {
		return prop.field_strain;
	}

	public void setStrain(String strain) {
		prop.field_strain = strain;
	}

	public String getSex() {
		return prop.field_sex;
	}

	public void setSex(String sex) {
		prop.field_sex = sex;
	}

	public String getCondition1() {
		return prop.field_stim2;
	}

	public void setCondition1(String condition1) {
		prop.field_stim2 = condition1;
	}

	public String getCondition2() {
		return prop.field_conc2;
	}

	public void setCondition2(String condition2) {
		prop.field_conc2 = condition2;
	}

	public String getGeneratorProgram() {
		return generatorProgram;
	}

	public void setGeneratorProgram(String generatorProgram) {
		this.generatorProgram = generatorProgram;
	}

	public static void setProgramContext(String programName) {
		staticProgramContext = programName;
	}

	public static String getProgramContext() {
		return staticProgramContext;
	}

	private String getOrDetermineGeneratorProgram() {
		if (generatorProgram != null) {
			return generatorProgram;
		}
		String programName = determineProgramFromStackTrace();
		if (programName != null) {
			return programName;
		}
		if (staticProgramContext != null) {
			return staticProgramContext;
		}
		return null;
	}

	private String determineProgramFromStackTrace() {
		return determineProgramFromStackTraceStatic();
	}

	public static String determineProgramFromStackTraceStatic() {
		StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
		for (StackTraceElement element : stackTrace) {
			String className = element.getClassName();
			try {
				Class<?> clazz = Class.forName(className);
				if (isRootProgramClass(clazz)) {
					return convertClassNameToProgramName(clazz.getSimpleName());
				}
			} catch (ClassNotFoundException e) {
				continue;
			} catch (NoClassDefFoundError e) {
				continue;
			} catch (Exception e) {
				continue;
			}
		}
		return null;
	}

	private static boolean isRootProgramClass(Class<?> clazz) {
		String className = clazz.getName();
		String simpleName = clazz.getSimpleName();

		if (className == null || simpleName == null) {
			return false;
		}

		if (className.contains("Experiment") || className.contains("Persistence")
				|| className.contains("fmp_experiment") || className.contains("fmp_tools")) {
			return false;
		}

		try {
			Class<?> superClass = clazz.getSuperclass();
			if (superClass != null && superClass.getName().equals("icy.plugin.abstract_.PluginActionable")) {
				if (simpleName.startsWith("Multi")) {
					return true;
				}
			}
		} catch (Exception e) {
		}

		return false;
	}

	public static String convertClassNameToProgramName(String className) {
		if (className == null || className.isEmpty()) {
			return null;
		}
		char[] chars = className.toCharArray();
		if (chars.length == 0) {
			return className.toLowerCase();
		}
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < chars.length; i++) {
			char c = chars[i];
			if (i == 0) {
				result.append(Character.toLowerCase(c));
			} else if (Character.isUpperCase(c)) {
				result.append(Character.toLowerCase(c));
			} else {
				result.append(c);
			}
		}
		return result.toString();
	}

	public boolean createDirectoryIfDoesNotExist(String directory) {
		Path pathDir = Paths.get(directory);
		if (Files.notExists(pathDir)) {
			try {
				Files.createDirectory(pathDir);
			} catch (IOException e) {
				e.printStackTrace();
				System.out
						.println("Experiment:createDirectoryIfDoesNotExist() Creating directory failed: " + directory);
				return false;
			}
		}
		return true;
	}

	public void checkKymosDirectory(String kymosSubDirectory) {
		if (kymosSubDirectory == null) {
			List<String> listTIFFlocations = Directories.getSortedListOfSubDirectoriesWithTIFF(getResultsDirectory());
			if (listTIFFlocations.size() < 1)
				return;
			boolean found = false;
			for (String subDir : listTIFFlocations) {
				String test = subDir.toLowerCase();
				if (test.contains(Experiment.BIN)) {
					kymosSubDirectory = subDir;
					found = true;
					break;
				}
				if (test.contains(Experiment.RESULTS)) {
					found = true;
					break;
				}
			}
			if (!found) {
				int lowest = getBinStepFromDirectoryName(listTIFFlocations.get(0)) + 1;
				for (String subDir : listTIFFlocations) {
					int val = getBinStepFromDirectoryName(subDir);
					if (val < lowest) {
						lowest = val;
						kymosSubDirectory = subDir;
					}
				}
			}
		}
//		setBinSubDirectory(kymosSubDirectory);
	}

	public void setCameraImagesDirectory(String name) {
		camDataImagesDirectory = name;
	}

	public String getCameraImagesDirectory() {
		return camDataImagesDirectory;
	}

	public void closeSequences() {
		if (seqCamData != null)
			seqCamData.closeSequence();
		if (seqReference != null)
			seqReference.close();
		if (seqKymos != null)
			seqKymos.closeSequence();
	}

	public boolean zopenPositionsMeasures() {
		if (seqCamData == null) {
			// Use builder pattern for initialization
			seqCamData = SequenceCamData.builder().withStatus(EnumStatus.FILESTACK).build();
		}
		load_MS96_experiment();
		getFileIntervalsFromSeqCamData();

		return loadCagesMeasures();
	}

//	private boolean zxmlReadDrosoTrack(String filename) {
//		if (filename == null) {
//			filename = getXML_MS96_cages_Location(cages.ID_MS96_cages_XML);
//			if (filename == null)
//				return false;
//		}
//		return cages.xmlReadCagesFromFileNoQuestion(filename);
//	}

	private String getRootWithNoResultNorBinString(String directoryName) {
		String name = directoryName.toLowerCase();
		while (name.contains(RESULTS) || name.contains(BIN))
			name = Paths.get(resultsDirectory).getParent().toString();
		return name;
	}

	private SequenceCamData loadImagesForSequenceCamData(String filename) {
		camDataImagesDirectory = ExperimentDirectories.getImagesDirectoryAsParentFromFileName(filename);
		List<String> imagesList = ExperimentDirectories.getImagesListFromPathV2(camDataImagesDirectory, "jpg");
		seqCamData = null;
		if (imagesList.size() > 0) {
			// Use builder pattern with images directory and list
			seqCamData = SequenceCamData.builder().withImagesDirectory(camDataImagesDirectory)
					.withStatus(EnumStatus.FILESTACK).build();
			seqCamData.setImagesList(imagesList);
			seqCamData.attachSequence(seqCamData.getImageLoader().loadSequenceFromImagesList(imagesList));
		}
		return seqCamData;
	}

	public boolean loadCamDataSpots() {
		load_MS96_cages();
		if (seqCamData != null && seqCamData.getSequence() != null) {
			seqCamData.removeROIsContainingString("spot");
			cages.transferCageSpotsToSequenceAsROIs(seqCamData, spotsArray);
		}
		return (seqCamData != null && seqCamData.getSequence() != null);
	}

	public SequenceCamData openSequenceCamData() {
		loadImagesForSequenceCamData(camDataImagesDirectory);
		if (seqCamData != null) {
			load_MS96_experiment();
			getFileIntervalsFromSeqCamData();
		}
		return seqCamData;
	}

	public void getFileIntervalsFromSeqCamData() {
		timeManager.getFileIntervalsFromSeqCamData(seqCamData, camDataImagesDirectory);
	}

	public void loadFileIntervalsFromSeqCamData() {
		timeManager.loadFileIntervalsFromSeqCamData(seqCamData, camDataImagesDirectory);
	}

	public String getBinNameFromKymoFrameStep() {
		return timeManager.getBinNameFromKymoFrameStep();
	}

	public long[] build_MsTimeIntervalsArray_From_SeqCamData_FileNamesList(long firstImage_ms) {
		return timeManager.build_MsTimeIntervalsArray_From_SeqCamData_FileNamesList(seqCamData, firstImage_ms);
	}

	public void initTmsForFlyPositions(long time_start_ms) {
		timeManager.build_MsTimeIntervalsArray_From_SeqCamData_FileNamesList(seqCamData, time_start_ms);
		cages.initCagesTmsForFlyPositions(timeManager.getCamImages_ms());
	}

	public int findNearestIntervalWithBinarySearch(long value, int low, int high) {
		return timeManager.findNearestIntervalWithBinarySearch(value, low, high);
	}

	public int getSeqCamSizeT() {
		int lastFrame = 0;
		if (seqCamData != null)
			lastFrame = seqCamData.getImageLoader().getNTotalFrames() - 1;
		return lastFrame;
	}

	public String getDirectoryToSaveResults() {
		Path dir = Paths.get(resultsDirectory);
		if (binDirectory != null)
			dir = dir.resolve(binDirectory);
		String directory = dir.toAbsolutePath().toString();
		if (!createDirectoryIfDoesNotExist(directory))
			directory = null;
		return directory;
	}

	// -------------------------------

	/**
	 * Loads experiment-level metadata (description and timing) from the v2/legacy
	 * {@code *Experiment.xml} file.
	 * <p>
	 * This is the primary entry point for experiment description persistence.
	 */
	public boolean xmlLoad_MCExperiment() {
		return load_MS96_experiment();
	}

	public boolean xmlSave_MCExperiment() {
		return save_MS96_experiment();
	}

	public boolean load_MS96_experiment() {
		if (resultsDirectory == null && seqCamData != null) {
			camDataImagesDirectory = seqCamData.getImagesDirectory();
			resultsDirectory = camDataImagesDirectory + File.separator + RESULTS;
		}

		// Priority 1: Try new v2_ format
		String v2FileName = concatenateExptDirectoryWithSubpathAndName(null, ID_V2_EXPERIMENT_XML);
		if (load_MS96_experiment(v2FileName)) {
			return true;
		}

		// Priority 2: Try legacy MCexperiment.xml
		String legacyFileName = concatenateExptDirectoryWithSubpathAndName(null, ID_MS96_experiment_XML);
		if (load_MS96_experiment(legacyFileName)) {
			return true;
		}

		// Priority 3: Try legacy MS96_experiment.xml
		String legacy2FileName = concatenateExptDirectoryWithSubpathAndName(null, ID_MCEXPERIMENT_XML_LEGACY);
		return load_MS96_experiment(legacy2FileName);
	}

	private boolean load_MS96_experiment(String csFileName) {
		try {
			final Document doc = XMLUtil.loadDocument(csFileName);
			if (doc == null) {
				String resultsDir = getResultsDirectory();
				File resultsDirFile = new File(resultsDir);
				if (!resultsDirFile.exists() || !resultsDirFile.isDirectory()) {
					return false;
				}
				System.err.println("ERROR: Could not load XML document from " + csFileName);
				return false;
			}

			// Schema validation removed as requested

			Node node = XMLUtil.getElement(XMLUtil.getRootElement(doc), ID_MCEXPERIMENT);
			if (node == null) {
				System.err.println("ERROR: Could not find MCexperiment node in XML");
				return false;
			}

			// Version validation with detailed logging
			String version = XMLUtil.getElementValue(node, ID_VERSION, ID_VERSIONNUM);
			// System.out.println("XML Version: " + version);
			if (!version.equals(ID_VERSIONNUM)) {
				System.err.println("ERROR: Version mismatch. Expected: " + ID_VERSIONNUM + ", Found: " + version);
				return false;
			}

			// Load ImageLoader configuration with validation
			ImageLoader imgLoader = seqCamData.getImageLoader();
			long frameFirst = XMLUtil.getElementLongValue(node, ID_FRAMEFIRST, 0);
			if (frameFirst < 0) {
				// System.out.println("WARNING: frameFirst < 0, setting to 0");
				frameFirst = 0;
			}
			imgLoader.setAbsoluteIndexFirstImage(frameFirst);

			// nframes is optional - older XML files may not have it
			long nImages = XMLUtil.getElementLongValue(node, ID_NFRAMES, -1);
			if (nImages > 0) {
				// only set if present and valid in XML
				imgLoader.setFixedNumberOfImages(nImages);
				imgLoader.setNTotalFrames((int) (nImages - frameFirst));
			} else {
				// nFrames not in XML
				int loadedImagesCount = imgLoader.getImagesCount();
				if (loadedImagesCount > 0) {
					nImages = loadedImagesCount + frameFirst;
					imgLoader.setFixedNumberOfImages(nImages);
					imgLoader.setNTotalFrames((int) (nImages - frameFirst));
				}
			}

			// Load TimeManager configuration with validation
			TimeManager timeManager = seqCamData.getTimeManager();
			long firstMs = XMLUtil.getElementLongValue(node, ID_TIMEFIRSTIMAGEMS, 0);
			timeManager.setFirstImageMs(firstMs);
			long lastMs = XMLUtil.getElementLongValue(node, ID_TIMELASTIMAGEMS, 0);
			timeManager.setLastImageMs(lastMs);
			long durationMs = lastMs - firstMs;
			timeManager.setDurationMs(durationMs);
			long frameDelta = XMLUtil.getElementLongValue(node, ID_FRAMEDELTA, 1);
			timeManager.setDeltaImage(frameDelta);
			long binFirstMs = XMLUtil.getElementLongValue(node, ID_FIRSTKYMOCOLMS, -1);
			timeManager.setBinFirst_ms(binFirstMs);
			long binLastMs = XMLUtil.getElementLongValue(node, ID_LASTKYMOCOLMS, -1);
			timeManager.setBinLast_ms(binLastMs);
			long binDurationMs = XMLUtil.getElementLongValue(node, ID_BINKYMOCOLMS, -1);
			timeManager.setBinDurationMs(binDurationMs);

			// Load properties with error handling
			try {
				prop.loadXML_Properties(node);
				// System.out.println("Experiment properties loaded successfully");
			} catch (Exception e) {
				System.err.println("ERROR: Failed to load experiment properties: " + e.getMessage());
				return false;
			}

			// Load generator program (optional field)
			String generatorProgramValue = XMLUtil.getElementValue(node, ID_GENERATOR_PROGRAM, null);
			if (generatorProgramValue != null) {
				generatorProgram = generatorProgramValue;
			}

			ugly_checkOffsetValues();

			return true;

		} catch (Exception e) {
			System.err.println("ERROR during experiment XML loading: " + e.getMessage());
			e.printStackTrace();
			return false;
		}
	}

	public boolean save_MS96_experiment() {
		try {
			final Document doc = XMLUtil.createDocument(true);
			if (doc == null) {
				System.err.println("ERROR: Could not create XML document");
				return false;
			}

			Node xmlRoot = XMLUtil.getRootElement(doc, true);
			Node node = XMLUtil.setElement(xmlRoot, ID_MCEXPERIMENT);
			if (node == null) {
				System.err.println("ERROR: Could not create MCexperiment node");
				return false;
			}

			// Version information
			XMLUtil.setElementValue(node, ID_VERSION, ID_VERSIONNUM);
			// System.out.println("Saving XML Version: " + ID_VERSIONNUM);

			// Save ImageLoader configuration
			ImageLoader imgLoader = seqCamData.getImageLoader();
			long frameFirst = imgLoader.getAbsoluteIndexFirstImage();
			long nImages = imgLoader.getFixedNumberOfImages();
			XMLUtil.setElementLongValue(node, ID_FRAMEFIRST, frameFirst);
			XMLUtil.setElementLongValue(node, ID_NFRAMES, nImages);

			// Save TimeManager configuration
			TimeManager timeManager = seqCamData.getTimeManager();
			long firstMs = timeManager.getFirstImageMs();
			long lastMs = timeManager.getLastImageMs();
			XMLUtil.setElementLongValue(node, ID_TIMEFIRSTIMAGEMS, firstMs);
			XMLUtil.setElementLongValue(node, ID_TIMELASTIMAGEMS, lastMs);
			XMLUtil.setElementLongValue(node, ID_FRAMEDELTA, timeManager.getDeltaImage());
			XMLUtil.setElementLongValue(node, ID_FIRSTKYMOCOLMS, timeManager.getBinFirst_ms());
			XMLUtil.setElementLongValue(node, ID_LASTKYMOCOLMS, timeManager.getBinLast_ms());
			XMLUtil.setElementLongValue(node, ID_BINKYMOCOLMS, timeManager.getBinDurationMs());

			// Save properties
			try {
				prop.saveXML_Properties(node);
				// System.out.println("Experiment properties saved successfully");
			} catch (Exception e) {
				System.err.println("ERROR: Failed to save experiment properties: " + e.getMessage());
				return false;
			}

			// Save generator program (optional field)
			// Auto-determine if not already set
			String programToSave = getOrDetermineGeneratorProgram();
			if (programToSave != null) {
				generatorProgram = programToSave;
				XMLUtil.setElementValue(node, ID_GENERATOR_PROGRAM, generatorProgram);
			}

			if (camDataImagesDirectory == null)
				camDataImagesDirectory = seqCamData.getImagesDirectory();
			XMLUtil.setElementValue(node, ID_IMAGESDIRECTORY, camDataImagesDirectory);

			// Always save to v2_ format
			String tempname = concatenateExptDirectoryWithSubpathAndName(null, ID_V2_EXPERIMENT_XML);
			boolean success = XMLUtil.saveDocument(doc, tempname);
			return success;
		} catch (Exception e) {
			System.err.println("ERROR during experiment XML saving: " + e.getMessage());
			e.printStackTrace();
			return false;
		}
	}

	private void ugly_checkOffsetValues() {
		if (seqCamData.getFirstImageMs() < 0)
			seqCamData.setFirstImageMs(0);
		if (seqCamData.getLastImageMs() < 0)
			seqCamData.setLastImageMs(0);
		if (seqCamData.getTimeManager().getBinFirst_ms() < 0)
			seqCamData.getTimeManager().setBinFirst_ms(0);
		if (seqCamData.getTimeManager().getBinLast_ms() < 0)
			seqCamData.getTimeManager().setBinLast_ms(0);
		if (seqCamData.getTimeManager().getBinDurationMs() < 0)
			seqCamData.getTimeManager().setBinDurationMs(60000);
	}

	// -------------------------------

	private String getXML_MS96_cages_Location(String XMLfileName) {
		String fileName = findFile_3Locations(XMLfileName, EXPT_DIRECTORY, BIN_DIRECTORY, IMG_DIRECTORY);
		if (fileName == null)
			fileName = concatenateExptDirectoryWithSubpathAndName(null, XMLfileName);
		return fileName;
	}

	/**
	 * High-level loader for cage description and measures (legacy \"MS96\" naming).
	 * <p>
	 * Responsibilities:
	 * <ul>
	 *   <li>load cage description from results directory (CSV/XML with migration),</li>
	 *   <li>load cage measures from bin directory,</li>
	 *   <li>materialize cages and spots as ROIs on {@link SequenceCamData}.</li>
	 * </ul>
	 */
	public boolean load_MS96_cages() {
		String resultsDir = getResultsDirectory();

		// Check if migration is needed
		MigrationDetector detector = new MigrationDetector();
		if (detector.needsMigration(resultsDir)) {
			Logger.info("Experiment:load_MS96_cages() Old format detected, running migration...");
			MigrationTool migrationTool = new MigrationTool();
			// Load old format first (spots will be in cage XML)
			boolean loaded = cages.getPersistence().load_Cages(cages, resultsDir);
			if (loaded) {
				// Run migration to convert to new format
				boolean migrated = migrationTool.migrateExperiment(this, resultsDir);
				if (migrated) {
					Logger.info("Experiment:load_MS96_cages() Migration completed successfully");
					// Reload in new format
					loaded = cages.getPersistence().load_Cages(cages, resultsDir);
				} else {
					Logger.warn("Experiment:load_MS96_cages() Migration failed, using old format");
				}
			}
			// Also load spots from CSV (may be empty if migration failed)
			spotsArray.getPersistence().loadSpotsArrayDescription(spotsArray, resultsDir);

			// Transfer cages to ROIs on sequence if loaded successfully
			if (loaded && seqCamData != null && seqCamData.getSequence() != null) {
				cages.cagesToROIs(seqCamData);
				cages.updateCagesFromSequence(seqCamData);
			}

			return loaded;
		}

		// New format - load descriptions from results directory
		boolean cagesLoaded = cages.getPersistence().load_Cages(cages, resultsDir);

		// Load measures from bin directory (if available)
		String binDir = getKymosBinFullDirectory();
		if (binDir != null) {
			cages.getPersistence().loadCagesArrayMeasures(cages, binDir);
		}

		// Transfer cages to ROIs on sequence if loaded successfully
		if (cagesLoaded && seqCamData != null && seqCamData.getSequence() != null) {
			CagesSequenceMapper.pushCagesToSequence(cages, seqCamData);
			CagesSequenceMapper.pullCagesFromSequence(cages, seqCamData);
		}

		// Also load spots descriptions from CSV (independent persistence)
		spotsArray.getPersistence().loadSpotsArrayDescription(spotsArray, resultsDir);

		// Load spots measures from bin directory (if available)
		if (binDir != null) {
			spotsArray.getPersistence().loadSpotsArrayMeasures(spotsArray, binDir);
		}

		return cagesLoaded;
	}

	public boolean save_MS96_cages() {
		String resultsDir = getResultsDirectory();
		// Always sync latest cage geometry from sequence before persisting.
		CagesSequenceMapper.syncCagesFromSequenceBeforeSave(cages, seqCamData);
		boolean descriptionsSaved = cages.getPersistence().save_Cages(cages, resultsDir);

		// Also save measures to bin directory (if available)
		String binDir = getKymosBinFullDirectory();
		if (binDir != null) {
			cages.getPersistence().saveCagesArrayMeasures(cages, binDir);
		}

		return descriptionsSaved;
	}

	// -------------------------------

	/**
	 * Loads spot description and measures for the current experiment.
	 * <p>
	 * Description comes from the results directory, measures from the bin
	 * directory.
	 */
	public boolean load_MS96_spotsMeasures() {
		String resultsDir = getResultsDirectory();
		boolean descriptionsLoaded = spotsArray.getPersistence().loadSpotsArrayDescription(spotsArray, resultsDir);

		// Load measures from bin directory (if available)
		String binDir = getKymosBinFullDirectory();
		if (binDir != null) {
			spotsArray.getPersistence().loadSpotsArrayMeasures(spotsArray, binDir);
		}

		return descriptionsLoaded;
	}

	/**
	 * Saves spot description and measures for the current experiment.
	 * <p>
	 * Description is written to the results directory, measures to the bin
	 * directory.
	 */
	public boolean save_MS96_spotsMeasures() {
		String resultsDir = getResultsDirectory();
		boolean descriptionsSaved = spotsArray.getPersistence().saveSpotsArrayDescription(spotsArray, resultsDir);

		// Also save measures to bin directory (if available)
		String binDir = getKymosBinFullDirectory();
		if (binDir != null) {
			spotsArray.getPersistence().saveSpotsArrayMeasures(spotsArray, binDir);
		}

		return descriptionsSaved;
	}

	public boolean load_MS96_fliesPositions() {
		// TODO write real code
		return false;
	}

	public boolean save_MS96_fliesPositions() {
		// Save fly positions to bin directory (e.g.,
		// results/bin60/CagesArrayMeasures.csv)
		String binDir = getKymosBinFullDirectory();
		if (binDir != null) {
			return cages.getPersistence().saveCagesArrayMeasures(cages, binDir);
		}
		return false;
	}

	// -------------------------------

	final String csvSep = ";";

	public Experiment getFirstChainedExperiment(boolean globalValue) {
		Experiment exp = this;
		if (globalValue && chainToPreviousExperiment != null)
			exp = chainToPreviousExperiment.getFirstChainedExperiment(globalValue);
		return exp;
	}

	public Experiment getLastChainedExperiment(boolean globalValue) {
		Experiment exp = this;
		if (globalValue && chainToNextExperiment != null)
			exp = chainToNextExperiment.getLastChainedExperiment(globalValue);
		return exp;
	}

	public List<String> getFieldValues(EnumXLSColumnHeader fieldEnumCode) {
		List<String> textList = new ArrayList<String>();
		switch (fieldEnumCode) {
		case EXP_STIM1:
		case EXP_CONC1:
		case EXP_EXPT:
		case EXP_BOXID:
		case EXP_STRAIN:
		case EXP_SEX:
		case EXP_STIM2:
		case EXP_CONC2:
			textList.add(prop.getField(fieldEnumCode));
			break;
		case SPOT_STIM:
		case SPOT_CONC:
		case SPOT_VOLUME:
			textList = getSpotsFieldValues(fieldEnumCode);
			break;
		case CAGE_SEX:
		case CAGE_STRAIN:
		case CAGE_AGE:
			textList = getCagesFieldValues(fieldEnumCode);
			break;
		case CAP_STIM:
		case CAP_CONC:
		case CAP_VOLUME:
			loadMCCapillaries();
			textList = getCapillariesFieldValues(fieldEnumCode);
			break;
		default:
			break;
		}
		return textList;
	}

	public boolean replaceExperimentFieldIfEqualOldValue(EnumXLSColumnHeader fieldEnumCode, String oldValue,
			String newValue) {
		boolean flag = prop.getField(fieldEnumCode).equals(oldValue);
		if (flag) {
			prop.setFieldNoTest(fieldEnumCode, newValue);
		}
		return flag;
	}

	public void replaceCapillariesFieldIfEqualOldValue(EnumXLSColumnHeader fieldEnumCode, String oldValue,
			String newValue) {
		for (Capillary cap : getCapillaries().getList()) {
			String capVal = cap.getField(fieldEnumCode);
			boolean flag = capVal.equals(oldValue);
			if (flag) {
				cap.setField(fieldEnumCode, newValue);
			}
		}
	}

	public String getExperimentField(EnumXLSColumnHeader fieldEnumCode) {
		// Handle non-property fields that are derived from experiment data
		switch (fieldEnumCode) {
		case PATH:
			return getPath();
		case DATE:
			return getDate();
		case CAM:
			return getCam();
		default:
			// Delegate all property fields to ExperimentProperties
			// This keeps property access logic in one place
			return prop.getField(fieldEnumCode);
		}
	}

	private String getPath() {
		String filename = getResultsDirectory();
		if (filename == null)
			filename = seqCamData != null ? seqCamData.getImagesDirectory() : null;
		if (filename == null)
			return "";
		Path path = Paths.get(filename);
		return path.toString();
	}

	private String getDate() {
		if (chainImageFirst_ms <= 0)
			return "";
		java.text.SimpleDateFormat df = new java.text.SimpleDateFormat("MM/dd/yyyy");
		return df.format(chainImageFirst_ms);
	}

	private String getCam() {
		String strField = getPath();
		int pos = strField.indexOf("cam");
		if (pos > 0) {
			int pos5 = pos + 5;
			if (pos5 >= strField.length())
				pos5 = strField.length() - 1;
			strField = strField.substring(pos, pos5);
		}
		return strField;
	}

	public void setExperimentFieldNoTest(EnumXLSColumnHeader fieldEnumCode, String newValue) {
		switch (fieldEnumCode) {
		case EXP_STIM1:
			prop.field_stim1 = newValue;
			break;
		case EXP_CONC1:
			prop.field_conc1 = newValue;
			break;
		case EXP_EXPT:
			setExperiment(newValue);
			break;
		case EXP_BOXID:
			setBoxID(newValue);
			break;
		case EXP_STRAIN:
			setStrain(newValue);
			break;
		case EXP_SEX:
			setSex(newValue);
			break;
		case EXP_STIM2:
			setCondition1(newValue);
			break;
		case EXP_CONC2:
			setCondition2(newValue);
			break;
		default:
			prop.setFieldNoTest(fieldEnumCode, newValue);
			break;
		}
	}

	public boolean replaceExperimentFieldIfEqualOld(EnumXLSColumnHeader fieldEnumCode, String oldValue,
			String newValue) {
		boolean flag = getExperimentField(fieldEnumCode).equals(oldValue);
		if (flag) {
			setExperimentFieldNoTest(fieldEnumCode, newValue);
		}
		return flag;
	}

	public void copyExperimentFields(Experiment expSource) {
		setExperimentFieldNoTest(EnumXLSColumnHeader.EXP_BOXID,
				expSource.getExperimentField(EnumXLSColumnHeader.EXP_BOXID));
		setExperimentFieldNoTest(EnumXLSColumnHeader.EXP_EXPT,
				expSource.getExperimentField(EnumXLSColumnHeader.EXP_EXPT));
		setExperimentFieldNoTest(EnumXLSColumnHeader.EXP_STIM1,
				expSource.getExperimentField(EnumXLSColumnHeader.EXP_STIM1));
		setExperimentFieldNoTest(EnumXLSColumnHeader.EXP_CONC1,
				expSource.getExperimentField(EnumXLSColumnHeader.EXP_CONC1));
		setExperimentFieldNoTest(EnumXLSColumnHeader.EXP_STRAIN,
				expSource.getExperimentField(EnumXLSColumnHeader.EXP_STRAIN));
		setExperimentFieldNoTest(EnumXLSColumnHeader.EXP_SEX,
				expSource.getExperimentField(EnumXLSColumnHeader.EXP_SEX));
		setExperimentFieldNoTest(EnumXLSColumnHeader.EXP_STIM2,
				expSource.getExperimentField(EnumXLSColumnHeader.EXP_STIM2));
		setExperimentFieldNoTest(EnumXLSColumnHeader.EXP_CONC2,
				expSource.getExperimentField(EnumXLSColumnHeader.EXP_CONC2));
	}

	public void getFieldValues(EnumXLSColumnHeader fieldEnumCode, List<String> textList) {
		switch (fieldEnumCode) {
		case EXP_STIM1:
		case EXP_CONC1:
		case EXP_EXPT:
		case EXP_BOXID:
		case EXP_STRAIN:
		case EXP_SEX:
		case EXP_STIM2:
		case EXP_CONC2:
			addValue(getExperimentField(fieldEnumCode), textList);
			break;
		case SPOT_STIM:
		case SPOT_CONC:
			addCapillariesValues(fieldEnumCode, textList);
			break;
		default:
			textList.add(prop.getField(fieldEnumCode));
			break;
		}
	}

	private void addValue(String text, List<String> textList) {
		if (!isFound(text, textList))
			textList.add(text);
	}

	// --------------------------------------------

	public boolean loadReferenceImage() {
		BufferedImage image = null;
		File inputfile = new File(getReferenceImageFullName());
		boolean exists = inputfile.exists();
		if (!exists)
			return false;
		image = ImageUtil.load(inputfile, true);
		if (image == null) {
			// System.out.println("Experiment:loadReferenceImage() image not loaded / not
			// found");
			return false;
		}
		seqCamData.setReferenceImage(IcyBufferedImage.createFrom(image));
		seqReference = new Sequence(seqCamData.getReferenceImage());
		seqReference.setName("referenceImage");
		return true;
	}

	public boolean saveReferenceImage(IcyBufferedImage referenceImage) {
		if (referenceImage == null) {
			Logger.error("Cannot save reference image: image is null");
			return false;
		}

		String fullPath = getReferenceImageFullName();
		File outputfile = new File(fullPath);
		File parentDir = outputfile.getParentFile();

		if (parentDir != null && !parentDir.exists()) {
			Logger.info("Creating directory for reference image: " + parentDir.getPath());
			if (!parentDir.mkdirs()) {
				Logger.error("Failed to create directory for reference image: " + parentDir.getPath());
				return false;
			}
		}

		Logger.info("Saving reference image to: " + fullPath);
		RenderedImage image = ImageUtil.toRGBImage(referenceImage);
		boolean success = ImageUtil.save(image, "jpg", outputfile);

		if (!success) {
			Logger.error("Failed to save reference image to: " + fullPath);
		} else {
			Logger.info("Reference image saved successfully to: " + fullPath);
		}

		return success;
	}

	public void cleanPreviousDetectedFliesROIs() {
		if (seqCamData == null || seqCamData.getSequence() == null) {
			return;
		}
		// Remove all ROIs containing "det" (detected fly positions)
		seqCamData.removeROIsContainingString("det");
	}

	public String zgetMCDrosoTrackFullName() {
		return resultsDirectory + File.separator + ID_MCDROSOTRACK_XML;
	}

	public void updateROIsAt(int t) {
		seqCamData.getSequence().beginUpdate();
		List<ROI2D> rois = seqCamData.getSequence().getROI2Ds();
		for (ROI2D roi : rois) {
			if (roi.getName().contains("det"))
				seqCamData.getSequence().removeROI(roi);
		}
		seqCamData.getSequence().addROIs(cages.getPositionsAsListOfROI2DRectanglesAtT(t), false);
		seqCamData.getSequence().endUpdate();
	}

	public void saveDetRoisToPositions() {
		List<ROI2D> detectedROIsList = seqCamData.getSequence().getROI2Ds();
		for (Cage cage : cages.cagesList) {
			cage.transferRoisToPositions(detectedROIsList);
		}
	}

	// ----------------------------------

	private int getBinStepFromDirectoryName(String resultsPath) {
		int step = -1;
		if (resultsPath.contains(BIN)) {
			if (resultsPath.length() < (BIN.length() + 1))
				step = (int) seqCamData.getTimeManager().getBinDurationMs();
			else
				step = Integer.valueOf(resultsPath.substring(BIN.length())) * 1000;
		}
		return step;
	}

	private String findFile_3Locations(String xmlFileName, int first, int second, int third) {
		// current directory
		String xmlFullFileName = findFile_1Location(xmlFileName, first);
		if (xmlFullFileName == null)
			xmlFullFileName = findFile_1Location(xmlFileName, second);
		if (xmlFullFileName == null)
			xmlFullFileName = findFile_1Location(xmlFileName, third);
		return xmlFullFileName;
	}

	private String findFile_1Location(String xmlFileName, int item) {
		String xmlFullFileName = File.separator + xmlFileName;
		switch (item) {
		case IMG_DIRECTORY:
			camDataImagesDirectory = getRootWithNoResultNorBinString(resultsDirectory);
			xmlFullFileName = camDataImagesDirectory + File.separator + xmlFileName;
			break;

		case BIN_DIRECTORY:
			// any directory (below)
			Path dirPath = Paths.get(resultsDirectory);
			List<Path> subFolders = Directories.getAllSubPathsOfDirectory(resultsDirectory, 1);
			if (subFolders == null)
				return null;
			List<String> resultsDirList = Directories.getPathsContainingString(subFolders, RESULTS);
			List<String> binDirList = Directories.getPathsContainingString(subFolders, BIN);
			resultsDirList.addAll(binDirList);
			for (String resultsSub : resultsDirList) {
				Path dir = dirPath.resolve(resultsSub + File.separator + xmlFileName);
				if (Files.notExists(dir))
					continue;
				xmlFullFileName = dir.toAbsolutePath().toString();
				break;
			}
			break;

		case EXPT_DIRECTORY:
		default:
			xmlFullFileName = resultsDirectory + xmlFullFileName;
			break;
		}

		// current directory
		if (xmlFullFileName != null && fileExists(xmlFullFileName)) {
			if (item == IMG_DIRECTORY) {
				camDataImagesDirectory = getRootWithNoResultNorBinString(resultsDirectory);
				ExperimentDirectories.moveAndRename(xmlFileName, camDataImagesDirectory, xmlFileName, resultsDirectory);
				xmlFullFileName = resultsDirectory + xmlFullFileName;
			}
			return xmlFullFileName;
		}
		return null;
	}

	private boolean fileExists(String fileName) {
		File f = new File(fileName);
		return (f.exists() && !f.isDirectory());
	}

	public boolean replaceSpotsFieldValueWithNewValueIfOld(EnumXLSColumnHeader fieldEnumCode, String oldValue,
			String newValue) {
		load_MS96_cages();
		boolean flag = false;
		for (Cage cage : cages.cagesList) {
			List<Spot> spots = cage.getSpots(spotsArray);
			for (Spot spot : spots) {
				String current = spot.getField(fieldEnumCode);
				if (current != null && oldValue != null && current.trim().equals(oldValue.trim())) {
					spot.setField(fieldEnumCode, newValue);
					flag = true;
				}
			}
		}
		return flag;
	}

	public boolean replaceCageFieldValueWithNewValueIfOld(EnumXLSColumnHeader fieldEnumCode, String oldValue,
			String newValue) {
		load_MS96_cages();
		boolean flag = false;
		for (Cage cage : cages.cagesList) {
			String current = cage.getField(fieldEnumCode);
			if (current != null && oldValue != null && current.trim().equals(oldValue.trim())) {
				cage.setField(fieldEnumCode, newValue);
				flag = true;
			}
		}
		return flag;
	}

	private String concatenateExptDirectoryWithSubpathAndName(String subpath, String name) {
		if (subpath != null)
			return resultsDirectory + File.separator + subpath + File.separator + name;
		else
			return resultsDirectory + File.separator + name;
	}

	private List<String> getSpotsFieldValues(EnumXLSColumnHeader fieldEnumCode) {
		load_MS96_cages();
		List<String> textList = new ArrayList<String>();
		for (Cage cage : cages.cagesList) {
			List<Spot> spots = cage.getSpots(spotsArray);
			for (Spot spot : spots)
				addValueIfUnique(spot.getField(fieldEnumCode), textList);
		}
		return textList;
	}

	private List<String> getCapillariesFieldValues(EnumXLSColumnHeader fieldEnumCode) {
		loadCapillaries();
		List<String> textList = new ArrayList<String>();
		for (Capillary cap : getCapillaries().getList())
			addValueIfUnique(cap.getField(fieldEnumCode), textList);
		return textList;
	}

	private List<String> getCagesFieldValues(EnumXLSColumnHeader fieldEnumCode) {
		load_MS96_cages();
		List<String> textList = new ArrayList<String>();
		for (Cage cage : cages.cagesList)
			addValueIfUnique(cage.getField(fieldEnumCode), textList);
		return textList;
	}

	private void addValueIfUnique(String text, List<String> textList) {
		if (!isFound(text, textList))
			textList.add(text);
	}

	private boolean isFound(String pattern, List<String> names) {
		boolean found = false;
		if (names.size() > 0) {
			for (String name : names) {
				found = name.equals(pattern);
				if (found)
					break;
			}
		}
		return found;
	}

	private String getReferenceImageFullName() {
		return resultsDirectory + File.separator + "referenceImage.jpg";
	}

	public void transferCagesROI_toSequence() {
		CagesSequenceMapper.pushCagesToSequence(cages, seqCamData);
	}

	public void transferSpotsROI_toSequence() {
		CagesSequenceMapper.pushSpotsToSequence(cages, spotsArray, seqCamData);
	}

	public boolean saveCagesArray_File() {
		CagesSequenceMapper.syncCagesFromSequenceBeforeSave(cages, seqCamData);
		save_MS96_cages();
		return save_MS96_spotsMeasures();
	}

	public boolean saveSpotsArray_file() {
		CagesSequenceMapper.pullSpotsFromSequence(cages, spotsArray, seqCamData);
		boolean flag = save_MS96_cages();
		flag &= save_MS96_spotsMeasures();
		return flag;
	}

	public ExperimentProperties getProperties() {
		return prop;
	}

	// ------------------------------ Capillaries Support

	public Capillaries getCapillaries() {
		return capillaries;
	}

	public void setCapillaries(Capillaries capillaries) {
		this.capillaries = capillaries;
	}

	// ------------------------------ Spots Support

	public SpotsArray getSpotsArray() {
		return spotsArray;
	}

	public void setSpotsArray(SpotsArray spotsArray) {
		this.spotsArray = spotsArray != null ? spotsArray : new SpotsArray();
	}

	public SequenceCamData getSeqCamData() {
		if (seqCamData == null)
			seqCamData = new SequenceCamData();
		return seqCamData;
	}

	public void setSeqCamData(SequenceCamData seqCamData) {
		this.seqCamData = seqCamData;
	}

	public SequenceKymos getSeqKymos() {
		if (seqKymos == null)
			seqKymos = new SequenceKymos();
		return seqKymos;
	}

	public void setSeqKymos(SequenceKymos seqKymos) {
		this.seqKymos = seqKymos;
	}

	public String getKymosBinFullDirectory() {
		if (binDirectory == null) {
			return resultsDirectory;
		}
		// Check if binDirectory is already an absolute path
		File binDirFile = new File(binDirectory);
		if (binDirFile.isAbsolute()) {
			return binDirectory;
		}
		// Otherwise, concatenate it to resultsDirectory
		return resultsDirectory + File.separator + binDirectory;
	}

	public String getKymoFullPath(String filename) {
		if (binDirectory == null) {
			return resultsDirectory + File.separator + filename + ".tiff";
		}
		// Check if binDirectory is already an absolute path
		File binDirFile = new File(binDirectory);
		if (binDirFile.isAbsolute()) {
			return binDirectory + File.separator + filename + ".tiff";
		}
		// Otherwise, concatenate it to resultsDirectory
		return resultsDirectory + File.separator + binDirectory + File.separator + filename + ".tiff";
	}

	public String getExperimentDirectory() {
		return resultsDirectory;
	}

	public void setExperimentDirectory(String fileName) {
		resultsDirectory = ExperimentDirectories.getParentIf(fileName, BIN);
	}

	public String getBinSubDirectory() {
		return binDirectory;
	}

	public void setBinSubDirectory(String bin) {
		binDirectory = bin;
	}

	/**
	 * Gets the number of output frames for the experiment.
	 * 
	 * @param exp     The experiment
	 * @param options The export options
	 * @return The number of output frames
	 */
	public int getNOutputFrames(ResultsOptions options) {
		// For capillaries, use kymograph timing
		long kymoFirst_ms = getKymoFirst_ms();
		long kymoLast_ms = getKymoLast_ms();
		long kymoBin_ms = getKymoBin_ms();

		// If buildExcelStepMs equals kymoBin_ms, we want 1:1 mapping - use actual frame
		// count
		if (kymoBin_ms > 0 && options.buildExcelStepMs == kymoBin_ms && getSeqKymos() != null) {
			ImageLoader imgLoader = getSeqKymos().getImageLoader();
			if (imgLoader != null) {
				int nFrames = imgLoader.getNTotalFrames();
				if (nFrames > 0) {
					return nFrames;
				}
			}
		}

		if (kymoLast_ms <= kymoFirst_ms) {
			// Try to get from kymograph sequence
			if (getSeqKymos() != null) {
				ImageLoader imgLoader = getSeqKymos().getImageLoader();
				if (imgLoader != null) {
					if (kymoBin_ms > 0) {
						kymoLast_ms = kymoFirst_ms + imgLoader.getNTotalFrames() * kymoBin_ms;
						setKymoLast_ms(kymoLast_ms);
					}
				}
			}
		}

		long durationMs = kymoLast_ms - kymoFirst_ms;
		int nOutputFrames = (int) (durationMs / options.buildExcelStepMs + 1);

		if (nOutputFrames <= 1) {
			handleError(-1);
			// Fallback to a reasonable default
			nOutputFrames = 1000;
		}

		return nOutputFrames;
	}

	/**
	 * Handles export errors by logging them.
	 * 
	 * @param exp           The experiment
	 * @param nOutputFrames The number of output frames
	 */
	protected void handleError(int nOutputFrames) {
		String error = String.format(
				"Experiment:GNOutputFrames Error() ERROR in %s\n nOutputFrames=%d kymoFirstCol_Ms=%d kymoLastCol_Ms=%d",
				getExperimentDirectory(), nOutputFrames, getKymoFirst_ms(), getKymoLast_ms());
		System.err.println(error);
	}

	// ------------------------------

	public boolean loadMCCapillaries_Only() {
		// Try to load from CSV first (new format)
		String resultsDir = getResultsDirectory();
		boolean csvLoaded = capillaries.getPersistence().load_CapillariesArrayDescription(capillaries, resultsDir);

		// New format methods already have internal fallback to legacy formats
		boolean flag = csvLoaded;

		// load MCcapillaries description of experiment
		if (prop.field_boxID.contentEquals("..") && prop.field_experiment.contentEquals("..")
				&& prop.field_comment1.contentEquals("..") && prop.field_comment2.contentEquals("..")
				&& prop.field_sex.contentEquals("..") && prop.field_strain.contentEquals("..")) {
			prop.field_boxID = capillaries.getCapillariesDescription().getOld_boxID();
			prop.field_experiment = capillaries.getCapillariesDescription().getOld_experiment();
			prop.field_comment1 = capillaries.getCapillariesDescription().getOld_comment1();
			prop.field_comment2 = capillaries.getCapillariesDescription().getOld_comment2();
			prop.field_sex = capillaries.getCapillariesDescription().getOld_sex();
			prop.field_strain = capillaries.getCapillariesDescription().getOld_strain();
			prop.field_stim2 = capillaries.getCapillariesDescription().getOld_cond1();
			prop.field_conc2 = capillaries.getCapillariesDescription().getOld_cond2();
		}
		return flag;
	}

	public boolean loadMCCapillaries() {
		// Try to load from CSV first (new format)
		String resultsDir = getResultsDirectory();
		boolean flag1 = capillaries.getPersistence().load_CapillariesArrayDescription(capillaries, resultsDir);

		// Load measures from bin directory (new format)
		String kymosImagesDirectory = getKymosBinFullDirectory();
		boolean flag2 = false;
		if (kymosImagesDirectory != null) {
			flag2 = capillaries.getPersistence().load_CapillariesArrayMeasures(capillaries, kymosImagesDirectory);
		}

		if (flag1 & flag2) {
			// TODO: Add loadListOfPotentialKymographsFromCapillaries method to
			// SequenceKymos
			// For now, this functionality may need to be implemented
		}
		return flag1 | flag2;
	}

	// TODO: Refactor migration logic - xmlLoadOldCapillaries() needs access to
	// legacy methods
	// for migration purposes. Consider moving this to a migration service or making
	// legacy methods accessible for migration only.
	private boolean xmlLoadOldCapillaries() {
		// Migration logic temporarily disabled - new format methods have internal
		// fallback
		// This method should be refactored to use migration service
		return false;
	}

	private boolean xmlReadCamDataROIs(String fileName) {
		Sequence seq = seqCamData.getSequence();
		if (fileName != null) {
			final Document doc = XMLUtil.loadDocument(fileName);
			if (doc != null) {
				List<ROI2D> seqRoisList = seq.getROI2Ds(false);
				List<ROI2D> newRoisList = ROI2DUtilities.loadROIsFromXML(doc);
				ROI2DUtilities.mergeROIsListNoDuplicate(seqRoisList, newRoisList, seq);
				seq.removeAllROI();
				seq.addROIs(seqRoisList, false);
				return true;
			}
		}
		return false;
	}

	private boolean xmlReadRoiLineParameters(String filename) {
		if (filename != null) {
			final Document doc = XMLUtil.loadDocument(filename);
			if (doc != null)
				return capillaries.getCapillariesDescription().xmlLoadCapillaryDescription(doc);
		}
		return false;
	}

	// ---------------------------------------------

	public boolean saveMCCapillaries_Only() {
		// XML save disabled - descriptions and ROI coordinates are now stored in CSV
		// String xmlCapillaryFileName = resultsDirectory + File.separator +
		// capillaries.getXMLNameToAppend();
		transferExpDescriptorsToCapillariesDescriptors();
		// return capillaries.xmlSaveCapillaries_Descriptors(xmlCapillaryFileName);
		return true; // Return true since CSV save is handled separately
	}

	private void transferExpDescriptorsToCapillariesDescriptors() {
		CapillariesDescription desc = capillaries.getCapillariesDescription();
		desc.setOld_boxID(prop.field_boxID);
		desc.setOld_experiment(prop.field_experiment);
		desc.setOld_comment1(prop.field_comment1);
		desc.setOld_comment2(prop.field_comment2);
		desc.setOld_strain(prop.field_strain);
		desc.setOld_sex(prop.field_sex);
		desc.setOld_cond1(prop.field_stim2);
		desc.setOld_cond2(prop.field_conc2);
	}

	public boolean loadCapillaries() {
		String resultsDir = getResultsDirectory();
		// Try new format: descriptions from results, measures from bin
		boolean descriptionsLoaded = capillaries.getPersistence().load_CapillariesArrayDescription(capillaries,
				resultsDir);

		String binDir = getKymosBinFullDirectory();
		if (binDir != null) {
			boolean measuresLoaded = capillaries.getPersistence().load_CapillariesArrayMeasures(capillaries, binDir);
			if (descriptionsLoaded || measuresLoaded) {
				return true;
			}
		}

		// New format methods already have internal fallback to legacy formats
		return descriptionsLoaded;
	}

	public boolean saveCapillaries() {
		String resultsDir = getResultsDirectory();
		// Save descriptions to new format
		boolean descriptionsSaved = capillaries.getPersistence().saveCapillariesArrayDescription(capillaries,
				resultsDir);

		String binDir = getKymosBinFullDirectory();
		if (binDir != null) {
			// Save measures to new format
			capillaries.getPersistence().save_CapillariesArrayMeasures(capillaries, binDir);
		}

		return descriptionsSaved;
	}

	public boolean loadCagesMeasures() {

		String resultsDir = getResultsDirectory();
		// Try new format: descriptions from results, measures from bin
		boolean descriptionsLoaded = cages.getPersistence().loadCagesArrayDescription(cages, resultsDir);
		if (!descriptionsLoaded)
			return descriptionsLoaded;

		String binDir = getKymosBinFullDirectory();
		boolean measuresLoaded = false;
		if (binDir != null) {
			measuresLoaded = cages.getPersistence().loadCagesArrayMeasures(cages, binDir);
		}

		if (measuresLoaded && seqCamData.getSequence() != null) {
			CagesSequenceMapper.pushCagesToSequence(cages, seqCamData);
			CagesSequenceMapper.pullCagesFromSequence(cages, seqCamData);
		}

		// If cages list is empty after loading, create cages from capillaries
		if (cages.getCageList().size() == 0 && capillaries.getList().size() > 0) {
			dispatchCapillariesToCages();
		}

		return measuresLoaded;
	}

//	private boolean moveCageMeasuresToExperimentDirectory(String pathToMeasures) {
//		boolean flag = false;
//		String pathToOldCsv = getKymosBinFullDirectory() + File.separator + "CagesMeasures.csv";
//		File fileToMove = new File(pathToOldCsv);
//		if (fileToMove.exists())
//			flag = fileToMove.renameTo(new File(pathToMeasures));
//		return flag;
//	}

	public boolean saveCagesMeasures() {
		String resultsDir = getResultsDirectory();
		boolean descriptionsSaved = cages.getPersistence().save_Cages(cages, resultsDir);

		// Also save measures to bin directory (if available)
		String binDir = getKymosBinFullDirectory();
		System.out.println(binDir);
		if (binDir != null) {
			cages.getPersistence().saveCagesArrayMeasures(cages, binDir);
		}

		return descriptionsSaved;
	}

	public void saveCagesAndMeasures() {
		CagesSequenceMapper.syncCagesFromSequenceBeforeSave(cages, seqCamData);
		saveCagesMeasures();
	}

	public boolean adjustCapillaryMeasuresDimensions() {
		KymographInfo kymoInfo = seqKymos.getKymographInfo();
		if (kymoInfo.getMaxWidth() < 1) {
			kymoInfo = seqKymos.updateMaxDimensionsFromSequence();
			if (kymoInfo.getMaxWidth() < 1)
				return false;
		}
		capillaries.adjustToImageWidth(kymoInfo.getMaxWidth());
		seqKymos.getSequence().removeAllROI();
		CapillariesKymosMapper.pushCapillaryMeasuresToKymos(capillaries, seqKymos);
		return true;
	}

	public boolean cropCapillaryMeasuresDimensions() {
		KymographInfo kymoInfo = seqKymos.getKymographInfo();
		if (kymoInfo.getMaxWidth() < 1) {
			kymoInfo = seqKymos.updateMaxDimensionsFromSequence();
			if (kymoInfo.getMaxWidth() < 1)
				return false;
		}
		int imageWidth = kymoInfo.getMaxWidth();
		capillaries.cropToImageWidth(imageWidth);
		seqKymos.getSequence().removeAllROI();
		CapillariesKymosMapper.pushCapillaryMeasuresToKymos(capillaries, seqKymos);
		return true;
	}

	public boolean saveCapillariesMeasures(String directory) {
		// CRITICAL: Load capillaries if not already loaded to prevent overwriting with
		// empty list
		// This protects against cases where BuildSeries operations (fly detection,
		// background building)
		// load only cages but not capillaries, and then a save operation is triggered
		if (capillaries.getList().isEmpty()) {
			loadMCCapillaries_Only();
			// Also try to load the actual measures if kymos directory exists
			String binDir = getKymosBinFullDirectory();
			if (binDir != null) {
				capillaries.getPersistence().load_CapillariesArrayMeasures(capillaries, binDir);
			}
		}

		if (seqKymos != null && seqKymos.getSequence() != null) {
			CapillariesKymosMapper.pullCapillaryMeasuresFromKymos(capillaries, seqKymos);
		}
		return capillaries.getPersistence().save_CapillariesArrayMeasures(capillaries, directory);
	}

	// ---------------------------------------------------------

	public void dispatchCapillariesToCages() {
		if (capillaries.getList().size() < 1)
			return;

		// Clear capillary ID lists in all cages
		for (Cage cage : cages.getCageList()) {
			cage.clearCapillaryList();
		}

		// Dispatch capillaries to cages using ID-based approach
		for (Capillary cap : capillaries.getList()) {
			int nflies = cap.getProperties().nFlies;
			int cageID = cap.getCageID();
			Cage cage = cages.getCageFromID(cageID);
			if (cage == null) {
				cage = new Cage();
				cage.getProperties().setCageID(cageID);
				cage.getProperties().setCageNFlies(nflies);
				cages.getCageList().add(cage);
			}
			// Add capillary ID instead of object reference
			CapillaryID capID = new CapillaryID(cap.getKymographIndex());
			cage.addCapillaryIDIfUnique(capID);
			// Also update legacy field for backward compatibility
			cage.addCapillaryIfUnique(cap);
		}

		if (cages.getCageList().size() > 0) {
			cages.checkAndCorrectCagePositions();
		}
	}

	public void getCapillaryMeasures(EnumResults resultType, boolean subtractEvaporation) {
		ResultsOptions resultsOptions = new ResultsOptions();
		resultsOptions.resultType = resultType;
		resultsOptions.correctEvaporation = subtractEvaporation;
		prepareComputations(resultsOptions);
	}

	public void prepareComputations(ResultsOptions resultsOptions) {
		getCages().prepareComputations(this, resultsOptions);
	}

	public ResultsArray transformDataScalingForOutput(EnumResults resultType, boolean subtractEvaporation) {
		ResultsOptions resultsOptions = new ResultsOptions();
		long kymoBin_ms = getKymoBin_ms();
		if (kymoBin_ms <= 0) {
			kymoBin_ms = 60000;
		}
		resultsOptions.buildExcelStepMs = (int) kymoBin_ms;
		resultsOptions.relativeToMaximum = false;
		resultsOptions.correctEvaporation = subtractEvaporation;
		resultsOptions.resultType = resultType;

		ResultsArray resultsArray = new ResultsArray();
		double scalingFactorToPhysicalUnits = getCapillaries().getScalingFactorToPhysicalUnits(resultType);

		ResultsArrayFromCapillaries collectResults = new ResultsArrayFromCapillaries(getCapillaries().getList().size());
		for (Capillary capillary : getCapillaries().getList()) {
			Results results = collectResults.getCapillaryMeasure(this, capillary, resultsOptions);
			if (results != null) {
				results.transferDataValuesToValuesOut(scalingFactorToPhysicalUnits, resultType);
				resultsArray.addRow(results);
			}
		}

		return resultsArray;
	}

	// ---------------------------------------------------------

	public boolean loadKymographs() {
		if (getSeqKymos() == null)
			setSeqKymos(new SequenceKymos());

		List<ImageFileData> myList = new KymographService()
				.loadListOfPotentialKymographsFromCapillaries(getKymosBinFullDirectory(), capillaries);

		// Filter to get existing file names
		ImageFileData.getExistingFileNames(myList);

		// Convert to experiment1 ImageFileDescriptor format
		ArrayList<ImageFileData> newList = new ArrayList<ImageFileData>();
		for (ImageFileData oldDesc : myList) {
			if (oldDesc.fileName != null && oldDesc.exists) {
				ImageFileData newDesc = new ImageFileData();
				newDesc.fileName = oldDesc.fileName;
				newDesc.exists = oldDesc.exists;
				newDesc.imageHeight = oldDesc.imageHeight;
				newDesc.imageWidth = oldDesc.imageWidth;
				newList.add(newDesc);
			}
		}

		// Load images using the new API
		Rectangle rectMax = getSeqKymos().calculateMaxDimensions(newList);
		ImageAdjustmentOptions options = ImageAdjustmentOptions.withSizeAdjustment(rectMax);
		ImageProcessingResult result = getSeqKymos().loadKymographs(newList, options);
		return result.isSuccess();
	}

	public boolean loadCamDataCapillaries() {
		if (resultsDirectory == null) {
			return false;
		}
		boolean flag = loadMCCapillaries_Only();
		if (flag && seqCamData != null && seqCamData.getSequence() != null) {
			capillaries.transferCapillaryRoiToSequence(seqCamData.getSequence());
		}
		return flag;
	}

	private void addCapillariesValues(EnumXLSColumnHeader fieldEnumCode, List<String> textList) {
		if (capillaries.getList().size() == 0)
			loadMCCapillaries_Only();
//		// Convert new enum to old enum for Capillary compatibility
//		EnumXLSColumnHeader oldEnum = convertToOldEnum(fieldEnumCode);
//		if (oldEnum == null)
//			return;
		for (Capillary cap : capillaries.getList())
			addValueIfUnique(cap.getCapillaryField(fieldEnumCode), textList);
	}

	private EnumXLSColumnHeader convertToOldEnum(EnumXLSColumnHeader newEnum) {
		// Convert new enum values to old enum values for Capillary compatibility
		switch (newEnum) {
		case SPOT_STIM:
			return EnumXLSColumnHeader.CAP_STIM;
		case SPOT_CONC:
			return EnumXLSColumnHeader.CAP_CONC;
		default:
			return null;
		}
	}

}
