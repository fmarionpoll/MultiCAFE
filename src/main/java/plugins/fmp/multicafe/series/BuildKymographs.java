package plugins.fmp.multicafe.series;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import javax.swing.SwingUtilities;

import icy.file.Saver;
import icy.gui.frame.progress.ProgressFrame;
import icy.gui.viewer.Viewer;
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
import plugins.fmp.multicafe.tools.Bresenham;
import plugins.fmp.multicafe.tools.ROI2D.ROI2DAlongT;
import plugins.fmp.multicafe.tools.ROI2D.ROI2DUtilities;

public class BuildKymographs extends BuildSeries {
	public Sequence seqData = new Sequence();
	private Viewer vData = null;
	ArrayList<IcyBufferedImage> cap_bufKymoImage = null;
	int kymoImageWidth = 0;

	void analyzeExperiment(Experiment exp) {
		loadExperimentDataToBuildKymos(exp);

		openKymoViewers(exp);
		getTimeLimitsOfSequence(exp);
		if (buildKymo(exp))
			saveComputation(exp);

		closeKymoViewers();
		exp.seqKymos.closeSequence();
	}

	private boolean loadExperimentDataToBuildKymos(Experiment exp) {
		boolean flag = exp.loadMCCapillaries_Only();
		exp.seqCamData.seq = exp.seqCamData.initSequenceFromFirstImage(exp.seqCamData.getImagesList(true));
		return flag;
	}

	private void getTimeLimitsOfSequence(Experiment exp) {
		exp.getFileIntervalsFromSeqCamData();
		exp.kymoBin_ms = options.t_Ms_BinDuration;
		if (options.isFrameFixed) {
			exp.kymoFirst_ms = options.t_Ms_First;
			exp.kymoLast_ms = options.t_Ms_Last;
			if (exp.kymoLast_ms + exp.camImageFirst_ms > exp.camImageLast_ms)
				exp.kymoLast_ms = exp.camImageLast_ms - exp.camImageFirst_ms;
		} else {
			exp.kymoFirst_ms = 0;
			exp.kymoLast_ms = exp.camImageLast_ms - exp.camImageFirst_ms;
		}
	}

