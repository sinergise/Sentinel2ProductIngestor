
package com.sinergise.sentinel.l1c.product;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.TimeZone;
import java.util.concurrent.RecursiveTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.s3.AmazonS3;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.sinergise.sentinel.l1c.product.info.ExtendedTileInfo;
import com.sinergise.sentinel.l1c.product.info.ProductInfo;
import com.sinergise.sentinel.l1c.product.info.TileInfo;
import com.sinergise.sentinel.l1c.product.mapping.S3Product;
import com.sinergise.sentinel.l1c.product.mapping.S3ProductDatastrip;
import com.sinergise.sentinel.l1c.product.mapping.S3ProductTile;
import com.sinergise.sentinel.l1c.product.mapping.TileSequenceProvider;
import com.sinergise.sentinel.l1c.product.mapping.scihub.AbstractSciHubProduct;
import com.sinergise.sentinel.l1c.product.mapping.scihub.AbstractSciHubProductDatastrip;
import com.sinergise.sentinel.l1c.product.mapping.scihub.AbstractSciHubProductTile;

/**
 * Transforms SciHub product and stores it to S3
 * @author pkolaric
 *
 */
public class ProductTransformer extends RecursiveTask<Boolean> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	
	
	private static final Logger logger = LoggerFactory.getLogger(ProductTransformer.class);
	
	public static final String PRODUCT_INFO_FILENAME="productInfo.json";
	private File basePath;
	private File s3ProductBase;
	private File s3TilesBase;
	private AbstractSciHubProduct sciHubProduct;
	private AmazonS3 s3;
	private String bucketName;
	private ObjectMapper objectMapper;
	private TileSequenceProvider tileSequenceProvider;
	
	private ProductInfo productInfo;
	
	private ArrayList<UploadToS3Task> forkedTasks = new ArrayList<>();
	
	public ProductTransformer(AbstractSciHubProduct product, File transformedProductBase, AmazonS3 s3, String bucketName, 
			TileSequenceProvider tileSequenceProvider) {
		this.sciHubProduct = product;
		this.s3=s3;
		this.bucketName = bucketName;
		this.basePath = transformedProductBase;
		this.tileSequenceProvider = tileSequenceProvider;
		objectMapper = new ObjectMapper();
		objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
		DateFormat dateFormatIso8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		dateFormatIso8601.setTimeZone(TimeZone.getTimeZone("UTC"));
		objectMapper.setDateFormat(dateFormatIso8601);
	}
	
	private void uploadToS3(File from, File to) {
		if (from.exists() && from.isFile()) {
			UploadToS3Task task = new UploadToS3Task(from, L1CProductConstants.getS3ObjectName(to));
			forkedTasks.add(task);
			task.fork();
		} else {
			logger.error("Expected file {} not found in product bundle!", from);
		}
	}

	private boolean joinTasks() {
		boolean allSucceeded=true;
		for (UploadToS3Task task:forkedTasks) {
			if (!task.join()) {
				allSucceeded=false;
			}
		}
		forkedTasks.clear();
		return allSucceeded;
	}
	
	public ObjectMapper getObjectMapper() {
		return objectMapper;
	}
	
	private class UploadToS3Task extends RecursiveTask<Boolean> {
		private static final long serialVersionUID = 1L;

		private File from;
		private String s3Key;
		
		public UploadToS3Task(File from, String s3Key) {
			this.from = from;
			this.s3Key = s3Key;
		}
		
		@Override
		protected Boolean compute() {
			for (int i=0;i<5;i++) {
				try {
					logger.trace("Uploading {} to S3!", s3Key);
					long start = System.currentTimeMillis();
//					s3.putObject(bucketName, s3Key, from);
					File target = new File("d:/temp/"+s3Key);
					new File(target.getParent()).mkdirs();
					Files.copy(from.toPath(), target.toPath());
					double size = from.length()/(1024.0*1024.0);
					logger.trace("{} uploaded to S3 @ {} mb/s!", s3Key, (size/((System.currentTimeMillis()-start)/1000.0)));
					return true;
				} catch (Exception ex) {
					logger.warn("Error while uploading {} to S3!",s3Key, ex);
				}
			}
			logger.error("Uploading {} to S3 has failed!",s3Key);
			return false;
		}
	}
			
	private File createProductInfoFile(ProductInfo infoDto) throws IOException {
		File productInfoFile = File.createTempFile("productInfo", ".json");
		productInfoFile.deleteOnExit();	
		try (PrintWriter out = new PrintWriter(productInfoFile)) {
			objectMapper.writeValue(out, infoDto);
		}
		return productInfoFile;
	}
	
	
	@Override
	protected Boolean compute() {
		File productInfoFile = null;
		try {
			long ulStartTs = System.currentTimeMillis();
			logger.info("Uploading {} to Amazon S3 bucket {}", sciHubProduct, bucketName);
			// transformedProductBase.mkdir();
			s3ProductBase = L1CProductConstants.createProductBasePath(basePath);
			s3TilesBase = new File(basePath, "tiles");
			S3Product s3Product = new S3Product(sciHubProduct, s3ProductBase, s3TilesBase, tileSequenceProvider);
			productInfo = new ProductInfo(s3Product);

			for (int i = 0; i < sciHubProduct.getTiles().size(); i++) {
				uploadTile(sciHubProduct.getTiles().get(i), s3Product.getTiles().get(i));
			}
			
			uploadToS3(sciHubProduct.getMetadataFile(), s3Product.getMetadataFile());
			
			sciHubProduct.getDatastrips().forEach(ds -> uploadDatastrip(ds, s3Product.getDatastrip(ds.getId())));
			
			uploadToS3(sciHubProduct.getInspireFile(), s3Product.getInspireFile());
			uploadToS3(sciHubProduct.getManifestFile(), s3Product.getManifestFile());
			uploadToS3(sciHubProduct.getPreviewFile(), s3Product.getPreviewFile());

			logger.info("Waiting for {} upload to S3 to finish.", sciHubProduct);
			if (joinTasks()) {
				productInfoFile = createProductInfoFile(productInfo);
				for (int i = 0; i < sciHubProduct.getTiles().size(); i++) {
					uploadToS3(productInfoFile, new File(s3Product.getTiles().get(i).getBaseDirectory(), PRODUCT_INFO_FILENAME));
				}
				// must be last to upload.
				uploadToS3(productInfoFile, new File(s3Product.getBaseDirectory(), PRODUCT_INFO_FILENAME));
				if (joinTasks()) {
					long ulDuration = (System.currentTimeMillis()-ulStartTs)/1000;
					logger.info("Done uploading {} to Amazon S3 bucket {} in {} seconds!", sciHubProduct, bucketName, ulDuration);
					return true;
				}
			} 
			logger.error("Error  uploading {} to Amazon S3 bucket {}!", sciHubProduct, bucketName);
		} catch (IOException ex) {
			logger.error("Error  uploading {} to Amazon S3 bucket {}!", sciHubProduct, bucketName, ex);
		} finally {
			if (productInfoFile!=null) {
				productInfoFile.delete();
			}
		}
		return false;
	}
	
	public ProductInfo getProductInfo() {
		return productInfo;
	}
	
	private void uploadDatastrip(AbstractSciHubProductDatastrip sciHubDatastrip, S3ProductDatastrip s3Datastrip) {
		uploadToS3(sciHubDatastrip.getMetadataFile(), s3Datastrip.getMetadataFile());
		
		File[] sciHubQiFiles = sciHubDatastrip.getQiFiles();
		File[] s3QiFiles = s3Datastrip.getQiFiles();
		for (int i = 0; i < sciHubQiFiles.length; i++) {
			uploadToS3(sciHubQiFiles[i], s3QiFiles[i]);
		}
	}

	private void uploadTile(AbstractSciHubProductTile sciHubTile, S3ProductTile s3Tile) {
			
		uploadToS3(sciHubTile.getMetadataFile(), s3Tile.getMetadataFile());
		uploadToS3(sciHubTile.getPreviewFile(), s3Tile.getPreviewFile());
		
		for (int i = 0; i < sciHubTile.getImageFiles().length; i++) {
			uploadToS3(sciHubTile.getImageFiles()[i], s3Tile.getImageFiles()[i]);
		}
		for (int i = 0; i < sciHubTile.getQiFiles().length; i++) {
			uploadToS3(sciHubTile.getQiFiles()[i], s3Tile.getQiFiles()[i]);
		}
		for (int i = 0; i < sciHubTile.getAuxFiles().length; i++) {
			uploadToS3(sciHubTile.getAuxFiles()[i], s3Tile.getAuxFiles()[i]);
		}
		
		try {
			File tileInfoFile = new File(sciHubTile.getTileDirectory(), "tileInfo.json");
			try (PrintWriter out = new PrintWriter(tileInfoFile)) {
				objectMapper.writeValue(out, new ExtendedTileInfo(new TileInfo(s3Tile), productInfo, sciHubTile.getTileMetadata()));
			}
			uploadToS3(tileInfoFile, s3Tile.getTileInfoFile());
		} catch (Exception ex) {
			logger.error("Failed to create tileInfo.json!", ex);  // we continue anyway..
		}

	}

}
