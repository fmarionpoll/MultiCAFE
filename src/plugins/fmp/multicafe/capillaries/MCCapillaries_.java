package plugins.fmp.multicafe.capillaries;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import icy.gui.component.PopupPanel;
import icy.gui.util.GuiUtil;
import plugins.fmp.multicafe.MultiCAFE;
import plugins.fmp.multicafeSequence.Experiment;
import plugins.fmp.multicafeSequence.SequenceKymosUtils;



public class MCCapillaries_ extends JPanel implements PropertyChangeListener, ChangeListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 853047648249832145L;
	
	JTabbedPane 			tabsPane 		= new JTabbedPane();
	Create 	tabCreate 		= new Create();
	public LoadSave 		tabFile 		= new LoadSave();
	Adjust 	tabAdjust 		= new Adjust();
	Infos		tabInfos		= new Infos();
	private int ID_INFOS = 1;
	private int ID_ADJUST = 2;
	
	private MultiCAFE parent0 = null;

	
	public void init (JPanel mainPanel, String string, MultiCAFE parent0) {
		
		this.parent0 = parent0;
		PopupPanel capPopupPanel = new PopupPanel(string);
		JPanel capPanel = capPopupPanel.getMainPanel();
		capPanel.setLayout(new BorderLayout());
		capPopupPanel.expand();
		mainPanel.add(GuiUtil.besidesPanel(capPopupPanel));
		
		GridLayout capLayout = new GridLayout(3, 1);
		int order = 0;
		tabCreate.init(capLayout, parent0);
		tabCreate.addPropertyChangeListener(this);
		tabsPane.addTab("Create", null, tabCreate, "Create lines defining capillaries");
		order++;
		
		ID_INFOS=order;
		tabInfos.init(capLayout, parent0);
		tabInfos.addPropertyChangeListener(this);
		tabsPane.addTab("Infos", null, tabInfos, "Define pixel conversion unit of images and capillaries content");
		order++;
		
		tabAdjust.init(capLayout, parent0);
		tabAdjust.addPropertyChangeListener(parent0);
		tabsPane.addTab("Adjust", null, tabAdjust, "Adjust ROIS position to the capillaries");
		order++;
		
		tabFile.init(capLayout, parent0);
		tabFile.addPropertyChangeListener(this);
		tabsPane.addTab("Load/Save", null, tabFile, "Load/Save xml file with capillaries descriptors");
		order++;
		
		tabsPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		capPanel.add(GuiUtil.besidesPanel(tabsPane));
		
		tabsPane.addChangeListener(this );
		
		capPopupPanel.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				parent0.mainFrame.revalidate();
				parent0.mainFrame.pack();
				parent0.mainFrame.repaint();
			}
		});
	}
	
	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getPropertyName().equals("CAP_ROIS_OPEN")) {
			setCapillariesInfosToDialogs();
		  	tabsPane.setSelectedIndex(ID_INFOS);
		  	firePropertyChange("CAPILLARIES_OPEN", false, true);
		}			  
		else if (event.getPropertyName().equals("CAP_ROIS_SAVE")) {
			tabsPane.setSelectedIndex(ID_INFOS);
		}
		else if (event.getPropertyName().equals("CAPILLARIES_NEW")) {
			parent0.paneSequence.tabDisplay.viewCapillariesCheckBox.setSelected(true);
			firePropertyChange("CAPILLARIES_NEW", false, true);
			tabsPane.setSelectedIndex(ID_INFOS);
		}

	}
	
	public void displayCapillariesInformation(Experiment exp) {
//		if (exp.seqKymos == null) 
//			exp.seqKymos = new SequenceKymos();

		SwingUtilities.invokeLater(new Runnable() { public void run() {
			SequenceKymosUtils.transferKymoCapillariesToCamData (exp);
			tabInfos.setAllDescriptors(exp.capillaries);
			tabCreate.setGroupingAndNumber(exp.capillaries);
			parent0.paneSequence.tabDisplay.viewCapillariesCheckBox.setSelected(true);
			parent0.paneSequence.tabInfosSeq.setExperimentsInfosToDialog(exp);
			parent0.paneKymos.tabCreate.setBuildKymosParametersToDialog(exp);
		}});
	}
	
	private void setCapillariesInfosToDialogs() {
		Experiment exp = parent0.expList.getCurrentExperiment();
		if (exp != null) {
			SequenceKymosUtils.transferCamDataROIStoKymo(exp);
			exp.capillaries.desc_old.copy(exp.capillaries.desc);
			tabInfos.setAllDescriptors(exp.capillaries);
			tabCreate.setGroupingAndNumber(exp.capillaries);
			parent0.paneSequence.tabInfosSeq.setExperimentsInfosToDialog(exp);
		}
	}
	
	boolean saveCapillaries(Experiment exp) {
		return tabFile.saveCapillaries(exp);
	}
	
	public void getCapillariesInfos(Experiment exp) {
		tabInfos.getDescriptors(exp.capillaries);
		tabCreate.getGrouping(exp.capillaries);
	}

	@Override
	public void stateChanged(ChangeEvent arg0) {
		JTabbedPane tabbedPane = (JTabbedPane) arg0.getSource();
        int selectedIndex = tabbedPane.getSelectedIndex();
        tabAdjust.roisDisplayrefBar(selectedIndex == ID_ADJUST);
        parent0.paneSequence.tabDisplay.viewCapillariesCheckBox.setSelected(selectedIndex == ID_INFOS);
	}

}