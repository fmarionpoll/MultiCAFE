package plugins.fmp.multicafe.experiment.cells;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import icy.roi.BooleanMask2D;
import icy.roi.ROI;
import icy.roi.ROI2D;
import icy.type.geom.Polygon2D;
import icy.util.XMLUtil;
import plugins.fmp.multicafe.experiment.capillaries.Capillaries;
import plugins.fmp.multicafe.experiment.capillaries.Capillary;
import plugins.kernel.roi.roi2d.ROI2DPolygon;
import plugins.kernel.roi.roi2d.ROI2DRectangle;

public class Cell {
	public ROI2D cellRoi2D = null;
	public BooleanMask2D cellMask2D = null;
	public FlyPositions flyPositions = new FlyPositions();
	public int cellNFlies = 0;
	public int cellAge = 5;
	public String cellComment = "..";
	public String cellSex = "..";
	public String cellStrain = "..";
	private String cellNumber = null;
	public boolean valid = false;
	public boolean bDetect = true;
	public boolean initialflyRemoved = false;

	private final String ID_CELLLIMITS = "CageLimits";
	private final String ID_FLYPOSITIONS = "FlyPositions";
	private final String ID_NFLIES = "nflies";
	private final String ID_AGE = "age";
	private final String ID_COMMENT = "comment";
	private final String ID_SEX = "sex";
	private final String ID_STRAIN = "strain";

	public String getRoiName() {
		if (cellRoi2D != null)
			return cellRoi2D.getName();
		return null;
	}

	public boolean xmlSaveCell(Node node, int index) {
		if (node == null)
			return false;
		Element xmlVal = XMLUtil.addElement(node, "Cage" + index);
		xmlSaveCellLimits(xmlVal);
		xmlSaveCellParameters(xmlVal);
		if (cellNFlies > 0)
			xmlSaveFlyPositions(xmlVal);
		return true;
	}

	public boolean xmlSaveCellParameters(Element xmlVal) {
		XMLUtil.setElementIntValue(xmlVal, ID_NFLIES, cellNFlies);
		XMLUtil.setElementIntValue(xmlVal, ID_AGE, cellAge);
		XMLUtil.setElementValue(xmlVal, ID_COMMENT, cellComment);
		XMLUtil.setElementValue(xmlVal, ID_SEX, cellSex);
		XMLUtil.setElementValue(xmlVal, ID_STRAIN, cellStrain);
		return true;
	}

	public boolean xmlSaveCellLimits(Element xmlVal) {
		Element xmlVal2 = XMLUtil.addElement(xmlVal, ID_CELLLIMITS);
		if (cellRoi2D != null) {
			cellRoi2D.setSelected(false);
			cellRoi2D.saveToXML(xmlVal2);
		}
		return true;
	}

	public boolean xmlSaveFlyPositions(Element xmlVal) {
		Element xmlVal2 = XMLUtil.addElement(xmlVal, ID_FLYPOSITIONS);
		flyPositions.xmlSaveXYTPositions(xmlVal2);
		return true;
	}

	public boolean xmlLoadCell(Node node, int index) {
		if (node == null)
			return false;
		Element xmlVal = XMLUtil.getElement(node, "Cage" + index);
		if (xmlVal == null)
			return false;
		xmlLoadCellLimits(xmlVal);
		xmlLoadCellParameters(xmlVal);
		xmlLoadFlyPositions(xmlVal);
		return true;
	}

	public boolean xmlLoadCellLimits(Element xmlVal) {
		Element xmlVal2 = XMLUtil.getElement(xmlVal, ID_CELLLIMITS);
		if (xmlVal2 != null) {
			cellRoi2D = (ROI2D) ROI.createFromXML(xmlVal2);
			cellRoi2D.setSelected(false);
			cellRoi2D.setColor(Color.MAGENTA);
		}
		return true;
	}

	public boolean xmlLoadCellParameters(Element xmlVal) {
		cellNFlies = XMLUtil.getElementIntValue(xmlVal, ID_NFLIES, cellNFlies);
		cellAge = XMLUtil.getElementIntValue(xmlVal, ID_AGE, cellAge);
		cellComment = XMLUtil.getElementValue(xmlVal, ID_COMMENT, cellComment);
		cellSex = XMLUtil.getElementValue(xmlVal, ID_SEX, cellSex);
		cellStrain = XMLUtil.getElementValue(xmlVal, ID_STRAIN, cellStrain);
		return true;
	}

