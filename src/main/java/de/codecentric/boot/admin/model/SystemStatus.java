package de.codecentric.boot.admin.model;

import java.util.HashMap;
import java.util.Map;

/**
 * 시스템 상태 정보
 * 
 * @author shortstop
 */
public class SystemStatus {
	
	/**
	 * operating system명을 돌려줍니다. 
	 * 이 메소드는 System.getProperty("os.name") 와 동등합니다.
	 */
	private String name;
	/**
	 * operating system의 버젼을 돌려줍니다. 
	 * 이 메소드는 System.getProperty("os.version") 와 동등합니다.
	 */
	private String version;
	/**
	 * operating system의 버젼을 돌려줍니다. 
	 * 이 메소드는 System.getProperty("os.version") 와 동등합니다.
	 */
	private String arch;
	/**
	 * Java 가상 머신이 이용 가능한 프로세서의 수를 돌려줍니다. 
	 * 이 메소드는,Runtime.availableProcessors() 메소드와 동등합니다.
	 */
	private int availableProcessors;
	/**
	 * 마지막 1 분의 시스템 부하 평균을 돌려줍니다. 
	 * 시스템 부하 평균이란,이용 가능한 프로세서 의 큐에 넣어진 실행 가능한 엔티티의 수와 어느 기간에 평균한, 이용 가능한 프로세서로 실행되고 있는 실행 가능한 엔티티의 수의 합계입니다.
	 *  부하 평균의 계산방법은 operating system에 따라서 다릅니다만, 일반적으로은, 감쇠 시간 의존 평균이 사용됩니다.
	 *  부하 평균을 이용할 수 없는 경우는, 0 보다 작은 값이 돌려주어집니다
	 */
	private double systemLoadAverage;
	/**
	 * 
	 */
	private double systemCpuLoad;
	/**
	 * Java 가상 머신이 실행되고 있는 프로세스로 사용되는 CPU 시간을 초단위로 돌려줍니다. 
	 */
	private long processCpuTime;	
	/**
	 * 
	 */
	private double processCpuLoad;
	/**
	 * 물리 메모리의 합계 용량을 바이트 단위로 돌려줍니다.
	 */
	private long totalPhysicalMemorySize;
	/**
	 * 빈물리 메모리의 용량을 바이트 단위로 돌려줍니다
	 */
	private long freePhysicalMemorySize;
	/**
	 * 프로세스의 실행에 이용 가능한 가상기억의 용량을 바이트 단위로 돌려줍니다
	 */
	private long committedVirtualMemorySize;
	/**
	 * 스왑 공간의 합계 용량을 바이트 단위로 돌려줍니다.
	 */
	private long totalSwapSpaceSize;
	/**
	 * 빈스왑 공간의 용량을 바이트 단위로 돌려줍니다
	 */
	private long freeSwapSpaceSize;
	/**
	 * 총 디스크 용량
	 */
	private long totalDiskSpace;
	/**
	 * 사용 가능한 디스크 용량 
	 */
	private long freeDiskSpace;
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getVersion() {
		return version;
	}
	
	public void setVersion(String version) {
		this.version = version;
	}
	
	public String getArch() {
		return arch;
	}
	
	public void setArch(String arch) {
		this.arch = arch;
	}
	
	public int getAvaliableProcessors() {
		return availableProcessors;
	}
	
	public void setAvaliableProcessors(int availableProcessors) {
		this.availableProcessors = availableProcessors;
	}
	
	public double getSystemLoadAverage() {
		return systemLoadAverage;
	}
	
	public void setSystemLoadAverage(double systemLoadAverage) {
		this.systemLoadAverage = systemLoadAverage;
	}
	
	public double getSystemCpuLoad() {
		return systemCpuLoad;
	}
	
	public void setSystemCpuLoad(double systemCpuLoad) {
		this.systemCpuLoad = systemCpuLoad;
	}
	
	public double getProcessCpuLoad() {
		return processCpuLoad;
	}
	
	public void setProcessCpuLoad(double processCpuLoad) {
		this.processCpuLoad = processCpuLoad;
	}
	
