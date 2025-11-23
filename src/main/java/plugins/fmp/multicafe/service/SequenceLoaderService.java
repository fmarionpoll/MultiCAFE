package plugins.fmp.multicafe.service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import icy.file.Loader;
import icy.file.SequenceFileImporter;
import icy.image.IcyBufferedImage;
import icy.sequence.Sequence;
import plugins.fmp.multicafe.experiment.ExperimentDirectories;
import plugins.fmp.multicafe.experiment.SequenceCamData;
import plugins.fmp.multicafe.tools.Logger;

public class SequenceLoaderService {

	public boolean loadImages(SequenceCamData seqData) {
		if (seqData.imagesList.size() == 0)
			return false;
		seqData.attachSequence(loadSequenceFromImagesList(seqData.imagesList));
		return (seqData.seq != null);
	}

	public boolean loadFirstImage(SequenceCamData seqData) {
		if (seqData.imagesList.size() == 0)
			return false;
		List<String> dummyList = new ArrayList<String>();
		dummyList.add(seqData.imagesList.get(0));
		seqData.attachSequence(loadSequenceFromImagesList(dummyList));
		return (seqData.seq != null);
	}

	public Sequence loadSequenceFromImagesList(List<String> imagesList) {
		SequenceFileImporter seqFileImporter = Loader.getSequenceFileImporter(imagesList.get(0), true);
		Sequence seq = Loader.loadSequences(seqFileImporter, imagesList, 0, // series index to load
				true, // force volatile
				false, // separate
				false, // auto-order
				false, // directory
				false, // add to recent
				false // show progress
		).get(0);
		return seq;
	}

	public Sequence initSequenceFromFirstImage(List<String> imagesList) {
		SequenceFileImporter seqFileImporter = Loader.getSequenceFileImporter(imagesList.get(0), true);
		Sequence seq = Loader.loadSequence(seqFileImporter, imagesList.get(0), 0, false);
		return seq;
	}

	public void loadImageList(SequenceCamData seqData) {
		List<String> imagesList = ExperimentDirectories.getV2ImagesListFromPath(seqData.imagesDirectory);
		imagesList = ExperimentDirectories.keepOnlyAcceptedNames_List(imagesList, "jpg");
		if (imagesList.size() > 0) {
			seqData.setImagesList(imagesList);
			seqData.attachSequence(loadSequenceFromImagesList(imagesList));
		}
	}

	public IcyBufferedImage imageIORead(String name) {
		BufferedImage image = null;
		try {
			image = ImageIO.read(new File(name));
		} catch (IOException e) {
			Logger.error("SequenceLoaderService:imageIORead() Failed to read image: " + name, e);
		}
		return IcyBufferedImage.createFrom(image);
	}
}

