package plugins.fmp.multicafe2.tools.Sequence;

import icy.sequence.Sequence;
import icy.sequence.SequenceUtil;


public abstract class SequenceTransformFunction 
{
	protected void makeSure_Sequence_Has_2_Z_planes(Sequence colorSeq) 
	{
		int dim = colorSeq.getSizeZ();
        
        if (dim < 2) {
			SequenceUtil.addZ(colorSeq, 1, false);
        }
	}
	
	
	
}
