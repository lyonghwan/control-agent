/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.codecentric.boot.admin.registry.web;

import java.util.Collection;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import de.codecentric.boot.admin.model.Application;
import de.codecentric.boot.admin.model.Application.Builder;
import de.codecentric.boot.admin.model.StatusInfo;
import de.codecentric.boot.admin.model.SystemStatus;
import de.codecentric.boot.admin.registry.ApplicationRegistry;
import de.codecentric.boot.admin.web.AdminController;

/**
 * REST controller for controlling registration of managed applications.
 */
@AdminController
@ResponseBody
@RequestMapping("/api/applications")
public class RegistryController {

	private static final Logger LOGGER = LoggerFactory.getLogger(RegistryController.class);

	private final ApplicationRegistry registry;

	public RegistryController(ApplicationRegistry registry) {
		this.registry = registry;
	}

	/**
	 * Register an application within this admin application.
	 *
	 * @param app The application infos.
	 * @return The registered application.
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping(method = RequestMethod.POST)
	public ResponseEntity<Application> register(@RequestBody Map<String, Object> appInfo) {
		String id = appInfo.containsKey("id") ? (String)appInfo.get("id") : null;
		String name = appInfo.get("name").toString();
		String managementUrl = appInfo.get("managementUrl").toString();
		String healthUrl = appInfo.get("healthUrl").toString();
		String serviceUrl = appInfo.get("serviceUrl").toString();
		
		Builder builder = Application.create(name).withHealthUrl(healthUrl).withManagementUrl(managementUrl).withServiceUrl(serviceUrl);
		if(id != null) {
			builder = builder.withId(id);
		}
		
		Map<String, Object> statusMap = (Map<String,Object>)appInfo.get("statusInfo");
		String statusCode = (String)statusMap.get("status");
		Long timestamp = (Long)statusMap.get("timestamp");
		StatusInfo statusInfo = null;
		
		if(statusMap.containsKey("systemStatus")) {
			Map<String, Object> systemMap = (Map<String, Object>)statusMap.get("systemStatus");
			SystemStatus ss = new SystemStatus();
			ss.setName(systemMap.containsKey("name") ? (String)systemMap.get("name") : null);
			ss.setVersion(systemMap.containsKey("version") ? (String)systemMap.get("version") : null);
			ss.setArch(systemMap.containsKey("arch") ? (String)systemMap.get("arch") : null);
			ss.setSystemCpuLoad(systemMap.containsKey("systemCpuLoad") ? (double)systemMap.get("systemCpuLoad") : 0);
			ss.setSystemLoadAverage(systemMap.containsKey("systemLoadAverage") ? (double)systemMap.get("systemLoadAverage") : 0);
			ss.setTotalDiskSpace(systemMap.containsKey("totalDiskSpace") ? (long)systemMap.get("totalDiskSpace") : 0);
			ss.setFreeDiskSpace(systemMap.containsKey("freeDiskSpace") ? (long)systemMap.get("freeDiskSpace") : 0);
			ss.setTotalPhysicalMemorySize(systemMap.containsKey("totalPhysicalMemorySize") ? (long)(double)systemMap.get("totalPhysicalMemorySize") : 0);
			ss.setFreePhysicalMemorySize(systemMap.containsKey("freePhysicalMemorySize") ? (long)(double)systemMap.get("freePhysicalMemorySize") : 0);
			ss.setTotalSwapSpaceSize(systemMap.containsKey("totalSwapSpaceSize") ? (long)(double)systemMap.get("totalSwapSpaceSize") : 0);
			ss.setFreeSwapSpaceSize(systemMap.containsKey("freeSwapSpaceSize") ? (long)(double)systemMap.get("freeSwapSpaceSize") : 0);
			statusInfo = StatusInfo.valueOf(statusCode, timestamp, ss);
			
		} else {
			statusInfo = StatusInfo.valueOf(statusCode, timestamp);
		}
		
		if(statusInfo != null) {
			builder = builder.withStatusInfo(statusInfo);
		}		
		
		Application app = builder.build();
		LOGGER.debug("Register application {}", app.toString());
		Application registeredApp = registry.register(app);
		return ResponseEntity.status(HttpStatus.CREATED).body(registeredApp);
	}

	/**
	 * List all registered applications with name
	 *
	 * @param name the name to search for
	 * @return List
	 */
	@RequestMapping(method = RequestMethod.GET)
	public Collection<Application> applications(
			@RequestParam(value = "name", required = false) String name) {
		LOGGER.debug("Deliver registered applications with name={}", name);
		if (name == null || name.isEmpty()) {
			return registry.getApplications();
		} else {
			return registry.getApplicationsByName(name);
		}
	}

	/**
	 * Get a single application out of the registry.
	 *
	 * @param id The application identifier.
	 * @return The registered application.
	 */
	@RequestMapping(value = "/{id}", method = RequestMethod.GET)
	public ResponseEntity<?> get(@PathVariable String id) {
		LOGGER.debug("Deliver registered application with ID '{}'", id);
		Application application = registry.getApplication(id);
		if (application != null) {
			return ResponseEntity.ok(application);
		} else {
			return ResponseEntity.notFound().build();
		}
	}

	/**
	 * Unregister an application within this admin application.
	 *
	 * @param id The application id.
	 * @return the unregistered application.
	 */
	@RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
	public ResponseEntity<?> unregister(@PathVariable String id) {
		LOGGER.debug("Unregister application with ID '{}'", id);
		Application application = registry.deregister(id);
		if (application != null) {
			return ResponseEntity.ok(application);
		} else {
			return ResponseEntity.notFound().build();
		}
	}

}
