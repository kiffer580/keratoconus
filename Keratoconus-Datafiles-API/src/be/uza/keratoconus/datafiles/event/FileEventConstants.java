package be.uza.keratoconus.datafiles.event;

public class FileEventConstants extends KeratoconusEventConstants {

	public static final String FILE_NAME = "fileName";
	public static final String SUCCESS_PROPERTY_RECORDS = "records";
	public static final String DATAFILE_WARNING_TOPIC_PREFIX = EVENT_TOPIC_PREFIX + "datafile/warning/";
	public static final String DATAFILE_CHANGED_TOPIC = EVENT_TOPIC_PREFIX + "datafile/changed";
	public static final String DATAFILE_CREATED_TOPIC = EVENT_TOPIC_PREFIX + "datafile/created";
	public static final String DATAFILE_SUCCESS_TOPIC_PREFIX = EVENT_TOPIC_PREFIX + "datafile/success/";
	public static final String CHANGED_PROPERTY_NEW_BYTES = "newBytes";
	public static final String CHANGED_PROPERTY_OLD_BYTES = "oldBytes";

}
