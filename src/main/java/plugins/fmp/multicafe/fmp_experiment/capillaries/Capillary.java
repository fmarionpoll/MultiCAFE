package plugins.fmp.multicafe.fmp_experiment.capillaries;

import java.awt.Color;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.w3c.dom.Node;

import icy.image.IcyBufferedImage;
import icy.roi.ROI;
import icy.roi.ROI2D;
import icy.type.geom.Polyline2D;
import plugins.fmp.multicafe.fmp_series.options.BuildSeriesOptions;
import plugins.fmp.multicafe.fmp_tools.Level2D;
import plugins.fmp.multicafe.fmp_tools.ROI2D.AlongT;
import plugins.fmp.multicafe.fmp_tools.results.EnumResults;
import plugins.fmp.multicafe.fmp_tools.toExcel.enums.EnumXLSColumnHeader;
import plugins.kernel.roi.roi2d.ROI2DArea;
import plugins.kernel.roi.roi2d.ROI2DLine;
import plugins.kernel.roi.roi2d.ROI2DPolyLine;

public class Capillary implements Comparable<Capillary> {

	// === CORE FIELDS ===
	public int kymographIndex = -1;
	public String version = null;
	public String filenameTIFF = null;
	public IcyBufferedImage cap_Image = null;

	// === PROPERTIES ===
	private final CapillaryProperties properties;

	// === MEASUREMENTS ===
	private final CapillaryMeasurements measurements;

	// === METADATA ===
	private final CapillaryMetadata metadata;

	// === PUBLIC FIELDS (Deprecated/Moved logic) ===
	// These are now delegated to properties but kept for backward compatibility
	// (where not private)
	// Actually, the plan says to update external references, but we can temporarily
	// keep them or just remove them.
	// Since I'm refactoring, I will remove the fields and add accessors.
	// WAIT. The user prompt asked "Update all external code to use getters" -> So I
	// should REMOVE these fields.

	public final String ID_TOPLEVEL = "toplevel";
	public final String ID_BOTTOMLEVEL = "bottomlevel";
	public final String ID_DERIVATIVE = "derivative";

	// === CONSTRUCTORS ===

	public Capillary(ROI2D roiCapillary) {
		this.properties = new CapillaryProperties();
		this.measurements = new CapillaryMeasurements();
		this.metadata = new CapillaryMetadata();

		this.metadata.roiCap = roiCapillary;
		this.metadata.kymographName = replace_LR_with_12(roiCapillary.getName());
	}

	Capillary(String name) {
		this.properties = new CapillaryProperties();
		this.measurements = new CapillaryMeasurements();
		this.metadata = new CapillaryMetadata();

		this.metadata.kymographName = replace_LR_with_12(name);
	}

	public Capillary() {
		this.properties = new CapillaryProperties();
		this.measurements = new CapillaryMeasurements();
		this.metadata = new CapillaryMetadata();
	}

	// === ACCESSORS ===

	public CapillaryProperties getProperties() {
		return properties;
	}

	// Delegate getters for properties
	public String getStimulus() {
		return properties.getStimulus();
	}

	public String getConcentration() {
		return properties.getConcentration();
	}

	public String getSide() {
		return properties.getSide();
	}

	public int getNFlies() {
		return properties.getNFlies();
	}

	public int getCageID() {
		return properties.getCageID();
	}

	public double getVolume() {
		return properties.getVolume();
	}

	public int getPixels() {
		return properties.getPixels();
	}

	public boolean isDescriptionOK() {
		return properties.isDescriptionOK();
	}

	public int getVersionInfos() {
		return properties.getVersionInfos();
	}

	public BuildSeriesOptions getLimitsOptions() {
		return properties.getLimitsOptions();
	}

	public void setStimulus(String s) {
		properties.setStimulus(s);
	}

	public void setConcentration(String s) {
		properties.setConcentration(s);
	}

	public void setSide(String s) {
		properties.setSide(s);
	}

	public void setNFlies(int n) {
		properties.setNFlies(n);
	}

	public void setCageID(int n) {
		properties.setCageID(n);
	}

