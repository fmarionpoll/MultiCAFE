package plugins.fmp.multicafe.series;

import plugins.fmp.multicafe.experiment.Capillary;
import plugins.fmp.multicafe.experiment.Experiment;
import plugins.fmp.multicafe.experiment.SequenceKymos;

public class CurvesRestoreLength extends BuildSeries {
	void analyzeExperiment(Experiment exp) {
		exp.xmlLoad_MCExperiment();
		exp.loadMCCapillaries();
		if (exp.loadKymographs()) {
			SequenceKymos seqKymos = exp.seqKymos;
			for (int t = 0; t < seqKymos.nTotalFrames; t++) {
				Capillary cap = exp.capillaries.capillariesList.get(t);
				cap.restoreClippedMeasures();
			}
			exp.saveCapillaries();
		}
		exp.seqCamData.closeSequence();
		exp.seqKymos.closeSequence();
	}
}
