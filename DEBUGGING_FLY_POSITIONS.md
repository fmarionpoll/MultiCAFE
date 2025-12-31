# Debugging Guide: Fly Positions Not Displaying

This guide helps debug why fly positions from `CagesArrayMeasures.csv` are not being displayed in multiCAFE.

## Overview of the Loading Process

1. **File Location**: `results/bin60/CagesArrayMeasures.csv` (where `bin60` is the bin directory)
2. **Loading Method**: `CagesArrayPersistence.loadCagesArrayMeasures()` is called from `Experiment.load_MS96_cages()`
3. **CSV Format Expected**: The file must contain a `#POSITION` section header

## Step-by-Step Debugging

### Step 1: Verify File Exists and Path is Correct

**Check 1: Confirm the file exists**
- Verify that `CagesArrayMeasures.csv` exists in `results/bin60/`
- The filename must be exactly `CagesArrayMeasures.csv` (case-sensitive)

**Check 2: Verify the bin directory path**
The method `getKymosBinFullDirectory()` constructs the path as:
```
resultsDirectory + File.separator + binDirectory
```
- Check what `binDirectory` is set to in your experiment (should be "bin60")
- Verify the full path resolves correctly

**Debug Code to Add:**
In `Experiment.java` around line 853, add logging:
```java
String binDir = getKymosBinFullDirectory();
System.out.println("DEBUG: binDir = " + binDir);
if (binDir != null) {
    String csvPath = binDir + File.separator + "CagesArrayMeasures.csv";
    File csvFile = new File(csvPath);
    System.out.println("DEBUG: CSV file exists? " + csvFile.exists());
    System.out.println("DEBUG: CSV file path: " + csvPath);
    boolean loaded = cages.getPersistence().loadCagesArrayMeasures(cages, binDir);
    System.out.println("DEBUG: loadCagesArrayMeasures returned: " + loaded);
}
```

### Step 2: Check CSV File Format

**Expected Format:**

The CSV file must have a `#POSITION` section. The format detection depends on the header line after `#POSITION`:

**Format 1: Parameter format (newer)**
```
#<separator>POSITION
cageID, measureType, npoints, value1, value2, value3, ...
```

Where:
- First line after `#POSITION`: header line (not parsed)
- Data lines: `cageID, measureType, npoints, values...`
- `measureType` is a single character: 't', 'x', 'y', 'w', or 'h'
- Multiple rows per cage (one per measure type)

**Format 2: v0 format (legacy)**
```
#<separator>POSITION
header line containing "x(i)" or "w(i)"
cageID, x1, y1, x2, y2, ... or cageID, x1, y1, w1, h1, x2, y2, w2, h2, ...
```

**Debug Steps:**

1. Open `CagesArrayMeasures.csv` in a text editor
2. Look for a line starting with `#` followed by the separator character and `POSITION`
3. Common separator characters: `,` (comma) or `;` (semicolon)
4. Verify the structure matches one of the formats above

**Example of correct format:**
```csv
#,POSITION
cageID,measureType,npoints,val1,val2,val3,...
0,x,100,10.5,11.2,12.1,...
0,y,100,20.3,21.1,22.0,...
1,x,100,15.0,16.1,17.2,...
1,y,100,25.0,26.1,27.2,...
```

### Step 3: Add Logging to Loading Method

**In `CagesArrayPersistence.java`, modify `loadCagesArrayMeasures()` method:**

Add debug logging around line 254-289:

```java
public boolean loadCagesArrayMeasures(CagesArray cages, String binDirectory) {
    if (binDirectory == null) {
        System.out.println("DEBUG: binDirectory is null");
        return false;
    }

    String pathToCsv = binDirectory + File.separator + ID_CAGESARRAYMEASURES_CSV;
    File csvFile = new File(pathToCsv);
    System.out.println("DEBUG: Checking CSV file: " + pathToCsv);
    System.out.println("DEBUG: File exists: " + csvFile.exists());
    System.out.println("DEBUG: File is file: " + csvFile.isFile());
    
    if (!csvFile.isFile()) {
        System.out.println("DEBUG: CSV file is not a file or doesn't exist");
        return false;
    }

    try {
        BufferedReader csvReader = new BufferedReader(new FileReader(pathToCsv));
        String row;
        String sep = csvSep;
        int lineCount = 0;
        
        while ((row = csvReader.readLine()) != null) {
            lineCount++;
            if (row.length() > 0 && row.charAt(0) == '#')
                sep = String.valueOf(row.charAt(1));

            String[] data = row.split(sep);
            System.out.println("DEBUG: Line " + lineCount + ": " + row);
            System.out.println("DEBUG: Split into " + data.length + " parts");
            
            if (data.length > 0 && data[0].equals("#")) {
                if (data.length > 1) {
                    System.out.println("DEBUG: Found section header: " + data[1]);
                    if (data[1].equals("POSITION")) {
                        System.out.println("DEBUG: Found POSITION section!");
                        csvLoad_Measures(cages, csvReader, EnumCageMeasures.POSITION, sep);
                        csvReader.close();
                        
                        // Count loaded positions
                        int cagesWithPositions = 0;
                        int totalPositions = 0;
                        for (Cage cage : cages.cagesList) {
                            if (cage.flyPositions != null && cage.flyPositions.flyPositionList != null
                                    && !cage.flyPositions.flyPositionList.isEmpty()) {
                                cagesWithPositions++;
                                totalPositions += cage.flyPositions.flyPositionList.size();
                            }
                        }
                        System.out.println("DEBUG: Loaded positions - Cages: " + cagesWithPositions + ", Total positions: " + totalPositions);
                        return true;
                    }
                }
            }
        }
        csvReader.close();
        System.out.println("DEBUG: POSITION section not found after reading " + lineCount + " lines");
        return false;
    } catch (Exception e) {
        System.out.println("DEBUG: Exception in loadCagesArrayMeasures: " + e.getMessage());
        e.printStackTrace();
        Logger.error("CagesArrayPersistence:loadCagesArrayMeasures() Error: " + e.getMessage(), e);
        return false;
    }
}
```

