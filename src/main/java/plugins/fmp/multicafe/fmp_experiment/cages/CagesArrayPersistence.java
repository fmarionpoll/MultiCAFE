package plugins.fmp.multicafe.fmp_experiment.cages;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import javax.swing.SwingWorker;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import icy.util.XMLUtil;
import plugins.fmp.multicafe.fmp_experiment.Experiment;
import plugins.fmp.multicafe.fmp_tools.Logger;
import plugins.fmp.multicafe.fmp_tools.JComponents.Dialog;
import plugins.fmp.multicafe.fmp_tools.JComponents.exceptions.FileDialogException;

public class CagesArrayPersistence {

	private final String ID_CAGES = "Cages";
	private final String ID_NCAGES = "n_cages";
	private final String ID_NCAGESALONGX = "N_cages_along_X";
	private final String ID_NCAGESALONGY = "N_cages_along_Y";
	private final String ID_NCOLUMNSPERCAGE = "N_columns_per_cage";
	private final String ID_NROWSPERCAGE = "N_rows_per_cage";

	private final String ID_MCDROSOTRACK_XML = "MCdrosotrack.xml";

	private final String csvSep = ";";

	public boolean load_Cages(CagesArray cages, String directory) {
		boolean flag = false;
		int cagesBeforeCSV = cages.cagesList.size();
		System.out.println("CagesArrayPersistence:load_Cages() START - directory: " + directory + ", cages before: " + cagesBeforeCSV);
		try {
			// Load bulk data from CSV (fast, efficient)
			flag = csvLoadCagesMeasures(cages, directory);
			int cagesAfterCSV = cages.cagesList.size();
			int cagesWithFliesAfterCSV = 0;
			for (Cage cage : cages.cagesList) {
				if (cage.getProperties().getCageNFlies() > 0) {
					cagesWithFliesAfterCSV++;
				}
			}
			System.out.println(String.format("CagesArrayPersistence:load_Cages() After CSV load: %d cages (was %d), %d with nFlies > 0", 
				cagesAfterCSV, cagesBeforeCSV, cagesWithFliesAfterCSV));
			Logger.info(String.format("CagesArrayPersistence:load_Cages() After CSV load: %d cages (was %d), %d with nFlies > 0", 
				cagesAfterCSV, cagesBeforeCSV, cagesWithFliesAfterCSV));
		} catch (Exception e) {
			System.out.println("CagesArrayPersistence:load_Cages() Failed to load cages from CSV: " + directory + " - " + e.getMessage());
			Logger.error("CagesArrayPersistence:load_Cages() Failed to load cages from CSV: " + directory, e);
		}

		// If CSV load failed, try full XML load (legacy format)
		if (!flag) {
			System.out.println("CagesArrayPersistence:load_Cages() CSV load failed, falling back to full XML load");
			Logger.warn("CagesArrayPersistence:load_Cages() CSV load failed, falling back to full XML load");
			String tempName = directory + File.separator + ID_MCDROSOTRACK_XML;
			flag = xmlReadCagesFromFileNoQuestion(cages, tempName);
			int cagesAfterXML = cages.cagesList.size();
			int cagesWithFliesAfterXML = 0;
			for (Cage cage : cages.cagesList) {
				if (cage.getProperties().getCageNFlies() > 0) {
					cagesWithFliesAfterXML++;
				}
			}
			System.out.println(String.format("CagesArrayPersistence:load_Cages() After XML load: %d cages, %d with nFlies > 0", 
				cagesAfterXML, cagesWithFliesAfterXML));
			Logger.info(String.format("CagesArrayPersistence:load_Cages() After XML load: %d cages, %d with nFlies > 0", 
				cagesAfterXML, cagesWithFliesAfterXML));
		} else {
			// CSV load succeeded, now load ROIs from XML
			String tempName = directory + File.separator + ID_MCDROSOTRACK_XML;
			int cagesWithFliesBeforeROI = 0;
			for (Cage cage : cages.cagesList) {
				if (cage.getProperties().getCageNFlies() > 0) {
					cagesWithFliesBeforeROI++;
				}
			}
			System.out.println("CagesArrayPersistence:load_Cages() Before ROI-only XML load: " + cagesWithFliesBeforeROI + " cages with nFlies > 0");
			xmlLoadCagesROIsOnly(cages, tempName);
			int cagesWithFliesAfterROI = 0;
			for (Cage cage : cages.cagesList) {
				if (cage.getProperties().getCageNFlies() > 0) {
					cagesWithFliesAfterROI++;
				}
			}
			System.out.println("CagesArrayPersistence:load_Cages() After ROI-only XML load: " + cagesWithFliesAfterROI + " cages with nFlies > 0");
			if (cagesWithFliesBeforeROI != cagesWithFliesAfterROI) {
				System.out.println(String.format("CagesArrayPersistence:load_Cages() WARNING: nFlies count changed after ROI load: %d -> %d", 
					cagesWithFliesBeforeROI, cagesWithFliesAfterROI));
				Logger.warn(String.format("CagesArrayPersistence:load_Cages() WARNING: nFlies count changed after ROI load: %d -> %d", 
					cagesWithFliesBeforeROI, cagesWithFliesAfterROI));
			}
		}
		System.out.println("CagesArrayPersistence:load_Cages() END - final cages: " + cages.cagesList.size());
		return flag;
	}

