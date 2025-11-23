__kernel void multiply2arrays(__global float* a, __global float* b, __global float* output)
{
	int index = get_global_id(0);
	
	output[index] = a[index] * b[index];
}

__kernel void convolve2D(__global float* input,		// the input image as a 1D linear array
                         int inputWidth,			// the image width
                         int inputHeight,			// the image height
                         __global float* k,			// the kernel as a 1D linear array
                         int kWidth,				// the rounded kernel half-width
                         int kHeight,               // the rounded kernel half-height
                         __global float* output)	// the output image as a 1D linear array
{
	int pixel = get_global_id(0);
	
	int inX, inY, inXY = 0, kXY = 0;
	float iSum = 0.f;
	const int x = pixel % inputWidth;
	const int y = pixel / inputWidth;
	
	for (int kY = -kHeight; kY <= kHeight; kY++) {
		inY = y + kY;
		if (inY < 0 || inY >= inputHeight) continue; // zero boundary condition
		inXY = inY * inputWidth;
		for (int kX = -kWidth; kX <= kWidth; kX++, kXY++) {
			inX = x + kX;
			if (inX < 0 || inX >= inputWidth) continue; // zero boundary condition
			iSum += input[inXY + inX] * k[kXY];
		}
	}
	output[pixel] = iSum;
}

__kernel void convolve2D_mirror(__global float* input, 	// the input image as a 1D linear array
                    	        int inputWidth,			// the image width
                    	        int inputHeight,		// the image height
                   		        __global float* k,		// the kernel as a 1D linear array
                   		        int kWidth,				// the rounded kernel half-width
                   		        int kHeight,            // the rounded kernel half-height
                   		        __global float* output)	// the output image as a 1D linear array
{
	int pixel = get_global_id(0);
	
	int inX, inY, inXY = 0, kXY = 0;
	float iSum = 0.f;
	const int x = pixel % inputWidth;
	const int y = pixel / inputWidth;
	
	for (int kY = -kHeight; kY <= kHeight; kY++) {
		inY = y + kY;
		// mirror boundary condition
		if (inY < 0) {
			inY = -inY + 1;
		} else if (inY >= inputHeight) {
			inY = (inputHeight << 1) - inY - 1;
		}
		inXY = inY * inputWidth;
		// sweep through the kernel along X
		for (int kX = -kWidth; kX <= kWidth; kX++, kXY++) {
			inX = x + kX;
			// mirror boundary condition
			if (inX < 0) {
				inX = -inX + 1;
			} else if (inX >= inputWidth) {
				inX = (inputWidth << 1) - inX - 1;
			}
			iSum += input[inXY + inX] * k[kXY];
		}
	}
	output[pixel] = iSum;
}

__kernel void affineTransform2D(__global float* input,		// the input image as a 1D linear array
                                int inputWidth,				// the image width
                                int inputHeight,			// the image height
                                float m00, float m10,		// affine transform matrix row 0
                                float m01, float m11,		// affine transform matrix row 1
                                float m02, float m12,		// affine transform matrix row 2 (translation)
                                __global float* output)		// the output image as a 1D linear array
{
	int pixel = get_global_id(0);
	const int x = pixel % inputWidth;
	const int y = pixel / inputWidth;
	
	// Apply inverse transform: map output pixel (x,y) to input pixel (srcX, srcY)
	// Affine transform: [x' y' 1] = [x y 1] * M where M = [m00 m01 m02; m10 m11 m12; 0 0 1]
	// Inverse: [x y 1] = [x' y' 1] * M^-1
	// M^-1 = [inv_rot_scale | -inv_rot_scale * translation]
	
	// Compute inverse of 2x2 rotation/scaling matrix [m00 m01; m10 m11]
	float det = m00 * m11 - m01 * m10;
	if (fabs(det) < 1e-6f) {
		output[pixel] = 0.0f;
		return;
	}
	
	float inv00 = m11 / det;
	float inv01 = -m01 / det;
	float inv10 = -m10 / det;
	float inv11 = m00 / det;
	
	// Compute inverse translation: -M^-1_rot * [m02; m12]
	float invTx = -(inv00 * m02 + inv01 * m12);
	float invTy = -(inv10 * m02 + inv11 * m12);
	
	// Apply full inverse transform
	float srcX = x * inv00 + y * inv01 + invTx;
	float srcY = x * inv10 + y * inv11 + invTy;
	
	// Bilinear interpolation
	int x0 = (int)floor(srcX);
	int y0 = (int)floor(srcY);
	int x1 = x0 + 1;
	int y1 = y0 + 1;
	
	float fx = srcX - x0;
	float fy = srcY - y0;
	
	// Boundary check with clamp
	x0 = clamp(x0, 0, inputWidth - 1);
	y0 = clamp(y0, 0, inputHeight - 1);
	x1 = clamp(x1, 0, inputWidth - 1);
	y1 = clamp(y1, 0, inputHeight - 1);
	
	int idx00 = y0 * inputWidth + x0;
	int idx10 = y0 * inputWidth + x1;
	int idx01 = y1 * inputWidth + x0;
	int idx11 = y1 * inputWidth + x1;
	
	float val00 = input[idx00];
	float val10 = input[idx10];
	float val01 = input[idx01];
	float val11 = input[idx11];
	
	// Bilinear interpolation
	float val0 = val00 * (1.0f - fx) + val10 * fx;
	float val1 = val01 * (1.0f - fx) + val11 * fx;
	output[pixel] = val0 * (1.0f - fy) + val1 * fy;
}

