package plugins.fmp.multicafe.sequence;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import icy.image.IcyBufferedImage;
import icy.image.ImageUtil;
import icy.roi.ROI2D;
import icy.sequence.Sequence;
import icy.type.collection.array.Array1DUtil;
import icy.util.XMLUtil;
import plugins.fmp.multicafe.tools.Comparators;
import plugins.fmp.multicafe.tools.ImageTransformTools;
import plugins.fmp.multicafe.tools.ImageTransformTools.TransformOp;
import plugins.kernel.roi.roi2d.ROI2DShape;



public class Experiment {
	
	private String			experimentDirectory		= null;
	private String			imagesDirectory			= null;
	
	public static String 	RESULTS					= "results";
	public String			resultsSubPath			= RESULTS;
	public List<String>		resultsDirList			= new ArrayList<String> ();
		
	public SequenceCamData 	seqCamData 				= null;
	public SequenceKymos 	seqKymos				= null;
	public Sequence 		seqBackgroundImage		= null;
	public Capillaries 		capillaries 			= new Capillaries();
	public Cages			cages 					= new Cages();
	
	public FileTime			firstImage_FileTime;
	public FileTime			lastImage_FileTime;
	
	// __________________________________________________
	
	public long				camFirstImage_Ms		= 0;
	public long				camLastImage_Ms			= 0;
	public long				camBinImage_Ms			= 0;
	
	public long				kymoFirstCol_Ms			= 0;
	public long				kymoLastCol_Ms			= 0;
	public long				kymoBinColl_Ms			= 60000;
	
	// _________________________________________________
	
	public String			exp_boxID 				= new String("..");
	public String			experiment				= new String("..");
	public String 			comment1				= new String("..");
	public String 			comment2				= new String("..");
	
	public int				col						= -1;
	public Experiment 		previousExperiment		= null;		// pointer to chain this experiment to another one before
	public Experiment 		nextExperiment 			= null;		// pointer to chain this experiment to another one after
	public int				experimentID 			= 0;
	
	ImageTransformTools 	tImg 					= null;
	
	private final static String ID_VERSION			= "version"; 
	private final static String ID_VERSIONNUM		= "1.0.0"; 
	private final static String ID_TIMEFIRSTIMAGE	= "fileTimeImageFirstMinute"; 
	private final static String ID_TIMELASTIMAGE 	= "fileTimeImageLastMinute";
	
	private final static String ID_TIMEFIRSTIMAGEMS	= "fileTimeImageFirstMs"; 
	private final static String ID_TIMELASTIMAGEMS 	= "fileTimeImageLastMs";
	private final static String ID_FIRSTKYMOCOLMS	= "firstKymoColMs"; 
	private final static String ID_LASTKYMOCOLMS 	= "lastKymoColMs";
	private final static String ID_BINKYMOCOLMS 	= "binKymoColMs";	

	private final static String ID_IMAGESDIRECTORY 	= "imagesDirectory";
	private final static String ID_MCEXPERIMENT 	= "MCexperiment";
	private final static String ID_MCDROSOTRACK     = "MCdrosotrack.xml";
	
	private final static String ID_BOXID 			= "boxID";
	private final static String ID_EXPERIMENT 		= "experiment";
	private final static String ID_COMMENT1 		= "comment";
	private final static String ID_COMMENT2 		= "comment2";
	
	// ----------------------------------
	
	public Experiment() {
		seqCamData = new SequenceCamData();
		seqKymos   = new SequenceKymos();
	}
	
	public Experiment(String filename) {
		seqCamData = new SequenceCamData();
		seqKymos   = new SequenceKymos();
		
		File f = new File(filename);
		String parent = f.getAbsolutePath();
		if (!f.isDirectory()) {
			Path path = Paths.get(parent);
			parent = path.getParent().toString();
		}
		this.experimentDirectory = parent;
	}
	
	public Experiment(SequenceCamData seqCamData) {
		this.seqCamData = seqCamData;
		this.seqKymos   = new SequenceKymos();
		this.seqCamData.setParentDirectoryAsFileName() ;
		experimentDirectory = this.seqCamData.getSeqDataDirectory() + File.separator + RESULTS;
		loadFileIntervalsFromSeqCamData();
	}
	
	// ----------------------------------
	
	public String getExperimentDirectory() {
		return experimentDirectory;
	}
	
