package plugins.fmp.multicafe2.experiment;

import java.awt.Color;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import icy.file.xml.XMLPersistent;
import icy.roi.ROI;
import icy.roi.ROI2D;
import icy.type.geom.Polyline2D;
import icy.util.XMLUtil;

import plugins.kernel.roi.roi2d.ROI2DLine;
import plugins.kernel.roi.roi2d.ROI2DPolyLine;

import plugins.fmp.multicafe2.series.BuildSeriesOptions;
import plugins.fmp.multicafe2.tools.ROI2DUtilities;
import plugins.fmp.multicafe2.tools.toExcel.EnumXLSColumnHeader;
import plugins.fmp.multicafe2.tools.toExcel.EnumXLSExportType;




public class Capillary implements XMLPersistent, Comparable <Capillary>  
{

	private ROI2D 						roi 			= null;
	private ArrayList<KymoROI2D>		roisForKymo 	= new ArrayList<KymoROI2D>();
	private String						kymographName 	= null;
	private String						roiNamePrefix	= null;
	
	public int							indexKymograph 	= -1;
	public String 						version 		= null;
	public String						filenameTIFF	= null;
	
	public ArrayList<int[]> 			cap_Integer		= null;
	
	public String 						capStimulus		= new String("..");
	public String 						capConcentration= new String("..");
	public String						capSide			= ".";
	public int							capNFlies		= 1;
	public int							capCageID		= 0;
	public double 						capVolume 		= 5.;
	public int 							capPixels 		= 5;
	public boolean						descriptionOK	= false;
	public int							versionInfos	= 0;
	
	public BuildSeriesOptions 			limitsOptions	= new BuildSeriesOptions();
	
	public  final String 				ID_TOPLEVEL 	= "toplevel";	
	public  final String 				ID_BOTTOMLEVEL 	= "bottomlevel";	
	public  final String 				ID_DERIVATIVE 	= "derivative";	
	
	public CapillaryLevel				ptsTop  		= new CapillaryLevel(ID_TOPLEVEL); 
	public CapillaryLevel				ptsBottom 		= new CapillaryLevel(ID_BOTTOMLEVEL); 
	public CapillaryLevel				ptsDerivative 	= new CapillaryLevel(ID_DERIVATIVE); 
	public CapillaryGulps 				ptsGulps 		= new CapillaryGulps(); 
	
	public boolean						valid			= true;

	private final String 				ID_META 		= "metaMC";
	private final String				ID_NFLIES		= "nflies";
	private final String				ID_CAGENB		= "cage_number";
	private final String 				ID_CAPVOLUME 	= "capillaryVolume";
	private final String 				ID_CAPPIXELS 	= "capillaryPixels";
	private final String 				ID_STIML 		= "stimulus";
	private final String 				ID_CONCL 		= "concentration";
	private final String 				ID_SIDE 		= "side";
	private final String 				ID_DESCOK 		= "descriptionOK";
	private final String				ID_VERSIONINFOS	= "versionInfos";
	
	private final String 				ID_INTERVALS 	= "INTERVALS";
	private final String				ID_NINTERVALS	= "nintervals";
	private final String 				ID_INTERVAL 	= "interval_";
	
	private final String 				ID_INDEXIMAGE 	= "indexImageMC";
	private final String 				ID_NAME 		= "nameMC";
	private final String 				ID_NAMETIFF 	= "filenameTIFF";
	private final String 				ID_VERSION		= "version"; 
	private final String 				ID_VERSIONNUM	= "1.0.0"; 
	
	// ----------------------------------------------------
	
	public Capillary(ROI2D roiCapillary) 
	{
		this.roi = roiCapillary;
		this.kymographName = replace_LR_with_12(roiCapillary.getName());
	}
	
	Capillary(String name) 
	{
		this.kymographName = replace_LR_with_12(name);
	}
	
	public Capillary() 
	{
	}

	@Override
	public int compareTo(Capillary o) 
	{
		if (o != null)
			return this.kymographName.compareTo(o.kymographName);
		return 1;
	}
	
	// ------------------------------------------
	
