package plugins.fmp.multicafe.tools.toExcel;

import java.awt.Point;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.List;

import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFSheet;

import icy.gui.frame.progress.ProgressFrame;
import plugins.fmp.multicafe.experiment.Experiment;
import plugins.fmp.multicafe.experiment.capillaries.Capillary;
import plugins.fmp.multicafe.experiment.cells.Cell;

public class XLSExportCapillariesResults extends XLSExport {
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
					getCapillaryDataAndExport(exp, column, charSeries, EnumXLSExportType.TOPRAW);
					getCapillaryDataAndExport(exp, column, charSeries, EnumXLSExportType.TOPLEVEL);
				}
				if (options.lrPI && options.topLevel)
					getCapillaryDataAndExport(exp, column, charSeries, EnumXLSExportType.TOPLEVEL_LR);
				if (options.topLevelDelta)
					getCapillaryDataAndExport(exp, column, charSeries, EnumXLSExportType.TOPLEVELDELTA);
				if (options.lrPI && options.topLevelDelta)
					getCapillaryDataAndExport(exp, column, charSeries, EnumXLSExportType.TOPLEVELDELTA_LR);
				if (options.bottomLevel)
					getCapillaryDataAndExport(exp, column, charSeries, EnumXLSExportType.BOTTOMLEVEL);
				if (options.derivative)
					getCapillaryDataAndExport(exp, column, charSeries, EnumXLSExportType.DERIVEDVALUES);

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

	int getCapillaryDataAndExport(Experiment exp, int col0, String charSeries, EnumXLSExportType xlsExport) {
		XLSResultsArray rowListForOneExp = getCapDataFromOneExperimentSeries(exp, xlsExport);
		XSSFSheet sheet = xlsInitSheet(xlsExport.toString(), xlsExport);
		int colmax = xlsExportCapillaryResultsArrayToSheet(rowListForOneExp, sheet, xlsExport, col0, charSeries);

		if (options.onlyalive) {
			trimDeadsFromArrayList(rowListForOneExp, exp);
			sheet = xlsInitSheet(xlsExport.toString() + "_alive", xlsExport);
			xlsExportCapillaryResultsArrayToSheet(rowListForOneExp, sheet, xlsExport, col0, charSeries);
		}

		if (options.sumPerCell) {
			combineDataForOneCell(rowListForOneExp, exp);
			sheet = xlsInitSheet(xlsExport.toString() + "_cage", xlsExport);
			xlsExportCapillaryResultsArrayToSheet(rowListForOneExp, sheet, xlsExport, col0, charSeries);
		}

		return colmax;
	}

	private void trimDeadsFromArrayList(XLSResultsArray rowListForOneExp, Experiment exp) {
		for (Cell cell : exp.cageBox.cellList) {
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
				while (expi.chainToNextExperiment != null
						&& expi.chainToNextExperiment.cageBox.isFlyAlive(cellNumber)) {
					expi = expi.chainToNextExperiment;
				}
				int lastIntervalFlyAlive = expi.cageBox.getLastIntervalFlyAlive(cellNumber);
				int lastMinuteAlive = (int) (lastIntervalFlyAlive * expi.camImageBin_ms
						+ (expi.camImageFirst_ms - expAll.camImageFirst_ms));
				ilastalive = (int) (lastMinuteAlive / expAll.kymoBin_ms);
			}
			if (ilastalive > 0)
				ilastalive += 1;

			for (int iRow = 0; iRow < rowListForOneExp.size(); iRow++) {
				XLSResults row = rowListForOneExp.getRow(iRow);
				if (desc_getCellFromCapillaryName(row.name) == cellNumber)
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

	private XLSResultsArray getCapDataFromOneExperimentSeries(Experiment exp, EnumXLSExportType xlsExportType) {
		XLSResultsArray rowListForOneExp = getDescriptorsForOneExperiment(exp, xlsExportType);
		Experiment expi = exp.getFirstChainedExperiment(true);

		while (expi != null) {
			int nOutputFrames = getNOutputFrames(expi);
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
				addResultsTo_rowsForOneExp(rowListForOneExp, expi, resultsArrayList);
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

	private int getNOutputFrames(Experiment expi) {
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

	public XLSResultsArray getCapDataFromOneExperiment(Experiment exp, EnumXLSExportType exportType,
			XLSExportOptions options) {
		this.options = options;
		expAll = new Experiment();
		expAll.camImageLast_ms = exp.camImageLast_ms;
		expAll.camImageFirst_ms = exp.camImageFirst_ms;
		return getCapDataFromOneExperimentSeries(exp, exportType);
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

	private XLSResultsArray getDescriptorsForOneExperiment(Experiment exp, EnumXLSExportType xlsOption) {
		if (expAll == null)
			return null;

		// loop to get all capillaries into expAll and init rows for this experiment
		expAll.cageBox.copy(exp.cageBox);
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

	private String desc_getChoiceTestType(List<Capillary> capList, int t) {
		Capillary cap = capList.get(t);
		String choiceText = "..";
		String side = cap.getCapillarySide();
		if (side.contains("L"))
			t = t + 1;
		else
			t = t - 1;
		if (t >= 0 && t < capList.size()) {
			Capillary othercap = capList.get(t);
			String otherSide = othercap.getCapillarySide();
			if (!otherSide.contains(side)) {
				if (cap.capStimulus.equals(othercap.capStimulus)
						&& cap.capConcentration.equals(othercap.capConcentration))
					choiceText = "no-choice";
				else
					choiceText = "choice";
			}
		}
		return choiceText;
	}

	private void outputStimAndConc_according_to_DataOption(XSSFSheet sheet, EnumXLSExportType xlsExportOption,
			Capillary cap, boolean transpose, int x, int y) {
		switch (xlsExportOption) {
		case TOPLEVEL_LR:
		case TOPLEVELDELTA_LR:
		case SUMGULPS_LR:
			if (cap.getCapillarySide().equals("L"))
				XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAP_STIM.getValue(), transpose, "L+R");
			else
				XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAP_STIM.getValue(), transpose, "(L-R)/(L+R)");
			XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAP_CONC.getValue(), transpose,
					cap.capStimulus + ": " + cap.capConcentration);
			break;

		case TTOGULP_LR:
			if (cap.getCapillarySide().equals("L")) {
				XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAP_STIM.getValue(), transpose, "min_t_to_gulp");
			} else {
				XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAP_STIM.getValue(), transpose, "max_t_to_gulp");
			}
			XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAP_CONC.getValue(), transpose,
					cap.capStimulus + ": " + cap.capConcentration);
			break;

		default:
			XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAP_STIM.getValue(), transpose, cap.capStimulus);
			XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAP_CONC.getValue(), transpose, cap.capConcentration);
			break;
		}
	}

	protected int xlsExportCapillaryResultsArrayToSheet(XLSResultsArray rowListForOneExp, XSSFSheet sheet,
			EnumXLSExportType xlsExportOption, int col0, String charSeries) {
		Point pt = new Point(col0, 0);
		writeExperiment_Capillary_descriptors(expAll, charSeries, sheet, pt, xlsExportOption);
		pt = writeExperiment_data(rowListForOneExp, sheet, xlsExportOption, pt);
		return pt.x;
	}

	protected Point writeExperiment_Capillary_descriptors(Experiment exp, String charSeries, XSSFSheet sheet, Point pt,
			EnumXLSExportType xlsExportOption) {
		boolean transpose = options.transpose;
		int row = pt.y;
		int col0 = pt.x;
		XLSUtils.setValue(sheet, pt, transpose, "..");
		pt.x++;
		XLSUtils.setValue(sheet, pt, transpose, "..");
		pt.x++;
		int colseries = pt.x;
		int len = EnumXLSColumnHeader.values().length;
		for (int i = 0; i < len; i++) {
			XLSUtils.setValue(sheet, pt, transpose, "--");
			pt.x++;
		}
		pt.x = colseries;

		String filename = exp.getExperimentDirectory();
		if (filename == null)
			filename = exp.seqCamData.getImagesDirectory();
		Path path = Paths.get(filename);

		SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy");
		String date = df.format(exp.chainImageFirst_ms);

		String name0 = path.toString();
		int pos = name0.indexOf("cam");
		String cam = "-";
		if (pos > 0) {
			int pos5 = pos + 5;
			if (pos5 >= name0.length())
				pos5 = name0.length() - 1;
			cam = name0.substring(pos, pos5);
		}

		String sheetName = sheet.getSheetName();

		int rowmax = -1;
		for (EnumXLSColumnHeader dumb : EnumXLSColumnHeader.values()) {
			if (rowmax < dumb.getValue())
				rowmax = dumb.getValue();
		}

		List<Capillary> capList = exp.capillaries.capillariesList;
		for (int t = 0; t < capList.size(); t++) {
			Capillary cap = capList.get(t);
			String name = cap.getRoiName();
			int col = getRowIndexFromKymoFileName(name);
			if (col >= 0)
				pt.x = colseries + col;
			int x = pt.x;
			int y = row;
			XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.PATH.getValue(), transpose, name0);
			XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.DATE.getValue(), transpose, date);
			XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAM.getValue(), transpose, cam);

			XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.EXP_BOXID.getValue(), transpose,
					exp.getExperimentField(EnumXLSColumnHeader.EXP_BOXID));
			XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.EXP_EXPT.getValue(), transpose,
					exp.getExperimentField(EnumXLSColumnHeader.EXP_EXPT));
			XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.EXP_STIM.getValue(), transpose,
					exp.getExperimentField(EnumXLSColumnHeader.EXP_STIM));
			XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.EXP_CONC.getValue(), transpose,
					exp.getExperimentField(EnumXLSColumnHeader.EXP_CONC));
			XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.EXP_STRAIN.getValue(), transpose,
					exp.getExperimentField(EnumXLSColumnHeader.EXP_STRAIN));
			XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.EXP_SEX.getValue(), transpose,
					exp.getExperimentField(EnumXLSColumnHeader.EXP_SEX));
			XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.EXP_COND1.getValue(), transpose,
					exp.getExperimentField(EnumXLSColumnHeader.EXP_COND1));
			XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.EXP_COND2.getValue(), transpose,
					exp.getExperimentField(EnumXLSColumnHeader.EXP_COND2));

			XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAP_VOLUME.getValue(), transpose,
					exp.capillaries.capillariesDescription.volume);
			XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAP_PIXELS.getValue(), transpose,
					exp.capillaries.capillariesDescription.pixels);

			XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAP.getValue(), transpose,
					cap.getSideDescriptor(xlsExportOption));
			outputStimAndConc_according_to_DataOption(sheet, xlsExportOption, cap, transpose, x, y);

			XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAP_CAGEINDEX.getValue(), transpose, cap.capCellID);
			XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAGEID.getValue(), transpose,
					charSeries + cap.capCellID);
			XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAP_NFLIES.getValue(), transpose, cap.capNFlies);

			XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.DUM4.getValue(), transpose, sheetName);
			XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CHOICE_NOCHOICE.getValue(), transpose,
					desc_getChoiceTestType(capList, t));
			if (exp.cageBox.cellList.size() > t / 2) {
				Cell cell = exp.cageBox.cellList.get(t / 2);
				XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAGE_STRAIN.getValue(), transpose,
						cell.strCellStrain);
				XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAGE_SEX.getValue(), transpose, cell.strCellSex);
				XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAGE_AGE.getValue(), transpose, cell.cellAge);
				XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAGE_COMMENT.getValue(), transpose,
						cell.strCellComment);
			}
		}
		pt.x = col0;
		pt.y = rowmax + 1;
		return pt;
	}

}
