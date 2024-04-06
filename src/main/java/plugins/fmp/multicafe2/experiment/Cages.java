package plugins.fmp.multicafe2.experiment;

import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import icy.roi.ROI;
import icy.roi.ROI2D;
import icy.util.XMLUtil;
import plugins.fmp.multicafe2.dlg.JComponents.Dialog;
import plugins.fmp.multicafe2.tools.Comparators;
import plugins.kernel.roi.roi2d.ROI2DArea;
import plugins.kernel.roi.roi2d.ROI2DPolygon;
import plugins.kernel.roi.roi2d.ROI2DShape;


public class Cages 
{	
	public ArrayList<Cage>	cagesList		= new ArrayList<Cage>();

	// ---------- not saved to xml:
	public long			detectFirst_Ms		= 0;
	public long			detectLast_Ms		= 0;
	public long			detectBin_Ms		= 60000;
	public int			detect_threshold	= 0;
	public int			detect_nframes		= 0;
	
	// ----------------------------

	private final String ID_CAGES 			= "Cages";
	private final String ID_NCAGES 			= "n_cages";
	private final String ID_DROSOTRACK 		= "drosoTrack";
	private final String ID_NBITEMS 		= "nb_items";
	private final String ID_CAGELIMITS 		= "Cage_Limits";
	private final String ID_FLYDETECTED 	= "Fly_Detected";
	
	private final static String ID_MCDROSOTRACK_XML = "MCdrosotrack.xml";
	
	
	// ---------------------------------
	
