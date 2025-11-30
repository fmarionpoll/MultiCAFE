package plugins.fmp.multicafe.tools1.toExcel.enums;

public enum EnumXLSExport {

	TOPRAW("topraw", "volume (ul)", "top liquid level (t-t0)"),
	TOPLEVEL("toplevel", "volume (ul)", "top liquid compensated for evaporation (t-t0)"),
	BOTTOMLEVEL("bottomlevel", "volume (ul)", "bottom liquid level (t-t0)"),
	DERIVEDVALUES("derivative", "volume (ul)", "derived top liquid level (t-t0)"),

	TOPLEVEL_LR("toplevel_L+R", "volume (ul)", "volume consumed in capillaries / cage (t-t0)"),
	TOPLEVELDELTA("topdelta", "volume (ul)", "top liquid consumed (t - t-1)"),
	TOPLEVELDELTA_LR("topdelta_L+R", "volume (ul)", "volume consumed in capillaries /cage (t - t-1)"),

	SUMGULPS("sumGulps", "volume (ul)", "cumulated volume of gulps (t-t0)"),
	SUMGULPS_LR("sumGulps_L+R", "volume (ul)", "cumulated volume of gulps / cage (t-t0)"),
	NBGULPS("nbGulps", "volume (ul)", "number of gulps (at t)"),
	AMPLITUDEGULPS("amplitudeGulps", "volume (ul)", "amplitude of gulps (at t)"),
	TTOGULP("tToGulp", "minutes", "time to previous gulp(at t)"),
	TTOGULP_LR("tToGulp_LR", "minutes", "time to previous gulp of either capillary (at t)"),

	MARKOV_CHAIN("markov_chain", "n observ", "boolean transition (at t)"),
	AUTOCORREL("autocorrel", "n observ", "auto-correlation (at t, over n intervals)"),
	AUTOCORREL_LR("autocorrel_LR", "n observ", "auto-correlation over capillaries/cage (at t, over n intervals)"),
	CROSSCORREL("crosscorrel", "n observ", "cross-correlation (at t, over n intervals)"),
	CROSSCORREL_LR("crosscorrel_LR", "n observ", "cross-correlation over capillaries/cage (at t, over n intervals)"),

	XYIMAGE("xy-image", "mm", "xy image"), //
	XYTOPCAGE("xy-topcage", "mm", "xy top cage"), //
	XYTIPCAPS("xy-tipcaps", "mm", "xy tip capillaries"), //
	ELLIPSEAXES("ellipse-axes", "mm", "Ellipse of axes"), //
	DISTANCE("distance", "mm", "Distance between consecutive points"), //
	ISALIVE("_alive", "yes/no", "Fly alive or not"), //
	SLEEP("sleep", "yes, no", "Fly sleeping"), //

	AREA_SUM("AREA_SUM", "grey value", "Consumption (estimated/threshold)"), //
	AREA_SUMCLEAN("AREA_SUMCLEAN", "grey value - no fly", "Consumption (estimated/threshold)"), //
	AREA_OUT("AREA_OUT", "pixel grey value", "background"), //
	AREA_DIFF("AREA_DIFF", "grey value - background", "diff"), //
	AREA_FLYPRESENT("AREA_FLYPRESENT", "boolean value", "Fly is present or not over the spot");

	private String label;
	private String unit;
	private String title;

	EnumXLSExport(String label, String unit, String title) {
		this.label = label;
		this.unit = unit;
		this.title = title;
	}

	public String toString() {
		return label;
	}

	public String toUnit() {
		return unit;
	}

	public String toTitle() {
		return title;
	}

	public static EnumXLSExport findByText(String abbr) {
		for (EnumXLSExport v : values()) {
			if (v.toString().equals(abbr))
				return v;
		}
		return null;
	}
}
