package plugins.fmp.multicafe.experiment;

import plugins.fmp.multicafe.experiment.capillaries.Capillary;
import plugins.fmp.multicafe.tools.toExcel.EnumXLSExportType;
import plugins.fmp.multicafe.tools.toExcel.XLSExportOptions;
import plugins.fmp.multicafe.tools.toExcel.XLSResults;
import plugins.fmp.multicafe.tools.toExcel.XLSResultsArray;
import plugins.fmp.multicafe.tools.toExcel.XLSResultsFromCapillaries;

public class CombinedExperiment extends Experiment {

	public XLSResultsArray getXLSResultArray_CapillaryData_From_CombinedExperiment(Experiment exp, EnumXLSExportType xlsExportType,
			XLSExportOptions options) {
		XLSResultsArray rowListForOneExp = getXLSResultsArray_Descriptors_From_CombinedExperiment(exp, xlsExportType, options);
		Experiment expi = exp.getFirstChainedExperiment(true);

		while (expi != null) {
			int nOutputFrames = getNOutputFrames(expi, options);
			if (nOutputFrames > 1) {
				XLSResultsFromCapillaries resultsArrayList = new XLSResultsFromCapillaries(
						expi.capillaries.capillariesList.size());
				options.compensateEvaporation = false;
				switch (xlsExportType) {
				case BOTTOMLEVEL:
				case NBGULPS:
				case AMPLITUDEGULPS:
				case TTOGULP:
				case TTOGULP_LR:

				case DERIVEDVALUES:
				case SUMGULPS:
				case SUMGULPS_LR:

				case AUTOCORREL:
				case AUTOCORREL_LR:
				case CROSSCORREL:
				case CROSSCORREL_LR:
					resultsArrayList.getResults1(expi.capillaries, xlsExportType, nOutputFrames, exp.kymoBin_ms,
							options);
					break;

				case TOPLEVEL:
				case TOPLEVEL_LR:
				case TOPLEVELDELTA:
				case TOPLEVELDELTA_LR:
					options.compensateEvaporation = options.subtractEvaporation;

				case TOPRAW:
					resultsArrayList.getResults_T0(expi.capillaries, xlsExportType, nOutputFrames, exp.kymoBin_ms,
							options);
					break;

				default:
					break;
				}
				add_resultsArrayList_To_rowListForOneExp(rowListForOneExp, expi, resultsArrayList, options);
			}
			expi = expi.chainToNextExperiment;
		}

		switch (xlsExportType) {
		case TOPLEVELDELTA:
		case TOPLEVELDELTA_LR:
			rowListForOneExp.subtractDeltaT(1, 1); // options.buildExcelStepMs);
			break;
		default:
			break;
		}
		return rowListForOneExp;
	}

	private int getNOutputFrames(Experiment expi, XLSExportOptions options) {
		int nOutputFrames = (int) ((expi.kymoLast_ms - expi.kymoFirst_ms) / options.buildExcelStepMs + 1);
		if (nOutputFrames <= 1) {
			if (expi.seqKymos.imageWidthMax == 0)
				expi.loadKymographs();
			expi.kymoLast_ms = expi.kymoFirst_ms + expi.seqKymos.imageWidthMax * expi.kymoBin_ms;
			if (expi.kymoLast_ms <= 0)
				exportError(expi, -1);
			nOutputFrames = (int) ((expi.kymoLast_ms - expi.kymoFirst_ms) / options.buildExcelStepMs + 1);

			if (nOutputFrames <= 1) {
				nOutputFrames = expi.seqCamData.nTotalFrames;
				exportError(expi, nOutputFrames);
			}
		}
		return nOutputFrames;
	}

	private void exportError(Experiment expi, int nOutputFrames) {
		String error = "XLSExport:ExportError() ERROR in " + expi.getExperimentDirectory() + "\n nOutputFrames="
				+ nOutputFrames + " kymoFirstCol_Ms=" + expi.kymoFirst_ms + " kymoLastCol_Ms=" + expi.kymoLast_ms;
		System.out.println(error);
	}

