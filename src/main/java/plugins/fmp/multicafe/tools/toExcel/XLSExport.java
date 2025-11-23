package plugins.fmp.multicafe.tools.toExcel;

import java.awt.Point;
import java.util.List;

import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import plugins.fmp.multicafe.experiment.CombinedExperiment;
import plugins.fmp.multicafe.experiment.Experiment;
import plugins.fmp.multicafe.experiment.cages.Cage;
import plugins.fmp.multicafe.experiment.capillaries.Capillary;
import plugins.fmp.multicafe.tools.JComponents.ExperimentsJComboBox;

public class XLSExport {
	protected XLSExportOptions options = null;
	protected CombinedExperiment expAll = null;

	XSSFCellStyle xssfCellStyle_red = null;
	XSSFCellStyle xssfCellStyle_blue = null;
	XSSFFont font_red = null;
	XSSFFont font_blue = null;
	XSSFWorkbook workbook = null;
	ExperimentsJComboBox expList = null;

	// ------------------------------------------------

	int writeTop_descriptors(XSSFSheet sheet, EnumXLSExport xlsExport) {
		Point pt = new Point(0, 0);
		int x = 0;
		boolean transpose = options.transpose;
		int columnIndex = 0;
		for (EnumXLSColumnHeader dumb : EnumXLSColumnHeader.values()) {
			EnumXLSMeasure columnType = dumb.toType();
			dumb.setValue(columnIndex);
			if (columnType == EnumXLSMeasure.COMMON || xlsExport.toType() == columnType) {
				XLSUtils.setValue(sheet, x, columnIndex, transpose, dumb.getName());
				columnIndex++;
			}
		}
		pt.y = columnIndex;
		return pt.y;
	}

	void writeTop_timeIntervals(XSSFSheet sheet, int row, EnumXLSExport xlsExport) {
		switch (xlsExport) {
		case AUTOCORREL:
		case CROSSCORREL:
		case AUTOCORREL_LR:
		case CROSSCORREL_LR:
			writeTop_timeIntervals_Correl(sheet, row);
			break;

		default:
			writeTop_timeIntervals_Default(sheet, row);
			break;
		}
	}