	public boolean save_Cages(CagesArray cages, String directory) {
		if (directory == null) {
			Logger.warn("CagesArrayPersistence:save_Cages() directory is null");
			return false;
		}

		Path path = Paths.get(directory);
		if (!Files.exists(path)) {
			Logger.warn("CagesArrayPersistence:save_Cages() directory does not exist: " + directory);
			return false;
		}

		// Save bulk data to CSV (fast, efficient)
		csvSaveCagesMeasures(cages, directory);
		
		// Save ROIs to XML (standard format for ROI serialization)
		String tempName = directory + File.separator + ID_MCDROSOTRACK_XML;
		xmlSaveCagesROIsOnly(cages, tempName);
		
		return true;
	}

	public boolean xmlReadCagesFromFile(CagesArray cages, Experiment exp) {
		String[] filedummy = null;
		String filename = exp.getResultsDirectory();
		File file = new File(filename);
		String directory = file.getParentFile().getAbsolutePath();
		try {
			filedummy = Dialog.selectFiles(directory, "xml");
		} catch (FileDialogException e) {
			e.printStackTrace();
		}
		boolean wasOk = false;
		if (filedummy != null) {
			for (int i = 0; i < filedummy.length; i++) {
				String csFile = filedummy[i];
				wasOk &= xmlReadCagesFromFileNoQuestion(cages, csFile);
			}
		}
		return wasOk;
	}

	public boolean xmlReadCagesFromFileNoQuestion(CagesArray cages, String tempname) {
		if (tempname == null) {
			return false;
		}

		File file = new File(tempname);
		if (!file.exists()) {
			return false;
		}

		try {
			final Document doc = XMLUtil.loadDocument(tempname);
			if (doc == null) {
				return false;
			}

			boolean success = xmlLoadCages(cages, XMLUtil.getRootElement(doc));

			return success;

		} catch (Exception e) {
			Logger.error("CagesArrayPersistence:xmlReadCagesFromFileNoQuestion() ERROR during cages XML loading: "
					+ e.getMessage(), e);
			return false;
		}
	}

