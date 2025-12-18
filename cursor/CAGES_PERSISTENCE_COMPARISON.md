# Comparison: CagesArrayPersistence (multiCAFE) vs CagesPersistence (xmultiCAFE0)

## Overview
This document compares the methods in:
- **multiCAFE**: `CagesArrayPersistence` (package `plugins.fmp.multicafe.fmp_experiment.cages`)
- **xmultiCAFE0**: `CagesPersistence` (package `plugins.fmp.multicafe0.experiment.cages`)

## Public Methods Comparison

### 1. `load_Cages()`

**multiCAFE:**
```java
public boolean load_Cages(CagesArray cages, String directory)
```
- Tries CSV load first, falls back to XML
- Uses `csvLoadCagesMeasures()` then `xmlReadCagesFromFileNoQuestion()`
- Returns `boolean`

**xmultiCAFE0:**
```java
public boolean load_Cages(Cages cages, String directory)
```
- Same pattern: CSV first, then XML fallback
- Uses `csvLoad_CageBox()` then `xmlLoadCages()` directly
- Uses `Logger.error()` for error handling
- Returns `boolean`

**Difference**: xmultiCAFE0 uses Logger utility, multiCAFE uses System.err.println

---

### 2. `save_Cages()`

**multiCAFE:**
```java
public boolean save_Cages(CagesArray cages, String directory)
```
- Saves both CSV and XML
- Calls `csvSaveCagesMeasures()` then `xmlWriteCagesToFileNoQuestion()`
- Always returns `true`

**xmultiCAFE0:**
```java
public boolean save_Cages(Cages cages, String directory)
```
- **Only saves CSV** (does not save XML)
- Checks if directory is null first
- Calls `csvSave_Cages()` only
- Uses `Files.exists()` to validate directory
- Returns `boolean` based on success

**Key Difference**: xmultiCAFE0 does NOT save XML in `save_Cages()`, only CSV!

---

### 3. `xmlReadCagesFromFile()`

**multiCAFE:**
```java
public boolean xmlReadCagesFromFile(CagesArray cages, Experiment exp)
```
- Gets directory from `exp.getResultsDirectory()`
- Uses `Dialog.selectFiles()` with try-catch
- Calls `xmlReadCagesFromFileNoQuestion()` for each selected file
- Returns `boolean`

**xmultiCAFE0:**
```java
public boolean xmlReadCagesFromFile(Cages cages, Experiment exp)
```
- Gets directory from `exp.getExperimentDirectory()` (different method name!)
- Uses `Dialog.selectFiles()` without try-catch
- Calls `xmlReadCagesFromFileNoQuestion(cages, csFile, exp)` - **passes Experiment parameter**
- Returns `boolean`

**Key Differences**:
1. Experiment method: `getResultsDirectory()` vs `getExperimentDirectory()`
2. xmultiCAFE0 passes `Experiment` to `xmlReadCagesFromFileNoQuestion()`

---

### 4. `xmlReadCagesFromFileNoQuestion()`

**multiCAFE:**
```java
public boolean xmlReadCagesFromFileNoQuestion(CagesArray cages, String tempname)
```
- Takes only `CagesArray` and filename
- Loads document and calls `xmlLoadCages(cages, XMLUtil.getRootElement(doc))`
- Does NOT transfer cages to ROIs after loading
- Returns `boolean`

**xmultiCAFE0:**
```java
public boolean xmlReadCagesFromFileNoQuestion(Cages cages, String tempname, Experiment exp)
```
- Takes `Cages`, filename, **and Experiment parameter**
- Loads document and calls `xmlLoadCages(cages, doc)` - **passes Document, not Node**
- **Calls `cages.cagesToROIs(exp.getSeqCamData())` after successful load**
- Uses `Logger.warn()` for warnings
- Returns `boolean`

**Key Differences**:
1. xmultiCAFE0 requires `Experiment` parameter
2. xmultiCAFE0 automatically transfers cages to ROIs after loading
3. xmultiCAFE0 passes `Document` to `xmlLoadCages()`, multiCAFE passes `Node`

---

### 5. `xmlWriteCagesToFileNoQuestion()`

**multiCAFE:**
```java
public boolean xmlWriteCagesToFileNoQuestion(CagesArray cages, String tempname)
```
- Creates document, gets root element
- Calls `xmlSaveCages(cages, node)` with root node
- Saves document

**xmultiCAFE0:**
```java
public boolean xmlWriteCagesToFileNoQuestion(Cages cages, String tempname)
```
- Checks if tempname is null first
- Creates document, gets root element
- **Adds a "drosoTrack" element** as intermediate node: `XMLUtil.addElement(XMLUtil.getRootElement(doc), ID_DROSOTRACK)`
- Calls `cage.xmlSaveCagel()` (note: different method name - "Cagel" not "Cage")
- **Does NOT save layout attributes** (nCagesAlongX, nCagesAlongY, etc.)
- Saves document

**Key Differences**:
1. xmultiCAFE0 has intermediate "drosoTrack" XML element wrapper
2. xmultiCAFE0 does NOT persist layout information (nCagesAlongX, etc.)
3. Different Cage save method name: `xmlSaveCagel()` vs `xmlSaveCage()`

