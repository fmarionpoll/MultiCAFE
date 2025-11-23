package plugins.fmp.multicafe.experiment.cages;

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
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import icy.roi.ROI;
import icy.roi.ROI2D;
import icy.util.XMLUtil;
import plugins.fmp.multicafe.experiment.Experiment;
import plugins.fmp.multicafe.tools.Comparators;
import plugins.fmp.multicafe.tools.Logger;
import plugins.fmp.multicafe.tools.JComponents.Dialog;
import plugins.kernel.roi.roi2d.ROI2DPolygon;

public class CagesPersistence {

	private final String ID_CAGES = "Cages";
	private final String ID_NCAGES = "n_cages";
	private final String ID_DROSOTRACK = "drosoTrack";
	private final String ID_NBITEMS = "nb_items";
	private final String ID_CAGELIMITS = "Cage_Limits";
	private final String ID_FLYDETECTED = "Fly_Detected";

	private final static String ID_MCDROSOTRACK_XML = "MCdrosotrack.xml";
	private final String csvSep = ";";

	public boolean load_Cages(Cages cages, String directory) {
		boolean flag = false;
		try {
			flag = csvLoad_CageBox(cages, directory);
		} catch (Exception e) {
			Logger.error("CagesPersistence:load_Cages() Failed to load cages from CSV: " + directory, e, true);
		}

		if (!flag) {
			String tempName = directory + File.separator + ID_MCDROSOTRACK_XML;
			final Document doc = XMLUtil.loadDocument(tempName);
			if (doc == null)
				return false;
			flag = xmlLoadCages(cages, doc);
		}
		return flag;
	}

	public boolean save_Cages(Cages cages, String directory) {
		if (directory == null)
			return false;

		csvSave_Cages(cages, directory);
		return true;
	}

	public boolean xmlWriteCagesToFileNoQuestion(Cages cages, String tempname) {
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
		int nCages = cages.cageList.size();
		XMLUtil.setAttributeIntValue(xmlVal, ID_NCAGES, nCages);
		for (Cage cage : cages.cageList) {
			cage.xmlSaveCagel(xmlVal, index);
			index++;
		}
		return XMLUtil.saveDocument(doc, tempname);
	}

	public boolean xmlReadCagesFromFile(Cages cages, Experiment exp) {
		String[] filedummy = null;
		String filename = exp.getExperimentDirectory();
		File file = new File(filename);
		String directory = file.getParentFile().getAbsolutePath();
		filedummy = Dialog.selectFiles(directory, "xml");
		boolean wasOk = false;
		if (filedummy != null) {
			for (int i = 0; i < filedummy.length; i++) {
				String csFile = filedummy[i];
				wasOk &= xmlReadCagesFromFileNoQuestion(cages, csFile, exp);
			}
		}
		return wasOk;
	}

	public boolean xmlReadCagesFromFileNoQuestion(Cages cages, String tempname, Experiment exp) {
		if (tempname == null)
			return false;
		final Document doc = XMLUtil.loadDocument(tempname);
		if (doc == null)
			return false;
		boolean flag = xmlLoadCages(cages, doc);
		if (flag) {
			cages.cagesToROIs(exp.getSeqCamData());
		} else {
			Logger.warn("CagesPersistence:xmlReadCageFromFileNoQuestion() failed to load cages from file");
			return false;
		}
		return true;
	}

	private boolean xmlLoadCages(Cages cages, Document doc) {
		Node node = XMLUtil.getElement(XMLUtil.getRootElement(doc), ID_DROSOTRACK);
		if (node == null)
			return false;

		cages.cageList.clear();
		Element xmlVal = XMLUtil.getElement(node, ID_CAGES);
		if (xmlVal != null) {
			int nCages = XMLUtil.getAttributeIntValue(xmlVal, ID_NCAGES, 0);
			for (int index = 0; index < nCages; index++) {
				Cage cage = new Cage();
				cage.xmlLoadCage(xmlVal, index);
				cages.cageList.add(cage);
			}
		} else {
			List<ROI2D> cageLimitROIList = new ArrayList<ROI2D>();
			if (xmlLoadCageLimits_v0(node, cageLimitROIList)) {
				List<FlyPositions> flyPositionsList = new ArrayList<FlyPositions>();
				xmlLoadFlyPositions_v0(node, flyPositionsList);
				transferDataToCageBox_v0(cages, cageLimitROIList, flyPositionsList);
			} else
				return false;
		}
		return true;
	}

