package plugins.fmp.multicafe.tools.toExcel;

public enum EnumXLSMoveParameters {
	T("t", "s"), 
	X("x", "mm"),  
	Y("y", "mm"), 
	W("w", "mm"), 
	H ("h", "mm"), 
	ALIVE("alive", "0/1"),
	SLEEP("sleep", "0/1"),
	DISTANCE("distance", "mm"), 
	CUMDIST("cumdist", "mm");

	private String label;
	private String unit;

	EnumXLSMoveParameters(String label, String unit) {
		this.label = label;
		this.unit = unit;
	}

	public String toString() {
		return label;
	}

	public String toUnit() {
		return unit;
	}

	public static EnumXLSMoveParameters findByText(String abbr) {
		for (EnumXLSMoveParameters v : values()) {
			if (v.toString().equals(abbr))
				return v;
		}
		return null;
	}
}
