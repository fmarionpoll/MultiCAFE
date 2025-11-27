package plugins.fmp.multicafe.tools1.imageTransform.transforms;

import icy.image.IcyBufferedImage;
import plugins.fmp.multicafe.tools1.imageTransform.ImageTransformFunctionAbstract;
import plugins.fmp.multicafe.tools1.imageTransform.ImageTransformInterface;
import plugins.fmp.multicafe.tools1.imageTransform.ImageTransformOptions;

public class None extends ImageTransformFunctionAbstract implements ImageTransformInterface {
	@Override
	public IcyBufferedImage getTransformedImage(IcyBufferedImage sourceImage, ImageTransformOptions options) {
		return sourceImage;
	}

}
