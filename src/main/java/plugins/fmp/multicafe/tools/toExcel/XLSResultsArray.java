package plugins.fmp.multicafe.tools.toExcel;

import java.util.ArrayList;
import java.util.Collections;


import plugins.fmp.multicafe.tools.Comparators;

public class XLSResultsArray {
	ArrayList<XLSResults> resultsList = null;

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
		Collections.sort(resultsList, new Comparators.XLSResults_Name_Comparator());
	}
	
	public void subtractDeltaT(int i, int j) {
		for (XLSResults row : resultsList)
			row.subtractDeltaT(1, 1); // options.buildExcelStepMs);
	}

}
