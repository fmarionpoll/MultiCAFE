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
import icy.type.geom.Polygon2D;
import plugins.fmp.multicafe2.MultiCAFE2;
import plugins.fmp.multicafe2.experiment.Capillaries;
import plugins.fmp.multicafe2.experiment.Experiment;
import plugins.fmp.multicafe2.experiment.SequenceCamData;
import plugins.fmp.multicafe2.experiment.SequenceKymosUtils;
import plugins.fmp.multicafe2.tools.ROI2DUtilities;
import plugins.kernel.roi.roi2d.ROI2DLine;
import plugins.kernel.roi.roi2d.ROI2DPolygon;


public class CreateForCapillaries extends JPanel 
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -5257698990389571518L;
	
	private JButton 	addPolygon2DButton 		= new JButton("(1) Draw frame");
	private JButton 	createROIsFromPolygonButton2 = new JButton("(2) Generate");
//	private JRadioButton selectGroupedby2Button = new JRadioButton("by 2");
//	private JRadioButton selectEvenlySpacedButton 	= new JRadioButton("even");
	private JComboBox<String> cagesJCombo 		= new JComboBox<String> (new String[] {"10", "4+(2)", "1+(2)"});

//	private ButtonGroup buttonGroup2 			= new ButtonGroup();
	private JSpinner 	nCapillariesPerCage 	= new JSpinner(new SpinnerNumberModel(2, 1, 500, 1));
	private JSpinner 	nbFliesPerCageJSpinner 	= new JSpinner(new SpinnerNumberModel(1, 0, 500, 1));
	private JSpinner 	width_between_capillariesJSpinner = new JSpinner(new SpinnerNumberModel(30, 0, 10000, 1));
	private JLabel		width_between_capillariesLabel = new JLabel("pixels btw. caps ");
	private JSpinner 	width_intervalJSpinner 	= new JSpinner(new SpinnerNumberModel(53, 0, 10000, 1)); 
	private JLabel 		width_intervalLabel 	= new JLabel("btw. groups ");
	private Polygon2D 	capillariesPolygon 		= null;

	
	private MultiCAFE2 	parent0 				= null;
	
	void init(GridLayout capLayout, MultiCAFE2 parent0) 
	{
		setLayout(capLayout);	
		FlowLayout flowLayout = new FlowLayout(FlowLayout.LEFT);
		flowLayout.setVgap(0);
		
		JPanel panel0 = new JPanel(flowLayout);
		panel0.add(addPolygon2DButton);		
		
		panel0.add(new JLabel ("cages/frame:"));
		panel0.add(cagesJCombo);	
		panel0.add(new JLabel ("fly/cage"));
		panel0.add(nbFliesPerCageJSpinner);
		nbFliesPerCageJSpinner.setPreferredSize(new Dimension (40, 20));
		
		
		JPanel panel2 = new JPanel(flowLayout);
		panel2.add(new JLabel ("caps/cage:"));
		panel2.add(nCapillariesPerCage);
		cagesJCombo.setPreferredSize(new Dimension (60, 20));
		nCapillariesPerCage.setPreferredSize(new Dimension (40, 20));
		panel2.add(new JLabel ("with:"));
		panel2.add(width_between_capillariesJSpinner);
		width_between_capillariesJSpinner.setPreferredSize(new Dimension (40, 20));
		panel2.add(width_between_capillariesLabel);
		panel2.add(width_intervalJSpinner);
		width_intervalJSpinner.setPreferredSize(new Dimension (40, 20));
		panel2.add(width_intervalLabel);		
		JPanel panel1 = new JPanel(flowLayout);
		
		panel1.add(createROIsFromPolygonButton2);
				
		add(panel0);
		add(panel2);
		add(panel1);
		
//		buttonGroup2.add(selectGroupedby2Button);
//		buttonGroup2.add(selectEvenlySpacedButton);
//		selectGroupedby2Button.setSelected(true);
		
		defineDlgItemsListeners();
		this.parent0 = parent0;
	}
	
	private void defineDlgItemsListeners() 
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
		
		nCapillariesPerCage.addChangeListener(new ChangeListener() {
		    @Override
		    public void stateChanged(ChangeEvent e) {
		    	boolean status = (int) nCapillariesPerCage.getValue() == 2? true:false;
		        EnableBinWidthItems(status);
		    	nCapillariesPerCage.requestFocus();
		    }});
		
//		selectEvenlySpacedButton.addActionListener(new ActionListener () 
//		{ 
//			@Override public void actionPerformed( final ActionEvent e ) { 
//				EnableBinWidthItems(false);
//			}});
//		
//		selectGroupedby2Button.addActionListener(new ActionListener () 
//		{ 
//			@Override public void actionPerformed( final ActionEvent e ) { 
//				EnableBinWidthItems(true);
//			}});
	}
	
	private void EnableBinWidthItems(boolean status) {
		width_between_capillariesJSpinner.setEnabled(status);
		width_between_capillariesLabel.setEnabled(status);
		width_intervalJSpinner.setEnabled(status);
		width_intervalLabel.setEnabled(status);
	}
	
	// set/ get	

	private int getNbCapillaries( ) 
	{
		int nCapillaries = (int) nCapillariesPerCage.getValue();
		// cagesJCombo 		= new JComboBox<String> (new String[] {"10", "4+(2)", "1+(2)"});
		int selectedCagesArrangement = cagesJCombo.getSelectedIndex();
		switch (selectedCagesArrangement) {
		case 2: // "4+ (2)"
			nCapillaries = nCapillaries * 4 + 2*2; // 
			break;
		case 3: // "1+(2)"
			break;
		case 1:
		default: //"10"
			nCapillaries = nCapillaries * 10;
			break;
		}
		return nCapillaries;
	}

	private int getWidthSmallInterval ( ) 
	{
		return (int) width_between_capillariesJSpinner.getValue();
	}
	
	private int getWidthLongInterval() 
	{
		return (int) width_intervalJSpinner.getValue();
	}
	
//	private boolean getGroupedBy2() 
//	{
//		return selectGroupedby2Button.isSelected();
//	}
	
	void setGroupedBy2(boolean flag) 
	{
//		buttonGroup2.clearSelection();
//		selectGroupedby2Button.setSelected(flag);
//		selectEvenlySpacedButton.setSelected(!flag);
		int nCapillaries = flag? 2:1;
		nCapillariesPerCage.setValue(nCapillaries);
	}
	
	void setGroupingAndNumber(Capillaries cap) 
	{
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
	
	Capillaries setCapillariesGrouping(Capillaries cap) 
	{
		cap.capillariesDescription.grouping = getCapillariesGrouping();
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
		int isel = 0; //orientationJCombo.getSelectedIndex();
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
		boolean statusGroup2Mode = (getCapillariesGrouping() == 2);
		
		int nbcapillaries = 20;
		int width_between_capillaries = 1;	
		int width_interval = 0;

		try 
		{ 
			nbcapillaries = getNbCapillaries();
			if(statusGroup2Mode) 
			{
				width_between_capillaries = getWidthSmallInterval();
				width_interval = getWidthLongInterval();
			}
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
