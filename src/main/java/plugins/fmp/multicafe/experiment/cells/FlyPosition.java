package plugins.fmp.multicafe.experiment.cells;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import icy.util.StringUtil;
import icy.util.XMLUtil;
import plugins.kernel.roi.roi2d.ROI2DArea;

public class FlyPosition {
	// public Rectangle2D rectPosition = new Rectangle2D.Double(Double.NaN,
	// Double.NaN, Double.NaN, Double.NaN);
	public double x = Double.NaN;
	public double y = Double.NaN;
	public double w = Double.NaN;
	public double h = Double.NaN;
	public ROI2DArea flyRoi = null;
	public int flyIndexT = 0;
	public boolean bAlive = false;
	public boolean bSleep = false;
	public boolean bPadded = false;
	public double distance = 0.;
	public double sumDistance = 0;
	public double axis1 = 0.;
	public double axis2 = 0.;

	public FlyPosition() {
	}

	public FlyPosition(int indexT) {
		this.flyIndexT = indexT;
	}

	public FlyPosition(int indexT, Rectangle2D rectangle) {
		if (rectangle != null)
			setRectangle2D(rectangle);
		flyIndexT = indexT;
	}

	public FlyPosition(int indexT, Rectangle2D rectangle, ROI2DArea roiArea) {
		if (rectangle != null)
			setRectangle2D(rectangle);
		flyRoi = new ROI2DArea(roiArea);
		flyRoi.setColor(Color.YELLOW);
		flyIndexT = indexT;
	}

	public FlyPosition(int indexT, Rectangle2D rectangle, boolean alive) {
		if (rectangle != null)
			setRectangle2D(rectangle);
		this.flyIndexT = indexT;
		this.bAlive = alive;
	}

	public void copy(FlyPosition source) {
		flyIndexT = source.flyIndexT;
		bAlive = source.bAlive;
		bSleep = source.bSleep;
		bPadded = source.bPadded;
		distance = source.distance;
		x = source.x;
		y = source.y;
		w = source.w;
		h = source.h;

		if (source.flyRoi != null && source.flyRoi.getBounds().height > 0 && source.flyRoi.getBounds().width > 0) {
			flyRoi = new ROI2DArea(source.flyRoi);
			flyRoi.setColor(Color.YELLOW);
		}
		axis1 = source.axis1;
		axis2 = source.axis2;
	}

	Point2D getCenterRectangle() {
		return new Point2D.Double(x + w / 2, y + h / 2);
	}

	public Rectangle2D getRectangle2D() {
		return new Rectangle2D.Double(x, y, w, h);
	}

	public void setRectangle2D(Rectangle2D rectangle) {
		x = rectangle.getX();
		y = rectangle.getY();
		w = rectangle.getWidth();
		h = rectangle.getHeight();
	}

	// --------------------------------------------

	public boolean loadXYTvaluesFromXML(Node node) {
		if (node == null)
			return false;

		Element node_XYTa = XMLUtil.getElement(node, "XYTa");
		if (node_XYTa != null) {
			double xR = XMLUtil.getAttributeDoubleValue(node_XYTa, "xR", Double.NaN);
			double yR = XMLUtil.getAttributeDoubleValue(node_XYTa, "yR", Double.NaN);
			double wR = XMLUtil.getAttributeDoubleValue(node_XYTa, "wR", Double.NaN);
			double hR = XMLUtil.getAttributeDoubleValue(node_XYTa, "hR", Double.NaN);
			if (!Double.isNaN(xR) && !Double.isNaN(yR)) {
				x = xR;
				y = yR;
				w = wR;
				h = hR;
			} else {
				xR = XMLUtil.getAttributeDoubleValue(node_XYTa, "x", Double.NaN);
				yR = XMLUtil.getAttributeDoubleValue(node_XYTa, "y", Double.NaN);
				if (!Double.isNaN(xR) && !Double.isNaN(yR)) {
					x -= 2.;
					y -= 2.;
					w = 4.;
					h = 4.;
				}
			}

			flyIndexT = XMLUtil.getAttributeIntValue(node_XYTa, "t", 0);
			bAlive = XMLUtil.getAttributeBooleanValue(node_XYTa, "a", false);
			bSleep = XMLUtil.getAttributeBooleanValue(node_XYTa, "s", false);
		}

		Element node_roi = XMLUtil.getElement(node, "roi");
		if (node_roi != null) {
			if (flyRoi == null)
				flyRoi = new ROI2DArea();
			flyRoi.loadFromXML(node_roi);
			flyRoi.setColor(Color.YELLOW);
		}
		return false;
	}