	public void setExperimentDirectory(String fileName) {
		experimentDirectory = fileName;
	}
	
	public void setImagesDirectory(String name) {
		imagesDirectory = name;
	}
	
	public String getImagesDirectory() {
		return imagesDirectory;
	}
	
	public void closeExperiment() {
		if (seqKymos != null) {
			seqKymos.closeSequence();
		}
		if (seqCamData != null) {
			seqCamData.closeSequence();
		}
		if (seqBackgroundImage != null) {
			seqBackgroundImage.close();
		}
	}
	
	public boolean openSequenceAndMeasures(boolean loadCapillaries, boolean loadDrosoPositions) {
		if (seqCamData == null) {
			seqCamData = new SequenceCamData();
		}
		xmlLoadMCExperiment ();
		if (null == seqCamData.loadSequenceOfImages(imagesDirectory))
			return false;
		loadFileIntervalsFromSeqCamData();
		if (seqKymos == null)
			seqKymos = new SequenceKymos();
		if (loadCapillaries) {
			xmlLoadMCcapillaries_Only();
			if (!xmlLoadMCCapillaries_Measures()) 
				return false;
		}

		if (loadDrosoPositions)
			xmlReadDrosoTrack(null);
		return true;
	}
	
	public SequenceCamData loadImagesForSequenceCamData(String filename) {
		if (filename .contains(RESULTS)) {
			experimentDirectory = filename;
			filename = Paths.get(filename).getParent().toString();
		}
		imagesDirectory = filename;
		if (seqCamData == null)
			seqCamData = new SequenceCamData();
		if (null == seqCamData.loadSequenceOfImages(filename))
			return null;
		return seqCamData;
	}
	
	public SequenceCamData openSequenceCamData(String filename) {
		if (null == loadImagesForSequenceCamData(filename))
			return null;		
		xmlLoadMCExperiment();
		seqCamData.setParentDirectoryAsFileName() ;
		loadFileIntervalsFromSeqCamData();
		return seqCamData;
	}
	
	public SequenceCamData openExperimentImagesData() {
		xmlLoadMCExperiment();
		loadImagesForSequenceCamData(experimentDirectory);
		loadFileIntervalsFromSeqCamData();
		return seqCamData;
	}
	
	public void loadFileIntervalsFromSeqCamData() {
		firstImage_FileTime = seqCamData.getFileTimeFromStructuredName(0);
		lastImage_FileTime = seqCamData.getFileTimeFromStructuredName(seqCamData.seq.getSizeT()-1);
		
		camFirstImage_Ms = firstImage_FileTime.toMillis();
		camLastImage_Ms = lastImage_FileTime.toMillis();
		camBinImage_Ms = (camLastImage_Ms - camFirstImage_Ms)/(seqCamData.seq.getSizeT()-1);
	}

	public String getResultsDirectoryNameFromKymoFrameStep() {
		return RESULTS + "_"+kymoBinColl_Ms/1000;
	}
	
	public String getDirectoryToSaveResults() {
		Path dir = Paths.get(experimentDirectory);
		dir = dir.resolve(resultsSubPath);
		String directory = dir.toAbsolutePath().toString();
		if (Files.notExists(dir))  {
			try {
				Files.createDirectory(dir);
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("Creating directory failed: "+ directory);
				return null;
			}
		}
		return directory;
	}
	
