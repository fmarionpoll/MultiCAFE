package plugins.fmp.multicafe.fmp_tools.chart.strategies;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;

import plugins.fmp.multicafe.fmp_tools.chart.ChartCageBuild;
import plugins.fmp.multicafe.fmp_tools.results.EnumResults;
import plugins.fmp.multicafe.fmp_tools.results.ResultsOptions;

/**
 * UI controls factory that provides a combobox for selecting result types
 * and a legend panel at the bottom. This is used for the levels dialog.
 */
public class ComboBoxUIControlsFactory implements ChartUIControlsFactory {
	
	private JComboBox<EnumResults> resultTypeComboBox;
	private JComboBox<EnumResults> parentComboBox;
	private JPanel bottomPanel;
	private EnumResults[] measurementTypes;
	
	/**
	 * Sets the parent combobox for synchronization.
	 * 
	 * @param comboBox the parent combobox
	 */
	public void setParentComboBox(JComboBox<EnumResults> comboBox) {
		this.parentComboBox = comboBox;
		if (comboBox != null) {
			ComboBoxModel<EnumResults> model = comboBox.getModel();
			int size = model.getSize();
			EnumResults[] types = new EnumResults[size];
			for (int i = 0; i < size; i++) {
				types[i] = model.getElementAt(i);
			}
			this.measurementTypes = types;
		}
	}
	
	/**
	 * Sets the available measurement types.
	 * 
	 * @param types the measurement types
	 */
	public void setMeasurementTypes(EnumResults[] types) {
		this.measurementTypes = types;
	}
	
	@Override
	public JPanel createTopPanel(ResultsOptions currentOptions, ActionListener changeListener) {
		JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		
		EnumResults[] typesToUse = getMeasurementTypes();
		resultTypeComboBox = new JComboBox<EnumResults>(typesToUse);
		if (currentOptions != null && currentOptions.resultType != null) {
			resultTypeComboBox.setSelectedItem(currentOptions.resultType);
		}
		
		resultTypeComboBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				EnumResults selectedType = (EnumResults) resultTypeComboBox.getSelectedItem();
				if (selectedType != null && currentOptions != null) {
					currentOptions.resultType = selectedType;
					
					// Synchronize with parent combobox if it exists
					if (parentComboBox != null && parentComboBox.getSelectedItem() != selectedType) {
						ActionListener[] listeners = parentComboBox.getActionListeners();
						for (ActionListener listener : listeners) {
							parentComboBox.removeActionListener(listener);
						}
						parentComboBox.setSelectedItem(selectedType);
						for (ActionListener listener : listeners) {
							parentComboBox.addActionListener(listener);
						}
					}
					
					// Notify the change listener
					if (changeListener != null) {
						changeListener.actionPerformed(e);
					}
				}
			}
		});
		
		topPanel.add(resultTypeComboBox);
		return topPanel;
	}
	
	@Override
	public JPanel createBottomPanel(ResultsOptions currentOptions) {
		bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		updateBottomPanel(currentOptions);
		return bottomPanel;
	}
	
	@Override
	public void updateControls(EnumResults newResultType, ResultsOptions currentOptions) {
		if (resultTypeComboBox != null && newResultType != null) {
			resultTypeComboBox.setSelectedItem(newResultType);
		}
		updateBottomPanel(currentOptions);
	}
	
	private void updateBottomPanel(ResultsOptions currentOptions) {
		if (bottomPanel == null || currentOptions == null) {
			return;
		}
		
		bottomPanel.removeAll();
		if (ChartCageBuild.isLRType(currentOptions.resultType)) {
			bottomPanel.add(new LegendItem("Sum", Color.BLUE));
			bottomPanel.add(new LegendItem("PI", Color.RED));
		} else {
			bottomPanel.add(new LegendItem("L", Color.BLUE));
			bottomPanel.add(new LegendItem("R", Color.RED));
		}
		bottomPanel.revalidate();
		bottomPanel.repaint();
	}
	
	private EnumResults[] getMeasurementTypes() {
		if (measurementTypes != null && measurementTypes.length > 0) {
			return measurementTypes;
		}
		// Fallback default list
		return new EnumResults[] { 
			EnumResults.TOPRAW,
			EnumResults.TOPLEVEL,
			EnumResults.BOTTOMLEVEL,
			EnumResults.TOPLEVEL_LR,
			EnumResults.DERIVEDVALUES,
			EnumResults.SUMGULPS,
			EnumResults.SUMGULPS_LR
		};
	}
	
	/**
	 * Gets the result type combobox for external access.
	 * 
	 * @return the combobox
	 */
	public JComboBox<EnumResults> getResultTypeComboBox() {
		return resultTypeComboBox;
	}
	
	/**
	 * Simple legend item component.
	 */
	private static class LegendItem extends JComponent {
		private static final long serialVersionUID = 1L;
		private String text;
		private Color color;

		public LegendItem(String text, Color color) {
			this.text = text;
			this.color = color;
			setPreferredSize(new Dimension(100, 20));
		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			g.setColor(color);
			g.drawLine(0, 10, 20, 10);
			g.setColor(Color.BLACK);
			g.drawString(text, 25, 15);
		}
	}
}

