package plugins.fmp.multicafe.fmp_experiment.cages;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import icy.util.XMLUtil;
import plugins.fmp.multicafe.fmp_experiment.Experiment;
import plugins.fmp.multicafe.fmp_tools.JComponents.Dialog;
import plugins.fmp.multicafe.fmp_tools.JComponents.exceptions.FileDialogException;
import plugins.fmp.multicafe.fmp_tools.Logger;

public class CagesArrayPersistence {

	private final String ID_CAGES = "Cages";
	private final String ID_NCAGES = "n_cages";
	private final String ID_NCAGESALONGX = "N_cages_along_X";
	private final String ID_NCAGESALONGY = "N_cages_along_Y";
	private final String ID_NCOLUMNSPERCAGE = "N_columns_per_cage";
	private final String ID_NROWSPERCAGE = "N_rows_per_cage";

	private final String ID_MCDROSOTRACK_XML = "MCdrosotrack.xml";

	private final String csvSep = ";";

	public boolean load_Cages(CagesArray cages, String directory) {
		boolean flag = false;
		try {
			flag = csvLoadCagesMeasures(cages, directory);
		} catch (Exception e) {
			Logger.error("CagesArrayPersistence:load_Cages() Failed to load cages from CSV: " + directory, e);
		}

		if (!flag) {
			String tempName = directory + File.separator + ID_MCDROSOTRACK_XML;
			flag = xmlReadCagesFromFileNoQuestion(cages, tempName);
		}
		return flag;
	}

	public boolean save_Cages(CagesArray cages, String directory) {
		if (directory == null) {
			Logger.warn("CagesArrayPersistence:save_Cages() directory is null");
			return false;
		}

		Path path = Paths.get(directory);
		if (!Files.exists(path)) {
			Logger.warn("CagesArrayPersistence:save_Cages() directory does not exist: " + directory);
			return false;
		}

		csvSaveCagesMeasures(cages, directory);
		return true;
	}

	public boolean xmlReadCagesFromFile(CagesArray cages, Experiment exp) {
		String[] filedummy = null;
		String filename = exp.getResultsDirectory();
		File file = new File(filename);
		String directory = file.getParentFile().getAbsolutePath();
		try {
			filedummy = Dialog.selectFiles(directory, "xml");
		} catch (FileDialogException e) {
			e.printStackTrace();
		}
		boolean wasOk = false;
		if (filedummy != null) {
			for (int i = 0; i < filedummy.length; i++) {
				String csFile = filedummy[i];
				wasOk &= xmlReadCagesFromFileNoQuestion(cages, csFile);
			}
		}
		return wasOk;
	}

	public boolean xmlReadCagesFromFileNoQuestion(CagesArray cages, String tempname) {
		if (tempname == null) {
			return false;
		}

		File file = new File(tempname);
		if (!file.exists()) {
			return false;
		}

		try {
			final Document doc = XMLUtil.loadDocument(tempname);
			if (doc == null) {
				return false;
			}

			boolean success = xmlLoadCages(cages, XMLUtil.getRootElement(doc));

			return success;

		} catch (Exception e) {
			Logger.error("CagesArrayPersistence:xmlReadCagesFromFileNoQuestion() ERROR during cages XML loading: " + e.getMessage(), e);
			return false;
		}
	}

	private boolean xmlWriteCagesToFileNoQuestion(CagesArray cages, String tempname) {
		try {
			final Document doc = XMLUtil.createDocument(true);
			if (doc == null) {
				Logger.error("CagesArrayPersistence:xmlWriteCagesToFileNoQuestion() ERROR: Could not create XML document");
				return false;
			}

			Node node = XMLUtil.getRootElement(doc);
			boolean success = xmlSaveCages(cages, node);

			if (success) {
				success = XMLUtil.saveDocument(doc, tempname);
			}

			return success;

		} catch (Exception e) {
			Logger.error("CagesArrayPersistence:xmlWriteCagesToFileNoQuestion() ERROR during cages XML saving: " + e.getMessage(), e);
			return false;
		}
	}

