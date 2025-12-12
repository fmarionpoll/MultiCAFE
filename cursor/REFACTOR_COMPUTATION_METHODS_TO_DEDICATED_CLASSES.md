# Refactor Computation Methods to Dedicated Classes

## Overview

Refactor capillary computation methods to follow the existing codebase pattern with dedicated classes:

1. Create `CagesArrayCapillariesComputation` (helper class) for experiment-wide computations (evaporation correction)
2. Create `CageCapillariesComputation` (helper class) for cage-level computations (L+R aggregation)

## Architecture Decisions

### 1. CagesArrayCapillariesComputation

**Purpose:** Handles experiment-wide capillary computations that require access to all cages:
- Evaporation correction (needs to find all capillaries with nFlies=0 across all cages)
- Aggregations across cages

**Implementation:** Helper class (not subclass) - follows the Properties/Configuration pattern
- Takes CagesArray as constructor parameter
- Contains methods: `computeEvaporationCorrection()`, `clearComputedMeasures()`
- Helper methods: `computeAverageMeasure()`, `getCapillarySide()`, `subtractEvaporation()`

### 2. CageCapillariesComputation  

**Purpose:** Handles cage-level capillary computations:
- L+R aggregation (SUM, PI) - handles 2, 4, 8+ capillaries per cage
- Other cage-specific aggregations

**Implementation:** Helper class (not subclass) - works with Cage instances
- Takes Cage as constructor parameter
- Stores computed measures in `transient Map<String, CapillaryMeasure> computedMeasures`
- Keys: "SUM", "PI"
- Handles multiple capillaries by grouping into Left and Right sets, then aggregating

**For multiple capillaries (4, 8):**
- Groups capillaries by side (L or R) based on name/capSide
- If no clear grouping, splits in half (first half = left, second half = right)
- Aggregates each group by summing absolute values
- Then computes SUM = |L| + |R| and PI = (L-R)/SUM

### 3. Storage Location for L+R Results

**Final Implementation:** 
- L+R measures stored in `CageCapillariesComputation.computedMeasures` Map
- Removed `ptsTopLRSum` and `ptsTopLRPI` from `Capillary` class
- `ptsTopCorrected` remains in `Capillary` (set by CagesArrayCapillariesComputation)

## Implementation Details

### Phase 1: Create CagesArrayCapillariesComputation

**File:** `src/main/java/plugins/fmp/multicafe/fmp_experiment/cages/CagesArrayCapillariesComputation.java`

**Methods:**
- `computeEvaporationCorrection(Experiment exp)` - computes evaporation from zero-fly capillaries and corrects all
- `clearComputedMeasures()` - clears all computed measures across all cages
- Helper: `computeAverageMeasure(List<Capillary>)` - averages measures from a list of capillaries
- Helper: `getCapillarySide(Capillary)` - determines L/R side from capSide or name
- Helper: `subtractEvaporation(CapillaryMeasure, Level2D)` - creates corrected measure

**Usage:**
```java
CagesArrayCapillariesComputation computation = new CagesArrayCapillariesComputation(cagesArray);
computation.computeEvaporationCorrection(exp);
```

### Phase 2: Create CageCapillariesComputation

**File:** `src/main/java/plugins/fmp/multicafe/fmp_experiment/cages/CageCapillariesComputation.java`

**Methods:**
- `computeLRMeasures(double threshold)` - handles 2/4/8 capillaries by grouping and aggregating
- `clearComputedMeasures()` - clears cage-level computed measures
- `getComputedMeasure(String key)` - gets measure by key ("SUM", "PI")
- `getSumMeasure()` - convenience method for SUM
- `getPIMeasure()` - convenience method for PI
- Helper: `aggregateMeasures(List<Capillary>)` - sums measures from multiple capillaries
- Helper: `computeSumAndPI(CapillaryMeasure, CapillaryMeasure, double)` - computes SUM and PI
- Helper: `getCapillarySide(Capillary)` - determines L/R side

**Storage:**
- `transient Map<String, CapillaryMeasure> computedMeasures` - stores "SUM" and "PI" measures

### Phase 3: Update CagesArray

**Updated methods to delegate to computation classes:**
- `computeEvaporationCorrection(Experiment)` - delegates to `CagesArrayCapillariesComputation`
- `computeLRMeasures(Experiment, double)` - creates `CageCapillariesComputation` for each cage and stores in transient map
- `clearComputedMeasures()` - delegates to both computation classes
- Added: `getCageComputation(int cageID)` - retrieves stored `CageCapillariesComputation` instance
- Added: `transient Map<Integer, CageCapillariesComputation> cageComputations` - stores computation instances

### Phase 4: Update Capillary

**Removed:**
- `ptsTopLRSum` field
- `ptsTopLRPI` field
- References in `clearComputedMeasures()`

**Kept:**
- `ptsTopCorrected` field (set by CagesArrayCapillariesComputation)

**Updated:**
- `getCapillaryMeasuresForXLSPass1()` - TOPLEVEL_LR case now returns fallback (handled by export code)

### Phase 5: Update Export Code

**File:** `src/main/java/plugins/fmp/multicafe/fmp_tools/toExcel/capillaries/XLSExportMeasuresFromCapillary.java`

**Updated:**
- `exportExperimentData()` - calls `computeEvaporationCorrection()` and `computeLRMeasures()` before export
- `getXLSResultsDataValuesFromCapillaryMeasures()` - handles TOPLEVEL_LR by calling `getLRDataFromCage()`
- Added: `getLRDataFromCage()` - reads SUM/PI from `CageCapillariesComputation` based on capillary side
- Added: `getCapillarySide(Capillary)` - helper to determine L/R side

**Export logic for TOPLEVEL_LR:**
- L capillaries: export SUM measure from `CageCapillariesComputation`
- R capillaries: export PI measure from `CageCapillariesComputation`
- Falls back to raw if computation not available

## Implementation Status

✅ **Completed:**
1. Created `CagesArrayCapillariesComputation` class
2. Created `CageCapillariesComputation` class  
3. Moved computation methods from `CagesArray` to new classes (via delegation)
4. Updated L+R storage to use `CageCapillariesComputation` fields
5. Removed `ptsTopLRSum` and `ptsTopLRPI` from `Capillary`
6. Updated `XLSExportMeasuresFromCapillary` to use new computation classes
7. Updated `Capillary.getCapillaryMeasuresForXLSPass1()` to handle new structure

## Notes

- Both computation classes are helper classes (composition pattern), not subclasses (inheritance)
- This follows the existing codebase pattern (similar to `CagesArrayProperties`, `CageProperties`)
- Computed measures are stored as `transient` fields (not persisted)
- The implementation supports flexible numbers of capillaries per cage (2, 4, 8+)
- L+R computation groups capillaries into Left and Right sets before aggregation
- All code compiles with no errors


