package plugins.fmp.multicafe.series;

import java.awt.geom.Rectangle2D;
import java.util.List;

import icy.gui.frame.progress.ProgressFrame;
import icy.image.IcyBufferedImage;
import icy.image.IcyBufferedImageUtil;
import plugins.fmp.multicafe.experiment.Experiment;
import plugins.fmp.multicafe.tools.ImageTransform.ImageTransformEnums;
import plugins.fmp.multicafe.tools.ImageTransform.ImageTransformInterface;
import plugins.fmp.multicafe.tools.ImageTransform.ImageTransformOptions;

public class FlyDetect2 extends BuildSeries {
	private FlyDetectTools find_flies = new FlyDetectTools();
	public boolean viewInternalImages = true;

	// -----------------------------------------

	void analyzeExperiment(Experiment exp) {
		if (!loadDrosoTrack(exp))
			return;
		if (!checkBoundsForCages(exp))
			return;

		runFlyDetect2(exp);
		exp.cages.orderFlyPositions();
		if (!stopFlag)
			exp.saveCageMeasures();
		exp.getSeqCamData().closeSequence();
		closeSequence(seqNegative);
	}

	private void runFlyDetect2(Experiment exp) {
		exp.cleanPreviousDetectedFliesROIs();
		find_flies.initParametersForDetection(exp, options);
		exp.cages.initFlyPositions(options.detectCage);
		options.threshold = options.thresholdDiff;
		if (exp.loadReferenceImage()) {
			openFlyDetectViewers(exp);
			findFliesInAllFrames(exp);
		}
	}

	private void findFliesInAllFrames(Experiment exp) {
		ProgressFrame progressBar = new ProgressFrame("Detecting flies...");
		ImageTransformOptions transformOptions = new ImageTransformOptions();
		transformOptions.transformOption = ImageTransformEnums.SUBTRACT_REF;
		transformOptions.backgroundImage = IcyBufferedImageUtil.getCopy(exp.getSeqCamData().refImage);
		ImageTransformInterface transformFunction = transformOptions.transformOption.getFunction();

		int totalFrames = exp.getSeqCamData().nTotalFrames;
		for (int index = 0; index < totalFrames; index++) {
			int t_from = index;
			String title = "Frame #" + t_from + "/" + exp.getSeqCamData().nTotalFrames;
			progressBar.setMessage(title);

			IcyBufferedImage workImage = imageIORead(exp.getSeqCamData().getFileNameFromImageList(t_from));
			IcyBufferedImage negativeImage = transformFunction.getTransformedImage(workImage, transformOptions);
			try {
				seqNegative.beginUpdate();
				seqNegative.setImage(0, 0, negativeImage);
				vNegative.setTitle(title);
				List<Rectangle2D> listRectangles = find_flies.findFlies(negativeImage, t_from);
				displayRectanglesAsROIs(seqNegative, listRectangles, true);
				seqNegative.endUpdate();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		progressBar.close();
	}

}