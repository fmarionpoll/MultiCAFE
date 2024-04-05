package plugins.fmp.multicafe2.series;

import java.awt.geom.Rectangle2D;
import java.util.List;

import icy.gui.frame.progress.ProgressFrame;
import icy.image.IcyBufferedImage;
import icy.image.IcyBufferedImageUtil;

import plugins.fmp.multicafe2.experiment.Experiment;
import plugins.fmp.multicafe2.tools.Image.ImageTransformEnums;
import plugins.fmp.multicafe2.tools.Image.ImageTransformInterface;
import plugins.fmp.multicafe2.tools.Image.ImageTransformOptions;




public class FlyDetect2 extends BuildSeries 
{
	private FlyDetectTools find_flies = new FlyDetectTools();	
	public boolean viewInternalImages = true;

	// -----------------------------------------

	void analyzeExperiment(Experiment exp) 
	{
		if (!loadDrosoTrack(exp))
			return;
		if (!checkBoundsForCages(exp))
			return;

		runFlyDetect2(exp);
		exp.cages.orderFlyPositions();
		if (!stopFlag)
			exp.saveCagesMeasures();
		exp.seqCamData.closeSequence();
//		closeSequence(seqNegative);
    }
	
	private void runFlyDetect2(Experiment exp) 
	{
		exp.cleanPreviousDetectedFliesROIs();
		find_flies.initParametersForDetection(exp, options);
		exp.cages.initFlyPositions(options.detectCage);
		options.threshold = options.thresholdDiff;

		if (exp.loadReferenceImage()) 
		{
			openFlyDetectViewers(exp);
			findFliesInAllFrames(exp);
		}
	}

	private void findFliesInAllFrames(Experiment exp) 
	{
		ProgressFrame progressBar = new ProgressFrame("Detecting flies...");
		seqNegative.removeAllROI();
		
		ImageTransformOptions transformOptions = new ImageTransformOptions();
		transformOptions.transformOption = ImageTransformEnums.SUBTRACT_REF;
		transformOptions.backgroundImage = IcyBufferedImageUtil.getCopy(exp.seqCamData.refImage);
		ImageTransformInterface transformFunction = transformOptions.transformOption.getFunction();
		
		long last_ms = exp.cages.detectLast_Ms + exp.cages.detectBin_Ms ;
		for (long index_ms = exp.cages.detectFirst_Ms ; index_ms <= last_ms; index_ms += exp.cages.detectBin_Ms ) 
		{
			final int t_from = (int) ((index_ms - exp.camImageFirst_ms)/exp.camImageBin_ms);
			if (t_from >= exp.seqCamData.nTotalFrames)
				continue;
			String title = "Frame #"+ t_from + "/" + exp.seqCamData.nTotalFrames;
			progressBar.setMessage(title);

			IcyBufferedImage workImage = imageIORead(exp.seqCamData.getFileNameFromImageList(t_from));
			IcyBufferedImage negativeImage = transformFunction.getTransformedImage(workImage, transformOptions);
			try {
				seqNegative.beginUpdate();
				seqNegative.setImage(0, 0, negativeImage);
				vNegative.setTitle(title);
				List<Rectangle2D> listRectangles = find_flies.findFlies2( negativeImage, t_from);
				displayRectanglesAsROIs(seqNegative, listRectangles, true);
				seqNegative.endUpdate();
			} 
			catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		progressBar.close();
	}

}