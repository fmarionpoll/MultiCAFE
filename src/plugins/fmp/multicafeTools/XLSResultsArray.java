package plugins.fmp.multicafeTools;

import java.util.ArrayList;
import java.util.List;

import plugins.fmp.multicafeSequence.Capillary;

public class XLSResultsArray {
	List <XLSResults> 	resultsArrayList 	= null;
	XLSResults 			evapL				= null;
	XLSResults 			evapR				= null;
	boolean				sameLR				= true;
	String				stim				= null;
	String				conc				= null;
	
	public XLSResultsArray (int size) {
		resultsArrayList = new ArrayList <XLSResults> (size);
	}
	
	void add(XLSResults results) {
		resultsArrayList.add(results);
	}
	
	void checkIfSameStim(Capillary cap) {
		if (!sameLR)
			return;
		if (stim == null)
			stim = cap.stimulus;
		if (conc == null)
			conc = cap.concentration;
		sameLR &= stim .equals(cap.stimulus);
		sameLR &= conc .equals(cap.concentration);
	}
	
	XLSResults get(int index) {
		if (index >= resultsArrayList.size())
			return null;
		return resultsArrayList.get(index);
	}
	
	void subtractEvaporation() {
		int dimension = 0;
		for (XLSResults result: resultsArrayList) {
			if (result.data.size() > dimension)
				dimension = result.data.size();
		}
		if (dimension== 0)
			return;
		evapL = new XLSResults("L", 0, null);
		evapL.initValIntArray(dimension, 0);
		evapR = new XLSResults("R", 0, null);
		evapR.initValIntArray(dimension, 0);
		computeEvaporationFromResultsWithZeroFlies();
		subtractEvaporationLocal();
	}
	
	private void computeEvaporationFromResultsWithZeroFlies() {
		for (XLSResults result: resultsArrayList) {
			if (result.nflies > 0)
				continue;
			String side = result.name.substring(result.name.length() -1);
			if (sameLR || side.equals("L"))
				evapL.addDataToValInt(result);
			else
				evapR.addDataToValInt(result);
		}
		evapL.averageEvaporation();
		evapR.averageEvaporation();
	}
	
	
	private void subtractEvaporationLocal() {
		for (XLSResults result: resultsArrayList) {
			String side = result.name.substring(result.name.length() -1);
			if (sameLR || side.equals("L"))
				result.subtractEvap(evapL);
			else
				result.subtractEvap(evapR);
		}
	}
	
	
}
