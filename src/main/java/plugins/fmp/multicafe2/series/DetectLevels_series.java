package plugins.fmp.multicafe2.series;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import icy.gui.frame.progress.ProgressFrame;
import icy.image.IcyBufferedImage;
import icy.system.SystemUtil;
import icy.system.thread.Processor;
import icy.type.collection.array.Array1DUtil;
import plugins.fmp.multicafe2.experiment.Capillary;
import plugins.fmp.multicafe2.experiment.CapillaryLevel;
import plugins.fmp.multicafe2.experiment.Experiment;
import plugins.fmp.multicafe2.experiment.SequenceKymos;
import plugins.fmp.multicafe2.tools.ImageToolsTransform;



public class DetectLevels_series extends BuildSeries  
{
	ImageToolsTransform tImg = new ImageToolsTransform();
	
	void analyzeExperiment(Experiment exp) 
	{
		if (loadExperimentDataToDetectLevels(exp)) 
		{ 
			exp.seqKymos.displayViewerAtRectangle(options.parent0Rect);
			detectCapillaryLevels(exp);
		}
		exp.closeSequences();
	}
	
	private boolean loadExperimentDataToDetectLevels(Experiment exp) 
	{
		exp.xmlLoadMCExperiment();
		exp.xmlLoadMCCapillaries();
		return exp.loadKymographs();
	}
	
	private boolean detectCapillaryLevels(Experiment exp) 
	{
		SequenceKymos seqKymos = exp.seqKymos;
		seqKymos.seq.removeAllROI();
		
		threadRunning = true;
		stopFlag = false;
		ProgressFrame progressBar = new ProgressFrame("Processing with subthreads started");
		int firstKymo = options.firstKymo;
		if (firstKymo > seqKymos.seq.getSizeT() || firstKymo < 0)
			firstKymo = 0;
		int lastKymo = options.lastKymo;
		if (lastKymo >= seqKymos.seq.getSizeT())
			lastKymo = seqKymos.seq.getSizeT() -1;
		seqKymos.seq.beginUpdate();
		
		int nframes = lastKymo - firstKymo +1;
	    final Processor processor = new Processor(SystemUtil.getNumberOfCPUs());
	    processor.setThreadName("detectlevel");
	    processor.setPriority(Processor.NORM_PRIORITY);
        ArrayList<Future<?>> futures = new ArrayList<Future<?>>(nframes);
		futures.clear();
		
		tImg.setSpanDiff(options.spanDiffTop);
		tImg.setSequence(seqKymos);
		final int jitter = 10;
		
		for (int index = firstKymo; index <= lastKymo; index++) 
		{
			final int t_index = index;
			final Capillary cap_index = exp.capillaries.capillariesList.get(t_index);
			if (!options.detectR && cap_index.getKymographName().endsWith("2"))
				continue;
			if (!options.detectL && cap_index.getKymographName().endsWith("1"))
				continue;
				
			futures.add(processor.submit(new Runnable () 
			{
				@Override
				public void run() 
				{	
					IcyBufferedImage rawImage = imageIORead(seqKymos.getFileName(t_index));
					IcyBufferedImage transform1Image = tImg.transformImage (rawImage, options.transform1);
					IcyBufferedImage transform2Image = tImg.transformImage (rawImage, options.transform2);
					
					int c = 0;
					Object transform1Array = transform1Image.getDataXY(c);
					int[] transform11DArray = Array1DUtil.arrayToIntArray(transform1Array, transform1Image.isSignedDataType());

					cap_index.indexKymograph = t_index;
					cap_index.ptsDerivative = null;
					cap_index.gulpsRois = null;
					cap_index.limitsOptions.copyFrom(options);
					
					int firstColumn = 0;
					int lastColumn = transform1Image.getSizeX()-1;
					int xwidth = transform1Image.getSizeX();
					int yheight = transform1Image.getSizeY();
					if (options.analyzePartOnly) 
					{
						firstColumn = options.firstPixel;
						lastColumn = options.lastPixel;
						if (lastColumn > xwidth-1)
							lastColumn = xwidth -1;
					} 
					else 
					{
						cap_index.ptsTop = null;
						cap_index.ptsBottom = null;
					}
					
					int oldiytop = 0;		// assume that curve goes from left to right with jitter 
					int oldiybottom = yheight-1;
					int nColumns = lastColumn - firstColumn +1;

					List<Point2D> limitTop = new ArrayList<Point2D>(nColumns);
					List<Point2D> limitBottom = new ArrayList<Point2D>(nColumns);
		
					// scan each image column
					for (int iColumn = firstColumn; iColumn <= lastColumn; iColumn++) 
					{
						int ytop = detectThresholdFromTop(iColumn, oldiytop, jitter, transform11DArray, xwidth, yheight, options);
						int ybottom = detectThresholdFromBottom(iColumn, oldiybottom, jitter, transform11DArray, xwidth, yheight, options);
						if (ybottom <= ytop) 
						{
							ybottom = oldiybottom;
							ytop = oldiytop;
						}
						limitTop.add(new Point2D.Double(iColumn, ytop));
						limitBottom.add(new Point2D.Double(iColumn, ybottom));
						oldiytop = ytop;
						oldiybottom = ybottom;
					}	
					
					if (options.analyzePartOnly) 
					{
						cap_index.ptsTop.polylineLevel.insertSeriesofYPoints(limitTop, firstColumn, lastColumn);
						cap_index.ptsBottom.polylineLevel.insertSeriesofYPoints(limitBottom, firstColumn, lastColumn);
					} 
					else 
					{
						cap_index.ptsTop    = new CapillaryLevel(cap_index.getLast2ofCapillaryName()+"_toplevel", t_index, limitTop);
						cap_index.ptsBottom = new CapillaryLevel(cap_index.getLast2ofCapillaryName()+"_bottomlevel", t_index, limitBottom);
					}
					exp.capillaries.xmlSaveCapillary_Measures(exp.getKymosBinFullDirectory(), cap_index);
				}}));
		}
		waitFuturesCompletion(processor, futures, progressBar);
		seqKymos.seq.endUpdate();
		
		progressBar.close();
		
		return true;
	}

