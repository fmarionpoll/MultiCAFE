# Retrieving Measurement Data from Capillaries

This document describes the unified system for retrieving measurement data from capillaries in MultiCAFE, including both stored measurements and computed derived measures.

## Overview

The data retrieval system provides a unified interface to access capillary measurements regardless of whether they are:
- **Stored directly** in the capillary's measurement fields (e.g., `measurements.ptsTop`, `measurements.ptsBottom`)
- **Computed on-the-fly** using computation strategies (e.g., number of gulps, autocorrelation)
- **Not yet implemented** (placeholders for future development)

All measurement types are defined in `EnumResults`, which acts as a comprehensive catalog of available measures and their retrieval methods.

## Main Entry Points

### 1. For Chart/Graph Display: `Capillary.getMeasurements()`

**Method Signature:**
```java
public CapillaryMeasure getMeasurements(EnumResults resultType, Experiment exp, ResultsOptions options)
```

**Usage:**
```java
// Example: Get number of gulps for charting
CapillaryMeasure measure = capillary.getMeasurements(
    EnumResults.NBGULPS, 
    experiment, 
    resultsOptions
);

if (measure != null && measure.polylineLevel != null) {
    // Access X and Y coordinates
    double[] xValues = measure.polylineLevel.xpoints;
    double[] yValues = measure.polylineLevel.ypoints;
    int nPoints = measure.polylineLevel.npoints;
}
```

**Return Format:** `CapillaryMeasure` object containing:
- `polylineLevel` (Level2D): A 2D polyline structure with:
  - `xpoints[]` (double[]): X-coordinates (typically time indices: 0, 1, 2, ...)
  - `ypoints[]` (double[]): Y-coordinates (measurement values)
  - `npoints` (int): Number of data points
- `capName` (String): Name identifier of the measurement

**When to Use:** 
- For generating charts and graphs
- When you need time-series data with X-Y coordinate pairs
- For visualization in JFreeChart or similar plotting libraries

---

### 2. For Excel Export: `Capillary.getCapillaryMeasuresForXLSPass1()`

**Method Signature:**
```java
public ArrayList<Integer> getCapillaryMeasuresForXLSPass1(
    EnumResults resultType, 
    long seriesBinMs, 
    long outputBinMs, 
    Experiment exp, 
    ResultsOptions options
)
```

**Usage:**
```java
// Example: Get top level data binned for Excel export
ArrayList<Integer> data = capillary.getCapillaryMeasuresForXLSPass1(
    EnumResults.TOPLEVEL,
    1000,  // seriesBinMs: original data bin duration in milliseconds
    5000,  // outputBinMs: desired output bin duration in milliseconds
    experiment,
    resultsOptions
);

// Data is automatically resampled from seriesBinMs to outputBinMs
for (Integer value : data) {
    // Process each binned value
}
```

**Return Format:** `ArrayList<Integer>`
- Integer values representing measurement values at each time bin
- Values are automatically resampled from `seriesBinMs` to `outputBinMs` resolution
- For computed measures, values are rounded to integers

**When to Use:**
- For Excel export operations
- When you need integer values at specific time resolutions
- For data that will be written to spreadsheet cells

**Note:** There is also a backward-compatibility version that doesn't require `exp` and `options`, but it only works for stored measures that don't require computation.

---

### 3. Via Results Helper Class: `Results.getDataFromCapillary()`

**Method Signature:**
```java
public void getDataFromCapillary(
    Experiment exp, 
    Capillary capillary, 
    long binData, 
    long binExcel, 
    ResultsOptions resultsOptions, 
    boolean subtractT0
)
```

**Usage:**
```java
// Example: Get data with T0 subtraction
Results results = new Results();
results.getDataFromCapillary(
    experiment,
    capillary,
    1000,  // binData: source data bin duration
    5000,  // binExcel: output bin duration
    resultsOptions,
    true   // subtractT0: subtract initial value
);

// Access the data
ArrayList<Double> values = results.dataValues;
```

