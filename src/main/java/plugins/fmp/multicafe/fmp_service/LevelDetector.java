package plugins.fmp.multicafe.fmp_service;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import icy.image.IcyBufferedImage;
import icy.system.SystemUtil;
import icy.system.thread.Processor;
import icy.type.collection.array.Array1DUtil;
import plugins.fmp.multicafe.fmp_experiment.Experiment;
import plugins.fmp.multicafe.fmp_experiment.capillaries.Capillary;
import plugins.fmp.multicafe.fmp_experiment.sequence.SequenceKymos;
import plugins.fmp.multicafe.fmp_series.BuildSeriesOptions;
import plugins.fmp.multicafe.fmp_tools.Logger;
import plugins.fmp.multicafe.fmp_tools.imageTransform.ImageTransformInterface;

public class LevelDetector {

	public void detectLevels(Experiment exp, BuildSeriesOptions options) {
		SequenceKymos seqKymos = exp.getSeqKymos();
		seqKymos.getSequence().removeAllROI();

		int tFirsKymo = options.kymoFirst;
		if (tFirsKymo > seqKymos.getSequence().getSizeT() || tFirsKymo < 0)
			tFirsKymo = 0;
		int tLastKymo = options.kymoLast;
		if (tLastKymo >= seqKymos.getSequence().getSizeT())
			tLastKymo = seqKymos.getSequence().getSizeT() - 1;
		seqKymos.getSequence().beginUpdate();

		int nframes = tLastKymo - tFirsKymo + 1;
		final Processor processor = new Processor(SystemUtil.getNumberOfCPUs());
		processor.setThreadName("detectlevel");
		processor.setPriority(Processor.NORM_PRIORITY);
		ArrayList<Future<?>> futures = new ArrayList<Future<?>>(nframes);
		futures.clear();

		final int jitter = 10;
		final ImageTransformInterface transformPass1 = options.transform01.getFunction();
		final ImageTransformInterface transformPass2 = options.transform02.getFunction();
		final Rectangle searchRect = options.searchArea;
		SequenceLoaderService loader = new SequenceLoaderService();

		for (int tKymo = tFirsKymo; tKymo <= tLastKymo; tKymo++) {
			final Capillary capi = exp.getCapillaries().getCapillariesList().get(tKymo);
			if (!options.detectR && capi.getKymographName().endsWith("2"))
				continue;
			if (!options.detectL && capi.getKymographName().endsWith("1"))
				continue;

			capi.kymographIndex = tKymo;
			capi.ptsDerivative.clear();
			capi.ptsGulps.gulps.clear();
			capi.limitsOptions.copyFrom(options);
			final IcyBufferedImage rawImage = loader
					.imageIORead(seqKymos.getFileNameFromImageList(capi.kymographIndex));

			futures.add(processor.submit(new Runnable() {
				@Override
				public void run() {
					int imageWidth = rawImage.getSizeX();
					int imageHeight = rawImage.getSizeY();

					if (options.pass1)
						detectPass1(rawImage, transformPass1, capi, imageWidth, imageHeight, searchRect, jitter,
								options);

					if (options.pass2)
						detectPass2(rawImage, transformPass2, capi, imageWidth, imageHeight, searchRect, jitter,
								options);

					int columnFirst = (int) searchRect.getX();
					int columnLast = (int) (searchRect.getWidth() + columnFirst);
					if (options.analyzePartOnly) {
						capi.ptsTop.polylineLevel.insertYPoints(capi.ptsTop.limit, columnFirst, columnLast);
						if (capi.ptsBottom.limit != null)
							capi.ptsBottom.polylineLevel.insertYPoints(capi.ptsBottom.limit, columnFirst, columnLast);
					} else {
						capi.ptsTop.setPolylineLevelFromTempData(capi.getLast2ofCapillaryName() + "_toplevel",
								capi.kymographIndex, columnFirst, columnLast);
						if (capi.ptsBottom.limit != null)
							capi.ptsBottom.setPolylineLevelFromTempData(capi.getLast2ofCapillaryName() + "_bottomlevel",
									capi.kymographIndex, columnFirst, columnLast);
					}
					capi.ptsTop.limit = null;
					capi.ptsBottom.limit = null;
				}
			}));
		}
		waitFuturesCompletion(processor, futures);

		exp.saveCapillaries();
		seqKymos.getSequence().endUpdate();
	}

