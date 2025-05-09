package plugins.fmp.multicafe.experiment.cage;

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
import plugins.fmp.multicafe.experiment.Experiment;
import plugins.fmp.multicafe.experiment.SequenceCamData;
import plugins.fmp.multicafe.experiment.capillaries.Capillary;
import plugins.fmp.multicafe.tools.Comparators;
import plugins.fmp.multicafe.tools.JComponents.Dialog;
import plugins.kernel.roi.roi2d.ROI2DArea;
import plugins.kernel.roi.roi2d.ROI2DPolygon;
import plugins.kernel.roi.roi2d.ROI2DShape;

public class Cage {
	public ArrayList<Cell> cellList = new ArrayList<Cell>();

	// ---------- not saved to xml:
	public long detectFirst_Ms = 0;
	public long detectLast_Ms = 0;
	public long detectBin_Ms = 60000;
	public int detect_threshold = 0;
	public int detect_nframes = 0;

	// ----------------------------

	private final String ID_CAGES = "Cages";
	private final String ID_NCAGES = "n_cages";
	private final String ID_DROSOTRACK = "drosoTrack";
	private final String ID_NBITEMS = "nb_items";
	private final String ID_CAGELIMITS = "Cage_Limits";
	private final String ID_FLYDETECTED = "Fly_Detected";

	private final static String ID_MCDROSOTRACK_XML = "MCdrosotrack.xml";

	// ---------------------------------

	public boolean load_Cage(String directory) {
		boolean flag = false;
		try {
			flag = csvLoad_Cage(directory);
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (!flag) {
			String tempName = directory + File.separator + ID_MCDROSOTRACK_XML;
			final Document doc = XMLUtil.loadDocument(tempName);
			if (doc == null)
				return false;
			flag = xmlLoadCage(doc);
		}
		return flag;
	}

	public boolean save_Cage(String directory) {
		if (directory == null)
			return false;

		csvSave_Cage(directory);
		return true;
	}

	// ---------------------------------

	public void clearAllMeasures(int option_detectCage) {
		for (Cell cell : cellList) {
			int cellnb = cell.getCellNumberInteger();
			if (option_detectCage < 0 || option_detectCage == cellnb)
				cell.clearMeasures();
		}
	}

	public void clearCellList() {
		cellList.clear();
	}

	public void mergeLists(Cage cagem) {
		for (Cell cellm : cagem.cellList) {
			if (!isPresent(cellm))
				cellList.add(cellm);
		}
	}

	// -----------------------------------------------------

	final String csvSep = ";";

	private boolean csvLoad_Cage(String directory) throws Exception {
		String pathToCsv = directory + File.separator + "CagesMeasures.csv";
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
			if (data[0].equals("#")) {
				switch (data[1]) {
				case "DESCRIPTION":
					csvLoad_DESCRIPTION(csvReader, sep);
					break;
				case "CAGES":
					csvLoad_CAGE(csvReader, sep);
					break;
				case "POSITION":
					csvLoad_Measures(csvReader, EnumCellMeasures.POSITION, sep);
					break;

				default:
					break;
				}
			}
		}
		csvReader.close();
		return true;
	}

