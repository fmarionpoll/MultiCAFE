package plugins.fmp.multicafe.tools;

import java.awt.Color;

/**
 * NHClass L2ColorDistance.
 */
public class NHL2ColorDistance extends NHColorDistance 
{
	/* (non-Javadoc)
	 * @see plugins.nherve.toolbox.image.feature.ColorDistance#computeDistance(double[], double[])
	 */
	@Override
	public double computeDistance(Color c1, Color c2) 
	{
		double dr = c1.getRed() 	- c2.getRed();
		double dg = c1.getGreen() 	- c2.getGreen();
		double db = c1.getBlue() 	- c2.getBlue();
		return Math.sqrt(dr * dr + dg * dg + db * db);
	}

}