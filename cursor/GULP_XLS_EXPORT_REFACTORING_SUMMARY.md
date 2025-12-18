# Gulp XLS Export Refactoring Summary

## Plan Overview

**Goal**: Refactor `XLSExportMeasuresFromGulp` to use the same data generation logic as the Charts, ensuring exported values match visualized data.

## Problem Statement

The gulp XLS export and Chart visualization used different code paths:
- **Export**: Iterated directly over capillaries using `getDataFromCapillary()` → `getCapillaryMeasuresForXLSPass1()`
- **Charts**: Used `CageCapillarySeriesBuilder` which processes data through `cap.getMeasurements()`

This led to discrepancies in exported values compared to chart displays.

## Implementation

### Supported Gulp Types

The following gulp types are now supported by the chart builder approach:
- `SUMGULPS` - Cumulated volume of gulps
- `SUMGULPS_LR` - Cumulated volume of gulps per cage (L+R)
- `NBGULPS` - Number of gulps
- `AMPLITUDEGULPS` - Amplitude of gulps

### Unsupported Types (Legacy Approach)

The following types continue to use the original implementation:
- `TTOGULP` - Time to previous gulp
- `TTOGULP_LR` - Time to previous gulp of either capillary
- `MARKOV_CHAIN` - Markov chain transitions
- `AUTOCORREL` - Autocorrelation
- `CROSSCORREL` - Cross-correlation
- `CROSSCORREL_LR` - Cross-correlation L+R

## Changes Made

### 1. Added Imports
- `CageCapillarySeriesBuilder`
- `XYSeries`, `XYSeriesCollection`
- `Cage`
- `ResultsCapillaries`
- `List`

### 2. Created Support Check Method
- `isSupportedByChartBuilder()`: Determines if a gulp result type can use the chart builder approach

### 3. Refactored Main Export Method
- `xlsExportExperimentGulpDataToSheet()`: Now conditionally routes to:
  - `xlsExportExperimentGulpDataToSheetUsingBuilder()` for supported types
  - `xlsExportExperimentGulpDataToSheetLegacy()` for unsupported types

### 4. New Builder-Based Export Method
- `xlsExportExperimentGulpDataToSheetUsingBuilder()`:
  - Iterates through cages (not just capillaries)
  - Uses `CageCapillarySeriesBuilder` to build datasets
  - Calls `exp.getCages().prepareComputations()` for consistent processing
  - Converts XYSeries to Results format for Excel export

### 5. Helper Methods (Reused from Capillary Export)
- `findSeriesForCapillary()`: Maps capillaries to correct XYSeries (handles LR types: L→Sum, R→PI)
- `findSeriesByKey()`: Finds series in collection by key
- `convertXYSeriesToResults()`: Converts sparse XYSeries (time, value) to dense Results binned array

### 6. Maintained Legacy Method
- `xlsExportExperimentGulpDataToSheetLegacy()`: Preserves original implementation for unsupported types

## Data Flow

```
CapillaryMeasure (raw data)
    ↓
CageCapillarySeriesBuilder.build()
    - Applies same processing as charts (evaporation correction, LR computations)
    ↓
XYSeriesCollection (contains XYSeries for each capillary/cage)
    ↓
findSeriesForCapillary() → XYSeries
    ↓
convertXYSeriesToResults() → Results (binned array)
    ↓
Excel export
```

## Benefits

1. **Consistency**: Exported values now match chart displays for supported types
2. **Same Processing Logic**: Both charts and export use `CageCapillarySeriesBuilder` and `prepareComputations()`
3. **Backward Compatibility**: Unsupported types continue to work with original implementation
4. **Code Reuse**: Helper methods shared with capillary export refactoring

## Files Modified

- [`src/main/java/plugins/fmp/multicafe/fmp_tools/toExcel/XLSExportMeasuresFromGulp.java`](src/main/java/plugins/fmp/multicafe/fmp_tools/toExcel/XLSExportMeasuresFromGulp.java)

## Related Refactoring

This follows the same pattern as the capillary levels export refactoring documented in `XLS_EXPORT_REFACTORING_PLAN.md`.
