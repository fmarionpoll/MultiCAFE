package plugins.fmp.multicafe.experiment;

import java.awt.Color;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FileUtils;

import loci.formats.FormatException;
import ome.xml.meta.OMEXMLMetadata;

import icy.common.exception.UnsupportedFormatException;
import icy.file.Loader;
import icy.file.Saver;
import icy.gui.frame.progress.ProgressFrame;
import icy.image.IcyBufferedImage;
import icy.roi.ROI;
import icy.roi.ROI2D;
import icy.sequence.MetaDataUtil;
import icy.type.DataType;
import icy.type.collection.array.Array1DUtil;
import icy.type.geom.Polyline2D;
import plugins.fmp.multicafe.experiment.capillaries.Capillaries;
import plugins.fmp.multicafe.experiment.capillaries.Capillary;
import plugins.fmp.multicafe.experiment.capillaries.CapillaryMeasure;
import plugins.fmp.multicafe.tools.Comparators;
import plugins.fmp.multicafe.tools.Logger;
import plugins.fmp.multicafe.tools.ROI2D.ROI2DUtilities;
import plugins.kernel.roi.roi2d.ROI2DPolyLine;

public class SequenceKymos extends SequenceCamData {
	public boolean isRunning_loadImages = false;
	public int imageWidthMax = 0;
	public int imageHeightMax = 0;

	// -----------------------------------------------------

	public SequenceKymos() {
		super();
		status = EnumStatus.KYMOGRAPH;
	}

	public SequenceKymos(String name, IcyBufferedImage image) {
		super(name, image);
		status = EnumStatus.KYMOGRAPH;
	}

	public SequenceKymos(List<String> listNames) {
		super();
		setImagesList(convertLinexLRFileNames(listNames));
		status = EnumStatus.KYMOGRAPH;
	}

	// ----------------------------

	public void validateRoisAtT(int t) {
		List<ROI2D> listRois = seq.getROI2Ds();
		int width = seq.getWidth();
		for (ROI2D roi : listRois) {
			if (!(roi instanceof ROI2DPolyLine))
				continue;
			if (roi.getT() == -1)
				roi.setT(t);
			if (roi.getT() != t)
				continue;
			// interpolate missing points if necessary
			if (roi.getName().contains("level") || roi.getName().contains("gulp")) {
				ROI2DUtilities.interpolateMissingPointsAlongXAxis((ROI2DPolyLine) roi, width);
				continue;
			}
			if (roi.getName().contains("deriv"))
				continue;
			// if gulp not found - add an index to it
			ROI2DPolyLine roiLine = (ROI2DPolyLine) roi;
			Polyline2D line = roiLine.getPolyline2D();
			roi.setName("gulp" + String.format("%07d", (int) line.xpoints[0]));
			roi.setColor(Color.red);
		}
		Collections.sort(listRois, new Comparators.ROI2D_Name_Comparator());
	}

	public void removeROIsPolylineAtT(int t) {
		List<ROI2D> listRois = seq.getROI2Ds();
		for (ROI2D roi : listRois) {
			if (!(roi instanceof ROI2DPolyLine))
				continue;
			if (roi.getT() == t)
				seq.removeROI(roi);
		}
	}

	public void updateROIFromCapillaryMeasure(Capillary cap, CapillaryMeasure caplimits) {
		int t = cap.kymographIndex;
		List<ROI2D> listRois = seq.getROI2Ds();
		for (ROI2D roi : listRois) {
			if (!(roi instanceof ROI2DPolyLine))
				continue;
			if (roi.getT() != t)
				continue;
			if (!roi.getName().contains(caplimits.capName))
				continue;

			((ROI2DPolyLine) roi).setPolyline2D(caplimits.polylineLevel);
			roi.setName(caplimits.capName);
			seq.roiChanged(roi);
			break;
		}
	}

	public void validateRois() {
		List<ROI2D> listRois = seq.getROI2Ds();
		int width = seq.getWidth();
		for (ROI2D roi : listRois) {
			if (!(roi instanceof ROI2DPolyLine))
				continue;
			// interpolate missing points if necessary
			if (roi.getName().contains("level") || roi.getName().contains("gulp")) {
				ROI2DUtilities.interpolateMissingPointsAlongXAxis((ROI2DPolyLine) roi, width);
				continue;
			}
			if (roi.getName().contains("derivative"))
				continue;
			// if gulp not found - add an index to it
			ROI2DPolyLine roiLine = (ROI2DPolyLine) roi;
			Polyline2D line = roiLine.getPolyline2D();
			roi.setName("gulp" + String.format("%07d", (int) line.xpoints[0]));
			roi.setColor(Color.red);
		}
		Collections.sort(listRois, new Comparators.ROI2D_Name_Comparator());
	}

