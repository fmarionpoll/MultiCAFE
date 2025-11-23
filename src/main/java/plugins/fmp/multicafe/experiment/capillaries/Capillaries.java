package plugins.fmp.multicafe.experiment.capillaries;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import icy.roi.ROI2D;
import icy.sequence.Sequence;
import icy.type.geom.Polygon2D;
import plugins.fmp.multicafe.experiment.KymoIntervals;
import plugins.fmp.multicafe.tools.Comparators;
import plugins.fmp.multicafe.tools.ROI2D.ROI2DAlongT;
import plugins.fmp.multicafe.tools.ROI2D.ROI2DUtilities;
import plugins.fmp.multicafe.tools.toExcel.EnumXLSExport;
import plugins.kernel.roi.roi2d.ROI2DShape;

public class Capillaries {
	public CapillariesDescription capillariesDescription = new CapillariesDescription();
	public CapillariesDescription desc_old = new CapillariesDescription();
	public ArrayList<Capillary> capillariesList = new ArrayList<Capillary>();
	private KymoIntervals capillariesListTimeIntervals = null;
	private CapillariesPersistence persistence = new CapillariesPersistence();

	// ---------------------------------

	public boolean load_Capillaries(String directory) {
		return persistence.load_Capillaries(this, directory);
	}

	public boolean save_Capillaries(String directory) {
		return persistence.save_Capillaries(this, directory);
	}

	// ---------------------------------

	public String getXMLNameToAppend() {
		return persistence.getXMLNameToAppend();
	}

	public boolean xmlSaveCapillaries_Descriptors(String csFileName) {
		return persistence.xmlSaveCapillaries_Descriptors(this, csFileName);
	}

	public boolean loadMCCapillaries_Descriptors(String csFileName) {
		return persistence.loadMCCapillaries_Descriptors(this, csFileName);
	}

	public boolean xmlLoadOldCapillaries_Only(String csFileName) {
		return persistence.xmlLoadOldCapillaries_Only(this, csFileName);
	}

	// ---------------------------------

	public void copy(Capillaries cap) {
		capillariesDescription.copy(cap.capillariesDescription);
		capillariesList.clear();
		for (Capillary ccap : cap.capillariesList) {
			if (ccap == null || ccap.getRoi() == null)
				continue;
			Capillary capi = new Capillary();
			capi.copy(ccap);
			capillariesList.add(capi);
		}
	}

	public boolean isPresent(Capillary capNew) {
		boolean flag = false;
		for (Capillary cap : capillariesList) {
			if (cap.getKymographName().contentEquals(capNew.getKymographName())) {
				flag = true;
				break;
			}
		}
		return flag;
	}

	public void mergeLists(Capillaries caplist) {
		for (Capillary capm : caplist.capillariesList) {
			if (!isPresent(capm))
				capillariesList.add(capm);
		}
	}

	public void adjustToImageWidth(int imageWidth) {
		for (Capillary cap : capillariesList)
			cap.adjustToImageWidth(imageWidth);
	}

	public void cropToImageWidth(int imageWidth) {
		for (Capillary cap : capillariesList)
			cap.cropToImageWidth(imageWidth);
	}

	public void transferDescriptionToCapillaries() {
		for (Capillary cap : capillariesList) {
			transferCapGroupCageIDToCapillary(cap);
			cap.setVolumeAndPixels(capillariesDescription.volume, capillariesDescription.pixels);
		}
	}

	private void transferCapGroupCageIDToCapillary(Capillary cap) {
		if (capillariesDescription.grouping != 2)
			return;
		String name = cap.getRoiName();
		String letter = name.substring(name.length() - 1);
		cap.capSide = letter;
		if (letter.equals("R")) {
			String nameL = name.substring(0, name.length() - 1) + "L";
			Capillary cap0 = getCapillaryFromRoiName(nameL);
			if (cap0 != null) {
//				cap.capNFlies = cap0.capNFlies;
				cap.capCageID = cap0.capCageID;
			}
		}
	}