	public void copy(Capillary cap) 
	{
		indexKymograph 	= cap.indexKymograph;
		kymographName 	= cap.kymographName;
		version 		= cap.version;
		roi 			= (ROI2D) cap.roi.getCopy();
		filenameTIFF	= cap.filenameTIFF;
		
		capStimulus		= cap.capStimulus;
		capConcentration= cap.capConcentration;
		capSide			= cap.capSide;
		capNFlies		= cap.capNFlies;
		capCageID		= cap.capCageID;
		capVolume 		= cap.capVolume;
		capPixels 		= cap.capPixels;
		
		limitsOptions	= cap.limitsOptions;
		
		ptsGulps.copy(cap.ptsGulps);
		ptsTop.copy(cap.ptsTop); 
		ptsBottom.copy(cap.ptsBottom); 
		ptsDerivative.copy(cap.ptsDerivative); 
	}
	
	public String getKymographName() 
	{
		return kymographName;
	}
	
	public void setKymographName(String name) 
	{
		this.kymographName = name;
	}
	
	public ROI2D getRoi() {
		return roi;
	}
	
	public void setRoi(ROI2D roi) {
		this.roi = roi;
	}
	
	public void setRoiName(String name) 
	{
		roi.setName(name);
	}
	
	public String getRoiName() {
		return roi.getName();
	}
	
	public String getLast2ofCapillaryName() 
	{
		return roi.getName().substring(roi.getName().length() -2);
	}
	
	public String getRoiNamePrefix() {
		return roiNamePrefix;
	}
	
 	public String getCapillarySide() 
	{
		return roi.getName().substring(roi.getName().length() -1);
	}
	
	public static String replace_LR_with_12(String name) 
	{
		String newname = name;
		if (name .contains("R"))
			newname = name.replace("R", "2");
		else if (name.contains("L"))
			newname = name.replace("L", "1");
		return newname;
	}
	
	public int getCageIndexFromRoiName() 
	{
		String name = roi.getName();
		if (!name .contains("line"))
			return -1;
		return Integer.valueOf(name.substring(4, 5));
	}
	
	public String getSideDescriptor(EnumXLSExportType xlsExportOption) 
	{
		String value = null;
		capSide = getCapillarySide();
		switch (xlsExportOption) 
		{
		case DISTANCE:
		case ISALIVE:
			value = capSide + "(L=R)";
			break;
		case SUMGULPS_LR:
		case TOPLEVELDELTA_LR:
		case TOPLEVEL_LR:
			if (capSide.equals("L"))
				value = "sum";
			else
				value = "PI";
			break;
		case XYIMAGE:
		case XYTOPCAGE:
		case XYTIPCAPS:
			if (capSide .equals ("L"))
				value = "x";
			else
				value = "y";
			break;
		default:
			value = capSide;
			break;
		}
		return value;
	}
	
	public String getCapillaryField(EnumXLSColumnHeader fieldEnumCode)
	{
		String stringValue = null;
		switch(fieldEnumCode) 
		{
		case CAP_STIM:
			stringValue = capStimulus;
			break;
		case CAP_CONC:
			stringValue = capConcentration;
			break;
		default:
			break;
		}
		return stringValue;
	}
	
	public void setCapillaryField(EnumXLSColumnHeader fieldEnumCode, String stringValue)
	{
		switch(fieldEnumCode) 
		{
		case CAP_STIM:
			capStimulus = stringValue;
			break;
		case CAP_CONC:
			capConcentration = stringValue;
			break;
		default:
			break;
		}
	}
	
	// -----------------------------------------
	
	public boolean isThereAnyMeasuresDone(EnumXLSExportType option) 
	{
		boolean yes = false;
		switch (option) 
		{
		case DERIVEDVALUES:
			yes= (ptsDerivative != null && ptsDerivative.isThereAnyMeasuresDone());
			break;
		case SUMGULPS:
			yes= (ptsGulps!= null && ptsGulps.isThereAnyMeasuresDone());
			break;
		case BOTTOMLEVEL:
			yes= ptsBottom.isThereAnyMeasuresDone();
			break;
		case TOPLEVEL:
		default:
			yes= ptsTop.isThereAnyMeasuresDone();
			break;
		}
		return yes;
	}
		
