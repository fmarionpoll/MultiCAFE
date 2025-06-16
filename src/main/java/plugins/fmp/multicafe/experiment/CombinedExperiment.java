package plugins.fmp.multicafe.experiment;

import java.util.ArrayList;

public class CombinedExperiment extends Experiment {
	ArrayList<Experiment> listExperiment = new ArrayList<Experiment>();
	boolean collateExperiments = false;

	public CombinedExperiment(Experiment exp) {
		listExperiment.add(exp);
	}

	public void setCollateExperimentsOption(boolean collateExperiments) {
		this.collateExperiments = collateExperiments;
		if (!collateExperiments)
			setSingleExperiment();
		else
			setAllConnectedExperiments();
	}

	public void loadExperimentDescriptors() {
		Experiment expi = listExperiment.get(0);
		copyExperimentFields(expi);
		firstImage_FileTime = expi.firstImage_FileTime;
		expi = listExperiment.get(listExperiment.size() - 1);
		lastImage_FileTime = expi.lastImage_FileTime;
		// TODO: load capillaries descriptors and load cells descriptors
		// loadMCCapillaries_Descriptors(filename)
	}

	private void setAllConnectedExperiments() {
		Experiment expi = getFirstChainedExperiment(true);
		listExperiment = new ArrayList<Experiment>(1);
		while (expi != null) {
			listExperiment.add(expi);
			expi = expi.chainToNextExperiment;
		}
	}

	private void setSingleExperiment() {
		Experiment expi = listExperiment.get(0);
		listExperiment = new ArrayList<Experiment>(1);
		listExperiment.add(expi);
	}

	public void loadCapillaryMeasures() {
		// convert T into Tms (add time_first? - time_first expi(0))

	}

	public void loadFlyPositions() {
		long time_start_ms = firstImage_FileTime.toMillis();
		for (Experiment expi : listExperiment) {
			expi.initTmsForFlyPositions(time_start_ms);
			cells.cellList.addAll(expi.cells.cellList);
		}
	}

}
