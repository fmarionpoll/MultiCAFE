package plugins.fmp.multicafe.tools.ImageTransform.Transforms;

import icy.image.IcyBufferedImage;
import plugins.fmp.multicafe.tools.ImageTransform.ImageTransformFunctionAbstract;
import plugins.fmp.multicafe.tools.ImageTransform.ImageTransformInterface;
import plugins.fmp.multicafe.tools.ImageTransform.ImageTransformOptions;

public class None extends ImageTransformFunctionAbstract implements ImageTransformInterface
{
	@Override
	public IcyBufferedImage getTransformedImage(IcyBufferedImage sourceImage, ImageTransformOptions options) 
	{
		return sourceImage;
	}


}
