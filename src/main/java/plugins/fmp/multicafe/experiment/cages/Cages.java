package plugins.fmp.multicafe.experiment.cages;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import icy.roi.ROI2D;
import plugins.fmp.multicafe.experiment.Experiment;
import plugins.fmp.multicafe.experiment.SequenceCamData;
import plugins.fmp.multicafe.experiment.capillaries.Capillary;
import plugins.fmp.multicafe.tools.Comparators;
import plugins.kernel.roi.roi2d.ROI2DArea;
import plugins.kernel.roi.roi2d.ROI2DPolygon;
import plugins.kernel.roi.roi2d.ROI2DShape;

public class Cages {
	public ArrayList<Cage> cageList = new ArrayList<Cage>();
	private CagesPersistence persistence = new CagesPersistence();

	// ---------- not saved to xml:
	public long detectFirst_Ms = 0;
	public long detectLast_Ms = 0;
	public long detectBin_Ms = 60000;
	public int detect_threshold = 0;
	public int detect_nframes = 0;

	// ----------------------------

	// ---------------------------------

	public boolean load_Cages(String directory) {
		return persistence.load_Cages(this, directory);
	}

	public boolean save_Cages(String directory) {
		return persistence.save_Cages(this, directory);
	}

	// ---------------------------------

	public void clearAllMeasures(int option_detectCage) {
		for (Cage cage : cageList) {
			int cageIndex = cage.getCageID();
			if (option_detectCage < 0 || option_detectCage == cageIndex)
				cage.clearMeasures();
		}
	}

	public void clearCageList() {
		cageList.clear();
	}

	public void mergeLists(Cages cagem) {
		for (Cage inputCagem : cagem.cageList) {
			if (!isPresent(inputCagem))
				cageList.add(inputCagem);
		}
	}

	// -----------------------------------------------------

	// -------------

	public boolean xmlWriteCagesToFileNoQuestion(String tempname) {
		return persistence.xmlWriteCagesToFileNoQuestion(this, tempname);
	}

	public boolean xmlReadCagesFromFile(Experiment exp) {
		return persistence.xmlReadCagesFromFile(this, exp);
	}

	public boolean xmlReadCagesFromFileNoQuestion(String tempname, Experiment exp) {
		return persistence.xmlReadCagesFromFileNoQuestion(this, tempname, exp);
	}

	// --------------

	public void copy(Cages cagesArray) {
//		detect.copyParameters(cag.detect);	
		cageList.clear();
		for (Cage cageSource : cagesArray.cageList) {
			Cage cageDestination = new Cage();
			cageDestination.copyCage(cageSource);
			cageList.add(cageDestination);
		}
	}

	// --------------

	private boolean isPresent(Cage cageNew) {
		boolean flag = false;
		for (Cage cage : cageList) {
			if (cage.cageRoi2D.getName().contentEquals(cageNew.cageRoi2D.getName())) {
				flag = true;
				break;
			}
		}
		return flag;
	}

	private void addMissingCages(List<ROI2D> roiList) {
		for (ROI2D roi : roiList) {
			boolean found = false;
			if (roi.getName() == null)
				break;
			for (Cage cage : cageList) {
				if (cage.cageRoi2D == null)
					break;
				if (roi.getName().equals(cage.cageRoi2D.getName())) {
					found = true;
					break;
				}
			}
			if (!found) {
				Cage cage = new Cage();
				cage.cageRoi2D = roi;
				cageList.add(cage);
			}
		}
	}

	private void removeOrphanCages(List<ROI2D> roiList) {
		// remove cages with names not in the list
		Iterator<Cage> iterator = cageList.iterator();
		while (iterator.hasNext()) {
			Cage cage = iterator.next();
			boolean found = false;
			if (cage.cageRoi2D != null) {
				String cageRoiName = cage.cageRoi2D.getName();
				for (ROI2D roi : roiList) {
					if (roi.getName().equals(cageRoiName)) {
						found = true;
						break;
					}
				}
			}
			if (!found)
				iterator.remove();
		}
	}

	private List<ROI2D> getRoisWithCageName(SequenceCamData seqCamData) {
		List<ROI2D> roiList = seqCamData.seq.getROI2Ds();
		List<ROI2D> cageList = new ArrayList<ROI2D>();
		for (ROI2D roi : roiList) {
			String csName = roi.getName();
			if ((roi instanceof ROI2DPolygon) || (roi instanceof ROI2DArea)) {
//				if (( csName.contains( "cage") 
				if ((csName.length() > 4
						&& (csName.substring(0, 4).contains("cage") || csName.substring(0, 4).contains("cell"))
						|| csName.contains("Polygon2D")))
					cageList.add(roi);
			}
		}
		return cageList;
	}

	// --------------

	public void cagesToROIs(SequenceCamData seqCamData) {
		List<ROI2D> cageLimitROIList = getRoisWithCageName(seqCamData);
		seqCamData.seq.removeROIs(cageLimitROIList, false);
		for (Cage cage : cageList)
			cageLimitROIList.add(cage.cageRoi2D);
		seqCamData.seq.addROIs(cageLimitROIList, true);
	}

