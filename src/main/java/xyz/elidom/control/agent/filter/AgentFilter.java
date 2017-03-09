package xyz.elidom.control.agent.filter;

import java.io.IOException;

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
public class AgentFilter implements Filter {

	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) res;
		String uri = request.getRequestURI();
		
		if(!uri.startsWith("/admin")) {
			String allowOrigin = response.getHeader("Access-Control-Allow-Origin");
			if(allowOrigin == null || allowOrigin.equalsIgnoreCase("")) {
				response.setHeader("Access-Control-Allow-Credentials", "true");
				response.setHeader("Access-Control-Allow-Methods", "POST, PUT, GET, OPTIONS, DELETE");
				response.setHeader("Access-Control-Max-Age", "3600000");
				response.setHeader("Access-Control-Allow-Headers", "Content-Type,Access-Control-Allow-Headers,Authorization,X-Requested-With,X-Content-Type-Options,X-Frame-Options,X-XSS-Protection,X-Locale,X-Domain-Id");
		
				String origin = request.getHeader("Origin");
				if(allowOrigin == null || allowOrigin.equalsIgnoreCase("")) {
					response.setHeader("Access-Control-Allow-Origin", origin);
				}
			}
		}

		chain.doFilter(req, res);
	}

	public void init(FilterConfig filterConfig) {
	}

	public void destroy() {
	}
}
