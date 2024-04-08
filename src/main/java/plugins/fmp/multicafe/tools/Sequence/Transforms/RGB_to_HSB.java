package plugins.fmp.multicafe.tools.Sequence.Transforms;

import java.awt.Color;

import icy.image.IcyBufferedImage;
import icy.sequence.Sequence;
import icy.sequence.VolumetricImage;
//import icy.sequence.VolumetricImageCursor;
import icy.type.collection.array.Array1DUtil;
import plugins.fmp.multicafe.tools.Sequence.SequenceTransformFunction;
import plugins.fmp.multicafe.tools.Sequence.SequenceTransformInterface;
import plugins.fmp.multicafe.tools.Sequence.SequenceTransformOptions;

public class RGB_to_HSB extends SequenceTransformFunction implements SequenceTransformInterface
{
	int channelOut = -1;
	int zIn = 0;
	int zOut = 1;
	
	public RGB_to_HSB(int channelOut)
	{
		this.channelOut = channelOut;
	}
	
	@Override
	public void getTransformedSequence(Sequence colorSeq, int t, SequenceTransformOptions options) 
	{ 	        
		VolumetricImage colorVol = colorSeq.getVolumetricImage(t);
		IcyBufferedImage img0 = colorVol.getImage(0);
		IcyBufferedImage img1 = colorVol.getImage(1);
		
		float[][] tabValues = {	Array1DUtil.arrayToFloatArray(img0.getDataXY(0), img0.isSignedDataType()),
								Array1DUtil.arrayToFloatArray(img0.getDataXY(1), img0.isSignedDataType()),
								Array1DUtil.arrayToFloatArray(img0.getDataXY(2), img0.isSignedDataType())};

		float[][] outValues = {	Array1DUtil.arrayToFloatArray(img1.getDataXY(0), img1.isSignedDataType()),
								Array1DUtil.arrayToFloatArray(img1.getDataXY(1), img1.isSignedDataType()),
								Array1DUtil.arrayToFloatArray(img1.getDataXY(2), img1.isSignedDataType())};
		// compute values
		float[] hsbVals = new float[3];
		for (int ky = 0; ky < tabValues[0].length; ky++) 
		{
			Color.RGBtoHSB((int)tabValues[0][ky], (int)tabValues[1][ky], (int)tabValues[2][ky], hsbVals) ;
			outValues[0] [ky] = hsbVals[0] * 100;
			outValues[1] [ky] = hsbVals[1] * 100;
			outValues[2] [ky] = hsbVals[2] * 100;
		}
		setPixelsOut(img1, outValues);
	}
	
	void setPixelsOut(IcyBufferedImage bufferedImage, float[][] outValues) {
		if (channelOut < 0) {
			setOneChannel(bufferedImage, 0, outValues[0]);
			setOneChannel(bufferedImage, 1, outValues[1]);
			setOneChannel(bufferedImage, 2, outValues[2]);
			}
		else {
			setOneChannel(bufferedImage, channelOut, outValues[channelOut]);
			for (int c=0; c < 3; c++)
				if (c != channelOut) 
					bufferedImage.setDataXY(c, bufferedImage.getDataXY(channelOut));
		}
	}
	
	void setOneChannel (IcyBufferedImage bufferedImage, int c, float[] outValues) {
		Array1DUtil.floatArrayToSafeArray(outValues,  bufferedImage.getDataXY(c), false); 
		bufferedImage.setDataXY(c, bufferedImage.getDataXY(c));
	}


}
