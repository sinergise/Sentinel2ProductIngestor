package com.sinergise.sentinel.l1c.product.mapping.scihub.v1;

import java.io.File;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.sinergise.sentinel.l1c.product.L1CProductConstants;

public class SciHubProductDatastrip {
	private File datastripDirectory;
	private File metadataFile;
	private File [] qiFiles;
	private String id;
	
	private Date sensingStart;
	
	public SciHubProductDatastrip(File datastripDirectory) {
		this.datastripDirectory = datastripDirectory;
		initialize();
	}
	
	private void initialize() {
		id = datastripDirectory.getName();
		File [] metadataXMLFiles = datastripDirectory.listFiles(FileSuffixFilter.xml());
		if (metadataXMLFiles.length != 1)  {
			throw new IllegalStateException("Found no or more than 1 datastrip xml file!");
		}
		metadataFile = metadataXMLFiles[0];
		processMetadataXml(metadataFile);
		
		qiFiles =  new File(datastripDirectory,"QI_DATA").listFiles(FileSuffixFilter.xml());
	}

	

	private void processMetadataXml(File metadataXmlFile) {
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(metadataXmlFile);
			doc.getDocumentElement().normalize();
			Element elUserProduct = (Element) doc.getFirstChild();
			Element elGeneralInfo = (Element) elUserProduct.getElementsByTagName("n1:General_Info").item(0);
			Element elDatastripTimeInfo = (Element) elGeneralInfo.getElementsByTagName("Datastrip_Time_Info").item(0);
			Element elDatastripSensingStart = (Element) elDatastripTimeInfo.getElementsByTagName("DATASTRIP_SENSING_START").item(0);
			sensingStart = L1CProductConstants.getMetadataXmlDateFormat().parse(elDatastripSensingStart.getFirstChild().getNodeValue());
			
		} catch (Exception ex) {
			throw new RuntimeException("Failed to extract product timestamp!", ex);
		}
	}
	
	public Date getSensingStart() {
		return sensingStart;
	}
	
	public String getId() {
		return id;
	}
	
	public File getMetadataFile() {
		return metadataFile;
	}
	
	public File[] getQiFiles() {
		return qiFiles;
	}
	
}
