package plugins.fmp.multicafe.series;

import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import javax.swing.SwingUtilities;

import icy.gui.frame.progress.ProgressFrame;
import icy.image.IcyBufferedImage;
import icy.sequence.Sequence;
import plugins.fmp.multicafe.experiment.Experiment;
import plugins.fmp.multicafe.service.SequenceLoaderService;
import plugins.fmp.multicafe.tools.Logger;
import plugins.fmp.multicafe.tools.ViewerFMP;
import plugins.fmp.multicafe.tools.ImageTransform.ImageTransformInterface;
import plugins.fmp.multicafe.tools.ImageTransform.ImageTransformOptions;
import plugins.kernel.roi.roi2d.ROI2DRectangle;

public abstract class FlyDetect extends BuildSeries {
	public FlyDetectTools find_flies = new FlyDetectTools();
	Sequence seqNegative = null;
	ViewerFMP vNegative = null;

	@Override
	void analyzeExperiment(Experiment exp) {
		if (!loadDrosoTrack(exp))
			return;
		if (!checkBoundsForCages(exp))
			return;

		runFlyDetect(exp);
		exp.cages.orderFlyPositions();
		if (!stopFlag)
			exp.saveCageMeasures();
		exp.getSeqCamData().closeSequence();
		closeSequence(seqNegative);
	}

	protected abstract void runFlyDetect(Experiment exp);

	protected void findFliesInAllFrames(Experiment exp) {
		ProgressFrame progressBar = new ProgressFrame("Detecting flies...");

		ImageTransformOptions transformOptions = setupTransformOptions(exp);
		ImageTransformInterface transformFunction = transformOptions.transformOption.getFunction();

		int t_previous = 0;
		int totalFrames = exp.getSeqCamData().nTotalFrames;
		SequenceLoaderService loader = new SequenceLoaderService();

		for (int index = 0; index < totalFrames; index++) {
			if (stopFlag)
				break;
			int t = index;
			String title = "Frame #" + t + "/" + totalFrames;
			progressBar.setMessage(title);

			IcyBufferedImage workImage = loader.imageIORead(exp.getSeqCamData().getFileNameFromImageList(t));
			updateTransformOptions(exp, t, t_previous, transformOptions);

			IcyBufferedImage negativeImage = transformFunction.getTransformedImage(workImage, transformOptions);
			try {
				seqNegative.beginUpdate();
				seqNegative.setImage(0, 0, negativeImage);
				vNegative.setTitle(title);
				List<Rectangle2D> listRectangles = find_flies.findFlies(negativeImage, t);
				displayRectanglesAsROIs(seqNegative, listRectangles, true);
				seqNegative.endUpdate();
			} catch (Exception e) {
				e.printStackTrace();
			}
			t_previous = t;
		}
		progressBar.close();
	}

	protected abstract ImageTransformOptions setupTransformOptions(Experiment exp);

	protected void updateTransformOptions(Experiment exp, int t, int t_previous, ImageTransformOptions options) {
		// default does nothing
	}

	void displayRectanglesAsROIs(Sequence seq, List<Rectangle2D> listRectangles, boolean eraseOldPoints) {
		if (eraseOldPoints)
			seq.removeAllROI();

		for (Rectangle2D rectangle : listRectangles) {
			ROI2DRectangle flyRectangle = new ROI2DRectangle(rectangle);
			flyRectangle.setColor(Color.YELLOW);
			seq.addROI(flyRectangle);
		}
	}

	void openFlyDetectViewers(Experiment exp) {
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				public void run() {
					seqNegative = newSequence("detectionImage", exp.getSeqCamData().refImage);
					vNegative = new ViewerFMP(seqNegative, false, true);
					vNegative.setVisible(true);
				}
			});
		} catch (InvocationTargetException | InterruptedException e) {
			Logger.error("BuildSeries:openFlyDetectViewers() Failed to open fly detection viewers", e);
		}
	}
}