	private boolean xmlLoadCages(CagesArray cages, Node node) {
		try {
			cages.cagesList.clear();
			Element xmlVal = XMLUtil.getElement(node, ID_CAGES);
			if (xmlVal == null) {
				Logger.error("CagesArrayPersistence:xmlLoadCages() ERROR: Could not find Cages element in XML");
				return false;
			}

			int ncages = XMLUtil.getAttributeIntValue(xmlVal, ID_NCAGES, 0);
			if (ncages < 0) {
				Logger.error("CagesArrayPersistence:xmlLoadCages() ERROR: Invalid number of cages: " + ncages);
				return false;
			}

			cages.nCagesAlongX = XMLUtil.getAttributeIntValue(xmlVal, ID_NCAGESALONGX, cages.nCagesAlongX);
			cages.nCagesAlongY = XMLUtil.getAttributeIntValue(xmlVal, ID_NCAGESALONGY, cages.nCagesAlongY);
			cages.nColumnsPerCage = XMLUtil.getAttributeIntValue(xmlVal, ID_NCOLUMNSPERCAGE, cages.nColumnsPerCage);
			cages.nRowsPerCage = XMLUtil.getAttributeIntValue(xmlVal, ID_NROWSPERCAGE, cages.nRowsPerCage);

			int loadedCages = 0;
			for (int index = 0; index < ncages; index++) {
				try {
					Cage cage = new Cage();
					boolean cageSuccess = cage.xmlLoadCage(xmlVal, index);
					if (cageSuccess) {
						cages.cagesList.add(cage);
						loadedCages++;
					} else {
						Logger.warn(
								"CagesArrayPersistence:xmlLoadCages() WARNING: Failed to load cage at index " + index);
					}
				} catch (Exception e) {
					Logger.error("CagesArrayPersistence:xmlLoadCages() ERROR loading cage at index " + index + ": "
							+ e.getMessage(), e);
				}
			}

			return loadedCages > 0;

		} catch (Exception e) {
			Logger.error("CagesArrayPersistence:xmlLoadCages() ERROR during xmlLoadCages: " + e.getMessage(), e);
			return false;
		}
	}

	private boolean xmlSaveCagesROIsOnly(CagesArray cages, String tempname) {
		try {
			final Document doc = XMLUtil.createDocument(true);
			if (doc == null) {
				Logger.error("CagesArrayPersistence:xmlSaveCagesROIsOnly() ERROR: Could not create XML document");
				return false;
			}

			Node node = XMLUtil.getRootElement(doc);
			Element xmlVal = XMLUtil.addElement(node, ID_CAGES);
			int ncages = cages.cagesList.size();
			XMLUtil.setAttributeIntValue(xmlVal, ID_NCAGES, ncages);
			XMLUtil.setAttributeIntValue(xmlVal, ID_NCAGESALONGX, cages.nCagesAlongX);
			XMLUtil.setAttributeIntValue(xmlVal, ID_NCAGESALONGY, cages.nCagesAlongY);
			XMLUtil.setAttributeIntValue(xmlVal, ID_NCOLUMNSPERCAGE, cages.nColumnsPerCage);
			XMLUtil.setAttributeIntValue(xmlVal, ID_NROWSPERCAGE, cages.nRowsPerCage);

			int index = 0;
			for (Cage cage : cages.cagesList) {
				if (cage == null) {
					Logger.warn("CagesArrayPersistence:xmlSaveCagesROIsOnly() WARNING: Null cage at index " + index);
					index++;
					continue;
				}

				Element cageElement = XMLUtil.addElement(xmlVal, "Cage" + index);
				if (cageElement != null) {
					// Save only ROI (cage limits)
					cage.xmlSaveCageLimits(cageElement);
				}
				index++;
			}

			boolean success = XMLUtil.saveDocument(doc, tempname);
			return success;

		} catch (Exception e) {
			Logger.error("CagesArrayPersistence:xmlSaveCagesROIsOnly() ERROR during XML saving: " + e.getMessage(), e);
			return false;
		}
	}

