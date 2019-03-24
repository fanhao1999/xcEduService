package com.xuecheng.ucenter.dao;

import com.xuecheng.framework.domain.ucenter.XcMenu;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest
public class XcMenuMapperTest {

    @Autowired
    private XcMenuMapper xcMenuMapper;

    @Test
    public void selectPermissionByUserId() {
        List<XcMenu> xcMenuList = xcMenuMapper.selectPermissionByUserId("49");
        System.out.println(xcMenuList);
    }
}