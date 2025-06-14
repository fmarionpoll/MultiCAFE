package plugins.fmp.multicafe.tools.toExcel;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum EnumXLSColumnHeader {
	EXP_PATH("Path", 0, XLSMeasureType.COMMON), EXP_DATE("Date", 1, XLSMeasureType.COMMON),
	EXP_BOXID("Box_ID", 2, XLSMeasureType.COMMON), EXP_CAM("Cam", 3, XLSMeasureType.COMMON),
	EXP_EXPT("Expmt", 4, XLSMeasureType.COMMON), EXP_STIM("Stim", 5, XLSMeasureType.COMMON),
	EXP_CONC("Conc", 6, XLSMeasureType.COMMON), EXP_COND1("Cond1", 9, XLSMeasureType.COMMON),
	EXP_COND2("Cond2", 10, XLSMeasureType.COMMON), EXP_STRAIN("Strain", 7, XLSMeasureType.COMMON),
	EXP_SEX("Sex", 8, XLSMeasureType.COMMON),
	//
	CAP("Cap", 11, XLSMeasureType.CAP), CAP_VOLUME("Cap_ul", 12, XLSMeasureType.CAP),
	CAP_PIXELS("Cap_npixels", 13, XLSMeasureType.CAP), CHOICE_NOCHOICE("Choice", 14, XLSMeasureType.CAP),
	CAP_STIM("Cap_stimulus", 15, XLSMeasureType.CAP), CAP_CONC("Cap_concentration", 16, XLSMeasureType.CAP),
	CAP_NFLIES("Nflies", 17, XLSMeasureType.CAP), // CAP_CAGEINDEX("Cell", 18, XLSMeasureType.CAP),
	//
	CELL_INDEX("Cell", 18, XLSMeasureType.COMMON), CELL_ID("Cell_ID", 19, XLSMeasureType.COMMON),
	CELL_STRAIN("Cell_strain", 20, XLSMeasureType.COMMON), CELL_SEX("Cell_sex", 21, XLSMeasureType.COMMON),
	CELL_AGE("Cell_age", 22, XLSMeasureType.COMMON), CELL_COMMENT("Cell_comment", 23, XLSMeasureType.COMMON),
	//
	DUM4("Dum4", 24, XLSMeasureType.COMMON);

	private final String name;
	private int value;
	private final XLSMeasureType type;

	EnumXLSColumnHeader(String label, int value, XLSMeasureType type) {
		this.name = label;
		this.value = value;
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public int getValue() {
		return value;
	}

	public void setValue(int value) {
		this.value = value;
	}

	static final Map<String, EnumXLSColumnHeader> names = Arrays.stream(EnumXLSColumnHeader.values())
			.collect(Collectors.toMap(EnumXLSColumnHeader::getName, Function.identity()));

	static final Map<Integer, EnumXLSColumnHeader> values = Arrays.stream(EnumXLSColumnHeader.values())
			.collect(Collectors.toMap(EnumXLSColumnHeader::getValue, Function.identity()));

	public static EnumXLSColumnHeader fromName(final String name) {
		return names.get(name);
	}

	public static EnumXLSColumnHeader fromValue(final int value) {
		return values.get(value);
	}

	public String toString() {
		return name;
	}

	public XLSMeasureType toType() {
		return type;
	}

	public static EnumXLSColumnHeader findByText(String abbr) {
		for (EnumXLSColumnHeader v : values()) {
			if (v.toString().equals(abbr))
				return v;
		}
		return null;
	}
}
