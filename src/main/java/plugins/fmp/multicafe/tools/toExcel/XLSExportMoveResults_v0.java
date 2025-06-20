package plugins.fmp.multicafe.tools.toExcel;

import java.awt.Point;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFSheet;

import icy.gui.frame.progress.ProgressFrame;
import plugins.fmp.multicafe.experiment.CombinedExperiment;
import plugins.fmp.multicafe.experiment.Experiment;
import plugins.fmp.multicafe.experiment.cells.Cell;
import plugins.fmp.multicafe.experiment.cells.FlyPosition;
import plugins.fmp.multicafe.experiment.cells.FlyPositions;

public class XLSExportMoveResults_v0 extends XLSExport {

	public void o1_exportToFile(String filename, XLSExportOptions opt) {
		System.out.println("XLSExpoportMove:exportToFile() start output");
		options = opt;
		expList = options.expList;

		boolean loadCapillaries = true;
		boolean loadDrosoTrack = true;
		expList.loadListOfMeasuresFromAllExperiments(loadCapillaries, loadDrosoTrack);
		expList.chainExperimentsUsingKymoIndexes(options.collateSeries);
		expList.setFirstImageForAllExperiments(options.collateSeries);
		expAll = expList.get_MsTime_of_StartAndEnd_AllExperiments(options);
		expList.maxSizeOfCellArrays = expAll.cells.cellList.size();
		ArrayList<Experiment> listExperiments = expList.getExperimentsAsList();

		ProgressFrame progress = new ProgressFrame("Export data to Excel");
		int nbexpts = expList.getItemCount();
		progress.setLength(nbexpts);

		try {
			int xlsRow = 1;
			int iSeries = 0;
			workbook = xlsInitWorkbook();
			for (int index = options.firstExp; index <= options.lastExp; index++) {
				Experiment exp = expList.getItemAt(index);
				if (exp.chainToPreviousExperiment != null)
					continue;

				CombinedExperiment expCombined = new CombinedExperiment(exp, options.collateSeries);
				expCombined.loadExperimentDescriptors();
				expCombined.loadFlyPositions();

				progress.setMessage("Export experiment " + (index + 1) + " of " + nbexpts);
				String charSeries = CellReference.convertNumToColString(iSeries);

				if (options.xyImage)
					o1_getMoveDataAndExport(expCombined, xlsRow, charSeries, EnumXLSExportType.XYIMAGE);
				if (options.xyCell)
					o1_getMoveDataAndExport(expCombined, xlsRow, charSeries, EnumXLSExportType.XYTOPCELL);
				if (options.xyCapillaries)
					o1_getMoveDataAndExport(expCombined, xlsRow, charSeries, EnumXLSExportType.XYTIPCAPS);
				if (options.ellipseAxes)
					o1_getMoveDataAndExport(expCombined, xlsRow, charSeries, EnumXLSExportType.ELLIPSEAXES);
				if (options.distance)
					o1_getMoveDataAndExport(expCombined, xlsRow, charSeries, EnumXLSExportType.DISTANCE);
				if (options.alive)
					o1_getMoveDataAndExport(expCombined, xlsRow, charSeries, EnumXLSExportType.ISALIVE);
				if (options.sleep)
					o1_getMoveDataAndExport(expCombined, xlsRow, charSeries, EnumXLSExportType.SLEEP);

				if (!options.collateSeries || exp.chainToPreviousExperiment == null)
					xlsRow += expList.maxSizeOfCellArrays + 2; // TODO check - may be:
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

	private int o1_getMoveDataAndExport(Experiment exp, int col0, String charSeries, EnumXLSExportType xlsExport) {
		XSSFSheet sheet = xlsGetSheet(xlsExport.toString(), xlsExport);

		XLSResultsArray moveDescriptorsForOneExp = getMoveDescriptorsForOneExperiment(exp, xlsExport);

		// TODO
		XLSResultsArray rowListForOneExp = new XLSResultsArray();
		o2_getMoveDataFromOneExperimentSeries(exp, xlsExport);
		int colmax = xlsExportMoveResultsArrayToSheet(rowListForOneExp, sheet, xlsExport, col0, charSeries);

		if (options.onlyalive) {
			trimDeadsFromRowMoveData(rowListForOneExp, exp);

			sheet = xlsGetSheet(xlsExport.toString() + "_alive", xlsExport);
			xlsExportMoveResultsArrayToSheet(rowListForOneExp, sheet, xlsExport, col0, charSeries);
		}

		return colmax;
	}

	protected int xlsExportMoveResultsArrayToSheet(XLSResultsArray rowListForOneExp, XSSFSheet sheet,
			EnumXLSExportType xlsExportOption, int col0, String charSeries) {
		Point pt = new Point(col0, 0);
		// writeExperiment_Cell_descriptors(expAll, charSeries, sheet, pt,
		// xlsExportOption);
		// pt = writeData2(rowListForOneExp, sheet, xlsExportOption, pt);
		pt = writeExperiment_data(rowListForOneExp, sheet, xlsExportOption, pt);
		return pt.x;
	}

	private void o2_getMoveDataFromOneExperimentSeries(Experiment exp, EnumXLSExportType xlsExport) {
		Experiment expi = exp.getFirstChainedExperiment(true);
		// List<FlyPositions> dummyPositionsArrayList = new ArrayList<FlyPositions>(0);
		while (expi != null) {
			int nframes = 1 + (int) (expi.camImageLast_ms - expi.camImageFirst_ms) / options.buildExcelStepMs;
			if (nframes == 0)
				continue;

			double pixelsize = 32. / expi.capillaries.capillariesList.get(0).capPixels;
			List<FlyPositions> positionsArrayList = FlyPositions.computeMoveResults(expi, xlsExport, options, nframes,
					pixelsize);
			// here add resultsArrayList to expAll
			addMoveResultsTo_rowsForOneExp(expi, positionsArrayList);
			// addResultsTo_rowsForOneExp(rowListForOneExp, expi, positionsArrayList);
			expi = expi.chainToNextExperiment;
		}

//		XLSResultsArray xlsResultsArray = combine(moveDescriptorsForOneExp, dummyPositionsArrayList);
//		return positionsArrayList;
	}

//	private XLSResultsArray combine(XLSResultsArray moveDescriptorsForOneExp, List<FlyPositions> positionsArrayList) {
//		return moveDescriptorsForOneExp;
//	}

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

	private List<FlyPositions> addMoveResultsTo_rowsForOneExp(Experiment expi, List<FlyPositions> rowsForOneExp) {
		long start_Ms = expi.camImageFirst_ms - expAll.camImageFirst_ms;
		long end_Ms = expi.camImageLast_ms - expAll.camImageFirst_ms;
		if (options.fixedIntervals) {
			if (start_Ms < options.startAll_Ms)
				start_Ms = options.startAll_Ms;
			if (start_Ms > expi.camImageLast_ms)
				return rowsForOneExp;

			if (end_Ms > options.endAll_Ms)
				end_Ms = options.endAll_Ms;
			if (end_Ms > expi.camImageFirst_ms)
				return rowsForOneExp;
		}

		final long from_first_Ms = start_Ms + expAll.camImageFirst_ms;
		final long from_lastMs = end_Ms + expAll.camImageFirst_ms;
		final int to_first_index = (int) (from_first_Ms - expAll.camImageFirst_ms) / options.buildExcelStepMs;
		final int to_nvalues = (int) ((from_lastMs - from_first_Ms) / options.buildExcelStepMs) + 1;

		for (FlyPositions rowFlyPositions : rowsForOneExp) {
			FlyPositions results = getResultsArrayWithThatName(rowFlyPositions.name, rowsForOneExp);
			if (results != null) {
				if (options.collateSeries && options.padIntervals && expi.chainToPreviousExperiment != null)
					padFlyPositionsWithLastPreviousValue(rowFlyPositions, to_first_index);

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
					FlyPosition posok = padFlyPositionsWithLastPreviousValue(rowFlyPositions, to_first_index);
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
		return rowsForOneExp;
	}

	private FlyPosition padFlyPositionsWithLastPreviousValue(FlyPositions flyPositions, int transfer_first_index) {
		FlyPosition posok = null;
		int index = getIndexOfFirstNonEmptyValueBackwards(flyPositions, transfer_first_index);
		if (index >= 0) {
			posok = flyPositions.flyPositionList.get(index);
			for (int i = index + 1; i < transfer_first_index; i++) {
				FlyPosition pos = flyPositions.flyPositionList.get(i);
				pos.copy(posok);
				pos.bPadded = true;
			}
		}
		return posok;
	}

	private int getIndexOfFirstNonEmptyValueBackwards(FlyPositions flyPositions, int fromindex) {
		int index = -1;
		for (int i = fromindex; i >= 0; i--) {
			FlyPosition pos = flyPositions.flyPositionList.get(i);
			if (!Double.isNaN(pos.x)) {
				index = i;
				break;
			}
		}
		return index;
	}

	private void trimDeadsFromRowMoveData(XLSResultsArray rowListForOneExp, Experiment exp) {
		for (Cell cell : exp.cells.cellList) {
			int cellNumber = Integer.valueOf(cell.cellRoi2D.getName().substring(4));
			int ilastalive = 0;
			if (cell.cellNFlies > 0) {
				Experiment expi = exp;
				while (expi.chainToNextExperiment != null && expi.chainToNextExperiment.cells.isFlyAlive(cellNumber)) {
					expi = expi.chainToNextExperiment;
				}
				long lastIntervalFlyAlive_Ms = expi.cells.getLastIntervalFlyAlive(cellNumber) * expi.cells.detectBin_Ms;
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

		expAll.cells.copy(exp.cells);
		expAll.capillaries.copy(exp.capillaries);
		expAll.firstImage_FileTime = exp.firstImage_FileTime;
		expAll.lastImage_FileTime = exp.lastImage_FileTime;
		expAll.chainImageFirst_ms = exp.chainImageFirst_ms;
		expAll.copyExperimentFields(exp);
		expAll.setExperimentDirectory(exp.getExperimentDirectory());

		Experiment expi = exp.chainToNextExperiment;
		while (expi != null) {
			expAll.cells.mergeLists(expi.cells);
			expAll.lastImage_FileTime = expi.lastImage_FileTime;
			expi = expi.chainToNextExperiment;
		}

		expAll.camImageFirst_ms = expAll.firstImage_FileTime.toMillis();
		expAll.camImageLast_ms = expAll.lastImage_FileTime.toMillis();
		int nFrames = (int) ((expAll.camImageLast_ms - expAll.camImageFirst_ms) / options.buildExcelStepMs + 1);
		int ncells = expAll.cells.cellList.size();

		XLSResultsArray rowListForOneExp = new XLSResultsArray(ncells);

		for (int i = 0; i < ncells; i++) {
			Cell cell = expAll.cells.cellList.get(i);
			cell.flyPositions.checkIsAliveFromAliveArray();
//			FlyPositions row = new FlyPositions(cell.cellRoi2D.getName(), xlsOption, nFrames, options.buildExcelStepMs);

			XLSResults row = new XLSResults(cell.cellRoi2D.getName(), cell.cellNFlies, cell.getCellNumberInteger(),
					xlsOption, nFrames);
			/* add positions to results row! */

			rowListForOneExp.addRow(row);
		}
		rowListForOneExp.sortRowsByName();
		return rowListForOneExp;
	}

}
