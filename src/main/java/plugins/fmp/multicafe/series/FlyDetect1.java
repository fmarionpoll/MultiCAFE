package plugins.fmp.multicafe.series;

import plugins.fmp.multicafe.experiment.Experiment;
import plugins.fmp.multicafe.tools.ImageTransform.ImageTransformOptions;

public class FlyDetect1 extends FlyDetect {
	public boolean buildBackground = true;
	public boolean detectFlies = true;

	// -----------------------------------------------------

	@Override
	protected void runFlyDetect(Experiment exp) {
		exp.cleanPreviousDetectedFliesROIs();
		find_flies.initParametersForDetection(exp, options);
		exp.cages.initFlyPositions(options.detectCage);

		openFlyDetectViewers(exp);
		findFliesInAllFrames(exp);
	}

	@Override
	protected ImageTransformOptions setupTransformOptions(Experiment exp) {
		ImageTransformOptions transformOptions = new ImageTransformOptions();
		transformOptions.transformOption = options.transformop;
		return transformOptions;
	}

	@Override
	protected void updateTransformOptions(Experiment exp, int t, int t_previous, ImageTransformOptions options) {
		switch (options.transformOption) {
		case SUBTRACT_TM1:
			options.backgroundImage = imageIORead(exp.getSeqCamData().getFileNameFromImageList(t));
			break;

		case SUBTRACT_T0:
		case SUBTRACT_REF:
			if (options.backgroundImage == null)
				options.backgroundImage = imageIORead(exp.getSeqCamData().getFileNameFromImageList(0));
			break;

		case NONE:
		default:
			break;
		}
	}
}
