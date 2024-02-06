package plugins.fmp.multicafe2.dlg.capillaries;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import icy.gui.frame.progress.AnnounceFrame;
import icy.roi.ROI2D;
import icy.type.geom.Polygon2D;
import plugins.fmp.multicafe2.MultiCAFE2;
import plugins.fmp.multicafe2.experiment.Capillaries;
import plugins.fmp.multicafe2.experiment.Experiment;
import plugins.fmp.multicafe2.experiment.SequenceCamData;
import plugins.fmp.multicafe2.experiment.SequenceKymosUtils;
import plugins.fmp.multicafe2.tools.ROI2DUtilities;
import plugins.kernel.roi.roi2d.ROI2DLine;
import plugins.kernel.roi.roi2d.ROI2DPolygon;


public class CreateForSpots extends JPanel 
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -5257698990389571518L;
	
	private JButton 	addPolygon2DButton 		= new JButton("(1) Draw frame");
	private JComboBox<String> modeJCombo 		= new JComboBox<String> (new String[] {"cages"});
	private JButton 	createROIsFromPolygonButton2 = new JButton("(2) Generate");
	
	
	private JRadioButton selectGroupedby2Button = new JRadioButton("by 2");
	private JRadioButton selectEvenlySpacedButton 	= new JRadioButton("even");
	private JComboBox<String> cagesJCombo 		= new JComboBox<String> (new String[] {"10", "4+(2)", "1+(2)"});
	private JComboBox<String> orientationJCombo = new JComboBox<String> (new String[] {"0°", "90°", "180°", "270°" });
	private ButtonGroup buttonGroup2 			= new ButtonGroup();
	private JSpinner 	nbcapillariesJSpinner 	= new JSpinner(new SpinnerNumberModel(2, 1, 500, 1));
	private JSpinner 	nbFliesPerCageJSpinner 	= new JSpinner(new SpinnerNumberModel(1, 0, 500, 1));


	private Polygon2D 	capillariesPolygon 		= null;
	
	private JSpinner 	nRowsJSpinner 		= new JSpinner(new SpinnerNumberModel(1, 1, 500, 1));
	private JSpinner 	nColumnsJSpinner 	= new JSpinner(new SpinnerNumberModel(1, 1, 500, 1));
	private JSpinner 	nPointsJSpinner 	= new JSpinner(new SpinnerNumberModel(2, 2, 500, 1));
	
	private MultiCAFE2 	parent0 				= null;
	
	void init(GridLayout capLayout, MultiCAFE2 parent0) 
	{
		setLayout(capLayout);	
		FlowLayout flowLayout = new FlowLayout(FlowLayout.LEFT);
		flowLayout.setVgap(0);
		
		JPanel panel0 = new JPanel(flowLayout);
		panel0.add(addPolygon2DButton);		
		panel0.add(new JLabel ("including all"));
		panel0.add(modeJCombo);
		panel0.add(createROIsFromPolygonButton2);
		
		JPanel panel2 = new JPanel(flowLayout);
		panel2.add(new JLabel ("grid rows"));
		panel2.add(nRowsJSpinner);
		nRowsJSpinner.setPreferredSize(new Dimension (40, 20));
		panel2.add(new JLabel ("cols"));
		panel2.add(nColumnsJSpinner);
		nColumnsJSpinner.setPreferredSize(new Dimension (40, 20));
		panel2.add(new JLabel ("cages/cell"));
		panel2.add(cagesJCombo);	
		cagesJCombo.setPreferredSize(new Dimension (60, 20));
		panel2.add(new JLabel ("fly/cage"));
		panel2.add(nbFliesPerCageJSpinner);
		nbFliesPerCageJSpinner.setPreferredSize(new Dimension (40, 20));
		
		JPanel panel1 = new JPanel(flowLayout);
		panel1.add(new JLabel ("polyline/cage"));
		panel1.add(nbcapillariesJSpinner);
		nbcapillariesJSpinner.setPreferredSize(new Dimension (40, 20));
		panel1.add(new JLabel (" angle"));
		panel1.add(orientationJCombo);
		orientationJCombo.setPreferredSize(new Dimension (50, 20));
		panel1.add(new JLabel ("points/polyline"));
		panel1.add(nPointsJSpinner);
		nPointsJSpinner.setPreferredSize(new Dimension (40, 20));
				
		add(panel0);
		add(panel2);
		add(panel1);
		
		buttonGroup2.add(selectGroupedby2Button);
		buttonGroup2.add(selectEvenlySpacedButton);
		selectGroupedby2Button.setSelected(true);
		
		defineActionListeners();
		this.parent0 = parent0;
	}
	
	private void defineActionListeners() 
	{
		addPolygon2DButton.addActionListener(new ActionListener () {
			@Override public void actionPerformed( final ActionEvent e ) { 
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if ((exp != null) && (exp.capillaries != null)) {
					Polygon2D extPolygon = exp.capillaries.get2DPolygonEnclosingCapillaries();
					if (extPolygon == null) {
						extPolygon = getCapillariesPolygon(exp.seqCamData);
					}
					ROI2DPolygon extRect = new ROI2DPolygon(extPolygon);
					exp.capillaries.deleteAllCapillaries();
					exp.capillaries.updateCapillariesFromSequence(exp.seqCamData.seq);
					exp.seqCamData.seq.removeAllROI();
					final String dummyname = "perimeter_enclosing_capillaries";
					extRect.setName(dummyname);
					exp.seqCamData.seq.addROI(extRect);
					exp.seqCamData.seq.setSelectedROI(extRect);
					// TODO delete kymos
				}
				else
					create2DPolygon();
			}});
		
		createROIsFromPolygonButton2.addActionListener(new ActionListener () {
			@Override public void actionPerformed( final ActionEvent e ) { 
				roisGenerateFromPolygon();
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null) {
					SequenceKymosUtils.transferCamDataROIStoKymo(exp);
					int nbFliesPerCage = (int) nbFliesPerCageJSpinner.getValue();
					switch(cagesJCombo.getSelectedIndex()) {
					case 0:
						exp.capillaries.initCapillariesWith10Cages(nbFliesPerCage);
						break;
					case 1:
						exp.capillaries.initCapillariesWith6Cages(nbFliesPerCage);
						break;
					default:
						break;		
					}
					firePropertyChange("CAPILLARIES_NEW", false, true);
				}
			}});
		

	}
	
	// set/ get	

	private int getNbCapillaries( ) 
	{
		return (int) nbcapillariesJSpinner.getValue();
	}

	
	private boolean getGroupedBy2() 
	{
		return selectGroupedby2Button.isSelected();
	}
	
	void setGroupedBy2(boolean flag) 
	{
		buttonGroup2.clearSelection();
//		selectGroupedby2Button.setSelected(flag);
//		selectEvenlySpacedButton.setSelected(!flag);
		selectGroupedby2Button.setVisible(flag);
		selectEvenlySpacedButton.setVisible(!flag);
	}
	
	void setGroupingAndNumber(Capillaries cap) 
	{
		setGroupedBy2(cap.capillariesDescription.grouping == 2);
	}
	
	Capillaries getGrouping(Capillaries cap) 
	{
		cap.capillariesDescription.grouping = getGroupedBy2() ? 2: 1;
		return cap;
	}

	// ---------------------------------
	private void create2DPolygon() 
	{
		Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
		if (exp == null)
			return;
		SequenceCamData seqCamData = exp.seqCamData;
		final String dummyname = "perimeter_enclosing_capillaries";
		if (isRoiPresent(seqCamData, dummyname))
			return;
		
		ROI2DPolygon roi = new ROI2DPolygon(getCapillariesPolygon(seqCamData));
		roi.setName(dummyname);
		seqCamData.seq.addROI(roi);
		seqCamData.seq.setSelectedROI(roi);
	}
	
	
	private boolean isRoiPresent(SequenceCamData seqCamData, String dummyname) 
	{
		ArrayList<ROI2D> listRois = seqCamData.seq.getROI2Ds();
		for (ROI2D roi: listRois) 
		{
			if (roi.getName() .equals(dummyname))
				return true;
		}
		return false;
	}
	
	private Polygon2D getCapillariesPolygon(SequenceCamData seqCamData)
	{
		if (capillariesPolygon == null)
		{		
			Rectangle rect = seqCamData.seq.getBounds2D();
			List<Point2D> points = new ArrayList<Point2D>();
			points.add(new Point2D.Double(rect.x + rect.width /5, rect.y + rect.height /5));
			points.add(new Point2D.Double(rect.x + rect.width*4 /5, rect.y + rect.height /5));
			points.add(new Point2D.Double(rect.x + rect.width*4 /5, rect.y + rect.height*2 /3));
			points.add(new Point2D.Double(rect.x + rect.width /5, rect.y + rect.height *2 /3));
			capillariesPolygon = new Polygon2D(points);
		}
		return capillariesPolygon;
	}
	
	private void rotate (Polygon2D roiPolygon) 
	{
		int isel = orientationJCombo.getSelectedIndex();
		if (isel == 0)
			return;
		
		Polygon2D roiPolygon_orig = (Polygon2D) roiPolygon.clone();
		for (int i=0; i<roiPolygon.npoints; i++) 
		{
			int j = (i + isel) % 4;
			roiPolygon.xpoints[j] = roiPolygon_orig.xpoints[i];
			roiPolygon.ypoints[j] = roiPolygon_orig.ypoints[i];
		}
	}
	
	private void roisGenerateFromPolygon() 
	{
		Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
		if (exp == null)
			return;
		SequenceCamData seqCamData = exp.seqCamData;
		boolean statusGroup2Mode = getGroupedBy2() ? true: false;
		
		int nbcapillaries = 20;
		int width_between_capillaries = 1;	
		int width_interval = 0;

		try 
		{ 
			nbcapillaries = getNbCapillaries();
		} 
		catch( Exception e ) 
		{ 
			new AnnounceFrame("Can't interpret one of the ROI parameters value"); 
		}

		ROI2D roi = seqCamData.seq.getSelectedROI2D();
		if ( ! ( roi instanceof ROI2DPolygon ) ) 
		{
			new AnnounceFrame("The frame must be a ROI2D POLYGON");
			return;
		}
		
		capillariesPolygon = ROI2DUtilities.orderVerticesofPolygon (((ROI2DPolygon) roi).getPolygon());
	
		rotate(capillariesPolygon);
		
		seqCamData.seq.removeROI(roi);

		if (statusGroup2Mode) 
		{	
			double span = (nbcapillaries/2)* (width_between_capillaries + width_interval) - width_interval;
			for (int i=0; i< nbcapillaries; i+= 2) 
			{
				double span0 = (width_between_capillaries + width_interval)*i/2;
				addROILine(seqCamData, "line"+i/2+"L", capillariesPolygon, span0, span);
				span0 += width_between_capillaries ;
				addROILine(seqCamData, "line"+i/2+"R", capillariesPolygon, span0, span);
			}
		}
		else 
		{
			double span = nbcapillaries-1;
			for (int i=0; i< nbcapillaries; i++) 
			{
				double span0 = width_between_capillaries*i;
				addROILine(seqCamData, "line"+i, capillariesPolygon, span0, span);
			}
		}
	}

	private void addROILine(SequenceCamData seqCamData, String name, Polygon2D roiPolygon, double span0, double span) 
	{
		double x0 = roiPolygon.xpoints[0] + (roiPolygon.xpoints[3]-roiPolygon.xpoints[0]) * span0 /span;
		double y0 = roiPolygon.ypoints[0] + (roiPolygon.ypoints[3]-roiPolygon.ypoints[0]) * span0 /span;
		if (x0 < 0) 
			x0= 0;
		if (y0 < 0) 
			y0=0;
		double x1 = roiPolygon.xpoints[1] + (roiPolygon.xpoints[2]-roiPolygon.xpoints[1]) * span0 /span ;
		double y1 = roiPolygon.ypoints[1] + (roiPolygon.ypoints[2]-roiPolygon.ypoints[1]) * span0 /span ;
		
		ROI2DLine roiL1 = new ROI2DLine (x0, y0, x1, y1);
		roiL1.setName(name);
		roiL1.setReadOnly(false);
		seqCamData.seq.addROI(roiL1, true);
	}

}