	private int checkLimits (int rowIndex, int maximumRowIndex) 
	{
		if (rowIndex < 0)
			rowIndex = 0;
		if (rowIndex > maximumRowIndex)
			rowIndex = maximumRowIndex;
		return rowIndex;
	}

	private int detectThresholdFromTop(int ix, int oldiytop, int jitter, int [] tabValues, int xwidth, int yheight, Options_BuildSeries options) 
	{
		int y = yheight-1;
		oldiytop = checkLimits(oldiytop - jitter, yheight-1);
		for (int iy = oldiytop; iy < yheight; iy++) 
		{
			boolean flag = false;
			if (options.directionUp)
				flag = tabValues [ix + iy* xwidth] > options.detectLevelThreshold;
			else 
				flag = tabValues [ix + iy* xwidth] < options.detectLevelThreshold;
			if (flag) {
				y = iy;
				break;
			}
		}
		return y;
	}
	
	private int detectThresholdFromBottom(int ix, int oldiybottom, int jitter, int[] tabValues, int xwidth, int yheight, Options_BuildSeries options) 
	{
		int y = 0;
		oldiybottom = yheight - 1; // no memory needed  - the bottom is quite stable
		for (int iy = oldiybottom; iy >= 0 ; iy--) 
		{
			boolean flag = false;
			if (options.directionUp)
				flag = tabValues [ix + iy* xwidth] > options.detectLevelThreshold;
			else 
				flag = tabValues [ix + iy* xwidth] < options.detectLevelThreshold;
			if (flag) {
				y = iy;
				break;
			}
		}
		return y;
	}
	
}

