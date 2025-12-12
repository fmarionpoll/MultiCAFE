# Documentation: Computation Classes Methods

This document describes all methods in the computation classes created for capillary measure computations.

## CagesArrayCapillariesComputation

**Location:** `src/main/java/plugins/fmp/multicafe/fmp_experiment/cages/CagesArrayCapillariesComputation.java`

**Purpose:** Handles experiment-wide capillary measure computations that require access to all cages across the experiment. Primarily handles evaporation correction.

### Public Methods

#### Constructor

**`CagesArrayCapillariesComputation(CagesArray cagesArray)`**
- **Parameters:**
  - `cagesArray` - The CagesArray instance to operate on (must not be null)
- **Throws:** `IllegalArgumentException` if cagesArray is null
- **Description:** Creates a new computation instance for the given CagesArray

#### Main Computation Methods

**`void computeEvaporationCorrection(Experiment exp)`**
- **Parameters:**
  - `exp` - The experiment containing all capillaries
- **Description:** 
  - Computes evaporation correction for all capillaries across all cages
  - First dispatches capillaries to cages to ensure proper organization
  - Collects all capillaries with `capNFlies == 0` for evaporation calculation
  - Separates zero-fly capillaries into L and R groups based on side (from `capSide` or name)
  - Computes average evaporation separately for L and R sides using `computeAverageMeasure()`
  - Applies the appropriate average evaporation (L or R) to each capillary based on its side
  - Creates `ptsTopCorrected` measure by subtracting evaporation from `ptsTop`
  - Stores the corrected measure in `cap.ptsTopCorrected` for each capillary
- **Side Effects:** Modifies `cap.ptsTopCorrected` for all capillaries in all cages
- **Note:** If side is unclear, uses L-side evaporation as default

**`void clearComputedMeasures()`**
- **Description:** 
  - Clears all computed measures from all capillaries in all cages
  - Calls `cap.clearComputedMeasures()` for each capillary in each cage
- **Side Effects:** Resets `ptsTopCorrected` to null for all capillaries

### Private Helper Methods

**`String getCapillarySide(Capillary cap)`**
- **Parameters:**
  - `cap` - The capillary to check
- **Returns:** "L", "R", or empty string
- **Description:**
  - Determines the side (L or R) of a capillary
  - First checks `cap.capSide` if not null and not "."
  - If not found, extracts from capillary ROI name (checks for "L"/"1" for left, "R"/"2" for right)
  - Returns empty string if side cannot be determined

**`Level2D computeAverageMeasure(List<Capillary> capillaries)`**
- **Parameters:**
  - `capillaries` - List of capillaries to average
- **Returns:** `Level2D` polyline containing averaged y-values, or null if input is invalid
- **Description:**
  - Computes the point-by-point average of `ptsTop` measures from a list of capillaries
  - Finds the maximum number of points across all capillaries
  - Accumulates y-values at each time point across all capillaries
  - Counts how many capillaries contributed to each time point
  - Computes average = sum / count for each time point
  - Returns a new `Level2D` with averaged values
- **Note:** Only uses capillaries that have valid `ptsTop` measures with data points

**`CapillaryMeasure subtractEvaporation(CapillaryMeasure original, Level2D evaporation)`**
- **Parameters:**
  - `original` - The original capillary measure (must have valid polylineLevel)
  - `evaporation` - The evaporation curve to subtract (must not be null)
- **Returns:** New `CapillaryMeasure` with evaporation subtracted, or null if inputs invalid
- **Description:**
  - Creates a new corrected measure by subtracting evaporation values point-by-point
  - Uses the minimum number of points between original and evaporation
  - For each point: `correctedY[i] = originalY[i] - evaporationY[i]`
  - Creates new `Level2D` polyline with corrected values
  - Creates new `CapillaryMeasure` with name `original.capName + "_corrected"`
  - Sets the corrected polyline as `polylineLevel` of the new measure
- **Note:** The returned measure is a new instance, not a modification of the original

---

## CageCapillariesComputation

