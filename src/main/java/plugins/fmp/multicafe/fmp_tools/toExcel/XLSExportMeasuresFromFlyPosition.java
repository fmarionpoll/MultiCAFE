package plugins.fmp.multicafe.fmp_tools.toExcel;

import java.awt.Point;

import org.apache.poi.xssf.streaming.SXSSFSheet;

import plugins.fmp.multicafe.fmp_experiment.Experiment;
import plugins.fmp.multicafe.fmp_experiment.cages.Cage;
import plugins.fmp.multicafe.fmp_experiment.cages.FlyPositions;
import plugins.fmp.multicafe.fmp_experiment.sequence.ImageLoader;
import plugins.fmp.multicafe.fmp_experiment.sequence.TimeManager;
import plugins.fmp.multicafe.fmp_tools.results.Results;
import plugins.fmp.multicafe.fmp_tools.toExcel.config.ExcelExportConstants;
import plugins.fmp.multicafe.fmp_tools.toExcel.config.XLSExportOptions;
import plugins.fmp.multicafe.fmp_tools.toExcel.enums.EnumXLSColumnHeader;
import plugins.fmp.multicafe.fmp_tools.toExcel.enums.EnumExport;
import plugins.fmp.multicafe.fmp_tools.toExcel.exceptions.ExcelExportException;
import plugins.fmp.multicafe.fmp_tools.toExcel.exceptions.ExcelResourceException;
import plugins.fmp.multicafe.fmp_tools.toExcel.utils.XLSUtils;

/**
 * Excel export implementation for fly position measurements. Uses the Template
 * Method pattern for structured export operations, following the same pattern
 * as spot and capillary exports.
 * 
 * <p>
 * This class exports fly position data including:
 * <ul>
 * <li>XYIMAGE - XY coordinates in image space</li>
 * <li>XYTOPCAGE - XY coordinates relative to top of cage</li>
 * <li>XYTIPCAPS - XY coordinates relative to tip of capillaries</li>
 * <li>ELLIPSEAXES - Ellipse axes (major and minor)</li>
 * <li>DISTANCE - Distance between consecutive points</li>
 * <li>ISALIVE - Fly alive status (1=alive, 0=dead)</li>
 * <li>SLEEP - Fly sleep status (1=sleeping, 0=awake)</li>
 * </ul>
 * </p>
 * 
 * @author MultiSPOTS96
 * @version 2.3.3
 */
public class XLSExportMeasuresFromFlyPosition extends XLSExport {

	/**
	 * Exports fly position data for a single experiment.
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

		if (options.xyImage) {
			column = getFlyPositionDataAndExport(exp, column, charSeries, EnumExport.XYIMAGE);
		}
		if (options.xyCage) {
			getFlyPositionDataAndExport(exp, column, charSeries, EnumExport.XYTOPCAGE);
		}
		if (options.xyCapillaries) {
			getFlyPositionDataAndExport(exp, column, charSeries, EnumExport.XYTIPCAPS);
		}
		if (options.ellipseAxes) {
			getFlyPositionDataAndExport(exp, column, charSeries, EnumExport.ELLIPSEAXES);
		}
		if (options.distance) {
			getFlyPositionDataAndExport(exp, column, charSeries, EnumExport.DISTANCE);
		}
		if (options.alive) {
			getFlyPositionDataAndExport(exp, column, charSeries, EnumExport.ISALIVE);
		}
		if (options.sleep) {
			getFlyPositionDataAndExport(exp, column, charSeries, EnumExport.SLEEP);
		}

		return column;
	}

	/**
	 * Exports fly position data for a specific export type.
	 * 
	 * @param exp        The experiment to export
	 * @param col0       The starting column
	 * @param charSeries The series identifier
	 * @param exportType The export type
	 * @return The next available column
	 * @throws ExcelExportException If export fails
	 */
	protected int getFlyPositionDataAndExport(Experiment exp, int col0, String charSeries, EnumExport exportType)
			throws ExcelExportException {
		try {
			options.exportType = exportType;
			SXSSFSheet sheet = getSheet(exportType.toString(), exportType);
			int colmax = xlsExportExperimentFlyPositionDataToSheet(exp, sheet, exportType, col0, charSeries);

			if (options.onlyalive) {
				sheet = getSheet(exportType.toString() + ExcelExportConstants.ALIVE_SHEET_SUFFIX, exportType);
				xlsExportExperimentFlyPositionDataToSheet(exp, sheet, exportType, col0, charSeries);
			}

			return colmax;
		} catch (ExcelResourceException e) {
			throw new ExcelExportException("Failed to export fly position data", "get_fly_position_data_and_export",
					exportType.toString(), e);
		}
	}

	/**
	 * Exports fly position data to a specific sheet.
	 * 
	 * @param exp           The experiment to export
	 * @param sheet         The sheet to write to
	 * @param xlsExportType The export type
	 * @param col0          The starting column
	 * @param charSeries    The series identifier
	 * @return The next available column
	 */
	protected int xlsExportExperimentFlyPositionDataToSheet(Experiment exp, SXSSFSheet sheet,
			EnumExport xlsExportType, int col0, String charSeries) {
		Point pt = new Point(col0, 0);
		pt = writeExperimentSeparator(sheet, pt);

		// For fly positions, scaling is typically 1.0 (already in physical units)
		double scalingFactorToPhysicalUnits = 1.0;

		for (Cage cage : exp.getCages().cagesList) {
			FlyPositions flyPositions = cage.flyPositions;
			if (flyPositions == null || flyPositions.flyPositionList == null
					|| flyPositions.flyPositionList.isEmpty()) {
				continue;
			}

			pt.y = 0;
			pt = writeExperimentFlyPositionInfos(sheet, pt, exp, charSeries, cage, xlsExportType);
			Results xlsResults = getXLSResultsDataValuesFromFlyPositionMeasures(exp, cage, flyPositions, options);
			xlsResults.transferDataValuesToValuesOut(scalingFactorToPhysicalUnits, xlsExportType);
			writeXLSResult(sheet, pt, xlsResults);
			pt.x++;
		}
		return pt.x;
	}

