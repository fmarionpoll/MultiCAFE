# MultiCAFE Project Analysis

**Date:** November 24, 2025
**Version:** 1.0 (Post-Image Registration Implementation)

## 1. Project Overview
MultiCAFE is an Icy plugin designed for the analysis of capillary-based experiments (e.g., CAFE assays). It handles image sequences, detects capillaries and cages, generates kymographs, and analyzes fly behavior (levels, gulps).

## 2. Recent Major Changes: Image Registration
A new module for Image Registration has been implemented to correct for camera movements, zoom changes, and cage shifts.

### 2.1. Architecture
-   **Strategy Pattern**: Implemented `ImageRegistration` abstract base class with three concrete strategies:
    1.  `ImageRegistrationGaspard`: Wraps the existing `GaspardRigidRegistration` (Translation only, FFT-based).
    2.  `ImageRegistrationFeatures`: CPU-based affine registration (Translation, Rotation, Scaling) using feature tracking (capillary endpoints, cage corners).
    3.  `ImageRegistrationFeaturesGPU`: GPU-accelerated version of the features algorithm using JavaCL/OpenCL.
-   **GPU Integration**: 
    -   Added `affineTransform2D` kernel to `CLfunctions.cl`.
    -   `ImageRegistrationFeaturesGPU` manages OpenCL context, memory buffers, and kernel execution.
    -   Fallback mechanism (conceptual/manual selection) provided via UI.

### 2.2. User Interface
-   **New Tab**: `Register` tab added to `MCKymos_` (Kymographs section).
-   **Controls**: Users can select algorithm, reference frame (Start/End), and direction (Forward/Backward).
-   **Safety**: Implemented a backup system that moves original images to an `original/` subdirectory before overwriting.

### 2.3. Data Access & Encapsulation
-   Recent changes enforced the use of getters/setters for core data classes (`Experiment`, `SequenceCamData`, `Cages`, `Capillaries`).
-   Direct field access is being deprecated/removed.

## 3. Codebase Status

### 3.1. Key Classes
-   **`plugins.fmp.multicafe.tools.ImageRegistration`**: Abstract base. Handles file backup logic.
-   **`plugins.fmp.multicafe.tools.ImageRegistrationFeaturesGPU`**: Handles JavaCL interactions. Key complexity involves buffer management and kernel arguments.
-   **`plugins.fmp.multicafe.workinprogress_gpu.CLfunctions.cl`**: OpenCL C code for image processing kernels.

### 3.2. Current Health
-   **Compilation**: All new files compile successfully. Linter checks pass.
-   **Encapsulation**: Transition to private fields is ongoing. New code strictly uses accessors.
-   **GPU Dependency**: Requires JavaCL libraries and OpenCL-capable hardware/drivers.

## 4. Outstanding Issues & Risks
1.  **GPU Compatibility**: JavaCL can be sensitive to driver versions and hardware. The fallback or error reporting needs robust testing.
2.  **Performance Parity**: GPU implementation needs to be benchmarked against CPU to justify the complexity.
3.  **Verification**: "End to Start" registration stability and the visual quality of registered stacks need empirical verification by the user.
4.  **Legacy Code**: Large portions of the codebase (e.g., `SequenceKymosUtils`, legacy detection algorithms) remain untouched and may rely on deprecated access patterns or public fields.

## 5. Recommendations
1.  **Testing**: Prioritize side-by-side comparison of CPU vs GPU registration results.
2.  **Error Handling**: Improve error reporting in `ImageRegistration` (currently prints stack traces). Integrate with the project's `Logger`.
3.  **Refactoring**: Continue the encapsulation drive. Ensure `Experiment` and `SequenceCamData` are fully protected.
4.  **UI Feedback**: Add a progress bar or specific status indicators for the registration process, as it can be time-consuming.

