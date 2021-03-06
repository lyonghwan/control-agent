package xyz.elidom.control.agent.admin;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Health.Builder;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.stereotype.Component;

import de.codecentric.boot.admin.model.Application;
import xyz.elidom.control.agent.rest.AgentController;
import xyz.elidom.control.agent.util.ResourceMonitorUtil;

@Component
public class HealthCheck implements HealthIndicator {
 	
	@Autowired
	private AgentController agentCtrl;
		
    @Override
    public Health health() {
        int errorCode = check();
        if (errorCode != 0) {
            return Health.down().withDetail("Error Code", errorCode).build();
        }
        
        return this.getCustomHealth();
    }
     
    public int check() {
        // Your logic to check health - 각 서브터미널의 애플리케이션별로 접속 체크해서 결과를 리턴 
        return 0;
    }
    
    /**
     * custom health 정보 리턴 
     * 
     * @return
     */
    public Health getCustomHealth() {
    	// 서브터미널의 애플리케이션 별 상태 및 리소스 상태 리턴
    	Map<String, Object> agentDetails = this.buildHealthDetails();
    	Builder builder = new Builder(Status.UP, agentDetails);
    	return builder.build();
    }
    
    /**
     * health 정보의 상세 정보를 리턴 
     * 
     * @return
     */
    private Map<String, Object> buildHealthDetails() {
    	Map<String, Object> details = new HashMap<String, Object>();
    	// 1. ontrol Agent가 실행되고 있는 System 들의 리소스 상태 정보를 추가 
    	details.put(Application.EXT_SYSTEM_STATUS_KEY, ResourceMonitorUtil.getCurrentSystemResourceStatus().toMap());
    	// 2. Control Agent가 관리하는 Application들의 상태 정보를 리턴 
    	details.put(Application.EXT_APPS_STATUS_KEY, this.agentCtrl.managedAppsStatus());
    	return details;
    }
    
}
