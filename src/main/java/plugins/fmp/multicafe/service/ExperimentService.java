package plugins.fmp.multicafe.service;

import java.util.List;

import plugins.fmp.multicafe.experiment.Experiment;
import plugins.fmp.multicafe.experiment.ExperimentDirectories;
import plugins.fmp.multicafe.experiment.ImageFileDescriptor;
import plugins.fmp.multicafe.experiment.SequenceCamData;
import plugins.fmp.multicafe.experiment.SequenceKymos;

public class ExperimentService {

	public void closeSequences(Experiment exp) {
		if (exp.getSeqKymos() != null) {
			exp.getSeqKymos().closeSequence();
		}
		if (exp.getSeqCamData() != null) {
			exp.getSeqCamData().closeSequence();
		}
		if (exp.seqReference != null) {
			exp.seqReference.close();
		}
	}

	public SequenceCamData openSequenceCamData(Experiment exp) {
		loadImagesForSequenceCamData(exp, exp.getStrImagesDirectory());
		if (exp.getSeqCamData() != null) {
			exp.xmlLoad_MCExperiment();
			exp.getFileIntervalsFromSeqCamData();
		}
		return exp.getSeqCamData();
	}

	private SequenceCamData loadImagesForSequenceCamData(Experiment exp, String filename) {
		String strImagesDirectory = ExperimentDirectories.getImagesDirectoryAsParentFromFileName(filename);
		exp.setStrImagesDirectory(strImagesDirectory);
		List<String> imagesList = ExperimentDirectories.getV2ImagesListFromPath(strImagesDirectory);
		imagesList = ExperimentDirectories.keepOnlyAcceptedNames_List(imagesList, "jpg");
		if (imagesList.size() < 1) {
			exp.setSeqCamData(null);
		} else {
			SequenceCamData seqCamData = new SequenceCamData();
			seqCamData.setImagesList(imagesList);
			seqCamData.attachSequence(seqCamData.loadSequenceFromImagesList(imagesList));
			exp.setSeqCamData(seqCamData);
		}
		return exp.getSeqCamData();
	}

	public boolean loadCamDataImages(Experiment exp) {
		if (exp.getSeqCamData() != null)
			exp.getSeqCamData().loadImages();

		return (exp.getSeqCamData() != null && exp.getSeqCamData().seq != null);
	}

	public boolean loadCamDataCapillaries(Experiment exp) {
		exp.loadMCCapillaries_Only();
		if (exp.getSeqCamData() != null && exp.getSeqCamData().seq != null)
			exp.getCapillaries().transferCapillaryRoiToSequence(exp.getSeqCamData().seq);

		return (exp.getSeqCamData() != null && exp.getSeqCamData().seq != null);
	}

	public boolean loadKymographs(Experiment exp) {
		if (exp.getSeqKymos() == null)
			exp.setSeqKymos(new SequenceKymos());
		List<ImageFileDescriptor> myList = exp.getSeqKymos()
				.loadListOfPotentialKymographsFromCapillaries(exp.getKymosBinFullDirectory(), exp.getCapillaries());
		ImageFileDescriptor.getExistingFileNames(myList);
		return exp.getSeqKymos().loadImagesFromList(myList, true);
	}
}