	public void setVolume(double v) {
		properties.setVolume(v);
	}

	public void setPixels(int p) {
		properties.setPixels(p);
	}

	public void setDescriptionOK(boolean b) {
		properties.setDescriptionOK(b);
	}

	public void setVersionInfos(int v) {
		properties.setVersionInfos(v);
	}

	// Delegate getters for measurements
	public CapillaryMeasure getTopLevel() {
		return measurements.ptsTop;
	}

	public CapillaryMeasure getBottomLevel() {
		return measurements.ptsBottom;
	}

	public CapillaryMeasure getDerivative() {
		return measurements.ptsDerivative;
	}

	public void setDerivative(CapillaryMeasure derivative) {
		measurements.ptsDerivative = derivative;
	}

	public CapillaryGulps getGulps() {
		return measurements.ptsGulps;
	}

	public CapillaryMeasure getTopCorrected() {
		return measurements.ptsTopCorrected;
	}

	public void setTopCorrected(CapillaryMeasure m) {
		measurements.ptsTopCorrected = m;
	}

	// Delegate getters for metadata
	public ROI2D getRoi() {
		return metadata.roiCap;
	}

	public void setRoi(ROI2D roi) {
		metadata.roiCap = roi;
	}

	public List<AlongT> getRoisForKymo() {
		if (metadata.roisForKymo.size() < 1)
			initROI2DForKymoList();
		return metadata.roisForKymo;
	}

	public String getKymographName() {
		return metadata.kymographName;
	}

	public void setKymographName(String name) {
		metadata.kymographName = name;
	}

	public String getRoiNamePrefix() {
		return metadata.kymographPrefix;
	}

	public void setKymographPrefix(String prefix) {
		metadata.kymographPrefix = prefix;
	} // Added setter for import

	public ArrayList<int[]> getCapInteger() {
		return metadata.cap_Integer;
	}

	public void setCapInteger(ArrayList<int[]> list) {
		metadata.cap_Integer = list;
	}

	@Override
	public int compareTo(Capillary o) {
		if (o != null)
			return this.metadata.kymographName.compareTo(o.getKymographName());
		return 1;
	}

	// ------------------------------------------

	public void copy(Capillary cap) {
		kymographIndex = cap.kymographIndex;
		metadata.kymographName = cap.getKymographName();
		version = cap.version;
		metadata.roiCap = cap.getRoi() != null ? (ROI2D) cap.getRoi().getCopy() : null;
		filenameTIFF = cap.filenameTIFF;

		properties.copyFrom(cap.properties);
		measurements.copyFrom(cap.measurements);
	}

	/**
	 * Clears computed measures (evaporation-corrected, L+R, etc.). Should be called
	 * when raw measures change.
	 */
	public void clearComputedMeasures() {
		// measurements.ptsTopCorrected = null;
		// Now persistent, so maybe we want to clear its data instead of nulling the
		// reference?
		// For now, keeping it cleared but object exists.
		if (measurements.ptsTopCorrected != null) {
			measurements.ptsTopCorrected.clear();
		}
	}

	public void setRoiName(String name) {
		metadata.roiCap.setName(name);
	}

	public String getRoiName() {
		if (metadata.roiCap != null)
			return metadata.roiCap.getName();
		return null;
	}

	public String getLast2ofCapillaryName() {
		if (metadata.roiCap != null && metadata.roiCap.getName() != null) {
			return metadata.roiCap.getName().substring(metadata.roiCap.getName().length() - 2);
		}
		// Fallback: extract from kymographName if ROI is not available
		if (metadata.kymographName != null && metadata.kymographName.length() >= 2) {
			return metadata.kymographName.substring(metadata.kymographName.length() - 2);
		}
		return "??";
	}