---

## Private Methods Comparison

### XML Methods

#### `xmlSaveCages()` / `xmlLoadCages()`

**multiCAFE:**
```java
private boolean xmlSaveCages(CagesArray cages, Node node)
private boolean xmlLoadCages(CagesArray cages, Node node)
```
- Saves/loads layout attributes: `nCagesAlongX`, `nCagesAlongY`, `nColumnsPerCage`, `nRowsPerCage`
- Uses `XMLUtil.addElement(node, ID_CAGES)` directly on provided node
- Validates cage count and handles null cages
- Returns `loadedCages > 0` for load

**xmultiCAFE0:**
```java
private boolean xmlLoadCages(Cages cages, Document doc)
```
- Takes `Document` instead of `Node`
- **Does NOT save/load layout attributes**
- Looks for `ID_DROSOTRACK` element first, then `ID_CAGES`
- **Has backward compatibility**: supports old format via `xmlLoadCageLimits_v0()` and `xmlLoadFlyPositions_v0()`
- Always returns `true` on success (no validation of loaded count)
- Has helper methods: `transferDataToCageBox_v0()`, `xmlLoadCageLimits_v0()`, `xmlLoadFlyPositions_v0()`

**Key Differences**:
1. multiCAFE persists layout information, xmultiCAFE0 does not
2. xmultiCAFE0 has legacy format support (v0)
3. xmultiCAFE0 uses Document parameter, multiCAFE uses Node

---

### CSV Methods

#### `csvLoad_CageBox()` / `csvLoadCagesMeasures()`

**multiCAFE:**
```java
private boolean csvLoadCagesMeasures(CagesArray cages, String directory) throws Exception
```
- Clears cages list before loading
- Checks `row.length() > 0` before checking first character
- Returns `cages.cagesList.size() > 0`

**xmultiCAFE0:**
```java
private boolean csvLoad_CageBox(Cages cages, String directory) throws Exception
```
- Does NOT clear cages list before loading
- Assumes row has at least one character (no length check)
- Returns `true` (always)

**Key Difference**: multiCAFE clears list first, xmultiCAFE0 does not

---

#### `csvLoad_DESCRIPTION()`

**multiCAFE:**
```java
private void csvLoad_DESCRIPTION(CagesArray cages, BufferedReader csvReader, String sep)
```
- Returns `void`
- Uses `System.err.println()` for errors

**xmultiCAFE0:**
```java
private String csvLoad_DESCRIPTION(Cages cages, BufferedReader csvReader, String sep)
```
- Returns `String` (returns `data[1]` when encountering "#" marker, or `null`)
- Uses `Logger.error()` for errors

**Key Difference**: Return type and error handling

---

#### `csvLoad_CAGE()` / `csvLoad_CageBox()`

**multiCAFE:**
```java
private void csvLoad_CAGE(CagesArray cages, BufferedReader csvReader, String sep)
```
- Returns `void`
- Skips header row explicitly
- Uses `System.err.println()` for errors
- Uses `continue` on NumberFormatException

