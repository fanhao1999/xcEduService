package com.xuecheng.manage_cms.controller;

import com.xuecheng.framework.exception.ExceptionCast;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.web.BaseController;
import com.xuecheng.manage_cms.service.CmsPageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import javax.servlet.ServletOutputStream;
import java.io.IOException;

@Controller
public class CmsPagePreviewController extends BaseController {

    @Autowired
    private CmsPageService cmsPageService;

    @GetMapping("/cms/preview/{pageId}")
    public void preview(@PathVariable("pageId") String pageId) {
        String pageHtml = cmsPageService.getPageHtml(pageId);
        try {
            ServletOutputStream outputStream = response.getOutputStream();
            response.setHeader("Content-type", "text/html;charset=utf-8");
            outputStream.write(pageHtml.getBytes("utf-8"));
        } catch (IOException e) {
            ExceptionCast.cast(CommonCode.FAIL);
        }
    }
}
