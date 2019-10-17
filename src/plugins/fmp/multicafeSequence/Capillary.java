package plugins.fmp.multicafeSequence;


import java.awt.Color;
import java.awt.geom.Point2D;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Node;

import icy.file.xml.XMLPersistent;
import icy.image.IcyBufferedImage;
import icy.roi.ROI;
import icy.util.XMLUtil;

import plugins.fmp.multicafeTools.EnumListType;
import plugins.fmp.multicafeTools.DetectGulps_Options;
import plugins.fmp.multicafeTools.DetectLimits_Options;
import plugins.kernel.roi.roi2d.ROI2DShape;

public class Capillary implements XMLPersistent  {

	public  final String ID_TOPLEVEL 	= "toplevel";	
	public  final String ID_BOTTOMLEVEL = "bottomlevel";	
	public  final String ID_DERIVATIVE 	= "derivedvalues";	
	private final String ID_META 		= "metaMC";
	private final String ID_ROI 		= "roiMC";
	private final String ID_INDEXIMAGE 	= "indexImageMC";
	private final String ID_NAME 		= "nameMC";
	private final String ID_NAMETIFF 	= "filenameTIFF";
	private final String ID_VERSION		= "version"; 
	private final String ID_VERSIONNUM	= "1.0.0"; 
	
	public int							indexImage 		= -1;
	private String						capillaryName 	= null;
	public String 						version 		= null;
	public ROI2DShape 					capillaryRoi 	= null;	// the capillary (source)
	public String						filenameTIFF	= null;
	public String 						stimulus		= new String("stimulus");
	public String 						concentration	= new String("xmM");

	public DetectLimits_Options 		limitsOptions	= new DetectLimits_Options();
	public DetectGulps_Options 			gulpsOptions	= new DetectGulps_Options();
	
	public CapillaryLimits				ptsTop  		= new CapillaryLimits(ID_TOPLEVEL, 0); 
	public CapillaryLimits				ptsBottom 		= new CapillaryLimits(ID_BOTTOMLEVEL, 0); 
	public CapillaryLimits				ptsDerivative 	= new CapillaryLimits(ID_DERIVATIVE, 0); 
	public CapillaryGulps 				gulpsRois 		= new CapillaryGulps(); 
	
	public List<ArrayList<int[]>> 		masksList 		= null;
	public List <double []> 			tabValuesList 	= null;
	public IcyBufferedImage 			bufImage 		= null;
		    
	// ----------------------------------------------------
	
	Capillary(ROI2DShape roi) {
		this.capillaryRoi = roi;
		this.capillaryName = replace_LR_with_12(roi.getName());
	}
	
	Capillary(String name) {
		this.capillaryName = replace_LR_with_12(name);
	}
	
	public Capillary() {
	}

	public void copy(Capillary cap) {
		indexImage 				= cap.indexImage;
		capillaryName 			= cap.capillaryName;
		version 				= cap.version;
		capillaryRoi 			= cap.capillaryRoi;
		filenameTIFF			= cap.filenameTIFF;
		limitsOptions			= cap.limitsOptions;
		gulpsOptions			= cap.gulpsOptions;
		
		gulpsRois.rois		= new ArrayList <ROI> ();
		gulpsRois.rois.addAll(cap.gulpsRois.rois);
		ptsTop.copy(cap.ptsTop); 
		ptsBottom.copy(cap.ptsBottom); 
		ptsDerivative.copy(cap.ptsDerivative); 
		
	}
	
	public String getName() {
		return capillaryName;
	}
	
	public void setName(String name) {
		this.capillaryName = name;
	}
	
	public String getLast2ofCapillaryName() {
		return capillaryRoi.getName().substring(capillaryRoi.getName().length() -2);
	}
	
	public String replace_LR_with_12(String name) {
		String newname = null;
		if (name .endsWith("R"))
			newname = name.replace("R",  "2");
		else if (name.endsWith("L"))
			newname = name.replace("L", "1");
		else 
			newname = name;
		return newname;
	}
	
	public List<Integer> getIntegerArrayFromPointArray(List<Point2D> ptsList) {
		if (ptsList == null)
			return null;
		List<Integer> arrayInt = new ArrayList<Integer> ();
		for (Point2D pt: ptsList) {
			int value = (int) pt.getY();
			arrayInt.add(value);
		}
		return arrayInt;
	}
	
	public List<Point2D> getPointArrayFromIntegerArray(List<Integer> data) {
		if (data == null)
			return null;
		List<Point2D> ptsList = null;
		if (data.size() > 0) {
			ptsList = new ArrayList<Point2D>(data.size());
			for (int i=0; i < data.size(); i++) {
				Point2D pt = new Point2D.Double((double) i, (double) data.get(i));
				ptsList.add(pt);
			}
		}
		return ptsList;
	}
	
	public boolean isThereAnyMeasuresDone(EnumListType option) {
		boolean yes = false;
		switch (option) {
		case derivedValues:
			yes= ptsDerivative.isThereAnyMeasuresDone();
			break;
		case cumSum:
			yes= gulpsRois.isThereAnyMeasuresDone();
			break;
		case bottomLevel:
			yes= ptsBottom.isThereAnyMeasuresDone();
			break;
		case topLevel:
		default:
			yes= ptsTop.isThereAnyMeasuresDone();
			break;
		}
		return yes;
	}
	
