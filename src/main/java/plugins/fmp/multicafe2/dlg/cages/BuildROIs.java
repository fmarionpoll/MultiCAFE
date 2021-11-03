package plugins.fmp.multicafe2.dlg.cages;

import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;

import icy.gui.frame.progress.AnnounceFrame;
import icy.gui.util.GuiUtil;
import icy.roi.ROI2D;
import icy.type.geom.Polygon2D;
import plugins.fmp.multicafe2.MultiCAFE2;
import plugins.fmp.multicafe2.experiment.Experiment;
import plugins.fmp.multicafe2.experiment.SequenceCamData;
import plugins.fmp.multicafe2.tools.ROI2DUtilities;
import plugins.kernel.roi.roi2d.ROI2DPolygon;

public class BuildROIs extends JPanel 
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -5257698990389571518L;
	private JButton addPolygon2DButton 			= new JButton("Draw Polygon2D");
	private JButton createROIsFromPolygonButton = new JButton("Create/add (from Polygon 2D)");
	private JSpinner nColumnsTextField 			= new JSpinner(new SpinnerNumberModel(10, 0, 10000, 1));
	private JSpinner width_cageTextField 		= new JSpinner(new SpinnerNumberModel(20, 0, 10000, 1));
	private JSpinner width_intervalTextField 	= new JSpinner(new SpinnerNumberModel(3, 0, 10000, 1));
	private JSpinner nRowsTextField 			= new JSpinner(new SpinnerNumberModel(1, 0, 10000, 1));
	
	private int 	ncolumns 					= 10;
	private int 	nrows 						= 1;
	private int 	width_cage 					= 10;
	private int 	width_interval 				= 2;

	private MultiCAFE2 parent0;
	
	void init(GridLayout capLayout, MultiCAFE2 parent0) 
	{
		setLayout(capLayout);
		this.parent0 = parent0;
		
		add( GuiUtil.besidesPanel(addPolygon2DButton, createROIsFromPolygonButton));
		JLabel nColumnsLabel = new JLabel("N columns ");
		JLabel nRowsLabel = new JLabel("N rows ");
		JLabel cagewidthLabel = new JLabel("cage width ");
		JLabel btwcagesLabel = new JLabel("between cages ");
		nColumnsLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		cagewidthLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		btwcagesLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		nRowsLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		add( GuiUtil.besidesPanel( cagewidthLabel,  width_cageTextField, nColumnsLabel, nColumnsTextField));
		add( GuiUtil.besidesPanel( btwcagesLabel, width_intervalTextField, nRowsLabel, nRowsTextField));
		
		defineActionListeners();
	}
	
	private void defineActionListeners() 
	{
		
		createROIsFromPolygonButton.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{ 
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null) 
				{
					ROI2DUtilities.removeRoisContainingString(-1, "cage", exp.seqCamData.seq);
					exp.cages.removeCages();
					createROIsFromSelectedPolygon(exp);
					exp.cages.getCagesFromROIs(exp.seqCamData);
					exp.cages.setFirstAndLastCageToZeroFly();
				}
			}});
		
		addPolygon2DButton.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{ 
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null)
					create2DPolygon(exp);
			}});
	}
	
	void updateNColumnsFieldFromSequence() 
	{
		Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
		if (exp != null) 
		{
			int nrois = exp.cages.cagesList.size();	
			if (nrois > 0) {
				nColumnsTextField.setValue(nrois);
				ncolumns = nrois;
			}
		}
	}

	private void create2DPolygon(Experiment exp) 
	{
		final String dummyname = "perimeter_enclosing";
		ArrayList<ROI2D> listRois = exp.seqCamData.seq.getROI2Ds();
		for (ROI2D roi: listRois) 
		{
			if (roi.getName() .equals(dummyname))
				return;
		}

		Rectangle rect = exp.seqCamData.seq.getBounds2D();
		List<Point2D> points = new ArrayList<Point2D>();
		int rectleft = rect.x + rect.width /6;
		int rectright = rect.x + rect.width*5 /6;
		int recttop = rect.y + rect.height *2/3; 
		if (exp.capillaries.capillariesList.size() > 0) 
		{
			Rectangle bound0 = exp.capillaries.capillariesList.get(0).getRoi().getBounds();
			int last = exp.capillaries.capillariesList.size() - 1;
			Rectangle bound1 = exp.capillaries.capillariesList.get(last).getRoi().getBounds();
			rectleft = bound0.x;
			rectright = bound1.x + bound1.width;
			int diff = (rectright - rectleft)*2/60;
			rectleft -= diff;
			rectright += diff;
			recttop = bound0.y+ bound0.height- (bound0.height /8);
		}
		
		points.add(new Point2D.Double(rectleft, recttop));
		points.add(new Point2D.Double(rectright, recttop));
		points.add(new Point2D.Double(rectright, rect.y + rect.height - 4));
		points.add(new Point2D.Double(rectleft, rect.y + rect.height - 4 ));
		ROI2DPolygon roi = new ROI2DPolygon(points);
		roi.setName(dummyname);
		exp.seqCamData.seq.addROI(roi);
		exp.seqCamData.seq.setSelectedROI(roi);
	}
		
	private void createROIsFromSelectedPolygon(Experiment exp) 
	{
		// read values from text boxes
		try 
		{ 
			ncolumns = (int) nColumnsTextField.getValue();
			nrows = (int) nRowsTextField.getValue();
			width_cage = (int) width_cageTextField.getValue();
			width_interval = (int) width_intervalTextField.getValue();
		} 
		catch( Exception e ) 
		{ 
			new AnnounceFrame("Can't interpret one of the ROI parameters value"); 
		}

		SequenceCamData seqCamData = exp.seqCamData;
		ROI2D roi = seqCamData.seq.getSelectedROI2D();
		boolean flag = (roi.getName().length() > 4 && roi.getName().substring( 0 , 4 ).contains("cage"));
		if ( ! ( roi instanceof ROI2DPolygon ) || flag) 
		{
			if ( ! ( roi instanceof ROI2DPolygon ) ) 
				new AnnounceFrame("The frame must be a ROI2D POLYGON");
			if (flag) 
				new AnnounceFrame("The roi name should not contain -cage-");
			return;
		}
		
		Polygon2D roiPolygonMin = ROI2DUtilities.orderVerticesofPolygon (((ROI2DPolygon) roi).getPolygon());
		seqCamData.seq.removeROI(roi);

		// generate cage frames
		int iRoot = exp.cages.removeAllRoiCagesFromSequence(exp.seqCamData);
		String cageRoot = "cage";
		
		Polygon2D roiPolygon = ROI2DUtilities.inflate( roiPolygonMin, ncolumns, nrows, width_cage, width_interval);
		
		double deltax_top = (roiPolygon.xpoints[3]- roiPolygon.xpoints[0]) / ncolumns;
		double deltax_bottom = (roiPolygon.xpoints[2]- roiPolygon.xpoints[1]) / ncolumns;
		double deltay_top = (roiPolygon.ypoints[3]- roiPolygon.ypoints[0]) / ncolumns ;
		double deltay_bottom = (roiPolygon.ypoints[2]- roiPolygon.ypoints[1]) / ncolumns;
		
		for (int i=0; i< ncolumns; i++) 
		{
			double x0i = roiPolygon.xpoints[0] + deltax_top * i;
			double x1i = roiPolygon.xpoints[1] + deltax_bottom * i;
			double x3i = x0i + deltax_top;
			double x2i = x1i + deltax_bottom;
			
			double y0i = roiPolygon.ypoints[0] + deltay_top * i ;
			double y1i = roiPolygon.ypoints[1] + deltay_bottom * i ;
			double y3i = y0i + deltay_top ;
			double y2i = y1i + deltay_bottom;
			
			for (int j = 0; j < nrows; j++) 
			{
				double deltax_left = (x1i - x0i) / nrows;
				double deltax_right = (x2i - x3i) / nrows;
				double deltay_left = (y1i - y0i) / nrows;
				double deltay_right = (y2i - y3i) / nrows;
				
				double x0ij = x0i + deltax_left *j;
				double x1ij = x0ij + deltax_left;
				double x3ij = x3i + deltax_right * j;
				double x2ij = x3ij + deltax_right;
				
				double y0ij = y0i + deltay_left * j;
				double y1ij = y0ij + deltay_left ;
				double y3ij = y3i + deltay_right * j;
				double y2ij = y3ij + deltay_right;
				
				// shrink by
				double xspacer_top =  (x3ij - x0ij) * width_interval / (width_cage + 2 * width_interval);				
				double xspacer_bottom = (x2ij - x1ij) * width_interval / (width_cage + 2 * width_interval);
				double yspacer_left =  (y1ij - y0ij) * width_interval / (width_cage + 2 * width_interval);				
				double yspacer_right = (y2ij - y3ij) * width_interval / (width_cage + 2 * width_interval);
				
				// define intersection
				List<Point2D> points = new ArrayList<>();

				Point2D point0 = ROI2DUtilities.lineIntersect(
						x0ij + xspacer_top, 	y0ij, 
						x1ij + xspacer_bottom, 	y1ij,  
						x0ij, 					y0ij + yspacer_left, 
						x3ij, 					y3ij + yspacer_right);
				points.add(point0);

				Point2D point1 = ROI2DUtilities.lineIntersect(
						x1ij, 					y1ij - yspacer_left, 
						x2ij, 					y2ij - yspacer_right,  
						x0ij + xspacer_top, 	y0ij, 
						x1ij+ xspacer_bottom, 	y1ij);
				points.add(point1);

				Point2D point2 = ROI2DUtilities.lineIntersect(
						x1ij, 					y1ij - yspacer_left, 
						x2ij, 					y2ij - yspacer_right, 
						x3ij-xspacer_top, 		y3ij, 
						x2ij-xspacer_bottom, 	y2ij);
				points.add(point2);

				Point2D point3 = ROI2DUtilities.lineIntersect(
						x0ij, 					y0ij + yspacer_left, 
						x3ij, 					y3ij + yspacer_right, 
						x3ij-xspacer_top, 		y3ij, 
						x2ij-xspacer_bottom, 	y2ij);
				points.add(point3);
	
				ROI2DPolygon roiP = new ROI2DPolygon (points);
				roiP.setName(cageRoot+String.format("%03d", iRoot));
				iRoot++;
				seqCamData.seq.addROI(roiP);
			}
		}
	}

}