### Step 4: Check Data After Loading

**Add logging to verify data was loaded:**

After the call to `loadCagesArrayMeasures` in `Experiment.java` (around line 855), add:

```java
if (binDir != null) {
    boolean loaded = cages.getPersistence().loadCagesArrayMeasures(cages, binDir);
    System.out.println("DEBUG: loadCagesArrayMeasures returned: " + loaded);
    
    // Check if positions were actually loaded
    int cagesWithPositions = 0;
    for (Cage cage : cages.cagesList) {
        if (cage.flyPositions != null && cage.flyPositions.flyPositionList != null
                && !cage.flyPositions.flyPositionList.isEmpty()) {
            cagesWithPositions++;
            System.out.println("DEBUG: Cage " + cage.prop.getCageID() + " has " + 
                cage.flyPositions.flyPositionList.size() + " positions");
        }
    }
    System.out.println("DEBUG: Total cages with positions: " + cagesWithPositions + " out of " + cages.cagesList.size());
}
```

### Step 5: Check Display Logic

**Verify the display code is checking positions correctly:**

In `CageFlyPositionSeriesBuilder.java` (around line 43), the code checks:
```java
FlyPositions flyPositions = cage.flyPositions != null ? cage.flyPositions : cage.getFlyPositions();
if (flyPositions == null || flyPositions.flyPositionList == null || flyPositions.flyPositionList.isEmpty()) {
    return new XYSeriesCollection();
}
```

Add logging here to see if this is being triggered:
```java
if (flyPositions == null || flyPositions.flyPositionList == null || flyPositions.flyPositionList.isEmpty()) {
    System.out.println("DEBUG: No fly positions for cage " + cage.getProperties().getCageID());
    System.out.println("DEBUG: flyPositions is null: " + (flyPositions == null));
    if (flyPositions != null) {
        System.out.println("DEBUG: flyPositionList is null: " + (flyPositions.flyPositionList == null));
        if (flyPositions.flyPositionList != null) {
            System.out.println("DEBUG: flyPositionList size: " + flyPositions.flyPositionList.size());
        }
    }
    return new XYSeriesCollection();
}
```

### Step 6: Common Issues and Fixes

**Issue 1: CSV file not found**
- **Symptom**: `loadCagesArrayMeasures` returns `false`, no error messages
- **Check**: Verify the path construction in `getKymosBinFullDirectory()`
- **Fix**: Ensure `binDirectory` property is set correctly in the experiment

**Issue 2: POSITION section not found**
- **Symptom**: File exists but `loadCagesArrayMeasures` returns `false`
- **Check**: Look for exact match of `#<separator>POSITION` in CSV (case-sensitive)
- **Fix**: Verify CSV format matches expected structure

**Issue 3: Format detection fails**
- **Symptom**: File is read but no positions are loaded into cages
- **Check**: The header line after `#POSITION` must contain either "x(i)" (v0) or match parameter format
- **Fix**: Verify CSV header matches expected format

**Issue 4: Positions loaded but not displayed**
- **Symptom**: `loadCagesArrayMeasures` returns `true`, positions exist in memory, but charts are empty
- **Check**: Verify `CageFlyPositionSeriesBuilder` is being called with correct result type
- **Check**: Verify display options are enabled in the UI

**Issue 5: Wrong separator character**
- **Symptom**: Data lines are not parsed correctly
- **Check**: First line starting with `#` should have separator as second character
- **Fix**: Ensure consistent separator throughout file

## Quick Test Script

Add this method to test loading directly:

```java
public void testLoadPositions() {
    String binDir = getKymosBinFullDirectory();
    System.out.println("Testing position loading from: " + binDir);
    
    if (binDir == null) {
        System.out.println("ERROR: binDir is null");
        return;
    }
    
    String csvPath = binDir + File.separator + "CagesArrayMeasures.csv";
    File csvFile = new File(csvPath);
    System.out.println("CSV file: " + csvPath);
    System.out.println("Exists: " + csvFile.exists());
    
    boolean loaded = cages.getPersistence().loadCagesArrayMeasures(cages, binDir);
    System.out.println("Load returned: " + loaded);
    
    for (Cage cage : cages.cagesList) {
        int posCount = cage.flyPositions != null && cage.flyPositions.flyPositionList != null 
            ? cage.flyPositions.flyPositionList.size() : 0;
        if (posCount > 0) {
            System.out.println("Cage " + cage.prop.getCageID() + ": " + posCount + " positions");
        }
    }
}
```

## Summary Checklist

- [ ] CSV file exists at correct path: `results/bin60/CagesArrayMeasures.csv`
- [ ] CSV contains `#<separator>POSITION` section header
- [ ] CSV format matches expected structure (parameter or v0 format)
- [ ] `loadCagesArrayMeasures()` is being called (check logs)
- [ ] `loadCagesArrayMeasures()` returns `true`
- [ ] Positions are loaded into `cage.flyPositions.flyPositionList`
- [ ] Display code receives non-empty flyPositionList
- [ ] Display options are enabled in UI

