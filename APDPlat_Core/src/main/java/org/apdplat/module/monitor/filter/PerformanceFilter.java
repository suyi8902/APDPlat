/**
 * 
 * APDPlat - Application Product Development Platform
 * Copyright (c) 2013, 杨尚川, yang-shangchuan@qq.com
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package org.apdplat.module.monitor.filter;

import org.apdplat.module.monitor.model.ProcessTime;
import org.apdplat.module.security.model.User;
import org.apdplat.module.security.service.OnlineUserService;
import org.apdplat.module.system.service.LogQueue;
import org.apdplat.module.system.service.PropertyHolder;
import org.apdplat.module.system.service.SystemListener;
import org.apdplat.platform.log.APDPlatLogger;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

/**
 *
 * @author 杨尚川
 */
public class PerformanceFilter implements Filter {
    private static final APDPlatLogger LOG = new APDPlatLogger(PerformanceFilter.class);
    private boolean enabled = false;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException {
        HttpServletRequest req=(HttpServletRequest)request;
        
        long start=0;
        if (enabled && filter(req)) {            
		start=System.currentTimeMillis();
        }
        chain.doFilter(request, response);
        if (enabled && filter(req)) {
		long end=System.currentTimeMillis();
                User user=OnlineUserService.getUser(req.getSession().getId());
                //如果没有用户登录，则不执行性能分析
                if(user==null){
                    return ;
                }
                ProcessTime logger=new ProcessTime();
                logger.setUsername(user.getUsername());
                logger.setUserIP(req.getRemoteAddr());
                try {
                    logger.setServerIP(InetAddress.getLocalHost().getHostAddress());
                } catch (UnknownHostException ex) {
                    LOG.error("保存日志出错(Error in saving log)",ex);
                }
                logger.setAppName(SystemListener.getContextPath());
                String resource=req.getRequestURI().replace(logger.getAppName(), "");
                logger.setResource(resource);
                logger.setStartTime(new Date(start));
                logger.setEndTime(new Date(end));
                logger.setProcessTime(end-start);
                LogQueue.addLog(logger);
        }
    }

    @Override
    public void init(FilterConfig fc) throws ServletException {
        LOG.info("初始化性能过滤器(Initialize the filter performance)");
        enabled = PropertyHolder.getBooleanProperty("monitor.performance");
        if(enabled){
            LOG.info("启用性能分析日志(Enable performance analyzing log)");
        }else{            
            LOG.info("禁用性能分析日志(Disable performance analyzing log)");
        }
    }

    @Override
    public void destroy() {
        LOG.info("销毁性能过滤器(Destroy the filter performance)");
    }

    private boolean filter(HttpServletRequest req) {
        String path=req.getRequestURI();
        if(path.contains("/log/")){
            LOG.info("路径包含/log/,不执行性能分析(/log/ in path, not execute performance analysis) "+path);
            return false;
        }
        if(path.contains("/monitor/")){
            LOG.info("路径包含/monitor/,不执行性能分析(/log/ in path, not execute performance analysis) "+path);
            return false;
        }
        return true;
    }
}