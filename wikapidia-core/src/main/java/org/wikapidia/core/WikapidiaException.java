package org.wikapidia.core;

public class WikapidiaException extends Exception {

	public WikapidiaException(Exception e) {
		super(e);
	}

	public WikapidiaException(String string) {
		super(string);
	}
	
	public WikapidiaException(){
		super();
	}
	
	public WikapidiaException(String string, Exception e){
		super(string, e);
	}
	
}