	public Capillary getCapillaryFromRoiName(String name) {
		Capillary capFound = null;
		for (Capillary cap : capillariesList) {
			if (cap.getRoiName().equals(name)) {
				capFound = cap;
				break;
			}
		}
		return capFound;
	}

	public Capillary getCapillaryFromKymographName(String name) {
		Capillary capFound = null;
		for (Capillary cap : capillariesList) {
			if (cap.getKymographName().equals(name)) {
				capFound = cap;
				break;
			}
		}
		return capFound;
	}

	public Capillary getCapillaryFromRoiNamePrefix(String name) {
		Capillary capFound = null;
		for (Capillary cap : capillariesList) {
			if (cap.getRoiNamePrefix().equals(name)) {
				capFound = cap;
				break;
			}
		}
		return capFound;
	}

	public void updateCapillariesFromSequence(Sequence seq) {
		List<ROI2D> listROISCap = ROI2DUtilities.getROIs2DContainingString("line", seq);
		Collections.sort(listROISCap, new Comparators.ROI2D_Name_Comparator());
		for (Capillary cap : capillariesList) {
			cap.valid = false;
			String capName = Capillary.replace_LR_with_12(cap.getRoiName());
			Iterator<ROI2D> iterator = listROISCap.iterator();
			while (iterator.hasNext()) {
				ROI2D roi = iterator.next();
				String roiName = Capillary.replace_LR_with_12(roi.getName());
				if (roiName.equals(capName)) {
					cap.setRoi((ROI2DShape) roi);
					cap.valid = true;
				}
				if (cap.valid) {
					iterator.remove();
					break;
				}
			}
		}
		Iterator<Capillary> iterator = capillariesList.iterator();
		while (iterator.hasNext()) {
			Capillary cap = iterator.next();
			if (!cap.valid)
				iterator.remove();
		}
		if (listROISCap.size() > 0) {
			for (ROI2D roi : listROISCap) {
				Capillary cap = new Capillary((ROI2DShape) roi);
				if (!isPresent(cap))
					capillariesList.add(cap);
			}
		}
		Collections.sort(capillariesList);
		return;
	}

	public void transferCapillaryRoiToSequence(Sequence seq) {
		seq.removeAllROI();
		for (Capillary cap : capillariesList) {
			seq.addROI(cap.getRoi());
		}
	}

	public void initCapillariesWith10Cages(int nflies, boolean optionZeroFlyFirstLastCapillary) {
		int capArraySize = capillariesList.size();
		for (int i = 0; i < capArraySize; i++) {
			Capillary cap = capillariesList.get(i);
			cap.capNFlies = nflies;
			if (optionZeroFlyFirstLastCapillary && (i <= 1 || i >= capArraySize - 2))
				cap.capNFlies = 0;
			cap.capCageID = i / 2;
		}
	}

	public void initCapillariesWith6Cages(int nflies) {
		int capArraySize = capillariesList.size();
		for (int i = 0; i < capArraySize; i++) {
			Capillary cap = capillariesList.get(i);
			cap.capNFlies = 1;
			if (i <= 1) {
				cap.capNFlies = 0;
				cap.capCageID = 0;
			} else if (i >= capArraySize - 2) {
				cap.capNFlies = 0;
				cap.capCageID = 5;
			} else {
				cap.capNFlies = nflies;
				cap.capCageID = 1 + (i - 2) / 4;
			}
		}
	}

	// -------------------------------------------------

	public KymoIntervals getKymoIntervalsFromCapillaries() {
		if (capillariesListTimeIntervals == null) {
			capillariesListTimeIntervals = new KymoIntervals();

			for (Capillary cap : capillariesList) {
				for (ROI2DAlongT roiFK : cap.getROIsForKymo()) {
					Long[] interval = { roiFK.getStart(), (long) -1 };
					capillariesListTimeIntervals.addIfNew(interval);
				}
			}
		}
		return capillariesListTimeIntervals;
	}

