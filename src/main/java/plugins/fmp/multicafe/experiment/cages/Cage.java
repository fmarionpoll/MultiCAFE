package plugins.fmp.multicafe.experiment.cages;

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

public class Cage {
	private ROI2D cageRoi2D = null;
	private BooleanMask2D cageMask2D = null;
	private FlyPositions flyPositions = new FlyPositions();
	private int cageNFlies = 0;
	private int cageAge = 5;
	private String cageComment = "..";
	private String cageSex = "..";
	private String cageStrain = "..";
	private String cageID = "-1";
	private boolean valid = false;
	private boolean bDetect = true;
	private boolean initialflyRemoved = false;
	private ArrayList<Capillary> capList = new ArrayList<Capillary>(2);

	private final String ID_CAGELIMITS = "CageLimits";
	private final String ID_FLYPOSITIONS = "FlyPositions";
	private final String ID_NFLIES = "nflies";
	private final String ID_AGE = "age";
	private final String ID_COMMENT = "comment";
	private final String ID_SEX = "sex";
	private final String ID_STRAIN = "strain";

	public ROI2D getCageRoi2D() {
		return cageRoi2D;
	}

	public void setCageRoi2D(ROI2D cageRoi2D) {
		this.cageRoi2D = cageRoi2D;
	}

	public BooleanMask2D getCageMask2D() {
		return cageMask2D;
	}

	public void setCageMask2D(BooleanMask2D cageMask2D) {
		this.cageMask2D = cageMask2D;
	}

	public FlyPositions getFlyPositions() {
		return flyPositions;
	}

	public void setFlyPositions(FlyPositions flyPositions) {
		this.flyPositions = flyPositions;
	}

	public int getCageNFlies() {
		return cageNFlies;
	}

	public void setCageNFlies(int cageNFlies) {
		this.cageNFlies = cageNFlies;
	}

	public int getCageAge() {
		return cageAge;
	}

	public void setCageAge(int cageAge) {
		this.cageAge = cageAge;
	}

	public String getCageComment() {
		return cageComment;
	}

	public void setCageComment(String cageComment) {
		this.cageComment = cageComment;
	}

	public String getCageSex() {
		return cageSex;
	}

	public void setCageSex(String cageSex) {
		this.cageSex = cageSex;
	}

	public String getCageStrain() {
		return cageStrain;
	}

	public void setCageStrain(String cageStrain) {
		this.cageStrain = cageStrain;
	}

	public boolean isValid() {
		return valid;
	}

	public void setValid(boolean valid) {
		this.valid = valid;
	}

	public boolean isbDetect() {
		return bDetect;
	}

	public void setbDetect(boolean bDetect) {
		this.bDetect = bDetect;
	}

	public boolean isInitialflyRemoved() {
		return initialflyRemoved;
	}

	public void setInitialflyRemoved(boolean initialflyRemoved) {
		this.initialflyRemoved = initialflyRemoved;
	}

	public String getRoiName() {
		if (cageRoi2D != null)
			return cageRoi2D.getName();
		return null;
	}

	public void addCapillaryIfUnique(Capillary cap) {
		if (capList.size() == 0) {
			capList.add(cap);
			return;
		}

		for (Capillary capCage : capList) {
			if (capCage.compareTo(cap) == 0) {
				return;
			}
		}
		capList.add(cap);
	}

	public void addCapillaryIfUniqueBulkFilteredOnCageID(List<Capillary> capillaryList) {
		for (Capillary cap : capillaryList) {
			if (cap.capCageID == getCageID())
				addCapillaryIfUnique(cap);
		}
	}

	public void clearCapillaryList() {
		capList.clear();
	}

	public ArrayList<Capillary> getCapillaryList() {
		return capList;
	}

	// ============= XML =============

