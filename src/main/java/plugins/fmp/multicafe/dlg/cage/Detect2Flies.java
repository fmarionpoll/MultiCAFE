package plugins.fmp.multicafe.dlg.cage;

import java.awt.Color;
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
import plugins.fmp.multicafe.experiment.cage.Cell;
import plugins.fmp.multicafe.series.BuildSeriesOptions;
import plugins.fmp.multicafe.series.FlyDetect2;
import plugins.fmp.multicafe.tools.ImageTransform.ImageTransformEnums;
import plugins.fmp.multicafe.tools.Overlay.OverlayThreshold;

public class Detect2Flies extends JPanel implements ChangeListener, PropertyChangeListener, PopupMenuListener {
	private static final long serialVersionUID = -5257698990389571518L;
	private MultiCAFE parent0 = null;

	private String detectString = "Detect...";
	private JButton startComputationButton = new JButton(detectString);
	private JComboBox<String> allCagesComboBox = new JComboBox<String>(new String[] { "all cages" });
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

	// ----------------------------------------------------

	void init(GridLayout capLayout, MultiCAFE parent0) {
		setLayout(capLayout);
		this.parent0 = parent0;

		FlowLayout flowLayout = new FlowLayout(FlowLayout.LEFT);
		flowLayout.setVgap(0);

		JPanel panel1 = new JPanel(flowLayout);
		panel1.add(startComputationButton);
		panel1.add(allCagesComboBox);
		panel1.add(allCheckBox);
		add(panel1);

		allCagesComboBox.addPopupMenuListener(this);

//		objectLowsizeCheckBox.setHorizontalAlignment(SwingConstants.RIGHT);
//		objectUpsizeCheckBox.setHorizontalAlignment(SwingConstants.RIGHT);
		JPanel panel3 = new JPanel(flowLayout);
		panel3.add(objectLowsizeCheckBox);
		panel3.add(objectLowsizeSpinner);
		panel3.add(objectUpsizeCheckBox);
		panel3.add(objectUpsizeSpinner);
		panel3.add(new JLabel("threshold"));
		panel3.add(thresholdDiffSpinner);
		add(panel3);

		JPanel panel4 = new JPanel(flowLayout);
		panel4.add(new JLabel("length/width<"));
		panel4.add(limitRatioSpinner);
		panel4.add(new JLabel("jitter <="));
		panel4.add(jitterTextField);
		add(panel4);

		defineActionListeners();
		thresholdDiffSpinner.addChangeListener(this);
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
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if (e.getSource() == thresholdDiffSpinner) {
			Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
			if (exp != null)
				exp.cages.detect_threshold = (int) thresholdDiffSpinner.getValue();
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

		options.btrackWhite = true;
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
			nitems = exp.cages.cellList.size() + 1;
		if (allCagesComboBox.getItemCount() != nitems) {
			allCagesComboBox.removeAllItems();
			allCagesComboBox.addItem("all cages");
			for (Cell cell : exp.cages.cellList) {
				allCagesComboBox.addItem(cell.getCellNumber());
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

}
