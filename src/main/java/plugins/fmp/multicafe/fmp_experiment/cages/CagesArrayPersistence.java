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
import plugins.fmp.multicafe.fmp_tools.Logger;
import plugins.fmp.multicafe.fmp_tools.JComponents.Dialog;
import plugins.fmp.multicafe.fmp_tools.JComponents.exceptions.FileDialogException;

public class CagesArrayPersistence {

	private final String ID_CAGES = "Cages";
	private final String ID_NCAGES = "n_cages";
	private final String ID_NCAGESALONGX = "N_cages_along_X";
	private final String ID_NCAGESALONGY = "N_cages_along_Y";
	private final String ID_NCOLUMNSPERCAGE = "N_columns_per_cage";
	private final String ID_NROWSPERCAGE = "N_rows_per_cage";

	// New format filenames
	private final String ID_CAGESARRAY_CSV = "CagesArray.csv";
	private final String ID_CAGESARRAYMEASURES_CSV = "CagesArrayMeasures.csv";

	// Legacy filenames (for fallback)
	private final String ID_MCDROSOTRACK_XML = "MCdrosotrack.xml";
	private final String ID_CAGESMEASURES_CSV = "CagesMeasures.csv";

	private final String csvSep = ";";

	public boolean load_Cages(CagesArray cages, String directory) {
		int cagesBefore = cages.cagesList.size();

		// Priority 1: Try new format (descriptions + measures separate)
		boolean descriptionsLoaded = loadCagesArrayDescription(cages, directory);

		// Try to load measures from bin directory (if provided, will be loaded
		// separately by Experiment)
		// For now, we just load descriptions from results directory

		if (!descriptionsLoaded) {
			// Optionally load ROIs from XML if CSV doesn't have them
			String tempName = directory + File.separator + ID_MCDROSOTRACK_XML;
			int cagesWithROIsBefore = 0;
			for (Cage cage : cages.cagesList) {
				if (cage.getRoi() != null) {
					cagesWithROIsBefore++;
				}
			}

			if (cagesWithROIsBefore < cages.cagesList.size()) {
				xmlLoadCagesROIsOnly(cages, tempName);
			}

			return true;
		}

		// Priority 2: Fall back to legacy combined CSV format
		boolean csvLoadSuccess = false;
		try {
			csvLoadSuccess = csvLoadCagesMeasures(cages, directory);
			if (!csvLoadSuccess) {
				int cagesAfterCSV = cages.cagesList.size();
				Logger.info(String.format("CagesArrayPersistence:load_Cages() Legacy CSV loaded: %d cages (was %d)",
						cagesAfterCSV, cagesBefore));

				// Optionally load ROIs from XML
				String tempName = directory + File.separator + ID_MCDROSOTRACK_XML;
				xmlLoadCagesROIsOnly(cages, tempName);

				return true;
			}
		} catch (Exception e) {
			Logger.error("CagesArrayPersistence:load_Cages() Failed to load from legacy CSV: " + directory, e);
		}

		// Priority 3: Fall back to XML (legacy format)
		Logger.warn("CagesArrayPersistence:load_Cages() CSV load failed, falling back to XML");
		String tempName = directory + File.separator + ID_MCDROSOTRACK_XML;
		boolean xmlLoadSuccess = xmlReadCagesFromFileNoQuestion(cages, tempName);
		int cagesAfterXML = cages.cagesList.size();
		Logger.info(String.format("CagesArrayPersistence:load_Cages() After XML load: %d cages", cagesAfterXML));

		return xmlLoadSuccess;
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

		// Save descriptions to new format file (includes ROI coordinates in CSV)
		saveCagesArrayDescription(cages, directory);

		// XML save disabled - ROI coordinates are now stored in CSV
		// String tempName = directory + File.separator + ID_MCDROSOTRACK_XML;
		// xmlSaveCagesROIsOnly(cages, tempName);

		return true;
	}