	public boolean transferKymosRoisToCapillaries_Measures(Capillaries capillaries) {
		List<ROI> allRois = seq.getROIs();
		if (allRois.size() < 1)
			return false;

		for (int kymo = 0; kymo < seq.getSizeT(); kymo++) {
			List<ROI> roisAtT = new ArrayList<ROI>();
			for (ROI roi : allRois) {
				if (roi instanceof ROI2D && ((ROI2D) roi).getT() == kymo)
					roisAtT.add(roi);
			}
			if (capillaries.capillariesList.size() <= kymo)
				capillaries.capillariesList.add(new Capillary());
			Capillary cap = capillaries.capillariesList.get(kymo);
			cap.filenameTIFF = getFileNameFromImageList(kymo);
			cap.kymographIndex = kymo;
			cap.transferROIsToMeasures(roisAtT);
		}
		return true;
	}

	public boolean transferKymosRoi_atT_ToCapillaries_Measures(int t, Capillaries capillaries) {
		List<ROI> allRois = seq.getROIs();
		if (allRois.size() < 1)
			return false;

		List<ROI> roisAtT = new ArrayList<ROI>();
		for (ROI roi : allRois) {
			if (roi instanceof ROI2D && ((ROI2D) roi).getT() == t)
				roisAtT.add(roi);
		}
		if (capillaries.capillariesList.size() <= t)
			capillaries.capillariesList.add(new Capillary());
		Capillary cap = capillaries.capillariesList.get(t);
		cap.filenameTIFF = getFileNameFromImageList(t);
		cap.kymographIndex = t;
		cap.transferROIsToMeasures(roisAtT);

		return true;
	}

	public void transferCapillariesMeasuresToKymos(Capillaries capillaries) {
		List<ROI2D> seqRoisList = seq.getROI2Ds(false);
		ROI2DUtilities.removeROIsMissingChar(seqRoisList, '_');

		List<ROI2D> newRoisList = new ArrayList<ROI2D>();
		int ncapillaries = capillaries.capillariesList.size();
		for (int i = 0; i < ncapillaries; i++) {
			List<ROI2D> listOfRois = capillaries.capillariesList.get(i).transferMeasuresToROIs();
			newRoisList.addAll(listOfRois);
		}
		ROI2DUtilities.mergeROIsListNoDuplicate(seqRoisList, newRoisList, seq);
		seq.removeAllROI();
		seq.addROIs(seqRoisList, false);
	}

	public void saveKymosCurvesToCapillariesMeasures(Experiment exp) {
		exp.getSeqKymos().validateRois();
		exp.getSeqKymos().transferKymosRoisToCapillaries_Measures(exp.getCapillaries());
		exp.saveCapillaries();
	}

	// ----------------------------

	public List<ImageFileDescriptor> loadListOfPotentialKymographsFromCapillaries(String dir, Capillaries capillaries) {
		renameCapillary_Files(dir);

		String directoryFull = dir + File.separator;
		int ncapillaries = capillaries.capillariesList.size();
		List<ImageFileDescriptor> myListOfFiles = new ArrayList<ImageFileDescriptor>(ncapillaries);
		for (int i = 0; i < ncapillaries; i++) {
			ImageFileDescriptor temp = new ImageFileDescriptor();
			temp.fileName = directoryFull + capillaries.capillariesList.get(i).getKymographName() + ".tiff";
			myListOfFiles.add(temp);
		}
		return myListOfFiles;
	}

	private void renameCapillary_Files(String directory) {
		File folder = new File(directory);
		File[] listFiles = folder.listFiles();
		if (listFiles == null || listFiles.length < 1)
			return;
		for (File file : folder.listFiles()) {
			String name = file.getName();
			if (name.toLowerCase().endsWith(".tiff") || name.toLowerCase().startsWith("line")) {
				String destinationName = Capillary.replace_LR_with_12(name);
				if (!name.contains(destinationName))
					file.renameTo(new File(directory + File.separator + destinationName));
			}
		}
	}

	// -------------------------

	public boolean loadImagesFromList(List<ImageFileDescriptor> kymoImagesDesc, boolean adjustImagesSize) {
		isRunning_loadImages = true;
		boolean flag = (kymoImagesDesc.size() > 0);
		if (!flag)
			return flag;

		if (adjustImagesSize)
			adjustImagesToMaxSize(kymoImagesDesc, getMaxSizeofTiffFiles(kymoImagesDesc));

		List<String> myList = new ArrayList<String>();
		for (ImageFileDescriptor prop : kymoImagesDesc) {
			if (prop.exists)
				myList.add(prop.fileName);
		}

		if (myList.size() > 0) {
			myList = ExperimentDirectories.keepOnlyAcceptedNames_List(myList, "tiff");
			setImagesList(convertLinexLRFileNames(myList));

			// threaded by default here
			loadImages();
			setParentDirectoryAsCSCamFileName(imagesList.get(0));
			status = EnumStatus.KYMOGRAPH;
		}
		isRunning_loadImages = false;
		return flag;
	}

	protected void setParentDirectoryAsCSCamFileName(String filename) {
		if (filename != null) {
			Path path = Paths.get(filename);
			csCamFileName = path.getName(path.getNameCount() - 2).toString();
			seq.setName(csCamFileName);
		}
	}

