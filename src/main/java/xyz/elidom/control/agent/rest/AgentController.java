package xyz.elidom.control.agent.rest;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.input.ReversedLinesFileReader;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import net.lingala.zip4j.core.ZipFile;
import xyz.elidom.control.agent.util.StreamPrinter;

@CrossOrigin
@RestController
public class AgentController {

	protected Logger logger = LoggerFactory.getLogger(AgentController.class);

	private static final String FILE_SEPARATOR = "/";
	private static final String EMPTY_STR = "";
	private static final String LOG_FILENAME = "application.";
	private static final String LOG_FILE_EXT = ".log";
	
	private static final int KEEP_DEPLOY_APP_CNT = 3;

	@Autowired
	private Environment env;
	/**
	 * Index Information
	 */
	private Map<String, Object> indexInfo = new HashMap<String, Object>();

	/**
	 * Elidom Control Agent Index URL 
	 */
	@RequestMapping(value = "/api-info", method = RequestMethod.GET)
	public Map<String, Object> index() {
		
		if(indexInfo.isEmpty()) {
			indexInfo.put("Application", "Control Agent");
			indexInfo.put("Version", "4.1");
			
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
			urlInfo.put("View Module Info's of Application By AppId", "/apps/{app_id}/module_infos:GET");
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
	 * version check
	 */
	@RequestMapping(value = "/control-version", method = RequestMethod.GET)
	public String version() {
		return "4.0";
	}
	
	/**
	 * client version check
	 */
	@RequestMapping(value = "/client-version", method = RequestMethod.GET)
	public String clientVersion() {
		String clientVersionPath = env.getProperty("client.home.path", "");
		
		if(StringUtils.isEmpty(clientVersionPath.trim())) {
			return "";
		} 
		
		clientVersionPath = clientVersionPath + File.separator + "cj-sms-sub-mgt" + File.separator + "version.txt";
		
		File versionTxtFile = new File(clientVersionPath);
		if(versionTxtFile.exists() == false) return "";
		
		try {
			return FileUtils.readFileToString(versionTxtFile, Charset.defaultCharset());
		}catch(Exception e) {
			return "";
		}
	}
	
	
	

	/**
	 * Control Agent가 관리하는 모든 애플리케이션 요약 정보를 리턴 
	 * 
	 * @return
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@RequestMapping(value = "/apps/infos", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public List<Object> getAppInfos(HttpServletRequest req) {
		String[] ids = this.getPropertiesByKey(this.env, "apps.id");

		if (ids == null || ids.length == 0) {
			throw new RuntimeException("Application Id is empty!");
		}

		List<Object> apps = new ArrayList<Object>();
		RestTemplate rest = new RestTemplate();
		String appsUrl = req.getRequestURL().toString().replace("apps/infos", "admin/api/applications");
		List<?> appResults = this.invokeListByRest(appsUrl);
		
		if(appResults == null) {
			return apps;
		}
		
		for(Object result : appResults) {
			Map<String, Object> appInfo = (Map<String, Object>)result;
			Map statusInfoMap = (Map)appInfo.get("statusInfo");
			String appStatus = (String)statusInfoMap.get("status");
			appInfo.put("status", appStatus);
			Long time = (Long)(statusInfoMap.get("timestamp"));
			Date uptime = new Date(time);
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			String uptimeStr = sdf.format(uptime);	
			appInfo.put("updatedAt", uptimeStr);			
			
			if(appStatus.equalsIgnoreCase("UP")) {
				String adminAppId = (String)appInfo.get("id");
				String url = appsUrl + FILE_SEPARATOR + adminAppId + "/info";
				Map<?, ?> appAddInfo = null;
				
				try {
					appAddInfo = rest.getForObject(url, Map.class, new HashMap<String, Object>());
				} catch (Exception e) {
					appInfo.put("status", "DOWN");
					apps.add(appInfo);
					continue;
				}
				
				appInfo.put("stage", appAddInfo.get("stage"));
				Map<?, ?> appInfoMap = (Map)appAddInfo.get("appInfo");
				
				if(appInfoMap != null && !appInfoMap.isEmpty()) {
					Iterator keyIter = appInfoMap.keySet().iterator();
					while(keyIter.hasNext()) {
						String key = (String)keyIter.next();
						Object val = appInfoMap.get(key);
						if(val instanceof String) {
							appInfo.put(key, appInfoMap.get(key));
						}
					}
				}
				
				String monitoringUrl = (String)appInfo.get("serviceUrl");
				String svcPort = (String)appInfo.get("port");
				String mntPort = (String)appInfo.get("monitorPort");
				monitoringUrl = monitoringUrl.replace(svcPort, mntPort);
				appInfo.put("monitorUrl", monitoringUrl);				
			}
			
			apps.add(appInfo);
		}

		return apps;
	}
	
	/**
	 * List를 리턴하는 REST API를 호출 
	 * 
	 * @param url
	 * @return
	 */
	public List<?> invokeListByRest(String url) {
		RestTemplate rest = new RestTemplate();
		List<?> appResults = null;
		
		try {
			appResults = rest.getForObject(url, List.class, new HashMap<String, Object>());
		} catch(Exception e) {
			return null;
		}
		
		return appResults;
	}
	
	/**
	 * 관리하고 있는 모든 Application의 appId - 상태 값을 리턴  
	 * 
	 * @return
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@RequestMapping(value = "/apps/app_statuses", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public Map<String, Object> managedAppsStatus() {
		String appStatusUrl = "http://localhost:" + this.env.getProperty("server.port") + "/admin/api/applications";
    	List<?> statusList = this.invokeListByRest(appStatusUrl);
    	Map<String, Object> appStatuses = new HashMap<String, Object>();
    	
    	for(Object status : statusList) {
    		Map<String, Object> statusMap = (Map<String, Object>)status;
    		String appName = (String)statusMap.get("name");
			Map statusInfoMap = (Map)statusMap.get("statusInfo");
			String appStatus = (String)statusInfoMap.get("status");
			appStatuses.put(appName, appStatus);
    	}
    	
    	return appStatuses;
	}

	/**
	 * Control Agent가 관리하는 모든 애플리케이션 중 appId에 해당하는 애플리케이션 요약 정보를 리턴 
	 * 
	 * @param appId
	 * @return
	 */
	@SuppressWarnings("rawtypes")
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
			appInfo.put("version", result.get("version"));
			appInfo.put("stage", result.get("stage"));
			Map<?, ?> appInfoMap = (Map)result.get("app_info");
			
			if(appInfoMap != null && !appInfoMap.isEmpty()) {
				Iterator keyIter = appInfoMap.keySet().iterator();
				while(keyIter.hasNext()) {
					String key = (String)keyIter.next();
					appInfo.put(key, appInfoMap.get(key));
				}
			}
			
		} catch(Exception e) {
			this.logger.error(e.getMessage(), e);
			appInfo.put("status", "DOWN");
			return appInfo;
		}
		
		url = this.getAgentUrl(appPort, "health");
		try {
			Map<?, ?> status = rest.getForObject(url, Map.class, new HashMap<String, Object>());
			appInfo.put("status", status.get("status"));
		} catch(Exception e) {
			this.logger.error(e.getMessage(), e);
			appInfo.put("status", "DOWN");
		}
					
		return appInfo;
	}
	