	/**
	 * Saves cage descriptions (DESCRIPTION and CAGE sections) to CagesArray.csv in
	 * results directory.
	 * 
	 * @param cages            the CagesArray to save
	 * @param resultsDirectory the results directory
	 * @return true if successful
	 */
	public boolean saveCagesArrayDescription(CagesArray cages, String resultsDirectory) {
		if (resultsDirectory == null) {
			Logger.warn("CagesArrayPersistence:saveCagesArray() directory is null");
			return false;
		}

		Path path = Paths.get(resultsDirectory);
		if (!Files.exists(path)) {
			Logger.warn("CagesArrayPersistence:saveCagesArray() directory does not exist: " + resultsDirectory);
			return false;
		}

		try {
			FileWriter csvWriter = new FileWriter(resultsDirectory + File.separator + ID_CAGESARRAY_CSV);
			csvSaveDESCRIPTIONSection(cages, csvWriter);
			csvSaveCAGESection(cages, csvWriter);
			csvWriter.flush();
			csvWriter.close();
			Logger.info("CagesArrayPersistence:saveCagesArray() Saved descriptions to " + ID_CAGESARRAY_CSV);
			return true;
		} catch (IOException e) {
			Logger.error("CagesArrayPersistence:saveCagesArray() Error: " + e.getMessage(), e);
			return false;
		}
	}

	/**
	 * Saves cage measures (POSITION section) to CagesArrayMeasures.csv in bin
	 * directory.
	 * 
	 * @param cages        the CagesArray to save
	 * @param binDirectory the bin directory (e.g., results/bin60)
	 * @return true if successful
	 */
	public boolean saveCagesArrayMeasures(CagesArray cages, String binDirectory) {
		if (binDirectory == null) {
			Logger.warn("CagesArrayPersistence:saveCagesArrayMeasures() directory is null");
			return false;
		}

		Path path = Paths.get(binDirectory);
		if (!Files.exists(path)) {
			Logger.warn("CagesArrayPersistence:saveCagesArrayMeasures() directory does not exist: " + binDirectory);
			return false;
		}

		try {
			FileWriter csvWriter = new FileWriter(binDirectory + File.separator + ID_CAGESARRAYMEASURES_CSV);
			csvSaveMeasuresSection(cages, csvWriter, EnumCageMeasures.POSITION);
			csvWriter.flush();
			csvWriter.close();
			Logger.info(
					"CagesArrayPersistence:saveCagesArrayMeasures() Saved measures to " + ID_CAGESARRAYMEASURES_CSV);
			return true;
		} catch (IOException e) {
			Logger.error("CagesArrayPersistence:saveCagesArrayMeasures() Error: " + e.getMessage(), e);
			return false;
		}
	}

	/**
	 * Loads cage descriptions (DESCRIPTION and CAGE sections) from CagesArray.csv.
	 * Stops reading when it encounters a POSITION section.
	 * 
	 * @param cages            the CagesArray to populate
	 * @param resultsDirectory the results directory
	 * @return true if successful
	 */
	public boolean loadCagesArrayDescription(CagesArray cages, String resultsDirectory) {
		if (resultsDirectory == null) {
			return false;
		}

		String pathToCsv = resultsDirectory + File.separator + ID_CAGESARRAY_CSV;
		File csvFile = new File(pathToCsv);
		if (!csvFile.isFile()) {
			return false;
		}

		try {
			BufferedReader csvReader = new BufferedReader(new FileReader(pathToCsv));
			String row;
			String sep = csvSep;
			boolean descriptionLoaded = false;
			boolean cageLoaded = false;

			while ((row = csvReader.readLine()) != null) {
				if (row.length() > 0 && row.charAt(0) == '#')
					sep = String.valueOf(row.charAt(1));

				String[] data = row.split(sep);
				if (data.length > 0 && data[0].equals("#")) {
					if (data.length > 1) {
						switch (data[1]) {
						case "DESCRIPTION":
							descriptionLoaded = true;
							csvLoad_DESCRIPTION(cages, csvReader, sep);
							break;
						case "CAGE":
						case "CAGES":
							cageLoaded = true;
							csvLoad_CAGE(cages, csvReader, sep);
							break;
						case "POSITION":
							// Stop reading when we hit measures section
							csvReader.close();
							return descriptionLoaded || cageLoaded;
						default:
							break;
						}
					}
				}
			}
			csvReader.close();
			return descriptionLoaded || cageLoaded;
		} catch (Exception e) {
			Logger.error("CagesArrayPersistence:loadCagesArray() Error: " + e.getMessage(), e);
			return false;
		}
	}

