package plugins.fmp.multicafe2.dlg.experiment;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import icy.gui.viewer.Viewer;
import icy.roi.ROI;
import icy.sequence.Sequence;
import icy.sequence.SequenceEvent;
import icy.sequence.SequenceListener;
import icy.sequence.SequenceEvent.SequenceEventSourceType;
import icy.gui.frame.progress.ProgressFrame;

import plugins.fmp.multicafe2.MultiCAFE2;
import plugins.fmp.multicafe2.dlg.JComponents.SequenceNameListRenderer;
import plugins.fmp.multicafe2.experiment.Experiment;
import plugins.fmp.multicafe2.experiment.ExperimentDirectories;




public class LoadSave extends JPanel implements PropertyChangeListener, ItemListener, SequenceListener 
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -690874563607080412L;
	
	private JButton 		createButton	= new JButton("Create...");
	private JButton 		openButton		= new JButton("Open...");
	private JButton			searchButton 	= new JButton("Search...");
	private JButton			closeButton		= new JButton("Close");
	protected JCheckBox		filteredCheck	= new JCheckBox("List filtered");
	
	public List<String> 	selectedNames 	= new ArrayList<String> ();
	private SelectFiles1 	dialogSelect 	= null;
	
	private JButton  		previousButton	= new JButton("<");
	private JButton			nextButton		= new JButton(">");

	private MultiCAFE2 		parent0 		= null;
	private MCExperiment_ 	parent1 		= null;
	
	

	public JPanel initPanel( MultiCAFE2 parent0, MCExperiment_ parent1) 
	{
		this.parent0 = parent0;
		this.parent1 = parent1;

		SequenceNameListRenderer renderer = new SequenceNameListRenderer();
		parent0.expListCombo.setRenderer(renderer);
		int bWidth = 30; 
		int height = 20;
		previousButton.setPreferredSize(new Dimension(bWidth, height));
		nextButton.setPreferredSize(new Dimension(bWidth, height));
		
		JPanel sequencePanel0 = new JPanel(new BorderLayout());
		sequencePanel0.add(previousButton, BorderLayout.LINE_START);
		sequencePanel0.add(parent0.expListCombo, BorderLayout.CENTER);
		sequencePanel0.add(nextButton, BorderLayout.LINE_END);
		
		JPanel sequencePanel = new JPanel(new BorderLayout());
		FlowLayout layout = new FlowLayout(FlowLayout.LEFT);
		layout.setVgap(1);
		JPanel subPanel = new JPanel(layout);
		subPanel.add(openButton);
		subPanel.add(createButton);
		subPanel.add(searchButton);
		subPanel.add(closeButton);
		subPanel.add(filteredCheck);
		sequencePanel.add(subPanel, BorderLayout.LINE_START);
	
		defineActionListeners();
		parent0.expListCombo.addItemListener(this);
		
		JPanel twoLinesPanel = new JPanel (new GridLayout(2, 1));
		twoLinesPanel.add(sequencePanel0);
		twoLinesPanel.add(sequencePanel);

		return twoLinesPanel;
	}
	
	@Override
	public void propertyChange(PropertyChangeEvent evt) 
	{
		if (evt.getPropertyName().equals("SELECT1_CLOSED")) 
		{
			parent1.tabInfos.disableChangeFile = true;
			if (selectedNames.size() < 1)
				return;
			
			ExperimentDirectories eDAF0 = new ExperimentDirectories(); 
			if (eDAF0.getDirectoriesFromExptPath(parent0.expListCombo, selectedNames.get(0), null))
			{
				final int item = addExperimentFrom3NamesAnd2Lists(eDAF0);
	        	final String binSubDirectory = parent0.expListCombo.expListBinSubDirectory;
	        	
	        	SwingUtilities.invokeLater(new Runnable() { public void run() 
				{	
		        	parent1.tabInfos.disableChangeFile = false;
		        	for (int i = 1; i < selectedNames.size(); i++) 
					{
						ExperimentDirectories eDAF = new ExperimentDirectories(); 
						if (eDAF.getDirectoriesFromExptPath(parent0.expListCombo, selectedNames.get(i), binSubDirectory))
							addExperimentFrom3NamesAnd2Lists(eDAF);
					}
					selectedNames.clear();
					updateBrowseInterface();
			     	parent1.tabInfos.disableChangeFile = true;
			     	parent1.tabInfos.initInfosCombos(); 
			     	parent0.expListCombo.setSelectedIndex(item);
			     	Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
					if (exp != null)
						parent1.tabInfos.transferPreviousExperimentInfosToDialog(exp, exp);
				}});
			}
		}
	}
	
	@Override
	public void itemStateChanged(ItemEvent e) 
	{
		if (e.getStateChange() == ItemEvent.SELECTED) 
		{
			openExperimentFromCombo();
		} 
		else if (e.getStateChange() == ItemEvent.DESELECTED) 
		{
			Experiment exp = (Experiment) e.getItem();
        	closeViewsForCurrentExperiment(exp); 
		}
	}
	
	void closeAllExperiments() 
	{
		closeCurrentExperiment();
		parent0.expListCombo.removeAllItems();
		parent1.tabFilter.clearAllCheckBoxes ();
		parent1.tabFilter.filterExpList.removeAllItems();
		parent1.tabInfos.clearCombos();
		filteredCheck.setSelected(false);
	}
	
	public void closeViewsForCurrentExperiment(Experiment exp) 
	{
		if (exp != null) 
		{
			if (exp.seqCamData != null) 
				exp.xmlSaveMCExperiment();
			exp.closeSequences();
		}
		parent0.paneKymos.tabDisplay.kymographsCombo.removeAllItems();
	}
	
	public void closeCurrentExperiment() 
	{
		if (parent0.expListCombo.getSelectedIndex() < 0)
			return;
		Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
		if (exp != null)
			closeViewsForCurrentExperiment(exp);
	}
	
	void updateBrowseInterface() 
	{
		int isel = parent0.expListCombo.getSelectedIndex();		
	    boolean flag1 = (isel == 0? false: true);
		boolean flag2 = (isel == (parent0.expListCombo.getItemCount() -1)? false: true);
		previousButton.setEnabled(flag1);
		nextButton.setEnabled(flag2);
	}
	
	boolean openExperimentFromCombo() 
	{
		JProgressBar progressBar = new JProgressBar();
		progressBar.setString("Load Data");
        // ----------------------- TODO
		long start, end;
		System.out.println("---------------------------openExperimentFromCombo():" );
        start = System.nanoTime();
        // -----------------------
        
		final Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
		if (exp == null)
			return false;
		
		exp.xmlLoadMCExperiment();
		// ----------------------- TODO
		end = System.nanoTime();
		System.out.println("openExperimentFromCombo(): xmloadMCExperiment: " + (end - start) / 1000000 + " milliseconds");
		start = end;
		// -----------------------
		
		boolean flag = true;
		exp.seqCamData.loadFirstImage();
		exp.seqCamData.seq.addListener(this);
		if (exp.seqCamData != null) 
		{
			progressBar.setString("Load data: get capillaries");
			loadCamCapillariesThread(exp);
			loadCamCagesThread(exp);
			progressBar.setString("Load data: get images");
			loadCamImagesThread(exp);
			progressBar.setString("Load data: get kymographs");
			loadKymoImagesThread(exp);		
			
			exp.seqKymos.loadFirstImage();
			if (exp.seqKymos != null) {	
				loadKymoMeasuresThread(exp);
				loadKymoImagesThread(exp);
			}
			progressBar.setString("Load data: update dialogs");
			parent1.updateViewerForSequenceCam(exp);
			parent1.updateExpDialogs(exp);
			parent0.paneCapillaries.updateDialogs(exp);
			parent0.paneLevels.updateDialogs(exp);
			
			// ----------------------- TODO
			end = System.nanoTime();
			System.out.println("openExperimentFromCombo(): updateDialogs: " + (end - start) / 1000000 + " milliseconds");
			start = end;
	        // -----------------------
		}
		else 
		{
			flag = false;
			System.out.println("Error: no jpg files found for this experiment\n");
		}
		parent1.tabInfos.transferPreviousExperimentInfosToDialog(exp, exp);

		return flag;
	}
	
	private void loadCamImagesThread(Experiment exp) {
		SwingUtilities.invokeLater(new Runnable() { 
		    public void run() {
		    	long start, end;
				start = System.nanoTime();
		        // -----------------------
				ArrayList<ROI> listROIs = exp.seqCamData.seq.getROIs();
				exp.loadCamDataImages();
				// ----------------------- TODO
				end = System.nanoTime();
				System.out.println("->loadCamImages: " + (end - start) / 1000000 + " milliseconds");
		        // -----------------------
				if (listROIs.size() > 0)
					exp.seqCamData.seq.addROIs(listROIs, false);
		    }});
	}
	
	private void loadCamCapillariesThread(Experiment exp) {
		java.awt.EventQueue.invokeLater(new Runnable() {
		    public void run() {
		    	long start, end;
				start = System.nanoTime();
		        // -----------------------
				exp.loadCamDataCapillaries();
				// ----------------------- TODO
				end = System.nanoTime();
				System.out.println("->loadCamCapillaries: " + (end - start) / 1000000 + " milliseconds");
				start = end;
		        // -----------------------
		    }});
	}
	
	private void loadCamCagesThread(Experiment exp)
	{
		java.awt.EventQueue.invokeLater(new Runnable() {
		    public void run() {
		    	long start, end;
				start = System.nanoTime();
		        // -----------------------
				exp.xmlReadDrosoTrack(null);
				// ----------------------- TODO
				end = System.nanoTime();
				System.out.println("->loadCamCages: " + (end - start) / 1000000 + " milliseconds");
				start = end;
		        // -----------------------
		    }});
	}
	
	private void loadKymoImagesThread(Experiment exp)
	{
		java.awt.EventQueue.invokeLater(new Runnable() {
		    public void run() {
		    	long start, end;
				start = System.nanoTime();
		        // -----------------------
				ArrayList<ROI> listROIs = exp.seqKymos.seq.getROIs();
				loadKymos(exp);
				if (listROIs.size() > 0)
					exp.seqKymos.seq.addROIs(listROIs, false);
				// ----------------------- TODO
				end = System.nanoTime();
				System.out.println("->loadCamCages: " + (end - start) / 1000000 + " milliseconds");
				start = end;
		        // -----------------------
		    }});
	}
	
	private void loadKymoMeasuresThread(Experiment exp) {
		if (parent1.tabOptions.measuresCheckBox.isSelected() )
		{
			SwingUtilities.invokeLater(new Runnable() { 
			    public void run() {
			    	// ----------------------- TODO
			    	long start, end;
					start = System.nanoTime();
			        // -----------------------
			        loadMeasures(exp);
			        // ----------------------- TODO
					end = System.nanoTime();
					System.out.println("->loadKymoImages: " + (end - start) / 1000000 + " milliseconds");
					start = end;
					// ----------------------- TODO
					if (parent0.paneExperiment.tabOptions.graphsCheckBox.isSelected())
						displayGraphs(exp);
			    }});
		}
	}
	
	private boolean loadKymos(Experiment exp) 
	{
		boolean flag = parent0.paneKymos.tabFile.loadDefaultKymos(exp);
		parent0.paneKymos.updateDialogs(exp);
		return flag;
	}
	
	private boolean loadMeasures(Experiment exp) 
	{
		boolean flag = parent0.paneLevels.tabFileLevels.loadCapillaries_Measures(exp);
		parent0.paneLevels.updateDialogs(exp);
		return flag;
	}
		
	private void displayGraphs(Experiment exp) 
	{
		SwingUtilities.invokeLater(new Runnable() { public void run() 
		{
			parent0.paneLevels.tabGraphs.displayGraphsPanels(exp);
		}});	
	}
	
	// ------------------------
	
	private void defineActionListeners() 
	{
		parent0.expListCombo.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{ 
				updateBrowseInterface();
			}});
		
		nextButton.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{ 
			// -----------------------
			long start, end;
			System.out.println("---------------------------next:" );
			start = System.nanoTime();
	        // -----------------------    
			if (parent0.expListCombo.getSelectedIndex() < (parent0.expListCombo.getItemCount()-1)) {
				System.out.println("ExpmtDlg: LoadSave");
				parent0.expListCombo.setSelectedIndex(parent0.expListCombo.getSelectedIndex()+1);
			}
			else {
				System.out.println("ExpmtDlg: updateBrowseInterface ");
				updateBrowseInterface();
			}
			
			// -----------------------
			end = System.nanoTime();
			System.out.println("next end: " + (end - start) / 1000000 + " milliseconds");
			start = end;
	       // -----------------------
			}});
		
		previousButton.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{ 
			if (parent0.expListCombo.getSelectedIndex() > 0) 
				parent0.expListCombo.setSelectedIndex(parent0.expListCombo.getSelectedIndex()-1);
			else 
				updateBrowseInterface();
			}});
		
		searchButton.addActionListener(new ActionListener()  
		{
            @Override
            public void actionPerformed(ActionEvent arg0) 
            {
            	selectedNames = new ArrayList<String> ();
            	dialogSelect = new SelectFiles1();
            	dialogSelect.initialize(parent0);
            }});
		
		createButton.addActionListener(new ActionListener()  
		{
            @Override
            public void actionPerformed(ActionEvent arg0) 
            {
            	ExperimentDirectories eDAF = new ExperimentDirectories(); 
            	if (eDAF.getDirectoriesFromDialog(parent0.expListCombo, null, true)) 
            	{
	            	int item = addExperimentFrom3NamesAnd2Lists(eDAF);
	            	parent1.tabInfos.initInfosCombos();
	            	parent0.expListCombo.setSelectedIndex(item);
            	}
            }});
		
		openButton.addActionListener(new ActionListener()  
		{
            @Override
            public void actionPerformed(ActionEvent arg0) 
            {
            	ExperimentDirectories eDAF = new ExperimentDirectories(); 
            	if (eDAF.getDirectoriesFromDialog(parent0.expListCombo, null, false)) 
            	{
            		int item = addExperimentFrom3NamesAnd2Lists(eDAF);
            		parent1.tabInfos.initInfosCombos();
            		parent0.expListCombo.setSelectedIndex(item);
            	}
            }});
		
		closeButton.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{ 
				closeAllExperiments();
				parent1.tabsPane.setSelectedIndex(0);
				parent0.expListCombo.removeAllItems();
				parent0.expListCombo.updateUI();
			}});
		
		filteredCheck.addActionListener(new ActionListener()  
		{
            @Override
            public void actionPerformed(ActionEvent arg0) 
            {
            	parent1.tabFilter.filterExperimentList(filteredCheck.isSelected());
            }});
	}
	
	private int addExperimentFrom3NamesAnd2Lists(ExperimentDirectories eDAF) 
	{
		Experiment exp = new Experiment (eDAF);
		int item = parent0.expListCombo.addExperiment(exp, false);
		return item;
	}

	@Override
	public void sequenceChanged(SequenceEvent sequenceEvent) {

		if (sequenceEvent.getSourceType() == SequenceEventSourceType.SEQUENCE_DATA )
		{
			Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
			if (exp != null)
			{
				if (exp.seqCamData.seq != null 
				&& sequenceEvent.getSequence() == exp.seqCamData.seq)
				{
					Viewer v = exp.seqCamData.seq.getFirstViewer();
					int t = v.getPositionT(); 
					v.setTitle(exp.seqCamData.getDecoratedImageName(t));
				}
				else if (exp.seqKymos.seq != null 
					&& sequenceEvent.getSequence() == exp.seqKymos.seq)
				{
					Viewer v = exp.seqKymos.seq.getFirstViewer();
					int t = v.getPositionT(); 
					String title = parent0.paneKymos.tabDisplay.getKymographTitle(t);
					v.setTitle(title);
				}
			}
		}
	}

	@Override
	public void sequenceClosed(Sequence sequence) {
		sequence.removeListener(this);
	}

	
}
