package plugins.fmp.multicafe.series;

import java.awt.Rectangle;
import java.util.ArrayList;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import icy.file.xml.XMLPersistent;
import icy.roi.ROI2D;
import icy.util.XMLUtil;
import plugins.fmp.multicafe.fmp_tools.JComponents.JComboBoxExperimentLazy;
import plugins.fmp.multicafe.fmp_tools.imageTransform.ImageTransformEnums;

public class BuildSeriesOptions implements XMLPersistent {
	public boolean isFrameFixed = false;
	public long t_Ms_First = 0;
	public long t_Ms_Last = 0;
	public long t_Ms_BinDuration = 1;

	public int diskRadius = 5;
	public boolean doRegistration = false;
	public int referenceFrame = 0;
	public boolean doCreateBinDir = false;
	public ArrayList<ROI2D> listROIStoBuildKymos = new ArrayList<ROI2D>();
	public JComboBoxExperimentLazy expList;
	public Rectangle parent0Rect = null;
	public String binSubDirectory = null;

	public boolean loopRunning = false;

	boolean detectTop = true;
	boolean detectBottom = true;

	public int detectCage = -1;
	public boolean detectL = true;
	public boolean detectR = true;
	public boolean detectAllKymos = true;
	public int kymoFirst = 0;
	public int kymoLast = 0;
	public boolean runBackwards = false;

	public boolean pass1 = true;
	public boolean directionUp1 = true;
	public int detectLevel1Threshold = 35;
	public ImageTransformEnums transform01 = ImageTransformEnums.R_RGB;

	public boolean pass2 = false;
	public boolean directionUp2 = true;
	public boolean concurrentDisplay = true;

	public int detectLevel2Threshold = 35;
	public ImageTransformEnums transform02 = ImageTransformEnums.L1DIST_TO_1RSTCOL;
	public boolean analyzePartOnly = false;
	public Rectangle searchArea = new Rectangle();

	public int spanDiffTop = 3;

	public double detectGulpsThreshold_uL = .3;
	public ImageTransformEnums transformForGulps = ImageTransformEnums.XDIFFN;
	public int spanDiff = 3;
	public boolean detectAllGulps = true;
	public boolean buildGulps = true;
	public boolean buildDerivative = true;

	public int threshold = -1;
	public int backgroundThreshold = 40;
	public int backgroundNFrames = 60;
	public int backgroundFirst = 0;

	public int thresholdDiff = 100;
	public boolean btrackWhite = false;
	public boolean blimitLow = false;
	public boolean blimitUp = false;
	public int limitLow = 0;
	public int limitUp = 1;
	public int limitRatio = 4;
	public int jitter = 10;
	public boolean forceBuildBackground = false;
	public boolean detectFlies = true;
	public int nFliesPresent = 1;

	public ImageTransformEnums transformop = ImageTransformEnums.NONE;
	public int videoChannel = 0;
	public boolean backgroundSubstraction = false;
	public int background_delta = 50;
	public int background_jitter = 1;

	// -----------------------

	public void copyTo(BuildSeriesOptions destination) {
		destination.detectTop = detectTop;
		destination.detectBottom = detectBottom;
		destination.transform01 = transform01;
		destination.directionUp1 = directionUp1;
		destination.detectLevel1Threshold = detectLevel1Threshold;
		destination.detectAllKymos = detectAllKymos;

		destination.detectGulpsThreshold_uL = detectGulpsThreshold_uL;
		destination.transformForGulps = transformForGulps;
		destination.detectAllGulps = detectAllGulps;
	}

	public void copyFrom(BuildSeriesOptions destination) {
		detectTop = destination.detectTop;
		detectBottom = destination.detectBottom;
		transform01 = destination.transform01;
		directionUp1 = destination.directionUp1;
		detectLevel1Threshold = destination.detectLevel1Threshold;
		detectAllKymos = destination.detectAllKymos;

		detectGulpsThreshold_uL = destination.detectGulpsThreshold_uL;
		transformForGulps = destination.transformForGulps;
		detectAllGulps = destination.detectAllGulps;
	}

	public void copyParameters(BuildSeriesOptions det) {
		threshold = det.threshold;
		backgroundThreshold = det.backgroundThreshold;
		thresholdDiff = det.thresholdDiff;
		btrackWhite = det.btrackWhite;
		blimitLow = det.blimitLow;
		blimitUp = det.blimitUp;
		limitLow = det.limitLow;
		limitUp = det.limitUp;
		limitRatio = det.limitRatio;
		jitter = det.jitter;
		forceBuildBackground = det.forceBuildBackground;
		detectFlies = det.detectFlies;
		transformop = det.transformop;
		videoChannel = det.videoChannel;
		backgroundSubstraction = det.backgroundSubstraction;
		isFrameFixed = det.isFrameFixed;
	}

