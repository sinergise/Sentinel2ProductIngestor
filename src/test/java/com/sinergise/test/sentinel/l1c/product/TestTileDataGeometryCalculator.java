package com.sinergise.test.sentinel.l1c.product;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import com.sinergise.sentinel.l1c.product.mapping.TileDataGeometryCalculator;

public class TestTileDataGeometryCalculator {
	private static final List<String> bandNames = Arrays.asList(
			"B01", "B02", "B03", "B04", "B05", "B06", "B07", "B08", "B8A", "B09", "B10", "B11", "B12");

	public static void main(String[] args) throws Exception {
		
		TileDataGeometryCalculator gc = new TileDataGeometryCalculator(12345);
		String tileLink = "http://sentinel-s2-l1c.s3.amazonaws.com/tiles/15/R/VM/2015/12/31/0/";
		for (String band : bandNames){
			String gmlFileUrl = tileLink + "qi" + "/" + "MSK_DETFOO_" + band + ".gml";
			URL sourceURL = new URL(gmlFileUrl);
			HttpURLConnection sourceConnection = (HttpURLConnection) sourceURL.openConnection();
			sourceConnection.connect();
			try (InputStream is = sourceConnection.getInputStream()) {
				gc.addDetectorFootprint(is);
			}
		}
		gc.getCoverage();
		
		/*
		SciHubEntry she = new SciHubEntry("01344441-2eb6-4d93-81d2-52c75f953e60",
				"S2A_OPER_PRD_MSIL1C_PDMC_20160108T234944_R141_V20160108T175813_20160108T175813");
		
		SciHubProduct shProduct = new SciHubProduct(new File("E:\\temp\\S2A_OPER_PRD_MSIL1C_PDMC_20160108T234944_R141_V20160108T175813_20160108T175813.SAFE"), she);
		
		System.out.println(shProduct.getTiles().get(0).getTileName());
		
		S3Product s3Product = new S3Product(shProduct, new File("products"), new File("tiles"),new TileSequenceProvider() {

			@Override
			public int getSequence(String s3TileKeyBaseName, String tileId) {
				return 0;
			}
			
		});
		TileInfo ti=new TileInfo(s3Product.getTiles().get(2));
		ProductInfo pi = new ProductInfo(s3Product);
		
		ExtendedTileInfo eti = new ExtendedTileInfo(ti, pi, shProduct.getTiles().get(2).getTileMetadata());
		
		
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
		DateFormat dateFormatIso8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		dateFormatIso8601.setTimeZone(TimeZone.getTimeZone("UTC"));
		objectMapper.setDateFormat(dateFormatIso8601);
		
		System.out.println(objectMapper.writeValueAsString(eti));*/
	}
}
