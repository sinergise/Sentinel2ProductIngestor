package com.sinergise.sentinel.l1c.product.mapping.scihub.v1;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.sinergise.sentinel.l1c.product.L1CProductConstants;
import com.sinergise.sentinel.scihub.SciHubEntry;

public class SciHubProduct {

	private File productBase;
	private File metadataFile;
	private File previewFile;
	private Date productStopTime;
	private File inspireFile;
	private File manifestFile;

	private String productId;
	private String productName;
	private Date ingestionDate;
	private String datatakeIdentifier;

	private ArrayList<SciHubProductTile> productTiles = new ArrayList<>();
	private Map<String, SciHubProductDatastrip> datastrips = new HashMap<>();

	
	public SciHubProduct(File productBase, SciHubEntry sciHubEntry) {
		this.productBase = productBase;
		this.productName = sciHubEntry.getName();
		this.productId = sciHubEntry.getId();
		this.ingestionDate = sciHubEntry.getIngestionDate();
		initialize();
	}

	private void initialize() {
		File metadataFiles[] = productBase.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith("xml") && name.contains("OPER_MTD_SAFL1C");
			}
		});
		if (metadataFiles.length!=1) {
			throw new RuntimeException("Couldn't find product metadata.xml file!");
		}
		metadataFile = metadataFiles[0];
		previewFile = new File(productBase, new String(productName).replaceFirst("_PRD_", "_BWI_") + ".png");
		manifestFile = new File(productBase, "manifest.safe");
		inspireFile = new File(productBase, "INSPIRE.xml");

		processMetadataXml(metadataFile);
		
		loadDatastrips(new File(productBase, "DATASTRIP"));
		File[] tileFolders = new File(productBase, "GRANULE").listFiles(new FileFilter() {

			@Override
			public boolean accept(File pathname) {
				if (pathname.isDirectory())
					return true;
				return false;
			}
		});
		for (File tileDir : tileFolders) {
			productTiles.add(new SciHubProductTile(this, tileDir));
		}

		File[] auxFiles = new File(productBase, "AUX_DATA").listFiles();
		if (auxFiles != null && auxFiles.length > 0) {
			throw new IllegalStateException("Product aux data found. Not supported!");
		}
	}
	
	private void loadDatastrips(File datastripsDirectory) {
		for (File datastripDirectory : datastripsDirectory.listFiles()) {
			if (!datastripDirectory.isDirectory()) {
				throw new IllegalStateException("Files found DATASTRIP directory!");
			}
			SciHubProductDatastrip datastrip = new SciHubProductDatastrip(datastripDirectory);
			datastrips.put(datastrip.getId(), datastrip);
		}
	}

	private void processMetadataXml(File metadataXmlFile) {
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(metadataXmlFile);
			doc.getDocumentElement().normalize();
			Element elUserProduct = (Element) doc.getFirstChild();
			Element elGeneralInfo = (Element) elUserProduct.getElementsByTagName("n1:General_Info").item(0);
			Element elProductInfo = (Element) elGeneralInfo.getElementsByTagName("Product_Info").item(0);
			Element elProductStopTime = (Element) elProductInfo.getElementsByTagName("PRODUCT_STOP_TIME").item(0);
			productStopTime = L1CProductConstants.getMetadataXmlDateFormat().parse(elProductStopTime.getFirstChild().getNodeValue());
			Element elDatatake = (Element) elProductInfo.getElementsByTagName("Datatake").item(0);
			datatakeIdentifier =  elDatatake.getAttribute("datatakeIdentifier");
		} catch (Exception ex) {
			throw new RuntimeException("Failed to extract product timestamp!", ex);
		}
	}

	public Date getProductStopTime() {
		return productStopTime;
	}
	
	public String getDatatakeIdentifier() {
		return datatakeIdentifier;
	}

	public File getMetadataFile() {
		return metadataFile;
	}
	
	public List<SciHubProductDatastrip> getDatastrips() {
		return datastrips.values()
					.stream()
					.sorted((s1,s2) -> Long.compare(s1.getSensingStart().getTime(), s2.getSensingStart().getTime()))
					.collect(Collectors.toList());
	}
	
	public SciHubProductDatastrip getDatastrip(String datastripId) {
		return datastrips.get(datastripId);
	}

	public ArrayList<SciHubProductTile> getTiles() {
		return productTiles;
	}

	public File getManifestFile() {
		return manifestFile;
	}

	public File getInspireFile() {
		return inspireFile;
	}

	public File getPreviewFile() {
		return previewFile;
	}

	public String getName() {
		return productName;
	}

	public Date getIngestionDate() {
		return ingestionDate;
	}

	public void setIngestionDate(Date ingestionDate) {
		this.ingestionDate = ingestionDate;
	}

	public String getProductId() {
		return productId;
	}
	
	@Override
	public String toString() {
		return getName();		
	}

}
