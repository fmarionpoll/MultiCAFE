package plugins.fmp.multicafe.tools1.toExcel;

import plugins.fmp.multicafe.fmp_experiment.Experiment;
import plugins.fmp.multicafe.tools0.toExcel.EnumXLSExport;

public class XLSExportGulpsResults extends XLSExportCapillariesResults {
	// -----------------------

	@Override
	protected int processExperiment(Experiment exp, int col0, String charSeries) {
		if (options.derivative)
			getCapillaryDataAndExport(exp, col0, charSeries, EnumXLSExport.DERIVEDVALUES);
		if (options.sumGulps)
			getCapillaryDataAndExport(exp, col0, charSeries, EnumXLSExport.SUMGULPS);
		if (options.lrPI && options.sumGulps)
			getCapillaryDataAndExport(exp, col0, charSeries, EnumXLSExport.SUMGULPS_LR);
		if (options.nbGulps)
			getCapillaryDataAndExport(exp, col0, charSeries, EnumXLSExport.NBGULPS);
		if (options.amplitudeGulps)
			getCapillaryDataAndExport(exp, col0, charSeries, EnumXLSExport.AMPLITUDEGULPS);
		if (options.tToNextGulp)
			getCapillaryDataAndExport(exp, col0, charSeries, EnumXLSExport.TTOGULP);
		if (options.tToNextGulp_LR)
			getCapillaryDataAndExport(exp, col0, charSeries, EnumXLSExport.TTOGULP_LR);

		if (options.markovChain)
			getCapillaryDataAndExport(exp, col0, charSeries, EnumXLSExport.MARKOV_CHAIN);

		if (options.autocorrelation) {
			getCapillaryDataAndExport(exp, col0, charSeries, EnumXLSExport.AUTOCORREL);
			getCapillaryDataAndExport(exp, col0, charSeries, EnumXLSExport.AUTOCORREL_LR);
		}
		if (options.crosscorrelation) {
			getCapillaryDataAndExport(exp, col0, charSeries, EnumXLSExport.CROSSCORREL);
			getCapillaryDataAndExport(exp, col0, charSeries, EnumXLSExport.CROSSCORREL_LR);
		}

		if (!options.collateSeries || exp.chainToPreviousExperiment == null)
			return col0 + expList.maxSizeOfCapillaryArrays + 2;

		return col0;
	}

}
