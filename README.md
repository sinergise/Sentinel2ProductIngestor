# About 
Sentinel2ProductIngestor is used to download earth observation data provided by the http://www.esa.int/Our_Activities/Observing_the_Earth/Copernicus/Sentinel-2 satellites and store it to Amazon S3.


# About the data
TODO

## S3 bucket data structure
TODO



# Running

Prerequisites:
1. SciHub account
2. Access to Amazon S3 bucket
3. Enough local storage space to download and unpack SciHub products

	java -Dconfig=ingestor.properties com.sinergise.sentinel.ProductsIngestorRunner

