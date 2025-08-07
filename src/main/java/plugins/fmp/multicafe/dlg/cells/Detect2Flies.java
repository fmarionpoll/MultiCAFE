package plugins.fmp.multicafe.dlg.cells;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JToggleButton;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import icy.util.StringUtil;
import plugins.fmp.multicafe.MultiCAFE;
import plugins.fmp.multicafe.experiment.Experiment;
import plugins.fmp.multicafe.experiment.cages.Cage;
import plugins.fmp.multicafe.series.BuildSeriesOptions;
import plugins.fmp.multicafe.series.FlyDetect2;
import plugins.fmp.multicafe.tools.Canvas2D.Canvas2DWithTransforms;
import plugins.fmp.multicafe.tools.ImageTransform.ImageTransformEnums;
import plugins.fmp.multicafe.tools.ImageTransform.ImageTransformOptions;
import plugins.fmp.multicafe.tools.Overlay.OverlayThreshold;

public class Detect2Flies extends JPanel implements ChangeListener, PropertyChangeListener, PopupMenuListener {
	private static final long serialVersionUID = -5257698990389571518L;
	private MultiCAFE parent0 = null;

	private String detectString = "Detect...";
	private JButton startComputationButton = new JButton(detectString);
	private JComboBox<String> allCellsComboBox = new JComboBox<String>(new String[] { "all cells" });
	private JCheckBox allCheckBox = new JCheckBox("ALL (current to last)", false);

	private JCheckBox objectLowsizeCheckBox = new JCheckBox("size >");
	private JSpinner objectLowsizeSpinner = new JSpinner(new SpinnerNumberModel(50, 0, 9999, 1));

	private JCheckBox objectUpsizeCheckBox = new JCheckBox("<");
	private JSpinner objectUpsizeSpinner = new JSpinner(new SpinnerNumberModel(500, 0, 9999, 1));
	private JSpinner thresholdSpinner = new JSpinner(new SpinnerNumberModel(100, 0, 255, 1));

	private JSpinner jitterTextField = new JSpinner(new SpinnerNumberModel(5, 0, 1000, 1));
	private JSpinner limitRatioSpinner = new JSpinner(new SpinnerNumberModel(4, 0, 1000, 1));

	private JToggleButton viewButton = new JToggleButton("View");
	private JCheckBox overlayCheckBox = new JCheckBox("overlay");
	private JCheckBox whiteObjectCheckBox = new JCheckBox("white object");

	private FlyDetect2 flyDetect2 = null;
	private OverlayThreshold overlayThreshold = null;

	// ----------------------------------------------------

	void init(GridLayout capLayout, MultiCAFE parent0) {
		setLayout(capLayout);
		this.parent0 = parent0;

		FlowLayout flowLayout = new FlowLayout(FlowLayout.LEFT);
		flowLayout.setVgap(0);

		JPanel panel1 = new JPanel(flowLayout);
		panel1.add(startComputationButton);
		panel1.add(allCellsComboBox);
		panel1.add(allCheckBox);
		add(panel1);

		allCellsComboBox.addPopupMenuListener(this);

		JPanel panel2 = new JPanel(flowLayout);
		panel2.add(new JLabel("threshold"));
		panel2.add(thresholdSpinner);
		panel2.add(viewButton);
		panel2.add(overlayCheckBox);

		add(panel2);

		JPanel panel3 = new JPanel(flowLayout);
		panel3.add(objectLowsizeCheckBox);
		panel3.add(objectLowsizeSpinner);
		objectLowsizeSpinner.setPreferredSize(new Dimension(80, 20));
		panel3.add(objectUpsizeCheckBox);
		panel3.add(objectUpsizeSpinner);
		objectUpsizeSpinner.setPreferredSize(new Dimension(80, 20));
		panel3.add(whiteObjectCheckBox);
		add(panel3);

		JPanel panel4 = new JPanel(flowLayout);
		panel4.add(new JLabel("length/width<"));
		panel4.add(limitRatioSpinner);
		limitRatioSpinner.setPreferredSize(new Dimension(40, 20));
		panel4.add(new JLabel("jitter <="));
		panel4.add(jitterTextField);
		jitterTextField.setPreferredSize(new Dimension(40, 20));
		add(panel4);

		defineActionListeners();
		defineItemListeners();
	}