**xmultiCAFE0:**
```java
private String csvLoad_CageBox(Cages cages, BufferedReader csvReader, String sep)
```
- Returns `String` (returns `data[1]` when encountering "#" marker, or `null`)
- Reads header row but doesn't skip it explicitly
- Uses `Logger.warn()` for NumberFormatException (doesn't continue)
- Uses `Logger.error()` for IOException

**Key Differences**: Return type, error handling, exception handling strategy

---

#### `csvLoad_Measures()`

**multiCAFE:**
```java
private void csvLoad_Measures(CagesArray cages, BufferedReader csvReader, EnumCageMeasures measureType, String sep)
```
- Returns `void`
- Checks `row != null` before calling `contains()`
- Creates new cage and sets ID if not found: `cage.prop.setCageID(cageID)`
- Uses `System.err.println()` for errors

**xmultiCAFE0:**
```java
private String csvLoad_Measures(Cages cages, BufferedReader csvReader, EnumCageMeasures measureType, String sep)
```
- Returns `String` (returns `data[1]` when encountering "#" marker, or `null`)
- Assumes row is not null (no null check)
- Creates new cage but **does NOT set ID** if not found
- Uses `Logger.warn()` and `Logger.error()` for errors

**Key Differences**:
1. Return type
2. Null safety checks
3. ID setting behavior when cage not found

---

#### `csvSave_Cages()` / `csvSaveCagesMeasures()`

**multiCAFE:**
```java
private boolean csvSaveCagesMeasures(CagesArray cages, String directory)
```
- No directory validation
- Uses `e.printStackTrace()` for errors

**xmultiCAFE0:**
```java
private boolean csvSave_Cages(Cages cages, String directory)
```
- **Validates directory exists** using `Files.exists(Paths.get(directory))`
- Returns `false` if directory doesn't exist
- Uses `Logger.error()` for errors

**Key Difference**: Directory validation

---

#### `csvSave_Description()` / `csvSaveDescriptionSection()`

**multiCAFE:**
```java
private boolean csvSaveDescriptionSection(CagesArray cages, FileWriter csvWriter)
```
- Writes: `"#;DESCRIPTION;Cages data\n"`
- Writes description for each cage: `cage.csvExportCageDescription(csvSep)`
- Uses `e.printStackTrace()` for errors

**xmultiCAFE0:**
```java
private boolean csvSave_Description(Cages cages, FileWriter csvWriter)
```
- Writes: `"#;DESCRIPTION\n"` (no "Cages data" suffix)
- Writes header from first cage: `cages.getCageList().get(0).csvExport_CAGE_Header(csvSep)`
- Writes data for each cage: `cage.csvExport_CAGE_Data(csvSep)`
- Uses `Logger.error()` for errors

**Key Differences**:
1. Different CSV format (header vs description)
2. xmultiCAFE0 writes header + data separately, multiCAFE writes description
3. Different method names on Cage: `csvExport_CAGE_Header/Data` vs `csvExportCageDescription`

---

#### `csvSave_Measures()` / `csvSaveMeasuresSection()`

**multiCAFE:**
```java
private boolean csvSaveMeasuresSection(CagesArray cages, FileWriter csvWriter, EnumCageMeasures measuresType)
```
- **Empty implementation** - just returns `true`
- Does nothing

**xmultiCAFE0:**
```java
private boolean csvSave_Measures(Cages cages, FileWriter csvWriter, EnumCageMeasures measureType)
```
- **Full implementation**
- Checks if `cages.getCageList().size() <= 1` and returns `false` if so
- Writes header: `cages.getCageList().get(0).csvExport_MEASURE_Header(measureType, csvSep, complete)`
- Writes data for each cage: `cage.csvExport_MEASURE_Data(measureType, csvSep, complete)`
- Uses `Logger.error()` for errors

**Key Difference**: multiCAFE has empty implementation, xmultiCAFE0 has full implementation!

---

## Constants Comparison

**multiCAFE:**
- `ID_CAGES`, `ID_NCAGES`
- `ID_NCAGESALONGX`, `ID_NCAGESALONGY`, `ID_NCOLUMNSPERCAGE`, `ID_NROWSPERCAGE` (layout constants)
- `ID_MCDROSOTRACK_XML`
- `csvSep = ";"`

**xmultiCAFE0:**
- `ID_CAGES`, `ID_NCAGES`
- `ID_DROSOTRACK` (for XML wrapper element)
- `ID_NBITEMS`, `ID_CAGELIMITS`, `ID_FLYDETECTED` (for legacy format support)
- `ID_MCDROSOTRACK_XML` (static final)
- `csvSep = ";"`

**Key Differences**:
1. multiCAFE has layout constants, xmultiCAFE0 does not
2. xmultiCAFE0 has legacy format constants
3. xmultiCAFE0 has `ID_DROSOTRACK` for XML structure

---

## Summary of Key Differences

### 1. **XML Structure**
- **multiCAFE**: Direct structure, saves layout info
- **xmultiCAFE0**: Wrapped in "drosoTrack" element, no layout info, supports legacy format

### 2. **Save Behavior**
- **multiCAFE**: `save_Cages()` saves both CSV and XML
- **xmultiCAFE0**: `save_Cages()` saves only CSV (no XML)

### 3. **CSV Save Measures**
- **multiCAFE**: Empty implementation
- **xmultiCAFE0**: Full implementation with header and data

### 4. **Error Handling**
- **multiCAFE**: Uses `System.err.println()` and `e.printStackTrace()`
- **xmultiCAFE0**: Uses `Logger.error()` and `Logger.warn()`

### 5. **CSV Load Return Types**
- **multiCAFE**: `void` for CSV load helpers
- **xmultiCAFE0**: `String` for CSV load helpers (returns next section marker)

### 6. **ROI Transfer**
- **multiCAFE**: Does NOT automatically transfer cages to ROIs after XML load
- **xmultiCAFE0**: Automatically calls `cages.cagesToROIs()` after XML load

### 7. **Layout Persistence**
- **multiCAFE**: Persists `nCagesAlongX`, `nCagesAlongY`, `nColumnsPerCage`, `nRowsPerCage`
- **xmultiCAFE0**: Does NOT persist layout information

### 8. **Legacy Format Support**
- **multiCAFE**: No legacy format support
- **xmultiCAFE0**: Supports old XML format via `xmlLoadCageLimits_v0()` and `xmlLoadFlyPositions_v0()`

---

## Recommendations for multiCAFE

1. **Consider implementing `csvSave_Measures()`** - Currently empty, xmultiCAFE0 has full implementation
2. **Consider adding Logger utility** - Better error handling than System.err.println
3. **Consider directory validation** - xmultiCAFE0 validates directory exists before saving
4. **Consider legacy format support** - If backward compatibility with old files is needed
5. **Review ROI transfer behavior** - Decide if automatic transfer after load is desired
6. **Keep layout persistence** - This is a feature multiCAFE has that xmultiCAFE0 doesn't

