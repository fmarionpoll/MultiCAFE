package plugins.fmp.multicafe.fmp_experiment.capillaries;

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
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import icy.roi.ROI;
import icy.roi.ROI2D;
import icy.util.XMLUtil;
import plugins.fmp.multicafe.fmp_tools.Logger;
import plugins.kernel.roi.roi2d.ROI2DShape;

public class CapillariesPersistence {

	public final static String ID_CAPILLARYTRACK = "capillaryTrack";
	public final static String ID_NCAPILLARIES = "N_capillaries";
	public final static String ID_LISTOFCAPILLARIES = "List_of_capillaries";
	public final static String ID_CAPILLARY_ = "capillary_";
	public final static String ID_MCCAPILLARIES_XML = "MCcapillaries.xml";
	private final String csvSep = ";";

	public boolean load_Capillaries(Capillaries capillaries, String directory) {
		boolean flag = false;
		try {
			flag = csvLoad_Capillaries(capillaries, directory);
		} catch (Exception e) {
			Logger.error("CapillariesPersistence:load_Capillaries() Failed to load capillaries from CSV: " + directory,
					e, true);
		}

		if (!flag) {
			flag = xmlLoadCapillaries_Measures(capillaries, directory);
		}
		return flag;
	}

	public boolean save_Capillaries(Capillaries capillaries, String directory) {
		if (directory == null)
			return false;

		csvSave_Capillaries(capillaries, directory);
		return true;
	}

	public String getXMLNameToAppend() {
		return ID_MCCAPILLARIES_XML;
	}

	public boolean xmlSaveCapillaries_Descriptors(Capillaries capillaries, String csFileName) {
		if (csFileName != null) {
			final Document doc = XMLUtil.createDocument(true);
			if (doc != null) {
				capillaries.getCapillariesDescription().xmlSaveCapillaryDescription(doc);
				xmlSaveListOfCapillaries(capillaries, doc);
				return XMLUtil.saveDocument(doc, csFileName);
			}
		}
		return false;
	}

	private boolean xmlSaveListOfCapillaries(Capillaries capillaries, Document doc) {
		Node node = XMLUtil.getElement(XMLUtil.getRootElement(doc), ID_CAPILLARYTRACK);
		if (node == null)
			return false;
		XMLUtil.setElementIntValue(node, "version", 2);
		Node nodecaps = XMLUtil.setElement(node, ID_LISTOFCAPILLARIES);
		XMLUtil.setElementIntValue(nodecaps, ID_NCAPILLARIES, capillaries.getList().size());
		int i = 0;
		Collections.sort(capillaries.getList());
		for (Capillary cap : capillaries.getList()) {
			Node nodecapillary = XMLUtil.setElement(node, ID_CAPILLARY_ + i);
			cap.xmlSave_CapillaryOnly(nodecapillary);
			i++;
		}
		return true;
	}

	public boolean loadMCCapillaries_Descriptors(Capillaries capillaries, String csFileName) {
		boolean flag = false;
		if (csFileName == null)
			return flag;

		final Document doc = XMLUtil.loadDocument(csFileName);
		if (doc != null) {
			capillaries.getCapillariesDescription().xmlLoadCapillaryDescription(doc);
			flag = xmlLoadCapillaries_Only_v1(capillaries, doc);
		}
		return flag;
	}

	public boolean xmlLoadOldCapillaries_Only(Capillaries capillaries, String csFileName) {
		if (csFileName == null)
			return false;
		final Document doc = XMLUtil.loadDocument(csFileName);
		if (doc != null) {
			capillaries.getCapillariesDescription().xmlLoadCapillaryDescription(doc);
			switch (capillaries.getCapillariesDescription().getVersion()) {
			case 1: // old xml storage structure
				xmlLoadCapillaries_Only_v1(capillaries, doc);
				break;
			case 0: // old-old xml storage structure
				xmlLoadCapillaries_v0(capillaries, doc, csFileName);
				break;
			default:
				xmlLoadCapillaries_Only_v2(capillaries, doc, csFileName);
				return false;
			}
			return true;
		}
		return false;
	}

	private boolean xmlLoadCapillaries_Measures(Capillaries capillaries, String directory) {
		boolean flag = false;
		int ncapillaries = capillaries.getList().size();
		for (int i = 0; i < ncapillaries; i++) {
			String csFile = directory + File.separator + capillaries.getList().get(i).getKymographName()
					+ ".xml";
			final Document capdoc = XMLUtil.loadDocument(csFile);
			if (capdoc != null) {
				Node node = XMLUtil.getRootElement(capdoc, true);
				Capillary cap = capillaries.getList().get(i);
				cap.kymographIndex = i;
				flag |= cap.xmlLoad_MeasuresOnly(node);
			}
		}
		return flag;
	}

