package plugins.fmp.multicafe.viewer1D;

import plugins.adufour.vars.gui.VarEditor;
import plugins.adufour.vars.lang.Var;
import plugins.adufour.vars.lang.VarGenericArray;

/**
 * Class defining a var chart with jfreechart
 * 
 * @author Will Ouyang
 * 
 */

public class VarXYChart extends VarGenericArray<double[][]> {
	public VarXYChart(String name, double[][] defaultValue) {
		super(name, double[][].class, defaultValue);
	}

	@Override
	public VarEditor<double[][]> createVarEditor() {
		return new XYChartViewer(this);// VarEditorFactory.getDefaultFactory().createTextField(this);
	}

	@Override
	public Object parseComponent(String s) {
		return Double.parseDouble(s);
	}

	@Override
	public double[][] getValue(boolean forbidNull) {
		// handle the case where the reference is not an array

		@SuppressWarnings("rawtypes")
		Var reference = getReference();

		if (reference == null)
			return super.getValue(forbidNull);

		Object value = reference.getValue();

		if (value == null)
			return super.getValue(forbidNull);

		return (double[][]) value;
	}
}
