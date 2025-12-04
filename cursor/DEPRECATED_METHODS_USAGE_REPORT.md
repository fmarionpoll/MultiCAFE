# Deprecated Methods Usage Report

This document lists all places where deprecated methods from `SequenceKymos` are still being used in the codebase.

**Last Updated**: Based on current codebase analysis

---

## Summary of Deprecated Methods in SequenceKymos

| Deprecated Method | Replacement | Status |
|-------------------|-------------|--------|
| `setImageWidthMax(int)` | `updateMaxDimensionsFromSequence()` or `getKymographInfo().getMaxWidth()` | ❌ Still used |
| `getImageWidthMax()` | `getKymographInfo().getMaxWidth()` | ❌ Still used |
| `setImageHeightMax(int)` | `updateMaxDimensionsFromSequence()` or `getKymographInfo().getMaxHeight()` | ❌ Still used |
| `getImageHeightMax()` | `getKymographInfo().getMaxHeight()` | ❌ Still used |
| `validateRois()` | `validateROIs()` | ✅ Recently fixed in Experiment.java |
| `isRunning_loadImages()` | `getKymographInfo().isLoading()` | ❌ Still used |
| `setRunning_loadImages(boolean)` | Internal state managed automatically | ❌ Still used |
| `loadKymographImagesFromList(...)` | `loadKymographs(...)` | ⚠️ Only in deprecated wrapper |
| `loadListOfPotentialKymographsFromSpots(...)` | `createKymographFileList(...)` | ⚠️ Only in deprecated wrapper |

---

## Detailed Usage by File

### 1. **Experiment.java** ✅ FIXED
- **Location**: Lines 1377-1401
- **Status**: ✅ Already updated to use `updateMaxDimensionsFromSequence()`
- **Details**: 
  - `adjustCapillaryMeasuresDimensions()` - Now uses fresh `KymographInfo`
  - `cropCapillaryMeasuresDimensions()` - Now uses fresh `KymographInfo`

---

### 2. **KymographService.java** ❌ NEEDS FIXING

**File**: `src/main/java/plugins/fmp/multicafe/fmp_service/KymographService.java`

#### Line 97: `setRunning_loadImages(true)`
```java
seqKymos.setRunning_loadImages(true);
```
- **Context**: In `loadImagesFromList()` method
- **Replacement**: Remove this call - loading state is now managed internally during `loadKymographs()`
- **Priority**: 🔴 High

#### Line 121: `setRunning_loadImages(false)`
```java
seqKymos.setRunning_loadImages(false);
```
- **Context**: End of `loadImagesFromList()` method
- **Replacement**: Remove this call - loading state is managed internally
- **Priority**: 🔴 High

#### Lines 133-147: Multiple deprecated method calls in `getMaxSizeofTiffFiles()`
```java
private Rectangle getMaxSizeofTiffFiles(SequenceKymos seqKymos, List<ImageFileData> files) {
    seqKymos.setImageWidthMax(0);
    seqKymos.setImageHeightMax(0);
    for (int i = 0; i < files.size(); i++) {
        ImageFileData fileProp = files.get(i);
        if (!fileProp.exists)
            continue;
        getImageDim(fileProp);
        if (fileProp.imageWidth > seqKymos.getImageWidthMax())
            seqKymos.setImageWidthMax(fileProp.imageWidth);
        if (fileProp.imageHeight > seqKymos.getImageHeightMax())
            seqKymos.setImageHeightMax(fileProp.imageHeight);
    }
    return new Rectangle(0, 0, seqKymos.getImageWidthMax(), seqKymos.getImageHeightMax());
}
```
- **Replacement**: This entire method duplicates functionality that exists in `SequenceKymos.calculateMaxDimensions()`
- **Suggested Fix**: Replace with:
  ```java
  private Rectangle getMaxSizeofTiffFiles(SequenceKymos seqKymos, List<ImageFileData> files) {
      Rectangle maxDim = seqKymos.calculateMaxDimensions(files);
      return maxDim;
  }
  ```
- **Priority**: 🔴 High - This affects image loading and dimension calculation

#### Lines 182-183: `getImageWidthMax()` and `getImageHeightMax()` in `adjustImagesToMaxSize()`
```java
IcyBufferedImage ibufImage2 = new IcyBufferedImage(
    seqKymos.getImageWidthMax(),
    seqKymos.getImageHeightMax(), 
    ibufImage1.getSizeC(), 
    ibufImage1.getDataType_());
```
- **Replacement**: Use `KymographInfo`:
  ```java
  KymographInfo info = seqKymos.getKymographInfo();
  IcyBufferedImage ibufImage2 = new IcyBufferedImage(
      info.getMaxWidth(),
      info.getMaxHeight(),
      ibufImage1.getSizeC(),
      ibufImage1.getDataType_());
  ```
- **Priority**: 🟡 Medium - Used in image adjustment process

---

### 3. **Intervals.java** ❌ NEEDS FIXING

**File**: `src/main/java/plugins/fmp/multicafe/dlg/kymos/Intervals.java`

#### Line 108: `getImageWidthMax()`
```java
lastColumnJSpinner.setValue((double) exp.getSeqKymos().getImageWidthMax());
```
- **Context**: Used in `displayDlgKymoIntervals()` to set the maximum value for the last column spinner
- **Replacement**: 
  ```java
  KymographInfo info = exp.getSeqKymos().getKymographInfo();
  lastColumnJSpinner.setValue((double) info.getMaxWidth());
  ```
