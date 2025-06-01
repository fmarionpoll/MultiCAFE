package plugins.fmp.multicafe.tools.toExcel;

import java.awt.Point;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.List;

import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import plugins.fmp.multicafe.experiment.Experiment;
import plugins.fmp.multicafe.experiment.capillaries.Capillary;
import plugins.fmp.multicafe.experiment.cells.Cell;
import plugins.fmp.multicafe.tools.JComponents.ExperimentCombo;

public class XLSExport {
	protected XLSExportOptions options = null;
	protected Experiment expAll = null;

	XSSFCellStyle xssfCellStyle_red = null;
	XSSFCellStyle xssfCellStyle_blue = null;
	XSSFFont font_red = null;
	XSSFFont font_blue = null;
	XSSFWorkbook workbook = null;
	ExperimentCombo expList = null;

	// ------------------------------------------------

	protected Point writeExperiment_descriptors(Experiment exp, String charSeries, XSSFSheet sheet, Point pt,
			EnumXLSExportType xlsExportOption) {
		boolean transpose = options.transpose;
		int row = pt.y;
		int col0 = pt.x;
		XLSUtils.setValue(sheet, pt, transpose, "..");
		pt.x++;
		XLSUtils.setValue(sheet, pt, transpose, "..");
		pt.x++;
		int colseries = pt.x;
		int len = EnumXLSColumnHeader.values().length;
		for (int i = 0; i < len; i++) {
			XLSUtils.setValue(sheet, pt, transpose, "--");
			pt.x++;
		}
		pt.x = colseries;

		String filename = exp.getExperimentDirectory();
		if (filename == null)
			filename = exp.seqCamData.getImagesDirectory();
		Path path = Paths.get(filename);

		SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy");
		String date = df.format(exp.chainImageFirst_ms);

		String name0 = path.toString();
		int pos = name0.indexOf("cam");
		String cam = "-";
		if (pos > 0) {
			int pos5 = pos + 5;
			if (pos5 >= name0.length())
				pos5 = name0.length() - 1;
			cam = name0.substring(pos, pos5);
		}

		String sheetName = sheet.getSheetName();

		int rowmax = -1;
		for (EnumXLSColumnHeader dumb : EnumXLSColumnHeader.values()) {
			if (rowmax < dumb.getValue())
				rowmax = dumb.getValue();
		}

		List<Capillary> capList = exp.capillaries.capillariesList;
		for (int t = 0; t < capList.size(); t++) {
			Capillary cap = capList.get(t);
			String name = cap.getRoiName();
			int col = getRowIndexFromKymoFileName(name);
			if (col >= 0)
				pt.x = colseries + col;
			int x = pt.x;
			int y = row;
			XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.PATH.getValue(), transpose, name0);
			XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.DATE.getValue(), transpose, date);
			XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAM.getValue(), transpose, cam);

			XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.EXP_BOXID.getValue(), transpose,
					exp.getExperimentField(EnumXLSColumnHeader.EXP_BOXID));
			XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.EXP_EXPT.getValue(), transpose,
					exp.getExperimentField(EnumXLSColumnHeader.EXP_EXPT));
			XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.EXP_STIM.getValue(), transpose,
					exp.getExperimentField(EnumXLSColumnHeader.EXP_STIM));
			XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.EXP_CONC.getValue(), transpose,
					exp.getExperimentField(EnumXLSColumnHeader.EXP_CONC));
			XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.EXP_STRAIN.getValue(), transpose,
					exp.getExperimentField(EnumXLSColumnHeader.EXP_STRAIN));
			XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.EXP_SEX.getValue(), transpose,
					exp.getExperimentField(EnumXLSColumnHeader.EXP_SEX));
			XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.EXP_COND1.getValue(), transpose,
					exp.getExperimentField(EnumXLSColumnHeader.EXP_COND1));
			XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.EXP_COND2.getValue(), transpose,
					exp.getExperimentField(EnumXLSColumnHeader.EXP_COND2));

			XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAP_VOLUME.getValue(), transpose,
					exp.capillaries.capillariesDescription.volume);
			XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAP_PIXELS.getValue(), transpose,
					exp.capillaries.capillariesDescription.pixels);

			XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAP.getValue(), transpose,
					cap.getSideDescriptor(xlsExportOption));
			outputStimAndConc_according_to_DataOption(sheet, xlsExportOption, cap, transpose, x, y);

			XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAP_CAGEINDEX.getValue(), transpose, cap.capCellID);
			XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAGEID.getValue(), transpose,
					charSeries + cap.capCellID);
			XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAP_NFLIES.getValue(), transpose, cap.capNFlies);

			XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.DUM4.getValue(), transpose, sheetName);
			XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CHOICE_NOCHOICE.getValue(), transpose,
					desc_getChoiceTestType(capList, t));
			if (exp.cageBox.cellList.size() > t / 2) {
				Cell cell = exp.cageBox.cellList.get(t / 2);
				XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAGE_STRAIN.getValue(), transpose,
						cell.strCellStrain);
				XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAGE_SEX.getValue(), transpose, cell.strCellSex);
				XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAGE_AGE.getValue(), transpose, cell.cellAge);
				XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAGE_COMMENT.getValue(), transpose,
						cell.strCellComment);
			}
		}
		pt.x = col0;
		pt.y = rowmax + 1;
		return pt;
	}

	private String desc_getChoiceTestType(List<Capillary> capList, int t) {
		Capillary cap = capList.get(t);
		String choiceText = "..";
		String side = cap.getCapillarySide();
		if (side.contains("L"))
			t = t + 1;
		else
			t = t - 1;
		if (t >= 0 && t < capList.size()) {
			Capillary othercap = capList.get(t);
			String otherSide = othercap.getCapillarySide();
			if (!otherSide.contains(side)) {
				if (cap.capStimulus.equals(othercap.capStimulus)
						&& cap.capConcentration.equals(othercap.capConcentration))
					choiceText = "no-choice";
				else
					choiceText = "choice";
			}
		}
		return choiceText;
	}

	private void outputStimAndConc_according_to_DataOption(XSSFSheet sheet, EnumXLSExportType xlsExportOption,
			Capillary cap, boolean transpose, int x, int y) {
		switch (xlsExportOption) {
		case TOPLEVEL_LR:
		case TOPLEVELDELTA_LR:
		case SUMGULPS_LR:
			if (cap.getCapillarySide().equals("L"))
				XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAP_STIM.getValue(), transpose, "L+R");
			else
				XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAP_STIM.getValue(), transpose, "(L-R)/(L+R)");
			XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAP_CONC.getValue(), transpose,
					cap.capStimulus + ": " + cap.capConcentration);
			break;

		case TTOGULP_LR:
			if (cap.getCapillarySide().equals("L")) {
				XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAP_STIM.getValue(), transpose, "min_t_to_gulp");
			} else {
				XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAP_STIM.getValue(), transpose, "max_t_to_gulp");
			}
			XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAP_CONC.getValue(), transpose,
					cap.capStimulus + ": " + cap.capConcentration);
			break;

		default:
			XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAP_STIM.getValue(), transpose, cap.capStimulus);
			XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAP_CONC.getValue(), transpose, cap.capConcentration);
			break;
		}
	}

	int writeTopRow_descriptors(XSSFSheet sheet) {
		Point pt = new Point(0, 0);
		int x = 0;
		boolean transpose = options.transpose;
		int nextcol = -1;
		for (EnumXLSColumnHeader dumb : EnumXLSColumnHeader.values()) {
			XLSUtils.setValue(sheet, x, dumb.getValue(), transpose, dumb.getName());
			if (nextcol < dumb.getValue())
				nextcol = dumb.getValue();
		}
		pt.y = nextcol + 1;
		return pt.y;
	}

	void writeTopRow_timeIntervals(XSSFSheet sheet, int row, EnumXLSExportType xlsExport) {
		switch (xlsExport) {
		case AUTOCORREL:
		case CROSSCORREL:
		case AUTOCORREL_LR:
		case CROSSCORREL_LR:
			writeTopRow_timeIntervals_Correl(sheet, row);
			break;

		default:
			writeTopRow_timeIntervals_Default(sheet, row);
			break;
		}
	}

	void writeTopRow_timeIntervals_Correl(XSSFSheet sheet, int row) {
		boolean transpose = options.transpose;
		Point pt = new Point(0, row);
		long interval = -options.nbinscorrelation;
		while (interval < options.nbinscorrelation) {
			int i = (int) interval;
			XLSUtils.setValue(sheet, pt, transpose, "t" + i);
			pt.y++;
			interval += 1;
		}
	}

	void writeTopRow_timeIntervals_Default(XSSFSheet sheet, int row) {
		boolean transpose = options.transpose;
		Point pt = new Point(0, row);
		long duration = expAll.camImageLast_ms - expAll.camImageFirst_ms;
		long interval = 0;
		while (interval < duration) {
			int i = (int) (interval / options.buildExcelUnitMs);
			XLSUtils.setValue(sheet, pt, transpose, "t" + i);
			pt.y++;
			interval += options.buildExcelStepMs;
		}
	}

	protected int desc_getCellFromCapillaryName(String name) {
		if (!name.contains("line"))
			return -1;
		String num = name.substring(4, 5);
		int numFromName = Integer.valueOf(num);
		return numFromName;
	}

	protected int getRowIndexFromKymoFileName(String name) {
		if (!name.contains("line"))
			return -1;
		String num = name.substring(4, 5);
		int numFromName = Integer.valueOf(num);
		if (name.length() > 5) {
			String side = name.substring(5, 6);
			if (side != null) {
				if (side.equals("R")) {
					numFromName = numFromName * 2;
					numFromName += 1;
				} else if (side.equals("L"))
					numFromName = numFromName * 2;
			}
		}
		return numFromName;
	}

	protected int getRowIndexFromCellName(String name) {
		if (!name.contains("cage") || !name.contains("cell"))
			return -1;
		String num = name.substring(4, name.length());
		int numFromName = Integer.valueOf(num);
		return numFromName;
	}

	protected Point getCellXCoordinateFromDataName(XLSResults xlsResults, Point pt_main, int colseries) {
		int col = getRowIndexFromKymoFileName(xlsResults.name);
		if (col >= 0)
			pt_main.x = colseries + col;
		return pt_main;
	}

	protected int getCageFromKymoFileName(String name) {
		if (!name.contains("line"))
			return -1;
		return Integer.valueOf(name.substring(4, 5));
	}

	XSSFWorkbook xlsInitWorkbook() {
		XSSFWorkbook workbook = new XSSFWorkbook();
		workbook.setMissingCellPolicy(Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
		xssfCellStyle_red = workbook.createCellStyle();
		font_red = workbook.createFont();
		font_red.setColor(HSSFColor.HSSFColorPredefined.RED.getIndex());
		xssfCellStyle_red.setFont(font_red);

		xssfCellStyle_blue = workbook.createCellStyle();
		font_blue = workbook.createFont();
		font_blue.setColor(HSSFColor.HSSFColorPredefined.BLUE.getIndex());
		xssfCellStyle_blue.setFont(font_blue);
		return workbook;
	}

	XSSFSheet xlsInitSheet(String title, EnumXLSExportType xlsExport) {
		XSSFSheet sheet = workbook.getSheet(title);
		if (sheet == null) {
			sheet = workbook.createSheet(title);
			int row = writeTopRow_descriptors(sheet);
			writeTopRow_timeIntervals(sheet, row, xlsExport);
		}
		return sheet;
	}

	public XLSResults getResultsArrayWithThatName(String testname, XLSResultsArray resultsArrayList) {
		XLSResults resultsFound = null;
		for (XLSResults results : resultsArrayList.resultsList) {
			if (results.name.equals(testname)) {
				resultsFound = results;
				break;
			}
		}
		return resultsFound;
	}

	public double padWithLastPreviousValue(XLSResults row, long to_first_index) {
		double dvalue = 0;
		if (to_first_index >= row.valuesOut.length)
			return dvalue;

		int index = getIndexOfFirstNonEmptyValueBackwards(row, to_first_index);
		if (index >= 0) {
			dvalue = row.valuesOut[index];
			for (int i = index + 1; i < to_first_index; i++) {
				row.valuesOut[i] = dvalue;
				row.padded_out[i] = true;
			}
		}
		return dvalue;
	}

	private int getIndexOfFirstNonEmptyValueBackwards(XLSResults row, long fromindex) {
		int index = -1;
		int ifrom = (int) fromindex;
		for (int i = ifrom; i >= 0; i--) {
			if (!Double.isNaN(row.valuesOut[i])) {
				index = i;
				break;
			}
		}
		return index;
	}

	protected Point writeExperiment_data(XLSResultsArray rowListForOneExp, XSSFSheet sheet, EnumXLSExportType option,
			Point pt_main) {
		int rowSeries = pt_main.x + 2;
		int column_dataArea = pt_main.y;
		Point pt = new Point(pt_main);
		writeExperiment_data_as_rows(rowListForOneExp, sheet, column_dataArea, rowSeries, pt);
		pt_main.x = pt.x + 1;
		return pt_main;
	}

	
	private void writeExperiment_data_as_rows(XLSResultsArray rowListForOneExp, XSSFSheet sheet, int column_dataArea,
			int rowSeries, Point pt) {
		for (int iRow = 0; iRow < rowListForOneExp.size(); iRow++) {
			XLSResults row = rowListForOneExp.getRow(iRow);
			writeExperiment_data_single_row(sheet, column_dataArea, rowSeries, pt, row);
		}
	}
	

	private void writeExperiment_data_single_row(XSSFSheet sheet, int column_dataArea, int rowSeries, Point pt, XLSResults row) {
		boolean transpose = options.transpose;
		pt.y = column_dataArea;
		int col = getRowIndexFromKymoFileName(row.name);
		pt.x = rowSeries + col;
		if (row.valuesOut == null)
			return;

		for (long coltime = expAll.camImageFirst_ms; coltime < expAll.camImageLast_ms; coltime += options.buildExcelStepMs, pt.y++) {
			int i_from = (int) ((coltime - expAll.camImageFirst_ms) / options.buildExcelStepMs);
			if (i_from >= row.valuesOut.length)
				break;
			double value = row.valuesOut[i_from];
			if (!Double.isNaN(value)) {
				XLSUtils.setValue(sheet, pt, transpose, value);
				if (i_from < row.padded_out.length && row.padded_out[i_from])
					XLSUtils.getCell(sheet, pt, transpose).setCellStyle(xssfCellStyle_red);
			}
		}
		pt.x++;
	}
	
	protected int xlsExportCapillaryResultsArrayToSheet(XLSResultsArray rowListForOneExp, XSSFSheet sheet,
			EnumXLSExportType xlsExportOption, int col0, String charSeries) {
		Point pt = new Point(col0, 0);
		writeExperiment_descriptors(expAll, charSeries, sheet, pt, xlsExportOption);
		pt = writeExperiment_data(rowListForOneExp, sheet, xlsExportOption, pt);
		return pt.x;
	}
	
	protected void addResultsTo_rowsForOneExp(XLSResultsArray rowListForOneExp, Experiment expi,
			XLSResultsArray resultsArrayList) {
		if (resultsArrayList.resultsList.size() < 1)
			return;

		EnumXLSExportType xlsoption = resultsArrayList.getRow(0).exportType;

		long offsetChain = expi.camImageFirst_ms - expi.chainImageFirst_ms;
		long start_Ms = expi.kymoFirst_ms + offsetChain; // TODO check when collate?
		long end_Ms = expi.kymoLast_ms + offsetChain;
		if (options.fixedIntervals) {
			if (start_Ms < options.startAll_Ms)
				start_Ms = options.startAll_Ms;
			if (start_Ms > expi.camImageLast_ms)
				return;

			if (end_Ms > options.endAll_Ms)
				end_Ms = options.endAll_Ms;
			if (end_Ms > expi.camImageFirst_ms)
				return;
		}

		// TODO check this
		final long from_first_Ms = start_Ms - offsetChain;
		final long from_lastMs = end_Ms - offsetChain;
		final int to_first_index = (int) (start_Ms / options.buildExcelStepMs);
		final int to_nvalues = (int) ((end_Ms - start_Ms) / options.buildExcelStepMs) + 1;

		for (int iRow = 0; iRow < rowListForOneExp.size(); iRow++) {
			XLSResults row = rowListForOneExp.getRow(iRow);
			XLSResults results = getResultsArrayWithThatName(row.name, resultsArrayList);
			if (results != null && results.valuesOut != null) {
				double dvalue = 0.;
				switch (xlsoption) {
				case TOPLEVEL:
				case TOPLEVEL_LR:
				case SUMGULPS:
				case SUMGULPS_LR:
				case TOPLEVELDELTA:
				case TOPLEVELDELTA_LR:
					if (options.collateSeries && options.padIntervals && expi.chainToPreviousExperiment != null)
						dvalue = padWithLastPreviousValue(row, to_first_index);
					break;
				default:
					break;
				}

				int icolTo = 0;
				if (options.collateSeries || options.absoluteTime)
					icolTo = to_first_index;
				for (long fromTime = from_first_Ms; fromTime <= from_lastMs; fromTime += options.buildExcelStepMs, icolTo++) {
					int from_i = (int) Math
							.round(((double) (fromTime - from_first_Ms)) / ((double) options.buildExcelStepMs));
					if (from_i >= results.valuesOut.length)
						break;
					// TODO check how this can happen
					if (from_i < 0)
						continue;
					double value = results.valuesOut[from_i] + dvalue;
					if (icolTo >= row.valuesOut.length)
						break;
					row.valuesOut[icolTo] = value;
				}

			} else {
				if (options.collateSeries && options.padIntervals && expi.chainToPreviousExperiment != null) {
					double dvalue = padWithLastPreviousValue(row, to_first_index);
					int tofirst = (int) to_first_index;
					int tolast = (int) (tofirst + to_nvalues);
					if (tolast > row.valuesOut.length)
						tolast = row.valuesOut.length;
					for (int toi = tofirst; toi < tolast; toi++)
						row.valuesOut[toi] = dvalue;
				}
			}
		}
	}

}
