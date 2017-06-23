package de.codecentric.boot.admin.services;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import de.codecentric.boot.admin.config.AdminClientProperties;
import de.codecentric.boot.admin.config.AdminProperties;
import de.codecentric.boot.admin.model.Application;
import xyz.elidom.control.agent.util.ResourceMonitorUtil;

/**
 * Admin Client가 Admin Server에 자신을 등록하거나 제거하는 요청 
 * Registers the client application at spring-boot-admin-server
 */
public class ApplicationRegistrator {

	private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationRegistrator.class);

	private static HttpHeaders HTTP_HEADERS = createHttpHeaders();

	private final AtomicReference<String> registeredId = new AtomicReference<>();

	private AdminClientProperties client;

	private AdminProperties admin;

	private final RestTemplate template;

	public ApplicationRegistrator(RestTemplate template, AdminProperties admin,
			AdminClientProperties client) {
		this.client = client;
		this.admin = admin;
		this.template = template;
	}

	private static HttpHeaders createHttpHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
		return HttpHeaders.readOnlyHttpHeaders(headers);
	}

	/**
	 * Registers the client application at spring-boot-admin-server.
	 *
	 * @return true if successful registration on at least one admin server
	 */
	public boolean register() {
		boolean isRegistrationSuccessful = false;
		Application self = createApplication();
		for (String adminUrl : admin.getAdminUrl()) {
			try {
				@SuppressWarnings("rawtypes")
				ResponseEntity<Map> response = template.postForEntity(adminUrl, 
						new HttpEntity<>(self, HTTP_HEADERS), Map.class);

				if (response.getStatusCode().equals(HttpStatus.CREATED)) {
					if (registeredId.compareAndSet(null, response.getBody().get("id").toString())) {
						LOGGER.info("Application registered itself as {}", response.getBody());
					} else {
						LOGGER.info("Application refreshed itself as {}", response.getBody());
					}

					isRegistrationSuccessful = true;
					if (admin.isRegisterOnce()) {
						break;
					}
				} else {
					LOGGER.warn("Application failed to registered itself as {}. Response: {}", self,
							response.toString());
				}
			} catch (Exception ex) {
				LOGGER.warn("Failed to register application as {} at spring-boot-admin ({}): {}",
						self, admin.getAdminUrl(), ex.getMessage());
			}
		}

		return isRegistrationSuccessful;
	}

	public void deregister() {
		String id = registeredId.get();
		if (id != null) {
			for (String adminUrl : admin.getAdminUrl()) {
				try {
					template.delete(adminUrl + "/" + id);
					registeredId.compareAndSet(id, null);
					if (admin.isRegisterOnce()) {
						break;
					}
				} catch (Exception ex) {
					LOGGER.warn(
							"Failed to deregister application (id={}) at spring-boot-admin ({}): {}",
							id, adminUrl, ex.getMessage());
				}
			}
		}
	}

	protected Application createApplication() {
		// Customized : Application 확장 정보 추가 
		Map<String, Object> extensions = new HashMap<String, Object>();
		extensions.put(Application.EXT_SYSTEM_STATUS_KEY, ResourceMonitorUtil.getCurrentSystemResourceStatus().toMap());
		extensions.put(Application.EXT_APPS_STATUS_KEY, ResourceMonitorUtil.managedAppsStatuses());
		
		return Application.create(client.getName())
				.withHealthUrl(client.getHealthUrl())
				.withManagementUrl(client.getManagementUrl())
				.withServiceUrl(client.getServiceUrl())
				.withExtensions(extensions)
				.build();
	}
}
