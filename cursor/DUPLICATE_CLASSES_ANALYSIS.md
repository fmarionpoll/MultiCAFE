# Analysis of Duplicate and Renamed Classes in MultiCAFE

This document outlines the mapping between legacy classes (in `experiment`, `series`, `tools`) and their updated counterparts (in `experiment1`, `series1`, `tools1`).

## 1. Time Management
*   **Old:** `src/main/java/plugins/fmp/multicafe/experiment/ExperimentTimeManager.java`
*   **New:** `src/main/java/plugins/fmp/multicafe/experiment1/sequence/TimeManager.java`
    *   **Function:** Manages file time extraction, interval calculation, and binary search for timeframes.
    *   **Changes:** The new `TimeManager` is cleaner and located within the `sequence` package.

## 2. Fly Detection
*   **Old:** `src/main/java/plugins/fmp/multicafe/series/FlyDetect.java` (and `FlyDetect1`, `FlyDetect2`)
*   **New:** `src/main/java/plugins/fmp/multicafe/series1/DetectFlyFromCleanBackground.java` (and `DetectFlyUsingSimpleThreshold.java`)
    *   **Function:** algorithms for detecting flies in images.
    *   **Changes:** The abstract class and implementations were renamed to be more descriptive.
    *   **Helper:** `series/FlyDetectTools.java` corresponds to `series1/DetectFlyTools.java`.

## 3. Image Registration (Drift Correction)
*   **Old:** `src/main/java/plugins/fmp/multicafe/tools/ImageRegistration.java` (and `ImageRegistrationGaspard.java`)
*   **New:** `src/main/java/plugins/fmp/multicafe/series1/Registration.java` (uses `tools1/GaspardRigidRegistration.java`)
    *   **Function:** Corrects image drift and rotation.
    *   **Changes:** Refactored from a simple "tool" to a full "Series" process (`Registration` extends `BuildSeries`), enabling batch processing.

## 4. Blob Detection
*   **Old:** `src/main/java/plugins/fmp/multicafe/tools/Blobs.java`
*   **New:** `src/main/java/plugins/fmp/multicafe/tools1/polyline/Blobs.java`
    *   **Function:** Connected component labeling and blob extraction.
    *   **Changes:** Moved from `tools` root to `tools1/polyline`. Code is nearly identical but the new version includes better Javadoc.

## 5. Cage Table Model (UI)
*   **Old:** `src/main/java/plugins/fmp/multicafe/tools/JComponents/CageTableModel.java`
*   **New:** `src/main/java/plugins/fmp/multicafe/experiment1/cages/CageTableModel.java`
    *   **Function:** Table model for displaying cage data in the UI.
    *   **Changes:** Moved from generic `JComponents` to the specific `experiment1/cages` package.

## 6. Excel Export
*   **Old:** `src/main/java/plugins/fmp/multicafe/tools/toExcel/XLSExportMoveResults.java`
*   **New:** `src/main/java/plugins/fmp/multicafe/tools1/toExcel/XLSExportMeasuresCagesAsQuery.java` (and `XLSExportMeasuresFromSpot`)
    *   **Function:** Exporting experiment results to Excel.
    *   **Changes:** Refactored from specific classes per export type to an inheritance-based approach with factory patterns.

## 7. ROI Measures
*   **Old:** `src/main/java/plugins/fmp/multicafe/tools/ROI2D/ROI2DMeasures.java`
*   **New:** `src/main/java/plugins/fmp/multicafe/tools1/ROI2D/ROI2DMeasures.java`
    *   **Function:** Geometric calculations for ROIs (ellipses, mass centers).
    *   **Changes:** Same name but different root package. `tools1` version contains additional methods (e.g., `getContourOfDetectedSpot`).

