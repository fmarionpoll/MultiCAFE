package plugins.fmp.multicafe.experiment.cages;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import icy.util.XMLUtil;
import plugins.fmp.multicafe.experiment.Experiment;
import plugins.fmp.multicafe.tools.Comparators;
import plugins.fmp.multicafe.tools.ROI2D.ROI2DMeasures;
import plugins.fmp.multicafe.tools.toExcel.EnumXLSExport;
import plugins.fmp.multicafe.tools.toExcel.XLSExportOptions;

public class FlyPositions {
	public Double moveThreshold = 50.;
	public int sleepThreshold = 5;
	public int lastTimeAlive = 0;
	public int lastIntervalAlive = 0;
	public ArrayList<FlyPosition> flyPositionList = new ArrayList<FlyPosition>();

	public String name = null;
	public EnumXLSExport exportType = null;
	public int binsize = 1;
	public Point2D origin = new Point2D.Double(0, 0);
	public double pixelsize = 1.;
	public int nflies = 1;
	public int csvReadVersion = 1;

	private String ID_NBITEMS = "nb_items";
	private String ID_POSITIONSLIST = "PositionsList";
	private String ID_LASTIMEITMOVED = "lastTimeItMoved";
	private String ID_TLAST = "tlast";
	private String ID_ILAST = "ilast";

	public FlyPositions() {
	}

	public FlyPositions(String name, EnumXLSExport exportType, int nFrames, int binsize) {
		this.name = name;
		this.exportType = exportType;
		this.binsize = binsize;
		flyPositionList = new ArrayList<FlyPosition>(nFrames);
		for (int i = 0; i < nFrames; i++)
			flyPositionList.add(new FlyPosition(i));
	}

	public void clear() {
		flyPositionList.clear();
	}

	public void ensureCapacity(int nFrames) {
		flyPositionList.ensureCapacity(nFrames);
	}

	void initArray(int nFrames) {
		for (int i = 0; i < nFrames; i++) {
			FlyPosition value = new FlyPosition(i);
			flyPositionList.add(value);
		}
	}

	public Rectangle2D getRectangle(int i) {
		return flyPositionList.get(i).getRectangle2D();
	}

	public Rectangle2D getValidPointAtOrBefore(int index) {
		Rectangle2D rect = new Rectangle2D.Double(-1, -1, Double.NaN, Double.NaN);
		for (int i = index; i >= 0; i--) {
			FlyPosition xyVal = flyPositionList.get(i);
			if (xyVal.x >= 0 && xyVal.y >= 0) {
				rect = xyVal.getRectangle2D();
				break;
			}
		}
		return rect;
	}

	public int getTime(int i) {
		return flyPositionList.get(i).flyIndexT;
	}

	public void addPositionWithoutRoiArea(int t, Rectangle2D rectangle) {
		FlyPosition pos = new FlyPosition(t, rectangle);
		flyPositionList.add(pos);
	}

	public void copyPositions(FlyPositions positionsFrom) {
		moveThreshold = positionsFrom.moveThreshold;
		sleepThreshold = positionsFrom.sleepThreshold;
		lastTimeAlive = positionsFrom.lastIntervalAlive;
		flyPositionList = new ArrayList<FlyPosition>(positionsFrom.flyPositionList.size());
		for (FlyPosition flyPosFrom : positionsFrom.flyPositionList) {
			FlyPosition flyPos = new FlyPosition(flyPosFrom);
			flyPositionList.add(flyPos);
		}
		name = positionsFrom.name;
		exportType = positionsFrom.exportType;
		binsize = positionsFrom.binsize;
	}

	// -----------------------------------------------