	/**
	 * Loads cage measures (POSITION section) from CagesArrayMeasures.csv in bin
	 * directory.
	 * 
	 * @param cages        the CagesArray to populate
	 * @param binDirectory the bin directory (e.g., results/bin60)
	 * @return true if successful
	 */
	public boolean loadCagesArrayMeasures(CagesArray cages, String binDirectory) {
		if (binDirectory == null) {
			return false;
		}

		String pathToCsv = binDirectory + File.separator + ID_CAGESARRAYMEASURES_CSV;
		File csvFile = new File(pathToCsv);
		if (!csvFile.isFile()) {
			return false;
		}

		try {
			BufferedReader csvReader = new BufferedReader(new FileReader(pathToCsv));
			String row;
			String sep = csvSep;

			while ((row = csvReader.readLine()) != null) {
				if (row.length() > 0 && row.charAt(0) == '#') {
					sep = String.valueOf(row.charAt(1));
				}

				String[] data = row.split(sep);
				if (data.length > 0 && data[0].equals("#")) {
					if (data.length > 1) {
						if (data[1].equals("POSITION")) {
							csvLoad_Measures(cages, csvReader, EnumCageMeasures.POSITION, sep);
							csvReader.close();
							return true;
						}
					}
				}
			}
			csvReader.close();
			return false;
		} catch (Exception e) {
			Logger.error("CagesArrayPersistence:loadCagesArrayMeasures() Error: " + e.getMessage(), e);
			e.printStackTrace();
			return false;
		}
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
			Logger.error("CagesArrayPersistence:xmlReadCagesFromFileNoQuestion() ERROR during cages XML loading: "
					+ e.getMessage(), e);
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
						Logger.warn(
								"CagesArrayPersistence:xmlLoadCages() WARNING: Failed to load cage at index " + index);
					}
				} catch (Exception e) {
					Logger.error("CagesArrayPersistence:xmlLoadCages() ERROR loading cage at index " + index + ": "
							+ e.getMessage(), e);
				}
			}

			return loadedCages > 0;

		} catch (Exception e) {
			Logger.error("CagesArrayPersistence:xmlLoadCages() ERROR during xmlLoadCages: " + e.getMessage(), e);
			return false;
		}
	}

	private boolean xmlSaveCagesROIsOnly(CagesArray cages, String tempname) {
		try {
			final Document doc = XMLUtil.createDocument(true);
			if (doc == null) {
				Logger.error("CagesArrayPersistence:xmlSaveCagesROIsOnly() ERROR: Could not create XML document");
				return false;
			}

			Node node = XMLUtil.getRootElement(doc);
			Element xmlVal = XMLUtil.addElement(node, ID_CAGES);
			int ncages = cages.cagesList.size();
			XMLUtil.setAttributeIntValue(xmlVal, ID_NCAGES, ncages);
			XMLUtil.setAttributeIntValue(xmlVal, ID_NCAGESALONGX, cages.nCagesAlongX);
			XMLUtil.setAttributeIntValue(xmlVal, ID_NCAGESALONGY, cages.nCagesAlongY);
			XMLUtil.setAttributeIntValue(xmlVal, ID_NCOLUMNSPERCAGE, cages.nColumnsPerCage);
			XMLUtil.setAttributeIntValue(xmlVal, ID_NROWSPERCAGE, cages.nRowsPerCage);

			int index = 0;
			for (Cage cage : cages.cagesList) {
				if (cage == null) {
					Logger.warn("CagesArrayPersistence:xmlSaveCagesROIsOnly() WARNING: Null cage at index " + index);
					index++;
					continue;
				}

				Element cageElement = XMLUtil.addElement(xmlVal, "Cage" + index);
				if (cageElement != null) {
					// Save only ROI (cage limits)
					cage.xmlSaveCageLimits(cageElement);
				}
				index++;
			}

			boolean success = XMLUtil.saveDocument(doc, tempname);
			return success;

		} catch (Exception e) {
			Logger.error("CagesArrayPersistence:xmlSaveCagesROIsOnly() ERROR during XML saving: " + e.getMessage(), e);
			return false;
		}
	}

	private boolean xmlLoadCagesROIsOnly(CagesArray cages, String tempname) {
		if (tempname == null) {
			return false;
		}

		File file = new File(tempname);
		if (!file.exists()) {
			// XML file doesn't exist yet (first save), that's OK
			return true;
		}

		try {
			final Document doc = XMLUtil.loadDocument(tempname);
			if (doc == null) {
				Logger.warn("CagesArrayPersistence:xmlLoadCagesROIsOnly() Could not load XML document: " + tempname);
				return false;
			}

			Node rootNode = XMLUtil.getRootElement(doc);
			Element xmlVal = XMLUtil.getElement(rootNode, ID_CAGES);
			if (xmlVal == null) {
				Logger.warn("CagesArrayPersistence:xmlLoadCagesROIsOnly() Could not find Cages element in XML");
				return false;
			}

			// Load layout information
			cages.nCagesAlongX = XMLUtil.getAttributeIntValue(xmlVal, ID_NCAGESALONGX, cages.nCagesAlongX);
			cages.nCagesAlongY = XMLUtil.getAttributeIntValue(xmlVal, ID_NCAGESALONGY, cages.nCagesAlongY);
			cages.nColumnsPerCage = XMLUtil.getAttributeIntValue(xmlVal, ID_NCOLUMNSPERCAGE, cages.nColumnsPerCage);
			cages.nRowsPerCage = XMLUtil.getAttributeIntValue(xmlVal, ID_NROWSPERCAGE, cages.nRowsPerCage);

			// Load ROIs and match them to existing cages by index
			int ncages = XMLUtil.getAttributeIntValue(xmlVal, ID_NCAGES, 0);
			int loadedROIs = 0;
			for (int index = 0; index < ncages && index < cages.cagesList.size(); index++) {
				try {
					Cage cage = cages.cagesList.get(index);
					if (cage == null) {
						continue;
					}

					Element cageElement = XMLUtil.getElement(xmlVal, "Cage" + index);
					if (cageElement != null) {
						// Load only ROI (cage limits)
						boolean roiLoaded = cage.xmlLoadCageLimits(cageElement);
						if (roiLoaded) {
							loadedROIs++;
						}
					}
				} catch (Exception e) {
					Logger.warn(
							"CagesArrayPersistence:xmlLoadCagesROIsOnly() WARNING: Failed to load ROI for cage at index "
									+ index);
				}
			}

			return loadedROIs > 0 || ncages == 0; // Return true if ROIs loaded or file was empty

		} catch (Exception e) {
			Logger.error("CagesArrayPersistence:xmlLoadCagesROIsOnly() ERROR during XML loading: " + e.getMessage(), e);
			return false;
		}
	}

	private boolean csvLoadCagesMeasures(CagesArray cages, String directory) throws Exception {
		String pathToCsv = directory + File.separator + ID_CAGESMEASURES_CSV;
		File csvFile = new File(pathToCsv);
//		System.out.println("CagesArrayPersistence:csvLoadCagesMeasures() Checking CSV file: " + pathToCsv + " exists: " + csvFile.exists() + " isFile: " + csvFile.isFile());
		if (!csvFile.isFile()) {
//			System.out.println("CagesArrayPersistence:csvLoadCagesMeasures() CSV file not found or not a file, returning false");
			return false;
		}

		// Use existing cages array to preserve ROIs and other data, just update
		// properties from CSV
		// Don't create a new tempCages array - work directly with existing cages
		System.out.println("CagesArrayPersistence:csvLoadCagesMeasures() Starting with " + cages.cagesList.size()
				+ " existing cages");

		BufferedReader csvReader = new BufferedReader(new FileReader(pathToCsv));
		String row;
		String sep = csvSep;
		int descriptionCount = 0;
		int cageCount = 0;
		int positionCount = 0;
		try {
			while ((row = csvReader.readLine()) != null) {
				if (row.length() > 0 && row.charAt(0) == '#')
					sep = String.valueOf(row.charAt(1));

				String[] data = row.split(sep);
				if (data.length > 0 && data[0].equals("#")) {
					if (data.length > 1) {
//						System.out.println("CagesArrayPersistence:csvLoadCagesMeasures() Found section: " + data[1]);
						switch (data[1]) {
						case "DIMENSION":
							// Legacy format: DIMENSION section with n_cages parameter
							csvLoad_DIMENSION(cages, csvReader, sep);
							break;
						case "DESCRIPTION":
							descriptionCount++;
							csvLoad_DESCRIPTION(cages, csvReader, sep);
							break;
						case "CAGE":
						case "CAGES":
							cageCount++;
							csvLoad_CAGE(cages, csvReader, sep);
							break;
						case "POSITION":
							positionCount++;
							csvLoad_Measures(cages, csvReader, EnumCageMeasures.POSITION, sep);
							break;
						default:
//							System.out.println("CagesArrayPersistence:csvLoadCagesMeasures() Unknown section: " + data[1]);
							break;
						}
					}
				}
			}
//			System.out.println(String.format("CagesArrayPersistence:csvLoadCagesMeasures() Processed sections - DESCRIPTION: %d, CAGE: %d, POSITION: %d", 
//				descriptionCount, cageCount, positionCount));
		} finally {
			csvReader.close();
		}

//		// Count cages with fly positions after loading
//		int cagesWithFlyPositions = 0;
//		int totalFlyPositions = 0;
//		for (Cage cage : cages.cagesList) {
//			if (cage.flyPositions != null && cage.flyPositions.flyPositionList != null
//					&& !cage.flyPositions.flyPositionList.isEmpty()) {
//				cagesWithFlyPositions++;
//				totalFlyPositions += cage.flyPositions.flyPositionList.size();
//			}
//		}
//
//		 //Log cage and fly position counts
//		System.out.println(String.format("CagesArrayPersistence:csvLoadCagesMeasures() Final: %d cages, %d with fly positions, %d total fly positions", 
//				cages.cagesList.size(), cagesWithFlyPositions, totalFlyPositions));
//		Logger.info(String.format("CagesArrayPersistence:csvLoadCagesMeasures() Loaded %d cages, %d with fly positions, %d total fly positions", 
//				cages.cagesList.size(), cagesWithFlyPositions, totalFlyPositions));

		return descriptionCount > 0 || cageCount > 0 || positionCount > 0;
	}

	private void csvLoad_DIMENSION(CagesArray cages, BufferedReader csvReader, String sep) {
		String row;
		try {
			while ((row = csvReader.readLine()) != null) {
				String[] data = row.split(sep);
				if (data.length > 0 && data[0].equals("#"))
					break;

				if (data.length > 0) {
					String test = data[0].substring(0, Math.min(data[0].length(), 7));
					if (test.equals("n cages") || test.equals("n cells")) {
						if (data.length > 1) {
							try {
								int ncages = Integer.valueOf(data[1]);
								if (ncages >= cages.cagesList.size()) {
									cages.cagesList.ensureCapacity(ncages);
								} else {
									cages.cagesList.subList(ncages, cages.cagesList.size()).clear();
								}
							} catch (NumberFormatException e) {
								Logger.warn(
										"CagesArrayPersistence:csvLoad_DIMENSION() Invalid n_cages value: " + data[1]);
							}
						}
						break; // Found n_cages parameter, done with DIMENSION section
					}
				}
			}
		} catch (IOException e) {
			Logger.error("CagesArrayPersistence:csvLoad_DIMENSION() Error: " + e.getMessage(), e);
		}
	}

	private void csvLoad_DESCRIPTION(CagesArray cages, BufferedReader csvReader, String sep) {
		String row;
//		int cagesLoaded = 0;
//		int cagesWithFlies = 0;
		try {
			while ((row = csvReader.readLine()) != null) {
				String[] data = row.split(sep);
				if (data.length > 0 && data[0].equals("#"))
					break;

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
					} else {
						System.out.println("skipped row with unknown format for DIMENSION section");
					}
				}
			}
