package plugins.fmp.multicafe.fmp_tools.toExcel.capillaries;

import java.awt.Point;

import org.apache.poi.xssf.streaming.SXSSFSheet;

import plugins.fmp.multicafe.fmp_experiment.Experiment;
import plugins.fmp.multicafe.fmp_experiment.capillaries.Capillary;
import plugins.fmp.multicafe.fmp_experiment.sequence.ImageLoader;
import plugins.fmp.multicafe.fmp_tools.toExcel.XLSExport;
import plugins.fmp.multicafe.fmp_tools.toExcel.config.ExcelExportConstants;
import plugins.fmp.multicafe.fmp_tools.toExcel.config.XLSExportOptions;
import plugins.fmp.multicafe.fmp_tools.toExcel.data.XLSResults;
import plugins.fmp.multicafe.fmp_tools.toExcel.enums.EnumXLSColumnHeader;
import plugins.fmp.multicafe.fmp_tools.toExcel.enums.EnumXLSExport;
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
	protected int exportExperimentData(Experiment exp, XLSExportOptions xlsExportOptions, int startColumn,
			String charSeries) throws ExcelExportException {
		int column = startColumn;

		if (options.topLevel) {
			column = getCapillaryDataAndExport(exp, column, charSeries, EnumXLSExport.TOPRAW, false);
			getCapillaryDataAndExport(exp, column, charSeries, EnumXLSExport.TOPLEVEL, true);
		}
		if (options.lrPI && options.topLevel) {
			getCapillaryDataAndExport(exp, column, charSeries, EnumXLSExport.TOPLEVEL_LR, true);
		}
		if (options.topLevelDelta) {
			getCapillaryDataAndExport(exp, column, charSeries, EnumXLSExport.TOPLEVELDELTA, true);
		}
		if (options.lrPI && options.topLevelDelta) {
			getCapillaryDataAndExport(exp, column, charSeries, EnumXLSExport.TOPLEVELDELTA_LR, true);
		}
		if (options.bottomLevel) {
			getCapillaryDataAndExport(exp, column, charSeries, EnumXLSExport.BOTTOMLEVEL, false);
		}
		if (options.derivative) {
			getCapillaryDataAndExport(exp, column, charSeries, EnumXLSExport.DERIVEDVALUES, false);
		}

		return column;
	}

	/**
	 * Exports capillary data for a specific export type.
	 * 
	 * @param exp        The experiment to export
	 * @param col0       The starting column
	 * @param charSeries The series identifier
	 * @param exportType The export type
	 * @param subtractT0 Whether to subtract T0 value
	 * @return The next available column
	 * @throws ExcelExportException If export fails
	 */
	protected int getCapillaryDataAndExport(Experiment exp, int col0, String charSeries, EnumXLSExport exportType,
			boolean subtractT0) throws ExcelExportException {
		try {
			options.exportType = exportType;
			SXSSFSheet sheet = getSheet(exportType.toString(), exportType);
			int colmax = xlsExportExperimentCapillaryDataToSheet(exp, sheet, exportType, col0, charSeries, subtractT0);

			if (options.onlyalive) {
				sheet = getSheet(exportType.toString() + ExcelExportConstants.ALIVE_SHEET_SUFFIX, exportType);
				xlsExportExperimentCapillaryDataToSheet(exp, sheet, exportType, col0, charSeries, subtractT0);
			}

			return colmax;
		} catch (ExcelResourceException e) {
			throw new ExcelExportException("Failed to export capillary data", "get_capillary_data_and_export",
					exportType.toString(), e);
		}
	}

	/**
	 * Exports capillary data to a specific sheet.
	 * 
	 * @param exp           The experiment to export
	 * @param sheet         The sheet to write to
	 * @param xlsExportType The export type
	 * @param col0          The starting column
	 * @param charSeries    The series identifier
	 * @param subtractT0    Whether to subtract T0 value
	 * @return The next available column
	 */
	protected int xlsExportExperimentCapillaryDataToSheet(Experiment exp, SXSSFSheet sheet, EnumXLSExport xlsExportType,
			int col0, String charSeries, boolean subtractT0) {
		Point pt = new Point(col0, 0);
		pt = writeExperimentSeparator(sheet, pt);

		double scalingFactorToPhysicalUnits = exp.getCapillaries().getScalingFactorToPhysicalUnits(xlsExportType);

		// TODO: add loop for cage?
		for (Capillary capillary : exp.getCapillaries().getCapillariesList()) {
			pt.y = 0;
			pt = writeExperimentCapillaryInfos(sheet, pt, exp, charSeries, capillary, xlsExportType);
			XLSResults xlsResults = getXLSResultsDataValuesFromCapillaryMeasures(exp, capillary, options, subtractT0);
			xlsResults.transferDataValuesToValuesOut(scalingFactorToPhysicalUnits, xlsExportType);
			writeXLSResult(sheet, pt, xlsResults);
			pt.x++;
		}
		return pt.x;
	}

	/**
	 * Gets the results for a capillary.
	 * 
	 * @param exp              The experiment
	 * @param capillary        The capillary
	 * @param xlsExportOptions The export options
	 * @param subtractT0       Whether to subtract T0 value
	 * @return The XLS results
	 */
	public XLSResults getXLSResultsDataValuesFromCapillaryMeasures(Experiment exp, Capillary capillary,
			XLSExportOptions xlsExportOptions, boolean subtractT0) {
		int nOutputFrames = getNOutputFrames(exp, xlsExportOptions);

		XLSResults xlsResults = new XLSResults(capillary.getRoiName(), capillary.capNFlies, capillary.capCageID, 0,
				xlsExportOptions.exportType);

		xlsResults.setStimulus(capillary.capStimulus);
		xlsResults.setConcentration(capillary.capConcentration);
		xlsResults.initValuesOutArray(nOutputFrames, Double.NaN);

		// Get bin durations
		long binData = exp.getKymoBin_ms();
		long binExcel = xlsExportOptions.buildExcelStepMs;
		xlsResults.getDataFromCapillary(capillary, binData, binExcel, xlsExportOptions, subtractT0);
		return xlsResults;
	}

	/**
	 * Gets the number of output frames for the experiment.
	 * 
	 * @param exp     The experiment
	 * @param options The export options
	 * @return The number of output frames
	 */
	protected int getNOutputFrames(Experiment exp, XLSExportOptions options) {
		// For capillaries, use kymograph timing
		long kymoFirst_ms = exp.getKymoFirst_ms();
		long kymoLast_ms = exp.getKymoLast_ms();

		if (kymoLast_ms <= kymoFirst_ms) {
			// Try to get from kymograph sequence
			if (exp.getSeqKymos() != null) {
				ImageLoader imgLoader = exp.getSeqKymos().getImageLoader();
				if (imgLoader != null) {
					long kymoBin_ms = exp.getKymoBin_ms();
					if (kymoBin_ms > 0) {
						kymoLast_ms = kymoFirst_ms + imgLoader.getNTotalFrames() * kymoBin_ms;
						exp.setKymoLast_ms(kymoLast_ms);
					}
				}
			}
		}

		long durationMs = kymoLast_ms - kymoFirst_ms;
		int nOutputFrames = (int) (durationMs / options.buildExcelStepMs + 1);

		if (nOutputFrames <= 1) {
			handleExportError(exp, -1);
			// Fallback to a reasonable default
			nOutputFrames = 1000;
		}

		return nOutputFrames;
	}

	/**
	 * Writes experiment capillary information to the sheet.
	 * 
	 * @param sheet         The sheet to write to
	 * @param pt            The starting point
	 * @param exp           The experiment
	 * @param charSeries    The series identifier
	 * @param capillary     The capillary
	 * @param xlsExportType The export type
	 * @return The updated point
	 */
	protected Point writeExperimentCapillaryInfos(SXSSFSheet sheet, Point pt, Experiment exp, String charSeries,
			Capillary capillary, EnumXLSExport xlsExportType) {
		int x = pt.x;
		int y = pt.y;
		boolean transpose = options.transpose;

		// Write basic file information (duplicate logic from base class since it's
		// private)
		writeFileInformationForCapillary(sheet, x, y, transpose, exp);

		// Write experiment properties (duplicate logic from base class since it's
		// private)
		writeExperimentPropertiesForCapillary(sheet, x, y, transpose, exp, charSeries);

		// Write capillary properties
		writeCapillaryProperties(sheet, x, y, transpose, capillary, charSeries, xlsExportType);

		pt.y = y + getDescriptorRowCount();
		return pt;
	}

	/**
	 * Writes basic file information to the sheet (for capillaries).
	 */
	private void writeFileInformationForCapillary(SXSSFSheet sheet, int x, int y, boolean transpose, Experiment exp) {
		String filename = exp.getResultsDirectory();
		if (filename == null) {
			filename = exp.getSeqCamData().getImagesDirectory();
		}

		java.nio.file.Path path = java.nio.file.Paths.get(filename);
		java.text.SimpleDateFormat df = new java.text.SimpleDateFormat(ExcelExportConstants.DEFAULT_DATE_FORMAT);
		String date = df.format(exp.chainImageFirst_ms);
		String name0 = path.toString();
		String cam = extractCameraInfo(name0);

		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.PATH.getValue(), transpose, name0);
		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.DATE.getValue(), transpose, date);
		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAM.getValue(), transpose, cam);
	}

	/**
	 * Extracts camera information from the filename.
	 */
	private String extractCameraInfo(String filename) {
		int pos = filename.indexOf(ExcelExportConstants.CAMERA_IDENTIFIER);
		if (pos > 0) {
			int pos5 = pos + ExcelExportConstants.CAMERA_IDENTIFIER_LENGTH;
			if (pos5 >= filename.length()) {
				pos5 = filename.length() - 1;
			}
			return filename.substring(pos, pos5);
		}
		return ExcelExportConstants.CAMERA_DEFAULT_VALUE;
	}

	/**
	 * Writes experiment properties to the sheet (for capillaries).
	 */
	private void writeExperimentPropertiesForCapillary(SXSSFSheet sheet, int x, int y, boolean transpose,
			Experiment exp, String charSeries) {
		plugins.fmp.multicafe.fmp_experiment.ExperimentProperties props = exp.getProperties();

		XLSUtils.setFieldValue(sheet, x, y, transpose, props, EnumXLSColumnHeader.EXP_BOXID);
		XLSUtils.setFieldValue(sheet, x, y, transpose, props, EnumXLSColumnHeader.EXP_EXPT);
		XLSUtils.setFieldValue(sheet, x, y, transpose, props, EnumXLSColumnHeader.EXP_STIM1);
		XLSUtils.setFieldValue(sheet, x, y, transpose, props, EnumXLSColumnHeader.EXP_CONC1);
		XLSUtils.setFieldValue(sheet, x, y, transpose, props, EnumXLSColumnHeader.EXP_STRAIN);
		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.EXP_BOXID.getValue(), transpose, charSeries);
		XLSUtils.setFieldValue(sheet, x, y, transpose, props, EnumXLSColumnHeader.EXP_SEX);
		XLSUtils.setFieldValue(sheet, x, y, transpose, props, EnumXLSColumnHeader.EXP_STIM2);
		XLSUtils.setFieldValue(sheet, x, y, transpose, props, EnumXLSColumnHeader.EXP_CONC2);
	}

	/**
	 * Writes capillary properties to the sheet.
	 */
	private void writeCapillaryProperties(SXSSFSheet sheet, int x, int y, boolean transpose, Capillary capillary,
			String charSeries, EnumXLSExport xlsExportType) {
		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAP.getValue(), transpose,
				capillary.getSideDescriptor(xlsExportType));
		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAP_INDEX.getValue(), transpose,
				charSeries + "_" + capillary.getLast2ofCapillaryName());
		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAP_VOLUME.getValue(), transpose, capillary.capVolume);
		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAP_PIXELS.getValue(), transpose, capillary.capPixels);
		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAP_STIM.getValue(), transpose, capillary.capStimulus);
		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAP_CONC.getValue(), transpose, capillary.capConcentration);
		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAP_NFLIES.getValue(), transpose, capillary.capNFlies);
	}

	/**
	 * Handles export errors by logging them.
	 * 
	 * @param exp           The experiment
	 * @param nOutputFrames The number of output frames
	 */
	protected void handleExportError(Experiment exp, int nOutputFrames) {
		String error = String.format(
				"XLSExport:ExportError() ERROR in %s\n nOutputFrames=%d kymoFirstCol_Ms=%d kymoLastCol_Ms=%d",
				exp.getExperimentDirectory(), nOutputFrames, exp.getKymoFirst_ms(), exp.getKymoLast_ms());
		System.err.println(error);
	}
}
