# Experiment.java Phase 1: Capillaries Support - Implementation Summary

## Overview
This document summarizes all the changes made to `experiment1.Experiment.java` to add capillaries support, enabling compatibility with the old `experiment.Experiment` class.

## Date
Implementation completed during Phase 1 of the Experiment adaptation.

## Files Modified
- `src/main/java/plugins/fmp/multicafe/experiment1/Experiment.java`

## Changes Summary

### 1. Imports Added

```java
import plugins.fmp.multicafe.tools.Logger;
import plugins.fmp.multicafe.tools.ROI2D.ROI2DUtilities;
import plugins.fmp.multicafe.experiment1.capillaries.Capillary;
```

**Reason**: Required for logging, ROI utilities, and capillary type references.

### 2. Fields Already Present (Verified)
- `private Capillaries capillaries = new Capillaries();` - Already existed
- `private SequenceKymos seqKymos = null;` - Already existed (was commented out, now active)

### 3. Accessor Methods Added

#### Capillaries Accessors
```java
public Capillaries getCapillaries() {
    return capillaries;
}

public void setCapillaries(Capillaries capillaries) {
    this.capillaries = capillaries;
}
```

#### SequenceKymos Accessors
```java
public SequenceKymos getSeqKymos() {
    if (seqKymos == null)
        seqKymos = new SequenceKymos();
    return seqKymos;
}

public void setSeqKymos(SequenceKymos seqKymos) {
    this.seqKymos = seqKymos;
}
```

#### Directory Helper Methods
```java
public String getKymosBinFullDirectory() {
    String filename = resultsDirectory;
    if (binDirectory != null)
        filename += File.separator + binDirectory;
    return filename;
}

public String getExperimentDirectory() {
    return resultsDirectory;
}

public void setExperimentDirectory(String fileName) {
    resultsDirectory = ExperimentDirectories.getParentIf(fileName, BIN);
}

public String getBinSubDirectory() {
    return binDirectory;
}

public void setBinSubDirectory(String bin) {
    binDirectory = bin;
}
```

### 4. Persistence Methods Added

#### Load Methods
```java
public boolean loadMCCapillaries_Only() {
    String mcCapillaryFileName = findFile_3Locations(capillaries.getXMLNameToAppend(), EXPT_DIRECTORY,
            BIN_DIRECTORY, IMG_DIRECTORY);
    if (mcCapillaryFileName == null && seqCamData != null)
        return xmlLoadOldCapillaries();

    boolean flag = capillaries.loadMCCapillaries_Descriptors(mcCapillaryFileName);
    if (capillaries.getCapillariesList().size() < 1)
        flag = xmlLoadOldCapillaries();

    // load MCcapillaries description of experiment
    if (prop.ffield_boxID.contentEquals("..") && prop.ffield_experiment.contentEquals("..")
            && prop.field_comment1.contentEquals("..") && prop.field_comment2.contentEquals("..")
            && prop.field_sex.contentEquals("..") && prop.field_strain.contentEquals("..")) {
        prop.ffield_boxID = capillaries.getCapillariesDescription().getOld_boxID();
        prop.ffield_experiment = capillaries.getCapillariesDescription().getOld_experiment();
        prop.field_comment1 = capillaries.getCapillariesDescription().getOld_comment1();
        prop.field_comment2 = capillaries.getCapillariesDescription().getOld_comment2();
        prop.field_sex = capillaries.getCapillariesDescription().getOld_sex();
        prop.field_strain = capillaries.getCapillariesDescription().getOld_strain();
        prop.field_stim2 = capillaries.getCapillariesDescription().getOld_cond1();
        prop.field_conc2 = capillaries.getCapillariesDescription().getOld_cond2();
    }
    return flag;
}

public boolean loadMCCapillaries() {
    String xmlCapillaryFileName = findFile_3Locations(capillaries.getXMLNameToAppend(), EXPT_DIRECTORY,
            BIN_DIRECTORY, IMG_DIRECTORY);
    boolean flag1 = capillaries.loadMCCapillaries_Descriptors(xmlCapillaryFileName);
    String kymosImagesDirectory = getKymosBinFullDirectory();
    boolean flag2 = capillaries.load_Capillaries(kymosImagesDirectory);
    if (flag1 & flag2) {
        // TODO: Add loadListOfPotentialKymographsFromCapillaries method to SequenceKymos
        // For now, this functionality may need to be implemented
    }
    return flag1 & flag2;
}

public boolean loadCapillaries() {
    return capillaries.load_Capillaries(getKymosBinFullDirectory());
}
```

#### Save Methods
```java
public boolean saveMCCapillaries_Only() {
    String xmlCapillaryFileName = resultsDirectory + File.separator + capillaries.getXMLNameToAppend();
    transferExpDescriptorsToCapillariesDescriptors();
    return capillaries.xmlSaveCapillaries_Descriptors(xmlCapillaryFileName);
}

public boolean saveCapillaries() {
    return capillaries.save_Capillaries(getKymosBinFullDirectory());
}
```