	public boolean xmlLoadFlyPositions(Element xmlVal) {
		Element xmlVal2 = XMLUtil.getElement(xmlVal, ID_FLYPOSITIONS);
		if (xmlVal2 != null) {
			flyPositions.xmlLoadXYTPositions(xmlVal2);
			return true;
		}
		return false;
	}

	// ------------------------------------

	public String csvExport_CELL_Header(String sep) {
		StringBuffer sbf = new StringBuffer();

		sbf.append("#" + sep + "CAGE\n");
		List<String> row2 = Arrays.asList("cellID", "nFlies", "age", "comment", "strain", "sex", "ROI", "npoints",
				"x(i)", "y(i)");
		sbf.append(String.join(sep, row2));
		sbf.append("\n");
		return sbf.toString();
	}

	public String csvExport_CELL_Data(String sep) {
		StringBuffer sbf = new StringBuffer();
		List<String> row = new ArrayList<String>();
		row.add(cellNumber);
		row.add(Integer.toString(cellNFlies));
		row.add(Integer.toString(cellAge));
		row.add(cellComment);
		row.add(cellStrain);
		row.add(cellSex);
		row.add(cellRoi2D.getName());

		if (cellRoi2D != null) {
			Polygon2D polygon = ((ROI2DPolygon) cellRoi2D).getPolygon2D();
			row.add(Integer.toString(polygon.npoints));
			for (int i = 0; i < polygon.npoints; i++) {
				row.add(Double.toString(polygon.xpoints[i]));
				row.add(Double.toString(polygon.ypoints[i]));
			}
		} else
			row.add("0");
		sbf.append(String.join(sep, row));
		sbf.append("\n");
		return sbf.toString();
	}

	public String csvExport_MEASURE_Header(EnumCellMeasures measureType, String sep, boolean complete) {
		StringBuffer sbf = new StringBuffer();
//		String explanation = "cellID" + sep + "npts" + sep + "t(i)" + sep + "x(i)" + sep + "y(i)" + sep;
//		if (complete)
//			explanation = explanation + "w(i)" + sep + "h(i)" + sep;
		String explanation = "cellID" + sep + "parm" + sep + "npts";
		switch (measureType) {
		case POSITION:
			sbf.append("#" + sep + "POSITION\n" + explanation + "\n");
			break;
		default:
			sbf.append("#" + sep + "UNDEFINED------------\n");
			break;
		}
		return sbf.toString();
	}

	public String csvExport_MEASURE_Data(EnumCellMeasures measureType, String sep, boolean complete) {
		StringBuffer sbf = new StringBuffer();
		switch (measureType) {
		case POSITION:
			flyPositions.cvsExport_Parameter_ToRow(sbf, "t(i)", cellNumber, sep);
			flyPositions.cvsExport_Parameter_ToRow(sbf, "x(i)", cellNumber, sep);
			flyPositions.cvsExport_Parameter_ToRow(sbf, "y(i)", cellNumber, sep);
			flyPositions.cvsExport_Parameter_ToRow(sbf, "w(i)", cellNumber, sep);
			flyPositions.cvsExport_Parameter_ToRow(sbf, "h(i)", cellNumber, sep);
			break;
		default:
			break;
		}
		return sbf.toString();
	}

	public void csvImport_CAGE_Header(String[] data) {
		int i = 0;
		cellNumber = data[i];
		i++;
		cellNFlies = Integer.valueOf(data[i]);
		i++;
		cellAge = Integer.valueOf(data[i]);
		i++;
		cellComment = data[i];
		i++;
		cellStrain = data[i];
		i++;
		cellSex = data[i];
		i++;
		String cellROI_name = data[i];
		i++;

		int npoints = Integer.valueOf(data[i]);
		i++;
		if (npoints > 0) {
			double[] x = new double[npoints];
			double[] y = new double[npoints];
			for (int j = 0; j < npoints; j++) {
				x[j] = Double.valueOf(data[i]);
				i++;
				y[j] = Double.valueOf(data[i]);
				i++;
			}
			Polygon2D polygon = new Polygon2D(x, y, npoints);
			cellRoi2D = new ROI2DPolygon(polygon);
			cellRoi2D.setName(cellROI_name);
			cellRoi2D.setColor(Color.MAGENTA);
		}

	}

