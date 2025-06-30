package plugins.fmp.multicafe.tools.toExcel;

import java.awt.Point;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFSheet;

import icy.gui.frame.progress.ProgressFrame;
import plugins.fmp.multicafe.experiment.CombinedExperiment;
import plugins.fmp.multicafe.experiment.Experiment;
import plugins.fmp.multicafe.experiment.capillaries.Capillary;
import plugins.fmp.multicafe.experiment.cells.Cell;

public class XLSExportCapillariesResults extends XLSExport {
	//
	public void exportToFile(String filename, XLSExportOptions opt) {
		System.out.println("XLSExpoportCapillaries:exportToFile() - start output");
		options = opt;
		expList = options.expList;

		boolean loadCapillaries = true;
		boolean loadDrosoTrack = options.onlyalive;
		expList.loadListOfMeasuresFromAllExperiments(loadCapillaries, loadDrosoTrack);
		expList.chainExperimentsUsingKymoIndexes(options.collateSeries);
		expList.setFirstImageForAllExperiments(options.collateSeries);
		expAll = expList.get_MsTime_of_StartAndEnd_AllExperiments(options);

		ProgressFrame progress = new ProgressFrame("Export data to Excel");
		int nbexpts = expList.getItemCount();
		progress.setLength(nbexpts);

		try {
			int column = 1;
			int iSeries = 0;
			workbook = xlsInitWorkbook();
			for (int index = options.firstExp; index <= options.lastExp; index++) {
				Experiment exp = expList.getItemAt(index);
				if (exp.chainToPreviousExperiment != null)
					continue;

				progress.setMessage("Export experiment " + (index + 1) + " of " + nbexpts);
				String charSeries = CellReference.convertNumToColString(iSeries);

				if (options.topLevel) {
					getCapillaryDataAndExport(exp, column, charSeries, EnumXLSExport.TOPRAW);
					getCapillaryDataAndExport(exp, column, charSeries, EnumXLSExport.TOPLEVEL);
				}
				if (options.lrPI && options.topLevel)
					getCapillaryDataAndExport(exp, column, charSeries, EnumXLSExport.TOPLEVEL_LR);
				if (options.topLevelDelta)
					getCapillaryDataAndExport(exp, column, charSeries, EnumXLSExport.TOPLEVELDELTA);
				if (options.lrPI && options.topLevelDelta)
					getCapillaryDataAndExport(exp, column, charSeries, EnumXLSExport.TOPLEVELDELTA_LR);
				if (options.bottomLevel)
					getCapillaryDataAndExport(exp, column, charSeries, EnumXLSExport.BOTTOMLEVEL);
				if (options.derivative)
					getCapillaryDataAndExport(exp, column, charSeries, EnumXLSExport.DERIVEDVALUES);

				if (!options.collateSeries || exp.chainToPreviousExperiment == null)
					column += expList.maxSizeOfCapillaryArrays + 2;
				iSeries++;
				progress.incPosition();
			}
			progress.setMessage("Save Excel file to disk... ");
			FileOutputStream fileOut = new FileOutputStream(filename);
			workbook.write(fileOut);
			fileOut.close();
			workbook.close();
			progress.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("XLSExpoportCapillaries:exportToFile() XLS output finished");
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

		if (options.sumPerCell) {
			combineDataForOneCell(rowListForOneExp, exp);
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

	private XLSResultsArray getXLSResultsArray_Descriptors_From_CombinedExperiment(Experiment exp,
			EnumXLSExport xlsOption, XLSExportOptions options) {

		// loop to get all capillaries into expAll and init rows for this experiment
		expAll.cells.copy(exp.cells);
		expAll.capillaries.copy(exp.capillaries);
		expAll.chainImageFirst_ms = exp.chainImageFirst_ms;
		expAll.copyExperimentFields(exp);
		expAll.setExperimentDirectory(exp.getExperimentDirectory());

		Experiment expi = exp.chainToNextExperiment;
		while (expi != null) {
			expAll.capillaries.mergeLists(expi.capillaries);
			expi = expi.chainToNextExperiment;
		}

		int nFrames = (int) ((expAll.camImageLast_ms - expAll.camImageFirst_ms) / options.buildExcelStepMs + 1);
		int ncapillaries = expAll.capillaries.capillariesList.size();
		XLSResultsArray rowListForOneExp = new XLSResultsArray(ncapillaries);
		for (int i = 0; i < ncapillaries; i++) {
			Capillary cap = expAll.capillaries.capillariesList.get(i);
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

		EnumXLSExport xlsoption = resultsArrayList.getRow(0).exportType;

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

	private void trimDeadsFromArrayList(XLSResultsArray rowListForOneExp, Experiment exp) {
		for (Cell cell : exp.cells.cellList) {
			String roiname = cell.cellRoi2D.getName();
			if (roiname.length() < 4)
				continue;
			String test = roiname.substring(0, 4);
			if (!test.contains("cage") || test.contains("cell"))
				continue;

			String cellNumberString = roiname.substring(4);
			int cellNumber = Integer.valueOf(cellNumberString);
			int ilastalive = 0;
			if (cell.cellNFlies > 0) {
				Experiment expi = exp;
				while (expi.chainToNextExperiment != null && expi.chainToNextExperiment.cells.isFlyAlive(cellNumber)) {
					expi = expi.chainToNextExperiment;
				}
				int lastIntervalFlyAlive = expi.cells.getLastIntervalFlyAlive(cellNumber);
				int lastMinuteAlive = (int) (lastIntervalFlyAlive * expi.camImageBin_ms
						+ (expi.camImageFirst_ms - expAll.camImageFirst_ms));
				ilastalive = (int) (lastMinuteAlive / expAll.kymoBin_ms);
			}
			if (ilastalive > 0)
				ilastalive += 1;

			for (int iRow = 0; iRow < rowListForOneExp.size(); iRow++) {
				XLSResults row = rowListForOneExp.getRow(iRow);
				if (desc_getIndex_CellFromCapillaryName(row.name) == cellNumber)
					row.clearValues(ilastalive);
			}
		}
	}

	private void combineDataForOneCell(XLSResultsArray rowListForOneExp, Experiment exp) {
		for (int iRow0 = 0; iRow0 < rowListForOneExp.size(); iRow0++) {
			XLSResults row_master = rowListForOneExp.getRow(iRow0);
			if (row_master.nflies == 0 || row_master.valuesOut == null)
				continue;

			for (int iRow = 0; iRow < rowListForOneExp.size(); iRow++) {
				XLSResults row = rowListForOneExp.getRow(iRow);
				if (row.nflies == 0 || row.valuesOut == null)
					continue;
				if (row.cellID != row_master.cellID)
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
		expAll.camImageLast_ms = exp.camImageLast_ms;
		expAll.camImageFirst_ms = exp.camImageFirst_ms;
		return getXLSResultArray_CapillaryData_From_CombinedExperiment(exp, exportType, options);
	}

//	private void addResultsTo_rowsForOneExp(XLSResultsArray rowListForOneExp, Experiment expi,
//			XLSResultsArray resultsArrayList) {
//		if (resultsArrayList.resultsList.size() < 1)
//			return;
//
//		EnumXLSExportType xlsoption = resultsArrayList.getRow(0).exportType;
//
//		long offsetChain = expi.camImageFirst_ms - expi.chainImageFirst_ms;
//		long start_Ms = expi.kymoFirst_ms + offsetChain; // TODO check when collate?
//		long end_Ms = expi.kymoLast_ms + offsetChain;
//		if (options.fixedIntervals) {
//			if (start_Ms < options.startAll_Ms)
//				start_Ms = options.startAll_Ms;
//			if (start_Ms > expi.camImageLast_ms)
//				return;
//
//			if (end_Ms > options.endAll_Ms)
//				end_Ms = options.endAll_Ms;
//			if (end_Ms > expi.camImageFirst_ms)
//				return;
//		}
//
//		// TODO check this
//		final long from_first_Ms = start_Ms - offsetChain;
//		final long from_lastMs = end_Ms - offsetChain;
//		final int to_first_index = (int) (start_Ms / options.buildExcelStepMs);
//		final int to_nvalues = (int) ((end_Ms - start_Ms) / options.buildExcelStepMs) + 1;
//
//		for (int iRow = 0; iRow < rowListForOneExp.size(); iRow++) {
//			XLSResults row = rowListForOneExp.getRow(iRow);
//			XLSResults results = getResultsArrayWithThatName(row.name, resultsArrayList);
//			if (results != null && results.valuesOut != null) {
//				double dvalue = 0.;
//				switch (xlsoption) {
//				case TOPLEVEL:
//				case TOPLEVEL_LR:
//				case SUMGULPS:
//				case SUMGULPS_LR:
//				case TOPLEVELDELTA:
//				case TOPLEVELDELTA_LR:
//					if (options.collateSeries && options.padIntervals && expi.chainToPreviousExperiment != null)
//						dvalue = padWithLastPreviousValue(row, to_first_index);
//					break;
//				default:
//					break;
//				}
//
//				int icolTo = 0;
//				if (options.collateSeries || options.absoluteTime)
//					icolTo = to_first_index;
//				for (long fromTime = from_first_Ms; fromTime <= from_lastMs; fromTime += options.buildExcelStepMs, icolTo++) {
//					int from_i = (int) Math
//							.round(((double) (fromTime - from_first_Ms)) / ((double) options.buildExcelStepMs));
//					if (from_i >= results.valuesOut.length)
//						break;
//					// TODO check how this can happen
//					if (from_i < 0)
//						continue;
//					double value = results.valuesOut[from_i] + dvalue;
//					if (icolTo >= row.valuesOut.length)
//						break;
//					row.valuesOut[icolTo] = value;
//				}
//
//			} else {
//				if (options.collateSeries && options.padIntervals && expi.chainToPreviousExperiment != null) {
//					double dvalue = padWithLastPreviousValue(row, to_first_index);
//					int tofirst = (int) to_first_index;
//					int tolast = (int) (tofirst + to_nvalues);
//					if (tolast > row.valuesOut.length)
//						tolast = row.valuesOut.length;
//					for (int toi = tofirst; toi < tolast; toi++)
//						row.valuesOut[toi] = dvalue;
//				}
//			}
//		}
//	}

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

		List<Capillary> capList = exp.capillaries.capillariesList;
		for (int index = 0; index < capList.size(); index++) {
			Capillary cap = capList.get(index);
			String name = cap.getRoiName();
			int col = getRowIndexFromKymoFileName(name);
			if (col >= 0)
				pt.x = colseries + col;
			int x = pt.x;
			int y = row;

			XLSExportExperimentParameters(sheet, transpose, x, y, exp);
			XLSExportCapillaryParameters(sheet, transpose, x, y, charSeries, exp, cap, xlsExportOption, index);
			if (exp.cells.cellList.size() > index / 2) {
				Cell cell = exp.cells.cellList.get(index / 2);
				XLSExportCellParameters(sheet, transpose, x, y, charSeries, exp, cell);
			}
			XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.DUM4.getValue(), transpose, sheet.getSheetName());
		}
		pt.x = col0;
		pt.y = rowmax + 1;
		return pt;
	}

}