	private void saveComputation(Experiment exp) {
		if (options.doCreateBinDir)
			exp.setBinSubDirectory(exp.getBinNameFromKymoFrameStep());
		String directory = exp.getDirectoryToSaveResults();
		if (directory == null)
			return;

		ProgressFrame progressBar = new ProgressFrame("Save kymographs");
		int nframes = exp.seqKymos.seq.getSizeT();
		int nCPUs = SystemUtil.getNumberOfCPUs();
		final Processor processor = new Processor(nCPUs);
		processor.setThreadName("buildkymo2");
		processor.setPriority(Processor.NORM_PRIORITY);
		ArrayList<Future<?>> futuresArray = new ArrayList<Future<?>>(nframes);
		futuresArray.clear();
		int t0 = (int) exp.binT0;
		for (int t = t0; t < exp.seqKymos.seq.getSizeT(); t++) {
			final int t_index = t;

			futuresArray.add(processor.submit(new Runnable() {
				@Override
				public void run() {
					Capillary cap = exp.capillaries.capillariesList.get(t_index);
					String filename = directory + File.separator + cap.getKymographName() + ".tiff";
					File file = new File(filename);
					IcyBufferedImage image = exp.seqKymos.getSeqImage(t_index, 0);
					try {
						Saver.saveImage(image, file, true);
					} catch (FormatException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}));
		}
		waitFuturesCompletion(processor, futuresArray, progressBar);
		progressBar.close();
		exp.xmlSave_MCExperiment();
	}

	private boolean buildKymo(Experiment exp) {
		if (exp.capillaries.capillariesList.size() < 1) {
			System.out.println("BuildKymographs:buildKymo Abort (1): nbcapillaries = 0");
			return false;
		}
		SequenceKymos seqKymos = exp.seqKymos;
		seqKymos.seq = new Sequence();
		initArraysToBuildKymographImages(exp);

		threadRunning = true;
		stopFlag = false;

		final int nKymographColumns = (int) ((exp.kymoLast_ms - exp.kymoFirst_ms) / exp.kymoBin_ms + 1);
		int iToColumn = 0;
		exp.build_MsTimeIntervalsArray_From_SeqCamData_FileNamesList();
		int sourceImageIndex = exp.findNearestIntervalWithBinarySearch(exp.kymoFirst_ms, 0,
				exp.seqCamData.nTotalFrames);
		String vDataTitle = new String(" / " + nKymographColumns);
		ProgressFrame progressBar1 = new ProgressFrame("Analyze stack frame ");

		final Processor processor = new Processor(SystemUtil.getNumberOfCPUs());
		processor.setThreadName("buildKymograph");
		processor.setPriority(Processor.NORM_PRIORITY);
		int ntasks = exp.capillaries.capillariesList.size(); //
		ArrayList<Future<?>> tasks = new ArrayList<Future<?>>(ntasks);

		tasks.clear();
		for (long ii_ms = exp.kymoFirst_ms; ii_ms <= exp.kymoLast_ms; ii_ms += exp.kymoBin_ms, iToColumn++) {
			sourceImageIndex = exp.findNearestIntervalWithBinarySearch(ii_ms, 0,
					exp.seqCamData.nTotalFrames);
			final int fromSourceImageIndex = sourceImageIndex;
			final int kymographColumn = iToColumn;
			if (options.concurrentDisplay) {
				IcyBufferedImage sourceImage = loadImageFromIndex(exp, fromSourceImageIndex);
				vData.setTitle("Analyzing frame: " + (fromSourceImageIndex + 1) + vDataTitle);
				seqData.setImage(0, 0, sourceImage);
			}

			final IcyBufferedImage sourceImage = loadImageFromIndex(exp, fromSourceImageIndex);
			progressBar1.setMessage("Analyze frame: " + fromSourceImageIndex + "//" + exp.seqCamData.nTotalFrames);

			tasks.add(processor.submit(new Runnable() {
				@Override
				public void run() {
					for (Capillary capi : exp.capillaries.capillariesList)
						analyzeImageWithCapillary(sourceImage, capi, fromSourceImageIndex, kymographColumn);
				}
			}));

		}
		waitFuturesCompletion(processor, tasks, null);
		progressBar1.close();

		ProgressFrame progressBar2 = new ProgressFrame("Combine results into kymograph");
		int sizeC = seqData.getSizeC();
		exportCapillaryIntegerArrays_to_Kymograph(exp, seqKymos.seq, sizeC);
		progressBar2.close();
		return true;
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

	private IcyBufferedImage loadImageFromIndex(Experiment exp, int indexFromFrame) {
		IcyBufferedImage sourceImage = imageIORead(exp.seqCamData.getFileNameFromImageList(indexFromFrame));
//		if (options.doRegistration ) 
//		{
//			String referenceImageName = exp.seqCamData.getFileNameFromImageList(options.referenceFrame);			
//			IcyBufferedImage referenceImage = imageIORead(referenceImageName);
//			adjustImage(sourceImage, referenceImage);
//		}
		return sourceImage;
	}

	private void exportCapillaryIntegerArrays_to_Kymograph(Experiment exp, Sequence seqKymo, final int sizeC) {
		seqKymo.beginUpdate();

		final Processor processor = new Processor(SystemUtil.getNumberOfCPUs());
		processor.setThreadName("buildKymograph");
		processor.setPriority(Processor.NORM_PRIORITY);
		int nbcapillaries = exp.capillaries.capillariesList.size();
		ArrayList<Future<?>> tasks = new ArrayList<Future<?>>(nbcapillaries);
		tasks.clear();

		for (int icap = 0; icap < nbcapillaries; icap++) {
			final Capillary cap = exp.capillaries.capillariesList.get(icap);
			final IcyBufferedImage cap_Image = cap_bufKymoImage.get(icap);
			final int indexCap = icap;

			tasks.add(processor.submit(new Runnable() {
				@Override
				public void run() {
					export_One_CapillaryIntegerArray_to_Kymograph(seqKymo, indexCap, cap, cap_Image, sizeC);
				}
			}));
		}

		waitFuturesCompletion(processor, tasks, null);

		seqKymo.endUpdate();
	}

	private void export_One_CapillaryIntegerArray_to_Kymograph(Sequence seqKymo, int icap, Capillary cap,
			IcyBufferedImage cap_Image, int sizeC) {
		ArrayList<int[]> cap_Integer = cap.cap_Integer;
		boolean isSignedDataType = cap_Image.isSignedDataType();
		for (int chan = 0; chan < sizeC; chan++) {
			int[] tabValues = cap_Integer.get(chan);
			;
			Object destArray = cap_Image.getDataXY(chan);
			Array1DUtil.intArrayToSafeArray(tabValues, 0, destArray, 0, -1, isSignedDataType, isSignedDataType);
			cap_Image.setDataXY(chan, destArray);
		}
		seqKymo.setImage(icap, 0, cap_Image);
		cap.cap_Integer = null;
	}

	private void initArraysToBuildKymographImages(Experiment exp) {
		SequenceCamData seqCamData = exp.seqCamData;
		if (seqCamData.seq == null)
			seqCamData.seq = exp.seqCamData.initSequenceFromFirstImage(exp.seqCamData.getImagesList(true));
		int sizex = seqCamData.seq.getSizeX();
		int sizey = seqCamData.seq.getSizeY();

		kymoImageWidth = (int) ((exp.kymoLast_ms - exp.kymoFirst_ms) / exp.kymoBin_ms + 1);

		int imageHeight = 0;
		for (Capillary cap : exp.capillaries.capillariesList) {
			for (ROI2DAlongT capT : cap.getROIsForKymo()) {
				int imageHeight_i = buildMasks(capT, sizex, sizey);
				if (imageHeight_i > imageHeight)
					imageHeight = imageHeight_i;
			}
		}
		buildCapInteger(exp, imageHeight);
	}

	private int buildMasks(ROI2DAlongT capT, int sizex, int sizey) {
		ArrayList<ArrayList<int[]>> masks = new ArrayList<ArrayList<int[]>>();
		getPointsfromROIPolyLineUsingBresenham(ROI2DUtilities.getCapillaryPoints(capT.getRoi()), masks,
				options.diskRadius, sizex, sizey);
		capT.setMasksList(masks);
		return masks.size();
	}

	private void buildCapInteger(Experiment exp, int imageHeight) {
		SequenceCamData seqCamData = exp.seqCamData;
		int numC = seqCamData.seq.getSizeC();
		if (numC <= 0)
			numC = 3;

		DataType dataType = seqCamData.seq.getDataType_();
		if (dataType.toString().equals("undefined"))
			dataType = DataType.UBYTE;

		int len = kymoImageWidth * imageHeight;
		int nbcapillaries = exp.capillaries.capillariesList.size();
		cap_bufKymoImage = new ArrayList<IcyBufferedImage>(nbcapillaries);

		for (int i = 0; i < nbcapillaries; i++) {
			IcyBufferedImage cap_Image = new IcyBufferedImage(kymoImageWidth, imageHeight, numC, dataType);
			cap_bufKymoImage.add(cap_Image);

			Capillary cap = exp.capillaries.capillariesList.get(i);
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
		int minY = pixel[1]; // getValueWithinLimits(pixel[1] - diskRadius, 0, sizey-1);
		int maxY = pixel[1]; // getValueWithinLimits(pixel[1] + diskRadius, minY, sizey-1);

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

//	private void adjustImage(IcyBufferedImage workImage, IcyBufferedImage referenceImage) 
//	{
//		int referenceChannel = 0;
//		GaspardRigidRegistration.correctTranslation2D(workImage, referenceImage, referenceChannel);
//        boolean rotate = GaspardRigidRegistration.correctRotation2D(workImage, referenceImage, referenceChannel);
//        if (rotate) 
//        	GaspardRigidRegistration.correctTranslation2D(workImage, referenceImage, referenceChannel);
//	}

	private void closeKymoViewers() {
		closeViewer(vData);
		closeSequence(seqData);
	}

	private void openKymoViewers(Experiment exp) {
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				public void run() {
					seqData = newSequence("analyze stack starting with file " + exp.seqCamData.seq.getName(),
							exp.seqCamData.getSeqImage(0, 0));
					vData = new Viewer(seqData, true);
				}
			});
		} catch (InvocationTargetException | InterruptedException e) {
			e.printStackTrace();
		}
	}

}
