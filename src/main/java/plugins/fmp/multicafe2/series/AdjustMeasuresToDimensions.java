package plugins.fmp.multicafe2.series;


import plugins.fmp.multicafe2.experiment.Experiment;



public class AdjustMeasuresToDimensions  extends BuildSeries 
{
	void analyzeExperiment(Experiment exp) 
	{
		exp.loadMCExperiment();
		exp.loadMCCapillaries();
		if (exp.loadKymographs()) 
		{
			exp.adjustCapillaryMeasuresDimensions();
			exp.saveCapillariesMeasures(exp.getKymosBinFullDirectory());
		}
		exp.seqCamData.closeSequence();
		exp.seqKymos.closeSequence();
	}


}
