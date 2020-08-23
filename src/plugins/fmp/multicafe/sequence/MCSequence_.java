package plugins.fmp.multicafe.sequence;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.nio.file.Paths;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import icy.gui.component.PopupPanel;
import icy.gui.util.GuiUtil;
import icy.gui.viewer.Viewer;
import icy.preferences.XMLPreferences;
import icy.system.thread.ThreadUtil;
import plugins.fmp.multicafeSequence.SequenceNameListRenderer;
import plugins.fmp.multicafe.MultiCAFE;
import plugins.fmp.multicafeSequence.Experiment;
import plugins.fmp.multicafeSequence.ExperimentList;
import plugins.fmp.multicafeSequence.SequenceCamData;



public class MCSequence_ extends JPanel implements PropertyChangeListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = -6826269677524125173L;
	
	private JTabbedPane 		tabsPane 		= new JTabbedPane();
	public Open 			tabOpen 		= new Open();
	public Infos			tabInfosSeq		= new Infos();
	public Intervals	tabIntervals	= new Intervals();
	public Display			tabDisplay 		= new Display();
	public Close 			tabClose 		= new Close();
	private JLabel				text 			= new JLabel("Experiment ");
	private JButton  			previousButton	= new JButton("<");
	private JButton				nextButton		= new JButton(">");
	public JComboBox <String>			expListComboBox	= new JComboBox <String>();
	private MultiCAFE 			parent0 		= null;
	
	
	
	public void init (JPanel mainPanel, String string, MultiCAFE parent0) {
		this.parent0 = parent0;
		
		SequenceNameListRenderer renderer = new SequenceNameListRenderer();
		expListComboBox.setRenderer(renderer);
		int bWidth = 28;
		int height = 20;
		previousButton.setPreferredSize(new Dimension(bWidth, height));
		nextButton.setPreferredSize(new Dimension(bWidth, height));
		
		FlowLayout flowLayout = new FlowLayout(FlowLayout.LEFT);
		flowLayout.setVgap(0);
		JPanel leftPanel = new JPanel(flowLayout);
		leftPanel.add(text);
		leftPanel.add(previousButton);
		
		BorderLayout borderlayout1 = new BorderLayout();
		JPanel sequencePanel = new JPanel(borderlayout1);
		sequencePanel.add(leftPanel, BorderLayout.LINE_START);
		
		sequencePanel.add(nextButton, BorderLayout.LINE_END);
		sequencePanel.add(expListComboBox, BorderLayout.CENTER);
		expListComboBox.setPrototypeDisplayValue("XXXXXXXXxxxxxxxxxxxxxxxxx______________XXXXXXXXXXXXXXXXXXX");
		
		PopupPanel capPopupPanel = new PopupPanel(string);			
		capPopupPanel.expand();
		mainPanel.add(GuiUtil.besidesPanel(capPopupPanel));
		GridLayout tabsLayout = new GridLayout(3, 1);
		
		tabOpen.init(tabsLayout, parent0);
		tabsPane.addTab("Open/Add", null, tabOpen, "Open one or several stacks of .jpg files");
		tabOpen.addPropertyChangeListener(this);
		
		tabInfosSeq.init(tabsLayout, parent0);
		tabsPane.addTab("Infos", null, tabInfosSeq, "Define infos for this experiment/box");
		tabInfosSeq.addPropertyChangeListener(this);
		
		tabIntervals.init(tabsLayout, parent0);
		tabsPane.addTab("Intervals", null, tabIntervals, "Browse and analysis parameters");
		tabIntervals.addPropertyChangeListener(this);

		tabDisplay.init(tabsLayout, parent0);
		tabsPane.addTab("Display", null, tabDisplay, "Display ROIs");
		tabDisplay.addPropertyChangeListener(this);

		tabClose.init(tabsLayout, parent0);
		tabsPane.addTab("Close", null, tabClose, "Close file and associated windows");
		tabClose.addPropertyChangeListener(this);

		tabsPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		
		JPanel capPanel = capPopupPanel.getMainPanel();
		BorderLayout borderlayout2 = new BorderLayout();
		capPanel.setLayout(borderlayout2);
		capPanel.add(sequencePanel, BorderLayout.PAGE_START);
		capPanel.add(tabsPane, BorderLayout.PAGE_END);	
		
		capPopupPanel.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				parent0.mainFrame.revalidate();
				parent0.mainFrame.pack();
				parent0.mainFrame.repaint();
			}
		});

		defineActionListeners();		
	}
	
	private void defineActionListeners() {
		expListComboBox.addActionListener(new ActionListener () { @Override public void actionPerformed( final ActionEvent e ) { 
			if (expListComboBox.getItemCount() == 0 || tabInfosSeq.disableChangeFile) {
				updateBrowseInterface();
				return;
			}
			Experiment exp = parent0.expList.getCurrentExperiment();
			if (exp == null)
				return;
			String oldtext = exp.seqCamData.getDirectory();
			File oldFile = new File( oldtext );
			if (oldFile.isFile())
				oldFile = oldFile.getParentFile();
			
			String newtext = (String) expListComboBox.getSelectedItem();
			File newFile = new File(newtext);
			if (newFile.isFile())
				newFile = newFile.getParentFile();
			
			if (!newtext.contentEquals(oldtext)) {
        		ThreadUtil.bgRun( new Runnable() { @Override public void run() {
	        		parent0.paneSequence.tabClose.closeExp(exp); 
        		}});
        		tabInfosSeq.updateCombos();
				parent0.expList.currentExperimentIndex = parent0.expList.getPositionOfCamFileName(newtext);						
				openSequenceCamFromCombo();
			}
			updateBrowseInterface();
		}});
		
		nextButton.addActionListener(new ActionListener () { @Override public void actionPerformed( final ActionEvent e ) { 
			if (expListComboBox.getSelectedIndex() < (expListComboBox.getItemCount() -1)) 
				expListComboBox.setSelectedIndex(expListComboBox.getSelectedIndex()+1);
			updateBrowseInterface();
		}});
		
		previousButton.addActionListener(new ActionListener () { @Override public void actionPerformed( final ActionEvent e ) { 
			if (expListComboBox.getSelectedIndex() > 0) 
				expListComboBox.setSelectedIndex(expListComboBox.getSelectedIndex()-1);
			updateBrowseInterface();
		}});
	}
	
	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getPropertyName() .equals ("SEQ_OPENFILE")) {
			tabClose.closeAll();
			expListComboBox.removeAllItems();
			expListComboBox.updateUI();
			openSeqCamData();
		}
		else if (event.getPropertyName().equals("SEQ_ADDFILE")) {
			tabClose.closeCurrentExperiment();
			openSeqCamData();
		}
		else if (event.getPropertyName().equals("SEQ_CLOSE")) {
			System.out.println("SEQ_CLOSE");
		}
		else if (event.getPropertyName().equals("CLOSE_ALL")) {
			tabsPane.setSelectedIndex(0);
			expListComboBox.removeAllItems();
			expListComboBox.updateUI();
		}
		else if (event.getPropertyName().equals("SEARCH_CLOSED")) {
			int index = expListComboBox.getSelectedIndex();
			if (index < 0)
				index = 0;
			tabInfosSeq.disableChangeFile = true;
			for (String name: tabOpen.selectedNames) {
				 addStringToCombo(name);
			}
			tabOpen.selectedNames.clear();
			if (expListComboBox.getItemCount() > 0) {
				expListComboBox.setSelectedIndex(index);
				updateBrowseInterface();
				tabInfosSeq.disableChangeFile = false;
				openSequenceCamFromCombo();
			}
		}

	}
	
	private void openSeqCamData() {
		Experiment exp = parent0.openExperimentFromString(null);
		SequenceCamData seqCamData = exp.seqCamData;
		if (seqCamData != null && seqCamData.seq != null) {
			tabInfosSeq.disableChangeFile = true;
			int item = addStringToCombo( seqCamData.getDirectory());
			seqCamData.closeSequence();
			expListComboBox.setSelectedIndex(item);
			updateBrowseInterface();
			tabInfosSeq.disableChangeFile = false;
			openSequenceCamFromCombo();	
		}
	}
	
	public void openExperiment(Experiment exp) {
		exp.xmlLoadExperiment();
		exp.seqCamData = exp.openSequenceCamData(exp.getExperimentFileName());
		if (exp.seqCamData != null && exp.seqCamData.seq != null) {
			parent0.addSequence(exp.seqCamData.seq);
			updateViewerForSequenceCam(exp.seqCamData);
			loadMeasuresAndKymos();
			parent0.paneKymos.tabDisplay.updateResultsAvailable(exp);
		}
	}
	
	void openSequenceCamFromCombo() {
		Experiment exp = parent0.openExperimentFromString((String) expListComboBox.getSelectedItem());
		parent0.updateDialogsAfterOpeningSequenceCam(exp);
		ThreadUtil.bgRun( new Runnable() { @Override public void run() {  
			loadMeasuresAndKymos();
		}});
		tabsPane.setSelectedIndex(1);
	}
	
	private int addStringToCombo(String strItem) {
		int item = findIndexItemInCombo(strItem);
		if(item < 0) { 
			expListComboBox.addItem(strItem);
			item = findIndexItemInCombo(strItem);
		}
		return item;
	}
	
	private int findIndexItemInCombo(String strItem) {
		int nitems = expListComboBox.getItemCount();
		int item = -1;
		for (int i=0; i < nitems; i++) {
			if (strItem.equalsIgnoreCase(expListComboBox.getItemAt(i))) {
				item = i;
				break;
			}
		}
		return item;
	}
	
	boolean addSequenceCamToCombo() {
		Experiment exp = parent0.expList.getCurrentExperiment();
		if (exp == null)
			return false;
		String filename = exp.seqCamData.getSequenceFileName();
		if (filename == null) 
			return false;
		String strItem = Paths.get(filename).toString();
		if (strItem != null) {
			addStringToCombo(strItem);
			expListComboBox.setSelectedItem(strItem);
			XMLPreferences guiPrefs = parent0.getPreferences("gui");
			guiPrefs.put("lastUsedPath", strItem);
		}
		return true;
	}
				
	void updateViewerForSequenceCam(SequenceCamData seqCamData) {
		Viewer v = seqCamData.seq.getFirstViewer();
		if (v != null) {
			Rectangle rectv = v.getBoundsInternal();
			Rectangle rect0 = parent0.mainFrame.getBoundsInternal();
			rectv.setLocation(rect0.x+ rect0.width, rect0.y);
			v.setBounds(rectv);
			v.toFront();
			v.requestFocus();
			v.addListener( parent0 );
		}
	}
	
	public void transferSequenceCamDataToDialogs(Experiment exp) {
		tabIntervals.setEndFrameToDialog((int)exp.seqCamData.seqAnalysisEnd);
		updateViewerForSequenceCam(exp.seqCamData);
		parent0.paneKymos.tabDisplay.updateResultsAvailable(exp);
	}

	void loadMeasuresAndKymos() {
		parent0.loadPreviousMeasures(
					tabOpen.isCheckedLoadPreviousProfiles(), 
					tabOpen.isCheckedLoadKymographs(),
					tabOpen.isCheckedLoadCages(),
					tabOpen.isCheckedLoadMeasures());
	}
	
	public void getExperimentInfosFromDialog(Experiment exp) {
		tabInfosSeq.getExperimentInfosFromDialog(exp);
		tabIntervals.getAnalyzeFrameFromDialog (exp);
	}
	
	void updateBrowseInterface() {
		int isel = expListComboBox.getSelectedIndex();		
	    boolean flag1 = (isel == 0? false: true);
		boolean flag2 = (isel == (expListComboBox.getItemCount() -1)? false: true);
		previousButton.setEnabled(flag1);
		nextButton.setEnabled(flag2);
	}
	
	public void transferExperimentNamesToExpList(ExperimentList expList, boolean clearOldList) {
		if (clearOldList)
			expList.clear();
		int nitems = expListComboBox.getItemCount();
		for (int i=0; i< nitems; i++) {
			String filename = expListComboBox.getItemAt(i);
			Experiment exp = expList.addNewExperiment(filename);
			exp.xmlLoadExperiment();
		}
	}

	public Experiment getSelectedExperimentFromCombo() {
		String newtext = (String) parent0.paneSequence.expListComboBox.getSelectedItem();
		parent0.expList.currentExperimentIndex = parent0.expList.getPositionOfCamFileName(newtext);	
		return parent0.expList.getCurrentExperiment();
	}
}