	private void xmlLoadCapillaries_v0(Capillaries capillaries, Document doc, String csFileName) {
		List<ROI> listOfCapillaryROIs = ROI.loadROIsFromXML(XMLUtil.getRootElement(doc));
		capillaries.getList().clear();
		Path directorypath = Paths.get(csFileName).getParent();
		String directory = directorypath + File.separator;
		int t = 0;
		for (ROI roiCapillary : listOfCapillaryROIs) {
			xmlLoadIndividualCapillary_v0(capillaries, (ROI2DShape) roiCapillary, directory, t);
			t++;
		}
	}

	private void xmlLoadIndividualCapillary_v0(Capillaries capillaries, ROI2D roiCapillary, String directory, int t) {
		Capillary cap = new Capillary(roiCapillary);
		if (!capillaries.isPresent(cap))
			capillaries.getList().add(cap);
		String csFile = directory + roiCapillary.getName() + ".xml";
		cap.kymographIndex = t;
		final Document dockymo = XMLUtil.loadDocument(csFile);
		if (dockymo != null) {
			NodeList nodeROISingle = dockymo.getElementsByTagName("roi");
			if (nodeROISingle.getLength() > 0) {
				List<ROI> rois = new ArrayList<ROI>();
				for (int i = 0; i < nodeROISingle.getLength(); i++) {
					Node element = nodeROISingle.item(i);
					ROI roi_i = ROI.createFromXML(element);
					if (roi_i != null)
						rois.add(roi_i);
				}
				cap.transferROIsToMeasures(rois);
			}
		}
	}

	private boolean xmlLoadCapillaries_Only_v1(Capillaries capillaries, Document doc) {
		Node node = XMLUtil.getElement(XMLUtil.getRootElement(doc), ID_CAPILLARYTRACK);
		if (node == null)
			return false;
		Node nodecaps = XMLUtil.getElement(node, ID_LISTOFCAPILLARIES);
		int nitems = XMLUtil.getElementIntValue(nodecaps, ID_NCAPILLARIES, 0);
		capillaries.setCapillariesList(new ArrayList<Capillary>(nitems));
		for (int i = 0; i < nitems; i++) {
			Node nodecapillary = XMLUtil.getElement(node, ID_CAPILLARY_ + i);
			Capillary cap = new Capillary();
			cap.xmlLoad_CapillaryOnly(nodecapillary);

			if (!capillaries.isPresent(cap))
				capillaries.getList().add(cap);
		}
		return true;
	}

	private void xmlLoadCapillaries_Only_v2(Capillaries capillaries, Document doc, String csFileName) {
		xmlLoadCapillaries_Only_v1(capillaries, doc);
		Path directorypath = Paths.get(csFileName).getParent();
		String directory = directorypath + File.separator;
		for (Capillary cap : capillaries.getList()) {
			String csFile = directory + cap.getKymographName() + ".xml";
			final Document capdoc = XMLUtil.loadDocument(csFile);
			if (capdoc != null) {
				Node node = XMLUtil.getRootElement(capdoc, true);
				cap.xmlLoad_CapillaryOnly(node);
			}
		}
	}

