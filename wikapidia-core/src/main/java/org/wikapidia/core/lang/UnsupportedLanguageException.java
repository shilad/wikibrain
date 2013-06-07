package org.wikapidia.core.lang;

public class UnsupportedLanguageException extends RuntimeException {
	private String langCode;
	
	public UnsupportedLanguageException(String langCode){
		this.langCode = langCode;
	}
	
	@Override
	public String getMessage(){
		return "Unsupported language: " + langCode;
	}

}
