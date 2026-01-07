# Performance Optimization and Fixes for Experiment Loading

## Issue Analysis
The user reported extremely slow loading times when opening experiments, particularly during the "transfer measures to kymos" step (Step 5). Profiling confirmed this step took ~4000ms, while file reading (Step 4) took only ~14ms.

The root cause was the creation of thousands of individual `ROI2DPolyLine` objects to represent "gulps" (events detected on the kymograph). Each gulp was being added as a separate ROI to the sequence, overwhelming the UI system.

## Changes Implemented

### 1. Visualization Optimization (Dots Display)
*   **File:** `src/main/java/plugins/fmp/multicafe/fmp_experiment/capillaries/Capillary.java`
*   **Change:** Refactored `getROIsFromCapillaryGulps` to aggregate all gulps for a single capillary into one `ROI2DArea`.
*   **Effect:** Instead of creating N objects (where N can be thousands), the system now creates 1 object per capillary. This displays gulps as simple red dots at their detection points.
*   **Result:** The "transfer measures to kymos" step should now be near-instantaneous.

### 2. Data Preservation Logic
*   **File:** `src/main/java/plugins/fmp/multicafe/fmp_experiment/capillaries/CapillaryGulps.java`
*   **Change:** Updated `transferROIsToMeasures` to recognize the new `ROI2DArea` ("dots") format.
*   **Logic:** When syncing ROIs back to data (e.g., before saving), if the system detects the "dots" ROI, it skips rebuilding the gulp data from the UI.
*   **Benefit:** This preserves the original high-fidelity gulp data (amplitudes/shapes) loaded from XML, preventing it from being overwritten by the simplified "dot" visualization.

### 3. Loading Sequence Optimization
*   **File:** `src/main/java/plugins/fmp/multicafe/dlg/browse/LoadSaveExperiment.java`
*   **Change:** Added timing instrumentation to identify bottlenecks and removed redundant calls to `transferCapillariesMeasuresToKymos`.
*   **File:** `src/main/java/plugins/fmp/multicafe/fmp_experiment/sequence/SequenceKymos.java`
*   **Change:** Wrapped the transfer logic in `beginUpdate()` and `endUpdate()` blocks to batch UI notifications, further improving performance.

## Status
*   **Performance:** Loading time bottleneck in Step 5 has been resolved.
*   **Data Integrity:** Gulp data is safely preserved during visualization.
*   **Visualization:** Gulps now appear as red dots, reducing visual clutter and memory usage.

