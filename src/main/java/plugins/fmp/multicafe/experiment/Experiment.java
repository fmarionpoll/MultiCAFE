package plugins.fmp.multicafe.experiment;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
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
import org.w3c.dom.Node;

import icy.image.IcyBufferedImage;
import icy.image.ImageUtil;
import icy.roi.ROI2D;
import icy.sequence.Sequence;
import icy.util.XMLUtil;
import plugins.fmp.multicafe.experiment.cages.Cage;
import plugins.fmp.multicafe.experiment.cages.Cages;
import plugins.fmp.multicafe.experiment.capillaries.Capillaries;
import plugins.fmp.multicafe.experiment.capillaries.Capillary;
import plugins.fmp.multicafe.tools.Directories;
import plugins.fmp.multicafe.tools.ImageTransform.ImageTransformEnums;
import plugins.fmp.multicafe.tools.ImageTransform.ImageTransformInterface;
import plugins.fmp.multicafe.tools.Logger;
import plugins.fmp.multicafe.tools.ROI2D.ROI2DUtilities;
import plugins.fmp.multicafe.tools.toExcel.EnumXLSColumnHeader;

public class Experiment {
	public final static String RESULTS = "results";
	public final static String BIN = "bin_";

	private String strImagesDirectory = null;
	private String strExperimentDirectory = null;
	private String strBinSubDirectory = null;

	private SequenceCamData seqCamData = null;
	private SequenceKymos seqKymos = null;
	public Sequence seqReference = null;
	private Capillaries capillaries = new Capillaries();
	public Cages cages = new Cages();

	public FileTime firstImage_FileTime;
	public FileTime lastImage_FileTime;

	// __________________________________________________

	public long camImageFirst_ms = -1;
	public long camImageLast_ms = -1;
	public long camImageBin_ms = -1;
	public long[] camImages_ms = null;

	public long binT0 = 0;
	public long kymoFirst_ms = 0;
	public long kymoLast_ms = 0;
	public long kymoBin_ms = 60000;

	// _________________________________________________

