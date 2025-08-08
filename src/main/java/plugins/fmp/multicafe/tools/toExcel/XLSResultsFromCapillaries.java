package plugins.fmp.multicafe.tools.toExcel;

import java.util.ArrayList;
import java.util.Arrays;

import plugins.fmp.multicafe.experiment.Experiment;
import plugins.fmp.multicafe.experiment.capillaries.Capillaries;
import plugins.fmp.multicafe.experiment.capillaries.Capillary;

public class XLSResultsFromCapillaries extends XLSResultsArray {

	XLSResults evapL = null;
	XLSResults evapR = null;
	boolean sameLR = true;
	String stim = null;
	String conc = null;
	double lowestPiAllowed = -1.2;
	double highestPiAllowed = 1.2;

	public XLSResultsFromCapillaries(int size) {
		resultsList = new ArrayList<XLSResults>(size);
	}

	public XLSResults getNextRowIfSameCage(int irow) {
		XLSResults rowL = resultsList.get(irow);
		int cellL = getCageFromKymoFileName(rowL.name);
		XLSResults rowR = null;
		if (irow + 1 < resultsList.size()) {
			rowR = resultsList.get(irow + 1);
			int cellR = getCageFromKymoFileName(rowR.name);
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

	public void subtractEvaporation() {
		int dimension = 0;
		for (XLSResults result : resultsList) {
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
		evapL = new XLSResults("L", 0, 0, null);
		evapR = new XLSResults("R", 0, 0, null);
		evapL.initValuesOutArray(dimension, 0.);
		evapR.initValuesOutArray(dimension, 0.);

		for (XLSResults result : resultsList) {
			if (result.valuesOut == null || result.nflies != 0)
				continue;
			String side = result.name.substring(result.name.length() - 1);
			if (sameLR || side.contains("L"))
				evapL.addDataToValOutEvap(result);
			else
				evapR.addDataToValOutEvap(result);
		}
		evapL.averageEvaporation();
		evapR.averageEvaporation();
	}

	private void subtractEvaporationLocal() {
		for (XLSResults result : resultsList) {
			String side = result.name.substring(result.name.length() - 1);
			if (sameLR || side.contains("L"))
				result.subtractEvap(evapL);
			else
				result.subtractEvap(evapR);
		}
	}

	private int getLen(XLSResults rowL, XLSResults rowR) {
		int lenL = rowL.valuesOut.length;
		int lenR = rowR.valuesOut.length;
		return Math.min(lenL, lenR);
	}

	public void getPI_and_SUM_from_LR(XLSResults rowL, XLSResults rowR, double threshold) {
		int len = getLen(rowL, rowR);
		for (int index = 0; index < len; index++) {
			double dataL = rowL.valuesOut[index];
			double dataR = rowR.valuesOut[index];

			double pi = 0.;
			double sum = Math.abs(dataL) + Math.abs(dataR);
			if (sum != 0. && !Double.isNaN(sum) && sum >= threshold)
				pi = (dataL - dataR) / sum;

			rowL.valuesOut[index] = sum;
			rowR.valuesOut[index] = pi;
		}
	}

	void getMinTimeToGulpLR(XLSResults rowL, XLSResults rowR, XLSResults rowOut) {
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

	void getMaxTimeToGulpLR(XLSResults rowL, XLSResults rowR, XLSResults rowOut) {
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
		buildDataForPass1(expi, nOutputFrames, kymoBinCol_Ms, xlsExportOptions, xlsExportOptions.t0);
		if (xlsExportOptions.compensateEvaporation)
			subtractEvaporation();
		buildDataForPass2(xlsExportOptions);
	}

	private void buildDataForPass1(Experiment expi, int nOutputFrames, long kymoBinCol_Ms,
			XLSExportOptions xlsExportOptions, boolean subtractT0) {
		Capillaries caps = expi.capillaries;
		double scalingFactorToPhysicalUnits = caps.getScalingFactorToPhysicalUnits(xlsExportOptions.exportType);
		for (Capillary cap : caps.capillariesList) {
			checkIfSameStimulusAndConcentration(cap);
			XLSResults results = new XLSResults(cap.getRoiName(), cap.capNFlies, cap.capCageID,
					xlsExportOptions.exportType, nOutputFrames);
			results.dataInt = cap.getCapillaryMeasuresForXLSPass1(xlsExportOptions.exportType, kymoBinCol_Ms,
					xlsExportOptions.buildExcelStepMs);
			if (subtractT0)
				results.subtractT0();
			results.transferDataIntToValuesOut(scalingFactorToPhysicalUnits, xlsExportOptions.exportType);
			addRow(results);
		}
	}

//	private void buildDataForPass1_using_Cages(Experiment expi, int nOutputFrames, long kymoBinCol_Ms,
//			XLSExportOptions xlsExportOptions, boolean subtractT0) {
//		Capillaries caps = expi.capillaries;
//		double scalingFactorToPhysicalUnits = caps.getScalingFactorToPhysicalUnits(xlsExportOptions.exportType);
//		expi.dispatchCapillariesToCages();
//
//		for (Cage cage : expi.cages.cageList) {
//
//			ArrayList<Capillary> capList = cage.getCapillaryList();
//			// search lowest and compensate if one is negative
//			// TODOTODOTODU
//			for (Capillary cap : caps.capillariesList) {
//				checkIfSameStimulusAndConcentration(cap);
//				XLSResults results = new XLSResults(cap.getRoiName(), cap.capNFlies, cap.capCageID,
//						xlsExportOptions.exportType, nOutputFrames);
//				results.dataInt = cap.getCapillaryMeasuresForXLSPass1(xlsExportOptions.exportType, kymoBinCol_Ms,
//						xlsExportOptions.buildExcelStepMs);
//				if (subtractT0)
//					results.subtractT0();
//				results.transferDataIntToValuesOut(scalingFactorToPhysicalUnits, xlsExportOptions.exportType);
//				addRow(results);
//			}
//		}
//	}

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
		default:
			break;
		}
	}

	private void buildLR(double threshold) {
		for (int irow = 0; irow < resultsList.size(); irow++) {
			XLSResults rowL = getRow(irow);
			XLSResults rowR = getNextRowIfSameCage(irow);
			if (rowR != null && rowL != null) {
				irow++;
				getPI_and_SUM_from_LR(rowL, rowR, threshold);
			}
		}
	}

	private void buildAutocorrel(XLSExportOptions xlsExportOptions) {
		for (int irow = 0; irow < resultsList.size(); irow++) {
			XLSResults rowL = getRow(irow);
			correl(rowL, rowL, rowL, xlsExportOptions.nbinscorrelation);
		}
	}

	private void buildCrosscorrel(XLSExportOptions xlsExportOptions) {
		for (int irow = 0; irow < resultsList.size(); irow++) {
			XLSResults rowL = getRow(irow);
			XLSResults rowR = getNextRowIfSameCage(irow);
			if (rowR != null) {
				irow++;
				XLSResults rowLtoR = new XLSResults("LtoR", 0, 0, null);
				rowLtoR.initValuesOutArray(rowL.dimension, 0.);
				correl(rowL, rowR, rowLtoR, xlsExportOptions.nbinscorrelation);

				XLSResults rowRtoL = new XLSResults("RtoL", 0, 0, null);
				rowRtoL.initValuesOutArray(rowL.dimension, 0.);
				correl(rowR, rowL, rowRtoL, xlsExportOptions.nbinscorrelation);

				rowL.copyValuesOut(rowLtoR);
				rowR.copyValuesOut(rowRtoL);
			}
		}
	}

	private void buildCrosscorrelLR(XLSExportOptions xlsExportOptions) {
		for (int irow = 0; irow < resultsList.size(); irow++) {
			XLSResults rowL = getRow(irow);
			XLSResults rowR = getNextRowIfSameCage(irow);
			if (rowR != null) {
				irow++;

				XLSResults rowLR = new XLSResults("LR", 0, 0, null);
				rowLR.initValuesOutArray(rowL.dimension, 0.);
				combineIntervals(rowL, rowR, rowLR);

				correl(rowL, rowLR, rowL, xlsExportOptions.nbinscorrelation);
				correl(rowR, rowLR, rowR, xlsExportOptions.nbinscorrelation);
			}
		}
	}

	private void correl(XLSResults row1, XLSResults row2, XLSResults rowOut, int nbins) {
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

	private void combineIntervals(XLSResults row1, XLSResults row2, XLSResults rowOut) {
		for (int i = 0; i < rowOut.valuesOut.length; i++) {
			if ((row2.valuesOut[i] + row1.valuesOut[i]) > 0.)
				rowOut.valuesOut[i] = 1.;
		}
	}

	private void buildAutocorrelLR(XLSExportOptions xlsExportOptions) {
		for (int irow = 0; irow < resultsList.size(); irow++) {
			XLSResults rowL = getRow(irow);
			XLSResults rowR = getNextRowIfSameCage(irow);
			if (rowR != null) {
				irow++;

				XLSResults rowLR = new XLSResults("LR", 0, 0, null);
				rowLR.initValuesOutArray(rowL.dimension, 0.);
				combineIntervals(rowL, rowR, rowLR);

				correl(rowLR, rowLR, rowL, xlsExportOptions.nbinscorrelation);
				correl(rowLR, rowLR, rowR, xlsExportOptions.nbinscorrelation);
			}
		}
	}
}