	private String csvLoad_DESCRIPTION(BufferedReader csvReader, String sep) {
		String row;
		try {
			while ((row = csvReader.readLine()) != null) {
				String[] data = row.split(sep);
				if (data[0].equals("#"))
					return data[1];

				String test = data[0].substring(0, Math.min(data[0].length(), 7));
				if (test.equals("n cages") || test.equals("n cells")) {
					int ncells = Integer.valueOf(data[1]);
					if (ncells >= cellList.size())
						cellList.ensureCapacity(ncells);
					else
						cellList.subList(ncells, cellList.size()).clear();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private String csvLoad_CAGE(BufferedReader csvReader, String sep) {
		String row;
		try {
			row = csvReader.readLine();
			while ((row = csvReader.readLine()) != null) {
				String[] data = row.split(sep);
				if (data[0].equals("#"))
					return data[1];

				int cageID = Integer.valueOf(data[0]);
				Cell cell = getCellFromNumber(cageID);
				if (cell == null) {
					cell = new Cell();
					cellList.add(cell);
				}
				cell.csvImport_CAGE_Header(data);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private String csvLoad_Measures(BufferedReader csvReader, EnumCellMeasures measureType, String sep) {
		String row;
		try {
			row = csvReader.readLine();
			boolean complete = row.contains("w(i)");
			while ((row = csvReader.readLine()) != null) {
				String[] data = row.split(sep);
				if (data[0].equals("#"))
					return data[1];

				int cellID = Integer.valueOf(data[0]);
				Cell cell = getCellFromNumber(cellID);
				if (cell == null)
					cell = new Cell();
				cell.csvImport_MEASURE_Data(measureType, data, complete);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	// ---------------------------------

	private boolean csvSave_Cage(String directory) {
		Path path = Paths.get(directory);
		if (!Files.exists(path))
			return false;

		try {
			FileWriter csvWriter = new FileWriter(directory + File.separator + "CagesMeasures.csv");
			csvSave_Description(csvWriter);
			csvSave_Measures(csvWriter, EnumCellMeasures.POSITION);
			csvWriter.flush();
			csvWriter.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

		return true;
	}

	private boolean csvSave_Description(FileWriter csvWriter) {
		try {
			csvWriter.append("#" + csvSep + "DESCRIPTION\n");
			csvWriter.append("n cells=" + csvSep + Integer.toString(cellList.size()) + "\n");

			csvWriter.append("#" + csvSep + "#\n");

			if (cellList.size() > 0) {
				csvWriter.append(cellList.get(0).csvExport_CELL_Header(csvSep));
				for (Cell cell : cellList)
					csvWriter.append(cell.csvExport_CELL_Data(csvSep));
				csvWriter.append("#" + csvSep + "#\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return true;
	}

	private boolean csvSave_Measures(FileWriter csvWriter, EnumCellMeasures measureType) {
		try {
			if (cellList.size() <= 1)
				return false;
			boolean complete = true;
			csvWriter.append(cellList.get(0).csvExport_MEASURE_Header(measureType, csvSep, complete));
			for (Cell cell : cellList)
				csvWriter.append(cell.csvExport_MEASURE_Data(measureType, csvSep, complete));

			csvWriter.append("#" + csvSep + "#\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}

	// -------------

	public boolean xmlWriteCageToFileNoQuestion(String tempname) {
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
		int ncells = cellList.size();
		XMLUtil.setAttributeIntValue(xmlVal, ID_NCAGES, ncells);
		for (Cell cell : cellList) {
			cell.xmlSaveCell(xmlVal, index);
			index++;
		}

		return XMLUtil.saveDocument(doc, tempname);
	}

	public boolean xmlReadCageFromFile(Experiment exp) {
		String[] filedummy = null;
		String filename = exp.getExperimentDirectory();
		File file = new File(filename);
		String directory = file.getParentFile().getAbsolutePath();
		filedummy = Dialog.selectFiles(directory, "xml");
		boolean wasOk = false;
		if (filedummy != null) {
			for (int i = 0; i < filedummy.length; i++) {
				String csFile = filedummy[i];
				wasOk &= xmlReadCageFromFileNoQuestion(csFile, exp);
			}
		}
		return wasOk;
	}

	public boolean xmlReadCageFromFileNoQuestion(String tempname, Experiment exp) {
		if (tempname == null)
			return false;
		final Document doc = XMLUtil.loadDocument(tempname);
		if (doc == null)
			return false;
		boolean flag = xmlLoadCage(doc);
		if (flag) {
			cageToROIs(exp.seqCamData);
		} else {
			System.out.println("Cages:xmlReadCageFromFileNoQuestion() failed to load cages from file");
			return false;
		}
		return true;
	}

	private boolean xmlLoadCage(Document doc) {
		Node node = XMLUtil.getElement(XMLUtil.getRootElement(doc), ID_DROSOTRACK);
		if (node == null)
			return false;

		cellList.clear();
		Element xmlVal = XMLUtil.getElement(node, ID_CAGES);
		if (xmlVal != null) {
			int ncells = XMLUtil.getAttributeIntValue(xmlVal, ID_NCAGES, 0);
			for (int index = 0; index < ncells; index++) {
				Cell cell = new Cell();
				cell.xmlLoadCell(xmlVal, index);
				cellList.add(cell);
			}
		} else {
			List<ROI2D> cellLimitROIList = new ArrayList<ROI2D>();
			if (xmlLoadCageLimits_v0(node, cellLimitROIList)) {
				List<FlyPositions> flyPositionsList = new ArrayList<FlyPositions>();
				xmlLoadFlyPositions_v0(node, flyPositionsList);
				transferDataToCage_v0(cellLimitROIList, flyPositionsList);
			} else
				return false;
		}
		return true;
	}

	// --------------

	public void copy(Cage cageSource) {
//		detect.copyParameters(cag.detect);	
		cellList.clear();
		for (Cell cellSource : cageSource.cellList) {
			Cell cellDestination = new Cell();
			cellDestination.copyCell(cellSource);
			cellList.add(cellDestination);
		}
	}

	// --------------

	private void transferDataToCage_v0(List<ROI2D> cellLimitROIList, List<FlyPositions> flyPositionsList) {
		cellList.clear();
		Collections.sort(cellLimitROIList, new Comparators.ROI2D_Name_Comparator());
		int ncells = cellLimitROIList.size();
		for (int index = 0; index < ncells; index++) {
			Cell cell = new Cell();
			cell.cellRoi2D = cellLimitROIList.get(index);
			cell.flyPositions = flyPositionsList.get(index);
			cellList.add(cell);
		}
	}

	private boolean xmlLoadCageLimits_v0(Node node, List<ROI2D> cageLimitROIList) {
		if (node == null)
			return false;
		Element xmlVal = XMLUtil.getElement(node, ID_CAGELIMITS);
		if (xmlVal == null)
			return false;
		cageLimitROIList.clear();
		int nb_items = XMLUtil.getAttributeIntValue(xmlVal, ID_NBITEMS, 0);
		for (int i = 0; i < nb_items; i++) {
			ROI2DPolygon roi = (ROI2DPolygon) ROI.create("plugins.kernel.roi.roi2d.ROI2DPolygon");
			Element subnode = XMLUtil.getElement(xmlVal, "cage" + i);
			roi.loadFromXML(subnode);
			cageLimitROIList.add((ROI2D) roi);
		}
		return true;
	}

	private boolean xmlLoadFlyPositions_v0(Node node, List<FlyPositions> flyPositionsList) {
		if (node == null)
			return false;
		Element xmlVal = XMLUtil.getElement(node, ID_FLYDETECTED);
		if (xmlVal == null)
			return false;
		flyPositionsList.clear();
		int nb_items = XMLUtil.getAttributeIntValue(xmlVal, ID_NBITEMS, 0);
		int ielement = 0;
		for (int i = 0; i < nb_items; i++) {
			Element subnode = XMLUtil.getElement(xmlVal, "cage" + ielement);
			FlyPositions pos = new FlyPositions();
			pos.loadXYTseriesFromXML(subnode);
			flyPositionsList.add(pos);
			ielement++;
		}
		return true;
	}

	private boolean isPresent(Cell cellNew) {
		boolean flag = false;
		for (Cell cell : cellList) {
			if (cell.cellRoi2D.getName().contentEquals(cellNew.cellRoi2D.getName())) {
				flag = true;
				break;
			}
		}
		return flag;
	}

	private void addMissingCells(List<ROI2D> roiList) {
		for (ROI2D roi : roiList) {
			boolean found = false;
			if (roi.getName() == null)
				break;
			for (Cell cell : cellList) {
				if (cell.cellRoi2D == null)
					break;
				if (roi.getName().equals(cell.cellRoi2D.getName())) {
					found = true;
					break;
				}
			}
			if (!found) {
				Cell cell = new Cell();
				cell.cellRoi2D = roi;
				cellList.add(cell);
			}
		}
	}

	private void removeOrphanCells(List<ROI2D> roiList) {
		// remove cells with names not in the list
		Iterator<Cell> iterator = cellList.iterator();
		while (iterator.hasNext()) {
			Cell cell = iterator.next();
			boolean found = false;
			if (cell.cellRoi2D != null) {
				String cageRoiName = cell.cellRoi2D.getName();
				for (ROI2D roi : roiList) {
					if (roi.getName().equals(cageRoiName)) {
						found = true;
						break;
					}
				}
			}
			if (!found)
				iterator.remove();
		}
	}

	private List<ROI2D> getRoisWithCageName(SequenceCamData seqCamData) {
		List<ROI2D> roiList = seqCamData.seq.getROI2Ds();
		List<ROI2D> cageList = new ArrayList<ROI2D>();
		for (ROI2D roi : roiList) {
			String csName = roi.getName();
			if ((roi instanceof ROI2DPolygon) || (roi instanceof ROI2DArea)) {
//				if (( csName.contains( "cage") 
				if ((csName.length() > 4 && csName.substring(0, 4).contains("cage") || csName.contains("Polygon2D")))
					cageList.add(roi);
			}
		}
		return cageList;
	}

	// --------------

	public void cageToROIs(SequenceCamData seqCamData) {
		List<ROI2D> cageLimitROIList = getRoisWithCageName(seqCamData);
		seqCamData.seq.removeROIs(cageLimitROIList, false);
		for (Cell cell : cellList)
			cageLimitROIList.add(cell.cellRoi2D);
		seqCamData.seq.addROIs(cageLimitROIList, true);
	}

	public void cageFromROIs(SequenceCamData seqCamData) {
		List<ROI2D> roiList = getRoisWithCageName(seqCamData);
		Collections.sort(roiList, new Comparators.ROI2D_Name_Comparator());
		addMissingCells(roiList);
		removeOrphanCells(roiList);
		Collections.sort(cellList, new Comparators.Cage_Name_Comparator());
	}

	public void setFirstAndLastCellToZeroFly() {
		for (Cell cell : cellList) {
			if (cell.cellRoi2D.getName().contains("000") || cell.cellRoi2D.getName().contains("009"))
				cell.cellNFlies = 0;
		}
	}

	public void removeAllRoiDetFromSequence(SequenceCamData seqCamData) {
		ArrayList<ROI2D> seqlist = seqCamData.seq.getROI2Ds();
		for (ROI2D roi : seqlist) {
			if (!(roi instanceof ROI2DShape))
				continue;
			if (!roi.getName().contains("det"))
				continue;
			seqCamData.seq.removeROI(roi);
		}
	}

	public void transferNFliesFromCapillariesToCage(List<Capillary> capList) {
		for (Cell cell : cellList) {
			int cagenb = cell.getCellNumberInteger();
			for (Capillary cap : capList) {
				if (cap.capCellID == cagenb) {
					cell.cellNFlies = cap.capNFlies;
					break;
				}
			}
		}
	}

	public void transferNFliesFromCageToCapillaries(List<Capillary> capList) {
		for (Cell cell : cellList) {
			int cellnb = cell.getCellNumberInteger();
			for (Capillary cap : capList) {
				if (cap.capCellID != cellnb)
					continue;
				cap.capNFlies = cell.cellNFlies;
			}
		}
	}

	public void setCellNbFromName(List<Capillary> capList) {
		for (Capillary cap : capList) {
			int cellnb = cap.getCellIndexFromRoiName();
			cap.capCellID = cellnb;
		}
	}

	public Cell getCellFromNumber(int number) {
		Cell cellFound = null;
		for (Cell cell : cellList) {
			if (number == cell.getCellNumberInteger()) {
				cellFound = cell;
				break;
			}
		}
		return cellFound;
	}

	// ---------------

	public List<ROI2D> getPositionsAsListOfROI2DRectanglesAtT(int t) {
		List<ROI2D> roiRectangleList = new ArrayList<ROI2D>(cellList.size());
		for (Cell cell : cellList) {
			ROI2D roiRectangle = cell.getRoiRectangleFromPositionAtT(t);
			if (roiRectangle != null)
				roiRectangleList.add(roiRectangle);
		}
		return roiRectangleList;
	}

	public void orderFlyPositions() {
		for (Cell cell : cellList)
			Collections.sort(cell.flyPositions.flyPositionList, new Comparators.XYTaValue_Tindex_Comparator());
	}

	public void initFlyPositions(int option_cagenumber) {
		int nbcells = cellList.size();
		for (int i = 0; i < nbcells; i++) {
			Cell cell = cellList.get(i);
			if (option_cagenumber != -1 && cell.getCellNumberInteger() != option_cagenumber)
				continue;
			if (cell.cellNFlies > 0) {
				cell.flyPositions = new FlyPositions();
				cell.flyPositions.ensureCapacity(detect_nframes);
			}
		}
	}

	// ----------------

	public void computeBooleanMasksForCells() {
		for (Cell cell : cellList) {
			try {
				cell.computeCageBooleanMask2D();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public int getLastIntervalFlyAlive(int cagenumber) {
		int flypos = -1;
		for (Cell cell : cellList) {
			String cellNumberString = cell.cellRoi2D.getName().substring(4);
			if (Integer.valueOf(cellNumberString) == cagenumber) {
				flypos = cell.flyPositions.getLastIntervalAlive();
				break;
			}
		}
		return flypos;
	}

	public boolean isFlyAlive(int cagenumber) {
		boolean isalive = false;
		for (Cell cell : cellList) {
			String cellNumberString = cell.cellRoi2D.getName().substring(4);
			if (Integer.valueOf(cellNumberString) == cagenumber) {
				isalive = (cell.flyPositions.getLastIntervalAlive() > 0);
				break;
			}
		}
		return isalive;
	}

	public boolean isDataAvailable(int cagenumber) {
		boolean isavailable = false;
		for (Cell cell : cellList) {
			String cellNumberString = cell.cellRoi2D.getName().substring(4);
			if (Integer.valueOf(cellNumberString) == cagenumber) {
				isavailable = true;
				break;
			}
		}
		return isavailable;
	}

	public int getHorizontalSpanOfCells() {
		int leftPixel = -1;
		int rightPixel = -1;

		for (Cell cell : cellList) {
			ROI2D roiCell = cell.cellRoi2D;
			Rectangle2D rect = roiCell.getBounds2D();
			int left = (int) rect.getX();
			int right = left + (int) rect.getWidth();
			if (leftPixel < 0 || left < leftPixel)
				leftPixel = left;
			if (right > rightPixel)
				rightPixel = right;
		}

		return rightPixel - leftPixel;
	}

}
