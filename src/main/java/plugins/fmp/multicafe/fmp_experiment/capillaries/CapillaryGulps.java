package plugins.fmp.multicafe.fmp_experiment.capillaries;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.w3c.dom.Node;

import icy.roi.ROI;
import icy.roi.ROI2D;
import icy.type.geom.Polyline2D;
import icy.util.StringUtil;
import icy.util.XMLUtil;
import plugins.fmp.multicafe.tools1.toExcel.enums.EnumXLSExport;
import plugins.kernel.roi.roi2d.ROI2DPolyLine;

public class CapillaryGulps {
	private final String ID_GULPS = "gulpsMC";
	public ArrayList<Polyline2D> gulps = new ArrayList<Polyline2D>();

	// -------------------------------

	public void copy(CapillaryGulps capG) {
		gulps = new ArrayList<Polyline2D>(capG.gulps.size());
		gulps.addAll(capG.gulps);
	}

	public boolean loadGulpsFromXML(Node node) {
		boolean flag = false;
		ArrayList<ROI2D> rois = new ArrayList<ROI2D>();
		final Node nodeROIs = XMLUtil.getElement(node, ID_GULPS);
		if (nodeROIs != null) {
			flag = true;
			List<ROI> roislocal = ROI.loadROIsFromXML(nodeROIs);
			for (ROI roislocal_i : roislocal) {
				ROI2D roi = (ROI2D) roislocal_i;
				rois.add(roi);
			}
		}

		buildGulpsFromROIs(rois);
		return flag;
	}

	// -------------------------------

	public void addNewGulpFromPoints(ArrayList<Point2D> gulpPoints) {
		int npoints = gulpPoints.size();
		if (npoints < 1)
			return;

		double[] xpoints = new double[npoints];
		double[] ypoints = new double[npoints];
		for (int i = 0; i < npoints; i++) {
			xpoints[i] = gulpPoints.get(i).getX();
			ypoints[i] = gulpPoints.get(i).getY();
		}
		Polyline2D gulpLine = new Polyline2D(xpoints, ypoints, npoints);
		gulps.add(gulpLine);
	}

	boolean isThereAnyMeasuresDone() {

		return (gulps != null && gulps.size() > 0);
	}

	private void convertPositiveAmplitudesIntoEvent(ArrayList<Integer> data_in) {
		if (data_in == null)
			return;

		int npoints = data_in.size();
		for (int i = 0; i < npoints; i++)
			data_in.set(i, data_in.get(i) != 0 ? 1 : 0);
	}

	private ArrayList<Integer> stretchArrayToOutputBins(ArrayList<Integer> data_in, long seriesBinMs,
			long outputBinMs) {
		if (data_in == null)
			return null;

		long npoints_out = data_in.size() * seriesBinMs / outputBinMs + 1;
		double time_last = data_in.size() * seriesBinMs;
		ArrayList<Integer> data_out = new ArrayList<Integer>((int) npoints_out);
		for (double time_out = 0; time_out <= time_last; time_out += outputBinMs) {
			int index_in = (int) (time_out / seriesBinMs);
			if (index_in >= data_in.size())
				index_in = data_in.size() - 1;
			data_out.add(data_in.get(index_in));
		}
		return data_out;
	}

	public ArrayList<Integer> getMeasuresFromGulps(EnumXLSExport option, int npoints, long seriesBinMs,
			long outputBinMs) {
		ArrayList<Integer> data_in = null;
		switch (option) {
		case SUMGULPS:
		case SUMGULPS_LR:
			data_in = getCumSumFromGulps(npoints);
			data_in = stretchArrayToOutputBins(data_in, seriesBinMs, outputBinMs);
			break;
		case NBGULPS:
			data_in = getIsGulpsFromROIsArray(npoints);
			data_in = stretchArrayToOutputBins(data_in, seriesBinMs, outputBinMs);
			break;
		case AMPLITUDEGULPS:
			data_in = getAmplitudeGulpsFromROIsArray(npoints);
			data_in = stretchArrayToOutputBins(data_in, seriesBinMs, outputBinMs);
			break;
		case TTOGULP:
		case TTOGULP_LR:
			List<Integer> datag = getIsGulpsFromROIsArray(npoints);
			data_in = getTToNextGulp(datag, npoints);
			data_in = stretchArrayToOutputBins(data_in, seriesBinMs, outputBinMs);
			break;

		case AUTOCORREL:
		case AUTOCORREL_LR:
		case CROSSCORREL:
		case CROSSCORREL_LR:
			data_in = getAmplitudeGulpsFromROIsArray(npoints);
			convertPositiveAmplitudesIntoEvent(data_in);
			data_in = stretchArrayToOutputBins(data_in, seriesBinMs, outputBinMs);
			break;
		default:
			break;
		}
		return data_in;
	}