	public String getCapillarySide() {
		if (metadata.roiCap != null && metadata.roiCap.getName() != null) {
			return metadata.roiCap.getName().substring(metadata.roiCap.getName().length() - 1);
		}
		// Fallback: try to extract from kymographName
		if (metadata.kymographName != null && metadata.kymographName.length() > 0) {
			String lastChar = metadata.kymographName.substring(metadata.kymographName.length() - 1);
			// Convert "1" to "L" and "2" to "R" if needed
			if (lastChar.equals("1"))
				return "L";
			if (lastChar.equals("2"))
				return "R";
			// If it's already L or R, use it
			if (lastChar.equals("L") || lastChar.equals("R"))
				return lastChar;
		}
		// Final fallback to stored capSide
		return properties.getSide() != null && !properties.getSide().equals(".") ? properties.getSide() : "?";
	}

	public static String replace_LR_with_12(String name) {
		String newname = name;
		if (name.contains("R"))
			newname = name.replace("R", "2");
		else if (name.contains("L"))
			newname = name.replace("L", "1");
		return newname;
	}

	public int getCageIndexFromRoiName() {
		if (metadata.roiCap == null) {
			System.out.println("roicap is null");
			return -1;
		}
		String name = metadata.roiCap.getName();
		if (!name.contains("line"))
			return -1;
		String stringNumber = name.substring(4, 5);
		return Integer.valueOf(stringNumber);
	}

	public String getSideDescriptor(EnumResults resultType) {
		String value = null;
		properties.setSide(getCapillarySide());
		String side = properties.getSide();

		switch (resultType) {
		case DISTANCE:
			value = side + "(DIST)";
			break;
		case ISALIVE:
			value = side + "(L=R)";
			break;
		case SUMGULPS_LR:
		case TOPLEVELDELTA_LR:
		case TOPLEVEL_LR:
			if (side.equals("L"))
				value = "sum";
			else
				value = "PI";
			break;
		case XYIMAGE:
		case XYTOPCAGE:
		case XYTIPCAPS:
			if (side.equals("L"))
				value = "x";
			else
				value = "y";
			break;
		default:
			value = side;
			break;
		}
		return value;
	}

	public String getCapillaryField(EnumXLSColumnHeader fieldEnumCode) {
		String stringValue = null;
		switch (fieldEnumCode) {
		case CAP_STIM:
			stringValue = properties.getStimulus();
			break;
		case CAP_CONC:
			stringValue = properties.getConcentration();
			break;
		default:
			break;
		}
		return stringValue;
	}

	public void setCapillaryField(EnumXLSColumnHeader fieldEnumCode, String stringValue) {
		switch (fieldEnumCode) {
		case CAP_STIM:
			properties.setStimulus(stringValue);
			break;
		case CAP_CONC:
			properties.setConcentration(stringValue);
			break;
		default:
			break;
		}
	}

	// -----------------------------------------

	public boolean isThereAnyMeasuresDone(EnumResults resultType) {
		boolean yes = false;
		switch (resultType) {
		case DERIVEDVALUES:
			yes = (measurements.ptsDerivative.isThereAnyMeasuresDone());
			break;
		case SUMGULPS:
			yes = (measurements.ptsGulps.isThereAnyMeasuresDone());
			break;
		case BOTTOMLEVEL:
			yes = measurements.ptsBottom.isThereAnyMeasuresDone();
			break;
		case TOPLEVEL:
		default:
			yes = measurements.ptsTop.isThereAnyMeasuresDone();
			break;
		}
		return yes;
	}

