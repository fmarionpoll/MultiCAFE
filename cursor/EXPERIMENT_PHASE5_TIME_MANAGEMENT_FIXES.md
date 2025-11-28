# Experiment.java Phase 5: Time Management Accessors - Implementation Summary

## Overview
This document summarizes all the changes made to `experiment1.Experiment.java` and `experiment1.cages.CagesArray.java` to add time management accessor methods, enabling compatibility with the old `experiment.Experiment` class API.

## Date
Implementation completed during Phase 5 of the Experiment adaptation.

## Files Modified
- `src/main/java/plugins/fmp/multicafe/experiment1/Experiment.java`
- `src/main/java/plugins/fmp/multicafe/experiment1/cages/CagesArray.java`

## Changes Summary

### 1. Basic Time Accessors (Already Present)
✅ **Already completed**: All basic time management accessors were already present in the class, delegating to `timeManager`:

```java
public long getCamImageFirst_ms() {
    return timeManager.getCamImageFirst_ms();
}

public void setCamImageFirst_ms(long ms) {
    timeManager.setCamImageFirst_ms(ms);
}

public long getCamImageLast_ms() {
    return timeManager.getCamImageLast_ms();
}

public void setCamImageLast_ms(long ms) {
    timeManager.setCamImageLast_ms(ms);
}

public long getCamImageBin_ms() {
    return timeManager.getCamImageBin_ms();
}

public void setCamImageBin_ms(long ms) {
    timeManager.setCamImageBin_ms(ms);
}

public long[] getCamImages_ms() {
    return timeManager.getCamImages_ms();
}

public void setCamImages_ms(long[] ms) {
    timeManager.setCamImages_ms(ms);
}

public long getBinT0() {
    return timeManager.getBinT0();
}

public void setBinT0(long val) {
    timeManager.setBinT0(val);
}

public long getKymoFirst_ms() {
    return timeManager.getKymoFirst_ms();
}

public void setKymoFirst_ms(long ms) {
    timeManager.setKymoFirst_ms(ms);
}

public long getKymoLast_ms() {
    return timeManager.getKymoLast_ms();
}

public void setKymoLast_ms(long ms) {
    timeManager.setKymoLast_ms(ms);
}

public long getKymoBin_ms() {
    return timeManager.getKymoBin_ms();
}

public void setKymoBin_ms(long ms) {
    timeManager.setKymoBin_ms(ms);
}
```

### 2. File Interval Methods (Fixed)

#### getFileIntervalsFromSeqCamData()
**Changed from**: Direct implementation checking seqCamData fields
**Changed to**: Delegates to `timeManager.getFileIntervalsFromSeqCamData()`

```java
public void getFileIntervalsFromSeqCamData() {
    timeManager.getFileIntervalsFromSeqCamData(seqCamData, camDataImagesDirectory);
}
```

#### loadFileIntervalsFromSeqCamData()
**Changed from**: Direct implementation with internal logic
**Changed to**: Delegates to `timeManager.loadFileIntervalsFromSeqCamData()`

```java
public void loadFileIntervalsFromSeqCamData() {
    timeManager.loadFileIntervalsFromSeqCamData(seqCamData, camDataImagesDirectory);
}
```

**Note**: The old internal implementation (`loadFileIntervalsFromSeqCamData_Internal()`) was removed as it's now handled by `ExperimentTimeManager`.

### 3. Time Array Building Methods (Added)

#### build_MsTimeIntervalsArray_From_SeqCamData_FileNamesList()
```java
public long[] build_MsTimeIntervalsArray_From_SeqCamData_FileNamesList(long firstImage_ms) {
    return timeManager.build_MsTimeIntervalsArray_From_SeqCamData_FileNamesList(seqCamData, firstImage_ms);
}
```

**Functionality**: Builds an array of time intervals (in milliseconds) from the sequence camera data file names, starting from `firstImage_ms`.

### 4. Fly Positions Time Initialization (Added)

#### initTmsForFlyPositions()
```java
public void initTmsForFlyPositions(long time_start_ms) {
    timeManager.build_MsTimeIntervalsArray_From_SeqCamData_FileNamesList(seqCamData, time_start_ms);
    cages.initCagesTmsForFlyPositions(timeManager.getCamImages_ms());
}
```

**Functionality**:
1. Builds time intervals array from sequence camera data
2. Initializes time values for all fly positions in all cages

