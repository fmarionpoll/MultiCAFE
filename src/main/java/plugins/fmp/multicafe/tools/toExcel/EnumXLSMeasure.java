package plugins.fmp.multicafe.tools.toExcel;

public enum EnumXLSMeasure {
	CAP("capillary"), MOVE("move"), COMMON("common");

	private String label;

	EnumXLSMeasure(String label) {
		this.label = label;
	}

	public String toString() {
		return label;
	}

}
