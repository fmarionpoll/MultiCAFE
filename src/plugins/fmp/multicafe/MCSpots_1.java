package plugins.fmp.multicafe;


import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import icy.gui.frame.IcyFrame;
import icy.gui.util.GuiUtil;
import icy.gui.viewer.Viewer;
import icy.image.IcyBufferedImage;
import icy.type.collection.array.Array1DUtil;
import plugins.fmp.multicafeSequence.Experiment;
import plugins.fmp.multicafeSequence.SequenceKymos;
import plugins.fmp.multicafeTools.ImageTransformTools.TransformOp;


public class MCSpots_1 extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7614659720134775871L;
	private MultiCAFE 	parent0 				= null;
	private JButton 	subtractButton 			= new JButton("Subtract first column");
	private JButton 	buildHistogramButton 	= new JButton("Build histogram");
	private double [][] stdXArray = null;
	private double [][] stdYArray = null;
	IcyFrame mainChartFrame = null;
	
	
	
	void init(GridLayout capLayout, MultiCAFE parent0) {
		setLayout(capLayout);
		this.parent0 = parent0;
		add( GuiUtil.besidesPanel(subtractButton));
		add( GuiUtil.besidesPanel(buildHistogramButton));		
		defineActionListeners();
	}
	
	private void defineActionListeners() {
		subtractButton.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
				Experiment exp = parent0.expList.getExperiment(parent0.currentExperimentIndex);
				subtractFirstColumn(exp);
			}});
		buildHistogramButton.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
				Experiment exp = parent0.expList.getExperiment(parent0.currentExperimentIndex);
				buildHistogram(exp);
			}});
	}
	
	// -------------------------------------------------
	
	void subtractFirstColumn(Experiment exp) {
		SequenceKymos seqKymos = exp.seqKymos;
		if (seqKymos == null)
			return;
		TransformOp transform = TransformOp.SUBFIRSTCOL;
//		List<Capillary> capList = exp.capillaries.capillariesArrayList;
//		for (int t=0; t < exp.seqKymos.seq.getSizeT(); t++) {
//			getInfosFromDialog(capList.get(t));		
//		}
		int zChannelDestination = 1;
		exp.kymosBuildFiltered(0, zChannelDestination, transform, 0);
		seqKymos.seq.getFirstViewer().getCanvas().setPositionZ(zChannelDestination);
	}
	
	void buildHistogram(Experiment exp) {
		SequenceKymos seqKymos = exp.seqKymos;
		if (seqKymos == null)
			return;
		
		int imageIndex = 0;
		getSTD (seqKymos, imageIndex);
//		getSTDRBminus2G();
		graphDisplay2Panels(exp, stdXArray, stdYArray);
//		TransformOp transform = TransformOp.SUBFIRSTCOL;
////		List<Capillary> capList = exp.capillaries.capillariesArrayList;
////		for (int t=0; t < exp.seqKymos.seq.getSizeT(); t++) {
////			getInfosFromDialog(capList.get(t));		
////		}
//		int zChannelDestination = 1;
//		exp.kymosBuildFiltered(0, zChannelDestination, transform, 0);
//		seqKymos.seq.getFirstViewer().getCanvas().setPositionZ(zChannelDestination);
	}
	
	private void getSTD (SequenceKymos seqKymos, int imageIndex) {
		int height = seqKymos.seq.getSizeY();
		int width = seqKymos.seq.getSizeX();
		Rectangle rect = new Rectangle(0, 0, width, height);
		
		Point2D.Double [] refpoint = new Point2D.Double [4];
		refpoint [0] = new Point2D.Double (rect.x, 					rect.y);
		refpoint [1] = new Point2D.Double (rect.x, 					rect.y + rect.height - 1);
		refpoint [2] = new Point2D.Double (rect.x + rect.width - 1, rect.y + rect.height - 1);
		refpoint [3] = new Point2D.Double (rect.x + rect.width - 1, rect.y );
		
		int nYpoints = (int) (refpoint[1].y - refpoint[0].y +1); 
		int nXpoints = (int) (refpoint[3].x - refpoint[0].x +1); 
		double [][] sumXArray = new double [nXpoints][3];
		double [][] sum2XArray = new double [nXpoints][3];
		double [][] countXArray = new double [nXpoints][3];
		stdXArray = new double [nXpoints][4];
		double [][] sumYArray = new double [nYpoints][3];
		double [][] sum2YArray = new double [nYpoints][3];
		double [][] countYArray = new double [nYpoints][3];
		stdYArray = new double [nYpoints][4];
		
		for (int chan= 0; chan< 3; chan++) {
			IcyBufferedImage virtualImage = seqKymos.getImage(imageIndex, chan) ;
			if (virtualImage == null) {
				System.out.println("An error occurred while reading image: " + seqKymos.currentFrame );
				return;
			}
			int widthImage = virtualImage.getSizeX();
			double [] image1DArray = Array1DUtil.arrayToDoubleArray(virtualImage.getDataXY(0), virtualImage.isSignedDataType());
			double deltaXUp 	= (refpoint[3].x - refpoint[0].x +1);
			double deltaXDown 	= (refpoint[2].x - refpoint[1].x +1);
			double deltaYUp 	= (refpoint[3].y - refpoint[0].y +1);
			double deltaYDown 	= (refpoint[2].y - refpoint[1].y +1);
			
			for (int ix = 0; ix < nXpoints; ix++) {
				double xUp 		= refpoint[0].x + deltaXUp * ix / nXpoints;
				double yUp 		= refpoint[0].y + deltaYUp * ix / nXpoints;
				double xDown 	= refpoint[1].x + deltaXDown * ix / nXpoints;
				double yDown 	= refpoint[1].y + deltaYDown * ix / nXpoints;

				for (int iy = 0; iy < nYpoints; iy++) {
					double x = xUp + (xDown - xUp +1) * iy / nYpoints;
					double y = yUp + (yDown - yUp +1) * iy / nYpoints;
					
					int index = (int) x + ((int) y* widthImage);
					double value = image1DArray[index];
					double value2 = value*value;
					
					sumXArray[ix][chan] = sumXArray[ix][chan] + value;
					sum2XArray[ix][chan] = sum2XArray[ix][chan] + value2;
					countXArray[ix][chan] = countXArray[ix][chan] + 1;
					
					sumYArray[iy][chan] = sumYArray[iy][chan] + value;
					sum2YArray[iy][chan] = sum2YArray[iy][chan] + value2;
					countYArray[iy][chan] = countYArray[iy][chan] +1;
				}
			}
		}
		
		// compute variance
		for (int chan = 0; chan <3; chan++) {
			for (int ix = 0; ix < nXpoints; ix++) {
				double n 		= countXArray[ix][chan];
				double sum2 	= sum2XArray[ix][chan];
				double sumsum 	= sumXArray[ix][chan];
				sumsum 			= sumsum*sumXArray[ix][chan]/n;
				stdXArray[ix][chan] = (sum2 - sumsum)/(n-1);
			}
			
			for (int iy = 0; iy < nYpoints; iy++) {
				double n 		= countYArray[iy][chan];
				double sum2 	= sum2YArray[iy][chan];
				double sumsum 	= sumYArray[iy][chan];
				sumsum 			= sumsum*sumYArray[iy][chan]/n;
				stdYArray[iy][chan] = (sum2 - sumsum)/(n-1);
			}
		}
	}
	
	private void getSTDRBminus2G() {	
		for (int i=0; i < stdXArray.length; i++) 
			stdXArray[i][3] = stdXArray [i][0]+stdXArray[i][2]-2*stdXArray[i][1];
		for (int i=0; i < stdYArray.length; i++) 
			stdYArray[i][3] = stdYArray[i][0]+stdYArray[i][2]-2*stdYArray[i][1];	
	}
	
	private void graphDisplay2Panels (Experiment exp, double [][] arrayX, double [][] arrayY) {
		if (mainChartFrame != null) {
			mainChartFrame.removeAll();
			mainChartFrame.close();
		}

		final JPanel mainPanel = new JPanel(); 
		mainPanel.setLayout( new BoxLayout( mainPanel, BoxLayout.LINE_AXIS ) );
		String localtitle = "Variance along X and Y";
		mainChartFrame = GuiUtil.generateTitleFrame(localtitle, 
				new JPanel(), new Dimension(1400, 800), true, true, true, true);	

		int totalpoints = 0;
		ArrayList<XYSeriesCollection> xyDataSetList = new ArrayList <XYSeriesCollection>();
		XYSeriesCollection xyDataset = graphCreateXYDataSet(arrayX, "X chan ");
		xyDataSetList.add(xyDataset);
		totalpoints += xyDataset.getSeries(0).getItemCount();
		xyDataset = graphCreateXYDataSet(arrayY, "Y chan ");
		xyDataSetList.add(xyDataset);
		totalpoints += xyDataset.getSeries(0).getItemCount();
		
		for (int i=0; i<xyDataSetList.size(); i++) {
			xyDataset = xyDataSetList.get(i);
			int npoints = xyDataset.getSeries(0).getItemCount();
			JFreeChart xyChart = ChartFactory.createXYLineChart(null, null, null, xyDataset, PlotOrientation.VERTICAL, true, true, true);
			xyChart.setAntiAlias( true );
			xyChart.setTextAntiAlias( true );
			int drawWidth =  npoints * 800 / totalpoints;
			int drawHeight = 400;
			ChartPanel xyChartPanel = new ChartPanel(xyChart, drawWidth, drawHeight, drawWidth, drawHeight, drawWidth, drawHeight, false, false, true, true, true, true);
			mainPanel.add(xyChartPanel);
		}

		mainChartFrame.add(mainPanel);
		mainChartFrame.pack();
		
		SequenceKymos seqKymos = exp.seqKymos;
		Viewer v = seqKymos.seq.getFirstViewer();
		Rectangle rectv = v.getBounds();
		Point pt = new Point((int) rectv.getX(), (int) rectv.getY()+30);
		mainChartFrame.setLocation(pt);

		mainChartFrame.setVisible(true);
		mainChartFrame.addToDesktopPane ();
		mainChartFrame.requestFocus();
	}
	
	private XYSeriesCollection graphCreateXYDataSet(double [][] array, String rootName) {
		XYSeriesCollection xyDataset = new XYSeriesCollection();
		for (int chan = 0; chan < 4; chan++) {
			XYSeries seriesXY = new XYSeries(rootName+chan);
			if (chan == 3)
				seriesXY.setDescription("1-2 + 3-2");
			int len = array.length;
			for ( int i = 0; i < len;  i++ ) {
				double value = array[i][chan];
				seriesXY.add( i, value);
			}
			xyDataset.addSeries(seriesXY );
		}
		return xyDataset;
	}
}