	/**
	 * Control Agent가 관리하는 모든 애플리케이션 중 appId에 해당하는 애플리케이션이 보유하는 모듈리스트의 요약 정보를 리턴 
	 * 
	 * @param appId
	 * @return
	 */
	@RequestMapping(value = "/apps/{app_id}/module_infos", method = RequestMethod.GET)
	private Object getAppModuleInfo(@PathVariable("app_id") String appId) {
		Map<String, String> props = this.checkProperties(appId, "info");
		
		RestTemplate rest = new RestTemplate();
		String appPort = props.get("PORT");
		String url = this.getAgentUrl(appPort, "rest/modules/infos");
		
		try {
			return rest.getForObject(url, Map.class, new HashMap<String, Object>());
		} catch(Exception e) {
			this.logger.error(e.getMessage(), e);
		}
		
		return null;
	}
	
	/**
	 * Agent URL를 리턴 
	 * 
	 * @param appPort
	 * @param api
	 * @return
	 */
	private String getAgentUrl(String appPort, String api) {
		return "http://localhost:" + appPort + FILE_SEPARATOR + api;
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

		return "OK";
	}

	/**
	 * Application Restart
	 * 
	 * @param appId
	 * @return execute message
	 */
	@RequestMapping(value = "/apps/{app_id}/restart", method = RequestMethod.POST)
	public String restartBoot(@PathVariable("app_id") String appId) {
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
		/*RestTemplate rest = new RestTemplate();
		String port = this.env.getProperty(appId + ".port");
		String url = "http://localhost:" + port + "/shutdown";
		ResponseEntity<String> response = rest.postForEntity(url, EMPTY_STR, String.class);
		return response.getBody();*/
		
		HashMap<String, String> pMap = this.checkProperties(appId, "stop");

		try {
			this.commandStart(pMap.get("PATH"));
		} catch (Exception e) {
			return "Error : \n\n" + e.getMessage();
		}

		return "OK";
	}

