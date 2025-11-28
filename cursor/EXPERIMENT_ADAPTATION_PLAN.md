# Experiment.java Adaptation Plan

## Overview
This document outlines the plan to adapt `experiment1.Experiment` to be compatible with `experiment.Experiment`, enabling it to replace the old version while maintaining new features.

## Analysis Summary

### Architectural Differences

1. **Persistence**:
   - **Old (`experiment`)**: Uses `ExperimentPersistence` helper class for XML operations
   - **New (`experiment1`)**: Implements XML logic directly in the class (`load_MS96_experiment`, `save_MS96_experiment`)

2. **Fields**:
   - **Old**: Has `Capillaries` and `CagesArray cages`
   - **New**: Has `CagesArray cagesArray` (note name difference) and **NO `Capillaries` field**
   - **Old**: Individual string fields for metadata (`boxID`, `experiment`, `comment1`, etc.)
   - **New**: Uses `ExperimentProperties prop` to hold metadata

3. **Sequence Management**:
   - **Old**: Manages `seqKymos` as a field
   - **New**: Has `seqKymos` **commented out** (`// public SequenceKymos seqKymos = null;`)

### Missing Functionality in experiment1.Experiment

#### 1. Capillaries Support
- Add `Capillaries capillaries` field
- Add accessors: `getCapillaries()`, `setCapillaries()`
- Add persistence methods:
  - `loadMCCapillaries_Only()`
  - `loadMCCapillaries()`
  - `saveMCCapillaries_Only()`
  - `loadCapillaries()`
  - `saveCapillaries()`
- Add logic methods:
  - `adjustCapillaryMeasuresDimensions()`
  - `cropCapillaryMeasuresDimensions()`
  - `saveCapillariesMeasures(String directory)`
  - `dispatchCapillariesToCages()`

#### 2. Kymograph Support
- Uncomment/re-add `SequenceKymos seqKymos` field
- Add accessors: `getSeqKymos()`, `setSeqKymos()`
- Add `loadKymographs()` method

#### 3. Legacy Accessors
- `getCages()` / `setCages()` (alias to `cagesArray`)
- `getImagesDirectory()` / `setImagesDirectory()` (alias to `camDataImagesDirectory`)
- `getBoxID()`, `getExperiment()`, `getComment1()`, etc. (delegating to `prop`)
- `getBinSubDirectory()`, `setBinSubDirectory()` (alias to `binDirectory`)

#### 4. Persistence Aliases
- `xmlLoad_MCExperiment()` / `xmlSave_MCExperiment()` (delegating to `load_MS96_experiment` / `save_MS96_experiment`)
- `loadCageMeasures()`, `saveCageMeasures()`, `saveCageAndMeasures()`

#### 5. Time Management Accessors
- `getCamImageFirst_ms()`, `setCamImageFirst_ms()`, etc. (delegating to `seqCamData.getTimeManager()`)
- `getCamImageLast_ms()`, `setCamImageLast_ms()`
- `getCamImageBin_ms()`, `setCamImageBin_ms()`
- `getCamImages_ms()`, `setCamImages_ms()`
- `getBinT0()`, `setBinT0()`
- `getKymoFirst_ms()`, `setKymoFirst_ms()`
- `getKymoLast_ms()`, `setKymoLast_ms()`
- `getKymoBin_ms()`, `setKymoBin_ms()`
- `getFirstImage_FileTime()`, `setFirstImage_FileTime()`
- `getLastImage_FileTime()`, `setLastImage_FileTime()`

#### 6. Experiment Field Management
- `getExperimentField(EnumXLSColumnHeader)` - may need adaptation for new enum values
- `setExperimentFieldNoTest(EnumXLSColumnHeader, String)`
- `replaceExperimentFieldIfEqualOld(EnumXLSColumnHeader, String, String)`
- `copyExperimentFields(Experiment)`
- `replaceFieldValue(EnumXLSColumnHeader, String, String)`
- `getFieldValues(EnumXLSColumnHeader, List<String>)` - signature may differ

#### 7. Additional Methods
- `openMeasures(boolean loadCapillaries, boolean loadDrosoPositions)`
- `loadCamDataCapillaries()`
- `getSeqCamSizeT()`
- `initTmsForFlyPositions(long time_start_ms)`
- `build_MsTimeIntervalsArray_From_SeqCamData_FileNamesList(long firstImage_ms)`
- `findNearestIntervalWithBinarySearch(long value, int low, int high)`

## Implementation Phases