	public void csvImport_MEASURE_Data_v0(EnumCellMeasures measureType, String[] data, boolean complete) {
		switch (measureType) {
		case POSITION:
			if (complete)
				flyPositions.csvImport_Rectangle_FromRow(data, 1);
			else
				flyPositions.csvImport_XY_FromRow(data, 1);
			break;
		default:
			break;
		}
	}

	public void csvImport_MEASURE_Data_Parameters(String[] data) {
		flyPositions.cvsImport_Parameter_FromRow(data);
	}

	// ------------------------------------

	public String getCellNumber() {
		if (cellNumber == null)
			cellNumber = cellRoi2D.getName().substring(cellRoi2D.getName().length() - 3);
		return cellNumber;
	}

	public int getCellNumberInteger() {
		int cellnb = -1;
		cellNumber = getCellNumber();
		if (cellNumber != null) {
			try {
				return Integer.parseInt(cellNumber);
			} catch (NumberFormatException e) {
				return cellnb;
			}
		}
		return cellnb;
	}

	public void clearMeasures() {
		flyPositions.clear();
	}

	public Point2D getCenterTopCell() {
		Rectangle2D rect = cellRoi2D.getBounds2D();
		Point2D pt = new Point2D.Double(rect.getX() + rect.getWidth() / 2, rect.getY());
		return pt;
	}

	public Point2D getCenterTipCapillaries(Capillaries capList) {
		List<Point2D> listpts = new ArrayList<Point2D>();
		for (Capillary cap : capList.capillariesList) {
			Point2D pt = cap.getCapillaryTipWithinROI2D(cellRoi2D);
			if (pt != null)
				listpts.add(pt);
		}
		double x = 0;
		double y = 0;
		int n = listpts.size();
		for (Point2D pt : listpts) {
			x += pt.getX();
			y += pt.getY();
		}
		Point2D pt = new Point2D.Double(x / n, y / n);
		return pt;
	}

	public void copyCell(Cell cellFrom) {
		cellRoi2D = cellFrom.cellRoi2D;
		cellNFlies = cellFrom.cellNFlies;
		cellComment = cellFrom.cellComment;
		cellNumber = cellFrom.cellNumber;
		cellStrain = cellFrom.cellStrain;
		cellSex = cellFrom.cellSex;
		valid = false;
		flyPositions.copyPositions(cellFrom.flyPositions);
	}

	public ROI2DRectangle getRoiRectangleFromPositionAtT(int t) {
		int nitems = flyPositions.flyPositionList.size();
		if (nitems == 0 || t >= nitems)
			return null;
		FlyPosition aValue = flyPositions.flyPositionList.get(t);

		ROI2DRectangle flyRoiR = new ROI2DRectangle(aValue.getRectangle2D());
		flyRoiR.setName("detR" + getCellNumber() + "_" + t);
		flyRoiR.setT(t);
		flyRoiR.setColor(Color.YELLOW);
		return flyRoiR;
	}

	public void transferRoisToPositions(List<ROI2D> detectedROIsList) {
		String filter = "detR" + getCellNumber();
		for (ROI2D roi : detectedROIsList) {
			String name = roi.getName();
			if (!name.contains(filter))
				continue;
			Rectangle2D rect = ((ROI2DRectangle) roi).getRectangle();
			int t = roi.getT();
			flyPositions.flyPositionList.get(t).setRectangle2D(rect);
		}
	}

	public void computeCageBooleanMask2D() throws InterruptedException {
		cellMask2D = cellRoi2D.getBooleanMask2D(0, 0, 1, true);
	}

	public void initTmsForFlyPositions(long[] intervalsMs) {
		for (FlyPosition flyPosition : flyPositions.flyPositionList) {
			flyPosition.tMs = intervalsMs[flyPosition.flyIndexT];
		}
	}

	public void addFlyPositionsFromOtherCell(Cell cellExpi) {
		flyPositions.flyPositionList.addAll(cellExpi.flyPositions.flyPositionList);
	}
}
