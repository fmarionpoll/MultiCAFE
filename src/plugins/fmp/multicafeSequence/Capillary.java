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
import plugins.fmp.multicafeTools.ROI2DUtilities;
import plugins.fmp.multicafeTools.DetectGulps_Options;
import plugins.fmp.multicafeTools.DetectLimits_Options;
import plugins.kernel.roi.roi2d.ROI2DPolyLine;
import plugins.kernel.roi.roi2d.ROI2DShape;

public class Capillary implements XMLPersistent  {

	public ROI2DShape 					capillaryRoi 	= null;	// the capillary (source)
	public int							indexImage 		= -1;
	private String						capillaryName 	= null;
	public String 						version 		= null;
	public String						filenameTIFF	= null;
	public String 						stimulus		= new String("stimulus");
	public String 						concentration	= new String("xmM");
	
	public DetectLimits_Options 		limitsOptions	= new DetectLimits_Options();
	public DetectGulps_Options 			gulpsOptions	= new DetectGulps_Options();
	
	public  final String 				ID_TOPLEVEL 	= "toplevel";	
	public  final String 				ID_BOTTOMLEVEL 	= "bottomlevel";	
	public  final String 				ID_DERIVATIVE 	= "derivative";	
	public CapillaryLimits				ptsTop  		= new CapillaryLimits(ID_TOPLEVEL, 0); 
	public CapillaryLimits				ptsBottom 		= new CapillaryLimits(ID_BOTTOMLEVEL, 0); 
	public CapillaryLimits				ptsDerivative 	= new CapillaryLimits(ID_DERIVATIVE, 0); 
	public CapillaryGulps 				gulpsRois 		= new CapillaryGulps(); 
	
	public List<ArrayList<int[]>> 		masksList 		= null;
	public List <double []> 			tabValuesList 	= null;
	public IcyBufferedImage 			bufImage 		= null;
	public boolean						valid			= true;

	private final String 				ID_META 		= "metaMC";
	private final String 				ID_ROI 			= "roiMC";
	private final String 				ID_INDEXIMAGE 	= "indexImageMC";
	private final String 				ID_NAME 		= "nameMC";
	private final String 				ID_NAMETIFF 	= "filenameTIFF";
	private final String 				ID_VERSION		= "version"; 
	private final String 				ID_VERSIONNUM	= "1.0.0"; 
	
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
	
