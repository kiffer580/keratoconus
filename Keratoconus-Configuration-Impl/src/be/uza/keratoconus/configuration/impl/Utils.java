package be.uza.keratoconus.configuration.impl;


public class Utils {

	static final String COMMA = ",";
	static final String SEMICOLON = ";";
	
	public static String[] splitOnComma(String s) {
		return s.split(COMMA);
	}
	
	public static String[] splitOnSemicolon(String s) {
		return s.split(SEMICOLON);
	}

}
