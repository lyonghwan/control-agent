package xyz.elidom.control.agent.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import xyz.elidom.control.agent.rest.AgentController;

/**
 * ControlAgent가 관리하는 애플리케이션의 로그 파일을 주기적으로 삭제하는 Job
 * 
 * @author shortstop
 */
@Component
public class DeleteAppLogJob {

	/**
	 * Logger
	 */
	private Logger logger = LoggerFactory.getLogger(DeleteAppLogJob.class);
	/**
	 * AgentController
	 */
	private AgentController agentCtrl;
	
	/**
	 * 매일 23시 50분에 (0 50 23 ? * *) 오래된 로그 파일 삭제  
	 */
	@Scheduled(cron = "0 50 23 ? * *")
	public void execute() {
		this.logger.info("Started to Delete Old Application Log Files...");
		
		try {
			this.agentCtrl.deleteOldLogFiles();
			this.logger.info("Finished to Delete Old Application Log Files!");
		} catch (Exception e) {
			this.logger.error("Failed to Delete Old Application Log Files", e);
		}
	}
	
}
