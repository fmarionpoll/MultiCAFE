package plugins.fmp.multicafe.fmp_series;

import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import javax.swing.SwingUtilities;

import icy.gui.frame.progress.ProgressFrame;
import icy.image.IcyBufferedImage;
import icy.sequence.Sequence;
import plugins.fmp.multicafe.fmp_experiment.Experiment;
import plugins.fmp.multicafe.fmp_service.SequenceLoaderService;
import plugins.fmp.multicafe.fmp_tools.Logger;
import plugins.fmp.multicafe.fmp_tools.ViewerFMP;
import plugins.fmp.multicafe.fmp_tools.imageTransform.ImageTransformInterface;
import plugins.fmp.multicafe.fmp_tools.imageTransform.ImageTransformOptions;
import plugins.kernel.roi.roi2d.ROI2DRectangle;

public abstract class FlyDetect extends BuildSeries {
	public DetectFlyTools find_flies = new DetectFlyTools();
	Sequence seqNegative = null;
	ViewerFMP vNegative = null;

	@Override
	void analyzeExperiment(Experiment exp) {
		if (!loadDrosoTrack(exp))
			return;
		if (!checkBoundsForCages(exp))
			return;
		if (!checkCagesForFlyDetection(exp))
			return;

		runFlyDetect(exp);
		exp.getCages().orderFlyPositions();
		if (!stopFlag)
			exp.saveCageMeasures();
		exp.getSeqCamData().closeSequence();
		closeSequence(seqNegative);
	}

	protected abstract void runFlyDetect(Experiment exp);

	protected boolean checkCagesForFlyDetection(Experiment exp) {
		if (exp.getCages() == null || exp.getCages().cagesList == null || exp.getCages().cagesList.isEmpty()) {
			Logger.error("FlyDetect:checkCagesForFlyDetection() No cages loaded for experiment: " + exp.getResultsDirectory());
			return false;
		}

		int cagesWithFlies = 0;
		int targetCageID = options.detectCage;
		
		for (plugins.fmp.multicafe.fmp_experiment.cages.Cage cage : exp.getCages().cagesList) {
			if (cage.getProperties().getCageNFlies() > 0) {
				cagesWithFlies++;
				if (targetCageID != -1 && cage.getProperties().getCageID() == targetCageID) {
					Logger.info("FlyDetect:checkCagesForFlyDetection() Found target cage " + targetCageID + " with " + cage.getProperties().getCageNFlies() + " fly(ies)");
					return true;
				}
			}
		}

		if (cagesWithFlies == 0) {
			Logger.error("FlyDetect:checkCagesForFlyDetection() No cages with flies (nFlies > 0) found. All " + exp.getCages().cagesList.size() + " cages have nFlies = 0. Experiment: " + exp.getResultsDirectory());
			return false;
		}

		if (targetCageID != -1) {
			Logger.error("FlyDetect:checkCagesForFlyDetection() Target cage " + targetCageID + " not found or has nFlies = 0. Found " + cagesWithFlies + " cage(s) with flies, but not the target cage.");
			return false;
		}

		Logger.info("FlyDetect:checkCagesForFlyDetection() Found " + cagesWithFlies + " cage(s) with flies out of " + exp.getCages().cagesList.size() + " total cages");
		return true;
	}

	protected void findFliesInAllFrames(Experiment exp) {
		ProgressFrame progressBar = new ProgressFrame("Detecting flies...");

		ImageTransformOptions transformOptions = setupTransformOptions(exp);
		ImageTransformInterface transformFunction = transformOptions.transformOption.getFunction();

		int t_previous = 0;
		int totalFrames = exp.getSeqCamData().getImageLoader().getNTotalFrames();
		SequenceLoaderService loader = new SequenceLoaderService();

		for (int index = 0; index < totalFrames; index++) {
			if (stopFlag)
				break;
			int t = index;
			String title = "Frame #" + t + "/" + totalFrames;
			progressBar.setMessage(title);

			IcyBufferedImage workImage = loader.imageIORead(exp.getSeqCamData().getFileNameFromImageList(t));
			updateTransformOptions(exp, t, t_previous, transformOptions);

			IcyBufferedImage negativeImage = transformFunction.getTransformedImage(workImage, transformOptions);
			try {
				seqNegative.beginUpdate();
				seqNegative.setImage(0, 0, negativeImage);
				vNegative.setTitle(title);
				List<Rectangle2D> listRectangles = find_flies.findFlies(negativeImage, t);
				displayRectanglesAsROIs(seqNegative, listRectangles, true);
				seqNegative.endUpdate();
			} catch (Exception e) {
				e.printStackTrace();
			}
			t_previous = t;
		}
		progressBar.close();
	}

	protected abstract ImageTransformOptions setupTransformOptions(Experiment exp);

	protected void updateTransformOptions(Experiment exp, int t, int t_previous, ImageTransformOptions options) {
		// default does nothing
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
					seqNegative = newSequence("detectionImage", exp.getSeqCamData().getReferenceImage());
					vNegative = new ViewerFMP(seqNegative, false, true);
					vNegative.setVisible(true);
				}
			});
		} catch (InvocationTargetException | InterruptedException e) {
			Logger.error("BuildSeries:openFlyDetectViewers() Failed to open fly detection viewers", e);
		}
	}
}
