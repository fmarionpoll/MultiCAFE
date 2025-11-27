package plugins.fmp.multicafe.tools1.imageTransform;

import icy.image.IcyBufferedImage;

public interface ImageTransformInterface {
	public IcyBufferedImage getTransformedImage(IcyBufferedImage sourceImage, ImageTransformOptions options);
}