	public long getProcessCpuTime() {
		return processCpuTime;
	}
	
	public void setProcessCpuTime(long processCpuTime) {
		this.processCpuTime = processCpuTime > 0 ? processCpuTime / 1000000000 : processCpuTime;
	}
	
	public double getTotalPhysicalMemorySize() {
		return totalPhysicalMemorySize;
	}
	
	public void setTotalPhysicalMemorySize(long totalPhysicalMemorySize) {
		this.totalPhysicalMemorySize = totalPhysicalMemorySize;
	}
	
	public double getFreePhysicalMemorySize() {
		return freePhysicalMemorySize;
	}
	
	public void setFreePhysicalMemorySize(long freePhysicalMemorySize) {
		this.freePhysicalMemorySize = freePhysicalMemorySize;
	}
	
	public double getCommittedVirtualMemorySize() {
		return committedVirtualMemorySize;
	}
	
	public void setCommittedVirtualMemorySize(long committedVirtualMemorySize) {
		this.committedVirtualMemorySize = committedVirtualMemorySize;
	}
	
	public double getTotalSwapSpaceSize() {
		return totalSwapSpaceSize;
	}
	
	public void setTotalSwapSpaceSize(long totalSwapSpaceSize) {
		this.totalSwapSpaceSize = totalSwapSpaceSize;
	}
	
	public double getFreeSwapSpaceSize() {
		return freeSwapSpaceSize;
	}
	
	public void setFreeSwapSpaceSize(long freeSwapSpaceSize) {
		this.freeSwapSpaceSize = freeSwapSpaceSize;
	}
	
	public long getTotalDiskSpace() {
		return totalDiskSpace;
	}
	
	public void setTotalDiskSpace(long totalDiskSpace) {
		this.totalDiskSpace = totalDiskSpace;
	}
	
	public long getFreeDiskSpace() {
		return freeDiskSpace;
	}
	
	public void setFreeDiskSpace(long freeDiskSpace) {
		this.freeDiskSpace = freeDiskSpace;
	}
	
	public Map<String, Object> toMap() {
		Map<String, Object> map = new HashMap<String, Object>();		
		map.put("name", this.name);
		map.put("version", this.version);
		map.put("arch", this.arch);
		map.put("availableProcessors", this.availableProcessors);
		map.put("systemLoadAverage", this.systemLoadAverage);
		map.put("systemCpuLoad", this.systemCpuLoad);
		map.put("processCpuTime", this.processCpuTime);
		map.put("processCpuLoad", this.processCpuLoad);
		map.put("totalPhysicalMemorySize", this.totalPhysicalMemorySize);
		map.put("freePhysicalMemorySize", this.freePhysicalMemorySize);
		map.put("committedVirtualMemorySize", this.committedVirtualMemorySize);
		map.put("totalSwapSpaceSize", this.totalSwapSpaceSize);
		map.put("freeSwapSpaceSize", this.freeSwapSpaceSize);
		map.put("totalDiskSpace", this.totalDiskSpace);
		map.put("freeDiskSpace", this.freeDiskSpace);
		return map;
	}
	
	/**
	 * map으로 부터 SystemStatus를 생성하여 리턴 
	 * 
	 * @param map
	 * @return
	 */
	public static SystemStatus fromMap(Map<String, Object> map) {
		SystemStatus ss = new SystemStatus();
		
		if(map != null) {
			ss.setName(map.containsKey("name") ? (String)map.get("name") : null);
			ss.setVersion(map.containsKey("version") ? (String)map.get("version") : null);
			ss.setArch(map.containsKey("arch") ? (String)map.get("arch") : null);
			ss.setAvaliableProcessors(valueToInt(map.get("availableProcessors")));
			ss.setSystemCpuLoad(valueToDouble(map.get("systemCpuLoad")));
			ss.setSystemLoadAverage(valueToDouble(map.get("systemLoadAverage")));
			ss.setProcessCpuLoad(valueToDouble(map.get("processCpuLoad")));
			ss.setTotalDiskSpace(valueToLong(map.get("totalDiskSpace")));
			ss.setFreeDiskSpace(valueToLong(map.get("freeDiskSpace")));
			ss.setProcessCpuTime(valueToLong(map.get("processCpuTime")));
			ss.setTotalPhysicalMemorySize(valueToLong(map.get("totalPhysicalMemorySize")));
			ss.setFreePhysicalMemorySize(valueToLong(map.get("freePhysicalMemorySize")));
			ss.setCommittedVirtualMemorySize(valueToLong(map.get("committedVirtualMemorySize")));
			ss.setTotalSwapSpaceSize(valueToLong(map.get("totalSwapSpaceSize")));
			ss.setFreeSwapSpaceSize(valueToLong(map.get("freeSwapSpaceSize")));
		}
		
		return ss;
	}
	
