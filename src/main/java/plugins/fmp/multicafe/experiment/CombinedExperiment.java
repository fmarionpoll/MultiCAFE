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
	}

	public void loadExperimentDescriptors() {
		copyExperimentFields(listExperiment.get(0));
	}

	public void loadCapillaryMeasures() {

	}

	public void loadFlyPositions() {

	}
}