	private void waitFuturesCompletion(Processor processor, ArrayList<Future<?>> futuresArray) {
		while (!futuresArray.isEmpty()) {
			final Future<?> f = futuresArray.get(futuresArray.size() - 1);
			try {
				f.get();
			} catch (ExecutionException e) {
				Logger.error("LevelDetector:waitFuturesCompletion - Execution exception", e);
			} catch (InterruptedException e) {
				Logger.warn("LevelDetector:waitFuturesCompletion - Interrupted exception: " + e.getMessage());
			}
			futuresArray.remove(f);
		}
	}

	private void detectPass1(IcyBufferedImage rawImage, ImageTransformInterface transformPass1, Capillary capi,
			int imageWidth, int imageHeight, Rectangle searchRect, int jitter, BuildSeriesOptions options) {
		IcyBufferedImage transformedImage1 = transformPass1.getTransformedImage(rawImage, null);
		Object transformedArray1 = transformedImage1.getDataXY(0);
		int[] transformed1DArray1 = Array1DUtil.arrayToIntArray(transformedArray1,
				transformedImage1.isSignedDataType());

		int topSearchFrom = 0;
		int columnFirst = (int) searchRect.getX();
		int columnLast = (int) (searchRect.getWidth() + columnFirst);

		int n_measures = columnLast - columnFirst + 1;
		capi.ptsTop.limit = new int[n_measures];
		capi.ptsBottom.limit = new int[n_measures];

		if (options.runBackwards)
			for (int ix = columnLast; ix >= columnFirst; ix--)
				topSearchFrom = detectLimitOnOneColumn(ix, columnFirst, topSearchFrom, jitter, imageWidth, imageHeight,
						capi, transformed1DArray1, searchRect, options);
		else
			for (int ix = columnFirst; ix <= columnLast; ix++)
				topSearchFrom = detectLimitOnOneColumn(ix, columnFirst, topSearchFrom, jitter, imageWidth, imageHeight,
						capi, transformed1DArray1, searchRect, options);
	}

	private int detectLimitOnOneColumn(int ix, int istart, int topSearchFrom, int jitter, int imageWidth,
			int imageHeight, Capillary capi, int[] transformed1DArray1, Rectangle searchRect,
			BuildSeriesOptions options) {
		int iyTop = detectThresholdFromTop(ix, topSearchFrom, jitter, transformed1DArray1, imageWidth, imageHeight,
				options, searchRect);
		int iyBottom = detectThresholdFromBottom(ix, jitter, transformed1DArray1, imageWidth, imageHeight, options,
				searchRect);
		if (iyBottom <= iyTop)
			iyTop = topSearchFrom;
		capi.ptsTop.limit[ix - istart] = iyTop;
		capi.ptsBottom.limit[ix - istart] = iyBottom;
		return iyTop;
	}

