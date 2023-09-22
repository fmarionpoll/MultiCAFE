package plugins.fmp.multicafe2.tools.Sequence;

import java.awt.Color;
import java.util.ArrayList;

import icy.image.IcyBufferedImage;
import icy.sequence.Sequence;

public class SequenceTransformOptions 
{
	public SequenceTransformEnums transform01; 
	
	public IcyBufferedImage backgroundImage = null;
	public IcyBufferedImage secondImage = null;
	public Sequence seq = null;
	public int npixels_changed = 0;
	
	public int xfirst;
	public int xlast;
	public int yfirst;
	public int ylast;
	public int channel0;
	public int channel1;
	public int channel2;
	public int w0 = 1;
	public int w1 = 1;
	public int w2 = 1;
	public int spanDiff = 3;
	public int simplethreshold = 255;
	public int background_delta = 50;
	public int background_jitter = 1;
		
	protected int colorthreshold = 0;
	protected int colordistanceType = 0;
	protected boolean ifGreater = true;
	
	protected final byte byteFALSE = 0;
	protected final byte byteTRUE = (byte) 0xFF;
	protected ArrayList<Color> colorarray = null;
	
}
