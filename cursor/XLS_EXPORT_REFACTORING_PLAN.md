# Plan: Refactor XLS Export to use Chart Logic

## Title

**Unify XLS Export and Chart Data Retrieval**

## Problem

The XLS export (`XLSExportMeasuresFromCapillary`) and Chart visualization (`ChartCageArrayFrame`) currently use different code paths to retrieve and process capillary data. This leads to discrepancies in the output, particularly for computed values like Sum/PI (LR) and evaporation correction.

## Goal

Modify the XLS export process to use the exact same data generation logic as the Charts, ensuring that exported values match the visualized data.

## Implementation Details

The refactoring will focus on [`src/main/java/plugins/fmp/multicafe/fmp_tools/toExcel/XLSExportMeasuresFromCapillary.java`](src/main/java/plugins/fmp/multicafe/fmp_tools/toExcel/XLSExportMeasuresFromCapillary.java).

### 1. Update `xlsExportExperimentCapDataToSheet`

Replace the current iteration over `ResultsArrayFromCapillaries` with a cage-based iteration using `CageCapillarySeriesBuilder`.

**Logic Flow:**

1.  Iterate through all `Cage` objects in the experiment (`exp.getCages().getCageList()`).
2.  For each `Cage`, instantiate `CageCapillarySeriesBuilder` (from `plugins.fmp.multicafe.fmp_tools.chart.builders`).
3.  Call `builder.build(exp, cage, options)` to get an `XYSeriesCollection` containing the processed data series (including Sum/PI if applicable).
4.  Iterate through the capillaries in the current cage.
5.  For each capillary:

    -   Determine the expected series key (e.g., "Sum", "PI", or raw capillary side) based on the export type.
    -   Retrieve the corresponding `XYSeries` from the collection.
    -   Convert the sparse `XYSeries` data (time, value) into a dense `Results` object (bins) compatible with the Excel writer.
    -   Call `writeExperimentCapInfos` and `writeXLSResult` to write the row to the Excel sheet.

### 2. Implement Data Conversion Helper

Create a helper method `convertXYSeriesToResults` within `XLSExportMeasuresFromCapillary` to bridge the gap between JFreeChart's `XYSeries` and the Excel export's `Results` format.

**Key responsibilities:**

-   Initialize a `Results` object with the correct size (based on `options.buildExcelStepMs`).
-   Iterate through the `XYSeries` items.
-   Map each data point's X value (time in minutes) to the corresponding time bin in the `Results` value array.
-   Handle scaling if necessary (though `CageCapillarySeriesBuilder` already handles most scaling).

### 3. Handle LR (Sum/PI) Mapping

Maintain the current Excel structure where "Sum" maps to the 'L' capillary row and "PI" maps to the 'R' capillary row (matching `ResultsCapillaries` logic).

## Files to Modify

-   [`src/main/java/plugins/fmp/multicafe/fmp_tools/toExcel/XLSExportMeasuresFromCapillary.java`](src/main/java/plugins/fmp/multicafe/fmp_tools/toExcel/XLSExportMeasuresFromCapillary.java)

## Existing Code Reuse

-   Leverages `CageCapillarySeriesBuilder` (from [`src/main/java/plugins/fmp/multicafe/fmp_tools/chart/builders/CageCapillarySeriesBuilder.java`](src/main/java/plugins/fmp/multicafe/fmp_tools/chart/builders/CageCapillarySeriesBuilder.java)) for robust data processing.

## Implementation Summary

The refactoring was successfully completed with the following changes:

1. **Added imports**: `CageCapillarySeriesBuilder`, `XYSeries`, `XYSeriesCollection`, and `ResultsCapillaries`.

2. **Refactored `xlsExportExperimentCapDataToSheet`**:
   - Replaced `ResultsArrayFromCapillaries.getMeasuresFromAllCapillaries()` with a cage-based loop
   - Uses `CageCapillarySeriesBuilder` to build datasets (same as charts)
   - Iterates through cages and capillaries to match series to capillaries

3. **Added helper methods**:
   - `findSeriesForCapillary()`: Maps capillaries to the correct XYSeries (handles LR types where L→Sum, R→PI)
   - `findSeriesByKey()`: Finds a series in the collection by key
   - `convertXYSeriesToResults()`: Converts XYSeries (time in minutes, value) to Results (binned array)

4. **Maintained compatibility**: Uses the same `prepareComputations` call as the chart code (`exp.getCages().prepareComputations(exp, resultsOptions)`)

The XLS export now uses the same data processing pipeline as the charts, ensuring exported values match the visualized data, including computed values like Sum/PI and evaporation correction.