	ArrayList<Integer> getTToNextGulp(List<Integer> datai, int npoints) {
		int nintervals = -1;
		ArrayList<Integer> data_out = null;
		for (int index = datai.size() - 1; index >= 0; index--) {
			if (datai.get(index) == 1) {
				if (nintervals < 0) {
					int nitems = index + 1;
					data_out = new ArrayList<Integer>(Collections.nCopies(nitems, 0));
				}
				nintervals = 0;
				data_out.set(index, nintervals);
			} else if (nintervals >= 0) {
				nintervals++;
				data_out.set(index, nintervals);
			}
		}
		return data_out;
	}

	public void removeGulpsWithinInterval(int startPixel, int endPixel) {
		Iterator<Polyline2D> iterator = gulps.iterator();
		while (iterator.hasNext()) {
			Polyline2D gulp = iterator.next();
			// if roi.first >= startpixel && roi.first <= endpixel
			Rectangle rect = ((Polyline2D) gulp).getBounds();
			if (rect.x >= startPixel && rect.x <= endPixel)
				iterator.remove();
		}
	}

	// -------------------------------

	public boolean csvExportDataToRow(StringBuffer sbf, String sep) {
		int ngulps = 0;
		if (gulps != null)
			ngulps = gulps.size();
		sbf.append(Integer.toString(ngulps) + sep);
		if (ngulps > 0) {
			for (int indexgulp = 0; indexgulp < gulps.size(); indexgulp++)
				csvExportOneGulp(sbf, indexgulp, sep);
		}
		return true;
	}

	private void csvExportOneGulp(StringBuffer sbf, int indexgulp, String sep) {
		sbf.append("g" + indexgulp + sep);
		Polyline2D gulp = gulps.get(indexgulp);
		sbf.append(StringUtil.toString((int) gulp.npoints));
		sbf.append(sep);
		for (int i = 0; i < gulp.npoints; i++) {
			sbf.append(StringUtil.toString((int) gulp.xpoints[i]));
			sbf.append(sep);
			sbf.append(StringUtil.toString((int) gulp.ypoints[i]));
			sbf.append(sep);
		}
	}

	public void csvImportDataFromRow(String[] data, int startAt) {
		if (data.length < startAt)
			return;

		int ngulps = Integer.valueOf(data[startAt]);
		if (ngulps > 0) {
			int offset = startAt + 1;
			for (int i = 0; i < ngulps; i++) {
				offset = csvImportOneGulp(data, offset);
			}
		}
	}

	private int csvImportOneGulp(String[] data, int offset) {
		offset++;
		int npoints = Integer.valueOf(data[offset]);
		offset++;

		int[] x = new int[npoints];
		int[] y = new int[npoints];
		for (int i = 0; i < npoints; i++) {
			x[i] = Integer.valueOf(data[offset]);
			offset++;
			y[i] = Integer.valueOf(data[offset]);
			offset++;
		}
		Polyline2D gulpLine = new Polyline2D(x, y, npoints);
		gulps.add(gulpLine);

		return offset;
	}

	// -------------------------------

	public void buildGulpsFromROIs(ArrayList<ROI2D> rois) {
		gulps = new ArrayList<Polyline2D>(rois.size());
		for (ROI2D roi : rois) {
			Polyline2D gulpLine = ((ROI2DPolyLine) roi).getPolyline2D();
			gulps.add(gulpLine);
		}
	}

