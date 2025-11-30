package plugins.fmp.multicafe.tools0.toExcel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public enum EnumXLSExport {
	TOPRAW("topraw", "volume (ul)", Arrays.asList(EnumMeasure.OTHER), EnumColumnType.CAP),
	TOPLEVEL("toplevel", "volume (ul)", Arrays.asList(EnumMeasure.OTHER), EnumColumnType.CAP),
	BOTTOMLEVEL("bottomlevel", "volume (ul)", Arrays.asList(EnumMeasure.OTHER), EnumColumnType.CAP),
	DERIVEDVALUES("derivative", "volume (ul)", Arrays.asList(EnumMeasure.OTHER), EnumColumnType.CAP),

	TOPLEVEL_LR("toplevel_L+R", "volume (ul)", Arrays.asList(EnumMeasure.OTHER), EnumColumnType.CAP),
	TOPLEVELDELTA("topdelta", "volume (ul)", Arrays.asList(EnumMeasure.OTHER), EnumColumnType.CAP),
	TOPLEVELDELTA_LR("topdelta_L+R", "volume (ul)", Arrays.asList(EnumMeasure.OTHER), EnumColumnType.CAP),

	SUMGULPS("sumGulps", "volume (ul)", Arrays.asList(EnumMeasure.OTHER), EnumColumnType.CAP),
	SUMGULPS_LR("sumGulps_L+R", "volume (ul)", Arrays.asList(EnumMeasure.OTHER), EnumColumnType.CAP),
	NBGULPS("nbGulps", "volume (ul)", Arrays.asList(EnumMeasure.OTHER), EnumColumnType.CAP),
	AMPLITUDEGULPS("amplitudeGulps", "volume (ul)", Arrays.asList(EnumMeasure.OTHER), EnumColumnType.CAP),
	TTOGULP("tToGulp", "minutes", Arrays.asList(EnumMeasure.OTHER), EnumColumnType.CAP),
	TTOGULP_LR("tToGulp_LR", "minutes", Arrays.asList(EnumMeasure.OTHER), EnumColumnType.CAP),

	MARKOV_CHAIN("markov_chain", "n observ", Arrays.asList(EnumMeasure.OTHER), EnumColumnType.CAP),
	AUTOCORREL("autocorrel", "n observ", Arrays.asList(EnumMeasure.OTHER), EnumColumnType.CAP),
	AUTOCORREL_LR("autocorrel_LR", "n observ", Arrays.asList(EnumMeasure.OTHER), EnumColumnType.CAP),
	CROSSCORREL("crosscorrel", "n observ", Arrays.asList(EnumMeasure.OTHER), EnumColumnType.CAP),
	CROSSCORREL_LR("crosscorrel_LR", "n observ", Arrays.asList(EnumMeasure.OTHER), EnumColumnType.CAP),

	XYIMAGE("xy-image", "mm", Arrays.asList(EnumMeasure.TS, EnumMeasure.X, EnumMeasure.Y), EnumColumnType.MOVE),
	XYTOPCELL("xy-topcell", "mm", Arrays.asList(EnumMeasure.TI, EnumMeasure.TS, EnumMeasure.X, EnumMeasure.Y),
			EnumColumnType.MOVE),
	XYTIPCAPS("xy-tipcaps", "mm", Arrays.asList(EnumMeasure.TS, EnumMeasure.X, EnumMeasure.Y), EnumColumnType.MOVE),
	ELLIPSEAXES("ellipse-axes", "mm", Arrays.asList(EnumMeasure.TS, EnumMeasure.W, EnumMeasure.H), EnumColumnType.MOVE),
	DISTANCE("distance", "mm", Arrays.asList(EnumMeasure.TS, EnumMeasure.DISTANCE), EnumColumnType.MOVE),
	ISALIVE("_alive", "yes/no", Arrays.asList(EnumMeasure.TS, EnumMeasure.ALIVE), EnumColumnType.MOVE),
	SLEEP("sleep", "yes, no", Arrays.asList(EnumMeasure.TS, EnumMeasure.SLEEP), EnumColumnType.MOVE);

	private String label;
	private String unit;
	private ArrayList<EnumMeasure> measures = new ArrayList<EnumMeasure>();
	private EnumColumnType type;

	EnumXLSExport(String label, String unit, List<EnumMeasure> list, EnumColumnType type) {
		this.label = label;
		this.unit = unit;
		if (list != null)
			this.measures.addAll(list);
		this.type = type;
	}

	public String toString() {
		return label;
	}

	public String toUnit() {
		return unit;
	}

	public ArrayList<EnumMeasure> toMeasures() {
		return measures;
	}

	public EnumColumnType toType() {
		return type;
	}

	public static EnumXLSExport findByText(String abbr) {
		for (EnumXLSExport v : values()) {
			if (v.toString().equals(abbr))
				return v;
		}
		return null;
	}
}