**Location:** `src/main/java/plugins/fmp/multicafe/fmp_experiment/cages/CageCapillariesComputation.java`

**Purpose:** Handles cage-level capillary measure computations for a specific cage. Primarily handles L+R aggregation (SUM and PI calculations).

### Public Methods

#### Constructor

**`CageCapillariesComputation(Cage cage)`**
- **Parameters:**
  - `cage` - The Cage instance to operate on (must not be null)
- **Throws:** `IllegalArgumentException` if cage is null
- **Description:** Creates a new computation instance for the given Cage

#### Accessor Methods

**`Cage getCage()`**
- **Returns:** The associated Cage instance
- **Description:** Returns the cage this computation instance is associated with

#### Main Computation Methods

**`void computeLRMeasures(double threshold)`**
- **Parameters:**
  - `threshold` - Minimum SUM value required to compute PI (values below threshold result in PI = 0)
- **Description:**
  - Computes L+R measures (SUM and PI) for capillaries within the associated cage
  - Groups capillaries by side (L or R) using `getCapillarySide()`
  - If no clear L/R grouping found, splits capillaries in half (first half = left, second half = right)
  - For each group (left and right):
    - Uses `aggregateMeasures()` to sum absolute values of all capillaries in the group
    - Uses `ptsTopCorrected` if available, otherwise falls back to `ptsTop`
  - Computes SUM and PI from aggregated left and right groups using `computeSumAndPI()`
  - Stores results in `computedMeasures` map with keys "SUM" and "PI"
- **Supported Configurations:**
  - 2 capillaries: standard L+R pairing
  - 4+ capillaries: groups multiple L capillaries together, multiple R capillaries together
- **Side Effects:** Modifies `computedMeasures` map
- **Note:** Requires at least 2 capillaries in the cage. Should be called AFTER evaporation correction if needed.

**`void clearComputedMeasures()`**
- **Description:**
  - Clears all computed measures for the associated cage
  - Clears the `computedMeasures` map
  - Also calls `cap.clearComputedMeasures()` for each capillary in the cage
- **Side Effects:** Clears the internal map and all capillary computed measures

#### Data Access Methods

**`CapillaryMeasure getComputedMeasure(String key)`**
- **Parameters:**
  - `key` - The measure key ("SUM", "PI", etc.)
- **Returns:** The computed measure, or null if not found or map is null
- **Description:** Generic method to retrieve computed measures by key

**`CapillaryMeasure getSumMeasure()`**
- **Returns:** The SUM measure (|L| + |R|), or null if not computed
- **Description:** Convenience method equivalent to `getComputedMeasure("SUM")`

**`CapillaryMeasure getPIMeasure()`**
- **Returns:** The PI measure ((L-R)/(L+R)), or null if not computed
- **Description:** Convenience method equivalent to `getComputedMeasure("PI")`

### Private Helper Methods

**`CapillaryMeasure aggregateMeasures(List<Capillary> capillaries)`**
- **Parameters:**
  - `capillaries` - List of capillaries to aggregate
- **Returns:** New `CapillaryMeasure` with summed values, or null if input is invalid
- **Description:**
  - Aggregates measures from multiple capillaries by summing their absolute values
  - Prefers `ptsTopCorrected` if available, otherwise uses `ptsTop`
  - Finds the maximum number of points across all capillaries
  - Sums absolute values: `sumY[i] += Math.abs(measure.ypoints[i])` for each capillary
  - Creates new `Level2D` polyline with summed values
  - Creates new `CapillaryMeasure` named "aggregated"
- **Note:** Uses absolute values to handle positive/negative variations

**`void computeSumAndPI(CapillaryMeasure aggregatedLeft, CapillaryMeasure aggregatedRight, double threshold)`**
- **Parameters:**
  - `aggregatedLeft` - The aggregated measure for left-side capillaries
  - `aggregatedRight` - The aggregated measure for right-side capillaries
  - `threshold` - Minimum SUM required to compute PI