	public boolean xmlSaveCagel(Node node, int index) {
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
		XMLUtil.setElementValue(xmlVal, ID_COMMENT, cageComment);
		XMLUtil.setElementValue(xmlVal, ID_SEX, cageSex);
		XMLUtil.setElementValue(xmlVal, ID_STRAIN, cageStrain);
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
		flyPositions.xmlSaveXYTPositions(xmlVal2);
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
			cageRoi2D.setColor(Color.MAGENTA);
		}
		return true;
	}

	public boolean xmlLoadCageParameters(Element xmlVal) {
		cageNFlies = XMLUtil.getElementIntValue(xmlVal, ID_NFLIES, cageNFlies);
		cageAge = XMLUtil.getElementIntValue(xmlVal, ID_AGE, cageAge);
		cageComment = XMLUtil.getElementValue(xmlVal, ID_COMMENT, cageComment);
		cageSex = XMLUtil.getElementValue(xmlVal, ID_SEX, cageSex);
		cageStrain = XMLUtil.getElementValue(xmlVal, ID_STRAIN, cageStrain);
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

	public String csvExport_CAGE_Header(String sep) {
		StringBuffer sbf = new StringBuffer();

		sbf.append("#" + sep + "CAGE\n");
		List<String> row2 = Arrays.asList("cageID", "nFlies", "age", "comment", "strain", "sex", "ROI", "npoints",
				"x(i)", "y(i)");
		sbf.append(String.join(sep, row2));
		sbf.append("\n");
		return sbf.toString();
	}

	public String csvExport_CAGE_Data(String sep) {
		StringBuffer sbf = new StringBuffer();
		List<String> row = new ArrayList<String>();
		row.add(cageID);
		row.add(Integer.toString(cageNFlies));
		row.add(Integer.toString(cageAge));
		row.add(cageComment);
		row.add(cageStrain);
		row.add(cageSex);
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
//		String explanation = "cageID" + sep + "npts" + sep + "t(i)" + sep + "x(i)" + sep + "y(i)" + sep;
//		if (complete)
//			explanation = explanation + "w(i)" + sep + "h(i)" + sep;
		String explanation = "cageID" + sep + "parm" + sep + "npts";
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
		switch (measureType) {
		case POSITION:
			flyPositions.cvsExport_Parameter_ToRow(sbf, "t(i)", cageID, sep);
			flyPositions.cvsExport_Parameter_ToRow(sbf, "x(i)", cageID, sep);
			flyPositions.cvsExport_Parameter_ToRow(sbf, "y(i)", cageID, sep);
			flyPositions.cvsExport_Parameter_ToRow(sbf, "w(i)", cageID, sep);
			flyPositions.cvsExport_Parameter_ToRow(sbf, "h(i)", cageID, sep);
			break;
		default:
			break;
		}
		return sbf.toString();
	}

	public void csvImport_CAGE_Header(String[] data) {
		int i = 0;
		cageID = data[i];
		i++;
		cageNFlies = Integer.valueOf(data[i]);
		i++;
		cageAge = Integer.valueOf(data[i]);
		i++;
		cageComment = data[i];
		i++;
		cageStrain = data[i];
		i++;
		cageSex = data[i];
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
			cageRoi2D.setColor(Color.MAGENTA);
		}

	}

	public void csvImport_MEASURE_Data_v0(EnumCageMeasures measureType, String[] data, boolean complete) {
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

	public String getCageIDasString() {
		if (cageID == null)
			cageID = cageRoi2D.getName().substring(cageRoi2D.getName().length() - 3);
		return cageID;
	}

	public void setCageID(int iID) {
		cageID = Integer.toString(iID);
	}

	public int getCageID() {
		int cageIndex = -1;
		cageID = getCageIDasString();
		if (cageID != null) {
			try {
				return Integer.parseInt(cageID);
			} catch (NumberFormatException e) {
				return cageIndex;
			}
		}
		return cageIndex;
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
		for (Capillary cap : capList.getCapillariesList()) {
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

	public void copyCage(Cage cageFrom) {
		cageRoi2D = cageFrom.cageRoi2D;
		cageNFlies = cageFrom.cageNFlies;
		cageComment = cageFrom.cageComment;
		cageID = cageFrom.cageID;
		cageStrain = cageFrom.cageStrain;
		cageSex = cageFrom.cageSex;
		valid = false;
		flyPositions.copyPositions(cageFrom.flyPositions);
	}

	public ROI2DRectangle getRoiRectangleFromPositionAtT(int t) {
		int nitems = flyPositions.getFlyPositionList().size();
		if (nitems == 0 || t >= nitems)
			return null;
		FlyPosition aValue = flyPositions.getFlyPositionList().get(t);

		ROI2DRectangle flyRoiR = new ROI2DRectangle(aValue.getRectangle2D());
		flyRoiR.setName("detR" + getCageIDasString() + "_" + t);
		flyRoiR.setT(t);
		flyRoiR.setColor(Color.YELLOW);
		return flyRoiR;
	}

	public void transferRoisToPositions(List<ROI2D> detectedROIsList) {
		String filter = "detR" + getCageIDasString();
		for (ROI2D roi : detectedROIsList) {
			String name = roi.getName();
			if (!name.contains(filter))
				continue;
			Rectangle2D rect = ((ROI2DRectangle) roi).getRectangle();
			int t = roi.getT();
			flyPositions.getFlyPositionList().get(t).setRectangle2D(rect);
		}
	}

	public void computeCageBooleanMask2D() throws InterruptedException {
		cageMask2D = cageRoi2D.getBooleanMask2D(0, 0, 1, true);
	}

	public void initTmsForFlyPositions(long[] intervalsMs) {
		for (FlyPosition flyPosition : flyPositions.getFlyPositionList()) {
			flyPosition.settMs(intervalsMs[flyPosition.getFlyIndexT()]);
		}
	}

	public void addFlyPositionsFromOtherCage(Cage cageExpi) {
		flyPositions.getFlyPositionList().addAll(cageExpi.flyPositions.getFlyPositionList());
	}
}