	private void transferDataToCageBox_v0(Cages cages, List<ROI2D> cageLimitROIList,
			List<FlyPositions> flyPositionsList) {
		cages.cageList.clear();
		Collections.sort(cageLimitROIList, new Comparators.ROI2D_Name_Comparator());
		int nCages = cageLimitROIList.size();
		for (int index = 0; index < nCages; index++) {
			Cage cage = new Cage();
			cage.cageRoi2D = cageLimitROIList.get(index);
			cage.flyPositions = flyPositionsList.get(index);
			cages.cageList.add(cage);
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
			pos.xmlLoadXYTPositions(subnode);
			flyPositionsList.add(pos);
			ielement++;
		}
		return true;
	}

	private boolean csvLoad_CageBox(Cages cages, String directory) throws Exception {
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
					csvLoad_DESCRIPTION(cages, csvReader, sep);
					break;
				case "CAGE":
					csvLoad_CageBox(cages, csvReader, sep);
					break;
				case "POSITION":
					csvLoad_Measures(cages, csvReader, EnumCageMeasures.POSITION, sep);
					break;

				default:
					break;
				}
			}
		}
		csvReader.close();
		return true;
	}

	private String csvLoad_DESCRIPTION(Cages cages, BufferedReader csvReader, String sep) {
		String row;
		try {
			while ((row = csvReader.readLine()) != null) {
				String[] data = row.split(sep);
				if (data[0].equals("#"))
					return data[1];

				String test = data[0].substring(0, Math.min(data[0].length(), 7));
				if (test.equals("n cages") || test.equals("n cells")) {
					int ncages = Integer.valueOf(data[1]);
					if (ncages >= cages.cageList.size())
						cages.cageList.ensureCapacity(ncages);
					else
						cages.cageList.subList(ncages, cages.cageList.size()).clear();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private String csvLoad_CageBox(Cages cages, BufferedReader csvReader, String sep) {
		String row;
		try {
			row = csvReader.readLine();
			while ((row = csvReader.readLine()) != null) {
				String[] data = row.split(sep);
				if (data[0].equals("#"))
					return data[1];

				int cageID = 0;
				try {
					cageID = Integer.valueOf(data[0]);
				} catch (NumberFormatException e) {
					Logger.warn("CagesPersistence: Invalid integer input: " + data[0]);
				}
				Cage cage = cages.getCageFromID(cageID);
				if (cage == null) {
					cage = new Cage();
					cages.cageList.add(cage);
				}
				cage.csvImport_CAGE_Header(data);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private String csvLoad_Measures(Cages cages, BufferedReader csvReader, EnumCageMeasures measureType, String sep) {
		String row;
		try {
			row = csvReader.readLine();
			boolean complete = row.contains("w(i)");
			boolean v0 = row.contains("x(i)");
			while ((row = csvReader.readLine()) != null) {
				String[] data = row.split(sep);
				if (data[0].equals("#"))
					return data[1];

				int cageID = -1;
				try {
					cageID = Integer.valueOf(data[0]);
				} catch (NumberFormatException e) {
					Logger.warn("CagesPersistence: Invalid integer input: " + data[0]);
				}
				Cage cage = cages.getCageFromID(cageID);
				if (cage == null)
					cage = new Cage();
				if (v0)
					cage.csvImport_MEASURE_Data_v0(measureType, data, complete);
				else
					cage.csvImport_MEASURE_Data_Parameters(data);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private boolean csvSave_Cages(Cages cages, String directory) {
		Path path = Paths.get(directory);
		if (!Files.exists(path))
			return false;

		try {
			FileWriter csvWriter = new FileWriter(directory + File.separator + "CagesMeasures.csv");
			csvSave_Description(cages, csvWriter);
			csvSave_Measures(cages, csvWriter, EnumCageMeasures.POSITION);
			csvWriter.flush();
			csvWriter.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

		return true;
	}

	private boolean csvSave_Description(Cages cages, FileWriter csvWriter) {
		try {
			csvWriter.append("#" + csvSep + "DESCRIPTION\n");
			csvWriter.append("n cages=" + csvSep + Integer.toString(cages.cageList.size()) + "\n");

			csvWriter.append("#" + csvSep + "#\n");

			if (cages.cageList.size() > 0) {
				csvWriter.append(cages.cageList.get(0).csvExport_CAGE_Header(csvSep));
				for (Cage cage : cages.cageList)
					csvWriter.append(cage.csvExport_CAGE_Data(csvSep));
				csvWriter.append("#" + csvSep + "#\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return true;
	}

	private boolean csvSave_Measures(Cages cages, FileWriter csvWriter, EnumCageMeasures measureType) {
		try {
			if (cages.cageList.size() <= 1)
				return false;
			boolean complete = true;
			csvWriter.append(cages.cageList.get(0).csvExport_MEASURE_Header(measureType, csvSep, complete));
			for (Cage cage : cages.cageList)
				csvWriter.append(cage.csvExport_MEASURE_Data(measureType, csvSep, complete));

			csvWriter.append("#" + csvSep + "#\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}
}