	/**
	 * Gets the results for fly positions.
	 * 
	 * @param exp              The experiment
	 * @param cage             The cage
	 * @param flyPositions     The fly positions
	 * @param xlsExportOptions The export options
	 * @return The XLS results
	 */
	public Results getXLSResultsDataValuesFromFlyPositionMeasures(Experiment exp, Cage cage,
			FlyPositions flyPositions, XLSExportOptions xlsExportOptions) {
		int nOutputFrames = getNOutputFrames(exp, xlsExportOptions);

		// Create XLSResults with cage properties
		Results xlsResults = new Results("Cage_" + cage.getProperties().getCageID(),
				cage.getProperties().getCageNFlies(), cage.getProperties().getCageID(), 0, xlsExportOptions.exportType);
		xlsResults.initValuesOutArray(nOutputFrames, Double.NaN);

		// Get bin durations
		long binData = exp.getSeqCamData().getTimeManager().getBinDurationMs();
		long binExcel = xlsExportOptions.buildExcelStepMs;

		// Get data from fly positions
		xlsResults.getDataFromFlyPositions(flyPositions, binData, binExcel, xlsExportOptions);

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
		// For fly positions, use camera sequence timing
		TimeManager timeManager = exp.getSeqCamData().getTimeManager();
		ImageLoader imgLoader = exp.getSeqCamData().getImageLoader();
		long durationMs = timeManager.getBinLast_ms() - timeManager.getBinFirst_ms();
		int nOutputFrames = (int) (durationMs / options.buildExcelStepMs + 1);

		if (nOutputFrames <= 1) {
			long binLastMs = timeManager.getBinFirst_ms()
					+ imgLoader.getNTotalFrames() * timeManager.getBinDurationMs();
			timeManager.setBinLast_ms(binLastMs);

			if (binLastMs <= 0) {
				handleExportError(exp, -1);
			}

			nOutputFrames = (int) ((binLastMs - timeManager.getBinFirst_ms()) / options.buildExcelStepMs + 1);

			if (nOutputFrames <= 1) {
				nOutputFrames = imgLoader.getNTotalFrames();
				handleExportError(exp, nOutputFrames);
			}
		}

		return nOutputFrames;
	}

	/**
	 * Writes experiment fly position information to the sheet.
	 * 
	 * @param sheet         The sheet to write to
	 * @param pt            The starting point
	 * @param exp           The experiment
	 * @param charSeries    The series identifier
	 * @param cage          The cage
	 * @param xlsExportType The export type
	 * @return The updated point
	 */
	protected Point writeExperimentFlyPositionInfos(SXSSFSheet sheet, Point pt, Experiment exp, String charSeries,
			Cage cage, EnumExport xlsExportType) {
		int x = pt.x;
		int y = pt.y;
		boolean transpose = options.transpose;

		// Write basic file information
		writeFileInformationForFlyPosition(sheet, x, y, transpose, exp);

		// Write experiment properties
		writeExperimentPropertiesForFlyPosition(sheet, x, y, transpose, exp, charSeries);

		// Write cage properties
		writeCagePropertiesForFlyPosition(sheet, x, y, transpose, cage, charSeries);

		pt.y = y + getDescriptorRowCount();
		return pt;
	}

	/**
	 * Writes basic file information to the sheet (for fly positions).
	 */
	private void writeFileInformationForFlyPosition(SXSSFSheet sheet, int x, int y, boolean transpose, Experiment exp) {
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
	 * Writes experiment properties to the sheet (for fly positions).
	 */
	private void writeExperimentPropertiesForFlyPosition(SXSSFSheet sheet, int x, int y, boolean transpose,
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
	 * Writes cage properties to the sheet (for fly positions).
	 */
	private void writeCagePropertiesForFlyPosition(SXSSFSheet sheet, int x, int y, boolean transpose, Cage cage,
			String charSeries) {
		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAGEID.getValue(), transpose,
				charSeries + cage.getProperties().getCageID());
		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAGE_STRAIN.getValue(), transpose,
				cage.getProperties().getFlyStrain());
		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAGE_SEX.getValue(), transpose,
				cage.getProperties().getFlySex());
		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAGE_AGE.getValue(), transpose,
				cage.getProperties().getFlyAge());
		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.SPOT_NFLIES.getValue(), transpose,
				cage.getProperties().getCageNFlies());
		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAGE_COMMENT.getValue(), transpose,
				cage.getProperties().getComment());
	}

	/**
	 * Handles export errors by logging them.
	 * 
	 * @param exp           The experiment
	 * @param nOutputFrames The number of output frames
	 */
	protected void handleExportError(Experiment exp, int nOutputFrames) {
		String error = String.format(
				"XLSExport:ExportError() ERROR in %s\n nOutputFrames=%d binFirst_Ms=%d binLast_Ms=%d",
				exp.getExperimentDirectory(), nOutputFrames, exp.getSeqCamData().getTimeManager().getBinFirst_ms(),
				exp.getSeqCamData().getTimeManager().getBinLast_ms());
		System.err.println(error);
	}
}
