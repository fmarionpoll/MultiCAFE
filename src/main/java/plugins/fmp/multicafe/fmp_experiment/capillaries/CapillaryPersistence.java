package plugins.fmp.multicafe.fmp_experiment.capillaries;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.w3c.dom.Node;

import icy.util.XMLUtil;
import plugins.fmp.multicafe.fmp_tools.ROI2D.ROI2DUtilities;

/**
 * Handles persistence (XML loading/saving, CSV export/import) for Capillary.
 */
public class CapillaryPersistence {

	private static final String ID_META = "metaMC";
	private static final String ID_VERSION = "version";
	private static final String ID_VERSIONNUM = "1.0.0";
	private static final String ID_INDEXIMAGE = "indexImageMC";
	private static final String ID_NAME = "nameMC";
	private static final String ID_NAMETIFF = "filenameTIFF";
	
	private static final String ID_INTERVALS = "INTERVALS";
	private static final String ID_NINTERVALS = "nintervals";
	private static final String ID_INTERVAL = "interval_";
	
	private static final String ID_TOPLEVEL = "toplevel";
	private static final String ID_BOTTOMLEVEL = "bottomlevel";
	private static final String ID_DERIVATIVE = "derivative";
	private static final String ID_TOPLEVEL_CORRECTED = "toplevel_corrected";

	/**
	 * Loads capillary configuration from XML (Meta).
	 * @param node The XML node to load from.
	 * @param cap The capillary to populate.
	 * @return true if successful.
	 */
	public static boolean xmlLoadCapillary(Node node, Capillary cap) {
		final Node nodeMeta = XMLUtil.getElement(node, ID_META);
		boolean flag = (nodeMeta != null);
		if (flag) {
			cap.version = XMLUtil.getElementValue(nodeMeta, ID_VERSION, "0.0.0");
			cap.kymographIndex = XMLUtil.getElementIntValue(nodeMeta, ID_INDEXIMAGE, cap.kymographIndex);
			cap.setKymographName(XMLUtil.getElementValue(nodeMeta, ID_NAME, cap.getKymographName()));
			cap.filenameTIFF = XMLUtil.getElementValue(nodeMeta, ID_NAMETIFF, cap.filenameTIFF);
			
			// Load properties
			cap.getProperties().loadFromXml(nodeMeta);

			// Load ROI
			cap.setRoi(ROI2DUtilities.loadFromXML_ROI(nodeMeta));
			
			// Load Intervals
			xmlLoadIntervals(node, cap);
		}
		return flag;
	}

	private static boolean xmlLoadIntervals(Node node, Capillary cap) {
		cap.getRoisForKymo().clear();
		final Node nodeMeta2 = XMLUtil.getElement(node, ID_INTERVALS);
		if (nodeMeta2 == null)
			return false;
		
		int nitems = XMLUtil.getElementIntValue(nodeMeta2, ID_NINTERVALS, 0);
		if (nitems > 0) {
			for (int i = 0; i < nitems; i++) {
				Node node_i = XMLUtil.setElement(nodeMeta2, ID_INTERVAL + i);
				// Depending on how Capillary exposes internal ROI list logic
				// We need to add to cap.roisForKymo. 
				// Since we are moving logic out, we might need access methods.
				// For now assuming we can add via a getter that returns the list or specific add method.
				// Checking Capillary structure plan: it will have delegating methods.
				
				// Re-implementing logic:
				plugins.fmp.multicafe.fmp_tools.ROI2D.AlongT roiInterval = new plugins.fmp.multicafe.fmp_tools.ROI2D.AlongT();
				roiInterval.loadFromXML(node_i);
				cap.getRoisForKymo().add(roiInterval);

				if (i == 0) {
					cap.setRoi(cap.getRoisForKymo().get(0).getRoi());
				}
			}
		}
		return true;
	}

