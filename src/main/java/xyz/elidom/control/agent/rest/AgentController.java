package xyz.elidom.control.agent.rest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

//import org.json.JSONArray;
//import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
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
	Environment env;
	 
	/**
	 * Application Start
	 * 
	 * @param appId
	 * @return execute message
	 */
	@RequestMapping(value = "/apps/{app_id}/start", method = RequestMethod.POST)
	public String startBoot(@PathVariable("app_id") String appId) {
		HashMap<String,String> pMap = this.checkProperties(appId, "start");
		if(pMap.get("RESULT").equals("FAIL")) return pMap.get("MSG");
		
		try {
			this.commandStart(pMap.get("PATH"));
		} catch (Exception e) {
			return "Error : \n\n" + e.getMessage();
		}
		
		return "Enterd Startup Command SUCCESS";
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
			Thread.sleep(10000);
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
	@RequestMapping(value = "/apps/{app_id}/udpate", method = RequestMethod.POST)
	public String deploy(@PathVariable("app_id") String appId) {
		this.stopBoot(appId);
		
		try {
			Thread.sleep(10000);
		} catch (Exception e) {			
		}		
		
		HashMap<String,String> pMap = this.checkProperties(appId, "update");
		if(pMap.get("RESULT").equals("FAIL")) return pMap.get("MSG");
		
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
	 * @param path
	 * @return
	 */
	private String getLogFilePath(String path) {
		StringBuffer logPath = new StringBuffer();
		Date today = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		logPath.append(path).append(path.endsWith(FILE_SEPARATOR) ? "" : FILE_SEPARATOR).append(LOG_FILENAME).append(sdf.format(today)).append(LOG_FILE_EXT);
		
		File file = new File(logPath.toString());
		if(!file.exists()) {
			logPath = new StringBuffer();
			logPath.append(path).append(path.endsWith(FILE_SEPARATOR) ? "" : FILE_SEPARATOR).append(LOG_FILENAME_2).append(sdf.format(today)).append(LOG_FILE_EXT);
			file = new File(logPath.toString());
			
			if(!file.exists()) {
				throw new RuntimeException("Log File (" + logPath.toString() + ") Not Found!");
			}
		}
		
		return logPath.toString();
	}
	
	/**
	 * 로그 파일을 읽어서 내용을 리턴  
	 * 
	 * @param appId
	 * @return today log file
	 */
	@RequestMapping(value = "/apps/{app_id}/log", method = RequestMethod.GET, produces=MediaType.APPLICATION_JSON_VALUE)
	public Map<String, String> readLog(@PathVariable("app_id") String appId) {
		
		HashMap<String,String> pMap = this.checkProperties(appId, "log");
		if(pMap.get("RESULT").equals("FAIL")) return pMap;
		
		String logPath = this.getLogFilePath(pMap.get("PATH")); 
		StringBuffer content = new StringBuffer();
		FileReader fReader = null;
		BufferedReader bReader = null;
		Map<String, String> result = new HashMap<String, String>();
		
		// TODO 마지막 1000 라인만 읽기 - 읽을 라인은 설정으로 ...
		try {
			fReader = new FileReader(logPath);
			bReader = new BufferedReader(fReader);
			String temp = "";
			while((temp = bReader.readLine()) != null) {
			    content.append(temp).append("\n");
			}
		} catch(FileNotFoundException e) {
			result.put("success", "false");
			result.put("msg", "Log File Not Found!");
			return result;
			
		} catch (Exception e) {
			result.put("success", "false");
			result.put("msg", e.getMessage());
			return result;
			
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
		
		result.put("id", "1");
		result.put("log", content.toString());
		return result;
	}
	
	/**
	 * ping & Pong 
	 */
	@RequestMapping(value = "/ping", method = RequestMethod.GET)
	public String ping() {
		return "pong";
	}
	
	/*@RequestMapping(value = "/apps/{app_id}/info", method = RequestMethod.POST)
	public String getOneDomainInfo(@PathVariable("app_id") String appId) {
		HashMap<String, String> retMap = this.getDomainInfo(appId);
		if(retMap.get("RESULT").equals("FAIL")) return retMap.get("MSG");
		return retMap.get("RES_JSON");
	}
	
	@RequestMapping(value = "/apps/infos", method = RequestMethod.GET)
	public String getDomainInfos()  {
		String[] ids = this.getPropertiesByKey(this.env, "apps.id");
		
		if(ids == null || ids.length == 0) {
			return "Domain Id Not Found !";
		}
		
		
		JSONArray array = new JSONArray();
		
		for(int i = 0 ; i < ids.length ; i++) {
			HashMap<String, String> retMap = this.getDomainInfo(ids[i]);
			if(retMap.get("RESULT").equals("FAIL")) return retMap.get("MSG");
			array.put(new JSONObject(retMap.get("RES_JSON")));
		}
		
		JSONObject resObj = new JSONObject();
		resObj.put("items", array);
		return resObj.toString();
	}
	
	private HashMap<String, String> getDomainInfo(String app_id) {
		HashMap<String,String> pMap = this.checkProperties(app_id, "info");
		if(pMap.get("RESULT").equals("FAIL")) return pMap;
		
		String url = "http://localhost:" + pMap.get("PORT") + "/info";
		BufferedReader brIn = null;
		InputStreamReader isr = null;
		StringBuffer resStr = null;
		
		try {
			URL obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();
			int responseCode = con.getResponseCode();
			
			if(responseCode == 401 ){
				pMap.put("RESULT", "FAIL");
				pMap.put("MSG", "Unauthorized Request !");
				return pMap;
			}
			
			isr = new InputStreamReader(con.getInputStream());
			brIn = new BufferedReader(isr);
			
			String inputLine;
			resStr = new StringBuffer();

			while ((inputLine = brIn.readLine()) != null) {
				resStr.append(inputLine);
			}
		} catch(Exception e) {
			pMap.put("RESULT", "FAIL");
			pMap.put("MSG", "Error : \n\n" + e.getMessage());
			return pMap;
			
		} finally {
			if(brIn != null) {
				try {
					brIn.close();
				} catch (Exception e) {
				}
			}
			if(isr != null) {
				try {
					isr.close();
				} catch (Exception e) {
				}
			}
		}
		
		JSONObject resJson = new JSONObject(resStr.toString());
		resJson.put("id", app_id);
		pMap.put("RES_JSON", resJson.toString());
		return pMap;
	}*/

	/**
	 * Command 실행 
	 * 
	 * @param Path
	 * @throws Exception
	 */
	private void commandStart(String Path) throws Exception{
		ProcessBuilder pBuilder = new ProcessBuilder();
		pBuilder.command(Path);
		Process process = pBuilder.start();
		StreamPrinter.printStream(process);
		
		while(process.isAlive()) {
			Thread.sleep(100);
		}
	}
	
	/**
	 * appId별 actionCode에 대응하는 프로퍼티를 찾아 리턴  
	 * 
	 * @param appId 
	 * @param actionCode
	 * @return HashMap
	 *         RESULT : 성공 혹은 실패 
	 *         MSG : 실패시 에러 메시지 
	 *         PATH : 액션 코드별 값  
	 */
	private HashMap<String, String> checkProperties(String appId, String actionCode) {
		HashMap<String, String> retMap = new HashMap<String, String>();
    	retMap.put("RESULT", "SUCCESS");
        String[] ids = this.getPropertiesByKey(this.env, "apps.id");
        
        if(ids == null) {
        	retMap.put("RESULT", "FAIL");
        	retMap.put("MSG", this.getReturnMsg(2));
        	return retMap;
        }
        
		boolean isExists = false;
		for(String id : ids) {
			if(id.equals(appId)) {
				isExists = true;
				break;
			}
		}
		
		if(isExists == false) {
        	retMap.put("RESULT", "FAIL");
        	retMap.put("MSG", this.getReturnMsg(3));
        	return retMap;
		}
		
		
		if(actionCode.equals("info")) {
			String port = this.env.getProperty(appId + ".port");
			
			if(port == null || port.isEmpty()) {
	        	retMap.put("RESULT", "FAIL");
	        	retMap.put("MSG", this.getReturnMsg(5));
	        	return retMap;
			}
			
			retMap.put("PORT", port);
			
		} else {
			String path = this.env.getProperty(appId + "." + actionCode + ".path");
			
			if(path == null || path.isEmpty()) {
	        	retMap.put("RESULT", "FAIL");
	        	retMap.put("MSG", this.getReturnMsg(4));
	        	return retMap;
			}
			
			retMap.put("PATH", path);
		}
		
		return retMap;
	}

	/**
	 * Properties 파일에서 key에 해당하는 데이터를 읽어 Array로 리턴 
	 * 
	 * @param _properties
	 * @param key
	 * @return String[]
	 */
	private String[] getPropertiesByKey(Environment _properties, String key) {
		String readValue = _properties.getProperty(key);
		
		if(readValue == null) {
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
		
		if(code == 1) {
			msg = "Can Not Read The Properties File!";
			
		} else if(code == 2) {
			msg = "Can Not Find Application ID Properties!";
			
		} else if(code == 3) {
			msg = "Wrong Application ID!";
			
		} else if(code == 4) {
			msg = "Can Not Find Application Batch Path!";
			
		} else if(code == 5) {
			msg = "Can Not Find Application Port No!";
		}
		
		return msg;
	}
}