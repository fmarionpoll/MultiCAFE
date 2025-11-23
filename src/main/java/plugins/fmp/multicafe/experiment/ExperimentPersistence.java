package plugins.fmp.multicafe.experiment;

import java.io.File;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import icy.util.XMLUtil;

public class ExperimentPersistence {

	private final static String ID_VERSION = "version";
	private final static String ID_VERSIONNUM = "1.0.0";
	private final static String ID_TIMEFIRSTIMAGE = "fileTimeImageFirstMinute";
	private final static String ID_TIMELASTIMAGE = "fileTimeImageLastMinute";

	private final static String ID_BINT0 = "indexBinT0";
	private final static String ID_TIMEFIRSTIMAGEMS = "fileTimeImageFirstMs";
	private final static String ID_TIMELASTIMAGEMS = "fileTimeImageLastMs";
	private final static String ID_FIRSTKYMOCOLMS = "firstKymoColMs";
	private final static String ID_LASTKYMOCOLMS = "lastKymoColMs";
	private final static String ID_BINKYMOCOLMS = "binKymoColMs";

	private final static String ID_IMAGESDIRECTORY = "imagesDirectory";
	private final static String ID_MCEXPERIMENT = "MCexperiment";
	public final static String ID_MCEXPERIMENT_XML = "MCexperiment.xml";

	private final static String ID_BOXID = "boxID";
	private final static String ID_EXPERIMENT = "experiment";
	private final static String ID_COMMENT1 = "comment";
	private final static String ID_COMMENT2 = "comment2";
	private final static String ID_STRAIN = "strain";
	private final static String ID_SEX = "sex";
	private final static String ID_COND1 = "cond1";
	private final static String ID_COND2 = "cond2";

	public boolean xmlLoadExperiment(Experiment exp, String csFileName) {
		final Document doc = XMLUtil.loadDocument(csFileName);
		if (doc == null)
			return false;
		Node node = XMLUtil.getElement(XMLUtil.getRootElement(doc), ID_MCEXPERIMENT);
		if (node == null)
			return false;

		String version = XMLUtil.getElementValue(node, ID_VERSION, ID_VERSIONNUM);
		if (!version.equals(ID_VERSIONNUM))
			return false;
		exp.setCamImageFirst_ms(XMLUtil.getElementLongValue(node, ID_TIMEFIRSTIMAGEMS, 0));
		exp.setCamImageLast_ms(XMLUtil.getElementLongValue(node, ID_TIMELASTIMAGEMS, 0));
		if (exp.getCamImageLast_ms() <= 0) {
			exp.setCamImageFirst_ms(XMLUtil.getElementLongValue(node, ID_TIMEFIRSTIMAGE, 0) * 60000);
			exp.setCamImageLast_ms(XMLUtil.getElementLongValue(node, ID_TIMELASTIMAGE, 0) * 60000);
		}

		exp.setBinT0(XMLUtil.getElementLongValue(node, ID_BINT0, 0));
		exp.setKymoFirst_ms(XMLUtil.getElementLongValue(node, ID_FIRSTKYMOCOLMS, -1));
		exp.setKymoLast_ms(XMLUtil.getElementLongValue(node, ID_LASTKYMOCOLMS, -1));
		exp.setKymoBin_ms(XMLUtil.getElementLongValue(node, ID_BINKYMOCOLMS, -1));

		if (exp.getKymoBin_ms() < 0)
			exp.setKymoBin_ms(60000); // Default value

		// check offsets
		if (exp.getCamImageFirst_ms() < 0)
			exp.setCamImageFirst_ms(0);
		if (exp.getCamImageLast_ms() < 0)
			exp.setCamImageLast_ms(0);
		if (exp.getKymoFirst_ms() < 0)
			exp.setKymoFirst_ms(0);
		if (exp.getKymoLast_ms() < 0)
			exp.setKymoLast_ms(0);

		if (exp.getBoxID() != null && exp.getBoxID().contentEquals("..")) {
			exp.setBoxID(XMLUtil.getElementValue(node, ID_BOXID, ".."));
			exp.setExperiment(XMLUtil.getElementValue(node, ID_EXPERIMENT, ".."));
			exp.setComment1(XMLUtil.getElementValue(node, ID_COMMENT1, ".."));
			exp.setComment2(XMLUtil.getElementValue(node, ID_COMMENT2, ".."));
			exp.setStrain(XMLUtil.getElementValue(node, ID_STRAIN, ".."));
			exp.setSex(XMLUtil.getElementValue(node, ID_SEX, ".."));
			exp.setCondition1(XMLUtil.getElementValue(node, ID_COND1, ".."));
			exp.setCondition2(XMLUtil.getElementValue(node, ID_COND2, ".."));
		}
		return true;
	}

