package plugins.fmp.multicafe.fmp_tools.toExcel.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import plugins.fmp.multicafe.fmp_experiment.Experiment;
import plugins.fmp.multicafe.fmp_experiment.capillaries.Capillaries;
import plugins.fmp.multicafe.fmp_experiment.capillaries.Capillary;
import plugins.fmp.multicafe.fmp_tools.toExcel.config.XLSExportOptions;
import plugins.fmp.multicafe.fmp_tools.toExcel.enums.EnumXLSExport;

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

	public ResultsArray getMeasuresFromAllCapillaries(Experiment exp, EnumXLSExport exportType,
			boolean correctEvaporation) {
		// Dispatch capillaries to cages first
		exp.dispatchCapillariesToCages();
		
		// Compute evaporation correction if needed (for TOPLEVEL exports)
		if (correctEvaporation && exportType == EnumXLSExport.TOPLEVEL) {
			exp.getCages().computeEvaporationCorrection(exp);
		}
		
		// Compute L+R measures if needed (must be done after evaporation correction)
		if (exportType == EnumXLSExport.TOPLEVEL_LR) {
			if (correctEvaporation) {
				exp.getCages().computeEvaporationCorrection(exp);
			}
			exp.getCages().computeLRMeasures(exp, 0.0); // Use default threshold of 0.0 for chart/display
		}
		
		ResultsArray resultsArray = new ResultsArray();
		double scalingFactorToPhysicalUnits = exp.getCapillaries().getScalingFactorToPhysicalUnits(exportType);
		XLSExportMeasuresFromCapillary xlsExport = new XLSExportMeasuresFromCapillary();

		XLSExportOptions options = new XLSExportOptions();
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

			XLSExportOptions capOptions = new XLSExportOptions();
			capOptions.buildExcelStepMs = options.buildExcelStepMs;
			capOptions.relativeToT0 = options.relativeToT0;
			capOptions.correctEvaporation = options.correctEvaporation;
			capOptions.exportType = exportType;

			try {
				Results xlsResults = xlsExport.getXLSResultsDataValuesFromCapillaryMeasures(exp, capillary,
						capOptions, false);
				if (xlsResults != null) {
					xlsResults.transferDataValuesToValuesOut(scalingFactorToPhysicalUnits, exportType);
					resultsArray.addRow(xlsResults);
				}
			} catch (Exception e) {
				LOGGER.warning("Error processing capillary: " + e.getMessage());
			}
		}

		// Note: Evaporation compensation is now handled by CagesArray.computeEvaporationCorrection()
		// which should be called before this method. The old compensateEvaporation() method
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

	public void getResults1(Experiment expi, EnumXLSExport exportType, int nOutputFrames, long kymoBinCol_Ms,
			XLSExportOptions xlsExportOptions) {
		xlsExportOptions.exportType = exportType;
		buildDataForPass1(expi, nOutputFrames, kymoBinCol_Ms, xlsExportOptions, false);
		if (xlsExportOptions.compensateEvaporation)
			subtractEvaporation();
		buildDataForPass2(xlsExportOptions);
	}

	public void getResults_T0(Experiment expi, EnumXLSExport exportType, int nOutputFrames, long kymoBinCol_Ms,
			XLSExportOptions xlsExportOptions) {
		xlsExportOptions.exportType = exportType;
		buildDataForPass1(expi, nOutputFrames, kymoBinCol_Ms, xlsExportOptions, xlsExportOptions.subtractT0);
		if (xlsExportOptions.compensateEvaporation)
			subtractEvaporation();
		buildDataForPass2(xlsExportOptions);
	}

	private void buildDataForPass1(Experiment expi, int nOutputFrames, long kymoBinCol_Ms,
			XLSExportOptions xlsExportOptions, boolean subtractT0) {
		Capillaries caps = expi.getCapillaries();
		double scalingFactorToPhysicalUnits = caps.getScalingFactorToPhysicalUnits(xlsExportOptions.exportType);
		for (Capillary cap : caps.getList()) {
			checkIfSameStimulusAndConcentration(cap);
			Results results = new Results(cap.getRoiName(), cap.capNFlies, cap.getCageID(),
					xlsExportOptions.exportType);
			results.initValuesOutArray(nOutputFrames, null);
			results.dataInt = cap.getCapillaryMeasuresForXLSPass1(xlsExportOptions.exportType, kymoBinCol_Ms,
					xlsExportOptions.buildExcelStepMs);
			if (subtractT0)
				results.subtractT0();
			results.transferDataIntToValuesOut(scalingFactorToPhysicalUnits, xlsExportOptions.exportType);
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

	public void buildDataForPass2(XLSExportOptions xlsExportOptions) {
		switch (xlsExportOptions.exportType) {
		case TOPLEVEL_LR:
		case TOPLEVELDELTA_LR:
		case SUMGULPS_LR:
			buildLR(xlsExportOptions.lrPIThreshold);
			break;
		case AUTOCORREL:
			buildAutocorrel(xlsExportOptions);
			break;
		case AUTOCORREL_LR:
			buildAutocorrelLR(xlsExportOptions);
			break;
		case CROSSCORREL:
			buildCrosscorrel(xlsExportOptions);
			break;
		case CROSSCORREL_LR:
			buildCrosscorrelLR(xlsExportOptions);
			break;
		case MARKOV_CHAIN:
			buildMarkovChain(xlsExportOptions);
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

	private void buildAutocorrel(XLSExportOptions xlsExportOptions) {
		for (int irow = 0; irow < resultsList.size(); irow++) {
			Results rowL = getRow(irow);
			correl(rowL, rowL, rowL, xlsExportOptions.nBinsCorrelation);
		}
	}

	private void buildCrosscorrel(XLSExportOptions xlsExportOptions) {
		for (int irow = 0; irow < resultsList.size(); irow++) {
			Results rowL = getRow(irow);
			Results rowR = getNextRowIfSameCage(irow);
			if (rowR != null) {
				irow++;
				Results rowLtoR = new Results("corrLtoR", 0, 0, null);
				rowLtoR.initValuesOutArray(rowL.dimension, 0.);
				correl(rowL, rowR, rowLtoR, xlsExportOptions.nBinsCorrelation);

				Results rowRtoL = new Results("corrRtoL", 0, 0, null);
				rowRtoL.initValuesOutArray(rowL.dimension, 0.);
				correl(rowR, rowL, rowRtoL, xlsExportOptions.nBinsCorrelation);

				rowL.copyValuesOut(rowLtoR);
				rowR.copyValuesOut(rowRtoL);
			}
		}
	}

	private void buildCrosscorrelLR(XLSExportOptions xlsExportOptions) {
		for (int irow = 0; irow < resultsList.size(); irow++) {
			Results rowL = getRow(irow);
			Results rowR = getNextRowIfSameCage(irow);
			if (rowR != null) {
				irow++;

				Results rowLR = new Results("corrLR", 0, 0, null);
				rowLR.initValuesOutArray(rowL.dimension, 0.);
				combineIntervals(rowL, rowR, rowLR);

				correl(rowL, rowLR, rowL, xlsExportOptions.nBinsCorrelation);
				correl(rowR, rowLR, rowR, xlsExportOptions.nBinsCorrelation);
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

	private void buildAutocorrelLR(XLSExportOptions xlsExportOptions) {
		for (int irow = 0; irow < resultsList.size(); irow++) {
			Results rowL = getRow(irow);
			Results rowR = getNextRowIfSameCage(irow);
			if (rowR != null) {
				irow++;

				Results rowLR = new Results("LR", 0, 0, null);
				rowLR.initValuesOutArray(rowL.dimension, 0.);
				combineIntervals(rowL, rowR, rowLR);

				correl(rowLR, rowLR, rowL, xlsExportOptions.nBinsCorrelation);
				correl(rowLR, rowLR, rowR, xlsExportOptions.nBinsCorrelation);
			}
		}
	}

	private void buildMarkovChain(XLSExportOptions xlsExportOptions) {
		ArrayList<Results> newResultsList = new ArrayList<Results>();
		Map<Integer, List<Results>> cagesMap = groupResultsByCage();

		for (Map.Entry<Integer, List<Results>> entry : cagesMap.entrySet()) {
			int cageID = entry.getKey();
			List<Results> cageResults = entry.getValue();
			List<Results> cageMarkovResults = processCageMarkovChain(cageID, cageResults, xlsExportOptions);
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

	private List<Results> processCageMarkovChain(int cageID, List<Results> cageResults,
			XLSExportOptions xlsExportOptions) {
		Results rowL = getResultSide(cageResults, "L");
		Results rowR = getResultSide(cageResults, "R");

		if (rowL == null || rowR == null || rowL.valuesOut == null || rowR.valuesOut == null)
			return null;

		int dimension = Math.min(rowL.valuesOut.length, rowR.valuesOut.length);
		if (dimension == 0)
			return null;

		int[] states = computeStates(rowL, rowR, dimension);

		List<Results> results = new ArrayList<>();
		results.addAll(createStateRows(cageID, rowL, states, dimension, xlsExportOptions));
		results.addAll(createTransitionRows(cageID, rowL, states, dimension, xlsExportOptions));

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
			XLSExportOptions xlsExportOptions) {
		List<Results> rows = new ArrayList<>();
		String[] stateNames = { "Ls", "Rs", "LR", "N" };

		for (int s = 0; s < 4; s++) {
			Results stateRow = new Results("cage" + cageID + "_" + stateNames[s], rowL.getNflies(), cageID,
					xlsExportOptions.exportType);
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
			XLSExportOptions xlsExportOptions) {
		List<Results> rows = new ArrayList<>();
		String[] transitionNames = { "Ls-Ls", "Rs-Ls", "LR-Ls", "N-Ls", "Ls-Rs", "Rs-Rs", "LR-Rs", "N-Rs", "Ls-LR",
				"Rs-LR", "LR-LR", "N-LR", "Ls-N", "Rs-N", "LR-N", "N-N" };

		int transitionIndex = 0;
		for (int fromState = 0; fromState < 4; fromState++) {
			for (int toState = 0; toState < 4; toState++) {
				Results transRow = new Results("cage" + cageID + "_" + transitionNames[transitionIndex],
						rowL.getNflies(), cageID, xlsExportOptions.exportType);
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
