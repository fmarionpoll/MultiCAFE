package plugins.fmp.multicafe2.experiment;


import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import icy.util.StringUtil;
import icy.util.XMLUtil;
import plugins.fmp.multicafe2.tools.Comparators;
import plugins.fmp.multicafe2.tools.ROI2DMeasures;
import plugins.fmp.multicafe2.tools.toExcel.EnumXLSExportType;
import plugins.kernel.roi.roi2d.ROI2DArea;




public class FlyPositions
{	
	public Double 			moveThreshold 		= 50.;
	public int				sleepThreshold		= 5;
	public int 				lastTimeAlive 		= 0;
	public int 				lastIntervalAlive 	= 0;
	public ArrayList<FlyCoordinates> flyCoordinatesList	= new ArrayList<FlyCoordinates>();
	
	public String			name 				= null;
	public EnumXLSExportType exportType 		= null;
	public int				binsize				= 1;
	public Point2D			origin				= new Point2D.Double(0, 0);
	public double			pixelsize			= 1.;
	public int				nflies				= 1;
	
	private String ID_NBITEMS 		= "nb_items";
	private String ID_POSITIONSLIST	= "PositionsList";
	private String ID_LASTIMEITMOVED= "lastTimeItMoved";
	private String ID_TLAST			= "tlast";
	private String ID_ILAST			= "ilast";

	
	public FlyPositions() 
	{
	}
	
	public FlyPositions(String name, EnumXLSExportType exportType, int nFrames, int binsize) 
	{
		this.name = name;
		this.exportType = exportType;
		this.binsize = binsize;
		flyCoordinatesList = new ArrayList<FlyCoordinates>(nFrames);
		for (int i = 0; i < nFrames; i++) 
			flyCoordinatesList.add(new FlyCoordinates(i));
	}
	
	public void clear() 
	{
		flyCoordinatesList.clear();
	}
	
	public void ensureCapacity(int nFrames) 
	{
		flyCoordinatesList.ensureCapacity(nFrames);
//		initArray(nFrames);
	}
	
	void initArray(int nFrames) 
	{
		for (int i = 0; i < nFrames; i++) {
			FlyCoordinates value = new FlyCoordinates(i);
			flyCoordinatesList.add(value);
		}
	}
	
	public Rectangle2D getRectangle(int i) 
	{
		return flyCoordinatesList.get(i).rectBounds;
	}
	
	public Rectangle2D getValidPointAtOrBefore(int index) 
	{
		Rectangle2D rect = new Rectangle2D.Double(-1, -1, Double.NaN, Double.NaN);
		for (int i = index; i>= 0; i--) 
		{
			FlyCoordinates xyVal = flyCoordinatesList.get(i);
			if (xyVal.rectBounds.getX() >= 0 && xyVal.rectBounds.getY() >= 0) {
				rect = xyVal.rectBounds;
				break;
			}	
		}
		return rect;
	}
	
	public int getTime(int i) 
	{
		return flyCoordinatesList.get(i).indexT;
	}

	public void addPosition (int frame, Rectangle2D rectangle, ROI2DArea roiArea) 
	{
		FlyCoordinates pos = new FlyCoordinates(frame, rectangle, roiArea);
		flyCoordinatesList.add(pos);
	}
	
	public void copyXYTaSeries (FlyPositions xySer) 
	{
		moveThreshold = xySer.moveThreshold;
		sleepThreshold = xySer.sleepThreshold;
		lastTimeAlive = xySer.lastIntervalAlive;
		flyCoordinatesList = new ArrayList<FlyCoordinates>(xySer.flyCoordinatesList.size());
		flyCoordinatesList.addAll(flyCoordinatesList);
		name = xySer.name;
		exportType = xySer.exportType;
		binsize = xySer.binsize;
	}

	// -----------------------------------------------
	
