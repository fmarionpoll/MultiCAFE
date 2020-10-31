package plugins.fmp.multicafe.series;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import icy.file.Saver;
import icy.gui.frame.progress.ProgressFrame;
import icy.image.IcyBufferedImage;
import icy.sequence.Sequence;
import icy.system.SystemUtil;
import icy.system.thread.Processor;
import icy.type.DataType;
import icy.type.collection.array.Array1DUtil;
import plugins.kernel.roi.roi2d.ROI2DShape;

import loci.formats.FormatException;
import plugins.fmp.multicafe.sequence.Capillary;
import plugins.fmp.multicafe.sequence.Experiment;
import plugins.fmp.multicafe.sequence.SequenceCamData;
import plugins.fmp.multicafe.sequence.SequenceKymos;

import plugins.nchenouard.kymographtracker.Util;
import plugins.nchenouard.kymographtracker.spline.CubicSmoothingSpline;



public class BuildKymographs_series extends BuildSeries  {
	public boolean		buildBackground		= true;
	private DataType 	dataType 			= DataType.INT;
//	private int 		imagewidth 			= 1;
	    
	// ------------------------------
	
	void runMeasurement(Experiment exp) {
		loadExperimentDataToBuildKymos(exp);
		exp.displaySequenceData(options.parent0Rect, exp.seqCamData.seq);
		exp.setKymoFrameStep (options.stepFrame);
		if (options.isFrameFixed) {
			exp.setKymoFrameStart( options.startFrame);
			exp.setKymoFrameEnd (options.endFrame);
			if (exp.getKymoFrameEnd() > (exp.getSeqCamSizeT() - 1))
				exp.setKymoFrameEnd (exp.getSeqCamSizeT() - 1);
		} else {
			exp.setKymoFrameStart (0);
			exp.setKymoFrameEnd (exp.seqCamData.seq.getSizeT() - 1);
		}
		if (computeKymo(exp)) {
			saveComputation(exp);
		}
		exp.seqCamData.closeSequence();
		exp.seqKymos.closeSequence();
	}
	
	private void loadExperimentDataToBuildKymos(Experiment exp) {
		exp.xmlLoadExperiment();
		exp.seqCamData.loadSequence(exp.getExperimentFileName()) ;
		exp.xmlLoadMCcapillaries_Only();
	}
			
