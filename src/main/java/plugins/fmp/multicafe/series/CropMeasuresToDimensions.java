package plugins.fmp.multicafe.series;

import plugins.fmp.multicafe.experiment.Experiment;

public class CropMeasuresToDimensions  extends BuildSeries  {
	void analyzeExperiment(Experiment exp) 
	{
		exp.xmlLoad_MCExperiment();
		exp.loadMCCapillaries();
		if (exp.loadKymographs()) 
		{
			exp.cropCapillaryMeasuresDimensions();
			exp.saveCapillariesMeasures(exp.getKymosBinFullDirectory());
		}
		exp.seqCamData.closeSequence();
		exp.seqKymos.closeSequence();
	}
}
