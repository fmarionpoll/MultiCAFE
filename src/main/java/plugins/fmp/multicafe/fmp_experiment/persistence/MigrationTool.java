package plugins.fmp.multicafe.fmp_experiment.persistence;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import plugins.fmp.multicafe.fmp_experiment.Experiment;
import plugins.fmp.multicafe.fmp_experiment.cages.Cage;
import plugins.fmp.multicafe.fmp_experiment.capillaries.Capillary;
import plugins.fmp.multicafe.fmp_experiment.ids.CapillaryID;
import plugins.fmp.multicafe.fmp_experiment.ids.SpotID;
import plugins.fmp.multicafe.fmp_experiment.spots.Spot;
import plugins.fmp.multicafe.fmp_experiment.spots.SpotsArray;
import plugins.fmp.multicafe.fmp_experiment.spots.SpotsArrayPersistence;
import plugins.fmp.multicafe.fmp_tools.Logger;

/**
 * Migrates experiments from old format (spots in cage XML) to new format
 * (spots in CSV, IDs in cage XML).
 */
public class MigrationTool {

	private MigrationDetector detector = new MigrationDetector();

	/**
	 * Migrates an experiment from old format to new format.
	 * 
	 * @param exp       the Experiment to migrate
	 * @param directory the experiment results directory
	 * @return true if migration was successful
	 */
	public boolean migrateExperiment(Experiment exp, String directory) {
		if (exp == null || directory == null) {
			Logger.error("MigrationTool:migrateExperiment() Invalid parameters", null);
			return false;
		}

		if (!detector.needsMigration(directory)) {
			Logger.info("MigrationTool:migrateExperiment() No migration needed - already in new format");
			return true;
		}

		Logger.info("MigrationTool:migrateExperiment() Starting migration for: " + directory);

		try {
			// Step 1: Backup old files
			backupOldFiles(directory);

			// Step 2: Load old format (spots from cage XML)
			// This happens when loading cages - spots are loaded from nested XML
			// We need to extract them before saving in new format

			// Step 3: Extract spots from all cages to global SpotsArray
			extractSpotsFromCages(exp);

			// Step 4: Convert cage's spotsArray to SpotID lists
			convertCageSpotsToIDs(exp);

			// Step 5: Convert cage's capillaries to CapillaryID lists
			convertCageCapillariesToIDs(exp);

			// Step 6: Save in new format
			// Save descriptions to results directory, measures to bin directory
			
			// Save spots descriptions to new format
			SpotsArrayPersistence spotsPersistence = new SpotsArrayPersistence();
			boolean spotsDescriptionsSaved = spotsPersistence.save_SpotsArray_Descriptions(exp.getSpotsArray(), directory);
			if (!spotsDescriptionsSaved) {
				Logger.warn("MigrationTool:migrateExperiment() Failed to save spot descriptions to CSV");
			}
			
			// Save cages descriptions to new format
			boolean cagesDescriptionsSaved = exp.getCages().getPersistence().save_CagesArray_Descriptions(exp.getCages(), directory);
			if (!cagesDescriptionsSaved) {
				Logger.warn("MigrationTool:migrateExperiment() Failed to save cage descriptions");
			}
			
			// Save capillary descriptions to new format (if available)
			boolean capillariesDescriptionsSaved = exp.getCapillaries().getPersistence().save_CapillariesArray_Descriptions(exp.getCapillaries(), directory);
			if (!capillariesDescriptionsSaved) {
				Logger.warn("MigrationTool:migrateExperiment() Failed to save capillary descriptions");
			}
			
			// Save measures to bin directory (if available)
			String binDir = exp.getKymosBinFullDirectory();
			if (binDir != null) {
				// Save spots measures
				spotsPersistence.save_SpotsArrayMeasures(exp.getSpotsArray(), binDir);
				
				// Save cages measures
				exp.getCages().getPersistence().save_CagesArrayMeasures(exp.getCages(), binDir);
				
				// Save capillary measures
				exp.getCapillaries().getPersistence().save_CapillariesArrayMeasures(exp.getCapillaries(), binDir);
			}

			Logger.info("MigrationTool:migrateExperiment() Migration completed successfully");
			return spotsDescriptionsSaved && cagesDescriptionsSaved;

		} catch (Exception e) {
			Logger.error("MigrationTool:migrateExperiment() Migration failed: " + e.getMessage(), e, true);
			return false;
		}
	}