	public boolean isThereAnyMeasuresDone(EnumListType option) {
		boolean yes = false;
		switch (option) {
		case derivedValues:
			yes= (ptsDerivative != null && ptsDerivative.isThereAnyMeasuresDone());
			break;
		case cumSum:
			yes= (gulpsRois!= null && gulpsRois.isThereAnyMeasuresDone());
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
			if (ptsDerivative != null)
				datai = ptsDerivative.getMeasures();
			break;
		case cumSum:
			if (gulpsRois != null)
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
	
	public int getLastMeasure(EnumListType option) {
		int lastMeasure = 0;
		switch (option) {
		case derivedValues:
			if (ptsDerivative != null)
				lastMeasure = ptsDerivative.getLastMeasure();
			break;
		case cumSum:
			if (gulpsRois != null) {
				List<Integer> datai = gulpsRois.getCumSumFromRoisArray(ptsTop.getNpoints());
				lastMeasure = datai.get(datai.size()-1);
			}
			break;
		case bottomLevel:
			lastMeasure = ptsBottom.getLastMeasure();
			break;
		case topLevel:
		default:
			lastMeasure = ptsTop.getLastMeasure();
			break;
		}
		return lastMeasure;
	}
	
	public int getLastDeltaMeasure(EnumListType option) {
		int lastMeasure = 0;
		switch (option) {
		case derivedValues:
			if (ptsDerivative != null)
				lastMeasure = ptsDerivative.getLastDeltaMeasure();
			break;
		case cumSum:
			if (gulpsRois != null) {
				List<Integer> datai = gulpsRois.getCumSumFromRoisArray(ptsTop.getNpoints());
				lastMeasure = datai.get(datai.size()-1) - datai.get(datai.size()-2);
			}
			break;
		case bottomLevel:
			lastMeasure = ptsBottom.getLastDeltaMeasure();
			break;
		case topLevel:
		default:
			lastMeasure = ptsTop.getLastDeltaMeasure();
			break;
		}
		return lastMeasure;
	}
	
	public int getT0Measure(EnumListType option) {
		int t0Measure = 0;
		switch (option) {
		case derivedValues:
			if (ptsDerivative != null)
				t0Measure = ptsDerivative.getT0Measure();
			break;
		case cumSum:
			if (gulpsRois != null) {
				List<Integer> datai = gulpsRois.getCumSumFromRoisArray(ptsTop.getNpoints());
				t0Measure = datai.get(0);
			}
			break;
		case bottomLevel:
			t0Measure = ptsBottom.getT0Measure();
			break;
		case topLevel:
		default:
			t0Measure = ptsTop.getT0Measure();
			break;
		}
		return t0Measure;
	}
	
	public List<ROI> transferMeasuresToROIs() {
		List<ROI> listrois = new ArrayList<ROI> ();
		if (ptsTop != null)
			ptsTop.addToROIs(listrois, indexImage);
		if (ptsBottom != null)
			ptsBottom.addToROIs(listrois, indexImage);
		if (gulpsRois != null)
			gulpsRois.addToROIs(listrois, indexImage);
		if (ptsDerivative != null)
			ptsDerivative.addToROIs(listrois, Color.yellow, 1., indexImage);
		return listrois;
	}
	
	public void transferROIsToMeasures(List<ROI> listRois) {
		if (ptsTop != null)
			ptsTop.transferROIsToMeasures(listRois);
		if (ptsBottom != null)
			ptsBottom.transferROIsToMeasures(listRois);
		if (gulpsRois != null)
			gulpsRois.transferROIsToMeasures(listRois);
		if (ptsDerivative != null)
			ptsDerivative.transferROIsToMeasures(listRois);
	}

	@Override
	public boolean loadFromXML(Node node) {
		boolean result = loadFromXML_CapillaryOnly(node);	
		result |= ptsDerivative.loadPolyline2DFromXML(node, ID_DERIVATIVE) > 0;
		result |= ptsTop.loadPolyline2DFromXML(node, ID_TOPLEVEL) > 0;
		result |= ptsBottom.loadPolyline2DFromXML(node, ID_BOTTOMLEVEL) > 0;
		result |= gulpsRois.loadFromXML(node);
		return result;
	}
	
	@Override
	public boolean saveToXML(Node node) {
		saveToXML_CapillaryOnly(node);
		if (ptsTop != null)
			ptsTop.savePolyline2DToXML(node, ID_TOPLEVEL);
		if (ptsBottom != null)
			ptsBottom.savePolyline2DToXML(node, ID_BOTTOMLEVEL);
		if (ptsDerivative != null)
			ptsDerivative.savePolyline2DToXML(node, ID_DERIVATIVE);
		if (gulpsRois != null)
			gulpsRois.saveToXML(node);
        return true;
	}
		
	
	boolean loadFromXML_CapillaryOnly(Node node) {
	    final Node nodeMeta = XMLUtil.getElement(node, ID_META);
	    if (nodeMeta == null)	// nothing to load
            return true;
	    if (nodeMeta != null) {
	    	version = XMLUtil.getElementValue(nodeMeta, ID_VERSION, ID_VERSIONNUM);
	    	indexImage = XMLUtil.getElementIntValue(nodeMeta, ID_INDEXIMAGE, indexImage);
	        capillaryName = XMLUtil.getElementValue(nodeMeta, ID_NAME, capillaryName);
	        filenameTIFF = XMLUtil.getElementValue(nodeMeta, ID_NAMETIFF, filenameTIFF);
	        capillaryRoi = (ROI2DShape) loadFromXML_ROI(nodeMeta);
	        limitsOptions.loadFromXML(nodeMeta);
	        gulpsOptions.loadFromXML(nodeMeta);
	        
	    }
	    return true;
	}
	
	void saveToXML_CapillaryOnly(Node node) {
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
	        saveToXML_ROI(nodeMeta, capillaryRoi); 
	    }
	}

	private void saveToXML_ROI(Node node, ROI roi) {
		final Node nodeROI = XMLUtil.setElement(node, ID_ROI);
        if (!roi.saveToXML(nodeROI)) {
            XMLUtil.removeNode(node, nodeROI);
            System.err.println("Error: the roi " + roi.getName() + " was not correctly saved to XML !");
        }
	}
 
	private ROI loadFromXML_ROI(Node node) {
		final Node nodeROI = XMLUtil.getElement(node, ID_ROI);
        if (nodeROI != null) {
			ROI roi = ROI.createFromXML(nodeROI);
	        return roi;
        }
        return null;
	}
	
	public void cleanGulps(DetectGulps_Options options) {
		if (gulpsRois == null) {
			gulpsRois = new CapillaryGulps();
			gulpsRois.rois = new ArrayList <> ();
			return;
		}
		
		if (options.analyzePartOnly) 
			ROI2DUtilities.removeROIsWithinInterval(gulpsRois.rois, options.startPixel, options.endPixel);
		else 
			gulpsRois.rois.clear();
	}
	
	public void getGulps(int indexkymo, DetectGulps_Options options) {
		int indexpixel = 0;
		int start = 1;
		int end = ptsTop.polyline.npoints;
		if (options.analyzePartOnly) {
			start = options.startPixel;
			end = options.endPixel;
		} 
		
		ROI2DPolyLine roiTrack = new ROI2DPolyLine ();
		List<Point2D> gulpPoints = new ArrayList<>();
		for (indexpixel = start; indexpixel < end; indexpixel++) {
			int derivativevalue = (int) ptsDerivative.polyline.ypoints[indexpixel-1];
			if (derivativevalue < options.detectGulpsThreshold)
				continue;
			
			if (gulpPoints.size() > 0) {
				Point2D prevPt = gulpPoints.get(gulpPoints.size() -1);
				if ((int) prevPt.getX() <  (indexpixel-1)) {
					roiTrack.setPoints(gulpPoints);
					gulpsRois.addGulp(roiTrack, indexkymo, getLast2ofCapillaryName()+"_gulp"+String.format("%07d", indexpixel));
					roiTrack = new ROI2DPolyLine ();
					gulpPoints = new ArrayList<>();
				}
			}
			if (gulpPoints.size() == 0)
				gulpPoints.add(new Point2D.Double (indexpixel-1, ptsTop.polyline.ypoints[indexpixel-1]));
			Point2D.Double detectedPoint = new Point2D.Double (indexpixel, ptsTop.polyline.ypoints[indexpixel]);
			gulpPoints.add(detectedPoint);
		}
		
		if (gulpPoints.size() > 1) {
			roiTrack.setPoints(gulpPoints);
			gulpsRois.addGulp(roiTrack, indexkymo, getLast2ofCapillaryName()+"_gulp"+String.format("%07d", indexpixel));
		}
		if (gulpPoints.size() == 1)
			System.out.print("only_1_point_detected");
	}
	
	
}
