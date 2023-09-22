package plugins.fmp.multicafe2.tools.Image.Transforms;

import icy.image.IcyBufferedImage;
import plugins.fmp.multicafe2.tools.Image.ImageTransformFunctionAbstract;
import plugins.fmp.multicafe2.tools.Image.ImageTransformInterface;
import plugins.fmp.multicafe2.tools.Image.ImageTransformOptions;

public class None extends ImageTransformFunctionAbstract implements ImageTransformInterface
{
	@Override
	public IcyBufferedImage getTransformedImage(IcyBufferedImage sourceImage, ImageTransformOptions options) 
	{
		return sourceImage;
	}


}
