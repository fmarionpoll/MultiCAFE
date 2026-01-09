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

	private static final String ID_CAGES = "Cages";
	private static final String ID_NCAGES = "n_cages";
	private static final String ID_NCAGESALONGX = "N_cages_along_X";
	private static final String ID_NCAGESALONGY = "N_cages_along_Y";
	private static final String ID_NCOLUMNSPERCAGE = "N_columns_per_cage";
	private static final String ID_NROWSPERCAGE = "N_rows_per_cage";

	// New v2 format filenames
	private static final String ID_V2_CAGESARRAY_CSV = "v2_CagesArray.csv";
	private static final String ID_V2_CAGESARRAYMEASURES_CSV = "v2_CagesArrayMeasures.csv";

	// Legacy filenames (for fallback)
	private static final String ID_CAGESARRAY_CSV = "CagesArray.csv";
	private static final String ID_CAGESARRAYMEASURES_CSV = "CagesArrayMeasures.csv";
	private static final String ID_MCDROSOTRACK_XML = "MCdrosotrack.xml";

	// ========================================================================
	// Public API methods (delegate to nested classes)
	// ========================================================================

	public boolean load_Cages(CagesArray cages, String directory) {
		int cagesBefore = cages.cagesList.size();

		// Priority 1: Try new v2_ format (descriptions + measures separate)
		boolean descriptionsLoaded = Persistence.loadDescription(cages, directory);

		if (descriptionsLoaded) {
			// Successfully loaded from v2_ format - return early to prevent legacy loading from overwriting ROIs
			return true;
		}

		// v2_ format not found - try fallback options
		// Optionally load ROIs from XML if CSV doesn't have them
		String tempName = directory + File.separator + ID_MCDROSOTRACK_XML;
		int cagesWithROIsBefore = 0;
		for (Cage cage : cages.cagesList) {
			if (cage.getRoi() != null) {
				cagesWithROIsBefore++;
			}
		}

		if (cagesWithROIsBefore < cages.cagesList.size()) {
			Legacy.xmlLoadCagesROIsOnly(cages, tempName);
		}

		// Priority 2: Fall back to legacy combined CSV format
		boolean csvLoadSuccess = false;
		try {
			csvLoadSuccess = Legacy.csvLoadCagesMeasures(cages, directory);
			if (!csvLoadSuccess) {
				int cagesAfterCSV = cages.cagesList.size();
				Logger.info(String.format("CagesArrayPersistence:load_Cages() Legacy CSV loaded: %d cages (was %d)",
						cagesAfterCSV, cagesBefore));

				// Optionally load ROIs from XML
				Legacy.xmlLoadCagesROIsOnly(cages, tempName);

				return true;
			}
		} catch (Exception e) {
			Logger.error("CagesArrayPersistence:load_Cages() Failed to load from legacy CSV: " + directory, e);
		}

		// Priority 3: Fall back to XML (legacy format)
		Logger.warn("CagesArrayPersistence:load_Cages() CSV load failed, falling back to XML");
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

		// Save descriptions to v2_ format file (includes ROI coordinates in CSV)
		return Persistence.saveDescription(cages, directory);
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
		return Persistence.saveDescription(cages, resultsDirectory);
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
		return Persistence.saveMeasures(cages, binDirectory);
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
		return Persistence.loadDescription(cages, resultsDirectory);
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
		return Persistence.loadMeasures(cages, binDirectory);
	}

	// ========================================================================
	// Legacy methods - private, only for internal use within persistence class
	// ========================================================================

	private boolean xmlReadCagesFromFileNoQuestion(CagesArray cages, String tempname) {
		return Legacy.xmlReadCagesFromFileNoQuestion(cages, tempname);
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

	// ========================================================================
	// Nested class for current v2 format persistence
	// ========================================================================

	public static class Persistence {
		private static final String csvSep = ";";

		/**
		 * Loads cage descriptions (DESCRIPTION and CAGE sections) from CagesArray.csv.
		 * Tries v2_ format first, then falls back to legacy format.
		 */
		public static boolean loadDescription(CagesArray cages, String resultsDirectory) {
			if (resultsDirectory == null) {
				return false;
			}

			// Priority 1: Try v2_ format
			String pathToCsv = resultsDirectory + File.separator + ID_V2_CAGESARRAY_CSV;
			File csvFile = new File(pathToCsv);
			if (!csvFile.isFile()) {
				// Priority 2: Fallback to legacy format
				pathToCsv = resultsDirectory + File.separator + ID_CAGESARRAY_CSV;
				csvFile = new File(pathToCsv);
				if (!csvFile.isFile()) {
					return false;
				}
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
								Legacy.csvLoad_DESCRIPTION(cages, csvReader, sep);
								break;
							case "CAGE":
							case "CAGES":
								cageLoaded = true;
								Legacy.csvLoad_CAGE(cages, csvReader, sep);
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
		 * directory. Tries v2_ format first, then falls back to legacy format.
		 */
		public static boolean loadMeasures(CagesArray cages, String binDirectory) {
			if (binDirectory == null) {
				return false;
			}

			// Priority 1: Try v2_ format
			String pathToCsv = binDirectory + File.separator + ID_V2_CAGESARRAYMEASURES_CSV;
			File csvFile = new File(pathToCsv);
			if (!csvFile.isFile()) {
				// Priority 2: Fallback to legacy format
				pathToCsv = binDirectory + File.separator + ID_CAGESARRAYMEASURES_CSV;
				csvFile = new File(pathToCsv);
				if (!csvFile.isFile()) {
					return false;
				}
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
								Legacy.csvLoad_Measures(cages, csvReader, EnumCageMeasures.POSITION, sep);
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

		/**
		 * Saves cage descriptions (DESCRIPTION and CAGE sections) to CagesArray.csv in
		 * results directory. Always saves to v2_ format.
		 */
		public static boolean saveDescription(CagesArray cages, String resultsDirectory) {
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
				// Always save to v2_ format
				FileWriter csvWriter = new FileWriter(resultsDirectory + File.separator + ID_V2_CAGESARRAY_CSV);
				Legacy.csvSaveDESCRIPTIONSection(cages, csvWriter, csvSep);
				Legacy.csvSaveCAGESection(cages, csvWriter, csvSep);
				csvWriter.flush();
				csvWriter.close();
				Logger.info("CagesArrayPersistence:saveCagesArray() Saved descriptions to " + ID_V2_CAGESARRAY_CSV);
				return true;
			} catch (IOException e) {
				Logger.error("CagesArrayPersistence:saveCagesArray() Error: " + e.getMessage(), e);
				return false;
			}
		}

		/**
		 * Saves cage measures (POSITION section) to CagesArrayMeasures.csv in bin
		 * directory. Always saves to v2_ format.
		 */
		public static boolean saveMeasures(CagesArray cages, String binDirectory) {
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
				// Always save to v2_ format
				FileWriter csvWriter = new FileWriter(binDirectory + File.separator + ID_V2_CAGESARRAYMEASURES_CSV);
				Legacy.csvSaveMeasuresSection(cages, csvWriter, EnumCageMeasures.POSITION, csvSep);
				csvWriter.flush();
				csvWriter.close();
				Logger.info("CagesArrayPersistence:saveCagesArrayMeasures() Saved measures to "
						+ ID_V2_CAGESARRAYMEASURES_CSV);
				return true;
			} catch (IOException e) {
				Logger.error("CagesArrayPersistence:saveCagesArrayMeasures() Error: " + e.getMessage(), e);
				return false;
			}
		}
	}

	// ========================================================================
	// Nested class for legacy format persistence
	// ========================================================================

	public static class Legacy {
		private static final String csvSep = ";";
		private static final String ID_CAGESMEASURES_CSV = "CagesMeasures.csv";

		public static boolean xmlReadCagesFromFile(CagesArray cages, Experiment exp) {
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

		public static boolean xmlReadCagesFromFileNoQuestion(CagesArray cages, String tempname) {
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

		static boolean xmlLoadCages(CagesArray cages, Node node) {
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
							Logger.warn("CagesArrayPersistence:xmlLoadCages() WARNING: Failed to load cage at index "
									+ index);
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

//		private static boolean xmlSaveCagesROIsOnly(CagesArray cages, String tempname) {
//			try {
//				final Document doc = XMLUtil.createDocument(true);
//				if (doc == null) {
//					Logger.error("CagesArrayPersistence:xmlSaveCagesROIsOnly() ERROR: Could not create XML document");
//					return false;
//				}
//
//				Node node = XMLUtil.getRootElement(doc);
//				Element xmlVal = XMLUtil.addElement(node, ID_CAGES);
//				int ncages = cages.cagesList.size();
//				XMLUtil.setAttributeIntValue(xmlVal, ID_NCAGES, ncages);
//				XMLUtil.setAttributeIntValue(xmlVal, ID_NCAGESALONGX, cages.nCagesAlongX);
//				XMLUtil.setAttributeIntValue(xmlVal, ID_NCAGESALONGY, cages.nCagesAlongY);
//				XMLUtil.setAttributeIntValue(xmlVal, ID_NCOLUMNSPERCAGE, cages.nColumnsPerCage);
//				XMLUtil.setAttributeIntValue(xmlVal, ID_NROWSPERCAGE, cages.nRowsPerCage);
//
//				int index = 0;
//				for (Cage cage : cages.cagesList) {
//					if (cage == null) {
//						Logger.warn("CagesArrayPersistence:xmlSaveCagesROIsOnly() WARNING: Null cage at index " + index);
//						index++;
//						continue;
//					}
//
//					Element cageElement = XMLUtil.addElement(xmlVal, "Cage" + index);
//					if (cageElement != null) {
//						// Save only ROI (cage limits)
//						cage.xmlSaveCageLimits(cageElement);
//					}
//					index++;
//				}
//
//				boolean success = XMLUtil.saveDocument(doc, tempname);
//				return success;
//
//			} catch (Exception e) {
//				Logger.error("CagesArrayPersistence:xmlSaveCagesROIsOnly() ERROR during XML saving: " + e.getMessage(), e);
//				return false;
//			}
//		}

		static boolean xmlLoadCagesROIsOnly(CagesArray cages, String tempname) {
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
					Logger.warn(
							"CagesArrayPersistence:xmlLoadCagesROIsOnly() Could not load XML document: " + tempname);
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
				Logger.error("CagesArrayPersistence:xmlLoadCagesROIsOnly() ERROR during XML loading: " + e.getMessage(),
						e);
				return false;
			}
		}

		static boolean csvLoadCagesMeasures(CagesArray cages, String directory) throws Exception {
			String pathToCsv = directory + File.separator + ID_CAGESMEASURES_CSV;
			File csvFile = new File(pathToCsv);
			if (!csvFile.isFile()) {
				return false;
			}

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
								break;
							}
						}
					}
				}
			} finally {
				csvReader.close();
			}

			return descriptionCount > 0 || cageCount > 0 || positionCount > 0;
		}

		static void csvLoad_DIMENSION(CagesArray cages, BufferedReader csvReader, String sep) {
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
									Logger.warn("CagesArrayPersistence:csvLoad_DIMENSION() Invalid n_cages value: "
											+ data[1]);
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

		static void csvLoad_DESCRIPTION(CagesArray cages, BufferedReader csvReader, String sep) {
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
			} catch (IOException e) {
				Logger.error("CagesArrayPersistence:csvLoad_DESCRIPTION() Error: " + e.getMessage(), e);
			}
		}

		static void csvLoad_CAGE(CagesArray cages, BufferedReader csvReader, String sep) {
			String row;
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
					}
				}
			} catch (IOException e) {
				Logger.error("CagesArrayPersistence:csvLoad_CAGE() Error: " + e.getMessage(), e);
			}
		}

		private static Cage getCagefromID(CagesArray cages, String data) {
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

		static void csvLoad_Measures(CagesArray cages, BufferedReader csvReader, EnumCageMeasures measureType,
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

//		private static boolean csvSaveCagesMeasures(CagesArray cages, String directory) {
//			try {
//				FileWriter csvWriter = new FileWriter(directory + File.separator + ID_CAGESMEASURES_CSV);
//				csvSaveDESCRIPTIONSection(cages, csvWriter, csvSep);
//				csvSaveCAGESection(cages, csvWriter, csvSep);
//				csvSaveMeasuresSection(cages, csvWriter, EnumCageMeasures.POSITION, csvSep);
//				csvWriter.flush();
//				csvWriter.close();
//
//			} catch (IOException e) {
//				Logger.error("CagesArrayPersistence:csvSaveCagesMeasures() Error: " + e.getMessage(), e);
//			}
//			return true;
//		}

		static boolean csvSaveDESCRIPTIONSection(CagesArray cages, FileWriter csvWriter, String csvSep) {
			try {
				csvWriter.append("#" + csvSep + "DESCRIPTION" + csvSep + "Cages data\n");
				csvWriter.append("n cages=" + csvSep + Integer.toString(cages.cagesList.size()) + "\n");
				csvWriter.append("#" + csvSep + "#\n");
			} catch (IOException e) {
				Logger.error("CagesArrayPersistence:csvSaveDescriptionSection() Error: " + e.getMessage(), e);
			}
			return true;
		}

		static boolean csvSaveCAGESection(CagesArray cages, FileWriter csvWriter, String csvSep) {
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
							cage.getProperties().getFlyStrain() != null ? cage.getProperties().getFlyStrain() : "",
							csvSep, cage.getProperties().getFlySex() != null ? cage.getProperties().getFlySex() : "",
							csvSep));

					// Add ROI name
					String roiName = (cage.getRoi() != null && cage.getRoi().getName() != null)
							? cage.getRoi().getName()
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

		static boolean csvSaveMeasuresSection(CagesArray cages, FileWriter csvWriter, EnumCageMeasures measuresType,
				String csvSep) {
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

		private static String csvExport_MEASURE_Header(EnumCageMeasures measureType, String sep, boolean complete) {
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

		private static String csvExport_MEASURE_Data(Cage cage, EnumCageMeasures measureType, String sep,
				boolean complete) {
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
}
