package plugins.fmp.multicafe.dlg.cells;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
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

import icy.image.IcyBufferedImageUtil;
import icy.util.StringUtil;
import plugins.fmp.multicafe.MultiCAFE;
import plugins.fmp.multicafe.experiment.Experiment;
import plugins.fmp.multicafe.experiment.SequenceCamData;
import plugins.fmp.multicafe.experiment.cells.Cell;
import plugins.fmp.multicafe.series.BuildSeriesOptions;
import plugins.fmp.multicafe.series.FlyDetect2;
import plugins.fmp.multicafe.tools.Canvas2D.Canvas2DWithTransforms;
import plugins.fmp.multicafe.tools.ImageTransform.ImageTransformEnums;
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
	private JSpinner thresholdDiffSpinner = new JSpinner(new SpinnerNumberModel(100, 0, 255, 1));

	private JSpinner jitterTextField = new JSpinner(new SpinnerNumberModel(5, 0, 1000, 1));
	private JSpinner limitRatioSpinner = new JSpinner(new SpinnerNumberModel(4, 0, 1000, 1));

	private FlyDetect2 flyDetect2 = null;
	private OverlayThreshold overlayThreshold2 = null;
	private JToggleButton transformPass1DisplayButton = new JToggleButton("View");
	private JCheckBox overlayPass1CheckBox = new JCheckBox("overlay");


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
		panel2.add(thresholdDiffSpinner);
		panel2.add(transformPass1DisplayButton);
		panel2.add(overlayPass1CheckBox);
		
		add(panel2);

		JPanel panel3 = new JPanel(flowLayout);
		panel3.add(objectLowsizeCheckBox);
		panel3.add(objectLowsizeSpinner);
		objectLowsizeSpinner.setPreferredSize(new Dimension(40, 20));
		panel3.add(objectUpsizeCheckBox);
		panel3.add(objectUpsizeSpinner);
		objectUpsizeSpinner.setPreferredSize(new Dimension(40, 20));
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
		
		thresholdDiffSpinner.addChangeListener(this);
	}
	
	private void defineItemListeners() {
		overlayPass1CheckBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null) {
					if (overlayPass1CheckBox.isSelected())
						updateOverlay(exp);
					else
						removeOverlay(exp);
				}
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
		
		transformPass1DisplayButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null) {
					boolean displayCheckOverlay = false;
					if (transformPass1DisplayButton.isSelected()) {
						Canvas2DWithTransforms canvas = getKymosCanvas(exp);
						canvas.updateTransformsComboStep1(transformPass1);
						int index = transformPass1ComboBox.getSelectedIndex();
						canvas.selectImageTransformFunctionStep1(index + 1);
						displayCheckOverlay = true;
					} else {
						removeOverlay(exp);
						overlayPass1CheckBox.setSelected(false);
						getKymosCanvas(exp).imageTransformFunctionsComboStep1.setSelectedIndex(0);
					}
					overlayPass1CheckBox.setEnabled(displayCheckOverlay);
				}
			}
		});
		
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if (e.getSource() == thresholdDiffSpinner) {
			Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
			if (exp != null)
				exp.cageBox.detect_threshold = (int) thresholdDiffSpinner.getValue();
		}
	}

	public void updateOverlay(Experiment exp, int threshold) {
		SequenceCamData seqCamData = exp.seqCamData;
		if (seqCamData == null)
			return;
		if (overlayThreshold2 == null) {
			overlayThreshold2 = new OverlayThreshold(seqCamData);
			exp.seqCamData.refImage = IcyBufferedImageUtil.getCopy(exp.seqCamData.getSeqImage(0, 0));
		} else {
			seqCamData.seq.removeOverlay(overlayThreshold2);
			overlayThreshold2.setSequence(seqCamData);
		}
		seqCamData.seq.addOverlay(overlayThreshold2);
		boolean ifGreater = true;
		ImageTransformEnums transformOp = ImageTransformEnums.SUBTRACT_REF;
		overlayThreshold2.setThresholdSingle(threshold, transformOp, ifGreater);
		overlayThreshold2.painterChanged();
	}

	private BuildSeriesOptions initTrackParameters(Experiment exp) {
		BuildSeriesOptions options = flyDetect2.options;
		options.expList = parent0.expListCombo;
		options.expList.index0 = parent0.expListCombo.getSelectedIndex();
		if (allCheckBox.isSelected())
			options.expList.index1 = options.expList.getItemCount() - 1;
		else
			options.expList.index1 = parent0.expListCombo.getSelectedIndex();

		options.btrackWhite = false; //true;
		options.blimitLow = objectLowsizeCheckBox.isSelected();
		options.blimitUp = objectUpsizeCheckBox.isSelected();
		options.limitLow = (int) objectLowsizeSpinner.getValue();
		options.limitUp = (int) objectUpsizeSpinner.getValue();
		options.limitRatio = (int) limitRatioSpinner.getValue();
		options.jitter = (int) jitterTextField.getValue();
		options.thresholdDiff = (int) thresholdDiffSpinner.getValue();
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
			nitems = exp.cageBox.cellList.size() + 1;
		if (allCellsComboBox.getItemCount() != nitems) {
			allCellsComboBox.removeAllItems();
			allCellsComboBox.addItem("all cells");
			for (Cell cell : exp.cageBox.cellList) {
				allCellsComboBox.addItem(cell.getCellNumber());
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
	
//	void updateOverlayThreshold() {
//		if (overlayThreshold2 == null)
//			return;
//
//		boolean ifGreater = true;
//		int threshold = 0;
//		ImageTransformEnums transform = ImageTransformEnums.NONE;
//		if (transformPass1DisplayButton.isSelected()) {
//			ifGreater = (direction1ComboBox.getSelectedIndex() == 0);
//			threshold = (int) threshold1Spinner.getValue();
//			transform = (ImageTransformEnums) transformPass1ComboBox.getSelectedItem();
//		} else
//			return;
//		overlayThreshold2.setThresholdSingle(threshold, transform, ifGreater);
//		overlayThreshold2.painterChanged();
//	}

	void removeOverlay(Experiment exp) {
		if (exp.seqKymos != null && exp.seqKymos.seq != null)
			exp.seqKymos.seq.removeOverlay(overlayThreshold2);
	}

}
