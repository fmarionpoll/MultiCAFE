package plugins.fmp.multicafeSequence;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import icy.roi.ROI;
import icy.util.XMLUtil;
import plugins.fmp.multicafeTools.DetectGulps_Options;
import plugins.fmp.multicafeTools.DetectLimits_Options;
import plugins.fmp.multicafeTools.MulticafeTools;
import plugins.kernel.roi.roi2d.ROI2DShape;


public class Capillaries {
	
	public CapillariesDescription desc				= new CapillariesDescription();
	public CapillariesDescription desc_old			= new CapillariesDescription();
	public List <Capillary> capillariesArrayList	= new ArrayList <Capillary>();
	
	public DetectLimits_Options limitsOptions		= new DetectLimits_Options();
	public DetectGulps_Options 	gulpsOptions		= new DetectGulps_Options();
	
	private final static String ID_CAPILLARYTRACK = "capillaryTrack";
	private final static String ID_NCAPILLARIES = "N_capillaries";
	private final static String ID_LISTOFCAPILLARIES = "List_of_capillaries";
	private final static String ID_CAPILLARY_ = "capillary_";

	// ---------------------------------
	
	public boolean xmlWriteROIsAndData(String name, SequenceKymos seq) {
		String csFile = MulticafeTools.saveFileAs(name, seq.getDirectory(), "xml");
		csFile.toLowerCase();
		if (!csFile.contains(".xml")) {
			csFile += ".xml";
		}
		return xmlSaveCapillaries_Only(csFile);
	}
	
	public boolean xmlSaveCapillaries_Only(String csFile) {
		if (csFile != null) {
			final Document doc = XMLUtil.createDocument(true);
			if (doc != null) {
				Collections.sort(capillariesArrayList);
				desc.xmlSaveCapillaryDescription (doc);
				xmlSaveListOfCapillaries(doc);
				XMLUtil.saveDocument(doc, csFile);
				return true;
			}
		}
		return false;
	}
	
	public boolean xmlSaveCapillaries_Measures(String dataFilesPathname) {
		if (dataFilesPathname != null) {
			String directoryFull = dataFilesPathname +File.separator +"results" + File.separator;
			Collections.sort(capillariesArrayList);
			for (Capillary cap: capillariesArrayList) {
				String tempname = directoryFull+cap.getName()+ ".xml";
				final Document capdoc = XMLUtil.createDocument(true);
				cap.saveToXML(XMLUtil.getRootElement(capdoc, true));
				XMLUtil.saveDocument(capdoc, tempname);
			}
			return true;
		}
		return false;
	}
	
	public boolean xmlLoadCapillaries(String csFileName) { 
		if (csFileName != null)  {		
			final Document doc = XMLUtil.loadDocument(csFileName);
			if (doc != null) {
				desc.xmlLoadCapillaryDescription(doc);
				switch (desc.version) {
				case 2:	// current xml storage structure
					xmlLoadCapillariesv2(doc, csFileName);
					break;
				case 1: // old xml storage structure
					xmlLoadCapillariesv1(doc);
					break;
				case 0: // old-old xml storage structure
					xmlLoadCapillariesv0(doc, csFileName);
					break;
				default:
					return false;
				}		
				Collections.sort(capillariesArrayList);
				return true;
			}
		}
		return false;
	}
	
	private boolean xmlSaveListOfCapillaries(Document doc) {
		Node node = XMLUtil.getElement(XMLUtil.getRootElement(doc), ID_CAPILLARYTRACK);
		if (node == null)
			return false;
		XMLUtil.setElementIntValue(node, "version", 2);
		Node nodecaps = XMLUtil.setElement(node, ID_LISTOFCAPILLARIES);
		XMLUtil.setElementIntValue(nodecaps, ID_NCAPILLARIES, capillariesArrayList.size());
		int i= 0;
		Collections.sort(capillariesArrayList);
		for (Capillary cap: capillariesArrayList) {
			Node nodecapillary = XMLUtil.setElement(node, ID_CAPILLARY_+i);
			cap.saveToXML_CapillaryOnly(nodecapillary);
			i++;
		}
		return true;
	}
	
	private void xmlLoadCapillariesv0(Document doc, String csFileName) {
		// load xml files stored in "results"
		int t = 0;
		List<ROI> listOfCapillaryROIs = ROI.loadROIsFromXML(XMLUtil.getRootElement(doc));
		capillariesArrayList.clear();
		Path directorypath = Paths.get(csFileName).getParent();
		String directory = directorypath + File.separator + "results"+File.separator;
		// then load measures stored into individual files
		for (ROI roiCapillary: listOfCapillaryROIs) {
			Capillary cap = new Capillary((ROI2DShape) roiCapillary);
			capillariesArrayList.add(cap);
			String csFile = directory + roiCapillary.getName() + ".xml";
			cap.indexImage = t;
			t++;
			final Document dockymo = XMLUtil.loadDocument(csFile);
			if (dockymo != null) {
				NodeList nodeROISingle = dockymo.getElementsByTagName("roi");					
				if (nodeROISingle.getLength() > 0) {	
					List<ROI> rois = new ArrayList<ROI>();
	                for (int i=0; i< nodeROISingle.getLength(); i++) {
	                	Node element = nodeROISingle.item(i);
	                    ROI roi_i = ROI.createFromXML(element);
	                    if (roi_i != null)
	                        rois.add(roi_i);
	                }
					cap.transferROIsToMeasures(rois);
				}
			}
		}
	}

	private boolean xmlLoadCapillariesv1(Document doc) {
		Node node = XMLUtil.getElement(XMLUtil.getRootElement(doc), ID_CAPILLARYTRACK);
		if (node == null)
			return false;
		Node nodecaps = XMLUtil.getElement(node, ID_LISTOFCAPILLARIES);
		int nitems = XMLUtil.getElementIntValue(nodecaps, ID_NCAPILLARIES, 0);
		capillariesArrayList = new ArrayList<Capillary> (nitems);
		for (int i= 0; i< nitems; i++) {
			Node nodecapillary = XMLUtil.getElement(node, ID_CAPILLARY_+i);
			Capillary cap = new Capillary();
			cap.loadFromXML(nodecapillary);
			capillariesArrayList.add(cap);
		}
		return true;
	}
	
	private void xmlLoadCapillariesv2(Document doc, String csFileName) {
		xmlLoadCapillariesv1(doc);
		Path directorypath = Paths.get(csFileName).getParent();
		String directory = directorypath + File.separator;
		if (!directory .contains("results"))
				directory = directory + "results"+File.separator;
		for (Capillary cap: capillariesArrayList) {
			String csFile = directory + cap.getName() + ".xml";
			final Document capdoc = XMLUtil.loadDocument(csFile);
			Node node = XMLUtil.getRootElement(capdoc, true);
			cap.loadFromXML(node);
		}
	}
	
	public void copy (Capillaries cap) {
		desc.copy(cap.desc);
		capillariesArrayList.clear();
		for (Capillary ccap: cap.capillariesArrayList) {
			Capillary capi = new Capillary();
			capi.copy(ccap);
			capillariesArrayList.add(capi);
		}
	}
	
	public boolean isChanged (Capillaries cap) {	
		return desc.isChanged(cap.desc);
	}

}
