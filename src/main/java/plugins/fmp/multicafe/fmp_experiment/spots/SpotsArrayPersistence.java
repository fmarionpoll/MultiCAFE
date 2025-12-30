package plugins.fmp.multicafe.fmp_experiment.spots;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import plugins.fmp.multicafe.fmp_tools.Logger;

/**
 * Handles CSV-only persistence for SpotsArray.
 * ROIs are not persisted - they are regenerated from coordinates when needed for display.
 */
public class SpotsArrayPersistence {

	// New format filenames
	private static final String ID_SPOTSARRAY_CSV = "SpotsArray.csv";
	private static final String ID_SPOTSARRAYMEASURES_CSV = "SpotsArrayMeasures.csv";
	
	// Legacy filename (for fallback)
	private static final String CSV_FILENAME = "SpotsMeasures.csv";

	/**
	 * Loads spots from CSV file with fallback logic.
	 * Tries new format first, then falls back to legacy format.
	 * 
	 * @param spotsArray the SpotsArray to populate
	 * @param directory  the directory containing spot files
	 * @return true if successful
	 */
	public boolean load_SpotsArray(SpotsArray spotsArray, String directory) {
		if (directory == null) {
			Logger.warn("SpotsArrayPersistence:load_SpotsArray() directory is null");
			return false;
		}

		Path path = Paths.get(directory);
		if (!Files.exists(path)) {
			Logger.warn("SpotsArrayPersistence:load_SpotsArray() directory does not exist: " + directory);
			return false;
		}

		// Priority 1: Try new format (descriptions only)
		boolean descriptionsLoaded = load_SpotsArray_Descriptions(spotsArray, directory);
		if (descriptionsLoaded) {
			Logger.info("SpotsArrayPersistence:load_SpotsArray() loaded " + spotsArray.getSpotsCount()
					+ " spot descriptions from new format");
			return true;
		}

		// Priority 2: Fall back to legacy format (combined file)
		try {
			boolean success = spotsArray.loadSpotsAll(directory);
			if (success) {
				Logger.info("SpotsArrayPersistence:load_SpotsArray() loaded " + spotsArray.getSpotsCount()
						+ " spots from legacy format");
			}
			return success;
		} catch (Exception e) {
			Logger.error("SpotsArrayPersistence:load_SpotsArray() Failed to load spots from: " + directory, e, true);
			return false;
		}
	}
	
	/**
	 * Loads spot descriptions (SPOTS_ARRAY and SPOTS sections) from SpotsArray.csv.
	 * Stops reading when it encounters measure sections.
	 * 
	 * @param spotsArray the SpotsArray to populate
	 * @param resultsDirectory the results directory
	 * @return true if successful
	 */
	public boolean load_SpotsArray_Descriptions(SpotsArray spotsArray, String resultsDirectory) {
		if (resultsDirectory == null) {
			return false;
		}

		Path csvPath = Paths.get(resultsDirectory, ID_SPOTSARRAY_CSV);
		if (!Files.exists(csvPath)) {
			return false;
		}

		try (BufferedReader reader = new BufferedReader(new FileReader(csvPath.toFile()))) {
			String line;
			String sep = ";";
			boolean descriptionLoaded = false;
			boolean spotsLoaded = false;
			
			while ((line = reader.readLine()) != null) {
				if (line.length() > 0 && line.charAt(0) == '#')
					sep = String.valueOf(line.charAt(1));
					
				String[] data = line.split(sep);
				if (data.length > 0 && data[0].equals("#")) {
					if (data.length > 1) {
						switch (data[1]) {
						case "SPOTS_ARRAY":
							descriptionLoaded = true;
							spotsArray.csvLoadSpotsDescription(reader, sep);
							break;
						case "SPOTS":
							spotsLoaded = true;
							spotsArray.csvLoadSpotsArray(reader, sep);
							break;
						case "AREA_SUM":
						case "AREA_SUMCLEAN":
						case "AREA_FLYPRESENT":
							// Stop reading when we hit measures section
							return descriptionLoaded || spotsLoaded;
						default:
							// Check if it's a measure type
							plugins.fmp.multicafe.fmp_experiment.spots.EnumSpotMeasures measure = 
								plugins.fmp.multicafe.fmp_experiment.spots.EnumSpotMeasures.findByText(data[1]);
							if (measure != null) {
								// Stop reading when we hit measures section
								return descriptionLoaded || spotsLoaded;
							}
							break;
						}
					}
				}
			}
			return descriptionLoaded || spotsLoaded;
		} catch (Exception e) {
			Logger.error("SpotsArrayPersistence:load_SpotsArray_Descriptions() Failed: " + e.getMessage(), e, true);
			return false;
		}
	}
	