	public boolean loadXYTseriesFromXML(Node node) 
	{
		if (node == null)
			return false;
		
		Element node_lastime = XMLUtil.getElement(node, ID_LASTIMEITMOVED);
		lastTimeAlive = XMLUtil.getAttributeIntValue(node_lastime, ID_TLAST, -1);
		lastIntervalAlive = XMLUtil.getAttributeIntValue(node_lastime, ID_ILAST, -1);

		Element node_position_list = XMLUtil.getElement(node, ID_POSITIONSLIST);
		if (node_position_list == null) 
			return false;
		
		flyCoordinatesList.clear();
		int nb_items =  XMLUtil.getAttributeIntValue(node_position_list, ID_NBITEMS, 0);
		flyCoordinatesList.ensureCapacity(nb_items);
		for (int i = 0; i< nb_items; i++) 
			flyCoordinatesList.add(new FlyCoordinates(i));
		boolean bAdded = false;
		
		for (int i = 0; i < nb_items; i++) 
		{
			String elementi = "i"+i;
			Element node_position_i = XMLUtil.getElement(node_position_list, elementi);
			FlyCoordinates pos = new FlyCoordinates();
			pos.loadXYTvaluesFromXML(node_position_i);
			if (pos.indexT < nb_items) 
				flyCoordinatesList.set(pos.indexT, pos);
			else 
			{
				flyCoordinatesList.add(pos);
				bAdded = true;
			}
		}
		
		if (bAdded)
			Collections.sort(flyCoordinatesList, new Comparators.XYTaValue_Tindex_Comparator());
		return true;
	}

	public boolean saveXYTseriesToXML(Node node) 
	{
		if (node == null)
			return false;
		
		Element node_lastime = XMLUtil.addElement(node, ID_LASTIMEITMOVED);
		XMLUtil.setAttributeIntValue(node_lastime, ID_TLAST, lastTimeAlive);
		lastIntervalAlive = getLastIntervalAlive();
		XMLUtil.setAttributeIntValue(node_lastime, ID_ILAST, lastIntervalAlive);
		
		Element node_position_list = XMLUtil.addElement(node, ID_POSITIONSLIST);
		XMLUtil.setAttributeIntValue(node_position_list, ID_NBITEMS, flyCoordinatesList.size());
		
		int i = 0;
		for (FlyCoordinates pos: flyCoordinatesList) 
		{
			String elementi = "i"+i;
			Element node_position_i = XMLUtil.addElement(node_position_list, elementi);
			pos.saveXYTvaluesToXML(node_position_i);
			i++;
		}
		return true;
	}
	
	// -----------------------------------------------
	
	public int computeLastIntervalAlive() 
	{
		computeIsAlive();
		return lastIntervalAlive;
	}
	
	public void computeIsAlive() 
	{
		computeDistanceBetweenConsecutivePoints();
		lastIntervalAlive = 0;
		boolean isalive = false;
		for (int i= flyCoordinatesList.size() - 1; i >= 0; i--) 
		{
			FlyCoordinates pos = flyCoordinatesList.get(i);
			if (pos.distance > moveThreshold && !isalive) 
			{
				lastIntervalAlive = i;
				lastTimeAlive = pos.indexT;
				isalive = true;				
			}
			pos.bAlive = isalive;
		}
	}
	
	public void checkIsAliveFromAliveArray() 
	{
		lastIntervalAlive = 0;
		boolean isalive = false;
		for (int i= flyCoordinatesList.size() - 1; i >= 0; i--) 
		{
			FlyCoordinates pos = flyCoordinatesList.get(i);
			if (!isalive && pos.bAlive) 
			{
				lastIntervalAlive = i;
				lastTimeAlive = pos.indexT;
				isalive = true;				
			}
			pos.bAlive = isalive;
		}
	}

	public void computeDistanceBetweenConsecutivePoints() 
	{
		if (flyCoordinatesList.size() <= 0)
			return;
		
		// assume ordered points
		Point2D previousPoint = flyCoordinatesList.get(0).getCenterRectangle();
		for (FlyCoordinates pos: flyCoordinatesList) 
		{
			Point2D currentPoint = pos.getCenterRectangle();
			pos.distance = currentPoint.distance(previousPoint);
			if (previousPoint.getX() < 0 || currentPoint.getX() < 0)
				pos.distance = Double.NaN;
			previousPoint = currentPoint;
		}
	}
	
	public void computeCumulatedDistance() 
	{
		if (flyCoordinatesList.size() <= 0)
			return;
		
		// assume ordered points
		double sum = 0.;
		for (FlyCoordinates pos: flyCoordinatesList) 
		{
			sum += pos.distance;
			pos.sumDistance = sum;
		}
	}
	
	// -----------------------------------------------------------
	
