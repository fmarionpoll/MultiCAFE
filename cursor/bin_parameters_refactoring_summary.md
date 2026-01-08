# Bin Parameters Refactoring Summary

## Overview
Refactored bin parameter storage from a single set of parameters in `v2_Experiment.xml` to per-directory `v2_bindescription.xml` files. This enables proper support for multiple bin directories (bin_10, bin_60, etc.) with different durations, each maintaining its own parameters.

## Problem Statement
Previously, bin parameters (`firstKymoColMs`, `lastKymoColMs`, `binKymoColMs`) were stored only once in `v2_Experiment.xml`, even though multiple bin directories could exist. This caused incorrect parameters when switching between bins, as only one set of parameters was persisted regardless of which bin was active.

## Solution
Moved bin parameters to individual `v2_bindescription.xml` files in each bin subdirectory, making each bin self-contained with its own parameters.

## Files Created

### 1. `BinDescription.java`
**Location**: `src/main/java/plugins/fmp/multicafe/fmp_experiment/BinDescription.java`

Data class containing:
- `firstKymoColMs` - First kymograph column time in milliseconds
- `lastKymoColMs` - Last kymograph column time in milliseconds
- `binKymoColMs` - Bin duration in milliseconds (default: 60000)
- `binDirectory` - Name of the bin directory

Methods:
- Standard getters/setters
- `copyFrom()` - Copy values from another BinDescription
- `isValid()` - Validate bin description parameters

### 2. `BinDescriptionPersistence.java`
**Location**: `src/main/java/plugins/fmp/multicafe/fmp_experiment/BinDescriptionPersistence.java`

Handles persistence of bin descriptions:
- `load(BinDescription, String binDirectory)` - Loads from `v2_bindescription.xml` in bin directory
- `save(BinDescription, String binDirectory)` - Saves to `v2_bindescription.xml` in bin directory
- XML format: `<binDescription>` root with `firstKymoColMs`, `lastKymoColMs`, `binKymoColMs` elements
- Filename: `v2_bindescription.xml` in each bin subdirectory

## Files Modified

### 1. `Experiment.java`
**Location**: `src/main/java/plugins/fmp/multicafe/fmp_experiment/Experiment.java`

**Changes**:
- Added `activeBinDescription` field to store current bin parameters
- Added `binDescriptionPersistence` instance for persistence operations
- Modified `getKymoFirst_ms()`, `getKymoLast_ms()`, `getKymoBin_ms()` to read from `activeBinDescription` (with fallback to TimeManager for backward compatibility)
- Modified `setKymoFirst_ms()`, `setKymoLast_ms()`, `setKymoBin_ms()` to update both `activeBinDescription` and TimeManager
- Updated `setBinSubDirectory()` to automatically load bin description when switching bins
- Added `loadBinDescription(String binSubDirectory)` method:
  - Loads parameters from bin directory
  - Syncs with TimeManager for backward compatibility
  - Falls back to TimeManager values if file doesn't exist
- Added `saveBinDescription(String binSubDirectory)` and `saveBinDescription()` methods:
  - Saves current bin parameters to bin directory
  - Updates activeBinDescription before saving
- Updated `saveExperimentDescriptors()` to also save bin description if bin directory is set
- Updated `load_MS96_experiment()` method with migration logic to extract bin parameters from old XML format

### 2. `ExperimentPersistence.java`
**Location**: `src/main/java/plugins/fmp/multicafe/fmp_experiment/ExperimentPersistence.java`

**Changes**:
- **Removed** saving of `ID_FIRSTKYMOCOLMS`, `ID_LASTKYMOCOLMS`, `ID_BINKYMOCOLMS` from `xmlSaveExperiment()`
- **Removed** direct loading of these fields into Experiment in `xmlLoadExperiment()`
- **Added** migration logic in `xmlLoadExperiment()`:
  - Detects if bin parameters exist in old XML format
  - Extracts bin parameters
  - Determines target bin directory (from `binDirectory` field or defaults to `bin_60` based on duration)
  - Creates `BinDescription` and saves to `v2_bindescription.xml` in target bin directory
  - Loads migrated description into active bin description
- **Removed** setting bin parameters in TimeManager (now handled by bin description loading)

### 3. `KymographBuilder.java`
**Location**: `src/main/java/plugins/fmp/multicafe/fmp_service/KymographBuilder.java`

**Changes**:
- Updated `buildKymograph()` method to save bin description when creating new bin directory
- When `options.doCreateBinDir` is true, after setting bin subdirectory, explicitly saves bin description with current parameters

## File Structure After Changes

```
results/
  ├── v2_Experiment.xml          (no bin parameters)
  ├── bin_60/
  │   ├── v2_bindescription.xml   (firstKymoColMs, lastKymoColMs, binKymoColMs)
  │   └── [kymograph files...]
  └── bin_10/
      ├── v2_bindescription.xml   (firstKymoColMs, lastKymoColMs, binKymoColMs)
      └── [kymograph files...]
```

## Migration Strategy

1. **On Load**: 
   - If `v2_Experiment.xml` contains bin parameters (old format), automatically migrates them to current `binDirectory` (or default `bin_60`)
   - Migration happens transparently during experiment load
   - Old parameters are preserved in bin directory, not lost

2. **On Save**: 
   - Never saves bin parameters to `v2_Experiment.xml`
   - Always saves to `v2_bindescription.xml` in the active bin directory

3. **When Switching Bins**: 
   - Automatically loads `v2_bindescription.xml` from target bin directory
   - Creates file with default values if missing
   - Syncs loaded values with TimeManager for backward compatibility

## Backward Compatibility

- **TimeManager Integration**: Bin parameters are synced with `ExperimentTimeManager` to maintain compatibility with existing code that reads from TimeManager
- **Fallback Behavior**: If bin description file doesn't exist, falls back to TimeManager values
- **Migration**: Old experiments with bin parameters in `v2_Experiment.xml` are automatically migrated on first load
- **Default Values**: If no bin parameters are found, defaults to 60000ms (60 seconds) for bin duration

## Benefits

1. **Multiple Bins Support**: Each bin directory can have its own parameters, properly supporting analyses with different bin durations
2. **Self-Contained**: Each bin directory is self-contained with its own data and parameters
3. **Cleaner Architecture**: Parameters are stored where the data is, improving organization
4. **Automatic Migration**: Existing experiments are automatically migrated without user intervention
5. **Backward Compatible**: Existing code continues to work through TimeManager synchronization

## Testing Considerations

- Test loading experiments with old format (bin params in v2_Experiment.xml)
- Test creating new bins with different durations
- Test switching between bins
- Test saving/loading bin parameters independently
- Verify backward compatibility with existing bin_60 directories
- Verify migration works correctly for experiments with multiple bins

## Implementation Date
January 2025
