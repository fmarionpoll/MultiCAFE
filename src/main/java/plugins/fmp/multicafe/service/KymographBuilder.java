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
import plugins.fmp.multicafe.experiment.capillaries.Capillary;
import plugins.fmp.multicafe.experiment1.sequence.SequenceCamData;
import plugins.fmp.multicafe.experiment1.sequence.SequenceKymos;
import plugins.fmp.multicafe.series.BuildSeriesOptions;
import plugins.fmp.multicafe.tools.Bresenham;
import plugins.fmp.multicafe.tools.Logger;
import plugins.fmp.multicafe.tools.ROI2D.ROI2DAlongT;
import plugins.fmp.multicafe.tools.ROI2D.ROI2DUtilities;

public class KymographBuilder {

	public boolean buildKymograph(Experiment exp, BuildSeriesOptions options) {
		if (exp.getCapillaries().getCapillariesList().size() < 1) {
			Logger.warn("KymographBuilder:buildKymo Abort (1): nbcapillaries = 0");
			return false;
		}
		SequenceKymos seqKymos = exp.getSeqKymos();
		seqKymos.setSequence(new Sequence());
		initArraysToBuildKymographImages(exp, options);

		int iToColumn = 0;

		final Processor processor = new Processor(SystemUtil.getNumberOfCPUs());
		processor.setThreadName("buildKymograph");
		processor.setPriority(Processor.NORM_PRIORITY);
		int ntasks = exp.getCapillaries().getCapillariesList().size();
		ArrayList<Future<?>> tasks = new ArrayList<Future<?>>(ntasks);

		tasks.clear();
		SequenceLoaderService loader = new SequenceLoaderService();
		long first = exp.getKymoFirst_ms();
		long last = exp.getKymoLast_ms();
		long step = exp.getKymoBin_ms();

		for (long ii_ms = first; ii_ms <= last; ii_ms += step, iToColumn++) {
			int sourceImageIndex = exp.findNearestIntervalWithBinarySearch(ii_ms, 0,
					exp.getSeqCamData().getnTotalFrames());
			final int fromSourceImageIndex = sourceImageIndex;
			final int kymographColumn = iToColumn;

			final IcyBufferedImage sourceImage = loader
					.imageIORead(exp.getSeqCamData().getFileNameFromImageList(fromSourceImageIndex));

			tasks.add(processor.submit(new Runnable() {
				@Override
				public void run() {
					for (Capillary capi : exp.getCapillaries().getCapillariesList())
						analyzeImageWithCapillary(sourceImage, capi, fromSourceImageIndex, kymographColumn);
				}
			}));
		}
		waitFuturesCompletion(processor, tasks);

		int sizeC = exp.getSeqCamData().getSequence().getSizeC();
		exportCapillaryIntegerArrays_to_Kymograph(exp, seqKymos.getSequence(), sizeC);
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
		int t0 = (int) exp.getBinT0();
		for (int t = t0; t < exp.getSeqKymos().getSequence().getSizeT(); t++) {
			final int t_index = t;

			futuresArray.add(processor.submit(new Runnable() {
				@Override
				public void run() {
					Capillary cap = exp.getCapillaries().getCapillariesList().get(t_index);
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
		int kymoImageWidth = cap.cap_Image.getWidth();

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
		int nbcapillaries = exp.getCapillaries().getCapillariesList().size();
		ArrayList<Future<?>> tasks = new ArrayList<Future<?>>(nbcapillaries);
		tasks.clear();

		for (int icap = 0; icap < nbcapillaries; icap++) {
			final Capillary cap = exp.getCapillaries().getCapillariesList().get(icap);
			final int indexCap = icap;

			tasks.add(processor.submit(new Runnable() {
				@Override
				public void run() {
					export_One_CapillaryIntegerArray_to_Kymograph(seqKymo, indexCap, cap, sizeC);
				}
			}));
		}

		waitFuturesCompletion(processor, tasks);
		seqKymo.endUpdate();
	}

	private void export_One_CapillaryIntegerArray_to_Kymograph(Sequence seqKymo, int icap, Capillary cap, int sizeC) {
		IcyBufferedImage cap_Image = cap.cap_Image;
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
		cap.cap_Image = null;
	}

	private void initArraysToBuildKymographImages(Experiment exp, BuildSeriesOptions options) {
		SequenceCamData seqCamData = exp.getSeqCamData();
		if (seqCamData.getSequence() == null)
			seqCamData.setSequence(exp.getSeqCamData().initSequenceFromFirstImage(exp.getSeqCamData().getImagesList(true)));
		int sizex = seqCamData.getSequence().getSizeX();
		int sizey = seqCamData.getSequence().getSizeY();

		int kymoImageWidth = (int) ((exp.getKymoLast_ms() - exp.getKymoFirst_ms()) / exp.getKymoBin_ms() + 1);
		int imageHeight = 0;
		for (Capillary cap : exp.getCapillaries().getCapillariesList()) {
			for (ROI2DAlongT capT : cap.getROIsForKymo()) {
				int imageHeight_i = buildMasks(capT, sizex, sizey, options);
				if (imageHeight_i > imageHeight)
					imageHeight = imageHeight_i;
			}
		}
		buildCapInteger(exp, kymoImageWidth, imageHeight);
	}

	private int buildMasks(ROI2DAlongT capT, int sizex, int sizey, BuildSeriesOptions options) {
		ArrayList<ArrayList<int[]>> masks = new ArrayList<ArrayList<int[]>>();
		getPointsfromROIPolyLineUsingBresenham(ROI2DUtilities.getCapillaryPoints(capT.getRoi()), masks,
				options.diskRadius, sizex, sizey);
		capT.setMasksList(masks);
		return masks.size();
	}

	private void buildCapInteger(Experiment exp, int imageWidth, int imageHeight) {
		SequenceCamData seqCamData = exp.getSeqCamData();
		int numC = seqCamData.getSequence().getSizeC();
		if (numC <= 0)
			numC = 3;

		DataType dataType = seqCamData.getSequence().getDataType_();
		if (dataType.toString().equals("undefined"))
			dataType = DataType.UBYTE;

		int len = imageWidth * imageHeight;
		int nbcapillaries = exp.getCapillaries().getCapillariesList().size();
		// cap_bufKymoImage = new ArrayList<IcyBufferedImage>(nbcapillaries);

		for (int i = 0; i < nbcapillaries; i++) {
			Capillary cap = exp.getCapillaries().getCapillariesList().get(i);
			cap.cap_Image = new IcyBufferedImage(imageWidth, imageHeight, numC, dataType);
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
