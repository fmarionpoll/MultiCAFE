# XLS Export Data Flow Explanation

## Question
Is data transformed from Results → XYSeries → Results?

## Answer
**No, not exactly.** The data flow is more direct:

## Actual Data Flow

### Source Data
The raw measurement data is stored in `CapillaryMeasure.polylineLevel` (a `Level2D` object containing xpoints/ypoints arrays).

### Chart Builder Path (New Approach)

```
CapillaryMeasure (raw data: polylineLevel.ypoints[])
    ↓
CageCapillarySeriesBuilder.createXYSeriesFromCapillaryMeasure()
    - Directly reads from capMeasure.getValueAt(j) → polylineLevel.ypoints[j]
    - Applies scaling if needed (volume conversion)
    - Creates XYSeries with (time_minutes, value) pairs
    ↓
XYSeries (JFreeChart format for visualization)
    ↓
convertXYSeriesToResults()
    - Maps sparse XYSeries points to dense binned array
    - Creates Results object with valuesOut[] array
    ↓
Results (binned array format for Excel export)
    ↓
Excel file
```

### Key Points

1. **No Results intermediate**: The `XYSeries` is created **directly** from `CapillaryMeasure`, not from a `Results` object first.

2. **Why the conversion?**
   - **Charts** need `XYSeries` format (JFreeChart library requirement)
   - **Excel export** needs `Results` format (binned array with fixed time intervals)
   - We want to use the **same processing logic** (evaporation correction, LR computations, etc.) that charts use

3. **The conversion is necessary** because:
   - `XYSeries` is sparse (only has data points where measurements exist)
   - `Results.valuesOut[]` is dense (one value per time bin, with NaN for missing data)
   - Excel export requires the dense format to match the time interval headers

### Old Approach (Before Refactoring)

```
CapillaryMeasure
    ↓
getCapillaryMeasuresForXLSPass1()
    - Returns ArrayList<Integer>
    ↓
Results.getDataFromCapillary()
    - Converts to ArrayList<Double>
    - Applies T0 subtraction, relative scaling, etc.
    ↓
Results (binned array)
    ↓
Excel file
```

### Why the Refactoring?

The old approach had **different processing logic** than the charts:
- Charts use `CageCapillarySeriesBuilder` which applies evaporation correction, LR computations at the cage level
- Old export used `getCapillaryMeasuresForXLSPass1()` which had different logic
- This caused **discrepancies** between chart values and exported values

The new approach ensures **both use the same processing pipeline**, guaranteeing consistency.

## Summary

The data flow is: **CapillaryMeasure → XYSeries → Results**

Not: **CapillaryMeasure → Results → XYSeries → Results**

The XYSeries is an intermediate format required to bridge between:
- The chart visualization system (which uses JFreeChart's XYSeries)
- The Excel export system (which uses Results binned arrays)

This conversion ensures both systems use the same underlying data processing logic, eliminating discrepancies.






