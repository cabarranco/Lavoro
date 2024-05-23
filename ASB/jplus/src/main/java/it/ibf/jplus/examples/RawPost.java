package it.ibf.jplus.examples;

import it.ibf.jplus.Jplus;
import it.ibf.jplus.nodes.Document;

import java.io.IOException;

/**
 * Example program to list links from a URL.
 */
public class RawPost {
    
	public static void main(String[] args) {
        
		String body = "{\"eventId\":"+1418332+",\"marketId\":"+28536045+",\"selectionId\":"+80449839+"}";
		
    	try {
    		
			Document doc = Jplus
					.connect("https://mobile.marathonbet.com/it_IT/betslip/addById")
					.timeout(600000)
					.ignoreContentType(true)
					.followRedirects(false)
					.header("Content-Type", "application/json")
					.rawData(body)
					.post();
			
			System.out.println(doc.body().text());
			
    	} catch (IOException e) {
    		System.out.println("connection fault");
    	}

	}
	
}