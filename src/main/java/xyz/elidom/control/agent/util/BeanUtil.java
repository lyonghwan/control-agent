/**
 * 
 */
package xyz.elidom.control.agent.util;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

import net.sf.common.util.BeanUtils;

/**
 * Spring Bean을 Bean 이름이나 클래스로 편리하게 가져오기 위한 유틸리티 클래스 
 * 
 * @author Minu.Kim
 */
@Service
public class BeanUtil implements ApplicationContextAware {
	
	/**
	 * net.sf.common.util.BeanUtils
	 */
	private static BeanUtils beanUtils;
	/**
	 * ApplicationContext
	 */
	private static ApplicationContext applicationContext;
	
	/**
	 * Application Context를 BeanUtils 객체에 Binding.
	 * @return
	 */
	private static BeanUtils getBeanUtils() {
		if (beanUtils == null) {
			beanUtils = BeanUtils.getInstance("elidom");
		}
		
		return beanUtils;
	}

	/**
	 * Name을 통한 Bean 가져오기 실행. 
	 * 
	 * @param beanName
	 * @return
	 */
	public static Object get(String beanName) {
		return getBeanUtils().get(beanName);
	}

	/**
	 * Type을 이용하여 Bean 가져오기 실행.
	 * 
	 * @param requiredType
	 * @return
	 */
	public static <T> T get(Class<T> requiredType) {
		return getBeanUtils().get(requiredType);
	}

	/**
	 * Name과 Type을 이용하여, Bean 가져오기 실행.
	 * 
	 * @param beanName
	 * @param requiredType
	 * @return
	 */
	public static <T> T get(String beanName, Class<T> requiredType) {
		T bean = getBeanUtils().get(beanName, requiredType);
		return bean;
	}

	/**
	 * ApplicationContext 가져오기 실행.
	 * 
	 * @param requiredType
	 * @return
	 */
	public static <T> T getByApplicationContext(Class<T> requiredType) {
		return applicationContext.getBean(requiredType);
	}
	
	/**
	 * ApplicationContext Dependency Injection.
	 * 
	 * @param applicationContext
	 */
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		BeanUtil.applicationContext = applicationContext;
	}
	
}