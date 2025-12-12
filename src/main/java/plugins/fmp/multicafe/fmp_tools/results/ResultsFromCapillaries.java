package plugins.fmp.multicafe.fmp_tools.results;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import plugins.fmp.multicafe.fmp_experiment.Experiment;
import plugins.fmp.multicafe.fmp_experiment.cages.CageCapillariesComputation;
import plugins.fmp.multicafe.fmp_experiment.capillaries.Capillaries;
import plugins.fmp.multicafe.fmp_experiment.capillaries.Capillary;
import plugins.fmp.multicafe.fmp_experiment.capillaries.CapillaryMeasure;
import plugins.fmp.multicafe.fmp_experiment.sequence.ImageLoader;

public class ResultsFromCapillaries extends ResultsArray {
	/** Logger for this class */
	private static final Logger LOGGER = Logger.getLogger(ResultsFromCapillaries.class.getName());
	Results evapL = null;
	Results evapR = null;
	boolean sameLR = true;
	String stim = null;
	String conc = null;
	double lowestPiAllowed = -1.2;
	double highestPiAllowed = 1.2;

	public ResultsFromCapillaries(int size) {
		resultsList = new ArrayList<Results>(size);
	}

	/**
	 * Gets the results for a capillary.
	 * 
	 * @param exp            The experiment
	 * @param capillary      The capillary
	 * @param resultsOptions The export options
	 * @param subtractT0     Whether to subtract T0 value
	 * @return The XLS results
	 */
	static public Results getResultsFromCapillaryMeasures(Experiment exp, Capillary capillary,
			ResultsOptions resultsOptions, boolean subtractT0) {
		Results results = new Results(capillary.getRoiName(), capillary.capNFlies, capillary.getCageID(), 0,
				resultsOptions.resultType);

		results.setStimulus(capillary.capStimulus);
		results.setConcentration(capillary.capConcentration);

		// Get bin durations
		long binData = exp.getKymoBin_ms();
		long binExcel = resultsOptions.buildExcelStepMs;

		// Validate bin sizes to prevent division by zero
		if (binData <= 0) {
			binData = 60000; // Default to 60 seconds if invalid
		}
		if (binExcel <= 0) {
			binExcel = binData; // Default to binData if invalid
		}

		// For TOPLEVEL_LR, read from CageCapillariesComputation instead of capillary
		if (resultsOptions.resultType == EnumResults.TOPLEVEL_LR) {
			getLRDataFromCage(exp, capillary, results, binData, binExcel, subtractT0);
		} else {
			results.getDataFromCapillary(capillary, binData, binExcel, resultsOptions, subtractT0);
		}

		// Initialize valuesOut array with the actual size of dataValues
		if (results.getDataValues() != null && results.getDataValues().size() > 0) {
			int actualSize = results.getDataValues().size();
			results.initValuesOutArray(actualSize, Double.NaN);
		} else {
			// Fallback to calculated size if no data
			int nOutputFrames = getNOutputFrames(exp, resultsOptions);
			results.initValuesOutArray(nOutputFrames, Double.NaN);
		}

		return results;
	}

