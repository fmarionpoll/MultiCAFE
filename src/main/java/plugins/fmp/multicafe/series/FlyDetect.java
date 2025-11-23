package plugins.fmp.multicafe.series;

import java.awt.geom.Rectangle2D;
import java.util.List;

import icy.gui.frame.progress.ProgressFrame;
import icy.image.IcyBufferedImage;
import plugins.fmp.multicafe.experiment.Experiment;
import plugins.fmp.multicafe.tools.ImageTransform.ImageTransformInterface;
import plugins.fmp.multicafe.tools.ImageTransform.ImageTransformOptions;

public abstract class FlyDetect extends BuildSeries {
	public FlyDetectTools find_flies = new FlyDetectTools();

	@Override
	void analyzeExperiment(Experiment exp) {
		if (!loadDrosoTrack(exp))
			return;
		if (!checkBoundsForCages(exp))
			return;

		runFlyDetect(exp);
		exp.cages.orderFlyPositions();
		if (!stopFlag)
			exp.saveCageMeasures();
		exp.getSeqCamData().closeSequence();
		closeSequence(seqNegative);
	}

	protected abstract void runFlyDetect(Experiment exp);

	protected void findFliesInAllFrames(Experiment exp) {
		ProgressFrame progressBar = new ProgressFrame("Detecting flies...");

		ImageTransformOptions transformOptions = setupTransformOptions(exp);
		ImageTransformInterface transformFunction = transformOptions.transformOption.getFunction();

		int t_previous = 0;
		int totalFrames = exp.getSeqCamData().nTotalFrames;

		for (int index = 0; index < totalFrames; index++) {
			if (stopFlag)
				break;
			int t = index;
			String title = "Frame #" + t + "/" + totalFrames;
			progressBar.setMessage(title);

			IcyBufferedImage workImage = imageIORead(exp.getSeqCamData().getFileNameFromImageList(t));
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
}

