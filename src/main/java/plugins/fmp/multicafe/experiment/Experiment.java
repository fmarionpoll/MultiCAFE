package plugins.fmp.multicafe.experiment;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;

import icy.roi.ROI2D;
import icy.sequence.Sequence;
import icy.util.XMLUtil;
import plugins.fmp.multicafe.experiment1.cages.Cage;
import plugins.fmp.multicafe.experiment.cages.CagesArray;
import plugins.fmp.multicafe.experiment1.capillaries.Capillaries;
import plugins.fmp.multicafe.experiment1.capillaries.Capillary;
import plugins.fmp.multicafe.experiment1.sequence.SequenceCamData;
import plugins.fmp.multicafe.experiment1.sequence.SequenceKymos;
import plugins.fmp.multicafe.experiment.sequence.ExperimentTimeManager;
import plugins.fmp.multicafe.tools.Directories;
import plugins.fmp.multicafe.tools.Logger;
import plugins.fmp.multicafe.tools.ROI2D.ROI2DUtilities;
import plugins.fmp.multicafe.tools.toExcel.EnumXLSColumnHeader;

public class Experiment {
	public final static String RESULTS = "results";
	public final static String BIN = "bin_";

	private String imagesDirectory = null;
	private String experimentDirectory = null;
	private String binSubDirectory = null;

	private SequenceCamData seqCamData = null;
	private SequenceKymos seqKymos = null;
	private Sequence seqReference = null;
	private Capillaries capillaries = new Capillaries();
	private CagesArray cages = new CagesArray();

	private ExperimentTimeManager timeManager = new ExperimentTimeManager();

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

	private String boxID = new String("..");
	private String experiment = new String("..");
	private String comment1 = new String("..");
	private String comment2 = new String("..");
	private String strain = new String("..");
	private String sex = new String("..");
	private String condition1 = new String("..");
	private String condition2 = new String("..");

	public int col = -1;
	public Experiment chainToPreviousExperiment = null;
	public Experiment chainToNextExperiment = null;
	public long chainImageFirst_ms = 0;
	public int experimentID = 0;

	private ExperimentPersistence persistence = new ExperimentPersistence();

	private final static int EXPT_DIRECTORY = 1;
	private final static int IMG_DIRECTORY = 2;
	private final static int BIN_DIRECTORY = 3;

	// ----------------------------------
	// ----------------------------------

	public Experiment() {
		seqCamData = new SequenceCamData();
		seqKymos = new SequenceKymos();
	}

	public Experiment(String expDirectory) {
		seqCamData = new SequenceCamData();
		seqKymos = new SequenceKymos();
		this.experimentDirectory = expDirectory;
	}

	public Experiment(SequenceCamData seqCamData) {
		this.seqCamData = seqCamData;
		this.seqKymos = new SequenceKymos();
		experimentDirectory = this.seqCamData.getImagesDirectory() + File.separator + RESULTS;
		getFileIntervalsFromSeqCamData();
		persistence.xmlLoadExperiment(this,
				concatenateExptDirectoryWithSubpathAndName(null, ExperimentPersistence.ID_MCEXPERIMENT_XML));
	}

	public Experiment(ExperimentDirectories eADF) {
		String imgDir = null;
		if (eADF.cameraImagesList.size() > 0)
			imgDir = eADF.cameraImagesList.get(0);
		imagesDirectory = Directories.getDirectoryFromName(imgDir);
		experimentDirectory = eADF.resultsDirectory;
		String binDirectory = experimentDirectory + File.separator + eADF.binSubDirectory;
		Path binDirectoryPath = Paths.get(binDirectory);
		Path lastSubPath = binDirectoryPath.getName(binDirectoryPath.getNameCount() - 1);
		binSubDirectory = lastSubPath.toString();

		seqCamData = new SequenceCamData(eADF.cameraImagesList);
		getFileIntervalsFromSeqCamData();
		seqKymos = new SequenceKymos(eADF.kymosImagesList);

		persistence.xmlLoadExperiment(this,
				concatenateExptDirectoryWithSubpathAndName(null, ExperimentPersistence.ID_MCEXPERIMENT_XML));
	}

	// ----------------------------------

	public String getExperimentDirectory() {
		return experimentDirectory;
	}

