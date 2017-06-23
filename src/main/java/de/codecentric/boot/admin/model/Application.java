package de.codecentric.boot.admin.model;

import java.io.Serializable;
import java.util.Map;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The domain model for all registered application at the spring boot admin application.
 */
public class Application implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 시스템 리소스 상태 정보 확장 키 - 'systemStatus'
	 */
	public static final String EXT_SYSTEM_STATUS_KEY = "systemStatus";
	/**
	 * 시스템 애플리케이션 상태 정보 확장 키 - 'appsStatus'
	 */
	public static final String EXT_APPS_STATUS_KEY = "appsStatus";
	
	private final String id;
	private final String name;
	private final String managementUrl;
	private final String healthUrl;
	private final String serviceUrl;
	private final StatusInfo statusInfo;
	/**
	 * 애플리케이션 확장 모델 - systemStatus : 시스템 리소스 상태, appsStatus : 관리 Application 상태 정보 
	 */
	private Map<String, Object> extensions;

	protected Application(Builder builder) {
		Assert.hasText(builder.name, "name must not be empty!");
		Assert.hasText(builder.healthUrl, "healthUrl must not be empty!");
		Assert.notNull(builder.statusInfo, "statusInfo must not be null!");
		this.healthUrl = builder.healthUrl;
		this.managementUrl = builder.managementUrl;
		this.serviceUrl = builder.serviceUrl;
		this.name = builder.name;
		this.id = builder.id;
		this.statusInfo = builder.statusInfo;
		this.extensions = builder.extensions;
	}

	@JsonCreator
	public static Application fromJson(
			@JsonProperty("url") String url,
			@JsonProperty("managementUrl") String managementUrl,
			@JsonProperty("healthUrl") String healthUrl,
			@JsonProperty("serviceUrl") String serviceUrl, 
			@JsonProperty("name") String name,
			@JsonProperty("id") String id, 
			@JsonProperty("statusInfo") StatusInfo statusInfo) {

		Builder builder = create(name).withId(id);
		if (statusInfo != null) {
			builder.withStatusInfo(statusInfo);
		}

		if (StringUtils.hasText(url)) {
			// old format
			builder.withHealthUrl(url.replaceFirst("/+$", "") + "/health").withManagementUrl(url);
		} else {
			builder.withHealthUrl(healthUrl)
				   .withManagementUrl(managementUrl)
				   .withServiceUrl(serviceUrl);
		}
		return builder.build();
	}

	public static Builder create(String name) {
		return new Builder(name);
	}

	public static Builder create(Application application) {
		return new Builder(application);
	}

	public static class Builder {
		private String id;
		private String name;
		private String managementUrl;
		private String healthUrl;
		private String serviceUrl;
		private StatusInfo statusInfo = StatusInfo.ofUnknown();
		private Map<String, Object> extensions;

		private Builder(String name) {
			this.name = name;
		}

		private Builder(Application application) {
			this.healthUrl = application.healthUrl;
			this.managementUrl = application.managementUrl;
			this.serviceUrl = application.serviceUrl;
			this.name = application.name;
			this.id = application.id;
			this.statusInfo = application.statusInfo;
			this.extensions = application.extensions;
		}

		public Builder withName(String name) {
			this.name = name;
			return this;
		}

		public Builder withId(String id) {
			this.id = id;
			return this;
		}

		public Builder withServiceUrl(String serviceUrl) {
			this.serviceUrl = serviceUrl;
			return this;
		}

		public Builder withHealthUrl(String healthUrl) {
			this.healthUrl = healthUrl;
			return this;
		}

		public Builder withManagementUrl(String managementUrl) {
			this.managementUrl = managementUrl;
			return this;
		}

		public Builder withStatusInfo(StatusInfo statusInfo) {
			this.statusInfo = statusInfo;
			return this;
		}
		
		public Builder withExtensions(Map<String, Object> extensions) {
			this.extensions = extensions;
			return this;
		}

		public Application build() {
			return new Application(this);
		}
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getManagementUrl() {
		return managementUrl;
	}

	public String getHealthUrl() {
		return healthUrl;
	}

	public String getServiceUrl() {
		return serviceUrl;
	}

	public StatusInfo getStatusInfo() {
		return statusInfo;
	}
	
	public Map<String, Object> getExtensions() {
		return extensions;
	}

	public void setExtensions(Map<String, Object> extensions) {
		this.extensions = extensions;
	}

	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer("{id:");
		buffer.append(id).append(", name:").append(name).append(", managementUrl:").append(managementUrl)
			  .append(", healthUrl:").append(healthUrl).append(", serviceUrl:").append(serviceUrl);
		
		if(this.extensions != null && !this.extensions.isEmpty()) {
			buffer.append(",").append(this.extensions.toString());
		}

		buffer.append("}");
		return buffer.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((healthUrl == null) ? 0 : healthUrl.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((managementUrl == null) ? 0 : managementUrl.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((serviceUrl == null) ? 0 : serviceUrl.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Application other = (Application) obj;
		if (healthUrl == null) {
			if (other.healthUrl != null) {
				return false;
			}
		} else if (!healthUrl.equals(other.healthUrl)) {
			return false;
		}
		if (id == null) {
			if (other.id != null) {
				return false;
			}
		} else if (!id.equals(other.id)) {
			return false;
		}
		if (managementUrl == null) {
			if (other.managementUrl != null) {
				return false;
			}
		} else if (!managementUrl.equals(other.managementUrl)) {
			return false;
		}
		if (name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!name.equals(other.name)) {
			return false;
		}
		if (serviceUrl == null) {
			if (other.serviceUrl != null) {
				return false;
			}
		} else if (!serviceUrl.equals(other.serviceUrl)) {
			return false;
		}
		return true;
	}

}
