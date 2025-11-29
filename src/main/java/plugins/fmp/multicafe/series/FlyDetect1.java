package plugins.fmp.multicafe.series;

import plugins.fmp.multicafe.fmp_experiment.Experiment;
import plugins.fmp.multicafe.fmp_service.SequenceLoaderService;
import plugins.fmp.multicafe.tools0.ImageTransform.ImageTransformOptions;

public class FlyDetect1 extends FlyDetect {
	public boolean buildBackground = true;
	public boolean detectFlies = true;

	// -----------------------------------------------------

	@Override
	protected void runFlyDetect(Experiment exp) {
		exp.cleanPreviousDetectedFliesROIs();
		find_flies.initParametersForDetection(exp, options);
		exp.getCages().initFlyPositions(options.detectCage);

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
		SequenceLoaderService loader = new SequenceLoaderService();
		switch (options.transformOption) {
		case SUBTRACT_TM1:
			options.backgroundImage = loader.imageIORead(exp.getSeqCamData().getFileNameFromImageList(t));
			break;

		case SUBTRACT_T0:
		case SUBTRACT_REF:
			if (options.backgroundImage == null)
				options.backgroundImage = loader.imageIORead(exp.getSeqCamData().getFileNameFromImageList(0));
			break;

		case NONE:
		default:
			break;
		}
	}
}
