package xyz.elidom.control.agent.rest;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.input.ReversedLinesFileReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import xyz.elidom.control.agent.util.StreamPrinter;

@RestController
public class AgentController {

	protected Logger logger = LoggerFactory.getLogger(AgentController.class);

	private static final String FILE_SEPARATOR = "/";
	private static final String LOG_FILENAME = "applicatoin.";
	private static final String LOG_FILENAME_2 = "application.";
	private static final String LOG_FILE_EXT = ".log";

	@Autowired
	private Environment env;
	/**
	 * Index Information
	 */
	private Map<String, Object> indexInfo = new HashMap<String, Object>();

	/**
	 * Elidom Control Agent Index URL 
	 */
	@RequestMapping(method = RequestMethod.GET)
	public Map<String, Object> index() {
		
		if(indexInfo.isEmpty()) {
			indexInfo.put("Application", "Elidom Control Agent");
			indexInfo.put("Version", "1.0");
			
			Map<String, Object> urlInfo = new HashMap<String, Object>();
			urlInfo.put("Control Agent Health Check", "/ping:GET");
			urlInfo.put("Application Start By AppId", "/apps/{app_id}/start:POST");
			urlInfo.put("Application Restart By AppId", "/apps/{app_id}/restart:POST");
			urlInfo.put("Application Stop By AppId", "/apps/{app_id}/stop:POST");
			urlInfo.put("Application Deploy By AppId", "/apps/{app_id}/deploy:POST");
			urlInfo.put("Application View Today's Log By AppId", "/apps/{app_id}/log/today/read:GET");
			urlInfo.put("Application Log File List By AppId", "/apps/{app_id}/log/files:GET");
			urlInfo.put("Application Download Today's Log By AppId", "/apps/{app_id}/log/today/download:GET");
			urlInfo.put("Application View Log By AppId, Log File Name", "/apps/{app_id}/log/{file_name}/read:GET");
			urlInfo.put("Application Delete File By AppId, Log File Name", "/apps/{app_id}/log/{file_name}/delete:DELETE");
			urlInfo.put("Application Download Log By AppId, Log File Name", "/apps/{app_id}/log/{file_name}/download:GET");
			urlInfo.put("View Application Informations", "/apps/infos:GET");
			urlInfo.put("View Application Information By AppId", "/apps/{app_id}/info:GET");
			urlInfo.put("Redis Flushall By AppId", "/apps/{app_id}/redis_flushall:DELETE");
			
			indexInfo.put("apis", urlInfo);
		}
		
		return indexInfo;
	}
	
	/**
	 * ping & pong
	 */
	@RequestMapping(value = "/ping", method = RequestMethod.GET)
	public String ping() {
		return "pong";
	}

