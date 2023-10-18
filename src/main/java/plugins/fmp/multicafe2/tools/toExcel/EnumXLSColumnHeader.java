package plugins.fmp.multicafe2.tools.toExcel;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;


public enum EnumXLSColumnHeader 
{
	PATH			("Path", 			0),
	DATE			("Date", 			1), 
	EXP_BOXID		("Box_ID", 			2), 
	CAM 			("Cam", 			3), 
	EXP_EXPT		("Expmt", 			4), 
	CAGEID 			("Cage_ID", 		5),
	EXP_STIM		("Stim", 			6),
	EXP_CONC		("Conc", 			7),
	EXP_STRAIN		("Strain", 			8),
	EXP_SEX			("Sex", 			9), 
	CAP 			("Cap", 			10),
	CAP_VOLUME		("Cap_ul", 			11), 
	CAP_PIXELS 		("Cap_npixels", 	12), 
	CHOICE_NOCHOICE	("Choice",			13),  
	CAP_STIM		("Cap_stimulus",	14), 
	CAP_CONC		("Cap_concentration",15),
	CAP_NFLIES		("Nflies", 			16), 
	CAP_CAGEINDEX 	("Cage", 			17), 
	DUM4			("Dum4", 			18),
	CAGE_STRAIN 	("Cage_strain", 	19),
	CAGE_SEX 		("Cage_sex", 		20),
	CAGE_AGE		("Cage_age", 		21),
	CAGE_COMMENT	("Cage_comment",	22);
	
	
	private final String 	name;
	private final int 		value;
	
	
	EnumXLSColumnHeader (String label, int value) 
	{ 
		this.name = label;
		this.value = value;
	}
	
	public String getName() 
	{
		return name;
	}

	public int getValue() 
	{
		return value;
	}
	
	static final Map<String, EnumXLSColumnHeader> names = Arrays.stream(EnumXLSColumnHeader.values())
		      .collect(Collectors.toMap(EnumXLSColumnHeader::getName, Function.identity()));
	
	static final Map<Integer, EnumXLSColumnHeader> values = Arrays.stream(EnumXLSColumnHeader.values())
		      .collect(Collectors.toMap(EnumXLSColumnHeader::getValue, Function.identity()));
	
	public static EnumXLSColumnHeader fromName(final String name) 
	{
	    return names.get(name);
	}

	public static EnumXLSColumnHeader fromValue(final int value) 
	{
	    return values.get(value);
	}
	
	public String toString() 
	{ 
		return name; 
	}
	
	public static EnumXLSColumnHeader findByText(String abbr)
	{
	    for(EnumXLSColumnHeader v : values())
	    { 
	    	if ( v.toString().equals(abbr)) 
	    		return v;  
	    }
	    return null;
	}
}