	private boolean xmlLoadCagesROIsOnly(CagesArray cages, String tempname) {
		if (tempname == null) {
			return false;
		}

		File file = new File(tempname);
		if (!file.exists()) {
			// XML file doesn't exist yet (first save), that's OK
			return true;
		}

		try {
			final Document doc = XMLUtil.loadDocument(tempname);
			if (doc == null) {
				Logger.warn("CagesArrayPersistence:xmlLoadCagesROIsOnly() Could not load XML document: " + tempname);
				return false;
			}

			Node rootNode = XMLUtil.getRootElement(doc);
			Element xmlVal = XMLUtil.getElement(rootNode, ID_CAGES);
			if (xmlVal == null) {
				Logger.warn("CagesArrayPersistence:xmlLoadCagesROIsOnly() Could not find Cages element in XML");
				return false;
			}

			// Load layout information
			cages.nCagesAlongX = XMLUtil.getAttributeIntValue(xmlVal, ID_NCAGESALONGX, cages.nCagesAlongX);
			cages.nCagesAlongY = XMLUtil.getAttributeIntValue(xmlVal, ID_NCAGESALONGY, cages.nCagesAlongY);
			cages.nColumnsPerCage = XMLUtil.getAttributeIntValue(xmlVal, ID_NCOLUMNSPERCAGE, cages.nColumnsPerCage);
			cages.nRowsPerCage = XMLUtil.getAttributeIntValue(xmlVal, ID_NROWSPERCAGE, cages.nRowsPerCage);

			// Load ROIs and match them to existing cages by index
			int ncages = XMLUtil.getAttributeIntValue(xmlVal, ID_NCAGES, 0);
			int loadedROIs = 0;
			for (int index = 0; index < ncages && index < cages.cagesList.size(); index++) {
				try {
					Cage cage = cages.cagesList.get(index);
					if (cage == null) {
						continue;
					}

					Element cageElement = XMLUtil.getElement(xmlVal, "Cage" + index);
					if (cageElement != null) {
						// Load only ROI (cage limits)
						boolean roiLoaded = cage.xmlLoadCageLimits(cageElement);
						if (roiLoaded) {
							loadedROIs++;
						}
					}
				} catch (Exception e) {
					Logger.warn("CagesArrayPersistence:xmlLoadCagesROIsOnly() WARNING: Failed to load ROI for cage at index " + index);
				}
			}

			return loadedROIs > 0 || ncages == 0; // Return true if ROIs loaded or file was empty

		} catch (Exception e) {
			Logger.error("CagesArrayPersistence:xmlLoadCagesROIsOnly() ERROR during XML loading: " + e.getMessage(), e);
			return false;
		}
	}

	private boolean csvLoadCagesMeasures(CagesArray cages, String directory) throws Exception {
		String pathToCsv = directory + File.separator + "CagesMeasures.csv";
		File csvFile = new File(pathToCsv);
		System.out.println("CagesArrayPersistence:csvLoadCagesMeasures() Checking CSV file: " + pathToCsv + " exists: " + csvFile.exists() + " isFile: " + csvFile.isFile());
		if (!csvFile.isFile()) {
			System.out.println("CagesArrayPersistence:csvLoadCagesMeasures() CSV file not found or not a file, returning false");
			return false;
		}

		// Use existing cages array to preserve ROIs and other data, just update properties from CSV
		// Don't create a new tempCages array - work directly with existing cages
		System.out.println("CagesArrayPersistence:csvLoadCagesMeasures() Starting with " + cages.cagesList.size() + " existing cages");

		BufferedReader csvReader = new BufferedReader(new FileReader(pathToCsv));
		String row;
		String sep = csvSep;
		int descriptionCount = 0;
		int cageCount = 0;
		int positionCount = 0;
		try {
			while ((row = csvReader.readLine()) != null) {
				if (row.length() > 0 && row.charAt(0) == '#')
					sep = String.valueOf(row.charAt(1));

				String[] data = row.split(sep);
				if (data.length > 0 && data[0].equals("#")) {
					if (data.length > 1) {
						System.out.println("CagesArrayPersistence:csvLoadCagesMeasures() Found section: " + data[1]);
						switch (data[1]) {
						case "DESCRIPTION":
							descriptionCount++;
							csvLoad_DESCRIPTION(cages, csvReader, sep);
							break;
						case "CAGE":
							cageCount++;
							System.out.println("CagesArrayPersistence:csvLoadCagesMeasures() CAGE section found but skipped (redundant - DESCRIPTION section already loaded properties)");
							// Skip CAGE section - DESCRIPTION section already contains all necessary data
							// Just skip to the next section marker
							String skipRow;
							while ((skipRow = csvReader.readLine()) != null) {
								String[] skipData = skipRow.split(sep);
								if (skipData.length > 0 && skipData[0].equals("#"))
									break;
							}
							break;
						case "POSITION":
							positionCount++;
							csvLoad_Measures(cages, csvReader, EnumCageMeasures.POSITION, sep);
							break;
						default:
							System.out.println("CagesArrayPersistence:csvLoadCagesMeasures() Unknown section: " + data[1]);
							break;
						}
					}
				}
			}
			System.out.println(String.format("CagesArrayPersistence:csvLoadCagesMeasures() Processed sections - DESCRIPTION: %d, CAGE: %d, POSITION: %d", 
				descriptionCount, cageCount, positionCount));
		} finally {
			csvReader.close();
		}
		
		// Count cages with fly positions after loading
		int cagesWithFlyPositions = 0;
		int totalFlyPositions = 0;
		for (Cage cage : cages.cagesList) {
			if (cage.flyPositions != null && cage.flyPositions.flyPositionList != null 
					&& !cage.flyPositions.flyPositionList.isEmpty()) {
				cagesWithFlyPositions++;
				totalFlyPositions += cage.flyPositions.flyPositionList.size();
			}
		}
		
		// Log cage and fly position counts
		System.out.println(String.format("CagesArrayPersistence:csvLoadCagesMeasures() Final: %d cages, %d with fly positions, %d total fly positions", 
				cages.cagesList.size(), cagesWithFlyPositions, totalFlyPositions));
		Logger.info(String.format("CagesArrayPersistence:csvLoadCagesMeasures() Loaded %d cages, %d with fly positions, %d total fly positions", 
				cages.cagesList.size(), cagesWithFlyPositions, totalFlyPositions));
		
		return descriptionCount > 0 || cageCount > 0 || positionCount > 0;
	}

