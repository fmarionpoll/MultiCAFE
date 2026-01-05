# Capillary Measures Erasure Analysis

## Problem Statement

Kymograph measures (capillary measures) are being erased when:
1. Fly detection is performed (using `flydetect1.java` or `flydetect2.java`)
2. Background detection is performed (using `Detect2Background` which calls `BuildBackground`)

## Initial Hypothesis and Failed Fixes

### First Attempt
**Hypothesis**: Capillaries weren't being loaded before save operations, causing empty lists to overwrite existing data.

**Fix Applied**: Added `exp.loadMCCapillaries_Only()` to `FlyDetect.analyzeExperiment()`.

**Result**: Did not resolve the issue.

### Second Attempt
**Hypothesis**: More complete data loading was needed, and the same issue occurred with background detection.

**Fixes Applied**:
1. Modified `Experiment.saveCapillariesMeasures()` to load capillaries if list is empty
2. Modified `FlyDetect.loadDrosoTrack2()` to load capillaries during fly detection
3. Modified `BuildBackground.loadExperimentData()` to load capillaries during background building

**Result**: Still did not resolve the issue.

## Deep Analysis: Root Cause Investigation

### Critical Discovery: `saveCapillariesMeasures()` Logic Flow

The `saveCapillariesMeasures()` method has a critical flaw in its logic:

```java
public boolean saveCapillariesMeasures(String directory) {
    // Load capillaries if empty (our fix)
    if (capillaries.getList().isEmpty()) {
        loadMCCapillaries_Only();
        if (getKymosBinFullDirectory() != null) {
            capillaries.load_Capillaries(getKymosBinFullDirectory());
        }
    }
    
    boolean flag = false;
    // CRITICAL: This condition can skip ROI transfer
    if (seqKymos != null && seqKymos.getSequence() != null) {
        seqKymos.validateROIs();
        seqKymos.transferKymosRoisToCapillaries_Measures(capillaries);
    }
    // ALWAYS executes, even if seqKymos is null or has no sequence
    flag = capillaries.save_Capillaries(directory);
    return flag;
}
```

### The Problem

1. **Conditional ROI Transfer**: The ROI transfer from `seqKymos` to `capillaries` only happens if:
   - `seqKymos != null` AND
   - `seqKymos.getSequence() != null`

2. **Unconditional Save**: The save operation (`capillaries.save_Capillaries()`) **always executes**, regardless of whether:
   - `seqKymos` is null
   - `seqKymos.getSequence()` is null
   - ROI transfer was skipped
   - Capillaries were successfully loaded

3. **State During Fly Detection/Background Building**:
   - `seqKymos` may be `null` (not initialized)
   - `seqKymos.getSequence()` may be `null` (sequence not attached)
   - `seqKymos.getSequence()` may exist but have no ROIs (empty sequence)

### `transferKymosRoisToCapillaries_Measures()` Behavior

```java
public boolean transferKymosRoisToCapillaries_Measures(Capillaries capillaries) {
    List<ROI> allRois = getSequence().getROIs();
    if (allRois.size() < 1)
        return false;  // Early return if no ROIs
    
    // ... transfers ROIs to capillary measures ...
    return true;
}
```

**Critical Issue**: If the sequence has no ROIs, this method returns `false` early. However, if it does execute and finds matching capillaries, it calls `cap.transferROIsToMeasures(roisAtT)`, which may **overwrite** existing loaded data with empty measures.

### `getSeqKymos()` Lazy Initialization

```java
public SequenceKymos getSeqKymos() {
    if (seqKymos == null)
        seqKymos = new SequenceKymos();  // Creates empty object
    return seqKymos;
}
```

**Problem**: This lazy initialization creates a new empty `SequenceKymos` object with:
- No sequence attached (`getSequence()` returns `null`)
- No ROIs
- No data

If `saveCapillariesMeasures()` is called after this lazy initialization, `seqKymos.getSequence()` will be `null`, causing the ROI transfer to be skipped.

## Key Questions to Investigate

### 1. When is `saveCapillariesMeasures()` Called During Fly Detection?

**Investigation Needed**:
- Is `closeViewsForCurrentExperiment()` being triggered?
- Are there other save paths being invoked?
- Is there an automatic save mechanism?

**Location**: `LoadSaveExperiment.closeViewsForCurrentExperiment()` calls:
```java
exp.xmlSave_MCExperiment();
exp.saveCapillariesMeasures(exp.getKymosBinFullDirectory());
```

### 2. What is the State of `seqKymos` During Fly Detection?

**Investigation Needed**:
- Is `seqKymos` `null`?
- Does it have a sequence attached?
- Does the sequence have ROIs?
- Is the sequence empty but not null?

