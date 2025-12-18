# Chart Interaction Handlers Implementation

## Overview

This document describes the refactoring of `ChartCageArrayFrame` to use a handler-based architecture for chart interactions. This design separates chart display logic from interaction logic, making the code more maintainable, testable, and extensible for future measurement types.

## Architecture

The implementation uses a handler-based architecture:

- **Base Interface**: `ChartInteractionHandler` - defines contract for chart click handling
- **SpotChartInteractionHandler**: Handles all spot-related chart interactions (extracted from original implementation)
- **CapillaryChartInteractionHandler**: Handles all capillary-related chart interactions (new implementation)
- **ChartCageArrayFrame**: Delegates to appropriate handler based on data type

### Benefits

1. **Separation of Concerns**: Chart display logic separate from interaction logic
2. **Extensibility**: Easy to add new measurement types (e.g., `FlyPositionChartInteractionHandler`)
3. **Testability**: Handlers can be tested independently
4. **Maintainability**: Changes to spot/capillary interactions don't affect each other
5. **Code Reuse**: Shared logic can be in base class or utility methods

## Implementation Plan

### Phase 1: Create Base Interface
- Create `ChartInteractionHandler` interface defining the contract

### Phase 2: Extract Spot Handler
- Move all spot-related methods from `ChartCageArrayFrame` to `SpotChartInteractionHandler`
- Implement `ChartInteractionHandler` interface
- Create `SpotChartMouseListener` inner class

### Phase 3: Implement Capillary Handler
- Create `CapillaryChartInteractionHandler` with all capillary interaction methods
- Handle series key parsing ("0_L", "0_R", "0_Sum", "0_PI")
- Implement closest curve finding for empty space clicks
- Create `CapillaryChartMouseListener` inner class

### Phase 4: Refactor ChartCageArrayFrame
- Add `interactionHandler` field
- Remove spot-specific methods
- Add handler selection logic
- Update `createChartPanelForCage()` to use handler

## Files Created

### 1. `ChartInteractionHandler.java`
**Location**: `src/main/java/plugins/fmp/multicafe/dlg/levels/ChartInteractionHandler.java`

Interface defining the contract for chart interaction handlers:
```java
public interface ChartInteractionHandler {
    ChartMouseListener createMouseListener();
}
```

### 2. `SpotChartInteractionHandler.java`
**Location**: `src/main/java/plugins/fmp/multicafe/dlg/levels/SpotChartInteractionHandler.java`

Extracted all spot-related methods from `ChartCageArrayFrame`:
- `getSpotFromClickedChart(ChartMouseEvent e)`
- `getSpotFromXYItemEntity(XYItemEntity xyItemEntity)`
- `chartSelectSpot(Experiment exp, Spot spot)`
- `selectT(Experiment exp, ResultsOptions resultsOptions, Spot spot)`
- `chartSelectKymograph(Experiment exp, Spot spot)`
- `chartSelectClickedSpot(Experiment exp, ResultsOptions resultsOptions, Spot clickedSpot)`
- `SpotChartMouseListener` inner class

### 3. `CapillaryChartInteractionHandler.java`
**Location**: `src/main/java/plugins/fmp/multicafe/dlg/levels/CapillaryChartInteractionHandler.java`

New handler implementing all capillary interaction methods:
- `getCapillaryFromClickedChart(ChartMouseEvent e)` - Main entry point for capillary selection
- `getCapillaryFromXYItemEntity(XYItemEntity xyItemEntity, Cage cage)` - Parse series keys from XYItemEntity
- `getCapillaryFromSeriesKey(String seriesKey, Cage cage)` - Map series keys to Capillary objects
  - Handles individual capillaries: "cageID_L", "cageID_R"
  - Handles LR types: "cageID_Sum", "cageID_PI"
- `findClosestCapillaryFromPoint(ChartMouseEvent e, Cage cage, XYPlot plot, ChartPanel panel)` - Find closest curve when clicking empty space
- `chartSelectCapillary(Experiment exp, Capillary capillary)` - Select capillary ROI in sequence
- `selectTForCapillary(Experiment exp, ResultsOptions resultsOptions, Capillary capillary, double timeMinutes)` - Select time/frame based on clicked X coordinate
- `chartSelectKymographForCapillary(Experiment exp, Capillary capillary)` - Select kymograph image
- `chartSelectClickedCapillary(...)` - Orchestrate all selection actions
- `getTimeMinutesFromEvent(ChartMouseEvent e, ChartPanel panel, XYPlot plot)` - Extract time from click coordinates
- `CapillaryChartMouseListener` inner class

## Files Modified

### `ChartCageArrayFrame.java`
**Location**: `src/main/java/plugins/fmp/multicafe/dlg/levels/ChartCageArrayFrame.java`

#### Changes Made:

1. **Added handler field**:
   ```java
   private ChartInteractionHandler interactionHandler = null;
   ```

2. **Removed spot-specific methods** (moved to `SpotChartInteractionHandler`):
   - `getSpotFromClickedChart()`
   - `getSpotFromXYItemEntity()`
   - `chartSelectSpot()`
   - `selectT()`
   - `chartSelectKymograph()`
   - `chartSelectClickedSpot()`
   - `SpotChartMouseListener` inner class

3. **Added handler selection logic**:
   - `createInteractionHandler(Experiment exp, ResultsOptions resultsOptions)` - Creates appropriate handler based on result type
   - `isSpotResultType(EnumResults resultType)` - Determines if result type is for spots (AREA_SUM, AREA_SUMCLEAN, etc.)

