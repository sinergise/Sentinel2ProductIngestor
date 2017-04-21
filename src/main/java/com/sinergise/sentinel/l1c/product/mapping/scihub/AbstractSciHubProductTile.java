package com.sinergise.sentinel.l1c.product.mapping.scihub;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.sinergise.sentinel.l1c.product.L1CProductConstants;
import com.sinergise.sentinel.l1c.product.TileMetadata;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.PrecisionModel;

public abstract class AbstractSciHubProductTile {
	
	public static final Logger logger = LoggerFactory.getLogger(AbstractSciHubProductTile.class);
	
	private static final String DETECTOR_FOOTPRINT_QI_FILE_KEYWORD = "DETFOO";
	private static final Pattern GRANULE_MGRS_PATTERN = Pattern.compile("^.*_T([0-9]{2})([A-Z])([A-Z]{2}).*$");
	private static final Pattern QI_XML_FILE_PATTERN = Pattern.compile("^([A-Za-z0-9]+)_([A-Za-z0-9]+).xml$");
	
	private File productTileDir;
	private File metadataXml;
	private File preview;
	private File[] images;
	private File[] qiData;
	private File[] auxData;
	private String tileName;
	private TileMetadata tileMetadata;
	private int utmZone;
	private String latitudeBand;
	private String gridSquare;

	
	
	public AbstractSciHubProductTile(AbstractSciHubProduct sciHubProduct, File productTileDir) {
		this.productTileDir = productTileDir;
		initialize();
	}

	private void initialize() {
		tileName = productTileDir.getName();
		
		Matcher m = GRANULE_MGRS_PATTERN.matcher(tileName);
		if (!m.matches()) {
			throw new IllegalStateException("Tile " + tileName + " is invalid!");
		}

		utmZone = Integer.parseInt(m.group(1));
		latitudeBand = m.group(2);
		gridSquare = m.group(3);

		
		File[] metadataFiles = productTileDir.listFiles(FileSuffixFilter.xml());
		if (metadataFiles.length != 1) {
			throw new IllegalStateException("Illegal metadata file count: " + metadataFiles.length);
		}
		metadataXml = metadataFiles[0];
		try (InputStream is = new FileInputStream(metadataXml)) {
			tileMetadata = createTileMetadata(is);
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
	
	private TileMetadata createTileMetadata(InputStream is) throws Exception {
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(is);

		XPathFactory xPathfactory = XPathFactory.newInstance();
		XPath xpath = xPathfactory.newXPath();
		doc.getDocumentElement().normalize();
		
		TileMetadata tmd = new TileMetadata();
		tmd.setDatastripId(				
						(String) xpath.compile("/Level-1C_Tile_ID/General_Info/DATASTRIP_ID").evaluate(doc, XPathConstants.STRING));
		tmd.setSensingTime(L1CProductConstants.getMetadataXmlDateFormat().parse(
				(String) xpath.compile("/Level-1C_Tile_ID/General_Info/SENSING_TIME").evaluate(doc, XPathConstants.STRING)));
		
		tmd.setTileId((String) xpath.compile("/Level-1C_Tile_ID/General_Info/TILE_ID").evaluate(doc, XPathConstants.STRING));
		String epsgCodeString = (String) xpath.compile("/Level-1C_Tile_ID/Geometric_Info/Tile_Geocoding/HORIZONTAL_CS_CODE").evaluate(doc, XPathConstants.STRING);
		tmd.setEpsgCode(Integer.parseInt(epsgCodeString.substring("EPSG:".length())));
		
		Double ulx = (Double) xpath.compile("/Level-1C_Tile_ID/Geometric_Info/Tile_Geocoding/Geoposition[@resolution='10']/ULX").evaluate(doc, XPathConstants.NUMBER);
		Double uly = (Double) xpath.compile("/Level-1C_Tile_ID/Geometric_Info/Tile_Geocoding/Geoposition[@resolution='10']/ULY").evaluate(doc, XPathConstants.NUMBER);
		Double width = (Double) xpath.compile("/Level-1C_Tile_ID/Geometric_Info/Tile_Geocoding/Size[@resolution='10']/NCOLS").evaluate(doc, XPathConstants.NUMBER);
		Double height = (Double) xpath.compile("/Level-1C_Tile_ID/Geometric_Info/Tile_Geocoding/Size[@resolution='10']/NROWS").evaluate(doc, XPathConstants.NUMBER);
		Double xDim = (Double) xpath.compile("/Level-1C_Tile_ID/Geometric_Info/Tile_Geocoding/Geoposition[@resolution='10']/XDIM").evaluate(doc, XPathConstants.NUMBER);
		Double yDim = (Double) xpath.compile("/Level-1C_Tile_ID/Geometric_Info/Tile_Geocoding/Geoposition[@resolution='10']/YDIM").evaluate(doc, XPathConstants.NUMBER);

		GeometryFactory gf = new GeometryFactory(new PrecisionModel(), tmd.getEpsgCode()); 		
		tmd.setTileOrigin(gf.createPoint(new Coordinate(ulx, uly)));
		
		
		tmd.setTileGeometry(gf.createPolygon(new Coordinate[] {
				new Coordinate(ulx,uly),
				new Coordinate(ulx+width*xDim, uly),
				new Coordinate(ulx+width*xDim, uly+height*yDim),
				new Coordinate(ulx, uly+height*yDim),
				new Coordinate(ulx,uly)
		}));

		tmd.setCloudyPixelsPercentage((Double) xpath.compile("/Level-1C_Tile_ID/Quality_Indicators_Info/Image_Content_QI/CLOUDY_PIXEL_PERCENTAGE").evaluate(doc, XPathConstants.NUMBER));
		return tmd;
	}

	public File getTileDirectory() {
		return productTileDir;
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
		
	public TileMetadata getTileMetadata() {
		return tileMetadata;
	}
	
	public int getUtmZone() {
		return utmZone;
	}
	
	public String getLatitudeBand() {
		return latitudeBand;
	}
	
	public String getGridSquare() {
		return gridSquare;
	}
	
	public abstract Pattern getJp2BandFilePattern();
	public abstract Pattern getQiReportFilePattern();
	public abstract Pattern getMaskGMLFilePattern();
	public abstract Pattern getAuxFilePattern();
	public Pattern getQiXMLFilePattern() {
		return QI_XML_FILE_PATTERN;
	}
}
