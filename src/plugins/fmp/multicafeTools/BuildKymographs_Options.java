package plugins.fmp.multicafeTools;

import java.util.ArrayList;

import icy.roi.ROI2D;
import plugins.fmp.multicafeSequence.SequenceCapillaries;

public class BuildKymographs_Options {
	public int 				analyzeStep 		= 1;
	public int 				startFrame 			= 1;
	public int				endFrame 			= 99999999;
	public SequenceCapillaries 	vSequence 			= null;
	public int 				diskRadius 			= 5;
	public boolean 			doRegistration 		= false;
	public ArrayList<ROI2D> listROIStoBuildKymos= new ArrayList<ROI2D> ();
	
}
