package xyz.elidom.control.agent.job;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import xyz.elidom.control.agent.rest.AgentController;

/**
 * Control Agent가 관리하는 모든 애플리케이션 상태 정보를 통합 관리 서버에 주기적으로 보고하는 Job 
 * 
 * @author shortstop
 */
@Component
public class AppStatusReportJob {

	/**
	 * Logger
	 */
	private Logger logger = LoggerFactory.getLogger(AppStatusReportJob.class);
	/**
	 * 관리 URL
	 */
	private String ADMIN_URL = "http://localhost:7001/admin/api/applications";
	/**
	 * AgentController
	 */
	private AgentController agentCtrl;	
	
	@SuppressWarnings("rawtypes")
	@Scheduled(initialDelay=30000, fixedDelay=30000)
	public void execute() {
		this.logger.info("Started to Report Applications' status...");
		
		try {
			// 자신이 관리하는 애플리케이션 관리 클라이언트들의 목록(Spring Admin API를 호출)을 가져와서 취합하여 서버 API를 호출 ... 
			List<Map> statuses = this.collectApplicationsStatus();
			for(Map statusMap : statuses) {
				String appId = statusMap.get("name").toString();
				String status = null;
				
				Object statusInfo = statusMap.get("statusInfo");
				if(statusInfo != null && statusInfo instanceof Map) {
					status = ((Map)statusInfo).get("status").toString();
				}
				
				this.logger.info("App : " + appId + ", status : " + status);
			}
			
			this.logger.info("Finished to Report Applications' status!");
			
		} catch (Exception e) {
			this.logger.error("Failed to Report Applications' status", e);
		}
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private List<Map> collectApplicationsStatus() {
		RestTemplate rest = new RestTemplate();
		ResponseEntity<Object> results = rest.getForEntity(this.ADMIN_URL, Object.class);
		return (List<Map>)results.getBody();
	}
	
}
