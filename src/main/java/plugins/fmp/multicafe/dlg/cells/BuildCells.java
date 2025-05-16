package plugins.fmp.multicafe.dlg.cells;

import java.awt.GridLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import plugins.fmp.multicafe.MultiCAFE;

public class BuildCells extends JPanel implements PropertyChangeListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	BuildCellsAsArray tabBuildCells1 = new BuildCellsAsArray();
	BuildCellsFromContours tabBuildCells2 = new BuildCellsFromContours();
	JTabbedPane tabsPane = new JTabbedPane();
	int iTAB_CAGES1 = 0;
	int iTAB_CAGES2 = 1;
	MultiCAFE parent0 = null;

	public void init(GridLayout capLayout, MultiCAFE parent0) {
		this.parent0 = parent0;

		createTabs(capLayout);
		add(tabsPane);

		tabsPane.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				int selectedIndex = tabsPane.getSelectedIndex();
				displayOverlay(selectedIndex == iTAB_CAGES2);
			}
		});

		tabsPane.setSelectedIndex(0);
	}

	void createTabs(GridLayout capLayout) {
		tabsPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

		int iTab = 0;
		iTAB_CAGES1 = iTab;
		tabBuildCells1.init(capLayout, parent0);
		tabBuildCells1.addPropertyChangeListener(this);
		tabsPane.addTab("Define array cols/rows", null, tabBuildCells1, "Build cells as an array of rectangles");

		iTab++;
		iTAB_CAGES2 = iTab;
		tabBuildCells2.init(capLayout, parent0);
		tabBuildCells2.addPropertyChangeListener(this);
		tabsPane.addTab("Detect contours of cells", null, tabBuildCells2, "Detect contours to build cells");
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
//		JTabbedPane sourceTabbedPane = (JTabbedPane) evt.getSource();
		int index = tabsPane.getSelectedIndex();
		displayOverlay(index == 1);
	}

	private void displayOverlay(boolean activateOverlay) {
		tabBuildCells2.overlayCheckBox.setSelected(activateOverlay);
	}
}