	public boolean xmlLoadXYTPositions(Node node) {
		if (node == null)
			return false;

		Element node_lastime = XMLUtil.getElement(node, ID_LASTIMEITMOVED);
		lastTimeAlive = XMLUtil.getAttributeIntValue(node_lastime, ID_TLAST, -1);
		lastIntervalAlive = XMLUtil.getAttributeIntValue(node_lastime, ID_ILAST, -1);

		Element node_position_list = XMLUtil.getElement(node, ID_POSITIONSLIST);
		if (node_position_list == null)
			return false;

		flyPositionList.clear();
		int nb_items = XMLUtil.getAttributeIntValue(node_position_list, ID_NBITEMS, 0);
		flyPositionList.ensureCapacity(nb_items);
		for (int i = 0; i < nb_items; i++)
			flyPositionList.add(new FlyPosition(i));
		boolean bAdded = false;

		for (int i = 0; i < nb_items; i++) {
			String elementi = "i" + i;
			Element node_position_i = XMLUtil.getElement(node_position_list, elementi);
			FlyPosition pos = new FlyPosition();
			pos.xmlLoadPosition(node_position_i);
			if (pos.flyIndexT < nb_items)
				flyPositionList.set(pos.flyIndexT, pos);
			else {
				flyPositionList.add(pos);
				bAdded = true;
			}
		}

		if (bAdded)
			Collections.sort(flyPositionList, new Comparators.XYTaValue_Tindex_Comparator());
		return true;
	}

	public boolean xmlSaveXYTPositions(Node node) {
		if (node == null)
			return false;

		Element node_lastime = XMLUtil.addElement(node, ID_LASTIMEITMOVED);
		XMLUtil.setAttributeIntValue(node_lastime, ID_TLAST, lastTimeAlive);
		lastIntervalAlive = getLastIntervalAlive();
		XMLUtil.setAttributeIntValue(node_lastime, ID_ILAST, lastIntervalAlive);

		Element node_position_list = XMLUtil.addElement(node, ID_POSITIONSLIST);
		XMLUtil.setAttributeIntValue(node_position_list, ID_NBITEMS, flyPositionList.size());

		int i = 0;
		for (FlyPosition pos : flyPositionList) {
			String elementi = "i" + i;
			Element node_position_i = XMLUtil.addElement(node_position_list, elementi);
			pos.xmlSavePosition(node_position_i);
			i++;
		}
		return true;
	}

	// -----------------------------------------------

	public int computeLastIntervalAlive() {
		computeIsAlive();
		return lastIntervalAlive;
	}

	public void computeIsAlive() {
		computeDistanceBetweenConsecutivePoints();
		lastIntervalAlive = 0;
		boolean isalive = false;
		for (int i = flyPositionList.size() - 1; i >= 0; i--) {
			FlyPosition pos = flyPositionList.get(i);
			if (pos.distance > moveThreshold && !isalive) {
				lastIntervalAlive = i;
				lastTimeAlive = pos.flyIndexT;
				isalive = true;
			}
			pos.bAlive = isalive;
		}
	}

	public void checkIsAliveFromAliveArray() {
		lastIntervalAlive = 0;
		boolean isalive = false;
		for (int i = flyPositionList.size() - 1; i >= 0; i--) {
			FlyPosition pos = flyPositionList.get(i);
			if (!isalive && pos.bAlive) {
				lastIntervalAlive = i;
				lastTimeAlive = pos.flyIndexT;
				isalive = true;
			}
			pos.bAlive = isalive;
		}
	}

	public void computeDistanceBetweenConsecutivePoints() {
		if (flyPositionList.size() <= 0)
			return;

		// assume ordered points
		Point2D previousPoint = flyPositionList.get(0).getCenterRectangle();
		for (FlyPosition pos : flyPositionList) {
			Point2D currentPoint = pos.getCenterRectangle();
			pos.distance = currentPoint.distance(previousPoint);
			if (previousPoint.getX() < 0 || currentPoint.getX() < 0)
				pos.distance = Double.NaN;
			previousPoint = currentPoint;
		}
	}

	public void computeCumulatedDistance() {
		if (flyPositionList.size() <= 0)
			return;

		// assume ordered points
		double sum = 0.;
		for (FlyPosition pos : flyPositionList) {
			sum += pos.distance;
			pos.sumDistance = sum;
		}
	}

	// -----------------------------------------------------------

