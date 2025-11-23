package plugins.fmp.multicafe.service;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import icy.file.Saver;
import icy.image.IcyBufferedImage;
import icy.sequence.Sequence;
import icy.system.SystemUtil;
import icy.system.thread.Processor;
import icy.type.DataType;
import icy.type.collection.array.Array1DUtil;
import loci.formats.FormatException;
import plugins.fmp.multicafe.experiment.Experiment;
import plugins.fmp.multicafe.experiment.SequenceCamData;
import plugins.fmp.multicafe.experiment.SequenceKymos;
import plugins.fmp.multicafe.experiment.capillaries.Capillary;
import plugins.fmp.multicafe.series.BuildSeriesOptions;
import plugins.fmp.multicafe.tools.Bresenham;
import plugins.fmp.multicafe.tools.Logger;
import plugins.fmp.multicafe.tools.ROI2D.ROI2DAlongT;
import plugins.fmp.multicafe.tools.ROI2D.ROI2DUtilities;

public class KymographBuilder {

	private ArrayList<IcyBufferedImage> cap_bufKymoImage = null;
	private int kymoImageWidth = 0;

	public boolean buildKymograph(Experiment exp, BuildSeriesOptions options) {
		if (exp.getCapillaries().capillariesList.size() < 1) {
			Logger.warn("KymographBuilder:buildKymo Abort (1): nbcapillaries = 0");
			return false;
		}
		SequenceKymos seqKymos = exp.getSeqKymos();
		seqKymos.seq = new Sequence();
		initArraysToBuildKymographImages(exp, options);

//		int nKymographColumns = (int) ((exp.getKymoLast_ms() - exp.getKymoFirst_ms()) / exp.getKymoBin_ms() + 1);
		int iToColumn = 0;

		// TODO: remove dependency on exp.getSeqCamData() for time intervals if
		// possible, or ensure it's loaded
		// exp.build_MsTimeIntervalsArray_From_SeqCamData_FileNamesList(firstImage_FileTime.toMillis());
		// // already done in getTimeLimitsOfSequence?

		final Processor processor = new Processor(SystemUtil.getNumberOfCPUs());
		processor.setThreadName("buildKymograph");
		processor.setPriority(Processor.NORM_PRIORITY);
		int ntasks = exp.getCapillaries().capillariesList.size();
		ArrayList<Future<?>> tasks = new ArrayList<Future<?>>(ntasks);

		tasks.clear();
		SequenceLoaderService loader = new SequenceLoaderService();

		for (long ii_ms = exp.getKymoFirst_ms(); ii_ms <= exp.getKymoLast_ms(); ii_ms += exp
				.getKymoBin_ms(), iToColumn++) {
			int sourceImageIndex = exp.findNearestIntervalWithBinarySearch(ii_ms, 0, exp.getSeqCamData().nTotalFrames);
			final int fromSourceImageIndex = sourceImageIndex;
			final int kymographColumn = iToColumn;

			final IcyBufferedImage sourceImage = loader
					.imageIORead(exp.getSeqCamData().getFileNameFromImageList(fromSourceImageIndex));

			tasks.add(processor.submit(new Runnable() {
				@Override
				public void run() {
					for (Capillary capi : exp.getCapillaries().capillariesList)
						analyzeImageWithCapillary(sourceImage, capi, fromSourceImageIndex, kymographColumn);
				}
			}));
		}
		waitFuturesCompletion(processor, tasks);

		int sizeC = exp.getSeqCamData().seq.getSizeC();
		exportCapillaryIntegerArrays_to_Kymograph(exp, seqKymos.seq, sizeC);
		return true;
	}