**Return Format:** Data stored internally in `Results.dataValues` as `ArrayList<Double>`
- Values may have T0 subtracted if `subtractT0` is true
- Values are converted from integers to doubles
- Can be normalized relative to maximum if `resultsOptions.relativeToMaximum` is enabled

**When to Use:**
- When using the Results aggregation framework
- When you need T0 subtraction or relative normalization
- For multi-capillary data aggregation

---

## Measurement Types and Their Access Methods

### Stored Data Measures

These measures are stored directly in the capillary's measurement fields and accessed via named accessor methods:

| EnumResults | Accessor Method | Data Source |
|-------------|----------------|-------------|
| `TOPRAW` | `StoredDataAccessors.accessStored_TOPRAW()` | `measurements.ptsTop` via `cap.getTopLevel()` |
| `TOPLEVEL` | `StoredDataAccessors.accessStored_TOPLEVEL()` | `measurements.ptsTopCorrected` or `measurements.ptsTop` |
| `BOTTOMLEVEL` | `StoredDataAccessors.accessStored_BOTTOMLEVEL()` | `measurements.ptsBottom` via `cap.getBottomLevel()` |
| `DERIVEDVALUES` | `StoredDataAccessors.accessStored_DERIVEDVALUES()` | `measurements.ptsDerivative` via `cap.getDerivative()` |
| `SUMGULPS` | `StoredDataAccessors.accessStored_SUMGULPS()` | `measurements.ptsGulps.getMeasuresFromGulps()` |
| `XYIMAGE`, `XYTOPCAGE`, `XYTIPCAPS` | `StoredDataAccessors.accessStored_XYIMAGE()` etc. | `cage.flyPositions` |
| `AREA_SUM`, `AREA_SUMCLEAN`, etc. | `StoredDataAccessors.accessStored_AREA_SUM()` etc. | `spot.getMeasurements()` |

**Characteristics:**
- Access via `isStoredData()` returns `true`
- No computation required - data is already calculated and stored
- Can be accessed without `Experiment` and `ResultsOptions` (backward compatibility)

---

### Computed Measures

These measures are calculated on-the-fly using computation strategies:

| EnumResults | Computation Method | Description |
|-------------|-------------------|-------------|
| `NBGULPS` | `GulpMeasureComputation.computeNbGulps()` | Number of gulps at each time point |
| `AMPLITUDEGULPS` | `GulpMeasureComputation.computeAmplitudeGulps()` | Amplitude of gulps at each time point |
| `TTOGULP` | `GulpMeasureComputation.computeTToGulp()` | Time to previous gulp |
| `MARKOV_CHAIN` | `CorrelationComputation.computeMarkovChain()` | Boolean transition states |
| `AUTOCORREL` | `CorrelationComputation.computeAutocorrelation()` | Auto-correlation over n intervals |
| `AUTOCORREL_LR` | `CorrelationComputation.computeAutocorrelationLR()` | Auto-correlation over capillaries/cage |
| `CROSSCORREL` | `CorrelationComputation.computeCrosscorrelation()` | Cross-correlation over n intervals |
| `CROSSCORREL_LR` | `CorrelationComputation.computeCrosscorrelationLR()` | Cross-correlation over capillaries/cage |

**Characteristics:**
- Access via `requiresComputation()` returns `true`
- Requires `Experiment` and `ResultsOptions` parameters
- Returns `ArrayList<Double>` from computation, converted to appropriate output format

---

### Not Yet Implemented

| EnumResults | Placeholder Method |
|-------------|-------------------|
| `TTOGULP_LR` | `StoredDataAccessors.notImplemented_TTOGULP_LR()` |

**Characteristics:**
- Access via `isNotImplemented()` returns `true`
- Returns `null` when computation is attempted
- Placeholder for future implementation

---

## Data Format Details

### Level2D Structure (for Chart Display)

The `Level2D` class extends `Polyline2D` and contains:

```java
public class Level2D extends Polyline2D {
    public double[] xpoints;  // X-coordinates (typically 0, 1, 2, ... n-1)
    public double[] ypoints;  // Y-coordinates (measurement values)
    public int npoints;       // Number of points
}
```

