# Window Position Persistence Analysis

## Current Situation

The MultiCAFE plugin displays several chart windows (managed by `LevelsChart.java` and `ChartLevels.java`) that are destroyed when browsing between experiments. Currently, window positions are not saved, so users lose their preferred window layouts when switching experiments.

## User Requirement

**Goal**: Give users the impression that windows (chart, source data, kymograph) come from the same source and maintain their positions when switching between experiments. This means:
- **Global window positions** - shared across all experiments (not per-experiment)
- Windows should appear in the same positions when switching experiments
- With 200-500 experiments, storing positions per-experiment would be wasteful and not the desired UX

## Your Proposed Strategy

**Strategy**: Store window positions in `LevelsChart`, follow the `seqKymo` sequence, and save coordinates when the sequence is closed.

## Analysis of Your Strategy

### ✅ **Excellent Points:**
1. **Following seqKymo sequence** - This is correct since charts are tied to the kymograph sequence
2. **Saving in sequenceClosed()** - This is the right lifecycle event
3. **Storing in LevelsChart** - Perfect for global positions! This is where charts are managed and persists across experiment switches

### ⚠️ **Issues to Address:**

1. **Timing Problem**: In `sequenceClosed()`, charts are already being disposed. We need to save positions **before** disposal.

2. **Persistence Across Sessions**: Storing positions only in `LevelsChart` means they're lost when the application restarts. We should optionally persist to a global settings file.

3. **Restoration**: When displaying graphs for a new experiment, we need to restore saved positions if they exist.

4. **Real-time Updates**: Positions should be saved when users manually move/resize windows, not just when sequence closes.

## Recommended Solution

### Architecture Overview

1. **Store positions in LevelsChart** - Add static or instance fields to store global window bounds for each chart type
2. **Save positions before disposal** - Capture window bounds when sequence is about to close
3. **Restore positions on display** - Use saved positions when creating charts for any experiment
4. **Optional persistence** - Save/load window positions to a global settings file (separate from experiment data)

### Implementation Plan

#### Step 1: Add Global Window Position Storage to LevelsChart

Add fields to store global window positions for each chart type:

```java
// In LevelsChart.java - these are global/shared across all experiments
private static Rectangle globalChartTopBottomBounds = null;
private static Rectangle globalChartDeltaBounds = null;
private static Rectangle globalChartDerivativeBounds = null;
private static Rectangle globalChartSumGulpsBounds = null;
```

**Note**: Using `static` makes these shared across all instances. Alternatively, you could store them as instance variables in the `LevelsChart` object that's part of `MultiCAFE`, which persists across experiment switches.

#### Step 2: Save Positions Before Chart Disposal

Modify `LevelsChart.sequenceClosed()` to save positions before closing:

```java
@Override
public void sequenceClosed(Sequence sequence) {
    sequence.removeListener(this);
    
    // Save window positions before closing (global positions, not per-experiment)
    saveChartPositions();
    
    closeAllCharts();
}
```

Add a method to save positions (global, not per-experiment):

```java
private void saveChartPositions() {
    // Save positions globally - these will be reused for all experiments
    if (plotTopAndBottom != null && plotTopAndBottom.mainChartFrame != null) {
        globalChartTopBottomBounds = plotTopAndBottom.mainChartFrame.getBounds();
    }
    if (plotDelta != null && plotDelta.mainChartFrame != null) {
        globalChartDeltaBounds = plotDelta.mainChartFrame.getBounds();
    }
    if (plotDerivative != null && plotDerivative.mainChartFrame != null) {
        globalChartDerivativeBounds = plotDerivative.mainChartFrame.getBounds();
    }
    if (plotSumgulps != null && plotSumgulps.mainChartFrame != null) {
        globalChartSumGulpsBounds = plotSumgulps.mainChartFrame.getBounds();
    }
}
```

#### Step 3: Restore Positions When Displaying Charts

Modify `displayGraphsPanels()` to use saved global positions:

