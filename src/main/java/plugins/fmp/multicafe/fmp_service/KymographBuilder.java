package plugins.fmp.multicafe.fmp_service;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import icy.file.Saver;
import icy.image.IcyBufferedImage;
import icy.system.SystemUtil;
import icy.system.thread.Processor;
import icy.type.DataType;
import icy.type.collection.array.Array1DUtil;
import loci.formats.FormatException;
import plugins.fmp.multicafe.fmp_experiment.Experiment;
import plugins.fmp.multicafe.fmp_experiment.capillaries.Capillary;
import plugins.fmp.multicafe.fmp_experiment.sequence.SequenceCamData;
import plugins.fmp.multicafe.fmp_service.SequenceLoaderService;
import plugins.fmp.multicafe.fmp_series.options.BuildSeriesOptions;
import plugins.fmp.multicafe.fmp_tools.Comparators;
import plugins.fmp.multicafe.fmp_tools.Logger;
import plugins.fmp.multicafe.fmp_tools.ROI2D.AlongT;
import plugins.fmp.multicafe.fmp_tools.ROI2D.ROI2DUtilities;
import plugins.fmp.multicafe.fmp_tools.polyline.Bresenham;

public class KymographBuilder {

	public boolean buildKymograph(Experiment exp, BuildSeriesOptions options) {
		if (exp.getCapillaries().getList().size() < 1) {
			Logger.warn("KymographBuilder:buildKymo Abort (1): nbcapillaries = 0");
			return false;
		}

		List<Capillary> capillariesList = getCapillariesToProcess(exp, options);
		
		// Initialize all capillaries at once (like legacy version)
		initArraysToBuildKymographImages(exp, options, capillariesList);

		int iToColumn = 0;

		final Processor processor = new Processor(SystemUtil.getNumberOfCPUs());
		processor.setThreadName("buildKymograph");
		processor.setPriority(Processor.NORM_PRIORITY);
		int ntasks = capillariesList.size();
		ArrayList<Future<?>> tasks = new ArrayList<Future<?>>(ntasks);

		tasks.clear();
		SequenceLoaderService loader = new SequenceLoaderService();
		long first = exp.getKymoFirst_ms();
		long last = exp.getKymoLast_ms();
		long step = exp.getKymoBin_ms();

		// Loop through images sequentially (like legacy version - no batching)
		for (long ii_ms = first; ii_ms <= last; ii_ms += step, iToColumn++) {
			int sourceImageIndex = exp.findNearestIntervalWithBinarySearch(ii_ms, 0,
					exp.getSeqCamData().getImageLoader().getNTotalFrames());
			final int fromSourceImageIndex = sourceImageIndex;
			final int kymographColumn = iToColumn;

			final IcyBufferedImage sourceImage = loader
					.imageIORead(exp.getSeqCamData().getFileNameFromImageList(fromSourceImageIndex));

			tasks.add(processor.submit(new Runnable() {
				@Override
				public void run() {
					for (Capillary capi : capillariesList)
						analyzeImageUnderCapillary(sourceImage, capi, fromSourceImageIndex, kymographColumn);
				}
			}));
		}
		waitFuturesCompletion(processor, tasks);

		// Export all capillaries at once (like legacy version - line 74)
		SequenceCamData seqCamData = exp.getSeqCamData();
		int sizeC = seqCamData.getSequence().getSizeC();
		exportCapillaryIntegerArrays_to_Kymograph(exp, capillariesList, sizeC);
		return true;
	}

	public void saveComputation(Experiment exp, BuildSeriesOptions options) {
		if (options.doCreateBinDir)
			exp.setBinSubDirectory(exp.getBinNameFromKymoFrameStep());
		String directory = exp.getDirectoryToSaveResults();
		if (directory == null)
			return;

		List<Capillary> capillariesList = getCapillariesToProcess(exp, options);
		int nframes = capillariesList.size();
		int nCPUs = SystemUtil.getNumberOfCPUs();
		final Processor processor = new Processor(nCPUs);
		processor.setThreadName("buildkymo2");
		processor.setPriority(Processor.NORM_PRIORITY);
		ArrayList<Future<?>> futuresArray = new ArrayList<Future<?>>(nframes);
		futuresArray.clear();

		for (int t = 0; t < nframes; t++) {
			final int t_index = t;
			final Capillary cap = capillariesList.get(t_index);

			futuresArray.add(processor.submit(new Runnable() {
				@Override
				public void run() {
					String filename = directory + File.separator + cap.getKymographFileName();
					File file = new File(filename);
					try {
						Saver.saveImage(cap.getCap_Image(), file, true);
						// Clear image immediately after saving to free memory
						cap.setCap_Image(null);
					} catch (FormatException e) {
						Logger.error("KymographBuilder: Failed to save kymograph (format error): " + filename, e);
					} catch (IOException e) {
						Logger.error("KymographBuilder: Failed to save kymograph (IO error): " + filename, e);
					}
				}
			}));
		}
		waitFuturesCompletion(processor, futuresArray);
		exp.xmlSave_MCExperiment();
	}

