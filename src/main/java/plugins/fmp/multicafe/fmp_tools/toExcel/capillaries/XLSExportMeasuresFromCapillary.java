package plugins.fmp.multicafe.fmp_tools.toExcel.capillaries;

import java.awt.Point;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

import org.apache.poi.xssf.streaming.SXSSFSheet;

import plugins.fmp.multicafe.fmp_experiment.Experiment;
import plugins.fmp.multicafe.fmp_experiment.cages.Cage;
import plugins.fmp.multicafe.fmp_experiment.cages.CageProperties;
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
	protected int exportExperimentData(Experiment exp, XLSExportOptions xlsExportOptions, int startColumn,
			String charSeries) throws ExcelExportException {
		int maxColumn = startColumn;

		if (options.topLevel) {
			int col = getCapillaryDataAndExport(exp, getSheetColumn(EnumXLSExport.TOPRAW.toString()), charSeries,
					EnumXLSExport.TOPRAW, false);
			updateSheetColumn(EnumXLSExport.TOPRAW.toString(), col);
			if (col > maxColumn)
				maxColumn = col;
			col = getCapillaryDataAndExport(exp, getSheetColumn(EnumXLSExport.TOPLEVEL.toString()), charSeries,
					EnumXLSExport.TOPLEVEL, true);
			updateSheetColumn(EnumXLSExport.TOPLEVEL.toString(), col);
			if (col > maxColumn)
				maxColumn = col;
		}
		if (options.lrPI && options.topLevel) {
			int col = getCapillaryDataAndExport(exp, getSheetColumn(EnumXLSExport.TOPLEVEL_LR.toString()), charSeries,
					EnumXLSExport.TOPLEVEL_LR, true);
			updateSheetColumn(EnumXLSExport.TOPLEVEL_LR.toString(), col);
			if (col > maxColumn)
				maxColumn = col;
		}
		if (options.topLevelDelta) {
			int col = getCapillaryDataAndExport(exp, getSheetColumn(EnumXLSExport.TOPLEVELDELTA.toString()), charSeries,
					EnumXLSExport.TOPLEVELDELTA, true);
			updateSheetColumn(EnumXLSExport.TOPLEVELDELTA.toString(), col);
			if (col > maxColumn)
				maxColumn = col;
		}
		if (options.lrPI && options.topLevelDelta) {
			int col = getCapillaryDataAndExport(exp, getSheetColumn(EnumXLSExport.TOPLEVELDELTA_LR.toString()),
					charSeries, EnumXLSExport.TOPLEVELDELTA_LR, true);
			updateSheetColumn(EnumXLSExport.TOPLEVELDELTA_LR.toString(), col);
			if (col > maxColumn)
				maxColumn = col;
		}
		if (options.bottomLevel) {
			int col = getCapillaryDataAndExport(exp, getSheetColumn(EnumXLSExport.BOTTOMLEVEL.toString()), charSeries,
					EnumXLSExport.BOTTOMLEVEL, false);
			updateSheetColumn(EnumXLSExport.BOTTOMLEVEL.toString(), col);
			if (col > maxColumn)
				maxColumn = col;
		}
		if (options.derivative) {
			int col = getCapillaryDataAndExport(exp, getSheetColumn(EnumXLSExport.DERIVEDVALUES.toString()), charSeries,
					EnumXLSExport.DERIVEDVALUES, false);
			updateSheetColumn(EnumXLSExport.DERIVEDVALUES.toString(), col);
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

		// Fallback: if cages weren't loaded (shouldn't happen as they're loaded in
		// executeExport),
		// create cage structures from capillaries
// if (exp.getCages().getCageList().size() < 2) {
		exp.dispatchCapillariesToCages();
//		}

		for (Cage cage : exp.getCages().getCageList()) {

			for (Capillary capillary : cage.getCapillaries().getList()) {
				pt.y = 0;
				pt = writeExperimentCapillaryInfos(sheet, pt, exp, charSeries, cage, capillary, xlsExportType);
				XLSResults xlsResults = getXLSResultsDataValuesFromCapillaryMeasures(exp, capillary, options,
						subtractT0);
				xlsResults.transferDataValuesToValuesOut(scalingFactorToPhysicalUnits, xlsExportType);
				writeXLSResult(sheet, pt, xlsResults);
				pt.x++;
			}
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
		XLSResults xlsResults = new XLSResults(capillary.getRoiName(), capillary.capNFlies, capillary.getCageID(), 0,
				xlsExportOptions.exportType);

		xlsResults.setStimulus(capillary.capStimulus);
		xlsResults.setConcentration(capillary.capConcentration);

		// Get bin durations
		long binData = exp.getKymoBin_ms();
		long binExcel = xlsExportOptions.buildExcelStepMs;
		xlsResults.getDataFromCapillary(capillary, binData, binExcel, xlsExportOptions, subtractT0);

		// Initialize valuesOut array with the actual size of dataValues
		if (xlsResults.getDataValues() != null && xlsResults.getDataValues().size() > 0) {
			int actualSize = xlsResults.getDataValues().size();
			xlsResults.initValuesOutArray(actualSize, Double.NaN);
		} else {
			// Fallback to calculated size if no data
			int nOutputFrames = getNOutputFrames(exp, xlsExportOptions);
			xlsResults.initValuesOutArray(nOutputFrames, Double.NaN);
		}

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
		long kymoBin_ms = exp.getKymoBin_ms();

		// If buildExcelStepMs equals kymoBin_ms, we want 1:1 mapping - use actual frame
		// count
		if (kymoBin_ms > 0 && options.buildExcelStepMs == kymoBin_ms && exp.getSeqKymos() != null) {
			ImageLoader imgLoader = exp.getSeqKymos().getImageLoader();
			if (imgLoader != null) {
				int nFrames = imgLoader.getNTotalFrames();
				if (nFrames > 0) {
					return nFrames;
				}
			}
		}

		if (kymoLast_ms <= kymoFirst_ms) {
			// Try to get from kymograph sequence
			if (exp.getSeqKymos() != null) {
				ImageLoader imgLoader = exp.getSeqKymos().getImageLoader();
				if (imgLoader != null) {
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
			Cage cage, Capillary capillary, EnumXLSExport xlsExportType) {
		int x = pt.x;
		int y = pt.y;
		boolean transpose = options.transpose;

		// Write basic file information (duplicate logic from base class since it's
		// private)
		writeFileInformationForCapillary(sheet, x, y, transpose, exp);

		writeExperimentProperties(sheet, x, y, transpose, exp, charSeries);
		writeCageProperties(sheet, x, y, transpose, cage);
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

		Path path = Paths.get(filename);
		SimpleDateFormat df = new SimpleDateFormat(ExcelExportConstants.DEFAULT_DATE_FORMAT);
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

	private void writeExperimentProperties(SXSSFSheet sheet, int x, int y, boolean transpose, Experiment exp,
			String charSeries) {
		// CRITICAL: Reload experiment properties right before writing to ensure
		// we have the correct properties for THIS specific experiment
		// This is especially important when exporting multiple experiments
		exp.load_MS96_experiment();

		// Use exp.getExperimentField() instead of props.getField() to match
		// how Infos.java correctly reads the properties (this is the key fix!)
		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.EXP_BOXID.getValue(), transpose,
				exp.getExperimentField(EnumXLSColumnHeader.EXP_BOXID));
		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.EXP_EXPT.getValue(), transpose,
				exp.getExperimentField(EnumXLSColumnHeader.EXP_EXPT));
		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.EXP_STIM1.getValue(), transpose,
				exp.getExperimentField(EnumXLSColumnHeader.EXP_STIM1));
		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.EXP_CONC1.getValue(), transpose,
				exp.getExperimentField(EnumXLSColumnHeader.EXP_CONC1));
		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.EXP_STRAIN.getValue(), transpose,
				exp.getExperimentField(EnumXLSColumnHeader.EXP_STRAIN));
		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.EXP_BOXID.getValue(), transpose, charSeries);
		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.EXP_SEX.getValue(), transpose,
				exp.getExperimentField(EnumXLSColumnHeader.EXP_SEX));
		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.EXP_STIM2.getValue(), transpose,
				exp.getExperimentField(EnumXLSColumnHeader.EXP_STIM2));
		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.EXP_CONC2.getValue(), transpose,
				exp.getExperimentField(EnumXLSColumnHeader.EXP_CONC2));
	}

	private void writeCageProperties(SXSSFSheet sheet, int x, int y, boolean transpose, Cage cage) {
		CageProperties props = cage.getProperties();
		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAGEID.getValue(), transpose, props.getCageID());
		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAGEPOS.getValue(), transpose, props.getCagePosition());
		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAGE_NFLIES.getValue(), transpose, props.getCageNFlies());
		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAGE_STRAIN.getValue(), transpose, props.getFlyStrain());
		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAGE_SEX.getValue(), transpose, props.getFlySex());
		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAGE_AGE.getValue(), transpose, props.getFlyAge());
		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAGE_COMMENT.getValue(), transpose, props.getComment());
	}

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

		// Add missing fields: CHOICE, CAGEID, CAGEPOS, DUM4
		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAGEID.getValue(), transpose,
				charSeries + capillary.getCageID());
		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAGEPOS.getValue(), transpose, capillary.getCageID());
		// DUM4 should show the export type name (e.g., "topraw", "toplevel",
		// "toplevel_L+R")
		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.DUM4.getValue(), transpose, xlsExportType.toString());
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
