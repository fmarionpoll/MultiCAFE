package plugins.fmp.multicafe.fmp_series;

import icy.sequence.Sequence;
import plugins.fmp.multicafe.fmp_experiment.Experiment;
import plugins.fmp.multicafe.fmp_experiment.sequence.SequenceCamData;
import plugins.fmp.multicafe.fmp_service.KymographBuilder;

public class BuildKymographs extends BuildSeries {
	public Sequence seqData = new Sequence();
//	private Viewer vData = null;

	void analyzeExperiment(Experiment exp) {
		try {
			loadExperimentDataToBuildKymos(exp);
			getTimeLimitsOfSequence(exp);

			KymographBuilder builder = new KymographBuilder();
			if (builder.buildKymograph(exp, options))
				builder.saveComputation(exp, options);
		} finally {
			// Close sequences to free memory after processing
			if (exp.getSeqCamData() != null) {
				exp.getSeqCamData().closeSequence();
			}
		}
	}

	private boolean loadExperimentDataToBuildKymos(Experiment exp) {
		boolean flag = exp.loadMCCapillaries_Only();
		// exp.getCapillaries().transferCapillaryRoiToSequence(exp.getSeqCamData().getSequence());
		SequenceCamData seqData = exp.getSeqCamData();

		seqData.setSequence(seqData.getImageLoader().initSequenceFromFirstImage(seqData.getImagesList(true)));
		exp.build_MsTimeIntervalsArray_From_SeqCamData_FileNamesList(exp.getCamImageFirst_ms());
		return flag;
	}

	protected void getTimeLimitsOfSequence(Experiment exp) {
		exp.getFileIntervalsFromSeqCamData();
		exp.setKymoBin_ms(options.t_Ms_BinDuration);
		if (options.isFrameFixed) {
			exp.setKymoFirst_ms(options.t_Ms_First);
			exp.setKymoLast_ms(options.t_Ms_Last);
			if (exp.getKymoLast_ms() > exp.getCamImageLast_ms())
				exp.setKymoLast_ms(exp.getCamImageLast_ms());
		} else {
			exp.setKymoFirst_ms(0);
			exp.setKymoLast_ms(exp.getCamImageLast_ms() - exp.getCamImageFirst_ms());
		}
	}

}