	public static boolean xmlSaveCapillary(Node node, Capillary cap) {
		final Node nodeMeta = XMLUtil.setElement(node, ID_META);
		if (nodeMeta == null)
			return false;
		
		if (cap.version == null)
			cap.version = ID_VERSIONNUM;
			
		XMLUtil.setElementValue(nodeMeta, ID_VERSION, cap.version);
		XMLUtil.setElementIntValue(nodeMeta, ID_INDEXIMAGE, cap.kymographIndex);
		XMLUtil.setElementValue(nodeMeta, ID_NAME, cap.getKymographName());
		
		if (cap.filenameTIFF != null) {
			String filename = Paths.get(cap.filenameTIFF).getFileName().toString();
			XMLUtil.setElementValue(nodeMeta, ID_NAMETIFF, filename);
		}
		
		// Save properties
		cap.getProperties().saveToXml(nodeMeta);

		ROI2DUtilities.saveToXML_ROI(nodeMeta, cap.getRoi());

		return xmlSaveIntervals(node, cap);
	}

	private static boolean xmlSaveIntervals(Node node, Capillary cap) {
		final Node nodeMeta2 = XMLUtil.setElement(node, ID_INTERVALS);
		if (nodeMeta2 == null)
			return false;
		
		List<plugins.fmp.multicafe.fmp_tools.ROI2D.AlongT> rois = cap.getRoisForKymo();
		int nitems = rois.size();
		XMLUtil.setElementIntValue(nodeMeta2, ID_NINTERVALS, nitems);
		if (nitems > 0) {
			for (int i = 0; i < nitems; i++) {
				Node node_i = XMLUtil.setElement(nodeMeta2, ID_INTERVAL + i);
				rois.get(i).saveToXML(node_i);
			}
		}
		return true;
	}

	public static boolean xmlLoadMeasures(Node node, Capillary cap) {
		String header = cap.getLast2ofCapillaryName() + "_";
		boolean result = cap.getTopLevel().loadCapillaryLimitFromXML(node, ID_TOPLEVEL, header) > 0;
		result |= cap.getBottomLevel().loadCapillaryLimitFromXML(node, ID_BOTTOMLEVEL, header) > 0;
		result |= cap.getDerivative().loadCapillaryLimitFromXML(node, ID_DERIVATIVE, header) > 0;
		result |= cap.getTopCorrected().loadCapillaryLimitFromXML(node, ID_TOPLEVEL_CORRECTED, header) > 0;
		result |= cap.getGulps().loadGulpsFromXML(node);
		return result;
	}

	// === CSV EXPORT/IMPORT ===
	
	public static String csvExportCapillarySubSectionHeader(String sep) {
		StringBuffer sbf = new StringBuffer();
		sbf.append("#" + sep + "CAPILLARIES" + sep + "describe each capillary\n");
		List<String> row2 = Arrays.asList("cap_prefix", "kymoIndex", "kymographName", "kymoFile", "cap_cage",
				"cap_nflies", "cap_volume", "cap_npixel", "cap_stim", "cap_conc", "cap_side");
		sbf.append(String.join(sep, row2));
		sbf.append("\n");
		return sbf.toString();
	}

	public static String csvExportCapillaryDescription(Capillary cap, String sep) {
		StringBuffer sbf = new StringBuffer();
		// Access properties via getter
		CapillaryProperties props = cap.getProperties();
		
		List<String> row = Arrays.asList(
				cap.getRoiNamePrefix(), 
				Integer.toString(cap.kymographIndex), 
				cap.getKymographName(), 
				cap.filenameTIFF,
				Integer.toString(props.getCageID()), 
				Integer.toString(props.getNFlies()), 
				Double.toString(props.getVolume()),
				Integer.toString(props.getPixels()), 
				props.getStimulus(), 
				props.getConcentration(), 
				props.getSide());
		sbf.append(String.join(sep, row));
		sbf.append("\n");
		return sbf.toString();
	}

