package plugins.fmp.multicafe.tools.toExcel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class XLSResults {
	public String name = null;
	String stimulus = null;
	String concentration = null;
	int nadded = 1;
	boolean[] padded_out = null;

	public int dimension = 0;
	public int nflies = 1;
	public int cellID = 0;
	public EnumXLSExportType exportType = null;
	public ArrayList<Integer> dataInt = null;
	public double[] valuesOut = null;

	public XLSResults(String name, int nflies, int cageID, EnumXLSExportType exportType) {
		this.name = name;
		this.nflies = nflies;
		this.cellID = cageID;
		this.exportType = exportType;
	}

	public XLSResults(String name, int nflies, int cageID, EnumXLSExportType exportType, int nFrames) {
		this.name = name;
		this.nflies = nflies;
		this.cellID = cageID;
		this.exportType = exportType;
		initValuesArray(nFrames);
	}

	void initValuesOutArray(int dimension, Double val) {
		this.dimension = dimension;
		valuesOut = new double[dimension];
		Arrays.fill(valuesOut, val);
	}

	private void initValuesArray(int dimension) {
		this.dimension = dimension;
		valuesOut = new double[dimension];
		Arrays.fill(valuesOut, Double.NaN);
		padded_out = new boolean[dimension];
		Arrays.fill(padded_out, false);
	}

	void clearValues(int fromindex) {
		int toindex = valuesOut.length;
		if (fromindex > 0 && fromindex < toindex) {
			Arrays.fill(valuesOut, fromindex, toindex, Double.NaN);
			Arrays.fill(padded_out, fromindex, toindex, false);
		}
	}

	void clearAll() {
		dataInt = null;
		valuesOut = null;
		nflies = 0;
	}

	public void transferDataIntToValuesOut(double scalingFactorToPhysicalUnits, EnumXLSExportType xlsExport) {
		if (dimension == 0 || dataInt == null || dataInt.size() < 1)
			return;

		boolean removeZeros = false;
		if (xlsExport == EnumXLSExportType.AMPLITUDEGULPS)
			removeZeros = true;

		int len = Math.min(dimension, dataInt.size());
		if (removeZeros) {
			for (int i = 0; i < len; i++) {
				int ivalue = dataInt.get(i);
				valuesOut[i] = (ivalue == 0 ? Double.NaN : ivalue) * scalingFactorToPhysicalUnits;
			}
		} else {
			for (int i = 0; i < len; i++)
				valuesOut[i] = dataInt.get(i) * scalingFactorToPhysicalUnits;
		}
	}

	public void copyValuesOut(XLSResults sourceRow) {
		if (sourceRow.valuesOut.length != valuesOut.length) {
			this.dimension = sourceRow.dimension;
			valuesOut = new double[dimension];
		}
		for (int i = 0; i < dimension; i++)
			valuesOut[i] = sourceRow.valuesOut[i];
	}

	public List<Integer> subtractT0() {
		if (dataInt == null || dataInt.size() < 1)
			return null;
		int item0 = dataInt.get(0);
		for (int index = 0; index < dataInt.size(); index++) {
			int value = dataInt.get(index);
			dataInt.set(index, value - item0);
		}
		return dataInt;
	}

	boolean subtractDeltaT(int arrayStep, int binStep) {
		if (valuesOut == null || valuesOut.length < 2)
			return false;
		for (int index = 0; index < valuesOut.length; index++) {
			int timeIndex = index * arrayStep + binStep;
			int indexDelta = (int) (timeIndex / arrayStep);
			if (indexDelta < valuesOut.length)
				valuesOut[index] = valuesOut[indexDelta] - valuesOut[index];
			else
				valuesOut[index] = Double.NaN;
		}
		return true;
	}

	void addDataToValOutEvap(XLSResults result) {
		if (result.valuesOut.length > valuesOut.length) {
			System.out.println("XLSResults:addDataToValOutEvap() Error: from len=" + result.valuesOut.length
					+ " to len=" + valuesOut.length);
			return;
		}

		for (int i = 0; i < result.valuesOut.length; i++) {
			valuesOut[i] += result.valuesOut[i];
		}
		nflies++;
	}

	void averageEvaporation() {
		if (nflies == 0)
			return;

		for (int i = 0; i < valuesOut.length; i++)
			valuesOut[i] = valuesOut[i] / nflies;
		nflies = 1;
	}

	void subtractEvap(XLSResults evap) {
		if (valuesOut == null)
			return;
		int len = Math.min(valuesOut.length, evap.valuesOut.length);
		for (int i = 0; i < len; i++) {
			valuesOut[i] -= evap.valuesOut[i];
		}
	}

	void sumValues_out(XLSResults dataToAdd) {
		int len = Math.min(valuesOut.length, dataToAdd.valuesOut.length);
		for (int i = 0; i < len; i++) {
			valuesOut[i] += dataToAdd.valuesOut[i];
		}
		nadded += 1;
	}

}