	public List<String> fetchListOfResultsSubDirectories(String directory) {
		Path pathExperimentDir = Paths.get(directory);
		List<Path> subfolders;
		List<String> resultsDirList = new ArrayList<String>();
		try {
			subfolders = Files.walk(pathExperimentDir, 1)
			        .filter(Files::isDirectory)
			        .collect(Collectors.toList());
			subfolders.remove(0);
			resultsDirList.clear();
			for (Path dirPath: subfolders) {
				String subString = dirPath.subpath(dirPath.getNameCount() - 1, dirPath.getNameCount()).toString();
				if (subString.contains(RESULTS)) {
					boolean found = false;
					for (String item: resultsDirList) {
						if (item.equals(subString)) {
							found = true;
							break;
						}
					}
					if (!found)
						resultsDirList.add(subString);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		Collections.sort(resultsDirList, Collections.reverseOrder(String.CASE_INSENSITIVE_ORDER));
		return resultsDirList;
	}
	
	public static List<String> fetchListOfResultsDirectories(String directory) {
		Path pathExperimentDir = Paths.get(directory);
		List<Path> subfolders;
		List<String> resultsDirList = new ArrayList<String>();
		try {
			subfolders = Files.walk(pathExperimentDir, 1)
			        .filter(Files::isDirectory)
			        .collect(Collectors.toList());
			subfolders.remove(0);
			resultsDirList.clear();
			for (Path dirPath: subfolders) {
				String subString = dirPath.toString();
				if (subString.contains(RESULTS)) {
					boolean found = false;
					for (String item: resultsDirList) {
						if (item.equals(subString)) {
							found = true;
							break;
						}
					}
					if (!found)
						resultsDirList.add(subString);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		Collections.sort(resultsDirList, Collections.reverseOrder(String.CASE_INSENSITIVE_ORDER));
		return resultsDirList;
	}
	
	public int getBinStepFromResultsDirectoryName(String resultsPath) {
		int step = -1;
		if (resultsPath.contains(RESULTS)) {
			if (resultsPath.length() < (RESULTS.length() +2)) {
				step = (int) kymoBinColl_Ms;
			} else {
				step = Integer.valueOf(resultsPath.substring(RESULTS.length()+1))*1000;
			}
		}
		return step;
	}

	public boolean xmlLoadMCExperiment () {
		if (experimentDirectory == null && seqCamData != null) {
			imagesDirectory = seqCamData.getSeqDataDirectory() ;
			experimentDirectory = imagesDirectory + File.separator + RESULTS;
		}
		return xmlLoadExperiment(getMCExperimentFileName(null));
	}
	
	private String getMCExperimentFileName(String subpath) {
		if (subpath != null)
			return experimentDirectory + File.separator + subpath + File.separator + "MCexperiment.xml";
		else
			return experimentDirectory + File.separator + "MCexperiment.xml";
	}
	
	private boolean xmlLoadExperiment (String csFileName) {	
		final Document doc = XMLUtil.loadDocument(csFileName);
		if (doc == null)
			return false;
		Node node = XMLUtil.getElement(XMLUtil.getRootElement(doc), ID_MCEXPERIMENT);
		if (node == null)
			return false;

		String version = XMLUtil.getElementValue(node, ID_VERSION, ID_VERSIONNUM);
		if (!version .equals(ID_VERSIONNUM))
			return false;
		camFirstImage_Ms= XMLUtil.getElementLongValue(node, ID_TIMEFIRSTIMAGEMS, -1);
		camLastImage_Ms = XMLUtil.getElementLongValue(node, ID_TIMELASTIMAGEMS, -1);
		if (camFirstImage_Ms < 0) 
			camFirstImage_Ms = XMLUtil.getElementLongValue(node, ID_TIMEFIRSTIMAGE, -1)*60000;
		if (camLastImage_Ms < 0)
			camLastImage_Ms = XMLUtil.getElementLongValue(node, ID_TIMELASTIMAGE, -1)*60000;

		kymoFirstCol_Ms = XMLUtil.getElementLongValue(node, ID_FIRSTKYMOCOLMS, -1); 
		kymoLastCol_Ms = XMLUtil.getElementLongValue(node, ID_LASTKYMOCOLMS, -1);
		kymoBinColl_Ms = XMLUtil.getElementLongValue(node, ID_BINKYMOCOLMS, -1); 	
		
		if (exp_boxID .contentEquals("..")) {
			exp_boxID	= XMLUtil.getElementValue(node, ID_BOXID, "..");
	        experiment 	= XMLUtil.getElementValue(node, ID_EXPERIMENT, "..");
	        comment1 	= XMLUtil.getElementValue(node, ID_COMMENT1, "..");
	        comment2 	= XMLUtil.getElementValue(node, ID_COMMENT2, "..");
		}
		
		imagesDirectory = XMLUtil.getElementValue(node, ID_IMAGESDIRECTORY, imagesDirectory);
		return true;
	}
	// TODO
	public boolean xmlSaveMCExperiment () {
		final Document doc = XMLUtil.createDocument(true);
		if (doc != null) {
			Node xmlRoot = XMLUtil.getRootElement(doc, true);
			Node node = XMLUtil.setElement(xmlRoot, ID_MCEXPERIMENT);
			if (node == null)
				return false;
			
			XMLUtil.setElementValue(node, ID_VERSION, ID_VERSIONNUM);
			XMLUtil.setElementLongValue(node, ID_TIMEFIRSTIMAGEMS, camFirstImage_Ms);
			XMLUtil.setElementLongValue(node, ID_TIMELASTIMAGEMS, camLastImage_Ms);
			
			XMLUtil.setElementLongValue(node, ID_FIRSTKYMOCOLMS, kymoFirstCol_Ms); 
			XMLUtil.setElementLongValue(node, ID_LASTKYMOCOLMS, kymoLastCol_Ms);
			XMLUtil.setElementLongValue(node, ID_BINKYMOCOLMS, kymoBinColl_Ms); 	
			
			XMLUtil.setElementValue(node, ID_BOXID, exp_boxID);
	        XMLUtil.setElementValue(node, ID_EXPERIMENT, experiment);
	        XMLUtil.setElementValue(node, ID_COMMENT1, comment1);
	        XMLUtil.setElementValue(node, ID_COMMENT2, comment2);
	        
	        if (imagesDirectory == null ) 
	        	imagesDirectory = seqCamData.getSeqDataDirectory();
	        XMLUtil.setElementValue(node, ID_IMAGESDIRECTORY, imagesDirectory);

	        String tempname = getMCExperimentFileName(null) ;
	        return XMLUtil.saveDocument(doc, tempname);
		}
		return false;
	}
	
 	public boolean loadKymographs() {
		if (seqKymos == null)
			seqKymos = new SequenceKymos();
		if (!xmlLoadMCCapillaries_Measures()) 
			return false;
		List<String> myList = seqKymos.loadListOfKymographsFromCapillaries(experimentDirectory, capillaries);
		boolean flag = seqKymos.loadImagesFromList(myList, true);
		if (!flag)
			return flag;
		seqKymos.transferCapillariesMeasuresToKymos(capillaries);
		return flag;
	}
	
	public boolean loadDrosotrack() {
		return xmlReadDrosoTrack(null);
	}
	
	public boolean loadKymos_Measures() {
		if (seqKymos == null)
			seqKymos = new SequenceKymos();
		if (!xmlLoadMCCapillaries_Measures()) 
			return false;
		return true;
	}
	
	// ----------------------------------
	
	public String getSubName(Path path, int subnameIndex) {
		String name = "-";
		if (path.getNameCount() >= subnameIndex)
			name = path.getName(path.getNameCount() -subnameIndex).toString();
		return name;
	}

	public FileTime getFileTimeImageFirst(boolean globalValue) {
		FileTime filetime = firstImage_FileTime;
		if (globalValue && previousExperiment != null)
			filetime = previousExperiment.getFileTimeImageFirst(globalValue);
		return filetime;
	}
		
	public void setFileTimeImageFirst(FileTime fileTimeImageFirst) {
		this.firstImage_FileTime = fileTimeImageFirst;
	}
	
	public FileTime getFileTimeImageLast(boolean globalValue) {
		FileTime filetime = lastImage_FileTime;
		if (globalValue && nextExperiment != null)
			filetime = nextExperiment.getFileTimeImageLast(globalValue);
		return filetime;
	}
	
	public void setFileTimeImageLast(FileTime fileTimeImageLast) {
		this.lastImage_FileTime = fileTimeImageLast;
	}
	
	// -----------------------
	
	public boolean isFlyAlive(int cagenumber) {
		boolean isalive = false;
		for (Cage cage: cages.cageList) {
			String cagenumberString = cage.cageRoi.getName().substring(4);
			if (Integer.valueOf(cagenumberString) == cagenumber) {
				isalive = (cage.flyPositions.getLastIntervalAlive() > 0);
				break;
			}
		}
		return isalive;
	}
	
	public int getLastIntervalFlyAlive(int cagenumber) {
		int flypos = -1;
		for (Cage cage: cages.cageList) {
			String cagenumberString = cage.cageRoi.getName().substring(4);
			if (Integer.valueOf(cagenumberString) == cagenumber) {
				flypos = cage.flyPositions.getLastIntervalAlive();
				break;
			}
		}
		return flypos;
	}
	
	public boolean isDataAvailable(int cagenumber) {
		boolean isavailable = false;
		for (Cage cage: cages.cageList) {
			String cagenumberString = cage.cageRoi.getName().substring(4);
			if (Integer.valueOf(cagenumberString) == cagenumber) {
				isavailable = true;
				break;
			}
		}
		return isavailable;
	}
	
	// --------------------------------------------
	
	public int getSeqCamSizeT() {
		int lastFrame = 0;
		if (seqCamData != null && seqCamData.seq != null)
			lastFrame = seqCamData.seq.getSizeT() -1;
		return lastFrame;
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
	
	public boolean xmlLoadMCcapillaries() {
		String xmlCapillaryFileName = getFileLocation(capillaries.getXMLNameToAppend());
		boolean flag1 = capillaries.xmlLoadCapillaries_Descriptors(xmlCapillaryFileName);
		boolean flag2 = capillaries.xmlLoadCapillaries_Measures2(experimentDirectory);
		if (flag1 & flag2) {
			seqKymos.seqDataDirectory = experimentDirectory;
			seqKymos.loadListOfKymographsFromCapillaries(seqKymos.seqDataDirectory, capillaries);
		}
		return flag1 & flag2;
	}
	
	public boolean transferCapillariesToROIs() {
		boolean flag = true;
		if (seqKymos != null && seqKymos.seq != null) {
			seqKymos.transferCapillariesMeasuresToKymos(capillaries);
		}
		return flag;
	}

	public void saveExperimentMeasures(String directory) {
		if (seqKymos != null && seqKymos.seq != null) {
			seqKymos.validateRois();
			seqKymos.transferKymosRoisToCapillaries(capillaries);
			capillaries.xmlSaveCapillaries_Measures(directory);
		}
	}
		
	public void kymosBuildFiltered(int zChannelSource, int zChannelDestination, TransformOp transformop, int spanDiff) {
		int nimages = seqKymos.seq.getSizeT();
		seqKymos.seq.beginUpdate();
		
		if (tImg == null) 
			tImg = new ImageTransformTools();
		tImg.setSpanDiff(spanDiff);
		tImg.setSequence(seqKymos);
		
		if (capillaries.capillariesArrayList.size() != nimages) {
			SequenceKymosUtils.transferCamDataROIStoKymo(this);
		}
		
		for (int t= 0; t < nimages; t++) {
			Capillary cap = capillaries.capillariesArrayList.get(t);
			cap.indexImage = t;
			IcyBufferedImage img = seqKymos.seq.getImage(t, zChannelSource);
			IcyBufferedImage img2 = tImg.transformImage (img, transformop);
			if (seqKymos.seq.getSizeZ(0) < (zChannelDestination+1)) 
				seqKymos.seq.addImage(t, img2);
			else
				seqKymos.seq.setImage(t, zChannelDestination, img2);
		}
		
		if (zChannelDestination == 1)
			capillaries.limitsOptions.transformForLevels = transformop;
		else
			capillaries.limitsOptions.transformForGulps = transformop;
		seqKymos.seq.dataChanged();
		seqKymos.seq.endUpdate();
	}
	
	public void setReferenceImageWithConstant (double [] pixel) {
		if (tImg == null) 
			tImg = new ImageTransformTools();
		tImg.setSpanDiff(0);
		Sequence seq = seqKymos.seq;
		tImg.referenceImage = new IcyBufferedImage(seq.getSizeX(), seq.getSizeY(), seq.getSizeC(), seq.getDataType_());
		IcyBufferedImage result = tImg.referenceImage;
		for (int c=0; c < seq.getSizeC(); c++) {
			double [] doubleArray = Array1DUtil.arrayToDoubleArray(result.getDataXY(c), result.isSignedDataType());
			Array1DUtil.fill(doubleArray, 0, doubleArray.length, pixel[c]);
			Array1DUtil.doubleArrayToArray(doubleArray, result.getDataXY(c));
		}
		result.dataChanged();
	}
	
	public boolean xmlLoadMCCapillaries_Measures() {
		boolean flag = capillaries.xmlLoadCapillaries_Measures2(experimentDirectory);
		if (flag) {
			seqKymos.seqDataDirectory = experimentDirectory;
			seqKymos.loadListOfKymographsFromCapillaries(seqKymos.seqDataDirectory, capillaries);
		}
		return flag;
	}
	
	public boolean xmlLoadMCcapillaries_Only() {
		String xmlCapillaryFileName = getFileLocation(capillaries.getXMLNameToAppend());
		if (xmlCapillaryFileName == null && seqCamData != null) {
			return xmlLoadOldCapillaries();
		}
		boolean flag = capillaries.xmlLoadCapillaries_Descriptors(xmlCapillaryFileName);
		if (capillaries.capillariesArrayList.size() < 1)
			flag = xmlLoadOldCapillaries();
		
		// load mccapillaries description of experiment
		if (exp_boxID .contentEquals("..")) {
			exp_boxID = capillaries.desc.old_boxID;
			experiment = capillaries.desc.old_experiment;
			comment1 = capillaries.desc.old_comment1;
			comment2 = capillaries.desc.old_comment2;
		}
		return flag;
	}
	
	private boolean xmlLoadOldCapillaries() {
		String filename = getFileLocation("capillarytrack.xml");
		if (capillaries.xmlLoadOldCapillaries_Only(filename)) {
			xmlSaveMCcapillaries();
			return true;
		}
		filename = getFileLocation("roislines.xml");
		if (seqCamData.xmlReadROIs(filename)) {
			xmlReadRoiLineParameters(filename);
			return true;
		}
		return false;
	}
	
	private String getFileLocation(String xmlFileName) {
		// current results directory
		String xmlFullFileName = experimentDirectory + File.separator + xmlFileName;
		if(fileExists (xmlFullFileName))
			return xmlFullFileName;
		// primary data
		if (imagesDirectory == null) {
			if (seqCamData != null )
				imagesDirectory = seqCamData.getSeqDataDirectory() ;
			if (imagesDirectory == null)
				imagesDirectory = getRootWithNoResultString(experimentDirectory);
		}
		xmlFullFileName = imagesDirectory + File.separator + xmlFileName;
		if(fileExists (xmlFullFileName))
			return xmlFullFileName;
		// any results directory
		Path dirPath = Paths.get(experimentDirectory);
		for (String resultsSub : resultsDirList) {
			Path dir = dirPath.resolve(resultsSub+ File.separator + xmlFileName);
			if (Files.notExists(dir))
				continue;
			return dir.toAbsolutePath().toString();	
		}
		return null;
	}
	
	private boolean fileExists (String fileName) {
		File f = new File(fileName);
		return (f.exists() && !f.isDirectory()); 
	}
	
	String getRootWithNoResultString(String directoryName) {
		String name = directoryName.toLowerCase();
		while (name .contains("result")) {
			name = Paths.get(experimentDirectory).getParent().toString();
		}
		return name;
	}
	
	// TODO
	public boolean xmlSaveMCcapillaries() {
		String xmlCapillaryFileName = experimentDirectory + File.separator + capillaries.getXMLNameToAppend();
		saveExpDescriptorsToCapillariesDescriptors();
		boolean flag = capillaries.xmlSaveCapillaries_Descriptors(xmlCapillaryFileName);
		flag &= capillaries.xmlSaveCapillaries_Measures(experimentDirectory);
		return flag;
	}
	
	private void saveExpDescriptorsToCapillariesDescriptors() {
		if (!exp_boxID 	.equals("..")) capillaries.desc.old_boxID = exp_boxID;
		if (!experiment	.equals("..")) capillaries.desc.old_experiment = experiment;
		if (!comment1	.equals("..")) capillaries.desc.old_comment1 = comment1;
		if (!comment2	.equals("..")) capillaries.desc.old_comment2 = comment2;	
	}
	
	public boolean xmlReadRoiLineParameters(String pathname) {
		if (pathname != null)  {
			final Document doc = XMLUtil.loadDocument(pathname);
			if (doc != null) 
				return capillaries.desc.xmlLoadCapillaryDescription(doc); 
		}
		return false;
	}
	
	public void updateCapillariesFromCamData() {
		if (seqCamData == null)
			return;
		
		List<ROI2D> listROISCap = seqCamData.getROIs2DContainingString ("line");
		Collections.sort(listROISCap, new Comparators.ROI2D_Name_Comparator());
		for (Capillary cap: capillaries.capillariesArrayList) {
			cap.valid = false;
			String capName = cap.replace_LR_with_12(cap.roi.getName());
			Iterator <ROI2D> iterator = listROISCap.iterator();
			while(iterator.hasNext()) { 
				ROI2D roi = iterator.next();
				String roiName = cap.replace_LR_with_12(roi.getName());
				if (roiName.equals (capName)) {
					cap.roi = (ROI2DShape) roi;
					cap.valid = true;
				}
				if (cap.valid) {
					iterator.remove();
					break;
				}
			}
		}
		Iterator <Capillary> iterator = capillaries.capillariesArrayList.iterator();
		while (iterator.hasNext()) {
			Capillary cap = iterator.next();
			if (!cap.valid )
				iterator.remove();
		}
		
		if (listROISCap.size() > 0) {
			for (ROI2D roi: listROISCap) {
				Capillary cap = new Capillary((ROI2DShape) roi);
				capillaries.capillariesArrayList.add(cap);
			}
		}
		Collections.sort(capillaries.capillariesArrayList);
		return;
	}
	
	public String getDecoratedImageNameFromCapillary(int t) {
		if (capillaries != null & capillaries.capillariesArrayList.size() > 0)
			return capillaries.capillariesArrayList.get(t).roi.getName() + " ["+(t+1)+ "/" + seqKymos.seq.getSizeT() + "]";
		return seqKymos.csFileName + " ["+(t+1)+ "/" + seqKymos.seq.getSizeT() + "]";
	}
	
	public boolean loadReferenceImage() {
		BufferedImage image = null;
		File inputfile = new File(getReferenceImageFullName());
		boolean exists = inputfile.exists();
		if (!exists) 
			return false;	
		image = ImageUtil.load(inputfile, true);
		if (image == null) {
			System.out.println("image not loaded / not found");
			return false;
		}			
		seqCamData.refImage =  IcyBufferedImage.createFrom(image);
		seqBackgroundImage = new Sequence(seqCamData.refImage);
		seqBackgroundImage.setName("referenceImage");
		return true;
	}
	
	public boolean saveReferenceImage() {
		File outputfile = new File(getReferenceImageFullName());
		RenderedImage image = ImageUtil.toRGBImage(seqCamData.refImage);
		return ImageUtil.save(image, "jpg", outputfile);
	}
	
	private String getReferenceImageFullName() {
		return experimentDirectory+File.separator+"referenceImage.jpg";
	}
	
	public void cleanPreviousDetectedFliesROIs() {
		ArrayList<ROI2D> list = seqCamData.seq.getROI2Ds();
		for (ROI2D roi: list) {
			if (roi.getName().contains("det")) {
				seqCamData.seq.removeROI(roi);
			}
		}
	}
	
	public void orderFlyPositionsForAllCages() {
		cages.orderFlyPositions();
	}

	public void xmlSaveFlyPositionsForAllCages() {			
		cages.xmlWriteCagesToFileNoQuestion(getMCDrosoTrackFullName());
	}
	
	// --------------------------
	private String getMCDrosoTrackFullName() {
		return experimentDirectory+File.separator+ID_MCDROSOTRACK;
	}
	
	private String getXMLDrosoTrackLocation() {
		String fileName = getFileLocation(ID_MCDROSOTRACK);
		if (fileName == null)  
			fileName = getFileLocation("drosotrack.xml");
		return fileName;
	}
	
	public boolean xmlReadDrosoTrack(String filename) {
		if (filename == null) {
			filename = getXMLDrosoTrackLocation();
			if (filename == null)
				return false;
		}
		return cages.xmlReadCagesFromFileNoQuestion(filename, this);
	}
	
	public boolean xmlWriteDrosoTrackDefault() {
		return cages.xmlWriteCagesToFileNoQuestion(getMCDrosoTrackFullName());
	}

	// --------------------------
		
	public void updateROIsAt(int t) {
		seqCamData.seq.beginUpdate();
		List<ROI2D> rois = seqCamData.seq.getROI2Ds();
		for (ROI2D roi: rois) {
		    if (roi.getName().contains("det") ) 
		    	seqCamData.seq.removeROI(roi);
		}
		seqCamData.seq.addROIs(cages.getPositionsAtT(t), false);
		seqCamData.seq.endUpdate();
	}
		
	public void saveDetRoisToPositions() {
		List<ROI2D> detectedROIsList= seqCamData.seq.getROI2Ds();
		for (Cage cage : cages.cageList) {
			cage.transferRoisToPositions(detectedROIsList);
		}
	}
	
}
