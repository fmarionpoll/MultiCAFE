package plugins.fmp.multicafeTools;

import java.awt.Point;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import plugins.fmp.multicafeSequence.Cage;
import plugins.fmp.multicafeSequence.Capillary;
import plugins.fmp.multicafeSequence.Experiment;
import plugins.fmp.multicafeSequence.ExperimentList;
import plugins.fmp.multicafeSequence.SequenceCamData;



public class XLSExport {

	protected XLSExportOptions 	options 			= null;
	protected Experiment 		expAll 				= null;

	XSSFCellStyle 				xssfCellStyle_red 	= null;
	XSSFCellStyle 				xssfCellStyle_blue 	= null;
    XSSFFont 					font_red 			= null;
    XSSFFont 					font_blue 			= null;
    XSSFWorkbook 				workbook			= null;		
    
	ExperimentList 				expList 	= null;
	List <XLSResults> 			rowsForOneExp = new ArrayList <XLSResults> ();


	// ------------------------------------------------
    
	public long getnearest(long value, int step) {
		long diff0 = (value /step)*step;
		long diff1 = diff0 + step;
		if ((value - diff0 ) < (diff1 - value))
			value = diff0;
		else
			value = diff1;
		return value;
	}
		
	protected Point writeExperimentDescriptors(Experiment exp, String charSeries, XSSFSheet sheet, Point pt, EnumXLSExportType xlsExportOption) {
		boolean transpose = options.transpose;
		int row = pt.y;
		int col0 = pt.x;
		XLSUtils.setValue(sheet, pt, transpose, "..");
		pt.x++;
		XLSUtils.setValue(sheet, pt, transpose, "..");
		pt.x++;
		int colseries = pt.x;
		for (int i = 0; i < 18; i++) {
			XLSUtils.setValue(sheet, pt, transpose, "--");
			pt.x++;
		}
		pt.x = colseries;
		
		String filename = exp.getExperimentFileName();
		if (filename == null)
			filename = exp.seqCamData.getDirectory();
		Path path = Paths.get(filename);

		SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy");
		String date = df.format(exp.getFileTimeImageFirst(false).toMillis());
		
		int subpath_i = 2;
		String name0 = path.toString();
		if (name0 .contains("grabs"))
			subpath_i++;
		String name1 = exp.getSubName(path, subpath_i);
		
		int pos = name0.indexOf("cam");
		String cam = "-"; 
		if (pos > 0) {
			int pos5 = pos+5;
			if (pos5 >= name0.length())
				pos5 = name0.length() -1;
			cam = name0.substring(pos, pos5);
		}
		
		String sheetName = sheet.getSheetName();
		
		int rowmax = -1;
		for (EnumXLSColumnHeader dumb: EnumXLSColumnHeader.values()) {
			if (rowmax < dumb.getValue())
				rowmax = dumb.getValue();		
		}
		
		List<Capillary> capList = exp.capillaries.capillariesArrayList;
		for (int t=0; t< capList.size(); t++) { 
			Capillary cap = capList.get(t);
			String	name = cap.roi.getName();
			int col = getColFromKymoFileName(name);
			if (col >= 0) 
				pt.x = colseries + col;
			int x = pt.x;
			int y = row;
			XLSUtils.setValue(sheet, x, y+EnumXLSColumnHeader.PATH.getValue(), transpose, 		name0);
			XLSUtils.setValue(sheet, x, y+EnumXLSColumnHeader.DATE.getValue(), transpose, 		date);
			XLSUtils.setValue(sheet, x, y+EnumXLSColumnHeader.BOXID.getValue(), transpose, 		exp.exp_boxID);
			XLSUtils.setValue(sheet, x, y+EnumXLSColumnHeader.EXPMT.getValue(), transpose, 		exp.experiment);
			XLSUtils.setValue(sheet, x, y+EnumXLSColumnHeader.COMMENT1.getValue(), transpose, 	exp.comment1);

			XLSUtils.setValue(sheet, x, y+EnumXLSColumnHeader.CAP.getValue(), transpose, cap.getSideDescriptor(xlsExportOption));
			switch (xlsExportOption) {
			case TOPLEVEL_LR:
			case TOPLEVELDELTA_LR:
			case SUMGULPS_LR:
				if (cap.getCapillarySide().equals("L")) {
					XLSUtils.setValue(sheet, x, y+EnumXLSColumnHeader.CAPSTIM.getValue(), transpose, "L+R");
					XLSUtils.setValue(sheet, x, y+EnumXLSColumnHeader.CAPCONC.getValue(), transpose, cap.capStimulus + ": "+ cap.capConcentration);
				} else {
					XLSUtils.setValue(sheet, x, y+EnumXLSColumnHeader.CAPSTIM.getValue(), transpose, 	"(L-R)/(L+R)");
					XLSUtils.setValue(sheet, x, y+EnumXLSColumnHeader.CAPCONC.getValue(), transpose, cap.capStimulus + ": "+ cap.capConcentration);
				}
				break;
			default:
				XLSUtils.setValue(sheet, x, y+EnumXLSColumnHeader.CAPSTIM.getValue(), transpose, 	cap.capStimulus);
				XLSUtils.setValue(sheet, x, y+EnumXLSColumnHeader.CAPCONC.getValue(), transpose, 	cap.capConcentration);	
				break;
			}

			XLSUtils.setValue(sheet, x, y+EnumXLSColumnHeader.CAM.getValue(), transpose, 		cam);
			XLSUtils.setValue(sheet, x, y+EnumXLSColumnHeader.CAGEINDEX.getValue(), transpose, 	cap.capCageID);
			XLSUtils.setValue(sheet, x, y+EnumXLSColumnHeader.CAGEID.getValue(), transpose, 	charSeries+cap.capCageID);
			XLSUtils.setValue(sheet, x, y+EnumXLSColumnHeader.NFLIES.getValue(), transpose, 	cap.capNFlies);
			XLSUtils.setValue(sheet, x, y+EnumXLSColumnHeader.DUM1.getValue(), transpose, 		name1);
			XLSUtils.setValue(sheet, x, y+EnumXLSColumnHeader.CAPVOLUME.getValue(), transpose, 	exp.capillaries.desc.volume);
			XLSUtils.setValue(sheet, x, y+EnumXLSColumnHeader.CAPPIXELS.getValue(), transpose, 	exp.capillaries.desc.pixels);
			XLSUtils.setValue(sheet, x, y+EnumXLSColumnHeader.DUM4.getValue(), transpose, 		sheetName);
			XLSUtils.setValue(sheet, x, y+EnumXLSColumnHeader.COMMENT2.getValue(), transpose, 	exp.comment2);
		}
		pt.x = col0;
		pt.y = rowmax +1;
		return pt;
	}
	
