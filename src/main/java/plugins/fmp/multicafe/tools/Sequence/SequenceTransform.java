package plugins.fmp.multicafe.tools.Sequence;

import java.util.ArrayList;
import java.util.concurrent.Future;

import icy.gui.frame.progress.ProgressFrame;
import icy.sequence.Sequence;
import icy.sequence.SequenceUtil;
import icy.system.SystemUtil;
import icy.system.thread.Processor;

public class SequenceTransform extends SequenceTransformAbstract {

	@Override
	void runFilter(Sequence seq) {

		int zChannelDestination = 1;
		buildFilteredSequence(seq, 0, zChannelDestination, options.transform01);
		
	}
	
	void buildFilteredSequence(Sequence seq, 
			int zChannelSource, 
			int zChannelDestination, 
			SequenceTransformEnums transformop1) 
	{

		int nimages = seq.getSizeT();
		int zDimensions = seq.getSizeZ();
		if (zDimensions <= 1) 
			SequenceUtil.addZ(seq, 1, false);
		openSequenceViewer(seq);
		temporaryViewer.setPositionZ(zChannelDestination);
		seq.beginUpdate();
		
		SequenceTransformInterface transform = transformop1.getFunction();
		if (transform == null)
			return;
		
		ProgressFrame progressBar = new ProgressFrame("Build filtered images");
		
		int nframes = seq.getSizeT();
		int nCPUs = SystemUtil.getNumberOfCPUs();
	    final Processor processor = new Processor(nCPUs);
	    processor.setThreadName("buildFilteredImages");
	    processor.setPriority(Processor.NORM_PRIORITY);
        ArrayList<Future<?>> futuresArray = new ArrayList<Future<?>>(nframes);
		futuresArray.clear();
		
		for (int t = 0; t < nimages; t++) 
		{
			final int t_index = t;
			futuresArray.add(processor.submit(new Runnable () {
				@Override
				public void run() {	
					transform.getTransformedSequence (seq, t_index, options);
					temporaryViewer.setPositionT(t_index);
				}}));
		}
		
		waitFuturesCompletion(processor, futuresArray, progressBar);
		closeSequenceViewer();
		seq.endUpdate();
		
		progressBar.close();
	}
	

}