	Rectangle getMaxSizeofTiffFiles(List<ImageFileDescriptor> files) {
		imageWidthMax = 0;
		imageHeightMax = 0;
		for (int i = 0; i < files.size(); i++) {
			ImageFileDescriptor fileProp = files.get(i);
			if (!fileProp.exists)
				continue;
			getImageDim(fileProp);
			if (fileProp.imageWidth > imageWidthMax)
				imageWidthMax = fileProp.imageWidth;
			if (fileProp.imageHeight > imageHeightMax)
				imageHeightMax = fileProp.imageHeight;
		}
		return new Rectangle(0, 0, imageWidthMax, imageHeightMax);
	}

	boolean getImageDim(final ImageFileDescriptor fileProp) {
		boolean flag = false;
		OMEXMLMetadata metaData = null;
		try {
			metaData = Loader.getOMEXMLMetaData(fileProp.fileName);
			fileProp.imageWidth = MetaDataUtil.getSizeX(metaData, 0);
			fileProp.imageHeight = MetaDataUtil.getSizeY(metaData, 0);
			flag = true;
		} catch (UnsupportedFormatException | IOException | InterruptedException e) {
			Logger.error("SequenceKymos:readImageProperties() Failed to read image properties: " + fileProp.fileName, e);
		}
		return flag;
	}

	void adjustImagesToMaxSize(List<ImageFileDescriptor> files, Rectangle rect) {
		ProgressFrame progress = new ProgressFrame("Make kymographs the same width and height");
		progress.setLength(files.size());
		for (int i = 0; i < files.size(); i++) {
			ImageFileDescriptor fileProp = files.get(i);
			if (!fileProp.exists)
				continue;
			if (fileProp.imageWidth == rect.width && fileProp.imageHeight == rect.height)
				continue;

			progress.setMessage("adjust image " + fileProp.fileName);
			IcyBufferedImage ibufImage1 = null;
			try {
				ibufImage1 = Loader.loadImage(fileProp.fileName);
			} catch (UnsupportedFormatException | IOException | InterruptedException e1) {
				Logger.error("SequenceKymos:adjustImagesToMaxSize() Failed to load image: " + fileProp.fileName, e1);
			}

			IcyBufferedImage ibufImage2 = new IcyBufferedImage(imageWidthMax, imageHeightMax, ibufImage1.getSizeC(),
					ibufImage1.getDataType_());
			transferImage1To2(ibufImage1, ibufImage2);

			try {
				Saver.saveImage(ibufImage2, new File(fileProp.fileName), true);
			} catch (FormatException | IOException e) {
				Logger.error("SequenceKymos:adjustImagesToMaxSize() Failed to save adjusted image: " + fileProp.fileName, e);
			}

			progress.incPosition();
		}
		progress.close();
	}

	private static void transferImage1To2(IcyBufferedImage source, IcyBufferedImage result) {
		final int sizeY = source.getSizeY();
		final int endC = source.getSizeC();
		final int sourceSizeX = source.getSizeX();
		final int destSizeX = result.getSizeX();
		final DataType dataType = source.getDataType_();
		final boolean signed = dataType.isSigned();
		result.lockRaster();
		try {
			for (int ch = 0; ch < endC; ch++) {
				final Object src = source.getDataXY(ch);
				final Object dst = result.getDataXY(ch);
				int srcOffset = 0;
				int dstOffset = 0;
				for (int curY = 0; curY < sizeY; curY++) {
					Array1DUtil.arrayToArray(src, srcOffset, dst, dstOffset, sourceSizeX, signed);
					result.setDataXY(ch, dst);
					srcOffset += sourceSizeX;
					dstOffset += destSizeX;
				}
			}
		} finally {
			result.releaseRaster(true);
		}
		result.dataChanged();
	}

	// ----------------------------

	private List<String> convertLinexLRFileNames(List<String> myListOfFilesNames) {
		List<String> newList = new ArrayList<String>();
		for (String oldName : myListOfFilesNames)
			newList.add(convertLinexLRFileName(oldName));
		return newList;
	}

	private String convertLinexLRFileName(String oldName) {
		Path path = Paths.get(oldName);
		String test = path.getFileName().toString();
		String newName = oldName;
		if (test.contains("R.")) {
			newName = path.getParent() + File.separator + test.replace("R.", "2.");
			renameOldFile(oldName, newName);
		} else if (test.contains("L")) {
			newName = path.getParent() + File.separator + test.replace("L.", "1.");
			renameOldFile(oldName, newName);
		}
		return newName;
	}

	private void renameOldFile(String oldName, String newName) {
		File oldfile = new File(oldName);
		if (newName != null && oldfile.exists()) {
			try {
				FileUtils.moveFile(FileUtils.getFile(oldName), FileUtils.getFile(newName));
			} catch (IOException e) {
				Logger.error("SequenceKymos:renameOldFile() Failed to rename file: " + oldName + " to " + newName, e);
			}
		}
	}

}
