package plugins.fmp.multicafe.fmp_tools.toExcel;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.xssf.streaming.SXSSFSheet;

import plugins.fmp.multicafe.fmp_experiment.Experiment;
import plugins.fmp.multicafe.fmp_experiment.cages.Cage;
import plugins.fmp.multicafe.fmp_experiment.capillaries.Capillary;
import plugins.fmp.multicafe.fmp_tools.results.EnumResults;
import plugins.fmp.multicafe.fmp_tools.results.Results;
import plugins.fmp.multicafe.fmp_tools.results.ResultsArray;
import plugins.fmp.multicafe.fmp_tools.results.ResultsArrayFromCapillaries;
import plugins.fmp.multicafe.fmp_tools.results.ResultsOptions;
import plugins.fmp.multicafe.fmp_tools.toExcel.config.ExcelExportConstants;
import plugins.fmp.multicafe.fmp_tools.toExcel.enums.EnumXLSColumnHeader;
import plugins.fmp.multicafe.fmp_tools.toExcel.exceptions.ExcelExportException;
import plugins.fmp.multicafe.fmp_tools.toExcel.exceptions.ExcelResourceException;
import plugins.fmp.multicafe.fmp_tools.toExcel.utils.XLSUtils;

/**
 * Excel export implementation for capillary measurements. Uses the Template
 * Method pattern for structured export operations, following the same pattern
 * as spot exports.
 * 
 * <p>
 * This class exports capillary data including:
 * <ul>
 * <li>TOPRAW - Raw top liquid level</li>
 * <li>TOPLEVEL - Top liquid level compensated for evaporation</li>
 * <li>TOPLEVEL_LR - Volume consumed in capillaries per cage</li>
 * <li>DERIVEDVALUES - Derived top liquid level</li>
 * <li>BOTTOMLEVEL - Bottom liquid level</li>
 * <li>TOPLEVELDELTA - Top liquid consumed (t - t-1)</li>
 * </ul>
 * </p>
 * 
 * @author MultiSPOTS96
 * @version 2.3.3
 */
public class XLSExportMeasuresFromCapillary extends XLSExport {

	/**
	 * Exports capillary data for a single experiment.
	 * 
	 * @param exp         The experiment to export
	 * @param startColumn The starting column for export
	 * @param charSeries  The series identifier
	 * @return The next available column
	 * @throws ExcelExportException If export fails
	 */
	@Override
	protected int exportExperimentData(Experiment exp, ResultsOptions resultsOptions, int startColumn,
			String charSeries) throws ExcelExportException {

		List<EnumResults> resultsToExport = new ArrayList<EnumResults>();

		if (options.topLevel) {
			resultsToExport.add(EnumResults.TOPRAW);
			resultsToExport.add(EnumResults.TOPLEVEL);
		}

		if (options.lrPI) {
			resultsToExport.add(EnumResults.TOPLEVEL_LR);
		}

		if (options.bottomLevel) {
			resultsToExport.add(EnumResults.BOTTOMLEVEL);
		}
		if (options.derivative) {
			resultsToExport.add(EnumResults.DERIVEDVALUES);
		}

		int colmax = 0;
		exp.dispatchCapillariesToCages();
		for (EnumResults resultType : resultsToExport) {
			int col = getCapDataAndExport(exp, startColumn, charSeries, resultType);
			if (col > colmax)
				colmax = col;
		}

		return colmax;
	}

	protected int getCapDataAndExport(Experiment exp, int col0, String charSeries, EnumResults resultType)
			throws ExcelExportException {
		try {
			options.resultType = resultType;
			SXSSFSheet sheet = getSheet(resultType.toString(), resultType);
			int colmax = xlsExportExperimentCapDataToSheet(exp, sheet, resultType, col0, charSeries);
			if (options.onlyalive) {
				sheet = getSheet(resultType.toString() + ExcelExportConstants.ALIVE_SHEET_SUFFIX, resultType);
				xlsExportExperimentCapDataToSheet(exp, sheet, resultType, col0, charSeries);
			}

			return colmax;
		} catch (ExcelResourceException e) {
			throw new ExcelExportException("Failed to export spot data", "get_spot_data_and_export",
					resultType.toString(), e);
		}
	}

