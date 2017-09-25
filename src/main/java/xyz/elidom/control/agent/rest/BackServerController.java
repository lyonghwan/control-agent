package xyz.elidom.control.agent.rest;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@CrossOrigin
@RestController
@RequestMapping("/apps/backup_server")
public class BackServerController {

	protected Logger logger = LoggerFactory.getLogger(BackServerController.class);
	
	@Autowired
	private Environment env;
	
	@Autowired
	private AgentController agentCtrl;
	/**
	 * application list 
	 */
	private List<String> backupApps = null;
	
	private Integer restTimeout = null;
	
	private static final String OK_STR = "OK";
	
	/**
	 * rest template - 타임아웃 3분
	 * 
	 * @return
	 */
	private RestTemplate getRestTemplate() {
	    HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
	    
	    if(this.restTimeout == null) {
		    String timeoutStr = this.env.getProperty("apps.backup.server.start.timeout");
		    this.restTimeout = (timeoutStr == null || timeoutStr.equalsIgnoreCase("")) ? 0 : Integer.parseInt(timeoutStr);	    	
	    }

	    factory.setConnectTimeout(this.restTimeout);
	    factory.setReadTimeout(this.restTimeout);
	    RestTemplate restTemplate = new RestTemplate(factory);
	    return restTemplate;
	}
	
	/**
	 * 데이터 동기화 호출 & Restart  
	 * 
	 * @param domainId
	 * @return
	 */
	@RequestMapping(value = "/start/{domain_id}", method = RequestMethod.POST)
	public String startBackupServer(@PathVariable("domain_id") String domainId) throws Exception {
		// 1. 동기화 서버 호출 
		RestTemplate rest = this.getRestTemplate();
		String appPortKey = "sync.port";
		String syncDataUrl = "http://localhost:" + this.env.getProperty(appPortKey) + "/sync/backupStart?domain_id=" + domainId;
		
		Boolean syncRes = false;
		
		try {
			syncRes = rest.getForObject(syncDataUrl, Boolean.class);
		} catch (Exception e) {
			this.logger.error("Failed to sync data", e);
			throw new Exception("Failed to sync data : " + e.getMessage());			
		}
		
		if(syncRes == false) {
			throw new Exception("Failed to sync data");
		}
		
		// 2. 서버 Restart 
		String result = OK_STR;
		
		try {
			for(String app : this.getBackupApps()) {
				if(OK_STR.equalsIgnoreCase(result)) {
					result = this.startApplication(app);
				}
			}
		} catch (Exception e) {
			this.logger.error("Failed to start application", e);
			throw new Exception("Failed to start application : " + e.getMessage());			
		}
		
		return result;
	}
	
	@RequestMapping(value = "/pause", method = RequestMethod.POST)
	public String pauseBackupServer() throws Exception {
		String result = OK_STR;
		
		try {
			for(String app : this.getBackupApps()) {
				if(OK_STR.equalsIgnoreCase(result)) {
					result = this.stopApplication(app);
				}
			}
		} catch (Exception e) {
			this.logger.error("Failed to restart application", e);
			throw new Exception("Failed to restart application : " + e.getMessage());			
		}
		
		return result;
	}
	
	@RequestMapping(value = "/restart", method = RequestMethod.POST)
	public String restartBackupServer() throws Exception {
		String result = OK_STR;
		
		try {
			for(String app : this.getBackupApps()) {
				if(OK_STR.equalsIgnoreCase(result)) {
					result = this.restartApplication(app);
				}
			}
		} catch (Exception e) {
			this.logger.error("Failed to restart application", e);
			throw new Exception("Failed to restart application : " + e.getMessage());			
		}
		
		return result;
	}
	
	@RequestMapping(value = "/stop/{domain_id}", method = RequestMethod.POST)
	public String stopBackupServer(@PathVariable("domain_id") String domainId) throws Exception {
		// 1. 동기화 서버 호출 
		RestTemplate rest = this.getRestTemplate();
		String appPortKey = "sync.port";
		String syncDataUrl = "http://localhost:" + this.env.getProperty(appPortKey) + "/sync/backupStop?domain_id=" + domainId;
		
		Boolean syncDataRes = false;
		
		try {
			syncDataRes = rest.getForObject(syncDataUrl, Boolean.class);
		} catch (Exception e) {
			this.logger.error("Failed to clear data", e);
			throw new Exception("Failed to clear data : " + e.getMessage());			
		}
		
		if(syncDataRes == false) {
			throw new Exception("Failed to clear data");
		}
		
		// 2. 서버 Stop 
		return this.pauseBackupServer();		
	}
	
	private List<String> getBackupApps() {
		if(this.backupApps == null || this.backupApps.isEmpty()) {
			String backupServerStr = this.env.getProperty("apps.backup.server.apps");
			if(backupServerStr == null || backupServerStr.equalsIgnoreCase("")) {
				backupServerStr = "mgt,agent,monitor";
			}
			
			this.backupApps = Arrays.asList(backupServerStr.split(","));
		}
		
		return this.backupApps;
	}
	
	private String startApplication(String appId) {
		return this.agentCtrl.startBoot(appId);
	}
	
	private String restartApplication(String appId) {
		return this.agentCtrl.restartBoot(appId);
	}
	
	private String stopApplication(String appId) {
		return this.agentCtrl.stopBoot(appId);
	}
	
}