4. **Updated `displayData()` method**:
   - Initializes `interactionHandler` before creating chart panels

5. **Updated `createChartPanelForCage()` method**:
   - Changed from: `chartCagePanel.addChartMouseListener(new SpotChartMouseListener(...))`
   - To: `chartCagePanel.addChartMouseListener(interactionHandler.createMouseListener())`

6. **Removed unused imports**:
   - Removed imports related to spot handling that are no longer needed

## Key Features Implemented

### Capillary Chart Interactions

#### Clicking on Curves
When a user clicks directly on a capillary curve:
1. **Series Key Parsing**: Extracts capillary from series key (e.g., "0_L", "0_R", "0_Sum", "0_PI")
2. **Capillary Selection**: Selects the corresponding capillary ROI in the sequence viewer
3. **Time Selection**: Extracts X coordinate (time in minutes) and selects corresponding frame in seqCamData
4. **Kymograph Selection**: Selects the corresponding kymograph image using `capillary.kymographIndex`

#### Clicking on Empty Space
When a user clicks on the chart but not on a curve:
1. **Closest Curve Finding**: Calculates distance from clicked point to all curves in the chart
2. **Minimum Distance**: Finds the capillary curve with minimum distance
3. **Selection**: Performs same actions as clicking directly on curve (capillary, time, kymograph selection)

### Series Key Handling

The implementation handles different series key formats:

- **Individual Capillaries**: `"cageID_L"`, `"cageID_R"` → Maps to specific L or R capillary
- **LR Types**: `"cageID_Sum"`, `"cageID_PI"` → Maps to L capillary for Sum, R capillary for PI (or first available)

### Time Selection

Time selection extracts the X coordinate from the clicked point:
1. Converts screen coordinates to chart coordinates using `java2DToValue()`
2. Converts time in minutes to frame index using `TimeManager.findNearestIntervalWithBinarySearch()`
3. Sets viewer position to the calculated frame

### Kymograph Selection

Kymograph selection uses:
1. `capillary.kymographIndex` if available
2. Otherwise, finds capillary index in the capillaries list
3. Sets kymograph sequence viewer position

## Handler Selection Logic

The handler is selected based on `ResultsOptions.resultType`:

- **Spot Types**: `AREA_SUM`, `AREA_SUMCLEAN`, `AREA_OUT`, `AREA_DIFF`, `AREA_FLYPRESENT`
  → Uses `SpotChartInteractionHandler`
- **All Other Types**: Defaults to `CapillaryChartInteractionHandler`
  - Includes: `TOPRAW`, `TOPLEVEL`, `BOTTOMLEVEL`, `DERIVEDVALUES`, `TOPLEVEL_LR`, etc.

## Testing Considerations

When testing the implementation, verify:

1. **Direct Curve Clicks**:
   - Click on L curve → selects L capillary
   - Click on R curve → selects R capillary
   - Click on Sum curve → selects appropriate capillary
   - Click on PI curve → selects appropriate capillary

2. **Empty Space Clicks**:
   - Click near L curve → selects closest capillary (should be L)
   - Click near R curve → selects closest capillary (should be R)
   - Click between curves → selects closest one

3. **Time Selection**:
   - Verify clicked X coordinate matches selected frame
   - Verify time conversion is accurate

4. **Capillary Selection**:
   - Verify correct capillary ROI is selected
   - Verify display centers on capillary

5. **Kymograph Selection**:
   - Verify correct kymograph image is displayed
   - Verify kymograph index is correct

6. **Different Result Types**:
   - Test with individual capillary types
   - Test with LR types (Sum/PI)
   - Test with spot types (should use spot handler)

## Future Extensions

To add support for new measurement types (e.g., fly positions):

1. Create new handler class implementing `ChartInteractionHandler`:
   ```java
   public class FlyPositionChartInteractionHandler implements ChartInteractionHandler {
       // Implement interaction methods
   }
   ```

2. Add handler selection logic in `ChartCageArrayFrame.createInteractionHandler()`:
   ```java
   if (isFlyPositionResultType(resultsOptions.resultType)) {
       return new FlyPositionChartInteractionHandler(...);
   }
   ```

3. No changes needed to existing handlers - they remain independent

## Implementation Status

✅ **All 11 to-do items completed:**

1. ✅ Create ChartInteractionHandler interface
2. ✅ Create SpotChartInteractionHandler class
3. ✅ Create CapillaryChartInteractionHandler class
4. ✅ Implement getCapillaryFromXYItemEntity
5. ✅ Implement getCapillaryFromSeriesKey
6. ✅ Implement findClosestCapillaryFromPoint
7. ✅ Implement chart selection methods (chartSelectCapillary, selectTForCapillary, chartSelectKymographForCapillary)
8. ✅ Implement getCapillaryFromClickedChart and chartSelectClickedCapillary
9. ✅ Refactor ChartCageArrayFrame to remove spot-specific methods
10. ✅ Add handler selection logic
11. ✅ Update createChartPanelForCage to use handler

## Code Quality

- ✅ All linter errors resolved
- ✅ Proper error handling and logging
- ✅ Code follows project conventions
- ✅ Methods are well-documented with JavaDoc
- ✅ Separation of concerns maintained

## Summary

The handler-based architecture successfully separates chart interaction logic from chart display logic. The implementation provides:

- **Clean separation**: Each measurement type has its own handler
- **Extensibility**: Easy to add new measurement types
- **Maintainability**: Changes to one handler don't affect others
- **Functionality**: Full capillary chart interaction support (curve clicks, empty space clicks, time selection, kymograph selection)

The code is ready for use and testing.

