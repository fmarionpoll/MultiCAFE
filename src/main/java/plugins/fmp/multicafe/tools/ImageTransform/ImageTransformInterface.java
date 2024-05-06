package plugins.fmp.multicafe.tools.ImageTransform;

import icy.image.IcyBufferedImage;

public interface ImageTransformInterface 
{
	public IcyBufferedImage getTransformedImage (IcyBufferedImage sourceImage, ImageTransformOptions options);
}