	public ArrayList<Integer> getCapillaryMeasuresForPass1(EnumXLSExportType option, long seriesBinMs, long outputBinMs) 
	{
		ArrayList<Integer> datai = null;
		switch (option) 
		{
		case DERIVEDVALUES:
			if (ptsDerivative != null) 
				datai = ptsDerivative.getMeasures(seriesBinMs, outputBinMs);
			break;
		case SUMGULPS:
		case SUMGULPS_LR:
		case NBGULPS:
		case AMPLITUDEGULPS:
		case TTOGULP:
		case TTOGULP_LR:
		case AUTOCORREL:
		case AUTOCORREL_LR:
		case CROSSCORREL:
		case CROSSCORREL_LR:
			if (ptsGulps != null)
				datai = ptsGulps.getMeasuresFromGulps(option, ptsTop.getNPoints(), seriesBinMs, outputBinMs);
			break;
		case BOTTOMLEVEL:
			datai = ptsBottom.getMeasures(seriesBinMs, outputBinMs);
			break;
		case TOPLEVEL:
		case TOPRAW:
		case TOPLEVEL_LR:
		case TOPLEVELDELTA:
		case TOPLEVELDELTA_LR:
			default:
			datai = ptsTop.getMeasures(seriesBinMs, outputBinMs);
			break;
		}
		return datai;
	}
		
	public void cropMeasuresToNPoints (int npoints) 
	{
		if (ptsTop.polylineLevel != null)
			ptsTop.cropToNPoints(npoints);
		if (ptsBottom.polylineLevel != null)
			ptsBottom.cropToNPoints(npoints);
		if (ptsDerivative.polylineLevel != null)
			ptsDerivative.cropToNPoints(npoints);
	}
	
	public void restoreClippedMeasures () 
	{
		if (ptsTop.polylineLevel != null)
			ptsTop.restoreNPoints();
		if (ptsBottom.polylineLevel != null)
			ptsBottom.restoreNPoints();
		if (ptsDerivative.polylineLevel != null)
			ptsDerivative.restoreNPoints();
	}
	
	public void setGulpsOptions (BuildSeriesOptions options) 
	{
		limitsOptions = options;
	}
	
	public BuildSeriesOptions getGulpsOptions () 
	{
		return limitsOptions;
	}
	
	public void cleanGulps() 
	{
		if (ptsGulps == null) 
		{
			ptsGulps = new CapillaryGulps();
			ptsGulps.gulpNamePrefix = roiNamePrefix;
			ptsGulps.gulps = new ArrayList <> ();
		}
		else {
			if (limitsOptions.analyzePartOnly) {
				ptsGulps.removeGulpsWithinInterval(limitsOptions.columnFirst, limitsOptions.columnLast);
			}
			else {
				if (ptsGulps.gulps != null) ptsGulps.gulps.clear();
			}
				
		}
	}
	
	public void initGulps() 
	{
		if (ptsGulps == null) 
			ptsGulps = new CapillaryGulps();
		ptsGulps.gulpNamePrefix = roiNamePrefix;
		ptsGulps.gulps = new ArrayList <> ();
	}
	
	public void detectGulps(int indexkymo) 
	{
		int indexPixel = 0;
		int firstPixel = 1;
		if (ptsTop.polylineLevel == null)
			return;
		int lastPixel = ptsTop.polylineLevel.npoints;
		if (limitsOptions.analyzePartOnly){
			firstPixel = limitsOptions.columnFirst;
			lastPixel = limitsOptions.columnLast;
		} 
		
		int threshold = (int) ((limitsOptions.detectGulpsThresholdUL / capVolume) * capPixels);
		ArrayList<Point2D> gulpPoints = new ArrayList<Point2D>();
		int indexLastDetected = -1;
		
		for (indexPixel = firstPixel; indexPixel < lastPixel; indexPixel++) 
		{
			int derivativevalue = (int) ptsDerivative.polylineLevel.ypoints[indexPixel-1];
			if (derivativevalue >= threshold) { 
				indexLastDetected = addPointMatchingThreshold(indexPixel, gulpPoints, indexLastDetected); 
			}
		}
		addNewGulp(gulpPoints);
	}
	
	private int addPointMatchingThreshold(int indexPixel, ArrayList<Point2D> gulpPoints, int indexLastDetected) 
	{
		double delta = ptsTop.polylineLevel.ypoints[indexPixel] - ptsTop.polylineLevel.ypoints[indexPixel-1];
		if (delta < 1.) {
			addNewGulp(gulpPoints);
			gulpPoints = new ArrayList<Point2D>();
			return -1;
		}

		if (indexLastDetected < 0) {
			indexLastDetected = indexPixel -1;
			gulpPoints.add(new Point2D.Double(indexLastDetected, ptsTop.polylineLevel.ypoints[indexPixel-1]));
		}
		
		if (indexPixel == (indexLastDetected+1) ) {
			gulpPoints.add(new Point2D.Double(indexPixel, ptsTop.polylineLevel.ypoints[indexPixel]));
			return indexPixel;
		}
		else {
			addNewGulp(gulpPoints);
			gulpPoints = new ArrayList<Point2D>();
			return -1;
		}
	}
	