	protected int xlsExportExperimentCapDataToSheet(Experiment exp, SXSSFSheet sheet, EnumResults resultType, int col0,
			String charSeries) {
		Point pt = new Point(col0, 0);
		pt = writeExperimentSeparator(sheet, pt);

		ResultsOptions resultsOptions = new ResultsOptions();
		long kymoBin_ms = exp.getKymoBin_ms();
		if (kymoBin_ms <= 0) {
			kymoBin_ms = 60000;
		}
		resultsOptions.buildExcelStepMs = (int) kymoBin_ms;
		resultsOptions.relativeToMaximum = false;
		resultsOptions.subtractT0 = false;
		resultsOptions.correctEvaporation = resultType == EnumResults.TOPLEVEL ? true : false;
		resultsOptions.resultType = resultType;

		ResultsArrayFromCapillaries resultsFromCapillaries = new ResultsArrayFromCapillaries(0);
		ResultsArray resultsArray = resultsFromCapillaries.getMeasuresFromAllCapillaries(exp, resultsOptions);

		for (Results results : resultsArray.getList()) {

			String capName = results.getName();
			Capillary cap = exp.getCapillaries().getCapillaryFromKymographName(capName);
			Cage cage = exp.getCages().getCageFromID(cap.getCageID());

			pt.y = 0;
			pt = writeExperimentCapInfos(sheet, pt, exp, charSeries, cage, cap, resultType);
			writeXLSResult(sheet, pt, results);
			pt.x++;
		}
		return pt.x;
	}

	/**
	 * Writes experiment capillary information to the sheet.
	 * 
	 * @param sheet      The sheet to write to
	 * @param pt         The starting point
	 * @param exp        The experiment
	 * @param charSeries The series identifier
	 * @param capillary  The capillary
	 * @param resultType The export type
	 * @return The updated point
	 */
	protected Point writeExperimentCapInfos(SXSSFSheet sheet, Point pt, Experiment exp, String charSeries, Cage cage,
			Capillary capillary, EnumResults resultType) {

		boolean transpose = options.transpose;

		writeFileInformation(sheet, pt, transpose, exp);
		writeExperimentProperties(sheet, pt, transpose, exp, charSeries);
		writeCageProperties(sheet, pt, transpose, cage);
		writeCapProperties(sheet, pt, transpose, capillary, charSeries, resultType);

		pt.y += getDescriptorRowCount();
		return pt;
	}

	private void writeCapProperties(SXSSFSheet sheet, Point pt, boolean transpose, Capillary capillary,
			String charSeries, EnumResults resultType) {
		XLSUtils.setValueAtColumn(sheet, pt, EnumXLSColumnHeader.CAP, transpose,
				capillary.getSideDescriptor(resultType));
		XLSUtils.setValueAtColumn(sheet, pt, EnumXLSColumnHeader.CAP_INDEX, transpose,
				charSeries + "_" + capillary.getLast2ofCapillaryName());
		XLSUtils.setValueAtColumn(sheet, pt, EnumXLSColumnHeader.CAP_VOLUME, transpose, capillary.capVolume);
		XLSUtils.setValueAtColumn(sheet, pt, EnumXLSColumnHeader.CAP_PIXELS, transpose, capillary.capPixels);
		XLSUtils.setValueAtColumn(sheet, pt, EnumXLSColumnHeader.CAP_STIM, transpose, capillary.capStimulus);
		XLSUtils.setValueAtColumn(sheet, pt, EnumXLSColumnHeader.CAP_CONC, transpose, capillary.capConcentration);
		XLSUtils.setValueAtColumn(sheet, pt, EnumXLSColumnHeader.CAP_NFLIES, transpose, capillary.capNFlies);
		XLSUtils.setValueAtColumn(sheet, pt, EnumXLSColumnHeader.DUM4, transpose, resultType.toString());
	}

}
