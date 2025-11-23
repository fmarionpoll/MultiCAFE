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
import plugins.fmp.multicafe.experiment.cages.Cage;
import plugins.fmp.multicafe.experiment.cages.FlyPosition;

public class XLSExportMoveResults extends XLSExport {
	//
	public void exportToFile(String filename, XLSExportOptions opt) {
		System.out.println("XLSExpoportMove:exportToFile() start output");
		options = opt;
		expList = options.expList;

		boolean loadCapillaries = true;
		boolean loadDrosoTrack = true; // options.onlyalive;// true;
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

				CombinedExperiment combinedExp = new CombinedExperiment(exp, options.collateSeries);
				combinedExp.loadExperimentDescriptors();
				combinedExp.loadExperimentCamFileNames();
				combinedExp.loadFlyPositions();

				progress.setMessage("Export experiment " + (index + 1) + " of " + nbexpts);
				String charSeries = CellReference.convertNumToColString(iSeries);

				if (options.xyImage)
					exportMoveDataFromExpCombined(combinedExp, charSeries, options, EnumXLSExport.XYIMAGE);
				if (options.xyCage)
					exportMoveDataFromExpCombined(combinedExp, charSeries, options, EnumXLSExport.XYTOPCELL);
				if (options.xyCapillaries)
					exportMoveDataFromExpCombined(combinedExp, charSeries, options, EnumXLSExport.XYTIPCAPS);
				if (options.ellipseAxes)
					exportMoveDataFromExpCombined(combinedExp, charSeries, options, EnumXLSExport.ELLIPSEAXES);
				if (options.distance)
					exportMoveDataFromExpCombined(combinedExp, charSeries, options, EnumXLSExport.DISTANCE);
				if (options.alive)
					exportMoveDataFromExpCombined(combinedExp, charSeries, options, EnumXLSExport.ISALIVE);
				if (options.sleep)
					exportMoveDataFromExpCombined(combinedExp, charSeries, options, EnumXLSExport.SLEEP);

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
			XLSExportOptions options, EnumXLSExport xlsExportOption) {
		XSSFSheet sheet = xlsGetSheet(xlsExportOption.toString(), xlsExportOption);
		CellAddress cellAddress = sheet.getActiveCell();
		int x = cellAddress.getRow();
		int y = 0;
		x = writeSeparator_Between_Experiments(sheet, new Point(x, y), options.transpose);

		ArrayList<EnumMeasure> measures = xlsExportOption.toMeasures();
		List<Cage> cellList = combinedExp.cages.cageList;

		for (int index = 0; index < cellList.size(); index++) {
			Cage cell = cellList.get(index);
			for (int j = 0; j < measures.size(); j++) {
				y = 0;
				XLSExportExperimentParameters(sheet, options.transpose, x, y, charSeries, combinedExp);
				xlsExportCageParameters(sheet, options.transpose, x, y, charSeries, combinedExp, cell);
				y += EnumXLSColumnHeader.DUM4.getValue();
				XLSUtils.setValue(sheet, x, y, options.transpose, measures.get(j).toString());
				y++;
				writeData(sheet, cell, x, y, measures.get(j));
				x++;
			}
		}
		sheet.setActiveCell(new CellAddress(x, y));
	}

	private void writeData(XSSFSheet sheet, Cage cell, int x, int y, EnumMeasure exportType) {
		boolean transpose = options.transpose;

		Point pt = new Point(x, y);
		if (cell.cageNFlies < 1)
			return;

		long last = expAll.getCamImageLast_ms() - expAll.getCamImageFirst_ms();
		if (options.fixedIntervals)
			last = options.endAll_Ms - options.startAll_Ms;
		if (exportType == EnumMeasure.TI)
			cell.flyPositions.computeDistanceBetweenConsecutivePoints();
		else if (exportType == EnumMeasure.SLEEP)
			cell.flyPositions.computeSleep();
		else if (exportType == EnumMeasure.ALIVE)
			cell.flyPositions.computeIsAlive();

		for (long coltime = 0; coltime <= last; coltime += options.buildExcelStepMs, pt.y++) {
			int i_from = (int) (coltime / options.buildExcelStepMs);
			if (i_from >= cell.flyPositions.flyPositionList.size())
				break;

			double value = Double.NaN;
			FlyPosition pos = cell.flyPositions.flyPositionList.get(i_from);

			switch (exportType) {
			case TI:
				value = pos.flyIndexT;
				break;
			case TS:
				value = pos.tMs / 60000.;
				break;
			case X:
				value = pos.x;
				break;
			case Y:
				value = pos.y;
				break;
			case W:
				value = pos.w;
				break;
			case H:
				value = pos.h;
				break;
			case DISTANCE:
				value = pos.distance;
				break;
			case ALIVE:
				value = pos.bAlive ? 1 : 0;
				break;
			case SLEEP:
				value = pos.bSleep ? 1 : 0;
				break;

			default:
				break;
			}

			if (!Double.isNaN(value)) {
				XLSUtils.setValue(sheet, pt, transpose, value);
				if (pos.bPadded)
					XLSUtils.getCell(sheet, pt, transpose).setCellStyle(xssfCellStyle_red);
			}

		}
		pt.x += 2;

	}

}