	/**
	 * Loads spot measures from SpotsArrayMeasures.csv in bin directory.
	 * 
	 * @param spotsArray the SpotsArray to populate
	 * @param binDirectory the bin directory (e.g., results/bin60)
	 * @return true if successful
	 */
	public boolean load_SpotsArrayMeasures(SpotsArray spotsArray, String binDirectory) {
		if (binDirectory == null) {
			return false;
		}

		Path csvPath = Paths.get(binDirectory, ID_SPOTSARRAYMEASURES_CSV);
		if (!Files.exists(csvPath)) {
			return false;
		}

		try (BufferedReader reader = new BufferedReader(new FileReader(csvPath.toFile()))) {
			String line;
			String sep = ";";
			
			while ((line = reader.readLine()) != null) {
				if (line.length() > 0 && line.charAt(0) == '#')
					sep = String.valueOf(line.charAt(1));
					
				String[] data = line.split(sep);
				if (data.length > 0 && data[0].equals("#")) {
					if (data.length > 1) {
						plugins.fmp.multicafe.fmp_experiment.spots.EnumSpotMeasures measure = 
							plugins.fmp.multicafe.fmp_experiment.spots.EnumSpotMeasures.findByText(data[1]);
						if (measure != null) {
							spotsArray.csvLoadSpotsMeasures(reader, measure, sep);
						}
					}
				}
			}
			return true;
		} catch (Exception e) {
			Logger.error("SpotsArrayPersistence:load_SpotsArrayMeasures() Failed: " + e.getMessage(), e, true);
			return false;
		}
	}

	/**
	 * Saves spots to CSV file (descriptions only, to results directory).
	 * 
	 * @param spotsArray the SpotsArray to save
	 * @param resultsDirectory the results directory
	 * @return true if successful
	 */
	public boolean save_SpotsArray(SpotsArray spotsArray, String resultsDirectory) {
		return save_SpotsArray_Descriptions(spotsArray, resultsDirectory);
	}
	
	/**
	 * Saves spot descriptions (SPOTS_ARRAY and SPOTS sections) to SpotsArray.csv.
	 * 
	 * @param spotsArray the SpotsArray to save
	 * @param resultsDirectory the results directory
	 * @return true if successful
	 */
	public boolean save_SpotsArray_Descriptions(SpotsArray spotsArray, String resultsDirectory) {
		if (resultsDirectory == null) {
			Logger.warn("SpotsArrayPersistence:save_SpotsArray_Descriptions() directory is null");
			return false;
		}

		Path path = Paths.get(resultsDirectory);
		if (!Files.exists(path)) {
			Logger.warn("SpotsArrayPersistence:save_SpotsArray_Descriptions() directory does not exist: " + resultsDirectory);
			return false;
		}

		Path csvPath = Paths.get(resultsDirectory, ID_SPOTSARRAY_CSV);
		try (FileWriter writer = new FileWriter(csvPath.toFile())) {
			// Save spots array section
			if (!spotsArray.csvSaveSpotsArraySection(writer)) {
				return false;
			}
			Logger.info("SpotsArrayPersistence:save_SpotsArray_Descriptions() saved " + spotsArray.getSpotsCount()
					+ " spot descriptions to " + ID_SPOTSARRAY_CSV);
			return true;
		} catch (IOException e) {
			Logger.error("SpotsArrayPersistence:save_SpotsArray_Descriptions() Failed: " + e.getMessage(), e, true);
			return false;
		}
	}
	
	/**
	 * Saves spot measures (AREA_SUM, AREA_SUMCLEAN sections) to SpotsArrayMeasures.csv in bin directory.
	 * 
	 * @param spotsArray the SpotsArray to save
	 * @param binDirectory the bin directory (e.g., results/bin60)
	 * @return true if successful
	 */
	public boolean save_SpotsArrayMeasures(SpotsArray spotsArray, String binDirectory) {
		if (binDirectory == null) {
			Logger.warn("SpotsArrayPersistence:save_SpotsArrayMeasures() directory is null");
			return false;
		}

		Path path = Paths.get(binDirectory);
		if (!Files.exists(path)) {
			Logger.warn("SpotsArrayPersistence:save_SpotsArrayMeasures() directory does not exist: " + binDirectory);
			return false;
		}

		Path csvPath = Paths.get(binDirectory, ID_SPOTSARRAYMEASURES_CSV);
		try (FileWriter writer = new FileWriter(csvPath.toFile())) {
			// Save measures sections
			if (!spotsArray.csvSaveMeasuresSection(writer, plugins.fmp.multicafe.fmp_experiment.spots.EnumSpotMeasures.AREA_SUM)) {
				return false;
			}
			if (!spotsArray.csvSaveMeasuresSection(writer, plugins.fmp.multicafe.fmp_experiment.spots.EnumSpotMeasures.AREA_SUMCLEAN)) {
				return false;
			}
			Logger.info("SpotsArrayPersistence:save_SpotsArrayMeasures() saved measures to " + ID_SPOTSARRAYMEASURES_CSV);
			return true;
		} catch (IOException e) {
			Logger.error("SpotsArrayPersistence:save_SpotsArrayMeasures() Failed: " + e.getMessage(), e, true);
			return false;
		}
	}

	/**
	 * Gets the CSV filename used for persistence (descriptions).
	 * 
	 * @return the CSV filename
	 */
	public String getCSVFilename() {
		return ID_SPOTSARRAY_CSV;
	}
	
	/**
	 * Gets the CSV filename used for measures persistence.
	 * 
	 * @return the measures CSV filename
	 */
	public String getMeasuresCSVFilename() {
		return ID_SPOTSARRAYMEASURES_CSV;
	}
	
	/**
	 * Gets the legacy CSV filename (for fallback).
	 * 
	 * @return the legacy CSV filename
	 */
	public String getLegacyCSVFilename() {
		return CSV_FILENAME;
	}
}

