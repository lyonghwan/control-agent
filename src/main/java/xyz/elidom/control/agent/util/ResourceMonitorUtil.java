package xyz.elidom.control.agent.util;

import java.io.File;
import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import de.codecentric.boot.admin.model.SystemStatus;

/**
 * Server Resource Monitor를 위한 유틸리티 
 * 
 * @author shortstop
 */
public class ResourceMonitorUtil {
	/**
	 * MBean Server
	 */
	private static MBeanServer MBS;
	/**
	 * MBean Object
	 */
	private static ObjectName OS_OBJ_NAME;
	
	/**
	 * MBean Server 리턴 
	 * 
	 * @return
	 */
	public static MBeanServer getMbeanServer() {
		if(MBS == null) {
			MBS = ManagementFactory.getPlatformMBeanServer();
		}
		
		return MBS;
	}
	
	
	public static ObjectName getObjectName() {
		if(OS_OBJ_NAME == null) {
			try {
				OS_OBJ_NAME = ObjectName.getInstance("java.lang:type=OperatingSystem");
			} catch(Exception e) {
				return null;
			}
		}
		
		return OS_OBJ_NAME;
	}
	
	/**
	 * MBean Attribute 값을 조회하여 리턴 
	 * 
	 * @param attrName
	 * @return
	 */
	public static String getMbeanAttrStr(String attrName) {
		try {
			String value = (String)getMbeanServer().getAttribute(getObjectName(), attrName);
			return value;
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * MBean Attribute 값을 조회하여 리턴 
	 * 
	 * @param attrName
	 * @return
	 */
	public static int getMbeanAttrInt(String attrName) {
		try {
			Integer value = (Integer)getMbeanServer().getAttribute(getObjectName(), attrName);
			return value.intValue();
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * MBean Attribute 값을 조회하여 리턴 
	 * 
	 * @param attrName
	 * @return
	 */
	public static long getMbeanAttrLong(String attrName) {
		try {
			Long value = (Long)getMbeanServer().getAttribute(getObjectName(), attrName);
			return value.longValue();
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}	
	
	/**
	 * MBean Attribute 값을 조회하여 리턴 
	 * 
	 * @param attrName
	 * @return
	 */
	public static double getMbeanAttrDouble(String attrName) {
		try {
			Double value = (Double)getMbeanServer().getAttribute(getObjectName(), attrName);
			return value.doubleValue();
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * OS Name
	 * 
	 * @return
	 */
	public static String getOsName() {
		return getMbeanAttrStr("Name");
	}
	
	/**
	 * OS Version
	 * 
	 * @return
	 */
	public static String getOsVersion() {
		return getMbeanAttrStr("Version");
	}
	
	/**
	 * OS Name + OS Version
	 * 
	 * @return
	 */
	public static String getOsDescription() {
		return getOsName() + " " + getOsVersion();
	}
	
	/**
	 * OS Architecture
	 * 
	 * @return
	 */
	public static String getOsArch() {
		return getMbeanAttrStr("Arch");
	}
	
	/**
	 * Available Processors
	 * 
	 * @return
	 */
	public static int getAvailableProcessors() {
		return getMbeanAttrInt("AvailableProcessors");
	}

	/**
	 * Process CPU Load
	 * 
	 * @return
	 */
	public static double getProcessCpuLoad() {
		return getMbeanAttrDouble("ProcessCpuLoad");
	}
	
	/**
	 * System CPU Load
	 * 
	 * @return
	 */
	public static double getSystemCpuLoad() {
		return getMbeanAttrDouble("SystemCpuLoad");
	}
	
	/**
	 * Process CPU Time
	 * 
	 * @return
	 */
	public static long getProcessCpuTime() {
		return getMbeanAttrLong("ProcessCpuTime");
	}
	
	/**
	 * System Load Average
	 * 
	 * @return
	 */
	public static double getSystemLoadAverage() {
		return getMbeanAttrDouble("SystemLoadAverage");
	}
	
	/**
	 * Committed Virtual Memory Size
	 * 
	 * @return
	 */
	public static long getCommittedVirtualMemorySize() {
		return getMbeanAttrLong("CommittedVirtualMemorySize");
	}
	
	/**
	 * Total Swap Space Size
	 * 
	 * @return
	 */
	public static long getTotalSwapSpaceSize() {
		return getMbeanAttrLong("TotalSwapSpaceSize");
	}
	
	/**
	 * Free Swap Space Size
	 * 
	 * @return
	 */
	public static long getFreeSwapSpaceSize() {
		return getMbeanAttrLong("FreeSwapSpaceSize");
	}
	
	/**
	 * Free Physical Memory Size
	 * 
	 * @return
	 */
	public static long getFreePhysicalMemorySize() {
		return getMbeanAttrLong("FreePhysicalMemorySize");
	}
	
	/**
	 * Total Physical Memory Size
	 * 
	 * @return
	 */
	public static long getTotalPhysicalMemorySize() {
		return getMbeanAttrLong("TotalPhysicalMemorySize");
	}
	
	/**
	 * Total Disk Space
	 * 
	 * @return
	 */
	public static long getTotalDiskSpace() {
		File[] roots = File.listRoots();
		long totalDiskSpace = 0;		
		for (File root : roots) {
	      totalDiskSpace += root.getTotalSpace();
	    }
		
		return totalDiskSpace;
	}
	
	/**
	 * Free Disk Space
	 * 
	 * @return
	 */
	public static long getFreeDiskSpace() {
		File[] roots = File.listRoots();
		long freeDiskSpace = 0;		
		for (File root : roots) {
			freeDiskSpace += root.getFreeSpace();
	    }
		
		return freeDiskSpace;
	}
	
	/**
	 * 현재 상태의 시스템 리소스 상태 
	 * 
	 * @return
	 */
	public static SystemStatus getCurrentSystemResourceStatus() {
		SystemStatus status = new SystemStatus();
		status.setName(getOsName());
		status.setVersion(getOsVersion());
		status.setArch(getOsArch());
		status.setAvaliableProcessors(getAvailableProcessors());
		status.setProcessCpuLoad(getProcessCpuLoad());
		status.setProcessCpuTime(getProcessCpuTime());
		status.setSystemCpuLoad(getSystemCpuLoad());
		status.setSystemLoadAverage(getSystemLoadAverage());
		status.setTotalPhysicalMemorySize(getTotalPhysicalMemorySize());
		status.setFreePhysicalMemorySize(getFreePhysicalMemorySize());
		status.setTotalSwapSpaceSize(getTotalSwapSpaceSize());
		status.setFreeSwapSpaceSize(getFreeSwapSpaceSize());
		status.setCommittedVirtualMemorySize(getCommittedVirtualMemorySize());
		status.setTotalDiskSpace(getTotalDiskSpace());
		status.setFreeDiskSpace(getFreeDiskSpace());
		return status;
	}
	
}