	private boolean xmlSaveCages(CagesArray cages, Node node) {
		try {
			int index = 0;
			Element xmlVal = XMLUtil.addElement(node, ID_CAGES);
			int ncages = cages.cagesList.size();
			XMLUtil.setAttributeIntValue(xmlVal, ID_NCAGES, ncages);
			XMLUtil.setAttributeIntValue(xmlVal, ID_NCAGESALONGX, cages.nCagesAlongX);
			XMLUtil.setAttributeIntValue(xmlVal, ID_NCAGESALONGY, cages.nCagesAlongY);
			XMLUtil.setAttributeIntValue(xmlVal, ID_NCOLUMNSPERCAGE, cages.nColumnsPerCage);
			XMLUtil.setAttributeIntValue(xmlVal, ID_NROWSPERCAGE, cages.nRowsPerCage);

			for (Cage cage : cages.cagesList) {
				if (cage == null) {
					Logger.warn("CagesArrayPersistence:xmlSaveCages() WARNING: Null cage at index " + index);
					continue;
				}

				boolean cageSuccess = cage.xmlSaveCage(xmlVal, index);
				if (!cageSuccess) {
					Logger.warn("CagesArrayPersistence:xmlSaveCages() ERROR: Failed to save cage at index " + index);
				}
				index++;
			}

			return true;

		} catch (Exception e) {
			Logger.error("CagesArrayPersistence:xmlSaveCages() ERROR during xmlSaveCages: " + e.getMessage(), e);
			return false;
		}
	}

	private boolean xmlLoadCages(CagesArray cages, Node node) {
		try {
			cages.cagesList.clear();
			Element xmlVal = XMLUtil.getElement(node, ID_CAGES);
			if (xmlVal == null) {
				Logger.error("CagesArrayPersistence:xmlLoadCages() ERROR: Could not find Cages element in XML");
				return false;
			}

			int ncages = XMLUtil.getAttributeIntValue(xmlVal, ID_NCAGES, 0);
			if (ncages < 0) {
				Logger.error("CagesArrayPersistence:xmlLoadCages() ERROR: Invalid number of cages: " + ncages);
				return false;
			}

			cages.nCagesAlongX = XMLUtil.getAttributeIntValue(xmlVal, ID_NCAGESALONGX, cages.nCagesAlongX);
			cages.nCagesAlongY = XMLUtil.getAttributeIntValue(xmlVal, ID_NCAGESALONGY, cages.nCagesAlongY);
			cages.nColumnsPerCage = XMLUtil.getAttributeIntValue(xmlVal, ID_NCOLUMNSPERCAGE, cages.nColumnsPerCage);
			cages.nRowsPerCage = XMLUtil.getAttributeIntValue(xmlVal, ID_NROWSPERCAGE, cages.nRowsPerCage);

			int loadedCages = 0;
			for (int index = 0; index < ncages; index++) {
				try {
					Cage cage = new Cage();
					boolean cageSuccess = cage.xmlLoadCage(xmlVal, index);
					if (cageSuccess) {
						cages.cagesList.add(cage);
						loadedCages++;
					} else {
						Logger.warn("CagesArrayPersistence:xmlLoadCages() WARNING: Failed to load cage at index " + index);
					}
				} catch (Exception e) {
					Logger.error("CagesArrayPersistence:xmlLoadCages() ERROR loading cage at index " + index + ": " + e.getMessage(), e);
				}
			}

			return loadedCages > 0;

		} catch (Exception e) {
			Logger.error("CagesArrayPersistence:xmlLoadCages() ERROR during xmlLoadCages: " + e.getMessage(), e);
			return false;
		}
	}

