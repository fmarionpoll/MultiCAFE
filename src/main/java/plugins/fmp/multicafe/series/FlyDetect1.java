package plugins.fmp.multicafe.series;

import java.awt.geom.Rectangle2D;
import java.util.List;

import icy.gui.frame.progress.ProgressFrame;
import icy.image.IcyBufferedImage;
import plugins.fmp.multicafe.experiment.Experiment;
import plugins.fmp.multicafe.tools.ImageTransform.ImageTransformInterface;
import plugins.fmp.multicafe.tools.ImageTransform.ImageTransformOptions;

public class FlyDetect1 extends BuildSeries {
	public boolean buildBackground = true;
	public boolean detectFlies = true;
	public FlyDetectTools find_flies = new FlyDetectTools();

	// -----------------------------------------------------

	void analyzeExperiment(Experiment exp) {
		if (!loadDrosoTrack(exp))
			return;
		if (!checkBoundsForCells(exp))
			return;

		runFlyDetect1(exp);
		exp.cells.orderFlyPositions();
		if (!stopFlag)
			exp.saveCageMeasures();
		exp.seqCamData.closeSequence();
		closeSequence(seqNegative);
	}

	private void runFlyDetect1(Experiment exp) {
		exp.cleanPreviousDetectedFliesROIs();
		find_flies.initParametersForDetection(exp, options);
		exp.cells.initFlyPositions(options.detectCell);

		openFlyDetectViewers(exp);
		findFliesInAllFrames(exp);
	}

	private void getReferenceImage(Experiment exp, int t, ImageTransformOptions options) {
		switch (options.transformOption) {
		case SUBTRACT_TM1:
			options.backgroundImage = imageIORead(exp.seqCamData.getFileNameFromImageList(t));
			break;

		case SUBTRACT_T0:
		case SUBTRACT_REF:
			if (options.backgroundImage == null)
				options.backgroundImage = imageIORead(exp.seqCamData.getFileNameFromImageList(0));
			break;

		case NONE:
		default:
			break;
		}
	}

	private void findFliesInAllFrames(Experiment exp) {
		ProgressFrame progressBar = new ProgressFrame("Detecting flies...");
		ImageTransformOptions transformOptions = new ImageTransformOptions();
		transformOptions.transformOption = options.transformop;
		ImageTransformInterface transformFunction = options.transformop.getFunction();

		int t_previous = 0;
		int totalFrames = exp.seqCamData.nTotalFrames;

		for (int index = 0; index < totalFrames; index++) {
			int t_from = index;
			String title = "Frame #" + t_from + "/" + exp.seqCamData.nTotalFrames;
			progressBar.setMessage(title);

			IcyBufferedImage sourceImage = imageIORead(exp.seqCamData.getFileNameFromImageList(t_from));
			getReferenceImage(exp, t_previous, transformOptions);
			IcyBufferedImage workImage = transformFunction.getTransformedImage(sourceImage, transformOptions);
			try {
				seqNegative.beginUpdate();
				seqNegative.setImage(0, 0, workImage);
				vNegative.setTitle(title);
				List<Rectangle2D> listRectangles = find_flies.findFlies(workImage, t_from);
				displayRectanglesAsROIs(seqNegative, listRectangles, true);
				seqNegative.endUpdate();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			t_previous = t_from;
		}
		progressBar.close();
	}
}