	private String field_boxID = new String("..");
	private String field_experiment = new String("..");
	private String field_comment1 = new String("..");
	private String field_comment2 = new String("..");
	private String field_strain = new String("..");
	private String field_sex = new String("..");
	private String field_cond1 = new String("..");
	private String field_cond2 = new String("..");

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
		this.strExperimentDirectory = expDirectory;
	}

	public Experiment(SequenceCamData seqCamData) {
		this.seqCamData = seqCamData;
		this.seqKymos = new SequenceKymos();
		strExperimentDirectory = this.seqCamData.getImagesDirectory() + File.separator + RESULTS;
		getFileIntervalsFromSeqCamData();
		persistence.xmlLoadExperiment(this, concatenateExptDirectoryWithSubpathAndName(null, ExperimentPersistence.ID_MCEXPERIMENT_XML));
	}

	public Experiment(ExperimentDirectories eADF) {
		String imgDir = null;
		if (eADF.cameraImagesList.size() > 0)
			imgDir = eADF.cameraImagesList.get(0);
		strImagesDirectory = Directories.getDirectoryFromName(imgDir);
		strExperimentDirectory = eADF.resultsDirectory;
		String binDirectory = strExperimentDirectory + File.separator + eADF.binSubDirectory;
		Path binDirectoryPath = Paths.get(binDirectory);
		Path lastSubPath = binDirectoryPath.getName(binDirectoryPath.getNameCount() - 1);
		strBinSubDirectory = lastSubPath.toString();

		seqCamData = new SequenceCamData(eADF.cameraImagesList);
		getFileIntervalsFromSeqCamData();
		seqKymos = new SequenceKymos(eADF.kymosImagesList);

		persistence.xmlLoadExperiment(this, concatenateExptDirectoryWithSubpathAndName(null, ExperimentPersistence.ID_MCEXPERIMENT_XML));
	}

	// ----------------------------------

	public String getExperimentDirectory() {
		return strExperimentDirectory;
	}

	public void setExperimentDirectory(String fileName) {
		strExperimentDirectory = ExperimentDirectories.getParentIf(fileName, BIN);
	}

	public String toString() {
		return strExperimentDirectory;
	}

	public String getKymosBinFullDirectory() {
		String filename = strExperimentDirectory;
		if (strBinSubDirectory != null)
			filename += File.separator + strBinSubDirectory;
		return filename;
	}

	public void setBinSubDirectory(String bin) {
		strBinSubDirectory = bin;
	}

	public String getBinSubDirectory() {
		return strBinSubDirectory;
	}

	public boolean createDirectoryIfDoesNotExist(String directory) {
		Path pathDir = Paths.get(directory);
		if (Files.notExists(pathDir)) {
			try {
				Files.createDirectory(pathDir);
			} catch (IOException e) {
				Logger.error("Experiment:createDirectoryIfDoesNotExist() Creating directory failed: " + directory, e, true);
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

	public void setImagesDirectory(String name) {
		strImagesDirectory = name;
	}

	public String getImagesDirectory() {
		return strImagesDirectory;
	}

	public void closeSequences() {
		if (seqKymos != null) {
			seqKymos.closeSequence();
		}
		if (seqCamData != null) {
			seqCamData.closeSequence();
		}
		if (seqReference != null) {
			seqReference.close();
		}
	}

	public boolean openMeasures(boolean loadCapillaries, boolean loadDrosoPositions) {
		return persistence.openMeasures(this, loadCapillaries, loadDrosoPositions);
	}

	private String getRootWithNoResultNorBinString(String directoryName) {
		String name = directoryName.toLowerCase();
		while (name.contains(RESULTS) || name.contains(BIN))
			name = Paths.get(strExperimentDirectory).getParent().toString();
		return name;
	}

	private SequenceCamData loadImagesForSequenceCamData(String filename) {
		strImagesDirectory = ExperimentDirectories.getImagesDirectoryAsParentFromFileName(filename);
		List<String> imagesList = ExperimentDirectories.getV2ImagesListFromPath(strImagesDirectory);
		imagesList = ExperimentDirectories.keepOnlyAcceptedNames_List(imagesList, "jpg");
		if (imagesList.size() < 1) {
			seqCamData = null;
		} else {
			seqCamData = new SequenceCamData();
			seqCamData.setImagesList(imagesList);
			seqCamData.attachSequence(seqCamData.loadSequenceFromImagesList(imagesList));
		}
		return seqCamData;
	}

	public boolean loadCamDataImages() {
		if (seqCamData != null)
			seqCamData.loadImages();

		return (seqCamData != null && seqCamData.seq != null);
	}

	public boolean loadCamDataCapillaries() {
		loadMCCapillaries_Only();
		if (seqCamData != null && seqCamData.seq != null)
			capillaries.transferCapillaryRoiToSequence(seqCamData.seq);

		return (seqCamData != null && seqCamData.seq != null);
	}

	public SequenceCamData openSequenceCamData() {
		loadImagesForSequenceCamData(strImagesDirectory);
		if (seqCamData != null) {
			xmlLoad_MCExperiment();
			getFileIntervalsFromSeqCamData();
		}
		return seqCamData;
	}

	public void getFileIntervalsFromSeqCamData() {
		if (seqCamData != null && (camImageFirst_ms < 0 || camImageLast_ms < 0 || camImageBin_ms < 0)) {
			loadFileIntervalsFromSeqCamData();
		}
	}

	public void loadFileIntervalsFromSeqCamData() {
		if (seqCamData != null) {
			seqCamData.setImagesDirectory(strImagesDirectory);
			firstImage_FileTime = seqCamData.getFileTimeFromStructuredName(0);
			lastImage_FileTime = seqCamData.getFileTimeFromStructuredName(seqCamData.nTotalFrames - 1);
			if (firstImage_FileTime != null && lastImage_FileTime != null) {
				camImageFirst_ms = firstImage_FileTime.toMillis();
				camImageLast_ms = lastImage_FileTime.toMillis();
				camImageBin_ms = (camImageLast_ms - camImageFirst_ms) / (seqCamData.nTotalFrames - 1);
				if (camImageBin_ms == 0)
					Logger.warn("Experiment:loadFileIntervalsFromSeqCamData() error / file interval size");
			} else {
				Logger.warn("Experiment:loadFileIntervalsFromSeqCamData() error / file intervals of "
						+ seqCamData.getImagesDirectory());
			}
		}
	}

	public long[] build_MsTimeIntervalsArray_From_SeqCamData_FileNamesList(long firstImage_ms) {
		camImages_ms = new long[seqCamData.nTotalFrames];
		for (int i = 0; i < seqCamData.nTotalFrames; i++) {
			FileTime image_FileTime = seqCamData.getFileTimeFromStructuredName(i);
			long image_ms = image_FileTime.toMillis() - firstImage_ms;
			camImages_ms[i] = image_ms;
		}
		return camImages_ms;
	}

	public void initTmsForFlyPositions(long time_start_ms) {
		build_MsTimeIntervalsArray_From_SeqCamData_FileNamesList(time_start_ms);
		cages.initCagesTmsForFlyPositions(camImages_ms);
	}

	public int findNearestIntervalWithBinarySearch(long value, int low, int high) {
		int result = -1;
		if (high - low > 1) {
			int mid = (low + high) / 2;
			if (camImages_ms[mid] > value)
				result = findNearestIntervalWithBinarySearch(value, low, mid);
			else if (camImages_ms[mid] < value)
				result = findNearestIntervalWithBinarySearch(value, mid, high);
			else
				result = mid;
		} else
			result = Math.abs(value - camImages_ms[low]) < Math.abs(value - camImages_ms[high]) ? low : high;
		return result;
	}

	public String getBinNameFromKymoFrameStep() {
		return BIN + kymoBin_ms / 1000;
	}

	public String getDirectoryToSaveResults() {
		Path dir = Paths.get(strExperimentDirectory);
		if (strBinSubDirectory != null)
			dir = dir.resolve(strBinSubDirectory);
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
		if (seqKymos == null)
			seqKymos = new SequenceKymos();
		List<ImageFileDescriptor> myList = seqKymos
				.loadListOfPotentialKymographsFromCapillaries(getKymosBinFullDirectory(), capillaries);
		ImageFileDescriptor.getExistingFileNames(myList);
		return seqKymos.loadImagesFromList(myList, true);
	}

	// ------------------------------------------------

	public boolean loadMCCapillaries_Only() {
		String mcCapillaryFileName = findFile_3Locations(capillaries.getXMLNameToAppend(), EXPT_DIRECTORY,
				BIN_DIRECTORY, IMG_DIRECTORY);
		if (mcCapillaryFileName == null && seqCamData != null)
			return xmlLoadOldCapillaries();

		boolean flag = capillaries.loadMCCapillaries_Descriptors(mcCapillaryFileName);
		if (capillaries.capillariesList.size() < 1)
			flag = xmlLoadOldCapillaries();

		// load MCcapillaries description of experiment
		if (field_boxID.contentEquals("..") && field_experiment.contentEquals("..")
				&& field_comment1.contentEquals("..") && field_comment2.contentEquals("..")
				&& field_sex.contentEquals("..") && field_strain.contentEquals("..")) {
			field_boxID = capillaries.capillariesDescription.old_boxID;
			field_experiment = capillaries.capillariesDescription.old_experiment;
			field_comment1 = capillaries.capillariesDescription.old_comment1;
			field_comment2 = capillaries.capillariesDescription.old_comment2;
			field_sex = capillaries.capillariesDescription.old_sex;
			field_strain = capillaries.capillariesDescription.old_strain;
			field_cond1 = capillaries.capillariesDescription.old_cond1;
			field_cond2 = capillaries.capillariesDescription.old_cond2;
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
		Sequence seq = seqCamData.seq;
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
				return capillaries.capillariesDescription.xmlLoadCapillaryDescription(doc);
		}
		return false;
	}

	// ---------------------------------------------

	public boolean saveMCCapillaries_Only() {
		String xmlCapillaryFileName = strExperimentDirectory + File.separator + capillaries.getXMLNameToAppend();
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
		if (flag & seqCamData.seq != null)
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
		this.firstImage_FileTime = fileTimeImageFirst;
	}

	public void setFileTimeImageLast(FileTime fileTimeImageLast) {
		this.lastImage_FileTime = fileTimeImageLast;
	}

	public int getSeqCamSizeT() {
		int lastFrame = 0;
		if (seqCamData != null)
			lastFrame = seqCamData.nTotalFrames - 1;
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
			strField = field_comment1;
			break;
		case EXP_CONC:
			strField = field_comment2;
			break;
		case EXP_EXPT:
			strField = field_experiment;
			break;
		case EXP_BOXID:
			strField = field_boxID;
			break;
		case EXP_STRAIN:
			strField = field_strain;
			break;
		case EXP_SEX:
			strField = field_sex;
			break;
		case EXP_COND1:
			strField = field_cond1;
			break;
		case EXP_COND2:
			strField = field_cond2;
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
			field_comment1 = newValue;
			break;
		case EXP_CONC:
			field_comment2 = newValue;
			break;
		case EXP_EXPT:
			field_experiment = newValue;
			break;
		case EXP_BOXID:
			field_boxID = newValue;
			break;
		case EXP_STRAIN:
			field_strain = newValue;
			break;
		case EXP_SEX:
			field_sex = newValue;
			break;
		case EXP_COND1:
			field_cond1 = newValue;
			break;
		case EXP_COND2:
			field_cond2 = newValue;
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
		if (seqKymos.imageWidthMax < 1) {
			seqKymos.imageWidthMax = seqKymos.seq.getSizeX();
			if (seqKymos.imageWidthMax < 1)
				return false;
		}
		int imageWidth = seqKymos.imageWidthMax;
		capillaries.adjustToImageWidth(imageWidth);
		seqKymos.seq.removeAllROI();
		seqKymos.transferCapillariesMeasuresToKymos(capillaries);
		return true;
	}

	public boolean cropCapillaryMeasuresDimensions() {
		if (seqKymos.imageWidthMax < 1) {
			seqKymos.imageWidthMax = seqKymos.seq.getSizeX();
			if (seqKymos.imageWidthMax < 1)
				return false;
		}
		int imageWidth = seqKymos.imageWidthMax;
		capillaries.cropToImageWidth(imageWidth);
		seqKymos.seq.removeAllROI();
		seqKymos.transferCapillariesMeasuresToKymos(capillaries);
		return true;
	}

	public boolean saveCapillariesMeasures(String directory) {
		boolean flag = false;
		if (seqKymos != null && seqKymos.seq != null) {
			seqKymos.validateRois();
			seqKymos.transferKymosRoisToCapillaries_Measures(capillaries);
			flag = capillaries.save_Capillaries(directory);
		}
		return flag;
	}

	public void dispatchCapillariesToCages() {
		for (Cage cage : cages.cageList) {
			cage.clearCapillaryList();
		}

		for (Capillary cap : capillaries.capillariesList) {
			int cageID = cap.getCageIndexFromRoiName();
			Cage cage = cages.getCageFromID(cageID);
			if (cage == null) {
				cage = new Cage();
				cage.setCageID(cageID);
				cages.cageList.add(cage);
			}
			cage.addCapillaryIfUnique(cap);
		}
	}

	public void kymosBuildFiltered01(int zChannelSource, int zChannelDestination, ImageTransformEnums transformop1,
			int spanDiff) {
		int nimages = seqKymos.seq.getSizeT();
		seqKymos.seq.beginUpdate();

		ImageTransformInterface transform = transformop1.getFunction();
		if (transform == null)
			return;

		if (capillaries.capillariesList.size() != nimages)
			SequenceKymosUtils.transferCamDataROIStoKymo(this);

		for (int t = 0; t < nimages; t++) {
			Capillary cap = capillaries.capillariesList.get(t);
			cap.kymographIndex = t;
			IcyBufferedImage img = seqKymos.getSeqImage(t, zChannelSource);
			IcyBufferedImage img2 = transform.getTransformedImage(img, null);
			if (seqKymos.seq.getSizeZ(0) < (zChannelDestination + 1))
				seqKymos.seq.addImage(t, img2);
			else
				seqKymos.seq.setImage(t, zChannelDestination, img2);
		}

		seqKymos.seq.dataChanged();
		seqKymos.seq.endUpdate();
	}

	public boolean loadReferenceImage() {
		BufferedImage image = null;
		File inputfile = new File(getReferenceImageFullName());
		boolean exists = inputfile.exists();
		if (!exists)
			return false;
		image = ImageUtil.load(inputfile, true);
		if (image == null) {
			Logger.warn("Experiment:loadReferenceImage() image not loaded / not found: " + getReferenceImageFullName());
			return false;
		}
		seqCamData.refImage = IcyBufferedImage.createFrom(image);
		seqReference = new Sequence(seqCamData.refImage);
		seqReference.setName("referenceImage");
		return true;
	}

	public boolean saveReferenceImage(IcyBufferedImage referenceImage) {
		File outputfile = new File(getReferenceImageFullName());
		RenderedImage image = ImageUtil.toRGBImage(referenceImage);
		return ImageUtil.save(image, "jpg", outputfile);
	}

	public void cleanPreviousDetectedFliesROIs() {
		ArrayList<ROI2D> list = seqCamData.seq.getROI2Ds();
		for (ROI2D roi : list) {
			if (roi.getName().contains("det"))
				seqCamData.seq.removeROI(roi);
		}
	}

	public void updateROIsAt(int t) {
		seqCamData.seq.beginUpdate();
		List<ROI2D> rois = seqCamData.seq.getROI2Ds();
		for (ROI2D roi : rois) {
			if (roi.getName().contains("det"))
				seqCamData.seq.removeROI(roi);
		}
		seqCamData.seq.addROIs(cages.getPositionsAsListOfROI2DRectanglesAtT(t), false);
		seqCamData.seq.endUpdate();
	}

	public void saveDetRoisToPositions() {
		List<ROI2D> detectedROIsList = seqCamData.seq.getROI2Ds();
		for (Cage cell : cages.cageList) {
			cell.transferRoisToPositions(detectedROIsList);
		}
	}

	// ----------------------------------

	private int getBinStepFromDirectoryName(String resultsPath) {
		int step = -1;
		if (resultsPath.contains(BIN)) {
			if (resultsPath.length() < (BIN.length() + 1)) {
				step = (int) kymoBin_ms;
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
			strImagesDirectory = getRootWithNoResultNorBinString(strExperimentDirectory);
			xmlFullFileName = strImagesDirectory + File.separator + xmlFileName;
			break;

		case BIN_DIRECTORY:
			// any directory (below)
			Path dirPath = Paths.get(strExperimentDirectory);
			List<Path> subFolders = Directories.getAllSubPathsOfDirectory(strExperimentDirectory, 1);
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
			xmlFullFileName = strExperimentDirectory + xmlFullFileName;
			break;
		}

		// current directory
		if (xmlFullFileName != null && fileExists(xmlFullFileName)) {
			if (item == IMG_DIRECTORY) {
				strImagesDirectory = getRootWithNoResultNorBinString(strExperimentDirectory);
				ExperimentDirectories.moveAndRename(xmlFileName, strImagesDirectory, xmlFileName,
						strExperimentDirectory);
				xmlFullFileName = strExperimentDirectory + xmlFullFileName;
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
		if (capillaries.capillariesList.size() == 0)
			loadMCCapillaries_Only();
		boolean flag = false;
		for (Capillary cap : capillaries.capillariesList) {
			if (cap.getCapillaryField(fieldEnumCode).equals(oldValue)) {
				cap.setCapillaryField(fieldEnumCode, newValue);
				flag = true;
			}
		}
		return flag;
	}

	private String concatenateExptDirectoryWithSubpathAndName(String subpath, String name) {
		if (subpath != null)
			return strExperimentDirectory + File.separator + subpath + File.separator + name;
		else
			return strExperimentDirectory + File.separator + name;
	}

	private void ugly_checkOffsetValues() {
		if (camImageFirst_ms < 0)
			camImageFirst_ms = 0;
		if (camImageLast_ms < 0)
			camImageLast_ms = 0;
		if (kymoFirst_ms < 0)
			kymoFirst_ms = 0;
		if (kymoLast_ms < 0)
			kymoLast_ms = 0;

		if (kymoBin_ms < 0)
			kymoBin_ms = 60000;
	}

	private void addCapillariesValues(EnumXLSColumnHeader fieldEnumCode, List<String> textList) {
		if (capillaries.capillariesList.size() == 0)
			loadMCCapillaries_Only();
		for (Capillary cap : capillaries.capillariesList)
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
		capillaries.capillariesDescription.old_boxID = field_boxID;
		capillaries.capillariesDescription.old_experiment = field_experiment;
		capillaries.capillariesDescription.old_comment1 = field_comment1;
		capillaries.capillariesDescription.old_comment2 = field_comment2;
		capillaries.capillariesDescription.old_strain = field_strain;
		capillaries.capillariesDescription.old_sex = field_sex;
		capillaries.capillariesDescription.old_cond1 = field_cond1;
		capillaries.capillariesDescription.old_cond2 = field_cond2;
	}

	private String getReferenceImageFullName() {
		return strExperimentDirectory + File.separator + "referenceImage.jpg";
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

	public String getStrImagesDirectory() {
		return strImagesDirectory;
	}

	public void setStrImagesDirectory(String strImagesDirectory) {
		this.strImagesDirectory = strImagesDirectory;
	}

	public String getField_boxID() {
		return field_boxID;
	}

	public void setField_boxID(String field_boxID) {
		this.field_boxID = field_boxID;
	}

	public String getField_experiment() {
		return field_experiment;
	}

	public void setField_experiment(String field_experiment) {
		this.field_experiment = field_experiment;
	}

	public String getField_comment1() {
		return field_comment1;
	}

	public void setField_comment1(String field_comment1) {
		this.field_comment1 = field_comment1;
	}

	public String getField_comment2() {
		return field_comment2;
	}

	public void setField_comment2(String field_comment2) {
		this.field_comment2 = field_comment2;
	}

	public String getField_strain() {
		return field_strain;
	}

	public void setField_strain(String field_strain) {
		this.field_strain = field_strain;
	}

	public String getField_sex() {
		return field_sex;
	}

	public void setField_sex(String field_sex) {
		this.field_sex = field_sex;
	}

	public String getField_cond1() {
		return field_cond1;
	}

	public void setField_cond1(String field_cond1) {
		this.field_cond1 = field_cond1;
	}

	public String getField_cond2() {
		return field_cond2;
	}

	public void setField_cond2(String field_cond2) {
		this.field_cond2 = field_cond2;
	}

}