	private XLSResultsArray getXLSResultsArray_Descriptors_From_CombinedExperiment(Experiment exp, EnumXLSExportType xlsOption,
			XLSExportOptions options) {

		// loop to get all capillaries into expAll and init rows for this experiment
		cageBox.copy(exp.cageBox);
		capillaries.copy(exp.capillaries);
		chainImageFirst_ms = exp.chainImageFirst_ms;
		copyExperimentFields(exp);
		setExperimentDirectory(exp.getExperimentDirectory());

		Experiment expi = exp.chainToNextExperiment;
		while (expi != null) {
			capillaries.mergeLists(expi.capillaries);
			expi = expi.chainToNextExperiment;
		}

		int nFrames = (int) ((camImageLast_ms - camImageFirst_ms) / options.buildExcelStepMs + 1);
		int ncapillaries = capillaries.capillariesList.size();
		XLSResultsArray rowListForOneExp = new XLSResultsArray(ncapillaries);
		for (int i = 0; i < ncapillaries; i++) {
			Capillary cap = capillaries.capillariesList.get(i);
			XLSResults row = new XLSResults(cap.getRoiName(), cap.capNFlies, cap.capCellID, xlsOption, nFrames);
			row.stimulus = cap.capStimulus;
			row.concentration = cap.capConcentration;
			row.cellID = cap.capCellID;
			rowListForOneExp.addRow(row);
		}
		rowListForOneExp.sortRowsByName();
		return rowListForOneExp;
	}

	protected void add_resultsArrayList_To_rowListForOneExp(XLSResultsArray rowListForOneExp, Experiment expi,
			XLSResultsArray resultsArrayList, XLSExportOptions options) {
		if (resultsArrayList.resultsList.size() < 1)
			return;

		EnumXLSExportType xlsoption = resultsArrayList.getRow(0).exportType;

		long offsetChain = expi.camImageFirst_ms - expi.chainImageFirst_ms;
		long start_Ms = expi.kymoFirst_ms + offsetChain; // TODO check when collate?
		long end_Ms = expi.kymoLast_ms + offsetChain;
		if (options.fixedIntervals) {
			if (start_Ms < options.startAll_Ms)
				start_Ms = options.startAll_Ms;
			if (start_Ms > expi.camImageLast_ms)
				return;

			if (end_Ms > options.endAll_Ms)
				end_Ms = options.endAll_Ms;
			if (end_Ms > expi.camImageFirst_ms)
				return;
		}

		// TODO check this
		final long from_first_Ms = start_Ms - offsetChain;
		final long from_lastMs = end_Ms - offsetChain;
		final int to_first_index = (int) (start_Ms / options.buildExcelStepMs);
		final int to_nvalues = (int) ((end_Ms - start_Ms) / options.buildExcelStepMs) + 1;

		for (int iRow = 0; iRow < rowListForOneExp.size(); iRow++) {
			XLSResults row = rowListForOneExp.getRow(iRow);
			XLSResults results = XLSResults.getResultsArrayWithThatName(row.name, resultsArrayList);
			if (results != null && results.valuesOut != null) {
				double dvalue = 0.;
				switch (xlsoption) {
				case TOPLEVEL:
				case TOPLEVEL_LR:
				case SUMGULPS:
				case SUMGULPS_LR:
				case TOPLEVELDELTA:
				case TOPLEVELDELTA_LR:
					if (options.collateSeries && options.padIntervals && expi.chainToPreviousExperiment != null)
						dvalue = row.padWithLastPreviousValue(to_first_index);
					break;
				default:
					break;
				}

				int icolTo = 0;
				if (options.collateSeries || options.absoluteTime)
					icolTo = to_first_index;
				for (long fromTime = from_first_Ms; fromTime <= from_lastMs; fromTime += options.buildExcelStepMs, icolTo++) {
					int from_i = (int) Math
							.round(((double) (fromTime - from_first_Ms)) / ((double) options.buildExcelStepMs));
					if (from_i >= results.valuesOut.length)
						break;
					// TODO check how this can happen
					if (from_i < 0)
						continue;
					double value = results.valuesOut[from_i] + dvalue;
					if (icolTo >= row.valuesOut.length)
						break;
					row.valuesOut[icolTo] = value;
				}

			} else {
				if (options.collateSeries && options.padIntervals && expi.chainToPreviousExperiment != null) {
					double dvalue = row.padWithLastPreviousValue(to_first_index);
					int tofirst = (int) to_first_index;
					int tolast = (int) (tofirst + to_nvalues);
					if (tolast > row.valuesOut.length)
						tolast = row.valuesOut.length;
					for (int toi = tofirst; toi < tolast; toi++)
						row.valuesOut[toi] = dvalue;
				}
			}
		}
	}

}