- **Priority**: 🟡 Medium - Affects UI initialization

---

### 4. **SequenceKymos.java** (Internal usage) ⚠️ ACCEPTABLE

**File**: `src/main/java/plugins/fmp/multicafe/fmp_experiment/sequence/SequenceKymos.java`

#### Line 332: `validateRois()` 
```java
exp.getSeqKymos().validateRois();
```
- **Context**: Used internally in `saveKymosCurvesToCapillariesMeasures()` method
- **Status**: ⚠️ This is a deprecated method calling the deprecated wrapper
- **Replacement**: Should be updated to `validateROIs()` for consistency
- **Priority**: 🟢 Low - Internal usage only

#### Lines 517, 525: Deprecated wrapper methods
- These are deprecated wrappers that delegate to new methods
- **Status**: ⚠️ Acceptable to keep for backward compatibility during migration period

---

## Recommendations by Priority

### 🔴 High Priority (Functional Issues)

1. **KymographService.java** - Lines 133-147 (`getMaxSizeofTiffFiles()`)
   - The method duplicates functionality already in `SequenceKymos`
   - Should use `SequenceKymos.calculateMaxDimensions()` instead
   - This affects image loading and dimension calculation

2. **KymographService.java** - Lines 97, 121 (`setRunning_loadImages()`)
   - These calls should be removed
   - Loading state is now managed internally by `loadKymographs()`

### 🟡 Medium Priority (Code Quality)

3. **Intervals.java** - Line 108
   - Simple replacement using `getKymographInfo()`
   - Affects UI initialization

4. **KymographService.java** - Lines 182-183
   - Replace with `KymographInfo` access
   - Used in image adjustment process

### 🟢 Low Priority (Internal/Cleanup)

5. **SequenceKymos.java** - Line 332
   - Update `validateRois()` to `validateROIs()` in internal method

---

## Migration Patterns

### Pattern 1: Replacing get/set ImageWidth/Height
**Before:**
```java
int width = seqKymos.getImageWidthMax();
seqKymos.setImageWidthMax(newWidth);
```

**After:**
```java
KymographInfo info = seqKymos.getKymographInfo();
int width = info.getMaxWidth();

// To update from sequence dimensions:
info = seqKymos.updateMaxDimensionsFromSequence();
int newWidth = info.getMaxWidth();
```

### Pattern 2: Checking loading state
**Before:**
```java
if (seqKymos.isRunning_loadImages()) { ... }
seqKymos.setRunning_loadImages(true);
```

**After:**
```java
KymographInfo info = seqKymos.getKymographInfo();
if (info.isLoading()) { ... }
// State is managed internally - no need to set manually
```

### Pattern 3: Calculating max dimensions
**Before:**
```java
// Manual loop with setImageWidthMax/getImageWidthMax
private Rectangle getMaxSizeofTiffFiles(SequenceKymos seqKymos, List<ImageFileData> files) {
    seqKymos.setImageWidthMax(0);
    seqKymos.setImageHeightMax(0);
    for (ImageFileData fileProp : files) {
        // ... manual calculation ...
        if (fileProp.imageWidth > seqKymos.getImageWidthMax())
            seqKymos.setImageWidthMax(fileProp.imageWidth);
    }
    return new Rectangle(0, 0, seqKymos.getImageWidthMax(), seqKymos.getImageHeightMax());
}
```

**After:**
```java
// Use existing method in SequenceKymos
private Rectangle getMaxSizeofTiffFiles(SequenceKymos seqKymos, List<ImageFileData> files) {
    Rectangle maxDim = seqKymos.calculateMaxDimensions(files);
    return maxDim;
}

// Or get from KymographInfo:
KymographInfo info = seqKymos.getKymographInfo();
Rectangle maxDim = info.getMaxDimensions();
```

---

## Files Summary

| File | Lines | Deprecated Calls | Priority | Status |
|------|-------|------------------|----------|--------|
| Experiment.java | 1377-1401 | 0 (fixed) | - | ✅ Fixed |
| KymographService.java | 97, 121, 134-146, 182-183 | 10 | 🔴 High | ❌ Needs fixing |
| Intervals.java | 108 | 1 | 🟡 Medium | ❌ Needs fixing |
| SequenceKymos.java | 332 | 1 | 🟢 Low | ⚠️ Optional |

**Total**: 12 deprecated method calls still in use (excluding wrapper methods in SequenceKymos itself)

---

## Quick Reference: All Deprecated Methods

### SequenceKymos.java Deprecated Methods:
1. `getImageWidthMax()` → `getKymographInfo().getMaxWidth()`
2. `setImageWidthMax(int)` → `updateMaxDimensionsFromSequence()` or update internal state
3. `getImageHeightMax()` → `getKymographInfo().getMaxHeight()`
4. `setImageHeightMax(int)` → `updateMaxDimensionsFromSequence()` or update internal state
5. `isRunning_loadImages()` → `getKymographInfo().isLoading()`
6. `setRunning_loadImages(boolean)` → Remove (managed internally)
7. `validateRois()` → `validateROIs()`
8. `loadKymographImagesFromList(...)` → `loadKymographs(...)`
9. `loadListOfPotentialKymographsFromSpots(...)` → `createKymographFileList(...)`

---

*Generated: Comprehensive analysis of deprecated method usage in MultiCAFE project*