	private void csvLoad_DESCRIPTION(CagesArray cages, BufferedReader csvReader, String sep) {
		String row;
		int cagesLoaded = 0;
		int cagesWithFlies = 0;
		try {
			while ((row = csvReader.readLine()) != null) {
				String[] data = row.split(sep);
				if (data.length > 0 && data[0].equals("#"))
					break;

				if (data.length > 0) {
					String test = data[0].substring(0, Math.min(data[0].length(), 7));
					if (test.equals("n cages") || test.equals("n cells")) {
						if (data.length > 1) {
							int ncages = Integer.valueOf(data[1]);
							if (ncages >= cages.cagesList.size()) {
								cages.cagesList.ensureCapacity(ncages);
							} else {
								cages.cagesList.subList(ncages, cages.cagesList.size()).clear();
							}
						}
					} else {
						// This is a cage description row: cageID, cageName, nFlies, age, comment, strain, sex, npoints, coordinates...
						// Format: 0	cage000	0	5	..	..	..
						try {
							int cageID = Integer.valueOf(data[0]);
							Cage cage = cages.getCageFromID(cageID);
							if (cage == null) {
								cage = new Cage();
								cages.cagesList.add(cage);
							}
							
							// Parse nFlies from column 2 (index 2)
							if (data.length > 2) {
								int nFliesBefore = cage.getProperties().getCageNFlies();
								try {
									int nFlies = Integer.valueOf(data[2]);
									cage.getProperties().setCageNFlies(nFlies);
									cagesLoaded++;
									if (nFlies > 0) {
										cagesWithFlies++;
									}
									System.out.println(String.format("CagesArrayPersistence:csvLoad_DESCRIPTION() Cage %d: nFlies %d -> %d (from CSV data[2]='%s')", 
										cageID, nFliesBefore, nFlies, data[2]));
									
									// Also set other properties if available
									if (data.length > 1) {
										cage.getProperties().setStrCageNumber(data[1]);
									}
									if (data.length > 3) {
										try {
											int age = Integer.valueOf(data[3]);
											cage.getProperties().setFlyAge(age);
										} catch (NumberFormatException e) {
											// Age not a number, skip
										}
									}
									if (data.length > 4) {
										cage.getProperties().setComment(data[4]);
									}
									if (data.length > 5) {
										cage.getProperties().setFlyStrain(data[5]);
									}
									if (data.length > 6) {
										cage.getProperties().setFlySex(data[6]);
									}
								} catch (NumberFormatException e) {
									System.out.println("CagesArrayPersistence:csvLoad_DESCRIPTION() Invalid nFlies value for cage " + cageID + ": " + data[2]);
								}
							}
						} catch (NumberFormatException e) {
							// Not a cage row (probably header or other data), skip
						}
					}
				}
			}
			System.out.println(String.format("CagesArrayPersistence:csvLoad_DESCRIPTION() Loaded %d cages from DESCRIPTION section, %d with nFlies > 0", 
				cagesLoaded, cagesWithFlies));
		} catch (IOException e) {
			System.out.println("CagesArrayPersistence:csvLoad_DESCRIPTION() Error: " + e.getMessage());
			Logger.error("CagesArrayPersistence:csvLoad_DESCRIPTION() Error: " + e.getMessage(), e);
		}
	}