	public boolean load_Cages(String directory) 
	{
		boolean flag = false;
		try 
		{
			flag = csvLoad_Cages(directory);
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (!flag) {
			String tempName = directory + File.separator + ID_MCDROSOTRACK_XML;
			final Document doc = XMLUtil.loadDocument(tempName);
			if (doc == null)
				return false;
			flag = xmlLoadCages(doc);
		}
		return flag;
	}
	
	public boolean save_Cages(String directory) 
	{
		if (directory == null)
			return false;
		
		csvSave_Cages(directory);
		return true;
	}
		
	// ---------------------------------
		
	public void clearAllMeasures(int option_detectCage) 
	{
		for (Cage cage: cagesList) 
		{
			int cagenb = cage.getCageNumberInteger();
			if (option_detectCage < 0 || option_detectCage == cagenb)
				cage.clearMeasures();
		}
	}
	
	public void removeCages() 
	{
		cagesList.clear();
	}
	
	public void mergeLists(Cages cagesm) 
	{
		for (Cage cagem : cagesm.cagesList ) 
		{
			if (!isPresent(cagem))
				cagesList.add(cagem);
		}
	}
	
	// -----------------------------------------------------
	
	final String csvSep = ";";

	private boolean csvLoad_Cages(String directory) throws Exception 
	{
		String pathToCsv = directory + File.separator +"CagesMeasures.csv";
		File csvFile = new File(pathToCsv);
		if (!csvFile.isFile()) 
			return false;
		
		BufferedReader csvReader = new BufferedReader(new FileReader(pathToCsv));
		String row;
		String sep = csvSep;
		while ((row = csvReader.readLine()) != null) {
			if (row.charAt(0) == '#') 
				sep = String.valueOf(row.charAt(1));
			
		    String[] data = row.split(sep);
		    if (data[0] .equals( "#")) 
		    {
		    	switch(data[1]) 
		    	{
		    	case "DESCRIPTION":
		    		csvLoad_DESCRIPTION (csvReader, sep);
		    		break;
		    	case "CAGES":
		    		csvLoad_CAGES (csvReader, sep);
		    		break;
		    	case "POSITION":
		    		csvLoad_Measures(csvReader, EnumCageMeasures.POSITION, sep);
		    		break;
		  
	    		default:
	    			break;
		    	}
		    }
		}
		csvReader.close();
		return true;
	}
	
	private String csvLoad_DESCRIPTION (BufferedReader csvReader, String sep) 
	{
		String row;
		try {
			while ((row = csvReader.readLine()) != null) 
			{
				String[] data = row.split(sep);
				if (data[0] .equals("#")) 
					return data[1];
				
				if ( data[0].substring(0, Math.min( data[0].length(), 7)).equals("n cages")) 
				{
					int ncages = Integer.valueOf(data[1]);
					if (ncages >= cagesList.size())
						cagesList.ensureCapacity(ncages);
					else
						cagesList.subList(ncages, cagesList.size()).clear();
				}
			}
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		return null;
	}
	
	private String csvLoad_CAGES (BufferedReader csvReader, String sep) 
	{
		String row;
		try 
		{
			row = csvReader.readLine();			
			while ((row = csvReader.readLine()) != null) 
			{
				String[] data = row.split(sep);
				if (data[0] .equals("#")) 
					return data[1];
				
				int cageID = Integer.valueOf(data[0]);
				Cage cage = getCageFromNumber(cageID);
				if (cage == null) 
				{
					cage = new Cage();
					cagesList.add(cage);
				}
				cage.csvImport_CAGE_Header(data);
			}
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		return null;
	}
	
	private String csvLoad_Measures(BufferedReader csvReader, EnumCageMeasures measureType, String sep) 
	{
		String row;
		try 
		{
			row = csvReader.readLine();
			boolean complete = row.contains("w(i)");
			while ((row = csvReader.readLine()) != null) 
			{
				String[] data = row.split(sep);
				if (data[0] .equals("#")) 
					return data[1];
				
				int cageID = Integer.valueOf(data[0]);
				Cage cage = getCageFromNumber(cageID);
				if (cage == null)
					cage = new Cage();
				cage.csvImport_MEASURE_Data(measureType, data, complete);
			}
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		return null;
	}
	
	// ---------------------------------
	
	private boolean csvSave_Cages(String directory) 
	{
		Path path = Paths.get(directory);
		if (!Files.exists(path))
			return false;
		
		try {
			FileWriter csvWriter = new FileWriter(directory + File.separator +"CagesMeasures.csv");
			csvSave_Description(csvWriter);
			csvSave_Measures(csvWriter, EnumCageMeasures.POSITION);
			csvWriter.flush();
			csvWriter.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return true;
	}
	
	private boolean csvSave_Description(FileWriter csvWriter) 
	{
		try {
			csvWriter.append("#"+csvSep+"DESCRIPTION\n");
			csvWriter.append("n cages="+csvSep + Integer.toString(cagesList.size()) + "\n");
			
			csvWriter.append("#"+csvSep+"#\n");
			
			if (cagesList.size() > 0) {
				csvWriter.append(cagesList.get(0).csvExport_CAGES_Header(csvSep));
				for (Cage cage:cagesList) 
					csvWriter.append(cage.csvExport_CAGES_Data(csvSep));
				csvWriter.append("#"+csvSep+"#\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return true;
	}
	
	private boolean csvSave_Measures(FileWriter csvWriter, EnumCageMeasures measureType) 
	{
		try 
		{
			if (cagesList.size() <= 1)
				return false;
			boolean complete = true;
			csvWriter.append(cagesList.get(0).csvExport_MEASURE_Header(measureType, csvSep, complete));
			for (Cage cage:cagesList) 
				csvWriter.append(cage.csvExport_MEASURE_Data(measureType, csvSep, complete));
			
			csvWriter.append("#"+csvSep+"#\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}
	
	// -------------
	
	public boolean xmlWriteCagesToFileNoQuestion(String tempname) 
	{
		if (tempname == null) 
			return false;
		final Document doc = XMLUtil.createDocument(true);
		if (doc == null)
			return false;
		
		Node node = XMLUtil.addElement(XMLUtil.getRootElement(doc), ID_DROSOTRACK);
		if (node == null)
			return false;

		int index = 0;
		Element xmlVal = XMLUtil.addElement(node, ID_CAGES);
		int ncages = cagesList.size();
		XMLUtil.setAttributeIntValue(xmlVal, ID_NCAGES, ncages);
		for (Cage cage: cagesList) 
		{
			cage.xmlSaveCage(xmlVal, index);
			index++;
		}
	
		return XMLUtil.saveDocument(doc, tempname);
	}
	
	public boolean xmlReadCagesFromFile(Experiment exp) 
	{
		String [] filedummy = null;
		String filename = exp.getExperimentDirectory();
		File file = new File(filename);
		String directory = file.getParentFile().getAbsolutePath();
		filedummy = Dialog.selectFiles(directory, "xml");
		boolean wasOk = false;
		if (filedummy != null) 
		{
			for (int i = 0; i < filedummy.length; i++) 
			{
				String csFile = filedummy[i];
				wasOk &= xmlReadCagesFromFileNoQuestion(csFile, exp);
			}
		}
		return wasOk;
	}
	
	public boolean xmlReadCagesFromFileNoQuestion(String tempname, Experiment exp) 
	{
		if (tempname == null) 
			return false;
		final Document doc = XMLUtil.loadDocument(tempname);
		if (doc == null)
			return false;
		boolean flag = xmlLoadCages(doc); 
		if (flag) 
		{
			cagesToROIs(exp.seqCamData);
		}
		else 
		{
			System.out.println("Cages:xmlReadCagesFromFileNoQuestion() failed to load cages from file");
			return false;
		}
		return true;
	}
	
	private boolean xmlLoadCages (Document doc) 
	{
		Node node = XMLUtil.getElement(XMLUtil.getRootElement(doc), ID_DROSOTRACK);
		if (node == null)
			return false;
		
		cagesList.clear();
		Element xmlVal = XMLUtil.getElement(node, ID_CAGES);
		if (xmlVal != null) 
		{
			int ncages = XMLUtil.getAttributeIntValue(xmlVal, ID_NCAGES, 0);
			for (int index = 0; index < ncages; index++) 
			{
				Cage cage = new Cage();
				cage.xmlLoadCage(xmlVal, index);
				cagesList.add(cage);
			}
		} 
		else 
		{
			List<ROI2D> cageLimitROIList = new ArrayList<ROI2D>();
			if (xmlLoadCagesLimits_v0(node, cageLimitROIList)) 
			{
				List<FlyPositions> flyPositionsList = new ArrayList<FlyPositions>();
				xmlLoadFlyPositions_v0(node, flyPositionsList);
				transferDataToCages_v0(cageLimitROIList, flyPositionsList);
			}
			else
				return false;
		}
		return true;
	}
	
	// --------------
	
	public void copy (Cages cagesSource) 
	{	
//		detect.copyParameters(cag.detect);	
		cagesList.clear();
		for (Cage cageSource: cagesSource.cagesList) 
		{
			Cage cageDestination = new Cage();
			cageDestination.copyCage(cageSource);
			cagesList.add(cageDestination);
		}
	}
	
	// --------------
	
	private void transferDataToCages_v0(List<ROI2D> cageLimitROIList, List<FlyPositions> flyPositionsList) 
	{
		cagesList.clear();
		Collections.sort(cageLimitROIList, new Comparators.ROI2D_Name_Comparator());
		int ncages = cageLimitROIList.size();
		for (int index=0; index< ncages; index++) 
		{
			Cage cage = new Cage();
			cage.cageRoi2D = cageLimitROIList.get(index);
			cage.flyPositions = flyPositionsList.get(index);
			cagesList.add(cage);
		}
	}

	private boolean xmlLoadCagesLimits_v0(Node node, List<ROI2D> cageLimitROIList) 
	{
		if (node == null)
			return false;
		Element xmlVal = XMLUtil.getElement(node, ID_CAGELIMITS);
		if (xmlVal == null) 
			return false;	
		cageLimitROIList.clear();
		int nb_items =  XMLUtil.getAttributeIntValue(xmlVal, ID_NBITEMS, 0);
		for (int i=0; i< nb_items; i++) 
		{
			ROI2DPolygon roi = (ROI2DPolygon) ROI.create("plugins.kernel.roi.roi2d.ROI2DPolygon");
			Element subnode = XMLUtil.getElement(xmlVal, "cage"+i);
			roi.loadFromXML(subnode);
			cageLimitROIList.add((ROI2D) roi);
		}
		return true;
	}
	
	private boolean xmlLoadFlyPositions_v0(Node node, List<FlyPositions> flyPositionsList) 
	{
		if (node == null)
			return false;
		Element xmlVal = XMLUtil.getElement(node, ID_FLYDETECTED);
		if (xmlVal == null) 
			return false;	
		flyPositionsList.clear();
		int nb_items =  XMLUtil.getAttributeIntValue(xmlVal, ID_NBITEMS, 0);
		int ielement = 0;
		for (int i =0; i < nb_items; i++) 
		{
			Element subnode = XMLUtil.getElement(xmlVal, "cage"+ielement);
			FlyPositions pos = new FlyPositions();
			pos.loadXYTseriesFromXML(subnode);
			flyPositionsList.add(pos);
			ielement++;
		}
		return true;
	}
	
	private boolean isPresent(Cage cagenew) 
	{
		boolean flag = false;
		for (Cage cage: cagesList) 
		{
			if (cage.cageRoi2D.getName().contentEquals(cagenew.cageRoi2D.getName())) 
			{
				flag = true;
				break;
			}
		}
		return flag;
	}
	
	private void addMissingCages(List<ROI2D> roiList) 
	{
		for (ROI2D roi:roiList) 
		{
			boolean found = false;
			if (roi.getName() == null)
				break;
			for (Cage cage: cagesList) 
			{
				if (cage.cageRoi2D == null)
					break;
				if (roi.getName().equals(cage.cageRoi2D.getName())) 
				{
					found = true;
					break;
				}
			}
			if (!found) 
			{
				Cage cage = new Cage();
				cage.cageRoi2D = roi;
				cagesList.add(cage);
			}
		}
	}
	
	private void removeOrphanCages(List<ROI2D> roiList) 
	{
		// remove cages with names not in the list
		Iterator<Cage> iterator = cagesList.iterator();
		while (iterator.hasNext()) 
		{
			Cage cage = iterator.next();
			boolean found = false;
			if (cage.cageRoi2D != null) 
			{
				String cageRoiName = cage.cageRoi2D.getName();
				for (ROI2D roi: roiList) 
				{
					if (roi.getName().equals(cageRoiName)) 
					{
						found = true;
						break;
					}
				}
			}
			if (!found ) 
				iterator.remove();
		}
	}
	
	private List <ROI2D> getRoisWithCageName(SequenceCamData seqCamData) 
	{
		List<ROI2D> roiList = seqCamData.seq.getROI2Ds();
		List<ROI2D> cageList = new ArrayList<ROI2D>();
		for ( ROI2D roi : roiList ) 
		{
			String csName = roi.getName();
			if ((roi instanceof ROI2DPolygon) || (roi instanceof ROI2DArea)) {
//				if (( csName.contains( "cage") 
				if ((csName.length() > 4 && csName.substring( 0 , 4 ).contains("cage")
						|| csName.contains("Polygon2D")) ) 
					cageList.add(roi);
			}
		}
		return cageList;
	}
	
	// --------------
	
	public void cagesToROIs(SequenceCamData seqCamData) 
	{
		List <ROI2D> cageLimitROIList = getRoisWithCageName(seqCamData);
		seqCamData.seq.removeROIs(cageLimitROIList, false);
		for (Cage cage: cagesList) 
			cageLimitROIList.add(cage.cageRoi2D);
		seqCamData.seq.addROIs(cageLimitROIList, true);
	}
	
	public void cagesFromROIs(SequenceCamData seqCamData) 
	{
		List <ROI2D> roiList = getRoisWithCageName(seqCamData);
		Collections.sort(roiList, new Comparators.ROI2D_Name_Comparator());
		addMissingCages(roiList);
		removeOrphanCages(roiList);
		Collections.sort(cagesList, new Comparators.Cage_Name_Comparator());
	}
	
	public void setFirstAndLastCageToZeroFly() 
	{
		for (Cage cage: cagesList) 
		{
			if (cage.cageRoi2D.getName().contains("000") || cage.cageRoi2D.getName().contains("009"))
				cage.cageNFlies = 0;
		}
	}
	
	public void removeAllRoiDetFromSequence(SequenceCamData seqCamData) 
	{
		ArrayList<ROI2D> seqlist = seqCamData.seq.getROI2Ds();
		for (ROI2D roi: seqlist) 
		{
			if (!(roi instanceof ROI2DShape))
				continue;
			if (!roi.getName().contains("det"))
				continue;
			seqCamData.seq.removeROI(roi);
		}
	}
	
	public void transferNFliesFromCapillariesToCages(List<Capillary> capList) 
	{
		for (Cage cage: cagesList ) 
		{
			int cagenb = cage.getCageNumberInteger();
			for (Capillary cap: capList) 
			{
				if (cap.capCageID == cagenb) 
				{
					cage.cageNFlies = cap.capNFlies;
					break;
				}
			}
		}
	}
		
	public void transferNFliesFromCagesToCapillaries(List<Capillary> capList) 
	{
		for (Cage cage: cagesList ) 
		{
			int cagenb = cage.getCageNumberInteger();
			for (Capillary cap: capList) 
			{
				if (cap.capCageID != cagenb)
					continue;
				cap.capNFlies = cage.cageNFlies;
			}
		}
	}
	
	public void setCageNbFromName(List<Capillary> capList) 
	{
		for (Capillary cap: capList) 
		{
			int cagenb = cap.getCageIndexFromRoiName();
			cap.capCageID = cagenb;
		}
	}
	
	public Cage getCageFromNumber (int number) 
	{
		Cage cageFound = null;
		for (Cage cage: cagesList) 
		{
			if (number == cage.getCageNumberInteger()) 
			{
				cageFound = cage;
				break;
			}
		}
		return cageFound;
	}

	// ---------------
	
	public List <ROI2D> getPositionsAsListOfROI2DRectanglesAtT(int t) 
	{
		List <ROI2D> roiRectangleList = new ArrayList<ROI2D> (cagesList.size());
		for (Cage cage: cagesList) 
		{
			ROI2D roiRectangle = cage.getRoiRectangleFromPositionAtT(t);
			if (roiRectangle != null)
				roiRectangleList.add(roiRectangle);
		}
		return roiRectangleList;
	}

	public void orderFlyPositions() 
	{
		for (Cage cage: cagesList) 
			Collections.sort(cage.flyPositions.flyPositionList, new Comparators.XYTaValue_Tindex_Comparator());
	}
	
	public void initFlyPositions(int option_cagenumber)
	{
		int nbcages = cagesList.size();
		for (int i = 0; i < nbcages; i++) 
		{
			Cage cage = cagesList.get(i);
			if (option_cagenumber != -1 && cage.getCageNumberInteger() != option_cagenumber)
				continue;
			if (cage.cageNFlies > 0) 
			{
				cage.flyPositions = new FlyPositions();
				cage.flyPositions.ensureCapacity(detect_nframes);
			}
		}
	}
	
	// ----------------
	
	public void computeBooleanMasksForCages() 
	{
		for (Cage cage : cagesList ) {
			try {
				cage.computeCageBooleanMask2D();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
		}
	}
	
	public int getLastIntervalFlyAlive(int cagenumber) 
	{
		int flypos = -1;
		for (Cage cage: cagesList) 
		{
			String cagenumberString = cage.cageRoi2D.getName().substring(4);
			if (Integer.valueOf(cagenumberString) == cagenumber) 
			{
				flypos = cage.flyPositions.getLastIntervalAlive();
				break;
			}
		}
		return flypos;
	}
	
	public boolean isFlyAlive(int cagenumber) 
	{
		boolean isalive = false;
		for (Cage cage: cagesList) 
		{
			String cagenumberString = cage.cageRoi2D.getName().substring(4);
			if (Integer.valueOf(cagenumberString) == cagenumber) 
			{
				isalive = (cage.flyPositions.getLastIntervalAlive() > 0);
				break;
			}
		}
		return isalive;
	}
	
	public boolean isDataAvailable(int cagenumber) 
	{
		boolean isavailable = false;
		for (Cage cage: cagesList) 
		{
			String cagenumberString = cage.cageRoi2D.getName().substring(4);
			if (Integer.valueOf(cagenumberString) == cagenumber) 
			{
				isavailable = true;
				break;
			}
		}
		return isavailable;
	}

	public int getHorizontalSpanOfCages() 
	{
		int leftPixel = -1;
		int rightPixel = -1;
		
		for (Cage cage: cagesList) {
			ROI2D roiCage = cage.cageRoi2D;
			Rectangle2D rect = roiCage.getBounds2D();
			int left = (int) rect.getX();
			int right = left + (int) rect.getWidth();
			if (leftPixel < 0 || left < leftPixel) leftPixel = left;
			if (right > rightPixel) rightPixel = right;
		}
		
		return rightPixel - leftPixel;
	}

}
