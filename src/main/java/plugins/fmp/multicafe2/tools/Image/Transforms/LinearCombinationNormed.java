package plugins.fmp.multicafe2.tools.Image.Transforms;

import icy.image.IcyBufferedImage;
import icy.type.DataType;
import icy.type.collection.array.Array1DUtil;
import plugins.fmp.multicafe2.tools.Image.ImageTransformFunctionAbstract;
import plugins.fmp.multicafe2.tools.Image.ImageTransformInterface;
import plugins.fmp.multicafe2.tools.Image.ImageTransformOptions;

public class LinearCombinationNormed extends ImageTransformFunctionAbstract implements ImageTransformInterface
{
	double w0 = 1;
	double w1 = 1;
	double w2 = 1;
	
	public LinearCombinationNormed (double w0, double w1, double w2)
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
		IcyBufferedImage img2 = new IcyBufferedImage (sourceImage.getWidth(), sourceImage.getHeight(), 3, sourceImage.getDataType_());
		double[] Rn = Array1DUtil.arrayToDoubleArray(sourceImage.getDataXY(0), sourceImage.isSignedDataType());
		double[] Gn = Array1DUtil.arrayToDoubleArray(sourceImage.getDataXY(1), sourceImage.isSignedDataType());
		double[] Bn = Array1DUtil.arrayToDoubleArray(sourceImage.getDataXY(2), sourceImage.isSignedDataType());
		double[] ExG = (double[]) Array1DUtil.createArray(DataType.DOUBLE, Rn.length);
		for (int i = 0; i < Rn.length; i++) 
		{
			double sum = (Rn[i] / 255) + (Gn[i] / 255) + (Bn [i] / 255);
			ExG[i] = ((Rn[i]*w0 / 255/ sum) + (Gn[i] * w1 / 255 / sum) + (Bn [i] * w2 / 255/sum)) * 255;
		}
		
		copyExGDoubleToIcyBufferedImage(ExG, img2);
		return img2;
	}
}
