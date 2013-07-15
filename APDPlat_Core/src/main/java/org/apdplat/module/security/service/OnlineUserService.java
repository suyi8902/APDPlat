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

package org.apdplat.module.security.service;

import org.apdplat.module.security.model.Org;
import org.apdplat.module.security.model.Role;
import org.apdplat.module.security.model.User;
import org.apdplat.platform.log.APDPlatLogger;
import org.apdplat.platform.util.SpringContextUtils;
import java.util.ArrayList;
import java.util.List;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;

public class OnlineUserService{
    private static final APDPlatLogger LOG = new APDPlatLogger(OnlineUserService.class);

    private static SessionRegistry sessionRegistry;
    public static String getUsername(String sessionID) {
        User user = getUser(sessionID);
        if (user == null) {
            return "匿名用户";
        }
        return user.getUsername();
    }
    public static User getUser(String sessionID) {
        User user = null;
        if(sessionRegistry==null){
            sessionRegistry=SpringContextUtils.getBean("sessionRegistry");
        }
        if(sessionRegistry==null){
            LOG.debug("没有从spring中获取到sessionRegistry");
            return null;
        }
        SessionInformation info=sessionRegistry.getSessionInformation(sessionID);
        if(info==null){
            LOG.debug("没有获取到会话ID为："+sessionID+" 的在线用户");
            return null;
        }
        user = (User)info.getPrincipal();
        LOG.debug("获取到会话ID为："+sessionID+" 的在线用户");
        
        
        return user;
    }

    public static List<User> getUser(Org org,Role role){
        if(sessionRegistry==null){
            sessionRegistry=SpringContextUtils.getBean("sessionRegistry");
        }
        if(sessionRegistry==null){
            LOG.info("没有从spring中获取到sessionRegistry");
            return null;
        }
        List<Object> users=sessionRegistry.getAllPrincipals();
        List<User> result=new ArrayList<>();
        LOG.info("获取在线用户,org:"+org+",role:"+role);
        if(org==null && role==null ){
            //返回所有在线用户
            for(Object obj : users){
                User user=(User)obj;
                LOG.info("获取到会话ID为："+sessionRegistry.getAllSessions(obj, false).get(0).getSessionId() +" 的在线用户");
                result.add(user);
            }
        }
        //取交集
        if(org!=null && role!=null){
            //返回特定组织架构及其所有子机构 且 属于特定角色的在线用户
            List<Integer> orgIds=OrgService.getChildIds(org);
            orgIds.add(org.getId());
            List<Integer> roleIds=RoleService.getChildIds(role);
            roleIds.add(role.getId());
            LOG.info("特定组织架构及其所有子组织架构:"+orgIds);
            LOG.info("特定角色及其所有子角色:"+orgIds);
            for(Object obj : users){
                User user=(User)obj;
                LOG.info("获取到会话ID为："+sessionRegistry.getAllSessions(obj, false).get(0).getSessionId() +" 的在线用户");
                if(orgIds.contains(user.getOrg().getId())){
                    for(Role r : user.getRoles()){
                        if(roleIds.contains(r.getId())){
                            result.add(user);
                            break;
                        }
                    }
                }
            }
            return result;
        }
        if(org!=null){
            //返回特定组织架构及其所有子组织架构的在线用户
            List<Integer> ids=OrgService.getChildIds(org);
            ids.add(org.getId());
            LOG.info("特定组织架构及其所有子机构:"+ids);
            for(Object obj : users){
                User user=(User)obj;
                LOG.info("获取到会话ID为："+sessionRegistry.getAllSessions(obj, false).get(0).getSessionId() +" 的在线用户");
                if(ids.contains(user.getOrg().getId())){
                    result.add(user);
                }
            }
        }
        if(role!=null){
            //返回属于特定角色及其所有子角色的在线用户
            List<Integer> roleIds=RoleService.getChildIds(role);
            roleIds.add(role.getId());
            for(Object obj : users){
                User user=(User)obj;
                LOG.info("获取到会话ID为："+sessionRegistry.getAllSessions(obj, false).get(0).getSessionId() +" 的在线用户");
                for(Role r : user.getRoles()){
                    if(roleIds.contains(r.getId())){
                        result.add(user);
                        break;
                    }
                }
            }
        }
        return result;
    }
}