	private boolean csvLoad_Capillaries(Capillaries capillaries, String directory) throws Exception {
		String pathToCsv = directory + File.separator + "CapillariesMeasures.csv";
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
					csvLoad_Description(capillaries, csvReader, sep);
					break;
				case "CAPILLARIES":
					csvLoad_Capillaries_Description(capillaries, csvReader, sep);
					break;
				case "TOPLEVEL":
					csvLoad_Capillaries_Measures(capillaries, csvReader, EnumCapillaryMeasures.TOPLEVEL, sep,
							row.contains("xi"));
					break;
				case "BOTTOMLEVEL":
					csvLoad_Capillaries_Measures(capillaries, csvReader, EnumCapillaryMeasures.BOTTOMLEVEL, sep,
							row.contains("xi"));
					break;
				case "TOPDERIVATIVE":
					csvLoad_Capillaries_Measures(capillaries, csvReader, EnumCapillaryMeasures.TOPDERIVATIVE, sep,
							row.contains("xi"));
					break;
				case "GULPS":
					csvLoad_Capillaries_Measures(capillaries, csvReader, EnumCapillaryMeasures.GULPS, sep, true);
					break;
				default:
					break;
				}
			}
		}
		csvReader.close();
		return true;
	}

	private String csvLoad_Capillaries_Description(Capillaries capillaries, BufferedReader csvReader, String sep) {
		String row;
		try {
			row = csvReader.readLine();
			while ((row = csvReader.readLine()) != null) {
				String[] data = row.split(sep);
				if (data[0].equals("#"))
					return data[1];
				Capillary cap = capillaries.getCapillaryFromKymographName(data[2]);
				if (cap == null) {
					cap = new Capillary();
					capillaries.getList().add(cap);
				}
				cap.csvImport_CapillaryDescription(data);
			}
		} catch (IOException e) {
			Logger.error("CapillariesPersistence:csvLoad_Capillaries() Failed to read CSV file", e);
		}
		return null;
	}

	private String csvLoad_Description(Capillaries capillaries, BufferedReader csvReader, String sep) {
		String row;
		try {
			row = csvReader.readLine();
			row = csvReader.readLine();
			String[] data = row.split(sep);
			capillaries.getCapillariesDescription().csvImportCapillariesDescriptionData(data);

			row = csvReader.readLine();
			data = row.split(sep);
			if (data[0].substring(0, Math.min(data[0].length(), 5)).equals("n cap")) {
				int ncapillaries = Integer.valueOf(data[1]);
				if (ncapillaries >= capillaries.getList().size())
					((ArrayList<Capillary>) capillaries.getList()).ensureCapacity(ncapillaries);
				else
					capillaries.getList().subList(ncapillaries, capillaries.getList().size())
							.clear();

				row = csvReader.readLine();
				data = row.split(sep);
			}
			if (data[0].equals("#")) {
				return data[1];
			}
		} catch (IOException e) {
			Logger.error("CapillariesPersistence:csvLoad_Description()", e);
		}
		return null;
	}

	private String csvLoad_Capillaries_Measures(Capillaries capillaries, BufferedReader csvReader,
			EnumCapillaryMeasures measureType, String sep, boolean x) {
		String row;
		final boolean y = true;
		try {
			while ((row = csvReader.readLine()) != null) {
				String[] data = row.split(sep);
				if (data[0].equals("#"))
					return data[1];

				Capillary cap = capillaries.getCapillaryFromRoiNamePrefix(data[0]);
				if (cap == null)
					cap = new Capillary();
				cap.csvImport_CapillaryData(measureType, data, x, y);
			}
		} catch (IOException e) {
			Logger.error("CapillariesPersistence:csvLoad_Capillaries() Failed to read CSV file", e);
		}
		return null;
	}

	private boolean csvSave_Capillaries(Capillaries capillaries, String directory) {
		Path path = Paths.get(directory);
		if (!Files.exists(path))
			return false;

		try {
			FileWriter csvWriter = new FileWriter(directory + File.separator + "CapillariesMeasures.csv");

			csvSave_DescriptionSection(capillaries, csvWriter);

			csvSave_MeasuresSection(capillaries, csvWriter, EnumCapillaryMeasures.TOPLEVEL);
			csvSave_MeasuresSection(capillaries, csvWriter, EnumCapillaryMeasures.BOTTOMLEVEL);
			csvSave_MeasuresSection(capillaries, csvWriter, EnumCapillaryMeasures.TOPDERIVATIVE);
			csvSave_MeasuresSection(capillaries, csvWriter, EnumCapillaryMeasures.GULPS);
			csvWriter.flush();
			csvWriter.close();

		} catch (IOException e) {
			Logger.error("CapillariesPersistence:csvSave_Capillaries()", e);
		}

		return true;
	}

	private boolean csvSave_DescriptionSection(Capillaries capillaries, FileWriter csvWriter) {
		try {
			csvWriter.append(capillaries.getCapillariesDescription().csvExportSectionHeader(csvSep));
			csvWriter.append(capillaries.getCapillariesDescription().csvExportExperimentDescriptors(csvSep));
			csvWriter.append("n caps=" + csvSep + Integer.toString(capillaries.getList().size()) + "\n");
			csvWriter.append("#" + csvSep + "#\n");

			if (capillaries.getList().size() > 0) {
				csvWriter.append(capillaries.getList().get(0).csvExport_CapillarySubSectionHeader(csvSep));
				for (Capillary cap : capillaries.getList())
					csvWriter.append(cap.csvExport_CapillaryDescription(csvSep));
				csvWriter.append("#" + csvSep + "#\n");
			}
		} catch (IOException e) {
			Logger.error("CapillariesPersistence:csvSave_DescriptionSection()", e);
		}

		return true;
	}

	private boolean csvSave_MeasuresSection(Capillaries capillaries, FileWriter csvWriter,
			EnumCapillaryMeasures measureType) {
		try {
			if (capillaries.getList().size() <= 1)
				return false;

			csvWriter.append(
					capillaries.getList().get(0).csvExport_MeasureSectionHeader(measureType, csvSep));
			for (Capillary cap : capillaries.getList())
				csvWriter.append(cap.csvExport_MeasuresOneType(measureType, csvSep));

			csvWriter.append("#" + csvSep + "#\n");
		} catch (IOException e) {
			Logger.error("CapillariesPersistence:csvSave_MeasuresSection()", e);
		}
		return true;
	}
}
