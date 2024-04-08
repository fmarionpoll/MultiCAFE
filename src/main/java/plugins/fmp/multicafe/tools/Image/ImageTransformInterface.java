package plugins.fmp.multicafe.tools.Image;

import icy.image.IcyBufferedImage;

public interface ImageTransformInterface 
{
	public IcyBufferedImage getTransformedImage (IcyBufferedImage sourceImage, ImageTransformOptions options);
}
