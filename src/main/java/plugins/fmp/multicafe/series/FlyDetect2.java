package plugins.fmp.multicafe.series;

import icy.image.IcyBufferedImageUtil;
import plugins.fmp.multicafe.fmp_experiment.Experiment;
import plugins.fmp.multicafe.service.SequenceLoaderService;
import plugins.fmp.multicafe.tools.ImageTransform.ImageTransformEnums;
import plugins.fmp.multicafe.tools.ImageTransform.ImageTransformOptions;

public class FlyDetect2 extends FlyDetect {
	public boolean viewInternalImages = true;

	// -----------------------------------------

	@Override
	protected void runFlyDetect(Experiment exp) {
		exp.cleanPreviousDetectedFliesROIs();
		find_flies.initParametersForDetection(exp, options);
		exp.getCages().initFlyPositions(options.detectCage);
		options.threshold = options.thresholdDiff;
		if (new SequenceLoaderService().loadReferenceImage(exp)) {
			openFlyDetectViewers(exp);
			findFliesInAllFrames(exp);
		}
	}

	@Override
	protected ImageTransformOptions setupTransformOptions(Experiment exp) {
		ImageTransformOptions transformOptions = new ImageTransformOptions();
		transformOptions.transformOption = ImageTransformEnums.SUBTRACT_REF;
		transformOptions.backgroundImage = IcyBufferedImageUtil.getCopy(exp.getSeqCamData().getRefImage());
		return transformOptions;
	}
}
