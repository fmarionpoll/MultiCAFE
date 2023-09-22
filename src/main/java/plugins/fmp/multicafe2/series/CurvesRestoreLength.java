package plugins.fmp.multicafe2.series;

import plugins.fmp.multicafe2.experiment.Capillary;
import plugins.fmp.multicafe2.experiment.Experiment;
import plugins.fmp.multicafe2.experiment.SequenceKymos;

public class CurvesRestoreLength extends BuildSeries 
{
	void analyzeExperiment(Experiment exp) 
	{
		exp.loadMCExperiment();
		exp.loadMCCapillaries();
		if (exp.loadKymographs()) 
		{
			SequenceKymos seqKymos = exp.seqKymos;
			for (int t= 0; t< seqKymos.nTotalFrames; t++) 
			{
				Capillary cap = exp.capillaries.capillariesList.get(t);
				cap.restoreClippedMeasures();
			}
			exp.saveCapillariesMeasures();
		}
		exp.seqCamData.closeSequence();
		exp.seqKymos.closeSequence();
	}
}
