package plugins.fmp.multicafe.fmp_tools.toExcel;

import java.awt.Point;

import org.apache.poi.xssf.streaming.SXSSFSheet;

import plugins.fmp.multicafe.fmp_experiment.Experiment;
import plugins.fmp.multicafe.fmp_experiment.capillaries.Capillary;
import plugins.fmp.multicafe.fmp_experiment.sequence.ImageLoader;
import plugins.fmp.multicafe.fmp_tools.results.Results;
import plugins.fmp.multicafe.fmp_tools.toExcel.config.ExcelExportConstants;
import plugins.fmp.multicafe.fmp_tools.toExcel.config.XLSExportOptions;
import plugins.fmp.multicafe.fmp_tools.toExcel.enums.EnumXLSColumnHeader;
import plugins.fmp.multicafe.fmp_tools.toExcel.enums.EnumExport;
import plugins.fmp.multicafe.fmp_tools.toExcel.exceptions.ExcelExportException;
import plugins.fmp.multicafe.fmp_tools.toExcel.exceptions.ExcelResourceException;
import plugins.fmp.multicafe.fmp_tools.toExcel.utils.XLSUtils;

/**
 * Excel export implementation for gulp measurements. Uses the Template Method
 * pattern for structured export operations, following the same pattern as spot
 * and capillary exports.
 * 
 * <p>
 * This class exports gulp data including:
 * <ul>
 * <li>SUMGULPS - Cumulated volume of gulps</li>
 * <li>SUMGULPS_LR - Cumulated volume of gulps per cage (L+R)</li>
 * <li>NBGULPS - Number of gulps</li>
 * <li>AMPLITUDEGULPS - Amplitude of gulps</li>
 * <li>TTOGULP - Time to previous gulp</li>
 * <li>TTOGULP_LR - Time to previous gulp of either capillary</li>
 * <li>MARKOV_CHAIN - Markov chain transitions</li>
 * <li>AUTOCORREL - Autocorrelation</li>
 * <li>CROSSCORREL - Cross-correlation</li>
 * </ul>
 * </p>
 * 
 * @author MultiSPOTS96
 * @version 2.3.3
 */
public class XLSExportMeasuresFromGulp extends XLSExport {

	/**
	 * Exports gulp data for a single experiment.
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

		if (options.sumGulps) {
			column = getGulpDataAndExport(exp, column, charSeries, EnumExport.SUMGULPS);
		}
		if (options.lrPI && options.sumGulps) {
			getGulpDataAndExport(exp, column, charSeries, EnumExport.SUMGULPS_LR);
		}
		if (options.nbGulps) {
			getGulpDataAndExport(exp, column, charSeries, EnumExport.NBGULPS);
		}
		if (options.amplitudeGulps) {
			getGulpDataAndExport(exp, column, charSeries, EnumExport.AMPLITUDEGULPS);
		}
		if (options.tToNextGulp) {
			getGulpDataAndExport(exp, column, charSeries, EnumExport.TTOGULP);
		}
		if (options.tToNextGulp_LR) {
			getGulpDataAndExport(exp, column, charSeries, EnumExport.TTOGULP_LR);
		}
		if (options.markovChain) {
			getGulpDataAndExport(exp, column, charSeries, EnumExport.MARKOV_CHAIN);
		}
		if (options.autocorrelation) {
			getGulpDataAndExport(exp, column, charSeries, EnumExport.AUTOCORREL);
		}
		if (options.crosscorrelation) {
			getGulpDataAndExport(exp, column, charSeries, EnumExport.CROSSCORREL);
		}
		if (options.crosscorrelationLR) {
			getGulpDataAndExport(exp, column, charSeries, EnumExport.CROSSCORREL_LR);
		}

		return column;
	}

	/**
	 * Exports gulp data for a specific export type.
	 * 
	 * @param exp        The experiment to export
	 * @param col0       The starting column
	 * @param charSeries The series identifier
	 * @param exportType The export type
	 * @return The next available column
	 * @throws ExcelExportException If export fails
	 */
	protected int getGulpDataAndExport(Experiment exp, int col0, String charSeries, EnumExport exportType)
			throws ExcelExportException {
		try {
			options.exportType = exportType;
			SXSSFSheet sheet = getSheet(exportType.toString(), exportType);
			int colmax = xlsExportExperimentGulpDataToSheet(exp, sheet, exportType, col0, charSeries);

			if (options.onlyalive) {
				sheet = getSheet(exportType.toString() + ExcelExportConstants.ALIVE_SHEET_SUFFIX, exportType);
				xlsExportExperimentGulpDataToSheet(exp, sheet, exportType, col0, charSeries);
			}

			return colmax;
		} catch (ExcelResourceException e) {
			throw new ExcelExportException("Failed to export gulp data", "get_gulp_data_and_export",
					exportType.toString(), e);
		}
	}

