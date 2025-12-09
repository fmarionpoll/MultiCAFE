package plugins.fmp.multicafe.fmp_tools.toExcel.data;

import java.util.ArrayList;
import java.util.Collections;

import plugins.fmp.multicafe.fmp_tools.Comparators;


public class XLSResultsArray {
	protected ArrayList<XLSResults> resultsList = null;
	String stim = null;
	String conc = null;
	double lowestPiAllowed = -1.2;
	double highestPiAllowed = 1.2;

	public XLSResultsArray(int size) {
		resultsList = new ArrayList<XLSResults>(size);
	}

	public XLSResultsArray() {
		resultsList = new ArrayList<XLSResults>();
	}

	public int size() {
		return resultsList.size();
	}

	public XLSResults getRow(int index) {
		if (index >= resultsList.size())
			return null;
		return resultsList.get(index);
	}

	public void addRow(XLSResults results) {
		resultsList.add(results);
	}

	public void sortRowsByName() {
		Collections.sort(resultsList, new Comparators.XLSResults_Name());
	}

	public void subtractDeltaT(int i, int j) {
		for (XLSResults row : resultsList)
			row.subtractDeltaT(1, 1); // options.buildExcelStepMs);
	}



}