	void writeTop_timeIntervals_Correl(XSSFSheet sheet, int row) {
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

	void writeTop_timeIntervals_Default(XSSFSheet sheet, int row) {
		boolean transpose = options.transpose;
		Point pt = new Point(0, row);
		long duration = expAll.getCamImageLast_ms() - expAll.getCamImageFirst_ms();
		long interval = 0;
		while (interval < duration) {
			int i = (int) (interval / options.buildExcelUnitMs);
			XLSUtils.setValue(sheet, pt, transpose, "t" + i);
			pt.y++;
			interval += options.buildExcelStepMs;
		}
	}

	protected int desc_getIndex_CageFromCapillaryName(String name) {
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

	protected int getRowIndexFromCageName(String name) {
		if (!name.contains("cage") && !name.contains("cell"))
			return -1;
		String num = name.substring(4, name.length());
		int numFromName = Integer.valueOf(num);
		return numFromName;
	}

	protected Point getCageXCoordinateFromDataName(XLSResults xlsResults, Point pt_main, int colseries) {
		int col = getRowIndexFromKymoFileName(xlsResults.name);
		if (col >= 0)
			pt_main.x = colseries + col;
		return pt_main;
	}

	protected int getCageIndexFromKymoFileName(String name) {
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

	XSSFSheet xlsGetSheet(String title, EnumXLSExport xlsExport) {
		XSSFSheet sheet = workbook.getSheet(title);
		if (sheet == null) {
			sheet = workbook.createSheet(title);
			int row = writeTop_descriptors(sheet, xlsExport);
			writeTop_timeIntervals(sheet, row, xlsExport);
			sheet.setActiveCell(new CellAddress(1, 0));
		}
		return sheet;
	}

	protected Point writeExperiment_data(XLSResultsArray rowListForOneExp, XSSFSheet sheet, EnumXLSExport option,
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

	private void writeExperiment_data_single_row(XSSFSheet sheet, int column_dataArea, int rowSeries, Point pt,
			XLSResults row) {
		boolean transpose = options.transpose;
		pt.y = column_dataArea;
		int col = getRowIndexFromKymoFileName(row.name);
		pt.x = rowSeries + col;
		if (row.valuesOut == null)
			return;

		for (long coltime = expAll.getCamImageFirst_ms(); coltime < expAll.getCamImageLast_ms(); coltime += options.buildExcelStepMs, pt.y++) {
			int i_from = (int) ((coltime - expAll.getCamImageFirst_ms()) / options.buildExcelStepMs);
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

	protected void exportExperimentField(XSSFSheet sheet, int x, int y, boolean transpose, Experiment exp,
			EnumXLSColumnHeader colHeader) {
		XLSUtils.setValue(sheet, x, y + colHeader.getValue(), transpose, exp.getExperimentField(colHeader));
	}

	protected void XLSExportExperimentParameters(XSSFSheet sheet, boolean transpose, int x, int y, String charSeries,
			Experiment exp) {
		exportExperimentField(sheet, x, y, transpose, exp, EnumXLSColumnHeader.EXP_PATH);
		exportExperimentField(sheet, x, y, transpose, exp, EnumXLSColumnHeader.EXP_DATE);
		exportExperimentField(sheet, x, y, transpose, exp, EnumXLSColumnHeader.EXP_CAM);
		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.EXP_BOXID.getValue(), transpose, charSeries);
		exportExperimentField(sheet, x, y, transpose, exp, EnumXLSColumnHeader.EXP_EXPT);
		exportExperimentField(sheet, x, y, transpose, exp, EnumXLSColumnHeader.EXP_STIM);
		exportExperimentField(sheet, x, y, transpose, exp, EnumXLSColumnHeader.EXP_CONC);
		exportExperimentField(sheet, x, y, transpose, exp, EnumXLSColumnHeader.EXP_STRAIN);
		exportExperimentField(sheet, x, y, transpose, exp, EnumXLSColumnHeader.EXP_SEX);
		exportExperimentField(sheet, x, y, transpose, exp, EnumXLSColumnHeader.EXP_COND1);
		exportExperimentField(sheet, x, y, transpose, exp, EnumXLSColumnHeader.EXP_COND2);
	}

	protected void xlsExportCageParameters(XSSFSheet sheet, boolean transpose, int x, int y, String charSeries,
			Experiment exp, Cage cage) {
		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAGE_INDEX.getValue(), transpose, cage.getCageIDasString());
		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAGE_ID.getValue(), transpose,
				charSeries + "_" + cage.getCageID());
		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAGE_STRAIN.getValue(), transpose, cage.cageStrain);
		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAGE_SEX.getValue(), transpose, cage.cageSex);
		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAGE_AGE.getValue(), transpose, cage.cageAge);
		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAGE_COMMENT.getValue(), transpose, cage.cageComment);
	}

	protected void XLSExportCapillaryParameters(XSSFSheet sheet, boolean transpose, int x, int y, String charSeries,
			Experiment exp, Capillary cap, EnumXLSExport xlsExportOption, int index) {
		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAP.getValue(), transpose,
				cap.getSideDescriptor(xlsExportOption));
		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAP_INDEX.getValue(), transpose,
				charSeries + "_" + cap.getLast2ofCapillaryName());
		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAP_VOLUME.getValue(), transpose,
				exp.getCapillaries().capillariesDescription.volume);
		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAP_PIXELS.getValue(), transpose,
				exp.getCapillaries().capillariesDescription.pixels);

		outputStimAndConc_according_to_DataOption(sheet, xlsExportOption, cap, transpose, x, y);

		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAP_NFLIES.getValue(), transpose, cap.capNFlies);
		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CHOICE_NOCHOICE.getValue(), transpose,
				desc_getChoiceTestType(exp.getCapillaries().capillariesList, index));
	}

	private void outputStimAndConc_according_to_DataOption(XSSFSheet sheet, EnumXLSExport xlsExportOption,
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

	private String desc_getChoiceTestType(List<Capillary> capList, int index) {
		Capillary cap = capList.get(index);
		String choiceText = "..";
		String side = cap.getCapillarySide();
		if (side.contains("L"))
			index = index + 1;
		else
			index = index - 1;
		if (index >= 0 && index < capList.size()) {
			Capillary othercap = capList.get(index);
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

	protected int writeSeparator_Between_Experiments(XSSFSheet sheet, Point pt, boolean transpose) {
		XLSUtils.setValue(sheet, pt, transpose, "..");
		pt.x++;
		XLSUtils.setValue(sheet, pt, transpose, "..");
		pt.x++;
		return pt.x;
	}

}
