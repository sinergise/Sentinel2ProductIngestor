package com.sinergise.sentinel.l1c.product.mapping.scihub;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import com.sinergise.sentinel.l1c.product.L1CProductConstants;
import com.sinergise.sentinel.scihub.SciHubEntry;

public abstract class AbstractSciHubProduct {

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
	
	protected Function<String, String> datastripIdConverter;

	private ArrayList<AbstractSciHubProductTile> productTiles = new ArrayList<>();
	private Map<String, AbstractSciHubProductDatastrip> datastrips = new HashMap<>();


	protected AbstractSciHubProduct(File productBase, SciHubEntry sciHubEntry) {
		this(productBase, sciHubEntry, id -> id);
	}

	protected AbstractSciHubProduct(File productBase, SciHubEntry sciHubEntry, Function<String,String> datastripIdConverter) {
		this.datastripIdConverter = datastripIdConverter;
		this.productBase = productBase;
		this.productName = sciHubEntry.getName();
		this.productId = sciHubEntry.getId();
		this.ingestionDate = sciHubEntry.getIngestionDate();
		initialize();
	}
	
	protected abstract File getMetadataFile(File productBase);
	protected AbstractSciHubProductDatastrip createDatastrip(File datastripDirectory, String datastripId) {
		return new AbstractSciHubProductDatastrip(datastripDirectory, datastripId) {
		};
	}
	protected abstract AbstractSciHubProductTile createTile(AbstractSciHubProduct sciHubProduct, File tileDir);
	
	protected void initialize() {
		metadataFile = getMetadataFile(productBase);
		previewFile = new File(productBase, new String(productName).replaceFirst("_PRD_", "_BWI_") + ".png");
		manifestFile = new File(productBase, "manifest.safe");
		inspireFile = new File(productBase, "INSPIRE.xml");

		processMetadataXml(metadataFile);
		
		File[] tileFolders = new File(productBase, "GRANULE").listFiles(new FileFilter() {

			@Override
			public boolean accept(File pathname) {
				// broken granule directory error that was never fixed (we've reported it) workarround
				if (pathname.isDirectory() && !pathname.getName().contains("null")) 
					return true;
				return false;
			}
		});
		for (File tileDir : tileFolders) {
			productTiles.add(createTile(this, tileDir));
		}

		File[] auxFiles = new File(productBase, "AUX_DATA").listFiles();
		if (auxFiles != null && auxFiles.length > 0) {
			throw new IllegalStateException("Product aux data found. Not supported!");
		}
	}
	
	private void loadDatastrip(File datastripsDirectory, String datastripId) {
		datastrips.put(datastripId, 
				createDatastrip(new File(datastripsDirectory, datastripIdConverter.apply(datastripId)), datastripId));
	}

	private void processMetadataXml(File metadataXmlFile) {
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(metadataXmlFile);
			
			XPathFactory xPathfactory = XPathFactory.newInstance();
			XPath xpath = xPathfactory.newXPath();
			
			
			productStopTime = L1CProductConstants.getMetadataXmlDateFormat().parse((String) xpath.compile(
					"/Level-1C_User_Product/General_Info/Product_Info/PRODUCT_STOP_TIME").evaluate(doc, XPathConstants.STRING));
			datatakeIdentifier = 	(String) xpath.compile(
					"/Level-1C_User_Product/General_Info/Product_Info/Datatake/@datatakeIdentifier").evaluate(doc, XPathConstants.STRING);
			NodeList dsIdentifierNodes = (NodeList) xpath.compile(
					"/Level-1C_User_Product/General_Info/Product_Info/Product_Organisation/Granule_List//@datastripIdentifier").evaluate(doc, XPathConstants.NODESET);

			File datastripsDirectory = new File(productBase, "DATASTRIP");
			for (int i=0;i<dsIdentifierNodes.getLength();i++) {
				String datastripId = dsIdentifierNodes.item(i).getNodeValue();
				loadDatastrip(datastripsDirectory, datastripId);
			}
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
	
	public List<AbstractSciHubProductDatastrip> getDatastrips() {
		return datastrips.values()
					.stream()
					.sorted((s1,s2) -> Long.compare(s1.getSensingStart().getTime(), s2.getSensingStart().getTime()))
					.collect(Collectors.toList());
	}
	
	public AbstractSciHubProductDatastrip getDatastrip(String datastripId) {
		return datastrips.get(datastripId);
	}

	public List<AbstractSciHubProductTile> getTiles() {
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
	
	public String toString() {
		return getName();		
	}

}
