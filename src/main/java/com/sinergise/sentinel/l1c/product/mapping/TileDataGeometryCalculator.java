
package com.sinergise.sentinel.l1c.product.mapping;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.operation.buffer.BufferParameters;
import com.vividsolutions.jts.simplify.DouglasPeuckerSimplifier;

public class TileDataGeometryCalculator {

	private static final Logger logger = LoggerFactory.getLogger(TileDataGeometryCalculator.class);

	private static final double BUFFER_SIZE = -1; // m
	private static final int QUADRANT_SEGMENTS = 1;
	private static final int ENDCAP_STYLE = BufferParameters.CAP_FLAT;
	private static final double SIMPLIFICATION_DISTANCE_TOLERANCE = 10; // m
	
	private static DocumentBuilder documentBuilder;
	private static XPathExpression xpathExpression;
	
	private int epsgCode;
	private Geometry coveragesIntersection = null;

	
	
	public TileDataGeometryCalculator(int epsgCode) {
		this.epsgCode = epsgCode;
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			XPathFactory xPathfactory = XPathFactory.newInstance();
			XPath xpath = xPathfactory.newXPath();
			xpathExpression = xpath
					.compile("/Mask/maskMembers/MaskFeature/extentOf/Polygon/exterior/LinearRing/posList");
			documentBuilder = factory.newDocumentBuilder();
		} catch (ParserConfigurationException | XPathExpressionException e) {
			logger.error("Could not initialize: {}", e);
			throw new RuntimeException("Could not initialize TileDataGeometryCalculator! ", e);
		}
	}

	
	public Geometry addDetectorFootprint (InputStream detFooStream) {
		try {
			Geometry detectorFootprintGeometry = extractPolygons(detFooStream).stream()
					.map(g -> (Geometry)g)
					.reduce(null,(joined, polygon) -> joined == null ? polygon : joined.union(polygon));
			
			coveragesIntersection =  coveragesIntersection == null ? detectorFootprintGeometry :  coveragesIntersection.intersection(detectorFootprintGeometry);
			return detectorFootprintGeometry;
		} catch (Exception ex) {
			logger.error("Failed to process detector footprint input stream.", ex);
			throw new IllegalArgumentException("Failed to process detector footprint input stream!",ex);
		}
	}
	
	public Geometry getCoverage() throws Exception {
		return getCoverage(BUFFER_SIZE);
	}
	
	public Geometry getCoverage(double bufferSize) throws Exception {
		Geometry geom =   bufferSize == 0 ? coveragesIntersection : coveragesIntersection == null ? null : shrink(coveragesIntersection, bufferSize);
		if (!(geom instanceof Polygon || geom instanceof MultiPolygon)) {
			logger.error("The result of buffering should be a polygon or multipolygon, its {} instead.",
					geom.getGeometryType());
			System.out.println(geom.toText());
			throw new Exception("The result of buffering should be a polygon or multipolygon, its "
					+ geom.getGeometryType() + " instead.");
		}
		return geom;
	}
	
	
	private List<Polygon> extractPolygons(InputStream is)
			throws XPathExpressionException, ParserConfigurationException, SAXException, IOException {
		Document doc = documentBuilder.parse(is);
		NodeList nodelist = (NodeList) xpathExpression.evaluate(doc, XPathConstants.NODESET);

		GeometryFactory gf = new GeometryFactory(new PrecisionModel(), epsgCode); 
		List<Polygon> polys = new ArrayList<>();
		for (int i = 0; i < nodelist.getLength(); i++) {
			int srsDimString = 3;
			try {
				srsDimString = Integer
						.valueOf(nodelist.item(i).getAttributes().getNamedItem("srsDimension").getTextContent())
						.intValue();
			} catch (Exception e) {
				logger.debug("Could not parse srsDimension for polygon {}.", i);
			}
			String[] coords = nodelist.item(i).getTextContent().split(" ");
			int nPoints = coords.length / srsDimString;
			Coordinate[] coordinates = new Coordinate[nPoints];
			for (int k = 0; k < nPoints; k++) {
				coordinates[k] = new Coordinate(Double.valueOf(coords[k * srsDimString]),
						Double.valueOf(coords[k * srsDimString + 1]));
			}
			polys.add(gf.createPolygon(coordinates));
		}
		return polys;
	}

	private Geometry shrink(Geometry joined, double bufferSize) throws Exception {
		Geometry bufferedGeom = joined.buffer(bufferSize, QUADRANT_SEGMENTS, ENDCAP_STYLE);
		return  DouglasPeuckerSimplifier.simplify(bufferedGeom, SIMPLIFICATION_DISTANCE_TOLERANCE);
		
	}
	
	
	/*
	public Polygon calculateCoverageForTile(TileInfo tileInfo, File[] qiFiles) throws Exception {
		
		List<Geometry> bandCoverages = new ArrayList<>();
		for (File qiFile: qiFiles) {
			if (qiFile.getName().contains(DETECTOR_FOOTPRINT_QI_FILE_KEYWORD)) {
				try (InputStream is = new FileInputStream(qiFile)){
					bandCoverages.add(extractPolygons(is, tileInfo).stream()
							.reduce(null,(joined, polygon) -> joined == null ? polygon : (Polygon) joined.union(polygon)));

				} catch (XPathExpressionException | ParserConfigurationException | SAXException | IOException e) {
					logger.error("Could not extract coverage polygon for file {}!", qiFile,	e);
				}
			}			
		}

		
		
		if (bandCoverages.size() < 1) {
			return null;
		}
		
		Polygon shrinked = shrink(
				bandCoverages.stream()
				.reduce(null, (intersection, polygon) -> intersection == null ? polygon : intersection.intersection(polygon)));
		return shrinked;
	}
*/
	
	public static void main(String[] args) throws MalformedURLException {
//		TileDataGeometryCalculator calc = new TileDataGeometryCalculator();
		try {
//			Polygon poly = calc
//					.calculateCoverageForTile("http://sentinel-s2-l1c.s3.amazonaws.com/tiles/32/N/KL/2016/1/11/0/");
//			System.out.println(poly.toText());
//			System.out.println("drawRegionCoverage [%] = " + calc.getDrawRegionInTileRatio(poly));
//			System.out.println("drawRegionArea    [m2] = " + calc.getDrawRegionArea(poly));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	


	
	

}