**For Computed Measures:**
- X-coordinates are sequential indices (0, 1, 2, ...)
- Y-coordinates are the computed measurement values
- Each point represents one time bin

**For Stored Measures:**
- X-coordinates represent time indices in the original data
- Y-coordinates are the stored measurement values
- Resolution depends on the original measurement collection settings

---

### ArrayList<Integer> Format (for Excel Export)

- Integer values representing measurement values
- Values are rounded for computed measures
- Automatically resampled to requested time bin resolution
- One value per output time bin

---

### ArrayList<Double> Format (via Results class)

- Double precision values
- May have T0 subtracted (if `subtractT0` is true)
- Can be normalized relative to maximum (if option is enabled)
- One value per output time bin

---

## Usage Patterns

### Pattern 1: Simple Chart Display

```java
// Get stored measurement
CapillaryMeasure measure = capillary.getMeasurements(EnumResults.TOPLEVEL);

// Get computed measurement (requires exp and options)
ResultsOptions options = new ResultsOptions();
options.resultType = EnumResults.NBGULPS;
CapillaryMeasure gulps = capillary.getMeasurements(
    EnumResults.NBGULPS, 
    experiment, 
    options
);

// Extract data for plotting
if (gulps != null && gulps.polylineLevel != null) {
    double[] x = gulps.polylineLevel.xpoints;
    double[] y = gulps.polylineLevel.ypoints;
    // Use x and y arrays for charting
}
```

### Pattern 2: Excel Export with Binning

```java
// Get data with specific binning
long seriesBin = experiment.getKymoBin_ms();  // Original data resolution
long outputBin = 5000;  // 5 second bins for Excel

ArrayList<Integer> data = capillary.getCapillaryMeasuresForXLSPass1(
    EnumResults.AMPLITUDEGULPS,
    seriesBin,
    outputBin,
    experiment,
    resultsOptions
);

// Write to Excel sheet
for (int i = 0; i < data.size(); i++) {
    int value = data.get(i);
    // Write value to Excel cell at row i+1
}
```

### Pattern 3: Using Results Framework

```java
Results results = new Results();
results.getDataFromCapillary(
    experiment,
    capillary,
    seriesBin,
    outputBin,
    resultsOptions,
    true  // Subtract T0
);

// Access processed data
ArrayList<Double> values = results.dataValues;
// Values are now relative to T0 and potentially normalized
```

---

## Determining Measurement Type

Before retrieving data, you can check the measurement type:

```java
EnumResults measureType = EnumResults.NBGULPS;

if (measureType.isStoredData()) {
    // Can use simpler method without exp/options
    CapillaryMeasure m = capillary.getMeasurements(measureType);
} else if (measureType.requiresComputation()) {
    // Must provide exp and options
    CapillaryMeasure m = capillary.getMeasurements(measureType, exp, options);
} else if (measureType.isNotImplemented()) {
    // Not yet available - will return null
    // Handle gracefully or skip
}
```

---

## Complete List of Available Measures

See `EnumResults.java` for the complete catalog. All measures include:
- Label (string identifier)
- Unit (measurement unit description)
- Title (human-readable description)
- Computation strategy (accessor or computation method reference)

The enum is organized into sections:
- **Stored Data Measures**: Direct access from measurement fields
- **Computed Gulp Measures**: Derived from gulp detection
- **Computed Correlation Measures**: Statistical analysis measures
- **Fly Position Measures**: Fly tracking data
- **Spot Area Measures**: Spot consumption data

---

## Notes

1. **Always check for null**: Both methods can return `null` if:
   - The measurement type is not implemented
   - Required data is not available
   - Computation fails

2. **Time binning**: Excel export methods automatically handle resampling from source bin duration to output bin duration using linear interpolation.

3. **Computed measures require context**: Always provide `Experiment` and `ResultsOptions` when accessing computed measures. Stored measures can work without these (backward compatibility).

4. **Performance**: Stored measures are faster to access. Computed measures require calculation time proportional to data size and complexity.

5. **Consistency**: The same measurement retrieved via different methods should produce consistent values, just in different formats (Level2D vs ArrayList<Integer> vs ArrayList<Double>).