	public void excelComputeDistanceBetweenPoints(FlyPositions flyPositionsSource, int dataStepMs, int excelStepMs) {
		if (flyPositionsSource.flyPositionList.size() <= 0)
			return;

		flyPositionsSource.computeDistanceBetweenConsecutivePoints();
		flyPositionsSource.computeCumulatedDistance();

		int excel_startMs = 0;
		int n_excel_intervals = flyPositionList.size();
		int excel_endMs = n_excel_intervals * excelStepMs;
		int n_data_intervals = flyPositionsSource.flyPositionList.size();

		double sumDistance_previous = 0.;

		for (int excel_Ms = excel_startMs; excel_Ms < excel_endMs; excel_Ms += excelStepMs) {
			int excel_bin = excel_Ms / excelStepMs;
			FlyPosition dataFlyPositionThis = flyPositionList.get(excel_bin);

			int data_bin = excel_Ms / dataStepMs;
			int data_bin_remainder = excel_Ms % dataStepMs;
			FlyPosition dataFlyPositionSource = flyPositionsSource.flyPositionList.get(data_bin);

			double delta = 0.;
			if (data_bin_remainder != 0 && (data_bin + 1 < n_data_intervals)) {
				delta = flyPositionsSource.flyPositionList.get(data_bin + 1).distance * data_bin_remainder / dataStepMs;
			}
			dataFlyPositionThis.distance = dataFlyPositionSource.sumDistance - sumDistance_previous + delta;
			sumDistance_previous = dataFlyPositionSource.sumDistance;
		}
	}

	public void excelComputeIsAlive(FlyPositions flyPositions, int stepMs, int buildExcelStepMs) {
		flyPositions.computeIsAlive();
		int it_start = 0;
		int it_end = flyPositions.flyPositionList.size() * stepMs;
		int it_out = 0;
		for (int it = it_start; it < it_end && it_out < flyPositionList.size(); it += buildExcelStepMs, it_out++) {
			int index = it / stepMs;
			FlyPosition pos = flyPositionList.get(it_out);
			pos.bAlive = flyPositions.flyPositionList.get(index).bAlive;
		}
	}

	public void excelComputeSleep(FlyPositions flyPositions, int stepMs, int buildExcelStepMs) {
		flyPositions.computeSleep();
		int it_start = 0;
		int it_end = flyPositions.flyPositionList.size() * stepMs;
		int it_out = 0;
		for (int it = it_start; it < it_end && it_out < flyPositionList.size(); it += buildExcelStepMs, it_out++) {
			int index = it / stepMs;
			FlyPosition pos = flyPositionList.get(it_out);
			pos.bSleep = flyPositions.flyPositionList.get(index).bSleep;
		}
	}

	public void excelComputeNewPointsOrigin(Point2D newOrigin, FlyPositions flyPositions, int stepMs,
			int buildExcelStepMs) {
		newOrigin.setLocation(newOrigin.getX() * pixelsize, newOrigin.getY() * pixelsize);
		double deltaX = newOrigin.getX() - origin.getX();
		double deltaY = newOrigin.getY() - origin.getY();
		if (deltaX == 0 && deltaY == 0)
			return;
		int it_start = 0;
		int it_end = flyPositions.flyPositionList.size() * stepMs;
		int it_out = 0;
		for (int it = it_start; it < it_end && it_out < flyPositionList.size(); it += buildExcelStepMs, it_out++) {
			int index = it / stepMs;
			FlyPosition pos_from = flyPositions.flyPositionList.get(index);
			FlyPosition pos_to = flyPositionList.get(it_out);
			pos_to.copy(pos_from);
			pos_to.x -= deltaX;
			pos_to.y -= deltaY;
		}
	}

	public void excelComputeEllipse(FlyPositions flyPositions, int dataStepMs, int excelStepMs) {
		if (flyPositions.flyPositionList.size() <= 0)
			return;

		flyPositions.computeEllipseAxes();
		int excel_startMs = 0;
		int n_excel_intervals = flyPositionList.size();
		int excel_endMs = (n_excel_intervals - 1) * excelStepMs;

		for (int excel_Ms = excel_startMs; excel_Ms < excel_endMs; excel_Ms += excelStepMs) {
			int excel_bin = excel_Ms / excelStepMs;
			FlyPosition excel_pos = flyPositionList.get(excel_bin);

			int data_bin = excel_Ms / dataStepMs;
			FlyPosition data_pos = flyPositions.flyPositionList.get(data_bin);

			excel_pos.axis1 = data_pos.axis1;
			excel_pos.axis2 = data_pos.axis2;
		}
	}