	public void excelComputeDistanceBetweenPoints(FlyPositions flyPositions, int dataStepMs, int excelStepMs) 
	{
		if (flyPositions.flyCoordinatesList.size() <= 0)
			return;
		
		flyPositions.computeDistanceBetweenConsecutivePoints();
		flyPositions.computeCumulatedDistance();
		
		int excel_startMs = 0;
		int n_excel_intervals = flyCoordinatesList.size();
		int excel_endMs = n_excel_intervals * excelStepMs;
		int n_data_intervals = flyPositions.flyCoordinatesList.size();
		
		double sumDistance_previous = 0.;
		
		for (int excel_Ms = excel_startMs; excel_Ms < excel_endMs; excel_Ms += excelStepMs) 
		{
			int excel_bin = excel_Ms / excelStepMs;
			FlyCoordinates excel_pos = flyCoordinatesList.get(excel_bin);
			
			int data_bin = excel_Ms / dataStepMs;
			int data_bin_remainder = excel_Ms % dataStepMs;
			FlyCoordinates data_pos = flyPositions.flyCoordinatesList.get(data_bin);
			
			double delta = 0.;
			if (data_bin_remainder != 0 && (data_bin + 1 < n_data_intervals)) 
			{
				delta = flyPositions.flyCoordinatesList.get(data_bin+1).distance * data_bin_remainder / dataStepMs;
			}
			excel_pos.distance = data_pos.sumDistance - sumDistance_previous + delta;
			sumDistance_previous = data_pos.sumDistance;
		}
	}
	
	public void excelComputeIsAlive(FlyPositions flyPositions, int stepMs, int buildExcelStepMs) 
	{
		flyPositions.computeIsAlive();
		int it_start = 0;
		int it_end = flyPositions.flyCoordinatesList.size() * stepMs;
		int it_out = 0;
		for (int it = it_start; it < it_end && it_out < flyCoordinatesList.size(); it += buildExcelStepMs, it_out++) 
		{
			int index = it/stepMs;
			FlyCoordinates pos = flyCoordinatesList.get(it_out);
			pos.bAlive = flyPositions.flyCoordinatesList.get(index).bAlive;
		}
	}
	
	public void excelComputeSleep(FlyPositions flyPositions, int stepMs, int buildExcelStepMs) 
	{
		flyPositions.computeSleep();
		int it_start = 0;
		int it_end = flyPositions.flyCoordinatesList.size() * stepMs;
		int it_out = 0;
		for (int it = it_start; it < it_end && it_out < flyCoordinatesList.size(); it += buildExcelStepMs, it_out++) 
		{
			int index = it/stepMs;
			FlyCoordinates pos = flyCoordinatesList.get(it_out);
			pos.bSleep = flyPositions.flyCoordinatesList.get(index).bSleep;
		}
	}
	
	public void excelComputeNewPointsOrigin(Point2D newOrigin, FlyPositions flyPositions, int stepMs, int buildExcelStepMs) 
	{
		newOrigin.setLocation(newOrigin.getX()*pixelsize, newOrigin.getY()*pixelsize);
		double deltaX = newOrigin.getX() - origin.getX();
		double deltaY = newOrigin.getY() - origin.getY();
		if (deltaX == 0 && deltaY == 0)
			return;
		int it_start = 0;
		int it_end = flyPositions.flyCoordinatesList.size()  * stepMs;
		int it_out = 0;
		for (int it = it_start; it < it_end && it_out < flyCoordinatesList.size(); it += buildExcelStepMs, it_out++) 
		{
			int index = it/stepMs;
			FlyCoordinates pos_from = flyPositions.flyCoordinatesList.get(index);
			FlyCoordinates pos_to = flyCoordinatesList.get(it_out);
			pos_to.copy(pos_from);
			pos_to.rectBounds.setRect( pos_to.rectBounds.getX()-deltaX, pos_to.rectBounds.getY()-deltaY,
					pos_to.rectBounds.getWidth(), pos_to.rectBounds.getHeight());
		}
	}
	
	public void excelComputeEllipse(FlyPositions flyPositions, int dataStepMs, int excelStepMs) 
	{
		if (flyPositions.flyCoordinatesList.size() <= 0)
			return;
		
		flyPositions.computeEllipseAxes();
		int excel_startMs = 0;
		int n_excel_intervals = flyCoordinatesList.size();
		int excel_endMs = (n_excel_intervals - 1) * excelStepMs;
		
		for (int excel_Ms = excel_startMs; excel_Ms < excel_endMs; excel_Ms += excelStepMs) 
		{
			int excel_bin = excel_Ms / excelStepMs;
			FlyCoordinates excel_pos = flyCoordinatesList.get(excel_bin);
			
			int data_bin = excel_Ms / dataStepMs;
			FlyCoordinates data_pos = flyPositions.flyCoordinatesList.get(data_bin);
			
			excel_pos.axis1 = data_pos.axis1;
			excel_pos.axis2 = data_pos.axis2;
		}
	}
	
	// ------------------------------------------------------------
	
