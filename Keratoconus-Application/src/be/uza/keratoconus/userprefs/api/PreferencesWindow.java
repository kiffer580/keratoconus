package be.uza.keratoconus.userprefs.api;

import javafx.scene.Node;

public interface PreferencesWindow {

	static final String STYLE_CLASS_BUTTON = "ka-button";
	static final String STYLE_CLASS_TITLE = "ka-title";
	static final String STYLE_CLASS_DESCRIPTION = "ka-description";
	static final String STYLE_CLASS_SLIDER = "ka-slider";
	static final String STYLE_CLASS_CHECKBOX = "ka-checkbox";
	public static final String STYLE_CLASS_ROOT = "ka-root";
	public static final String STYLE_CLASS_FOOTER = "ka-footer";
	public static final String STYLE_CLASS_CONTENT = "ka-content";
	public static final String STYLE_CLASS_HEADER = "ka-header";

	void setup(UserPreferences prefs);
	
	String getTitle();
	
	String getHeader();
	
	Node getContent();

	String getFooter();

}
