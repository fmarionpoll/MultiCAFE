package plugins.fmp.multicafe.experiment.sequence;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import icy.image.IcyBufferedImage;
import icy.roi.ROI;
import icy.roi.ROI2D;
import icy.type.geom.Polyline2D;
import plugins.fmp.multicafe.experiment1.capillaries.Capillaries;
import plugins.fmp.multicafe.experiment1.capillaries.Capillary;
import plugins.fmp.multicafe.experiment1.capillaries.CapillaryMeasure;
import plugins.fmp.multicafe.tools.Comparators;
import plugins.fmp.multicafe.tools.ROI2D.ROI2DUtilities;
import plugins.kernel.roi.roi2d.ROI2DPolyLine;

public class SequenceKymos extends SequenceCamData {
	private boolean isRunning_loadImages = false;
	private int imageWidthMax = 0;
	private int imageHeightMax = 0;

	// -----------------------------------------------------

	public SequenceKymos() {
		super();
		setStatus(EnumStatus.KYMOGRAPH);
	}

	public SequenceKymos(String name, IcyBufferedImage image) {
		super(name, image);
		setStatus(EnumStatus.KYMOGRAPH);
	}

	public SequenceKymos(List<String> listNames) {
		super();
		setImagesList(new plugins.fmp.multicafe.service.KymographService().convertLinexLRFileNames(listNames));
		setStatus(EnumStatus.KYMOGRAPH);
	}

	public boolean isRunning_loadImages() {
		return isRunning_loadImages;
	}

	public void setRunning_loadImages(boolean isRunning_loadImages) {
		this.isRunning_loadImages = isRunning_loadImages;
	}

	public int getImageWidthMax() {
		return imageWidthMax;
	}

	public void setImageWidthMax(int imageWidthMax) {
		this.imageWidthMax = imageWidthMax;
	}

	public int getImageHeightMax() {
		return imageHeightMax;
	}

	public void setImageHeightMax(int imageHeightMax) {
		this.imageHeightMax = imageHeightMax;
	}

	// ----------------------------

	public void validateRoisAtT(int t) {
		List<ROI2D> listRois = getSequence().getROI2Ds();
		int width = getSequence().getWidth();
		for (ROI2D roi : listRois) {
			if (!(roi instanceof ROI2DPolyLine))
				continue;
			if (roi.getT() == -1)
				roi.setT(t);
			if (roi.getT() != t)
				continue;
			// interpolate missing points if necessary
			if (roi.getName().contains("level") || roi.getName().contains("gulp")) {
				ROI2DUtilities.interpolateMissingPointsAlongXAxis((ROI2DPolyLine) roi, width);
				continue;
			}
			if (roi.getName().contains("deriv"))
				continue;
			// if gulp not found - add an index to it
			ROI2DPolyLine roiLine = (ROI2DPolyLine) roi;
			Polyline2D line = roiLine.getPolyline2D();
			roi.setName("gulp" + String.format("%07d", (int) line.xpoints[0]));
			roi.setColor(Color.red);
		}
		Collections.sort(listRois, new Comparators.ROI2D_Name_Comparator());
	}

	public void removeROIsPolylineAtT(int t) {
		List<ROI2D> listRois = getSequence().getROI2Ds();
		for (ROI2D roi : listRois) {
			if (!(roi instanceof ROI2DPolyLine))
				continue;
			if (roi.getT() == t)
				getSequence().removeROI(roi);
		}
	}

	public void updateROIFromCapillaryMeasure(Capillary cap, CapillaryMeasure caplimits) {
		int t = cap.kymographIndex;
		List<ROI2D> listRois = getSequence().getROI2Ds();
		for (ROI2D roi : listRois) {
			if (!(roi instanceof ROI2DPolyLine))
				continue;
			if (roi.getT() != t)
				continue;
			if (!roi.getName().contains(caplimits.capName))
				continue;

			((ROI2DPolyLine) roi).setPolyline2D(caplimits.polylineLevel);
			roi.setName(caplimits.capName);
			getSequence().roiChanged(roi);
			break;
		}
	}

