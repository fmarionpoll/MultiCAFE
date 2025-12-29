package plugins.fmp.multicafe.fmp_series;

import java.awt.geom.Rectangle2D;
import java.util.List;

import icy.gui.frame.progress.ProgressFrame;
import icy.image.IcyBufferedImage;
import icy.sequence.Sequence;
import plugins.fmp.multicafe.fmp_experiment.Experiment;
import plugins.fmp.multicafe.fmp_service.SequenceLoaderService;
import plugins.fmp.multicafe.fmp_tools.Logger;
import plugins.fmp.multicafe.fmp_tools.ViewerFMP;
import plugins.fmp.multicafe.fmp_tools.imageTransform.ImageTransformInterface;
import plugins.fmp.multicafe.fmp_tools.imageTransform.ImageTransformOptions;

public abstract class FlyDetect extends BuildSeries {
	public DetectFlyTools find_flies = new DetectFlyTools();
	Sequence seqNegative = null;
	ViewerFMP vNegative = null;

	@Override
	void analyzeExperiment(Experiment exp) {
		if (!loadDrosoTrack2(exp))
			return;
		// Add this line to ensure capillaries are loaded and won't be overwritten as
		// empty if a save occurs
		exp.loadMCCapillaries_Only();
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
			Logger.error("FlyDetect:checkCagesForFlyDetection() No cages loaded for experiment: "
					+ exp.getResultsDirectory());
			return false;
		}

		int cagesWithFlies = 0;
		int targetCageID = options.detectCage;

		for (plugins.fmp.multicafe.fmp_experiment.cages.Cage cage : exp.getCages().cagesList) {
			if (cage.getProperties().getCageNFlies() > 0) {
				cagesWithFlies++;
				if (targetCageID != -1 && cage.getProperties().getCageID() == targetCageID) {
					Logger.info("FlyDetect:checkCagesForFlyDetection() Found target cage " + targetCageID + " with "
							+ cage.getProperties().getCageNFlies() + " fly(ies)");
					return true;
				}
			}
		}

		if (cagesWithFlies == 0) {
			Logger.error("FlyDetect:checkCagesForFlyDetection() No cages with flies (nFlies > 0) found. All "
					+ exp.getCages().cagesList.size() + " cages have nFlies = 0. Experiment: "
					+ exp.getResultsDirectory());
			return false;
		}

		if (targetCageID != -1) {
			Logger.error("FlyDetect:checkCagesForFlyDetection() Target cage " + targetCageID
					+ " not found or has nFlies = 0. Found " + cagesWithFlies
					+ " cage(s) with flies, but not the target cage.");
			return false;
		}

		Logger.info("FlyDetect:checkCagesForFlyDetection() Found " + cagesWithFlies + " cage(s) with flies out of "
				+ exp.getCages().cagesList.size() + " total cages");
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
				displayRectanglesAsROIs1(seqNegative, listRectangles, true);
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

	protected boolean loadDrosoTrack2(Experiment exp) {
		List<String> imagesList = exp.getSeqCamData().getImagesList(true);
		Sequence seq = new SequenceLoaderService().initSequenceFromFirstImage(imagesList);
		exp.getSeqCamData().setSequence(seq);
		boolean flag = exp.loadCageMeasures();
		return flag;
	}
}