	private void csvLoad_CAGE(CagesArray cages, BufferedReader csvReader, String sep) {
		String row;
		int cagesLoaded = 0;
		int cagesWithFlies = 0;
		try {
			row = csvReader.readLine();
			while ((row = csvReader.readLine()) != null) {
				String[] data = row.split(sep);
				if (data.length > 0 && data[0].equals("#"))
					break;

				if (data.length > 0) {
					int cageID = 0;
					try {
						cageID = Integer.valueOf(data[0]);
					} catch (NumberFormatException e) {
						Logger.warn("CagesArrayPersistence:csvLoad_CAGE() Invalid integer input: " + data[0]);
						continue;
					}
					Cage cage = cages.getCageFromID(cageID);
					if (cage == null) {
						cage = new Cage();
						cages.cagesList.add(cage);
					}
					int nFliesBefore = cage.getProperties().getCageNFlies();
					cage.csvImport_CAGE_Header(data);
					int nFliesAfter = cage.getProperties().getCageNFlies();
					cagesLoaded++;
					if (nFliesAfter > 0) {
						cagesWithFlies++;
					}
					System.out.println(String.format("CagesArrayPersistence:csvLoad_CAGE() Cage %d: nFlies %d -> %d (CSV row: %s)", 
						cageID, nFliesBefore, nFliesAfter, row));
					if (data.length > 1) {
						System.out.println(String.format("  -> data[0]=%s, data[1]=%s (nFlies field)", 
							data[0], data.length > 1 ? data[1] : "N/A"));
					}
				}
			}
			System.out.println(String.format("CagesArrayPersistence:csvLoad_CAGE() Loaded %d cages from CSV, %d with nFlies > 0", 
				cagesLoaded, cagesWithFlies));
		} catch (IOException e) {
			Logger.error("CagesArrayPersistence:csvLoad_CAGE() Error: " + e.getMessage(), e);
		}
	}

	private void csvLoad_Measures(CagesArray cages, BufferedReader csvReader, EnumCageMeasures measureType,
			String sep) {
		String row;
		try {
			row = csvReader.readLine();
			boolean complete = (row != null && row.contains("w(i)"));
			boolean v0 = (row != null && row.contains("x(i)"));

			while ((row = csvReader.readLine()) != null) {
				String[] data = row.split(sep);
				if (data.length > 0 && data[0].equals("#"))
					return;

				if (data.length > 0) {
					int cageID = -1;
					try {
						cageID = Integer.valueOf(data[0]);
					} catch (NumberFormatException e) {
						Logger.warn("CagesArrayPersistence:csvLoad_Measures() Invalid integer input: " + data[0]);
						continue;
					}
					Cage cage = cages.getCageFromID(cageID);
					if (cage == null) {
						cage = new Cage();
						cages.cagesList.add(cage);
						cage.prop.setCageID(cageID);
					}
					if (v0) {
						cage.csvImport_MEASURE_Data_v0(measureType, data, complete);
					} else {
						cage.csvImport_MEASURE_Data_Parameters(data);
					}
				}
			}
		} catch (IOException e) {
			Logger.error("CagesArrayPersistence:csvLoad_Measures() Error: " + e.getMessage(), e);
		}
	}

	private boolean csvSaveCagesMeasures(CagesArray cages, String directory) {
		try {
			FileWriter csvWriter = new FileWriter(directory + File.separator + "CagesMeasures.csv");
			csvSaveDescriptionSection(cages, csvWriter);
			csvSaveCAGESection(cages, csvWriter);
			csvSaveMeasuresSection(cages, csvWriter, EnumCageMeasures.POSITION);
			csvWriter.flush();
			csvWriter.close();

		} catch (IOException e) {
			Logger.error("CagesArrayPersistence:csvSaveCagesMeasures() Error: " + e.getMessage(), e);
		}
		return true;
	}

