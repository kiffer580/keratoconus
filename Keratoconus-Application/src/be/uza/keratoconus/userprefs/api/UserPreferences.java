package be.uza.keratoconus.userprefs.api;

import javafx.geometry.Pos;
import javafx.stage.StageStyle;
import aQute.bnd.annotation.ProviderType;
import be.uza.keratoconus.configuration.api.Classification;
import be.uza.keratoconus.configuration.api.Classification.Category;

/**
 * The GuiConfig service provides access to the user configuration settings.
 * @author Chris Gray
 *
 */
@ProviderType
public interface UserPreferences {

	public static enum ChartType {
		BAR, PIE;
	};
	
	/**
	 * Get the type of chart to be used in the detailed view of the analysis results.
	 * @return
	 */
	ChartType getDetailChartType();
	
	/**
	 * Configure the type of chart to be used in the detailed view of the analysis results.
	 * Also persists the setting in the user preferences.
	 * @param chartType
	 */
	void configureDetailChartType(ChartType chartType);
	
	/**
	 * Set a default value for the type of chart to be used in the detailed view of the analysis results.
	 * This value will only be used if there is no specific user setting.
	 * @param detail_chart_type
	 */
	void setDefaultDetailChartType(String detail_chart_type);
	
	/**
	 * Check whether the main pop-up window is enabled.
	 * (This applies only to the automatic display of the pop-up window: it can always be shown by double-clicking on the system tray icon.)
	 * @return true if enabled, else false.
	 */
	boolean isPopupsEnabled();
	
	/**
	 * Configure whether the main pop-up window is enabled.
	 * Also persists the setting in the user preferences.
	 * @param newVal true to enable, false to disable.
	 */
	void configurePopupsOnOff(boolean newVal);

	/**
	 * Check whether the &ldquo;balloon&rdquo; messages are enabled.
	 * @return true if enabled, else false.
	 */
	boolean isMessagesEnabled();

	/**
	 * Configure whether the &ldquo;balloon&rdquo; messages are enabled.
	 * Also persists the setting in the user preferences.
	 * @param newVal true to enable, false to disable.
	 */
	void configureMessagesOnOff(boolean newVal);

	/**
	 * Check whether animation of the system tray icon is enabled.
	 * @return true if enabled, else false.
	 */
	boolean isAnimateEnabled();

	/**
	 * Configure whether animation of the system tray icon is enabled.
	 * Also persists the setting in the user preferences.
	 * @param newVal true to enable, false to disable.
	 */
	void configureAnimateOnOff(boolean newVal);

	/**
	 * Get the time for which the pop-up window should be displayed for a given classification category.
	 * (This applies only when the window is shown automatically: if it is manually recalled by double-clicking on the system tray icon then it remains visible until it is dismissed by clicking on the window itself.)
	 * @param cat the category
	 * @return the time in seconds.
	 */
	double getDisplayTimeSeconds(Classification.Category cat);

	/**
	 * Configure the time for which the pop-up window should be displayed for a given classification category.
	 * (See note above.) Also persists the setting in the user preferences.
	 * @param cat the category
	 * @param seconds the time in seconds.
	 */
	void configureDisplayTimeSeconds(Category cat, double seconds);
	
	/**
	 * Set the time for which the pop-up window should be displayed if there is no setting for the specific classification category.
	 * @param seconds the time in seconds.
	 */
	void setDefaultDisplayTimeSeconds(double seconds);
	
	/**
	 * Get the &ldquo;home&rdquo; position of the main pop-up window.
	 * @return
	 */
	Pos getMainPopupPosition();

	/**
	 * Configure the &ldquo;home&rdquo; position of the main pop-up window.
	 * Also persists the setting in the user preferences.
	 * @param position The position, as a {@link Pos}.
	 */
	void configureMainPopupPosition(Pos position);
	
	/**
	 * Set a default value for the window position.
	 * This value will only be used if there is no specific user setting.
	 * @param window_position The position, as a string:
	 * <dl compact>
	 * <dt>top left<dd>{@link Pos#TOP_LEFT}
	 * <dt>top center<dd>{@link Pos#TOP_CENTER}
	 * <dt>top right<dd>{@link Pos#TOP_RIGHT}
	 * <dt>center left<dd>{@link Pos#CENTER_LEFT}
	 * <dt>center center<dd>{@link Pos#CENTER}
	 * <dt>center right<dd>{@link Pos#CENTER_RIGHT}
	 * <dt>bottom left<dd>{@link Pos#BOTTOM_LEFT}
	 * <dt>bottom center<dd>{@link Pos#BOTTOM_CENTER}
	 * <dt>bottom right<dd>{@link Pos#BOTTOM_RIGHT}
	 * </dl>
	 */
	void setDefaultWindowPosition(String window_position);
	
	/**
	 * Get the style (in the sense of {@link StageStyle} of the main pop-up window.
	 * @return
	 */
	StageStyle getMainPopupStageStyle();

	/**
	 * Configure the style (in the sense of {@link StageStyle} of the main pop-up window.
	 * Also persists the setting in the user preferences.
	 * @param style
	 */
	void configureMainPopupStageStyle(StageStyle style);
	
	/**
	 * Get directory in which graphic files representing the results of the analysis of each patient exam are to be stored.
	 * @return
	 */
	String getPatientRecordDirectory();
	
	/**
	 * Display the dialogue with which the user can select the patient record directory.
	 */
//	void showPatientRecordDirectoryDialogue();
	
	/**
	 * Get the path (relative to the <tt>/resource</tt> directory of the bundle) to the basic icon for the app.
	 * Example value: <tt>/icon/KTC.png</tt>
	 * @return
	 */
	String getBaseIconPath();
	
	/**
	 * Get the path (relative to the <tt>/resource</tt> directory of the bundle) to a variant icon.
	 * Variants are derived by adding a suffix to the file name before the extension, e.g. variant
	 * &ldquo;large&rdquo; of <tt>/icon/KTC.png</tt> would be  <tt>/icon/KTC-large.png</tt>.
	 * @return
	 */
	String getIconPath(String string);

	/**
	 * Set a default value for the window stage style.
	 * This value will only be used if there is no specific user setting.
	 * @param string
	 */
	void setDefaultWindowStageStyle(String string);

	/**
	 * Configure whether graphical patient records are to be created.
	 * Also persists the setting in the user preferences.
	 * @param newVal true to enable, false to disable.
	 */
	void configureGraphicalPatientRecordOnOff(Boolean newVal);

	/**
	 * Configure the directory where graphical patient records are to be created.
	 * Also persists the setting in the user preferences.
	 * @param path absolute path to the directory.
	 */
	void configurePatientRecordDirectory(String path);

	/**
	 * Check whether graphical patient records are enabled.
	 * @return true if enabled, else false.
	 */
	boolean isPatientRecordEnabled();

	String getSelectedModelName();

}