#### initCagesTmsForFlyPositions() in CagesArray (Added)
```java
public void initCagesTmsForFlyPositions(long[] camImages_ms) {
    if (camImages_ms == null)
        return;
    for (Cage cage : cagesList) {
        if (cage.flyPositions != null && cage.flyPositions.flyPositionList != null) {
            for (FlyPosition flyPos : cage.flyPositions.flyPositionList) {
                if (flyPos.flyIndexT >= 0 && flyPos.flyIndexT < camImages_ms.length) {
                    flyPos.tMs = camImages_ms[flyPos.flyIndexT];
                }
            }
        }
    }
}
```

**Functionality**: Sets the `tMs` (time in milliseconds) field for each fly position based on the time intervals array.

### 5. Binary Search Method (Added)

#### findNearestIntervalWithBinarySearch()
```java
public int findNearestIntervalWithBinarySearch(long value, int low, int high) {
    return timeManager.findNearestIntervalWithBinarySearch(value, low, high);
}
```

**Functionality**: Finds the nearest time interval index using binary search within the specified range.

### 6. Bin Name Method (Fixed)

#### getBinNameFromKymoFrameStep()
**Changed from**: Direct calculation using `seqCamData.getTimeManager().getBinDurationMs()`
**Changed to**: Delegates to `timeManager.getBinNameFromKymoFrameStep()`

```java
public String getBinNameFromKymoFrameStep() {
    return timeManager.getBinNameFromKymoFrameStep();
}
```

### 7. Sequence Size Method (Added)

#### getSeqCamSizeT()
```java
public int getSeqCamSizeT() {
    int lastFrame = 0;
    if (seqCamData != null)
        lastFrame = seqCamData.getImageLoader().getNTotalFrames() - 1;
    return lastFrame;
}
```

**Functionality**: Returns the last frame index (size - 1) of the camera sequence.

### 8. Architecture Notes

#### Time Manager Delegation
The new `experiment1.Experiment` uses `ExperimentTimeManager` to handle all time-related operations, while the old version had the logic directly in `Experiment`. The accessors provide a compatibility layer that:
- Maintains the old API
- Delegates to the new architecture
- Ensures backward compatibility

#### Method Mapping

| Old Implementation | New Implementation | Notes |
|-------------------|-------------------|-------|
| Direct time field access | `timeManager.getXxx()` / `timeManager.setXxx()` | Delegation pattern |
| Direct file interval logic | `timeManager.getFileIntervalsFromSeqCamData()` | Centralized in timeManager |
| Direct time array building | `timeManager.build_MsTimeIntervalsArray_From_SeqCamData_FileNamesList()` | Centralized in timeManager |
| Direct binary search | `timeManager.findNearestIntervalWithBinarySearch()` | Centralized in timeManager |

### 9. CagesArray Enhancement

#### New Method: initCagesTmsForFlyPositions()
This method was added to `CagesArray` to support the `initTmsForFlyPositions()` workflow. It:
- Iterates through all cages
- For each cage, iterates through fly positions
- Sets the `tMs` field based on the time intervals array
- Includes bounds checking for safety

### 10. Compatibility Considerations

#### Time Manager Field
The old Experiment uses `ExperimentTimeManager` directly, while the new one also uses it. This ensures compatibility at the API level.

#### Directory Parameter
The new implementation uses `camDataImagesDirectory` instead of `imagesDirectory`, but this is handled transparently through the accessors.

### 11. Testing Recommendations

1. **Time Accessor Tests**:
   - Test all getters return correct values
   - Test all setters update values correctly
   - Verify changes are reflected in `timeManager`

2. **File Interval Tests**:
   - Test `getFileIntervalsFromSeqCamData()` with valid sequences
   - Test `loadFileIntervalsFromSeqCamData()` loads correctly
   - Test with missing or invalid file names

3. **Time Array Tests**:
   - Test `build_MsTimeIntervalsArray_From_SeqCamData_FileNamesList()` with various start times
   - Verify array values are correct

4. **Fly Positions Tests**:
   - Test `initTmsForFlyPositions()` initializes times correctly
   - Test with empty cages
   - Test with cages containing fly positions

5. **Binary Search Tests**:
   - Test `findNearestIntervalWithBinarySearch()` with various values
   - Test edge cases (value at boundaries, value outside range)

### 12. Status
✅ **Phase 5 Complete** - All time management accessor methods have been added/fixed and compilation errors resolved. The methods properly delegate to the new architecture while maintaining backward compatibility.

## Next Steps
Proceed to Phase 6: Experiment Field Management.

