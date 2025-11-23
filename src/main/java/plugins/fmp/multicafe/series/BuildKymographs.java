package plugins.fmp.multicafe.series;

import java.lang.reflect.InvocationTargetException;

import javax.swing.SwingUtilities;

import icy.gui.viewer.Viewer;
import icy.sequence.Sequence;
import plugins.fmp.multicafe.experiment.Experiment;
import plugins.fmp.multicafe.service.KymographBuilder;
import plugins.fmp.multicafe.tools.Logger;

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
		exp.getSeqCamData().seq = exp.getSeqCamData()
				.initSequenceFromFirstImage(exp.getSeqCamData().getImagesList(true));
		return flag;
	}

	private void getTimeLimitsOfSequence(Experiment exp) {
		exp.getFileIntervalsFromSeqCamData();
		exp.setKymoBin_ms(options.t_Ms_BinDuration);
		if (options.isFrameFixed) {
			exp.setKymoFirst_ms(options.t_Ms_First);
			exp.setKymoLast_ms(options.t_Ms_Last);
			if (exp.getKymoLast_ms() + exp.getCamImageFirst_ms() > exp.getCamImageLast_ms())
				exp.setKymoLast_ms(exp.getCamImageLast_ms() - exp.getCamImageFirst_ms());
		} else {
			exp.setKymoFirst_ms(0);
			exp.setKymoLast_ms(exp.getCamImageLast_ms() - exp.getCamImageFirst_ms());
		}
	}

	private void closeKymoViewers() {
		closeViewer(vData);
		closeSequence(seqData);
	}

	private void openKymoViewers(Experiment exp) {
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				public void run() {
					seqData = newSequence("analyze stack starting with file " + exp.getSeqCamData().seq.getName(),
							exp.getSeqCamData().getSeqImage(0, 0));
					vData = new Viewer(seqData, true);
				}
			});
		} catch (InvocationTargetException | InterruptedException e) {
			Logger.error("BuildKymographs:openKymoViewers() Failed to open kymograph viewers", e);
		}
	}

}
