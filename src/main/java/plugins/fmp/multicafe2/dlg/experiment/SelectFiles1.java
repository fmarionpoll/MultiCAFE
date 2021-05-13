package plugins.fmp.multicafe2.dlg.experiment;


import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;

import icy.gui.frame.IcyFrame;
import icy.gui.util.GuiUtil;
import icy.preferences.XMLPreferences;
import plugins.fmp.multicafe2.MultiCAFE2;
import plugins.fmp.multicafe2.experiment.Experiment;


public class SelectFiles1 extends JPanel 
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 4172927636287523049L;
	IcyFrame 			dialogFrame 			= null;	
	private JComboBox<String> filterCombo		= new JComboBox <String>(new String[] { "capillarytrack", "multicafe", "roisline", "grabs", "MCcapillaries", "MCexperiment"} );
	private JButton 	findButton				= new JButton("Select root directory and search...");
	private JButton 	clearSelectedButton		= new JButton("Clear selected");
	private JButton 	clearAllButton			= new JButton("Clear all");
	private JButton 	addSelectedButton		= new JButton("Add selected");
	private JButton 	addAllButton			= new JButton("Add all");
	private JList<String> directoriesJList		= new JList<String>(new DefaultListModel<String>());
	private MultiCAFE2 	parent0 				= null;
	private LoadSave 	parent1 				= null;

	
	
	public void initialize (MultiCAFE2 parent0) 
	{
		filterCombo.setEditable(true);
		this.parent0 = parent0;
		this.parent1 = parent0.paneExperiment.panelLoadSave;
		addPropertyChangeListener(parent1);

		dialogFrame = new IcyFrame ("Select files", true, true);
		JPanel mainPanel = GuiUtil.generatePanelWithoutBorder();
		dialogFrame.setLayout(new BorderLayout());
		dialogFrame.add(mainPanel, BorderLayout.CENTER);
		
		mainPanel.add(GuiUtil.besidesPanel(findButton, filterCombo));
		filterCombo.setSelectedIndex(5);
		
		directoriesJList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		directoriesJList.setLayoutOrientation(JList.VERTICAL);
		directoriesJList.setVisibleRowCount(20);
		JScrollPane scrollPane = new JScrollPane(directoriesJList);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		mainPanel.add(GuiUtil.besidesPanel(scrollPane));
	
		mainPanel.add(GuiUtil.besidesPanel(clearSelectedButton, clearAllButton));
		mainPanel.add(GuiUtil.besidesPanel(addSelectedButton, addAllButton));
		
		addActionListeners();
		
		dialogFrame.pack();
		dialogFrame.addToDesktopPane();
		dialogFrame.requestFocus();
		dialogFrame.center();
		dialogFrame.setVisible(true);
	}
	
	void close() 
	{
		dialogFrame.close();
	}
	
	void addActionListeners() 
	{
		findButton.addActionListener(new ActionListener()  
		{
            @Override
            public void actionPerformed(ActionEvent arg0)  
            {	
    			final String pattern = (String) filterCombo.getSelectedItem();
   	    		getListofFilesMatchingPattern(pattern);
            }});

		clearSelectedButton.addActionListener(new ActionListener()  
		{
            @Override
            public void actionPerformed(ActionEvent arg0) 
            {
            	List<String> selectedItems = directoriesJList.getSelectedValuesList();
    		    removeListofNamesFromList (selectedItems);
            }});
		clearAllButton.addActionListener(new ActionListener()  
		{
            @Override
            public void actionPerformed(ActionEvent arg0) 
            {
            	((DefaultListModel<String>) directoriesJList.getModel()).removeAllElements();
            }});
		addSelectedButton.addActionListener(new ActionListener()  
		{
            @Override
            public void actionPerformed(ActionEvent arg0) 
            {
            	List<String> selectedItems = directoriesJList.getSelectedValuesList();
    			addNamesToSelectedList(selectedItems);
    			removeListofNamesFromList(selectedItems);
    			firePropertyChange("SELECT1_CLOSED", false, true);
    			close();
            }});
		addAllButton.addActionListener(new ActionListener()  
		{
            @Override
            public void actionPerformed(ActionEvent arg0) 
            {
    			List<String> allItems = new ArrayList <String> (directoriesJList.getModel().getSize());
    			for(int i = 0; i< directoriesJList.getModel().getSize();i++) 
    			{
    			    String name = directoriesJList.getModel().getElementAt(i);
    				allItems.add(name);
    				}
    			addNamesToSelectedList(allItems);
    			((DefaultListModel<String>) directoriesJList.getModel()).removeAllElements();
    			firePropertyChange("SELECT1_CLOSED", false, true);
    			close();
            }});
	}

	private void removeListofNamesFromList(List<String> selectedItems) 
	{
		for (String oo: selectedItems)
	    	 ((DefaultListModel<String>) directoriesJList.getModel()).removeElement(oo);
	}
	
	private void setPreferencesPath(String pathString)
	{
		XMLPreferences guiPrefs = parent0.getPreferences("gui");
		guiPrefs.put("lastUsedPath", pathString);
	}
	
	private String getPreferencesPath()
	{
		XMLPreferences guiPrefs = parent0.getPreferences("gui");
		return guiPrefs.get("lastUsedPath", "");
	}
	
 	private void getListofFilesMatchingPattern(String pattern) 
 	{
		File dir = chooseDirectory(getPreferencesPath());
		if (dir == null) 
			return;
		
		final String lastUsedPathString = dir.getAbsolutePath();
		setPreferencesPath(lastUsedPathString);
		Path lastPath = Paths.get(lastUsedPathString);
		boolean option1 = true;
		
		if (option1) 
		{
			List<Path> result = null;
	        try (Stream<Path> walk = Files.walk(lastPath)) {
	            result = walk
	                    .filter(Files::isRegularFile)   // is a file
	                    .filter(p -> p.getFileName().toString().contains(pattern))
	                    .collect(Collectors.toList());
	        } catch (IOException e) {
				e.printStackTrace();
			}
	        if (result != null)
	        	for (Path path: result)
	        		addNameToListIfNew(path.toString());
		}
		else 
		{
			final String patternLowerCase = pattern.toLowerCase();
			try 
			{
				if (Files.exists(lastPath)) 
				{
					Files.walk(lastPath)
					.filter(Files::isRegularFile)		
					.forEach((f)->{
					    String fileName = f.toString().toLowerCase();
					    if( fileName.contains(patternLowerCase)) 
					    	addNameToListIfNew(fileName);
					});
				}
			} 
			catch (IOException e) 
			{
				e.printStackTrace();
			}
		}
	}
	
	private void addNameToListIfNew(String fileName) 
	{	
		int ilast = ((DefaultListModel<String>) directoriesJList.getModel()).getSize();
		boolean found = false;
		for (int i=0; i < ilast; i++) 
		{
			String oo = ((DefaultListModel<String>) directoriesJList.getModel()).getElementAt(i);
			if (oo.equalsIgnoreCase (fileName)) 
			{
				found = true;
				break;
			}
		}
		if (!found)
			((DefaultListModel<String>) directoriesJList.getModel()).addElement(fileName);
	}
	
	private File chooseDirectory(String rootdirectory) 
	{
		File dummy_selected = null;
		JFileChooser fc = new JFileChooser(); 
		if (rootdirectory != null)
			fc.setCurrentDirectory(new File(rootdirectory));
	    fc.setDialogTitle("Select a root directory...");
	    fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
	    fc.setAcceptAllFileFilterUsed(false);
	    if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) 
	    { 
	      dummy_selected = fc.getSelectedFile();
	    }
	    else 
	    {
	      System.out.println("No directory selected ");
	    }
		return dummy_selected;
	}
	
	private void addNamesToSelectedList(List<String> stringList) 
	{
		for (String name : stringList) 
		{
			String directoryName = Paths.get(name).getParent().toString();
			if (isDirectoryWithJpg(directoryName))
					parent1.selectedNames.add(directoryName);
		}
	}

	private boolean isDirectoryWithJpg(String directoryName) {
		String imageDirectory = Experiment.getImagesDirectoryAsParentFromFileName(directoryName); 
//		HashSet <String> hSet = Directories.getDirectoriesWithFilesType (imageDirectory, ".jpg");
		File dir = new File(imageDirectory);
		File[] files = dir.listFiles((d, name) -> name.endsWith(".jpg"));
		boolean flag = (files.length > 0);
		return flag;
	}

}
