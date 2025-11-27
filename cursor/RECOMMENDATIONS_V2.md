# MultiCAFE Project Analysis & Recommendations (V2)

**Date:** November 23, 2025
**Status:** Post-Initial Refactoring

## 1. Progress Update
Significant improvements have been made to the codebase structure:
*   **`Experiment.java`**: Much logic has been moved to `ExperimentPersistence`, `ExperimentTimeManager`, and `ExperimentService`. However, many public fields remain.
*   **Series Analysis**: The `series` package has been refactored into a Controller-Service architecture. `BuildKymographs`, `DetectGulps`, `DetectLevels` now delegate domain logic to `KymographBuilder`, `GulpDetector`, and `LevelDetector` in the `service` package.
*   **Error Handling**: A `Logger` class has been introduced, but usage is not yet universal.

## 2. High Priority Recommendations

### 2.1. Complete Encapsulation of Data Classes
Many core data structures still expose their internal state via public fields. This breaks encapsulation and makes the code brittle.

*   **Target Classes:**
    *   `plugins.fmp.multicafe.experiment.SequenceCamData`
    *   `plugins.fmp.multicafe.experiment.SequenceKymos`
    *   `plugins.fmp.multicafe.experiment.capillaries.Capillaries` & `CapillariesDescription`
    *   `plugins.fmp.multicafe.experiment.cages.Cages`, `Cage`, `FlyPositions`, `FlyPosition`
    *   `plugins.fmp.multicafe.experiment.ExperimentTimeManager` (fields like `camImageFirst_ms`)
    *   `plugins.fmp.multicafe.experiment.Experiment` (remaining fields like `seqReference`, `cages`, `timeManager`)

*   **Recommendation:**
    *   Change `public` fields to `private`.
    *   Generate getters and setters.
    *   Refactor call sites (potentially hundreds) to use accessors.

### 2.2. Standardize Error Handling
Despite the introduction of `Logger`, legacy error handling persists in many files.

*   **Findings:**
    *   **`System.out.println`**: ~39 remaining instances (e.g., `XLSExport`, `LevelsKMeans`, `SequenceKymosUtils`).
    *   **`printStackTrace`**: ~35 remaining instances (e.g., `CagesPersistence`, `CapillariesPersistence`, `FlyDetect`).

*   **Recommendation:**
    *   Systematically replace all `System.out.println` debug messages with `Logger.debug()` or `Logger.info()`.
    *   Replace `e.printStackTrace()` with `Logger.error("msg", e)`.

## 3. Medium Priority Recommendations

### 3.1. Refactor Data Transfer Objects (DTOs)
Classes like `Capillary` and `Cage` act as DTOs but also contain business logic (e.g., `csvExport_...`, `detectGulps`).

*   **Issue:** Violation of Single Responsibility Principle. `Capillary` should hold data; `CapillaryService` or `CapillaryExportService` should handle logic.
*   **Recommendation:**
    *   Extract CSV/XLS export logic from `Capillary` into `XLSExportCapillariesResults` or a dedicated helper.
    *   Extract detection logic (`detectGulps`) from `Capillary` to `GulpDetector` (partially done, but check for remnants).

### 3.2. Separate UI and Business Logic in Dialogs
The `dlg` (Dialog) classes often mix UI code (Swing) with business logic.

*   **Example:** `MCCapillaries_.updateDialogs` calls `SequenceKymosUtils.transferCamDataROIStoKymo(exp)`.
*   **Recommendation:**
    *   Move business logic calls out of UI event handlers into `ExperimentService` or specific Controllers.
    *   The UI should only invoke high-level Service methods.

### 3.3. Clean up "Utils" Classes
Classes like `SequenceKymosUtils` often become dumping grounds for miscellaneous logic.

*   **Recommendation:**
    *   Evaluate if methods in `Utils` classes belong to a specific Service (e.g., `KymographService`).

## 4. Low Priority / Maintenance

*   **Naming Conventions:** Some classes and fields use Hungarian notation or inconsistent naming (e.g., `strImagesDirectory` was fixed, but others might exist).
*   **Unused Code:** Review imports and unused private methods periodically.
*   **Documentation:** Javadoc is missing for most Service methods.

## 5. Proposed Next Steps
1.  **Fix Error Handling:** A mechanical but important task to ensure all logs go through the standard Logger.
2.  **Encapsulate `SequenceCamData` and `SequenceKymos`:** These are heavily used, so refactoring them will touch many files.
3.  **Refactor `Capillary`:** Move logic out and encapsulate fields.

