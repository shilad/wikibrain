package org.wikibrain.core;

public class WikiBrainException extends Exception {

	public WikiBrainException(Exception e) {
		super(e);
	}

	public WikiBrainException(String string) {
		super(string);
	}
	
	public WikiBrainException(){
		super();
	}
	
	public WikiBrainException(String string, Exception e){
		super(string, e);
	}
	
}

