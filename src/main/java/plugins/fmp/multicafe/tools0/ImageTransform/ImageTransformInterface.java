package plugins.fmp.multicafe.tools0.ImageTransform;

import icy.image.IcyBufferedImage;

public interface ImageTransformInterface {
	public IcyBufferedImage getTransformedImage(IcyBufferedImage sourceImage, ImageTransformOptions options);
}