	public boolean xmlSaveExperiment(Experiment exp, String csFileName) {
		final Document doc = XMLUtil.createDocument(true);
		if (doc != null) {
			Node xmlRoot = XMLUtil.getRootElement(doc, true);
			Node node = XMLUtil.setElement(xmlRoot, ID_MCEXPERIMENT);
			if (node == null)
				return false;

			XMLUtil.setElementValue(node, ID_VERSION, ID_VERSIONNUM);
			XMLUtil.setElementLongValue(node, ID_TIMEFIRSTIMAGEMS, exp.getCamImageFirst_ms());
			XMLUtil.setElementLongValue(node, ID_TIMELASTIMAGEMS, exp.getCamImageLast_ms());

			XMLUtil.setElementLongValue(node, ID_BINT0, exp.getBinT0());
			XMLUtil.setElementLongValue(node, ID_FIRSTKYMOCOLMS, exp.getKymoFirst_ms());
			XMLUtil.setElementLongValue(node, ID_LASTKYMOCOLMS, exp.getKymoLast_ms());
			XMLUtil.setElementLongValue(node, ID_BINKYMOCOLMS, exp.getKymoBin_ms());

			XMLUtil.setElementValue(node, ID_IMAGESDIRECTORY, exp.getImagesDirectory());

			XMLUtil.setElementValue(node, ID_BOXID, exp.getBoxID());
			XMLUtil.setElementValue(node, ID_EXPERIMENT, exp.getExperiment());
			XMLUtil.setElementValue(node, ID_COMMENT1, exp.getComment1());
			XMLUtil.setElementValue(node, ID_COMMENT2, exp.getComment2());
			XMLUtil.setElementValue(node, ID_STRAIN, exp.getStrain());
			XMLUtil.setElementValue(node, ID_SEX, exp.getSex());
			XMLUtil.setElementValue(node, ID_COND1, exp.getCondition1());
			XMLUtil.setElementValue(node, ID_COND2, exp.getCondition2());

			XMLUtil.saveDocument(doc, csFileName);
			return true;
		}
		return false;
	}

	public boolean xmlLoad_MCExperiment(Experiment exp) {
		String filename = concatenateExptDirectoryWithSubpathAndName(exp, null, ID_MCEXPERIMENT_XML);
		boolean found = xmlLoadExperiment(exp, filename);
		if (!found && exp.getSeqCamData() != null) {
			// try to load from the images directory
			String imagesDirectory = exp.getSeqCamData().getImagesDirectory();
			if (imagesDirectory != null) {
				filename = imagesDirectory + File.separator + ID_MCEXPERIMENT_XML;
				found = xmlLoadExperiment(exp, filename);
			}
		}
		return found;
	}

	public boolean xmlSave_MCExperiment(Experiment exp) {
		String filename = concatenateExptDirectoryWithSubpathAndName(exp, null, ID_MCEXPERIMENT_XML);
		return xmlSaveExperiment(exp, filename);
	}

	// ------------------------------------------------------------------

	public boolean openMeasures(Experiment exp, boolean loadCapillaries, boolean loadDrosoPositions) {
		if (exp.getSeqCamData() == null)
			exp.setSeqCamData(new SequenceCamData());
		xmlLoad_MCExperiment(exp);
		exp.getFileIntervalsFromSeqCamData();

		if (exp.getSeqKymos() == null)
			exp.setSeqKymos(new SequenceKymos());

		if (loadCapillaries) {
			exp.loadMCCapillaries_Only();
			if (!exp.getCapillaries().load_Capillaries(exp.getKymosBinFullDirectory()))
				return false;
		}
		if (loadDrosoPositions) {
			exp.loadCageMeasures();
		}
		return true;
	}

	public boolean saveExperimentMeasures(Experiment exp, String directory) {
		boolean flag = false;
		if (exp.getSeqKymos() != null) {
			flag = exp.xmlSave_MCExperiment();
			exp.saveCapillariesMeasures(directory);
		}
		return flag;
	}

	// -----------------------------------------------------------------

	private String concatenateExptDirectoryWithSubpathAndName(Experiment exp, String subpath, String name) {
		String strExperimentDirectory = exp.getExperimentDirectory();
		if (subpath != null)
			return strExperimentDirectory + File.separator + subpath + File.separator + name;
		else
			return strExperimentDirectory + File.separator + name;
	}

}
