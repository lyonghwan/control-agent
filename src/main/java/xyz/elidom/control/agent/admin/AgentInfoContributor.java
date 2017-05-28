package xyz.elidom.control.agent.admin;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Agent가 Spring Administration Server에 제공하기 위한 커스터마이징 정보를 설정한다.
 * 
 * @author shortstop
 */
@Component
public class AgentInfoContributor implements InfoContributor {
	
	@Autowired
	private Environment env;
	
	/**
	 * info 서비스 URL에서 제공할 추가 정보를 커스터마이징 
	 * 1) app_id : 애플리케이션 ID
	 * 2) name : 애플리케이션 이름 
	 * 3) status : 애플리케이션 상태 
	 * 4) port : 애플리케이션 서비스 포트
	 * 5) monitor_port : 리소스 모니터링 포트 
	 * 6) monitor_url : 모니터링 URL
	 */
    @Override
    public void contribute(Info.Builder builder) {
    		Map<String, String> appInfo = new HashMap<String, String>(); 
    		appInfo.put("appId", "monitor-agent");
    		appInfo.put("name", "모니터링 에이전트");
    		appInfo.put("port", env.getProperty("server.port"));
    		appInfo.put("monitorPort", env.getProperty("info.agent.port"));
        builder.withDetail("appInfo", appInfo);
    }
    
}
