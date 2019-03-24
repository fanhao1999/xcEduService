package com.xuecheng.manage_cms.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.*;

@SpringBootTest
@RunWith(SpringRunner.class)
public class CmsPageServiceTest {

    @Autowired
    private CmsPageService cmsPageService;

    @Test
    public void getPageHtml() {
        String pageHtml = cmsPageService.getPageHtml("5c81e765b153244bb8f6522b");
        System.out.println(pageHtml);
    }
}