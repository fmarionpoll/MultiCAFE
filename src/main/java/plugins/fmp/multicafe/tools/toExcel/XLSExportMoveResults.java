package plugins.fmp.multicafe.tools.toExcel;

import java.awt.Point;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFSheet;

import icy.gui.frame.progress.ProgressFrame;
import plugins.fmp.multicafe.experiment.Experiment;
import plugins.fmp.multicafe.experiment.cells.Cell;
import plugins.fmp.multicafe.experiment.cells.FlyPosition;
import plugins.fmp.multicafe.experiment.cells.FlyPositions;

public class XLSExportMoveResults extends XLSExport {

//	List<FlyPositions> rowsForOneExp = new ArrayList<FlyPositions>();

	public void exportToFile(String filename, XLSExportOptions opt) {
		System.out.println("XLSExpoportMove:exportToFile() start output");
		options = opt;
		expList = options.expList;

		boolean loadCapillaries = true;
		boolean loadDrosoTrack = true;
		expList.loadListOfMeasuresFromAllExperiments(loadCapillaries, loadDrosoTrack);
		expList.chainExperimentsUsingKymoIndexes(options.collateSeries);
		expList.setFirstImageForAllExperiments(options.collateSeries);
		expAll = expList.get_MsTime_of_StartAndEnd_AllExperiments(options);
		expList.maxSizeOfCellArrays = expAll.cageBox.cellList.size();

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

				if (options.xyImage)
					getMoveDataAndExport(exp, column, charSeries, EnumXLSExportType.XYIMAGEC);
				if (options.xyCell)
					getMoveDataAndExport(exp, column, charSeries, EnumXLSExportType.XYTOPCAGEC);
				if (options.xyCapillaries)
					getMoveDataAndExport(exp, column, charSeries, EnumXLSExportType.XYTIPCAPSC);
				if (options.ellipseAxes)
					getMoveDataAndExport(exp, column, charSeries, EnumXLSExportType.ELLIPSEAXES);
				if (options.distance)
					getMoveDataAndExport(exp, column, charSeries, EnumXLSExportType.DISTANCE);
				if (options.alive)
					getMoveDataAndExport(exp, column, charSeries, EnumXLSExportType.ISALIVE);
				if (options.sleep)
					getMoveDataAndExport(exp, column, charSeries, EnumXLSExportType.SLEEP);

				if (!options.collateSeries || exp.chainToPreviousExperiment == null)
					column += expList.maxSizeOfCellArrays + 2; // TODO check - may be:
																// expList.maxSizeOfCapillaryArrays/2 + 2
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
		System.out.println("XLSExpoportMove:exportToFile() - output finished");
	}

	private int getMoveDataAndExport(Experiment exp, int col0, String charSeries, EnumXLSExportType xlsExport) {
		XLSResultsArray rowListForOneExp = getMoveDataFromOneExperimentSeries(exp, xlsExport);
		XSSFSheet sheet = xlsInitSheet(xlsExport.toString(), xlsExport);
		int colmax = xlsExportMoveResultsArrayToSheet(rowListForOneExp, sheet, xlsExport, col0, charSeries);

		if (options.onlyalive) {
			trimDeadsFromRowMoveData(rowListForOneExp, exp);
			sheet = xlsInitSheet(xlsExport.toString() + "_alive", xlsExport);
			xlsExportMoveResultsArrayToSheet(rowListForOneExp, sheet, xlsExport, col0, charSeries);
		}

		return colmax;
	}

	protected int xlsExportMoveResultsArrayToSheet(XLSResultsArray rowListForOneExp, XSSFSheet sheet,
			EnumXLSExportType xlsExportOption, int col0, String charSeries) {
		Point pt = new Point(col0, 0);
		writeExperiment_Cell_descriptors(expAll, charSeries, sheet, pt, xlsExportOption);
		// pt = writeData2(rowListForOneExp, sheet, xlsExportOption, pt);
		pt = writeExperiment_data(rowListForOneExp, sheet, xlsExportOption, pt);
		return pt.x;
	}

	private XLSResultsArray getMoveDataFromOneExperimentSeries(Experiment exp, EnumXLSExportType xlsOption) {
		XLSResultsArray moveDescriptorsForOneExp = getMoveDescriptorsForOneExperiment(exp, xlsOption);
		Experiment expi = exp.getFirstChainedExperiment(true);

		List<FlyPositions> dummyPositionsArrayList = new ArrayList<FlyPositions>(0);

		while (expi != null) {
			int nframes = 1 + (int) (expi.camImageLast_ms - expi.camImageFirst_ms) / options.buildExcelStepMs;
			if (nframes == 0)
				continue;

			List<FlyPositions> positionsArrayList = computeMoveResults(expi, xlsOption, options, nframes);
			// here add resultsArrayList to expAll
			addMoveResultsTo_rowsForOneExp(expi, positionsArrayList);
			// addResultsTo_rowsForOneExp(rowListForOneExp, expi, positionsArrayList);
			expi = expi.chainToNextExperiment;
		}

		XLSResultsArray xlsResultsArray = combine(moveDescriptorsForOneExp, dummyPositionsArrayList);
		return xlsResultsArray;
	}

