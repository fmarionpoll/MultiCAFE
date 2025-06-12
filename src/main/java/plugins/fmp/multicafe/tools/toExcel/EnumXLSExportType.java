package plugins.fmp.multicafe.tools.toExcel;

public enum EnumXLSExportType {
	TOPRAW("topraw", "volume (ul)"), TOPLEVEL("toplevel", "volume (ul)"), BOTTOMLEVEL("bottomlevel", "volume (ul)"),
	DERIVEDVALUES("derivative", "volume (ul)"),

	TOPLEVEL_LR("toplevel_L+R", "volume (ul)"), TOPLEVELDELTA("topdelta", "volume (ul)"),
	TOPLEVELDELTA_LR("topdelta_L+R", "volume (ul)"),

	SUMGULPS("sumGulps", "volume (ul)"), SUMGULPS_LR("sumGulps_L+R", "volume (ul)"), NBGULPS("nbGulps", "volume (ul)"),
	AMPLITUDEGULPS("amplitudeGulps", "volume (ul)"), TTOGULP("tToGulp", "minutes"), TTOGULP_LR("tToGulp_LR", "minutes"),

	AUTOCORREL("autocorrel", "n observ"), AUTOCORREL_LR("autocorrel_LR", "n observ"),
	CROSSCORREL("crosscorrel", "n observ"), CROSSCORREL_LR("crosscorrel_LR", "n observ"),

	XYIMAGEC("xy-image", "mm"), XYTOPCAGEC("xy-topcell", "mm"), XYTIPCAPSC("xy-tipcaps", "mm"),
	ELLIPSEAXES("ellipse-axes", "mm"), DISTANCE("distance", "mm"), ISALIVE("_alive", "yes/no"),
	SLEEP("sleep", "yes, no");

	private String label;
	private String unit;

	EnumXLSExportType(String label, String unit) {
		this.label = label;
		this.unit = unit;
	}

	public String toString() {
		return label;
	}

	public String toUnit() {
		return unit;
	}

	public static EnumXLSExportType findByText(String abbr) {
		for (EnumXLSExportType v : values()) {
			if (v.toString().equals(abbr))
				return v;
		}
		return null;
	}
}