	private void addNewGulp(ArrayList<Point2D> gulpPoints) 
	{
		if (gulpPoints.size() <1)
			return;
		
		int npoints = gulpPoints.size();
		double[] xpoints = new double[npoints] ;
		double[] ypoints = new double[npoints] ;
		for (int i = 0; i< npoints; i++) {
			xpoints[i] = gulpPoints.get(i).getX();
			ypoints[i] = gulpPoints.get(i).getY();
		}
		Polyline2D gulpLine = new Polyline2D (xpoints, ypoints, npoints);
		ptsGulps.gulps.add(gulpLine);
	}
	
	public int getLastMeasure(EnumXLSExportType option) 
	{
		int lastMeasure = 0;
		switch (option) 
		{
		case DERIVEDVALUES:
			if (ptsDerivative != null)
				lastMeasure = ptsDerivative.getLastMeasure();
			break;
		case SUMGULPS:
			if (ptsGulps != null) 
			{
				List<Integer> datai = ptsGulps.getCumSumFromRoisArray(ptsTop.getNPoints());
				lastMeasure = datai.get(datai.size()-1);
			}
			break;
		case BOTTOMLEVEL:
			lastMeasure = ptsBottom.getLastMeasure();
			break;
		case TOPLEVEL:
		default:
			lastMeasure = ptsTop.getLastMeasure();
			break;
		}
		return lastMeasure;
	}
	
	public int getLastDeltaMeasure(EnumXLSExportType option) 
	{
		int lastMeasure = 0;
		switch (option) 
		{
		case DERIVEDVALUES:
			if (ptsDerivative != null)
				lastMeasure = ptsDerivative.getLastDeltaMeasure();
			break;
		case SUMGULPS:
			if (ptsGulps != null) {
				List<Integer> datai = ptsGulps.getCumSumFromRoisArray(ptsTop.getNPoints());
				lastMeasure = datai.get(datai.size()-1) - datai.get(datai.size()-2);
			}
			break;
		case BOTTOMLEVEL:
			lastMeasure = ptsBottom.getLastDeltaMeasure();
			break;
		case TOPLEVEL:
		default:
			lastMeasure = ptsTop.getLastDeltaMeasure();
			break;
		}
		return lastMeasure;
	}
	
	public int getT0Measure(EnumXLSExportType option) 
	{
		int t0Measure = 0;
		switch (option) 
		{
		case DERIVEDVALUES:
			if (ptsDerivative != null)
				t0Measure = ptsDerivative.getT0Measure();
			break;
		case SUMGULPS:
			if (ptsGulps != null) {
				List<Integer> datai = ptsGulps.getCumSumFromRoisArray(ptsTop.getNPoints());
				t0Measure = datai.get(0);
			}
			break;
		case BOTTOMLEVEL:
			t0Measure = ptsBottom.getT0Measure();
			break;
		case TOPLEVEL:
		default:
			t0Measure = ptsTop.getT0Measure();
			break;
		}
		return t0Measure;
	}

	public ArrayList<ROI2D> getAllGulpsAsROIs() 
	{	
		if (ptsGulps.gulps == null)
			return null; 
		
		ArrayList<ROI2D> rois = new ArrayList<ROI2D> ( ptsGulps.gulps.size());
		for (int indexGulp = 0; indexGulp < ptsGulps.gulps.size(); indexGulp++) {
			ROI2DPolyLine roi = ptsGulps.getRoiFromGulp(indexGulp, indexKymograph);
			rois.add(roi);
		}
		return rois;
	}

	public List<ROI2D> transferMeasuresToROIs() 
	{
		List<ROI2D> listrois = new ArrayList<ROI2D> ();
		if (ptsTop != null)
			ptsTop.addToROIs(listrois, indexKymograph);
		if (ptsBottom != null)
			ptsBottom.addToROIs(listrois, indexKymograph);
		if (ptsGulps != null)
			ptsGulps.addToROIs(listrois, indexKymograph);
		if (ptsDerivative != null)
			ptsDerivative.addToROIs(listrois, Color.yellow, 1., indexKymograph);
		return listrois;
	}
	
