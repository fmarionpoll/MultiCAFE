package plugins.fmp.multicafe.tools0.ROI2D;

import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import icy.gui.frame.progress.AnnounceFrame;
import icy.roi.ROI;
import icy.roi.ROI2D;
import icy.sequence.Sequence;
import icy.type.geom.Polygon2D;
import icy.type.geom.Polyline2D;
import icy.util.XMLUtil;
import plugins.fmp.multicafe.fmp_experiment.sequence.SequenceKymos;
import plugins.fmp.multicafe.tools1.Comparators;
import plugins.kernel.roi.roi2d.ROI2DLine;
import plugins.kernel.roi.roi2d.ROI2DPolyLine;
import plugins.kernel.roi.roi2d.ROI2DShape;

public class ROI2DUtilities {
	private static final String ID_ROIMC = "roiMC";

	public static void interpolateMissingPointsAlongXAxis(ROI2DPolyLine roiLine, int nintervals) {
		if (nintervals <= 1)
			return;
		// interpolate points so that each x step has a value
		// assume that points are ordered along x
		Polyline2D polyline = roiLine.getPolyline2D();
		int roiLine_npoints = polyline.npoints;
		if (roiLine_npoints == 0)
			return;

		if (roiLine_npoints > nintervals)
			roiLine_npoints = nintervals;

		List<Point2D> pts = new ArrayList<Point2D>(roiLine_npoints);
		double ylast = polyline.ypoints[roiLine_npoints - 1];
		int xfirst0 = (int) polyline.xpoints[0];

		for (int i = 1; i < roiLine_npoints; i++) {
			int xfirst = (int) polyline.xpoints[i - 1];
			if (xfirst < 0)
				xfirst = 0;
			int xlast = (int) polyline.xpoints[i];
			if (xlast > xfirst0 + nintervals - 1)
				xlast = xfirst0 + nintervals - 1;
			double yfirst = polyline.ypoints[i - 1];
			ylast = polyline.ypoints[i];
			for (int j = xfirst; j < xlast; j++) {
				int val = (int) (yfirst + (ylast - yfirst) * (j - xfirst) / (xlast - xfirst));
				Point2D pt = new Point2D.Double(j, val);
				pts.add(pt);
			}
		}
		Point2D pt = new Point2D.Double(polyline.xpoints[roiLine_npoints - 1], ylast);
		pts.add(pt);
		roiLine.setPoints(pts);
	}

	private static List<Integer> transferROIYpointsToIntList(ROI2DPolyLine roiLine) {
		Polyline2D line = roiLine.getPolyline2D();
		List<Integer> intArray = new ArrayList<Integer>(line.npoints);
		for (int i = 0; i < line.npoints; i++)
			intArray.add((int) line.ypoints[i]);
		return intArray;
	}

	public static void mergeROIsListNoDuplicate(List<ROI2D> seqList, List<ROI2D> listRois, Sequence seq) {
		if (seqList.isEmpty())
			seqList.addAll(listRois);
		for (ROI2D seqRoi : seqList) {
			Iterator<ROI2D> iterator = listRois.iterator();
			while (iterator.hasNext()) {
				ROI2D roi = iterator.next();
				if (seqRoi == roi)
					iterator.remove();
				else if (seqRoi.getName().equals(roi.getName())) {
					seqRoi.copyFrom(roi);
					iterator.remove();
				}
			}
		}
	}

	public static void removeROIsMissingChar(List<ROI2D> listRois, char character) {
		Iterator<ROI2D> iterator = listRois.iterator();
		while (iterator.hasNext()) {
			ROI2D roi = iterator.next();
			if (roi.getName().indexOf(character) < 0)
				iterator.remove();
		}
	}

	public static ROI2DPolyLine transfertDataArrayToROI(List<Integer> intArray) {
		Polyline2D line = new Polyline2D();
		for (int i = 0; i < intArray.size(); i++) {
			Point2D pt = new Point2D.Double(i, intArray.get(i));
			line.addPoint(pt);
		}
		return new ROI2DPolyLine(line);
	}

	public static List<Integer> copyFirstROIMatchingFilterToDataArray(SequenceKymos seqKymos, String filter) {
		List<ROI2D> listRois = seqKymos.getSequence().getROI2Ds();
		int width = seqKymos.getSequence().getWidth();
		for (ROI2D roi : listRois) {
			if (roi.getName().contains(filter)) {
				interpolateMissingPointsAlongXAxis((ROI2DPolyLine) roi, width);
				return transferROIYpointsToIntList((ROI2DPolyLine) roi);
			}
		}
		return null;
	}

	public static List<ROI2D> loadROIsFromXML(Document doc) {
		List<ROI> localList = ROI.loadROIsFromXML(XMLUtil.getRootElement(doc));
		List<ROI2D> finalList = new ArrayList<ROI2D>(localList.size());
		for (ROI roi : localList)
			finalList.add((ROI2D) roi);
		return finalList;
	}