	public void saveComputation(Experiment exp, BuildSeriesOptions options) {
		if (options.doCreateBinDir)
			exp.setBinSubDirectory(exp.getBinNameFromKymoFrameStep());
		String directory = exp.getDirectoryToSaveResults();
		if (directory == null)
			return;

		int nframes = exp.getSeqKymos().seq.getSizeT();
		int nCPUs = SystemUtil.getNumberOfCPUs();
		final Processor processor = new Processor(nCPUs);
		processor.setThreadName("buildkymo2");
		processor.setPriority(Processor.NORM_PRIORITY);
		ArrayList<Future<?>> futuresArray = new ArrayList<Future<?>>(nframes);
		futuresArray.clear();
		int t0 = (int) exp.getBinT0();
		for (int t = t0; t < exp.getSeqKymos().seq.getSizeT(); t++) {
			final int t_index = t;

			futuresArray.add(processor.submit(new Runnable() {
				@Override
				public void run() {
					Capillary cap = exp.getCapillaries().capillariesList.get(t_index);
					String filename = directory + File.separator + cap.getKymographName() + ".tiff";
					File file = new File(filename);
					IcyBufferedImage image = exp.getSeqKymos().getSeqImage(t_index, 0);
					try {
						Saver.saveImage(image, file, true);
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

	private void analyzeImageWithCapillary(IcyBufferedImage sourceImage, Capillary cap, int t, int kymographColumn) {
		ROI2DAlongT kymoROI2DatT = cap.getROI2DKymoAtIntervalT(t);
		int sizeC = sourceImage.getSizeC();

		for (int chan = 0; chan < sizeC; chan++) {
			int[] sourceImageChannel = Array1DUtil.arrayToIntArray(sourceImage.getDataXY(chan),
					sourceImage.isSignedDataType());
			int[] capImageChannel = cap.cap_Integer.get(chan);

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

	private void exportCapillaryIntegerArrays_to_Kymograph(Experiment exp, Sequence seqKymo, final int sizeC) {
		seqKymo.beginUpdate();

		final Processor processor = new Processor(SystemUtil.getNumberOfCPUs());
		processor.setThreadName("buildKymograph");
		processor.setPriority(Processor.NORM_PRIORITY);
		int nbcapillaries = exp.getCapillaries().capillariesList.size();
		ArrayList<Future<?>> tasks = new ArrayList<Future<?>>(nbcapillaries);
		tasks.clear();

		for (int icap = 0; icap < nbcapillaries; icap++) {
			final Capillary cap = exp.getCapillaries().capillariesList.get(icap);
			final IcyBufferedImage cap_Image = cap_bufKymoImage.get(icap);
			final int indexCap = icap;

			tasks.add(processor.submit(new Runnable() {
				@Override
				public void run() {
					export_One_CapillaryIntegerArray_to_Kymograph(seqKymo, indexCap, cap, cap_Image, sizeC);
				}
			}));
		}

		waitFuturesCompletion(processor, tasks);
		seqKymo.endUpdate();
	}

	private void export_One_CapillaryIntegerArray_to_Kymograph(Sequence seqKymo, int icap, Capillary cap,
			IcyBufferedImage cap_Image, int sizeC) {
		ArrayList<int[]> cap_Integer = cap.cap_Integer;
		boolean isSignedDataType = cap_Image.isSignedDataType();
		for (int chan = 0; chan < sizeC; chan++) {
			int[] tabValues = cap_Integer.get(chan);
			Object destArray = cap_Image.getDataXY(chan);
			Array1DUtil.intArrayToSafeArray(tabValues, 0, destArray, 0, -1, isSignedDataType, isSignedDataType);
			cap_Image.setDataXY(chan, destArray);
		}
		seqKymo.setImage(icap, 0, cap_Image);
		cap.cap_Integer = null;
	}

	private void initArraysToBuildKymographImages(Experiment exp, BuildSeriesOptions options) {
		SequenceCamData seqCamData = exp.getSeqCamData();
		if (seqCamData.seq == null)
			seqCamData.seq = exp.getSeqCamData().initSequenceFromFirstImage(exp.getSeqCamData().getImagesList(true));
		int sizex = seqCamData.seq.getSizeX();
		int sizey = seqCamData.seq.getSizeY();

		kymoImageWidth = (int) ((exp.getKymoLast_ms() - exp.getKymoFirst_ms()) / exp.getKymoBin_ms() + 1);

		int imageHeight = 0;
		for (Capillary cap : exp.getCapillaries().capillariesList) {
			for (ROI2DAlongT capT : cap.getROIsForKymo()) {
				int imageHeight_i = buildMasks(capT, sizex, sizey, options);
				if (imageHeight_i > imageHeight)
					imageHeight = imageHeight_i;
			}
		}
		buildCapInteger(exp, imageHeight);
	}

	private int buildMasks(ROI2DAlongT capT, int sizex, int sizey, BuildSeriesOptions options) {
		ArrayList<ArrayList<int[]>> masks = new ArrayList<ArrayList<int[]>>();
		getPointsfromROIPolyLineUsingBresenham(ROI2DUtilities.getCapillaryPoints(capT.getRoi()), masks,
				options.diskRadius, sizex, sizey);
		capT.setMasksList(masks);
		return masks.size();
	}

	private void buildCapInteger(Experiment exp, int imageHeight) {
		SequenceCamData seqCamData = exp.getSeqCamData();
		int numC = seqCamData.seq.getSizeC();
		if (numC <= 0)
			numC = 3;

		DataType dataType = seqCamData.seq.getDataType_();
		if (dataType.toString().equals("undefined"))
			dataType = DataType.UBYTE;

		int len = kymoImageWidth * imageHeight;
		int nbcapillaries = exp.getCapillaries().capillariesList.size();
		cap_bufKymoImage = new ArrayList<IcyBufferedImage>(nbcapillaries);

		for (int i = 0; i < nbcapillaries; i++) {
			IcyBufferedImage cap_Image = new IcyBufferedImage(kymoImageWidth, imageHeight, numC, dataType);
			cap_bufKymoImage.add(cap_Image);

			Capillary cap = exp.getCapillaries().capillariesList.get(i);
			cap.cap_Integer = new ArrayList<int[]>(numC);

			for (int chan = 0; chan < numC; chan++) {
				int[] tabValues = new int[len];
				cap.cap_Integer.add(tabValues);
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
