package plugins.fmp.multicafe.series;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import icy.gui.frame.progress.ProgressFrame;
import icy.image.IcyBufferedImage;
import icy.roi.BooleanMask2D;
import icy.system.SystemUtil;
import icy.system.thread.Processor;
import plugins.fmp.multicafe.experiment.Experiment;
import plugins.fmp.multicafe.experiment.cells.Cell;
import plugins.fmp.multicafe.experiment.cells.Cells;
import plugins.kernel.roi.roi2d.ROI2DArea;

public class FlyDetectTools {
	public List<BooleanMask2D> cellMaskList = new ArrayList<BooleanMask2D>();
	public Rectangle rectangleAllCells = null;
	public BuildSeriesOptions options = null;
	public Cells box = null;

	// -----------------------------------------------------

	BooleanMask2D findLargestBlob(ROI2DArea roiAll, BooleanMask2D cellMask) throws InterruptedException {
		if (cellMask == null)
			return null;

		ROI2DArea roi = new ROI2DArea(roiAll.getBooleanMask(true).getIntersection(cellMask));

		// find largest component in the threshold
		int max = 0;
		BooleanMask2D bestMask = null;
		BooleanMask2D roiBooleanMask = roi.getBooleanMask(true);
		for (BooleanMask2D mask : roiBooleanMask.getComponents()) {
			int len = mask.getPoints().length;
			if (options.blimitLow && len < options.limitLow)
				len = 0;
			if (options.blimitUp && len > options.limitUp)
				len = 0;

			// trap condition where only a line is found
			int width = mask.bounds.width;
			int height = mask.bounds.height;
			int ratio = width / height;
			if (width < height)
				ratio = height / width;
			if (ratio > 4)
				len = 0;

			// get largest blob
			if (len > max) {
				bestMask = mask;
				max = len;
			}
		}
		return bestMask;
	}

	public ROI2DArea binarizeImage(IcyBufferedImage img, int threshold) {
		if (img == null)
			return null;
		boolean[] mask = new boolean[img.getSizeX() * img.getSizeY()];
		if (options.btrackWhite) {
			byte[] arrayRed = img.getDataXYAsByte(0);
			byte[] arrayGreen = img.getDataXYAsByte(1);
			byte[] arrayBlue = img.getDataXYAsByte(2);
			for (int i = 0; i < arrayRed.length; i++) {
				float r = (arrayRed[i] & 0xFF);
				float g = (arrayGreen[i] & 0xFF);
				float b = (arrayBlue[i] & 0xFF);
				float intensity = (r + g + b) / 3f;
				mask[i] = (intensity) > threshold;
			}
		} else {
			byte[] arrayChan = img.getDataXYAsByte(options.videoChannel);
			for (int i = 0; i < arrayChan.length; i++)
				mask[i] = (((int) arrayChan[i]) & 0xFF) < threshold;
		}
		BooleanMask2D bmask = new BooleanMask2D(img.getBounds(), mask);
		return new ROI2DArea(bmask);
	}

	public List<Rectangle2D> findFlies(IcyBufferedImage workimage, int t) throws InterruptedException {
		final Processor processor = new Processor(SystemUtil.getNumberOfCPUs());
		processor.setThreadName("detectFlies");
		processor.setPriority(Processor.NORM_PRIORITY);
		ArrayList<Future<?>> futures = new ArrayList<Future<?>>(box.cellList.size());
		futures.clear();

		final ROI2DArea binarizedImageRoi = binarizeImage(workimage, options.threshold);
		List<Rectangle2D> listRectangles = new ArrayList<Rectangle2D>(box.cellList.size());

		for (Cell cell : box.cellList) {
			if (options.detectCell != -1 && cell.getCellNumberInteger() != options.detectCell)
				continue;
			if (cell.cellNFlies < 1)
				continue;

			futures.add(processor.submit(new Runnable() {
				@Override
				public void run() {
					BooleanMask2D bestMask = getBestMask(binarizedImageRoi, cell.cellMask2D);
					Rectangle2D rect = saveBestMask(bestMask, cell, t);
					if (rect != null)
						listRectangles.add(rect);
				}
			}));
		}

		waitDetectCompletion(processor, futures, null);
		processor.shutdown();
		return listRectangles;
	}

	BooleanMask2D getBestMask(ROI2DArea binarizedImageRoi, BooleanMask2D cellMask) {
		BooleanMask2D bestMask = null;
		try {
			bestMask = findLargestBlob(binarizedImageRoi, cellMask);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return bestMask;
	}

	Rectangle2D saveBestMask(BooleanMask2D bestMask, Cell cell, int t) {
		Rectangle2D rect = null;
		if (bestMask != null)
			rect = bestMask.getOptimizedBounds();
		cell.flyPositions.addPositionWithoutRoiArea(t, rect);
		return rect;
	}

	public ROI2DArea binarizeInvertedImage(IcyBufferedImage img, int threshold) {
		if (img == null)
			return null;
		boolean[] mask = new boolean[img.getSizeX() * img.getSizeY()];
		if (options.btrackWhite) {
			byte[] arrayRed = img.getDataXYAsByte(0);
			byte[] arrayGreen = img.getDataXYAsByte(1);
			byte[] arrayBlue = img.getDataXYAsByte(2);
			for (int i = 0; i < arrayRed.length; i++) {
				float r = (arrayRed[i] & 0xFF);
				float g = (arrayGreen[i] & 0xFF);
				float b = (arrayBlue[i] & 0xFF);
				float intensity = (r + g + b) / 3f;
				mask[i] = (intensity < threshold);
			}
		} else {
			byte[] arrayChan = img.getDataXYAsByte(options.videoChannel);
			for (int i = 0; i < arrayChan.length; i++)
				mask[i] = (((int) arrayChan[i]) & 0xFF) > threshold;
		}
		BooleanMask2D bmask = new BooleanMask2D(img.getBounds(), mask);
		return new ROI2DArea(bmask);
	}

	public void initParametersForDetection(Experiment exp, BuildSeriesOptions options) {
		this.options = options;
		exp.cells.detect_nframes = (int) (((exp.cells.detectLast_Ms - exp.cells.detectFirst_Ms)
				/ exp.cells.detectBin_Ms) + 1);
		exp.cells.clearAllMeasures(options.detectCell);
		box = exp.cells;
		box.computeBooleanMasksForCells();
		rectangleAllCells = null;
		for (Cell cell : box.cellList) {
			if (cell.cellNFlies < 1)
				continue;
			Rectangle rect = cell.cellRoi2D.getBounds();
			if (rectangleAllCells == null)
				rectangleAllCells = new Rectangle(rect);
			else
				rectangleAllCells.add(rect);
		}
	}

	protected void waitDetectCompletion(Processor processor, ArrayList<Future<?>> futuresArray,
			ProgressFrame progressBar) {
		int frame = 1;
		int nframes = futuresArray.size();

		while (!futuresArray.isEmpty()) {
			final Future<?> f = futuresArray.get(futuresArray.size() - 1);
			if (progressBar != null)
				progressBar.setMessage("Analyze frame: " + (frame) + "//" + nframes);
			try {
				f.get();
			} catch (ExecutionException e) {
				System.out
						.println("FlyDetectTools:waitDetectCompletion - frame:" + frame + " Execution exception: " + e);
			} catch (InterruptedException e) {
				System.out.println("FlyDetectTools:waitDetectCompletion - Interrupted exception: " + e);
			}
			futuresArray.remove(f);
			frame++;
		}
	}

}