	public ArrayList<Integer> getCapillaryMeasuresForXLSPass1(EnumResults resultType, long seriesBinMs,
			long outputBinMs) {
		ArrayList<Integer> datai = null;
		switch (resultType) {
		case DERIVEDVALUES:
			datai = measurements.ptsDerivative.getMeasures(seriesBinMs, outputBinMs);
			break;
		case SUMGULPS:
		case SUMGULPS_LR:
		case NBGULPS:
		case AMPLITUDEGULPS:
		case TTOGULP:
		case TTOGULP_LR:
		case AUTOCORREL:
		case AUTOCORREL_LR:
		case CROSSCORREL:
		case CROSSCORREL_LR:
			if (measurements.ptsGulps != null)
				datai = measurements.ptsGulps.getMeasuresFromGulps(resultType, measurements.ptsTop.getNPoints(),
						seriesBinMs, outputBinMs);
			break;
		case BOTTOMLEVEL:
			datai = measurements.ptsBottom.getMeasures(seriesBinMs, outputBinMs);
			break;
		case TOPLEVEL:
			// Use evaporation-corrected measure if available, otherwise raw
			if (measurements.ptsTopCorrected != null && measurements.ptsTopCorrected.polylineLevel != null
					&& measurements.ptsTopCorrected.polylineLevel.npoints > 0) {
				datai = measurements.ptsTopCorrected.getMeasures(seriesBinMs, outputBinMs);
			} else {
				datai = measurements.ptsTop.getMeasures(seriesBinMs, outputBinMs);
			}
			break;
		case TOPLEVEL_LR:
			// Note: L+R measures are now stored at the Cage level in
			// CageCapillariesComputation.
			// This method cannot access them directly. The export code should handle
			// TOPLEVEL_LR
			// differently by reading from CageCapillariesComputation.
			// Fallback to raw for now (should not be reached in normal flow)
			datai = measurements.ptsTop.getMeasures(seriesBinMs, outputBinMs);
			break;
		case TOPRAW:
		case TOPLEVELDELTA:
		case TOPLEVELDELTA_LR:
		default:
			datai = measurements.ptsTop.getMeasures(seriesBinMs, outputBinMs);
			break;
		}
		return datai;
	}

	public void cropMeasuresToNPoints(int npoints) {
		measurements.cropMeasuresToNPoints(npoints);
	}

	public void restoreClippedMeasures() {
		measurements.restoreClippedMeasures();
	}

	public void setGulpsOptions(BuildSeriesOptions options) {
		properties.limitsOptions = options;
	}

	public BuildSeriesOptions getGulpsOptions() {
		return properties.limitsOptions;
	}

	public void initGulps() {
		if (measurements.ptsGulps == null)
			measurements.ptsGulps = new CapillaryGulps();

		if (properties.limitsOptions.analyzePartOnly) {
			int searchFromXFirst = (int) properties.limitsOptions.searchArea.getX();
			int searchFromXLast = (int) properties.limitsOptions.searchArea.getWidth() + searchFromXFirst;
			measurements.ptsGulps.removeGulpsWithinInterval(searchFromXFirst, searchFromXLast);
		} else
			measurements.ptsGulps.gulps.clear();
	}

	public void detectGulps() {
		int indexPixel = 0;
		int firstPixel = 1;
		if (measurements.ptsTop.polylineLevel == null)
			return;
		int lastPixel = measurements.ptsTop.polylineLevel.npoints;
		if (properties.limitsOptions.analyzePartOnly) {
			firstPixel = (int) properties.limitsOptions.searchArea.getX();
			lastPixel = (int) properties.limitsOptions.searchArea.getWidth() + firstPixel;

		}
		int threshold = (int) ((properties.limitsOptions.detectGulpsThreshold_uL / properties.getVolume())
				* properties.getPixels());
		ArrayList<Point2D> gulpPoints = new ArrayList<Point2D>();
		int indexLastDetected = -1;

		for (indexPixel = firstPixel; indexPixel < lastPixel; indexPixel++) {
			int derivativevalue = (int) measurements.ptsDerivative.polylineLevel.ypoints[indexPixel - 1];
			if (derivativevalue >= threshold)
				indexLastDetected = addPointMatchingThreshold(indexPixel, gulpPoints, indexLastDetected);
		}
		if (indexLastDetected > 0)
			addNewGulp(gulpPoints);
	}

	private int addPointMatchingThreshold(int indexPixel, ArrayList<Point2D> gulpPoints, int indexLastDetected) {
		if (indexLastDetected > 0 && indexPixel > indexLastDetected) {
			if (gulpPoints.size() == 1)
				gulpPoints.add(new Point2D.Double(indexLastDetected,
						measurements.ptsTop.polylineLevel.ypoints[indexLastDetected]));
			addNewGulp(gulpPoints);
			gulpPoints.clear();
			gulpPoints
					.add(new Point2D.Double(indexPixel - 1, measurements.ptsTop.polylineLevel.ypoints[indexPixel - 1]));
		}
		gulpPoints.add(new Point2D.Double(indexPixel, measurements.ptsTop.polylineLevel.ypoints[indexPixel]));
		return indexPixel;
	}

