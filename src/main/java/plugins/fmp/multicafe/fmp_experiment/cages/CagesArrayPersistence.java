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
			CagesArrayPersistenceLegacy.xmlLoadCagesROIsOnly(cages, tempName);
		}

		// Priority 2: Fall back to legacy combined CSV format
		boolean csvLoadSuccess = false;
		try {
			csvLoadSuccess = CagesArrayPersistenceLegacy.csvLoadCagesMeasures(cages, directory);
			if (csvLoadSuccess) {
				int cagesAfterCSV = cages.cagesList.size();
				Logger.info(String.format("CagesArrayPersistence:load_Cages() Legacy CSV loaded: %d cages (was %d)",
						cagesAfterCSV, cagesBefore));

				// Optionally load ROIs from XML
				CagesArrayPersistenceLegacy.xmlLoadCagesROIsOnly(cages, tempName);
				
				// Check if fly positions were loaded, if not try loading from XML
				boolean hasFlyPositions = false;
				for (Cage cage : cages.cagesList) {
					if (cage.flyPositions != null && cage.flyPositions.flyPositionList != null 
							&& !cage.flyPositions.flyPositionList.isEmpty()) {
						hasFlyPositions = true;
						break;
					}
				}
				if (!hasFlyPositions) {
					Logger.info("CagesArrayPersistence:load_Cages() No fly positions found in CSV, trying XML fallback");
					CagesArrayPersistenceLegacy.xmlLoadFlyPositionsFromXML(cages, tempName);
				}

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
		return CagesArrayPersistenceLegacy.xmlReadCagesFromFileNoQuestion(cages, tempname);
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
								CagesArrayPersistenceLegacy.csvLoad_DESCRIPTION(cages, csvReader, sep);
								break;
							case "CAGE":
							case "CAGES":
								cageLoaded = true;
								CagesArrayPersistenceLegacy.csvLoad_CAGE(cages, csvReader, sep);
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
								CagesArrayPersistenceLegacy.csvLoad_Measures(cages, csvReader, EnumCageMeasures.POSITION, sep);
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
				CagesArrayPersistenceLegacy.csvSaveDESCRIPTIONSection(cages, csvWriter, csvSep);
				CagesArrayPersistenceLegacy.csvSaveCAGESection(cages, csvWriter, csvSep);
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
				CagesArrayPersistenceLegacy.csvSaveMeasuresSection(cages, csvWriter, EnumCageMeasures.POSITION, csvSep);
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
	// Legacy format persistence has been moved to CagesArrayPersistenceLegacy
	// ========================================================================
}