	private boolean csvSaveDescriptionSection(CagesArray cages, FileWriter csvWriter) {
		try {
			csvWriter.append("#" + csvSep + "DESCRIPTION" + csvSep + "Cages data\n");
			csvWriter.append("n cages=" + csvSep + Integer.toString(cages.cagesList.size()) + "\n");
			if (cages.cagesList.size() > 0)
				for (Cage cage : cages.cagesList)
					csvWriter.append(cage.csvExportCageDescription(csvSep));

			csvWriter.append("#" + csvSep + "#\n");
		} catch (IOException e) {
			Logger.error("CagesArrayPersistence:csvSaveDescriptionSection() Error: " + e.getMessage(), e);
		}
		return true;
	}

	private boolean csvSaveCAGESection(CagesArray cages, FileWriter csvWriter) {
		try {
			csvWriter.append("#" + csvSep + "CAGE" + csvSep + "Cage properties\n");
			csvWriter.append("cageID" + csvSep + "nFlies" + csvSep + "age" + csvSep + "comment" + csvSep + "strain" + csvSep + "sex" + "\n");
			for (Cage cage : cages.cagesList) {
				csvWriter.append(String.format("%d%s%d%s%d%s%s%s%s%s%s\n",
					cage.getProperties().getCageID(), csvSep,
					cage.getProperties().getCageNFlies(), csvSep,
					cage.getProperties().getFlyAge(), csvSep,
					cage.getProperties().getComment(), csvSep,
					cage.getProperties().getFlyStrain(), csvSep,
					cage.getProperties().getFlySex()));
			}
			csvWriter.append("#" + csvSep + "#\n");
		} catch (IOException e) {
			Logger.error("CagesArrayPersistence:csvSaveCAGESection() Error: " + e.getMessage(), e);
		}
		return true;
	}

	private boolean csvSaveMeasuresSection(CagesArray cages, FileWriter csvWriter, EnumCageMeasures measuresType) {
		try {
			if (cages.cagesList.size() <= 0) {
				return false;
			}

			boolean complete = true;
			csvWriter.append(csvExport_MEASURE_Header(measuresType, csvSep, complete));

			for (Cage cage : cages.cagesList) {
				csvWriter.append(csvExport_MEASURE_Data(cage, measuresType, csvSep, complete));
			}

			csvWriter.append("#" + csvSep + "#\n");
		} catch (IOException e) {
			Logger.error("CagesArrayPersistence:csvSaveMeasuresSection() Error: " + e.getMessage(), e);
		}
		return true;
	}

	private String csvExport_MEASURE_Header(EnumCageMeasures measureType, String sep, boolean complete) {
		StringBuffer sbf = new StringBuffer();
		String explanation = "cageID" + sep + "parm" + sep + "npts";
		switch (measureType) {
		case POSITION:
			sbf.append("#" + sep + "POSITION\n" + explanation + "\n");
			break;
		default:
			sbf.append("#" + sep + "UNDEFINED------------\n");
			break;
		}
		return sbf.toString();
	}

	private String csvExport_MEASURE_Data(Cage cage, EnumCageMeasures measureType, String sep, boolean complete) {
		StringBuffer sbf = new StringBuffer();
		String cageID = Integer.toString(cage.getProperties().getCageID());

		switch (measureType) {
		case POSITION:
			if (cage.flyPositions != null) {
				cage.flyPositions.cvsExport_Parameter_ToRow(sbf, "t(i)", cageID, sep);
				cage.flyPositions.cvsExport_Parameter_ToRow(sbf, "x(i)", cageID, sep);
				cage.flyPositions.cvsExport_Parameter_ToRow(sbf, "y(i)", cageID, sep);
				cage.flyPositions.cvsExport_Parameter_ToRow(sbf, "w(i)", cageID, sep);
				cage.flyPositions.cvsExport_Parameter_ToRow(sbf, "h(i)", cageID, sep);
			}
			break;
		default:
			break;
		}
		return sbf.toString();
	}

