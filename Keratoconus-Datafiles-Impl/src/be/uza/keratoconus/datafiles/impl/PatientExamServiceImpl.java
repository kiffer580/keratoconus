/*
    This file is part of Keratoconus Assistant.

    Keratoconus Assistant is free software: you can redistribute it 
    and/or modify it under the terms of the GNU General Public License 
    as published by the Free Software Foundation, either version 3 of 
    the License, or (at your option) any later version.

    Keratoconus Assistant is distributed in the hope that it will be 
    useful, but WITHOUT ANY WARRANTY; without even the implied warranty 
    of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Keratoconus Assistant.  If not, see 
    <http://www.gnu.org/licenses/>.
 */

package be.uza.keratoconus.datafiles.impl;

import java.text.MessageFormat;
import java.util.LinkedHashMap;
import java.util.Map;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.log.LogService;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import be.uza.keratoconus.datafiles.api.PatientExam;
import be.uza.keratoconus.datafiles.api.PatientExamService;


@Component
public class PatientExamServiceImpl implements PatientExamService {

	private Map<String, PatientExam> allExams = new LinkedHashMap<String, PatientExam>();
	private LogService logService;
	private ComponentContext ownComponentContext;
	
	@Reference
	protected void setLogService(LogService ls) {
		this.logService = ls;
	}
	
	@Activate
	protected void activate(ComponentContext cc) {
		ownComponentContext = cc;
	}
	
	@Override
	public PatientExam createPatientExamRecord(String key) {
		final PatientExamImpl newPatientExam = new PatientExamImpl(this);
		PatientExam existing = allExams.put(key, newPatientExam);
		if (existing != null) {
			logService.log(ownComponentContext.getServiceReference(), LogService.LOG_INFO, "Patient exam record with key '" + key + "' overwrites an existing record");
		}
		return newPatientExam;
	}

	@Override
	public Map<String, PatientExam> getAllPatientExamRecords() {
		return allExams;
	}

	public void warn(String format, Object ... params) {
		String message = MessageFormat.format(format, params);
		logService.log(ownComponentContext.getServiceReference(), LogService.LOG_WARNING, message);
	}

	public void error(String format, Object ... params) {
		String message = MessageFormat.format(format, params);
		logService.log(ownComponentContext.getServiceReference(), LogService.LOG_ERROR, message);
	}

}
