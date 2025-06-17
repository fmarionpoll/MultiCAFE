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

				CombinedExperiment expCombined = new CombinedExperiment(exp);
				expCombined.setCollateExperimentsOption(options.collateSeries);
				expCombined.loadExperimentDescriptors();
				expCombined.loadExperimentCamFileNames();
				expCombined.loadFlyPositions();

				progress.setMessage("Export experiment " + (index + 1) + " of " + nbexpts);
				String charSeries = CellReference.convertNumToColString(iSeries);

				if (options.xyImage)
					exportMoveDataFromExpCombined(expCombined, charSeries, options, EnumXLSExportType.XYIMAGE);
				if (options.xyCell)
					exportMoveDataFromExpCombined(expCombined, charSeries, options, EnumXLSExportType.XYTOPCELL);
				if (options.xyCapillaries)
					exportMoveDataFromExpCombined(expCombined, charSeries, options, EnumXLSExportType.XYTIPCAPS);
				if (options.ellipseAxes)
					exportMoveDataFromExpCombined(expCombined, charSeries, options, EnumXLSExportType.ELLIPSEAXES);
				if (options.distance)
					exportMoveDataFromExpCombined(expCombined, charSeries, options, EnumXLSExportType.DISTANCE);
				if (options.alive)
					exportMoveDataFromExpCombined(expCombined, charSeries, options, EnumXLSExportType.ISALIVE);
				if (options.sleep)
					exportMoveDataFromExpCombined(expCombined, charSeries, options, EnumXLSExportType.SLEEP);

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

	private void exportMoveDataFromExpCombined(Experiment exp, String charSeries, XLSExportOptions options,
			EnumXLSExportType xlsExportOption) {
		XSSFSheet sheet = xlsGetSheet(xlsExportOption.toString(), xlsExportOption);
		CellAddress cellAddress = sheet.getActiveCell();
		int x = cellAddress.getRow();
		int y = 0;
		x = writeSeparator_Between_Experiments(sheet, new Point(x, y), options.transpose);

		ArrayList<EnumMeasure> measures = xlsExportOption.toMeasures();
		List<Cell> cellList = exp.cells.cellList;

		for (int index = 0; index < cellList.size(); index++) {
			Cell cell = cellList.get(index);
			for (int j = 0; j < measures.size(); j++) {
				XLSExportExperimentParameters(sheet, options.transpose, x, y, exp);
				XLSExportCellParameters(sheet, options.transpose, x, y, charSeries, exp, cell);
				XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.DUM4.getValue(), options.transpose,
						measures.get(j).toString());
				x++;
			}
		}
		sheet.setActiveCell(new CellAddress(x, y));
	}

}
