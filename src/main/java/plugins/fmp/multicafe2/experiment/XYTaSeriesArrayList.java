package plugins.fmp.multicafe2.experiment;


import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import icy.util.XMLUtil;
import plugins.fmp.multicafe2.tools.Comparators;
import plugins.fmp.multicafe2.tools.ROI2DMeasures;
import plugins.fmp.multicafe2.tools.toExcel.EnumXLSExportType;
import plugins.kernel.roi.roi2d.ROI2DArea;




public class XYTaSeriesArrayList
{	
	public Double 			moveThreshold 		= 50.;
	public int				sleepThreshold		= 5;
	public int 				lastTimeAlive 		= 0;
	public int 				lastIntervalAlive 	= 0;
	public ArrayList<XYTaValue> xytArrayList	= new ArrayList<XYTaValue>();
	
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

	
	public XYTaSeriesArrayList() 
	{
	}
	
	public XYTaSeriesArrayList(String name, EnumXLSExportType exportType, int nFrames, int binsize) 
	{
		this.name = name;
		this.exportType = exportType;
		this.binsize = binsize;
		xytArrayList = new ArrayList<XYTaValue>(nFrames);
		for (int i = 0; i < nFrames; i++) 
			xytArrayList.add(new XYTaValue(i));
	}
	
	public void clear() 
	{
		xytArrayList.clear();
	}
	
	public void ensureCapacity(int nFrames) 
	{
		xytArrayList.ensureCapacity(nFrames);
//		initArray(nFrames);
	}
	
	void initArray(int nFrames) 
	{
		for (int i = 0; i < nFrames; i++) {
			XYTaValue value = new XYTaValue(i);
			xytArrayList.add(value);
		}
	}
	
	public Rectangle2D getRectangle(int i) 
	{
		return xytArrayList.get(i).rectBounds;
	}
	
	public Rectangle2D getValidPointAtOrBefore(int index) 
	{
		Rectangle2D rect = new Rectangle2D.Double(-1, -1, Double.NaN, Double.NaN);
		for (int i = index; i>= 0; i--) 
		{
			XYTaValue xyVal = xytArrayList.get(i);
			if (xyVal.rectBounds.getX() >= 0 && xyVal.rectBounds.getY() >= 0) {
				rect = xyVal.rectBounds;
				break;
			}	
		}
		return rect;
	}
	
	public int getTime(int i) 
	{
		return xytArrayList.get(i).indexT;
	}

	public void addPosition (int frame, Rectangle2D rectangle, ROI2DArea roiArea) 
	{
		XYTaValue pos = new XYTaValue(frame, rectangle, roiArea);
		xytArrayList.add(pos);
	}
	
