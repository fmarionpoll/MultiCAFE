package plugins.fmp.multicafe.fmp_experiment;

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
import plugins.fmp.multicafe.fmp_experiment.capillaries.Capillaries;
import plugins.fmp.multicafe.fmp_experiment.capillaries.Capillary;
import plugins.fmp.multicafe.fmp_experiment.sequence.ImageLoader;
import plugins.fmp.multicafe.fmp_experiment.sequence.SequenceCamData;
import plugins.fmp.multicafe.fmp_experiment.sequence.SequenceKymos;
import plugins.fmp.multicafe.fmp_experiment.sequence.TimeManager;
import plugins.fmp.multicafe.fmp_experiment.spots.Spot;
import plugins.fmp.multicafe.tools.Logger;
import plugins.fmp.multicafe.tools.ROI2D.ROI2DUtilities;
import plugins.fmp.multicafe.tools1.Directories;
import plugins.fmp.multicafe.tools1.toExcel.EnumXLSColumnHeader;

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
	private ExperimentTimeManager timeManager = new ExperimentTimeManager();

	public FileTime firstImage_FileTime;
	public FileTime lastImage_FileTime;

	private ExperimentProperties prop = new ExperimentProperties();
	public int col = -1;
	public Experiment chainToPreviousExperiment = null;
	public Experiment chainToNextExperiment = null;
	public long chainImageFirst_ms = 0;
	public int experimentID = 0;

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

	public ExperimentTimeManager getTimeManager() {
		return timeManager;
	}

	public void setTimeManager(ExperimentTimeManager timeManager) {
		this.timeManager = timeManager;
	}

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

	// __________________________________________________

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
	private final String ID_MS96_experiment_XML = "MS96_experiment.xml";
	private final static String ID_MCDROSOTRACK_XML = "MCdrosotrack.xml";

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
		load_MS96_experiment(concatenateExptDirectoryWithSubpathAndName(null, ID_MS96_experiment_XML));
	}

	public Experiment(ExperimentDirectories eADF) {
		camDataImagesDirectory = eADF.getCameraImagesDirectory();
		resultsDirectory = eADF.getResultsDirectory();
		seqCamData = SequenceCamData.builder().withStatus(EnumStatus.FILESTACK).build();
		String fileName = concatenateExptDirectoryWithSubpathAndName(null, ID_MS96_experiment_XML);
		load_MS96_experiment(fileName);

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
		return prop.ffield_boxID;
	}

	public void setBoxID(String boxID) {
		prop.ffield_boxID = boxID;
	}

	public String getExperiment() {
		return prop.ffield_experiment;
	}

	public void setExperiment(String experiment) {
		prop.ffield_experiment = experiment;
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
	}

	public boolean zopenPositionsMeasures() {
		if (seqCamData == null) {
			// Use builder pattern for initialization
			seqCamData = SequenceCamData.builder().withStatus(EnumStatus.FILESTACK).build();
		}
		load_MS96_experiment();
		getFileIntervalsFromSeqCamData();

		return zxmlReadDrosoTrack(null);
	}

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
			cages.transferCageSpotsToSequenceAsROIs(seqCamData);
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
//		if (binSubDirectory != null)
//			dir = dir.resolve(binSubDirectory);
		String directory = dir.toAbsolutePath().toString();
		if (!createDirectoryIfDoesNotExist(directory))
			directory = null;
		return directory;
	}

	// -------------------------------

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
		String csFileName = concatenateExptDirectoryWithSubpathAndName(null, ID_MS96_experiment_XML);
		return load_MS96_experiment(csFileName);
	}

	private boolean load_MS96_experiment(String csFileName) {
		try {
			final Document doc = XMLUtil.loadDocument(csFileName);
			if (doc == null) {
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

			long nImages = XMLUtil.getElementLongValue(node, ID_NFRAMES, -1);
			if (nImages <= 0) {
				System.err.println("ERROR: Invalid number of frames: " + nImages + " in " + csFileName);
				return false;
			}
			imgLoader.setFixedNumberOfImages(nImages);
			imgLoader.setNTotalFrames((int) (nImages - frameFirst));

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

			if (camDataImagesDirectory == null)
				camDataImagesDirectory = seqCamData.getImagesDirectory();
			XMLUtil.setElementValue(node, ID_IMAGESDIRECTORY, camDataImagesDirectory);

			String tempname = concatenateExptDirectoryWithSubpathAndName(null, ID_MS96_experiment_XML);
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

	public boolean load_MS96_cages() {
		String fileName = getXML_MS96_cages_Location(cages.ID_MS96_cages_XML);
		return cages.xmlReadCagesFromFileNoQuestion(fileName);
	}

	public boolean save_MS96_cages() {
		String fileName = getXML_MS96_cages_Location(cages.ID_MS96_cages_XML);
		return cages.xmlWriteCagesToFileNoQuestion(fileName);
	}

	// -------------------------------

	public boolean load_MS96_spotsMeasures() {
		return cages.load_SpotsMeasures(getResultsDirectory());
	}

	public boolean save_MS96_spotsMeasures() {
		return cages.save_SpotsMeasures(getResultsDirectory());
	}

	public boolean load_MS96_fliesPositions() {
		// TODO write real code
		return false;
	}

	public boolean save_MS96_fliesPositions() {
		// TODO write real code
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

	public void setFileTimeImageFirst(FileTime fileTimeImageFirst) {
		this.firstImage_FileTime = fileTimeImageFirst;
	}

	public void setFileTimeImageLast(FileTime fileTimeImageLast) {
		this.lastImage_FileTime = fileTimeImageLast;
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
			textList.add(prop.getExperimentField(fieldEnumCode));
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
		default:
			break;
		}
		return textList;
	}

	public boolean replaceExperimentFieldIfEqualOldValue(EnumXLSColumnHeader fieldEnumCode, String oldValue,
			String newValue) {
		boolean flag = prop.getExperimentField(fieldEnumCode).equals(oldValue);
		if (flag) {
			prop.setExperimentFieldNoTest(fieldEnumCode, newValue);
		}
		return flag;
	}

	public String getExperimentField(EnumXLSColumnHeader fieldEnumCode) {
		String strField = null;
		switch (fieldEnumCode) {
		case PATH:
			strField = getPath();
			break;
		case DATE:
			strField = getDate();
			break;
		case CAM:
			strField = getCam();
			break;
		case EXP_STIM1:
			strField = getComment1();
			break;
		case EXP_CONC1:
			strField = getComment2();
			break;
		case EXP_EXPT:
			strField = getExperiment();
			break;
		case EXP_BOXID:
			strField = getBoxID();
			break;
		case EXP_STRAIN:
			strField = getStrain();
			break;
		case EXP_SEX:
			strField = getSex();
			break;
		case EXP_STIM2:
			strField = getCondition1();
			break;
		case EXP_CONC2:
			strField = getCondition2();
			break;
		default:
			strField = prop.getExperimentField(fieldEnumCode);
			break;
		}
		return strField;
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
			setComment1(newValue);
			break;
		case EXP_CONC1:
			setComment2(newValue);
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
			prop.setExperimentFieldNoTest(fieldEnumCode, newValue);
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
			textList.add(prop.getExperimentField(fieldEnumCode));
			break;
		}
	}

	public void replaceFieldValue(EnumXLSColumnHeader fieldEnumCode, String oldValue, String newValue) {
		switch (fieldEnumCode) {
		case EXP_STIM1:
		case EXP_CONC1:
		case EXP_EXPT:
		case EXP_BOXID:
		case EXP_STRAIN:
		case EXP_SEX:
		case EXP_STIM2:
		case EXP_CONC2:
			replaceExperimentFieldIfEqualOld(fieldEnumCode, oldValue, newValue);
			break;
		case SPOT_STIM:
		case SPOT_CONC:
			if (replaceCapillariesValuesIfEqualOld(fieldEnumCode, oldValue, newValue))
				;
			saveMCCapillaries_Only();
			break;
		default:
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
		File outputfile = new File(getReferenceImageFullName());
		RenderedImage image = ImageUtil.toRGBImage(referenceImage);
		return ImageUtil.save(image, "jpg", outputfile);
	}

	public void cleanPreviousDetectedFliesROIs() {
		ArrayList<ROI2D> list = seqCamData.getSequence().getROI2Ds();
		for (ROI2D roi : list) {
			if (roi.getName().contains("det"))
				seqCamData.getSequence().removeROI(roi);
		}
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

	private boolean zxmlReadDrosoTrack(String filename) {
		if (filename == null) {
			filename = getXML_MS96_cages_Location(cages.ID_MS96_cages_XML);
			if (filename == null)
				return false;
		}
		return cages.xmlReadCagesFromFileNoQuestion(filename);
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
			for (Spot spot : cage.spotsArray.getSpotsList()) {
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
		for (Cage cage : cages.cagesList)
			for (Spot spot : cage.spotsArray.getSpotsList())
				addValueIfUnique(spot.getField(fieldEnumCode), textList);
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
		seqCamData.removeROIsContainingString("cage");
		cages.transferCagesToSequenceAsROIs(seqCamData);
	}

	public void transferSpotsROI_toSequence() {
		seqCamData.removeROIsContainingString("spot");
		cages.transferCageSpotsToSequenceAsROIs(seqCamData);
	}

	public boolean saveCagesArray_File() {
		cages.transferROIsFromSequenceToCages(seqCamData);
		save_MS96_cages();
		return save_MS96_spotsMeasures();
	}

	public boolean saveSpotsArray_file() {
		cages.transferROIsFromSequenceToCageSpots(seqCamData);
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

	public SequenceKymos getSeqKymos() {
		if (seqKymos == null)
			seqKymos = new SequenceKymos();
		return seqKymos;
	}

	public void setSeqKymos(SequenceKymos seqKymos) {
		this.seqKymos = seqKymos;
	}

	public String getKymosBinFullDirectory() {
		String filename = resultsDirectory;
		if (binDirectory != null)
			filename += File.separator + binDirectory;
		return filename;
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

	// ------------------------------

	public boolean loadMCCapillaries_Only() {
		String mcCapillaryFileName = findFile_3Locations(capillaries.getXMLNameToAppend(), EXPT_DIRECTORY,
				BIN_DIRECTORY, IMG_DIRECTORY);
		if (mcCapillaryFileName == null && seqCamData != null)
			return xmlLoadOldCapillaries();

		boolean flag = capillaries.loadMCCapillaries_Descriptors(mcCapillaryFileName);
		if (capillaries.getCapillariesList().size() < 1)
			flag = xmlLoadOldCapillaries();

		// load MCcapillaries description of experiment
		if (prop.ffield_boxID.contentEquals("..") && prop.ffield_experiment.contentEquals("..")
				&& prop.field_comment1.contentEquals("..") && prop.field_comment2.contentEquals("..")
				&& prop.field_sex.contentEquals("..") && prop.field_strain.contentEquals("..")) {
			prop.ffield_boxID = capillaries.getCapillariesDescription().getOld_boxID();
			prop.ffield_experiment = capillaries.getCapillariesDescription().getOld_experiment();
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
		String xmlCapillaryFileName = findFile_3Locations(capillaries.getXMLNameToAppend(), EXPT_DIRECTORY,
				BIN_DIRECTORY, IMG_DIRECTORY);
		boolean flag1 = capillaries.loadMCCapillaries_Descriptors(xmlCapillaryFileName);
		String kymosImagesDirectory = getKymosBinFullDirectory();
		boolean flag2 = capillaries.load_Capillaries(kymosImagesDirectory);
		if (flag1 & flag2) {
			// TODO: Add loadListOfPotentialKymographsFromCapillaries method to
			// SequenceKymos
			// For now, this functionality may need to be implemented
		}
		return flag1 & flag2;
	}

	private boolean xmlLoadOldCapillaries() {
		String filename = findFile_3Locations("capillarytrack.xml", IMG_DIRECTORY, EXPT_DIRECTORY, BIN_DIRECTORY);
		if (capillaries.xmlLoadOldCapillaries_Only(filename)) {
			saveMCCapillaries_Only();
			saveCapillaries();
			try {
				Files.delete(Paths.get(filename));
			} catch (IOException e) {
				Logger.warn("Experiment:xmlLoadOldCapillaries() Failed to delete old file: " + filename, e);
			}
			return true;
		}

		filename = findFile_3Locations("roislines.xml", IMG_DIRECTORY, EXPT_DIRECTORY, BIN_DIRECTORY);
		if (xmlReadCamDataROIs(filename)) {
			xmlReadRoiLineParameters(filename);
			try {
				Files.delete(Paths.get(filename));
			} catch (IOException e) {
				Logger.warn("Experiment:xmlLoadOldCapillaries() Failed to delete old file: " + filename, e);
			}
			return true;
		}
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
		String xmlCapillaryFileName = resultsDirectory + File.separator + capillaries.getXMLNameToAppend();
		transferExpDescriptorsToCapillariesDescriptors();
		return capillaries.xmlSaveCapillaries_Descriptors(xmlCapillaryFileName);
	}

	private void transferExpDescriptorsToCapillariesDescriptors() {
		capillaries.getCapillariesDescription().setOld_boxID(prop.ffield_boxID);
		capillaries.getCapillariesDescription().setOld_experiment(prop.ffield_experiment);
		capillaries.getCapillariesDescription().setOld_comment1(prop.field_comment1);
		capillaries.getCapillariesDescription().setOld_comment2(prop.field_comment2);
		capillaries.getCapillariesDescription().setOld_strain(prop.field_strain);
		capillaries.getCapillariesDescription().setOld_sex(prop.field_sex);
		capillaries.getCapillariesDescription().setOld_cond1(prop.field_stim2);
		capillaries.getCapillariesDescription().setOld_cond2(prop.field_conc2);
	}

	public boolean loadCapillaries() {
		return capillaries.load_Capillaries(getKymosBinFullDirectory());
	}

	public boolean saveCapillaries() {
		return capillaries.save_Capillaries(getKymosBinFullDirectory());
	}

	public boolean loadCageMeasures() {
		String pathToMeasures = getResultsDirectory() + File.separator + "CagesMeasures.csv";
		File f = new File(pathToMeasures);
		if (!f.exists())
			moveCageMeasuresToExperimentDirectory(pathToMeasures);

		boolean flag = cages.load_Cages(getResultsDirectory());
		if (flag & seqCamData.getSequence() != null)
			cages.cagesToROIs(seqCamData);
		return flag;
	}

	private boolean moveCageMeasuresToExperimentDirectory(String pathToMeasures) {
		boolean flag = false;
		String pathToOldCsv = getKymosBinFullDirectory() + File.separator + "CagesMeasures.csv";
		File fileToMove = new File(pathToOldCsv);
		if (fileToMove.exists())
			flag = fileToMove.renameTo(new File(pathToMeasures));
		return flag;
	}

	public boolean saveCageMeasures() {
		return cages.save_Cages(getResultsDirectory());
	}

	public void saveCageAndMeasures() {
		cages.cagesFromROIs(seqCamData);
		saveCageMeasures();
	}

	public boolean adjustCapillaryMeasuresDimensions() {
		if (seqKymos.getImageWidthMax() < 1) {
			seqKymos.setImageWidthMax(seqKymos.getSequence().getSizeX());
			if (seqKymos.getImageWidthMax() < 1)
				return false;
		}
		int imageWidth = seqKymos.getImageWidthMax();
		capillaries.adjustToImageWidth(imageWidth);
		seqKymos.getSequence().removeAllROI();
		seqKymos.transferCapillariesMeasuresToKymos(capillaries);
		return true;
	}

	public boolean cropCapillaryMeasuresDimensions() {
		if (seqKymos.getImageWidthMax() < 1) {
			seqKymos.setImageWidthMax(seqKymos.getSequence().getSizeX());
			if (seqKymos.getImageWidthMax() < 1)
				return false;
		}
		int imageWidth = seqKymos.getImageWidthMax();
		capillaries.cropToImageWidth(imageWidth);
		seqKymos.getSequence().removeAllROI();
		seqKymos.transferCapillariesMeasuresToKymos(capillaries);
		return true;
	}

	public boolean saveCapillariesMeasures(String directory) {
		boolean flag = false;
		if (seqKymos != null && seqKymos.getSequence() != null) {
			seqKymos.validateRois();
			seqKymos.transferKymosRoisToCapillaries_Measures(capillaries);
			flag = capillaries.save_Capillaries(directory);
		}
		return flag;
	}

	public void dispatchCapillariesToCages() {
		for (Cage cage : cages.getCageList()) {
			cage.clearCapillaryList();
		}

		for (plugins.fmp.multicafe.fmp_experiment.capillaries.Capillary cap : capillaries.getCapillariesList()) {
			int cageID = cap.getCageIndexFromRoiName();
			Cage cage = cages.getCageFromID(cageID);
			if (cage == null) {
				cage = new Cage();
				cage.getProperties().setCageID(cageID);
				cages.getCageList().add(cage);
			}
			cage.addCapillaryIfUnique(cap);
		}
	}

	public boolean loadKymographs() {
		if (getSeqKymos() == null)
			setSeqKymos(new SequenceKymos());

		// Use KymographService to get list of potential kymographs from capillaries
		plugins.fmp.multicafe.service.KymographService kymoService = new plugins.fmp.multicafe.service.KymographService();
		List<plugins.fmp.multicafe.fmp_experiment.ImageFileDescriptor> myList = kymoService
				.loadListOfPotentialKymographsFromCapillaries(getKymosBinFullDirectory(), capillaries);

		// Filter to get existing file names
		plugins.fmp.multicafe.fmp_experiment.ImageFileDescriptor.getExistingFileNames(myList);

		// Convert to experiment1 ImageFileDescriptor format
		List<plugins.fmp.multicafe.fmp_experiment.sequence.ImageFileDescriptor> newList = new ArrayList<plugins.fmp.multicafe.fmp_experiment.sequence.ImageFileDescriptor>();
		for (plugins.fmp.multicafe.fmp_experiment.ImageFileDescriptor oldDesc : myList) {
			if (oldDesc.fileName != null && oldDesc.exists) {
				plugins.fmp.multicafe.fmp_experiment.sequence.ImageFileDescriptor newDesc = new plugins.fmp.multicafe.fmp_experiment.sequence.ImageFileDescriptor();
				newDesc.fileName = oldDesc.fileName;
				newDesc.exists = oldDesc.exists;
				newDesc.imageHeight = oldDesc.imageHeight;
				newDesc.imageWidth = oldDesc.imageWidth;
				newList.add(newDesc);
			}
		}

		if (newList.isEmpty())
			return false;

		// Load images using the new API
		return getSeqKymos().loadKymographImagesFromList(newList, true);
	}

	public boolean loadCamDataCapillaries() {
		// TODO: Adapt ExperimentService.loadCamDataCapillaries to work with
		// experiment1.Experiment
		// For now, return false - this needs to be implemented
		return false;
	}

	public boolean openMeasures(boolean loadCapillaries, boolean loadDrosoPositions) {
		// TODO: Implement full logic similar to old ExperimentPersistence.openMeasures
		// For now, delegate to appropriate load methods
		boolean flag = true;
		if (loadCapillaries) {
			flag = loadMCCapillaries_Only();
		}
		if (loadDrosoPositions) {
			flag &= load_MS96_cages();
		}
		return flag;
	}

	private boolean replaceCapillariesValuesIfEqualOld(EnumXLSColumnHeader fieldEnumCode, String oldValue,
			String newValue) {
		if (capillaries.getCapillariesList().size() == 0)
			loadMCCapillaries_Only();
		// Convert new enum to old enum for Capillary compatibility
		EnumXLSColumnHeader oldEnum = convertToOldEnum(fieldEnumCode);
		if (oldEnum == null)
			return false;
		boolean flag = false;
		for (Capillary cap : capillaries.getCapillariesList()) {
			if (cap.getCapillaryField(oldEnum).equals(oldValue)) {
				cap.setCapillaryField(oldEnum, newValue);
				flag = true;
			}
		}
		return flag;
	}

	private void addCapillariesValues(EnumXLSColumnHeader fieldEnumCode, List<String> textList) {
		if (capillaries.getCapillariesList().size() == 0)
			loadMCCapillaries_Only();
//		// Convert new enum to old enum for Capillary compatibility
//		EnumXLSColumnHeader oldEnum = convertToOldEnum(fieldEnumCode);
//		if (oldEnum == null)
//			return;
		for (Capillary cap : capillaries.getCapillariesList())
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