	public void transferROIsToMeasures(List<ROI> listRois) 
	{
		if (ptsTop != null)
			ptsTop.transferROIsToMeasures(listRois);
		if (ptsBottom != null)
			ptsBottom.transferROIsToMeasures(listRois);
		if (ptsGulps != null)
			ptsGulps.transferROIsToMeasures(listRois);
		if (ptsDerivative != null)
			ptsDerivative.transferROIsToMeasures(listRois);
	}

	// -------------------------------------------
	
	@Override
	public boolean loadFromXML(Node node) 
	{
		boolean result = loadFromXML_CapillaryOnly(node);	
		result |= loadFromXML_MeasuresOnly( node);
		return result;
	}
	
	@Override
	public boolean saveToXML(Node node) 
	{
		saveToXML_CapillaryOnly(node);
		saveToXML_MeasuresOnly(node); 
        return true;
	}
		
	public boolean loadFromXML_CapillaryOnly(Node node) 
	{
	    final Node nodeMeta = XMLUtil.getElement(node, ID_META);
	    boolean flag = (nodeMeta != null); 
	    if (flag) 
	    {
	    	version 		= XMLUtil.getElementValue(nodeMeta, ID_VERSION, "0.0.0");
	    	indexKymograph 	= XMLUtil.getElementIntValue(nodeMeta, ID_INDEXIMAGE, indexKymograph);
	        kymographName 	= XMLUtil.getElementValue(nodeMeta, ID_NAME, kymographName);
	        filenameTIFF 	= XMLUtil.getElementValue(nodeMeta, ID_NAMETIFF, filenameTIFF);	        
	        descriptionOK 	= XMLUtil.getElementBooleanValue(nodeMeta, ID_DESCOK, false);
	        versionInfos 	= XMLUtil.getElementIntValue(nodeMeta, ID_VERSIONINFOS, 0);
	        capNFlies 		= XMLUtil.getElementIntValue(nodeMeta, ID_NFLIES, capNFlies);
	        capCageID 		= XMLUtil.getElementIntValue(nodeMeta, ID_CAGENB, capCageID);
	        capVolume 		= XMLUtil.getElementDoubleValue(nodeMeta, ID_CAPVOLUME, Double.NaN);
			capPixels 		= XMLUtil.getElementIntValue(nodeMeta, ID_CAPPIXELS, 5);
			capStimulus 	= XMLUtil.getElementValue(nodeMeta, ID_STIML, ID_STIML);
			capConcentration= XMLUtil.getElementValue(nodeMeta, ID_CONCL, ID_CONCL);
			capSide 		= XMLUtil.getElementValue(nodeMeta, ID_SIDE, ".");
			
	        roi = ROI2DUtilities.loadFromXML_ROI(nodeMeta);
	        limitsOptions.loadFromXML(nodeMeta);
	        
	        loadFromXML_intervals(node);
	    }
	    return flag;
	}
	
	// -----------------------------------------------------------------------------

	private boolean loadFromXML_intervals(Node node) 
	{
		roisForKymo.clear();
		final Node nodeMeta2 = XMLUtil.getElement(node, ID_INTERVALS);
	    if (nodeMeta2 == null)
	    	return false;
	    int nitems = XMLUtil.getElementIntValue(nodeMeta2, ID_NINTERVALS, 0);
		if (nitems > 0) {
        	for (int i=0; i < nitems; i++) {
        		Node node_i = XMLUtil.setElement(nodeMeta2, ID_INTERVAL+i);
        		KymoROI2D roiInterval = new KymoROI2D();
        		roiInterval.loadFromXML(node_i);
        		roisForKymo.add(roiInterval);
        		
        		if (i == 0) {
        			roi = roisForKymo.get(0).getRoi();
        		}
        	}
        }
        return true;
	}
	
	public boolean loadFromXML_MeasuresOnly(Node node) 
	{
		String header = getLast2ofCapillaryName()+"_";
		boolean result = ptsTop.loadCapillaryLimitFromXML(node, ID_TOPLEVEL, header) > 0;
		result |= ptsBottom.loadCapillaryLimitFromXML(node, ID_BOTTOMLEVEL, header) > 0;
		result |= ptsDerivative.loadCapillaryLimitFromXML(node, ID_DERIVATIVE, header) > 0;
		result |= ptsGulps.loadFromXML(node);
		return result;
	}
	