	private XLSResultsArray combine(XLSResultsArray moveDescriptorsForOneExp, List<FlyPositions> positionsArrayList) {
		return moveDescriptorsForOneExp;
	}

	protected List<FlyPositions> computeMoveResults(Experiment expi, EnumXLSExportType xlsOption,
			XLSExportOptions options, int nFrames) {
		List<FlyPositions> positionsArrayList = new ArrayList<FlyPositions>(expi.cageBox.cellList.size());
		double pixelsize = 32. / expi.capillaries.capillariesList.get(0).capPixels;
		for (Cell cell : expi.cageBox.cellList) {
			FlyPositions flyPositionsResults = new FlyPositions(cell.cellRoi2D.getName(), xlsOption, nFrames,
					options.buildExcelStepMs);
			flyPositionsResults.nflies = cell.cellNFlies;
			if (flyPositionsResults.nflies < 0) {
				flyPositionsResults.setPixelSize(pixelsize);
				switch (xlsOption) {
				case DISTANCE:
					flyPositionsResults.excelComputeDistanceBetweenPoints(cell.flyPositions, (int) expi.camImageBin_ms,
							options.buildExcelStepMs);
					break;
				case ISALIVE:
					flyPositionsResults.excelComputeIsAlive(cell.flyPositions, (int) expi.camImageBin_ms,
							options.buildExcelStepMs);
					break;
				case SLEEP:
					flyPositionsResults.excelComputeSleep(cell.flyPositions, (int) expi.camImageBin_ms,
							options.buildExcelStepMs);
					break;
				case XYTOPCAGEC:
					flyPositionsResults.excelComputeNewPointsOrigin(cell.getCenterTopCell(), cell.flyPositions,
							(int) expi.camImageBin_ms, options.buildExcelStepMs);
					break;
				case XYTIPCAPSC:
					flyPositionsResults.excelComputeNewPointsOrigin(cell.getCenterTipCapillaries(expi.capillaries),
							cell.flyPositions, (int) expi.camImageBin_ms, options.buildExcelStepMs);
					break;
				case ELLIPSEAXES:
					flyPositionsResults.excelComputeEllipse(cell.flyPositions, (int) expi.camImageBin_ms,
							options.buildExcelStepMs);
					break;
				case XYIMAGEC:
				default:
					break;
				}
				flyPositionsResults.convertPixelsToPhysicalValues();
				positionsArrayList.add(flyPositionsResults);
			}
		}
		return positionsArrayList;
	}

	private FlyPositions getResultsArrayWithThatName(String testname, List<FlyPositions> resultsArrayList) {
		FlyPositions resultsFound = null;
		for (FlyPositions results : resultsArrayList) {
			if (!results.name.equals(testname))
				continue;
			resultsFound = results;
			break;
		}
		return resultsFound;
	}

	private void addMoveResultsTo_rowsForOneExp(Experiment expi, List<FlyPositions> rowsForOneExp) {
		long start_Ms = expi.camImageFirst_ms - expAll.camImageFirst_ms;
		long end_Ms = expi.camImageLast_ms - expAll.camImageFirst_ms;
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

		final long from_first_Ms = start_Ms + expAll.camImageFirst_ms;
		final long from_lastMs = end_Ms + expAll.camImageFirst_ms;
		final int to_first_index = (int) (from_first_Ms - expAll.camImageFirst_ms) / options.buildExcelStepMs;
		final int to_nvalues = (int) ((from_lastMs - from_first_Ms) / options.buildExcelStepMs) + 1;

		for (FlyPositions rowFlyPositions : rowsForOneExp) {
			FlyPositions results = getResultsArrayWithThatName(rowFlyPositions.name, rowsForOneExp);
			if (results != null) {
				if (options.collateSeries && options.padIntervals && expi.chainToPreviousExperiment != null)
					padWithLastPreviousValue(rowFlyPositions, to_first_index);

				for (long fromTime = from_first_Ms; fromTime <= from_lastMs; fromTime += options.buildExcelStepMs) {
					int from_i = (int) ((fromTime - from_first_Ms) / options.buildExcelStepMs);
					if (from_i >= results.flyPositionList.size())
						break;
					FlyPosition aVal = results.flyPositionList.get(from_i);
					int to_i = (int) ((fromTime - expAll.camImageFirst_ms) / options.buildExcelStepMs);
					if (to_i >= rowFlyPositions.flyPositionList.size())
						break;
					if (to_i < 0)
						continue;
					rowFlyPositions.flyPositionList.get(to_i).copy(aVal);
				}

			} else {
				if (options.collateSeries && options.padIntervals && expi.chainToPreviousExperiment != null) {
					FlyPosition posok = padWithLastPreviousValue(rowFlyPositions, to_first_index);
					int nvalues = to_nvalues;
					if (posok != null) {
						if (nvalues > rowFlyPositions.flyPositionList.size())
							nvalues = rowFlyPositions.flyPositionList.size();
						int tofirst = to_first_index;
						int tolast = tofirst + nvalues;
						if (tolast > rowFlyPositions.flyPositionList.size())
							tolast = rowFlyPositions.flyPositionList.size();
						for (int toi = tofirst; toi < tolast; toi++)
							rowFlyPositions.flyPositionList.get(toi).copy(posok);
					}
				}
			}
		}
	}