	public static String csvExportMeasureSectionHeader(EnumCapillaryMeasures measureType, String sep) {
		StringBuffer sbf = new StringBuffer();
		String explanation1 = "columns=" + sep + "name" + sep + "index" + sep + "npts" + sep + "yi\n";
		String explanation2 = "columns=" + sep + "name" + sep + "index" + sep + " n_gulps(i)" + sep + " ..." + sep
				+ " gulp_i" + sep + " .npts(j)" + sep + "." + sep + "(xij" + sep + "yij))\n";
		switch (measureType) {
		case TOPRAW:
			sbf.append("#" + sep + "TOPRAW" + sep + explanation1);
			break;
		case TOPLEVEL:
			sbf.append("#" + sep + "TOPLEVEL" + sep + explanation1);
			break;
		case BOTTOMLEVEL:
			sbf.append("#" + sep + "BOTTOMLEVEL" + sep + explanation1);
			break;
		case TOPDERIVATIVE:
			sbf.append("#" + sep + "TOPDERIVATIVE" + sep + explanation1);
			break;
		case GULPS:
			sbf.append("#" + sep + "GULPS" + sep + explanation2);
			break;
		default:
			sbf.append("#" + sep + "UNDEFINED------------\n");
			break;
		}
		return sbf.toString();
	}

	public static String csvExportMeasuresOneType(Capillary cap, EnumCapillaryMeasures measureType, String sep) {
		StringBuffer sbf = new StringBuffer();
		sbf.append(cap.getRoiNamePrefix() + sep + cap.kymographIndex + sep);

		switch (measureType) {
		case TOPRAW:
			cap.getTopLevel().cvsExportYDataToRow(sbf, sep);
			break;
		case TOPLEVEL:
			if (cap.getTopCorrected() != null && cap.getTopCorrected().isThereAnyMeasuresDone())
				cap.getTopCorrected().cvsExportYDataToRow(sbf, sep);
			else
				cap.getTopLevel().cvsExportYDataToRow(sbf, sep);
			break;
		case BOTTOMLEVEL:
			cap.getBottomLevel().cvsExportYDataToRow(sbf, sep);
			break;
		case TOPDERIVATIVE:
			cap.getDerivative().cvsExportYDataToRow(sbf, sep);
			break;
		case GULPS:
			cap.getGulps().csvExportDataToRow(sbf, sep);
			break;
		default:
			break;
		}
		sbf.append("\n");
		return sbf.toString();
	}

	public static void csvImportCapillaryDescription(Capillary cap, String[] data) {
		int i = 0;
		cap.setKymographPrefix(data[i]);
		i++;
		cap.kymographIndex = Integer.valueOf(data[i]);
		i++;
		cap.setKymographName(data[i]);
		i++;
		cap.filenameTIFF = data[i];
		i++;
		CapillaryProperties props = cap.getProperties();
		props.setCageID(Integer.valueOf(data[i]));
		i++;
		props.setNFlies(Integer.valueOf(data[i]));
		i++;
		props.setVolume(Double.valueOf(data[i]));
		i++;
		props.setPixels(Integer.valueOf(data[i]));
		i++;
		props.setStimulus(data[i]);
		i++;
		props.setConcentration(data[i]);
		i++;
		props.setSide(data[i]);
	}

	public static void csvImportCapillaryData(Capillary cap, EnumCapillaryMeasures measureType, String[] data, boolean x, boolean y) {
		switch (measureType) {
		case TOPRAW:
			if (x && y)
				cap.getTopLevel().csvImportXYDataFromRow(data, 2);
			else if (!x && y)
				cap.getTopLevel().csvImportYDataFromRow(data, 2);
			break;
		case TOPLEVEL:
			if (x && y)
				cap.getTopCorrected().csvImportXYDataFromRow(data, 2);
			else if (!x && y)
				cap.getTopCorrected().csvImportYDataFromRow(data, 2);
			break;
		case BOTTOMLEVEL:
			if (x && y)
				cap.getBottomLevel().csvImportXYDataFromRow(data, 2);
			else if (!x && y)
				cap.getBottomLevel().csvImportYDataFromRow(data, 2);
			break;
		case TOPDERIVATIVE:
			if (x && y)
				cap.getDerivative().csvImportXYDataFromRow(data, 2);
			else if (!x && y)
				cap.getDerivative().csvImportYDataFromRow(data, 2);
			break;
		case GULPS:
			cap.getGulps().csvImportDataFromRow(data, 2);
			break;
		default:
			break;
		}
	}
}

