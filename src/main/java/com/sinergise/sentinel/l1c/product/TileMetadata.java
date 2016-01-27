package com.sinergise.sentinel.l1c.product;

import java.io.InputStream;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;

import com.sinergise.sentinel.l1c.product.mapping.SciHubProduct;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.PrecisionModel;

public class TileMetadata {
	
	private String datastripId;
	private Date sensingTime;
	private String tileId;
	
	private int epsgCode;
	
	private Geometry tileDataGeometry;
	private Polygon tileGeometry;
	private Point tileOrigin;
	
	private double cloudyPixelsPercentage;
	
	public TileMetadata(InputStream is) throws Exception {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(is);

			XPathFactory xPathfactory = XPathFactory.newInstance();
			XPath xpath = xPathfactory.newXPath();
			doc.getDocumentElement().normalize();
			
			
			datastripId = (String) xpath.compile("/Level-1C_Tile_ID/General_Info/DATASTRIP_ID").evaluate(doc, XPathConstants.STRING);
			sensingTime = SciHubProduct.METADATA_XML_DATE_FORMAT.parse(
					(String) xpath.compile("/Level-1C_Tile_ID/General_Info/SENSING_TIME").evaluate(doc, XPathConstants.STRING));
			
			tileId = (String) xpath.compile("/Level-1C_Tile_ID/General_Info/TILE_ID").evaluate(doc, XPathConstants.STRING);
			
			
			String epsgCodeString = (String) xpath.compile("/Level-1C_Tile_ID/Geometric_Info/Tile_Geocoding/HORIZONTAL_CS_CODE").evaluate(doc, XPathConstants.STRING);
			epsgCode = Integer.parseInt(epsgCodeString.substring("EPSG:".length()));
			
			Double ulx = (Double) xpath.compile("/Level-1C_Tile_ID/Geometric_Info/Tile_Geocoding/Geoposition[@resolution='10']/ULX").evaluate(doc, XPathConstants.NUMBER);
			Double uly = (Double) xpath.compile("/Level-1C_Tile_ID/Geometric_Info/Tile_Geocoding/Geoposition[@resolution='10']/ULY").evaluate(doc, XPathConstants.NUMBER);
			Double width = (Double) xpath.compile("/Level-1C_Tile_ID/Geometric_Info/Tile_Geocoding/Size[@resolution='10']/NCOLS").evaluate(doc, XPathConstants.NUMBER);
			Double height = (Double) xpath.compile("/Level-1C_Tile_ID/Geometric_Info/Tile_Geocoding/Size[@resolution='10']/NROWS").evaluate(doc, XPathConstants.NUMBER);
			Double xDim = (Double) xpath.compile("/Level-1C_Tile_ID/Geometric_Info/Tile_Geocoding/Geoposition[@resolution='10']/XDIM").evaluate(doc, XPathConstants.NUMBER);
			Double yDim = (Double) xpath.compile("/Level-1C_Tile_ID/Geometric_Info/Tile_Geocoding/Geoposition[@resolution='10']/YDIM").evaluate(doc, XPathConstants.NUMBER);

			GeometryFactory gf = new GeometryFactory(new PrecisionModel(), epsgCode); 		
			tileOrigin = gf.createPoint(new Coordinate(ulx, uly));
			
			
			tileGeometry =  gf.createPolygon(new Coordinate[] {
					new Coordinate(ulx,uly),
					new Coordinate(ulx+width*xDim, uly),
					new Coordinate(ulx+width*xDim, uly+height*yDim),
					new Coordinate(ulx, uly+height*yDim),
					new Coordinate(ulx,uly)
			});

			cloudyPixelsPercentage= (Double) xpath.compile("/Level-1C_Tile_ID/Quality_Indicators_Info/Image_Content_QI/CLOUDY_PIXEL_PERCENTAGE").evaluate(doc, XPathConstants.NUMBER);

	}
	

	
	public double getCloudyPixelsPercentage() {
		return cloudyPixelsPercentage;
	}
	
	public String getDatastripId() {
		return datastripId;
	}
	
	public int getEpsgCode() {
		return epsgCode;
	}
	
	public Point getTileOrigin() {
		return tileOrigin;
	}
	
	public Polygon getTileGeometry() {
		return tileGeometry;
	}

	public void setTileDataGeometry(Geometry tileDataGeometry) {
		this.tileDataGeometry = tileDataGeometry;
	}
	
	public Geometry getTileDataGeometry() {
		return tileDataGeometry;
	}

	public String getTileId() {
		return tileId;
	}

	public Date getSensingTime() {
		return sensingTime;
	}
	
}
