package be.uza.keratoconus.datafiles.event;

public class FileCreatedEvent extends FileEvent {

	public FileCreatedEvent(String baseName) {
		super(FileEventConstants.DATAFILE_CREATED_TOPIC, toProperties(baseName));
	}

}