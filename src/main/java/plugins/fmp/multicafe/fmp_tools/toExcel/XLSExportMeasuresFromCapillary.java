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
import plugins.fmp.multicafe.fmp_tools.results.ResultsArray;
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
		int column = startColumn;

		// Dispatch capillaries to cages for computation
		exp.dispatchCapillariesToCages();

		if (options.topLevel) {
			column = getCapillaryDataAndExport(exp, getSheetColumn(EnumResults.TOPRAW.toString()), charSeries,
					EnumResults.TOPRAW, false);

			column = getCapillaryDataAndExport(exp, getSheetColumn(EnumResults.TOPLEVEL.toString()), charSeries,
					EnumResults.TOPLEVEL, true);
		}

		if (options.lrPI && options.topLevel) {
			column = getCapillaryDataAndExport(exp, getSheetColumn(EnumResults.TOPLEVEL_LR.toString()), charSeries,
					EnumResults.TOPLEVEL_LR, true);
		}

		if (options.bottomLevel) {
			column = getCapillaryDataAndExport(exp, getSheetColumn(EnumResults.BOTTOMLEVEL.toString()), charSeries,
					EnumResults.BOTTOMLEVEL, false);
		}
		if (options.derivative) {
			column = getCapillaryDataAndExport(exp, getSheetColumn(EnumResults.DERIVEDVALUES.toString()), charSeries,
					EnumResults.DERIVEDVALUES, false);
		}

		return column;
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

		for (Cage cage : exp.getCages().cagesList) {
			double scalingFactorToPhysicalUnits = cage.spotsArray.getScalingFactorToPhysicalUnits(resultType);
			cage.updateSpotsStimulus_i();

			for (Capillary cap : cage.getCapillaries().getList()) {
				pt.y = 0;
				pt = writeExperimentCapillaryInfos(sheet, pt, exp, charSeries, cage, cap, resultType);
				Results results = getResultsDataValuesFromCapMeasures(exp, cage, cap, options);
				results.transferDataValuesToValuesOut(scalingFactorToPhysicalUnits, resultType);
				writeXLSResult(sheet, pt, results);
				pt.x++;
			}
		}
		return pt.x;
	}

	/**
	 * Gets the results for a spot.
	 * 
	 * @param exp           The experiment
	 * @param cage          The cage
	 * @param spot          The spot
	 * @param xlsExportType The export type
	 * @return The XLS results
	 */
	public Results getResultsDataValuesFromCapMeasures(Experiment exp, Cage cage, Capillary cap,
			ResultsOptions xlsExportOptions) {
		/*
		 * 1) get n input frames for signal between timefirst and time last; locate
		 * binfirst and bin last in the array of long in seqcamdata 2) given excelBinms,
		 * calculate n output bins
		 */
		int nOutputFrames = getNOutputFrames(exp, xlsExportOptions);

		Results results = new Results(cage.getProperties(), cap, nOutputFrames);

		long binData = exp.getSeqCamData().getTimeManager().getBinDurationMs();
		long binExcel = xlsExportOptions.buildExcelStepMs;
		results.getDataFromCapillary(cap, binData, binExcel, xlsExportOptions);
		return results;
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
	 * @param exp        The experiment to export
	 * @param sheet      The sheet to write to
	 * @param resultType The export type
	 * @param col0       The starting column
	 * @param charSeries The series identifier
	 * @param subtractT0 Whether to subtract T0 value
	 * @return The next available column
	 */
	protected int xlsExportExperimentCapillaryDataToSheet(Experiment exp, SXSSFSheet sheet, EnumResults resultType,
			int col0, String charSeries, boolean subtractT0) {
		Point pt = new Point(col0, 0);
		pt = writeExperimentSeparator(sheet, pt);

		ResultsOptions resultsOptions = new ResultsOptions();
		resultsOptions.copy(options);
		resultsOptions.resultType = resultType;
		resultsOptions.subtractT0 = subtractT0;

		ResultsFromCapillaries xlsResultsFromCaps = new ResultsFromCapillaries(exp.getCapillaries().getList().size());
		ResultsArray resultsArray = xlsResultsFromCaps.getMeasuresFromAllCapillaries(exp, resultType, resultsOptions);

		for (Results xlsResults : resultsArray.getList()) {
			String name = xlsResults.getName();
			Capillary capillary = exp.getCapillaries().getCapillaryFromRoiName(name);
			if (capillary == null)
				continue;

			Cage cage = exp.getCages().getCageFromID(capillary.getCageID());

			pt.y = 0;
			pt = writeExperimentCapillaryInfos(sheet, pt, exp, charSeries, cage, capillary, resultType);
			writeXLSResult(sheet, pt, xlsResults);
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