	public void setExperimentDirectory(String fileName) {
		experimentDirectory = ExperimentDirectories.getParentIf(fileName, BIN);
	}

	public String toString() {
		return experimentDirectory;
	}

	public String getKymosBinFullDirectory() {
		String filename = experimentDirectory;
		if (binSubDirectory != null)
			filename += File.separator + binSubDirectory;
		return filename;
	}

	public void setBinSubDirectory(String bin) {
		binSubDirectory = bin;
	}

	public String getBinSubDirectory() {
		return binSubDirectory;
	}

	public boolean createDirectoryIfDoesNotExist(String directory) {
		Path pathDir = Paths.get(directory);
		if (Files.notExists(pathDir)) {
			try {
				Files.createDirectory(pathDir);
			} catch (IOException e) {
				Logger.error("Experiment:createDirectoryIfDoesNotExist() Creating directory failed: " + directory, e,
						true);
				return false;
			}
		}
		return true;
	}

	public void checkKymosDirectory(String kymosSubDirectory) {
		if (kymosSubDirectory == null) {
			List<String> listTIFFlocations = Directories
					.getSortedListOfSubDirectoriesWithTIFF(getExperimentDirectory());
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
		setBinSubDirectory(kymosSubDirectory);
	}

	public void closeSequences() {
		new plugins.fmp.multicafe.service.ExperimentService().closeSequences(this);
	}

	public boolean openMeasures(boolean loadCapillaries, boolean loadDrosoPositions) {
		return persistence.openMeasures(this, loadCapillaries, loadDrosoPositions);
	}

	private String getRootWithNoResultNorBinString(String directoryName) {
		String name = directoryName.toLowerCase();
		while (name.contains(RESULTS) || name.contains(BIN))
			name = Paths.get(experimentDirectory).getParent().toString();
		return name;
	}

	public SequenceCamData openSequenceCamData() {
		return new plugins.fmp.multicafe.service.ExperimentService().openSequenceCamData(this);
	}

	public boolean loadCamDataImages() {
		return new plugins.fmp.multicafe.service.ExperimentService().loadCamDataImages(this);
	}

	public boolean loadCamDataCapillaries() {
		return new plugins.fmp.multicafe.service.ExperimentService().loadCamDataCapillaries(this);
	}

	public void getFileIntervalsFromSeqCamData() {
		timeManager.getFileIntervalsFromSeqCamData(seqCamData, imagesDirectory);
	}

	public void loadFileIntervalsFromSeqCamData() {
		timeManager.loadFileIntervalsFromSeqCamData(seqCamData, imagesDirectory);
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

	public String getBinNameFromKymoFrameStep() {
		return timeManager.getBinNameFromKymoFrameStep();
	}

	public String getDirectoryToSaveResults() {
		Path dir = Paths.get(experimentDirectory);
		if (binSubDirectory != null)
			dir = dir.resolve(binSubDirectory);
		String directory = dir.toAbsolutePath().toString();
		if (!createDirectoryIfDoesNotExist(directory))
			directory = null;
		return directory;
	}

	// -------------------------------

	public boolean xmlLoad_MCExperiment() {
		return persistence.xmlLoad_MCExperiment(this);
	}

	public boolean xmlSave_MCExperiment() {
		return persistence.xmlSave_MCExperiment(this);
	}

	public boolean loadKymographs() {
		return new plugins.fmp.multicafe.service.ExperimentService().loadKymographs(this);
	}

	// ------------------------------------------------

	public boolean loadMCCapillaries_Only() {
		String mcCapillaryFileName = findFile_3Locations(capillaries.getXMLNameToAppend(), EXPT_DIRECTORY,
				BIN_DIRECTORY, IMG_DIRECTORY);
		if (mcCapillaryFileName == null && seqCamData != null)
			return xmlLoadOldCapillaries();

		boolean flag = capillaries.loadMCCapillaries_Descriptors(mcCapillaryFileName);
		if (capillaries.getCapillariesList().size() < 1)
			flag = xmlLoadOldCapillaries();

		// load MCcapillaries description of experiment
		if (boxID.contentEquals("..") && experiment.contentEquals("..") && comment1.contentEquals("..")
				&& comment2.contentEquals("..") && sex.contentEquals("..") && strain.contentEquals("..")) {
			boxID = capillaries.getCapillariesDescription().getOld_boxID();
			experiment = capillaries.getCapillariesDescription().getOld_experiment();
			comment1 = capillaries.getCapillariesDescription().getOld_comment1();
			comment2 = capillaries.getCapillariesDescription().getOld_comment2();
			sex = capillaries.getCapillariesDescription().getOld_sex();
			strain = capillaries.getCapillariesDescription().getOld_strain();
			condition1 = capillaries.getCapillariesDescription().getOld_cond1();
			condition2 = capillaries.getCapillariesDescription().getOld_cond2();
		}
		return flag;
	}

	public boolean loadMCCapillaries() {
		String xmlCapillaryFileName = findFile_3Locations(capillaries.getXMLNameToAppend(), EXPT_DIRECTORY,
				BIN_DIRECTORY, IMG_DIRECTORY);
		boolean flag1 = capillaries.loadMCCapillaries_Descriptors(xmlCapillaryFileName);
		String kymosImagesDirectory = getKymosBinFullDirectory();
		boolean flag2 = capillaries.load_Capillaries(kymosImagesDirectory);
		if (flag1 & flag2)
			seqKymos.loadListOfPotentialKymographsFromCapillaries(kymosImagesDirectory, capillaries);
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
		String xmlCapillaryFileName = experimentDirectory + File.separator + capillaries.getXMLNameToAppend();
		transferExpDescriptorsToCapillariesDescriptors();
		return capillaries.xmlSaveCapillaries_Descriptors(xmlCapillaryFileName);
	}

	public boolean loadCapillaries() {
		return capillaries.load_Capillaries(getKymosBinFullDirectory());
	}

	public boolean saveCapillaries() {
		return capillaries.save_Capillaries(getKymosBinFullDirectory());
	}

	public boolean loadCageMeasures() {
		String pathToMeasures = getExperimentDirectory() + File.separator + "CagesMeasures.csv";
		File f = new File(pathToMeasures);
		if (!f.exists())
			moveCageMeasuresToExperimentDirectory(pathToMeasures);

		boolean flag = cages.load_Cages(getExperimentDirectory());
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
		return cages.save_Cages(getExperimentDirectory());
	}

	public void saveCageAndMeasures() {
		cages.cagesFromROIs(seqCamData);
		saveCageMeasures();
	}

	// ----------------------------------

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
		timeManager.setFirstImage_FileTime(fileTimeImageFirst);
	}

	public void setFileTimeImageLast(FileTime fileTimeImageLast) {
		timeManager.setLastImage_FileTime(fileTimeImageLast);
	}

	public int getSeqCamSizeT() {
		int lastFrame = 0;
		if (seqCamData != null)
			lastFrame = seqCamData.getnTotalFrames() - 1;
		return lastFrame;
	}

	public String getExperimentField(EnumXLSColumnHeader fieldEnumCode) {
		String strField = null;
		switch (fieldEnumCode) {
		case EXP_PATH:
			strField = getPath();
			break;
		case EXP_DATE:
			strField = getDate();
			break;
		case EXP_CAM:
			strField = getCam();
			break;
		case EXP_STIM:
			strField = comment1;
			break;
		case EXP_CONC:
			strField = comment2;
			break;
		case EXP_EXPT:
			strField = experiment;
			break;
		case EXP_BOXID:
			strField = boxID;
			break;
		case EXP_STRAIN:
			strField = strain;
			break;
		case EXP_SEX:
			strField = sex;
			break;
		case EXP_COND1:
			strField = condition1;
			break;
		case EXP_COND2:
			strField = condition2;
			break;
		default:
			break;
		}
		return strField;
	}

	private String getPath() {
		String filename = getExperimentDirectory();
		if (filename == null)
			filename = seqCamData.getImagesDirectory();
		Path path = Paths.get(filename);
		return path.toString();
	}

	private String getDate() {
		SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy");
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

	public void getFieldValues(EnumXLSColumnHeader fieldEnumCode, List<String> textList) {
		switch (fieldEnumCode) {
		case EXP_STIM:
		case EXP_CONC:
		case EXP_EXPT:
		case EXP_BOXID:
		case EXP_STRAIN:
		case EXP_SEX:
		case EXP_COND1:
		case EXP_COND2:
			addValue(getExperimentField(fieldEnumCode), textList);
			break;
		case CAP_STIM:
		case CAP_CONC:
			addCapillariesValues(fieldEnumCode, textList);
			break;
		default:
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
		setExperimentFieldNoTest(EnumXLSColumnHeader.EXP_STIM,
				expSource.getExperimentField(EnumXLSColumnHeader.EXP_STIM));
		setExperimentFieldNoTest(EnumXLSColumnHeader.EXP_CONC,
				expSource.getExperimentField(EnumXLSColumnHeader.EXP_CONC));
		setExperimentFieldNoTest(EnumXLSColumnHeader.EXP_STRAIN,
				expSource.getExperimentField(EnumXLSColumnHeader.EXP_STRAIN));
		setExperimentFieldNoTest(EnumXLSColumnHeader.EXP_SEX,
				expSource.getExperimentField(EnumXLSColumnHeader.EXP_SEX));
		setExperimentFieldNoTest(EnumXLSColumnHeader.EXP_COND1,
				expSource.getExperimentField(EnumXLSColumnHeader.EXP_COND1));
		setExperimentFieldNoTest(EnumXLSColumnHeader.EXP_COND2,
				expSource.getExperimentField(EnumXLSColumnHeader.EXP_COND2));
	}

	public void setExperimentFieldNoTest(EnumXLSColumnHeader fieldEnumCode, String newValue) {
		switch (fieldEnumCode) {
		case EXP_STIM:
			comment1 = newValue;
			break;
		case EXP_CONC:
			comment2 = newValue;
			break;
		case EXP_EXPT:
			experiment = newValue;
			break;
		case EXP_BOXID:
			boxID = newValue;
			break;
		case EXP_STRAIN:
			strain = newValue;
			break;
		case EXP_SEX:
			sex = newValue;
			break;
		case EXP_COND1:
			condition1 = newValue;
			break;
		case EXP_COND2:
			condition2 = newValue;
			break;
		default:
			break;
		}
	}

	public void replaceFieldValue(EnumXLSColumnHeader fieldEnumCode, String oldValue, String newValue) {
		switch (fieldEnumCode) {
		case EXP_STIM:
		case EXP_CONC:
		case EXP_EXPT:
		case EXP_BOXID:
		case EXP_STRAIN:
		case EXP_SEX:
		case EXP_COND1:
		case EXP_COND2:
			replaceExperimentFieldIfEqualOld(fieldEnumCode, oldValue, newValue);
			break;
		case CAP_STIM:
		case CAP_CONC:
			if (replaceCapillariesValuesIfEqualOld(fieldEnumCode, oldValue, newValue))
				;
			saveMCCapillaries_Only();
			break;
		default:
			break;
		}
	}

	// --------------------------------------------

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

		for (Capillary cap : capillaries.getCapillariesList()) {
			int cageID = cap.getCageIndexFromRoiName();
			Cage cage = cages.getCageFromID(cageID);
			if (cage == null) {
				cage = new Cage();
				cage.setCageID(cageID);
				cages.getCageList().add(cage);
			}
			cage.addCapillaryIfUnique(cap);
		}
	}

	public void cleanPreviousDetectedFliesROIs() {
		ArrayList<ROI2D> list = seqCamData.getSequence().getROI2Ds();
		for (ROI2D roi : list) {
			if (roi.getName().contains("det"))
				seqCamData.getSequence().removeROI(roi);
		}
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
		for (Cage cell : cages.getCageList()) {
			cell.transferRoisToPositions(detectedROIsList);
		}
	}

	// ----------------------------------

	private int getBinStepFromDirectoryName(String resultsPath) {
		int step = -1;
		if (resultsPath.contains(BIN)) {
			if (resultsPath.length() < (BIN.length() + 1)) {
				step = (int) timeManager.getKymoBin_ms();
			} else {
				step = Integer.valueOf(resultsPath.substring(BIN.length())) * 1000;
			}
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
			imagesDirectory = getRootWithNoResultNorBinString(experimentDirectory);
			xmlFullFileName = imagesDirectory + File.separator + xmlFileName;
			break;

		case BIN_DIRECTORY:
			// any directory (below)
			Path dirPath = Paths.get(experimentDirectory);
			List<Path> subFolders = Directories.getAllSubPathsOfDirectory(experimentDirectory, 1);
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
			xmlFullFileName = experimentDirectory + xmlFullFileName;
			break;
		}

		// current directory
		if (xmlFullFileName != null && fileExists(xmlFullFileName)) {
			if (item == IMG_DIRECTORY) {
				imagesDirectory = getRootWithNoResultNorBinString(experimentDirectory);
				ExperimentDirectories.moveAndRename(xmlFileName, imagesDirectory, xmlFileName, experimentDirectory);
				xmlFullFileName = experimentDirectory + xmlFullFileName;
			}
			return xmlFullFileName;
		}
		return null;
	}

	private boolean fileExists(String fileName) {
		File f = new File(fileName);
		return (f.exists() && !f.isDirectory());
	}

	private boolean replaceCapillariesValuesIfEqualOld(EnumXLSColumnHeader fieldEnumCode, String oldValue,
			String newValue) {
		if (capillaries.getCapillariesList().size() == 0)
			loadMCCapillaries_Only();
		boolean flag = false;
		for (Capillary cap : capillaries.getCapillariesList()) {
			if (cap.getCapillaryField(fieldEnumCode).equals(oldValue)) {
				cap.setCapillaryField(fieldEnumCode, newValue);
				flag = true;
			}
		}
		return flag;
	}

	private String concatenateExptDirectoryWithSubpathAndName(String subpath, String name) {
		if (subpath != null)
			return experimentDirectory + File.separator + subpath + File.separator + name;
		else
			return experimentDirectory + File.separator + name;
	}

	private void addCapillariesValues(EnumXLSColumnHeader fieldEnumCode, List<String> textList) {
		if (capillaries.getCapillariesList().size() == 0)
			loadMCCapillaries_Only();
		for (Capillary cap : capillaries.getCapillariesList())
			addValue(cap.getCapillaryField(fieldEnumCode), textList);
	}

	private void addValue(String text, List<String> textList) {
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

	private void transferExpDescriptorsToCapillariesDescriptors() {
		capillaries.getCapillariesDescription().setOld_boxID(boxID);
		capillaries.getCapillariesDescription().setOld_experiment(experiment);
		capillaries.getCapillariesDescription().setOld_comment1(comment1);
		capillaries.getCapillariesDescription().setOld_comment2(comment2);
		capillaries.getCapillariesDescription().setOld_strain(strain);
		capillaries.getCapillariesDescription().setOld_sex(sex);
		capillaries.getCapillariesDescription().setOld_cond1(condition1);
		capillaries.getCapillariesDescription().setOld_cond2(condition2);
	}

	public SequenceCamData getSeqCamData() {
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

	public Capillaries getCapillaries() {
		return capillaries;
	}

	public void setCapillaries(Capillaries capillaries) {
		this.capillaries = capillaries;
	}

	public String getImagesDirectory() {
		return imagesDirectory;
	}

	public void setImagesDirectory(String imagesDirectory) {
		this.imagesDirectory = imagesDirectory;
	}

	public String getBoxID() {
		return boxID;
	}

	public void setBoxID(String boxID) {
		this.boxID = boxID;
	}

	public String getExperiment() {
		return experiment;
	}

	public void setExperiment(String experiment) {
		this.experiment = experiment;
	}

	public String getComment1() {
		return comment1;
	}

	public void setComment1(String comment1) {
		this.comment1 = comment1;
	}

	public String getComment2() {
		return comment2;
	}

	public void setComment2(String comment2) {
		this.comment2 = comment2;
	}

	public String getStrain() {
		return strain;
	}

	public void setStrain(String strain) {
		this.strain = strain;
	}

	public String getSex() {
		return sex;
	}

	public void setSex(String sex) {
		this.sex = sex;
	}

	public String getCondition1() {
		return condition1;
	}

	public void setCondition1(String condition1) {
		this.condition1 = condition1;
	}

	public String getCondition2() {
		return condition2;
	}

	public void setCondition2(String condition2) {
		this.condition2 = condition2;
	}

}
