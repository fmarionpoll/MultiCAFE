package plugins.fmp.multicafe.fmp_service;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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
import plugins.fmp.multicafe.fmp_series.options.BuildSeriesOptions;
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

		// Safety check: warn if there are too many capillaries
		if (exp.getCapillaries().getList().size() > 100) {
			Logger.warn("KymographBuilder:buildCapInteger - Warning: Processing "
					+ exp.getCapillaries().getList().size() + " capillaries. This may cause memory issues.");
			return false;
		}

		initArraysToBuildKymographImages(exp, options);

		int iToColumn = 0;

		final Processor processor = new Processor(SystemUtil.getNumberOfCPUs());
		processor.setThreadName("buildKymograph");
		processor.setPriority(Processor.NORM_PRIORITY);
		int ntasks = exp.getCapillaries().getList().size();
		ArrayList<Future<?>> futures = new ArrayList<Future<?>>(ntasks);
		futures.clear();

		SequenceLoaderService loader = new SequenceLoaderService();
		long first = exp.getKymoFirst_ms();
		long last = exp.getKymoLast_ms();
		long step = exp.getKymoBin_ms();

		for (long ii_ms = first; ii_ms <= last; ii_ms += step, iToColumn++) {
			int sourceImageIndex = exp.findNearestIntervalWithBinarySearch(ii_ms, 0,
					exp.getSeqCamData().getImageLoader().getNTotalFrames());
			final int fromSourceImageIndex = sourceImageIndex;
			final int kymographColumn = iToColumn;
			String fullPath = exp.getSeqCamData().getFileNameFromImageList(fromSourceImageIndex);
			final IcyBufferedImage sourceImage = loader.imageIORead(fullPath);

			futures.add(processor.submit(new Runnable() {
				@Override
				public void run() {
					for (Capillary capi : exp.getCapillaries().getList()) {
						if (capi.kymographBuild)
							analyzeImageUnderCapillary(sourceImage, capi, fromSourceImageIndex, kymographColumn);
					}
				}
			}));
		}
		waitFuturesCompletion(processor, futures);

		int sizeC = exp.getSeqCamData().getSequence().getSizeC();
		export_Kymographs_to_file(exp, sizeC);
		return true;
	}

	public void saveComputation(Experiment exp, BuildSeriesOptions options) {
		if (options.doCreateBinDir)
			exp.setBinSubDirectory(exp.getBinNameFromKymoFrameStep());
		String directory = exp.getDirectoryToSaveResults();
		if (directory == null)
			return;

		int nframes = exp.getSeqKymos().getSequence().getSizeT();
		int nCPUs = SystemUtil.getNumberOfCPUs();
		final Processor processor = new Processor(nCPUs);
		processor.setThreadName("buildkymo2");
		processor.setPriority(Processor.NORM_PRIORITY);
		ArrayList<Future<?>> futuresArray = new ArrayList<Future<?>>(nframes);
		futuresArray.clear();

		for (Capillary cap : exp.getCapillaries().getList()) {
			if (!cap.kymographBuild)
				continue;

			final String filename = directory + File.separator + cap.kymographFilename;
			cap.kymographFilename = filename;
			final Capillary capi = cap;
			futuresArray.add(processor.submit(new Runnable() {
				@Override
				public void run() {
					File file = new File(filename);
					try {
						Saver.saveImage(capi.cap_Image, file, true);
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
		int kymoImageWidth = cap.cap_Image.getWidth();

		for (int chan = 0; chan < sizeC; chan++) {
			int[] sourceImageChannel = Array1DUtil.arrayToIntArray(sourceImage.getDataXY(chan),
					sourceImage.isSignedDataType());
			int[] capImageChannel = cap.getCapInteger().get(chan);

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

	private void export_Kymographs_to_file(Experiment exp, final int sizeC) {

		final Processor processor = new Processor(SystemUtil.getNumberOfCPUs());
		processor.setThreadName("buildKymograph");
		processor.setPriority(Processor.NORM_PRIORITY);
		int nbcapillaries = exp.getCapillaries().getList().size();
		ArrayList<Future<?>> tasks = new ArrayList<Future<?>>(nbcapillaries);
		tasks.clear();

		for (int iicap = 0; iicap < nbcapillaries; iicap++) {
			final Capillary cap = exp.getCapillaries().getList().get(iicap);
			if (!cap.kymographBuild)
				continue;
			tasks.add(processor.submit(new Runnable() {
				@Override
				public void run() {
					build_capillaryImage_from_capInteger(cap, sizeC);
				}
			}));
		}

		waitFuturesCompletion(processor, tasks);
	}

	private void build_capillaryImage_from_capInteger(final Capillary cap, final int sizeC) {
		if (cap.cap_Image == null) {
			return;
		}

		ArrayList<int[]> cap_Integer = cap.getCapInteger();
		boolean isSignedDataType = cap.cap_Image.isSignedDataType();
		for (int chan = 0; chan < sizeC; chan++) {
			int[] tabValues = cap_Integer.get(chan);
			Object destArray = cap.cap_Image.getDataXY(chan);
			Array1DUtil.intArrayToSafeArray(tabValues, 0, destArray, 0, -1, isSignedDataType, isSignedDataType);
			cap.cap_Image.setDataXY(chan, destArray);
		}
	}

	private void initArraysToBuildKymographImages(Experiment exp, BuildSeriesOptions options) {
		SequenceCamData seqCamData = exp.getSeqCamData();
		if (seqCamData.getSequence() == null)
			seqCamData.setSequence(exp.getSeqCamData().getImageLoader()
					.initSequenceFromFirstImage(exp.getSeqCamData().getImagesList(true)));
		int sizex = seqCamData.getSequence().getSizeX();
		int sizey = seqCamData.getSequence().getSizeY();

		int kymoImageWidth = (int) ((exp.getKymoLast_ms() - exp.getKymoFirst_ms()) / exp.getKymoBin_ms() + 1);
		for (Capillary cap : exp.getCapillaries().getList()) {
			int i = cap.kymographIndex;
			cap.kymographBuild = (i >= options.kymoFirst && i <= options.kymoLast);
			if (!cap.kymographBuild) {
				continue;
			}
			int imageHeight = 0;
			for (AlongT capT : cap.getROIsForKymo()) {
				int imageHeight_i = buildMasks(capT, sizex, sizey, options);
				if (imageHeight_i > imageHeight)
					imageHeight = imageHeight_i;
			}
			buildCapInteger(exp, cap, kymoImageWidth, imageHeight);
		}
	}

	private int buildMasks(AlongT capT, int sizex, int sizey, BuildSeriesOptions options) {
		ArrayList<ArrayList<int[]>> masks = new ArrayList<ArrayList<int[]>>();
		getPointsfromROIPolyLineUsingBresenham(ROI2DUtilities.getCapillaryPoints(capT.getRoi()), masks,
				options.diskRadius, sizex, sizey);
		capT.setMasksList(masks);
		return masks.size();
	}

	private void buildCapInteger(Experiment exp, Capillary cap, int imageWidth, int imageHeight) {
		SequenceCamData seqCamData = exp.getSeqCamData();
		int numC = seqCamData.getSequence().getSizeC();
		if (numC <= 0)
			numC = 3;

		DataType dataType = seqCamData.getSequence().getDataType_();
		if (dataType.toString().equals("undefined"))
			dataType = DataType.UBYTE;
		int len = imageWidth * imageHeight;

		cap.cap_Image = new IcyBufferedImage(imageWidth, imageHeight, numC, dataType);
		cap.setCapInteger(new ArrayList<int[]>(numC));
		for (int chan = 0; chan < numC; chan++) {
			int[] tabValues = new int[len];
			cap.getCapInteger().add(tabValues);
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