	static public List<FlyPositions> computeMoveResults(Experiment expi, EnumXLSExport xlsOption,
			XLSExportOptions options, int nFrames, double pixelsize) {
		List<FlyPositions> positionsArrayList = new ArrayList<FlyPositions>(expi.cages.cageList.size());
		for (Cage cell : expi.cages.cageList) {
			FlyPositions flyPositions = new FlyPositions(cell.cageRoi2D.getName(), xlsOption, nFrames,
					options.buildExcelStepMs);
			flyPositions.nflies = cell.cageNFlies;
			if (flyPositions.nflies < 0) {
				flyPositions.setPixelSize(pixelsize);
				switch (xlsOption) {
				case DISTANCE:
					flyPositions.excelComputeDistanceBetweenPoints(cell.flyPositions, (int) expi.getCamImageBin_ms(),
							options.buildExcelStepMs);
					break;
				case ISALIVE:
					flyPositions.excelComputeIsAlive(cell.flyPositions, (int) expi.getCamImageBin_ms(),
							options.buildExcelStepMs);
					break;
				case SLEEP:
					flyPositions.excelComputeSleep(cell.flyPositions, (int) expi.getCamImageBin_ms(),
							options.buildExcelStepMs);
					break;
				case XYTOPCELL:
					flyPositions.excelComputeNewPointsOrigin(cell.getCenterTopCage(), cell.flyPositions,
							(int) expi.getCamImageBin_ms(), options.buildExcelStepMs);
					break;
				case XYTIPCAPS:
					flyPositions.excelComputeNewPointsOrigin(cell.getCenterTipCapillaries(expi.getCapillaries()),
							cell.flyPositions, (int) expi.getCamImageBin_ms(), options.buildExcelStepMs);
					break;
				case ELLIPSEAXES:
					flyPositions.excelComputeEllipse(cell.flyPositions, (int) expi.getCamImageBin_ms(),
							options.buildExcelStepMs);
					break;
				case XYIMAGE:
				default:
					break;
				}
				flyPositions.convertPixelsToPhysicalValues();
				positionsArrayList.add(flyPositions);
			}
		}
		return positionsArrayList;
	}

	// ------------------------------------------------------------

	public List<Double> getIsAliveAsDoubleArray() {
		ArrayList<Double> dataArray = new ArrayList<Double>();
		dataArray.ensureCapacity(flyPositionList.size());
		for (FlyPosition pos : flyPositionList)
			dataArray.add(pos.bAlive ? 1.0 : 0.0);
		return dataArray;
	}

	public List<Integer> getIsAliveAsIntegerArray() {
		ArrayList<Integer> dataArray = new ArrayList<Integer>();
		dataArray.ensureCapacity(flyPositionList.size());
		for (FlyPosition pos : flyPositionList) {
			dataArray.add(pos.bAlive ? 1 : 0);
		}
		return dataArray;
	}

	public int getLastIntervalAlive() {
		if (lastIntervalAlive >= 0)
			return lastIntervalAlive;
		return computeLastIntervalAlive();
	}

	private int getDeltaT() {
		return flyPositionList.get(1).flyIndexT - flyPositionList.get(0).flyIndexT;
	}

	public Double getDistanceBetween2Points(int firstTimeIndex, int secondTimeIndex) {
		if (flyPositionList.size() < 2)
			return Double.NaN;
		int firstIndex = firstTimeIndex / getDeltaT();
		int secondIndex = secondTimeIndex / getDeltaT();
		if (firstIndex < 0 || secondIndex < 0 || firstIndex >= flyPositionList.size()
				|| secondIndex >= flyPositionList.size())
			return Double.NaN;
		FlyPosition pos1 = flyPositionList.get(firstIndex);
		FlyPosition pos2 = flyPositionList.get(secondIndex);
		if (pos1.x < 0 || pos2.x < 0)
			return Double.NaN;

		Point2D point2 = pos2.getCenterRectangle();
		Double distance = point2.distance(pos1.getCenterRectangle());
		return distance;
	}