	private boolean csvLoadCagesMeasures(CagesArray cages, String directory) throws Exception {
		String pathToCsv = directory + File.separator + "CagesMeasures.csv";
		File csvFile = new File(pathToCsv);
		if (!csvFile.isFile())
			return false;

		cages.cagesList.clear();

		BufferedReader csvReader = new BufferedReader(new FileReader(pathToCsv));
		String row;
		String sep = csvSep;
		while ((row = csvReader.readLine()) != null) {
			if (row.length() > 0 && row.charAt(0) == '#')
				sep = String.valueOf(row.charAt(1));

			String[] data = row.split(sep);
			if (data.length > 0 && data[0].equals("#")) {
				if (data.length > 1) {
					switch (data[1]) {
					case "DESCRIPTION":
						csvLoad_DESCRIPTION(cages, csvReader, sep);
						break;
					case "CAGE":
						csvLoad_CAGE(cages, csvReader, sep);
						break;
					case "POSITION":
						csvLoad_Measures(cages, csvReader, EnumCageMeasures.POSITION, sep);
						break;
					default:
						break;
					}
				}
			}
		}
		csvReader.close();
		return cages.cagesList.size() > 0;
	}

	private void csvLoad_DESCRIPTION(CagesArray cages, BufferedReader csvReader, String sep) {
		String row;
		try {
			while ((row = csvReader.readLine()) != null) {
				String[] data = row.split(sep);
				if (data.length > 0 && data[0].equals("#"))
					return;

				if (data.length > 0) {
					String test = data[0].substring(0, Math.min(data[0].length(), 7));
					if (test.equals("n cages") || test.equals("n cells")) {
						if (data.length > 1) {
							int ncages = Integer.valueOf(data[1]);
							if (ncages >= cages.cagesList.size()) {
								cages.cagesList.ensureCapacity(ncages);
							} else {
								cages.cagesList.subList(ncages, cages.cagesList.size()).clear();
							}
						}
					}
				}
			}
		} catch (IOException e) {
			Logger.error("CagesArrayPersistence:csvLoad_DESCRIPTION() Error: " + e.getMessage(), e);
		}
	}

	private void csvLoad_CAGE(CagesArray cages, BufferedReader csvReader, String sep) {
		String row;
		try {
			row = csvReader.readLine();
			while ((row = csvReader.readLine()) != null) {
				String[] data = row.split(sep);
				if (data.length > 0 && data[0].equals("#"))
					return;

				if (data.length > 0) {
					int cageID = 0;
					try {
						cageID = Integer.valueOf(data[0]);
					} catch (NumberFormatException e) {
						Logger.warn("CagesArrayPersistence:csvLoad_CAGE() Invalid integer input: " + data[0]);
						continue;
					}
					Cage cage = cages.getCageFromID(cageID);
					if (cage == null) {
						cage = new Cage();
						cages.cagesList.add(cage);
					}
					cage.csvImport_CAGE_Header(data);
				}
			}
		} catch (IOException e) {
			Logger.error("CagesArrayPersistence:csvLoad_CAGE() Error: " + e.getMessage(), e);
		}
	}

	private void csvLoad_Measures(CagesArray cages, BufferedReader csvReader, EnumCageMeasures measureType, String sep) {
		String row;
		try {
			row = csvReader.readLine();
			boolean complete = (row != null && row.contains("w(i)"));
			boolean v0 = (row != null && row.contains("x(i)"));

			while ((row = csvReader.readLine()) != null) {
				String[] data = row.split(sep);
				if (data.length > 0 && data[0].equals("#"))
					return;

				if (data.length > 0) {
					int cageID = -1;
					try {
						cageID = Integer.valueOf(data[0]);
					} catch (NumberFormatException e) {
						Logger.warn("CagesArrayPersistence:csvLoad_Measures() Invalid integer input: " + data[0]);
						continue;
					}
					Cage cage = cages.getCageFromID(cageID);
					if (cage == null) {
						cage = new Cage();
						cages.cagesList.add(cage);
						cage.prop.setCageID(cageID);
					}
					if (v0) {
						cage.csvImport_MEASURE_Data_v0(measureType, data, complete);
					} else {
						cage.csvImport_MEASURE_Data_Parameters(data);
					}
				}
			}
		} catch (IOException e) {
			Logger.error("CagesArrayPersistence:csvLoad_Measures() Error: " + e.getMessage(), e);
		}
	}