- **Description:**
  - Computes SUM and PI from left and right aggregated measures
  - For each time point:
    - `SUM = |L| + |R|` (absolute values of left and right)
    - If `SUM > 0` and `SUM >= threshold`: `PI = (L - R) / SUM`
    - Otherwise: `PI = 0`
  - Creates two new `Level2D` polylines: one for SUM, one for PI
  - Creates two new `CapillaryMeasure` instances with cage name prefix
  - Stores them in `computedMeasures` map with keys "SUM" and "PI"
- **Side Effects:** Modifies `computedMeasures` map
- **Note:** Uses the minimum number of points between left and right measures

**`String getCapillarySide(Capillary cap)`**
- **Parameters:**
  - `cap` - The capillary to check
- **Returns:** "L", "R", or empty string
- **Description:**
  - Determines the side (L or R) of a capillary
  - First checks `cap.capSide` if not null and not "."
  - If not found, extracts from capillary ROI name (uppercase check for "L"/"1" = left, "R"/"2" = right)
  - Returns empty string if side cannot be determined

---

## Usage Notes

### Typical Workflow

1. **Create computation instances:**
   ```java
   CagesArrayCapillariesComputation arrayComp = new CagesArrayCapillariesComputation(cagesArray);
   ```

2. **Compute evaporation correction (experiment-wide):**
   ```java
   arrayComp.computeEvaporationCorrection(exp);
   // This sets cap.ptsTopCorrected for all capillaries
   ```

3. **Compute L+R measures (per cage):**
   ```java
   for (Cage cage : cagesArray.getCageList()) {
       CageCapillariesComputation cageComp = new CageCapillariesComputation(cage);
       cageComp.computeLRMeasures(threshold);
       // Access via: cageComp.getSumMeasure() or cageComp.getPIMeasure()
   }
   ```

4. **Clear when done:**
   ```java
   arrayComp.clearComputedMeasures();
   ```

### Integration with CagesArray

The `CagesArray` class delegates to these computation classes:
- `CagesArray.computeEvaporationCorrection()` → delegates to `CagesArrayCapillariesComputation`
- `CagesArray.computeLRMeasures()` → creates `CageCapillariesComputation` for each cage and stores in transient map
- `CagesArray.getCageComputation(cageID)` → retrieves stored `CageCapillariesComputation` instance

---

## Changes Summary

### Modified Files

1. **XLSExportMeasuresFromCapillary.java**
   - Updated `exportExperimentData()` to call computation methods before export
   - Added `getLRDataFromCage()` method to read L+R measures from `CageCapillariesComputation`
   - Modified `getXLSResultsDataValuesFromCapillaryMeasures()` to handle TOPLEVEL_LR via new method

2. **CagesArray.java**
   - Updated computation methods to delegate to new computation classes
   - Added `getCageComputation(cageID)` method
   - Added transient map to store `CageCapillariesComputation` instances

3. **Capillary.java**
   - Removed `ptsTopLRSum` and `ptsTopLRPI` fields
   - Updated `clearComputedMeasures()` to only clear `ptsTopCorrected`
   - Updated `getCapillaryMeasuresForXLSPass1()` TOPLEVEL_LR case (now handled by export code)

### Unchanged Files

**Chart/Graph Methods:** 
- ❌ `ChartLevelsFrame.java` - **NO CHANGES**
- ❌ `ChartLevels.java` - **NO CHANGES**
- ❌ `ChartPositions.java` - **NO CHANGES**
- ❌ `ChartCageArrayFrame.java` - **NO CHANGES**
- ❌ `ChartFlyPositions.java` - **NO CHANGES**
- ❌ `ChartCageBuild.java` - **NO CHANGES**

All chart/graph display code continues to work as before, using the same data access patterns.

**Other Export Methods:**
- ❌ `XLSResultsFromCapillaries.java` - **NO CHANGES** (continues to work with existing patterns)

The graph and chart methods were not modified because they access capillary data directly through existing methods (`getCapillaryMeasuresForXLSPass1()`, etc.) which continue to work for non-L+R measures. L+R measures are only needed for export, not for display.



