package plugins.fmp.multicafeSequence;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import icy.file.xml.XMLPersistent;
import icy.roi.ROI;
import icy.roi.ROI2D;
import icy.type.geom.Polyline2D;
import icy.util.XMLUtil;
import plugins.kernel.roi.roi2d.ROI2DPolyLine;



public class CapillaryLimits  implements XMLPersistent  {

	public Polyline2D 	polyline = null;
	public String 		typename = "notype";
	public String		name = "noname";
	public int			indexImage = 0;
	
	private final String ID_NPOINTS		= "npoints";
	private final String ID_N			= "n";
	private final String ID_X			= "x";
	private final String ID_Y			= "y";
	
	// -------------------------
	
	CapillaryLimits(String typename, int indexImage) {
		this.typename = typename;
		this.indexImage = indexImage;
	}
	
	public CapillaryLimits(String name, int indexImage, Polyline2D polyline) {
		this.name = name;
		this.indexImage = indexImage;
		this.polyline = polyline;
	}
	
	public int getNpoints() {
		if (polyline == null)
			return 0;
		return polyline.npoints;
	}
	
	public void copy(CapillaryLimits cap) {
		polyline = (Polyline2D) cap.polyline.clone(); 
	}
	
	public boolean isThereAnyMeasuresDone() {
		boolean yes = (polyline != null && polyline.npoints > 0);
		return yes;
	}
	
	public List<Integer> getMeasures() {
		List<Integer> datai = getIntegerArrayFromPolyline2D();
		return datai;
	}
	
	public List<ROI> addToROIs(List<ROI> listrois, int indexImage) {
		this.indexImage = indexImage;
		if (polyline != null) 
			listrois.add(transferPolyline2DToROI());
		return listrois;
	}
	
	public List<ROI> addToROIs(List<ROI> listrois, Color color, double stroke, int indexImage) {
		if (polyline != null) { 
			ROI2D roi = transferPolyline2DToROI();
			roi.setColor(color);
			roi.setStroke(stroke);
			roi.setT(indexImage);
			roi.setName(name);
			listrois.add(roi);
		}
		return listrois;
	}
	
	public void transferROIsToMeasures(List<ROI> listRois) {	
		for (ROI roi: listRois) {		
			String roiname = roi.getName();
			if (roi instanceof ROI2DPolyLine ) {
				if  (roiname .contains (name)) {
					polyline = ((ROI2DPolyLine)roi).getPolyline2D();
					name = roiname;
					((ROI2DPolyLine) roi).setT(indexImage);
				}
			}
		}
	}
	
	@Override
	public boolean loadFromXML(Node node) {
		loadPolyline2DFromXML(node, typename);
		return false;
	}

	@Override
	public boolean saveToXML(Node node) {
		savePolyline2DToXML(node, typename);
		return false;
	}
	
	public List<Integer> getIntegerArrayFromPolyline2D() {
		if (polyline == null)
			return null;
		double [] array = polyline.ypoints;
		List<Integer> arrayInt = new ArrayList<Integer>(array.length);
		for (int i=0; i< array.length; i++)
			arrayInt.add((int) array[i]);
		return arrayInt;
	}
	
	public ROI2D transferPolyline2DToROI() {
		if (polyline == null)
			return null;
		
		ROI2D roi = new ROI2DPolyLine(polyline); 
		roi.setName(name);
		roi.setT(indexImage);
		return roi;
	}
	
	int loadPolyline2DFromXML(Node node, String nodename) {
		final Node nodeMeta = XMLUtil.getElement(node, nodename);
		int npoints = 0;
		polyline = null;
	    if (nodeMeta != null) {
	    	name =  XMLUtil.getElementValue(nodeMeta, "name", "noname");
	    	int npoints1 = XMLUtil.getElementIntValue(nodeMeta, ID_NPOINTS, 0);
	    	double[] xpoints = new double [npoints1];
	    	double[] ypoints = new double [npoints1];
	    	for (int i=0; i< npoints1; i++) {
	    		Element elmt = XMLUtil.getElement(nodeMeta, ID_N+i);
	    		if (i ==0)
	    			xpoints[i] = XMLUtil.getAttributeDoubleValue(elmt, ID_X, 0);
	    		else
	    			xpoints[i] = i+xpoints[0];
	    		ypoints[i] = XMLUtil.getAttributeDoubleValue(elmt, ID_Y, 0);
    		}
	    	polyline = new Polyline2D(xpoints, ypoints, npoints1);
	    }
	    if (polyline != null)
	    	npoints = polyline.npoints;
	    return npoints;
	}
	
	void savePolyline2DToXML(Node node, String nodename) {
		if (polyline == null)
			return;
		final Node nodeMeta = XMLUtil.setElement(node, nodename);
	    if (nodeMeta != null) {
	    	XMLUtil.setElementValue(nodeMeta, "name", name);
	    	XMLUtil.setElementIntValue(nodeMeta, ID_NPOINTS, polyline.npoints);
	    	for (int i=0; i< polyline.npoints; i++) {
	    		Element elmt = XMLUtil.setElement(nodeMeta, ID_N+i);
	    		if (i==0)
	    			XMLUtil.setAttributeDoubleValue(elmt, ID_X, polyline.xpoints[i]);
	    		XMLUtil.setAttributeDoubleValue(elmt, ID_Y, polyline.ypoints[i]);
	    	}
	    }
	}


}