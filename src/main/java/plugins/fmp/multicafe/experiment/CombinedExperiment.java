package plugins.fmp.multicafe.experiment;

import java.util.ArrayList;

public class CombinedExperiment extends Experiment {
	ArrayList<Experiment> groupedExperiment = null;
	boolean collateExperiments = false;

//	public CombinedExperiment(Experiment exp) {
//		groupedExperiment = new ArrayList<Experiment>(1);
//		groupedExperiment.add(exp);
//	}

	public CombinedExperiment(Experiment exp, boolean collate) {
		groupedExperiment = new ArrayList<Experiment>(1);
		groupedExperiment.add(exp);
		this.collateExperiments = collate;
		if (collateExperiments)
			setGroupedExperiment(exp);
	}

	public void loadExperimentDescriptors() {
		Experiment expi = groupedExperiment.get(0);
		copyExperimentFields(expi);
		copyOtherExperimentFields(expi);
		firstImage_FileTime = expi.firstImage_FileTime;
		expi = groupedExperiment.get(groupedExperiment.size() - 1);
		lastImage_FileTime = expi.lastImage_FileTime;
		// TODO: load capillaries descriptors and load cells descriptors
		// loadMCCapillaries_Descriptors(filename)
	}

	public void loadExperimentCamFileNames() {
		Experiment expi = groupedExperiment.get(0);
		while (expi != null) {
			seqCamData.imagesList.addAll(expi.seqCamData.imagesList);
			expi = expi.chainToNextExperiment;
		}
	}

	private void copyOtherExperimentFields(Experiment source) {
		setImagesDirectory(source.getImagesDirectory());
		setExperimentDirectory(source.getExperimentDirectory());
		setBinSubDirectory(source.getBinSubDirectory());
	}

	private void setGroupedExperiment(Experiment exp) {
		Experiment expi = exp.getFirstChainedExperiment(true);
		groupedExperiment = new ArrayList<Experiment>(1);
		while (expi != null) {
			groupedExperiment.add(expi);
			expi = expi.chainToNextExperiment;
		}
	}

	public void setSingleExperiment() {
		Experiment expi = groupedExperiment.get(0);
		groupedExperiment = new ArrayList<Experiment>(1);
		groupedExperiment.add(expi);
	}

	public void loadCapillaryMeasures() {
		// convert T into Tms (add time_first? - time_first expi(0))

	}

	public void loadFlyPositions() {
		long time_start_ms = firstImage_FileTime.toMillis();
		for (Experiment expi : groupedExperiment) {
			expi.initTmsForFlyPositions(time_start_ms);
			cells.cellList.addAll(expi.cells.cellList);
		}
	}

}
