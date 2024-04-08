package plugins.fmp.multicafe.series;


import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import icy.gui.frame.progress.ProgressFrame;
import icy.image.IcyBufferedImage;
import icy.sequence.Sequence;
import icy.system.SystemUtil;
import icy.system.thread.Processor;
import icy.type.collection.array.Array1DUtil;
import icy.type.geom.Polyline2D;
import plugins.fmp.multicafe.experiment.Capillary;
import plugins.fmp.multicafe.experiment.CapillaryMeasure;
import plugins.fmp.multicafe.experiment.Experiment;
import plugins.fmp.multicafe.experiment.SequenceKymos;




public class DetectGulps extends BuildSeries  
{
	
	void analyzeExperiment(Experiment exp) 
	{
		if (loadExperimentDataToDetectGulps(exp)) 
		{
			buildFilteredImage(exp);
			detectGulpsFromExperiment(exp);
		}
		exp.seqKymos.closeSequence();
	}
	
	private boolean loadExperimentDataToDetectGulps(Experiment exp) 
	{
		exp.xmlLoad_MCExperiment();
		boolean flag = exp.loadMCCapillaries_Only();
		flag &= exp.loadKymographs();
		flag &= exp.loadCapillaries();
		return flag;
	}
	
	private void buildFilteredImage(Experiment exp) 
	{
		if (exp.seqKymos == null)
			return;
		int zChannelDestination = 2;
		exp.kymosBuildFiltered01(0, zChannelDestination, options.transformForGulps, options.spanDiff);
	}
	
	public void detectGulpsFromExperiment(Experiment exp) 
	{			
		SequenceKymos seqCapillariesKymographs = exp.seqKymos;	
		int jitter = 5;
		int firstCapillary = 0;
		int lastCapillary = seqCapillariesKymographs.seq.getSizeT() -1;
		if (!options.detectAllGulps) {
			firstCapillary = options.kymoFirst;
			lastCapillary = firstCapillary;
		}
		seqCapillariesKymographs.seq.beginUpdate();
		threadRunning = true;
		stopFlag = false;
		ProgressFrame progressBar = new ProgressFrame("Processing with subthreads started");
		
		int nframes = lastCapillary - firstCapillary +1;
	    final Processor processor = new Processor(SystemUtil.getNumberOfCPUs());
	    processor.setThreadName("detect_levels");
	    processor.setPriority(Processor.NORM_PRIORITY);
        ArrayList<Future<?>> futures = new ArrayList<Future<?>>(nframes);
		futures.clear();
		
		final Sequence seqAnalyzed = seqCapillariesKymographs.seq;
		
		for (int indexCapillary = firstCapillary; indexCapillary <= lastCapillary; indexCapillary++) 
		{
			final Capillary capi = exp.capillaries.capillariesList.get(indexCapillary);
			capi.setGulpsOptions(options);
			futures.add(processor.submit(new Runnable () 
			{
				@Override
				public void run() 
				{
					if (options.buildDerivative) 
						capi.ptsDerivative = new CapillaryMeasure(
								capi.getLast2ofCapillaryName()+"_derivative", 
								capi.kymographIndex,
								getDerivativeProfile(seqAnalyzed, capi, jitter));
					
					if (options.buildGulps) 
					{
						capi.initGulps();
						capi.detectGulps();
					}
				}}));
		}
		
		waitFuturesCompletion(processor, futures, progressBar);
		exp.saveCapillaries() ;

		processor.shutdown();
		
		seqCapillariesKymographs.seq.endUpdate();
		progressBar.close();
	}	

	private List<Point2D> getDerivativeProfile(Sequence seq, Capillary cap, int jitter) 
	{	
		Polyline2D 	polyline = cap.ptsTop.polylineLevel;
		if (polyline == null)
			return null;
		
		int z = seq.getSizeZ() -1;
		int c = 0;
		IcyBufferedImage image = seq.getImage(cap.kymographIndex, z, c);
		List<Point2D> listOfMaxPoints = new ArrayList<>();
		int[] kymoImageValues = Array1DUtil.arrayToIntArray(image.getDataXY(c), image.isSignedDataType());	
		int xwidth = image.getSizeX();
		int yheight = image.getSizeY();

		for (int ix = 1; ix < polyline.npoints; ix++) 
		{
			// for each point of topLevelArray, define a bracket of rows to look at ("jitter" = 10)
			int low = (int) polyline.ypoints[ix]- jitter;
			int high = low + 2*jitter;
			if (low < 0) 
				low = 0;
			if (high >= yheight) 
				high = yheight-1;
			int max = kymoImageValues [ix + low*xwidth];
			for (int iy = low + 1; iy < high; iy++) 
			{
				int val = kymoImageValues [ix  + iy*xwidth];
				if (max < val) 
					max = val;
			}
			listOfMaxPoints.add(new Point2D.Double((double) ix, (double) max));
		}
		return listOfMaxPoints;
	}
	
}


