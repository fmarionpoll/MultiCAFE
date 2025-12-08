package plugins.fmp.multicafe.fmp_service;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import icy.image.IcyBufferedImage;
import icy.sequence.Sequence;
import icy.system.SystemUtil;
import icy.system.thread.Processor;
import icy.type.collection.array.Array1DUtil;
import icy.type.geom.Polyline2D;
import plugins.fmp.multicafe.fmp_experiment.Experiment;
import plugins.fmp.multicafe.fmp_experiment.capillaries.Capillary;
import plugins.fmp.multicafe.fmp_experiment.capillaries.CapillaryMeasure;
import plugins.fmp.multicafe.fmp_series.options.BuildSeriesOptions;
import plugins.fmp.multicafe.fmp_tools.Logger;

public class GulpDetector {

	public void detectGulps(Experiment exp, BuildSeriesOptions options) {
		int jitter = 5;
		int firstKymo = 0;
		int lastKymo = exp.getSeqKymos().getSequence().getSizeT() - 1;
		if (!options.detectAllGulps) {
			firstKymo = options.kymoFirst;
			lastKymo = firstKymo;
		}
		exp.getSeqKymos().getSequence().beginUpdate();

		int nframes = lastKymo - firstKymo + 1;
		final Processor processor = new Processor(SystemUtil.getNumberOfCPUs());
		processor.setThreadName("detect_levels");
		processor.setPriority(Processor.NORM_PRIORITY);
		ArrayList<Future<?>> futures = new ArrayList<Future<?>>(nframes);
		futures.clear();

		final Sequence seqAnalyzed = exp.getSeqKymos().getSequence();

		for (int indexCapillary = firstKymo; indexCapillary <= lastKymo; indexCapillary++) {
			final Capillary capi = exp.getCapillaries().getCapillariesList().get(indexCapillary);
			capi.setGulpsOptions(options);
			futures.add(processor.submit(new Runnable() {
				@Override
				public void run() {
					if (options.buildDerivative)
						capi.ptsDerivative = new CapillaryMeasure(capi.getLast2ofCapillaryName() + "_derivative",
								capi.kymographIndex, getDerivativeProfile(seqAnalyzed, capi, jitter));

					if (options.buildGulps) {
						capi.initGulps();
						capi.detectGulps();
					}
				}
			}));
		}

		waitFuturesCompletion(processor, futures);
		exp.saveCapillaries();

		processor.shutdown();
		exp.getSeqKymos().getSequence().endUpdate();
	}

	private void waitFuturesCompletion(Processor processor, ArrayList<Future<?>> futuresArray) {
		while (!futuresArray.isEmpty()) {
			final Future<?> f = futuresArray.get(futuresArray.size() - 1);
			try {
				f.get();
			} catch (ExecutionException e) {
				Logger.error("GulpDetector:waitFuturesCompletion - Execution exception", e);
			} catch (InterruptedException e) {
				Logger.warn("GulpDetector:waitFuturesCompletion - Interrupted exception: " + e.getMessage());
			}
			futuresArray.remove(f);
		}
	}

	private List<Point2D> getDerivativeProfile(Sequence seq, Capillary cap, int jitter) {
		Polyline2D polyline = cap.ptsTop.polylineLevel;
		if (polyline == null)
			return null;

		int z = seq.getSizeZ() - 1;
		int c = 0;
		IcyBufferedImage image = seq.getImage(cap.kymographIndex, z, c);
		List<Point2D> listOfMaxPoints = new ArrayList<>();
		int[] kymoImageValues = Array1DUtil.arrayToIntArray(image.getDataXY(c), image.isSignedDataType());
		int xwidth = image.getSizeX();
		int yheight = image.getSizeY();

		for (int ix = 1; ix < polyline.npoints; ix++) {
			// for each point of topLevelArray, define a bracket of rows to look at
			// ("jitter" = 10)
			int low = (int) polyline.ypoints[ix] - jitter;
			int high = low + 2 * jitter;
			if (low < 0)
				low = 0;
			if (high >= yheight)
				high = yheight - 1;
			int max = kymoImageValues[ix + low * xwidth];
			for (int iy = low + 1; iy < high; iy++) {
				int val = kymoImageValues[ix + iy * xwidth];
				if (max < val)
					max = val;
			}
			listOfMaxPoints.add(new Point2D.Double((double) ix, (double) max));
		}
		return listOfMaxPoints;
	}
}
