package com.sinergise.sentinel.ingestor.resource;

import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sinergise.sentinel.ingestor.ProductIngestor;
import com.sinergise.sentinel.scihub.SciHubEntry;
import com.sinergise.sentinel.scihub.SciHubSearcher;
import com.sinergise.sentinel.scihub.opensearch.OpenSearchResult;

@Path("/status")
@Consumes({MediaType.APPLICATION_JSON + "; charset=utf-8"})
@Produces({MediaType.APPLICATION_JSON + "; charset=utf-8"})
public class StatusResource {
	public static SciHubSearcher shs;
	
	@GET
	@Path("toIngest")
	public List<String> getToIngest() {
		return ProductIngestor.instance().getToIngest().stream().map(i -> i.getName()).collect(Collectors.toList());
	}
	
	@GET
	@Path("ingesting")
	public List<String> getIngesting() {
		return ProductIngestor.instance().getIngesting().stream().map(i -> i.getName()).collect(Collectors.toList());
	}
	
	@GET
	@Path("downloading")
	public List<String> getDownloading() {
		return ProductIngestor.instance().getDownloading().stream().map(i -> i.getName()).collect(Collectors.toList());
	}

	@GET
	@Path("toDownload")
	public List<String> getToDownload() {
		return ProductIngestor.instance().getToDownload().stream().map(i -> i.getName()).collect(Collectors.toList());
	}
	
	@POST
	@Path("addProduct")
	public void addProduct(String productsName) {
		try {
			OpenSearchResult osr =shs.search(null, null, productsName, 0, 100);
			while (osr != null) {
				osr.getFeed().getEntries()
					.forEach(e -> {
						ProductIngestor.instance().addEntry(new SciHubEntry(e));
					});
				osr = shs.next(osr);
			}
		} catch (Exception ex) {
			throw new InternalServerErrorException(ex);
		}
	}

}

