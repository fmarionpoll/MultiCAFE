package plugins.fmp.multicafe2.series;

import plugins.fmp.multicafe2.experiment.Experiment;

public class CropMeasuresToDimensions  extends BuildSeries  {
	void analyzeExperiment(Experiment exp) 
	{
		exp.xmlLoadMCExperiment();
		exp.loadMCCapillaries();
		if (exp.loadKymographs()) 
		{
			exp.cropCapillaryMeasuresDimensions();
			exp.saveExperimentMeasures(exp.getKymosBinFullDirectory());
		}
		exp.seqCamData.closeSequence();
		exp.seqKymos.closeSequence();
	}
}
