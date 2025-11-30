package plugins.fmp.multicafe.tools1.toExcel.enums;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum EnumXLSColumnHeader {
	PATH("Path", 0, EnumColumnType.COMMON), //
	DATE("Date", 1, EnumColumnType.COMMON), //
	EXP_BOXID("Box_ID", 2, EnumColumnType.COMMON), //
	CAM("Cam", 3, EnumColumnType.COMMON), //
	EXP_EXPT("Expmt", 4, EnumColumnType.COMMON), //
	CAGEID("Cage_ID", 5, EnumColumnType.COMMON), //
	EXP_STIM1("Stim1", 6, EnumColumnType.COMMON), //
	EXP_CONC1("Conc1", 7, EnumColumnType.COMMON), //
	EXP_STIM2("Stim2", 8, EnumColumnType.COMMON), //
	EXP_CONC2("Conc2", 9, EnumColumnType.COMMON), //
	EXP_STRAIN("Strain", 10, EnumColumnType.COMMON), //
	EXP_SEX("Sex", 11, EnumColumnType.COMMON), //
	//
	CHOICE_NOCHOICE("Choice", 12, EnumColumnType.COMMON), //
	CAGEPOS("Position", 13, EnumColumnType.COMMON), //
	CAGE_STRAIN("Cage_strain", 14, EnumColumnType.COMMON), //
	CAGE_SEX("Cage_sex", 15, EnumColumnType.COMMON), //
	CAGE_AGE("Cage_age", 16, EnumColumnType.COMMON), //
	CAGE_COMMENT("Cage_comment", 17, EnumColumnType.COMMON), //
	//
	SPOT_CAGEID("Cage", 18, EnumColumnType.COMMON), //
	SPOT_CAGEROW("cageRow", 19, EnumColumnType.COMMON), //
	SPOT_CAGECOL("cageCol", 20, EnumColumnType.COMMON), //
	SPOT_VOLUME("Spot_ul", 21, EnumColumnType.COMMON), //
	SPOT_PIXELS("Spot_npixels", 22, EnumColumnType.COMMON), //
	SPOT_STIM("Spot_stimulus", 23, EnumColumnType.COMMON), //
	SPOT_CONC("Spot_concentration", 24, EnumColumnType.COMMON), //
	SPOT_NFLIES("Nflies", 25, EnumColumnType.COMMON), //
	//
	CAP("Cap", 18, EnumColumnType.COMMON), //
	CAP_INDEX("Cap_ID", 19, EnumColumnType.COMMON), //
	CAP_VOLUME("Cap_ul", 20, EnumColumnType.COMMON), //
	CAP_PIXELS("Cap_npixels", 21, EnumColumnType.COMMON), //
	CAP_STIM("Cap_stimulus", 22, EnumColumnType.COMMON), //
	CAP_CONC("Cap_concentration", 23, EnumColumnType.COMMON), //
	CAP_NFLIES("Nflies", 24, EnumColumnType.COMMON), //
	CAP_COMMENT("Cap_comment", 25, EnumColumnType.COMMON), //
	//
	DUM4("Dum4", 26, EnumColumnType.COMMON);

	private final String name;
	private final int value;
	private final EnumColumnType type;

	EnumXLSColumnHeader(String label, int value, EnumColumnType type) {
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

	public EnumColumnType toType() {
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
