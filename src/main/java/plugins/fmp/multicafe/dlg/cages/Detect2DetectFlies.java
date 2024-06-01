package plugins.fmp.multicafe.dlg.cages;

import java.awt.GridLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import plugins.fmp.multicafe.MultiCAFE;

public class Detect2DetectFlies extends JPanel implements PropertyChangeListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	Detect2 tabDetect2 = new Detect2();
	Detect2BuildBackground tabBackground = new Detect2BuildBackground();
	JTabbedPane tabsPane = new JTabbedPane();
	int previouslySelected = -1;

	int iTAB_BACKGND = 0;
	int iTAB_DETECT2 = 1;

	MultiCAFE parent0 = null;

	public void init(GridLayout capLayout, MultiCAFE parent0) {
		this.parent0 = parent0;

		createTabs();
		tabsPane.setSelectedIndex(0);
		add(tabsPane);

		tabsPane.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				int selectedIndex = tabsPane.getSelectedIndex();
				previouslySelected = selectedIndex;
			}
		});
	}

	void createTabs() {
		GridLayout capLayout = new GridLayout(4, 1);
//		tabsPane.setTabPlacement(JTabbedPane.LEFT);
		tabsPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

		int iTab = 0;
		iTAB_BACKGND = iTab;
		tabBackground.init(capLayout, parent0);
		tabBackground.addPropertyChangeListener(this);
		tabsPane.addTab("Build background" + "", null, tabBackground, "Build background without flies");

		iTab++;
		iTAB_DETECT2 = iTab;
		tabDetect2.init(capLayout, parent0);
		tabDetect2.addPropertyChangeListener(this);
		tabsPane.addTab("Detect flies", null, tabDetect2, "Detect flies position using background subtraction");
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		// TODO Auto-generated method stub

	}

}