	public boolean saveToXML_CapillaryOnly(Node node) 
	{
	    final Node nodeMeta = XMLUtil.setElement(node, ID_META);
	    if (nodeMeta == null)
	    	return false;
    	if (version == null)
    		version = ID_VERSIONNUM;
    	XMLUtil.setElementValue(nodeMeta, ID_VERSION, version);
        XMLUtil.setElementIntValue(nodeMeta, ID_INDEXIMAGE, indexKymograph);
        XMLUtil.setElementValue(nodeMeta, ID_NAME, kymographName);
        if (filenameTIFF != null ) {
        	String filename = Paths.get(filenameTIFF).getFileName().toString();
        	XMLUtil.setElementValue(nodeMeta, ID_NAMETIFF, filename);
        }
        XMLUtil.setElementBooleanValue(nodeMeta, ID_DESCOK, descriptionOK);
        XMLUtil.setElementIntValue(nodeMeta, ID_VERSIONINFOS, versionInfos);
        XMLUtil.setElementIntValue(nodeMeta, ID_NFLIES, capNFlies);
        XMLUtil.setElementIntValue(nodeMeta, ID_CAGENB, capCageID);
		XMLUtil.setElementDoubleValue(nodeMeta, ID_CAPVOLUME, capVolume);
		XMLUtil.setElementIntValue(nodeMeta, ID_CAPPIXELS, capPixels);
		XMLUtil.setElementValue(nodeMeta, ID_STIML, capStimulus);
		XMLUtil.setElementValue(nodeMeta, ID_SIDE, capSide);
		XMLUtil.setElementValue(nodeMeta, ID_CONCL, capConcentration);

		ROI2DUtilities.saveToXML_ROI(nodeMeta, roi); 
		
		boolean flag = saveToXML_intervals(node);
	    return flag;
	}
	
	private boolean saveToXML_intervals(Node node) 
	{
		final Node nodeMeta2 = XMLUtil.setElement(node, ID_INTERVALS);
	    if (nodeMeta2 == null)
	    	return false;
		int nitems = roisForKymo.size();
		XMLUtil.setElementIntValue(nodeMeta2, ID_NINTERVALS, nitems);
        if (nitems > 0) {
        	for (int i=0; i < nitems; i++) {
        		Node node_i = XMLUtil.setElement(nodeMeta2, ID_INTERVAL+i);
        		roisForKymo.get(i).saveToXML(node_i);
        	}
        }
        return true;
	}
	
	public void saveToXML_MeasuresOnly(Node node) 
	{
		if (ptsTop != null)
			ptsTop.saveCapillaryLimit2XML(node, ID_TOPLEVEL);
		if (ptsBottom != null)
			ptsBottom.saveCapillaryLimit2XML(node, ID_BOTTOMLEVEL);
		if (ptsDerivative != null)
			ptsDerivative.saveCapillaryLimit2XML(node, ID_DERIVATIVE);
		if (ptsGulps != null)
			ptsGulps.saveToXML(node);
	}
	 
	public boolean xmlSaveCapillary_Measures(String directory) 
	{
		if (directory == null || getRoi() == null)
			return false;
		String tempname = directory + File.separator + getKymographName()+ ".xml";

		final Document capdoc = XMLUtil.createDocument(true);
		saveToXML(XMLUtil.getRootElement(capdoc, true));
		XMLUtil.saveDocument(capdoc, tempname);
		
		return true;
	}
	
	// -------------------------------------------
	
	public Point2D getCapillaryTipWithinROI2D (ROI2D roi2D) 
	{
		Point2D pt = null;		
		if (roi instanceof ROI2DPolyLine) 
		{
			Polyline2D line = (( ROI2DPolyLine) roi).getPolyline2D();
			int last = line.npoints - 1;
			if (roi2D.contains(line.xpoints[0],  line.ypoints[0]))
				pt = new Point2D.Double(line.xpoints[0],  line.ypoints[0]);
			else if (roi2D.contains(line.xpoints[last],  line.ypoints[last])) 
				pt = new Point2D.Double(line.xpoints[last],  line.ypoints[last]);
		} 
		else if (roi instanceof ROI2DLine) 
		{
			Line2D line = (( ROI2DLine) roi).getLine();
			if (roi2D.contains(line.getP1()))
				pt = line.getP1();
			else if (roi2D.contains(line.getP2())) 
				pt = line.getP2();
		}
		return pt;
	}
	