	public int isAliveAtTimeIndex(int timeIndex) {
		if (flyPositionList.size() < 2)
			return 0;
		getLastIntervalAlive();
		int index = timeIndex / getDeltaT();
		FlyPosition pos = flyPositionList.get(index);
		return (pos.bAlive ? 1 : 0);
	}

	private List<Integer> getDistanceAsMoveOrNot() {
		computeDistanceBetweenConsecutivePoints();
		ArrayList<Integer> dataArray = new ArrayList<Integer>();
		dataArray.ensureCapacity(flyPositionList.size());
		for (int i = 0; i < flyPositionList.size(); i++)
			dataArray.add(flyPositionList.get(i).distance < moveThreshold ? 1 : 0);
		return dataArray;
	}

	public void computeSleep() {
		if (flyPositionList.size() < 1)
			return;
		List<Integer> datai = getDistanceAsMoveOrNot();
		int timeBinSize = getDeltaT();
		int j = 0;
		for (FlyPosition pos : flyPositionList) {
			int isleep = 1;
			int k = 0;
			for (int i = 0; i < sleepThreshold; i += timeBinSize) {
				if ((k + j) >= datai.size())
					break;
				isleep = datai.get(k + j) * isleep;
				if (isleep == 0)
					break;
				k++;
			}
			pos.bSleep = (isleep == 1);
			j++;
		}
	}

	public List<Double> getSleepAsDoubleArray() {
		ArrayList<Double> dataArray = new ArrayList<Double>();
		dataArray.ensureCapacity(flyPositionList.size());
		for (FlyPosition pos : flyPositionList)
			dataArray.add(pos.bSleep ? 1.0 : 0.0);
		return dataArray;
	}

	public int isAsleepAtTimeIndex(int timeIndex) {
		if (flyPositionList.size() < 2)
			return -1;
		int index = timeIndex / getDeltaT();
		if (index >= flyPositionList.size())
			return -1;
		return (flyPositionList.get(index).bSleep ? 1 : 0);
	}

	public void computeNewPointsOrigin(Point2D newOrigin) {
		newOrigin.setLocation(newOrigin.getX() * pixelsize, newOrigin.getY() * pixelsize);
		double deltaX = newOrigin.getX() - origin.getX();
		double deltaY = newOrigin.getY() - origin.getY();
		if (deltaX == 0 && deltaY == 0)
			return;
		for (FlyPosition pos : flyPositionList) {
			pos.x -= deltaX;
			pos.y -= deltaY;
		}
	}

