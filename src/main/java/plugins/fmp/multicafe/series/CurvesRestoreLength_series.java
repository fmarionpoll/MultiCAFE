package plugins.fmp.multicafe.series;

import plugins.fmp.multicafe.experiment.Capillary;
import plugins.fmp.multicafe.experiment.Experiment;
import plugins.fmp.multicafe.experiment.SequenceKymos;

public class CurvesRestoreLength_series extends BuildSeries 
{
	void analyzeExperiment(Experiment exp) 
	{
		exp.xmlLoadMCExperiment();
		exp.xmlLoadMCcapillaries();
		if (exp.loadKymographs(false)) 
		{
			SequenceKymos seqKymos = exp.seqKymos;
			for (int t= 0; t< seqKymos.nTotalFrames; t++) 
			{
				Capillary cap = exp.capillaries.capillariesArrayList.get(t);
				cap.restoreCroppedMeasures();
			}
			exp.capillaries.xmlSaveCapillaries_Measures(exp.getKymosBinFullDirectory());
		}
		exp.seqCamData.closeSequence();
		exp.seqKymos.closeSequence();
	}
}