	private boolean csvSaveCagesMeasures(CagesArray cages, String directory) {
		try {
			FileWriter csvWriter = new FileWriter(directory + File.separator + "CagesMeasures.csv");
			csvSaveDescriptionSection(cages, csvWriter);
			csvSaveMeasuresSection(cages, csvWriter, EnumCageMeasures.POSITION);
			csvWriter.flush();
			csvWriter.close();

		} catch (IOException e) {
			Logger.error("CagesArrayPersistence:csvSaveCagesMeasures() Error: " + e.getMessage(), e);
		}
		return true;
	}

	private boolean csvSaveDescriptionSection(CagesArray cages, FileWriter csvWriter) {
		try {
			csvWriter.append("#" + csvSep + "DESCRIPTION" + csvSep + "Cages data\n");
			csvWriter.append("n cages=" + csvSep + Integer.toString(cages.cagesList.size()) + "\n");
			if (cages.cagesList.size() > 0)
				for (Cage cage : cages.cagesList)
					csvWriter.append(cage.csvExportCageDescription(csvSep));

			csvWriter.append("#" + csvSep + "#\n");
		} catch (IOException e) {
			Logger.error("CagesArrayPersistence:csvSaveDescriptionSection() Error: " + e.getMessage(), e);
		}
		return true;
	}

	private boolean csvSaveMeasuresSection(CagesArray cages, FileWriter csvWriter, EnumCageMeasures measuresType) {
		try {
			if (cages.cagesList.size() <= 0) {
				return false;
			}

			boolean complete = true;
			csvWriter.append(csvExport_MEASURE_Header(measuresType, csvSep, complete));
			
			for (Cage cage : cages.cagesList) {
				csvWriter.append(csvExport_MEASURE_Data(cage, measuresType, csvSep, complete));
			}

			csvWriter.append("#" + csvSep + "#\n");
		} catch (IOException e) {
			Logger.error("CagesArrayPersistence:csvSaveMeasuresSection() Error: " + e.getMessage(), e);
		}
		return true;
	}

	private String csvExport_MEASURE_Header(EnumCageMeasures measureType, String sep, boolean complete) {
		StringBuffer sbf = new StringBuffer();
		String explanation = "cageID" + sep + "parm" + sep + "npts";
		switch (measureType) {
		case POSITION:
			sbf.append("#" + sep + "POSITION\n" + explanation + "\n");
			break;
		default:
			sbf.append("#" + sep + "UNDEFINED------------\n");
			break;
		}
		return sbf.toString();
	}

	private String csvExport_MEASURE_Data(Cage cage, EnumCageMeasures measureType, String sep, boolean complete) {
		StringBuffer sbf = new StringBuffer();
		String cageID = Integer.toString(cage.getProperties().getCageID());
		
		switch (measureType) {
		case POSITION:
			if (cage.flyPositions != null) {
				cage.flyPositions.cvsExport_Parameter_ToRow(sbf, "t(i)", cageID, sep);
				cage.flyPositions.cvsExport_Parameter_ToRow(sbf, "x(i)", cageID, sep);
				cage.flyPositions.cvsExport_Parameter_ToRow(sbf, "y(i)", cageID, sep);
				cage.flyPositions.cvsExport_Parameter_ToRow(sbf, "w(i)", cageID, sep);
				cage.flyPositions.cvsExport_Parameter_ToRow(sbf, "h(i)", cageID, sep);
			}
			break;
		default:
			break;
		}
		return sbf.toString();
	}
}