	public int addKymoROI2DInterval(long start) {
		Long[] interval = { start, (long) -1 };
		int item = capillariesListTimeIntervals.addIfNew(interval);

		for (Capillary cap : capillariesList) {
			List<ROI2DAlongT> listROI2DForKymo = cap.getROIsForKymo();
			ROI2D roi = cap.getRoi();
			if (item > 0)
				roi = (ROI2D) listROI2DForKymo.get(item - 1).getRoi().getCopy();
			listROI2DForKymo.add(item, new ROI2DAlongT(start, roi));
		}
		return item;
	}

	public void deleteKymoROI2DInterval(long start) {
		capillariesListTimeIntervals.deleteIntervalStartingAt(start);
		for (Capillary cap : capillariesList)
			cap.removeROI2DIntervalStartingAt(start);
	}

	public int findKymoROI2DIntervalStart(long intervalT) {
		return capillariesListTimeIntervals.findStartItem(intervalT);
	}

	public long getKymoROI2DIntervalsStartAt(int selectedItem) {
		return capillariesListTimeIntervals.get(selectedItem)[0];
	}

	public double getScalingFactorToPhysicalUnits(EnumXLSExport xlsoption) {
		double scalingFactorToPhysicalUnits;
		switch (xlsoption) {
		case NBGULPS:
		case TTOGULP:
		case TTOGULP_LR:
		case AUTOCORREL:
		case CROSSCORREL:
		case CROSSCORREL_LR:
			scalingFactorToPhysicalUnits = 1.;
			break;
		default:
			scalingFactorToPhysicalUnits = capillariesDescription.volume / capillariesDescription.pixels;
			break;
		}
		return scalingFactorToPhysicalUnits;
	}

	public Polygon2D get2DPolygonEnclosingCapillaries() {
		Capillary cap0 = capillariesList.get(0);

		Point2D upperLeft = (Point2D) cap0.getCapillaryROIFirstPoint().clone();
		Point2D lowerLeft = (Point2D) cap0.getCapillaryROILastPoint().clone();
		Point2D upperRight = (Point2D) upperLeft.clone();
		Point2D lowerRight = (Point2D) lowerLeft.clone();

		for (Capillary cap : capillariesList) {
			Point2D capFirst = (Point2D) cap.getCapillaryROIFirstPoint();
			Point2D capLast = (Point2D) cap.getCapillaryROILastPoint();

			if (capFirst.getX() < upperLeft.getX())
				upperLeft.setLocation(capFirst.getX(), upperLeft.getY());
			if (capFirst.getY() < upperLeft.getY())
				upperLeft.setLocation(upperLeft.getX(), capFirst.getY());

			if (capLast.getX() < lowerLeft.getX())
				lowerLeft.setLocation(capLast.getX(), lowerLeft.getY());
			if (capLast.getY() > lowerLeft.getY())
				lowerLeft.setLocation(lowerLeft.getX(), capLast.getY());

			if (capFirst.getX() > upperRight.getX())
				upperRight.setLocation(capFirst.getX(), upperRight.getY());
			if (capFirst.getY() < upperRight.getY())
				upperRight.setLocation(upperRight.getX(), capFirst.getY());

			if (capLast.getX() > lowerRight.getX())
				lowerRight.setLocation(capLast.getX(), lowerRight.getY());
			if (capLast.getY() > lowerRight.getY())
				lowerRight.setLocation(lowerRight.getX(), capLast.getY());
		}

		List<Point2D> listPoints = new ArrayList<Point2D>(4);
		listPoints.add(upperLeft);
		listPoints.add(lowerLeft);
		listPoints.add(lowerRight);
		listPoints.add(upperRight);
		return new Polygon2D(listPoints);
	}

	public void deleteAllCapillaries() {
		capillariesList.clear();
	}

}