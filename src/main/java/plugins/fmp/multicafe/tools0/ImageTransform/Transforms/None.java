package plugins.fmp.multicafe.tools0.ImageTransform.Transforms;

import icy.image.IcyBufferedImage;
import plugins.fmp.multicafe.tools0.ImageTransform.ImageTransformFunctionAbstract;
import plugins.fmp.multicafe.tools0.ImageTransform.ImageTransformInterface;
import plugins.fmp.multicafe.tools0.ImageTransform.ImageTransformOptions;

public class None extends ImageTransformFunctionAbstract implements ImageTransformInterface {
	@Override
	public IcyBufferedImage getTransformedImage(IcyBufferedImage sourceImage, ImageTransformOptions options) {
		return sourceImage;
	}

}
