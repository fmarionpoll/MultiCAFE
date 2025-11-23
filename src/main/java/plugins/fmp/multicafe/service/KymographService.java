package plugins.fmp.multicafe.service;

import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;

import loci.formats.FormatException;
import ome.xml.meta.OMEXMLMetadata;

import icy.common.exception.UnsupportedFormatException;
import icy.file.Loader;
import icy.file.Saver;
import icy.gui.frame.progress.ProgressFrame;
import icy.image.IcyBufferedImage;
import icy.sequence.MetaDataUtil;
import icy.type.DataType;
import icy.type.collection.array.Array1DUtil;
import plugins.fmp.multicafe.experiment.EnumStatus;
import plugins.fmp.multicafe.experiment.Experiment;
import plugins.fmp.multicafe.experiment.ExperimentDirectories;
import plugins.fmp.multicafe.experiment.ImageFileDescriptor;
import plugins.fmp.multicafe.experiment.SequenceKymos;
import plugins.fmp.multicafe.experiment.SequenceKymosUtils;
import plugins.fmp.multicafe.experiment.capillaries.Capillaries;
import plugins.fmp.multicafe.experiment.capillaries.Capillary;
import plugins.fmp.multicafe.tools.Logger;
import plugins.fmp.multicafe.tools.ImageTransform.ImageTransformEnums;
import plugins.fmp.multicafe.tools.ImageTransform.ImageTransformInterface;

public class KymographService {

	public void buildFiltered(Experiment exp, int zChannelSource, int zChannelDestination, ImageTransformEnums transformop1,
			int spanDiff) {
		SequenceKymos seqKymos = exp.getSeqKymos();
		int nimages = seqKymos.seq.getSizeT();
		seqKymos.seq.beginUpdate();

		ImageTransformInterface transform = transformop1.getFunction();
		if (transform == null)
			return;

		if (exp.getCapillaries().capillariesList.size() != nimages)
			SequenceKymosUtils.transferCamDataROIStoKymo(exp);

		for (int t = 0; t < nimages; t++) {
			Capillary cap = exp.getCapillaries().capillariesList.get(t);
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

	public boolean loadImagesFromList(SequenceKymos seqKymos, List<ImageFileDescriptor> kymoImagesDesc, boolean adjustImagesSize) {
		seqKymos.isRunning_loadImages = true;
		boolean flag = (kymoImagesDesc.size() > 0);
		if (!flag)
			return flag;

		if (adjustImagesSize)
			adjustImagesToMaxSize(seqKymos, kymoImagesDesc, getMaxSizeofTiffFiles(seqKymos, kymoImagesDesc));

		List<String> myList = new ArrayList<String>();
		for (ImageFileDescriptor prop : kymoImagesDesc) {
			if (prop.exists)
				myList.add(prop.fileName);
		}

		if (myList.size() > 0) {
			myList = ExperimentDirectories.keepOnlyAcceptedNames_List(myList, "tiff");
			seqKymos.setImagesList(convertLinexLRFileNames(myList));

			// threaded by default here
			seqKymos.loadImages();
			setParentDirectoryAsCSCamFileName(seqKymos, seqKymos.imagesList.get(0));
			seqKymos.status = EnumStatus.KYMOGRAPH;
		}
		seqKymos.isRunning_loadImages = false;
		return flag;
	}

	private void setParentDirectoryAsCSCamFileName(SequenceKymos seqKymos, String filename) {
		if (filename != null) {
			Path path = Paths.get(filename);
			String csCamFileName = path.getName(path.getNameCount() - 2).toString();
			seqKymos.seq.setName(csCamFileName);
		}
	}

	private Rectangle getMaxSizeofTiffFiles(SequenceKymos seqKymos, List<ImageFileDescriptor> files) {
		seqKymos.imageWidthMax = 0;
		seqKymos.imageHeightMax = 0;
		for (int i = 0; i < files.size(); i++) {
			ImageFileDescriptor fileProp = files.get(i);
			if (!fileProp.exists)
				continue;
			getImageDim(fileProp);
			if (fileProp.imageWidth > seqKymos.imageWidthMax)
				seqKymos.imageWidthMax = fileProp.imageWidth;
			if (fileProp.imageHeight > seqKymos.imageHeightMax)
				seqKymos.imageHeightMax = fileProp.imageHeight;
		}
		return new Rectangle(0, 0, seqKymos.imageWidthMax, seqKymos.imageHeightMax);
	}

	private boolean getImageDim(final ImageFileDescriptor fileProp) {
		boolean flag = false;
		OMEXMLMetadata metaData = null;
		try {
			metaData = Loader.getOMEXMLMetaData(fileProp.fileName);
			fileProp.imageWidth = MetaDataUtil.getSizeX(metaData, 0);
			fileProp.imageHeight = MetaDataUtil.getSizeY(metaData, 0);
			flag = true;
		} catch (UnsupportedFormatException | IOException | InterruptedException e) {
			Logger.error("KymographService:readImageProperties() Failed to read image properties: " + fileProp.fileName, e);
		}
		return flag;
	}

	private void adjustImagesToMaxSize(SequenceKymos seqKymos, List<ImageFileDescriptor> files, Rectangle rect) {
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
				Logger.error("KymographService:adjustImagesToMaxSize() Failed to load image: " + fileProp.fileName, e1);
			}

			IcyBufferedImage ibufImage2 = new IcyBufferedImage(seqKymos.imageWidthMax, seqKymos.imageHeightMax, ibufImage1.getSizeC(),
					ibufImage1.getDataType_());
			transferImage1To2(ibufImage1, ibufImage2);

			try {
				Saver.saveImage(ibufImage2, new File(fileProp.fileName), true);
			} catch (FormatException | IOException e) {
				Logger.error("KymographService:adjustImagesToMaxSize() Failed to save adjusted image: " + fileProp.fileName, e);
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

	public List<String> convertLinexLRFileNames(List<String> myListOfFilesNames) {
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
				Logger.error("KymographService:renameOldFile() Failed to rename file: " + oldName + " to " + newName, e);
			}
		}
	}
}