	/**
	 * Gets L+R data (SUM or PI) from CageCapillariesComputation for TOPLEVEL_LR
	 * export. For L capillaries: exports SUM measure For R capillaries: exports PI
	 * measure
	 * 
	 * @param exp        The experiment
	 * @param capillary  The capillary
	 * @param xlsResults The XLS results to populate
	 * @param binData    The bin duration for the data
	 * @param binExcel   The bin duration for Excel output
	 * @param subtractT0 Whether to subtract T0 value
	 */
	private static void getLRDataFromCage(Experiment exp, Capillary capillary, Results xlsResults, long binData,
			long binExcel, boolean subtractT0) {

		int cageID = capillary.getCageID();
		CageCapillariesComputation cageComp = exp.getCages().getCageComputation(cageID);

		if (cageComp == null) {
			// No computation available, fall back to raw
			ResultsOptions fallbackOptions = new ResultsOptions();
			fallbackOptions.resultType = EnumResults.TOPRAW;
			xlsResults.getDataFromCapillary(capillary, binData, binExcel, fallbackOptions, subtractT0);
			return;
		}

		// Determine which measure to use based on capillary side
		String side = getCapillarySide(capillary);
		CapillaryMeasure measure = null;

		if (side != null && (side.contains("L") || side.contains("1"))) {
			// L capillary: use SUM
			measure = cageComp.getSumMeasure();
		} else if (side != null && (side.contains("R") || side.contains("2"))) {
			// R capillary: use PI
			measure = cageComp.getPIMeasure();
		} else {
			// Side unclear, try first capillary as L, second as R
			List<Capillary> caps = exp.getCages().getCageList().stream().filter(c -> c.getCageID() == cageID)
					.findFirst().map(c -> c.getCapillaries().getList()).orElse(java.util.Collections.emptyList());

			if (!caps.isEmpty() && caps.get(0) == capillary) {
				measure = cageComp.getSumMeasure();
			} else if (caps.size() >= 2 && caps.get(1) == capillary) {
				measure = cageComp.getPIMeasure();
			}
		}

		if (measure != null && measure.polylineLevel != null && measure.polylineLevel.npoints > 0) {
			// Get measures by binning polyline data (similar to getMeasures implementation)
			if (binData <= 0 || binExcel <= 0) {
				// Invalid bin sizes, fall back to raw
				ResultsOptions fallbackOptions = new ResultsOptions();
				fallbackOptions.resultType = EnumResults.TOPRAW;
				xlsResults.getDataFromCapillary(capillary, binData, binExcel, fallbackOptions, subtractT0);
				return;
			}
			plugins.fmp.multicafe.fmp_tools.Level2D polyline = measure.polylineLevel;
			long maxMs = (polyline.npoints - 1) * binData;
			int nOutputFrames = (int) (maxMs / binExcel) + 1;

			java.util.ArrayList<Integer> intData = new ArrayList<>(nOutputFrames);
			for (int i = 0; i < nOutputFrames; i++) {
				long timeMs = i * binExcel;
				int index = (int) (timeMs / binData);
				if (index >= 0 && index < polyline.npoints) {
					intData.add((int) polyline.ypoints[index]);
				} else {
					intData.add(0);
				}
			}

			if (intData != null && !intData.isEmpty()) {
				// Convert Integer to Double
				java.util.ArrayList<Double> dataValues = new ArrayList<>(intData.size());
				int t0Value = 0;

				if (subtractT0 && intData.size() > 0) {
					t0Value = intData.get(0);
				}

				for (Integer intValue : intData) {
					if (subtractT0) {
						dataValues.add((double) (intValue - t0Value));
					} else {
						dataValues.add(intValue.doubleValue());
					}
				}

				xlsResults.setDataValues(dataValues);
				return;
			}
		}

		// Fallback to raw if computation failed
		ResultsOptions fallbackOptions = new ResultsOptions();
		fallbackOptions.resultType = EnumResults.TOPRAW;
		xlsResults.getDataFromCapillary(capillary, binData, binExcel, fallbackOptions, subtractT0);
	}