	private List<Capillary> getCapillariesToProcess(Experiment exp, BuildSeriesOptions options) {
		// Sort and mark capillaries
		Collections.sort(exp.getCapillaries().getList(), new Comparators.Capillary_ROIName());
		int index = 0;
		List<Capillary> capillariesToProcess = new ArrayList<Capillary>();
		for (Capillary cap : exp.getCapillaries().getList()) {
			int i = cap.getKymographIndex();
			if (i < 0) {
				i = index;
				cap.setKymographIndex(i);
				cap.setKymographFileName(cap.getKymographName() + ".tiff");
				System.out.println(
						"buildkymos - index=" + cap.getKymographIndex() + " name=" + cap.getKymographFileName());
			}
			index++;
			cap.setKymographBuild(i >= options.kymoFirst && i <= options.kymoLast);
			if (cap.getKymographBuild()) {
				capillariesToProcess.add(cap);
			}
		}
		return capillariesToProcess;
	}

	private void waitFuturesCompletion(Processor processor, ArrayList<Future<?>> futuresArray) {
		while (!futuresArray.isEmpty()) {
			final Future<?> f = futuresArray.get(futuresArray.size() - 1);
			try {
				f.get();
			} catch (ExecutionException e) {
				Logger.error("KymographBuilder:waitFuturesCompletion - Execution exception", e);
			} catch (InterruptedException e) {
				Logger.warn("KymographBuilder:waitFuturesCompletion - Interrupted exception: " + e.getMessage());
			}
			futuresArray.remove(f);
		}
	}

	private void analyzeImageUnderCapillary(IcyBufferedImage sourceImage, Capillary cap, int t, int kymographColumn) {
		AlongT kymoROI2DatT = cap.getROI2DKymoAtIntervalT(t);
		int sizeC = sourceImage.getSizeC();
		int kymoImageWidth = cap.getCap_Image().getWidth();

		for (int chan = 0; chan < sizeC; chan++) {
			int[] sourceImageChannel = Array1DUtil.arrayToIntArray(sourceImage.getDataXY(chan),
					sourceImage.isSignedDataType());
			int[] capImageChannel = cap.getCapIntegerArray().get(chan);

			int cnt = 0;
			int sourceImageWidth = sourceImage.getWidth();
			for (ArrayList<int[]> mask : kymoROI2DatT.getMasksList()) {
				int sum = 0;
				for (int[] m : mask)
					sum += sourceImageChannel[m[0] + m[1] * sourceImageWidth];
				if (mask.size() > 0)
					capImageChannel[cnt * kymoImageWidth + kymographColumn] = (int) (sum / mask.size());
				cnt++;
			}
		}
	}

	private void exportCapillaryIntegerArrays_to_Kymograph(Experiment exp, List<Capillary> capillariesList,
			final int sizeC) {
		final Processor processor = new Processor(SystemUtil.getNumberOfCPUs());
		processor.setThreadName("buildKymograph");
		processor.setPriority(Processor.NORM_PRIORITY);
		int nbcapillaries = capillariesList.size();
		ArrayList<Future<?>> tasks = new ArrayList<Future<?>>(nbcapillaries);
		tasks.clear();

		for (int icap = 0; icap < nbcapillaries; icap++) {
			final Capillary cap = capillariesList.get(icap);
			final int indexCap = icap;

			tasks.add(processor.submit(new Runnable() {
				@Override
				public void run() {
					export_One_CapillaryIntegerArray_to_Kymograph(cap, indexCap, sizeC);
				}
			}));
		}

		waitFuturesCompletion(processor, tasks);
	}

	private void export_One_CapillaryIntegerArray_to_Kymograph(Capillary cap, int icap, int sizeC) {
		IcyBufferedImage cap_Image = cap.getCap_Image();
		ArrayList<int[]> cap_Integer = cap.getCapIntegerArray();
		boolean isSignedDataType = cap_Image.isSignedDataType();
		for (int chan = 0; chan < sizeC; chan++) {
			int[] tabValues = cap_Integer.get(chan);
			Object destArray = cap_Image.getDataXY(chan);
			Array1DUtil.intArrayToSafeArray(tabValues, 0, destArray, 0, -1, isSignedDataType, isSignedDataType);
			cap_Image.setDataXY(chan, destArray);
		}
		// Clear integer arrays immediately after export to free memory
		cap.setCapIntegerArray(null);
		// Keep cap_Image for saving
	}

