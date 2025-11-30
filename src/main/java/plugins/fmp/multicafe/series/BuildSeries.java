package plugins.fmp.multicafe.series;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import icy.gui.frame.progress.ProgressFrame;
import icy.gui.viewer.Viewer;
import icy.image.IcyBufferedImage;
import icy.sequence.Sequence;
import icy.system.thread.Processor;
import plugins.fmp.multicafe.fmp_experiment.Experiment;
import plugins.fmp.multicafe.tools1.Logger;
import plugins.fmp.multicafe.tools1.JComponents.ExperimentsJComboBox;

public abstract class BuildSeries extends SwingWorker<Integer, Integer> {

	public BuildSeriesOptions options = new BuildSeriesOptions();
	public boolean stopFlag = false;
	public boolean threadRunning = false;
	int nframescomputed = 0;
	int nframestotal = 0;

	int selectedExperimentIndex = -1;
	public final String THREAD_ENDED = "thread_ended";
	public final String THREAD_DONE = "thread_done";

	@Override
	protected Integer doInBackground() throws Exception {
		Logger.info("BuildSeries:doInBackground loop over experiments");
		threadRunning = true;
		int nbiterations = 0;
		ExperimentsJComboBox expList = options.expList;
		ProgressFrame progress = new ProgressFrame("Analyze series");
		selectedExperimentIndex = expList.getSelectedIndex();

		selectList(expList, -1);

		for (int index = expList.index0; index <= expList.index1; index++, nbiterations++) {
			if (stopFlag)
				break;
			long startTimeInNs = System.nanoTime();

			Experiment exp = expList.getItemAt(index);

			progress.setMessage("Processing file: " + (index + 1) + "//" + (expList.index1 + 1));
			Logger.info("BuildSeries:doInBackground " + (index + 1) + ": " + exp.getExperimentDirectory());
			exp.setBinSubDirectory(options.binSubDirectory);
			boolean flag = exp.createDirectoryIfDoesNotExist(exp.getKymosBinFullDirectory());
			if (flag) {
				analyzeExperiment(exp);
				long endTime2InNs = System.nanoTime();
				Logger.debug("BuildSeries:doInBackground process ended - duration: "
						+ ((endTime2InNs - startTimeInNs) / 1000000000f) + " s");
			} else {
				Logger.warn("BuildSeries:doInBackground process aborted - subdirectory not created: "
						+ exp.getKymosBinFullDirectory());
			}
		}
		progress.close();
		threadRunning = false;

		selectList(expList, selectedExperimentIndex);
		return nbiterations;
	}

	private void selectList(ExperimentsJComboBox expList, int index) {
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				public void run() {
					expList.setSelectedIndex(index);
				}
			});
		} catch (InvocationTargetException | InterruptedException e) {
			Logger.error("BuildSeries:selectList() Failed to select experiment in list", e);
		}
	}

	@Override
	protected void done() {
		int statusMsg = 0;
		try {
			statusMsg = super.get();
		} catch (InterruptedException | ExecutionException e) {
			Logger.error("BuildSeries:done() Failed to get result from background task", e);
		}

		if (!threadRunning || stopFlag)
			firePropertyChange(THREAD_ENDED, null, statusMsg);
		else
			firePropertyChange(THREAD_DONE, null, statusMsg);

	}

	abstract void analyzeExperiment(Experiment exp);

	protected void waitFuturesCompletion(Processor processor, ArrayList<Future<?>> futuresArray,
			ProgressFrame progressBar) {
		int frame = 1;
		nframescomputed = futuresArray.size();
		nframestotal = 0;

		while (!futuresArray.isEmpty()) {
			final Future<?> f = futuresArray.get(futuresArray.size() - 1);
			if (progressBar != null)
				progressBar.setMessage("Analyze frame: " + nframestotal + "//" + nframescomputed);

			try {
				f.get();
			} catch (ExecutionException e) {
				Logger.error("BuildSeries:waitFuturesCompletion - frame:" + frame + " Execution exception", e);
			} catch (InterruptedException e) {
				Logger.warn("BuildSeries:waitFuturesCompletion - Interrupted exception: " + e.getMessage());
			}
			futuresArray.remove(f);

		}
	}

	void closeSequence(Sequence seq) {
		if (seq != null) {
			seq.close();
			seq = null;
		}
	}

	void closeViewer(Viewer v) {
		if (v != null) {
			v.close();
			v = null;
		}
	}

	Sequence newSequence(String title, IcyBufferedImage image) {
		Sequence seq = new Sequence();
		seq.setName(title);
		seq.setImage(0, 0, image);
		return seq;
	}

	protected boolean checkBoundsForCages(Experiment exp) {
		exp.getCages().setDetectBin_Ms(options.t_Ms_BinDuration);
		if (options.isFrameFixed) {
			exp.getCages().setDetectFirst_Ms(options.t_Ms_First);
			exp.getCages().setDetectLast_Ms(options.t_Ms_Last);
			if (exp.getCages().getDetectLast_Ms() > exp.getCamImageLast_ms())
				exp.getCages().setDetectLast_Ms(exp.getCamImageLast_ms());
		} else {
			exp.getCages().setDetectFirst_Ms(exp.getCamImageFirst_ms());
			exp.getCages().setDetectLast_Ms(exp.getCamImageLast_ms());
		}
		exp.getCages().setDetect_threshold(options.threshold);

		boolean flag = true;
		if (exp.getCages().getCageList().size() < 1) {
			Logger.warn("BuildSeries:checkBoundsForCells ! skipped experiment with no cell: "
					+ exp.getExperimentDirectory());
			flag = false;
		}
		return flag;
	}

	protected boolean loadDrosoTrack(Experiment exp) {
		exp.getSeqCamData()
				.setSequence(exp.getSeqCamData().initSequenceFromFirstImage(exp.getSeqCamData().getImagesList(true)));
		boolean flag = exp.loadCageMeasures();
		return flag;
	}
}