	public void validateRois() {
		List<ROI2D> listRois = getSequence().getROI2Ds();
		int width = getSequence().getWidth();
		for (ROI2D roi : listRois) {
			if (!(roi instanceof ROI2DPolyLine))
				continue;
			// interpolate missing points if necessary
			if (roi.getName().contains("level") || roi.getName().contains("gulp")) {
				ROI2DUtilities.interpolateMissingPointsAlongXAxis((ROI2DPolyLine) roi, width);
				continue;
			}
			if (roi.getName().contains("derivative"))
				continue;
			// if gulp not found - add an index to it
			ROI2DPolyLine roiLine = (ROI2DPolyLine) roi;
			Polyline2D line = roiLine.getPolyline2D();
			roi.setName("gulp" + String.format("%07d", (int) line.xpoints[0]));
			roi.setColor(Color.red);
		}
		Collections.sort(listRois, new Comparators.ROI2D_Name_Comparator());
	}

	public boolean transferKymosRoisToCapillaries_Measures(Capillaries capillaries) {
		List<ROI> allRois = getSequence().getROIs();
		if (allRois.size() < 1)
			return false;

		for (int kymo = 0; kymo < getSequence().getSizeT(); kymo++) {
			List<ROI> roisAtT = new ArrayList<ROI>();
			for (ROI roi : allRois) {
				if (roi instanceof ROI2D && ((ROI2D) roi).getT() == kymo)
					roisAtT.add(roi);
			}
			if (capillaries.getCapillariesList().size() <= kymo)
				capillaries.getCapillariesList().add(new Capillary());
			Capillary cap = capillaries.getCapillariesList().get(kymo);
			cap.filenameTIFF = getFileNameFromImageList(kymo);
			cap.kymographIndex = kymo;
			cap.transferROIsToMeasures(roisAtT);
		}
		return true;
	}

	public boolean transferKymosRoi_atT_ToCapillaries_Measures(int t, Capillaries capillaries) {
		List<ROI> allRois = getSequence().getROIs();
		if (allRois.size() < 1)
			return false;

		List<ROI> roisAtT = new ArrayList<ROI>();
		for (ROI roi : allRois) {
			if (roi instanceof ROI2D && ((ROI2D) roi).getT() == t)
				roisAtT.add(roi);
		}
		if (capillaries.getCapillariesList().size() <= t)
			capillaries.getCapillariesList().add(new Capillary());
		Capillary cap = capillaries.getCapillariesList().get(t);
		cap.filenameTIFF = getFileNameFromImageList(t);
		cap.kymographIndex = t;
		cap.transferROIsToMeasures(roisAtT);

		return true;
	}

	public void transferCapillariesMeasuresToKymos(Capillaries capillaries) {
		List<ROI2D> seqRoisList = getSequence().getROI2Ds(false);
		ROI2DUtilities.removeROIsMissingChar(seqRoisList, '_');

		List<ROI2D> newRoisList = new ArrayList<ROI2D>();
		int ncapillaries = capillaries.getCapillariesList().size();
		for (int i = 0; i < ncapillaries; i++) {
			List<ROI2D> listOfRois = capillaries.getCapillariesList().get(i).transferMeasuresToROIs();
			newRoisList.addAll(listOfRois);
		}
		ROI2DUtilities.mergeROIsListNoDuplicate(seqRoisList, newRoisList, getSequence());
		getSequence().removeAllROI();
		getSequence().addROIs(seqRoisList, false);
	}

	public void saveKymosCurvesToCapillariesMeasures(Experiment exp) {
		exp.getSeqKymos().validateRois();
		exp.getSeqKymos().transferKymosRoisToCapillaries_Measures(exp.getCapillaries());
		exp.saveCapillaries();
	}

	// ----------------------------

	public List<ImageFileDescriptor> loadListOfPotentialKymographsFromCapillaries(String dir, Capillaries capillaries) {
		return new plugins.fmp.multicafe.service.KymographService().loadListOfPotentialKymographsFromCapillaries(dir,
				capillaries);
	}

	// -------------------------

	public boolean loadImagesFromList(List<ImageFileDescriptor> kymoImagesDesc, boolean adjustImagesSize) {
		return new plugins.fmp.multicafe.service.KymographService().loadImagesFromList(this, kymoImagesDesc,
				adjustImagesSize);
	}

	// ----------------------------

}
