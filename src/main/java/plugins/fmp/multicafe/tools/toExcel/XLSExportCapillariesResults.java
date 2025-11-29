package plugins.fmp.multicafe.tools.toExcel;

import java.awt.Point;
import java.util.List;

import org.apache.poi.xssf.usermodel.XSSFSheet;

import plugins.fmp.multicafe.fmp_experiment.CombinedExperiment;
import plugins.fmp.multicafe.fmp_experiment.Experiment;
import plugins.fmp.multicafe.fmp_experiment.cages.Cage;
import plugins.fmp.multicafe.fmp_experiment.capillaries.Capillary;
import plugins.fmp.multicafe.tools.Logger;

public class XLSExportCapillariesResults extends XLSExport {
	//
	@Override
	protected void loadMeasures() {
		boolean loadCapillaries = true;
		boolean loadDrosoTrack = options.onlyalive;
		expList.loadListOfMeasuresFromAllExperiments(loadCapillaries, loadDrosoTrack);
		expList.chainExperimentsUsingKymoIndexes(options.collateSeries);
		expList.setFirstImageForAllExperiments(options.collateSeries);
		expAll = expList.get_MsTime_of_StartAndEnd_AllExperiments(options);
	}

	@Override
	protected int processExperiment(Experiment exp, int col0, String charSeries) {
		if (options.topLevel) {
			getCapillaryDataAndExport(exp, col0, charSeries, EnumXLSExport.TOPRAW);
			getCapillaryDataAndExport(exp, col0, charSeries, EnumXLSExport.TOPLEVEL);
		}
		if (options.lrPI && options.topLevel)
			getCapillaryDataAndExport(exp, col0, charSeries, EnumXLSExport.TOPLEVEL_LR);
		if (options.topLevelDelta)
			getCapillaryDataAndExport(exp, col0, charSeries, EnumXLSExport.TOPLEVELDELTA);
		if (options.lrPI && options.topLevelDelta)
			getCapillaryDataAndExport(exp, col0, charSeries, EnumXLSExport.TOPLEVELDELTA_LR);
		if (options.bottomLevel)
			getCapillaryDataAndExport(exp, col0, charSeries, EnumXLSExport.BOTTOMLEVEL);
		if (options.derivative)
			getCapillaryDataAndExport(exp, col0, charSeries, EnumXLSExport.DERIVEDVALUES);

		if (!options.collateSeries || exp.chainToPreviousExperiment == null)
			return col0 + expList.maxSizeOfCapillaryArrays + 2;

		return col0;
	}

	int getCapillaryDataAndExport(Experiment exp, int col0, String charSeries, EnumXLSExport xlsExport) {
		XLSResultsArray rowListForOneExp = getXLSResultArray_CapillaryData_From_CombinedExperiment(exp, xlsExport,
				options);
		XSSFSheet sheet = xlsGetSheet(xlsExport.toString(), xlsExport);
		int colmax = xlsExportCapillaryResultsArrayToSheet(rowListForOneExp, sheet, xlsExport, col0, charSeries);

		if (options.onlyalive) {
			trimDeadsFromArrayList(rowListForOneExp, exp);
			sheet = xlsGetSheet(xlsExport.toString() + "_alive", xlsExport);
			xlsExportCapillaryResultsArrayToSheet(rowListForOneExp, sheet, xlsExport, col0, charSeries);
		}

		if (options.sumPerCage) {
			combineDataForOneCage(rowListForOneExp, exp);
			sheet = xlsGetSheet(xlsExport.toString() + "_cage", xlsExport);
			xlsExportCapillaryResultsArrayToSheet(rowListForOneExp, sheet, xlsExport, col0, charSeries);
		}

		return colmax;
	}

