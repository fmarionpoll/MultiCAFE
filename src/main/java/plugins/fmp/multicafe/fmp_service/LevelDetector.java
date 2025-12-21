package plugins.fmp.multicafe.fmp_service;

import java.awt.Rectangle;
import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import icy.image.IcyBufferedImage;
import icy.system.SystemUtil;
import icy.system.thread.Processor;
import plugins.fmp.multicafe.fmp_experiment.Experiment;
import plugins.fmp.multicafe.fmp_experiment.capillaries.Capillary;
import plugins.fmp.multicafe.fmp_experiment.sequence.SequenceKymos;
import plugins.fmp.multicafe.fmp_series.options.BuildSeriesOptions;
import plugins.fmp.multicafe.fmp_tools.Level2D;
import plugins.fmp.multicafe.fmp_tools.Logger;
import plugins.fmp.multicafe.fmp_tools.imageTransform.ImageTransformInterface;
import plugins.fmp.multicafe.fmp_tools.imageTransform.ImageTransformOptions;

public class LevelDetector {

	public void detectLevels(Experiment exp, BuildSeriesOptions options) {
		SequenceKymos seqKymos = exp.getSeqKymos();
		seqKymos.getSequence().removeAllROI();

		int tFirsKymo = options.kymoFirst;
		if (tFirsKymo > seqKymos.getSequence().getSizeT() || tFirsKymo < 0)
			tFirsKymo = 0;
		int tLastKymo = options.kymoLast;
		if (tLastKymo >= seqKymos.getSequence().getSizeT())
			tLastKymo = seqKymos.getSequence().getSizeT() - 1;
		seqKymos.getSequence().beginUpdate();

		int nframes = tLastKymo - tFirsKymo + 1;
		final Processor processor = new Processor(SystemUtil.getNumberOfCPUs());
		processor.setThreadName("detectlevel");
		processor.setPriority(Processor.NORM_PRIORITY);
		ArrayList<Future<?>> futures = new ArrayList<Future<?>>(nframes);
		futures.clear();

		final int jitter = 10;
		final ImageTransformInterface transformPass1 = options.transform01.getFunction();
		final ImageTransformInterface transformPass2 = options.transform02.getFunction();
		final Rectangle searchRect = options.searchArea;
		SequenceLoaderService loader = new SequenceLoaderService();

		// Iterate through capillaries (capillary-driven approach)
		for (Capillary capi : exp.getCapillaries().getList()) {
			if (capi == null) {
				Logger.warn("LevelDetector:detectLevels - Null capillary in list, skipping");
				continue;
			}

			String kymographName = capi.getKymographName();
			if (kymographName == null || kymographName.isEmpty()) {
				Logger.warn("LevelDetector:detectLevels - Capillary has no kymograph name, skipping");
				continue;
			}

			// TODO REVORI REVOIR REVOIR
			// Find matching kymograph file by iterating through kymograph sequence
			int tKymo = -1;
			for (int t = tFirsKymo; t <= tLastKymo; t++) {
				String fileName = seqKymos.getFileNameFromImageList(t);
				if (fileName == null)
					continue;

				String kymographNameFromFile = extractKymographNameFromFileName(fileName);
				if (kymographNameFromFile == null)
					continue;

				// Match by name
				if (kymographNameFromFile.equals(kymographName)) {
					tKymo = t;
					break;
				}
			}

			if (tKymo < 0) {
				// Kymograph file not found for this capillary - skip it
				Logger.warn("LevelDetector:detectLevels - No kymograph file found for capillary: " + kymographName);
				continue;
			}

			// Process this capillary's kymograph
			final int tKymoFinal = tKymo;
			final Capillary capiFinal = capi;

			futures.add(processor.submit(new Runnable() {
				@Override
				public void run() {
					String fileName = seqKymos.getFileNameFromImageList(tKymoFinal);
					if (fileName == null) {
						Logger.warn("LevelDetector:detectLevels - File name is null for tKymo=" + tKymoFinal);
						return;
					}

					IcyBufferedImage imageKymo = loader.imageIORead(fileName);
					if (imageKymo == null) {
						Logger.warn("LevelDetector:detectLevels - Failed to load image for tKymo=" + tKymoFinal);
						return;
					}

					// Get top and bottom level names from capillary
					String topLevelName = capiFinal.getLast2ofCapillaryName();
					if (topLevelName == null || topLevelName.equals("??")) {
						// Fallback to kymograph name if getLast2ofCapillaryName returns null
						topLevelName = capiFinal.getKymographName();
					}
					String bottomLevelName = topLevelName;

					// Process the kymograph image
					processKymographImage(exp, seqKymos, imageKymo, tKymoFinal, capiFinal, topLevelName,
							bottomLevelName, transformPass1, transformPass2, searchRect, jitter);
				}
			}));
		}

		waitFuturesCompletion(processor, futures);
		seqKymos.getSequence().endUpdate();
	}