	private void addNewGulp(ArrayList<Point2D> gulpPoints) {
		measurements.ptsGulps.addNewGulpFromPoints(gulpPoints);
	}

	public int getLastMeasure(EnumResults resultType) {
		int lastMeasure = 0;
		switch (resultType) {
		case DERIVEDVALUES:
			lastMeasure = measurements.ptsDerivative.getLastMeasure();
			break;
		case SUMGULPS:
			if (measurements.ptsGulps != null) {
				List<Integer> datai = measurements.ptsGulps.getCumSumFromGulps(measurements.ptsTop.getNPoints());
				lastMeasure = datai.get(datai.size() - 1);
			}
			break;
		case BOTTOMLEVEL:
			lastMeasure = measurements.ptsBottom.getLastMeasure();
			break;
		case TOPLEVEL:
		default:
			lastMeasure = measurements.ptsTop.getLastMeasure();
			break;
		}
		return lastMeasure;
	}

	public int getLastDeltaMeasure(EnumResults resultType) {
		int lastMeasure = 0;
		switch (resultType) {
		case DERIVEDVALUES:
			lastMeasure = measurements.ptsDerivative.getLastDeltaMeasure();
			break;
		case SUMGULPS:
			if (measurements.ptsGulps != null) {
				List<Integer> datai = measurements.ptsGulps.getCumSumFromGulps(measurements.ptsTop.getNPoints());
				lastMeasure = datai.get(datai.size() - 1) - datai.get(datai.size() - 2);
			}
			break;
		case BOTTOMLEVEL:
			lastMeasure = measurements.ptsBottom.getLastDeltaMeasure();
			break;
		case TOPLEVEL:
		default:
			lastMeasure = measurements.ptsTop.getLastDeltaMeasure();
			break;
		}
		return lastMeasure;
	}

	public int getT0Measure(EnumResults resultType) {
		int t0Measure = 0;
		switch (resultType) {
		case DERIVEDVALUES:
			t0Measure = measurements.ptsDerivative.getT0Measure();
			break;
		case SUMGULPS:
			if (measurements.ptsGulps != null) {
				List<Integer> datai = measurements.ptsGulps.getCumSumFromGulps(measurements.ptsTop.getNPoints());
				t0Measure = datai.get(0);
			}
			break;
		case BOTTOMLEVEL:
			t0Measure = measurements.ptsBottom.getT0Measure();
			break;
		case TOPLEVEL:
		default:
			t0Measure = measurements.ptsTop.getT0Measure();
			break;
		}
		return t0Measure;
	}

	public List<ROI2D> transferMeasuresToROIs() {
		List<ROI2D> listrois = new ArrayList<ROI2D>();
		getROIFromCapillaryLevel(measurements.ptsTop, listrois);
		getROIFromCapillaryLevel(measurements.ptsBottom, listrois);
		getROIFromCapillaryLevel(measurements.ptsDerivative, listrois);
		getROIsFromCapillaryGulps(measurements.ptsGulps, listrois);
		return listrois;
	}

	private void getROIFromCapillaryLevel(CapillaryMeasure capLevel, List<ROI2D> listrois) {
		if (capLevel.polylineLevel == null || capLevel.polylineLevel.npoints == 0)
			return;

		ROI2D roi = new ROI2DPolyLine(capLevel.polylineLevel);
		String name = metadata.kymographPrefix + "_" + capLevel.capName;
		roi.setName(name);
		roi.setT(kymographIndex);
		if (capLevel.capName.contains(ID_DERIVATIVE)) {
			roi.setColor(Color.yellow);
			roi.setStroke(1);
		}
		listrois.add(roi);
	}

