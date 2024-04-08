package plugins.fmp.multicafe.tools.Image.Transforms;

import icy.image.IcyBufferedImage;
import icy.type.DataType;
import icy.type.collection.array.Array1DUtil;
import plugins.fmp.multicafe.tools.Image.ImageTransformFunctionAbstract;
import plugins.fmp.multicafe.tools.Image.ImageTransformInterface;
import plugins.fmp.multicafe.tools.Image.ImageTransformOptions;

public class LinearCombination extends ImageTransformFunctionAbstract implements ImageTransformInterface
{
	double w0 = 1;
	double w1 = 1;
	double w2 = 1;
	public LinearCombination (double w0, double w1, double w2)
	{
		this.w0 = w0;
		this.w1 = w1;
		this.w2 = w2;
	}
	
	@Override
	public IcyBufferedImage getTransformedImage(IcyBufferedImage sourceImage, ImageTransformOptions options) 
	{
		return functionRGBtoLinearCombination(sourceImage);
	}

	protected IcyBufferedImage functionRGBtoLinearCombination(IcyBufferedImage sourceImage) 
	{
		IcyBufferedImage img2 = new IcyBufferedImage(sourceImage.getWidth(), sourceImage.getHeight(), 3, sourceImage.getDataType_());
		double[] tabAdd0 = Array1DUtil.arrayToDoubleArray(sourceImage.getDataXY(0), sourceImage.isSignedDataType());
		double[] tabAdd1 = Array1DUtil.arrayToDoubleArray(sourceImage.getDataXY(1), sourceImage.isSignedDataType());
		double[] tabAdd2 = Array1DUtil.arrayToDoubleArray(sourceImage.getDataXY(2), sourceImage.isSignedDataType());
		double[] tabResult =  (double[]) Array1DUtil.createArray(DataType.DOUBLE, tabAdd0.length);
		for (int i = 0; i < tabResult.length; i++) 
		{	
			double val = tabAdd0[i]* w0 + tabAdd1[i] * w1 + tabAdd2[i] * w2 ;
			tabResult [i] = val;
		}
		copyExGDoubleToIcyBufferedImage(tabResult, img2);
		return img2; 
	}
}


