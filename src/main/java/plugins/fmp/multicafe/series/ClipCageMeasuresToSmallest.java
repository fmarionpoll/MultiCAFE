package plugins.fmp.multicafe.series;

import java.util.ArrayList;

import plugins.fmp.multicafe.experiment.Experiment;
import plugins.fmp.multicafe.experiment.SequenceKymos;
import plugins.fmp.multicafe.experiment.capillaries.Capillary;

public class ClipCageMeasuresToSmallest extends BuildSeries {
	void analyzeExperiment(Experiment exp) {
		exp.xmlLoad_MCExperiment();
		exp.loadMCCapillaries();
		if (exp.loadKymographs()) {
			SequenceKymos seqKymos = exp.seqKymos;
			ArrayList<Integer> listCageID = new ArrayList<Integer>(seqKymos.nTotalFrames);
			for (int t = 0; t < seqKymos.nTotalFrames; t++) {
				Capillary tcap = exp.capillaries.capillariesList.get(t);
				int tcage = tcap.capCageID;
				if (findCageID(tcage, listCageID))
					continue;
				listCageID.add(tcage);
				int minLength = findMinLength(exp, t, tcage);
				for (int tt = t; tt < seqKymos.nTotalFrames; tt++) {
					Capillary ttcap = exp.capillaries.capillariesList.get(tt);
					int ttcage = ttcap.capCageID;
					if (ttcage == tcage && ttcap.ptsTop.polylineLevel.npoints > minLength)
						ttcap.cropMeasuresToNPoints(minLength);
				}
			}
			exp.saveCapillaries();
		}
		exp.seqCamData.closeSequence();
		exp.seqKymos.closeSequence();
	}

	boolean findCageID(int cageID, ArrayList<Integer> listCageID) {
		boolean found = false;
		for (int iID : listCageID) {
			if (iID == cageID) {
				found = true;
				break;
			}
		}
		return found;
	}

	private int findMinLength(Experiment exp, int t, int tCell) {
		Capillary tcap = exp.capillaries.capillariesList.get(t);
		int minLength = tcap.ptsTop.polylineLevel.npoints;
		for (int tt = t; tt < exp.capillaries.capillariesList.size(); tt++) {
			Capillary ttcap = exp.capillaries.capillariesList.get(tt);
			int ttCell = ttcap.capCageID;
			if (ttCell == tCell) {
				int dataLength = ttcap.ptsTop.polylineLevel.npoints;
				if (dataLength < minLength)
					minLength = dataLength;
			}
		}
		return minLength;
	}
}