	/**
	 * Control Agent가 관리하는 모든 애플리케이션 요약 정보를 리턴 
	 * 
	 * @return
	 */
	@RequestMapping(value = "/apps/infos", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public List<Object> getAppInfos() {
		String[] ids = this.getPropertiesByKey(this.env, "apps.id");

		if (ids == null || ids.length == 0) {
			throw new RuntimeException("Application Id is empty!");
		}

		List<Object> apps = new ArrayList<Object>();

		for (int i = 0; i < ids.length; i++) {
			Map<String, Object> appInfo = this.getAppInfo(ids[i]);
			if(appInfo != null && !appInfo.isEmpty()) {
				apps.add(appInfo);
			}
		}

		return apps;
	}

	/**
	 * Control Agent가 관리하는 모든 애플리케이션 중 appId에 해당하는 애플리케이션 요약 정보를 리턴 
	 * 
	 * @param appId
	 * @return
	 */
	@RequestMapping(value = "/apps/{app_id}/info", method = RequestMethod.GET)
	private Map<String, Object> getAppInfo(@PathVariable("app_id") String appId) {
		Map<String, String> props = this.checkProperties(appId, "info");
		
		RestTemplate rest = new RestTemplate();
		String appPort = props.get("PORT");
		String url = this.getAgentUrl(appPort, "info");
		
		Map<String, Object> appInfo = new HashMap<String, Object>();
		appInfo.put("id", appId);
		appInfo.put("port", appPort);
		
		try {
			Map<?, ?> result = rest.getForObject(url, Map.class, appInfo);
			String appStage = (String)result.get("stage");
			String appTitle = (String)result.get("name");
			appInfo.put("name", appTitle);
			appInfo.put("stage", appStage);
			
		} catch(Exception e) {
			appInfo.put("status", "DOWN");
			return appInfo;
		}
		
		url = this.getAgentUrl(appPort, "health");
		try {
			Map<?, ?> status = rest.getForObject(url, Map.class, new HashMap<String, Object>());
			appInfo.put("status", status.get("status"));
		} catch(Exception e) {
			appInfo.put("status", "DOWN");
		}
					
		return appInfo;
	}
	
	/**
	 * Agent URL를 리턴 
	 * 
	 * @param appPort
	 * @param api
	 * @return
	 */
	private String getAgentUrl(String appPort, String api) {
		return "http://localhost:" + appPort + "/" + api;
	}
	
	/**
	 * Redis Flushall 
	 * 
	 * @param appId
	 * @return execute message
	 */
	@RequestMapping(value = "/apps/{app_id}/redis_flushall", method = RequestMethod.DELETE)
	public String redisFlushall(@PathVariable("app_id") String appId) {
		HashMap<String, String> pMap = this.checkProperties(appId, "redisFlushall");
		
		try {
			this.commandStart(pMap.get("PATH"));
		} catch (Exception e) {
			return "Error : \n\n" + e.getMessage();
		}
		
		return "Entered Redis Flushall Command SUCCESS";
	}
	
	/**
	 * Application Start
	 * 
	 * @param appId
	 * @return execute message
	 */
	@RequestMapping(value = "/apps/{app_id}/start", method = RequestMethod.POST)
	public String startBoot(@PathVariable("app_id") String appId) {
		HashMap<String, String> pMap = this.checkProperties(appId, "start");

		try {
			this.commandStart(pMap.get("PATH"));
		} catch (Exception e) {
			return "Error : \n\n" + e.getMessage();
		}

		return "Entered Startup Command SUCCESS";		
	}

	/**
	 * Application Restart
	 * 
	 * @param appId
	 * @return execute message
	 */
	@RequestMapping(value = "/apps/{app_id}/restart", method = RequestMethod.POST)
	public String retartBoot(@PathVariable("app_id") String appId) {
		try {
			this.stopBoot(appId);
		} catch (Exception e) {
			this.logger.error("Failed to shutdown application : " + e.getMessage());
		}

		try {
			Thread.sleep(5000);
		} catch (Exception e) {
		}

		return this.startBoot(appId);
	}

	/**
	 * Application Stop
	 * 
	 * @param appId
	 * @return execute message
	 */
	@RequestMapping(value = "/apps/{app_id}/stop", method = RequestMethod.POST)
	public String stopBoot(@PathVariable("app_id") String appId) {
		RestTemplate rest = new RestTemplate();
		String port = this.env.getProperty(appId + ".port");
		String url = "http://localhost:" + port + "/shutdown";
		ResponseEntity<String> response = rest.postForEntity(url, "", String.class);
		return response.getBody();
	}

	/**
	 * Application Deploy
	 * 
	 * @param appId
	 * @return execute message
	 */
	@RequestMapping(value = "/apps/{app_id}/deploy", method = RequestMethod.POST)
	public String deploy(@PathVariable("app_id") String appId) {
		this.stopBoot(appId);

		try {
			Thread.sleep(5000);
		} catch (Exception e) {
		}

		HashMap<String, String> pMap = this.checkProperties(appId, "update");

		try {
			this.commandStart(pMap.get("PATH"));
		} catch (Exception e) {
			return "Failed to update application execution file : " + e.getMessage();
		}

		this.startBoot(appId);
		return "OK";
	}

	/**
	 * Log File Path를 리턴
	 * 
	 * @param appId
	 * @return
	 */
	private String getLogFilePath(String appId, boolean first) {
		HashMap<String, String> pMap = this.checkProperties(appId, "log");
		String path = pMap.get("PATH");
		Date today = null; 
		
		// 첫번째 오늘 날짜 두번째 어제 날짜
		if(first) {
			today = new Date();
			
		} else {
			Calendar c = Calendar.getInstance(); 
			c.setTime(new Date()); 
			c.add(Calendar.DATE, -1);
			today = c.getTime();
		}
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		StringBuffer logPath = new StringBuffer();
		logPath.append(path).append(path.endsWith(FILE_SEPARATOR) ? "" : FILE_SEPARATOR).append(LOG_FILENAME).append(sdf.format(today)).append(LOG_FILE_EXT);

		File file = new File(logPath.toString());
		if (!file.exists()) {
			logPath = new StringBuffer();
			logPath.append(path).append(path.endsWith(FILE_SEPARATOR) ? "" : FILE_SEPARATOR).append(LOG_FILENAME_2).append(sdf.format(today)).append(LOG_FILE_EXT);
			file = new File(logPath.toString());

			if (!file.exists()) {
				if(first) { 
					return this.getLogFilePath(appId, false);
				} else {
					throw new RuntimeException("Log File (" + logPath.toString() + ") Not Found!");
				}
			}
		}

		return logPath.toString();
	}
	
	/**
	 * Log File Path를 리턴
	 * 
	 * @param appId
	 * @param fileName
	 * @return
	 */	
	private String getLogFilePath(String appId, String fileName) {
		HashMap<String, String> pMap = this.checkProperties(appId, "log");
		String path = pMap.get("PATH");
		if(!path.endsWith(FILE_SEPARATOR)) {
			return path + FILE_SEPARATOR + fileName;
		} else {
			return path + fileName;
		}
	}
	
	/**
	 * 로그 파일 리스트 
	 * 
	 * @param appId
	 * @return
	 */
	@RequestMapping(value = "/apps/{app_id}/log/files", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public List<Map<String, Object>> logFileList(@PathVariable("app_id") String appId) {
		HashMap<String, String> pMap = this.checkProperties(appId, "log");
		String path = pMap.get("PATH");
		File logDir = new File(path);
		List<Map<String, Object>> fileList = new ArrayList<Map<String, Object>>();
		
		if(logDir.isDirectory()) {
			File[] logFiles = logDir.listFiles();
			for(File logFile : logFiles) {
				String fileName = logFile.getName();
				if(fileName.endsWith(".log")) {
					String logPath = logFile.getAbsolutePath();
					Long fileSize = logFile.length();
					Map<String, Object> logData = new HashMap<String, Object>(3);
					logData.put("id", logPath);
					logData.put("name", fileName);
					logData.put("size", fileSize);
					fileList.add(logData);
				}
			}
		}
		
		fileList.sort(new LogFileComparator());
		return fileList;
	}
	
	/**
	 * 로그 파일 삭제  
	 * 
	 * @param appId
	 * @param fileName
	 * @return
	 */
	@RequestMapping(value = "/apps/{app_id}/log/{file_name}/delete", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
	public Boolean deleteFileList(@PathVariable("app_id") String appId, @PathVariable("file_name") String fileName) {
		String filePath = this.getLogFilePath(appId, fileName);
		File logFile = new File(filePath);
		
		if(logFile.exists() && logFile.isFile()) {
			logFile.delete();
		}
		
		return true;
	}
	
	/**
	 * 로그 파일을 읽어서 내용을 리턴
	 * 
	 * @param appId
	 * @param fileName
	 * @param lines
	 * @return 로그 파일의 내용을 읽어서 리턴  
	 */
	@RequestMapping(value = "/apps/{app_id}/log/{file_name}/read", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public Map<String, String> readLog(
			@PathVariable("app_id") String appId,
			@PathVariable("file_name") String fileName,
			@RequestParam(name = "lines", required = false) Integer lines) {
		
		String logPath = (fileName != null) ? 
				this.getLogFilePath(appId, fileName) : 
				this.getLogFilePath(appId, true);
				
		String content = (lines == null || lines == 0) ? 
						 this.readAllLines(logPath) : 
						 this.readLastLines(new File(logPath), lines);

		Map<String, String> result = new HashMap<String, String>();
		result.put("id", "1");
		result.put("log", content);
		return result;		
	}

	/**
	 * 오늘의 로그 파일을 읽어서 내용을 리턴
	 * 
	 * @param appId
	 * @param lines
	 * @return 오늘의 로그의 내용을 리턴 
	 */
	@RequestMapping(value = "/apps/{app_id}/log/today/read", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public Map<String, String> readTodayLog(
			@PathVariable("app_id") String appId,
			@RequestParam(name = "lines", required = false) Integer lines) {
		return this.readLog(appId, null, lines);
	}
	
	/**
	 * 로그 파일을 다운로드 
	 * 
	 * @param appId
	 * @param fileName
	 * @return filePath에 해당하는 로그의 내용을 리턴 
	 */
	@RequestMapping(value = "/apps/{app_id}/log/{file_name}/download", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public boolean downloadLog(HttpServletRequest req, HttpServletResponse res, @PathVariable("app_id") String appId, @PathVariable("file_name") String fileName) {
		String logPath = (fileName != null) ? 
				this.getLogFilePath(appId, fileName) : 
				this.getLogFilePath(appId, true);
		File file = new File(logPath);
		
		res.setCharacterEncoding("UTF-8");
		res.setContentType("text/plain;charset=UTF-8");
		res.addHeader("Content-Type", "application/octet-stream");
		res.addHeader("Content-Transfer-Encoding", "binary;");
		res.addHeader("Content-Length", Long.toString(file.length()));
		res.setHeader("Pragma", "cache");
		res.setHeader("Cache-Control", "public");
		
		ServletOutputStream outStream = null;
		ByteArrayInputStream inStream = null;
		byte[] buffer = new byte[4096];
		
		try {
			res.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(file.getName(), "UTF-8"));
			outStream = res.getOutputStream();
			inStream = new ByteArrayInputStream(FileUtils.readFileToByteArray(file));			
			int byteCount = 0;
			
			do {
				byteCount = inStream.read(buffer);
				if(byteCount == -1) {
					break;
				}
				
				outStream.write(buffer, 0, byteCount);
				outStream.flush();
			} while(true);

		} catch (Exception e) {
			throw new RuntimeException("Failed to File Download!", e);
			
		} finally {
			try {
				inStream.close();
			} catch (IOException e) {
			}
			
			try {
				outStream.close();
			} catch (IOException e) {
			}
		}
		
		return true;		
	}
	
	/**
	 * 로그 파일을 다운로드 
	 * 
	 * @param appId
	 * @return 오늘의 로그의 내용을 리턴 
	 */
	@RequestMapping(value = "/apps/{app_id}/log/today/download", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public boolean downloadLog(HttpServletRequest req, HttpServletResponse res, @PathVariable("app_id") String appId) {
		return this.downloadLog(req, res, appId, null);
	}
	
	/**
	 * 파일의 모든 라인 읽어 리턴 
	 * 
	 * @param logPath
	 * @return
	 */
	private String readAllLines(String logPath) {
		StringBuffer content = new StringBuffer();
		FileReader fReader = null;
		BufferedReader bReader = null;

		try {
			fReader = new FileReader(logPath);
			bReader = new BufferedReader(fReader);
			String temp = null;
			
			while((temp = bReader.readLine()) != null) {
			    content.append(temp).append("\n");
			}
			
			return content.toString();
			
		} catch(FileNotFoundException e) {
			throw new RuntimeException("Log file not found!", e);
			
		} catch (Exception e) {
			throw new RuntimeException("Log file not found!", e);
			
		} finally {
			if(bReader != null) {
				try {
					bReader.close();
				} catch (IOException e) {
				}
			}
			if(fReader != null) {
				try {
					fReader.close();
				} catch (IOException e) {
				}
			}
		}
	}
	
	/**
	 * 마지막 lines 라인을 읽어 리턴 
	 * 
	 * @param filePath
	 * @param lines
	 * @return
	 */
	private String readLastLines(File file, int lines) {
		StringBuilder builder = new StringBuilder();
		ReversedLinesFileReader rlfr = null;
		
		try {
			rlfr = new ReversedLinesFileReader(file, Charset.forName("UTF-8"));
			String temp = null;
			int counter = 0;

			while(counter < lines) {
				try {
					temp = rlfr.readLine();
				} catch (NullPointerException npe) {
					break;
				}
				
				if(temp != null) {
					builder.insert(0, temp + "\n");
					counter++;
				}
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to read log file!", e);
			
		} finally {
			try {
				rlfr.close();
			} catch (IOException e) {
			}
		}
		
		return builder.toString();
	}

	/**
	 * Command 실행
	 * 
	 * @param path
	 * @throws Exception
	 */
	private void commandStart(String path) throws Exception {
		ProcessBuilder pBuilder = new ProcessBuilder();
		pBuilder.command(path);
		Process process = pBuilder.start();
		StreamPrinter.printStream(process);

		while (process.isAlive()) {
			Thread.sleep(100);
		}
	}

	/**
	 * appId별 actionCode에 대응하는 프로퍼티를 찾아 리턴
	 * 
	 * @param appId
	 * @param actionCode
	 * @return HashMap RESULT : 성공 혹은 실패 MSG : 실패시 에러 메시지 PATH : 액션 코드별 값
	 */
	private HashMap<String, String> checkProperties(String appId, String actionCode) {
		HashMap<String, String> retMap = new HashMap<String, String>();
		retMap.put("RESULT", "SUCCESS");
		String[] ids = this.getPropertiesByKey(this.env, "apps.id");

		if (ids == null) {
			throw new RuntimeException("Failed to get app information! - " + this.getReturnMsg(1));
		}

		boolean isExists = false;
		for (String id : ids) {
			if (id.equals(appId)) {
				isExists = true;
				break;
			}
		}

		if (isExists == false) {
			throw new RuntimeException("Failed to get app information! - " + this.getReturnMsg(3));
		}

		if (actionCode.equals("info")) {
			String port = this.env.getProperty(appId + ".port");

			if (port == null || port.isEmpty()) {
				throw new RuntimeException("Failed to get app information! - " + this.getReturnMsg(5));
			}

			retMap.put("PORT", port);

		} else {
			String path = this.env.getProperty(appId + "." + actionCode + ".path");

			if (path == null || path.isEmpty()) {
				throw new RuntimeException("Failed to get app information! - " + this.getReturnMsg(4));
			}

			retMap.put("PATH", path);
		}

		return retMap;
	}

	/**
	 * Properties 파일에서 key에 해당하는 데이터를 읽어 Array로 리턴
	 * 
	 * @param props
	 * @param key
	 * @return String[]
	 */
	private String[] getPropertiesByKey(Environment props, String key) {
		String readValue = props.getProperty(key);

		if (readValue == null) {
			return null;
		}

		return readValue.split(",");
	}

	/**
	 * 에러 코드별 리턴 메시지를 리턴한다.
	 * 
	 * @param code
	 * @return
	 */
	private String getReturnMsg(int code) {
		String msg = "";

		if (code == 1) {
			msg = "Can Not Read The Properties File!";

		} else if (code == 2) {
			msg = "Can Not Find Application ID Properties!";

		} else if (code == 3) {
			msg = "Wrong Application ID!";

		} else if (code == 4) {
			msg = "Can Not Find Application Batch Path!";

		} else if (code == 5) {
			msg = "Can Not Find Application Port No!";
			
		} else if (code == 6) {
			msg = "Can Not Find Property";
		}

		return msg;
	}
	
	/**
	 * Log File Comparator
	 * 
	 * @author shortstop
	 */
	class LogFileComparator implements Comparator<Map<String, Object>> {
		@Override
		public int compare(Map<String, Object> data1, Map<String, Object> data2) {
			String name1 = data1.get("name").toString();
			String name2 = data2.get("name").toString();
			return name1.compareTo(name2);
		}
	}
}