	private void saveComputation(Experiment exp) {	
		if (options.doCreateResults_bin) {
			exp.resultsSubPath = exp.getResultsDirectoryNameFromKymoFrameStep();
		}
		String directory = exp.getResultsDirectory();
		if (directory == null)
			return;
		
		for (int t = 0; t < exp.seqKymos.seq.getSizeT(); t++) {
			Capillary cap = exp.capillaries.capillariesArrayList.get(t);
			String filename = directory + File.separator + cap.getCapillaryName() + ".tiff";
			File file = new File (filename);
			IcyBufferedImage image = exp.seqKymos.seq.getImage(t, 0);
			try {
				Saver.saveImage(image, file, true);
			} catch (FormatException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		exp.xmlSaveExperiment();
	}
	
	private boolean computeKymo (Experiment exp) {
		SequenceCamData seqCamData = exp.seqCamData;
		SequenceKymos seqKymos = exp.seqKymos;
		if (seqCamData == null || seqKymos == null)
			return false;
	
		threadRunning = true;
		stopFlag = false;
		ProgressFrame progressBar = new ProgressFrame("Processing with subthreads started");
		
		initArraysToBuildKymographImages(exp);
		if (exp.capillaries.capillariesArrayList.size() < 1) {
			System.out.println("Abort (1): nbcapillaries = 0");
			return false;
		}
		
		IcyBufferedImage  workImage0 = seqCamData.seq.getImage(options.startFrame, 0); 
		seqCamData.seq.removeAllROI();
		
		final Sequence seqForRegistration = new Sequence();
		if (options.doRegistration) {
			seqForRegistration.addImage(0, workImage0);
			seqForRegistration.addImage(1, workImage0);
		}
		
		int nbcapillaries = exp.capillaries.capillariesArrayList.size();
		if (nbcapillaries == 0) {
			System.out.println("Abort(2): nbcapillaries = 0");
			return false;
		}
		
		int nframes = (exp.getKymoFrameEnd() - exp.getKymoFrameStart()) / exp.getKymoFrameStep() +1;
	    final Processor processor = new Processor(SystemUtil.getNumberOfCPUs());
	    processor.setThreadName("buildkymo2");
	    processor.setPriority(Processor.NORM_PRIORITY);
        ArrayList<Future<?>> futures = new ArrayList<Future<?>>(nframes);
		futures.clear();

		seqCamData.seq.beginUpdate();
		
		int ipixelcolumn = 0;
		for (int frame = exp.getKymoFrameStart() ; frame <= exp.getKymoFrameEnd(); frame += exp.getKymoFrameStep(), ipixelcolumn++ ) {
			final int t_from = frame;
			final int t_out = ipixelcolumn;
			futures.add(processor.submit(new Runnable () {
			@Override
			public void run() {		
				final IcyBufferedImage  workImage = seqCamData.seq.getImage(t_from, 0);
				if (options.doRegistration ) 
					adjustImage(seqForRegistration, workImage);
				int vinputSizeX = workImage.getWidth();
				
				ArrayList<double []> sourceValuesList = transferWorkImageToDoubleArrayList (workImage);
				for (int iroi=0; iroi < nbcapillaries; iroi++) {
					Capillary cap = exp.capillaries.capillariesArrayList.get(iroi);
					int imagewidth = cap.bufKymoImage.getWidth();
					for (int chan = 0; chan < seqCamData.seq.getSizeC(); chan++) { 
						double [] tabValues = cap.tabValuesList.get(chan); 
						double [] sourceValues = sourceValuesList.get(chan);
						int cnt = 0;
						for (ArrayList<int[]> mask:cap.masksList) {
							double sum = 0;
							for (int[] m:mask)
								sum += sourceValues[m[0] + m[1]*vinputSizeX];
							if (mask.size() > 1)
								sum = sum/mask.size();
							tabValues[cnt*imagewidth + t_out] = sum; 
							cnt ++;
						}
					}
				}
			}
			}));
		}
		waitCompletion(processor, futures, progressBar);
		seqCamData.seq.endUpdate();
		
        progressBar.close();
		seqKymos.seq.removeAllImages();

		for (int icap=0; icap < nbcapillaries; icap++) {
			Capillary cap = exp.capillaries.capillariesArrayList.get(icap);
			for (int chan = 0; chan < seqCamData.seq.getSizeC(); chan++) {
				double [] tabValues = cap.tabValuesList.get(chan); 
				Object destArray = cap.bufKymoImage.getDataXY(chan);
				Array1DUtil.doubleArrayToSafeArray(tabValues, destArray, cap.bufKymoImage.isSignedDataType());
				cap.bufKymoImage.setDataXY(chan, destArray);
			}
			seqKymos.seq.setImage(icap, 0, cap.bufKymoImage);
			cap.masksList.clear();
			cap.tabValuesList.clear();
			cap.bufKymoImage = null;
		}
		seqKymos.seq.setName(exp.getDecoratedImageNameFromCapillary(0));

		return true;
	}
	
	// -------------------------------------------
	
	private ArrayList<double []> transferWorkImageToDoubleArrayList(IcyBufferedImage  workImage) {	
		ArrayList<double []> sourceValuesList = new ArrayList<double []>();
		for (int chan = 0; chan < workImage.getSizeC(); chan++)  {
			double [] sourceValues = Array1DUtil.arrayToDoubleArray(workImage.getDataXY(chan), workImage.isSignedDataType()); 
			sourceValuesList.add(sourceValues);
		}
		return sourceValuesList;
	}
	
	private void initArraysToBuildKymographImages(Experiment exp) {
		SequenceCamData seqCamData = exp.seqCamData;
		int sizex = seqCamData.seq.getSizeX();
		int sizey = seqCamData.seq.getSizeY();	
		int numC = seqCamData.seq.getSizeC();
		if (numC <= 0)
			numC = 3;
		double fimagewidth =  1 + (exp.getKymoFrameEnd() - exp.getKymoFrameStart() )/options.stepFrame;
		if (fimagewidth < 0) {
			options.stepFrame = 1;
			exp.setKymoFrameStep (options.stepFrame);
			fimagewidth =  1 + (exp.getKymoFrameEnd() - exp.getKymoFrameStart() )/options.stepFrame;
		}
			
		int imagewidth = (int) fimagewidth;
		dataType = seqCamData.seq.getDataType_();
		if (dataType.toString().equals("undefined"))
			dataType = DataType.UBYTE;

		int nbcapillaries = exp.capillaries.capillariesArrayList.size();
		int masksizeMax = 0;
		for (int t=0; t < nbcapillaries; t++) {
			Capillary cap = exp.capillaries.capillariesArrayList.get(t);
			cap.masksList = new ArrayList<ArrayList<int[]>>();
			initExtractionParametersfromROI(cap.roi, cap.masksList, options.diskRadius, sizex, sizey);
			if (cap.masksList.size() > masksizeMax)
				masksizeMax = cap.masksList.size();
		}
		
		for (int t=0; t < nbcapillaries; t++) {
			Capillary cap = exp.capillaries.capillariesArrayList.get(t);
			cap.bufKymoImage = new IcyBufferedImage(imagewidth, masksizeMax, numC, dataType);
			cap.tabValuesList = new ArrayList <double []>();
			for (int chan = 0; chan < numC; chan++) {
				Object dataArray = cap.bufKymoImage.getDataXY(chan);
				double[] tabValues =  Array1DUtil.arrayToDoubleArray(dataArray, false);
				cap.tabValuesList.add(tabValues);
			}
		} 
	}
	
	private double initExtractionParametersfromROI( ROI2DShape roi, List<ArrayList<int[]>> masks,  double diskRadius, int sizex, int sizey) {
		CubicSmoothingSpline xSpline 	= Util.getXsplineFromROI((ROI2DShape) roi);
		CubicSmoothingSpline ySpline 	= Util.getYsplineFromROI((ROI2DShape) roi);
		double length 					= Util.getSplineLength((ROI2DShape) roi);
		double len = 0;
		while (len < length) {
			ArrayList<int[]> mask = new ArrayList<int[]>();
			double x = xSpline.evaluate(len);
			double y = ySpline.evaluate(len);
			double dx = xSpline.derivative(len);
			double dy = ySpline.derivative(len);
			double ux = dy/Math.sqrt(dx*dx + dy*dy);
			double uy = -dx/Math.sqrt(dx*dx + dy*dy);
			double tt = -diskRadius;
			while (tt <= diskRadius) {
				int xx = (int) Math.round(x + tt*ux);
				int yy = (int) Math.round(y + tt*uy);
				if (xx >= 0 && xx < sizex && yy >= 0 && yy < sizey)
					mask.add(new int[]{xx, yy});
				tt += 1d;
			}
			masks.add(mask);			
			len ++;
		}
		return length;
	}
		
	private void adjustImage(Sequence seqForRegistration, IcyBufferedImage  workImage) {
		seqForRegistration.setImage(1, 0, workImage);
		int referenceChannel = 1;
		int referenceSlice = 0;
		GaspardRigidRegistration.correctTemporalTranslation2D(seqForRegistration, referenceChannel, referenceSlice);
        boolean rotate = GaspardRigidRegistration.correctTemporalRotation2D(seqForRegistration, referenceChannel, referenceSlice);
        if (rotate) 
        	GaspardRigidRegistration.correctTemporalTranslation2D(seqForRegistration, referenceChannel, referenceSlice);
        workImage = seqForRegistration.getLastImage(1);
	}



}