	public void copyXYTaSeries (XYTaSeriesArrayList xySer) 
	{
		moveThreshold = xySer.moveThreshold;
		sleepThreshold = xySer.sleepThreshold;
		lastTimeAlive = xySer.lastIntervalAlive;
		xytArrayList = new ArrayList<XYTaValue>(xySer.xytArrayList.size());
		xytArrayList.addAll(xytArrayList);
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
		
		xytArrayList.clear();
		int nb_items =  XMLUtil.getAttributeIntValue(node_position_list, ID_NBITEMS, 0);
		xytArrayList.ensureCapacity(nb_items);
		for (int i = 0; i< nb_items; i++) 
			xytArrayList.add(new XYTaValue(i));
		boolean bAdded = false;
		
		for (int i=0; i< nb_items; i++) 
		{
			String elementi = "i"+i;
			Element node_position_i = XMLUtil.getElement(node_position_list, elementi);
			XYTaValue pos = new XYTaValue();
			pos.loadXYTvaluesFromXML(node_position_i);
			if (pos.indexT < nb_items) 
				xytArrayList.set(pos.indexT, pos);
			else 
			{
				xytArrayList.add(pos);
				bAdded = true;
			}
		}
		
		if (bAdded)
			Collections.sort(xytArrayList, new Comparators.XYTaValue_Tindex_Comparator());
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
		XMLUtil.setAttributeIntValue(node_position_list, ID_NBITEMS, xytArrayList.size());
		
		int i = 0;
		for (XYTaValue pos: xytArrayList) 
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
		for (int i= xytArrayList.size() - 1; i >= 0; i--) 
		{
			XYTaValue pos = xytArrayList.get(i);
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
		for (int i= xytArrayList.size() - 1; i >= 0; i--) 
		{
			XYTaValue pos = xytArrayList.get(i);
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
		if (xytArrayList.size() <= 0)
			return;
		
		// assume ordered points
		Point2D previousPoint = xytArrayList.get(0).getCenterRectangle();
		for (XYTaValue pos: xytArrayList) 
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
		if (xytArrayList.size() <= 0)
			return;
		
		// assume ordered points
		double sum = 0.;
		for (XYTaValue pos: xytArrayList) 
		{
			sum += pos.distance;
			pos.sumDistance = sum;
		}
	}
	
	// -----------------------------------------------------------
	
	public void excelComputeDistanceBetweenPoints(XYTaSeriesArrayList flyPositions, int dataStepMs, int excelStepMs) 
	{
		if (flyPositions.xytArrayList.size() <= 0)
			return;
		
		flyPositions.computeDistanceBetweenConsecutivePoints();
		flyPositions.computeCumulatedDistance();
		
		int excel_startMs = 0;
		int n_excel_intervals = xytArrayList.size();
		int excel_endMs = n_excel_intervals * excelStepMs;
		int n_data_intervals = flyPositions.xytArrayList.size();
		
		double sumDistance_previous = 0.;
		
		for (int excel_Ms = excel_startMs; excel_Ms < excel_endMs; excel_Ms += excelStepMs) 
		{
			int excel_bin = excel_Ms / excelStepMs;
			XYTaValue excel_pos = xytArrayList.get(excel_bin);
			
			int data_bin = excel_Ms / dataStepMs;
			int data_bin_remainder = excel_Ms % dataStepMs;
			XYTaValue data_pos = flyPositions.xytArrayList.get(data_bin);
			
			double delta = 0.;
			if (data_bin_remainder != 0 && (data_bin + 1 < n_data_intervals)) 
			{
				delta = flyPositions.xytArrayList.get(data_bin+1).distance * data_bin_remainder / dataStepMs;
			}
			excel_pos.distance = data_pos.sumDistance - sumDistance_previous + delta;
			sumDistance_previous = data_pos.sumDistance;
		}
	}
	
	public void excelComputeIsAlive(XYTaSeriesArrayList flyPositions, int stepMs, int buildExcelStepMs) 
	{
		flyPositions.computeIsAlive();
		int it_start = 0;
		int it_end = flyPositions.xytArrayList.size() * stepMs;
		int it_out = 0;
		for (int it = it_start; it < it_end && it_out < xytArrayList.size(); it += buildExcelStepMs, it_out++) 
		{
			int index = it/stepMs;
			XYTaValue pos = xytArrayList.get(it_out);
			pos.bAlive = flyPositions.xytArrayList.get(index).bAlive;
		}
	}
	
	public void excelComputeSleep(XYTaSeriesArrayList flyPositions, int stepMs, int buildExcelStepMs) 
	{
		flyPositions.computeSleep();
		int it_start = 0;
		int it_end = flyPositions.xytArrayList.size() * stepMs;
		int it_out = 0;
		for (int it = it_start; it < it_end && it_out < xytArrayList.size(); it += buildExcelStepMs, it_out++) 
		{
			int index = it/stepMs;
			XYTaValue pos = xytArrayList.get(it_out);
			pos.bSleep = flyPositions.xytArrayList.get(index).bSleep;
		}
	}
	
	public void excelComputeNewPointsOrigin(Point2D newOrigin, XYTaSeriesArrayList flyPositions, int stepMs, int buildExcelStepMs) 
	{
		newOrigin.setLocation(newOrigin.getX()*pixelsize, newOrigin.getY()*pixelsize);
		double deltaX = newOrigin.getX() - origin.getX();
		double deltaY = newOrigin.getY() - origin.getY();
		if (deltaX == 0 && deltaY == 0)
			return;
		int it_start = 0;
		int it_end = flyPositions.xytArrayList.size()  * stepMs;
		int it_out = 0;
		for (int it = it_start; it < it_end && it_out < xytArrayList.size(); it += buildExcelStepMs, it_out++) 
		{
			int index = it/stepMs;
			XYTaValue pos_from = flyPositions.xytArrayList.get(index);
			XYTaValue pos_to = xytArrayList.get(it_out);
			pos_to.copy(pos_from);
			pos_to.rectBounds.setRect( pos_to.rectBounds.getX()-deltaX, pos_to.rectBounds.getY()-deltaY,
					pos_to.rectBounds.getWidth(), pos_to.rectBounds.getHeight());
		}
	}
	
	public void excelComputeEllipse(XYTaSeriesArrayList flyPositions, int dataStepMs, int excelStepMs) 
	{
		if (flyPositions.xytArrayList.size() <= 0)
			return;
		
		flyPositions.computeEllipseAxes();
		int excel_startMs = 0;
		int n_excel_intervals = xytArrayList.size();
		int excel_endMs = (n_excel_intervals - 1) * excelStepMs;
		
		for (int excel_Ms = excel_startMs; excel_Ms < excel_endMs; excel_Ms += excelStepMs) 
		{
			int excel_bin = excel_Ms / excelStepMs;
			XYTaValue excel_pos = xytArrayList.get(excel_bin);
			
			int data_bin = excel_Ms / dataStepMs;
			XYTaValue data_pos = flyPositions.xytArrayList.get(data_bin);
			
			excel_pos.axis1 = data_pos.axis1;
			excel_pos.axis2 = data_pos.axis2;
		}
	}
	
	// ------------------------------------------------------------
	
	public List<Double> getIsAliveAsDoubleArray() 
	{
		ArrayList<Double> dataArray = new ArrayList<Double>();
		dataArray.ensureCapacity(xytArrayList.size());
		for (XYTaValue pos: xytArrayList) 
			dataArray.add(pos.bAlive ? 1.0: 0.0);
		return dataArray;
	}
	
	public List<Integer> getIsAliveAsIntegerArray() 
	{
		ArrayList<Integer> dataArray = new ArrayList<Integer>();
		dataArray.ensureCapacity(xytArrayList.size());
		for (XYTaValue pos: xytArrayList) 
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
		return xytArrayList.get(1).indexT - xytArrayList.get(0).indexT;
	}
	
	public Double getDistanceBetween2Points(int firstTimeIndex, int secondTimeIndex) 
	{
		if (xytArrayList.size() < 2)
			return Double.NaN;
		int firstIndex = firstTimeIndex / getTimeBinSize();
		int secondIndex = secondTimeIndex / getTimeBinSize();
		if (firstIndex < 0 || secondIndex < 0 || firstIndex >= xytArrayList.size() || secondIndex >= xytArrayList.size())
			return Double.NaN;
		XYTaValue pos1 = xytArrayList.get(firstIndex);
		XYTaValue pos2 = xytArrayList.get(secondIndex);
		if (pos1.rectBounds.getX() < 0 || pos2.rectBounds.getX()  < 0)
			return Double.NaN;

		Point2D point2 = pos2.getCenterRectangle();
		Double distance = point2.distance(pos1.getCenterRectangle()); 
		return distance;
	}
	
	public int isAliveAtTimeIndex(int timeIndex) 
	{
		if (xytArrayList.size() < 2)
			return 0;
		getLastIntervalAlive();
		int index = timeIndex / getTimeBinSize();
		XYTaValue pos = xytArrayList.get(index);
		return (pos.bAlive ? 1: 0); 
	}

	private List<Integer> getDistanceAsMoveOrNot() 
	{
		computeDistanceBetweenConsecutivePoints();
		ArrayList<Integer> dataArray = new ArrayList<Integer>();
		dataArray.ensureCapacity(xytArrayList.size());
		for (int i= 0; i< xytArrayList.size(); i++) 
			dataArray.add(xytArrayList.get(i).distance < moveThreshold ? 1: 0);
		return dataArray;
	}
	
	public void computeSleep() 
	{
		if (xytArrayList.size() < 1)
			return;
		List <Integer> datai = getDistanceAsMoveOrNot();
		int timeBinSize = getTimeBinSize() ;
		int j = 0;
		for (XYTaValue pos: xytArrayList) 
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
		dataArray.ensureCapacity(xytArrayList.size());
		for (XYTaValue pos: xytArrayList) 
			dataArray.add(pos.bSleep ? 1.0: 0.0);
		return dataArray;
	}
	
	public int isAsleepAtTimeIndex(int timeIndex) 
	{
		if (xytArrayList.size() < 2)
			return -1;
		int index = timeIndex / getTimeBinSize();
		if (index >= xytArrayList.size())
			return -1;
		return (xytArrayList.get(index).bSleep ? 1: 0); 
	}

	public void computeNewPointsOrigin(Point2D newOrigin) 
	{
		newOrigin.setLocation(newOrigin.getX()*pixelsize, newOrigin.getY()*pixelsize);
		double deltaX = newOrigin.getX() - origin.getX();
		double deltaY = newOrigin.getY() - origin.getY();
		if (deltaX == 0 && deltaY == 0)
			return;
		for (XYTaValue pos : xytArrayList) {
			pos.rectBounds.setRect(
					pos.rectBounds.getX()-deltaX, 
					pos.rectBounds.getY()-deltaY, 
					pos.rectBounds.getWidth(), 
					pos.rectBounds.getHeight());
		}
	}
	
	public void computeEllipseAxes() 
	{
		if (xytArrayList.size() < 1)
			return;

		for (XYTaValue pos: xytArrayList) 
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
		for (XYTaValue pos : xytArrayList) {
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
		int toIndex = xytArrayList.size();
		if (fromIndex > 0 && fromIndex < toIndex) 
			xytArrayList.subList(fromIndex, toIndex).clear();
		
	}

}


