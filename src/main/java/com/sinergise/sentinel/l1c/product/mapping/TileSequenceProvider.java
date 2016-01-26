package com.sinergise.sentinel.l1c.product.mapping;

public interface TileSequenceProvider {
	public int getSequence(String s3TileKeyBaseName, String tileId);
}