	/**
	 * Application Deploy
	 * 
	 * @param appId
	 * @return execute message
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/apps/{app_id}/deploy", method = RequestMethod.POST)
	public String deploy(@PathVariable("app_id") String appId) throws Exception {
		try {
			// 1. 프로퍼티 체크
			String appsAdminServerUrlKey = "apps.admin.server.url";
			String appsTypeKey = appId + ".type";
			String appHomePathKey = appId + ".home.path";
			String appFileInfoUrlKey = appId + ".app.file.info.url";
			
			Assert.assertNotNull("[" + appsAdminServerUrlKey + "] must not be empty", this.env.getProperty(appsAdminServerUrlKey));
			Assert.assertNotNull("[" + appHomePathKey + "] must not be empty", this.env.getProperty(appHomePathKey));
			Assert.assertNotNull("[" + appFileInfoUrlKey + "] must not be empty", this.env.getProperty(appFileInfoUrlKey));
			
			// 2. http invoke by jar information url
			String appType = this.env.getProperty(appsTypeKey);
			appType = (appType == null || appType.equalsIgnoreCase("")) ? "server" : appType;
			
			String appsAdminServerUrl = this.env.getProperty(appsAdminServerUrlKey);
			String homePath = this.env.getProperty(appHomePathKey);
			String appFileInfoUrl = this.env.getProperty(appFileInfoUrlKey);
			String deployFileName = (appType.equalsIgnoreCase("client")) ? "deploy-version.zip" : "deploy-version.jar";
			String jarFileInfoUrl = null;
			
			RestTemplate rest = new RestTemplate();
			if(!appFileInfoUrl.startsWith("http")) {
				jarFileInfoUrl = appsAdminServerUrl;
				jarFileInfoUrl += (appsAdminServerUrl.endsWith(FILE_SEPARATOR) || appFileInfoUrl.startsWith(FILE_SEPARATOR)) ? EMPTY_STR : FILE_SEPARATOR;
				jarFileInfoUrl += appFileInfoUrl;
			} else {
				jarFileInfoUrl = appFileInfoUrl;
			}
			
			// 3. file 정보로 부터 복사할 파일의 URL을 추출 ...
			this.logger.info("Get file information by url [" + jarFileInfoUrl + "]");
			Map<String, Object> fileInfo = rest.getForObject(jarFileInfoUrl, Map.class, new HashMap<String, Object>());
			String fileId = (String)fileInfo.get("id");
			String downloadUrl = appsAdminServerUrl + (appsAdminServerUrl.endsWith(FILE_SEPARATOR) ? EMPTY_STR : FILE_SEPARATOR) + "rest/download/public/" + fileId;
			String downloadPath = homePath + File.separator + deployFileName;
			this.logger.info("File downloading by url [" + downloadPath + "]");
			this.downloadByUrl(downloadUrl, downloadPath);
			this.logger.info("File downloaded!");
			
			// 4.1 서버 타입인 경우
			if(appType.equalsIgnoreCase("server")) {
				// 4.1.1. 애플리케이션 stop 
				this.stopBoot(appId);
				this.logger.info("Application stopping...");
				
				// 4.1.2. 애플리케이션 죽을 때 까지 약간 기다렸다가 TODO 답이 없을 때까지 health api를 계속 호출하여 ...
				try {
					Thread.sleep(10000);
				} catch(Exception e) {
				}
				
				// 4.1.3. 기존 jar 파일 rename
				String appFileName = appFileInfoUrl.split("=")[1];
				String appFilePath = homePath + (homePath.endsWith(FILE_SEPARATOR) ? EMPTY_STR : FILE_SEPARATOR) + appFileName;
				String currentTime = this.formatDate(new Date(), "yyyyMMddHHmmss");
				String appBakFilePath = appFilePath.replace(".jar", "-" + currentTime + "-sys-deploy.jar");
				this.logger.info("Jar file backup from [" + appFilePath + "] to [" + appBakFilePath + "]");
				FileUtils.moveFile(new File(appFilePath), new File(appBakFilePath));

				// 4.1.4 기존 jar 파일에 대한 보관 
				this.deployFileMgmt(homePath, appType);
				
				// 4.1.5. deploy-version.jar 파일을 기존 jar 파일로 rename
				String latestFilePath = homePath + (homePath.endsWith(FILE_SEPARATOR) ? EMPTY_STR : FILE_SEPARATOR) + deployFileName;
				this.logger.info("Latest file rename from [" + latestFilePath + "] to [" + appFilePath + "]");
				FileUtils.moveFile(new File(latestFilePath), new File(appFilePath));
				
				// 4.1.6. 애플리케이션 다시 시작 ...
				this.logger.info("Restart application...");
				this.startBoot(appId);
			
			// 4.2 클라이언트 타입인 경우 
			} else {
				// 4.2.1 기존 client 폴더 rename.
				String appFileName = appFileInfoUrl.split("=")[1].replaceAll(".zip","");
				String appFilePath = homePath + (homePath.endsWith(FILE_SEPARATOR) ? EMPTY_STR : FILE_SEPARATOR) + appFileName;
				String currentTime = this.formatDate(new Date(), "yyyyMMddHHmmss");
				String appBakFilePath = appFilePath + "-" + currentTime + "-sys-deploy";
				this.logger.info("client directory backup from [" + appFilePath + "] to [" + appBakFilePath + "]");
				FileUtils.moveDirectory(new File(appFilePath), new File(appBakFilePath));

				// 4.2.2 client 에 대한 보관 
				this.deployFileMgmt(homePath, appType);
				
				// 4.2.3 deploy-version.zip 압축해제 
				String zipFilePath = homePath + (homePath.endsWith(FILE_SEPARATOR) ? EMPTY_STR : FILE_SEPARATOR) + deployFileName;
				File deployZip = new File (zipFilePath);
				
				try {
					ZipFile zipFile = new ZipFile(deployZip);
			         zipFile.extractAll(homePath);
			    } catch (Exception e) {
			    		throw e;
			    } finally {
			    		deployZip.delete();
			    }
			}
			
		} catch (Exception e) {
			this.logger.error("Failed to deploy application", e);
			throw new Exception("Failed to deploy application : " + e.getMessage());
		}

		return "OK";
	}

	/**
	 * Application Rollback
	 * @param appId
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/apps/{app_id}/rollback", method = RequestMethod.POST)
	public String rollback(@PathVariable("app_id") String appId) throws Exception {
		try {
			// 1. 프로퍼티 체크
			String appsTypeKey = appId + ".type";
			String appHomePathKey = appId + ".home.path";
			String appFileInfoUrlKey = appId + ".app.file.info.url";
			
			Assert.assertNotNull("[" + appHomePathKey + "] must not be empty", this.env.getProperty(appHomePathKey));
			Assert.assertNotNull("[" + appFileInfoUrlKey + "] must not be empty", this.env.getProperty(appFileInfoUrlKey));
			
			// 2. http invoke by jar information url
			String appType = this.env.getProperty(appsTypeKey);
			appType = (appType == null || appType.equalsIgnoreCase("")) ? "server" : appType;
			
			String homePath = this.env.getProperty(appHomePathKey);
			String appFileInfoUrl = this.env.getProperty(appFileInfoUrlKey);
			
			// 3. 가장 최근 배포한 파일 정보 
			List<String> pathList = this.getAppFileList(homePath, appType);
			if(pathList.size() == 0) {
				throw new Exception("Not exist Rollback target Apps ");
			}
			
			String latestPath = pathList.get(0);
			
			
			// 4.1 서버 타입인 경우
			if(appType.equalsIgnoreCase("server")) {
				// 4.1.1. 애플리케이션 stop 
				this.stopBoot(appId);
				this.logger.info("Application stopping...");
				
				// 4.1.2. 애플리케이션 죽을 때 까지 약간 기다렸다가 TODO 답이 없을 때까지 health api를 계속 호출하여 ...
				try {
					Thread.sleep(10000);
				} catch(Exception e) {
				}

				// 4.1.3 ㅇ어플리케이션 backup
				String appFileName = appFileInfoUrl.split("=")[1];
				String appFilePath = homePath + (homePath.endsWith(FILE_SEPARATOR) ? EMPTY_STR : FILE_SEPARATOR) + appFileName;
				String currentTime = this.formatDate(new Date(), "yyyyMMddHHmmss");
				String appBakFilePath = appFilePath.replace(".jar", "-" + currentTime + "-sys-deploy.jar");
				this.logger.info("Jar file backup from [" + appFilePath + "] to [" + appBakFilePath + "]");
				FileUtils.moveFile(new File(appFilePath), new File(appBakFilePath));

				// 4.1.4 가장 최근 배포한 어플리케이션 복구
				this.logger.info("Jar file recover from [" + latestPath + "] to [" + appFilePath + "]");
				FileUtils.moveFile(new File(latestPath), new File(appFilePath));
				
				// 4.1.5. 애플리케이션 다시 시작 ...
				this.logger.info("Restart application...");
				this.startBoot(appId);
			
			// 3.2 클라이언트 타입인 경우 
			} else {
				// 4.2.1 기존 client 폴더 rename.
				String appFileName = appFileInfoUrl.split("=")[1].replaceAll(".zip","");
				String appFilePath = homePath + (homePath.endsWith(FILE_SEPARATOR) ? EMPTY_STR : FILE_SEPARATOR) + appFileName;
				String currentTime = this.formatDate(new Date(), "yyyyMMddHHmmss");
				String appBakFilePath = appFilePath + "-" + currentTime + "-sys-deploy";
				this.logger.info("client directory backup from [" + appFilePath + "] to [" + appBakFilePath + "]");
				FileUtils.moveDirectory(new File(appFilePath), new File(appBakFilePath));

				// 4.2.2 가장 최근 배포한 client 복구
				this.logger.info("client directory recover from [" + latestPath + "] to [" + appFilePath + "]");
				FileUtils.moveDirectory(new File(latestPath), new File(appFilePath));
			}
		} catch (Exception e) {
			this.logger.error("Failed to Rollback application", e);
			throw new Exception("Failed to Rollback application : " + e.getMessage());
		}

		return "OK";
	}
	
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/apps/{app_id}/deploy_back", method = RequestMethod.POST)
	public String deploy_back(@PathVariable("app_id") String appId) throws Exception {
		try {
			// 1. 프로퍼티 체크
			String appsAdminServerUrlKey = "apps.admin.server.url";
			String appsTypeKey = appId + ".type";
			String appHomePathKey = appId + ".home.path";
			String appFileInfoUrlKey = appId + ".app.file.info.url";
			String deployScriptPathKey = appId + ".deploy.path";
			
			Assert.assertNotNull("[" + appsAdminServerUrlKey + "] must not be empty", this.env.getProperty(appsAdminServerUrlKey));
			Assert.assertNotNull("[" + appHomePathKey + "] must not be empty", this.env.getProperty(appHomePathKey));
			Assert.assertNotNull("[" + appFileInfoUrlKey + "] must not be empty", this.env.getProperty(appFileInfoUrlKey));
			
			// 2. http invoke by jar information url
			String appType = this.env.getProperty(appsTypeKey);
			appType = (appType == null || appType.equalsIgnoreCase("")) ? "server" : appType;
			
			if(!appType.equalsIgnoreCase("server")) {
				Assert.assertNotNull("[" + deployScriptPathKey + "] must not be empty", this.env.getProperty(deployScriptPathKey));
			}
			
			String appsAdminServerUrl = this.env.getProperty(appsAdminServerUrlKey);
			String homePath = this.env.getProperty(appHomePathKey);
			String appFileInfoUrl = this.env.getProperty(appFileInfoUrlKey);
			String latestFileName = (appType.equalsIgnoreCase("client")) ? "latest-version.zip" : "latest-version.jar";
			String jarFileInfoUrl = null;
			
			RestTemplate rest = new RestTemplate();
			if(!appFileInfoUrl.startsWith("http")) {
				jarFileInfoUrl = appsAdminServerUrl;
				jarFileInfoUrl += (appsAdminServerUrl.endsWith(FILE_SEPARATOR) || appFileInfoUrl.startsWith(FILE_SEPARATOR)) ? EMPTY_STR : FILE_SEPARATOR;
				jarFileInfoUrl += appFileInfoUrl;
			} else {
				jarFileInfoUrl = appFileInfoUrl;
			}
			
			// 3. file 정보로 부터 복사할 파일의 URL을 추출 ...
			this.logger.info("Get file information by url [" + jarFileInfoUrl + "]");
			Map<String, Object> fileInfo = rest.getForObject(jarFileInfoUrl, Map.class, new HashMap<String, Object>());
			String fileId = (String)fileInfo.get("id");
			String downloadUrl = appsAdminServerUrl + (appsAdminServerUrl.endsWith(FILE_SEPARATOR) ? EMPTY_STR : FILE_SEPARATOR) + "rest/download/public/" + fileId;
			String downloadPath = homePath + File.separator + latestFileName;
			this.logger.info("File downloading by url [" + downloadPath + "]");
			this.downloadByUrl(downloadUrl, downloadPath);
			this.logger.info("File downloaded!");
			
			// 4.1 서버 타입인 경우
			if(appType.equalsIgnoreCase("server")) {
				// 4.1.1. 애플리케이션 stop 
				this.stopBoot(appId);
				this.logger.info("Application stopping...");
				
				// 4.1.2. 애플리케이션 죽을 때 까지 약간 기다렸다가 TODO 답이 없을 때까지 health api를 계속 호출하여 ...
				try {
					Thread.sleep(6000);
				} catch(Exception e) {
				}
				
				// 4.1.3. 기존 jar 파일 rename
				String appFileName = appFileInfoUrl.split("=")[1];
				String appFilePath = homePath + (homePath.endsWith(FILE_SEPARATOR) ? EMPTY_STR : FILE_SEPARATOR) + appFileName;
				String currentTime = this.formatDate(new Date(), "yyyyMMddHHmmss");
				String appBakFilePath = appFilePath.replace(".jar", "-" + currentTime + ".jar");
				this.logger.info("Jar file backup from [" + appFilePath + "] to [" + appBakFilePath + "]");
				FileUtils.moveFile(new File(appFilePath), new File(appBakFilePath));
				
				// 4.1.4. latest-version.jar 파일을 기존 jar 파일로 rename
				String latestFilePath = homePath + (homePath.endsWith(FILE_SEPARATOR) ? EMPTY_STR : FILE_SEPARATOR) + latestFileName;
				this.logger.info("Latest file rename from [" + latestFilePath + "] to [" + appFilePath + "]");
				FileUtils.moveFile(new File(latestFilePath), new File(appFilePath));
				
				// 4.1.5. 애플리케이션 다시 시작 ...
				this.logger.info("Restart application...");
				this.startBoot(appId);
			
			// 4.2 클라이언트 타입인 경우 
			} else {
				// 4.2.1 배포 스크립트를 호출한다.
				String deployScriptPath = this.env.getProperty(deployScriptPathKey);
				this.logger.info("Call client deploy script [" + deployScriptPath + "]");
				this.commandStart(deployScriptPath);
			}
			
		} catch (Exception e) {
			this.logger.error("Failed to deploy application", e);
			throw new Exception("Failed to deploy application : " + e.getMessage());
		}

		return "OK";
	}

	/**
	 * Application File List
	 * 
	 * @param appId
	 * @return
	 */
	@RequestMapping(value = "/apps/{app_id}/file/view", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public List<Map<String, Object>> appsFileList(@PathVariable("app_id") String appId) {
		String path;
		if(appId.equalsIgnoreCase("client")) {
			path = this.env.getProperty(appId + ".home.path");
		} else {
			HashMap<String, String> pMap = this.checkProperties(appId, "home");
			path = pMap.get("PATH");
		}
		
		return this.getFileList(path);
	}
	
	
	private List<Map<String,Object>> getFileList(String path){
		File dirPath = new File(path);
		List<Map<String, Object>> fileList = new ArrayList<Map<String, Object>>();
		
		File[] pathFiles = dirPath.listFiles();
		for(File pFile : pathFiles) {
			String fileName = pFile.getName();
			
			Map<String, Object> fData = new HashMap<String, Object>(3);
			if(pFile.isDirectory()) {
				fData.put("type","d");
				fData.put("name", fileName);
				fData.put("child", this.getFileList(pFile.getAbsolutePath()));
			} else {
				fData.put("type","f");
				fData.put("name", fileName);
				fData.put("size", pFile.length());
			}
			fileList.add(fData);
		}
		
		fileList.sort(new FileListComparator());
		return fileList;
	}
	
	
	/**
	 * URL로 다운로드 
	 * 
	 * @param downloadUrl
	 * @param downloadPath
	 * @return
	 * @throws Exception
	 */
	private int downloadByUrl(String downloadUrl, String downloadPath) throws Exception {
		URLConnection urlConn = null;
		OutputStream outStream = null;
		InputStream inStream = null;
		int byteWritten = 0;
		
		try {
			URL url = new URL(downloadUrl);
			urlConn = url.openConnection();
			inStream = urlConn.getInputStream();
			outStream = new BufferedOutputStream(new FileOutputStream(downloadPath));
			byte[] buf = new byte[4096];
			int byteRead = 0;
			
			while ((byteRead = inStream.read(buf)) != -1) {
				outStream.write(buf, 0, byteRead);
				byteWritten += byteRead;
			}
		} catch (IOException ioe) {
			if(urlConn == null) {
				this.logger.error("Failed to connect to url [" + downloadPath + "]");
			}
			
			throw ioe;
			
		} catch (Exception e) {
			throw e;
		
		} finally {
			try {
				if(inStream != null) {
					inStream.close();
				}
				
				if(outStream != null) {
					outStream.close();
				}
			} catch (IOException e) {
				this.logger.error(e.getMessage(), e);
			}
		}

		return byteWritten;
	}

	/**
	 * Log File Path를 리턴
	 * 
	 * @param appId
	 * @param first
	 * @return
	 */
	private String getLogFilePath(String appId, boolean first) {
		HashMap<String, String> pMap = this.checkProperties(appId, "log");
		String path = pMap.get("PATH");
		
		// 첫번째 인 경우 : 오늘 날짜 두번째 어제 날짜
		Date today = first ? new Date() : this.getDate(new Date(), -1); 
		StringBuffer logPath = new StringBuffer();
		logPath.append(path).append(path.endsWith(FILE_SEPARATOR) ? EMPTY_STR : FILE_SEPARATOR).append(LOG_FILENAME).append(this.formatDate(today, null)).append(LOG_FILE_EXT);

		File file = new File(logPath.toString());
		if (!file.exists()) {
			if(first) {
				return this.getLogFilePath(appId, false);
			} else {
				throw new RuntimeException("Log File (" + logPath.toString() + ") Not Found!");
			}
		}

		return logPath.toString();
	}
	
	/**
	 * date에 addDate를 더한 날짜를 리턴 
	 * 
	 * @param date
	 * @param addDate
	 * @return
	 */
	private Date getDate(Date date, int addDate) {
		Calendar c = Calendar.getInstance(); 
		c.setTime(date); 
		c.add(Calendar.DATE, addDate);
		Date newDate = c.getTime();
		return newDate;
	}
	
	/**
	 * date 객체를 format으로 포매팅하여 리턴 
	 * 
	 * @param date
	 * @param format
	 * @return
	 */
	private String formatDate(Date date, String format) {
		SimpleDateFormat sdf = new SimpleDateFormat(this.getDateFormat(format));
		return sdf.format(date);
	}
	
	/**
	 * String format의 날짜 형식을 Date 형식으로 변환 
	 * 
	 * @param dateStr
	 * @param format
	 * @return
	 * @throws ParseException
	 */
	private Date parseDate(String dateStr, String format) throws ParseException {
		SimpleDateFormat formatter = new SimpleDateFormat(this.getDateFormat(format));
		return formatter.parse(dateStr);
	}
	
	/**
	 * 데이트 포맷이 없다면 기본 데이트 포맷을 리턴 
	 * 
	 * @param format
	 * @return
	 */
	private String getDateFormat(String format) {
		return (format == null) ? "yyyy-MM-dd" : format;
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
	 * 로그 파일을 유지할 날짜 수 - 이 날짜가 지난 파일은 자동 삭제된다.
	 * 
	 * @return
	 */
	private int getLogKeepDate() {
		String keepDate = this.getPropertiesByKey(this.env, "apps.log.file.keep.date")[0];
		return Integer.parseInt(keepDate) * -1;
	}
	
	/**
	 * 오래된 로그 파일 삭제  
	 */
	public void deleteOldLogFiles() {
		this.logger.info("==================================");
		this.logger.info("Starting delete Old log files...");
		this.logger.info("==================================");
		
		int keepDate = this.getLogKeepDate();
		Date stdDate = this.getDate(new Date(), keepDate);
		String[] ids = this.getPropertiesByKey(this.env, "apps.id");
		
		for(String appId : ids) {
			List<Map<String, Object>> fileList = this.logFileList(appId);
			for(Map<String, Object> fileInfo : fileList) {
				String filePath = fileInfo.get("id").toString();
				String dateStr = filePath.substring(filePath.indexOf(".") + 1, filePath.lastIndexOf("."));
				Date logDate = null;
				try {
					logDate = this.parseDate(dateStr, null);
				} catch (ParseException e) {
					this.logger.error(e.getMessage(), e);
					continue;
				}

				if(stdDate.getTime() > logDate.getTime()) {
					File logFile = new File(filePath);
					if(logFile.delete()) {
						this.logger.info("================================================");
						this.logger.info("File [" + filePath + "] Deleted");
						this.logger.info("================================================");
					} else {
						this.logger.info("================================================");
						this.logger.info("Failed to Delete Log File [" + filePath + "]");
						this.logger.info("================================================");			
					}					
				}
			}
		}
		
		this.logger.info("==================================");
		this.logger.info("Finished delete Old log files!");
		this.logger.info("==================================");
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
		String msg = EMPTY_STR;

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
	 * 파일 보관 룰 관리 
	 * @param homePath
	 * @param appType
	 * @throws Exception
	 */
	private void deployFileMgmt(String homePath, String appType) throws Exception {
		 
		List<String> pathList = this.getAppFileList(homePath, appType);
		
		int skipCnt = 0;
		for(String delPath : pathList) {
			skipCnt++;
			this.logger.info(delPath);
			if(skipCnt > KEEP_DEPLOY_APP_CNT) FileUtils.forceDelete(new File(delPath));
		}
	}
	
	private List<String> getAppFileList(String homePath, String appType){
		List<String> pathList = new ArrayList<String>() ;
		String endWith;
		
		if(appType.equalsIgnoreCase("server")) {
			endWith = "-sys-deploy.jar";
		} else {
			endWith = "-sys-deploy";
		}
		

		File homeDir = new File(homePath);
		File[] homeFileList = homeDir.listFiles();
		for(File appFile : homeFileList) {
			String fileName = appFile.getName();
			if(fileName.endsWith(endWith)) {
				String jarPath = appFile.getAbsolutePath();
				pathList.add(jarPath);
			}
		}
		
		Collections.sort(pathList, Collections.reverseOrder());
		
		return pathList;
	}
	
	
	
	class FileListComparator implements Comparator<Map<String, Object>> {
		@Override
		public int compare(Map<String, Object> data1, Map<String, Object> data2) {
			String name1 = data1.get("type").toString() + data1.get("name").toString();
			String name2 = data2.get("type").toString() + data2.get("name").toString();
			return name1.compareTo(name2);
		}
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