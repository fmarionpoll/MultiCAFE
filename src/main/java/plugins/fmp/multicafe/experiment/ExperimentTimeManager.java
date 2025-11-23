package plugins.fmp.multicafe.experiment;

import java.nio.file.attribute.FileTime;
import plugins.fmp.multicafe.tools.Logger;

public class ExperimentTimeManager {

	public FileTime firstImage_FileTime;
	public FileTime lastImage_FileTime;

	public long camImageFirst_ms = -1;
	public long camImageLast_ms = -1;
	public long camImageBin_ms = -1;
	public long[] camImages_ms = null;

	public long binT0 = 0;
	public long kymoFirst_ms = 0;
	public long kymoLast_ms = 0;
	public long kymoBin_ms = 60000;

	public void getFileIntervalsFromSeqCamData(SequenceCamData seqCamData, String imagesDirectory) {
		if (seqCamData != null && (camImageFirst_ms < 0 || camImageLast_ms < 0 || camImageBin_ms < 0)) {
			loadFileIntervalsFromSeqCamData(seqCamData, imagesDirectory);
		}
	}

	public void loadFileIntervalsFromSeqCamData(SequenceCamData seqCamData, String imagesDirectory) {
		if (seqCamData != null) {
			seqCamData.setImagesDirectory(imagesDirectory);
			firstImage_FileTime = seqCamData.getFileTimeFromStructuredName(0);
			lastImage_FileTime = seqCamData.getFileTimeFromStructuredName(seqCamData.nTotalFrames - 1);
			if (firstImage_FileTime != null && lastImage_FileTime != null) {
				camImageFirst_ms = firstImage_FileTime.toMillis();
				camImageLast_ms = lastImage_FileTime.toMillis();
				if (seqCamData.nTotalFrames > 1)
					camImageBin_ms = (camImageLast_ms - camImageFirst_ms) / (seqCamData.nTotalFrames - 1);
				else
					camImageBin_ms = 0;
				
				if (camImageBin_ms == 0)
					Logger.warn("ExperimentTimeManager:loadFileIntervalsFromSeqCamData() error / file interval size");
			} else {
				Logger.warn("ExperimentTimeManager:loadFileIntervalsFromSeqCamData() error / file intervals of "
						+ seqCamData.getImagesDirectory());
			}
		}
	}

	public long[] build_MsTimeIntervalsArray_From_SeqCamData_FileNamesList(SequenceCamData seqCamData, long firstImage_ms) {
		camImages_ms = new long[seqCamData.nTotalFrames];
		for (int i = 0; i < seqCamData.nTotalFrames; i++) {
			FileTime image_FileTime = seqCamData.getFileTimeFromStructuredName(i);
			long image_ms = image_FileTime.toMillis() - firstImage_ms;
			camImages_ms[i] = image_ms;
		}
		return camImages_ms;
	}

	public int findNearestIntervalWithBinarySearch(long value, int low, int high) {
		int result = -1;
		if (high - low > 1) {
			int mid = (low + high) / 2;
			if (camImages_ms[mid] > value)
				result = findNearestIntervalWithBinarySearch(value, low, mid);
			else if (camImages_ms[mid] < value)
				result = findNearestIntervalWithBinarySearch(value, mid, high);
			else
				result = mid;
		} else
			result = Math.abs(value - camImages_ms[low]) < Math.abs(value - camImages_ms[high]) ? low : high;
		return result;
	}

	public String getBinNameFromKymoFrameStep() {
		return Experiment.BIN + kymoBin_ms / 1000;
	}
}

