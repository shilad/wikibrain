package org.wikibrain.webapi;

public class WikiBrainWebException extends RuntimeException {

	public WikiBrainWebException(Exception e) {
		super(e);
	}

	public WikiBrainWebException(String string) {
		super(string);
	}
	
	public WikiBrainWebException(){
		super();
	}
	
	public WikiBrainWebException(String string, Exception e){
		super(string, e);
	}
	
}