	@Override
	public boolean loadFromXML(Node node) {
		final Node nodeMeta = XMLUtil.getElement(node, "LimitsOptions");
		if (nodeMeta != null) {
			detectTop = XMLUtil.getElementBooleanValue(nodeMeta, "detectTop", detectTop);
			detectBottom = XMLUtil.getElementBooleanValue(nodeMeta, "detectBottom", detectBottom);
			detectAllKymos = XMLUtil.getElementBooleanValue(nodeMeta, "detectAllImages", detectAllKymos);
			directionUp1 = XMLUtil.getElementBooleanValue(nodeMeta, "directionUp", directionUp1);
			kymoFirst = XMLUtil.getElementIntValue(nodeMeta, "firstImage", kymoFirst);
			detectLevel1Threshold = XMLUtil.getElementIntValue(nodeMeta, "detectLevelThreshold", detectLevel1Threshold);
			transform01 = ImageTransformEnums
					.findByText(XMLUtil.getElementValue(nodeMeta, "Transform", transform01.toString()));

			detectAllGulps = XMLUtil.getElementBooleanValue(nodeMeta, "detectAllGulps", detectAllGulps);
			buildGulps = XMLUtil.getElementBooleanValue(nodeMeta, "buildGulps", buildGulps);
			buildDerivative = XMLUtil.getElementBooleanValue(nodeMeta, "buildDerivative", buildDerivative);
			transformForGulps = ImageTransformEnums
					.findByText(XMLUtil.getElementValue(nodeMeta, "Transform", transformForGulps.toString()));
		}

		Element xmlVal = XMLUtil.getElement(node, "DetectFliesParameters");
		if (xmlVal != null) {
			threshold = XMLUtil.getElementIntValue(xmlVal, "threshold", -1);
			btrackWhite = XMLUtil.getElementBooleanValue(xmlVal, "btrackWhite", false);
			blimitLow = XMLUtil.getElementBooleanValue(xmlVal, "blimitLow", false);
			blimitUp = XMLUtil.getElementBooleanValue(xmlVal, "blimitUp", false);
			limitLow = XMLUtil.getElementIntValue(xmlVal, "limitLow", -1);
			limitUp = XMLUtil.getElementIntValue(xmlVal, "limitUp", -1);
			jitter = XMLUtil.getElementIntValue(xmlVal, "jitter", 10);
			String op1 = XMLUtil.getElementValue(xmlVal, "transformOp", null);
			transformop = ImageTransformEnums.findByText(op1);
			videoChannel = XMLUtil.getAttributeIntValue(xmlVal, "videoChannel", 0);
		}
		return true;
	}

	@Override
	public boolean saveToXML(Node node) {
		final Node nodeMeta = XMLUtil.setElement(node, "LimitsOptions");
		if (nodeMeta != null) {
			XMLUtil.setElementBooleanValue(nodeMeta, "detectTop", detectTop);
			XMLUtil.setElementBooleanValue(nodeMeta, "detectBottom", detectBottom);
			XMLUtil.setElementBooleanValue(nodeMeta, "detectAllImages", detectAllKymos);
			XMLUtil.setElementBooleanValue(nodeMeta, "directionUp", directionUp1);
			XMLUtil.setElementIntValue(nodeMeta, "firstImage", kymoFirst);
			XMLUtil.setElementIntValue(nodeMeta, "detectLevelThreshold", detectLevel1Threshold);
			XMLUtil.setElementValue(nodeMeta, "Transform", transform01.toString());

			XMLUtil.setElementBooleanValue(nodeMeta, "detectAllGulps", detectAllGulps);
			XMLUtil.setElementBooleanValue(nodeMeta, "buildGulps", buildGulps);
			XMLUtil.setElementBooleanValue(nodeMeta, "buildDerivative", buildDerivative);
			XMLUtil.setElementValue(nodeMeta, "Transform", transformForGulps.toString());
		}

		Element xmlVal = XMLUtil.addElement(node, "DetectFliesParameters");
		if (xmlVal != null) {
			XMLUtil.setElementIntValue(xmlVal, "threshold", threshold);
			XMLUtil.setElementBooleanValue(xmlVal, "btrackWhite", btrackWhite);
			XMLUtil.setElementBooleanValue(xmlVal, "blimitLow", blimitLow);
			XMLUtil.setElementBooleanValue(xmlVal, "blimitUp", blimitUp);
			XMLUtil.setElementIntValue(xmlVal, "limitLow", limitLow);
			XMLUtil.setElementIntValue(xmlVal, "limitUp", limitUp);
			XMLUtil.setElementIntValue(xmlVal, "jitter", jitter);
			if (transformop != null) {
				String transform1 = transformop.toString();
				XMLUtil.setElementValue(xmlVal, "transformOp", transform1);
			}
			XMLUtil.setAttributeIntValue(xmlVal, "videoChannel", videoChannel);
		}
		return true;
	}

}
