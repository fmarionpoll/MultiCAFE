package plugins.fmp.multicafe.series;

import plugins.fmp.multicafe.fmp_experiment.Experiment;
import plugins.fmp.multicafe.fmp_experiment.capillaries.Capillary;
import plugins.fmp.multicafe.fmp_experiment.sequence.SequenceKymos;

public class CurvesRestoreLength extends BuildSeries {
	void analyzeExperiment(Experiment exp) {
		exp.xmlLoad_MCExperiment();
		exp.loadMCCapillaries();
		if (exp.loadKymographs()) {
			SequenceKymos seqKymos = exp.getSeqKymos();
			for (int t = 0; t < seqKymos.getImageLoader().getNTotalFrames(); t++) {
				Capillary cap = exp.getCapillaries().getCapillariesList().get(t);
				cap.restoreClippedMeasures();
			}
			exp.saveCapillaries();
		}
		exp.getSeqCamData().closeSequence();
		exp.getSeqKymos().closeSequence();
	}
}
