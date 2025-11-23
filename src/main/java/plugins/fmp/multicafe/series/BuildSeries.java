package plugins.fmp.multicafe.series;

import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import icy.gui.frame.progress.ProgressFrame;
import icy.gui.viewer.Viewer;
import icy.image.IcyBufferedImage;
import icy.sequence.Sequence;
import icy.system.thread.Processor;
import plugins.fmp.multicafe.experiment.Experiment;
import plugins.fmp.multicafe.tools.Logger;
import plugins.fmp.multicafe.tools.ViewerFMP;
import plugins.fmp.multicafe.tools.JComponents.ExperimentsJComboBox;
import plugins.kernel.roi.roi2d.ROI2DRectangle;

public abstract class BuildSeries extends SwingWorker<Integer, Integer> {

	public BuildSeriesOptions options = new BuildSeriesOptions();
	public boolean stopFlag = false;
	public boolean threadRunning = false;
	int nframescomputed = 0;
	int nframestotal = 0;

	int selectedExperimentIndex = -1;
	Sequence seqNegative = null;
	ViewerFMP vNegative = null;
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

	protected boolean checkBoundsForCages(Experiment exp) {
		exp.cages.detectBin_Ms = options.t_Ms_BinDuration;
		if (options.isFrameFixed) {
			exp.cages.detectFirst_Ms = options.t_Ms_First;
			exp.cages.detectLast_Ms = options.t_Ms_Last;
			if (exp.cages.detectLast_Ms > exp.getCamImageLast_ms())
				exp.cages.detectLast_Ms = exp.getCamImageLast_ms();
		} else {
			exp.cages.detectFirst_Ms = exp.getCamImageFirst_ms();
			exp.cages.detectLast_Ms = exp.getCamImageLast_ms();
		}
		exp.cages.detect_threshold = options.threshold;

		boolean flag = true;
		if (exp.cages.cageList.size() < 1) {
			Logger.warn("BuildSeries:checkBoundsForCells ! skipped experiment with no cell: "
					+ exp.getExperimentDirectory());
			flag = false;
		}
		return flag;
	}

	public IcyBufferedImage imageIORead(String name) {
		BufferedImage image = null;
		try {
			image = ImageIO.read(new File(name));
		} catch (IOException e) {
			Logger.error("BuildSeries:imageIORead() Failed to read image: " + name, e);
		}
		return IcyBufferedImage.createFrom(image);
	}

	protected boolean loadDrosoTrack(Experiment exp) {
		exp.getSeqCamData().seq = exp.getSeqCamData().initSequenceFromFirstImage(exp.getSeqCamData().getImagesList(true));
		boolean flag = exp.loadCageMeasures();
		return flag;
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

	void displayRectanglesAsROIs(Sequence seq, List<Rectangle2D> listRectangles, boolean eraseOldPoints) {
		if (eraseOldPoints)
			seq.removeAllROI();

		for (Rectangle2D rectangle : listRectangles) {
			ROI2DRectangle flyRectangle = new ROI2DRectangle(rectangle);
			flyRectangle.setColor(Color.YELLOW);
			seq.addROI(flyRectangle);
		}
	}

	void openFlyDetectViewers(Experiment exp) {
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				public void run() {
					seqNegative = newSequence("detectionImage", exp.getSeqCamData().refImage);
					vNegative = new ViewerFMP(seqNegative, false, true);
					vNegative.setVisible(true);
				}
			});
		} catch (InvocationTargetException | InterruptedException e) {
			Logger.error("BuildSeries:openFlyDetectViewers() Failed to open fly detection viewers", e);
		}
	}

}