//			System.out.println(String.format("CagesArrayPersistence:csvLoad_DESCRIPTION() Loaded %d cages from DESCRIPTION section, %d with nFlies > 0", 
//				cagesLoaded, cagesWithFlies));
		} catch (IOException e) {
//			System.out.println("CagesArrayPersistence:csvLoad_DESCRIPTION() Error: " + e.getMessage());
			Logger.error("CagesArrayPersistence:csvLoad_DESCRIPTION() Error: " + e.getMessage(), e);
		}
	}

	private void csvLoad_CAGE(CagesArray cages, BufferedReader csvReader, String sep) {
		String row;
//		int cagesLoaded = 0;
//		int cagesWithFlies = 0;
		try {
			// Skip header line (cageID, nFlies, age, Comment, strain, sect, ROIname,
			// npoints, x1, y1, ...)
			row = csvReader.readLine();
			if (row == null) {
				return;
			}

			// Read cage data rows
			while ((row = csvReader.readLine()) != null) {
				String[] data = row.split(sep);
				if (data.length > 0 && data[0].equals("#"))
					break;

				if (data.length > 0) {
					Cage cage = getCagefromID(cages, data[0]);
					cage.csvImport_CAGE_Header(data);
//					cagesLoaded++;
//					cagesWithFlies += cage.getProperties().getCageNFlies();
				}
			}
		} catch (IOException e) {
			Logger.error("CagesArrayPersistence:csvLoad_CAGE() Error: " + e.getMessage(), e);
		}
	}

	private Cage getCagefromID(CagesArray cages, String data) {
		int cageID = 0;
		try {
			cageID = Integer.valueOf(data);
		} catch (NumberFormatException e) {
			Logger.warn("CagesArrayPersistence:csvLoad_CAGE() Invalid integer input: " + data);
			cageID = -1;
		}
		Cage cage = cages.getCageFromID(cageID);
		if (cage == null) {
			cage = new Cage();
			cages.cagesList.add(cage);
		}
		return cage;
	}

	private void csvLoad_Measures(CagesArray cages, BufferedReader csvReader, EnumCageMeasures measureType,
			String sep) {
		String row;
		try {
			row = csvReader.readLine();
			boolean complete = (row != null && row.contains("w(i)"));
			boolean v0 = (row != null && row.contains("x(i)"));

			while ((row = csvReader.readLine()) != null) {
				String[] data = row.split(sep);
				if (data.length > 0 && data[0].equals("#")) {
					return;
				}

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
			e.printStackTrace();
		}
	}

	private boolean csvSaveCagesMeasures(CagesArray cages, String directory) {
		try {
			FileWriter csvWriter = new FileWriter(directory + File.separator + ID_CAGESMEASURES_CSV);
			csvSaveDESCRIPTIONSection(cages, csvWriter);
			csvSaveCAGESection(cages, csvWriter);
			csvSaveMeasuresSection(cages, csvWriter, EnumCageMeasures.POSITION);
			csvWriter.flush();
			csvWriter.close();

		} catch (IOException e) {
			Logger.error("CagesArrayPersistence:csvSaveCagesMeasures() Error: " + e.getMessage(), e);
		}
		return true;
	}

	private boolean csvSaveDESCRIPTIONSection(CagesArray cages, FileWriter csvWriter) {
		try {
			csvWriter.append("#" + csvSep + "DESCRIPTION" + csvSep + "Cages data\n");
			csvWriter.append("n cages=" + csvSep + Integer.toString(cages.cagesList.size()) + "\n");
			csvWriter.append("#" + csvSep + "#\n");
		} catch (IOException e) {
			Logger.error("CagesArrayPersistence:csvSaveDescriptionSection() Error: " + e.getMessage(), e);
		}
		return true;
	}

	private boolean csvSaveCAGESection(CagesArray cages, FileWriter csvWriter) {
		try {
			// Legacy format: CAGE section with header line and ROI data
			csvWriter.append("#" + csvSep + "CAGE" + csvSep + "Cage properties\n");
			// Header: cageID, nFlies, age, Comment, strain, sect, ROIname, npoints, x1, y1,
			// x2, y2, ...
			csvWriter.append("cageID" + csvSep + "nFlies" + csvSep + "age" + csvSep + "Comment" + csvSep + "strain"
					+ csvSep + "sect" + csvSep + "ROIname" + csvSep + "npoints\n");

			for (Cage cage : cages.cagesList) {
				// Legacy format: cageID, nFlies, age, Comment, strain, sect, ROIname, npoints,
				// x1, y1, x2, y2, ...
				csvWriter.append(String.format("%d%s%d%s%d%s%s%s%s%s%s%s", cage.getProperties().getCageID(), csvSep,
						cage.getProperties().getCageNFlies(), csvSep, cage.getProperties().getFlyAge(), csvSep,
						cage.getProperties().getComment() != null ? cage.getProperties().getComment() : "", csvSep,
						cage.getProperties().getFlyStrain() != null ? cage.getProperties().getFlyStrain() : "", csvSep,
						cage.getProperties().getFlySex() != null ? cage.getProperties().getFlySex() : "", csvSep));

				// Add ROI name
				String roiName = (cage.getRoi() != null && cage.getRoi().getName() != null) ? cage.getRoi().getName()
						: "cage" + String.format("%03d", cage.getProperties().getCageID());
				csvWriter.append(roiName + csvSep);

				// Add ROI polygon points if available
				if (cage.getRoi() != null && cage.getRoi() instanceof plugins.kernel.roi.roi2d.ROI2DPolygon) {
					plugins.kernel.roi.roi2d.ROI2DPolygon polyRoi = (plugins.kernel.roi.roi2d.ROI2DPolygon) cage
							.getRoi();
					icy.type.geom.Polygon2D polygon = polyRoi.getPolygon2D();
					csvWriter.append(Integer.toString(polygon.npoints));
					for (int i = 0; i < polygon.npoints; i++) {
						csvWriter.append(csvSep + Integer.toString((int) polygon.xpoints[i]));
						csvWriter.append(csvSep + Integer.toString((int) polygon.ypoints[i]));
					}
				} else {
					csvWriter.append("0");
				}
				csvWriter.append("\n");
			}
			csvWriter.append("#" + csvSep + "#\n");
		} catch (IOException e) {
			Logger.error("CagesArrayPersistence:csvSaveCAGESection() Error: " + e.getMessage(), e);
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

	/**
	 * Synchronously loads cage descriptions and measures from CSV/XML files.
	 * 
	 * @param cages     The CagesArray to populate
	 * @param directory The directory containing cage files
	 * @param exp       The experiment being loaded (for validation, currently
	 *                  unused)
	 * @return true if successful, false otherwise
	 */
	public boolean loadCages(CagesArray cages, String directory, Experiment exp) {
		if (directory == null) {
			Logger.warn("CagesArrayPersistence:loadCages() directory is null");
			return false;
		}

		try {
			// Use existing synchronous load method to load descriptions
			boolean descriptionsLoaded = load_Cages(cages, directory);

			// Also load measures from bin directory (if available)
			if (exp != null) {
				String binDir = exp.getKymosBinFullDirectory();
				if (binDir != null) {
					loadCagesArrayMeasures(cages, binDir);
				}
			}

			return descriptionsLoaded;
		} catch (Exception e) {
			Logger.error("CagesArrayPersistence:loadCages() Error: " + e.getMessage(), e);
			return false;
		}
	}

	/**
	 * Synchronously saves cage descriptions to CSV file.
	 * 
	 * @param cages     The CagesArray to save
	 * @param directory The directory to save CagesArray.csv
	 * @param exp       The experiment being saved (for validation, currently
	 *                  unused)
	 * @return true if successful, false otherwise
	 */
	public boolean saveCages(CagesArray cages, String directory, Experiment exp) {
		if (directory == null) {
			Logger.warn("CagesArrayPersistence:saveCages() directory is null");
			return false;
		}

		Path path = Paths.get(directory);
		if (!Files.exists(path)) {
			Logger.warn("CagesArrayPersistence:saveCages() directory does not exist: " + directory);
			return false;
		}

		try {
			// Save descriptions to new format (CagesArray.csv in results directory)
			return saveCagesArrayDescription(cages, directory);
		} catch (Exception e) {
			Logger.error("CagesArrayPersistence:saveCages() Error: " + e.getMessage(), e);
			return false;
		}
	}
}