	/**
	 * value값을 int 값으로 변환 
	 * 
	 * @param value
	 * @return
	 */
	private static int valueToInt(Object value) {
		if(value == null || value.toString().equalsIgnoreCase("")) {
			return 0;
			
		} else if(value instanceof Integer) {
			return ((Integer)value).intValue();
			
		} else if(value instanceof Double) {
			return ((Double)value).intValue();
			
		} else if(value instanceof Float) {
			return ((Float)value).intValue();
			
		} else if(value instanceof Long) {
			return ((Long)value).intValue();
			
		} else {
			return new Integer(value.toString()).intValue();
		}		
	}
	
	/**
	 * value값을 double 값으로 변환 
	 * 
	 * @param value
	 * @return
	 */
	private static double valueToDouble(Object value) {
		if(value == null || value.toString().equalsIgnoreCase("")) {
			return 0;
			
		} else if(value instanceof Integer) {
			return ((Integer)value).doubleValue();
			
		} else if(value instanceof Double) {
			return ((Double)value).doubleValue();
			
		} else if(value instanceof Float) {
			return ((Float)value).doubleValue();
			
		} else if(value instanceof Long) {
			return ((Long)value).doubleValue();
			
		} else {
			return new Double(value.toString()).doubleValue();
		}
	}
	
	/**
	 * value값을 long값으로 변환 
	 * 
	 * @param value
	 * @return
	 */
	private static long valueToLong(Object value) {
		if(value == null || value.toString().equalsIgnoreCase("")) {
			return 0l;
			
		} else if(value instanceof Integer) {
			return ((Integer)value).longValue();
			
		} else if(value instanceof Double) {
			return ((Double)value).longValue();
			
		} else if(value instanceof Float) {
			return ((Float)value).longValue();
			
		} else if(value instanceof Long) {
			return ((Long)value).longValue();
			
		} else {
			return new Long(value.toString()).longValue();
		}
	}
	
	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer("{");
		buffer.append("name:").append(this.name).append(",");
		buffer.append("version:").append(this.version).append(",");
		buffer.append("arch:").append(this.arch).append(",");
		buffer.append("availableProcessors:").append(this.availableProcessors).append(",");
		buffer.append("systemLoadAverage:").append(this.systemLoadAverage).append(",");
		buffer.append("systemCpuLoad:").append(this.systemCpuLoad).append(",");
		buffer.append("processCpuTime:").append(this.processCpuTime).append(",");
		buffer.append("processCpuLoad:").append(this.processCpuLoad).append(",");
		buffer.append("totalPhysicalMemorySize:").append(this.totalPhysicalMemorySize).append(",");
		buffer.append("freePhysicalMemorySize:").append(this.freePhysicalMemorySize).append(",");
		buffer.append("committedVirtualMemorySize:").append(this.committedVirtualMemorySize).append(",");
		buffer.append("totalSwapSpaceSize:").append(this.totalSwapSpaceSize).append(",");
		buffer.append("freeSwapSpaceSize:").append(this.freeSwapSpaceSize).append(",");
		buffer.append("totalDiskSpace:").append(this.totalDiskSpace).append(",");
		buffer.append("freeDiskSpace:").append(this.freeDiskSpace);
		buffer.append("}");
		return buffer.toString();
	}
	
}