	public void computeEllipseAxes() {
		if (flyPositionList.size() < 1)
			return;

		for (FlyPosition pos : flyPositionList) {
			if (pos.flyRoi != null) {
				double[] ellipsoidValues = null;
				try {
					ellipsoidValues = ROI2DMeasures.computeOrientation(pos.flyRoi, null);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				pos.axis1 = ellipsoidValues[0];
				pos.axis2 = ellipsoidValues[1];
			} else if (pos.x != Double.NaN && pos.y != Double.NaN) {
				pos.axis1 = pos.h;
				pos.axis2 = pos.w;
				if (pos.axis2 > pos.axis1) {
					double x = pos.axis1;
					pos.axis1 = pos.axis2;
					pos.axis2 = x;
				}
			}
		}
	}

	public void setPixelSize(double newpixelSize) {
		pixelsize = newpixelSize;
	}

	public void convertPixelsToPhysicalValues() {
		for (FlyPosition pos : flyPositionList) {
			pos.x *= pixelsize;
			pos.y *= pixelsize;
			pos.w *= pixelsize;
			pos.h *= pixelsize;
			pos.axis1 = pos.axis1 * pixelsize;
			pos.axis2 = pos.axis2 * pixelsize;
		}
		origin.setLocation(origin.getX() * pixelsize, origin.getY() * pixelsize);
	}

	public void clearValues(int fromIndex) {
		int toIndex = flyPositionList.size();
		if (fromIndex > 0 && fromIndex < toIndex)
			flyPositionList.subList(fromIndex, toIndex).clear();
	}

	// --------------------------------------------------------

	public boolean cvsExport_Parameter_ToRow(StringBuffer sbf, String measure, String strCellNumber, String sep) {
		int npoints = 0;
		if (flyPositionList != null && flyPositionList.size() > 0)
			npoints = flyPositionList.size();

		sbf.append(strCellNumber + sep);
		sbf.append(measure + sep);
		sbf.append(Integer.toString(npoints) + sep);
		if (npoints > 0) {
			char measureType = measure.charAt(0);
			if (measureType == 't')
				for (int i = 0; i < npoints; i++)
					flyPositionList.get(i).cvsExportT(sbf, sep);
			else if (measureType == 'x')
				for (int i = 0; i < npoints; i++)
					flyPositionList.get(i).cvsExportX(sbf, sep);
			else if (measureType == 'y')
				for (int i = 0; i < npoints; i++)
					flyPositionList.get(i).cvsExportY(sbf, sep);
			else if (measureType == 'w')
				for (int i = 0; i < npoints; i++)
					flyPositionList.get(i).cvsExportWidth(sbf, sep);
			else if (measureType == 'h')
				for (int i = 0; i < npoints; i++)
					flyPositionList.get(i).cvsExportHeight(sbf, sep);
		}
		sbf.append("\n");
		return true;
	}

	public boolean cvsImport_Parameter_FromRow(String[] data) {
		if (data.length < 1)
			return false;

		char measureType = data[1].charAt(0);
		int npoints = Integer.valueOf(data[2]);
		if (flyPositionList.size() != npoints) {
			flyPositionList = new ArrayList<FlyPosition>(npoints);
			for (int i = 0; i < npoints; i++) {
				FlyPosition flyPosition = new FlyPosition();
				flyPositionList.add(flyPosition);
			}
		}
		int offset = 3;

		if (measureType == 't')
			for (int i = 0; i < npoints; i++)
				flyPositionList.get(i).cvsImportT(data[i + offset]);
		else if (measureType == 'x')
			for (int i = 0; i < npoints; i++)
				flyPositionList.get(i).cvsImportX(data[i + offset]);
		else if (measureType == 'y')
			for (int i = 0; i < npoints; i++)
				flyPositionList.get(i).cvsImportY(data[i + offset]);
		else if (measureType == 'w')
			for (int i = 0; i < npoints; i++)
				flyPositionList.get(i).cvsImportWidth(data[i + offset]);
		else if (measureType == 'h')
			for (int i = 0; i < npoints; i++)
				flyPositionList.get(i).cvsImportHeight(data[i + offset]);

		return true;
	}

	public boolean csvImport_Rectangle_FromRow(String[] data, int startAt) {
		if (data.length < startAt)
			return false;

		int npoints = Integer.valueOf(data[startAt]);
		if (npoints > 0) {
			flyPositionList = new ArrayList<FlyPosition>(npoints);
			int offset = startAt + 1;
			for (int i = 0; i < npoints; i++) {
				FlyPosition flyPosition = new FlyPosition();
				flyPosition.csvImportRectangle(data, offset);
				flyPositionList.add(flyPosition);
				offset += 5;
			}
		}
		return true;
	}

	public boolean csvImport_XY_FromRow(String[] data, int startAt) {
		if (data.length < startAt)
			return false;

		int npoints = Integer.valueOf(data[startAt]);
		if (npoints > 0) {
			flyPositionList = new ArrayList<FlyPosition>(npoints);
			int offset = startAt + 1;
			for (int i = 0; i < npoints; i++) {
				FlyPosition flyPosition = new FlyPosition();
				flyPosition.csvImportXY(data, offset);
				flyPositionList.add(flyPosition);
				offset += 3;
			}
		}
		return true;
	}

}