	private FlyPosition padWithLastPreviousValue(FlyPositions row, int transfer_first_index) {
		FlyPosition posok = null;
		int index = getIndexOfFirstNonEmptyValueBackwards(row, transfer_first_index);
		if (index >= 0) {
			posok = row.flyPositionList.get(index);
			for (int i = index + 1; i < transfer_first_index; i++) {
				FlyPosition pos = row.flyPositionList.get(i);
				pos.copy(posok);
				pos.bPadded = true;
			}
		}
		return posok;
	}

	private int getIndexOfFirstNonEmptyValueBackwards(FlyPositions row, int fromindex) {
		int index = -1;
		for (int i = fromindex; i >= 0; i--) {
			FlyPosition pos = row.flyPositionList.get(i);
			if (!Double.isNaN(pos.x)) {
				index = i;
				break;
			}
		}
		return index;
	}

	private void trimDeadsFromRowMoveData(XLSResultsArray rowListForOneExp, Experiment exp) {
		for (Cell cell : exp.cageBox.cellList) {
			int cellNumber = Integer.valueOf(cell.cellRoi2D.getName().substring(4));
			int ilastalive = 0;
			if (cell.cellNFlies > 0) {
				Experiment expi = exp;
				while (expi.chainToNextExperiment != null
						&& expi.chainToNextExperiment.cageBox.isFlyAlive(cellNumber)) {
					expi = expi.chainToNextExperiment;
				}
				long lastIntervalFlyAlive_Ms = expi.cageBox.getLastIntervalFlyAlive(cellNumber)
						* expi.cageBox.detectBin_Ms;
				long lastMinuteAlive = lastIntervalFlyAlive_Ms + expi.camImageFirst_ms - expAll.camImageFirst_ms;
				ilastalive = (int) (lastMinuteAlive / options.buildExcelStepMs);
			}
			if (ilastalive > 0)
				ilastalive += 1;

//			for (FlyPositions row : rowsForOneExp) {
//				int rowCellNumber = Integer.valueOf(row.name.substring(4));
//				if (rowCellNumber == cellNumber) {
//					row.clearValues(ilastalive + 1);
//				}
			for (int iRow = 0; iRow < rowListForOneExp.size(); iRow++) {
				XLSResults row = rowListForOneExp.getRow(iRow);
				int rowCellNumber = Integer.valueOf(row.name.substring(4));
				if (rowCellNumber == cellNumber)
					row.clearValues(ilastalive);
			}
		}
	}

//	private Point writeData2(XLSResultsArray rowListForOneExp, XSSFSheet sheet, EnumXLSExportType option, Point pt_main) {
//		int rowseries = pt_main.x + 2;
//		int columndataarea = pt_main.y;
//		Point pt = new Point(pt_main);
//		writeRows(sheet, columndataarea, rowseries, pt);
//		pt_main.x = pt.x + 1;
//		return pt_main;
//	}

//	private void writeRows(XSSFSheet sheet, int column_dataArea, int rowSeries, Point pt) {
//		boolean transpose = options.transpose;
//		for (FlyPositions row : rowsForOneExp) {
//			pt.y = column_dataArea;
//			int col = getRowIndexFromCellName(row.name) * 2;
//			pt.x = rowSeries + col;
//			if (row.nflies < 1)
//				continue;
//
//			long last = expAll.camImageLast_ms - expAll.camImageFirst_ms;
//			if (options.fixedIntervals)
//				last = options.endAll_Ms - options.startAll_Ms;
//
//			for (long coltime = 0; coltime <= last; coltime += options.buildExcelStepMs, pt.y++) {
//				int i_from = (int) (coltime / options.buildExcelStepMs);
//				if (i_from >= row.flyPositionList.size())
//					break;
//
//				double valueL = Double.NaN;
//				double valueR = Double.NaN;
//				FlyPosition pos = row.flyPositionList.get(i_from);
//
//				switch (row.exportType) {
//				case DISTANCE:
//					valueL = pos.distance;
//					valueR = valueL;
//					break;
//				case ISALIVE:
//					valueL = pos.bAlive ? 1 : 0;
//					valueR = valueL;
//					break;
//				case SLEEP:
//					valueL = pos.bSleep ? 1 : 0;
//					valueR = valueL;
//					break;
//				case XYTOPCAGE:
//				case XYTIPCAPS:
//				case XYIMAGE:
//					valueL = pos.rectPosition.getX() + pos.rectPosition.getWidth() / 2.;
//					valueR = pos.rectPosition.getY() + pos.rectPosition.getHeight() / 2.;
//					break;
//				case ELLIPSEAXES:
//					valueL = pos.axis1;
//					valueR = pos.axis2;
//					break;
//				default:
//					break;
//				}
//
//				if (!Double.isNaN(valueL)) {
//					XLSUtils.setValue(sheet, pt, transpose, valueL);
//					if (pos.bPadded)
//						XLSUtils.getCell(sheet, pt, transpose).setCellStyle(xssfCellStyle_red);
//				}
//				if (!Double.isNaN(valueR)) {
//					pt.x++;
//					XLSUtils.setValue(sheet, pt, transpose, valueR);
//					if (pos.bPadded)
//						XLSUtils.getCell(sheet, pt, transpose).setCellStyle(xssfCellStyle_red);
//					pt.x--;
//				}
//			}
//			pt.x += 2;
//		}
//	}