	public void transferROIsToMeasures(List<ROI> listRois) {
		ArrayList<ROI2D> rois = new ArrayList<ROI2D>();
		for (ROI roi : listRois) {
			String roiname = roi.getName();
			if (roi instanceof ROI2DPolyLine) {
				if (roiname.contains("gulp"))
					rois.add((ROI2DPolyLine) roi);
			}
		}
		buildGulpsFromROIs(rois);
	}

	ArrayList<Integer> getCumSumFromGulps(int npoints) {
		ArrayList<Integer> sumArrayList = new ArrayList<Integer>(Collections.nCopies(npoints, 0));
		if (gulps == null || gulps.size() == 0)
			return sumArrayList;

		for (Polyline2D gulpLine : gulps) {
			int width = (int) gulpLine.xpoints[gulpLine.npoints - 1] - (int) gulpLine.xpoints[0] + 1;

			List<Point2D> pts = interpolateMissingPointsAlongXAxis(gulpLine, width);
			if (pts == null || pts.size() < 1)
				continue;

			List<Integer> intArray = transferYPointsToIntList(pts);
			int jstart = (int) gulpLine.xpoints[0];
			int previousY = intArray.get(0);
			for (int i = 1; i < intArray.size(); i++) {
				int val = intArray.get(i);
				int deltaY = val - previousY;
				previousY = val;
				for (int j = jstart + i; j < sumArrayList.size(); j++)
					sumArrayList.set(j, sumArrayList.get(j) + deltaY);
			}
		}
		return sumArrayList;
	}

	private List<Integer> transferYPointsToIntList(List<Point2D> pts) {
		List<Integer> intArray = new ArrayList<Integer>(pts.size());
		for (int i = 0; i < pts.size(); i++)
			intArray.add((int) pts.get(i).getY());
		return intArray;
	}

	private List<Point2D> interpolateMissingPointsAlongXAxis(Polyline2D polyline, int nintervals) {
		if (nintervals <= 1)
			return null;
		// interpolate points so that each x step has a value
		// assume that points are ordered along x

		int roiLine_npoints = polyline.npoints;
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
		return pts;
	}

	private ArrayList<Integer> getIsGulpsFromROIsArray(int npoints) {
		if (gulps == null || gulps.size() == 0)
			return null;

		ArrayList<Integer> arrayInt = new ArrayList<Integer>(Collections.nCopies(npoints, 0));
		for (Polyline2D gulpLine : gulps)
			addROItoIsGulpsArray(gulpLine, arrayInt);
		return arrayInt;
	}

	private void addROItoIsGulpsArray(Polyline2D gulpLine, ArrayList<Integer> isGulpsArrayList) {
		double yvalue = gulpLine.ypoints[0];
		int npoints = gulpLine.npoints;
		for (int j = 0; j < npoints; j++) {
			if (gulpLine.ypoints[j] != yvalue) {
				int timeIndex = (int) gulpLine.xpoints[j];
				isGulpsArrayList.set(timeIndex, 1);
			}
			yvalue = gulpLine.ypoints[j];
		}
	}

	private ArrayList<Integer> getAmplitudeGulpsFromROIsArray(int npoints) {
		if (gulps == null || gulps.size() == 0)
			return null;

		ArrayList<Integer> amplitudeGulpsArray = new ArrayList<Integer>(Collections.nCopies(npoints, 0));
		for (Polyline2D gulpLine : gulps)
			addROItoAmplitudeGulpsArray(gulpLine, amplitudeGulpsArray);
		return amplitudeGulpsArray;
	}

	private void addROItoAmplitudeGulpsArray(Polyline2D polyline2D, ArrayList<Integer> amplitudeGulpsArray) {
		double yvalue = polyline2D.ypoints[0];
		int npoints = polyline2D.npoints;
		for (int j = 0; j < npoints; j++) {
			int timeIndex = (int) polyline2D.xpoints[j];
			int delta = (int) (polyline2D.ypoints[j] - yvalue);
			amplitudeGulpsArray.set(timeIndex, delta);
			yvalue = polyline2D.ypoints[j];
		}
	}

}