### Phase 1: Capillaries Support ✅ COMPLETED
- [x] Add `Capillaries capillaries` field
- [x] Add accessors `getCapillaries()`, `setCapillaries()`
- [x] Add persistence methods (`loadMCCapillaries_Only`, `loadMCCapillaries`, `saveMCCapillaries_Only`, `loadCapillaries`, `saveCapillaries`)
- [x] Add logic methods (`adjustCapillaryMeasuresDimensions`, `cropCapillaryMeasuresDimensions`, `saveCapillariesMeasures`, `dispatchCapillariesToCages`)
- [x] Add `getSeqKymos()`, `setSeqKymos()` accessors
- [x] Add `getKymosBinFullDirectory()`, `getExperimentDirectory()`, `setExperimentDirectory()` methods
- [x] Add `transferExpDescriptorsToCapillariesDescriptors()` method
- [x] Add `openMeasures()`, `loadKymographs()`, `loadCamDataCapillaries()` methods (with TODOs for ExperimentService adaptation)
- [x] Add `replaceCapillariesValuesIfEqualOld()`, `addCapillariesValues()` methods with enum conversion
- [x] Fix imports (Logger, ROI2DUtilities)
- [x] Fix directory references (`experimentDirectory` -> `resultsDirectory`)

### Phase 2: Kymograph Support ✅ COMPLETED
- [x] Verify `SequenceKymos seqKymos` field is active (already present)
- [x] Verify accessors `getSeqKymos()`, `setSeqKymos()` are present (added in Phase 1)
- [x] Implement `loadKymographs()` method (completed with ImageFileDescriptor conversion)

### Phase 3: Legacy Accessors ✅ COMPLETED
- [x] Verify `getCages()` / `setCages()` aliases (already present)
- [x] Add `getImagesDirectory()` / `setImagesDirectory()` aliases (delegating to `camDataImagesDirectory`)
- [x] Add metadata accessors (delegating to `prop`):
  - `getBoxID()` / `setBoxID()` → `prop.ffield_boxID`
  - `getExperiment()` / `setExperiment()` → `prop.ffield_experiment`
  - `getComment1()` / `setComment1()` → `prop.field_comment1`
  - `getComment2()` / `setComment2()` → `prop.field_comment2`
  - `getStrain()` / `setStrain()` → `prop.field_strain`
  - `getSex()` / `setSex()` → `prop.field_sex`
  - `getCondition1()` / `setCondition1()` → `prop.field_stim2`
  - `getCondition2()` / `setCondition2()` → `prop.field_conc2`
- [x] Verify `getBinSubDirectory()` / `setBinSubDirectory()` aliases (already present, delegating to `binDirectory`)

### Phase 4: Persistence Aliases ✅ COMPLETED
- [x] Add `xmlLoad_MCExperiment()` / `xmlSave_MCExperiment()` aliases (delegating to `load_MS96_experiment()` / `save_MS96_experiment()`)
- [x] Verify `loadCageMeasures()`, `saveCageMeasures()`, `saveCageAndMeasures()` methods (already present, verified correct)

### Phase 5: Time Management Accessors ✅ COMPLETED
- [x] Verify basic time accessors (already present: getCamImageFirst_ms, setCamImageFirst_ms, etc.)
- [x] Fix getFileIntervalsFromSeqCamData() / loadFileIntervalsFromSeqCamData() to delegate to timeManager
- [x] Add build_MsTimeIntervalsArray_From_SeqCamData_FileNamesList()
- [x] Add initTmsForFlyPositions() (with initCagesTmsForFlyPositions() in CagesArray)
- [x] Add findNearestIntervalWithBinarySearch()
- [x] Fix getBinNameFromKymoFrameStep() to delegate to timeManager
- [x] Add getSeqCamSizeT()

### Phase 6: Experiment Field Management ✅ COMPLETED
- [x] Add getExperimentField() - handles PATH, DATE, CAM, and maps old enum values to new ones
- [x] Add setExperimentFieldNoTest() - maps old enum values to new ones
- [x] Add replaceExperimentFieldIfEqualOld() - compatibility alias
- [x] Add copyExperimentFields(Experiment) - copies from old Experiment type
- [x] Add replaceFieldValue() - handles both experiment and capillary fields
- [x] Add getFieldValues(EnumXLSColumnHeader, List<String>) - signature matches old version
- [x] Add helper methods: getPath(), getDate(), getCam(), addValue(), isFound()
- [x] Handle enum mapping: EXP_STIM->EXP_STIM1, EXP_CONC->EXP_CONC1, EXP_COND1->EXP_STIM2, EXP_COND2->EXP_CONC2, CAP_STIM->SPOT_STIM, CAP_CONC->SPOT_CONC

### Phase 7: Additional Methods
- [ ] Port remaining utility methods

## Notes

- The old `Experiment` uses `ExperimentPersistence` for XML operations, while the new one does it directly. We should maintain the new approach but add aliases for compatibility.
- The old version has `CagesArray cages`, while the new has `CagesArray cagesArray`. We should add `getCages()` / `setCages()` as aliases.
- The old version has individual string fields for metadata, while the new uses `ExperimentProperties`. We should add getters/setters that delegate to `prop`.
- `seqKymos` is commented out in the new version - this needs to be restored for full compatibility.

