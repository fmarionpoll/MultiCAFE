# Generic ChartCageArrayFrame Refactoring Plan

## Current Situation

`ChartCageArrayFrame` is currently in `dlg.levels` and is tightly coupled to:
- Capillary data (uses `CageCapillarySeriesBuilder`)
- Levels dialog context
- Specific interaction handlers

However, similar chart display needs exist for:
1. **Fly positions** (`ChartPositions`) - horizontal layout (uses `CageFlyPositionSeriesBuilder`)
2. **Spot measurements** - could use same grid layout
3. **Capillary levels** (current) - grid layout

## Key Abstractions Already in Place

✅ **Data Building**: `CageSeriesBuilder` interface already exists
- `CageCapillarySeriesBuilder` - for capillary data
- `CageSpotSeriesBuilder` - for spot data
- Could add `CageFlyPositionSeriesBuilder` - for fly position data

## Proposed Generic Structure

### 1. Base Generic Class: `CageChartArrayFrame`

**Location**: `fmp_tools.chart.CageChartArrayFrame`

**Responsibilities**:
- Frame/panel management
- Grid layout management
- Chart panel creation from `XYSeriesCollection`
- Axis configuration
- Common chart display logic
- Preferences management

**Generic Parameters/Configuration**:
- `CageSeriesBuilder` - for data building (already abstracted!)
- `ChartInteractionHandler` - for mouse interactions (optional)
- Layout strategy (Grid vs Horizontal)
- UI controls factory (Combobox vs Checkboxes vs None)

### 2. Specialized Subclasses

**Option A: Inheritance-based**
```java
// Base class
public abstract class CageChartArrayFrame extends IcyFrame {
    protected CageSeriesBuilder dataBuilder;
    protected ChartInteractionHandler interactionHandler;
    // ... common logic
}

// Specialized classes
public class CapillaryChartArrayFrame extends CageChartArrayFrame {
    // Uses CageCapillarySeriesBuilder
    // Uses CapillaryChartInteractionHandler
}

public class SpotChartArrayFrame extends CageChartArrayFrame {
    // Uses CageSpotSeriesBuilder  
    // Uses SpotChartInteractionHandler
}

public class FlyPositionChartArrayFrame extends CageChartArrayFrame {
    // Uses CageFlyPositionSeriesBuilder
    // No interaction handler (or simple one)
    // Horizontal layout instead of grid
}
```

**Option B: Composition-based (Preferred)**
```java
// Generic class with strategy pattern
public class CageChartArrayFrame extends IcyFrame {
    private final CageSeriesBuilder dataBuilder;
    private final ChartInteractionHandler interactionHandler;
    private final ChartLayoutStrategy layoutStrategy;
    private final ChartUIControlsFactory uiControlsFactory;
    
    public CageChartArrayFrame(
        CageSeriesBuilder dataBuilder,
        ChartInteractionHandler interactionHandler,
        ChartLayoutStrategy layoutStrategy,
        ChartUIControlsFactory uiControlsFactory
    ) {
        // ...
    }
}

// Usage:
new CageChartArrayFrame(
    new CageCapillarySeriesBuilder(),
    new CapillaryChartInteractionHandler(...),
    new GridLayoutStrategy(...),
    new ComboBoxUIControlsFactory(...)
);
```

### 3. New Interfaces/Classes Needed

#### `ChartLayoutStrategy`
```java
public interface ChartLayoutStrategy {
    void arrangePanels(JPanel mainPanel, ChartCagePair[][] chartArray, 
                       int nPanelsX, int nPanelsY);
    LayoutManager createLayout(int nPanelsX, int nPanelsY);
}
```

Implementations:
- `GridLayoutStrategy` - for cage grids
- `HorizontalLayoutStrategy` - for fly positions (one chart per cage in a row)

#### `ChartUIControlsFactory`
```java
public interface ChartUIControlsFactory {
    JComponent createResultTypeSelector(EnumResults[] types, 
                                       EnumResults selected,
                                       ActionListener listener);
    JPanel createTopPanel();
    JPanel createBottomPanel();
}
```

Implementations:
- `ComboBoxUIControlsFactory` - for levels dialog (combobox + legend)
- `CheckboxUIControlsFactory` - for cages dialog (multiple checkboxes)
- `NoUIControlsFactory` - for simple displays

#### `CageFlyPositionSeriesBuilder` (new)
```java
public class CageFlyPositionSeriesBuilder implements CageSeriesBuilder {
    @Override
    public XYSeriesCollection build(Experiment exp, Cage cage, ResultsOptions options) {
        // Build from FlyPositions data
    }
}
```

## Migration Strategy

### Phase 1: Extract Generic Base
1. Create `CageChartArrayFrame` in `fmp_tools.chart`
2. Move common logic from current `ChartCageArrayFrame`
3. Keep current `ChartCageArrayFrame` as a thin wrapper that delegates

### Phase 2: Implement Strategies
1. Create `ChartLayoutStrategy` interface and implementations
2. Create `ChartUIControlsFactory` interface and implementations
3. Refactor `CageChartArrayFrame` to use strategies

### Phase 3: Migrate Existing Code ✅ COMPLETE
1. ✅ Update `dlg.levels.ChartCageArrayFrame` to use generic base
2. ✅ Create `CageFlyPositionSeriesBuilder`
3. ✅ Migrate `ChartPositions` to use builder (partial - uses builder, maintains API)
4. ✅ Create spot chart display using `CageChartArrayFrame` (automatic builder selection)

### Phase 4: Cleanup ✅ COMPLETE
1. ✅ Remove old `ChartFlyPositions` class (dead code - ~491 lines removed)
2. ✅ Verify `ChartPositions` uses builder pattern (already migrated in Phase 3)
3. ✅ Update documentation

**Note**: `ChartPositions` is kept for backward compatibility with `ChartPositionsPanel`. 
It uses `CageFlyPositionSeriesBuilder` for data building. Full migration to the generic 
framework would require extending `CageChartArrayFrame` to support `List<Cage>` mode.

## Benefits

1. **Code Reuse**: Single implementation for all cage-based chart displays
2. **Consistency**: Same look/feel across all chart types
3. **Maintainability**: Fix bugs once, benefit everywhere
4. **Extensibility**: Easy to add new chart types (just implement `CageSeriesBuilder`)
5. **Testability**: Strategies can be tested independently

## Package Organization

```
fmp_tools.chart/
  ├── CageChartArrayFrame.java          (generic base)
  ├── strategies/
  │   ├── ChartLayoutStrategy.java      (interface)
  │   ├── GridLayoutStrategy.java
  │   └── HorizontalLayoutStrategy.java
  │   ├── ChartUIControlsFactory.java   (interface)
  │   ├── ComboBoxUIControlsFactory.java
  │   ├── CheckboxUIControlsFactory.java
  │   └── NoUIControlsFactory.java
  ├── builders/
  │   ├── CageSeriesBuilder.java        (existing)
  │   ├── CageCapillarySeriesBuilder.java (existing)
  │   ├── CageSpotSeriesBuilder.java     (existing)
  │   └── CageFlyPositionSeriesBuilder.java (new)
  └── ...

dlg.levels/
  └── ChartCageArrayFrame.java          (thin wrapper/delegate)

dlg.cages/
  └── (uses CageChartArrayFrame directly)
```

## Considerations

1. **Backward Compatibility**: Keep existing `ChartCageArrayFrame` API during migration
2. **Interaction Handlers**: May need to be more generic or stay dialog-specific
3. **Layout Differences**: Fly positions use horizontal, others use grid - strategy handles this
4. **UI Controls**: Different dialogs need different controls - factory pattern handles this


