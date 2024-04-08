package plugins.fmp.multicafe.tools.Sequence;

import plugins.fmp.multicafe.tools.Sequence.Transforms.None;
import plugins.fmp.multicafe.tools.Sequence.Transforms.RGB_to_HSB;

public enum SequenceTransformEnums 
{

//	R_RGB			("R(RGB)", 					new LinearCombination(1, 0, 0)),
//  G_RGB			("G(RGB)", 					new LinearCombination(0, 1, 0)),
//  B_RGB			("B(RGB)", 					new LinearCombination(0, 0, 1)),
//  R2MINUS_GB 		("2R-(G+B)", 				new LinearCombination(2, -1, -1)), 
//	G2MINUS_RB		("2G-(R+B)", 				new LinearCombination(-1, 2, -1)), 
//	B2MINUS_RG		("2B-(R+G)", 				new LinearCombination(-1, -1, 2)), 
//	GBMINUS_2R 		("(G+B)-2R", 				new LinearCombination(-2, 1, 1)),  
//	RBMINUS_2G		("(R+B)-2G", 				new LinearCombination(1, -2, 1)),  
//	RGMINUS_2B		("(R+G)-2B", 				new LinearCombination(1, 1, -2)), 
//	RGB_DIFFS		("S(diffRGB)", 				new SumDiff()),
//	RGB 			("(R+G+B)/3", 				new LinearCombination(1/3, 1/3, 1/3)),
	HSB 			("HSB", 					new RGB_to_HSB(-1)), 
	H_HSB 			("H(HSB)", 					new RGB_to_HSB(0)),
	S_HSB 			("S(HSB)", 					new RGB_to_HSB(1)), 
	B_HSB			("B(HSB)", 					new RGB_to_HSB(2)),
//	H_HSV 			("H(HSV)", 					new RGBtoHSV(0)), 
//	S_HSV 			("S(HSV)", 					new RGBtoHSV(1)), 
//	V_HSV			("B(HSV)", 					new RGBtoHSV(2)),
//	XDIFFN			("XDiffn", 					new XDiffn(3)), 
//	YDIFFN			("YDiffn", 					new YDiffn(5)), 
//	YDIFFN2			("YDiffn_1D", 				new YDiffn1D(4)), 
//	XYDIFFN			("XYDiffn", 				new XYDiffn(5)), 
//	SUBTRACT_T0		("t-start",			 		new SubtractReferenceImage()), 
//	SUBTRACT_TM1	("t-(t-1)", 				new SubtractReferenceImage()), 
//	SUBTRACT_REF	("t-ref",		 			new SubtractReferenceImage()),
//	SUBTRACT		("neg(t-ref)", 				new NegativeDifference()),
//	NORM_BRMINUSG	("|aR+bG+cB|",				new LinearCombinationNormed(-1, 2, -1)),
//	RGB_TO_H1H2H3	("H1H2H3", 					new H1H2H3()), 
//	SUBTRACT_1RSTCOL("[t-t0]", 					new SubtractColumn(0)), 
//	L1DIST_TO_1RSTCOL("L1[t-t0]", 				new L1DistanceToColumn(0)),
//	COLORDISTANCE_L1_Y("color dist L1", 		new YDifferenceL(0, 0, 4, 0, false)), 
//	COLORDISTANCE_L2_Y("color dist L2", 		new YDifferenceL(0, 0, 5, 0, true)),
//	DERICHE			("edge detection", 			new Deriche(1., true)), 
//	DERICHE_COLOR	("Deriche's edges", 		new Deriche(1., false)),
//	MINUSHORIZAVG	("remove Hz traces", 		new RemoveHorizontalAverage()),
//	THRESHOLD_SINGLE("threshold 1 value",		new ThresholdSingleValue()),
//	THRESHOLD_COLORS("threshold colors",		new ThresholdColors()),
//	ZIGZAG			("remove spikes",			new None()),
	NONE			("none",					new None());

	private SequenceTransformInterface klass;
    private String label;
	
    SequenceTransformEnums(String label, SequenceTransformInterface klass ) 
	{ 
		this.label = label; 
		this.klass = klass;
	}
    
	public String toString() 
	{ 
		return label; 
	}
	
	public SequenceTransformInterface getFunction() 
	{ 
		return klass; 
	}
	
	public static SequenceTransformEnums findByText(String abbr)
	{
	    for(SequenceTransformEnums v : values())
	    { 
	    	if ( v.toString().equals(abbr)) 
	    		return v;  
	    }
	    return null;
	}
}
