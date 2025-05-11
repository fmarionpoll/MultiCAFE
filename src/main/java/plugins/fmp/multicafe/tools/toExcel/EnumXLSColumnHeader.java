package plugins.fmp.multicafe.tools.toExcel;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum EnumXLSColumnHeader {
	PATH("Path", 0), 
	DATE("Date", 1), 
	EXP_BOXID("Box_ID", 2), 
	CAM("Cam", 3), 
	EXP_EXPT("Expmt", 4),
	EXP_STIM("Stim", 5), 
	EXP_CONC("Conc", 6), 
	EXP_STRAIN("Strain", 7), 
	EXP_SEX("Sex", 8), 
	EXP_COND1("Cond1", 9),
	EXP_COND2("Cond2", 10), 
	CAP("Cap", 11), 
	CAP_VOLUME("Cap_ul", 12), 
	CAP_PIXELS("Cap_npixels", 13),
	CHOICE_NOCHOICE("Choice", 14), 
	CAP_STIM("Cap_stimulus", 15), 
	CAP_CONC("Cap_concentration", 16),
	CAP_NFLIES("Nflies", 17), 
	CAP_CAGEINDEX("Cell", 18), 
	CAGEID("Cell_ID", 19), 
	CAGE_STRAIN("Cell_strain", 20),
	CAGE_SEX("Cell_sex", 21), 
	CAGE_AGE("Cell_age", 22), 
	CAGE_COMMENT("Cell_comment", 23), 
	DUM4("Dum4", 24);

	private final String name;
	private final int value;

	EnumXLSColumnHeader(String label, int value) {
		this.name = label;
		this.value = value;
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

	public static EnumXLSColumnHeader findByText(String abbr) {
		for (EnumXLSColumnHeader v : values()) {
			if (v.toString().equals(abbr))
				return v;
		}
		return null;
	}
}
