package plugins.fmp.multicafe.experiment.sequence;

import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.swing.SwingUtilities;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;

import icy.image.IcyBufferedImage;
import icy.roi.ROI;
import icy.sequence.Sequence;
import plugins.fmp.multicafe.tools.Logger;
import plugins.fmp.multicafe.tools.ViewerFMP;
import plugins.kernel.roi.roi2d.ROI2DPolygon;

public class SequenceCamData {
	private Sequence seq = null;
	private IcyBufferedImage refImage = null;

	private long seqAnalysisStart = 0;
	private int seqAnalysisStep = 1;

	private int currentFrame = 0;
	private int nTotalFrames = 0;

	private EnumStatus status = EnumStatus.REGULAR;
	protected String csCamFileName = null;
	private String imagesDirectory = null;
	private List<String> imagesList = new ArrayList<String>();
	private ROI2DPolygon referenceROI2DPolygon = null;

	long timeFirstImageInMs = 0;
	int indexTimePattern = -1;

	FileNameTimePattern[] timePatternArray = new FileNameTimePattern[] { new FileNameTimePattern(),
			new FileNameTimePattern("yyyy-MM-dd_HH-mm-ss", "\\d{4}-\\d{2}-\\d{2}_\\d{2}\\-\\d{2}\\-\\d{2}"),
			new FileNameTimePattern("yy-MM-dd_HH-mm-ss", "\\d{2}-\\d{2}-\\d{2}_\\d{2}\\-\\d{2}\\-\\d{2}"),
			new FileNameTimePattern("yy.MM.dd_HH.mm.ss", "\\d{2}.\\d{2}.\\d{2}_\\d{2}\\.\\d{2}\\.\\d{2}") };

	// -------------------------

	public SequenceCamData() {
		seq = new Sequence();
		status = EnumStatus.FILESTACK;
	}

	public SequenceCamData(String name, IcyBufferedImage image) {
		seq = new Sequence(name, image);
		status = EnumStatus.FILESTACK;
	}

	public SequenceCamData(List<String> listNames) {
		setImagesList(listNames);
		status = EnumStatus.FILESTACK;
	}

	// -----------------------

	public Sequence getSequence() {
		return seq;
	}

	public void setSequence(Sequence seq) {
		this.seq = seq;
	}

	public EnumStatus getStatus() {
		return status;
	}

	public void setStatus(EnumStatus status) {
		this.status = status;
	}

	public int getCurrentFrame() {
		return currentFrame;
	}

	public void setCurrentFrame(int currentFrame) {
		this.currentFrame = currentFrame;
	}

	public IcyBufferedImage getRefImage() {
		return refImage;
	}

	public void setRefImage(IcyBufferedImage refImage) {
		this.refImage = refImage;
	}

	public long getSeqAnalysisStart() {
		return seqAnalysisStart;
	}

	public void setSeqAnalysisStart(long seqAnalysisStart) {
		this.seqAnalysisStart = seqAnalysisStart;
	}

	public int getSeqAnalysisStep() {
		return seqAnalysisStep;
	}

	public void setSeqAnalysisStep(int seqAnalysisStep) {
		this.seqAnalysisStep = seqAnalysisStep;
	}

	public int getnTotalFrames() {
		return nTotalFrames;
	}

	public void setnTotalFrames(int nTotalFrames) {
		this.nTotalFrames = nTotalFrames;
	}

	public ROI2DPolygon getReferenceROI2DPolygon() {
		return referenceROI2DPolygon;
	}

	public void setReferenceROI2DPolygon(ROI2DPolygon roi) {
		referenceROI2DPolygon = roi;
	}

	public List<String> getImagesList() {
		return imagesList;
	}

	public String getImagesDirectory() {
		Path strPath = Paths.get(imagesList.get(0));
		imagesDirectory = strPath.getParent().toString();
		return imagesDirectory;
	}

	public void setImagesDirectory(String directoryString) {
		imagesDirectory = directoryString;
	}

	public List<String> getImagesList(boolean bsort) {
		if (bsort)
			Collections.sort(imagesList);
		return imagesList;
	}

	public String getDecoratedImageName(int t) {
		currentFrame = t;
		if (seq != null)
			return getCSCamFileName() + " [" + (t) + "/" + (seq.getSizeT() - 1) + "]";
		else
			return getCSCamFileName() + "[]";
	}

	public String getCSCamFileName() {
		if (csCamFileName == null) {
			Path path = Paths.get(imagesList.get(0));
			int rootlevel = path.getNameCount() - 4;
			if (rootlevel < 0)
				rootlevel = 0;
			csCamFileName = path.subpath(rootlevel, path.getNameCount() - 1).toString();
		}
		return csCamFileName;
	}

	public String getFileNameFromImageList(int t) {
		String csName = null;
		if (status == EnumStatus.FILESTACK || status == EnumStatus.KYMOGRAPH) {
			if (imagesList.size() < 1)
				loadImageList();
			csName = imagesList.get(t);
		}
//		else if (status == EnumStatus.AVIFILE)
//			csName = csFileName;
		return csName;
	}

	private void loadImageList() {
		new plugins.fmp.multicafe.service.SequenceLoaderService().loadImageList(this);
	}

