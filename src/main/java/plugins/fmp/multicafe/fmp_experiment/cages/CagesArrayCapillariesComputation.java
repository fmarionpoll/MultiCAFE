package plugins.fmp.multicafe.fmp_experiment.cages;

import java.util.ArrayList;
import java.util.List;

import plugins.fmp.multicafe.fmp_experiment.Experiment;
import plugins.fmp.multicafe.fmp_experiment.capillaries.Capillary;
import plugins.fmp.multicafe.fmp_experiment.capillaries.CapillaryMeasure;

/**
 * Handles experiment-wide capillary measure computations that require access to all cages.
 * This includes evaporation correction which needs to find all capillaries with nFlies=0
 * across all cages to compute the average evaporation.
 * 
 * @author MultiSPOTS96
 * @version 2.3.3
 */
public class CagesArrayCapillariesComputation {
	
	private final CagesArray cagesArray;
	
	public CagesArrayCapillariesComputation(CagesArray cagesArray) {
		if (cagesArray == null) {
			throw new IllegalArgumentException("CagesArray cannot be null");
		}
		this.cagesArray = cagesArray;
	}
	
	/**
	 * Computes evaporation correction for all capillaries across all cages.
	 * For capillaries with capNFlies == 0, computes average evaporation separately for L and R sides.
	 * Subtracts the average evaporation from ptsTop to create ptsTopCorrected.
	 * 
	 * @param exp The experiment containing all capillaries
	 */
	public void computeEvaporationCorrection(Experiment exp) {
		if (exp == null || exp.getCapillaries() == null)
			return;

		// First, dispatch capillaries to cages to ensure they're organized
		exp.dispatchCapillariesToCages();

		// Collect all capillaries with zero flies for evaporation calculation
		List<Capillary> zeroFliesCapillariesL = new ArrayList<>();
		List<Capillary> zeroFliesCapillariesR = new ArrayList<>();

		for (Cage cage : cagesArray.getCageList()) {
			for (Capillary cap : cage.getCapillaries().getList()) {
				if (cap.capNFlies == 0 && cap.ptsTop != null && cap.ptsTop.polylineLevel != null && cap.ptsTop.polylineLevel.npoints > 0) {
					// Determine side from capSide or capillary name
					String side = getCapillarySide(cap);
					if (side.contains("L") || side.contains("1")) {
						zeroFliesCapillariesL.add(cap);
					} else if (side.contains("R") || side.contains("2")) {
						zeroFliesCapillariesR.add(cap);
					} else {
						// If side unclear, add to both (sameLR case)
						zeroFliesCapillariesL.add(cap);
						zeroFliesCapillariesR.add(cap);
					}
				}
			}
		}

		// Compute average evaporation for L and R sides
		plugins.fmp.multicafe.fmp_tools.Level2D avgEvapL = computeAverageMeasure(zeroFliesCapillariesL);
		plugins.fmp.multicafe.fmp_tools.Level2D avgEvapR = computeAverageMeasure(zeroFliesCapillariesR);

		// Apply evaporation correction to all capillaries
		for (Cage cage : cagesArray.getCageList()) {
			for (Capillary cap : cage.getCapillaries().getList()) {
				if (cap.ptsTop == null || cap.ptsTop.polylineLevel == null || cap.ptsTop.polylineLevel.npoints == 0)
					continue;

				String side = getCapillarySide(cap);
				plugins.fmp.multicafe.fmp_tools.Level2D avgEvap = null;
				if (side.contains("L") || side.contains("1")) {
					avgEvap = avgEvapL;
				} else if (side.contains("R") || side.contains("2")) {
					avgEvap = avgEvapR;
				} else {
					avgEvap = avgEvapL; // Default to L if side unclear
				}

				if (avgEvap != null) {
					// Create corrected measure by subtracting evaporation
					cap.ptsTopCorrected = subtractEvaporation(cap.ptsTop, avgEvap);
				}
			}
		}
	}

	/**
	 * Clears all computed measures from capillaries in all cages.
	 */
	public void clearComputedMeasures() {
		for (Cage cage : cagesArray.getCageList()) {
			for (Capillary cap : cage.getCapillaries().getList()) {
				cap.clearComputedMeasures();
			}
		}
	}

	// --------------------------------------------------------
	// Helper methods for capillary computation

	private String getCapillarySide(Capillary cap) {
		if (cap.capSide != null && !cap.capSide.equals("."))
			return cap.capSide;
		// Try to get from name
		String name = cap.getRoiName();
		if (name != null) {
			name = name.toUpperCase();
			if (name.contains("L") || name.contains("1"))
				return "L";
			if (name.contains("R") || name.contains("2"))
				return "R";
		}
		return "";
	}

	private plugins.fmp.multicafe.fmp_tools.Level2D computeAverageMeasure(
			List<Capillary> capillaries) {
		if (capillaries == null || capillaries.isEmpty())
			return null;

		// Find maximum dimension
		int maxPoints = 0;
		for (Capillary cap : capillaries) {
			if (cap.ptsTop != null && cap.ptsTop.polylineLevel != null) {
				int npoints = cap.ptsTop.polylineLevel.npoints;
				if (npoints > maxPoints)
					maxPoints = npoints;
			}
		}

		if (maxPoints == 0)
			return null;

		// Accumulate values
		double[] sumY = new double[maxPoints];
		int[] count = new int[maxPoints];
		for (int i = 0; i < maxPoints; i++) {
			sumY[i] = 0.0;
			count[i] = 0;
		}

		for (Capillary cap : capillaries) {
			if (cap.ptsTop == null || cap.ptsTop.polylineLevel == null || cap.ptsTop.polylineLevel.npoints == 0)
				continue;
			plugins.fmp.multicafe.fmp_tools.Level2D polyline = cap.ptsTop.polylineLevel;
			if (polyline == null)
				continue;

			int npoints = Math.min(polyline.npoints, maxPoints);
			for (int i = 0; i < npoints; i++) {
				sumY[i] += polyline.ypoints[i];
				count[i]++;
			}
		}

		// Average
		double[] avgY = new double[maxPoints];
		double[] xpoints = new double[maxPoints];
		for (int i = 0; i < maxPoints; i++) {
			xpoints[i] = i;
			if (count[i] > 0)
				avgY[i] = sumY[i] / count[i];
			else
				avgY[i] = 0.0;
		}

		return new plugins.fmp.multicafe.fmp_tools.Level2D(xpoints, avgY, maxPoints);
	}

	private CapillaryMeasure subtractEvaporation(
			CapillaryMeasure original,
			plugins.fmp.multicafe.fmp_tools.Level2D evaporation) {
		if (original == null || original.polylineLevel == null || original.polylineLevel.npoints == 0 || evaporation == null)
			return null;

		plugins.fmp.multicafe.fmp_tools.Level2D polyline = original.polylineLevel;
		if (polyline == null)
			return null;

		int npoints = Math.min(polyline.npoints, evaporation.npoints);
		double[] correctedY = new double[npoints];
		double[] xpoints = new double[npoints];

		for (int i = 0; i < npoints; i++) {
			xpoints[i] = i;
			correctedY[i] = polyline.ypoints[i] - evaporation.ypoints[i];
		}

		plugins.fmp.multicafe.fmp_tools.Level2D correctedPolyline = 
			new plugins.fmp.multicafe.fmp_tools.Level2D(xpoints, correctedY, npoints);

		CapillaryMeasure corrected = 
			new CapillaryMeasure(original.capName + "_corrected", -1, null);
		corrected.polylineLevel = correctedPolyline;

		return corrected;
	}
}