	public Point2D getCapillaryROILowestPoint () 
	{
		Point2D pt = null;		
		if (roi instanceof ROI2DPolyLine) 
		{
			Polyline2D line = ((ROI2DPolyLine) roi).getPolyline2D();
			int last = line.npoints - 1;
			if (line.ypoints[0] > line.ypoints[last])
				pt = new Point2D.Double(line.xpoints[0],  line.ypoints[0]);
			else  
				pt = new Point2D.Double(line.xpoints[last],  line.ypoints[last]);
		} 
		else if (roi instanceof ROI2DLine) 
		{
			Line2D line = ((ROI2DLine) roi).getLine();
			if (line.getP1().getY() > line.getP2().getY())
				pt = line.getP1();
			else
				pt = line.getP2();
		}
		return pt;
	}
	
	public Point2D getCapillaryROIFirstPoint () 
	{
		Point2D pt = null;		
		if (roi instanceof ROI2DPolyLine) 
		{
			Polyline2D line = ((ROI2DPolyLine) roi).getPolyline2D();
			pt = new Point2D.Double(line.xpoints[0],  line.ypoints[0]);
		} 
		else if (roi instanceof ROI2DLine) 
		{
			Line2D line = ((ROI2DLine) roi).getLine();
			pt = line.getP1();
		}
		return pt;
	}
	
	public Point2D getCapillaryROILastPoint () 
	{
		Point2D pt = null;		
		if (roi instanceof ROI2DPolyLine) 
		{
			Polyline2D line = ((ROI2DPolyLine) roi).getPolyline2D();
			int last = line.npoints - 1;
			pt = new Point2D.Double(line.xpoints[last],  line.ypoints[last]);
		} 
		else if (roi instanceof ROI2DLine) 
		{
			Line2D line = ((ROI2DLine) roi).getLine();
			pt = line.getP2();
		}
		return pt;
	}
	
	public int getCapillaryROILength () 
	{
		Point2D pt1 = getCapillaryROIFirstPoint();
		Point2D pt2 = getCapillaryROILastPoint();
		double npixels = Math.sqrt(
				(pt2.getY() - pt1.getY()) * (pt2.getY() - pt1.getY()) 
				+ (pt2.getX() - pt1.getX()) * (pt2.getX() - pt1.getX()));
		return (int) npixels;
	}
	
	// --------------------------------------------
	
	public List<KymoROI2D> getROIsForKymo() {
		if (roisForKymo.size() < 1) 
			initROI2DForKymoList();
		return roisForKymo;
	}
	
 	public KymoROI2D getROI2DKymoAt(int i) {
		if (roisForKymo.size() < 1) 
			initROI2DForKymoList();
		return roisForKymo.get(i);
	}
 	
 	public KymoROI2D getROI2DKymoAtIntervalT(long t) {
		if (roisForKymo.size() < 1) 
			initROI2DForKymoList();
		
		KymoROI2D capRoi = null;
		for (KymoROI2D item : roisForKymo) {
			if (t < item.getStart())
				break;
			capRoi = item;
		}
		return capRoi;
	}
 	
 	public void removeROI2DIntervalStartingAt(long start) {
 		KymoROI2D itemFound = null;
 		for (KymoROI2D item : roisForKymo) {
			if (start != item.getStart())
				continue;
			itemFound = item;
		}
 		if (itemFound != null)
 			roisForKymo.remove(itemFound);
	}
	
	private void initROI2DForKymoList() { 
		roisForKymo.add(new KymoROI2D(0, roi));		
	}
	
	public void setVolumeAndPixels(double volume, int pixels) 
	{
		capVolume = volume;
		capPixels = pixels;
		descriptionOK = true;
	}
	
	// -----------------------------------------------------------------------------
	
	public String csvExportCapillaryDescriptionHeader() {
		StringBuffer sbf = new StringBuffer();
		
		sbf.append("#\tCAPILLARIES\tdescribe each capillary\n");
		List<String> row2 = Arrays.asList(
				"cap_prefix",
				"kymoIndex", 
				"kymographName", 
				"kymoFile", 
				"cap_cage",
				"cap_nflies",
				"cap_volume", 
				"cap_npixel", 
				"cap_stim", 
				"cap_conc", 
				"cap_side");
		sbf.append(String.join("\t", row2));
		sbf.append("\n");
		return sbf.toString();
	}
	
