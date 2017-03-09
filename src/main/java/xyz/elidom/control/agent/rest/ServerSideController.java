package xyz.elidom.control.agent.rest;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import xyz.elidom.control.agent.util.StreamPrinter;

@RestController
public class ServerSideController {
	
	protected Logger logger = LoggerFactory.getLogger(ServerSideController.class);

	@Autowired
	Environment env;
	 
	/**
	 * App Container Start Batch
	 * @param appId
	 * @return execute message
	 */
	@RequestMapping(value = "/apps/{appId}/start", method = RequestMethod.POST)
	public String startBoot(@PathVariable("appId") String appId) {
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
	 * App Container Start Batch
	 * @param appId
	 * @return execute message
	 */
	@RequestMapping(value = "/apps/{app_id}/restart", method = RequestMethod.POST)
	public String retartBoot(@PathVariable("app_id") String appId) {
		this.stopBoot(appId);
		
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
		}
		
		return this.startBoot(appId);
	}
	
	/**
	 * App Container Stop Batch
	 * @param app_id
	 * @return execute message
	 */
	@RequestMapping(value = "/apps/{app_id}/stop", method = RequestMethod.POST)
	public String stopBoot(@PathVariable("app_id") String app_id){
		HashMap<String,String> pMap = this.checkProperties(app_id, "stop");
		if(pMap.get("RESULT").equals("FAIL")) return pMap.get("MSG");
		
		try {
			this.commandStart(pMap.get("PATH"));
		} catch (Exception e) {
			return "Error : \n\n" + e.getMessage();
		}
		return "Enterd Stop Command SUCCESS";
	}
	
	/**
	 * App Container Update Batch
	 * @param app_id
	 * @return execute message
	 */
	@RequestMapping(value = "/apps/{app_id}/udpate", method = RequestMethod.POST)
	public String deploy(@PathVariable("app_id") String app_id){
		HashMap<String,String> pMap = this.checkProperties(app_id, "update");
		if(pMap.get("RESULT").equals("FAIL")) return pMap.get("MSG");
		
		try {
			this.commandStart(pMap.get("PATH"));
		} catch (Exception e) {
			return "Error : \n\n" + e.getMessage();
		}
		return "Enterd Update Command SUCCESS";
	}
	
