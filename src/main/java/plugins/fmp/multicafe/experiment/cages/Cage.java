package plugins.fmp.multicafe.experiment.cages;

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
import icy.util.XMLUtil;
import plugins.fmp.multicafe.experiment.capillaries.Capillaries;
import plugins.fmp.multicafe.experiment.capillaries.Capillary;
import plugins.kernel.roi.roi2d.ROI2DPolygon;
import plugins.kernel.roi.roi2d.ROI2DRectangle;
import icy.type.geom.Polygon2D;

public class Cage {
	public ROI2D cageRoi2D = null;
	public BooleanMask2D cageMask2D = null;
	public FlyPositions flyPositions = new FlyPositions();
	public int cageNFlies = 0;
	public int cageAge = 5;
	public String strCageComment = "..";
	public String strCageSex = "..";
	public String strCageStrain = "..";
	private String strCageNumber = null;
	public boolean valid = false;
	public boolean bDetect = true;
	public boolean initialflyRemoved = false;

	private final String ID_CAGELIMITS = "CageLimits";
	private final String ID_FLYPOSITIONS = "FlyPositions";
	private final String ID_NFLIES = "nflies";
	private final String ID_AGE = "age";
	private final String ID_COMMENT = "comment";
	private final String ID_SEX = "sex";
	private final String ID_STRAIN = "strain";

	public boolean xmlSaveCage(Node node, int index) {
		if (node == null)
			return false;
		Element xmlVal = XMLUtil.addElement(node, "Cage" + index);
		xmlSaveCageLimits(xmlVal);
		xmlSaveCageParameters(xmlVal);
		if (cageNFlies > 0)
			xmlSaveFlyPositions(xmlVal);
		return true;
	}

	public boolean xmlSaveCageParameters(Element xmlVal) {
		XMLUtil.setElementIntValue(xmlVal, ID_NFLIES, cageNFlies);
		XMLUtil.setElementIntValue(xmlVal, ID_AGE, cageAge);
		XMLUtil.setElementValue(xmlVal, ID_COMMENT, strCageComment);
		XMLUtil.setElementValue(xmlVal, ID_SEX, strCageSex);
		XMLUtil.setElementValue(xmlVal, ID_STRAIN, strCageStrain);
		return true;
	}

	public boolean xmlSaveCageLimits(Element xmlVal) {
		Element xmlVal2 = XMLUtil.addElement(xmlVal, ID_CAGELIMITS);
		if (cageRoi2D != null) {
			cageRoi2D.setSelected(false);
			cageRoi2D.saveToXML(xmlVal2);
		}
		return true;
	}

	public boolean xmlSaveFlyPositions(Element xmlVal) {
		Element xmlVal2 = XMLUtil.addElement(xmlVal, ID_FLYPOSITIONS);
		flyPositions.saveXYTseriesToXML(xmlVal2);
		return true;
	}

	public boolean xmlLoadCage(Node node, int index) {
		if (node == null)
			return false;
		Element xmlVal = XMLUtil.getElement(node, "Cage" + index);
		if (xmlVal == null)
			return false;
		xmlLoadCageLimits(xmlVal);
		xmlLoadCageParameters(xmlVal);
		xmlLoadFlyPositions(xmlVal);
		return true;
	}

	public boolean xmlLoadCageLimits(Element xmlVal) {
		Element xmlVal2 = XMLUtil.getElement(xmlVal, ID_CAGELIMITS);
		if (xmlVal2 != null) {
			cageRoi2D = (ROI2D) ROI.createFromXML(xmlVal2);
			cageRoi2D.setSelected(false);
		}
		return true;
	}

	public boolean xmlLoadCageParameters(Element xmlVal) {
		cageNFlies = XMLUtil.getElementIntValue(xmlVal, ID_NFLIES, cageNFlies);
		cageAge = XMLUtil.getElementIntValue(xmlVal, ID_AGE, cageAge);
		strCageComment = XMLUtil.getElementValue(xmlVal, ID_COMMENT, strCageComment);
		strCageSex = XMLUtil.getElementValue(xmlVal, ID_SEX, strCageSex);
		strCageStrain = XMLUtil.getElementValue(xmlVal, ID_STRAIN, strCageStrain);
		return true;
	}

	public boolean xmlLoadFlyPositions(Element xmlVal) {
		Element xmlVal2 = XMLUtil.getElement(xmlVal, ID_FLYPOSITIONS);
		if (xmlVal2 != null) {
			flyPositions.loadXYTseriesFromXML(xmlVal2);
			return true;
		}
		return false;
	}

	// ------------------------------------

	public String csvExport_CAGES_Header(String sep) {
		StringBuffer sbf = new StringBuffer();

		sbf.append("#" + sep + "CAGES\n");
		List<String> row2 = Arrays.asList("cageID", "nFlies", "age", "comment", "strain", "sex", "ROI", "npoints",
				"x(i)", "y(i)");
		sbf.append(String.join(sep, row2));
		sbf.append("\n");
		return sbf.toString();
	}

