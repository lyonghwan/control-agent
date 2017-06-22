package xyz.elidom.control.agent.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Service;

@Service
@WebFilter("/*.json")
public class CorsFilter implements Filter {
	
	/**
	 * Admin API URL
	 */
	private static final String ADMIN_API_URL = "/admin/api/applications";
	
	/**
	 * Apps API URL
	 */
	private static final String APPS_API_URL = "/apps";
	
	/**
	 * 헤더 키 Access-Control-Allow-Origin 
	 */
	private static final String HEADER_ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
	/**
	 * 헤더 키 Access-Control-Allow-Methods
	 */	
	private static final String HEADER_ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";
	/**
	 * 헤더 키 Access-Control-Max-Age
	 */	
	private static final String HEADER_ACCESS_CONTROL_MAX_AGE = "Access-Control-Max-Age";
	/**
	 * 헤더 키 Access-Control-Allow-Headers
	 */	
	private static final String HEADER_ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";
	/**
	 * Access-Control-Allow-Methods 기본 값 
	 */
	private static final String VALUE_ACCESS_CONTROL_ALLOW_METHODS = "POST, PUT, GET, OPTIONS, DELETE";
	/**
	 * Access-Control-Max-Age 기본 값 
	 */	
	private static final String VALUE_ACCESS_CONTROL_MAX_AGE = "3600000";
	/**
	 * Access-Control-Allow-Headers 기본 값 
	 */	
	private static final String VALUE_ACCESS_CONTROL_ALLOW_HEADERS = "Content-Type,Access-Control-Allow-Headers,Authorization,X-Requested-With,X-Content-Type-Options,X-Frame-Options,X-XSS-Protection,X-Locale,X-Domain-Id";
	/**
	 * 헤더 키 Origin 
	 */	
	private static final String HEADER_ORIGIN = "Origin";
	/**
	 * Empty String 
	 */	
	private static final String EMPTY_STR = "";
	
	/**
	 * permit URLs
	 */
	private List<String> permitUrls = new ArrayList<String>();

	@Override
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) res;
		String uri = request.getRequestURI();
		
		if(this.checkUrl(uri)) {
			response.setHeader(HEADER_ACCESS_CONTROL_MAX_AGE, VALUE_ACCESS_CONTROL_MAX_AGE);
			response.setHeader(HEADER_ACCESS_CONTROL_ALLOW_METHODS, VALUE_ACCESS_CONTROL_ALLOW_METHODS);
			response.setHeader(HEADER_ACCESS_CONTROL_ALLOW_HEADERS, VALUE_ACCESS_CONTROL_ALLOW_HEADERS);
	
			String origin = request.getHeader(HEADER_ORIGIN);
			if(origin != null && !origin.equalsIgnoreCase(EMPTY_STR)) {
				String allowOrigin = response.getHeader(HEADER_ACCESS_CONTROL_ALLOW_ORIGIN);
				if(allowOrigin == null || allowOrigin.equalsIgnoreCase(EMPTY_STR)) {
					response.setHeader(HEADER_ACCESS_CONTROL_ALLOW_ORIGIN, origin);
				}
			}
		}

		chain.doFilter(req, res);
	}
	
	/**
	 * URL Check
	 * 
	 * @param url
	 * @return
	 */
	private boolean checkUrl(String url) {
		for(String permitUrl : this.permitUrls) {
			if(url.startsWith(permitUrl)) {
				return true;
			}
		}
		
		return false;
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		this.permitUrls.add(ADMIN_API_URL);
		this.permitUrls.add(APPS_API_URL);
	}

	@Override
	public void destroy() {
	}
	
}