	public XLSResultsArray getXLSResultArray_CapillaryData_From_CombinedExperiment(Experiment exp,
			EnumXLSExport xlsExportType, XLSExportOptions options) {
		XLSResultsArray rowListForOneExp = getXLSResultsArray_Descriptors_From_CombinedExperiment(exp, xlsExportType,
				options);
		Experiment expi = exp.getFirstChainedExperiment(true);

		while (expi != null) {
			int nOutputFrames = getNOutputFrames(expi, options);
			if (nOutputFrames > 1) {
				XLSResultsFromCapillaries resultsArrayList = new XLSResultsFromCapillaries(
						expi.getCapillaries().getCapillariesList().size());
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

				case MARKOV_CHAIN:
				case AUTOCORREL:
				case AUTOCORREL_LR:
				case CROSSCORREL:
				case CROSSCORREL_LR:
					resultsArrayList.getResults1(expi, xlsExportType, nOutputFrames, exp.getKymoBin_ms(), options);
					break;

				case TOPLEVEL:
				case TOPLEVEL_LR:
				case TOPLEVELDELTA:
				case TOPLEVELDELTA_LR:
					options.compensateEvaporation = options.subtractEvaporation;
					resultsArrayList.getResults_T0(expi, xlsExportType, nOutputFrames, exp.getKymoBin_ms(), options);
					break;

				case TOPRAW:
					resultsArrayList.getResults_T0(expi, xlsExportType, nOutputFrames, exp.getKymoBin_ms(), options);
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
		int nOutputFrames = (int) ((expi.getKymoLast_ms() - expi.getKymoFirst_ms()) / options.buildExcelStepMs + 1);
		if (nOutputFrames <= 1) {
			if (expi.getSeqKymos().getImageWidthMax() == 0)
				expi.loadKymographs();
			expi.setKymoLast_ms(expi.getKymoFirst_ms() + expi.getSeqKymos().getImageWidthMax() * expi.getKymoBin_ms());
			if (expi.getKymoLast_ms() <= 0)
				exportError(expi, -1);
			nOutputFrames = (int) ((expi.getKymoLast_ms() - expi.getKymoFirst_ms()) / options.buildExcelStepMs + 1);

			if (nOutputFrames <= 1) {
				nOutputFrames = expi.getSeqCamData().getnTotalFrames();
				exportError(expi, nOutputFrames);
			}
		}
		return nOutputFrames;
	}

	private void exportError(Experiment expi, int nOutputFrames) {
		String error = "XLSExport:ExportError() ERROR in " + expi.getExperimentDirectory() + "\n nOutputFrames="
				+ nOutputFrames + " kymoFirstCol_Ms=" + expi.getKymoFirst_ms() + " kymoLastCol_Ms="
				+ expi.getKymoLast_ms();
		Logger.error(error);
	}

	private XLSResultsArray getXLSResultsArray_Descriptors_From_CombinedExperiment(Experiment exp,
			EnumXLSExport xlsOption, XLSExportOptions options) {

		// loop to get all capillaries into expAll and init rows for this experiment
		expAll.getCages().copy(exp.getCages());
		expAll.getCapillaries().copy(exp.getCapillaries());
		expAll.chainImageFirst_ms = exp.chainImageFirst_ms;
		expAll.copyExperimentFields(exp);
		expAll.setExperimentDirectory(exp.getExperimentDirectory());

		Experiment expi = exp.chainToNextExperiment;
		while (expi != null) {
			expAll.getCapillaries().mergeLists(expi.getCapillaries());
			expi = expi.chainToNextExperiment;
		}

		int nFrames = (int) ((expAll.getCamImageLast_ms() - expAll.getCamImageFirst_ms()) / options.buildExcelStepMs
				+ 1);
		int ncapillaries = expAll.getCapillaries().getCapillariesList().size();
		XLSResultsArray rowListForOneExp = new XLSResultsArray(ncapillaries);
		for (int i = 0; i < ncapillaries; i++) {
			Capillary cap = expAll.getCapillaries().getCapillariesList().get(i);
			XLSResults row = new XLSResults(cap.getRoiName(), cap.capNFlies, cap.capCageID, xlsOption, nFrames);
			row.stimulus = cap.capStimulus;
			row.concentration = cap.capConcentration;
			row.cageID = cap.capCageID;
			rowListForOneExp.addRow(row);
		}
		rowListForOneExp.sortRowsByName();
		return rowListForOneExp;
	}

	protected void add_resultsArrayList_To_rowListForOneExp(XLSResultsArray rowListForOneExp, Experiment expi,
			XLSResultsArray resultsArrayList, XLSExportOptions options) {
		if (resultsArrayList.resultsList.size() < 1)
			return;

		EnumXLSExport xlsoption = resultsArrayList.getRow(0).exportType;

		long offsetChain = expi.getCamImageFirst_ms() - expi.chainImageFirst_ms;
		long start_Ms = expi.getKymoFirst_ms() + offsetChain; // TODO check when collate?
		long end_Ms = expi.getKymoLast_ms() + offsetChain;
		if (options.fixedIntervals) {
			if (start_Ms < options.startAll_Ms)
				start_Ms = options.startAll_Ms;
			if (start_Ms > expi.getCamImageLast_ms())
				return;

			if (end_Ms > options.endAll_Ms)
				end_Ms = options.endAll_Ms;
			if (end_Ms > expi.getCamImageFirst_ms())
				return;
		}

		// TODO check this
		final long from_first_Ms = start_Ms - offsetChain;
		final long from_lastMs = end_Ms - offsetChain;
		final int to_first_index = (int) (start_Ms / options.buildExcelStepMs);
		final int to_nvalues = (int) ((end_Ms - start_Ms) / options.buildExcelStepMs) + 1;

		// For MARKOV_CHAIN, replace descriptor rows with the computed rows
		if (xlsoption == EnumXLSExport.MARKOV_CHAIN) {
			// On first call, replace descriptor rows with markov chain rows
			if (rowListForOneExp.size() > 0 && rowListForOneExp.getRow(0).name.contains("line")) {
				int dimension = rowListForOneExp.getRow(0).dimension;
				rowListForOneExp.resultsList.clear();
				for (XLSResults result : resultsArrayList.resultsList) {
					XLSResults newRow = new XLSResults(result.name, result.nflies, result.cageID, xlsoption, dimension);
					newRow.stimulus = result.stimulus;
					newRow.concentration = result.concentration;
					newRow.initValuesOutArray(dimension, Double.NaN);
					rowListForOneExp.addRow(newRow);
				}
			}
			// Copy data with time offset handling
			for (XLSResults result : resultsArrayList.resultsList) {
				XLSResults row = XLSResults.getResultsArrayWithThatName(result.name, rowListForOneExp);
				if (row != null && result.valuesOut != null) {
					int icolTo = 0;
					if (options.collateSeries || options.absoluteTime)
						icolTo = to_first_index;
					for (long fromTime = from_first_Ms; fromTime <= from_lastMs; fromTime += options.buildExcelStepMs, icolTo++) {
						int from_i = (int) Math
								.round(((double) (fromTime - from_first_Ms)) / ((double) options.buildExcelStepMs));
						if (from_i >= result.valuesOut.length)
							break;
						if (from_i < 0)
							continue;
						double value = result.valuesOut[from_i];
						if (icolTo >= row.valuesOut.length)
							break;
						row.valuesOut[icolTo] = value;
					}
				}
			}
			return;
		}

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

	private void trimDeadsFromArrayList(XLSResultsArray rowListForOneExp, Experiment exp) {
		for (Cage cell : exp.getCages().getCageList()) {
			String roiname = cell.getCageRoi2D().getName();
			if (roiname.length() < 4)
				continue;
			String test = roiname.substring(0, 4);
			if (!test.contains("cage") || test.contains("cell"))
				continue;

			String cellNumberString = roiname.substring(4);
			int cellNumber = Integer.valueOf(cellNumberString);
			int ilastalive = 0;
			if (cell.getCageNFlies() > 0) {
				Experiment expi = exp;
				while (expi.chainToNextExperiment != null
						&& expi.chainToNextExperiment.getCages().isFlyAlive(cellNumber)) {
					expi = expi.chainToNextExperiment;
				}
				int lastIntervalFlyAlive = expi.getCages().getLastIntervalFlyAlive(cellNumber);
				int lastMinuteAlive = (int) (lastIntervalFlyAlive * expi.getCamImageBin_ms()
						+ (expi.getCamImageFirst_ms() - expAll.getCamImageFirst_ms()));
				ilastalive = (int) (lastMinuteAlive / expAll.getKymoBin_ms());
			}
			if (ilastalive > 0)
				ilastalive += 1;

			for (int iRow = 0; iRow < rowListForOneExp.size(); iRow++) {
				XLSResults row = rowListForOneExp.getRow(iRow);
				if (desc_getIndex_CageFromCapillaryName(row.name) == cellNumber)
					row.clearValues(ilastalive);
			}
		}
	}

	private void combineDataForOneCage(XLSResultsArray rowListForOneExp, Experiment exp) {
		for (int iRow0 = 0; iRow0 < rowListForOneExp.size(); iRow0++) {
			XLSResults row_master = rowListForOneExp.getRow(iRow0);
			if (row_master.nflies == 0 || row_master.valuesOut == null)
				continue;

			for (int iRow = 0; iRow < rowListForOneExp.size(); iRow++) {
				XLSResults row = rowListForOneExp.getRow(iRow);
				if (row.nflies == 0 || row.valuesOut == null)
					continue;
				if (row.cageID != row_master.cageID)
					continue;
				if (row.name.equals(row_master.name))
					continue;
				if (row.stimulus.equals(row_master.stimulus) && row.concentration.equals(row_master.concentration)) {
					row_master.sumValues_out(row);
					row.clearAll();
				}
			}
		}
	}

	protected int xlsExportCapillaryResultsArrayToSheet(XLSResultsArray rowListForOneExp, XSSFSheet sheet,
			EnumXLSExport xlsExportOption, int col0, String charSeries) {
		Point pt = new Point(col0, 0);
		writeExperiment_Capillary_descriptors(expAll, charSeries, sheet, pt, xlsExportOption);
		pt = writeExperiment_data(rowListForOneExp, sheet, xlsExportOption, pt);
		return pt.x;
	}

	public XLSResultsArray getCapDataFromOneExperiment(Experiment exp, EnumXLSExport exportType,
			XLSExportOptions options) {
		this.options = options;
		expAll = new CombinedExperiment(exp, false);
		expAll.setCamImageLast_ms(exp.getCamImageLast_ms());
		expAll.setCamImageFirst_ms(exp.getCamImageFirst_ms());
		return getXLSResultArray_CapillaryData_From_CombinedExperiment(exp, exportType, options);
	}

	protected Point writeExperiment_Capillary_descriptors(Experiment exp, String charSeries, XSSFSheet sheet, Point pt,
			EnumXLSExport xlsExportOption) {
		boolean transpose = options.transpose;
		int row = pt.y;
		int col0 = pt.x;
		int colseries = writeSeparator_Between_Experiments(sheet, pt, transpose);

		int len = EnumXLSColumnHeader.values().length;
		for (int i = 0; i < len; i++) {
			XLSUtils.setValue(sheet, pt, transpose, "--");
			pt.x++;
		}
		pt.x = colseries;

		int rowmax = -1;
		for (EnumXLSColumnHeader dumb : EnumXLSColumnHeader.values()) {
			if (rowmax < dumb.getValue())
				rowmax = dumb.getValue();
		}

		if (exp.getCages().getCageList().size() < exp.getCapillaries().getCapillariesList().size() / 2)
			exp.dispatchCapillariesToCages();

		List<Capillary> capList = exp.getCapillaries().getCapillariesList();
		for (int index = 0; index < capList.size(); index++) {
			Capillary cap = capList.get(index);
			String name = cap.getRoiName();
			int col = getRowIndexFromKymoFileName(name);
			if (col >= 0)
				pt.x = colseries + col;
			int x = pt.x;
			int y = row;

			XLSExportExperimentParameters(sheet, transpose, x, y, charSeries, exp);
			if (exp.getCages().getCageList().size() > index / 2) {
				Cage cage = exp.getCages().getCageList().get(index / 2);
				xlsExportCageParameters(sheet, transpose, x, y, charSeries, exp, cage);
			}
			XLSExportCapillaryParameters(sheet, transpose, x, y, charSeries, exp, cap, xlsExportOption, index);
			XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.DUM4.getValue(), transpose, sheet.getSheetName());
		}
		pt.x = col0;
		pt.y = rowmax + 1;
		return pt;
	}

}
