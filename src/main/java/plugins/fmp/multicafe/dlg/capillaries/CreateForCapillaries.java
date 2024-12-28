package plugins.fmp.multicafe.dlg.capillaries;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import icy.gui.frame.progress.AnnounceFrame;
import icy.roi.ROI2D;
import icy.sequence.Sequence;
import icy.type.geom.Polygon2D;
import plugins.fmp.multicafe.MultiCAFE;
import plugins.fmp.multicafe.experiment.Experiment;
import plugins.fmp.multicafe.experiment.SequenceCamData;
import plugins.fmp.multicafe.experiment.SequenceKymosUtils;
import plugins.fmp.multicafe.experiment.capillaries.Capillaries;
import plugins.fmp.multicafe.tools.ROI2D.ROI2DUtilities;
import plugins.kernel.roi.roi2d.ROI2DLine;
import plugins.kernel.roi.roi2d.ROI2DPolygon;

public class CreateForCapillaries extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5257698990389571518L;

	private JButton displayFrameButton = new JButton("(1) Display frame");
	private JButton generateCapillariesButton = new JButton("(2) Generate capillaries from edited frame");

	private JComboBox<String> cagesJCombo = new JComboBox<String>(new String[] { "10", "4+(2)", "1+(2)" });
	private JSpinner nbFliesPerCageJSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 500, 1));

	private JLabel widthLabel = new JLabel("with a ratio of");
	private JSpinner nCapillariesPerCage = new JSpinner(new SpinnerNumberModel(2, 1, 500, 1));
	private JSpinner width_between_capillariesJSpinner = new JSpinner(new SpinnerNumberModel(30, 0, 10000, 1));
	private JLabel width_between_capillariesLabel = new JLabel("pixels btw. caps to");
	private JSpinner width_intervalJSpinner = new JSpinner(new SpinnerNumberModel(53, 0, 10000, 1));
	private JLabel width_intervalLabel = new JLabel("pixels btw. cages");

	private String[] flyString = new String[] { "fly", "flies" };
	private String[] capString = new String[] { "cap &", "caps &" };
	private JLabel flyLabel = new JLabel(flyString[0]);
	private JLabel capLabel = new JLabel(capString[1]);

	private ROI2DPolygon capillariesRoiPolygon = null;
	private MultiCAFE parent0 = null;

	void init(GridLayout capLayout, MultiCAFE parent0) {
		setLayout(capLayout);
		FlowLayout flowLayout = new FlowLayout(FlowLayout.LEFT);
		flowLayout.setVgap(0);

		JPanel panel0 = new JPanel(flowLayout);
		panel0.add(displayFrameButton);
		panel0.add(generateCapillariesButton);

		JPanel panel1 = new JPanel(flowLayout);
		panel1.add(new JLabel("Grouped as"));
		panel1.add(cagesJCombo);
		cagesJCombo.setPreferredSize(new Dimension(60, 20));
		panel1.add(new JLabel("cages each with"));
		panel1.add(nCapillariesPerCage);
		nCapillariesPerCage.setPreferredSize(new Dimension(40, 20));
		panel1.add(capLabel);
		panel1.add(nbFliesPerCageJSpinner);
		nbFliesPerCageJSpinner.setPreferredSize(new Dimension(40, 20));
		panel1.add(flyLabel);

		JPanel panel2 = new JPanel(flowLayout);
		panel2.add(widthLabel);
		panel2.add(width_between_capillariesJSpinner);
		width_between_capillariesJSpinner.setPreferredSize(new Dimension(40, 20));
		panel2.add(width_between_capillariesLabel);
		panel2.add(width_intervalJSpinner);
		width_intervalJSpinner.setPreferredSize(new Dimension(40, 20));
		panel2.add(width_intervalLabel);

		add(panel0);
		add(panel1);
		add(panel2);

		defineDlgItemsListeners();
		this.parent0 = parent0;
	}

	private void defineDlgItemsListeners() {
		displayFrameButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp == null)
					return;

				if (exp.capillaries != null && exp.capillaries.capillariesList.size() > 0) {
					Polygon2D extPolygon = exp.capillaries.get2DPolygonEnclosingCapillaries();
					if (extPolygon == null) {
						extPolygon = getCapillariesPolygon(exp.seqCamData.seq);
					}
					capillariesRoiPolygon = new ROI2DPolygon(extPolygon);
					exp.capillaries.deleteAllCapillaries();
					exp.capillaries.updateCapillariesFromSequence(exp.seqCamData.seq);
					exp.seqCamData.seq.removeAllROI();
					final String dummyname = "perimeter_enclosing_capillaries";
					capillariesRoiPolygon.setName(dummyname);
					exp.seqCamData.seq.addROI(capillariesRoiPolygon);
					exp.seqCamData.seq.setSelectedROI(capillariesRoiPolygon);
					// TODO delete kymos
				} else
					create_capillariesRoiPolygon(exp);
			}
		});

		generateCapillariesButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				roisGenerateFromPolygon();
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null) {
					SequenceKymosUtils.transferCamDataROIStoKymo(exp);
					int nbFliesPerCage = (int) nbFliesPerCageJSpinner.getValue();
					switch (cagesJCombo.getSelectedIndex()) {
					case 0:
						exp.capillaries.initCapillariesWith10Cages(nbFliesPerCage, true);
						break;
					case 1:
						exp.capillaries.initCapillariesWith6Cages(nbFliesPerCage);
						break;
					default:
						break;
					}
					firePropertyChange("CAPILLARIES_NEW", false, true);
				}
			}
		});

		nCapillariesPerCage.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				boolean status = (int) nCapillariesPerCage.getValue() == 2 ? true : false;
				EnableBinWidthItems(status);
				int i = (int) nCapillariesPerCage.getValue() > 1 ? 1 : 0;
				capLabel.setText(capString[i]);
				nCapillariesPerCage.requestFocus();
			}
		});

		nbFliesPerCageJSpinner.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				int i = (int) nbFliesPerCageJSpinner.getValue() > 1 ? 1 : 0;
				flyLabel.setText(flyString[i]);
				nbFliesPerCageJSpinner.requestFocus();
			}
		});

	}

	private void EnableBinWidthItems(boolean status) {
		widthLabel.setVisible(status);
		width_between_capillariesJSpinner.setVisible(status);
		width_between_capillariesLabel.setVisible(status);
		width_intervalJSpinner.setVisible(status);
		width_intervalLabel.setVisible(status);
	}

	// set/ get

	private int getNbCapillaries() {
		int nCapillaries = (int) nCapillariesPerCage.getValue();
		int selectedCagesArrangement = cagesJCombo.getSelectedIndex();
		switch (selectedCagesArrangement) {
		case 2: // "4+ (2)"
			nCapillaries = nCapillaries * 4 + 2 * 2; //
			break;
		case 3: // "1+(2)"
			break;
		case 1:
		default: // "10"
			nCapillaries = nCapillaries * 10;
			break;
		}
		return nCapillaries;
	}

	private int getWidthSmallInterval() {
		return (int) width_between_capillariesJSpinner.getValue();
	}

	private int getWidthLongInterval() {
		return (int) width_intervalJSpinner.getValue();
	}

	void setGroupedBy2(boolean flag) {
		int nCapillaries = flag ? 2 : 1;
		nCapillariesPerCage.setValue(nCapillaries);
	}

	void setGroupingAndNumber(Capillaries cap) {
		setGroupedBy2(cap.capillariesDescription.grouping == 2);
	}

	int getCapillariesGrouping() {
		int nCapillaries = (int) nCapillariesPerCage.getValue();
		int grouping = nCapillaries;
		int selectedCagesArrangement = cagesJCombo.getSelectedIndex();
		if (selectedCagesArrangement == 3)
			grouping = 1;
		return grouping;
	}

	Capillaries setCapillariesGrouping(Capillaries cap) {
		cap.capillariesDescription.grouping = getCapillariesGrouping();
		return cap;
	}

	// ---------------------------------

	private void create_capillariesRoiPolygon(Experiment exp) {
		Sequence seq = exp.seqCamData.seq;
		final String dummyname = "perimeter_enclosing_capillaries";
		capillariesRoiPolygon = (ROI2DPolygon) isRoiPresent(seq, dummyname);
		if (capillariesRoiPolygon == null) {
			capillariesRoiPolygon = new ROI2DPolygon(getCapillariesPolygon(seq));
			capillariesRoiPolygon.setName(dummyname);
			seq.addROI(capillariesRoiPolygon);
		}

		seq.setSelectedROI(capillariesRoiPolygon);
	}

	private ROI2D isRoiPresent(Sequence seq, String dummyname) {
		ArrayList<ROI2D> listRois = seq.getROI2Ds();
		for (ROI2D roi : listRois) {
			if (roi.getName().equals(dummyname))
				return roi;
		}
		return null;
	}

	private Polygon2D getCapillariesPolygon(Sequence seq) {
		Rectangle rect = seq.getBounds2D();
		List<Point2D> points = new ArrayList<Point2D>();
		points.add(new Point2D.Double(rect.x + rect.width / 5, rect.y + rect.height / 5));
		points.add(new Point2D.Double(rect.x + rect.width * 4 / 5, rect.y + rect.height / 5));
		points.add(new Point2D.Double(rect.x + rect.width * 4 / 5, rect.y + rect.height * 2 / 3));
		points.add(new Point2D.Double(rect.x + rect.width / 5, rect.y + rect.height * 2 / 3));
		return new Polygon2D(points);
	}

	private void roisGenerateFromPolygon() {
		Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
		if (exp == null)
			return;
		SequenceCamData seqCamData = exp.seqCamData;
		boolean statusGroup2Mode = (getCapillariesGrouping() == 2);

		int nbcapillaries = 20;
		int width_between_capillaries = 1;
		int width_interval = 0;

		try {
			nbcapillaries = getNbCapillaries();
			if (statusGroup2Mode) {
				width_between_capillaries = getWidthSmallInterval();
				width_interval = getWidthLongInterval();
			}
		} catch (Exception e) {
			new AnnounceFrame("Can't interpret one of the ROI parameters value");
		}

//		ROI2D roi = seqCamData.seq.getSelectedROI2D();
//		if ( ! ( roi instanceof ROI2DPolygon ) ) 
//		{
//			new AnnounceFrame("The frame must be a ROI2D POLYGON");
//			return;
//		}

		Polygon2D capillariesPolygon = ROI2DUtilities.orderVerticesofPolygon(capillariesRoiPolygon.getPolygon());

		seqCamData.seq.removeROI(capillariesRoiPolygon);

		if (statusGroup2Mode) {
			double span = (nbcapillaries / 2) * (width_between_capillaries + width_interval) - width_interval;
			for (int i = 0; i < nbcapillaries; i += 2) {
				double span0 = (width_between_capillaries + width_interval) * i / 2;
				addROILine(seqCamData, "line" + i / 2 + "L", capillariesPolygon, span0, span);
				span0 += width_between_capillaries;
				addROILine(seqCamData, "line" + i / 2 + "R", capillariesPolygon, span0, span);
			}
		} else {
			double span = nbcapillaries - 1;
			for (int i = 0; i < nbcapillaries; i++) {
				double span0 = width_between_capillaries * i;
				addROILine(seqCamData, "line" + i, capillariesPolygon, span0, span);
			}
		}
	}

	private void addROILine(SequenceCamData seqCamData, String name, Polygon2D roiPolygon, double span0, double span) {
		double x0 = roiPolygon.xpoints[0] + (roiPolygon.xpoints[3] - roiPolygon.xpoints[0]) * span0 / span;
		double y0 = roiPolygon.ypoints[0] + (roiPolygon.ypoints[3] - roiPolygon.ypoints[0]) * span0 / span;
		if (x0 < 0)
			x0 = 0;
		if (y0 < 0)
			y0 = 0;
		double x1 = roiPolygon.xpoints[1] + (roiPolygon.xpoints[2] - roiPolygon.xpoints[1]) * span0 / span;
		double y1 = roiPolygon.ypoints[1] + (roiPolygon.ypoints[2] - roiPolygon.ypoints[1]) * span0 / span;

		ROI2DLine roiL1 = new ROI2DLine(x0, y0, x1, y1);
		roiL1.setName(name);
		roiL1.setReadOnly(false);
		seqCamData.seq.addROI(roiL1, true);
	}

}