	public int outputFieldDescriptors(XSSFSheet sheet) {		
		Point pt = new Point(0,0);
		int x = 0;
		boolean transpose = options.transpose;
		int nextcol = -1;
		for (EnumXLSColumnHeader dumb: EnumXLSColumnHeader.values()) {
			XLSUtils.setValue(sheet, x, dumb.getValue(), transpose, dumb.getName());
			if (nextcol < dumb.getValue())
				nextcol = dumb.getValue();
		}
		pt.y = nextcol+1;
		return pt.y;
	}
	
	protected int getCageFromCapillaryName(String name) {
		if (!name .contains("line"))
			return -1;
	
		String num = name.substring(4, 5);
		int numFromName = Integer.parseInt(num);
		return numFromName;
	}
	
	protected String getShortenedName(SequenceCamData seq, int t) {
		return seq.getFileNameNoPath(t);
	}
	
	protected int getColFromKymoFileName(String name) {
		if (!name .contains("line"))
			return -1;
		String num = name.substring(4, 5);
		int numFromName = Integer.parseInt(num);
		if( name.length() > 5) {
			String side = name.substring(5, 6);
			if (side != null) {
				if (side .equals("R")) {
					numFromName = numFromName* 2;
					numFromName += 1;
				}
				else if (side .equals("L"))
					numFromName = numFromName* 2;
			}
		}
		return numFromName;
	}
		
	protected int getColFromCageName(String name) {
		if (!name .contains("cage"))
			return -1;
		String num = name.substring(4, name.length());
		int numFromName = Integer.parseInt(num);
		return numFromName;
	}
	
