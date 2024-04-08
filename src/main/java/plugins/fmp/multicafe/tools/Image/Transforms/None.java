package plugins.fmp.multicafe.tools.Image.Transforms;

import icy.image.IcyBufferedImage;
import plugins.fmp.multicafe.tools.Image.ImageTransformFunctionAbstract;
import plugins.fmp.multicafe.tools.Image.ImageTransformInterface;
import plugins.fmp.multicafe.tools.Image.ImageTransformOptions;

public class None extends ImageTransformFunctionAbstract implements ImageTransformInterface
{
	@Override
	public IcyBufferedImage getTransformedImage(IcyBufferedImage sourceImage, ImageTransformOptions options) 
	{
		return sourceImage;
	}


}
