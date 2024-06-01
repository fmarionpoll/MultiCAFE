package plugins.fmp.multicafe.experiment;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JOptionPane;

import icy.file.FileUtil;
import icy.gui.dialog.LoaderDialog;
import plugins.fmp.multicafe.tools.Directories;
import plugins.fmp.multicafe.tools.JComponents.ExperimentCombo;

public class ExperimentDirectories {
	public String cameraImagesDirectory = null;
	public List<String> cameraImagesList = null;

	public String resultsDirectory = null;
	public String binSubDirectory = null;
	public List<String> kymosImagesList = null;

	public static List<String> keepOnlyAcceptedNames_List(List<String> namesList, String strExtension) {
		int count = namesList.size();
		List<String> outList = new ArrayList<String>(count);
		String ext = strExtension.toLowerCase();
		for (String name : namesList) {
			String nameGeneric = FileUtil.getGenericPath(name);
			if (nameGeneric.toLowerCase().endsWith(ext))
				outList.add(nameGeneric);
		}
		Collections.sort(outList);
		return outList;
	}

	public static List<String> getV2ImagesListFromPath(String strDirectory) {
		List<String> list = new ArrayList<String>();
		Path pathDir = Paths.get(strDirectory);
		if (Files.exists(pathDir)) {
			try (DirectoryStream<Path> stream = Files.newDirectoryStream(pathDir)) {
				for (Path entry : stream) {
					String toAdd = FileUtil.getGenericPath(entry.toString());
					list.add(toAdd);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return list;
	}

	public List<String> getV2ImagesListFromDialog(String strPath) {
		List<String> list = new ArrayList<String>();
		LoaderDialog dialog = new LoaderDialog(false);
		if (strPath != null)
			dialog.setCurrentDirectory(new File(strPath));
		File[] selectedFiles = dialog.getSelectedFiles();
		if (selectedFiles.length == 0)
			return null;

		// TODO check strPath and provide a way to skip the dialog part (or different
		// routine)
		String strDirectory = Directories.getDirectoryFromName(selectedFiles[0].toString());
		if (strDirectory != null) {
			if (selectedFiles.length == 1)
				list = getV2ImagesListFromPath(strDirectory);
		}
		return list;
	}

	public boolean checkCameraImagesList() {
		boolean isOK = false;
		if (!(cameraImagesList == null)) {
			boolean imageFound = false;
			String jpg = "jpg";
			String grabs = "grabs";
			String grabsDirectory = null;
			for (String name : cameraImagesList) {
				if (name.toLowerCase().endsWith(jpg)) {
					imageFound = true;
					break;
				}
				if (name.toLowerCase().endsWith(grabs))
					grabsDirectory = name;
			}
			if (imageFound) {
				cameraImagesList = keepOnlyAcceptedNames_List(cameraImagesList, "jpg");
				isOK = true;
			} else if (grabsDirectory != null) {
				cameraImagesList = getV2ImagesListFromPath(grabsDirectory);
				isOK = checkCameraImagesList();
			}
		}
		return isOK;
	}

	public boolean getDirectoriesFromDialog(ExperimentCombo expListCombo, String rootDirectory, boolean createResults) {
		cameraImagesList = getV2ImagesListFromDialog(rootDirectory);
		if (!checkCameraImagesList())
			return false;

		cameraImagesDirectory = Directories.getDirectoryFromName(cameraImagesList.get(0));

		resultsDirectory = getV2ResultsDirectoryDialog(cameraImagesDirectory, Experiment.RESULTS, createResults);
		binSubDirectory = getV2BinSubDirectory(expListCombo.expListBinSubDirectory, resultsDirectory, null);

		String kymosDir = resultsDirectory + File.separator + binSubDirectory;
		kymosImagesList = getV2ImagesListFromPath(kymosDir);
		kymosImagesList = keepOnlyAcceptedNames_List(kymosImagesList, "tiff");
		// TODO wrong if any bin
		return true;
	}

	public boolean getDirectoriesFromExptPath(ExperimentCombo expListCombo, String exptDirectory,
			String binSubDirectory) {
		String strDirectory = getImagesDirectoryAsParentFromFileName(exptDirectory);
		cameraImagesList = getV2ImagesListFromPath(strDirectory);
		cameraImagesList = keepOnlyAcceptedNames_List(cameraImagesList, "jpg");
		cameraImagesDirectory = strDirectory; // Directories.getDirectoryFromName(cameraImagesList.get(0));

		resultsDirectory = getV2ResultsDirectory(cameraImagesDirectory, exptDirectory);
		this.binSubDirectory = getV2BinSubDirectory(expListCombo.expListBinSubDirectory, resultsDirectory,
				binSubDirectory);

		String kymosDir = resultsDirectory + File.separator + this.binSubDirectory;
		kymosImagesList = getV2ImagesListFromPath(kymosDir);
		kymosImagesList = keepOnlyAcceptedNames_List(kymosImagesList, "tiff");
		// TODO wrong if any bin
		return true;
	}

	public boolean getDirectoriesFromGrabPath(String grabsDirectory) {
		cameraImagesList = getV2ImagesListFromPath(grabsDirectory);
		cameraImagesList = keepOnlyAcceptedNames_List(cameraImagesList, "jpg");
		cameraImagesDirectory = grabsDirectory;

		resultsDirectory = getV2ResultsDirectory(cameraImagesDirectory, Experiment.RESULTS);
		binSubDirectory = getV2BinSubDirectory(null, resultsDirectory, null);

		String kymosDir = resultsDirectory + File.separator + this.binSubDirectory;
		kymosImagesList = getV2ImagesListFromPath(kymosDir);
		kymosImagesList = keepOnlyAcceptedNames_List(kymosImagesList, "tiff");
		// TODO wrong if any bin
		return true;
	}

	private String getV2BinSubDirectory(String expListBinSubDirectory, String resultsDirectory,
			String binSubDirectory) {
		List<String> expList = Directories.getSortedListOfSubDirectoriesWithTIFF(resultsDirectory);
		move_TIFFandLINEfiles_From_Results_to_BinDirectory(resultsDirectory, expList);
		String binDirectory = binSubDirectory;
		if (binDirectory == null) {
			if (expList.size() > 1) {
				if (expListBinSubDirectory == null)
					binDirectory = selectSubDirDialog(expList, "Select item", Experiment.BIN, false);
			} else if (expList.size() == 1) {
				binDirectory = expList.get(0).toLowerCase();
				if (!binDirectory.contains(Experiment.BIN))
					binDirectory = Experiment.BIN + "60";
			} else
				binDirectory = Experiment.BIN + "60";
		}
		if (expListBinSubDirectory != null)
			binDirectory = expListBinSubDirectory;

		move_XML_From_Bin_to_Results(binDirectory, resultsDirectory);

		return binDirectory;
	}

	static public String getParentIf(String filename, String filter) {
		if (filename.contains(filter))
			filename = Paths.get(filename).getParent().toString();
		return filename;
	}

	static public String getImagesDirectoryAsParentFromFileName(String filename) {
		filename = getParentIf(filename, Experiment.BIN);
		filename = getParentIf(filename, Experiment.RESULTS);
		return filename;
	}

	private String getV2ResultsDirectory(String parentDirectory, String resultsSubDirectory) {
		resultsSubDirectory = getParentIf(resultsSubDirectory, Experiment.BIN);

		if (!resultsSubDirectory.contains(Experiment.RESULTS) || !resultsSubDirectory.contains(parentDirectory))
			resultsSubDirectory = parentDirectory + File.separator + Experiment.RESULTS;
		return resultsSubDirectory;
	}

	private String getV2ResultsDirectoryDialog(String parentDirectory, String filter, boolean createResults) {
		List<String> expList = Directories.fetchSubDirectoriesMatchingFilter(parentDirectory, filter);
		expList = Directories.reduceFullNameToLastDirectory(expList);
		String name = null;
		if (createResults || expList.size() > 1) {
			name = selectSubDirDialog(expList, "Select item or type " + Experiment.RESULTS + "xxx", Experiment.RESULTS,
					true);
		} else if (expList.size() == 1)
			name = expList.get(0);
		else
			name = filter;
		return parentDirectory + File.separator + name;
	}

	private void move_TIFFandLINEfiles_From_Results_to_BinDirectory(String parentDirectory, List<String> expList) {
		if (expList == null)
			return;
		for (String subDirectory : expList) {
			if (subDirectory.contains(Experiment.RESULTS)) { // TODO estimate bin size for ex by comparing x size and n
																// jpg files?
				subDirectory = Experiment.BIN + "60";
				Directories.move_TIFFfiles_To_Subdirectory(parentDirectory, subDirectory);
				Directories.move_xmlLINEfiles_To_Subdirectory(parentDirectory, subDirectory, true);
			}
		}
	}

	private void move_XML_From_Bin_to_Results(String binSubDirectory, String resultsDirectory) {
		String binDirectory = resultsDirectory + File.separator + binSubDirectory;
		moveAndRename("MCcapi.xml", binDirectory, "MCcapillaries.xml", resultsDirectory);
		moveAndRename("MCexpe.xml", binDirectory, "MCexperiment.xml", resultsDirectory);
		moveAndRename("MCdros.xml", binDirectory, "MCdrosotrack.xml", resultsDirectory);
	}

	static void moveAndRename(String oldFileName, String oldDirectory, String newFileName, String newDirectory) {
		String oldFilePathString = oldDirectory + File.separator + oldFileName;
		File oldFile = new File(oldFilePathString);
		if (!oldFile.exists() || oldFile.isDirectory())
			return;
		Path oldFilePath = Paths.get(oldFilePathString);

		String newFilePathString = newDirectory + File.separator + newFileName;
		Path newFilePath = Paths.get(newFilePathString);
		if (Files.exists(newFilePath))
			newFilePath = Paths.get(newDirectory + File.separator + oldFileName);

		try {
			Files.move(oldFilePath, newFilePath);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private String selectSubDirDialog(List<String> expList, String title, String type, boolean editable) {
		Object[] array = expList.toArray();
		JComboBox<Object> jcb = new JComboBox<Object>(array);
		jcb.setEditable(editable);
		JOptionPane.showMessageDialog(null, jcb, title, JOptionPane.QUESTION_MESSAGE);
		return (String) jcb.getSelectedItem();
	}

}