	private void initArraysToBuildKymographImages(Experiment exp, BuildSeriesOptions options, List<Capillary> capillariesList) {
		SequenceCamData seqCamData = exp.getSeqCamData();
		if (seqCamData.getSequence() == null)
			seqCamData.setSequence(exp.getSeqCamData().getImageLoader()
					.initSequenceFromFirstImage(exp.getSeqCamData().getImagesList(true)));
		int sizex = seqCamData.getSequence().getSizeX();
		int sizey = seqCamData.getSequence().getSizeY();

		int kymoImageWidth = (int) ((exp.getKymoLast_ms() - exp.getKymoFirst_ms()) / exp.getKymoBin_ms() + 1);
		int imageHeight = 0;
		for (Capillary cap : capillariesList) {
			for (AlongT capT : cap.getROIsForKymo()) {
				int imageHeight_i = buildMasks(capT, sizex, sizey, options);
				if (imageHeight_i > imageHeight)
					imageHeight = imageHeight_i;
			}
		}
		buildCapInteger(exp, capillariesList, kymoImageWidth, imageHeight);
	}

	private int buildMasks(AlongT capT, int sizex, int sizey, BuildSeriesOptions options) {
		ArrayList<ArrayList<int[]>> masks = new ArrayList<ArrayList<int[]>>();
		getPointsfromROIPolyLineUsingBresenham(ROI2DUtilities.getCapillaryPoints(capT.getRoi()), masks,
				options.diskRadius, sizex, sizey);
		capT.setMasksList(masks);
		return masks.size();
	}

	private void buildCapInteger(Experiment exp, List<Capillary> capillariesList, int imageWidth, int imageHeight) {
		SequenceCamData seqCamData = exp.getSeqCamData();
		int numC = seqCamData.getSequence().getSizeC();
		if (numC <= 0)
			numC = 3;

		DataType dataType = seqCamData.getSequence().getDataType_();
		if (dataType.toString().equals("undefined"))
			dataType = DataType.UBYTE;

		int len = imageWidth * imageHeight;
		int nbcapillaries = capillariesList.size();

		for (int i = 0; i < nbcapillaries; i++) {
			Capillary cap = capillariesList.get(i);
			cap.setCap_Image(new IcyBufferedImage(imageWidth, imageHeight, numC, dataType));
			cap.setCapIntegerArray(new ArrayList<int[]>(numC));

			for (int chan = 0; chan < numC; chan++) {
				int[] tabValues = new int[len];
				cap.getCapIntegerArray().add(tabValues);
			}
		}
	}

	private void getPointsfromROIPolyLineUsingBresenham(ArrayList<Point2D> pointsList, List<ArrayList<int[]>> masks,
			double diskRadius, int sizex, int sizey) {
		ArrayList<int[]> pixels = Bresenham.getPixelsAlongLineFromROI2D(pointsList);
		int idiskRadius = (int) diskRadius;
		for (int[] pixel : pixels)
			masks.add(getAllPixelsAroundPixel(pixel, idiskRadius, sizex, sizey));
	}

	private ArrayList<int[]> getAllPixelsAroundPixel(int[] pixel, int diskRadius, int sizex, int sizey) {
		ArrayList<int[]> maskAroundPixel = new ArrayList<int[]>();
		double m1 = pixel[0];
		double m2 = pixel[1];
		double radiusSquared = diskRadius * diskRadius;
		int minX = clipValueToLimits(pixel[0] - diskRadius, 0, sizex - 1);
		int maxX = clipValueToLimits(pixel[0] + diskRadius, minX, sizex - 1);
		int minY = pixel[1];
		int maxY = pixel[1];

		for (int x = minX; x <= maxX; x++) {
			for (int y = minY; y <= maxY; y++) {
				double dx = x - m1;
				double dy = y - m2;
				double distanceSquared = dx * dx + dy * dy;
				if (distanceSquared <= radiusSquared) {
					maskAroundPixel.add(new int[] { x, y });
				}
			}
		}
		return maskAroundPixel;
	}

	private int clipValueToLimits(int x, int min, int max) {
		if (x < min)
			x = min;
		if (x > max)
			x = max;
		return x;
	}
}