	private void getROIsFromCapillaryGulps(CapillaryGulps capGulps, List<ROI2D> listrois) {
		if (capGulps.gulps.size() == 0)
			return;

		ROI2DArea roiDots = new ROI2DArea();
		for (Polyline2D gulpLine : capGulps.gulps) {
			if (gulpLine.npoints > 0) {
				roiDots.addPoint((int) gulpLine.xpoints[0], (int) gulpLine.ypoints[0]);
			}
		}
		if (roiDots.getBounds().isEmpty())
			return;
		
		roiDots.setName(metadata.kymographPrefix + "_gulps");
		roiDots.setColor(Color.red);
		roiDots.setT(kymographIndex);
		listrois.add(roiDots);
	}


	public void transferROIsToMeasures(List<ROI> listRois) {
		measurements.ptsTop.transferROIsToMeasures(listRois);
		measurements.ptsBottom.transferROIsToMeasures(listRois);
		measurements.ptsGulps.transferROIsToMeasures(listRois);
		measurements.ptsDerivative.transferROIsToMeasures(listRois);
	}

	// -----------------------------------------------------------------------------

	public boolean xmlLoad_CapillaryOnly(Node node) {
		return CapillaryPersistence.xmlLoadCapillary(node, this);
	}

	public boolean xmlLoad_MeasuresOnly(Node node) {
		return CapillaryPersistence.xmlLoadMeasures(node, this);
	}

	public boolean xmlSave_CapillaryOnly(Node node) {
		return CapillaryPersistence.xmlSaveCapillary(node, this);
	}

	// -------------------------------------------

	public Point2D getCapillaryTipWithinROI2D(ROI2D roi2D) {
		Point2D pt = null;
		if (metadata.roiCap instanceof ROI2DPolyLine) {
			Polyline2D line = ((ROI2DPolyLine) metadata.roiCap).getPolyline2D();
			int last = line.npoints - 1;
			if (roi2D.contains(line.xpoints[0], line.ypoints[0]))
				pt = new Point2D.Double(line.xpoints[0], line.ypoints[0]);
			else if (roi2D.contains(line.xpoints[last], line.ypoints[last]))
				pt = new Point2D.Double(line.xpoints[last], line.ypoints[last]);
		} else if (metadata.roiCap instanceof ROI2DLine) {
			Line2D line = ((ROI2DLine) metadata.roiCap).getLine();
			if (roi2D.contains(line.getP1()))
				pt = line.getP1();
			else if (roi2D.contains(line.getP2()))
				pt = line.getP2();
		}
		return pt;
	}

	public Point2D getCapillaryROILowestPoint() {
		Point2D pt = null;
		if (metadata.roiCap instanceof ROI2DPolyLine) {
			Polyline2D line = ((ROI2DPolyLine) metadata.roiCap).getPolyline2D();
			int last = line.npoints - 1;
			if (line.ypoints[0] > line.ypoints[last])
				pt = new Point2D.Double(line.xpoints[0], line.ypoints[0]);
			else
				pt = new Point2D.Double(line.xpoints[last], line.ypoints[last]);
		} else if (metadata.roiCap instanceof ROI2DLine) {
			Line2D line = ((ROI2DLine) metadata.roiCap).getLine();
			if (line.getP1().getY() > line.getP2().getY())
				pt = line.getP1();
			else
				pt = line.getP2();
		}
		return pt;
	}

	public Point2D getCapillaryROIFirstPoint() {
		Point2D pt = null;
		if (metadata.roiCap instanceof ROI2DPolyLine) {
			Polyline2D line = ((ROI2DPolyLine) metadata.roiCap).getPolyline2D();
			pt = new Point2D.Double(line.xpoints[0], line.ypoints[0]);
		} else if (metadata.roiCap instanceof ROI2DLine) {
			Line2D line = ((ROI2DLine) metadata.roiCap).getLine();
			pt = line.getP1();
		}
		return pt;
	}

	public Point2D getCapillaryROILastPoint() {
		Point2D pt = null;
		if (metadata.roiCap instanceof ROI2DPolyLine) {
			Polyline2D line = ((ROI2DPolyLine) metadata.roiCap).getPolyline2D();
			int last = line.npoints - 1;
			pt = new Point2D.Double(line.xpoints[last], line.ypoints[last]);
		} else if (metadata.roiCap instanceof ROI2DLine) {
			Line2D line = ((ROI2DLine) metadata.roiCap).getLine();
			pt = line.getP2();
		}
		return pt;
	}