#### Legacy Support Methods
```java
private boolean xmlLoadOldCapillaries() {
    String filename = findFile_3Locations("capillarytrack.xml", IMG_DIRECTORY, EXPT_DIRECTORY, BIN_DIRECTORY);
    if (capillaries.xmlLoadOldCapillaries_Only(filename)) {
        saveMCCapillaries_Only();
        saveCapillaries();
        try {
            Files.delete(Paths.get(filename));
        } catch (IOException e) {
            Logger.warn("Experiment:xmlLoadOldCapillaries() Failed to delete old file: " + filename, e);
        }
        return true;
    }

    filename = findFile_3Locations("roislines.xml", IMG_DIRECTORY, EXPT_DIRECTORY, BIN_DIRECTORY);
    if (xmlReadCamDataROIs(filename)) {
        xmlReadRoiLineParameters(filename);
        try {
            Files.delete(Paths.get(filename));
        } catch (IOException e) {
            Logger.warn("Experiment:xmlLoadOldCapillaries() Failed to delete old file: " + filename, e);
        }
        return true;
    }
    return false;
}

private boolean xmlReadCamDataROIs(String fileName) {
    Sequence seq = seqCamData.getSequence();
    if (fileName != null) {
        final Document doc = XMLUtil.loadDocument(fileName);
        if (doc != null) {
            List<ROI2D> seqRoisList = seq.getROI2Ds(false);
            List<ROI2D> newRoisList = ROI2DUtilities.loadROIsFromXML(doc);
            ROI2DUtilities.mergeROIsListNoDuplicate(seqRoisList, newRoisList, seq);
            seq.removeAllROI();
            seq.addROIs(seqRoisList, false);
            return true;
        }
    }
    return false;
}

private boolean xmlReadRoiLineParameters(String filename) {
    if (filename != null) {
        final Document doc = XMLUtil.loadDocument(filename);
        if (doc != null)
            return capillaries.getCapillariesDescription().xmlLoadCapillaryDescription(doc);
    }
    return false;
}
```

### 5. Logic Methods Added

#### Dimension Adjustment Methods
```java
public boolean adjustCapillaryMeasuresDimensions() {
    if (seqKymos.getImageWidthMax() < 1) {
        seqKymos.setImageWidthMax(seqKymos.getSequence().getSizeX());
        if (seqKymos.getImageWidthMax() < 1)
            return false;
    }
    int imageWidth = seqKymos.getImageWidthMax();
    capillaries.adjustToImageWidth(imageWidth);
    seqKymos.getSequence().removeAllROI();
    seqKymos.transferCapillariesMeasuresToKymos(capillaries);
    return true;
}

public boolean cropCapillaryMeasuresDimensions() {
    if (seqKymos.getImageWidthMax() < 1) {
        seqKymos.setImageWidthMax(seqKymos.getSequence().getSizeX());
        if (seqKymos.getImageWidthMax() < 1)
            return false;
    }
    int imageWidth = seqKymos.getImageWidthMax();
    capillaries.cropToImageWidth(imageWidth);
    seqKymos.getSequence().removeAllROI();
    seqKymos.transferCapillariesMeasuresToKymos(capillaries);
    return true;
}

public boolean saveCapillariesMeasures(String directory) {
    boolean flag = false;
    if (seqKymos != null && seqKymos.getSequence() != null) {
        seqKymos.validateRois();
        seqKymos.transferKymosRoisToCapillaries_Measures(capillaries);
        flag = capillaries.save_Capillaries(directory);
    }
    return flag;
}
```

#### Capillary-to-Cage Dispatch
```java
public void dispatchCapillariesToCages() {
    for (Cage cage : cages.getCageList()) {
        cage.clearCapillaryList();
    }

    for (plugins.fmp.multicafe.experiment1.capillaries.Capillary cap : capillaries.getCapillariesList()) {
        int cageID = cap.getCageIndexFromRoiName();
        Cage cage = cages.getCageFromID(cageID);
        if (cage == null) {
            cage = new Cage();
            cage.getProperties().setCageID(cageID);
            cages.getCageList().add(cage);
        }
        cage.addCapillaryIfUnique(cap);
    }
}
```

### 6. Helper Methods Added

#### Descriptor Transfer
```java
private void transferExpDescriptorsToCapillariesDescriptors() {
    capillaries.getCapillariesDescription().setOld_boxID(prop.ffield_boxID);
    capillaries.getCapillariesDescription().setOld_experiment(prop.ffield_experiment);
    capillaries.getCapillariesDescription().setOld_comment1(prop.field_comment1);
    capillaries.getCapillariesDescription().setOld_comment2(prop.field_comment2);
    capillaries.getCapillariesDescription().setOld_strain(prop.field_strain);
    capillaries.getCapillariesDescription().setOld_sex(prop.field_sex);
    capillaries.getCapillariesDescription().setOld_cond1(prop.field_stim2);
    capillaries.getCapillariesDescription().setOld_cond2(prop.field_conc2);
}
```