	public String csvExport_CAGES_Data(String sep) {
		StringBuffer sbf = new StringBuffer();
		List<String> row = new ArrayList<String>();
		row.add(strCageNumber);
		row.add(Integer.toString(cageNFlies));
		row.add(Integer.toString(cageAge));
		row.add(strCageComment);
		row.add(strCageStrain);
		row.add(strCageSex);
		row.add(cageRoi2D.getName());

		if (cageRoi2D != null) {
			Polygon2D polygon = ((ROI2DPolygon) cageRoi2D).getPolygon2D();
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

	public String csvExport_MEASURE_Header(EnumCageMeasures measureType, String sep, boolean complete) {
		StringBuffer sbf = new StringBuffer();
		String explanation = "cageID" + sep + "npts" + sep + "t(i)" + sep + "x(i)" + sep + "y(i)" + sep;
		if (complete)
			explanation = explanation + "w(i)" + sep + "h(i)" + sep;
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

	public String csvExport_MEASURE_Data(EnumCageMeasures measureType, String sep, boolean complete) {
		StringBuffer sbf = new StringBuffer();
		sbf.append(strCageNumber + sep);

		switch (measureType) {
		case POSITION:
			if (complete)
				flyPositions.cvsExport_XYwh_ToRow(sbf, sep);
			else
				flyPositions.cvsExport_XY_ToRow(sbf, sep);
			break;

		default:
			break;
		}
		sbf.append("\n");
		return sbf.toString();
	}

	public void csvImport_CAGE_Header(String[] data) {
		int i = 0;
		strCageNumber = data[i];
		i++;
		cageNFlies = Integer.valueOf(data[i]);
		i++;
		cageAge = Integer.valueOf(data[i]);
		i++;
		strCageComment = data[i];
		i++;
		strCageStrain = data[i];
		i++;
		strCageSex = data[i];
		i++;
		String cageROI_name = data[i];
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
			cageRoi2D = new ROI2DPolygon(polygon);
			cageRoi2D.setName(cageROI_name);
		}

	}

	public void csvImport_MEASURE_Data(EnumCageMeasures measureType, String[] data, boolean complete) {
		switch (measureType) {
		case POSITION:
			if (complete)
				flyPositions.csvImportXYWHDataFromRow(data, 1);
			else
				flyPositions.csvImportXYDataFromRow(data, 1);
			break;
		default:
			break;
		}
	}

	// ------------------------------------

	public String getCageNumber() {
		if (strCageNumber == null)
			strCageNumber = cageRoi2D.getName().substring(cageRoi2D.getName().length() - 3);
		return strCageNumber;
	}

	public int getCageNumberInteger() {
		int cagenb = -1;
		strCageNumber = getCageNumber();
		if (strCageNumber != null) {
			try {
				return Integer.parseInt(strCageNumber);
			} catch (NumberFormatException e) {
				return cagenb;
			}
		}
		return cagenb;
	}

	public void clearMeasures() {
		flyPositions.clear();
	}

	public Point2D getCenterTopCage() {
		Rectangle2D rect = cageRoi2D.getBounds2D();
		Point2D pt = new Point2D.Double(rect.getX() + rect.getWidth() / 2, rect.getY());
		return pt;
	}

	public Point2D getCenterTipCapillaries(Capillaries capList) {
		List<Point2D> listpts = new ArrayList<Point2D>();
		for (Capillary cap : capList.capillariesList) {
			Point2D pt = cap.getCapillaryTipWithinROI2D(cageRoi2D);
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

	public void copyCage(Cage cage) {
		cageRoi2D = cage.cageRoi2D;
		cageNFlies = cage.cageNFlies;
		strCageComment = cage.strCageComment;
		strCageNumber = cage.strCageNumber;
		valid = false;
		flyPositions.copyXYTaSeries(cage.flyPositions);
	}

	public ROI2DRectangle getRoiRectangleFromPositionAtT(int t) {
		int nitems = flyPositions.flyPositionList.size();
		if (nitems == 0 || t >= nitems)
			return null;
		FlyPosition aValue = flyPositions.flyPositionList.get(t);

		ROI2DRectangle flyRoiR = new ROI2DRectangle(aValue.rectPosition);
		flyRoiR.setName("detR" + getCageNumber() + "_" + t);
		flyRoiR.setT(t);
		return flyRoiR;
	}

	public void transferRoisToPositions(List<ROI2D> detectedROIsList) {
		String filter = "detR" + getCageNumber();
		for (ROI2D roi : detectedROIsList) {
			String name = roi.getName();
			if (!name.contains(filter))
				continue;
			Rectangle2D rect = ((ROI2DRectangle) roi).getRectangle();
			int t = roi.getT();
			flyPositions.flyPositionList.get(t).rectPosition = rect;
		}
	}

	public void computeCageBooleanMask2D() throws InterruptedException {
		cageMask2D = cageRoi2D.getBooleanMask2D(0, 0, 1, true);
	}

}
