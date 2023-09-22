package plugins.fmp.multicafe2.tools.toExcel;

import java.io.FileOutputStream;
import java.io.IOException;
import org.apache.poi.ss.util.CellReference;
import icy.gui.frame.progress.ProgressFrame;
import plugins.fmp.multicafe2.experiment.Experiment;



public class XLSExportCapillariesResults extends XLSExport 
{	
	public void exportToFile(String filename, XLSExportOptions opt) 
	{	
		System.out.println("XLS capillary measures output");
		options = opt;
		expList = options.expList;
		
		boolean loadCapillaries = true;
		boolean loadDrosoTrack =  options.onlyalive;
		expList.loadListOfMeasuresFromAllExperiments(loadCapillaries, loadDrosoTrack);
		expList.chainExperimentsUsingKymoIndexes(options.collateSeries);
		expList.setFirstImageForAllExperiments(options.collateSeries);
		expAll = expList.get_MsTime_of_StartAndEnd_AllExperiments(options);
	
		ProgressFrame progress = new ProgressFrame("Export data to Excel");
		int nbexpts = expList.getItemCount();
		progress.setLength(nbexpts);

		try 
		{ 
			int column = 1;
			int iSeries = 0;
			workbook = xlsInitWorkbook();
			for (int index = options.firstExp; index <= options.lastExp; index++) 
			{
				Experiment exp = expList.getItemAt(index);
				if (exp.chainToPreviousExperiment != null)
					continue;
				progress.setMessage("Export experiment "+ (index+1) +" of "+ nbexpts);
				String charSeries = CellReference.convertNumToColString(iSeries);
				
				if (options.topLevel) 
				{	
					getDataAndExport(exp, column, charSeries, EnumXLSExportType.TOPRAW);
					getDataAndExport(exp, column, charSeries, EnumXLSExportType.TOPLEVEL);
				}
				
				if (options.lrPI && options.topLevel) 		
					getDataAndExport(exp, column, charSeries, EnumXLSExportType.TOPLEVEL_LR);
				if (options.topLevelDelta) 	
					getDataAndExport(exp, column, charSeries, EnumXLSExportType.TOPLEVELDELTA);
				if (options.lrPI && options.topLevelDelta) 	
					getDataAndExport(exp, column, charSeries, EnumXLSExportType.TOPLEVELDELTA_LR);
				

				if (options.bottomLevel) 	
					getDataAndExport(exp, column, charSeries, EnumXLSExportType.BOTTOMLEVEL);		
				if (options.derivative) 	
					getDataAndExport(exp, column, charSeries, EnumXLSExportType.DERIVEDVALUES);	
				
				if (!options.collateSeries || exp.chainToPreviousExperiment == null)
					column += expList.maxSizeOfCapillaryArrays +2;
				iSeries++;
				progress.incPosition();
			}
			progress.setMessage( "Save Excel file to disk... ");
			FileOutputStream fileOut = new FileOutputStream(filename);
			workbook.write(fileOut);
	        fileOut.close();
	        workbook.close();
	        progress.close();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		System.out.println("XLS output finished");
	}
	
}
