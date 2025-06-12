package plugins.fmp.multicafe.tools.toExcel;

public enum XLSMeasureType {
	CAP("capillary"), MOVE("move"), COMMON("common");

	private String label;

	XLSMeasureType(String label) {
		this.label = label;
	}

	public String toString() {
		return label;
	}

}
