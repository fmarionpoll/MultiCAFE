package plugins.fmp.multicafe.dlg.kymos;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;

import icy.canvas.Canvas2D;
import icy.canvas.IcyCanvas;
import icy.canvas.Layer;
import icy.gui.viewer.Viewer;
import icy.gui.viewer.ViewerEvent;
import icy.gui.viewer.ViewerListener;
import icy.main.Icy;
import icy.roi.ROI;
import icy.sequence.Sequence;

import plugins.fmp.multicafe.MultiCAFE;
import plugins.fmp.multicafe.experiment.Experiment;
import plugins.fmp.multicafe.experiment.SequenceKymos;
import plugins.fmp.multicafe.experiment.capillaries.Capillaries;
import plugins.fmp.multicafe.experiment.capillaries.Capillary;
import plugins.fmp.multicafe.tools.Directories;
import plugins.fmp.multicafe.tools.Canvas2D.Canvas2DWithTransforms;

public class Display extends JPanel implements ViewerListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = -2103052112476748890L;

	public int indexImagesCombo = -1;
	JComboBox<String> kymographsCombo = new JComboBox<String>(new String[] { "none" });
	JComboBox<String> viewsCombo = new JComboBox<String>();
	JButton previousButton = new JButton("<");
	JButton nextButton = new JButton(">");
	JCheckBox viewLevelsCheckbox = new JCheckBox("top/bottom level (green)", true);
	JCheckBox viewDerivativeCheckbox = new JCheckBox("derivative (yellow)", true);
	JCheckBox viewGulpsCheckbox = new JCheckBox("gulps (red)", true);

	private MultiCAFE parent0 = null;
	private boolean isActionEnabled = true;

	void init(GridLayout capLayout, MultiCAFE parent0) {
		setLayout(capLayout);
		this.parent0 = parent0;

		FlowLayout layout = new FlowLayout(FlowLayout.LEFT);
		layout.setVgap(0);

		JPanel panel1 = new JPanel(layout);
		panel1.add(new JLabel("views"));
		panel1.add(viewsCombo);
		panel1.add(new JLabel(" kymograph"));
		int bWidth = 30;
		int bHeight = 21;
		panel1.add(previousButton, BorderLayout.WEST);
		previousButton.setPreferredSize(new Dimension(bWidth, bHeight));
		panel1.add(kymographsCombo, BorderLayout.CENTER);
		nextButton.setPreferredSize(new Dimension(bWidth, bHeight));
		panel1.add(nextButton, BorderLayout.EAST);
		add(panel1);

		JPanel panel2 = new JPanel(layout);
		panel2.add(viewLevelsCheckbox);
		panel2.add(viewDerivativeCheckbox);
		panel2.add(viewGulpsCheckbox);
		add(panel2);

		defineActionListeners();
	}

	private void defineActionListeners() {
		kymographsCombo.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				if (isActionEnabled)
					displayUpdateOnSwingThread();
			}
		});

		viewDerivativeCheckbox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				displayROIs("deriv", viewDerivativeCheckbox.isSelected());
			}
		});

		viewGulpsCheckbox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				displayROIs("gulp", viewGulpsCheckbox.isSelected());
			}
		});

		viewLevelsCheckbox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				displayROIs("level", viewLevelsCheckbox.isSelected());
			}
		});

		nextButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				int isel = kymographsCombo.getSelectedIndex() + 1;
				if (isel < kymographsCombo.getItemCount()) {
					isel = selectKymographImage(isel);
					selectKymographComboItem(isel);
				}
			}
		});

		previousButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				int isel = kymographsCombo.getSelectedIndex() - 1;
				if (isel < kymographsCombo.getItemCount()) {
					isel = selectKymographImage(isel);
					selectKymographComboItem(isel);
				}
			}
		});

		viewsCombo.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				String localString = (String) viewsCombo.getSelectedItem();
				if (localString != null && localString.contains("."))
					localString = null;
				if (isActionEnabled)
					changeBinSubdirectory(localString);
			}
		});

	}

	public void transferCapillaryNamesToComboBox(Experiment exp) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				kymographsCombo.removeAllItems();
				Collections.sort(exp.capillaries.capillariesList);
				int ncapillaries = exp.capillaries.capillariesList.size();
				for (int i = 0; i < ncapillaries; i++) {
					Capillary cap = exp.capillaries.capillariesList.get(i);
					kymographsCombo.addItem(cap.getRoiName());
				}
			}
		});
	}

	public void displayROIsAccordingToUserSelection() {
		displayROIs("deriv", viewDerivativeCheckbox.isSelected());
		displayROIs("gulp", viewGulpsCheckbox.isSelected());
		displayROIs("level", viewLevelsCheckbox.isSelected());
	}

	private void displayROIs(String filter, boolean visible) {
		Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
		if (exp == null)
			return;
		Viewer v = exp.seqKymos.seq.getFirstViewer();
		if (v == null)
			return;
		IcyCanvas canvas = v.getCanvas();
		List<Layer> layers = canvas.getLayers(false);
		if (layers != null) {
			for (Layer layer : layers) {
				ROI roi = layer.getAttachedROI();
				if (roi != null) {
					String cs = roi.getName();
					if (cs.contains(filter))
						layer.setVisible(visible);
				}
			}
		}
	}

	void displayON() {
		Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
		if (exp != null) {
			SequenceKymos seqKymographs = exp.seqKymos;
			if (seqKymographs == null || seqKymographs.seq == null)
				return;

			ArrayList<Viewer> vList = seqKymographs.seq.getViewers();
			if (vList.size() == 0) {
				Viewer viewerKymographs = new Viewer(seqKymographs.seq, true);
				List<String> list = IcyCanvas.getCanvasPluginNames();
				String pluginName = list.stream().filter(s -> s.contains("Canvas2DWithTransforms")).findFirst()
						.orElse(null);
				viewerKymographs.setCanvas(pluginName);
				viewerKymographs.setRepeat(false);
				viewerKymographs.addListener(this);

				JToolBar toolBar = viewerKymographs.getToolBar();
				Canvas2DWithTransforms canvas = (Canvas2DWithTransforms) viewerKymographs.getCanvas();
				canvas.customizeToolbarStep2(toolBar);

				placeKymoViewerNextToCamViewer(exp);

				int isel = seqKymographs.currentFrame;
				isel = selectKymographImage(isel);
				selectKymographComboItem(isel);
			}
		}
	}

	void placeKymoViewerNextToCamViewer(Experiment exp) {
		Sequence seqCamData = exp.seqCamData.seq;
		Viewer viewerCamData = seqCamData.getFirstViewer();
		if (viewerCamData == null)
			return;

		Rectangle rectViewerCamData = viewerCamData.getBounds();
		Sequence seqKymograph = exp.seqKymos.seq;

		Rectangle rectViewerKymograph = (Rectangle) rectViewerCamData.clone();
		Rectangle rectImageKymograph = seqKymograph.getBounds2D();
		int desktopwidth = Icy.getMainInterface().getMainFrame().getDesktopWidth();

		rectViewerKymograph.width = (int) rectImageKymograph.getWidth();

		if ((rectViewerKymograph.width + rectViewerKymograph.x) > desktopwidth) {
			rectViewerKymograph.x = 0;
			rectViewerKymograph.y = rectViewerCamData.y + rectViewerCamData.height + 5;
			rectViewerKymograph.width = desktopwidth;
			rectViewerKymograph.height = rectImageKymograph.height;
		} else
			rectViewerKymograph.translate(5 + rectViewerCamData.width, 0);

		Viewer viewerKymograph = seqKymograph.getFirstViewer();
		if (viewerKymograph == null)
			return;
		viewerKymograph.setBounds(rectViewerKymograph);
		((Canvas2D) viewerKymograph.getCanvas()).setFitToCanvas(false);
	}

	void displayOFF() {
		Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
		if (exp == null || exp.seqKymos == null)
			return;
		ArrayList<Viewer> vList = exp.seqKymos.seq.getViewers();
		if (vList.size() > 0) {
			for (Viewer v : vList)
				v.close();
			vList.clear();
		}
	}

	public void displayUpdateOnSwingThread() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				int isel = selectKymographImage(displayUpdate());
				selectKymographComboItem(isel);
			}
		});
	}

	int displayUpdate() {
		int item = -1;
		if (kymographsCombo.getItemCount() < 1)
			return item;
		displayON();

		item = kymographsCombo.getSelectedIndex();
		if (item < 0) {
			item = indexImagesCombo >= 0 ? indexImagesCombo : 0;
			indexImagesCombo = -1;
		}
		return item;
	}

	private void selectKymographComboItem(int isel) {
		int icurrent = kymographsCombo.getSelectedIndex();
		if (isel >= 0 && isel != icurrent)
			kymographsCombo.setSelectedIndex(isel);
	}

	public int selectKymographImage(int isel) {
		int selectedImageIndex = -1;
		Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
		if (exp == null)
			return selectedImageIndex;

		SequenceKymos seqKymos = exp.seqKymos;
		if (seqKymos == null || seqKymos.seq == null)
			return selectedImageIndex;
		if (seqKymos.seq.isUpdating())
			return selectedImageIndex;

		if (isel < 0)
			isel = 0;
		if (isel >= seqKymos.seq.getSizeT())
			isel = seqKymos.seq.getSizeT() - 1;

		seqKymos.seq.beginUpdate();
		Viewer v = seqKymos.seq.getFirstViewer();
		if (v != null) {
			int icurrent = v.getPositionT();
			if (icurrent != isel)
				v.setPositionT(isel);
			seqKymos.validateRoisAtT(seqKymos.currentFrame);
			seqKymos.currentFrame = isel;
		}
		seqKymos.seq.endUpdate();

		selectedImageIndex = seqKymos.currentFrame;
		parent0.paneKymos.tabDisplay.displayROIsAccordingToUserSelection();
		selectCapillary(exp, selectedImageIndex);
		return selectedImageIndex;
	}

	private void selectCapillary(Experiment exp, int isel) {
		Capillaries capillaries = exp.capillaries;
		for (Capillary cap : capillaries.capillariesList) {
			if (cap.getRoi() != null) {
				cap.getRoi().setSelected(false);
				Capillary capSel = capillaries.capillariesList.get(isel);
				capSel.getRoi().setSelected(true);
			}
		}
	}

	@Override
	public void viewerChanged(ViewerEvent event) {
		if (event.getType() == ViewerEvent.ViewerEventType.POSITION_CHANGED) {
			Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
			if (exp != null) {
				Viewer v = exp.seqKymos.seq.getFirstViewer();
				int t = v.getPositionT();
				t = selectKymographImage(t);
				if (t >= 0)
					selectKymographComboItem(t);
			}
		}
	}

	@Override
	public void viewerClosed(Viewer viewer) {
		viewer.removeListener(this);
	}

	public void updateResultsAvailable(Experiment exp) {
		isActionEnabled = false;
		// isActionEnabled: hack to select the right directory and then add subsequent
		// available dir without calling actionListener
		// see
		// https://stackoverflow.com/questions/13434688/calling-additem-on-an-empty-jcombobox-triggers-actionperformed-event
		// when JComboBox is empty, adding the first item will trigger setSelected(0)
		viewsCombo.removeAllItems();
		List<String> list = Directories.getSortedListOfSubDirectoriesWithTIFF(exp.getExperimentDirectory());
		for (int i = 0; i < list.size(); i++) {
			String dirName = list.get(i);
			if (dirName == null || dirName.contains(Experiment.RESULTS))
				dirName = ".";
			viewsCombo.addItem(dirName);
		}
		isActionEnabled = true;

		String select = exp.getBinSubDirectory();
		if (select == null)
			select = ".";
		viewsCombo.setSelectedItem(select);
	}

	public String getBinSubdirectory() {
		String name = (String) viewsCombo.getSelectedItem();
		if (name != null && !name.contains("bin_"))
			name = null;
		return name;
	}

	private void changeBinSubdirectory(String localString) {
		Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
		if (exp == null || localString == null || exp.getBinSubDirectory().contains(localString))
			return;

		parent0.expListCombo.expListBinSubDirectory = localString;
		exp.setBinSubDirectory(localString);
		exp.seqKymos.seq.close();
		exp.loadKymographs();
		parent0.paneKymos.updateDialogs(exp);
	}

}
