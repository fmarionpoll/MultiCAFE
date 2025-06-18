package plugins.fmp.multicafe.experiment;

import java.util.ArrayList;

import plugins.fmp.multicafe.experiment.cells.Cell;

public class CombinedExperiment extends Experiment {
	ArrayList<Experiment> experimentList = null;
	boolean collateExperiments = false;

//	public CombinedExperiment(Experiment exp) {
//		groupedExperiment = new ArrayList<Experiment>(1);
//		groupedExperiment.add(exp);
//	}

	public CombinedExperiment(Experiment exp, boolean collate) {
		experimentList = new ArrayList<Experiment>(1);
		experimentList.add(exp);
		this.collateExperiments = collate;
		if (collateExperiments)
			setGroupedExperiment(exp);
	}

	public void loadExperimentDescriptors() {
		Experiment expi = experimentList.get(0);
		copyExperimentFields(expi);
		copyOtherExperimentFields(expi);
		firstImage_FileTime = expi.firstImage_FileTime;
		expi = experimentList.get(experimentList.size() - 1);
		lastImage_FileTime = expi.lastImage_FileTime;
		// TODO: load capillaries descriptors and load cells descriptors
		// loadMCCapillaries_Descriptors(filename)
	}

	public void loadExperimentCamFileNames() {
		Experiment expi = experimentList.get(0);
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
		experimentList = new ArrayList<Experiment>(1);
		while (expi != null) {
			experimentList.add(expi);
			expi = expi.chainToNextExperiment;
		}
	}

	public void setSingleExperiment() {
		Experiment expi = experimentList.get(0);
		experimentList = new ArrayList<Experiment>(1);
		experimentList.add(expi);
	}

	public void loadCapillaryMeasures() {
		// convert T into Tms (add time_first? - time_first expi(0))

	}

	public void loadFlyPositions() {
		long time_start_ms = firstImage_FileTime.toMillis();
		Experiment exp = experimentList.get(0);
		exp.initTmsForFlyPositions(time_start_ms);
		cells.cellList.addAll(exp.cells.cellList);

		for (int i = 1; i < experimentList.size(); i++) {
			Experiment expi = experimentList.get(i);
			expi.initTmsForFlyPositions(time_start_ms);
			for (Cell cell : cells.cellList) {
				String cellName = cell.getRoiName();
				for (Cell cellExpi : expi.cells.cellList) {
					if (!cellName.equals(cellExpi.getRoiName()))
						continue;
					cell.addFlyPositionsFromOtherCell(cellExpi);
				}
			}
		}

	}

}
