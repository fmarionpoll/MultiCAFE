package plugins.fmp.multicafe.tools.toExcel;

import java.awt.Point;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFSheet;

import icy.gui.frame.progress.ProgressFrame;
import plugins.fmp.multicafe.experiment.CombinedExperiment;
import plugins.fmp.multicafe.experiment.Experiment;
import plugins.fmp.multicafe.experiment.cells.Cell;
import plugins.fmp.multicafe.experiment.cells.FlyPosition;
import plugins.fmp.multicafe.experiment.cells.FlyPositions;

public class XLSExportMoveResults extends XLSExport {
	//
	public void exportToFile(String filename, XLSExportOptions opt) {
		System.out.println("XLSExpoportMove:exportToFile() start output");
		options = opt;
		expList = options.expList;

		boolean loadCapillaries = true;
		boolean loadDrosoTrack = options.onlyalive;// true;
		expList.loadListOfMeasuresFromAllExperiments(loadCapillaries, loadDrosoTrack);
		expList.chainExperimentsUsingKymoIndexes(options.collateSeries);
		expList.setFirstImageForAllExperiments(options.collateSeries);
		expAll = expList.get_MsTime_of_StartAndEnd_AllExperiments(options);

		ProgressFrame progress = new ProgressFrame("Export data to Excel");
		int nbexpts = expList.getItemCount();
		progress.setLength(nbexpts);

		try {
			int iSeries = 0;
			workbook = xlsInitWorkbook();
			for (int index = options.firstExp; index <= options.lastExp; index++) {
				Experiment exp = expList.getItemAt(index);
				if (exp.chainToPreviousExperiment != null)
					continue;

				CombinedExperiment combinedExp = new CombinedExperiment(exp);
				combinedExp.setCollateExperimentsOption(options.collateSeries);
				combinedExp.loadExperimentDescriptors();
				combinedExp.loadExperimentCamFileNames();
				combinedExp.loadFlyPositions();

				progress.setMessage("Export experiment " + (index + 1) + " of " + nbexpts);
				String charSeries = CellReference.convertNumToColString(iSeries);

				if (options.xyImage)
					exportMoveDataFromExpCombined(combinedExp, charSeries, options, EnumXLSExportType.XYIMAGE);
				if (options.xyCell)
					exportMoveDataFromExpCombined(combinedExp, charSeries, options, EnumXLSExportType.XYTOPCELL);
				if (options.xyCapillaries)
					exportMoveDataFromExpCombined(combinedExp, charSeries, options, EnumXLSExportType.XYTIPCAPS);
				if (options.ellipseAxes)
					exportMoveDataFromExpCombined(combinedExp, charSeries, options, EnumXLSExportType.ELLIPSEAXES);
				if (options.distance)
					exportMoveDataFromExpCombined(combinedExp, charSeries, options, EnumXLSExportType.DISTANCE);
				if (options.alive)
					exportMoveDataFromExpCombined(combinedExp, charSeries, options, EnumXLSExportType.ISALIVE);
				if (options.sleep)
					exportMoveDataFromExpCombined(combinedExp, charSeries, options, EnumXLSExportType.SLEEP);

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

	private void exportMoveDataFromExpCombined(CombinedExperiment combinedExp, String charSeries,
			XLSExportOptions options, EnumXLSExportType xlsExportOption) {
		XSSFSheet sheet = xlsGetSheet(xlsExportOption.toString(), xlsExportOption);
		CellAddress cellAddress = sheet.getActiveCell();
		int x = cellAddress.getRow();
		int y = 0;
		x = writeSeparator_Between_Experiments(sheet, new Point(x, y), options.transpose);

		ArrayList<EnumMeasure> measures = xlsExportOption.toMeasures();
		List<Cell> cellList = combinedExp.cells.cellList;

		for (int index = 0; index < cellList.size(); index++) {
			Cell cell = cellList.get(index);
			for (int j = 0; j < measures.size(); j++) {
				y = 0;
				XLSExportExperimentParameters(sheet, options.transpose, x, y, combinedExp);
				XLSExportCellParameters(sheet, options.transpose, x, y, charSeries, combinedExp, cell);
				y += EnumXLSColumnHeader.DUM4.getValue();
				XLSUtils.setValue(sheet, x, y, options.transpose, measures.get(j).toString());
				y++;
				x++;
			}
		}
		sheet.setActiveCell(new CellAddress(x, y));
	}

	private void writeData(XSSFSheet sheet, CombinedExperiment expCombined, int column_dataArea, int rowSeries,
			Point pt) {
		boolean transpose = options.transpose;
		for (FlyPositions row : rowsForOneExp) {
			pt.y = column_dataArea;
			int col = getRowIndexFromCellName(row.name) * 2;
			pt.x = rowSeries + col;
			if (row.nflies < 1)
				continue;

			long last = expAll.camImageLast_ms - expAll.camImageFirst_ms;
			if (options.fixedIntervals)
				last = options.endAll_Ms - options.startAll_Ms;

			for (long coltime = 0; coltime <= last; coltime += options.buildExcelStepMs, pt.y++) {
				int i_from = (int) (coltime / options.buildExcelStepMs);
				if (i_from >= row.flyPositionList.size())
					break;

				double valueL = Double.NaN;
				double valueR = Double.NaN;
				FlyPosition pos = row.flyPositionList.get(i_from);

				switch (row.exportType) {
				case DISTANCE:
					valueL = pos.distance;
					valueR = valueL;
					break;
				case ISALIVE:
					valueL = pos.bAlive ? 1 : 0;
					valueR = valueL;
					break;
				case SLEEP:
					valueL = pos.bSleep ? 1 : 0;
					valueR = valueL;
					break;
				case XYTOPCAGE:
				case XYTIPCAPS:
				case XYIMAGE:
					valueL = pos.rectPosition.getX() + pos.rectPosition.getWidth() / 2.;
					valueR = pos.rectPosition.getY() + pos.rectPosition.getHeight() / 2.;
					break;
				case ELLIPSEAXES:
					valueL = pos.axis1;
					valueR = pos.axis2;
					break;
				default:
					break;
				}

				if (!Double.isNaN(valueL)) {
					XLSUtils.setValue(sheet, pt, transpose, valueL);
					if (pos.bPadded)
						XLSUtils.getCell(sheet, pt, transpose).setCellStyle(xssfCellStyle_red);
				}
				if (!Double.isNaN(valueR)) {
					pt.x++;
					XLSUtils.setValue(sheet, pt, transpose, valueR);
					if (pos.bPadded)
						XLSUtils.getCell(sheet, pt, transpose).setCellStyle(xssfCellStyle_red);
					pt.x--;
				}
			}
			pt.x += 2;
		}
	}

}