	public void cagesFromROIs(SequenceCamData seqCamData) {
		List<ROI2D> roiList = getRoisWithCageName(seqCamData);
		if (roiList.size() > 0) {
			Collections.sort(roiList, new Comparators.ROI2D_Name_Comparator());
			addMissingCages(roiList);
			removeOrphanCages(roiList);
			Collections.sort(cageList, new Comparators.Cage_Name_Comparator());
		}
	}

	public void setFirstAndLastCageToZeroFly() {
		for (Cage cage : cageList) {
			if (cage.cageRoi2D.getName().contains("000") || cage.cageRoi2D.getName().contains("009"))
				cage.cageNFlies = 0;
		}
	}

	public void removeAllRoiDetFromSequence(SequenceCamData seqCamData) {
		ArrayList<ROI2D> seqlist = seqCamData.seq.getROI2Ds();
		for (ROI2D roi : seqlist) {
			if (!(roi instanceof ROI2DShape))
				continue;
			if (!roi.getName().contains("det"))
				continue;
			seqCamData.seq.removeROI(roi);
		}
	}

	public void transferNFliesFromCapillariesToCageBox(List<Capillary> capList) {
		for (Cage cage : cageList) {
			int cagenb = cage.getCageID();
			for (Capillary cap : capList) {
				if (cap.capCageID == cagenb) {
					cage.cageNFlies = cap.capNFlies;
					break;
				}
			}
		}
	}

	public void transferNFliesFromCagesToCapillaries(List<Capillary> capList) {
		for (Cage cage : cageList) {
			int cageIndex = cage.getCageID();
			for (Capillary cap : capList) {
				if (cap.capCageID != cageIndex)
					continue;
				cap.capNFlies = cage.cageNFlies;
			}
		}
	}

	public void setCageNbFromName(List<Capillary> capList) {
		for (Capillary cap : capList) {
			int cageIndex = cap.getCageIndexFromRoiName();
			cap.capCageID = cageIndex;
		}
	}

	public Cage getCageFromID(int number) {
		Cage cageFound = null;
		for (Cage cage : cageList) {
			if (number == cage.getCageID()) {
				cageFound = cage;
				break;
			}
		}
		return cageFound;
	}

	// ---------------

	public List<ROI2D> getPositionsAsListOfROI2DRectanglesAtT(int t) {
		List<ROI2D> roiRectangleList = new ArrayList<ROI2D>(cageList.size());
		for (Cage cage : cageList) {
			ROI2D roiRectangle = cage.getRoiRectangleFromPositionAtT(t);
			if (roiRectangle != null)
				roiRectangleList.add(roiRectangle);
		}
		return roiRectangleList;
	}

	public void orderFlyPositions() {
		for (Cage cage : cageList)
			Collections.sort(cage.flyPositions.flyPositionList, new Comparators.XYTaValue_Tindex_Comparator());
	}

	public void initFlyPositions(int option_cagenumber) {
		int nCages = cageList.size();
		for (int i = 0; i < nCages; i++) {
			Cage cage = cageList.get(i);
			if (option_cagenumber != -1 && cage.getCageID() != option_cagenumber)
				continue;
			if (cage.cageNFlies > 0) {
				cage.flyPositions = new FlyPositions();
				cage.flyPositions.ensureCapacity(detect_nframes);
			}
		}
	}

	// ----------------

	public void initCagesTmsForFlyPositions(long[] intervalsMs) {
		for (Cage cage : cageList) {
			cage.initTmsForFlyPositions(intervalsMs);
		}
	}

	public void computeBooleanMasksForCages() {
		for (Cage cage : cageList) {
			try {
				cage.computeCageBooleanMask2D();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public int getLastIntervalFlyAlive(int cagenumber) {
		int flypos = -1;
		for (Cage cage : cageList) {
			String cageNumberString = cage.cageRoi2D.getName().substring(4);
			if (Integer.valueOf(cageNumberString) == cagenumber) {
				flypos = cage.flyPositions.getLastIntervalAlive();
				break;
			}
		}
		return flypos;
	}

	public boolean isFlyAlive(int cagenumber) {
		boolean isalive = false;
		for (Cage cage : cageList) {
			String cageNumberString = cage.cageRoi2D.getName().substring(4);
			if (Integer.valueOf(cageNumberString) == cagenumber) {
				isalive = (cage.flyPositions.getLastIntervalAlive() > 0);
				break;
			}
		}
		return isalive;
	}

	public boolean isDataAvailable(int cagenumber) {
		boolean isavailable = false;
		for (Cage cage : cageList) {
			String cageNumberString = cage.cageRoi2D.getName().substring(4);
			if (Integer.valueOf(cageNumberString) == cagenumber) {
				isavailable = true;
				break;
			}
		}
		return isavailable;
	}

	public int getHorizontalSpanOfCages() {
		int leftPixel = -1;
		int rightPixel = -1;
		for (Cage cage : cageList) {
			ROI2D roiCage = cage.cageRoi2D;
			Rectangle2D rect = roiCage.getBounds2D();
			int left = (int) rect.getX();
			int right = left + (int) rect.getWidth();
			if (leftPixel < 0 || left < leftPixel)
				leftPixel = left;
			if (right > rightPixel)
				rightPixel = right;
		}
		return rightPixel - leftPixel;
	}

}