	protected Point getCellXCoordinateFromDataName(XLSResults xlsResults, Point pt_main, int colseries) {
		int col = getColFromKymoFileName(xlsResults.name);
		if (col >= 0)
			pt_main.x = colseries + col;
		return pt_main;
	}
	
	protected int getCageFromKymoFileName(String name) {
		if (!name .contains("line"))
			return -1;
		return Integer.parseInt(name.substring(4, 5));
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
	
	XSSFSheet xlsInitSheet(String title) {
		XSSFSheet sheet = workbook.getSheet(title);
		if (sheet == null) {
			sheet = workbook.createSheet(title);
			int row = outputFieldDescriptors(sheet);
			outputDataTimeIntervals(sheet, row);
		}
		return sheet;
	}
	
	void outputDataTimeIntervals(XSSFSheet sheet, int row) {
		boolean transpose = options.transpose;
		Point pt = new Point(0, row);
		for (int i = expAll.getKymoFrameStart(); i <= expAll.getKymoFrameEnd(); i += options.buildExcelBinStep) {
			XLSUtils.setValue(sheet, pt, transpose, "t"+i);
			pt.y++;
		}
	}
	
	protected int getDataAndExport(Experiment exp, int col0, String charSeries, EnumXLSExportType xlsOption) {	
		getDataFromOneSeriesOfExperiments(exp, xlsOption);
		XSSFSheet sheet = xlsInitSheet(xlsOption.toString());
		int colmax = xlsExportResultsArrayToSheet(sheet, xlsOption, col0, charSeries);
		
		if (options.onlyalive) {
			trimDeadsFromArrayList(exp);
			sheet = xlsInitSheet(xlsOption.toString()+"_alive");
			xlsExportResultsArrayToSheet(sheet, xlsOption, col0, charSeries);
		}
		
		if (options.cage) {
			combineData(exp);
			sheet = xlsInitSheet(xlsOption.toString()+"_cage");
			xlsExportResultsArrayToSheet(sheet, xlsOption, col0, charSeries);
		}
		
		return colmax;
	}
	
	private void getDataFromOneSeriesOfExperiments(Experiment exp, EnumXLSExportType xlsOption) {	
		// loop to get all capillaries into expAll and init rows for this experiment
		expAll.capillaries.copy(exp.capillaries);
		expAll.fileTimeImageFirst 	= exp.fileTimeImageFirst;
		expAll.fileTimeImageLast 	= exp.fileTimeImageLast;
		expAll.setExperimentFileName(exp.getExperimentFileName());
		expAll.exp_boxID 			= exp.exp_boxID;
		expAll.experiment 			= exp.experiment;
		expAll.comment1 			= exp.comment1;
		expAll.comment2 			= exp.comment2;	

		Experiment expi = exp.nextExperiment;
		while (expi != null ) {
			expAll.capillaries.mergeLists(expi.capillaries);
			expAll.fileTimeImageLast = expi.fileTimeImageLast;
			expi = expi.nextExperiment;
		}
		expAll.fileTimeImageFirstMinute = (long) (expAll.fileTimeImageFirst.toMillis()/60000d);
		expAll.fileTimeImageLastMinute = (long) (expAll.fileTimeImageLast.toMillis()/60000d);
		int nFrames = (expAll.getKymoFrameEnd() - expAll.getKymoFrameStart())/expAll.getKymoFrameStep() +1 ;
		
		int ncapillaries = expAll.capillaries.capillariesArrayList.size();
		rowsForOneExp = new ArrayList <XLSResults> (ncapillaries);
		for (int i=0; i< ncapillaries; i++) {
			Capillary cap = expAll.capillaries.capillariesArrayList.get(i);
			XLSResults row = new XLSResults (cap.roi.getName(), cap.capNFlies, xlsOption, nFrames, expAll.getKymoFrameStep());
			row.stimulus = cap.capStimulus;
			row.concentration = cap.capConcentration;
			row.cageID = cap.capCageID;
			rowsForOneExp.add(row);
		}
		Collections.sort(rowsForOneExp, new Comparators.XLSResultsComparator());
				
		// load data for one experiment - assume that exp = first experiment in the chain and iterate through the chain
		expi = exp;
		EnumXLSExportType getOption = null;
		switch (xlsOption) {
		case TOPRAW:
		case TOPLEVEL_LR:
		case TOPLEVELDELTA:
		case TOPLEVELDELTA_LR:
			getOption = EnumXLSExportType.TOPLEVEL;
			break;
		case SUMGULPS_LR:
			getOption = EnumXLSExportType.SUMGULPS;
			break;
		default:
			getOption = xlsOption;
			break;
		}
		
		while (expi != null) {
			expi.resultsSubPath = expAll.resultsSubPath;
			XLSResultsArray resultsArrayList = new XLSResultsArray (expi.capillaries.capillariesArrayList.size());
			
			switch (xlsOption) {
				case TOPRAW:
				case BOTTOMLEVEL:
				case ISGULPS:
				case TTONEXTGULP:
				case TTONEXTGULP_LR:
					for (Capillary cap: expi.capillaries.capillariesArrayList) {
						resultsArrayList.checkIfSameStimulusAndConcentration(cap);
						XLSResults results = new XLSResults(cap.roi.getName(), cap.capNFlies, xlsOption, expi.getKymoFrameStep());
						results.data = cap.getMeasures(getOption);
						resultsArrayList.add(results);
					}
					break;
					
				case TOPLEVEL:
				case TOPLEVEL_LR:
				case TOPLEVELDELTA:
				case TOPLEVELDELTA_LR:
					for (Capillary cap: expi.capillaries.capillariesArrayList) {
						resultsArrayList.checkIfSameStimulusAndConcentration(cap);
						XLSResults results = new XLSResults(cap.roi.getName(), cap.capNFlies, xlsOption, expi.getKymoFrameStep());
						if (options.t0) 
							results.data = exp.seqKymos.subtractT0(cap.getMeasures(getOption));
						else
							results.data = cap.getMeasures(getOption);
						resultsArrayList.add(results);
					}
					if (options.subtractEvaporation)
						resultsArrayList.subtractEvaporation();
					break;
					
				case DERIVEDVALUES:
				case SUMGULPS:
				case SUMGULPS_LR:
					for (Capillary cap: expi.capillaries.capillariesArrayList) {
						resultsArrayList.checkIfSameStimulusAndConcentration(cap);
						XLSResults results = new XLSResults(cap.roi.getName(), cap.capNFlies, xlsOption, expi.getKymoFrameStep());
						results.data = cap.getMeasures(getOption);
						resultsArrayList.add(results);
					}
					if (options.subtractEvaporation)
						resultsArrayList.subtractEvaporation();
					break;
				default:
					break;
			}
				
			addResultsTo_rowsForOneExp(expi, resultsArrayList);
			expi = expi.nextExperiment;
		}
		
		switch (xlsOption) {
			case TOPLEVELDELTA:
			case TOPLEVELDELTA_LR:
				for (XLSResults row: rowsForOneExp ) 
					row.subtractDeltaT(expAll.getKymoFrameStep(), options.buildExcelBinStep);
				break;
			default:
				break;
		}
	}
	
	private XLSResults getResultsArrayWithThatName(String testname, XLSResultsArray resultsArrayList) {
		XLSResults resultsFound = null;
		for (XLSResults results: resultsArrayList.resultsArrayList) {
			if (results.name.equals(testname)) {
				resultsFound = results;
				break;
			}
		}
		return resultsFound;
	}
	
	private void addResultsTo_rowsForOneExp(Experiment expi, XLSResultsArray resultsArrayList) {
		if (resultsArrayList.resultsArrayList.size() <1)
			return;
		EnumXLSExportType xlsoption = resultsArrayList.get(0).exportType;
		double scalingFactorToPhysicalUnits = expi.capillaries.desc.volume / expi.capillaries.desc.pixels;
		switch (xlsoption) {
			case ISGULPS:
			case TTONEXTGULP:
			case TTONEXTGULP_LR:
				scalingFactorToPhysicalUnits = 1.;
				break;
			default:
				break;
		}
		
		long to_first_index = (expi.fileTimeImageFirstMinute - expAll.fileTimeImageFirstMinute) / expAll.getKymoFrameStep() ;
		long to_nvalues = ((expi.fileTimeImageLastMinute - expi.fileTimeImageFirstMinute)/expi.getKymoFrameStep())+1;
		for (XLSResults row: rowsForOneExp ) {
			XLSResults results = getResultsArrayWithThatName(row.name,  resultsArrayList);
			if (results != null && results.data != null) {
				double dvalue = 0.;
				switch (xlsoption) {
					case TOPLEVEL:
					case TOPLEVEL_LR:
					case SUMGULPS:
					case SUMGULPS_LR:
					case TOPLEVELDELTA:
					case TOPLEVELDELTA_LR:
						if (options.collateSeries && options.padIntervals && expi.previousExperiment != null) 
							dvalue = padWithLastPreviousValue(row, to_first_index);
						break;
					default:
						break;
				}

				for (int fromTime = expi.getKymoFrameStart(); fromTime <= expi.getKymoFrameEnd(); fromTime += expi.getKymoFrameStep()) {
					int from_i = fromTime / expi.getKymoFrameStep();
					if (from_i >= results.data.size())
						break;
					double value = results.data.get(from_i) * scalingFactorToPhysicalUnits + dvalue;
					int to_i = (int) (fromTime + expi.fileTimeImageFirstMinute - expAll.fileTimeImageFirstMinute) / expAll.getKymoFrameStep() ;
					if (to_i >= row.values_out.length)
						break;
					if (to_i < 0)
						break;
					row.values_out[to_i]= value;
				}
				
			} else {
				if (options.collateSeries && options.padIntervals && expi.previousExperiment != null) {
					double dvalue = padWithLastPreviousValue(row, to_first_index);
					int tofirst = (int) to_first_index;
					int tolast = (int) (tofirst + to_nvalues);
					if (tolast > row.values_out.length)
						tolast = row.values_out.length;
					for (int toi = tofirst; toi < tolast; toi++) {
						row.values_out[toi]= dvalue;
					}
				}
			}
		}
	}
	
	private double padWithLastPreviousValue(XLSResults row, long to_first_index) {
		double dvalue = 0;
		int index = getIndexOfFirstNonEmptyValueBackwards(row, to_first_index);
		if (index >= 0) {
			dvalue = row.values_out[index];
			for (int i=index+1; i< to_first_index; i++) {
				row.values_out[i] = dvalue;
				row.padded_out[i] = true;
			}
		}
		return dvalue;
	}
	
	private int getIndexOfFirstNonEmptyValueBackwards(XLSResults row, long fromindex) {
		int index = -1;
		int ifrom = (int) fromindex;
		for (int i= ifrom; i>= 0; i--) {
			if (!Double.isNaN(row.values_out[i])) {
				index = i;
				break;
			}
		}
		return index;
	}
	
	private void trimDeadsFromArrayList(Experiment exp) {
		for (Cage cage: exp.cages.cageList) {
			String cagenumberString = cage.roi.getName().substring(4);
			int cagenumber = Integer.parseInt(cagenumberString);
			int ilastalive = 0;
			if (cage.cageNFlies > 0) {
				Experiment expi = exp;
				while (expi.nextExperiment != null && expi.nextExperiment.isFlyAlive(cagenumber)) {
					expi = expi.nextExperiment;
				}
				int lastIntervalFlyAlive = expi.getLastIntervalFlyAlive(cagenumber);
				int lastMinuteAlive = (int) (lastIntervalFlyAlive * expi.getKymoFrameStep() 
						+ (expi.fileTimeImageFirstMinute - expAll.fileTimeImageFirstMinute));		
				ilastalive = lastMinuteAlive / expAll.getKymoFrameStep();
			}
			for (XLSResults row : rowsForOneExp) {
				if (getCageFromCapillaryName (row.name) == cagenumber) {
					row.clearValues(ilastalive+1);
				}
			}
		}	
	}
	
	private void combineData(Experiment exp) {
		for (XLSResults row_master : rowsForOneExp) {
			if (row_master.nflies == 0 || row_master.values_out == null)
				continue;
			for (XLSResults row : rowsForOneExp) {
				if (row.nflies == 0 || row.values_out == null)
					continue;
				if (row.cageID != row_master.cageID)
					continue;
				if (row.name .equals(row_master.name))
					continue;
				if (row.stimulus .equals(row_master.stimulus) && row.concentration .equals(row_master.concentration)) {
					row_master.addValues_out(row);
					row.clearAll();
				}
			}
		}
	}
	
	private int xlsExportResultsArrayToSheet(XSSFSheet sheet, EnumXLSExportType xlsExportOption, int col0, String charSeries) {
		Point pt = new Point(col0, 0);
		writeExperimentDescriptors(expAll, charSeries, sheet, pt, xlsExportOption);
		pt = writeData2(sheet, xlsExportOption, pt);
		return pt.x;
	}
			
	private Point writeData2 (XSSFSheet sheet, EnumXLSExportType option, Point pt_main) {
		int rowseries = pt_main.x +2;
		int columndataarea = pt_main.y;
		Point pt = new Point(pt_main);
		switch (option) {
			case TOPLEVEL_LR:
			case TOPLEVELDELTA_LR:
			case SUMGULPS_LR:
				writeLRRows(sheet, columndataarea, rowseries, pt);
				break;
			case TTONEXTGULP_LR:
				writeTOGulpLR(sheet, columndataarea, rowseries, pt);
				break;
			default:
				writeSimpleRows(sheet, columndataarea, rowseries, pt);
				break;
		}			
		pt_main.x = pt.x+1;
		return pt_main;
	}
	
	private void writeSimpleRows(XSSFSheet sheet, int column_dataArea, int rowSeries, Point pt) {
		boolean transpose = options.transpose;
		for (XLSResults row: rowsForOneExp) {
			pt.y = column_dataArea;
			int col = getColFromKymoFileName(row.name);
			pt.x = rowSeries + col; 
			if (row.values_out == null)
				continue;
			//System.out.println("writeSimpleRows row.binsize =" +row.binsize);
			for (int coltime=expAll.getKymoFrameStart(); coltime < expAll.getKymoFrameEnd(); coltime+=options.buildExcelBinStep, pt.y++) {
				int i_from = coltime / row.rowbinsize;
				if (i_from >= row.values_out.length)
					break;
				double value = row.values_out[i_from];
				if (!Double.isNaN(value)) {
					XLSUtils.setValue(sheet, pt, transpose, value);
					if (i_from < row.padded_out.length && row.padded_out[i_from])
						XLSUtils.getCell(sheet, pt, transpose).setCellStyle(xssfCellStyle_red);
				}
			}
			pt.x++;
		}
	}
	
	private void writeLRRows(XSSFSheet sheet, int columndataarea, int rowseries, Point pt) {
		boolean transpose = options.transpose;
		for (int irow = 0; irow < rowsForOneExp.size(); irow ++) {
			XLSResults rowL = rowsForOneExp.get(irow);
			pt.y = columndataarea;
			int colL = getColFromKymoFileName(rowL.name);
			pt.x = rowseries + colL; 
			int cageL = getCageFromKymoFileName(rowL.name);
			XLSResults rowR = null;
			if (irow+1 < rowsForOneExp.size()) {
				rowR = rowsForOneExp.get(irow+1);
				int cageR = getCageFromKymoFileName(rowR.name);
				if (cageR == cageL)
					irow++;
				else
					rowR = null;
			}
			// output values from the row
			int lenL = rowL.values_out.length;
			if (rowR != null && rowR.values_out != null && lenL != rowR.values_out.length)
				System.out.println("length of data - rowL="+lenL+" rowR="+rowR.values_out.length);
			int row0 = pt.x;
			
			for (int coltime=expAll.getKymoFrameStart(); coltime < expAll.getKymoFrameEnd(); coltime+=options.buildExcelBinStep, pt.y++) {
				pt.x = row0;
				int i_from = coltime / rowL.rowbinsize;
				if (i_from >= rowL.values_out.length)
					break;
				double dataL = rowL.values_out[i_from];
				double dataR = Double.NaN;
				if (rowR != null && rowR.values_out != null) 
					dataR = rowR.values_out[i_from];
				
				boolean ratioOK = true;
				if (Double.isNaN(dataR)) {
					dataR=0;
					ratioOK = false;
				}
				if (Double.isNaN(dataL)) { 
					dataL=0;
					ratioOK = false;
				}
					
				double sum = Math.abs(dataL)+Math.abs(dataR);
				if (!Double.isNaN(sum)) {
					XLSUtils.setValue(sheet, pt, transpose, sum);
					if (i_from < rowL.padded_out.length && rowL.padded_out[i_from])
						XLSUtils.getCell(sheet, pt, transpose).setCellStyle(xssfCellStyle_red);
				}
				pt.x ++;
				if (ratioOK && sum != 0 && !Double.isNaN(sum)) {
					double ratio = (dataL-dataR)/sum;
					if (ratio > 1. || ratio < -1.)
						System.out.println("ratio out of limits: "+ ratio);
					if (!Double.isNaN(ratio)) {
						XLSUtils.setValue(sheet, pt, transpose, ratio);
						if (i_from < rowL.padded_out.length && rowL.padded_out[i_from])
							XLSUtils.getCell(sheet, pt, transpose).setCellStyle(xssfCellStyle_red);
					}
				}
			}
		}
	}
	
	private void writeTOGulpLR(XSSFSheet sheet, int columndataarea, int rowseries, Point pt) {
		boolean transpose = options.transpose;
		for (int irow = 0; irow < rowsForOneExp.size(); irow ++) {
			XLSResults rowL = rowsForOneExp.get(irow);
			pt.y = columndataarea;
			int colL = getColFromKymoFileName(rowL.name);
			pt.x = rowseries + colL; 
			int cageL = getCageFromKymoFileName(rowL.name);
			XLSResults rowR = null;
			if (irow+1 < rowsForOneExp.size()) {
				rowR = rowsForOneExp.get(irow+1);
				int cageR = getCageFromKymoFileName(rowR.name);
				if (cageR == cageL)
					irow++;
				else
					rowR = null;
			}
			// TODO here: merge the 2 results series (integer or double?): call a routine from gulps?
			
			// output values from the row
			int lenL = rowL.values_out.length;
			if (rowR != null && rowR.values_out != null && lenL != rowR.values_out.length)
				System.out.println("length of data - rowL="+lenL+" rowR="+rowR.values_out.length);
			int row0 = pt.x;
			
			for (int coltime=expAll.getKymoFrameStart(); coltime < expAll.getKymoFrameEnd(); coltime+=options.buildExcelBinStep, pt.y++) {
				pt.x = row0;
				int i_from = coltime / rowL.rowbinsize;
				if (i_from >= rowL.values_out.length)
					break;
				double dataL = rowL.values_out[i_from];
				double dataR = Double.NaN;
				if (rowR != null && rowR.values_out != null) 
					dataR = rowR.values_out[i_from];
				
				boolean ratioOK = true;
				if (Double.isNaN(dataR)) {
					dataR=0;
					ratioOK = false;
				}
				if (Double.isNaN(dataL)) { 
					dataL=0;
					ratioOK = false;
				}
					
				double sum = Math.abs(dataL)+Math.abs(dataR);
				if (!Double.isNaN(sum)) {
					XLSUtils.setValue(sheet, pt, transpose, sum);
					if (i_from < rowL.padded_out.length && rowL.padded_out[i_from])
						XLSUtils.getCell(sheet, pt, transpose).setCellStyle(xssfCellStyle_red);
				}
				pt.x ++;
				if (ratioOK && sum != 0 && !Double.isNaN(sum)) {
					double ratio = (dataL-dataR)/sum;
					if (ratio > 1. || ratio < -1.)
						System.out.println("ratio out of limits: "+ ratio);
					if (!Double.isNaN(ratio)) {
						XLSUtils.setValue(sheet, pt, transpose, ratio);
						if (i_from < rowL.padded_out.length && rowL.padded_out[i_from])
							XLSUtils.getCell(sheet, pt, transpose).setCellStyle(xssfCellStyle_red);
					}
				}
			}
		}
	}
	
}
