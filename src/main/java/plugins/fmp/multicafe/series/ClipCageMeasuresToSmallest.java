package plugins.fmp.multicafe.series;

import java.util.ArrayList;

import plugins.fmp.multicafe.fmp_experiment.Experiment;
import plugins.fmp.multicafe.fmp_experiment.capillaries.Capillary;
import plugins.fmp.multicafe.fmp_experiment.sequence.SequenceKymos;

public class ClipCageMeasuresToSmallest extends BuildSeries {
	void analyzeExperiment(Experiment exp) {
		exp.xmlLoad_MCExperiment();
		exp.loadMCCapillaries();
		if (exp.loadKymographs()) {
			SequenceKymos seqKymos = exp.getSeqKymos();
			ArrayList<Integer> listCageID = new ArrayList<Integer>(seqKymos.getImageLoader().getNTotalFrames());
			for (int t = 0; t < seqKymos.getImageLoader().getNTotalFrames(); t++) {
				Capillary tcap = exp.getCapillaries().getCapillariesList().get(t);
				int tcage = tcap.capCageID;
				if (findCageID(tcage, listCageID))
					continue;
				listCageID.add(tcage);
				int minLength = findMinLength(exp, t, tcage);
				for (int tt = t; tt < seqKymos.getImageLoader().getNTotalFrames(); tt++) {
					Capillary ttcap = exp.getCapillaries().getCapillariesList().get(tt);
					int ttcage = ttcap.capCageID;
					if (ttcage == tcage && ttcap.ptsTop.polylineLevel.npoints > minLength)
						ttcap.cropMeasuresToNPoints(minLength);
				}
			}
			exp.saveCapillaries();
		}
		exp.getSeqCamData().closeSequence();
		exp.getSeqKymos().closeSequence();
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
		Capillary tcap = exp.getCapillaries().getCapillariesList().get(t);
		int minLength = tcap.ptsTop.polylineLevel.npoints;
		for (int tt = t; tt < exp.getCapillaries().getCapillariesList().size(); tt++) {
			Capillary ttcap = exp.getCapillaries().getCapillariesList().get(tt);
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