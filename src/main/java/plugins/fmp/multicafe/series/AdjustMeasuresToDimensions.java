package plugins.fmp.multicafe.series;

import plugins.fmp.multicafe.experiment.Experiment;

public class AdjustMeasuresToDimensions extends BuildSeries {
	void analyzeExperiment(Experiment exp) {
		exp.xmlLoad_MCExperiment();
		exp.loadMCCapillaries();
		if (exp.loadKymographs()) {
			exp.adjustCapillaryMeasuresDimensions();
			exp.saveCapillariesMeasures(exp.getKymosBinFullDirectory());
		}
		exp.getSeqCamData().closeSequence();
		exp.getSeqKymos().closeSequence();
	}

}