	/**
	 * 로그 파일을 읽어서 내용을 리턴  
	 * 
	 * @param app_id
	 * @return today log file
	 */
	@RequestMapping(value = "/apps/{app_id}/log", method = RequestMethod.GET, produces=MediaType.APPLICATION_JSON_VALUE)
	public Map<String, String> readLog(@PathVariable("app_id") String app_id) {
		
		HashMap<String,String> pMap = this.checkProperties(app_id, "log");
		if(pMap.get("RESULT").equals("FAIL")) return pMap;
		
		Date date = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		String logPath = (pMap.get("PATH").endsWith("/") ? pMap.get("PATH") : pMap.get("PATH") + "/") + "applicatoin." + sdf.format(date) + ".log";
		
		String content = "";
		FileReader fReader = null;
		BufferedReader bReader = null;
		Map<String, String> result = new HashMap<String, String>();
		
		try {
			fReader = new FileReader(logPath);
			bReader = new BufferedReader(fReader);
			
			String temp = "";
			while( (temp = bReader.readLine()) != null) {
			    content += temp + "\n";
			}
		} catch(FileNotFoundException e) {
			result.put("success", "false");
			result.put("msg", "Log File Not Found!");
			return result;
			
		} catch (Exception e) {
			result.put("success", "false");
			result.put("msg", e.getMessage());
			return result;
			
		} finally{
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
		result.put("log", content);
		return result;
	}
	
	@RequestMapping(value = "/apps/{app_id}/info", method = RequestMethod.POST)
	public String getOneDomainInfo(@PathVariable("app_id") String app_id)
	{
		HashMap<String, String> retMap = this.getDomainInfo(app_id);
		if(retMap.get("RESULT").equals("FAIL")) return retMap.get("MSG");
		
		return retMap.get("RES_JSON");
	}
	
	@RequestMapping(value = "/apps/infos", method = RequestMethod.GET)
	public String getDomainInfos() 
	{
		String[] ids = this.readPropertiesArray(this.env, "apps.id");
		
		if(ids == null || ids.length == 0)
		{
			return "Domain Id Not Found !";
		}
		
		
		JSONArray array = new JSONArray();
		
		for(int i = 0 ; i < ids.length ; i++){
			HashMap<String, String> retMap = this.getDomainInfo(ids[i]);
			if(retMap.get("RESULT").equals("FAIL")) return retMap.get("MSG");
			array.put(new JSONObject(retMap.get("RES_JSON")));
		}
		
		JSONObject resObj = new JSONObject();
		resObj.put("items", array);
		
		return resObj.toString();
	}
	
	/**
	 * Ping pong 
	 */
	@RequestMapping(value = "/ping", method = RequestMethod.GET)
	public String ping(){
		return "OK";
	}
	
	
	/**
	 * ������ ������ ���� ��������
	 * @param app_id
	 * @return
	 */
	private HashMap<String,String> getDomainInfo(String app_id) {
		HashMap<String,String> pMap = this.checkProperties(app_id, "info");
		if(pMap.get("RESULT").equals("FAIL")) return pMap;
		
		String url = "http://localhost:" + pMap.get("PORT") + "/info";

		BufferedReader brIn = null;
		InputStreamReader isr = null;
		StringBuffer resStr = null;
		
		try{
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
		}catch(Exception e){
			pMap.put("RESULT", "FAIL");
			pMap.put("MSG", "Error : \n\n" + e.getMessage());
			return pMap;
		}finally {
			if(brIn != null){
				try {
					brIn.close();
				} catch (Exception e) {
				}
			}
			if(isr != null){
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
	}

	/**
	 * Batch ���� ����
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
	 * �� ��ġ �۾��� �ʿ��� Property ���� Ȯ��
	 * @param app_id 
	 * @param batchCode
	 * @return HashMap
	 *         RESULT : Ȯ�� ���
	 *         MSG : ���� �޽���
	 *         PATH : �ش� �۾��� ó���� Batch ���� ���
	 */
	private HashMap<String, String> checkProperties(String app_id, String batchCode){
		HashMap<String, String> retMap = new HashMap<String, String>();

    	retMap.put("RESULT", "SUCCESS");

        String[] ids = this.readPropertiesArray(this.env, "apps.id");
        
        if(ids == null) {
        	retMap.put("RESULT", "FAIL");
        	retMap.put("MSG", this.getReturnMsg(2));
        	return retMap;
        }
        
		boolean isExists = false;
		for(String id : ids){
			if(id.equals(app_id)) isExists = true;
		}
		
		if(isExists == false){
        	retMap.put("RESULT", "FAIL");
        	retMap.put("MSG", this.getReturnMsg(3));
        	return retMap;
		}
		
		
		if(batchCode.equals("info")){
			String port = this.env.getProperty(app_id + ".port");
			
			if(port == null || port.isEmpty())
			{
	        	retMap.put("RESULT", "FAIL");
	        	retMap.put("MSG", this.getReturnMsg(5));
	        	return retMap;
			}
			
			retMap.put("PORT", port);
		}else {
			String path = this.env.getProperty(app_id + "." + batchCode + ".path");
			
			if(path == null || path.isEmpty())
			{
	        	retMap.put("RESULT", "FAIL");
	        	retMap.put("MSG", this.getReturnMsg(4));
	        	return retMap;
			}
			
			retMap.put("PATH", path);
		}
		
		return retMap;
	}

	/**
	 * Properties ���� key �� �ش��ϴ� String �迭�� �����´�
	 * @param _properties
	 * @param key
	 * @return String[]
	 */
	private String[] readPropertiesArray(Environment _properties, String key){
		String readValue = _properties.getProperty(key);
		
		if(readValue == null) return null;
		return readValue.split(",");
	}
	
	private String getReturnMsg(int code){
		String msg = "";
		if(code == 1){
			msg = "Can Not Read The Properties File!";
		} else if(code == 2){
			msg = "Can Not Find Application ID Properties !" ;
		} else if(code == 3){
			msg = "Wrong Application ID!";
		} else if(code == 4){
			msg = "Can Not Find Application Batch Path !";
		} else if(code == 5){
			msg = "Can Not Find Application Port No !";
		}
		
		return msg;
	}
	
}
