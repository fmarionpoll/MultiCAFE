# Registration Algorithm Status Update

**Date:** November 24, 2025
**Topic:** Image Registration (Translation, Rotation, Perspective/Trapezoid correction)

## Summary of Achieved Improvements
We successfully upgraded the registration algorithms in MultiCAFE to improve precision and handle complex deformations (tilting plates).

1.  **Sub-Pixel Precision (CPU)**
    *   Implemented parabolic fitting in `GaspardRigidRegistration` to detect shifts with < 1 pixel accuracy.
    *   Updated `ImageRegistrationFeatures` (CPU) to use **Bilinear Interpolation** during image warping, preventing integer-pixel locking and jitter.

2.  **Perspective Transform (Homography)**
    *   Upgraded `ImageRegistrationFeatures` (CPU) to automatically switch to a **Perspective Transform** when exactly **4 landmarks** are used.
    *   This allows the algorithm to correct for **trapezoidal deformations** (e.g., a plate tilting in 3D space), which Affine/Rigid transforms could not handle.
    *   **Status:** Validated by user. "Definite plus" compared to previous version.

## Current Issue: GPU Implementation
We attempted to port these improvements to the GPU (`ImageRegistrationFeaturesGPU` + `CLfunctions.cl`) for speed.

*   **Status:** The GPU version produces **black images** (except for the reference frame).
*   **Likely Cause:**
    *   Coordinate system mismatch between Java `AffineTransform`/Homography and the OpenCL kernel.
    *   The Inverse Matrix passed to the GPU might be transforming coordinates *away* from the source image (out of bounds), resulting in black pixels.
    *   Argument order for Affine matrix coefficients was corrected but might still be mismatched with the kernel's expectation.

## Recommendations for Next Session

1.  **Immediate Use:**
    *   Use **"Feature Tracking (CPU)"** for all analysis. It is fully functional and precise. The performance cost (vs GPU) is acceptable for the gain in accuracy.

2.  **Debugging the GPU Version (Future Task):**
    *   Create a debug kernel that outputs the *transformed X coordinate* as the pixel value (red channel) and *Y coordinate* (green channel) instead of sampling the image.
    *   This will visually show if the coordinate map is reasonable (gradients) or broken (solid values/NaNs).
    *   Verify the `Homography` matrix elements passed to OpenCL. Java `double` to OpenCL `float` conversion is generally safe, but checking for `NaN` or `Infinity` in the Java code before calling the kernel is a good sanity check.

## File Locations
*   **CPU Logic:** `src/main/java/plugins/fmp/multicafe/tools/ImageRegistrationFeatures.java` (Working)
*   **GPU Wrapper:** `src/main/java/plugins/fmp/multicafe/tools/ImageRegistrationFeaturesGPU.java` (Needs Fix)
*   **OpenCL Code:** `src/main/java/plugins/fmp/multicafe/workinprogress_gpu/CLfunctions.cl` (Needs Fix)