	private XLSResultsArray getMoveDescriptorsForOneExperiment(Experiment exp, EnumXLSExportType xlsOption) {
		if (expAll == null)
			return null;

		// loop to get all capillaries into expAll and init rows for this experiment
		expAll.cageBox.copy(exp.cageBox);
		expAll.capillaries.copy(exp.capillaries);
		expAll.firstImage_FileTime = exp.firstImage_FileTime;
		expAll.lastImage_FileTime = exp.lastImage_FileTime;
		expAll.chainImageFirst_ms = exp.chainImageFirst_ms;
		expAll.copyExperimentFields(exp);
		expAll.setExperimentDirectory(exp.getExperimentDirectory());

		Experiment expi = exp.chainToNextExperiment;
		while (expi != null) {
			expAll.cageBox.mergeLists(expi.cageBox);
			expAll.lastImage_FileTime = expi.lastImage_FileTime;
			expi = expi.chainToNextExperiment;
		}

		expAll.camImageFirst_ms = expAll.firstImage_FileTime.toMillis();
		expAll.camImageLast_ms = expAll.lastImage_FileTime.toMillis();
		int nFrames = (int) ((expAll.camImageLast_ms - expAll.camImageFirst_ms) / options.buildExcelStepMs + 1);
		int ncells = expAll.cageBox.cellList.size();

		XLSResultsArray rowListForOneExp = new XLSResultsArray(ncells);

		for (int i = 0; i < ncells; i++) {
			Cell cell = expAll.cageBox.cellList.get(i);
//			cell.flyPositions.checkIsAliveFromAliveArray();
//			FlyPositions row = new FlyPositions(cell.cellRoi2D.getName(), xlsOption, nFrames, options.buildExcelStepMs);
//			row.nflies = cell.cellNFlies;
//			rowsForOneExp.add(row);
			XLSResults row = new XLSResults(cell.cellRoi2D.getName(), cell.cellNFlies, cell.getCellNumberInteger(),
					xlsOption, nFrames);
			/* add positions to results row! */
//			row.stimulus = cell.cellStimulus;
//			row.concentration = cap.capConcentration;
//			row.cellID = cap.capCellID;
			rowListForOneExp.addRow(row);
		}
//		Collections.sort(rowsForOneExp, new Comparators.XYTaSeries_Name_Comparator());
		rowListForOneExp.sortRowsByName();
		return rowListForOneExp;
	}

	protected Point writeExperiment_Cell_descriptors(Experiment exp, String charSeries, XSSFSheet sheet, Point pt,
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
		String sheetName = sheet.getSheetName();

		int rowmax = -1;
		for (EnumXLSColumnHeader dumb : EnumXLSColumnHeader.values()) {
			if (rowmax < dumb.getValue())
				rowmax = dumb.getValue();
		}

		List<Cell> cellList = exp.cageBox.cellList;
		for (int index = 0; index < cellList.size(); index++) {
			Cell cell = cellList.get(index);
			int col = getRowIndexFromCellName(cell.getRoiName());
			if (col >= 0)
				pt.x = colseries + col;
			int x = pt.x;
			int y = row;

			XLSExportExperimentParameters(sheet, transpose, x, y, exp);
			XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.DUM4.getValue(), transpose, sheetName);
			XLSExportCellParameters(sheet, transpose, x, y, charSeries, exp, cell);
		}
		pt.x = col0;
		pt.y = rowmax + 1;
		return pt;
	}
}
