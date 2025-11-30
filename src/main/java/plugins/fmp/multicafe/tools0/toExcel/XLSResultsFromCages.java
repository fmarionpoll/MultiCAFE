package plugins.fmp.multicafe.tools0.toExcel;

import java.util.ArrayList;

import plugins.fmp.multicafe.tools1.toExcel.XLSResultsArray;

public class XLSResultsFromCages extends XLSResultsArray {
	public XLSResultsFromCages(int size) {
		resultsList = new ArrayList<XLSResults>(size);
	}

}
