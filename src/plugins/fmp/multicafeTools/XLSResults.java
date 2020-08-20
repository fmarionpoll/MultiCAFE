package plugins.fmp.multicafeTools;


import java.util.Arrays;
import java.util.List;


public class XLSResults {
	String				name 		= null;
	String 				stimulus	= null;
	String 				concentration = null;
	int 				nadded		= 1;
	int					nflies		= 1;
	int 				cageID		= 0;
	int 				dimension	= 0;
	EnumXLSExportType 	exportType 	= null;
	List<Integer > 		data 		= null;
	int					rowbinsize	= 1;
	int[]				valint		= null;
	double [] 			values_out	= null;
	boolean[]			padded_out	= null;
	
	public XLSResults (String name, int nflies, EnumXLSExportType exportType) {
		this.name = name;
		this.nflies = nflies;
		this.exportType = exportType;
	}
	
	public XLSResults(String name, int nflies, EnumXLSExportType exportType, int binsize) {
		this.name = name;
		this.nflies = nflies;
		this.exportType = exportType;
		this.rowbinsize = binsize;
	}
	
	public XLSResults(String name, int nflies, EnumXLSExportType exportType, int nFrames, int binsize) {
		this.name = name;
		this.nflies = nflies;
		this.exportType = exportType;
		this.rowbinsize = binsize;
		initValuesArray(nFrames);
	}
	
	double getAt(int indexData, double scale) {			
		double value = Double.NaN;
		if (indexData < data.size()) {
			value = data.get(indexData) * scale;
		}
		return value;
	}
	
	double getLast(double scale) {			
		double value = Double.NaN;
		if (data.size()>0) {
			value = data.get(data.size()-1) * scale;
		}
		return value;
	}
	
	void initValIntArray(int dimension, int val) {
		this.dimension = dimension; 
		valint = new int [dimension];
		Arrays.fill(valint, 0);
	}
	
	private void initValuesArray(int dimension) {
		this.dimension = dimension; 
		values_out = new double [dimension];
		Arrays.fill(values_out, Double.NaN);
		padded_out = new boolean [dimension];
		Arrays.fill(padded_out, false);
	}
	
	void clearValues (int fromindex) {
		int toindex = values_out.length;
		if (fromindex > 0 && fromindex < toindex) {
			Arrays.fill(values_out, fromindex,  toindex, Double.NaN);
			Arrays.fill(padded_out, fromindex,  toindex, false);
		}
	}
	
	void clearAll() {
		data = null;
		values_out = null;
		nflies = 0;
	}
	
	boolean subtractDeltaT(int arrayStep, int deltaT) {
		if (values_out == null || values_out.length < 2)
			return false;
		for (int index=0; index < values_out.length; index++) {
			int timeIndex = index * arrayStep + deltaT;
			int indexDelta = timeIndex/arrayStep;
			if (indexDelta < values_out.length) 
				values_out[index] = values_out[indexDelta] - values_out[index];
			else
				values_out[index] = Double.NaN;
		}
		return true;
	}
	
	void addDataToValInt(XLSResults result) {
		if (result.data.size() > valint.length) {
			System.out.println("Error: from len="+result.data.size() + " to len="+ valint.length);
			return;
		}
		for (int i=0; i < result.data.size(); i++) {
			valint[i] += result.data.get(i);			
		}
		nflies ++;
	}
	
	void averageEvaporation() {
		if (nflies != 0) {
			for (int i=0; i < valint.length; i++) {
				valint[i] = valint[i] / nflies;			
			}
		}
		nflies = 1;
	}
	
	void subtractEvap(XLSResults evap) {
		if (data == null)
			return;
		
		for (int i = 0; i < data.size(); i++) {
			if (evap.valint.length > i)
				data.set(i, data.get(i) - evap.valint[i]);			
		}
		evap.nflies = 1;
	}
	
	void addValues_out (XLSResults addedData) {
		for (int i = 0; i < values_out.length; i++) {
			if (addedData.values_out.length > i)
				values_out[i] += addedData.values_out[i];			
		}
		nadded += 1;
	}

	void getSumLR(XLSResults rowL, XLSResults rowR) {
		int lenL = rowL.values_out.length;
		int lenR = rowR.values_out.length;
		int len = Math.max(lenL,  lenR);
		for (int index = 0; index < len; index++) {
			double dataL = Double.NaN;
			double dataR = Double.NaN;
			double sum = Double.NaN;
			if (rowL.values_out != null && index < lenL) 
				dataL = rowR.values_out[index];
			if (rowR.values_out != null && index < lenR) 
				dataR = rowR.values_out[index];
			
			sum = Math.abs(dataL)+Math.abs(dataR);
			values_out[index]= sum;
		}
	}
	
	void getRatioLR(XLSResults rowL, XLSResults rowR) {
		int lenL = rowL.values_out.length;
		int lenR = rowR.values_out.length;
		int len = Math.max(lenL,  lenR);
		for (int index = 0; index < len; index++) {
			double dataL = Double.NaN;
			double dataR = Double.NaN;
			if (rowL.values_out != null && index < lenL) 
				dataL = rowR.values_out[index];
			if (rowR.values_out != null && index < lenR) 
				dataR = rowR.values_out[index];
			
			boolean ratioOK = true;
			if (Double.isNaN(dataR)) {
				dataR=0;
				ratioOK = false;
			}
			if (Double.isNaN(dataL)) { 
				dataL=0;
				ratioOK = false;
			}
			
			double ratio = Double.NaN;
			double sum = Math.abs(dataL)+Math.abs(dataR);
			if (ratioOK && sum != 0 && !Double.isNaN(sum))
				ratio = (dataL-dataR)/sum;
			
			values_out[index]= ratio;
		}
	}
	
	void getMixBackwardsLR(XLSResults rowL, XLSResults rowR) {
		int lenL = rowL.values_out.length;
		int lenR = rowR.values_out.length;
		int len = Math.max(lenL,  lenR);
		boolean isL = false;
		boolean isR = false;
		for (int index = len-1; index >= 0; index--) {
			double dataL = Double.NaN;
			double dataR = Double.NaN;
			double dataMix = Double.NaN;
			if (rowL.values_out != null && index < lenL) {
				dataL = rowR.values_out[index];
				if (dataL == 0.)
					isL = true;
			}
			if (rowR.values_out != null && index < lenR) { 
				dataR = rowR.values_out[index];
				if (dataR == 0.)
					isR = true;
			}
			
			if (isL)
				dataMix = dataL;
			if (isR)
				dataMix = dataR;
			
			values_out[index]= dataMix;
		}
	}
}
