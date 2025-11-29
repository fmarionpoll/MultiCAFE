package plugins.fmp.multicafe.series;

import plugins.fmp.multicafe.fmp_experiment.Experiment;
import plugins.fmp.multicafe.fmp_service.LevelDetector;

public class DetectLevels extends BuildSeries {
	void analyzeExperiment(Experiment exp) {
		if (loadExperimentDataToDetectLevels(exp)) {
			exp.getSeqKymos().displayViewerAtRectangle(options.parent0Rect);
			new LevelDetector().detectLevels(exp, options);
		}
		exp.closeSequences();
	}

	private boolean loadExperimentDataToDetectLevels(Experiment exp) {
		exp.xmlLoad_MCExperiment();
		exp.loadMCCapillaries();
		return exp.loadKymographs();
	}
}
