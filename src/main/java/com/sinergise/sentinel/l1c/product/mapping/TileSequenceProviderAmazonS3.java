package com.sinergise.sentinel.l1c.product.mapping;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;

public class TileSequenceProviderAmazonS3 implements TileSequenceProvider {
	public static final Logger logger = LoggerFactory.getLogger(TileSequenceProvider.class);

	private AmazonS3 s3;
	private String bucketName;
	
	private Map<String, AtomicInteger> cachedSequences = new HashMap<>();
	
	public TileSequenceProviderAmazonS3(AmazonS3 s3, String bucketName) {
		this.s3 = s3;
		this.bucketName = bucketName;
	}
	
	
	
	public int getSequence(String s3TileKeyBaseName, String tileId) {
		synchronized (this) {
			AtomicInteger sequencer = cachedSequences.get(s3TileKeyBaseName);
			if (sequencer == null) {
				sequencer = new AtomicInteger(-1);
				cachedSequences.put(s3TileKeyBaseName, sequencer);
			}
			while (true) {
				int proposedId = sequencer.incrementAndGet();
				if (!existsInS3(s3TileKeyBaseName + "/" + proposedId, tileId)) {
					return proposedId;
				}
				if (proposedId > 100) {
					logger.error("Daily tile sequence over 100?!?!!!!");
					System.exit(-1);
				}
			}
		}
	}	
	
	
	private boolean existsInS3(String key, String tileId) {
		S3Object s3MetadataObject=null;;
		try {
			s3MetadataObject = s3.getObject(bucketName, key+"/metadata.xml");
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(s3MetadataObject.getObjectContent());
			doc.getDocumentElement().normalize();
			Element elUserProduct = (Element) doc.getFirstChild();
			Element elGeneralInfo = (Element) elUserProduct.getElementsByTagName("n1:General_Info").item(0);
			Element elTileId = (Element) elGeneralInfo.getElementsByTagName("TILE_ID").item(0);
			String s3TileId = elTileId.getFirstChild().getNodeValue();

			if (tileId.equals(s3TileId)) {
				logger.info("Reuploading existing tile key:{} tileId:{}", key, tileId);
				return false;
			}
			return true;
		} catch (Exception ex) {
			return false;				
		} finally {
			if (s3MetadataObject!=null) {
				try {
					s3MetadataObject.close();
				} catch (IOException ex) {
					logger.error("Failed to close s3Object!",ex);
				}
			}
		}
		
	}

}