	/**
	 * Helper method to determine capillary side from capSide or name.
	 */
	private static String getCapillarySide(Capillary cap) {
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

	/**
	 * Gets the number of output frames for the experiment.
	 * 
	 * @param exp     The experiment
	 * @param options The export options
	 * @return The number of output frames
	 */
	protected static int getNOutputFrames(Experiment exp, ResultsOptions options) {
		// For capillaries, use kymograph timing
		long kymoFirst_ms = exp.getKymoFirst_ms();
		long kymoLast_ms = exp.getKymoLast_ms();
		long kymoBin_ms = exp.getKymoBin_ms();

		// If buildExcelStepMs equals kymoBin_ms, we want 1:1 mapping - use actual frame
		// count
		if (kymoBin_ms > 0 && options.buildExcelStepMs == kymoBin_ms && exp.getSeqKymos() != null) {
			ImageLoader imgLoader = exp.getSeqKymos().getImageLoader();
			if (imgLoader != null) {
				int nFrames = imgLoader.getNTotalFrames();
				if (nFrames > 0) {
					return nFrames;
				}
			}
		}

		if (kymoLast_ms <= kymoFirst_ms) {
			// Try to get from kymograph sequence
			if (exp.getSeqKymos() != null) {
				ImageLoader imgLoader = exp.getSeqKymos().getImageLoader();
				if (imgLoader != null) {
					if (kymoBin_ms > 0) {
						kymoLast_ms = kymoFirst_ms + imgLoader.getNTotalFrames() * kymoBin_ms;
						exp.setKymoLast_ms(kymoLast_ms);
					}
				}
			}
		}

		long durationMs = kymoLast_ms - kymoFirst_ms;
		int nOutputFrames = (int) (durationMs / options.buildExcelStepMs + 1);

		if (nOutputFrames <= 1) {
			handleExportError(exp, -1);
			// Fallback to a reasonable default
			nOutputFrames = 1000;
		}

		return nOutputFrames;
	}

	/**
	 * Handles export errors by logging them.
	 * 
	 * @param exp           The experiment
	 * @param nOutputFrames The number of output frames
	 */
	protected static void handleExportError(Experiment exp, int nOutputFrames) {
		String error = String.format(
				"XLSExport:ExportError() ERROR in %s\n nOutputFrames=%d kymoFirstCol_Ms=%d kymoLastCol_Ms=%d",
				exp.getExperimentDirectory(), nOutputFrames, exp.getKymoFirst_ms(), exp.getKymoLast_ms());
		System.err.println(error);
	}

	public ResultsArray getMeasuresFromAllCapillaries(Experiment exp, EnumResults resultType,
			boolean correctEvaporation) {
		// Dispatch capillaries to cages first
		exp.dispatchCapillariesToCages();

		// Compute evaporation correction if needed (for TOPLEVEL exports)
		if (correctEvaporation && resultType == EnumResults.TOPLEVEL) {
			exp.getCages().computeEvaporationCorrection(exp);
		}

		// Compute L+R measures if needed (must be done after evaporation correction)
		if (resultType == EnumResults.TOPLEVEL_LR) {
			if (correctEvaporation) {
				exp.getCages().computeEvaporationCorrection(exp);
			}
			exp.getCages().computeLRMeasures(exp, 0.0); // Use default threshold of 0.0 for chart/display
		}

		ResultsArray resultsArray = new ResultsArray();
		double scalingFactorToPhysicalUnits = exp.getCapillaries().getScalingFactorToPhysicalUnits(resultType);

		ResultsOptions options = new ResultsOptions();
		long kymoBin_ms = exp.getKymoBin_ms();
		if (kymoBin_ms <= 0) {
			kymoBin_ms = 60000;
		}
		options.buildExcelStepMs = (int) kymoBin_ms;
		options.relativeToT0 = false;
		options.correctEvaporation = correctEvaporation;

		List<Capillary> capillaries = exp.getCapillaries().getList();
		if (capillaries == null) {
			LOGGER.warning("Capillaries list is null");
			return resultsArray;
		}

		for (Capillary capillary : capillaries) {
			if (capillary == null) {
				continue;
			}

			ResultsOptions resultsOptions = new ResultsOptions();
			resultsOptions.buildExcelStepMs = options.buildExcelStepMs;
			resultsOptions.relativeToT0 = options.relativeToT0;
			resultsOptions.correctEvaporation = options.correctEvaporation;
			resultsOptions.resultType = resultType;

			try {
				Results xlsResults = getResultsFromCapillaryMeasures(exp, capillary, resultsOptions, false);
				if (xlsResults != null) {
					xlsResults.transferDataValuesToValuesOut(scalingFactorToPhysicalUnits, resultType);
					resultsArray.addRow(xlsResults);
				}
			} catch (Exception e) {
				LOGGER.warning("Error processing capillary: " + e.getMessage());
			}
		}

		// Note: Evaporation compensation is now handled by
		// CagesArray.computeEvaporationCorrection()
		// which should be called before this method. The old compensateEvaporation()
		// method
		// has been deprecated in favor of the new computation-based approach.

		return resultsArray;
	}

	public void compensateEvaporation(ResultsArray resultsArray) {
		int dimension = 0;
		for (Results result : resultsArray.getList()) {
			if (result.valuesOut == null)
				continue;
			if (result.valuesOut.length > dimension)
				dimension = result.valuesOut.length;
		}
		if (dimension == 0)
			return;

		computeEvaporationFromResultsWithZeroFlies(resultsArray, dimension);
		subtractEvaporationLocal(resultsArray);
	}

	private void computeEvaporationFromResultsWithZeroFlies(ResultsArray resultsArray, int dimension) {
		evapL = new Results("L", 0, 0, null);
		evapR = new Results("R", 0, 0, null);
		evapL.initValuesOutArray(dimension, 0.);
		evapR.initValuesOutArray(dimension, 0.);

		for (Results result : resultsArray.getList()) {
			if (result.valuesOut == null || result.getNflies() != 0)
				continue;
			String side = result.getName().substring(result.getName().length() - 1);
			if (sameLR || side.contains("L"))
				evapL.addDataToValOutEvap(result);
			else
				evapR.addDataToValOutEvap(result);
		}
		evapL.averageEvaporation();
		evapR.averageEvaporation();
	}

	private void subtractEvaporationLocal(ResultsArray resultsArray) {
		for (Results result : resultsArray.getList()) {
			String side = result.getName().substring(result.getName().length() - 1);
			if (sameLR || side.contains("L"))
				result.subtractEvap(evapL);
			else
				result.subtractEvap(evapR);
		}
	}

	// ---------------------------------

	public void subtractEvaporation() {
		int dimension = 0;
		for (Results result : resultsList) {
			if (result.valuesOut == null)
				continue;
			if (result.valuesOut.length > dimension)
				dimension = result.valuesOut.length;
		}
		if (dimension == 0)
			return;

		computeEvaporationFromResultsWithZeroFlies(dimension);
		subtractEvaporationLocal();
	}

	private void computeEvaporationFromResultsWithZeroFlies(int dimension) {
		evapL = new Results("L", 0, 0, null);
		evapR = new Results("R", 0, 0, null);
		evapL.initValuesOutArray(dimension, 0.);
		evapR.initValuesOutArray(dimension, 0.);

		for (Results result : resultsList) {
			if (result.valuesOut == null || result.getNflies() != 0)
				continue;
			String side = result.getName().substring(result.getName().length() - 1);
			if (sameLR || side.contains("L"))
				evapL.addDataToValOutEvap(result);
			else
				evapR.addDataToValOutEvap(result);
		}
		evapL.averageEvaporation();
		evapR.averageEvaporation();
	}

	private void subtractEvaporationLocal() {
		for (Results result : resultsList) {
			String side = result.getName().substring(result.getName().length() - 1);
			if (sameLR || side.contains("L"))
				result.subtractEvap(evapL);
			else
				result.subtractEvap(evapR);
		}
	}

	private int getLen(Results rowL, Results rowR) {
		int lenL = rowL.valuesOut.length;
		int lenR = rowR.valuesOut.length;
		return Math.min(lenL, lenR);
	}

	public void getPI_and_SUM_from_LR(Results rowL, Results rowR, double threshold) {
		int len = getLen(rowL, rowR);
		for (int index = 0; index < len; index++) {
			double dataL = rowL.valuesOut[index];
			double dataR = rowR.valuesOut[index];

			double pi = 0.;
			double sum = Math.abs(dataL) + Math.abs(dataR);
			if (sum != 0. && sum >= threshold) {
				pi = (dataL - dataR) / sum;
			}
			rowL.valuesOut[index] = sum;
			rowR.valuesOut[index] = pi;
		}
	}

	void getMinTimeToGulpLR(Results rowL, Results rowR, Results rowOut) {
		int len = getLen(rowL, rowR);
		for (int index = 0; index < len; index++) {
			double dataMax = Double.NaN;
			double dataL = rowL.valuesOut[index];
			double dataR = rowR.valuesOut[index];
			if (dataL <= dataR)
				dataMax = dataL;
			else if (dataL > dataR)
				dataMax = dataR;
			rowOut.valuesOut[index] = dataMax;
		}
	}

	void getMaxTimeToGulpLR(Results rowL, Results rowR, Results rowOut) {
		int len = getLen(rowL, rowR);
		for (int index = 0; index < len; index++) {
			double dataMin = Double.NaN;
			double dataL = rowL.valuesOut[index];
			double dataR = rowR.valuesOut[index];
			if (dataL >= dataR)
				dataMin = dataL;
			else if (dataL < dataR)
				dataMin = dataR;
			rowOut.valuesOut[index] = dataMin;
		}
	}

	// ---------------------------------------------------

	public void getResults1(Experiment expi, EnumResults resultType, int nOutputFrames, long kymoBinCol_Ms,
			ResultsOptions resultsOptions) {
		resultsOptions.resultType = resultType;
		buildDataForPass1(expi, nOutputFrames, kymoBinCol_Ms, resultsOptions, false);
		if (resultsOptions.compensateEvaporation)
			subtractEvaporation();
		buildDataForPass2(resultsOptions);
	}

	public void getResults_T0(Experiment expi, EnumResults resultType, int nOutputFrames, long kymoBinCol_Ms,
			ResultsOptions resultsOptions) {
		resultsOptions.resultType = resultType;
		buildDataForPass1(expi, nOutputFrames, kymoBinCol_Ms, resultsOptions, resultsOptions.subtractT0);
		if (resultsOptions.compensateEvaporation)
			subtractEvaporation();
		buildDataForPass2(resultsOptions);
	}

	private void buildDataForPass1(Experiment expi, int nOutputFrames, long kymoBinCol_Ms,
			ResultsOptions resultsOptions, boolean subtractT0) {
		Capillaries caps = expi.getCapillaries();
		double scalingFactorToPhysicalUnits = caps.getScalingFactorToPhysicalUnits(resultsOptions.resultType);
		for (Capillary cap : caps.getList()) {
			checkIfSameStimulusAndConcentration(cap);
			Results results = new Results(cap.getRoiName(), cap.capNFlies, cap.getCageID(), resultsOptions.resultType);
			results.initValuesOutArray(nOutputFrames, null);
			results.dataInt = cap.getCapillaryMeasuresForXLSPass1(resultsOptions.resultType, kymoBinCol_Ms,
					resultsOptions.buildExcelStepMs);
			if (subtractT0)
				results.subtractT0();
			results.transferDataIntToValuesOut(scalingFactorToPhysicalUnits, resultsOptions.resultType);
			addRow(results);
		}
	}

	public void checkIfSameStimulusAndConcentration(Capillary cap) {
		if (!sameLR)
			return;
		if (stim == null)
			stim = cap.capStimulus;
		if (conc == null)
			conc = cap.capConcentration;
		sameLR &= stim.equals(cap.capStimulus);
		sameLR &= conc.equals(cap.capConcentration);
	}

	public void buildDataForPass2(ResultsOptions resultsOptions) {
		switch (resultsOptions.resultType) {
		case TOPLEVEL_LR:
		case TOPLEVELDELTA_LR:
		case SUMGULPS_LR:
			buildLR(resultsOptions.lrPIThreshold);
			break;
		case AUTOCORREL:
			buildAutocorrel(resultsOptions);
			break;
		case AUTOCORREL_LR:
			buildAutocorrelLR(resultsOptions);
			break;
		case CROSSCORREL:
			buildCrosscorrel(resultsOptions);
			break;
		case CROSSCORREL_LR:
			buildCrosscorrelLR(resultsOptions);
			break;
		case MARKOV_CHAIN:
			buildMarkovChain(resultsOptions);
			break;
		default:
			break;
		}
	}

	private void buildLR(double threshold) {
		for (int irow = 0; irow < resultsList.size(); irow++) {
			Results rowL = getRow(irow);
			Results rowR = getNextRowIfSameCage(irow);
			if (rowR != null && rowL != null) {
				irow++;
				getPI_and_SUM_from_LR(rowL, rowR, threshold);
			}
		}
	}

	private void buildAutocorrel(ResultsOptions resultsOptions) {
		for (int irow = 0; irow < resultsList.size(); irow++) {
			Results rowL = getRow(irow);
			correl(rowL, rowL, rowL, resultsOptions.nBinsCorrelation);
		}
	}

	private void buildCrosscorrel(ResultsOptions resultsOptions) {
		for (int irow = 0; irow < resultsList.size(); irow++) {
			Results rowL = getRow(irow);
			Results rowR = getNextRowIfSameCage(irow);
			if (rowR != null) {
				irow++;
				Results rowLtoR = new Results("corrLtoR", 0, 0, null);
				rowLtoR.initValuesOutArray(rowL.dimension, 0.);
				correl(rowL, rowR, rowLtoR, resultsOptions.nBinsCorrelation);

				Results rowRtoL = new Results("corrRtoL", 0, 0, null);
				rowRtoL.initValuesOutArray(rowL.dimension, 0.);
				correl(rowR, rowL, rowRtoL, resultsOptions.nBinsCorrelation);

				rowL.copyValuesOut(rowLtoR);
				rowR.copyValuesOut(rowRtoL);
			}
		}
	}

	private void buildCrosscorrelLR(ResultsOptions resultsOptions) {
		for (int irow = 0; irow < resultsList.size(); irow++) {
			Results rowL = getRow(irow);
			Results rowR = getNextRowIfSameCage(irow);
			if (rowR != null) {
				irow++;

				Results rowLR = new Results("corrLR", 0, 0, null);
				rowLR.initValuesOutArray(rowL.dimension, 0.);
				combineIntervals(rowL, rowR, rowLR);

				correl(rowL, rowLR, rowL, resultsOptions.nBinsCorrelation);
				correl(rowR, rowLR, rowR, resultsOptions.nBinsCorrelation);
			}
		}
	}

	private void correl(Results row1, Results row2, Results rowOut, int nbins) {
		double[] sumBins = new double[2 * nbins + 1];
		Arrays.fill(sumBins, 0);
		double nitems = 0;
		for (int i1 = 0; i1 < row1.valuesOut.length; i1++) {
			if (row1.valuesOut[i1] == 0.)
				continue;
			nitems++;
			for (int i2 = 0; i2 < row2.valuesOut.length; i2++) {
				int ibin = i2 - i1;
				if (ibin < -nbins || ibin > nbins)
					continue;
				if (row2.valuesOut[i2] != 0.) {
					sumBins[ibin + nbins]++;
				}
			}
		}

		Arrays.fill(rowOut.valuesOut, Double.NaN);
		for (int i = 0; i < 2 * nbins; i++)
			rowOut.valuesOut[i] = sumBins[i] / nitems;
	}

	private void combineIntervals(Results row1, Results row2, Results rowOut) {
		for (int i = 0; i < rowOut.valuesOut.length; i++) {
			if ((row2.valuesOut[i] + row1.valuesOut[i]) > 0.)
				rowOut.valuesOut[i] = 1.;
		}
	}

	private void buildAutocorrelLR(ResultsOptions resultsOptions) {
		for (int irow = 0; irow < resultsList.size(); irow++) {
			Results rowL = getRow(irow);
			Results rowR = getNextRowIfSameCage(irow);
			if (rowR != null) {
				irow++;

				Results rowLR = new Results("LR", 0, 0, null);
				rowLR.initValuesOutArray(rowL.dimension, 0.);
				combineIntervals(rowL, rowR, rowLR);

				correl(rowLR, rowLR, rowL, resultsOptions.nBinsCorrelation);
				correl(rowLR, rowLR, rowR, resultsOptions.nBinsCorrelation);
			}
		}
	}

	private void buildMarkovChain(ResultsOptions resultsOptions) {
		ArrayList<Results> newResultsList = new ArrayList<Results>();
		Map<Integer, List<Results>> cagesMap = groupResultsByCage();

		for (Map.Entry<Integer, List<Results>> entry : cagesMap.entrySet()) {
			int cageID = entry.getKey();
			List<Results> cageResults = entry.getValue();
			List<Results> cageMarkovResults = processCageMarkovChain(cageID, cageResults, resultsOptions);
			if (cageMarkovResults != null)
				newResultsList.addAll(cageMarkovResults);
		}

		// Replace resultsList with new results
		resultsList.clear();
		resultsList.addAll(newResultsList);
	}

	private Map<Integer, List<Results>> groupResultsByCage() {
		Map<Integer, List<Results>> cagesMap = new HashMap<>();
		for (Results result : resultsList) {
			if (result.valuesOut == null)
				continue;
			int cageID = result.getCageID();
			if (!cagesMap.containsKey(cageID)) {
				cagesMap.put(cageID, new ArrayList<>());
			}
			cagesMap.get(cageID).add(result);
		}
		return cagesMap;
	}

	private List<Results> processCageMarkovChain(int cageID, List<Results> cageResults, ResultsOptions resultsOptions) {
		Results rowL = getResultSide(cageResults, "L");
		Results rowR = getResultSide(cageResults, "R");

		if (rowL == null || rowR == null || rowL.valuesOut == null || rowR.valuesOut == null)
			return null;

		int dimension = Math.min(rowL.valuesOut.length, rowR.valuesOut.length);
		if (dimension == 0)
			return null;

		int[] states = computeStates(rowL, rowR, dimension);

		List<Results> results = new ArrayList<>();
		results.addAll(createStateRows(cageID, rowL, states, dimension, resultsOptions));
		results.addAll(createTransitionRows(cageID, rowL, states, dimension, resultsOptions));

		return results;
	}

	private Results getResultSide(List<Results> cageResults, String side) {
		for (Results result : cageResults) {
			String name = result.getName();
			if (name != null && name.endsWith(side)) {
				return result;
			}
		}
		return null;
	}

	private int[] computeStates(Results rowL, Results rowR, int dimension) {
		int[] states = new int[dimension];
		for (int t = 0; t < dimension; t++) {
			int gulpL = (rowL.valuesOut[t] > 0) ? 1 : 0;
			int gulpR = (rowR.valuesOut[t] > 0) ? 1 : 0;
			if (gulpL == 1 && gulpR == 0)
				states[t] = 0; // STATE_Ls
			else if (gulpL == 0 && gulpR == 1)
				states[t] = 1; // STATE_Rs
			else if (gulpL == 1 && gulpR == 1)
				states[t] = 2; // STATE_LR
			else
				states[t] = 3; // STATE_N
		}
		return states;
	}

	private List<Results> createStateRows(int cageID, Results rowL, int[] states, int dimension,
			ResultsOptions resultsOptions) {
		List<Results> rows = new ArrayList<>();
		String[] stateNames = { "Ls", "Rs", "LR", "N" };

		for (int s = 0; s < 4; s++) {
			Results stateRow = new Results("cage" + cageID + "_" + stateNames[s], rowL.getNflies(), cageID,
					resultsOptions.resultType);
			stateRow.initValuesOutArray(dimension, null);
			stateRow.setStimulus(rowL.getStimulus());
			stateRow.setConcentration(rowL.getConcentration());
			stateRow.initValuesOutArray(dimension, 0.);
			for (int t = 0; t < dimension; t++) {
				stateRow.valuesOut[t] = (states[t] == s) ? 1. : 0.;
			}
			rows.add(stateRow);
		}
		return rows;
	}

	private List<Results> createTransitionRows(int cageID, Results rowL, int[] states, int dimension,
			ResultsOptions resultsOptions) {
		List<Results> rows = new ArrayList<>();
		String[] transitionNames = { "Ls-Ls", "Rs-Ls", "LR-Ls", "N-Ls", "Ls-Rs", "Rs-Rs", "LR-Rs", "N-Rs", "Ls-LR",
				"Rs-LR", "LR-LR", "N-LR", "Ls-N", "Rs-N", "LR-N", "N-N" };

		int transitionIndex = 0;
		for (int fromState = 0; fromState < 4; fromState++) {
			for (int toState = 0; toState < 4; toState++) {
				Results transRow = new Results("cage" + cageID + "_" + transitionNames[transitionIndex],
						rowL.getNflies(), cageID, resultsOptions.resultType);
				transRow.initValuesOutArray(dimension, null);
				transRow.setStimulus(rowL.getStimulus());
				transRow.setConcentration(rowL.getConcentration());
				transRow.initValuesOutArray(dimension, 0.);
				// Count transitions at each time point
				for (int t = 1; t < dimension; t++) {
					if (states[t - 1] == fromState && states[t] == toState) {
						transRow.valuesOut[t] = 1.;
					}
				}
				rows.add(transRow);
				transitionIndex++;
			}
		}
		return rows;
	}

	public Results getNextRowIfSameCage(int irow) {
		Results rowL = resultsList.get(irow);
		int cellL = getCageFromKymoFileName(rowL.getName());
		Results rowR = null;
		if (irow + 1 < resultsList.size()) {
			rowR = resultsList.get(irow + 1);
			int cellR = getCageFromKymoFileName(rowR.getName());
			if (cellR != cellL)
				rowR = null;
		}
		return rowR;
	}

	protected int getCageFromKymoFileName(String name) {
		if (!name.contains("line"))
			return -1;
		return Integer.valueOf(name.substring(4, 5));
	}

}