	private void detectPass2(IcyBufferedImage rawImage, ImageTransformInterface transformPass2, Capillary capi,
			int imageWidth, int imageHeight, Rectangle searchRect, int jitter, BuildSeriesOptions options) {
		if (capi.ptsTop.limit == null)
			capi.ptsTop.setTempDataFromPolylineLevel();

		IcyBufferedImage transformedImage2 = transformPass2.getTransformedImage(rawImage, null);
		Object transformedArray2 = transformedImage2.getDataXY(0);
		int[] transformed1DArray2 = Array1DUtil.arrayToIntArray(transformedArray2,
				transformedImage2.isSignedDataType());
		int columnFirst = (int) searchRect.getX();
		int columnLast = (int) (searchRect.getWidth() + columnFirst);
		switch (options.transform02) {
		case COLORDISTANCE_L1_Y:
		case COLORDISTANCE_L2_Y:
			findBestPosition(capi.ptsTop.limit, columnFirst, columnLast, transformed1DArray2, imageWidth, imageHeight,
					5);
			break;

		case SUBTRACT_1RSTCOL:
		case L1DIST_TO_1RSTCOL:
			detectThresholdUp(capi.ptsTop.limit, columnFirst, columnLast, transformed1DArray2, imageWidth, imageHeight,
					20, options.detectLevel2Threshold);
			break;

		case DERICHE:
			findBestPosition(capi.ptsTop.limit, columnFirst, columnLast, transformed1DArray2, imageWidth, imageHeight,
					5);
			break;

		default:
			break;
		}
	}

	private void findBestPosition(int[] limits, int firstColumn, int lastColumn, int[] transformed1DArray2,
			int imageWidth, int imageHeight, int delta) {
		for (int ix = firstColumn; ix <= lastColumn; ix++) {
			int iy = limits[ix];
			int maxVal = transformed1DArray2[ix + iy * imageWidth];
			int iyVal = iy;
			for (int irow = iy + delta; irow > iy - delta; irow--) {
				if (irow < 0 || irow >= imageHeight)
					continue;

				int val = transformed1DArray2[ix + irow * imageWidth];
				if (val > maxVal) {
					maxVal = val;
					iyVal = irow;
				}
			}
			limits[ix] = iyVal;
		}
	}

	private void detectThresholdUp(int[] limits, int firstColumn, int lastColumn, int[] transformed1DArray2,
			int imageWidth, int imageHeight, int delta, int threshold) {
		for (int ix = firstColumn; ix <= lastColumn; ix++) {
			int iy = limits[ix];
			int iyVal = iy;
			for (int irow = iy + delta; irow > iy - delta; irow--) {
				if (irow < 0 || irow >= imageHeight)
					continue;

				int val = transformed1DArray2[ix + irow * imageWidth];
				if (val > threshold) {
					iyVal = irow;
					break;
				}
			}
			limits[ix] = iyVal;
		}
	}

	private int checkIndexLimits(int rowIndex, int maximumRowIndex) {
		if (rowIndex < 0)
			rowIndex = 0;
		if (rowIndex > maximumRowIndex)
			rowIndex = maximumRowIndex;
		return rowIndex;
	}

	private int detectThresholdFromTop(int ix, int searchFrom, int jitter, int[] tabValues, int imageWidth,
			int imageHeight, BuildSeriesOptions options, Rectangle searchRect) {
		int y = imageHeight - 1;
		searchFrom = checkIndexLimits(searchFrom - jitter, imageHeight - 1);
		if (searchFrom < searchRect.y)
			searchFrom = searchRect.y;

		for (int iy = searchFrom; iy < imageHeight; iy++) {
			boolean flag = false;
			if (options.directionUp1)
				flag = tabValues[ix + iy * imageWidth] > options.detectLevel1Threshold;
			else
				flag = tabValues[ix + iy * imageWidth] < options.detectLevel1Threshold;
			if (flag) {
				y = iy;
				break;
			}
		}
		return y;
	}

	private int detectThresholdFromBottom(int ix, int jitter, int[] tabValues, int imageWidth, int imageHeight,
			BuildSeriesOptions options, Rectangle searchRect) {
		int y = 0;
		int searchFrom = imageHeight - 1;
		if (searchFrom > (searchRect.y + searchRect.height))
			searchFrom = searchRect.y + searchRect.height - 1;

		for (int iy = searchFrom; iy >= 0; iy--) {
			boolean flag = false;
			if (options.directionUp1)
				flag = tabValues[ix + iy * imageWidth] > options.detectLevel1Threshold;
			else
				flag = tabValues[ix + iy * imageWidth] < options.detectLevel1Threshold;
			if (flag) {
				y = iy;
				break;
			}
		}
		return y;
	}

}