	// --------------------------

	public IcyBufferedImage getSeqImage(int t, int z) {
		currentFrame = t;
		return seq.getImage(t, z);
	}

	// --------------------------

	String fileComponent(String fname) {
		int pos = fname.lastIndexOf("/");
		if (pos > -1)
			return fname.substring(pos + 1);
		else
			return fname;
	}

	public FileTime getFileTimeFromStructuredName(int t) {
		long timeInMs = 0;
		String fileName = fileComponent(getFileNameFromImageList(t));

		if (fileName == null) {
			timeInMs = timePatternArray[0].getDummyTime(t);
		} else {
			if (indexTimePattern < 0) {
				indexTimePattern = findProperFilterIfAny(fileName);
			}
			FileNameTimePattern tp = timePatternArray[indexTimePattern];
			timeInMs = tp.getTimeFromString(fileName, t);
		}

		FileTime fileTime = FileTime.fromMillis(timeInMs);
		return fileTime;
	}

	int findProperFilterIfAny(String fileName) {
		int index = 0;
		for (int i = 1; i < timePatternArray.length; i++) {
			if (timePatternArray[i].findMatch(fileName))
				return i;
		}
		return index;
	}

	public FileTime getFileTimeFromFileAttributes(int t) {
		FileTime filetime = null;
		File file = new File(getFileNameFromImageList(t));
		Path filePath = file.toPath();

		BasicFileAttributes attributes = null;
		try {
			attributes = Files.readAttributes(filePath, BasicFileAttributes.class);
		} catch (IOException exception) {
			Logger.warn("SeqCamData:getFileTimeFromFileAttributes() Exception handled when trying to get file "
					+ "attributes: " + exception.getMessage());
		}

		long milliseconds = attributes.creationTime().to(TimeUnit.MILLISECONDS);
		if ((milliseconds > Long.MIN_VALUE) && (milliseconds < Long.MAX_VALUE)) {
			Date creationDate = new Date(attributes.creationTime().to(TimeUnit.MILLISECONDS));
			filetime = FileTime.fromMillis(creationDate.getTime());
		}
		return filetime;
	}

	public FileTime getFileTimeFromJPEGMetaData(int t) {
		FileTime filetime = null;
		File file = new File(getFileNameFromImageList(t));
		Metadata metadata;
		try {
			metadata = ImageMetadataReader.readMetadata(file);
			ExifSubIFDDirectory directory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
			Date date = directory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
			filetime = FileTime.fromMillis(date.getTime());
		} catch (ImageProcessingException e) {
			Logger.warn("SeqCamData:getFileTimeFromJPEGMetaData() Failed to read JPEG metadata: " + file.getName(), e);
		} catch (IOException e) {
			Logger.error("SeqCamData:getFileTimeFromJPEGMetaData() IO error reading file: " + file.getName(), e);
		}
		return filetime;
	}

	public void displayViewerAtRectangle(Rectangle parent0Rect) {
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				public void run() {
					ViewerFMP v = (ViewerFMP) seq.getFirstViewer();
					if (v == null)
						v = new ViewerFMP(seq, true, true);
					Rectangle rectv = v.getBoundsInternal();
					rectv.setLocation(parent0Rect.x + parent0Rect.width, parent0Rect.y);
					v.setBounds(rectv);
				}
			});
		} catch (InvocationTargetException | InterruptedException e) {
			Logger.error("SeqCamData:displayViewerAtRectangle() Failed to display viewer", e);
		}
	}

	// ------------------------

	public void closeSequence() {
		if (seq == null)
			return;

		seq.removeAllROI();
		seq.close();
	}

	public void setImagesList(List<String> extImagesList) {
		imagesList.clear();
		imagesList.addAll(extImagesList);
		nTotalFrames = imagesList.size();
		status = EnumStatus.FILESTACK;
	}

	public void attachSequence(Sequence seq) {
		this.seq = seq;
		status = EnumStatus.FILESTACK;
		seqAnalysisStart = 0;
	}

	public void completeSequence(Sequence seq2) {
		if (seq != null) {
			ArrayList<ROI> listROIS = seq.getROIs();
			seq2.addROIs(listROIS, false);
		}
		seq = seq2;
		status = EnumStatus.FILESTACK;
		seqAnalysisStart = 0;
	}

	public IcyBufferedImage imageIORead(String name) {
		return new plugins.fmp.multicafe.service.SequenceLoaderService().imageIORead(name);
	}

	public boolean loadImages() {
		return new plugins.fmp.multicafe.service.SequenceLoaderService().loadImages(this);
	}

	public boolean loadFirstImage() {
		return new plugins.fmp.multicafe.service.SequenceLoaderService().loadFirstImage(this);
	}

	public Sequence loadSequenceFromImagesList(List<String> imagesList) {
		return new plugins.fmp.multicafe.service.SequenceLoaderService().loadSequenceFromImagesList(imagesList);
	}

	public Sequence initSequenceFromFirstImage(List<String> imagesList) {
		return new plugins.fmp.multicafe.service.SequenceLoaderService().initSequenceFromFirstImage(imagesList);
	}

}