package plugins.fmp.multicafe.dlg.kymos;

import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import icy.file.Saver;
import icy.gui.frame.progress.ProgressFrame;
import icy.gui.util.FontUtil;
import icy.image.IcyBufferedImage;
import icy.system.thread.ThreadUtil;
import loci.formats.FormatException;
import plugins.fmp.multicafe.MultiCAFE;
import plugins.fmp.multicafe.experiment.Experiment;
import plugins.fmp.multicafe.experiment.ImageFileDescriptor;
import plugins.fmp.multicafe.experiment.SequenceKymos;
import plugins.fmp.multicafe.experiment.capillaries.Capillary;

public class LoadSave extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = -4381802490262298749L;
	private JButton openButtonKymos = new JButton("Load...");
	private JButton saveButtonKymos = new JButton("Save...");
	private MultiCAFE parent0 = null;

	void init(GridLayout capLayout, MultiCAFE parent0) {
		setLayout(capLayout);
		this.parent0 = parent0;

		JLabel loadsaveText = new JLabel("-> Kymographs (tiff) ", SwingConstants.RIGHT);
		loadsaveText.setFont(FontUtil.setStyle(loadsaveText.getFont(), Font.ITALIC));

		FlowLayout flowLayout = new FlowLayout(FlowLayout.RIGHT);
		flowLayout.setVgap(0);
		JPanel panel1 = new JPanel(flowLayout);
		panel1.add(loadsaveText);
		panel1.add(openButtonKymos);
		panel1.add(saveButtonKymos);
		panel1.validate();
		add(panel1);

		defineActionListeners();
	}

	private void defineActionListeners() {
		openButtonKymos.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null && loadDefaultKymos(exp))
					firePropertyChange("KYMOS_OPEN", false, true);
			}
		});

		saveButtonKymos.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null) {
					String path = exp.getExperimentDirectory();
					saveKymographFiles(path);
					firePropertyChange("KYMOS_SAVE", false, true);
				}
			}
		});
	}

	void saveKymographFiles(String directory) {
		ProgressFrame progress = new ProgressFrame("Save kymographs");
		Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
		if (exp == null)
			return;
		SequenceKymos seqKymos = exp.seqKymos;
		if (directory == null) {
			directory = exp.getDirectoryToSaveResults();
			try {
				Files.createDirectories(Paths.get(directory));
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		String outputpath = directory;
		JFileChooser f = new JFileChooser(outputpath);
		f.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		int returnedval = f.showSaveDialog(null);
		if (returnedval == JFileChooser.APPROVE_OPTION) {
			outputpath = f.getSelectedFile().getAbsolutePath();
			for (int t = 0; t < seqKymos.seq.getSizeT(); t++) {
				Capillary cap = exp.capillaries.capillariesList.get(t);
				progress.setMessage("Save kymograph file : " + cap.getKymographName());
				cap.filenameTIFF = outputpath + File.separator + cap.getKymographName() + ".tiff";
				final File file = new File(cap.filenameTIFF);
				IcyBufferedImage image = seqKymos.getSeqImage(t, 0);
				ThreadUtil.bgRun(new Runnable() {
					@Override
					public void run() {
						try {
							Saver.saveImage(image, file, true);
						} catch (FormatException | IOException e) {
							e.printStackTrace();
						}
						System.out.println(
								"LoadSaveKymos:saveKymographFiles() File " + cap.getKymographName() + " saved ");
					}
				});
			}
		}
		progress.close();
	}

	public boolean loadDefaultKymos(Experiment exp) {
		boolean flag = false;
		SequenceKymos seqKymos = exp.seqKymos;
		if (seqKymos == null || exp.capillaries == null) {
			System.out.println("LoadSaveKymos:loadDefaultKymos() no parent sequence or no capillaries found");
			return flag;
		}

		String localString = parent0.expListCombo.expListBinSubDirectory;
		if (localString == null) {
			exp.checkKymosDirectory(exp.getBinSubDirectory());
			parent0.expListCombo.expListBinSubDirectory = exp.getBinSubDirectory();
		} else
			exp.setBinSubDirectory(localString);

		List<ImageFileDescriptor> myList = exp.seqKymos
				.loadListOfPotentialKymographsFromCapillaries(exp.getKymosBinFullDirectory(), exp.capillaries);
		int nItems = ImageFileDescriptor.getExistingFileNames(myList);

		if (nItems > 0) {
			flag = seqKymos.loadImagesFromList(myList, true);
			parent0.paneKymos.tabDisplay.transferCapillaryNamesToComboBox(exp);
		} else
			seqKymos.closeSequence();
		return flag;
	}

}
