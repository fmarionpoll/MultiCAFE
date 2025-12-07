package plugins.fmp.multicafe.fmp_tools.toExcel.data;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import plugins.fmp.multicafe.fmp_experiment.cages.CageProperties;
import plugins.fmp.multicafe.fmp_experiment.cages.FlyPosition;
import plugins.fmp.multicafe.fmp_experiment.cages.FlyPositions;
import plugins.fmp.multicafe.fmp_experiment.capillaries.Capillary;
import plugins.fmp.multicafe.fmp_experiment.spots.Spot;
import plugins.fmp.multicafe.fmp_experiment.spots.SpotProperties;
import plugins.fmp.multicafe.fmp_tools.toExcel.config.XLSExportOptions;
import plugins.fmp.multicafe.fmp_tools.toExcel.enums.EnumXLSExport;

public class XLSResults {
	private String name = null;
	private String stimulus = null;
	private String concentration = null;
	private int nflies = 1;
	private int cageID = 0;
	private int cagePosition = 0;
	private Color color;
	private ArrayList<Double> dataValues = null;
	private int valuesOutLength = 0;
	private double[] valuesOut = null;

	public XLSResults(String name, int nflies, int cageID, int cagePos, EnumXLSExport exportType) {
		this.name = name;
		this.nflies = nflies;
		this.cageID = cageID;
		this.cagePosition = cagePos;
	}

	public XLSResults(CageProperties cageProperties, SpotProperties spotProperties, int nFrames) {
		this.name = spotProperties.getName();
		this.color = spotProperties.getColor();
		this.nflies = cageProperties.getCageNFlies();
		this.cageID = cageProperties.getCageID();
		this.cagePosition = spotProperties.getCagePosition();
		this.stimulus = spotProperties.getStimulus();
		this.concentration = spotProperties.getConcentration();
		initValuesArray(nFrames);
	}

	// ---------------------------
	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getStimulus() {
		return this.stimulus;
	}

	public void setStimulus(String stimulus) {
		this.stimulus = stimulus;
	}

	public String getConcentration() {
		return this.concentration;
	}

	public void setConcentration(String concentration) {
		this.concentration = concentration;
	}

	public int getNflies() {
		return this.nflies;
	}

	public void setNflies(int nFlies) {
		this.nflies = nFlies;
	}

	public int getCageID() {
		return this.cageID;
	}

	public void setCageID(int cageID) {
		this.cageID = cageID;
	}

	public int getCagePosition() {
		return this.cagePosition;
	}

	public void getCagePosition(int cagePosition) {
		this.cagePosition = cagePosition;
	}

	public Color getColor() {
		return this.color;
	}

	public void setColor(Color color) {
		this.color = color;
	}

	public ArrayList<Double> getDataValues() {
		return this.dataValues;
	}

	public void setDataValues(ArrayList<Double> dataValues) {
		this.dataValues = dataValues;
	}

	public int getValuesOutLength() {
		return this.valuesOutLength;
	}

	public double[] getValuesOut() {
		return valuesOut;
	}

	public void setValuesOut(double[] valuesOut) {
		this.valuesOut = valuesOut;
	}

	// ---------------------------

	public void initValuesOutArray(int dimension, Double val) {
		this.valuesOutLength = dimension;
		valuesOut = new double[dimension];
		Arrays.fill(valuesOut, val);
	}

	private void initValuesArray(int dimension) {
		this.valuesOutLength = dimension;
		valuesOut = new double[dimension];
		Arrays.fill(valuesOut, Double.NaN);
	}

	void clearValues(int fromindex) {
		int toindex = valuesOut.length;
		if (fromindex > 0 && fromindex < toindex) {
			Arrays.fill(valuesOut, fromindex, toindex, Double.NaN);
		}
	}

	void clearAll() {
		dataValues = null;
		valuesOut = null;
		nflies = 0;
	}

	public void getDataFromSpot(Spot spot, long binData, long binExcel, XLSExportOptions xlsExportOptions) {
		dataValues = (ArrayList<Double>) spot.getMeasuresForExcelPass1(xlsExportOptions.exportType, binData, binExcel);
		if (xlsExportOptions.relativeToT0 && xlsExportOptions.exportType != EnumXLSExport.AREA_FLYPRESENT) {
			relativeToMaximum();
		}
	}

	/**
	 * Gets data from a capillary and converts it to dataValues.
	 * Capillary.getCapillaryMeasuresForXLSPass1() returns ArrayList<Integer>, so we
	 * convert to ArrayList<Double>.
	 * 
	 * @param capillary        The capillary to get data from
	 * @param binData          The bin duration for the data
	 * @param binExcel         The bin duration for Excel output
	 * @param xlsExportOptions The export options
	 * @param subtractT0       Whether to subtract T0 value (for TOPLEVEL, TOPRAW,
	 *                         etc.)
	 */
	public void getDataFromCapillary(Capillary capillary, long binData, long binExcel,
			XLSExportOptions xlsExportOptions, boolean subtractT0) {
		ArrayList<Integer> intData = capillary.getCapillaryMeasuresForXLSPass1(xlsExportOptions.exportType, binData,
				binExcel);

		if (intData == null || intData.isEmpty()) {
			dataValues = new ArrayList<>();
			return;
		}

		// Convert Integer to Double
		dataValues = new ArrayList<>(intData.size());
		int t0Value = 0;

		if (subtractT0 && intData.size() > 0) {
			t0Value = intData.get(0);
		}

		for (Integer intValue : intData) {
			if (subtractT0) {
				dataValues.add((double) (intValue - t0Value));
			} else {
				dataValues.add(intValue.doubleValue());
			}
		}

		if (xlsExportOptions.relativeToT0 && xlsExportOptions.exportType != EnumXLSExport.AREA_FLYPRESENT) {
			relativeToMaximum();
		}
	}

