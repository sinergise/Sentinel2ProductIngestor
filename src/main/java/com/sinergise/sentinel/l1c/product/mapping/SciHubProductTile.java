package com.sinergise.sentinel.l1c.product.mapping;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sinergise.sentinel.l1c.product.TileMetadata;

public class SciHubProductTile {
	
	public static final Logger logger = LoggerFactory.getLogger(SciHubProductTile.class);
	
	private static final String DETECTOR_FOOTPRINT_QI_FILE_KEYWORD = "DETFOO";

	private File productTileDir;
	
	private File metadataXml;
	private File preview;

	private File[] images;
	private File[] qiData;
	private File[] auxData;
	
	private String tileName;
	private SciHubProduct sciHubProduct;
		

	private TileMetadata tileMetadata;
	
	
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
		try (InputStream is = new FileInputStream(metadataXml)) {
			tileMetadata = new TileMetadata(is);
		} catch (Exception ex) {
			logger.error("Failed to parse metadata xml file!",ex);
			throw new IllegalArgumentException("Failed to parse metadata.xml!",ex);
		}
		
		qiData = new File(productTileDir, "QI_DATA").listFiles(new FileSuffixFilter(new String[] { ".xml", ".gml" }));
		auxData = new File(productTileDir, "AUX_DATA").listFiles();
		
		File [] previews = new File(productTileDir, "QI_DATA").listFiles(new FileSuffixFilter(new String[] { ".jp2" }));
		if (previews.length != 1) {
			throw new IllegalStateException("More than 1 preview file found!");
		}
		
		preview = previews[0];		
		images = new File(productTileDir,"IMG_DATA").listFiles(new FileSuffixFilter(new String[] { ".jp2" }));
		
		
		
		TileDataGeometryCalculator coverageCalc = new TileDataGeometryCalculator(tileMetadata.getEpsgCode());
		for (File qiFile:qiData) {
			if (qiFile.getName().contains(DETECTOR_FOOTPRINT_QI_FILE_KEYWORD)) {
				try (InputStream is = new FileInputStream(qiFile)){
					coverageCalc.addDetectorFootprint(is);
				} catch (Exception e) {
					logger.warn("Error while calculating tile coverage for file {}!", qiFile, e);
				}
			}
		}
		
		try {
			tileMetadata.setTileDataGeometry(coverageCalc.getCoverage());
		} catch (Exception ex) {
			logger.warn("Error while calculating tile coverage!", ex);
		}
		
	}
	
	public File getTileDirectory() {
		return productTileDir;
	}
	
	
//	private void processMetadataXml(File metadataXmlFile) {
//		try {
//			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
//			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
//			Document doc = dBuilder.parse(metadataXmlFile);
//
//			XPathFactory xPathfactory = XPathFactory.newInstance();
//			XPath xpath = xPathfactory.newXPath();
//			doc.getDocumentElement().normalize();
//			
//			
//			datastripId = (String) xpath.compile("/Level-1C_Tile_ID/General_Info/DATASTRIP_ID").evaluate(doc, XPathConstants.STRING);
//			sensingTime = SciHubProduct.METADATA_XML_DATE_FORMAT.parse(
//					(String) xpath.compile("/Level-1C_Tile_ID/General_Info/SENSING_TIME").evaluate(doc, XPathConstants.STRING));
//			
//			tileId = (String) xpath.compile("/Level-1C_Tile_ID/General_Info/TILE_ID").evaluate(doc, XPathConstants.STRING);
//			
//			
//			String epsgCodeString = (String) xpath.compile("/Level-1C_Tile_ID/Geometric_Info/Tile_Geocoding/HORIZONTAL_CS_CODE").evaluate(doc, XPathConstants.STRING);
//			epsgCode = Integer.parseInt(epsgCodeString.substring("EPSG:".length()));
//			
//			Double ulx = (Double) xpath.compile("/Level-1C_Tile_ID/Geometric_Info/Tile_Geocoding/Geoposition[@resolution='10']/ULX").evaluate(doc, XPathConstants.NUMBER);
//			Double uly = (Double) xpath.compile("/Level-1C_Tile_ID/Geometric_Info/Tile_Geocoding/Geoposition[@resolution='10']/ULY").evaluate(doc, XPathConstants.NUMBER);
//			Double width = (Double) xpath.compile("/Level-1C_Tile_ID/Geometric_Info/Tile_Geocoding/Size[@resolution='10']/NCOLS").evaluate(doc, XPathConstants.NUMBER);
//			Double height = (Double) xpath.compile("/Level-1C_Tile_ID/Geometric_Info/Tile_Geocoding/Size[@resolution='10']/NROWS").evaluate(doc, XPathConstants.NUMBER);
//			Double xDim = (Double) xpath.compile("/Level-1C_Tile_ID/Geometric_Info/Tile_Geocoding/Geoposition[@resolution='10']/XDIM").evaluate(doc, XPathConstants.NUMBER);
//			Double yDim = (Double) xpath.compile("/Level-1C_Tile_ID/Geometric_Info/Tile_Geocoding/Geoposition[@resolution='10']/YDIM").evaluate(doc, XPathConstants.NUMBER);
//
//			GeometryFactory gf = new GeometryFactory(new PrecisionModel(), epsgCode); 		
//			tileOrigin = gf.createPoint(new Coordinate(ulx, uly));
//			
//			
//			tileGeometry =  gf.createPolygon(new Coordinate[] {
//					new Coordinate(ulx,uly),
//					new Coordinate(ulx+width*xDim, uly),
//					new Coordinate(ulx+width*xDim, uly+height*yDim),
//					new Coordinate(ulx, uly+height*yDim),
//					new Coordinate(ulx,uly)
//			});
//
//			cloudyPixelsPercentage= (Double) xpath.compile("/Level-1C_Tile_ID/Quality_Indicators_Info/Image_Content_QI/CLOUDY_PIXEL_PERCENTAGE").evaluate(doc, XPathConstants.NUMBER);
//
//						
//			
//			
//		} catch (Exception ex) {
//			throw new RuntimeException("Failed to extract product timestamp!", ex);
//		}
//	}
	
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
		
	public TileMetadata getTileMetadata() {
		return tileMetadata;
	}	
}
