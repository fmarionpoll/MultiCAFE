 package plugins.fmp.multicafe2.experiment;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
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

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;

import icy.file.Loader;
import icy.file.SequenceFileImporter;
import icy.gui.viewer.Viewer;
import icy.image.IcyBufferedImage;
import icy.roi.ROI;
import icy.roi.ROI2D;
import icy.sequence.Sequence;
import plugins.fmp.multicafe2.tools.Comparators;
import plugins.kernel.roi.roi2d.ROI2DPolygon;



public class SequenceCamData 
{
	public Sequence					seq						= null;
	public IcyBufferedImage 		refImage 				= null;
	
	public long						seqAnalysisStart 		= 0;
	public int 						seqAnalysisStep 		= 1;
	
	public int 						currentFrame 			= 0;
	public int						nTotalFrames 			= 0;

	public EnumStatus 				status					= EnumStatus.REGULAR;		
	protected String 				csCamFileName 			= null;
	public String					imagesDirectory			= null;
	public List <String>			imagesList 				= new ArrayList<String>();
	
	long 							timeFirstImageInMs		= 0;
	int								indexTimePattern 		= -1;
	
    FileNameTimePattern[] timePatternArray = new FileNameTimePattern[] {
    		new FileNameTimePattern(),
    		new FileNameTimePattern("yyyy-MM-dd_HH-mm-ss", 	"\\d{4}-\\d{2}-\\d{2}_\\d{2}\\-\\d{2}\\-\\d{2}"),
    		new FileNameTimePattern("yy-MM-dd_HH-mm-ss", 	"\\d{2}-\\d{2}-\\d{2}_\\d{2}\\-\\d{2}\\-\\d{2}"),
    		new FileNameTimePattern("yy.MM.dd_HH.mm.ss", 	"\\d{2}.\\d{2}.\\d{2}_\\d{2}\\.\\d{2}\\.\\d{2}")
		};
    
	
	// -------------------------
	
	public SequenceCamData () 
	{
		seq = new Sequence();
		status = EnumStatus.FILESTACK;
	}
	
	public SequenceCamData(String name, IcyBufferedImage image) 
	{
		seq = new Sequence (name, image);
		status = EnumStatus.FILESTACK;
	}

	public SequenceCamData (List<String> listNames) 
	{
		setImagesList(listNames);
		status = EnumStatus.FILESTACK;
	}
	
	// -----------------------
	
	public String getImagesDirectory () 
	{
		Path strPath = Paths.get(imagesList.get(0));
		imagesDirectory = strPath.getParent().toString();
		return imagesDirectory;
	}

	public void setImagesDirectory (String directoryString)
	{	
		imagesDirectory = directoryString;
	}
	
	public List <String> getImagesList(boolean bsort) 
	{
		if (bsort)
			Collections.sort(imagesList);		
		return imagesList;
	}

	public String getDecoratedImageName(int t) 
	{
		currentFrame = t; 
		if (seq != null)
			return getCSCamFileName() + " ["+(t)+ "/" + (seq.getSizeT()-1) + "]";
		else
			return getCSCamFileName() + "[]";
	}
	
	public String getCSCamFileName() 
	{
		if (csCamFileName == null) 
		{
			Path path = Paths.get(imagesList.get(0));
			int rootlevel = path.getNameCount()-4;
			if (rootlevel < 0) 
				rootlevel = 0;
			csCamFileName = path.subpath(rootlevel, path.getNameCount()-1).toString();
		}
		return csCamFileName;		
	}
	
	public String getFileNameFromImageList(int t) 
	{
		String csName = null;
		if (status == EnumStatus.FILESTACK || status == EnumStatus.KYMOGRAPH)  
		{
			if (imagesList.size() < 1)
				loadImageList();
			csName = imagesList.get(t);
		}
//		else if (status == EnumStatus.AVIFILE)
//			csName = csFileName;
		return csName;
	}
	
	private void loadImageList()
	{
		List<String> imagesList = ExperimentDirectories.getV2ImagesListFromPath(imagesDirectory);
		imagesList = ExperimentDirectories.keepOnlyAcceptedNames_List(imagesList, "jpg");
		if (imagesList.size() >0) 
		{
			setImagesList(imagesList);
			attachSequence(loadSequenceFromImagesList(imagesList));
		}
	}
	
	public String getFileNameNoPath(int t) 
	{
		String csName = null;
		csName = imagesList.get(t);
		if (csName != null) 
		{
			Path path = Paths.get(csName);
			return path.getName(path.getNameCount()-1).toString();
		}
		return csName;
	}
	
	// --------------------------
	
	public IcyBufferedImage getSeqImage(int t, int z) 
	{
		currentFrame = t;
		return seq.getImage(t, z);
	}
	
	// --------------------------
	
	String fileComponent(String fname) {
	      int pos = fname.lastIndexOf("/");
	      if(pos > -1)
	         return fname.substring(pos + 1);
	      else
	         return fname;
	   }
	