	/**
	 * Gets data from fly positions and converts it to dataValues.
	 * 
	 * @param flyPositions     The fly positions to get data from
	 * @param binData          The bin duration for the data
	 * @param binExcel         The bin duration for Excel output
	 * @param xlsExportOptions The export options
	 */
	public void getDataFromFlyPositions(FlyPositions flyPositions, long binData, long binExcel,
			XLSExportOptions xlsExportOptions) {
		if (flyPositions == null || flyPositions.flyPositionList == null || flyPositions.flyPositionList.isEmpty()) {
			dataValues = new ArrayList<>();
			return;
		}

		dataValues = new ArrayList<>();
		EnumXLSExport exportType = xlsExportOptions.exportType;

		switch (exportType) {
		case XYIMAGE:
		case XYTOPCAGE:
		case XYTIPCAPS:
			// Extract X or Y coordinate based on export type
			for (FlyPosition pos : flyPositions.flyPositionList) {
				Point2D center = pos.getCenterRectangle();
				if (exportType == EnumXLSExport.XYIMAGE || exportType == EnumXLSExport.XYTOPCAGE) {
					// For XYIMAGE and XYTOPCAGE, we might need to extract X or Y
					// Defaulting to Y coordinate (vertical position)
					dataValues.add(center.getY());
				} else {
					// XYTIPCAPS - could be X coordinate
					dataValues.add(center.getX());
				}
			}
			break;

		case DISTANCE:
			// Compute distance between consecutive points
			flyPositions.computeDistanceBetweenConsecutivePoints();
			for (FlyPosition pos : flyPositions.flyPositionList) {
				dataValues.add(pos.distance);
			}
			break;

		case ISALIVE:
			// Get alive status as double array
			flyPositions.computeIsAlive();
			for (FlyPosition pos : flyPositions.flyPositionList) {
				dataValues.add(pos.bAlive ? 1.0 : 0.0);
			}
			break;

		case SLEEP:
			// Get sleep status as double array
			flyPositions.computeSleep();
			for (FlyPosition pos : flyPositions.flyPositionList) {
				dataValues.add(pos.bSleep ? 1.0 : 0.0);
			}
			break;

		case ELLIPSEAXES:
			// Get ellipse axes
			flyPositions.computeEllipseAxes();
			for (FlyPosition pos : flyPositions.flyPositionList) {
				// Use axis1 (major axis) or could combine both
				dataValues.add(pos.axis1);
			}
			break;

		default:
			// Default: extract Y coordinate
			for (FlyPosition pos : flyPositions.flyPositionList) {
				Point2D center = pos.getCenterRectangle();
				dataValues.add(center.getY());
			}
			break;
		}

		// Apply relative to T0 if needed (not applicable for boolean types)
		if (xlsExportOptions.relativeToT0 && exportType != EnumXLSExport.ISALIVE && exportType != EnumXLSExport.SLEEP) {
			relativeToMaximum();
		}
	}

	public void transferDataValuesToValuesOut(double scalingFactorToPhysicalUnits, EnumXLSExport xlsExport) {
		if (valuesOutLength == 0 || dataValues == null || dataValues.size() < 1)
			return;

		boolean removeZeros = false;
		int len = Math.min(valuesOutLength, dataValues.size());
		if (removeZeros) {
			for (int i = 0; i < len; i++) {
				double ivalue = dataValues.get(i);
				valuesOut[i] = (ivalue == 0 ? Double.NaN : ivalue) * scalingFactorToPhysicalUnits;
			}
		} else {
			for (int i = 0; i < len; i++)
				valuesOut[i] = dataValues.get(i) * scalingFactorToPhysicalUnits;
		}
	}

	public void copyValuesOut(XLSResults sourceRow) {
		if (sourceRow.valuesOut.length != valuesOut.length) {
			this.valuesOutLength = sourceRow.valuesOutLength;
			valuesOut = new double[valuesOutLength];
		}
		for (int i = 0; i < valuesOutLength; i++)
			valuesOut[i] = sourceRow.valuesOut[i];
	}

	public List<Double> relativeToMaximum() {
		if (dataValues == null || dataValues.size() < 1)
			return null;

		double value0 = getMaximum();
		relativeToValue(value0);
		return dataValues;
	}

	public double getMaximum() {
		double maximum = 0.;
		if (dataValues == null || dataValues.size() < 1)
			return maximum;

		maximum = dataValues.get(0);
		for (int index = 0; index < dataValues.size(); index++) {
			double value = dataValues.get(index);
			maximum = Math.max(maximum, value);
		}

		return maximum;
	}

	private void relativeToValue(double value0) {
		for (int index = 0; index < dataValues.size(); index++) {
			double value = dataValues.get(index);
			// dataValues.set(index, ((value0 - value) / value0));
			dataValues.set(index, value / value0);
		}
	}

	boolean subtractDeltaT(int arrayStep, int binStep) {
		if (valuesOut == null || valuesOut.length < 2)
			return false;
		for (int index = 0; index < valuesOut.length; index++) {
			int timeIndex = index * arrayStep + binStep;
			int indexDelta = (int) (timeIndex / arrayStep);
			if (indexDelta < valuesOut.length)
				valuesOut[index] = valuesOut[indexDelta] - valuesOut[index];
			else
				valuesOut[index] = Double.NaN;
		}
		return true;
	}

}
