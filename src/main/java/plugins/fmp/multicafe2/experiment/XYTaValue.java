package plugins.fmp.multicafe2.experiment;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import icy.util.XMLUtil;
import plugins.kernel.roi.roi2d.ROI2DArea;


public class XYTaValue
{
	public Rectangle2D rectBounds = new Rectangle2D.Double(Double.NaN,Double.NaN,Double.NaN,Double.NaN);
	public ROI2DArea flyRoi 	= null;
	public int 		indexT 		= 0;
	public boolean 	bAlive 		= false;
	public boolean 	bSleep 		= false;
	public boolean  bPadded		= false;
	public double	distance 	= 0.;
	public double  	sumDistance = 0;
	public double   axis1	    = 0.;
	public double   axis2	   	= 0.;
	
	
	public XYTaValue() 
	{
	}
	
	public XYTaValue(int indexT) 
	{
		this.indexT = indexT;
	}
	
	public XYTaValue(int indexT, Rectangle2D rectangle, ROI2DArea roiArea) 
	{
		if (rectangle != null)
			this.rectBounds.setRect(rectangle);
		this.flyRoi = new ROI2DArea(roiArea);
		this.indexT = indexT;
	}
	
	public XYTaValue(int indexT, Rectangle2D rectangle, boolean alive) 
	{
		if (rectangle != null)
			this.rectBounds.setRect(rectangle);
		this.indexT = indexT;
		this.bAlive = alive;
	}
	
	public void copy (XYTaValue aVal) 
	{
		indexT = aVal.indexT;
		bAlive = aVal.bAlive;
		bSleep = aVal.bSleep;
		bPadded = aVal.bPadded;
		distance = aVal.distance;
		rectBounds.setRect(aVal.rectBounds); 
		if (		aVal.flyRoi != null 
				&& aVal.flyRoi.getBounds().height > 0 
				&& aVal.flyRoi.getBounds().width > 0
			) 
			flyRoi = new ROI2DArea(aVal.flyRoi);
		axis1 = aVal.axis1;
		axis2 = aVal.axis2;
	}
	
	Point2D getCenterRectangle() {
		return new Point2D.Double (
				rectBounds.getX() + rectBounds.getWidth()/2,
				rectBounds.getY() + rectBounds.getHeight()/2);
	}
	
	// --------------------------------------------
	
	public boolean loadXYTvaluesFromXML(Node node) 
	{
		if (node == null)
			return false;	
		
		Element node_XYTa = XMLUtil.getElement(node, "XYTa");	
		if (node_XYTa != null) {
			double xR =  XMLUtil.getAttributeDoubleValue( node_XYTa, "xR", Double.NaN);	
			double yR =  XMLUtil.getAttributeDoubleValue( node_XYTa, "yR", Double.NaN);
			double wR =  XMLUtil.getAttributeDoubleValue( node_XYTa, "wR", Double.NaN);
			double hR =  XMLUtil.getAttributeDoubleValue( node_XYTa, "hR", Double.NaN);
			if (!Double.isNaN(xR) && !Double.isNaN(yR)) {
				rectBounds.setRect(xR, yR, wR, hR);
			} else {
				xR =  XMLUtil.getAttributeDoubleValue( node_XYTa, "x", Double.NaN);
				yR =  XMLUtil.getAttributeDoubleValue( node_XYTa, "y", Double.NaN);
				if (!Double.isNaN(xR) && !Double.isNaN(yR)) {
					xR -= 2.;
					yR -= 2.;
					wR = 4.;
					hR = 4.;
					rectBounds.setRect(xR, yR, wR, hR);
				}
			}
			
			indexT =  XMLUtil.getAttributeIntValue(node_XYTa, "t", 0);
			bAlive = XMLUtil.getAttributeBooleanValue(node_XYTa, "a", false);
			bSleep = XMLUtil.getAttributeBooleanValue(node_XYTa, "s", false);
		}
		
		Element node_roi = XMLUtil.getElement(node, "roi");
		if (node_roi != null) {
			if (flyRoi == null)
				flyRoi = new ROI2DArea();
			flyRoi.loadFromXML(node_roi);
		}
		
		return false;
	}

	public boolean saveXYTvaluesToXML(Node node) 
	{
		if (node == null)
			return false;
		
		Element node_XYTa = XMLUtil.addElement(node, "XYTa");
		
		if (!Double.isNaN(rectBounds.getX())) {
			XMLUtil.setAttributeDoubleValue(node_XYTa, "xR", rectBounds.getX());
			XMLUtil.setAttributeDoubleValue(node_XYTa, "yR", rectBounds.getY());
			XMLUtil.setAttributeDoubleValue(node_XYTa, "wR", rectBounds.getWidth());
			XMLUtil.setAttributeDoubleValue(node_XYTa, "hR", rectBounds.getHeight());
		}
		
		XMLUtil.setAttributeIntValue(node_XYTa, "t", indexT);
		XMLUtil.setAttributeBooleanValue(node_XYTa, "a", bAlive);
		XMLUtil.setAttributeBooleanValue(node_XYTa, "s", bSleep);
		
		Element node_roi = XMLUtil.addElement(node, "roi");
		if (flyRoi != null)
			flyRoi.saveToXML(node_roi);
		return false;
	}
}