	private void defineItemListeners() {

		thresholdSpinner.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				updateOverlayThreshold();
			}
		});
	}

	private void defineActionListeners() {
		startComputationButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				if (startComputationButton.getText().equals(detectString))
					startComputation();
				else
					stopComputation();
			}
		});

		allCheckBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Color color = Color.BLACK;
				if (allCheckBox.isSelected())
					color = Color.RED;
				allCheckBox.setForeground(color);
				startComputationButton.setForeground(color);
			}
		});

		viewButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null) {

					if (!viewButton.isSelected()) {
						viewDifference(exp, false);
						overlayCheckBox.setSelected(false);
						removeOverlay(exp);
					} else {
						viewDifference(exp, true);
					}
					overlayCheckBox.setEnabled(viewButton.isSelected());
				}
			}
		});

		overlayCheckBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null) {
					if (overlayCheckBox.isSelected()) {
						updateOverlay(exp);
						updateOverlayThreshold();
					} else {
						removeOverlay(exp);
					}
				}
			}
		});

		whiteObjectCheckBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				updateOverlayThreshold();
			}
		});

	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if (e.getSource() == thresholdSpinner) {
			Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
			if (exp != null)
				exp.cages.detect_threshold = (int) thresholdSpinner.getValue();
		}
	}

	public void updateOverlay(Experiment exp) {
		if (exp.seqCamData == null)
			return;
		if (overlayThreshold == null) {
			overlayThreshold = new OverlayThreshold(exp.seqCamData);
		} else {
			exp.seqCamData.seq.removeOverlay(overlayThreshold);
			overlayThreshold.setSequence(exp.seqCamData);
		}
		overlayThreshold.setReferenceImage(exp.seqCamData.refImage);
		exp.seqCamData.seq.addOverlay(overlayThreshold);
	}

	void updateOverlayThreshold() {
		if (overlayThreshold == null)
			return;

		boolean ifGreater = !whiteObjectCheckBox.isSelected();
		ImageTransformEnums transformOp = ImageTransformEnums.SUBTRACT_REF;
		int threshold = (int) thresholdSpinner.getValue();
		overlayThreshold.setThresholdSingle(threshold, transformOp, ifGreater);

		overlayThreshold.painterChanged();
	}

	private BuildSeriesOptions initTrackParameters(Experiment exp) {
		BuildSeriesOptions options = flyDetect2.options;
		options.expList = parent0.expListCombo;
		options.expList.index0 = parent0.expListCombo.getSelectedIndex();
		if (allCheckBox.isSelected())
			options.expList.index1 = options.expList.getItemCount() - 1;
		else
			options.expList.index1 = parent0.expListCombo.getSelectedIndex();

		options.btrackWhite = whiteObjectCheckBox.isSelected();
		options.blimitLow = objectLowsizeCheckBox.isSelected();
		options.blimitUp = objectUpsizeCheckBox.isSelected();
		options.limitLow = (int) objectLowsizeSpinner.getValue();
		options.limitUp = (int) objectUpsizeSpinner.getValue();
		options.limitRatio = (int) limitRatioSpinner.getValue();
		options.jitter = (int) jitterTextField.getValue();
		options.thresholdDiff = (int) thresholdSpinner.getValue();
		options.detectFlies = true;

		options.parent0Rect = parent0.mainFrame.getBoundsInternal();
		options.binSubDirectory = exp.getBinSubDirectory();

		options.isFrameFixed = parent0.paneExcel.tabCommonOptions.getIsFixedFrame();
		options.t_Ms_First = parent0.paneExcel.tabCommonOptions.getStartMs();
		options.t_Ms_Last = parent0.paneExcel.tabCommonOptions.getEndMs();
		options.t_Ms_BinDuration = parent0.paneExcel.tabCommonOptions.getBinMs();

		return options;
	}

	void startComputation() {
		Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
		if (exp == null)
			return;
		parent0.paneBrowse.panelLoadSave.closeViewsForCurrentExperiment(exp);

		flyDetect2 = new FlyDetect2();
		flyDetect2.options = initTrackParameters(exp);
		flyDetect2.stopFlag = false;
		flyDetect2.addPropertyChangeListener(this);
		flyDetect2.execute();
		startComputationButton.setText("STOP");
	}

	private void stopComputation() {
		if (flyDetect2 != null && !flyDetect2.stopFlag)
			flyDetect2.stopFlag = true;
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if (StringUtil.equals("thread_ended", evt.getPropertyName())) {
			startComputationButton.setText(detectString);
			parent0.paneKymos.tabDisplay.selectKymographImage(parent0.paneKymos.tabDisplay.indexImagesCombo);
			parent0.paneKymos.tabDisplay.indexImagesCombo = -1;
		}
	}

	@Override
	public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
		int nitems = 1;
		Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
		if (exp != null)
			nitems = exp.cages.cageList.size() + 1;
		if (allCellsComboBox.getItemCount() != nitems) {
			allCellsComboBox.removeAllItems();
			allCellsComboBox.addItem("all cells");
			for (Cage cell : exp.cages.cageList) {
				allCellsComboBox.addItem(cell.getCageNumber());
			}
		}
	}

	@Override
	public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void popupMenuCanceled(PopupMenuEvent e) {
		// TODO Auto-generated method stub

	}

	void removeOverlay(Experiment exp) {
		if (exp.seqCamData != null && exp.seqCamData.seq != null)
			exp.seqCamData.seq.removeOverlay(overlayThreshold);
	}

	void viewDifference(Experiment exp, boolean display) {
		Canvas2DWithTransforms canvas = (Canvas2DWithTransforms) exp.seqCamData.seq.getFirstViewer().getCanvas();
		ImageTransformEnums[] imageTransformStep1 = new ImageTransformEnums[] { ImageTransformEnums.NONE,
				ImageTransformEnums.SUBTRACT_REF };
		ImageTransformOptions optionsStep1 = canvas.getOptionsStep1();

		optionsStep1.backgroundImage = null;
		int index = 0;
		if (display) {
			if (exp.seqCamData.refImage == null) {
				exp.loadReferenceImage();
			}
			optionsStep1.backgroundImage = exp.seqCamData.refImage;
			index = 1;
		}
		canvas.selectItemStep1(imageTransformStep1[index], optionsStep1);
	}

}