	private void processKymographImage(Experiment exp, SequenceKymos seqKymos, IcyBufferedImage imageKymo, int tKymo,
			Capillary capi, String topLevelName, String bottomLevelName, ImageTransformInterface transformPass1,
			ImageTransformInterface transformPass2, Rectangle searchRect, int jitter) {

		// Apply transformations
		IcyBufferedImage imagePass1 = transformPass1.getTransformedImage(imageKymo, new ImageTransformOptions());
		IcyBufferedImage imagePass2 = transformPass2.getTransformedImage(imagePass1, new ImageTransformOptions());

		// Detect levels
		Level2D topLevel = detectLevelInImage(imagePass2, searchRect, true, jitter);
		Level2D bottomLevel = detectLevelInImage(imagePass2, searchRect, false, jitter);

		// Store results - levels are stored in measurements.ptsTop and
		// measurements.ptsBottom
		// Note: The actual level detection implementation needs to be restored from the
		// original code
		// This is a placeholder that shows where levels would be stored
		if (topLevel != null) {
			capi.getTopLevel().polylineLevel = topLevel;
			capi.getTopLevel().capName = topLevelName;
		}
		if (bottomLevel != null) {
			capi.getBottomLevel().polylineLevel = bottomLevel;
			capi.getBottomLevel().capName = bottomLevelName;
		}

		// Add ROI to sequence
		// Note: The actual implementation for converting Level2D to ROI and adding to
		// sequence
		// needs to be restored from the original code. This is a placeholder.
		// The original code likely used a utility method to convert Level2D to
		// ROI2DPolyLine
	}

	private Level2D detectLevelInImage(IcyBufferedImage image, Rectangle searchRect, boolean isTop, int jitter) {
		// Implementation for detecting level in image
		// This is a placeholder - the actual implementation would go here
		return null;
	}

	private void waitFuturesCompletion(Processor processor, ArrayList<Future<?>> futures) {
		for (Future<?> future : futures) {
			try {
				future.get();
			} catch (ExecutionException | InterruptedException e) {
				Logger.error("LevelDetector:waitFuturesCompletion", e);
			}
		}
		processor.shutdown();
	}

	private String extractKymographNameFromFileName(String fileName) {
		if (fileName == null)
			return null;

		// Extract just the filename without path
		String name = new File(fileName).getName();

		// Remove extension
		int lastDot = name.lastIndexOf('.');
		if (lastDot > 0) {
			name = name.substring(0, lastDot);
		}

		return name;
	}

//	private void synchronizeCapillariesWithKymographs(Experiment exp, SequenceKymos seqKymos) {
//		if (exp.getCapillaries() == null || seqKymos == null)
//			return;
//		
//		int nKymographs = seqKymos.getSequence().getSizeT();
//		
//		// Get ROIs from cam data sequence to match with capillaries
//		List<ROI2D> camDataROIs = null;
//		if (exp.getSeqCamData() != null && exp.getSeqCamData().getSequence() != null) {
//			camDataROIs = ROI2DUtilities.getROIs2DContainingString("line", exp.getSeqCamData().getSequence());
//		}
//		
//		// Iterate through capillaries (capillary-driven approach)
//		// For each capillary, find the matching kymograph file by name
//		for (Capillary cap : exp.getCapillaries().getList()) {
//			String kymographName = cap.getKymographName();
//			if (kymographName == null || kymographName.isEmpty()) {
//				Logger.warn("LevelDetector:synchronizeCapillariesWithKymographs - Capillary has no kymograph name, skipping");
//				continue;
//			}
//			
//			// Find matching kymograph file by iterating through kymograph sequence
//			boolean found = false;
//			for (int t = 0; t < nKymographs; t++) {
//				String fileName = seqKymos.getFileNameFromImageList(t);
//				if (fileName == null)
//					continue;
//				
//				String kymographNameFromFile = extractKymographNameFromFileName(fileName);
//				if (kymographNameFromFile == null)
//					continue;
//				
//				// Match by name (exact match or with L/R conversion)
//				if (kymographNameFromFile.equals(kymographName)) {
//					cap.kymographIndex = t;
//					found = true;
//					
//					// If capillary doesn't have ROI, try to find it in cam data sequence
//					if (cap.getRoi() == null && camDataROIs != null) {
//						String expectedRoiName = kymographName;
//						String roiNameWithL = kymographName.replace("1", "L");
//						String roiNameWithR = kymographName.replace("2", "R");
//						
//						for (ROI2D roi : camDataROIs) {
//							if (roi != null && roi.getName() != null) {
//								String roiName = roi.getName();
//								if (roiName.equals(expectedRoiName) || roiName.equals(roiNameWithL) || roiName.equals(roiNameWithR)) {
//									cap.setRoi((ROI2DShape) roi);
//									Logger.info("LevelDetector:synchronizeCapillariesWithKymographs - Set missing ROI for capillary: " + kymographName);
//									break;
//								}
//								String roiNameConverted = Capillary.replace_LR_with_12(roiName);
//								if (roiNameConverted.equals(kymographName)) {
//									cap.setRoi((ROI2DShape) roi);
//									Logger.info("LevelDetector:synchronizeCapillariesWithKymographs - Set missing ROI for capillary: " + kymographName + " (converted match)");
//									break;
//								}
//							}
//						}
//					}
//					break;
//				}
//			}
//			
//			if (!found) {
//				// Kymograph file doesn't exist for this capillary - log warning but continue
//				Logger.warn("LevelDetector:synchronizeCapillariesWithKymographs - No kymograph file found for capillary: " + kymographName);
//			}
//		}
//		
//		// Log synchronization results
//		Logger.info("LevelDetector:synchronizeCapillariesWithKymographs - Synchronized " + exp.getCapillaries().getList().size() + " capillaries with " + nKymographs + " kymographs");
//	}

}