```java
public void displayGraphsPanels(Experiment exp) {
    exp.seqKymos.seq.addListener(this);
    Rectangle rectv = getInitialUpperLeftPosition(exp);
    int dx = 5;
    int dy = 10;

    if (limitsCheckbox.isSelected() && isThereAnyDataToDisplay(exp, EnumXLSExport.TOPLEVEL)
            && isThereAnyDataToDisplay(exp, EnumXLSExport.BOTTOMLEVEL)) {
        // Use saved global position if available, otherwise use initial position
        Rectangle savedPos = globalChartTopBottomBounds;
        Rectangle pos = (savedPos != null) ? savedPos : rectv;
        plotTopAndBottom = plotToChart(exp, "top + bottom levels", EnumXLSExport.TOPLEVEL, 
                                       plotTopAndBottom, pos);
        if (savedPos == null) {
            rectv.translate(dx, dy);
        }
        plotTopAndBottom.toFront();
    } else if (plotTopAndBottom != null)
        closeChart(plotTopAndBottom);
    
    // Similar pattern for other charts:
    if (deltaCheckbox.isSelected() && isThereAnyDataToDisplay(exp, EnumXLSExport.TOPLEVELDELTA)) {
        Rectangle savedPos = globalChartDeltaBounds;
        Rectangle pos = (savedPos != null) ? savedPos : rectv;
        plotDelta = plotToChart(exp, "top delta t -(t-1)", EnumXLSExport.TOPLEVELDELTA, plotDelta, pos);
        if (savedPos == null) {
            rectv.translate(dx, dy);
        }
        plotDelta.toFront();
    } else if (plotDelta != null)
        closeChart(plotDelta);
    
    // ... similar for derivative and sumGulps
}
```

#### Step 4: Save Positions When Experiment is Deselected (Optional but Recommended)

In `LoadSaveExperiment.closeViewsForCurrentExperiment()`, save chart positions before closing:

```java
public void closeViewsForCurrentExperiment(Experiment exp) {
    if (exp != null) {
        // Save global chart window positions before closing sequences
        if (parent0.paneLevels.tabGraphs != null) {
            parent0.paneLevels.tabGraphs.saveChartPositions();
        }
        
        if (exp.seqCamData != null) {
            exp.xmlSave_MCExperiment();
            exp.saveCapillariesMeasures(exp.getKymosBinFullDirectory());
            exp.saveCageAndMeasures();
        }
        exp.closeSequences();
    }
}
```

#### Step 5: Optional - Persist to Global Settings File

If you want positions to persist across application restarts, create a global settings file (separate from experiment data):

**Create a new class or add to LevelsChart:**

```java
// In LevelsChart.java - add methods to save/load global positions
private static final String SETTINGS_FILE = "multicafe_window_positions.xml";

public static void saveGlobalPositionsToFile() {
    Document doc = XMLUtil.createDocument(true);
    if (doc != null) {
        Node root = XMLUtil.getRootElement(doc, true);
        Node node = XMLUtil.setElement(root, "windowPositions");
        
        if (globalChartTopBottomBounds != null) {
            saveRectangleToXML(node, "chartTopBottom", globalChartTopBottomBounds);
        }
        if (globalChartDeltaBounds != null) {
            saveRectangleToXML(node, "chartDelta", globalChartDeltaBounds);
        }
        if (globalChartDerivativeBounds != null) {
            saveRectangleToXML(node, "chartDerivative", globalChartDerivativeBounds);
        }
        if (globalChartSumGulpsBounds != null) {
            saveRectangleToXML(node, "chartSumGulps", globalChartSumGulpsBounds);
        }
        
        // Save to user's home directory or application data directory
        String settingsPath = System.getProperty("user.home") + File.separator + SETTINGS_FILE;
        XMLUtil.saveDocument(doc, settingsPath);
    }
}

public static void loadGlobalPositionsFromFile() {
    String settingsPath = System.getProperty("user.home") + File.separator + SETTINGS_FILE;
    Document doc = XMLUtil.loadDocument(settingsPath);
    if (doc != null) {
        Node root = XMLUtil.getRootElement(doc);
        Node node = XMLUtil.getElement(root, "windowPositions");
        if (node != null) {
            globalChartTopBottomBounds = loadRectangleFromXML(node, "chartTopBottom");
            globalChartDeltaBounds = loadRectangleFromXML(node, "chartDelta");
            globalChartDerivativeBounds = loadRectangleFromXML(node, "chartDerivative");
            globalChartSumGulpsBounds = loadRectangleFromXML(node, "chartSumGulps");
        }
    }
}

private static void saveRectangleToXML(Node parent, String name, Rectangle rect) {
    Node node = XMLUtil.setElement(parent, name);
    XMLUtil.setElementIntValue(node, "x", rect.x);
    XMLUtil.setElementIntValue(node, "y", rect.y);
    XMLUtil.setElementIntValue(node, "width", rect.width);
    XMLUtil.setElementIntValue(node, "height", rect.height);
}

private static Rectangle loadRectangleFromXML(Node parent, String name) {
    Node node = XMLUtil.getElement(parent, name);
    if (node != null) {
        int x = XMLUtil.getElementIntValue(node, "x", 0);
        int y = XMLUtil.getElementIntValue(node, "y", 0);
        int width = XMLUtil.getElementIntValue(node, "width", 800);
        int height = XMLUtil.getElementIntValue(node, "height", 300);
        return new Rectangle(x, y, width, height);
    }
    return null;
}
```

