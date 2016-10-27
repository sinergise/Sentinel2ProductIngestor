package com.sinergise.sentinel.l1c.product.mapping;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.sinergise.sentinel.l1c.product.L1CProductConstants;
import com.sinergise.sentinel.l1c.product.mapping.scihub.AbstractSciHubProduct;
import com.sinergise.sentinel.l1c.product.mapping.scihub.AbstractSciHubProductDatastrip;
import com.sinergise.sentinel.l1c.product.mapping.scihub.AbstractSciHubProductTile;

public class S3Product {

	private File baseDirectory;
	
	private File metadataFile;
	private File previewFile;
	private File inspireFile;
	private File manifestFile;
	private File auxDirectory;
	private ArrayList<S3ProductTile> tiles;
	
	private Map<String,S3ProductDatastrip> dataStripMap;
	
	private String name;
	private String id;
	private String datatakeIdentifier;
	
	private Date sciHubIngestionTs;
	private Date timestamp;
		
	public S3Product(AbstractSciHubProduct sciHubProduct, File s3ProductBase, File s3TilesBase, TileSequenceProvider tileSequenceProvider) {
		this.name = sciHubProduct.getName();
		this.id = sciHubProduct.getProductId();
		this.sciHubIngestionTs = sciHubProduct.getIngestionDate();
		this.timestamp = sciHubProduct.getProductStopTime();
		this.datatakeIdentifier = sciHubProduct.getDatatakeIdentifier();
		
		baseDirectory = L1CProductConstants.getProductBaseDirectory(s3ProductBase, sciHubProduct.getProductStopTime(), sciHubProduct.getName()); 
		metadataFile = new File(baseDirectory, "metadata.xml");
		previewFile = new File(baseDirectory, "preview.png");
		inspireFile = new File(baseDirectory, "inspire.xml");
		manifestFile = new File(baseDirectory, "manifest.safe");
		auxDirectory = new File(baseDirectory, "aux");
		
		
		
		List<AbstractSciHubProductDatastrip> sciHubDataStrips = sciHubProduct.getDatastrips();
		dataStripMap = IntStream.range(0, sciHubDataStrips.size())
							.mapToObj(i->new S3ProductDatastrip(sciHubDataStrips.get(i), baseDirectory, i))
							.collect(Collectors.toMap(S3ProductDatastrip::getId, Function.identity()));
				
		
		tiles = new ArrayList<>();
		for (AbstractSciHubProductTile sciHubTile:sciHubProduct.getTiles()) {
			tiles.add(new S3ProductTile(this, sciHubTile, s3TilesBase, tileSequenceProvider));
		}
	}
	
	public String getId() {
		return id;
	}
	
	public String getName() {
		return name;
	}

	public Date getTimestamp() {
		return timestamp;
	}
	
	public Date getSciHubIngestionTs() {
		return sciHubIngestionTs;
	}
	
	public File getMetadataFile() {
		return metadataFile;
	}

	public S3ProductDatastrip getDatastrip(String datastripId) {
		return dataStripMap.get(datastripId);
	}
	
	public List<S3ProductDatastrip> getDatastrips() {
		return dataStripMap.values().stream().collect(Collectors.toList());
	}

	public ArrayList<S3ProductTile> getTiles() {
		return tiles;
	}
	
	public File getPreviewFile() {
		return previewFile;
	}
	
	public File getManifestFile() {
		return manifestFile;
	}
	
	public File getInspireFile() {
		return inspireFile;
	}
	
	public File getAuxDirectory() {
		return auxDirectory;
	}

	public File getBaseDirectory() {
		return baseDirectory;
	}

	public String getDatatakeIdentifier() {
		return datatakeIdentifier;
	}
}
