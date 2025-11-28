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
import icy.sequence.DimensionId;
import icy.sequence.Sequence;
import plugins.fmp.multicafe.MultiCAFE;
import plugins.fmp.multicafe.experiment.Experiment;
import plugins.fmp.multicafe.experiment.capillaries.Capillaries;
import plugins.fmp.multicafe.experiment.capillaries.Capillary;
import plugins.fmp.multicafe.experiment1.sequence.SequenceKymos;
import plugins.fmp.multicafe.tools.Directories;
import plugins.fmp.multicafe.tools.ViewerFMP;
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

	// Global window position - shared across all experiments
	private static Rectangle globalKymographViewerBounds = null;

	void init(GridLayout capLayout, MultiCAFE parent0) {
		setLayout(capLayout);
		this.parent0 = parent0;

		FlowLayout layout = new FlowLayout(FlowLayout.LEFT);
		layout.setVgap(0);

		JPanel panel1 = new JPanel(layout);
		panel1.add(new JLabel("bin size"));
		panel1.add(viewsCombo);
		panel1.add(new JLabel(" kymograph from"));
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
				Collections.sort(exp.getCapillaries().getCapillariesList());
				int ncapillaries = exp.getCapillaries().getCapillariesList().size();
				for (int i = 0; i < ncapillaries; i++) {
					Capillary cap = exp.getCapillaries().getCapillariesList().get(i);
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
		Experiment exp = (Experiment) parent0.expListComboLazy.getSelectedItem();
		if (exp == null)
			return;
		Viewer v = exp.getSeqKymos().getSequence().getFirstViewer();
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
		Experiment exp = (Experiment) parent0.expListComboLazy.getSelectedItem();
		if (exp != null) {
			SequenceKymos seqKymographs = exp.getSeqKymos();
			if (seqKymographs == null || seqKymographs.getSequence() == null)
				return;

			// Calculate position before any viewer operations to avoid flickering
			Rectangle initialBounds = calculateKymographViewerBounds(exp);

			ArrayList<Viewer> vList = seqKymographs.getSequence().getViewers();
			if (vList.size() == 0) {
				// Create viewer with visible=false to prevent flickering
				ViewerFMP viewerKymographs = new ViewerFMP(seqKymographs.getSequence(), false, true);

				List<String> list = IcyCanvas.getCanvasPluginNames();
				String pluginName = list.stream().filter(s -> s.contains("Canvas2DWithTransforms")).findFirst()
						.orElse(null);
				viewerKymographs.setCanvas(pluginName);
				viewerKymographs.setRepeat(false);
				viewerKymographs.addListener(this);

				JToolBar toolBar = viewerKymographs.getToolBar();
				Canvas2DWithTransforms canvas = (Canvas2DWithTransforms) viewerKymographs.getCanvas();
				canvas.customizeToolbarStep2(toolBar);

				// Set position before making viewer visible
				if (initialBounds != null) {
					viewerKymographs.setBounds(initialBounds);
					((Canvas2D) viewerKymographs.getCanvas()).setFitToCanvas(false);
				}

				// Now make the viewer visible with the correct position already set
				viewerKymographs.setVisible(true);

				int isel = seqKymographs.getCurrentFrame();
				isel = selectKymographImage(isel);
				selectKymographComboItem(isel);
			} else {
				// Viewer already exists (might have been auto-created by ICY) - reposition it
				// immediately
				Viewer existingViewer = vList.get(0);
				if (initialBounds != null) {
					// Hide viewer, set bounds, then show to avoid flickering
					boolean wasVisible = existingViewer.isVisible();
					if (wasVisible) {
						existingViewer.setVisible(false);
					}
					existingViewer.setBounds(initialBounds);
					if (existingViewer.getCanvas() instanceof Canvas2D) {
						((Canvas2D) existingViewer.getCanvas()).setFitToCanvas(false);
					}
					if (wasVisible) {
						existingViewer.setVisible(true);
					}
				}
				// Ensure listener is added (safe to call even if already added)
				existingViewer.addListener(this);
			}
		}
	}

	private Rectangle calculateKymographViewerBounds(Experiment exp) {
		// Use saved global position if available
		if (globalKymographViewerBounds != null) {
			return globalKymographViewerBounds;
		}

		// Initial positioning logic (original behavior)
		Sequence seqCamData = exp.getSeqCamData().getSequence();
		Viewer viewerCamData = seqCamData.getFirstViewer();
		if (viewerCamData == null)
			return null;

		Sequence seqKymograph = exp.getSeqKymos().getSequence();
		Rectangle rectViewerCamData = viewerCamData.getBounds();
		Rectangle rectImageKymograph = seqKymograph.getBounds2D();
		int desktopwidth = Icy.getMainInterface().getMainFrame().getDesktopWidth();

		Rectangle rectViewerKymograph = (Rectangle) rectViewerCamData.clone();
		rectViewerKymograph.width = (int) rectImageKymograph.getWidth();

		if ((rectViewerKymograph.width + rectViewerKymograph.x) > desktopwidth) {
			rectViewerKymograph.x = 0;
			rectViewerKymograph.y = rectViewerCamData.y + rectViewerCamData.height + 5;
			rectViewerKymograph.width = desktopwidth;
			rectViewerKymograph.height = rectImageKymograph.height;
		} else
			rectViewerKymograph.translate(5 + rectViewerCamData.width, 0);

		return rectViewerKymograph;
	}

	void placeKymoViewerNextToCamViewer(Experiment exp) {
		Sequence seqKymograph = exp.getSeqKymos().getSequence();
		Viewer viewerKymograph = seqKymograph.getFirstViewer();
		if (viewerKymograph == null)
			return;

		// Calculate and set position
		Rectangle bounds = calculateKymographViewerBounds(exp);
		if (bounds != null) {
			viewerKymograph.setBounds(bounds);
			((Canvas2D) viewerKymograph.getCanvas()).setFitToCanvas(false);
		}
	}

	void displayOFF() {
		Experiment exp = (Experiment) parent0.expListComboLazy.getSelectedItem();
		if (exp == null || exp.getSeqKymos() == null)
			return;
		ArrayList<Viewer> vList = exp.getSeqKymos().getSequence().getViewers();
		if (vList.size() > 0) {
			// Save window position before closing
			for (Viewer v : vList) {
				saveKymographViewerPosition(v);
				v.close();
			}
			vList.clear();
		}
	}

	private void saveKymographViewerPosition(Viewer viewer) {
		// Save position globally - this will be reused for all experiments
		if (viewer != null) {
			globalKymographViewerBounds = viewer.getBounds();
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
		Experiment exp = (Experiment) parent0.expListComboLazy.getSelectedItem();
		if (exp == null)
			return selectedImageIndex;

		SequenceKymos seqKymos = exp.getSeqKymos();
		if (seqKymos == null || seqKymos.getSequence() == null)
			return selectedImageIndex;
		if (seqKymos.getSequence().isUpdating())
			return selectedImageIndex;

		if (isel < 0)
			isel = 0;
		if (isel >= seqKymos.getSequence().getSizeT())
			isel = seqKymos.getSequence().getSizeT() - 1;

		seqKymos.getSequence().beginUpdate();
		Viewer v = seqKymos.getSequence().getFirstViewer();
		if (v != null) {
			int icurrent = v.getPositionT();
			if (icurrent != isel)
				v.setPositionT(isel);
			seqKymos.validateRoisAtT(seqKymos.getCurrentFrame());
			seqKymos.setCurrentFrame(isel);
		}
		seqKymos.getSequence().endUpdate();

		selectedImageIndex = seqKymos.getCurrentFrame();
		parent0.paneKymos.tabDisplay.displayROIsAccordingToUserSelection();
		selectCapillary(exp, selectedImageIndex);
		return selectedImageIndex;
	}

	private void selectCapillary(Experiment exp, int isel) {
		Capillaries capillaries = exp.getCapillaries();
		for (Capillary cap : capillaries.getCapillariesList()) {
			if (cap.getRoi() != null) {
				cap.getRoi().setSelected(false);
				Capillary capSel = capillaries.getCapillariesList().get(isel);
				capSel.getRoi().setSelected(true);
			}
		}
	}

	@Override
	public void viewerChanged(ViewerEvent event) {
		if ((event.getType() == ViewerEvent.ViewerEventType.POSITION_CHANGED) && (event.getDim() == DimensionId.T)) {
			Viewer v = event.getSource();
			int t = v.getPositionT();
			if (t >= 0)
				selectKymographComboItem(t);

			String title = kymographsCombo.getItemAt(t) + "  :" + viewsCombo.getSelectedItem() + " s";
			v.setTitle(title);
		}
	}

	@Override
	public void viewerClosed(Viewer viewer) {
		// Save window position before closing (global position, shared across all
		// experiments)
		saveKymographViewerPosition(viewer);
		viewer.removeListener(this);
	}

	public void updateResultsAvailable(Experiment exp) {
		isActionEnabled = false;
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

	private void changeBinSubdirectory(String localString) {
		Experiment exp = (Experiment) parent0.expListComboLazy.getSelectedItem();
		if (exp == null || localString == null || exp.getBinSubDirectory().contains(localString))
			return;

		parent0.expListComboLazy.expListBinSubDirectory = localString;
		exp.setBinSubDirectory(localString);
		exp.getSeqKymos().getSequence().close();
		exp.loadKymographs();
		parent0.paneKymos.updateDialogs(exp);
	}

}
