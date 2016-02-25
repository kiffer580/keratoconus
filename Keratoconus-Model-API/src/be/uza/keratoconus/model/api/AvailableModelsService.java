package be.uza.keratoconus.model.api;

import java.util.List;

import aQute.bnd.annotation.ProviderType;

/**
 * Service which is used to interact with the WEKA-based model.
 * 
 * @author Chris Gray
 *
 */
@ProviderType
public interface AvailableModelsService {

	/**
	 * Get the names of all available models in this installation.
	 */
	List<String> getAvailableModelNames();

	/**
	 * Get the human-readable description of an available models given its name.
	 */
	String getModelDescription(String name);

	/**
	 * Select the model to be used. This should be called before any of the
	 * methods {@link #getCommonFields()}, {@link #getKeyFields()}
	 * are called.
	 * 
	 * @param name
	 *            The name of the model (must be one of those returned by
	 *            {@link #getAvailableModelNames()}).
	 * @throws Exception 
	 */
	void selectModel(String name) throws Exception;

	/**
	 * Get the name of the currently selected model.
	 * @return
	 */
	String getSelectedModelName();

}
