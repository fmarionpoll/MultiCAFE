package plugins.fmp.multicafe.fmp_tools.toExcel.capillaries;

import java.awt.Point;
import java.util.HashMap;
import java.util.Map;

import org.apache.poi.xssf.streaming.SXSSFSheet;

import plugins.fmp.multicafe.fmp_experiment.Experiment;
import plugins.fmp.multicafe.fmp_experiment.cages.Cage;
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

		// update cage structures from capillaries so that we can do operations within a
		// cage more easily (not yet implemented)
		exp.dispatchCapillariesToCages();
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
		
		// For TOPLEVEL_LR, read from CageCapillariesComputation instead of capillary
		if (xlsExportOptions.exportType == EnumXLSExport.TOPLEVEL_LR) {
			getLRDataFromCage(exp, capillary, xlsResults, binData, binExcel, subtractT0);
		} else {
			xlsResults.getDataFromCapillary(capillary, binData, binExcel, xlsExportOptions, subtractT0);
		}

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
	 * Gets L+R data (SUM or PI) from CageCapillariesComputation for TOPLEVEL_LR export.
	 * For L capillaries: exports SUM measure
	 * For R capillaries: exports PI measure
	 * 
	 * @param exp The experiment
	 * @param capillary The capillary
	 * @param xlsResults The XLS results to populate
	 * @param binData The bin duration for the data
	 * @param binExcel The bin duration for Excel output
	 * @param subtractT0 Whether to subtract T0 value
	 */
	private void getLRDataFromCage(Experiment exp, Capillary capillary, XLSResults xlsResults,
			long binData, long binExcel, boolean subtractT0) {
		
		int cageID = capillary.getCageID();
		plugins.fmp.multicafe.fmp_experiment.cages.CageCapillariesComputation cageComp = 
			exp.getCages().getCageComputation(cageID);
		
		if (cageComp == null) {
			// No computation available, fall back to raw
			XLSExportOptions fallbackOptions = new XLSExportOptions();
			fallbackOptions.exportType = EnumXLSExport.TOPRAW;
			xlsResults.getDataFromCapillary(capillary, binData, binExcel, fallbackOptions, subtractT0);
			return;
		}
		
		// Determine which measure to use based on capillary side
		String side = getCapillarySide(capillary);
		plugins.fmp.multicafe.fmp_experiment.capillaries.CapillaryMeasure measure = null;
		
		if (side != null && (side.contains("L") || side.contains("1"))) {
			// L capillary: use SUM
			measure = cageComp.getSumMeasure();
		} else if (side != null && (side.contains("R") || side.contains("2"))) {
			// R capillary: use PI
			measure = cageComp.getPIMeasure();
		} else {
			// Side unclear, try first capillary as L, second as R
			java.util.List<plugins.fmp.multicafe.fmp_experiment.capillaries.Capillary> caps = 
				exp.getCages().getCageList().stream()
					.filter(c -> c.getCageID() == cageID)
					.findFirst()
					.map(c -> c.getCapillaries().getList())
					.orElse(java.util.Collections.emptyList());
			
			if (!caps.isEmpty() && caps.get(0) == capillary) {
				measure = cageComp.getSumMeasure();
			} else if (caps.size() >= 2 && caps.get(1) == capillary) {
				measure = cageComp.getPIMeasure();
			}
		}
		
		if (measure != null && measure.polylineLevel != null && measure.polylineLevel.npoints > 0) {
			// Get measures by binning polyline data (similar to getMeasures implementation)
			plugins.fmp.multicafe.fmp_tools.Level2D polyline = measure.polylineLevel;
			long maxMs = (polyline.npoints - 1) * binData;
			int nOutputFrames = (int) (maxMs / binExcel) + 1;
			
			java.util.ArrayList<Integer> intData = new java.util.ArrayList<>(nOutputFrames);
			for (int i = 0; i < nOutputFrames; i++) {
				long timeMs = i * binExcel;
				int index = (int) (timeMs / binData);
				if (index >= 0 && index < polyline.npoints) {
					intData.add((int) polyline.ypoints[index]);
				} else {
					intData.add(0);
				}
			}
			
			if (intData != null && !intData.isEmpty()) {
				// Convert Integer to Double
				java.util.ArrayList<Double> dataValues = new java.util.ArrayList<>(intData.size());
				int t0Value = 0;
				
				if (subtractT0 && intData.size() > 0) {
					t0Value = intData.get(0);
				}
				
				for (Integer intValue : intData) {
					if (subtractT0) {
						dataValues.add((double) (intValue - t0Value));
					} else {
						dataValues.add(intValue.doubleValue());
					}
				}
				
				xlsResults.setDataValues(dataValues);
				return;
			}
		}
		
		// Fallback to raw if computation failed
		XLSExportOptions fallbackOptions = new XLSExportOptions();
		fallbackOptions.exportType = EnumXLSExport.TOPRAW;
		xlsResults.getDataFromCapillary(capillary, binData, binExcel, fallbackOptions, subtractT0);
	}
	
	/**
	 * Helper method to determine capillary side from capSide or name.
	 */
	private String getCapillarySide(plugins.fmp.multicafe.fmp_experiment.capillaries.Capillary cap) {
		if (cap.capSide != null && !cap.capSide.equals("."))
			return cap.capSide;
		// Try to get from name
		String name = cap.getRoiName();
		if (name != null) {
			name = name.toUpperCase();
			if (name.contains("L") || name.contains("1"))
				return "L";
			if (name.contains("R") || name.contains("2"))
				return "R";
		}
		return "";
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

		boolean transpose = options.transpose;

		writeFileInformation(sheet, pt, transpose, exp);
		writeExperimentProperties(sheet, pt, transpose, exp, charSeries);
		writeCageProperties(sheet, pt, transpose, cage);
		writeCapillaryProperties(sheet, pt, transpose, capillary, charSeries, xlsExportType);

		pt.y += getDescriptorRowCount();
		return pt;
	}

	private void writeCapillaryProperties(SXSSFSheet sheet, Point pt, boolean transpose, Capillary capillary,
			String charSeries, EnumXLSExport xlsExportType) {
		XLSUtils.setValueAtColumn(sheet, pt, EnumXLSColumnHeader.CAP, transpose,
				capillary.getSideDescriptor(xlsExportType));
		XLSUtils.setValueAtColumn(sheet, pt, EnumXLSColumnHeader.CAP_INDEX, transpose,
				charSeries + "_" + capillary.getLast2ofCapillaryName());
		XLSUtils.setValueAtColumn(sheet, pt, EnumXLSColumnHeader.CAP_VOLUME, transpose, capillary.capVolume);
		XLSUtils.setValueAtColumn(sheet, pt, EnumXLSColumnHeader.CAP_PIXELS, transpose, capillary.capPixels);
		XLSUtils.setValueAtColumn(sheet, pt, EnumXLSColumnHeader.CAP_STIM, transpose, capillary.capStimulus);
		XLSUtils.setValueAtColumn(sheet, pt, EnumXLSColumnHeader.CAP_CONC, transpose, capillary.capConcentration);
		XLSUtils.setValueAtColumn(sheet, pt, EnumXLSColumnHeader.CAP_NFLIES, transpose, capillary.capNFlies);
		XLSUtils.setValueAtColumn(sheet, pt, EnumXLSColumnHeader.DUM4, transpose, xlsExportType.toString());
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