**Check**: Add logging to verify the state of `seqKymos` and its sequence during fly detection operations.

### 3. What Happens When `transferKymosRoisToCapillaries_Measures()` is Skipped?

**Investigation Needed**:
- Does `capillaries.save_Capillaries()` preserve existing loaded data?
- Or does it save an empty list if no transfer occurred?
- Does `cap.transferROIsToMeasures()` clear existing measures before transferring?

**Critical**: Need to understand if the save operation itself is destructive when no ROI transfer occurs.

### 4. Is There a Race Condition?

**Investigation Needed**:
- Could a save happen before capillaries are fully loaded?
- Could multiple saves happen concurrently?
- Is there a timing issue between loading and saving?

**Note**: The `Experiment` class has `isLoading` and `isSaving` flags, but we need to verify they're being used correctly.

### 5. Does `capillaries.save_Capillaries()` Save Empty Data?

**Investigation Needed**:
- Does `CapillariesPersistence.save_Capillaries()` save an empty CSV if the list is empty?
- Or does it skip saving?
- What happens to `MCcapillaries.xml` (descriptors) - is it saved separately?

**Location**: `src/main/java/plugins/fmp/multicafe/fmp_experiment/capillaries/CapillariesPersistence.java`

### 6. Could `seqKymos` and `seqCamData` Share the Same Sequence?

**Investigation Needed**:
- Are `seqKymos.getSequence()` and `seqCamData.getSequence()` the same object?
- Could operations on `seqCamData` affect `seqKymos`?
- Is there sequence sharing that could cause interference?

**Note**: `SequenceKymos extends SequenceCamData`, but they should have separate sequence instances.

## Potential Root Causes

### Hypothesis 1: Save Executes with Empty State
- `seqKymos` is null or has no sequence during fly detection
- ROI transfer is skipped
- Capillaries are loaded, but save operation somehow overwrites with empty data
- **Need to verify**: Does the save operation check if data exists before writing?

### Hypothesis 2: ROI Transfer Overwrites Loaded Data
- Capillaries are loaded from CSV/XML
- `seqKymos.getSequence()` exists but is empty (no ROIs)
- `transferKymosRoisToCapillaries_Measures()` is called but finds no ROIs
- The transfer process somehow clears existing loaded data
- **Need to verify**: Does `cap.transferROIsToMeasures()` clear existing measures?

### Hypothesis 3: Sequence State Corruption
- `seqKymos` sequence is replaced or cleared during fly detection
- Existing ROIs are lost
- When save occurs, it transfers an empty sequence to capillaries
- **Need to verify**: Does `setSequence()` or `attachSequence()` clear existing ROIs?

### Hypothesis 4: Multiple Save Operations
- One save operation loads capillaries correctly
- Another save operation runs with empty `seqKymos` state
- Second save overwrites the first save's data
- **Need to verify**: Are there multiple save calls happening?

## Recommended Investigation Steps

1. **Add Logging**:
   - Log the state of `seqKymos` and `seqKymos.getSequence()` before save
   - Log whether ROI transfer occurs
   - Log the size of `capillaries.getList()` before and after transfer
   - Log when `saveCapillariesMeasures()` is called and from where

2. **Verify Save Behavior**:
   - Check if `capillaries.save_Capillaries()` saves empty data or skips saving
   - Verify if `MCcapillaries.xml` is saved separately and could be overwritten
   - Check if there are multiple save operations happening

3. **Check Transfer Logic**:
   - Verify if `cap.transferROIsToMeasures()` clears existing data
   - Check if `transferKymosRoisToCapillaries_Measures()` has side effects when sequence is empty

4. **Verify Sequence State**:
   - Check if `seqKymos` sequence is being replaced or cleared
   - Verify if `seqKymos` and `seqCamData` share sequence instances
   - Check if sequence operations on `seqCamData` affect `seqKymos`

## Conclusion

The fix attempts failed because they only addressed **loading** capillaries, but did not address the **save logic** when `seqKymos` is unavailable. The save operation should either:

1. **Skip saving** if `seqKymos` is unavailable AND capillaries weren't loaded, OR
2. **Preserve existing loaded data** when sequence transfer is skipped

The current implementation has a dangerous pattern where:
- ROI transfer is conditional (may be skipped)
- Save operation is unconditional (always executes)
- This can lead to saving empty or corrupted data

## Next Steps

1. Add comprehensive logging to understand the exact state during save operations
2. Investigate the save behavior when ROI transfer is skipped
3. Determine if the save operation should be conditional or if it should preserve existing data
4. Consider adding a check to prevent saving when no valid data exists



