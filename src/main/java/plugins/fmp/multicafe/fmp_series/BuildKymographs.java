package plugins.fmp.multicafe.fmp_series;

import java.lang.reflect.InvocationTargetException;

import javax.swing.SwingUtilities;

import icy.gui.viewer.Viewer;
import icy.sequence.Sequence;
import plugins.fmp.multicafe.fmp_experiment.Experiment;
import plugins.fmp.multicafe.fmp_experiment.sequence.SequenceCamData;
import plugins.fmp.multicafe.fmp_service.KymographBuilder;
import plugins.fmp.multicafe.fmp_tools.Logger;

public class BuildKymographs extends BuildSeries {
	public Sequence seqData = new Sequence();
	private Viewer vData = null;

	void analyzeExperiment(Experiment exp) {
		loadExperimentDataToBuildKymos(exp);

		openKymoViewers(exp);
		getTimeLimitsOfSequence(exp);

		KymographBuilder builder = new KymographBuilder();
		if (builder.buildKymograph(exp, options))
			builder.saveComputation(exp, options);

		closeKymoViewers();
		exp.getSeqKymos().closeSequence();
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
		System.out.println("first:" + exp.getCamImageFirst_ms() + " last:" + exp.getCamImageLast_ms());

		exp.setKymoBin_ms(options.t_Ms_BinDuration);
		if (options.isFrameFixed) {
			exp.setKymoFirst_ms(options.t_Ms_First);
			exp.setKymoLast_ms(options.t_Ms_Last);
//			if (exp.getKymoLast_ms() + exp.getCamImageFirst_ms() > exp.getCamImageLast_ms())
//				exp.setKymoLast_ms(exp.getCamImageLast_ms() - exp.getCamImageFirst_ms());
			if (exp.getKymoLast_ms() > exp.getCamImageLast_ms())
				exp.setKymoLast_ms(exp.getCamImageLast_ms());
		} else {
			exp.setKymoFirst_ms(0);
			exp.setKymoLast_ms(exp.getCamImageLast_ms() - exp.getCamImageFirst_ms());
		}

		int kymoImageWidth = (int) ((exp.getKymoLast_ms() - exp.getKymoFirst_ms()) / exp.getKymoBin_ms() + 1);
		System.out.println("kymoImageWidth =" + kymoImageWidth);
	}

	private void closeKymoViewers() {
		closeViewer(vData);
		closeSequence(seqData);
	}

	private void openKymoViewers(Experiment exp) {
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				public void run() {
					seqData = newSequence(
							"analyze stack starting with file " + exp.getSeqCamData().getSequence().getName(),
							exp.getSeqCamData().getSeqImage(0, 0));
					vData = new Viewer(seqData, true);
				}
			});
		} catch (InvocationTargetException | InterruptedException e) {
			Logger.error("BuildKymographs:openKymoViewers() Failed to open kymograph viewers", e);
		}
	}

}
