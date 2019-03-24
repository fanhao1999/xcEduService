package com.xuecheng.ucenter.service;

import com.xuecheng.framework.domain.ucenter.XcCompanyUser;
import com.xuecheng.framework.domain.ucenter.XcMenu;
import com.xuecheng.framework.domain.ucenter.XcUser;
import com.xuecheng.framework.domain.ucenter.ext.XcUserExt;
import com.xuecheng.ucenter.dao.XcCompanyUserRepository;
import com.xuecheng.ucenter.dao.XcMenuMapper;
import com.xuecheng.ucenter.dao.XcUserRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Service
@Transactional
public class UserService {

    @Autowired
    private XcUserRepository xcUserRepository;

    @Autowired
    private XcCompanyUserRepository xcCompanyUserRepository;

    @Autowired
    private XcMenuMapper xcMenuMapper;

    //根据账号查询用户的信息，返回用户扩展信息
    public XcUserExt getUserext(String username) {
        XcUser xcUser = findXcUserByUsername(username);
        if (xcUser == null) {
            return null;
        }
        XcUserExt xcUserExt = new XcUserExt();
        BeanUtils.copyProperties(xcUser, xcUserExt);
        //用户id
        String userId = xcUserExt.getId();
        //查询用户所属公司
        XcCompanyUser xcCompanyUser = xcCompanyUserRepository.findByUserId(userId);
        if (xcCompanyUser != null) {
            String companyId = xcCompanyUser.getCompanyId();
            xcUserExt.setCompanyId(companyId);
        }
        //根据用户id查询用户权限
        List<XcMenu> xcMenuList = xcMenuMapper.selectPermissionByUserId(userId);
        if (!CollectionUtils.isEmpty(xcMenuList)) {
            //用户的权限
            xcUserExt.setPermissions(xcMenuList);
        }
        return xcUserExt;
    }

    //根据用户账号查询用户信息
    private XcUser findXcUserByUsername(String username) {
        return xcUserRepository.findByUsername(username);
    }
}
