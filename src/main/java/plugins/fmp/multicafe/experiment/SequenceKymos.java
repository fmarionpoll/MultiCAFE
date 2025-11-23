package plugins.fmp.multicafe.experiment;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import icy.image.IcyBufferedImage;
import icy.roi.ROI;
import icy.roi.ROI2D;
import icy.type.geom.Polyline2D;
import plugins.fmp.multicafe.experiment.capillaries.Capillaries;
import plugins.fmp.multicafe.experiment.capillaries.Capillary;
import plugins.fmp.multicafe.experiment.capillaries.CapillaryMeasure;
import plugins.fmp.multicafe.tools.Comparators;
import plugins.fmp.multicafe.tools.ROI2D.ROI2DUtilities;
import plugins.kernel.roi.roi2d.ROI2DPolyLine;

public class SequenceKymos extends SequenceCamData {
	public boolean isRunning_loadImages = false;
	public int imageWidthMax = 0;
	public int imageHeightMax = 0;

	// -----------------------------------------------------

	public SequenceKymos() {
		super();
		status = EnumStatus.KYMOGRAPH;
	}

	public SequenceKymos(String name, IcyBufferedImage image) {
		super(name, image);
		status = EnumStatus.KYMOGRAPH;
	}

	public SequenceKymos(List<String> listNames) {
		super();
		setImagesList(new plugins.fmp.multicafe.service.KymographService().convertLinexLRFileNames(listNames));
		status = EnumStatus.KYMOGRAPH;
	}

	// ----------------------------

	public void validateRoisAtT(int t) {
		List<ROI2D> listRois = seq.getROI2Ds();
		int width = seq.getWidth();
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
		List<ROI2D> listRois = seq.getROI2Ds();
		for (ROI2D roi : listRois) {
			if (!(roi instanceof ROI2DPolyLine))
				continue;
			if (roi.getT() == t)
				seq.removeROI(roi);
		}
	}

	public void updateROIFromCapillaryMeasure(Capillary cap, CapillaryMeasure caplimits) {
		int t = cap.kymographIndex;
		List<ROI2D> listRois = seq.getROI2Ds();
		for (ROI2D roi : listRois) {
			if (!(roi instanceof ROI2DPolyLine))
				continue;
			if (roi.getT() != t)
				continue;
			if (!roi.getName().contains(caplimits.capName))
				continue;

			((ROI2DPolyLine) roi).setPolyline2D(caplimits.polylineLevel);
			roi.setName(caplimits.capName);
			seq.roiChanged(roi);
			break;
		}
	}

	public void validateRois() {
		List<ROI2D> listRois = seq.getROI2Ds();
		int width = seq.getWidth();
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
		List<ROI> allRois = seq.getROIs();
		if (allRois.size() < 1)
			return false;

		for (int kymo = 0; kymo < seq.getSizeT(); kymo++) {
			List<ROI> roisAtT = new ArrayList<ROI>();
			for (ROI roi : allRois) {
				if (roi instanceof ROI2D && ((ROI2D) roi).getT() == kymo)
					roisAtT.add(roi);
			}
			if (capillaries.capillariesList.size() <= kymo)
				capillaries.capillariesList.add(new Capillary());
			Capillary cap = capillaries.capillariesList.get(kymo);
			cap.filenameTIFF = getFileNameFromImageList(kymo);
			cap.kymographIndex = kymo;
			cap.transferROIsToMeasures(roisAtT);
		}
		return true;
	}

	public boolean transferKymosRoi_atT_ToCapillaries_Measures(int t, Capillaries capillaries) {
		List<ROI> allRois = seq.getROIs();
		if (allRois.size() < 1)
			return false;

		List<ROI> roisAtT = new ArrayList<ROI>();
		for (ROI roi : allRois) {
			if (roi instanceof ROI2D && ((ROI2D) roi).getT() == t)
				roisAtT.add(roi);
		}
		if (capillaries.capillariesList.size() <= t)
			capillaries.capillariesList.add(new Capillary());
		Capillary cap = capillaries.capillariesList.get(t);
		cap.filenameTIFF = getFileNameFromImageList(t);
		cap.kymographIndex = t;
		cap.transferROIsToMeasures(roisAtT);

		return true;
	}

	public void transferCapillariesMeasuresToKymos(Capillaries capillaries) {
		List<ROI2D> seqRoisList = seq.getROI2Ds(false);
		ROI2DUtilities.removeROIsMissingChar(seqRoisList, '_');

		List<ROI2D> newRoisList = new ArrayList<ROI2D>();
		int ncapillaries = capillaries.capillariesList.size();
		for (int i = 0; i < ncapillaries; i++) {
			List<ROI2D> listOfRois = capillaries.capillariesList.get(i).transferMeasuresToROIs();
			newRoisList.addAll(listOfRois);
		}
		ROI2DUtilities.mergeROIsListNoDuplicate(seqRoisList, newRoisList, seq);
		seq.removeAllROI();
		seq.addROIs(seqRoisList, false);
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
