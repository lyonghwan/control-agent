package xyz.elidom.control.agent.job;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
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
	@Autowired
	private AgentController agentCtrl;
	
	@Autowired
	private Environment env;
	
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
	
	
	@Scheduled(cron = "0 50 23 ? * *")
	public void executeNginxLogManage() {
		this.logger.info("Started to Nginx Log Manager ...");
		
		try {
			this.deleteNginxLogFiles();
			this.logger.info("Finished to Nginx Log Manager ...");
		} catch (Exception e) {
			this.logger.error("Failed to Nginx Log Manager ...", e);
		}
	}
	
	private void deleteNginxLogFiles() throws Exception{
		int nginxLogKeepDate = env.getProperty("nginx.log.keep.date", Integer.class, 180);
		String nginxLogPath = env.getProperty("nginx.log.path", String.class, "C:/cj-sms-sub/infra-sw/nginx/nginx-1.10.3/logs");
		
		String accessStartWith = "access_";
		String accessFileTemplate = accessStartWith + "%s.log";
		
		List<String> accessFileList = this.getFileList(nginxLogPath, accessStartWith);
		
		// 보관 대상 파일 필터 
		for(int i = 0 ; i <= nginxLogKeepDate ; i++) {
			accessFileList.remove(String.format(accessFileTemplate, this.getDate(i*-1)));
		}
		
		for(String fileName : accessFileList ) {
			FileUtils.forceDelete(new File(nginxLogPath + "/" + fileName));
		}
	}
	
	private List<String> getFileList(String path, String fileNameStartWith){
		File dirPath = new File(path);
		List<String> fileList = new ArrayList<String>();
		
		File[] pathFiles = dirPath.listFiles();
		for(File pFile : pathFiles) {
			String fileName = pFile.getName();
			if(pFile.isDirectory() == false) {
				if(fileName.startsWith(fileNameStartWith)) fileList.add(fileName);
			}
		}
		return fileList;
	}
	
	private String getDate(int addDate) {
		Date dt = new Date();
		Calendar c = Calendar.getInstance(); 
		c.setTime(dt); 
		c.add(Calendar.DATE, addDate);
		dt = c.getTime();
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		return sdf.format(dt);
	}
}
