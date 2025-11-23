# MultiCAFE Project Analysis

## Overview

**MultiCAFE** is a Java plugin for ICY (ImageJ-like image analysis platform) designed to analyze multiple Capillary Feeding Experiments (CAFE). The plugin measures how much flies (particularly Drosophila) feed from capillary tubes containing liquid (typically sweet water with blue dye).

## Project Purpose

The plugin analyzes time-lapse image sequences from multiCAFE experiments where:
- One webcam monitors 10 "cages", each containing 1 fly
- Each cage can be equipped with 2 capillaries
- Images are captured at regular intervals (e.g., 1980x1080 JPG every minute over hours to days)
- The goal is to track liquid consumption in capillaries and fly positions over time

## Technical Details

### Build System
- **Maven** project (pom.xml)
- **Version**: 2.2.2
- **Parent POM**: Icy 2.1.0
- **License**: GNU GPLv3
- **Organization**: IDEEV UMR EGCE (CNRS-IRD-Paris-Saclay)

### Key Dependencies
- **Icy Kernel** (2.5.1) - Core ICY platform
- **EzPlug** - UI framework for ICY plugins
- **Apache POI** (4.1.1) - Excel file generation
- **OpenCV** (4.5.1-2) - Image processing
- **JavaCL** - OpenCL support for GPU acceleration
- **JFreeChart** - Charting capabilities
- **Parallel Colt** - Parallel computing
- Various ICY-specific libraries (mask-editor, roi-statistics, etc.)

## Project Architecture

### Package Structure

```
plugins.fmp.multicafe/
├── MultiCAFE.java                    # Main plugin entry point
├── dlg/                              # Dialog/UI components (tabbed interface)
│   ├── browse/                       # File browsing and experiment loading
│   ├── experiment/                   # Experiment configuration
│   ├── capillaries/                  # Capillary ROI definition and editing
│   ├── kymos/                        # Kymograph creation and display
│   ├── levels/                       # Liquid level detection
│   ├── cages/                        # Fly position detection
│   └── excel/                        # Excel export functionality
├── experiment/                       # Core data models
│   ├── Experiment.java               # Main experiment container
│   ├── capillaries/                  # Capillary data structures
│   │   ├── Capillaries.java
│   │   ├── Capillary.java
│   │   └── CapillaryMeasure.java
│   ├── cages/                        # Cage and fly position data
│   │   ├── Cages.java
│   │   ├── Cage.java
│   │   └── FlyPosition.java
│   ├── SequenceCamData.java          # Camera image sequence handling
│   └── SequenceKymos.java           # Kymograph sequence handling
├── series/                           # Image processing routines
│   ├── BuildKymographs.java          # Kymograph generation
│   ├── BuildBackground.java          # Background subtraction
│   ├── DetectLevels.java             # Liquid level detection
│   ├── DetectGulps.java              # Drinking event detection
│   ├── FlyDetect1.java               # Fly detection (method 1)
│   └── FlyDetect2.java               # Fly detection (method 2)
├── tools/                            # Utility classes
│   ├── chart/                        # Charting components
│   ├── toExcel/                      # Excel export utilities
│   ├── ROI2D/                        # ROI manipulation
│   ├── ImageTransform/               # Image transformation
│   └── JComponents/                  # Custom UI components
├── viewer1D/                         # 1D chart viewers
└── workinprogress_gpu/               # GPU acceleration (OpenCL)
    ├── CLfunctions.cl                # OpenCL kernel code
    └── MCSpots_.java                 # GPU spot detection
```

## Main Workflow

1. **Browse** - Load experiment images and define experiment parameters
2. **Experiment** - Configure experiment metadata (strain, sex, conditions, etc.)
3. **Capillaries** - Define capillary positions using linear ROIs (typically 20 for 10 cages)
4. **Kymographs** - Generate kymographs from capillary ROIs to track liquid levels over time
5. **Capillary Levels** - Detect top and bottom levels of liquid in each capillary
6. **Fly Positions** - Detect and track fly positions in cages using:
   - Simple thresholding, or
   - Background subtraction (reference image - current frame)
7. **Export** - Export measurements to Excel files for analysis with pivot tables

## Key Features

### Kymograph Generation
- Creates time-space images from linear ROIs along capillaries
- Tracks liquid consumption over time
- Can be batch-processed for multiple experiments

### Level Detection
- Automatically detects liquid levels in capillaries
- Tracks top and bottom boundaries
- Measures consumption rates

### Fly Tracking
- Detects single fly per cage
- Tracks XYT (x, y, time) positions
- Monitors fly activity and survival
- Uses background subtraction for improved detection

### Data Export
- Exports raw data to Excel
- Includes experiment metadata
- Supports pivot table analysis
- Handles multiple experiments and boxes

### Experiment Management
- Supports multiple "boxes" per image
- Can chain experiments together
- Tracks time intervals and binning
- Manages experiment directories and file organization

## Data Model

### Experiment Class
- Contains references to:
  - `SequenceCamData` - Original camera images
  - `SequenceKymos` - Generated kymographs
  - `Capillaries` - Capillary definitions and measurements
  - `Cages` - Cage definitions and fly positions
- Stores metadata: box ID, strain, sex, conditions, comments
- Manages time information (first/last image times, binning)

### Measurement Units
- Basic unit: "box" (e.g., 10 cages with capillaries)
- Can handle multiple boxes per image
- Empty cages used for evaporation rate estimation

## UI Structure

The plugin uses a tabbed interface with 7 main tabs:
1. **Browse** - File selection and experiment loading
2. **Experiment** - Experiment configuration
3. **Capillaries** - ROI definition and editing
4. **Kymographs** - Kymograph creation and visualization
5. **Capillary levels** - Level detection and editing
6. **Fly positions** - Fly detection and tracking
7. **Export** - Excel export with various options

## Window Management

The application displays several windows:
- **Camera data window** - Displays the stack of images captured
- **Kymograph window** - Shows kymographs generated from capillary ROIs
- **Chart windows** - Multiple graph windows displaying results (managed by `LevelsChart.java` and `ChartLevels.java`)

**Recent Enhancement**: Window positions and sizes are now preserved globally across experiment switches, eliminating flickering and maintaining a consistent workspace layout.

## File Organization

- **Source**: `src/main/java/plugins/fmp/multicafe/`
- **Resources**: `src/main/resources/plugins/fmp/multicafe/icon/`
- **Build Output**: `target/classes/`
- **Configuration**: `pom.xml`, `setting.xml`, `_config.yml`

## Development Notes

- Written in Java for ICY platform
- Uses extensive ICY libraries by Stephane Dallongevile and Fabrice de Chaumont (Institut Pasteur)
- Architecture separates UI (dlg), data models (experiment), processing (series), and utilities (tools)
- Includes experimental GPU acceleration features (workinprogress_gpu)
- Supports batch processing for kymograph generation

## Potential Areas for Analysis

1. **Code Quality**: Review architecture, design patterns, error handling
2. **Performance**: Analyze kymograph generation, image processing efficiency
3. **GPU Acceleration**: Review OpenCL implementation in workinprogress_gpu
4. **Testing**: Check for unit tests, integration tests
5. **Documentation**: Review inline documentation and user guides
6. **Dependencies**: Analyze dependency versions and potential updates
7. **UI/UX**: Review dialog implementations and user workflows