	/**
	 * Backs up old files before migration.
	 */
	private void backupOldFiles(String directory) {
		try {
			Path dirPath = Paths.get(directory);
			Path backupDir = dirPath.resolve("backup_before_migration");
			if (!Files.exists(backupDir)) {
				Files.createDirectories(backupDir);
			}

			// Backup cage XML if it exists
			Path cagesXml = dirPath.resolve("MCdrosotrack.xml");
			if (Files.exists(cagesXml)) {
				Path backupXml = backupDir.resolve("MCdrosotrack.xml.backup");
				Files.copy(cagesXml, backupXml, StandardCopyOption.REPLACE_EXISTING);
				Logger.info("MigrationTool:backupOldFiles() Backed up: " + backupXml);
			}

		} catch (Exception e) {
			Logger.warn("MigrationTool:backupOldFiles() Failed to create backup: " + e.getMessage());
		}
	}

	/**
	 * Extracts all spots from cages and adds them to the global SpotsArray.
	 */
	private void extractSpotsFromCages(Experiment exp) {
		SpotsArray globalSpots = exp.getSpotsArray();
		globalSpots.clearSpots();

		for (Cage cage : exp.getCages().getCageList()) {
			// Get spots from legacy spotsArray (loaded from old XML format)
			for (Spot spot : cage.getSpotsArray().getList()) {
				// Ensure coordinates are saved (extract from ROI if needed)
				if (spot.getRoi() != null && (spot.getProperties().getSpotXCoord() < 0
						|| spot.getProperties().getSpotYCoord() < 0)) {
					// Extract coordinates from ROI
					java.awt.geom.Rectangle2D bounds = spot.getRoi().getBounds2D();
					spot.getProperties().setSpotXCoord((int) bounds.getCenterX());
					spot.getProperties().setSpotYCoord((int) bounds.getCenterY());
					if (spot.getProperties().getSpotRadius() <= 0) {
						spot.getProperties().setSpotRadius((int) Math.max(bounds.getWidth(), bounds.getHeight()) / 2);
					}
				}

				// Add to global array if not already present
				if (!globalSpots.isSpotPresent(spot)) {
					globalSpots.addSpot(spot);
				}
			}
		}

		Logger.info("MigrationTool:extractSpotsFromCages() Extracted " + globalSpots.getSpotsCount() + " spots");
	}

	/**
	 * Converts cage's spotsArray to SpotID lists.
	 */
	private void convertCageSpotsToIDs(Experiment exp) {
		for (Cage cage : exp.getCages().getCageList()) {
			List<SpotID> spotIDs = new ArrayList<>();
			for (Spot spot : cage.getSpotsArray().getList()) {
				int cageID = spot.getProperties().getCageID();
				int position = spot.getProperties().getCagePosition();
				if (cageID >= 0 && position >= 0) {
					spotIDs.add(new SpotID(cageID, position));
				}
			}
			cage.setSpotIDs(spotIDs);
		}
		Logger.info("MigrationTool:convertCageSpotsToIDs() Converted spots to IDs for all cages");
	}

	/**
	 * Converts cage's capillaries to CapillaryID lists.
	 */
	private void convertCageCapillariesToIDs(Experiment exp) {
		for (Cage cage : exp.getCages().getCageList()) {
			List<CapillaryID> capillaryIDs = new ArrayList<>();
			for (Capillary cap : cage.getCapillaries().getList()) {
				capillaryIDs.add(new CapillaryID(cap.getKymographIndex()));
			}
			cage.setCapillaryIDs(capillaryIDs);
		}
		Logger.info("MigrationTool:convertCageCapillariesToIDs() Converted capillaries to IDs for all cages");
	}
}

