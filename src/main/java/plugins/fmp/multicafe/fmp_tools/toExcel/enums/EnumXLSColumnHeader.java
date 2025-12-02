package plugins.fmp.multicafe.fmp_tools.toExcel.enums;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum EnumXLSColumnHeader {
	PATH("Path", 0, EnumColumnType.COMMON), // 0
	DATE("Date", 1, EnumColumnType.COMMON), // 1
	EXP_BOXID("Box_ID", 2, EnumColumnType.COMMON), // 2
	CAM("Cam", 3, EnumColumnType.COMMON), // 3
	EXP_EXPT("Expmt", 4, EnumColumnType.COMMON), // 4
	EXP_STIM1("Stim1", 5, EnumColumnType.COMMON), // 5
	EXP_CONC1("Conc1", 6, EnumColumnType.COMMON), // 6
	EXP_STIM2("Stim2", 7, EnumColumnType.COMMON), // 7
	EXP_CONC2("Conc2", 8, EnumColumnType.COMMON), // 8
	EXP_STRAIN("Strain", 9, EnumColumnType.COMMON), // 9
	EXP_SEX("Sex", 10, EnumColumnType.COMMON), // 10
	CHOICE_NOCHOICE("Choice", 11, EnumColumnType.COMMON), // 11
	//
	CAGEID("Cage_ID", 12, EnumColumnType.COMMON), // 12
	CAGEPOS("Cage_Position", 13, EnumColumnType.COMMON), // 13
	CAGE_STRAIN("Cage_strain", 14, EnumColumnType.COMMON), // 14
	CAGE_SEX("Cage_sex", 15, EnumColumnType.COMMON), // 15
	CAGE_AGE("Cage_age", 16, EnumColumnType.COMMON), // 16
	CAGE_COMMENT("Cage_comment", 17, EnumColumnType.COMMON), // 17
	//
	SPOT_INDEX("spot_index", 18, EnumColumnType.COMMON), // 18
	SPOT_CAGEROW("spot_cageRow", 19, EnumColumnType.COMMON), // 19
	SPOT_CAGECOL("spot_cageCol", 20, EnumColumnType.COMMON), // 20
	SPOT_VOLUME("Spot_ul", 21, EnumColumnType.COMMON), // 21
	SPOT_PIXELS("Spot_npixels", 22, EnumColumnType.COMMON), // 22
	SPOT_STIM("Spot_stimulus", 23, EnumColumnType.COMMON), // 23
	SPOT_CONC("Spot_concentration", 24, EnumColumnType.COMMON), // 24
	SPOT_NFLIES("Spot_Nflies", 25, EnumColumnType.COMMON), // 25
	//
	CAP("Cap", 26, EnumColumnType.COMMON), //
	CAP_INDEX("Cap_ID", 27, EnumColumnType.COMMON), //
	CAP_VOLUME("Cap_ul", 28, EnumColumnType.COMMON), //
	CAP_PIXELS("Cap_npixels", 29, EnumColumnType.COMMON), //
	CAP_STIM("Cap_stimulus", 30, EnumColumnType.COMMON), //
	CAP_CONC("Cap_concentration", 31, EnumColumnType.COMMON), //
	CAP_NFLIES("Cap_Nflies", 32, EnumColumnType.COMMON), //
	CAP_COMMENT("Cap_comment", 33, EnumColumnType.COMMON), //
	//
	DUM4("Dum4", 34, EnumColumnType.COMMON);

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
