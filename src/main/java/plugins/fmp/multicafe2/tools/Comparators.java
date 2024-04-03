package plugins.fmp.multicafe2.tools;

import java.util.Comparator;

import icy.roi.ROI;
import icy.roi.ROI2D;
import icy.sequence.Sequence;
import plugins.fmp.multicafe2.experiment.Cage;
import plugins.fmp.multicafe2.experiment.Experiment;
import plugins.fmp.multicafe2.experiment.FlyPositions;
import plugins.fmp.multicafe2.experiment.FlyCoordinates;
import plugins.fmp.multicafe2.tools.toExcel.XLSResults;


public class Comparators 
{
	public static class ROI_Name_Comparator implements Comparator<ROI> {
		@Override
		public int compare(ROI o1, ROI o2) 
		{
			return o1.getName().compareTo(o2.getName());
		}
	}
	
	public static class ROI2D_Name_Comparator implements Comparator<ROI2D> 
	{
		@Override
		public int compare(ROI2D o1, ROI2D o2) 
		{
			return o1.getName().compareTo(o2.getName());
		}
	}
	
	public static class ROI2D_T_Comparator implements Comparator<ROI2D> 
	{
		@Override
		public int compare(ROI2D o1, ROI2D o2) 
		{
			return o1.getT()-o2.getT();
		}
	}

	public static class Sequence_Name_Comparator implements Comparator<Sequence> 
	{
		@Override
		public int compare(Sequence o1, Sequence o2) 
		{
			return o1.getName().compareTo(o2.getName());
		}
	}
	
	public static class XLSResults_Name_Comparator implements Comparator <XLSResults> 
	{
		@Override
		public int compare (XLSResults o1, XLSResults o2) 
		{
			return o1.name.compareTo(o2.name);
		}
	}
	
	public static class XYTaSeries_Name_Comparator implements Comparator <FlyPositions> 
	{
		@Override
		public int compare (FlyPositions o1, FlyPositions o2) 
		{
			return o1.name.compareTo(o2.name);
		}
	}
	
	public static class Cage_Name_Comparator implements Comparator <Cage> 
	{
		@Override
		public int compare (Cage o1, Cage o2) 
		{
			return o1.cageRoi2D.getName().compareTo(o2.cageRoi2D.getName());
		}
	}
	
	public static class XYTaValue_Tindex_Comparator implements Comparator <FlyCoordinates> 
	{
		@Override
		public int compare (FlyCoordinates o1, FlyCoordinates o2) 
		{
			return o1.indexT - o2.indexT;
		}
	}
	
	public static class Experiment_Start_Comparator implements Comparator <Experiment>
	{
		@Override
		public int compare(Experiment exp1, Experiment exp2)
		{
			return Long.compare(exp1.camImageFirst_ms + exp1.kymoFirst_ms, 
					exp2.camImageFirst_ms + exp2.kymoFirst_ms);
		}
	}
}
