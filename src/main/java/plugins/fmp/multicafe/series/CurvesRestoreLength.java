package plugins.fmp.multicafe.series;

import plugins.fmp.multicafe.experiment.Experiment;
import plugins.fmp.multicafe.experiment.SequenceKymos;
import plugins.fmp.multicafe.experiment.capillaries.Capillary;

public class CurvesRestoreLength extends BuildSeries {
	void analyzeExperiment(Experiment exp) {
		exp.xmlLoad_MCExperiment();
		exp.loadMCCapillaries();
		if (exp.loadKymographs()) {
			SequenceKymos seqKymos = exp.getSeqKymos();
			for (int t = 0; t < seqKymos.getnTotalFrames(); t++) {
				Capillary cap = exp.getCapillaries().getCapillariesList().get(t);
				cap.restoreClippedMeasures();
			}
			exp.saveCapillaries();
		}
		exp.getSeqCamData().closeSequence();
		exp.getSeqKymos().closeSequence();
	}
}