	public int getCapillaryROILength() {
		Point2D pt1 = getCapillaryROIFirstPoint();
		Point2D pt2 = getCapillaryROILastPoint();
		double npixels = Math.sqrt((pt2.getY() - pt1.getY()) * (pt2.getY() - pt1.getY())
				+ (pt2.getX() - pt1.getX()) * (pt2.getX() - pt1.getX()));
		return (int) npixels;
	}

	public void adjustToImageWidth(int imageWidth) {
		measurements.adjustToImageWidth(imageWidth);
	}

	public void cropToImageWidth(int imageWidth) {
		measurements.cropToImageWidth(imageWidth);
	}
	// --------------------------------------------

	public List<AlongT> getROIsForKymo() {
		if (metadata.roisForKymo.size() < 1)
			initROI2DForKymoList();
		return metadata.roisForKymo;
	}

	public AlongT getROI2DKymoAt(int i) {
		if (metadata.roisForKymo.size() < 1)
			initROI2DForKymoList();
		return metadata.roisForKymo.get(i);
	}

	public AlongT getROI2DKymoAtIntervalT(long t) {
		if (metadata.roisForKymo.size() < 1)
			initROI2DForKymoList();

		AlongT capRoi = null;
		for (AlongT item : metadata.roisForKymo) {
			if (t < item.getStart())
				break;
			capRoi = item;
		}
		return capRoi;
	}

	public void removeROI2DIntervalStartingAt(long start) {
		AlongT itemFound = null;
		for (AlongT item : metadata.roisForKymo) {
			if (start != item.getStart())
				continue;
			itemFound = item;
		}
		if (itemFound != null)
			metadata.roisForKymo.remove(itemFound);
	}

	private void initROI2DForKymoList() {
		metadata.roisForKymo.add(new AlongT(0, metadata.roiCap));
	}

	public void setVolumeAndPixels(double volume, int pixels) {
		properties.setVolume(volume);
		properties.setPixels(pixels);
		properties.setDescriptionOK(true);
	}

	// -----------------------------------------------------------------------------

	public String csvExport_CapillarySubSectionHeader(String sep) {
		return CapillaryPersistence.csvExportCapillarySubSectionHeader(sep);
	}

	public String csvExport_CapillaryDescription(String sep) {
		return CapillaryPersistence.csvExportCapillaryDescription(this, sep);
	}

	public String csvExport_MeasureSectionHeader(EnumCapillaryMeasures measureType, String sep) {
		return CapillaryPersistence.csvExportMeasureSectionHeader(measureType, sep);
	}

	public String csvExport_MeasuresOneType(EnumCapillaryMeasures measureType, String sep) {
		return CapillaryPersistence.csvExportMeasuresOneType(this, measureType, sep);
	}

	public void csvImport_CapillaryDescription(String[] data) {
		CapillaryPersistence.csvImportCapillaryDescription(this, data);
	}

	public void csvImport_CapillaryData(EnumCapillaryMeasures measureType, String[] data, boolean x, boolean y) {
		CapillaryPersistence.csvImportCapillaryData(this, measureType, data, x, y);
	}

	// -----------------------------------------------------------------------------