**Call load on initialization and save on shutdown:**

```java
// In LevelsChart.init() or MultiCAFE.run()
LevelsChart.loadGlobalPositionsFromFile();

// In sequenceClosed() or when application closes
LevelsChart.saveGlobalPositionsToFile();
```

#### Step 6: Add Window Listener for Real-time Updates

To save positions when user manually moves/resizes windows, add a window listener to `ChartLevels`. We need to pass a reference back to LevelsChart to update the global positions:

**Modify ChartLevels to accept a callback:**

```java
// In ChartLevels.java - add interface for position updates
public interface PositionUpdateCallback {
    void updatePosition(String chartType, Rectangle bounds);
}

private PositionUpdateCallback positionCallback = null;

public void setPositionUpdateCallback(PositionUpdateCallback callback) {
    this.positionCallback = callback;
}

// In createChartPanel(), after creating mainChartFrame:
mainChartFrame.addComponentListener(new ComponentAdapter() {
    @Override
    public void componentMoved(ComponentEvent e) {
        saveCurrentPosition();
    }
    
    @Override
    public void componentResized(ComponentEvent e) {
        saveCurrentPosition();
    }
    
    private void saveCurrentPosition() {
        if (mainChartFrame != null && positionCallback != null) {
            Rectangle bounds = mainChartFrame.getBounds();
            positionCallback.updatePosition(title, bounds);
        }
    }
});
```

**In LevelsChart.plotToChart(), set the callback:**

```java
private ChartLevels plotToChart(Experiment exp, String title, EnumXLSExport option, ChartLevels iChart,
        Rectangle rectv) {
    if (iChart != null)
        iChart.mainChartFrame.dispose();
    iChart = new ChartLevels();
    iChart.setPositionUpdateCallback(new ChartLevels.PositionUpdateCallback() {
        @Override
        public void updatePosition(String chartType, Rectangle bounds) {
            // Update global position based on chart type
            if (chartType.equals("top + bottom levels")) {
                globalChartTopBottomBounds = bounds;
            } else if (chartType.equals("top delta t -(t-1)")) {
                globalChartDeltaBounds = bounds;
            } else if (chartType.equals("Derivative")) {
                globalChartDerivativeBounds = bounds;
            } else if (chartType.equals("Cumulated gulps")) {
                globalChartSumGulpsBounds = bounds;
            }
        }
    });
    iChart.createChartPanel(parent0, title, rectv);
    iChart.displayData(exp, option, title, correctEvaporationCheckbox.isSelected());
    iChart.mainChartFrame.toFront();
    iChart.mainChartFrame.requestFocus();
    return iChart;
}
```

## Alternative: Simpler Initial Implementation

If you want to start simple without file persistence:

1. Store positions only in memory (static fields in LevelsChart)
2. Save positions in `sequenceClosed()` before disposal
3. Restore positions when displaying charts for any experiment
4. Add file persistence later if needed

This gives you the core functionality (windows maintain positions across experiment switches) without the complexity of file I/O.

## Summary

Your strategy is **excellent** for global window positions! Here's what to implement:

1. ✅ Store positions in **LevelsChart** as static/global fields (shared across all experiments)
2. ✅ Save positions **before** charts are disposed in `sequenceClosed()`
3. ✅ Restore positions when displaying charts for any experiment
4. ✅ Optionally add file persistence for cross-session persistence
5. ✅ Consider saving positions when experiment is deselected (not just when sequence closes)
6. ✅ Add real-time position updates when users move/resize windows

**Key Insight**: Since you want windows to appear as if they're the same windows across experiments, storing positions globally in `LevelsChart` is the perfect approach. This is much simpler than per-experiment storage and matches your UX goal perfectly!

