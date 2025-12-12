package plugins.fmp.multicafe.fmp_tools.toExcel;

import java.awt.Point;
import java.util.HashMap;
import java.util.Map;

import org.apache.poi.xssf.streaming.SXSSFSheet;

import plugins.fmp.multicafe.fmp_experiment.Experiment;
import plugins.fmp.multicafe.fmp_experiment.cages.Cage;
import plugins.fmp.multicafe.fmp_experiment.capillaries.Capillary;
import plugins.fmp.multicafe.fmp_tools.results.EnumResults;
import plugins.fmp.multicafe.fmp_tools.results.Results;
import plugins.fmp.multicafe.fmp_tools.results.ResultsFromCapillaries;
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

	// Track column position per sheet (each export type uses a different sheet)
	private Map<String, Integer> sheetColumns = new HashMap<String, Integer>();

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
		int maxColumn = startColumn;

		// Dispatch capillaries to cages for computation
		exp.dispatchCapillariesToCages();

		// Compute evaporation correction if needed (for TOPLEVEL exports)
		if (options.correctEvaporation && (options.topLevel || (options.lrPI && options.topLevel))) {
			exp.getCages().computeEvaporationCorrection(exp);
		}

		// Compute L+R measures if needed (must be done after evaporation correction)
		if (options.lrPI && options.topLevel) {
			exp.getCages().computeLRMeasures(exp, options.lrPIThreshold);
		}

		if (options.topLevel) {
			int col = getCapillaryDataAndExport(exp, getSheetColumn(EnumResults.TOPRAW.toString()), charSeries,
					EnumResults.TOPRAW, false);
			updateSheetColumn(EnumResults.TOPRAW.toString(), col);
			if (col > maxColumn)
				maxColumn = col;
			col = getCapillaryDataAndExport(exp, getSheetColumn(EnumResults.TOPLEVEL.toString()), charSeries,
					EnumResults.TOPLEVEL, true);
			updateSheetColumn(EnumResults.TOPLEVEL.toString(), col);
			if (col > maxColumn)
				maxColumn = col;
		}
		if (options.lrPI && options.topLevel) {
			int col = getCapillaryDataAndExport(exp, getSheetColumn(EnumResults.TOPLEVEL_LR.toString()), charSeries,
					EnumResults.TOPLEVEL_LR, true);
			updateSheetColumn(EnumResults.TOPLEVEL_LR.toString(), col);
			if (col > maxColumn)
				maxColumn = col;
		}
		if (options.topLevelDelta) {
			int col = getCapillaryDataAndExport(exp, getSheetColumn(EnumResults.TOPLEVELDELTA.toString()), charSeries,
					EnumResults.TOPLEVELDELTA, true);
			updateSheetColumn(EnumResults.TOPLEVELDELTA.toString(), col);
			if (col > maxColumn)
				maxColumn = col;
		}
		if (options.lrPI && options.topLevelDelta) {
			int col = getCapillaryDataAndExport(exp, getSheetColumn(EnumResults.TOPLEVELDELTA_LR.toString()), charSeries,
					EnumResults.TOPLEVELDELTA_LR, true);
			updateSheetColumn(EnumResults.TOPLEVELDELTA_LR.toString(), col);
			if (col > maxColumn)
				maxColumn = col;
		}
		if (options.bottomLevel) {
			int col = getCapillaryDataAndExport(exp, getSheetColumn(EnumResults.BOTTOMLEVEL.toString()), charSeries,
					EnumResults.BOTTOMLEVEL, false);
			updateSheetColumn(EnumResults.BOTTOMLEVEL.toString(), col);
			if (col > maxColumn)
				maxColumn = col;
		}
		if (options.derivative) {
			int col = getCapillaryDataAndExport(exp, getSheetColumn(EnumResults.DERIVEDVALUES.toString()), charSeries,
					EnumResults.DERIVEDVALUES, false);
			updateSheetColumn(EnumResults.DERIVEDVALUES.toString(), col);
			if (col > maxColumn)
				maxColumn = col;
		}

		return maxColumn;
	}

	private int getSheetColumn(String sheetName) {
		return sheetColumns.getOrDefault(sheetName, 1);
	}

	private void updateSheetColumn(String sheetName, int column) {
		sheetColumns.put(sheetName, column);
	}

	/**
	 * Exports capillary data for a specific export type.
	 * 
	 * @param exp        The experiment to export
	 * @param col0       The starting column
	 * @param charSeries The series identifier
	 * @param resultType The export type
	 * @param subtractT0 Whether to subtract T0 value
	 * @return The next available column
	 * @throws ExcelExportException If export fails
	 */
	protected int getCapillaryDataAndExport(Experiment exp, int col0, String charSeries, EnumResults resultType,
			boolean subtractT0) throws ExcelExportException {
		try {
			options.resultType = resultType;
			SXSSFSheet sheet = getSheet(resultType.toString(), resultType);
			int colmax = xlsExportExperimentCapillaryDataToSheet(exp, sheet, resultType, col0, charSeries, subtractT0);

			if (options.onlyalive) {
				sheet = getSheet(resultType.toString() + ExcelExportConstants.ALIVE_SHEET_SUFFIX, resultType);
				xlsExportExperimentCapillaryDataToSheet(exp, sheet, resultType, col0, charSeries, subtractT0);
			}

			return colmax;
		} catch (ExcelResourceException e) {
			throw new ExcelExportException("Failed to export capillary data", "get_capillary_data_and_export",
					resultType.toString(), e);
		}
	}

	/**
	 * Exports capillary data to a specific sheet.
	 * 
	 * @param exp           The experiment to export
	 * @param sheet         The sheet to write to
	 * @param resultType The export type
	 * @param col0          The starting column
	 * @param charSeries    The series identifier
	 * @param subtractT0    Whether to subtract T0 value
	 * @return The next available column
	 */
	protected int xlsExportExperimentCapillaryDataToSheet(Experiment exp, SXSSFSheet sheet, EnumResults resultType,
			int col0, String charSeries, boolean subtractT0) {
		Point pt = new Point(col0, 0);
		pt = writeExperimentSeparator(sheet, pt);

		double scalingFactorToPhysicalUnits = exp.getCapillaries().getScalingFactorToPhysicalUnits(resultType);

		// update cage structures from capillaries so that we can do operations within a
		// cage more easily (not yet implemented)
		exp.dispatchCapillariesToCages();
		for (Cage cage : exp.getCages().getCageList()) {

			for (Capillary capillary : cage.getCapillaries().getList()) {
				pt.y = 0;
				pt = writeExperimentCapillaryInfos(sheet, pt, exp, charSeries, cage, capillary, resultType);

				// Create a copy of options with the correct exportType for this specific export
				ResultsOptions resultsOptions = new ResultsOptions();
				resultsOptions.buildExcelStepMs = options.buildExcelStepMs;
				resultsOptions.relativeToT0 = options.relativeToT0;
				resultsOptions.correctEvaporation = options.correctEvaporation;
				resultsOptions.resultType = resultType; // Use the parameter, not the field

				Results xlsResults = ResultsFromCapillaries.getResultsFromCapillaryMeasures(exp, capillary,
						resultsOptions, subtractT0);
				xlsResults.transferDataValuesToValuesOut(scalingFactorToPhysicalUnits, resultType);
				writeXLSResult(sheet, pt, xlsResults);
				pt.x++;
			}
		}
		return pt.x;
	}

	/**
	 * Writes experiment capillary information to the sheet.
	 * 
	 * @param sheet         The sheet to write to
	 * @param pt            The starting point
	 * @param exp           The experiment
	 * @param charSeries    The series identifier
	 * @param capillary     The capillary
	 * @param resultType The export type
	 * @return The updated point
	 */
	protected Point writeExperimentCapillaryInfos(SXSSFSheet sheet, Point pt, Experiment exp, String charSeries,
			Cage cage, Capillary capillary, EnumResults resultType) {

		boolean transpose = options.transpose;

		writeFileInformation(sheet, pt, transpose, exp);
		writeExperimentProperties(sheet, pt, transpose, exp, charSeries);
		writeCageProperties(sheet, pt, transpose, cage);
		writeCapillaryProperties(sheet, pt, transpose, capillary, charSeries, resultType);

		pt.y += getDescriptorRowCount();
		return pt;
	}

	private void writeCapillaryProperties(SXSSFSheet sheet, Point pt, boolean transpose, Capillary capillary,
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