	public CapillaryMeasure getMeasurements(EnumResults resultType) {
		Objects.requireNonNull(resultType, "Export option cannot be null");
		CapillaryMeasure measure = null;
		switch (resultType) {
		case DERIVEDVALUES:
			measure = measurements.ptsDerivative;
			break;
		case SUMGULPS:
		case SUMGULPS_LR:
		case NBGULPS:
		case AMPLITUDEGULPS:
		case TTOGULP:
		case TTOGULP_LR:
		case AUTOCORREL:
		case AUTOCORREL_LR:
		case CROSSCORREL:
		case CROSSCORREL_LR:
			if (measurements.ptsGulps != null) {
				// Create a temporary CapillaryMeasure from gulps data
				// For now, we default to SUMGULPS-like behavior (cumulative sum)
				// or we could throw an exception if the caller expects a specific type of data
				// that isn't a simple level.
				// However, the plan is to return a computed measure.

				// We need to know the dimensions (npoints)
				int npoints = measurements.ptsTop.getNPoints();
				ArrayList<Integer> data = measurements.ptsGulps.getMeasuresFromGulps(resultType, npoints, 1, 1);
				// Note: seriesBinMs=1, outputBinMs=1 implies we want the raw data in the same
				// resolution as the image/kymo

				if (data != null) {
					measure = new CapillaryMeasure(resultType.toString());
					double[] x = new double[data.size()];
					double[] y = new double[data.size()];
					for (int i = 0; i < data.size(); i++) {
						x[i] = i;
						y[i] = data.get(i);
					}
					measure.polylineLevel = new Level2D(x, y, data.size());
				}
			}
			break;
		case BOTTOMLEVEL:
			measure = measurements.ptsBottom;
			break;
		case TOPLEVEL:
		case TOPLEVEL_LR:
		case TOPRAW:
		case TOPLEVELDELTA:
		case TOPLEVELDELTA_LR:
		default:
			measure = measurements.ptsTop;
			break;
		}
		return measure;
	}

	public void resetDerivative() {
		if (measurements.ptsDerivative != null)
			measurements.ptsDerivative.clear();
	}

	public void resetGulps() {
		if (measurements.ptsGulps != null && measurements.ptsGulps.gulps != null)
			measurements.ptsGulps.gulps.clear();
	}

	// === INNER CLASSES ===

	private static class CapillaryMeasurements {
		public CapillaryMeasure ptsTop = new CapillaryMeasure("toplevel");
		public CapillaryMeasure ptsBottom = new CapillaryMeasure("bottomlevel");
		public CapillaryMeasure ptsDerivative = new CapillaryMeasure("derivative");
		public CapillaryGulps ptsGulps = new CapillaryGulps();
		public CapillaryMeasure ptsTopCorrected = new CapillaryMeasure("toplevel_corrected");

		void copyFrom(CapillaryMeasurements source) {
			ptsGulps.copy(source.ptsGulps);
			ptsTop.copy(source.ptsTop);
			ptsBottom.copy(source.ptsBottom);
			ptsDerivative.copy(source.ptsDerivative);
			ptsTopCorrected.copy(source.ptsTopCorrected);
		}

		void cropMeasuresToNPoints(int npoints) {
			if (ptsTop.polylineLevel != null)
				ptsTop.cropToNPoints(npoints);
			if (ptsBottom.polylineLevel != null)
				ptsBottom.cropToNPoints(npoints);
			if (ptsDerivative.polylineLevel != null)
				ptsDerivative.cropToNPoints(npoints);
		}

		void restoreClippedMeasures() {
			if (ptsTop.polylineLevel != null)
				ptsTop.restoreNPoints();
			if (ptsBottom.polylineLevel != null)
				ptsBottom.restoreNPoints();
			if (ptsDerivative.polylineLevel != null)
				ptsDerivative.restoreNPoints();
		}

		void adjustToImageWidth(int imageWidth) {
			ptsTop.adjustToImageWidth(imageWidth);
			ptsBottom.adjustToImageWidth(imageWidth);
			ptsDerivative.adjustToImageWidth(imageWidth);
			ptsGulps.gulps.clear();
		}

		void cropToImageWidth(int imageWidth) {
			ptsTop.cropToImageWidth(imageWidth);
			ptsBottom.cropToImageWidth(imageWidth);
			ptsDerivative.cropToImageWidth(imageWidth);
			ptsGulps.gulps.clear();
		}
	}

	private static class CapillaryMetadata {
		public ROI2D roiCap = null;
		public ArrayList<AlongT> roisForKymo = new ArrayList<AlongT>();
		public String kymographName = null;
		public String kymographPrefix = null;
		public ArrayList<int[]> cap_Integer = null;
	}

}
