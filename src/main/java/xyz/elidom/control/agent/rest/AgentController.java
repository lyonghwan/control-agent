package xyz.elidom.control.agent.rest;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
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
	Environment env;

	/**
	 * Application Start
	 * 
	 * @param appId
	 * @return execute message
	 */
	@RequestMapping(value = "/apps/{app_id}/start", method = RequestMethod.POST)
	public String startBoot(@PathVariable("app_id") String appId) {
		HashMap<String, String> pMap = this.checkProperties(appId, "start");
		if (pMap.get("RESULT").equals("FAIL"))
			return pMap.get("MSG");

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

		HashMap<String, String> pMap = this.checkProperties(appId, "update");
		if (pMap.get("RESULT").equals("FAIL"))
			return pMap.get("MSG");

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
		
		if (pMap.get("RESULT").equals("FAIL")) {
			throw new RuntimeException(pMap.get("MSG"));
		}
		
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
	 * 로그 파일을 읽어서 내용을 리턴
	 * 
	 * @param appId
	 * @param lines
	 * @return 오늘의 로그의 내용을 리턴 
	 */
	@RequestMapping(value = "/apps/{app_id}/log", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public Map<String, String> readLog(
			@PathVariable("app_id") String appId,
			@RequestParam(name = "lines", required = false) Integer lines) {
		
		String logPath = this.getLogFilePath(appId, true);
		String content = (lines == null || lines == 0) ? 
						 this.readAllLines(logPath) : 
						 this.readLastLines(new File(logPath), lines);

		Map<String, String> result = new HashMap<String, String>();
		result.put("id", "1");
		result.put("log", content);
		return result;
	}
	
	/**
	 * 로그 파일을 다운로드 
	 * 
	 * @param appId
	 * @return 오늘의 로그의 내용을 리턴 
	 */
	@RequestMapping(value = "/apps/{app_id}/download_log", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public boolean downloadLog(HttpServletRequest req, HttpServletResponse res, @PathVariable("app_id") String appId) {
		String logPath = this.getLogFilePath(appId, true);
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
	 * ping & pong
	 */
	@RequestMapping(value = "/ping", method = RequestMethod.GET)
	public String ping() {
		return "pong";
	}

	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/apps/{app_id}/info", method = RequestMethod.GET)
	public Map<String, Object> getOneDomainInfo(@PathVariable("app_id") String appId) {
		Map<String, Object> appInfo = (Map<String, Object>) this.getAppInfo(appId);

		if (appInfo.get("RESULT").equals("FAIL")) {
			return appInfo;
		}

		return appInfo;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@RequestMapping(value = "/apps/infos", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public Map<String, Object> getAppInfos() {
		String[] ids = this.getPropertiesByKey(this.env, "apps.id");
		Map<String, Object> result = new HashMap<String, Object>();

		if (ids == null || ids.length == 0) {
			result.put("RESULT", "FAIL");
			result.put("MSG", "Application Id Not Found !");
			return result;
		}

		List apps = new ArrayList();

		for (int i = 0; i < ids.length; i++) {
			Map<String, Object> itemObj = (Map<String, Object>) this.getAppInfo(ids[i]);
			if (itemObj.get("RESULT").equals("FAIL")) {
				return itemObj;
			}

			apps.add(itemObj.get("RES_JSON"));
		}

		result.put("items", apps);
		return result;
	}

	private Object getAppInfo(String appId) {
		Map<String, String> pMap = this.checkProperties(appId, "info");
		if (pMap.get("RESULT").equals("FAIL")) {
			return pMap;
		}

		String url = "http://localhost:" + pMap.get("PORT") + "/info";
		BufferedReader brIn = null;
		InputStreamReader isr = null;
		StringBuffer resStr = null;

		try {
			URL obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();
			int responseCode = con.getResponseCode();

			if (responseCode == 401) {
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
		} catch (Exception e) {
			pMap.put("RESULT", "FAIL");
			pMap.put("MSG", "Error : \n\n" + e.getMessage());
			return pMap;

		} finally {
			if (brIn != null) {
				try {
					brIn.close();
				} catch (Exception e) {
				}
			}
			if (isr != null) {
				try {
					isr.close();
				} catch (Exception e) {
				}
			}
		}

		pMap.put("RES_JSON", resStr.toString());
		return pMap;
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
			retMap.put("RESULT", "FAIL");
			retMap.put("MSG", this.getReturnMsg(2));
			return retMap;
		}

		boolean isExists = false;
		for (String id : ids) {
			if (id.equals(appId)) {
				isExists = true;
				break;
			}
		}

		if (isExists == false) {
			retMap.put("RESULT", "FAIL");
			retMap.put("MSG", this.getReturnMsg(3));
			return retMap;
		}

		if (actionCode.equals("info")) {
			String port = this.env.getProperty(appId + ".port");

			if (port == null || port.isEmpty()) {
				retMap.put("RESULT", "FAIL");
				retMap.put("MSG", this.getReturnMsg(5));
				return retMap;
			}

			retMap.put("PORT", port);

		} else {
			String path = this.env.getProperty(appId + "." + actionCode + ".path");

			if (path == null || path.isEmpty()) {
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
		}

		return msg;
	}
}