	public List<Double> getIsAliveAsDoubleArray() 
	{
		ArrayList<Double> dataArray = new ArrayList<Double>();
		dataArray.ensureCapacity(flyCoordinatesList.size());
		for (FlyCoordinates pos: flyCoordinatesList) 
			dataArray.add(pos.bAlive ? 1.0: 0.0);
		return dataArray;
	}
	
	public List<Integer> getIsAliveAsIntegerArray() 
	{
		ArrayList<Integer> dataArray = new ArrayList<Integer>();
		dataArray.ensureCapacity(flyCoordinatesList.size());
		for (FlyCoordinates pos: flyCoordinatesList) 
		{
			dataArray.add(pos.bAlive ? 1: 0);
		}
		return dataArray;
	}
		
	public int getLastIntervalAlive() 
	{
		if (lastIntervalAlive >= 0)
			return lastIntervalAlive;
		return computeLastIntervalAlive();
	}
	
	public int getTimeBinSize () 
	{
		return flyCoordinatesList.get(1).indexT - flyCoordinatesList.get(0).indexT;
	}
	
	public Double getDistanceBetween2Points(int firstTimeIndex, int secondTimeIndex) 
	{
		if (flyCoordinatesList.size() < 2)
			return Double.NaN;
		int firstIndex = firstTimeIndex / getTimeBinSize();
		int secondIndex = secondTimeIndex / getTimeBinSize();
		if (firstIndex < 0 || secondIndex < 0 || firstIndex >= flyCoordinatesList.size() || secondIndex >= flyCoordinatesList.size())
			return Double.NaN;
		FlyCoordinates pos1 = flyCoordinatesList.get(firstIndex);
		FlyCoordinates pos2 = flyCoordinatesList.get(secondIndex);
		if (pos1.rectBounds.getX() < 0 || pos2.rectBounds.getX()  < 0)
			return Double.NaN;

		Point2D point2 = pos2.getCenterRectangle();
		Double distance = point2.distance(pos1.getCenterRectangle()); 
		return distance;
	}
	
	public int isAliveAtTimeIndex(int timeIndex) 
	{
		if (flyCoordinatesList.size() < 2)
			return 0;
		getLastIntervalAlive();
		int index = timeIndex / getTimeBinSize();
		FlyCoordinates pos = flyCoordinatesList.get(index);
		return (pos.bAlive ? 1: 0); 
	}

	private List<Integer> getDistanceAsMoveOrNot() 
	{
		computeDistanceBetweenConsecutivePoints();
		ArrayList<Integer> dataArray = new ArrayList<Integer>();
		dataArray.ensureCapacity(flyCoordinatesList.size());
		for (int i= 0; i< flyCoordinatesList.size(); i++) 
			dataArray.add(flyCoordinatesList.get(i).distance < moveThreshold ? 1: 0);
		return dataArray;
	}
	
	public void computeSleep() 
	{
		if (flyCoordinatesList.size() < 1)
			return;
		List <Integer> datai = getDistanceAsMoveOrNot();
		int timeBinSize = getTimeBinSize() ;
		int j = 0;
		for (FlyCoordinates pos: flyCoordinatesList) 
		{
			int isleep = 1;
			int k = 0;
			for (int i= 0; i < sleepThreshold; i+= timeBinSize) 
			{
				if ((k+j) >= datai.size())
					break;
				isleep = datai.get(k+j) * isleep;
				if (isleep == 0)
					break;
				k++;
			}
			pos.bSleep = (isleep == 1);
			j++;
		}
	}
	
	public List<Double> getSleepAsDoubleArray() 
	{
		ArrayList<Double> dataArray = new ArrayList<Double>();
		dataArray.ensureCapacity(flyCoordinatesList.size());
		for (FlyCoordinates pos: flyCoordinatesList) 
			dataArray.add(pos.bSleep ? 1.0: 0.0);
		return dataArray;
	}
	
	public int isAsleepAtTimeIndex(int timeIndex) 
	{
		if (flyCoordinatesList.size() < 2)
			return -1;
		int index = timeIndex / getTimeBinSize();
		if (index >= flyCoordinatesList.size())
			return -1;
		return (flyCoordinatesList.get(index).bSleep ? 1: 0); 
	}

	public void computeNewPointsOrigin(Point2D newOrigin) 
	{
		newOrigin.setLocation(newOrigin.getX()*pixelsize, newOrigin.getY()*pixelsize);
		double deltaX = newOrigin.getX() - origin.getX();
		double deltaY = newOrigin.getY() - origin.getY();
		if (deltaX == 0 && deltaY == 0)
			return;
		for (FlyCoordinates pos : flyCoordinatesList) {
			pos.rectBounds.setRect(
					pos.rectBounds.getX()-deltaX, 
					pos.rectBounds.getY()-deltaY, 
					pos.rectBounds.getWidth(), 
					pos.rectBounds.getHeight());
		}
	}
	
