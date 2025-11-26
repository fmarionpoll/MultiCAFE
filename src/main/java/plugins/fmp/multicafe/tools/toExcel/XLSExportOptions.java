package plugins.fmp.multicafe.tools.toExcel;

import plugins.fmp.multicafe.tools.JComponents.ExperimentsJComboBox;

public class XLSExportOptions {
	public boolean xyImage = true;
	public boolean xyCage = true;
	public boolean xyCapillaries = true;
	public boolean ellipseAxes = false;

	public boolean distance = false;
	public boolean alive = true;
	public boolean sleep = true;
	public int sleepThreshold = 5;

	public boolean topLevel = true;
	public boolean topLevelDelta = false;
	public boolean bottomLevel = false;
	public boolean derivative = false;
	public boolean lrPI = true;
	public double lrPIThreshold = 0.;

	public boolean markovChain = false;
	public boolean autocorrelation = false;
	public boolean crosscorrelation = false;
	public boolean crosscorrelationLR = false;
	public int nbinscorrelation = 40;

	public boolean sumPerCage = true;
	public boolean t0 = true;
	public boolean onlyalive = true;
	public boolean subtractEvaporation = true;

	public boolean sumGulps = false;
	public boolean nbGulps = false;
	public boolean amplitudeGulps = false;
	public boolean tToNextGulp = false;
	public boolean tToNextGulp_LR = false;

	public boolean transpose = false;
	public boolean duplicateSeries = true;
	public int buildExcelStepMs = 1;
	public int buildExcelUnitMs = 1;
	public boolean fixedIntervals = false;
	public long startAll_Ms = 0;
	public long endAll_Ms = 999999;
	public boolean exportAllFiles = true;
	public boolean absoluteTime = false;
	public boolean collateSeries = false;
	public boolean padIntervals = true;

	public int firstExp = -1;
	public int lastExp = -1;
	public ExperimentsJComboBox expList = null;

	// internal parameters
	public boolean trim_alive = false;
	public boolean compensateEvaporation = false;
	public EnumXLSExport exportType;
}
