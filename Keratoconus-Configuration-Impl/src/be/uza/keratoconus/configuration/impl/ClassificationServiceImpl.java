package be.uza.keratoconus.configuration.impl;

import java.util.Map;
import java.util.Set;

import be.uza.keratoconus.configuration.api.Classification;
import be.uza.keratoconus.configuration.api.ClassificationService;
import be.uza.keratoconus.configuration.api.PentacamConfigurationService;
import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;

@Component
public class ClassificationServiceImpl implements ClassificationService {
	private Map<String, Classification> allClassifications;
	private PentacamConfigurationService pentacamConfigurationService;

	@Reference
	protected void setPentacamConfigurationService(PentacamConfigurationService s) {
		pentacamConfigurationService = s;
	}
	
	@Activate
	protected void activate() {
		allClassifications = pentacamConfigurationService
				.getClassifications();
	}

	@Override
	public Classification getByKey(String key) {
		return allClassifications.get(key);
	}

	@Override
	public Set<String> keys() {
		return allClassifications.keySet();
	}

	@Override
	public String getHeadline(String key) {
		return pentacamConfigurationService.getHeadlines().get(key);
	}

}
