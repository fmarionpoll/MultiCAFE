package plugins.fmp.multicafeSequence;



import java.util.Iterator;
import java.util.List;
import icy.roi.ROI2D;
import plugins.kernel.roi.roi2d.ROI2DShape;


public class SequenceKymosUtils {
	

	public static void transferCamDataROIStoKymo (Experiment exp){
		if (exp.seqKymos == null) {
			System.out.println("seqkymos null - return");
			return;
		}
		if (exp.capillaries == null) {
			exp.capillaries = new Capillaries();
			System.out.println("Error in SequenceKymosUtils:transferCamDataROIstoKymo = seqkymos.capillaries was null");
		}
		
		// rois not in cap? add
		List<ROI2D> listROISCap = exp.seqCamData.get2DLineORPolylineRoisContainingString ("line");
		for (ROI2D roi:listROISCap) {
			boolean found = false;
			for (Capillary cap: exp.capillaries.capillariesArrayList) {
				if (roi.getName().equals(cap.capillaryRoi.getName())) {
					found = true;
					break;
				}
			}
			if (!found)
				exp.capillaries.capillariesArrayList.add(new Capillary((ROI2DShape)roi));
		}
		
		// cap with no corresponding roi? remove
		Iterator<Capillary> iterator = exp.capillaries.capillariesArrayList.iterator();
		while(iterator.hasNext()) {
			Capillary cap = iterator.next();
			boolean found = false;
			for (ROI2D roi:listROISCap) {
				if (roi.getName().equals(cap.capillaryRoi.getName())) {
					found = true;
					break;
				}
			}
			if (!found)
				iterator.remove();
		}
	}
	
	public static void transferKymoCapillariesToCamData (Experiment exp) {
		if (exp.capillaries == null)
			return;
		List<ROI2D> listROISCap = exp.seqCamData.get2DLineORPolylineRoisContainingString ("line");
		// roi with no corresponding cap? add ROI
		for (Capillary cap: exp.capillaries.capillariesArrayList) {
			boolean found = false;
			for (ROI2D roi:listROISCap) {
				if (roi.getName().equals(cap.capillaryRoi.getName())) {
					found = true;
					break;
				}
			}
			if (!found)
				exp.seqCamData.seq.addROI(cap.capillaryRoi);
		}
	}
	
}
