package com.sinergise.test.sentinel.l1c.product;

import java.io.File;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.soap.Node;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.geojson.LngLatAlt;
import org.geojson.Polygon;
import org.w3c.dom.Document;

public class Test {
	public static void main(String[] args) throws Exception {
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(new File(
				"E:\\temp\\S2A_OPER_PRD_MSIL1C_PDMC_20160108T234944_R141_V20160108T175813_20160108T175813.SAFE\\GRANULE\\S2A_OPER_MSI_L1C_TL_SGS__20160108T204646_A002855_T14VLH_N02.01\\S2A_OPER_MTD_L1C_TL_SGS__20160108T204646_A002855_T14VLH.xml"));
		
		
		XPathFactory xPathfactory = XPathFactory.newInstance();
		XPath xpath = xPathfactory.newXPath();
		
		Double ulx = (Double) xpath.compile("/Level-1C_Tile_ID/Geometric_Info/Tile_Geocoding/Geoposition[@resolution='10']/ULX").evaluate(doc, XPathConstants.NUMBER);
		Double uly = (Double) xpath.compile("/Level-1C_Tile_ID/Geometric_Info/Tile_Geocoding/Geoposition[@resolution='10']/ULY").evaluate(doc, XPathConstants.NUMBER);
		Double width = (Double) xpath.compile("/Level-1C_Tile_ID/Geometric_Info/Tile_Geocoding/Size[@resolution='10']/NCOLS").evaluate(doc, XPathConstants.NUMBER);
		Double height = (Double) xpath.compile("/Level-1C_Tile_ID/Geometric_Info/Tile_Geocoding/Size[@resolution='10']/NROWS").evaluate(doc, XPathConstants.NUMBER);
		Double xDim = (Double) xpath.compile("/Level-1C_Tile_ID/Geometric_Info/Tile_Geocoding/Geoposition[@resolution='10']/ULX").evaluate(doc, XPathConstants.NUMBER);
		Double yDim = (Double) xpath.compile("/Level-1C_Tile_ID/Geometric_Info/Tile_Geocoding/Geoposition[@resolution='10']/ULY").evaluate(doc, XPathConstants.NUMBER);
		
		
		Polygon tilePolygon = new Polygon(									
				Stream.of(new LngLatAlt(ulx, uly),
						new LngLatAlt(ulx+width*xDim, uly),
						new LngLatAlt(ulx+width*xDim, uly+height*yDim),
						new LngLatAlt(ulx, uly+height*yDim),
						new LngLatAlt(ulx, uly))
					   .collect(Collectors.toList()));
		
		
		
	}
}