	public void computeEllipseAxes() 
	{
		if (flyCoordinatesList.size() < 1)
			return;

		for (FlyCoordinates pos: flyCoordinatesList) 
		{
			if (pos.flyRoi != null) 
			{
				double[] ellipsoidValues = null;
				try {
					ellipsoidValues = ROI2DMeasures.computeOrientation(pos.flyRoi, null);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				pos.axis1 = ellipsoidValues[0];
				pos.axis2 = ellipsoidValues[1];
			}
			else if (pos.rectBounds != null) 
			{
				pos.axis1 = pos.rectBounds.getHeight();
				pos.axis2 = pos.rectBounds.getWidth();
				if (pos.axis2 > pos.axis1) {
					double x = pos.axis1;
					pos.axis1 = pos.axis2;
					pos.axis2 = x;
				}
			}
		}
	}
	
	public void setPixelSize(double newpixelSize) 
	{
		pixelsize = newpixelSize;
	}
	
	public void convertPixelsToPhysicalValues() 
	{
		for (FlyCoordinates pos : flyCoordinatesList) {
			pos.rectBounds.setRect(
					pos.rectBounds.getX()*pixelsize, 
					pos.rectBounds.getY()*pixelsize, 
					pos.rectBounds.getWidth()*pixelsize, 
					pos.rectBounds.getHeight()*pixelsize);
			
			pos.axis1 = pos.axis1 * pixelsize;
			pos.axis2 = pos.axis2 * pixelsize;
		}
		
		origin.setLocation(origin.getX()*pixelsize, origin.getY()*pixelsize);
	}

	public void clearValues(int fromIndex) 
	{
		int toIndex = flyCoordinatesList.size();
		if (fromIndex > 0 && fromIndex < toIndex) 
			flyCoordinatesList.subList(fromIndex, toIndex).clear();
		
	}

	// --------------------------------------------------------
	
	public boolean cvsExportXYDataToRow(StringBuffer sbf, String sep) 
	{
		int npoints = 0;
		if (flyCoordinatesList != null && flyCoordinatesList.npoints > 0)
			npoints = polylineLevel.npoints; 
			
		sbf.append(Integer.toString(npoints)+ sep);
		if (npoints > 0) {
			for (int i = 0; i < polylineLevel.npoints; i++)
	        {
	            sbf.append(StringUtil.toString((double) polylineLevel.xpoints[i]));
	            sbf.append(sep);
	            sbf.append(StringUtil.toString((double) polylineLevel.ypoints[i]));
	            sbf.append(sep);
	        }
		}
		return true;
	}
	
	public boolean cvsExportYDataToRow(StringBuffer sbf, String sep) 
	{
		int npoints = 0;
		if (polylineLevel != null && polylineLevel.npoints > 0)
			npoints = polylineLevel.npoints; 
			
		sbf.append(Integer.toString(npoints)+ sep);
		if (npoints > 0) {
			for (int i = 0; i < polylineLevel.npoints; i++)
	        {
	            sbf.append(StringUtil.toString((double) polylineLevel.ypoints[i]));
	            sbf.append(sep);
	        }
		}
		return true;
	}
	
	public boolean csvImportXYDataFromRow(String[] data, int startAt) 
	{
		if (data.length < startAt)
			return false;
		
		int npoints = Integer.valueOf(data[startAt]);
		if (npoints > 0) {
			double[] x = new double[npoints];
			double[] y = new double[npoints];
			int offset = startAt+1;
			for (int i = 0; i < npoints; i++) { 
				x[i] = Double.valueOf(data[offset]);
				offset++;
				y[i] = Double.valueOf(data[offset]);
				offset++;
			}
			polylineLevel = new Level2D(x, y, npoints);
		}
		return true;
	}
	
	public boolean csvImportYDataFromRow(String[] data, int startAt) 
	{
		if (data.length < startAt)
			return false;
		
		int npoints = Integer.valueOf(data[startAt]);
		if (npoints > 0) {
			double[] x = new double[npoints];
			double[] y = new double[npoints];
			int offset = startAt+1;
			for (int i = 0; i < npoints; i++) { 
				x[i] = i;
				y[i] = Double.valueOf(data[offset]);
				offset++;
			}
			polylineLevel = new Level2D(x, y, npoints);
		}
		return true;
	}
	
}


