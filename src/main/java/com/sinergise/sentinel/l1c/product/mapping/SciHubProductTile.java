package com.sinergise.sentinel.l1c.product.mapping;

import java.io.File;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class SciHubProductTile {
	private File productTileDir;
	
	private File metadataXml;
	private File preview;

	private File[] images;
	private File[] qiData;
	private File[] auxData;
	
	private String tileName;
	private SciHubProduct sciHubProduct;
	private File productInfoFile;
	
	private String datastripId;
	private Date sensingTime;
	private String tileId;
	
	public SciHubProductTile(SciHubProduct sciHubProduct, File productTileDir) {
		this.productTileDir = productTileDir;
		this.sciHubProduct = sciHubProduct;
		initialize();
	}

	private void initialize() {
		tileName = productTileDir.getName();
		
		File[] metadataFiles = productTileDir.listFiles(FileSuffixFilter.xml());
		if (metadataFiles.length != 1) {
			throw new IllegalStateException("More than 1 metadata file found!");
		}
		metadataXml = metadataFiles[0];
		processMetadataXml(metadataXml);
		
		productInfoFile = new File(productTileDir, "product.info");
		qiData = new File(productTileDir, "QI_DATA").listFiles(new FileSuffixFilter(new String[] { ".xml", ".gml" }));
		auxData = new File(productTileDir, "AUX_DATA").listFiles();
		
		File [] previews = new File(productTileDir, "QI_DATA").listFiles(new FileSuffixFilter(new String[] { ".jp2" }));
		if (previews.length != 1) {
			throw new IllegalStateException("More than 1 preview file found!");
		}
		
		preview = previews[0];		
		images = new File(productTileDir,"IMG_DATA").listFiles(new FileSuffixFilter(new String[] { ".jp2" }));		
	}
	
	
	private void processMetadataXml(File metadataXmlFile) {
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(metadataXmlFile);
			doc.getDocumentElement().normalize();
			Element elUserProduct = (Element) doc.getFirstChild();
			Element elGeneralInfo = (Element) elUserProduct.getElementsByTagName("n1:General_Info").item(0);
			Element elDatastripId = (Element) elGeneralInfo.getElementsByTagName("DATASTRIP_ID").item(0);
			Element elSensingTime = (Element) elGeneralInfo.getElementsByTagName("SENSING_TIME").item(0);
			datastripId = elDatastripId.getFirstChild().getNodeValue();
			sensingTime = SciHubProduct.METADATA_XML_DATE_FORMAT.parse(elSensingTime.getFirstChild().getNodeValue());
			Element elTileId = (Element) elGeneralInfo.getElementsByTagName("TILE_ID").item(0);
			tileId = elTileId.getFirstChild().getNodeValue();
		} catch (Exception ex) {
			throw new RuntimeException("Failed to extract product timestamp!", ex);
		}
	}
	
	public String getTileName() {
		return tileName;
	}
	
	public File getPreviewFile() {
		return preview;
	}
	
	public File[] getQiFiles() {
		return qiData;
	}
	
	
	public File[] getImageFiles() {
		return images;
	}
	
	public File getMetadataFile() {
		return metadataXml;
	}
	
	public File[] getAuxFiles() {
		return auxData;
	}	
	
	public File getProductInfoFile() {
		return productInfoFile;
	}
	
	public Date getSensingTime() {
		return sensingTime;
	}
	
	public String getDatastripId() {
		return datastripId;
	}
	
	public String getTileId() {
		return tileId;
	}
}