	public static Polygon2D orderVerticesofPolygon(Polygon roiPolygon) {
		if (roiPolygon.npoints > 4)
			new AnnounceFrame("Only the first 4 points of the polygon will be used...");
		Polygon2D extFrame = new Polygon2D();
		Rectangle rect = roiPolygon.getBounds();
		Rectangle rect1 = new Rectangle(rect);
		// find upper left
		rect1.setSize(rect.width / 2, rect.height / 2);
		for (int i = 0; i < roiPolygon.npoints; i++) {
			if (rect1.contains(roiPolygon.xpoints[i], roiPolygon.ypoints[i])) {
				extFrame.addPoint(roiPolygon.xpoints[i], roiPolygon.ypoints[i]);
				break;
			}
		}
		// find lower left
		rect1.translate(0, rect.height / 2 + 2);
		for (int i = 0; i < roiPolygon.npoints; i++) {
			if (rect1.contains(roiPolygon.xpoints[i], roiPolygon.ypoints[i])) {
				extFrame.addPoint(roiPolygon.xpoints[i], roiPolygon.ypoints[i]);
				break;
			}
		}
		// find lower right
		rect1.translate(rect.width / 2 + 2, 0);
		for (int i = 0; i < roiPolygon.npoints; i++) {
			if (rect1.contains(roiPolygon.xpoints[i], roiPolygon.ypoints[i])) {
				extFrame.addPoint(roiPolygon.xpoints[i], roiPolygon.ypoints[i]);
				break;
			}
		}
		// find upper right
		rect1.translate(0, -rect.height / 2 - 2);
		for (int i = 0; i < roiPolygon.npoints; i++) {
			if (rect1.contains(roiPolygon.xpoints[i], roiPolygon.ypoints[i])) {
				extFrame.addPoint(roiPolygon.xpoints[i], roiPolygon.ypoints[i]);
				break;
			}
		}
		return extFrame;
	}

	public static Polygon2D inflate(Polygon2D roiPolygon, int ncolumns, int nrows, int width_cell, int width_interval) {
		double width_x_current = ncolumns * (width_cell + 2 * width_interval) - 2 * width_interval;
		double deltax_top = (roiPolygon.xpoints[3] - roiPolygon.xpoints[0]) * width_interval / width_x_current;
		double deltax_bottom = (roiPolygon.xpoints[2] - roiPolygon.xpoints[1]) * width_interval / width_x_current;

		double width_y_current = nrows * (width_cell + 2 * width_interval) - 2 * width_interval;
		double deltay_left = (roiPolygon.ypoints[1] - roiPolygon.ypoints[0]) * width_interval / width_y_current;
		double deltay_right = (roiPolygon.ypoints[2] - roiPolygon.ypoints[3]) * width_interval / width_y_current;

		double[] xpoints = new double[4];
		double[] ypoints = new double[4];
		int npoints = 4;

		xpoints[0] = roiPolygon.xpoints[0] - deltax_top;
		xpoints[1] = roiPolygon.xpoints[1] - deltax_bottom;
		xpoints[3] = roiPolygon.xpoints[3] + deltax_top;
		xpoints[2] = roiPolygon.xpoints[2] + deltax_bottom;

		ypoints[0] = roiPolygon.ypoints[0] - deltay_left;
		ypoints[3] = roiPolygon.ypoints[3] - deltay_right;
		ypoints[1] = roiPolygon.ypoints[1] + deltay_left;
		ypoints[2] = roiPolygon.ypoints[2] + deltay_right;

		Polygon2D result = new Polygon2D(xpoints, ypoints, npoints);
		return result;
	}

	public static Point2D lineIntersect(double x1, double y1, double x2, double y2, double x3, double y3, double x4,
			double y4) {
		double denom = (y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1);
		if (denom == 0.0) // Lines are parallel.
			return null;

		double ua = ((x4 - x3) * (y1 - y3) - (y4 - y3) * (x1 - x3)) / denom;
		double ub = ((x2 - x1) * (y1 - y3) - (y2 - y1) * (x1 - x3)) / denom;
		if (ua >= 0.0f && ua <= 1.0f && ub >= 0.0f && ub <= 1.0f)
			// Get the intersection point.
			return new Point2D.Double((x1 + ua * (x2 - x1)), (y1 + ua * (y2 - y1)));
		return null;
	}

	public static List<ROI2D> getROIs2DContainingString(String string, Sequence seq) {
		List<ROI2D> roiList = seq.getROI2Ds();
		Collections.sort(roiList, new Comparators.ROI2D_Name());
		List<ROI2D> listROIsMatchingString = new ArrayList<ROI2D>();
		for (ROI2D roi : roiList) {
			if ((roi instanceof ROI2DShape) && roi.getName().contains(string))
				listROIsMatchingString.add(roi);
		}
		return listROIsMatchingString;
	}