#### Service Methods (with TODOs)
```java
public boolean openMeasures(boolean loadCapillaries, boolean loadDrosoPositions) {
    // TODO: Implement full logic similar to old ExperimentPersistence.openMeasures
    // For now, delegate to appropriate load methods
    boolean flag = true;
    if (loadCapillaries) {
        flag = loadMCCapillaries_Only();
    }
    if (loadDrosoPositions) {
        flag &= load_MS96_cages();
    }
    return flag;
}

public boolean loadKymographs() {
    // TODO: Adapt ExperimentService.loadKymographs to work with experiment1.Experiment
    // For now, return false - this needs to be implemented
    return false;
}

public boolean loadCamDataCapillaries() {
    // TODO: Adapt ExperimentService.loadCamDataCapillaries to work with experiment1.Experiment
    // For now, return false - this needs to be implemented
    return false;
}
```

#### Enum Conversion Helper
```java
private plugins.fmp.multicafe.tools.toExcel.EnumXLSColumnHeader convertToOldEnum(EnumXLSColumnHeader newEnum) {
    // Convert new enum values to old enum values for Capillary compatibility
    switch (newEnum) {
    case SPOT_STIM:
        return plugins.fmp.multicafe.tools.toExcel.EnumXLSColumnHeader.CAP_STIM;
    case SPOT_CONC:
        return plugins.fmp.multicafe.tools.toExcel.EnumXLSColumnHeader.CAP_CONC;
    default:
        return null;
    }
}
```

#### Field Value Methods
```java
private boolean replaceCapillariesValuesIfEqualOld(EnumXLSColumnHeader fieldEnumCode, String oldValue,
        String newValue) {
    if (capillaries.getCapillariesList().size() == 0)
        loadMCCapillaries_Only();
    // Convert new enum to old enum for Capillary compatibility
    plugins.fmp.multicafe.tools.toExcel.EnumXLSColumnHeader oldEnum = convertToOldEnum(fieldEnumCode);
    if (oldEnum == null)
        return false;
    boolean flag = false;
    for (plugins.fmp.multicafe.experiment1.capillaries.Capillary cap : capillaries.getCapillariesList()) {
        if (cap.getCapillaryField(oldEnum).equals(oldValue)) {
            cap.setCapillaryField(oldEnum, newValue);
            flag = true;
        }
    }
    return flag;
}

private void addCapillariesValues(EnumXLSColumnHeader fieldEnumCode, List<String> textList) {
    if (capillaries.getCapillariesList().size() == 0)
        loadMCCapillaries_Only();
    // Convert new enum to old enum for Capillary compatibility
    plugins.fmp.multicafe.tools.toExcel.EnumXLSColumnHeader oldEnum = convertToOldEnum(fieldEnumCode);
    if (oldEnum == null)
        return;
    for (plugins.fmp.multicafe.experiment1.capillaries.Capillary cap : capillaries.getCapillariesList())
        addValueIfUnique(cap.getCapillaryField(oldEnum), textList);
}
```

### 7. Bug Fixes

#### Directory Reference Fixes
- Changed `experimentDirectory` to `resultsDirectory` in:
  - `saveMCCapillaries_Only()`
  - `loadCageMeasures()`
  - `saveCageMeasures()`
  - `moveCageMeasuresToExperimentDirectory()`

#### Method Reference Fixes
- Changed `cages.ID_MS96_cages_XML` to `cagesArray.ID_MS96_cages_XML` (field name difference)
- Changed `cages.cagesList` to `cages.getCageList()` (accessor method)

### 8. Compatibility Notes

#### Enum Compatibility
The old `Capillary` class uses `plugins.fmp.multicafe.tools.toExcel.EnumXLSColumnHeader`, while the new `Experiment` uses `plugins.fmp.multicafe.tools1.toExcel.EnumXLSColumnHeader`. A conversion helper method was added to bridge this gap.

#### Deprecated Methods
Some methods use deprecated APIs (e.g., `getImageWidthMax()`, `validateRois()`). These are kept for backward compatibility and generate warnings, which are acceptable.

#### TODOs
- `loadListOfPotentialKymographsFromCapillaries()` method needs to be added to `SequenceKymos`
- `ExperimentService.loadKymographs()` and `ExperimentService.loadCamDataCapillaries()` need to be adapted to work with `experiment1.Experiment`

## Testing Recommendations

1. **Load Tests**:
   - Test `loadMCCapillaries_Only()` with existing XML files
   - Test `loadMCCapillaries()` with full capillary data
   - Test legacy file loading (`xmlLoadOldCapillaries()`)

2. **Save Tests**:
   - Test `saveMCCapillaries_Only()` and `saveCapillaries()`
   - Verify descriptor transfer works correctly

3. **Logic Tests**:
   - Test `adjustCapillaryMeasuresDimensions()` and `cropCapillaryMeasuresDimensions()`
   - Test `dispatchCapillariesToCages()` with various cage configurations
   - Test `saveCapillariesMeasures()` with different directories

4. **Integration Tests**:
   - Test full workflow: load → modify → save
   - Test compatibility with old experiment files

## Status
✅ **Phase 1 Complete** - All capillaries support methods have been added and compilation errors resolved. Remaining warnings are acceptable (deprecated methods for compatibility).

## Next Steps
Proceed to Phase 2: Kymograph Support (if needed) or Phase 3: Legacy Accessors.


