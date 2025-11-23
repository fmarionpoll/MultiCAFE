package plugins.fmp.multicafe.experiment;

import java.util.Iterator;
import java.util.List;

import icy.roi.ROI2D;
import plugins.fmp.multicafe.experiment.capillaries.Capillaries;
import plugins.fmp.multicafe.experiment.capillaries.Capillary;
import plugins.fmp.multicafe.tools.ROI2D.ROI2DUtilities;
import plugins.kernel.roi.roi2d.ROI2DShape;

public class SequenceKymosUtils {
	public static void transferCamDataROIStoKymo(Experiment exp) {
		if (exp.getSeqKymos() == null) {
			System.out.println("SequenceKymosUtils:transferCamDataROIstoKymo seqkymos null - return");
			return;
		}
		if (exp.getCapillaries() == null) {
			exp.setCapillaries(new Capillaries());
			System.out.println("SequenceKymosUtils:transferCamDataROIstoKymo error: seqkymos.capillaries was null");
		}

		// rois not in cap? add
		List<ROI2D> listROISCap = ROI2DUtilities.getROIs2DContainingString("line", exp.getSeqCamData().seq);
		for (ROI2D roi : listROISCap) {
			boolean found = false;
			for (Capillary cap : exp.getCapillaries().capillariesList) {
				if (cap.getRoi() != null && roi.getName().equals(cap.getRoiName())) {
					found = true;
					break;
				}
			}
			if (!found)
				exp.getCapillaries().capillariesList.add(new Capillary((ROI2DShape) roi));
		}

		// cap with no corresponding roi? remove
		Iterator<Capillary> iterator = exp.getCapillaries().capillariesList.iterator();
		while (iterator.hasNext()) {
			Capillary cap = iterator.next();
			boolean found = false;
			for (ROI2D roi : listROISCap) {
				if (roi.getName().equals(cap.getRoiName())) {
					found = true;
					break;
				}
			}
			if (!found)
				iterator.remove();
		}
	}

	public static void transferKymoCapillariesToCamData(Experiment exp) {
		if (exp.getCapillaries() == null)
			return;
		List<ROI2D> listROISCap = ROI2DUtilities.getROIs2DContainingString("line", exp.getSeqCamData().seq);
		// roi with no corresponding cap? add ROI
		for (Capillary cap : exp.getCapillaries().capillariesList) {
			boolean found = false;
			for (ROI2D roi : listROISCap) {
				if (roi.getName().equals(cap.getRoiName())) {
					found = true;
					break;
				}
			}
			if (!found)
				exp.getSeqCamData().seq.addROI(cap.getRoi());
		}
	}

}