	public static void removeRoisContainingString(int t, String string, Sequence seq) {
		for (ROI roi : seq.getROIs()) {
			if (roi instanceof ROI2D && roi.getName().contains(string) && (t < 0 || ((ROI2D) roi).getT() == t))
				seq.removeROI(roi);
		}
	}

	public static Polygon2D getPolygonEnclosingCapillaries(ArrayList<ROI2D> listRois) {
		ROI2D roi1 = listRois.get(0);
		Point2D pt1 = getFirstPoint(roi1);
		Point2D pt2 = getLastPoint(roi1);
		double[] xpoints = { pt1.getX(), pt1.getX(), pt2.getX(), pt2.getX() };
		double[] ypoints = { pt1.getY(), pt1.getY(), pt2.getY(), pt2.getY() };

		for (ROI2D roi : listRois) {
			if (!roi.getName().contains("line"))
				continue;

			pt1 = getFirstPoint(roi);
			updateUpperLeftCorner(pt1, xpoints, ypoints);
			updateUpperRightCorner(pt1, xpoints, ypoints);

			pt2 = getLastPoint(roi);
			updateLowerLeftCorner(pt2, xpoints, ypoints);
			updateLowerRightCorner(pt2, xpoints, ypoints);
		}
		return new Polygon2D(xpoints, ypoints, 4);
	}

	private static void updateUpperLeftCorner(Point2D pt, double[] xpoints, double[] ypoints) {
		final int i = 0;
		if (pt.getX() < xpoints[i]) {
			xpoints[i] = pt.getX();
			if (pt.getY() < ypoints[i])
				ypoints[i] = pt.getY();
		}
	}

	private static void updateUpperRightCorner(Point2D pt, double[] xpoints, double[] ypoints) {
		final int i = 1;
		if (pt.getX() > xpoints[i]) {
			xpoints[i] = pt.getX();
			if (pt.getY() < ypoints[i])
				ypoints[i] = pt.getY();
		}
	}

	private static void updateLowerLeftCorner(Point2D pt, double[] xpoints, double[] ypoints) {
		final int i = 3;
		if (pt.getX() < xpoints[i]) {
			xpoints[i] = pt.getX();
			if (pt.getY() > ypoints[i])
				ypoints[i] = pt.getY();
		}
	}

	private static void updateLowerRightCorner(Point2D pt, double[] xpoints, double[] ypoints) {
		final int i = 2;
		if (pt.getX() > xpoints[i]) {
			xpoints[i] = pt.getX();
			if (pt.getY() > ypoints[i])
				ypoints[i] = pt.getY();
		}
	}

	private static Point2D getFirstPoint(ROI2D roi) {
		Rectangle rect = roi.getBounds();
		return new Point2D.Double(rect.getX(), rect.getY());
	}

	private static Point2D getLastPoint(ROI2D roi) {
		Rectangle rect = roi.getBounds();
		return new Point2D.Double(rect.getX() + rect.getWidth(), rect.getY() + rect.getHeight());
	}

	public static ArrayList<Point2D> getCapillaryPoints(ROI2D roi) {
		ArrayList<Point2D> points = new ArrayList<Point2D>();
		if (roi instanceof ROI2DPolyLine) {
			Polyline2D line = ((ROI2DPolyLine) roi).getPolyline2D();
			for (int i = 0; i < line.npoints; i++) {
				Point2D pt = new Point2D.Double(line.xpoints[i], line.ypoints[i]);
				points.add(pt);
			}
		} else if (roi instanceof ROI2DLine) {
			Line2D line = ((ROI2DLine) roi).getLine();
			Point2D pt = new Point2D.Double(line.getP1().getX(), line.getP1().getY());
			points.add(pt);
			pt = new Point2D.Double(line.getP2().getX(), line.getP2().getY());
			points.add(pt);
		}
		return points;
	}

	public static void saveToXML_ROI(Node node, ROI2D roi) {
		final Node nodeROI = XMLUtil.setElement(node, ID_ROIMC);
		if (!roi.saveToXML(nodeROI)) {
			XMLUtil.removeNode(node, nodeROI);
			System.err.println("Error: the roi " + roi.getName() + " was not correctly saved to XML !");
		}
	}

	public static ROI2D loadFromXML_ROI(Node node) {
		final Node nodeROI = XMLUtil.getElement(node, ID_ROIMC);
		if (nodeROI != null) {
			ROI2D roi = (ROI2D) ROI2D.createFromXML(nodeROI);
			return roi;
		}
		return null;
	}

}