	public boolean saveXYTvaluesToXML(Node node) {
		if (node == null)
			return false;

		Element node_XYTa = XMLUtil.addElement(node, "XYTa");

		if (!Double.isNaN(x)) {
			XMLUtil.setAttributeDoubleValue(node_XYTa, "xR", x);
			XMLUtil.setAttributeDoubleValue(node_XYTa, "yR", y);
			XMLUtil.setAttributeDoubleValue(node_XYTa, "wR", w);
			XMLUtil.setAttributeDoubleValue(node_XYTa, "hR", h);
		}

		XMLUtil.setAttributeDoubleValue(node_XYTa, "t", flyIndexT);
		XMLUtil.setAttributeBooleanValue(node_XYTa, "a", bAlive);
		XMLUtil.setAttributeBooleanValue(node_XYTa, "s", bSleep);

		Element node_roi = XMLUtil.addElement(node, "roi");
		if (flyRoi != null)
			flyRoi.saveToXML(node_roi);
		return false;
	}

	// --------------------------------------------

	public boolean cvsExportT(StringBuffer sbf, String sep) {
		sbf.append(StringUtil.toString(flyIndexT));
		sbf.append(sep);
		return true;
	}

	public boolean cvsExportX(StringBuffer sbf, String sep) {
		sbf.append(StringUtil.toString(x));
		sbf.append(sep);
		return true;
	}

	public boolean cvsExportY(StringBuffer sbf, String sep) {
		sbf.append(StringUtil.toString(y));
		sbf.append(sep);
		return true;
	}

	public boolean cvsExportWidth(StringBuffer sbf, String sep) {
		sbf.append(StringUtil.toString(w));
		sbf.append(sep);
		return true;
	}

	public boolean cvsExportHeight(StringBuffer sbf, String sep) {
		sbf.append(StringUtil.toString(h));
		sbf.append(sep);
		return true;
	}

	public boolean csvImportRectangle(String[] data, int startAt) {
		int npoints = 5;
		if (data.length < npoints + startAt - 1)
			return false;

		int offset = startAt;
		flyIndexT = Integer.valueOf(data[offset]);
		offset++;
		x = Double.valueOf(data[offset]);
		offset++;
		y = Double.valueOf(data[offset]);
		offset++;
		w = Double.valueOf(data[offset]);
		offset++;
		h = Double.valueOf(data[offset]);
		offset++;

		return true;
	}

	public boolean csvImportXY(String[] data, int startAt) {
		int npoints = 3;
		if (data.length < npoints + startAt - 1)
			return false;

		int offset = startAt;
		flyIndexT = Integer.valueOf(data[offset]);
		offset++;
		x = Double.valueOf(data[offset]);
		offset++;
		y = Double.valueOf(data[offset]);
		offset++;

		if (!Double.isNaN(x) && !Double.isNaN(y)) {
			x -= 2.;
			y -= 2.;
			w = 4.;
			h = 4.;
		}

		return true;
	}

	public boolean cvsImportT(String strData) {
		flyIndexT = Integer.valueOf(strData);
		return true;
	}

	public boolean cvsImportX(String strData) {
		x = Double.valueOf(strData);
		return true;
	}

	public boolean cvsImportY(String strData) {
		y = Double.valueOf(strData);
		return true;
	}

	public boolean cvsImportWidth(String strData) {
		w = Double.valueOf(strData);
		return true;
	}

	public boolean cvsImportHeight(String strData) {
		h = Double.valueOf(strData);
		return true;
	}

}
