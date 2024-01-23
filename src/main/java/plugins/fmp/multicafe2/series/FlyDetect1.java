package plugins.fmp.multicafe2.series;

import java.awt.geom.Rectangle2D;
import java.util.List;

import icy.gui.frame.progress.ProgressFrame;
import icy.image.IcyBufferedImage;

import plugins.fmp.multicafe2.experiment.Experiment;
import plugins.fmp.multicafe2.tools.Image.ImageTransformInterface;
import plugins.fmp.multicafe2.tools.Image.ImageTransformOptions;




public class FlyDetect1 extends BuildSeries 
{
	public boolean buildBackground	= true;
	public boolean	detectFlies = true;
	public FlyDetectTools find_flies = new FlyDetectTools();
	
	// -----------------------------------------------------
	
	void analyzeExperiment(Experiment exp) 
	{
		if (!loadDrosoTrack(exp))
			return;
		if (!checkBoundsForCages(exp))
			return;
		
		runFlyDetect1(exp);
		if (!stopFlag)
			exp.saveCagesMeasures() ;
		exp.seqCamData.closeSequence();
		closeSequence(seqNegative);
    }
	
	private void runFlyDetect1(Experiment exp) 
	{
		exp.cleanPreviousDetectedFliesROIs();
		find_flies.initParametersForDetection(exp, options);
		find_flies.initCagesPositions(exp, options.detectCage);
		
		openFlyDetectViewers(exp);
		findFliesInAllFrames(exp);
		exp.cages.orderFlyPositions();
	}
	
	private void getReferenceImage (Experiment exp, int t, ImageTransformOptions options) 
	{
		switch (options.transformOption) 
		{
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
	
	private void findFliesInAllFrames(Experiment exp) 
	{
		ProgressFrame progressBar = new ProgressFrame("Detecting flies...");
		ImageTransformOptions transformOptions = new ImageTransformOptions();
		transformOptions.transformOption = options.transformop;
		getReferenceImage (exp, 0, transformOptions);
		ImageTransformInterface transformFunction = options.transformop.getFunction();
		
		int t_current = 0;
	
		long last_ms = exp.cages.detectLast_Ms + exp.cages.detectBin_Ms ;
		for (long index_ms = exp.cages.detectFirst_Ms; index_ms <= last_ms; index_ms += exp.cages.detectBin_Ms ) 
		{
			final int t_previous = t_current;
			final int t_from = (int) ((index_ms - exp.camImageFirst_ms)/exp.camImageBin_ms);
			if (t_from >= exp.seqCamData.nTotalFrames)
				continue;
			
			t_current = t_from;
			String title = "Frame #"+ t_from + "/" + exp.seqCamData.nTotalFrames;
			progressBar.setMessage(title);
	
			IcyBufferedImage sourceImage = imageIORead(exp.seqCamData.getFileNameFromImageList(t_from));
			getReferenceImage (exp, t_previous, transformOptions);
			IcyBufferedImage workImage = transformFunction.getTransformedImage(sourceImage, transformOptions); 
			if (workImage == null)
				return;

			try 
			{
				seqNegative.beginUpdate();
				seqNegative.setImage(0, 0, workImage);
				vNegative.setTitle(title);
				List<Rectangle2D> listRectangles = find_flies.findFlies1 (workImage, t_from);
				addGreenROI2DPoints(seqNegative, listRectangles, true);
				seqNegative.endUpdate();
			} 
			catch (InterruptedException e) {
				e.printStackTrace();
			}					
		}

		progressBar.close();
	}
}