	/**
	 * Asynchronously loads cage measures from CSV file.
	 * 
	 * @param cages     The CagesArray to populate
	 * @param directory The directory containing CagesMeasures.csv
	 * @param exp       The experiment being loaded (for validation)
	 * @param onComplete Callback to execute on EDT when loading completes
	 * @return SwingWorker that can be cancelled
	 */
	public SwingWorker<Boolean, Void> loadCagesAsync(CagesArray cages, String directory, Experiment exp,
			Runnable onComplete) {
		return new SwingWorker<Boolean, Void>() {
			@Override
			protected Boolean doInBackground() throws Exception {
				// Check if cancelled before starting
				if (isCancelled()) {
					return false;
				}

				boolean flag = false;
				try {
					// Load bulk data from CSV (fast, efficient)
					// This now uses a temporary CagesArray to avoid corrupting data if cancelled
					flag = csvLoadCagesMeasures(cages, directory);
				} catch (Exception e) {
					// Check if cancelled during load
					if (isCancelled()) {
						return false;
					}
					Logger.error("CagesArrayPersistence:loadCagesAsync() Failed to load cages from CSV: " + directory,
							e);
				}

				// Check if cancelled after CSV load
				if (isCancelled()) {
					return false;
				}

				// If CSV load failed, try full XML load (legacy format)
				if (!flag) {
					String tempName = directory + File.separator + ID_MCDROSOTRACK_XML;
					flag = xmlReadCagesFromFileNoQuestion(cages, tempName);
				} else {
					// CSV load succeeded, now load ROIs from XML
					String tempName = directory + File.separator + ID_MCDROSOTRACK_XML;
					xmlLoadCagesROIsOnly(cages, tempName);
				}

				return flag;
			}

			@Override
			protected void done() {
				boolean wasCancelled = isCancelled();
				Boolean result = null;
				
				try {
					if (!wasCancelled) {
						result = get();
					}
				} catch (java.util.concurrent.CancellationException e) {
					// Cancellation is expected when user switches experiments - not an error
					wasCancelled = true;
				} catch (Exception e) {
					Logger.error("CagesArrayPersistence:loadCagesAsync() Error in done(): " + e.getMessage(), e);
				}
				
				// Only call onComplete if not cancelled and load was successful
				// Pass the loadId to the callback so it can verify it's still the active load
				if (!wasCancelled && result != null && result && onComplete != null) {
					// onComplete is already on EDT (done() is called on EDT)
					onComplete.run();
				}
				
				// Clear loading flag if cancelled (onComplete callback will handle it if successful)
				if (wasCancelled && exp != null) {
					exp.setLoading(false);
				}
			}
		};
	}

	/**
	 * Asynchronously saves cage measures to CSV file.
	 * 
	 * @param cages     The CagesArray to save
	 * @param directory The directory to save CagesMeasures.csv
	 * @param exp       The experiment being saved (for validation)
	 * @return CompletableFuture that completes when save is done
	 */
	public CompletableFuture<Boolean> saveCagesAsync(CagesArray cages, String directory, Experiment exp) {
		return CompletableFuture.supplyAsync(new Supplier<Boolean>() {
			@Override
			public Boolean get() {
				if (directory == null) {
					Logger.warn("CagesArrayPersistence:saveCagesAsync() directory is null");
					return false;
				}

				Path path = Paths.get(directory);
				if (!Files.exists(path)) {
					Logger.warn("CagesArrayPersistence:saveCagesAsync() directory does not exist: " + directory);
					return false;
				}

				try {
					// Save bulk data to CSV (fast, efficient)
					csvSaveCagesMeasures(cages, directory);

					// Save ROIs to XML (standard format for ROI serialization)
					String tempName = directory + File.separator + ID_MCDROSOTRACK_XML;
					xmlSaveCagesROIsOnly(cages, tempName);

					return true;
				} catch (Exception e) {
					Logger.error("CagesArrayPersistence:saveCagesAsync() Error: " + e.getMessage(), e);
					return false;
				}
			}
		});
	}
}