	public List<Integer> getMeasures(EnumListType option) {
		List<Integer> datai = null;
		switch (option) {
		case derivedValues:
			datai = ptsDerivative.getMeasures();
			break;
		case cumSum:
			datai = gulpsRois.getCumSumFromRoisArray(ptsTop.getNpoints());
			break;
		case bottomLevel:
			datai = ptsBottom.getMeasures();
			break;
		case topLevel:
		default:
			datai = ptsTop.getMeasures();
			break;
		}
		return datai;
	}
	
	public List<ROI> transferMeasuresToROIs() {
		List<ROI> listrois = new ArrayList<ROI> ();
		if (ptsTop != null) 
			ptsTop.transferMeasuresToROIs(listrois);
		if (ptsBottom != null) 
			ptsBottom.transferMeasuresToROIs(listrois);
		if (gulpsRois != null)	
			listrois.addAll(gulpsRois.rois);
		if (ptsDerivative != null) {
			ptsDerivative.transferMeasuresToROIs(listrois, Color.yellow, 1.);
		}
		return listrois;
	}
	
	public void transferROIsToMeasures(List<ROI> listRois) {	
		gulpsRois.transferROIsToMeasures(listRois);
		ptsTop.transferROIsToMeasures(listRois);
		ptsBottom.transferROIsToMeasures(listRois);
		ptsDerivative.transferROIsToMeasures(listRois);
	}

/*
//	public ROI2D transferPolyline2DToROI(String name, Polyline2D polyline) {
//		if (polyline == null)
//			return null;
//		
//		ROI2D roi = new ROI2DPolyLine(polyline); 
//		if (indexImage >= 0) {
//			roi.setT(indexImage);
//			roi.setName(getLast2ofCapillaryName()+"_"+name);
//		}
//		else
//			roi.setName(name);
//		return roi;
//	}
*/
	
	// ---------------------
	
	@Override
	public boolean loadFromXML(Node node) {
		boolean result = loadMetaDataFromXML(node);	
		result |= ptsDerivative.loadPolyline2DFromXML(node, ID_DERIVATIVE) > 0;
		result |= ptsTop.loadPolyline2DFromXML(node, ID_TOPLEVEL) > 0;
		result |= ptsBottom.loadPolyline2DFromXML(node, ID_BOTTOMLEVEL) > 0;
		result |= gulpsRois.loadFromXML(node);
		return result;
	}
	
	public boolean loadFromXML_CapillaryOnly(Node node) {
		boolean result = loadMetaDataFromXML(node);	
		return result;
	}

	@Override
	public boolean saveToXML(Node node) {
		saveMetaDataToXML(node);
		ptsDerivative.savePolyline2DToXML(node);
		ptsTop.savePolyline2DToXML(node);
		ptsBottom.savePolyline2DToXML(node);
		gulpsRois.saveToXML(node);
        return true;
	}
	
	public boolean saveToXML_(Node node) {
		saveMetaDataToXML(node);
		ptsDerivative.savePolyline2DToXML(node);
		ptsTop.savePolyline2DToXML(node);
		ptsBottom.savePolyline2DToXML(node);
		gulpsRois.saveToXML(node);
        return true;
	}
	
	public boolean saveToXML_CapillaryOnly(Node node) {
		saveMetaDataToXML(node);
        return true;
	}
	
	private boolean loadMetaDataFromXML(Node node) {
	    final Node nodeMeta = XMLUtil.getElement(node, ID_META);
	    if (nodeMeta == null)	// nothing to load
            return true;
	    if (nodeMeta != null) {
	    	version = XMLUtil.getElementValue(nodeMeta, ID_VERSION, ID_VERSIONNUM);
	    	indexImage = XMLUtil.getElementIntValue(nodeMeta, ID_INDEXIMAGE, indexImage);
	        capillaryName = XMLUtil.getElementValue(nodeMeta, ID_NAME, capillaryName);
	        filenameTIFF = XMLUtil.getElementValue(nodeMeta, ID_NAMETIFF, filenameTIFF);
	        capillaryRoi = (ROI2DShape) loadROIFromXML(nodeMeta);
	        limitsOptions.loadFromXML(nodeMeta);
	        gulpsOptions.loadFromXML(nodeMeta);
	    }
	    return true;
	}
	
	private void saveMetaDataToXML(Node node) {
	    final Node nodeMeta = XMLUtil.setElement(node, ID_META);
	    if (nodeMeta != null) {
	    	if (version == null)
	    		version = ID_VERSIONNUM;
	    	XMLUtil.setElementValue(nodeMeta, ID_VERSION, version);
	        XMLUtil.setElementIntValue(nodeMeta, ID_INDEXIMAGE, indexImage);
	        XMLUtil.setElementValue(nodeMeta, ID_NAME, capillaryName);
	        if (filenameTIFF != null ) {
	        	String filename = Paths.get(filenameTIFF).getFileName().toString();
	        	XMLUtil.setElementValue(nodeMeta, ID_NAMETIFF, filename);
	        }
	        saveROIToXML(nodeMeta, capillaryRoi); 
	    }
	}

	private void saveROIToXML(Node node, ROI roi) {
		final Node nodeROI = XMLUtil.addElement(node, ID_ROI);
        if (!roi.saveToXML(nodeROI)) {
            XMLUtil.removeNode(node, nodeROI);
            System.err.println("Error: the roi " + roi.getName() + " was not correctly saved to XML !");
        }
	}
 
	private ROI loadROIFromXML(Node node) {
		final Node nodeROI = XMLUtil.getElement(node, ID_ROI);
        if (nodeROI != null) {
			ROI roi = ROI.createFromXML(nodeROI);
	        return roi;
        }
        return null;
	}
	


	
}
