# Create New Excel Export System for Capillaries, Gulps, and Fly Positions

## Overview

Create new Excel export implementations that follow the same clean pattern as spot exports, with three implementations (original, optimized, streaming) for each data type. Use a unified factory for maintainability and user-friendliness.

## Architecture

### 1. Export Type Enum

Create `ExportType.java` enum to identify export categories:

- `SPOT` - Spot measurements (already exists)
- `CAPILLARY` - Capillary measurements (topraw, toplevel, toplevel_L+R, derivative)
- `GULP` - Gulp measurements (sumGulps, nbGulps, amplitudeGulps, etc.)
- `FLY_POSITION` - Fly position measurements (XYIMAGE, XYTOPCAGE, XYTIPCAPS, etc.)
- `MARKOV_CHAIN` - Markov chain analysis (new)

### 2. Capillary Export Implementations

#### Files to Create:

- `XLSExportMeasuresFromCapillary.java` (original)
- `XLSExportMeasuresFromCapillaryOptimized.java` (optimized)
- `XLSExportMeasuresFromCapillaryStreaming.java` (streaming)

#### Key Methods to Implement:

- `exportExperimentData()` - Main export method (replaces `processExperiment`)
- `getCapillaryDataAndExport()` - Get data and export for specific EnumXLSExport type
- `writeCapillaryDataDirectly()` - Direct write for optimized/streaming
- `getCapillaryDataIterator()` - Streaming iterator for capillary data
- `writeExperimentCapillaryInfos()` - Write capillary descriptors (similar to `writeExperimentSpotInfos`)

#### Data Access:

- Use `Capillary.getMeasuresForExcel()` or similar method (need to check if exists)
- If not exists, adapt `XLSResultsFromCapillaries.getResults1()` and `getResults_T0()` logic
- Handle chained experiments properly
- Support evaporation compensation

### 3. Gulp Export Implementations

#### Files to Create:

- `XLSExportMeasuresFromGulp.java` (original)
- `XLSExportMeasuresFromGulpOptimized.java` (optimized)
- `XLSExportMeasuresFromGulpStreaming.java` (streaming)

#### Key Methods:

- Extend capillary exports or create separate implementations
- Handle gulp-specific measures: SUMGULPS, NBGULPS, AMPLITUDEGULPS, TTOGULP, etc.
- Support markov chain export (MARKOV_CHAIN enum)
- Support autocorrelation and cross-correlation

### 4. Fly Position Export Implementations

#### Files to Create:

- `XLSExportMeasuresFromFlyPosition.java` (original)
- `XLSExportMeasuresFromFlyPositionOptimized.java` (optimized)
- `XLSExportMeasuresFromFlyPositionStreaming.java` (streaming)

#### Key Methods:

- `exportExperimentData()` - Export fly position data
- `writeFlyPositionDataDirectly()` - Direct write for optimized/streaming
- `getFlyPositionDataIterator()` - Streaming iterator
- Handle measures: XYIMAGE, XYTOPCAGE, XYTIPCAPS, ELLIPSEAXES, DISTANCE, ISALIVE, SLEEP
- Use `FlyPosition` class methods directly (getX(), getY(), getDistance(), etc.)

### 5. Unified Factory Enhancement

#### File to Modify:

- `XLSExportFactory.java`

#### Changes:

- Add `ExportType` parameter to factory methods
- Create methods: `createExporter(ExportType, int experimentCount, XLSExportOptions)`
- Add convenience methods: `createCapillaryExporter()`, `createGulpExporter()`, `createFlyPositionExporter()`
- Update memory estimation to account for different data types
- Add factory methods for markov chain export

#### Example Usage:

```java
// Unified approach
XLSExport exporter = XLSExportFactory.createExporter(
    ExportType.CAPILLARY, 
    experimentCount, 
    options
);

// Convenience methods
XLSExport exporter = XLSExportFactory.createCapillaryExporter(experimentCount, options);
```

### 6. Data Access Layer

#### Files to Create/Modify:

- Create helper methods in `Capillary` class or create `CapillaryDataExtractor.java`
- Create helper methods for `FlyPosition` or create `FlyPositionDataExtractor.java`
- Adapt existing `XLSResultsFromCapillaries` logic to work with new pattern

#### Key Requirements:

- Extract data without creating full `XLSResultsArray` objects (for optimized/streaming)
- Support streaming data access
- Handle time binning and scaling
- Support relative to T0 calculations

### 7. Common Infrastructure

#### Files to Modify:

- `XLSExport.java` - Ensure base class supports all export types
- Add helper methods: `writeExperimentCapillaryInfos()`, `writeExperimentFlyPositionInfos()`
- Ensure `prepareExperiments()` handles all data types correctly

## Implementation Steps

1. **Create ExportType enum** - Simple enum for type identification
2. **Create capillary export base class** - Original implementation following spot pattern
3. **Create optimized capillary export** - Memory-efficient version
4. **Create streaming capillary export** - Ultra-efficient version
5. **Create gulp export classes** - Extend or adapt capillary exports
6. **Create fly position export classes** - New implementations
7. **Enhance factory** - Add unified factory methods
8. **Add markov chain support** - Implement markov chain export logic
9. **Test and validate** - Ensure all exports work correctly

## Key Design Decisions

1. **Consistency**: All exports follow the same pattern as spot exports
2. **Factory Pattern**: Unified factory for easy usage and maintenance
3. **Three Implementations**: Original, optimized, streaming for each type
4. **Data Access**: Direct data extraction without intermediate objects (for optimized/streaming)
5. **Backward Compatibility**: New implementations don't break existing code
6. **Extensibility**: Easy to add new export types in the future

## Files Structure

```
tools1/toExcel/
├── ExportType.java (NEW)
├── XLSExport.java (MODIFY - add helper methods)
├── XLSExportFactory.java (MODIFY - add unified methods)
├── XLSExportMeasuresFromCapillary.java (NEW)
├── XLSExportMeasuresFromCapillaryOptimized.java (NEW)
├── XLSExportMeasuresFromCapillaryStreaming.java (NEW)
├── XLSExportMeasuresFromGulp.java (NEW)
├── XLSExportMeasuresFromGulpOptimized.java (NEW)
├── XLSExportMeasuresFromGulpStreaming.java (NEW)
├── XLSExportMeasuresFromFlyPosition.java (NEW)
├── XLSExportMeasuresFromFlyPositionOptimized.java (NEW)
└── XLSExportMeasuresFromFlyPositionStreaming.java (NEW)
```

## Migration Notes

- New implementations are separate from existing `XLSExportCapillariesResults` and `XLSExportGulpsResults`
- Old implementations can coexist during transition
- New implementations use `exportExperimentData()` instead of `processExperiment()`
- New implementations use `SXSSFSheet` consistently (not `XSSFSheet`)

## Current Status

### Completed:
1. ✅ ExportType enum created
2. 🔄 XLSExportMeasuresFromCapillary.java (original) - in progress, has compilation errors due to package visibility issues

### Remaining Issues:
- Package visibility: `resultsList` field in `XLSResultsArray` is package-private, making it difficult to access from `XLSResultsFromCapillaries` (tools0) in new classes (tools1)
- Need to resolve: Either make field protected/public, add getter methods, or restructure package organization

### Next Steps:
1. Fix compilation errors in capillary export
2. Create optimized and streaming versions
3. Create gulp export classes
4. Create fly position export classes
5. Enhance factory with unified methods
6. Add markov chain support

