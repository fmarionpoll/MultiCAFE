package plugins.fmp.multicafe.tools.toExcel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public enum EnumXLSExportType {
	TOPRAW("topraw", "volume (ul)", Arrays.asList(EnumMeasure.OTHER), XLSMeasureType.CAP),
	TOPLEVEL("toplevel", "volume (ul)", Arrays.asList(EnumMeasure.OTHER), XLSMeasureType.CAP),
	BOTTOMLEVEL("bottomlevel", "volume (ul)", Arrays.asList(EnumMeasure.OTHER), XLSMeasureType.CAP),
	DERIVEDVALUES("derivative", "volume (ul)", Arrays.asList(EnumMeasure.OTHER), XLSMeasureType.CAP),

	TOPLEVEL_LR("toplevel_L+R", "volume (ul)", Arrays.asList(EnumMeasure.OTHER), XLSMeasureType.CAP),
	TOPLEVELDELTA("topdelta", "volume (ul)", Arrays.asList(EnumMeasure.OTHER), XLSMeasureType.CAP),
	TOPLEVELDELTA_LR("topdelta_L+R", "volume (ul)", Arrays.asList(EnumMeasure.OTHER), XLSMeasureType.CAP),

	SUMGULPS("sumGulps", "volume (ul)", Arrays.asList(EnumMeasure.OTHER), XLSMeasureType.CAP),
	SUMGULPS_LR("sumGulps_L+R", "volume (ul)", Arrays.asList(EnumMeasure.OTHER), XLSMeasureType.CAP),
	NBGULPS("nbGulps", "volume (ul)", Arrays.asList(EnumMeasure.OTHER), XLSMeasureType.CAP),
	AMPLITUDEGULPS("amplitudeGulps", "volume (ul)", Arrays.asList(EnumMeasure.OTHER), XLSMeasureType.CAP),
	TTOGULP("tToGulp", "minutes", Arrays.asList(EnumMeasure.OTHER), XLSMeasureType.CAP),
	TTOGULP_LR("tToGulp_LR", "minutes", Arrays.asList(EnumMeasure.OTHER), XLSMeasureType.CAP),

	AUTOCORREL("autocorrel", "n observ", Arrays.asList(EnumMeasure.OTHER), XLSMeasureType.CAP),
	AUTOCORREL_LR("autocorrel_LR", "n observ", Arrays.asList(EnumMeasure.OTHER), XLSMeasureType.CAP),
	CROSSCORREL("crosscorrel", "n observ", Arrays.asList(EnumMeasure.OTHER), XLSMeasureType.CAP),
	CROSSCORREL_LR("crosscorrel_LR", "n observ", Arrays.asList(EnumMeasure.OTHER), XLSMeasureType.CAP),

	XYIMAGEC("xy-image", "mm", Arrays.asList(EnumMeasure.T, EnumMeasure.X, EnumMeasure.Y), XLSMeasureType.MOVE),
	XYTOPCAGEC("xy-topcell", "mm", Arrays.asList(EnumMeasure.T, EnumMeasure.X, EnumMeasure.Y), XLSMeasureType.MOVE),
	XYTIPCAPSC("xy-tipcaps", "mm", Arrays.asList(EnumMeasure.T, EnumMeasure.X, EnumMeasure.Y), XLSMeasureType.MOVE),
	ELLIPSEAXES("ellipse-axes", "mm", Arrays.asList(EnumMeasure.T, EnumMeasure.W, EnumMeasure.H), XLSMeasureType.MOVE),
	DISTANCE("distance", "mm", Arrays.asList(EnumMeasure.T, EnumMeasure.DISTANCE), XLSMeasureType.MOVE),
	ISALIVE("_alive", "yes/no", Arrays.asList(EnumMeasure.T, EnumMeasure.ALIVE), XLSMeasureType.MOVE),
	SLEEP("sleep", "yes, no", Arrays.asList(EnumMeasure.T, EnumMeasure.SLEEP), XLSMeasureType.MOVE);

	private String label;
	private String unit;
	private ArrayList<EnumMeasure> measures = new ArrayList<EnumMeasure>();
	private XLSMeasureType type;

	EnumXLSExportType(String label, String unit, List<EnumMeasure> list, XLSMeasureType type) {
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

	public XLSMeasureType toType() {
		return type;
	}

	public static EnumXLSExportType findByText(String abbr) {
		for (EnumXLSExportType v : values()) {
			if (v.toString().equals(abbr))
				return v;
		}
		return null;
	}
}