	public FileTime getFileTimeFromStructuredName(int t) 
	{
		long timeInMs = 0;
		String fileName = fileComponent(getFileNameFromImageList(t));
		
		if (fileName == null) 
		{
			timeInMs = timePatternArray[0].getDummyTime(t);
		} 
		else 
		{
			if (indexTimePattern < 0) 
			{
				indexTimePattern = findProperFilterIfAny(fileName);
			}
			FileNameTimePattern tp = timePatternArray[indexTimePattern];
			timeInMs = tp.getTimeFromString(fileName, t);   
		}

		FileTime fileTime = FileTime.fromMillis(timeInMs);		
		return fileTime;
	}
	
	int findProperFilterIfAny(String fileName) 
	{
		int index = 0;
		for (int i = 1; i < timePatternArray.length; i++) 
		{
			if (timePatternArray[i].findMatch(fileName)) 
				return i;
		}
		return index;
	}
	
	public FileTime getFileTimeFromFileAttributes(int t) 
	{
		FileTime filetime=null;
		File file = new File(getFileNameFromImageList(t));
        Path filePath = file.toPath();

        BasicFileAttributes attributes = null;
        try 
        {
            attributes = Files.readAttributes(filePath, BasicFileAttributes.class);
        }
        catch (IOException exception) 
        {
            System.out.println("SeqCamData:getFileTimeFromFileAttributes() Exception handled when trying to get file " +
                    "attributes: " + exception.getMessage());
        }
        
        long milliseconds = attributes.creationTime().to(TimeUnit.MILLISECONDS);
        if((milliseconds > Long.MIN_VALUE) && (milliseconds < Long.MAX_VALUE)) 
        {
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
		} 
		catch (ImageProcessingException e) 
		{
			e.printStackTrace();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		return filetime;
	}
	
	public List<Cage> getCagesFromROIs () 
	{
		List<ROI2D> roiList = seq.getROI2Ds();
		Collections.sort(roiList, new Comparators.ROI2D_Name_Comparator());
		List<Cage> cageList = new ArrayList<Cage>();
		for ( ROI2D roi : roiList ) 
		{
			String csName = roi.getName();
			if (!(roi instanceof ROI2DPolygon))
				continue;
			if ((csName.length() > 4 && csName.substring( 0 , 4 ).contains("cage")
				|| csName.contains("Polygon2D")) ) 
			{
				Cage cage = new Cage();
				cage.cageRoi2D = roi;
				cageList.add(cage);
			}
		}
		return cageList;
	}

	public void displayViewerAtRectangle(Rectangle parent0Rect) 
	{
		try 
		{
			SwingUtilities.invokeAndWait(new Runnable() 
			{ 
				public void run() 
				{
					Viewer v = seq.getFirstViewer();
					if (v == null)
						v = new Viewer(seq, true);
					Rectangle rectv = v.getBoundsInternal();
					rectv.setLocation(parent0Rect.x+ parent0Rect.width, parent0Rect.y);
					v.setBounds(rectv);				
				}});
		} 
		catch (InvocationTargetException | InterruptedException e) 
		{
			e.printStackTrace();
		} 
	}
	
	// ------------------------

	public void closeSequence() 
	{
		if (seq == null)
			return;

		seq.removeAllROI();
		seq.close();
	}

	public void setImagesList(List <String> extImagesList) 
	{
		imagesList.clear();
		imagesList.addAll(extImagesList);
		nTotalFrames = imagesList.size();
		status = EnumStatus.FILESTACK;
	}
	
	public void attachSequence(Sequence seq)
	{
		this.seq = seq;
		status = EnumStatus.FILESTACK;	
		seqAnalysisStart = 0;
	}
	
	public void completeSequence(Sequence seq2)
	{
		if (seq != null) {
			ArrayList<ROI> listROIS = seq.getROIs();
			seq2.addROIs(listROIS, false);
		}
		seq = seq2;
		status = EnumStatus.FILESTACK;	
		seqAnalysisStart = 0;
	}
	
	public IcyBufferedImage imageIORead(String name) 
	{
    	BufferedImage image = null;
		try 
		{
	    	image = ImageIO.read(new File(name));
		} 
		catch (IOException e) 
		{
			 e.printStackTrace();
		}
		return IcyBufferedImage.createFrom(image);
	}
	
	public boolean loadImages() 
	{
		if (imagesList.size() == 0)
			return false;
		attachSequence(loadSequenceFromImagesList(imagesList));
		return (seq != null);
	}
	
	public boolean loadFirstImage() 
	{
		if (imagesList.size() == 0)
			return false;
		List <String> dummyList = new ArrayList<String>();
		dummyList.add(imagesList.get(0));
		attachSequence(loadSequenceFromImagesList(dummyList));
		return (seq != null);
	}
	
	public Sequence loadSequenceFromImagesList(List <String> imagesList) 
	{
		SequenceFileImporter seqFileImporter = Loader.getSequenceFileImporter(imagesList.get(0), true);
		Sequence seq = Loader.loadSequences(seqFileImporter, imagesList, 
				0,          // series index to load
			    true, 		// force volatile 
			    false,      // separate       
			    false,      // auto-order
			    false,      // directory
			    false,      // add to recent
			    false 		// show progress
			).get(0);
		return seq;
	}

	public Sequence initSequenceFromFirstImage(List <String> imagesList) 
	{
		SequenceFileImporter seqFileImporter = Loader.getSequenceFileImporter(imagesList.get(0), true);
		Sequence seq = Loader.loadSequence(seqFileImporter, imagesList.get(0), 0, false);
        return seq;
	}
	
}