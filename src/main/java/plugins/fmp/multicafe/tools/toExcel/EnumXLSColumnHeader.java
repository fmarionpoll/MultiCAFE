package plugins.fmp.multicafe.tools.toExcel;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum EnumXLSColumnHeader {
	EXP_PATH("Path", 0, EnumXLSMeasure.COMMON), EXP_DATE("Date", 1, EnumXLSMeasure.COMMON),
	EXP_BOXID("Box_ID", 2, EnumXLSMeasure.COMMON), EXP_CAM("Cam", 3, EnumXLSMeasure.COMMON),
	EXP_EXPT("Expmt", 4, EnumXLSMeasure.COMMON), EXP_STIM("Stim", 5, EnumXLSMeasure.COMMON),
	EXP_CONC("Conc", 6, EnumXLSMeasure.COMMON), EXP_COND1("Cond1", 9, EnumXLSMeasure.COMMON),
	EXP_COND2("Cond2", 10, EnumXLSMeasure.COMMON), EXP_STRAIN("Strain", 7, EnumXLSMeasure.COMMON),
	EXP_SEX("Sex", 8, EnumXLSMeasure.COMMON),
	//
	CAP("Cap", 11, EnumXLSMeasure.CAP), CAP_VOLUME("Cap_ul", 12, EnumXLSMeasure.CAP),
	CAP_PIXELS("Cap_npixels", 13, EnumXLSMeasure.CAP), CHOICE_NOCHOICE("Choice", 14, EnumXLSMeasure.CAP),
	CAP_STIM("Cap_stimulus", 15, EnumXLSMeasure.CAP), CAP_CONC("Cap_concentration", 16, EnumXLSMeasure.CAP),
	CAP_NFLIES("Nflies", 17, EnumXLSMeasure.CAP), // CAP_CAGEINDEX("Cage", 18, XLSMeasureType.CAP),
	//
	CAGE_INDEX("Cage", 18, EnumXLSMeasure.COMMON), CAGE_ID("Cage_ID", 19, EnumXLSMeasure.COMMON),
	CAGE_STRAIN("Cage_strain", 20, EnumXLSMeasure.COMMON), CAGE_SEX("Cage_sex", 21, EnumXLSMeasure.COMMON),
	CAGE_AGE("Cage_age", 22, EnumXLSMeasure.COMMON), CAGE_COMMENT("Cage_comment", 23, EnumXLSMeasure.COMMON),
	//
	DUM4("Dum4", 24, EnumXLSMeasure.COMMON);

	private final String name;
	private int value;
	private final EnumXLSMeasure type;

	EnumXLSColumnHeader(String label, int value, EnumXLSMeasure type) {
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

	public EnumXLSMeasure toType() {
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