	/**
	 * Exports gulp data to a specific sheet.
	 * 
	 * @param exp           The experiment to export
	 * @param sheet         The sheet to write to
	 * @param xlsExportType The export type
	 * @param col0          The starting column
	 * @param charSeries    The series identifier
	 * @return The next available column
	 */
	protected int xlsExportExperimentGulpDataToSheet(Experiment exp, SXSSFSheet sheet, EnumExport xlsExportType,
			int col0, String charSeries) {
		Point pt = new Point(col0, 0);
		pt = writeExperimentSeparator(sheet, pt);

		double scalingFactorToPhysicalUnits = exp.getCapillaries().getScalingFactorToPhysicalUnits(xlsExportType);

		for (Capillary capillary : exp.getCapillaries().getList()) {
			pt.y = 0;
			pt = writeExperimentGulpInfos(sheet, pt, exp, charSeries, capillary, xlsExportType);
			Results xlsResults = getXLSResultsDataValuesFromGulpMeasures(exp, capillary, options);
			xlsResults.transferDataValuesToValuesOut(scalingFactorToPhysicalUnits, xlsExportType);
			writeXLSResult(sheet, pt, xlsResults);
			pt.x++;
		}
		return pt.x;
	}

	/**
	 * Gets the results for a gulp.
	 * 
	 * @param exp              The experiment
	 * @param capillary        The capillary
	 * @param xlsExportOptions The export options
	 * @return The XLS results
	 */
	public Results getXLSResultsDataValuesFromGulpMeasures(Experiment exp, Capillary capillary,
			XLSExportOptions xlsExportOptions) {
		int nOutputFrames = getNOutputFrames(exp, xlsExportOptions);

		// Create XLSResults with capillary properties
		Results results = new Results(capillary.getRoiName(), capillary.capNFlies, capillary.getCageID(), 0,
				xlsExportOptions.exportType);
		results.setStimulus(capillary.capStimulus);
		results.setConcentration(capillary.capConcentration);
		results.initValuesOutArray(nOutputFrames, Double.NaN);

		// Get bin durations
		long binData = exp.getKymoBin_ms();
		long binExcel = xlsExportOptions.buildExcelStepMs;

		// Get data from capillary (gulps are extracted via
		// getCapillaryMeasuresForXLSPass1)
		results.getDataFromCapillary(capillary, binData, binExcel, xlsExportOptions, false);

		return results;
	}

	/**
	 * Gets the number of output frames for the experiment.
	 * 
	 * @param exp     The experiment
	 * @param options The export options
	 * @return The number of output frames
	 */
	protected int getNOutputFrames(Experiment exp, XLSExportOptions options) {
		// For gulps, use kymograph timing (same as capillaries)
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
	 * Writes experiment gulp information to the sheet.
	 * 
	 * @param sheet         The sheet to write to
	 * @param pt            The starting point
	 * @param exp           The experiment
	 * @param charSeries    The series identifier
	 * @param capillary     The capillary
	 * @param xlsExportType The export type
	 * @return The updated point
	 */
	protected Point writeExperimentGulpInfos(SXSSFSheet sheet, Point pt, Experiment exp, String charSeries,
			Capillary capillary, EnumExport xlsExportType) {
		int x = pt.x;
		int y = pt.y;
		boolean transpose = options.transpose;

		// Write basic file information
		writeFileInformationForGulp(sheet, x, y, transpose, exp);

		// Write experiment properties
		writeExperimentPropertiesForGulp(sheet, x, y, transpose, exp, charSeries);

		// Write capillary properties (same as capillary export)
		writeCapillaryProperties(sheet, x, y, transpose, capillary, charSeries, xlsExportType);

		pt.y = y + getDescriptorRowCount();
		return pt;
	}

	/**
	 * Writes basic file information to the sheet (for gulps).
	 */
	private void writeFileInformationForGulp(SXSSFSheet sheet, int x, int y, boolean transpose, Experiment exp) {
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
	 * Writes experiment properties to the sheet (for gulps).
	 */
	private void writeExperimentPropertiesForGulp(SXSSFSheet sheet, int x, int y, boolean transpose, Experiment exp,
			String charSeries) {
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
			String charSeries, EnumExport xlsExportType) {
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