	public String csvExportCapillaryDescription() {	
		StringBuffer sbf = new StringBuffer();
		List<String> row = Arrays.asList(
				getLast2ofCapillaryName(),
				Integer.toString(indexKymograph), 
				kymographName, 
				filenameTIFF, 
				Integer.toString(capCageID),
				Integer.toString(capNFlies),
				Double.toString(capVolume), 
				Integer.toString(capPixels), 
				capStimulus, 
				capConcentration, 
				capSide);
		sbf.append(String.join("\t", row));
		sbf.append("\n");
		return sbf.toString();
	}
	
	public String csvExportCapillaryDataHeader(EnumCapillaryMeasureType measureType) {
		StringBuffer sbf = new StringBuffer();
		switch(measureType) {
			case TOPLEVEL:
				sbf.append("#\tTOPLEVEL\tliquid level at the top\n");
				sbf.append(csvExportCapillaryData(measureType, true, false));
				break;
			case BOTTOMLEVEL:
				sbf.append("#\tBOTTOMLEVEL\tliquid level at the bottom\n");
				sbf.append(csvExportCapillaryData(measureType, true, false));
				break;
			case TOPDERIVATIVE:
				sbf.append("#\tTOPDERIVATIVE\tderivative of liquid level at the top\n");
				sbf.append(csvExportCapillaryData(measureType, true, false));
				break;
			case GULPS:
				sbf.append("#\tGULPS\tgulps\n");
				break;
			default:
				sbf.append("#\tUNDEFINED\t------------\n");
				break;
		}
		return sbf.toString();
	}
	
	public String csvExportCapillaryData(EnumCapillaryMeasureType measureType, boolean exportX, boolean exportY) {
		switch(measureType) {
			case TOPLEVEL:
				return csvExportCapillaryLevel(ptsTop, exportX, exportY);
			case BOTTOMLEVEL:
				return csvExportCapillaryLevel(ptsBottom, exportX, exportY);
			case TOPDERIVATIVE:
				return csvExportCapillaryLevel(ptsDerivative, exportX, exportY);
			case GULPS:
				return csvExportCapillaryGulps(ptsGulps);
			default:
				break;
		}
		return null;
	}
	
	private String csvExportCapillaryLevel(CapillaryLevel ptsArray, boolean exportX, boolean exportY) {
		if (ptsArray == null || ptsArray.polylineLevel == null )
			return null;
		return ptsArray.csvExportData(exportX, exportY);
	}
	
	private String csvExportCapillaryGulps(CapillaryGulps capillaryGulps) {
		if (capillaryGulps == null || capillaryGulps.gulps == null)
			return null;
		return capillaryGulps.csvExportData(getLast2ofCapillaryName());
	}
	// --------------------------------------------
	
	public void csvImportCapillaryDescription(String[] data) {
		int i = 0;
		roiNamePrefix = data[i]; i++;
		indexKymograph = Integer.valueOf(data[i]); i++; 
		kymographName = data[i]; i++; 
		filenameTIFF = data[i]; i++; 
		capCageID = Integer.valueOf(data[i]); i++;
		capNFlies = Integer.valueOf(data[i]); i++;
		capVolume = Double.valueOf(data[i]); i++; 
		capPixels = Integer.valueOf(data[i]); i++; 
		capStimulus = data[i]; i++; 
		capConcentration = data[i]; i++; 
		capSide = data[i]; 
	}
		
	public void csvImportCapillaryData(EnumCapillaryMeasureType measureType, int [] dataN, int [] dataX, int [] dataY) {
		switch(measureType) {
		case TOPLEVEL:
			ptsTop.csvImportData( dataX, dataY, roiNamePrefix); 
			break;
		case BOTTOMLEVEL:
			ptsBottom.csvImportData( dataX, dataY, roiNamePrefix); 
			break;
		case TOPDERIVATIVE:
			ptsDerivative.csvImportData( dataX, dataY, roiNamePrefix); 
			break;
		case GULPS:
			ptsGulps.csvImportGulpsFrom3Rows(dataN, dataX, dataY, roiNamePrefix, indexKymograph);
			break;
		default:
			break;
		}
	}
		
}