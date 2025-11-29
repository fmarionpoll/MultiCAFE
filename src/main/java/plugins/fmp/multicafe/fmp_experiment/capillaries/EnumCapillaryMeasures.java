package plugins.fmp.multicafe.fmp_experiment.capillaries;

public enum EnumCapillaryMeasures {
	TOPLEVEL("TOPLEVEL", "top capillary limit"), BOTTOMLEVEL("BOTTOMLEVEL", "bottom capillary limit"),
	TOPDERIVATIVE("TOPDERIVATIVE", "derivative of top capillary limit"),
	GULPS("GULPS", "gulps detected from derivative"), ALL("ALL", "all options");

	private String label;
	private String unit;

	EnumCapillaryMeasures(String label, String unit) {
		this.label = label;
		this.unit = unit;
	}

	public String toString() {
		return label;
	}

	public String toUnit() {
		return unit;
	}

	public static EnumCapillaryMeasures findByText(String abbr) {
		for (EnumCapillaryMeasures v : values()) {
			if (v.toString().equals(abbr))
				return v;
		}
		return null;
	}
}
