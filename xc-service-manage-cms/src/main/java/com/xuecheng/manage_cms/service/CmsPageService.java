package com.xuecheng.manage_cms.service;

import com.alibaba.fastjson.JSON;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSDownloadStream;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.xuecheng.framework.domain.cms.CmsPage;
import com.xuecheng.framework.domain.cms.CmsSite;
import com.xuecheng.framework.domain.cms.CmsTemplate;
import com.xuecheng.framework.domain.cms.request.QueryPageRequest;
import com.xuecheng.framework.domain.cms.response.CmsCode;
import com.xuecheng.framework.domain.cms.response.CmsPageResult;
import com.xuecheng.framework.domain.cms.response.CmsPostPageResult;
import com.xuecheng.framework.exception.ExceptionCast;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.QueryResponseResult;
import com.xuecheng.framework.model.response.QueryResult;
import com.xuecheng.framework.model.response.ResponseResult;
import com.xuecheng.manage_cms.config.RabbitmqConfig;
import com.xuecheng.manage_cms.dao.CmsPageRepository;
import com.xuecheng.manage_cms.dao.CmsSiteRepository;
import com.xuecheng.manage_cms.dao.CmsTemplateRepository;
import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class CmsPageService {

    @Autowired
    private CmsPageRepository cmsPageRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private CmsTemplateRepository cmsTemplateRepository;

    @Autowired
    private GridFsTemplate gridFsTemplate;

    @Autowired
    private GridFSBucket gridFSBucket;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private CmsSiteRepository cmsSiteRepository;

    public QueryResponseResult findList(int page, int size, QueryPageRequest queryPageRequest) {
        page = page <= 0 ? 1 : page;
        size = size <= 0 ? 10 : size;

        // 非空判断
        queryPageRequest = queryPageRequest == null ? new QueryPageRequest() : queryPageRequest;
        // 根据站点id、模板id以及页面别名条件判断
        CmsPage cmsPage = new CmsPage();
        if (StringUtils.isNotBlank(queryPageRequest.getSiteId())) {
            cmsPage.setSiteId(queryPageRequest.getSiteId());
        }
        if (StringUtils.isNotBlank(queryPageRequest.getTemplateId())) {
            cmsPage.setTemplateId(queryPageRequest.getTemplateId());
        }
        // 页面别名模糊查询
        ExampleMatcher exampleMatcher = null;
        if (StringUtils.isNotBlank(queryPageRequest.getPageAliase())) {
            cmsPage.setPageAliase(queryPageRequest.getPageAliase());
            exampleMatcher = ExampleMatcher.matching().withMatcher("pageAliase",
                    ExampleMatcher.GenericPropertyMatchers.contains());
        }

        Example<CmsPage> example = exampleMatcher == null ? Example.of(cmsPage) : Example.of(cmsPage, exampleMatcher);

        Pageable pageable = PageRequest.of(page -1, size);
        Page<CmsPage> cmsPagePage = cmsPageRepository.findAll(example, pageable);

        QueryResult<CmsPage> queryResult = new QueryResult();
        queryResult.setList(cmsPagePage.getContent());
        queryResult.setTotal(cmsPagePage.getTotalElements());
        QueryResponseResult responseResult = new QueryResponseResult(CommonCode.SUCCESS, queryResult);
        return responseResult;
    }

    public CmsPageResult addPage(CmsPage cmsPage) {
        CmsPage page = cmsPageRepository.findByPageNameAndPageWebPathAndSiteId(
                cmsPage.getPageName(), cmsPage.getPageWebPath(), cmsPage.getSiteId());
        if (page != null) {
            ExceptionCast.cast(CmsCode.CMS_ADDPAGE_EXISTSNAME);
        }
        cmsPage.setPageId(null);
        cmsPage = cmsPageRepository.save(cmsPage);
        return new CmsPageResult(CommonCode.SUCCESS, cmsPage);
    }

    public CmsPage findById(String pageId) {
        Optional<CmsPage> cmsPageOptional = cmsPageRepository.findById(pageId);
        if (cmsPageOptional.isPresent()) {
            return cmsPageOptional.get();
        }
        return null;
    }

    public CmsPageResult updatePage(String pageId, CmsPage cmsPage) {
        CmsPage one = findById(pageId);
        if (one == null) {
            return new CmsPageResult(CommonCode.FAIL, null);
        }
        one.setTemplateId(cmsPage.getTemplateId());
        one.setSiteId(cmsPage.getSiteId());
        one.setPageAliase(cmsPage.getPageAliase());
        one.setPageName(cmsPage.getPageName());
        one.setPageWebPath(cmsPage.getPageWebPath());
        one.setPagePhysicalPath(cmsPage.getPagePhysicalPath());
        one.setDataUrl(cmsPage.getDataUrl());

        CmsPage save = cmsPageRepository.save(one);
        if (save == null) {
            return new CmsPageResult(CommonCode.FAIL, null);
        }
        return new CmsPageResult(CommonCode.SUCCESS, save);
    }

    public ResponseResult deletePage(String pageId) {
        CmsPage cmsPage = findById(pageId);
        if (cmsPage == null) {
            return new ResponseResult(CommonCode.FAIL);
        }
        cmsPageRepository.deleteById(pageId);
        return new ResponseResult(CommonCode.SUCCESS);
    }

    // 页面静态化
    public String getPageHtml(String pageId) {
        // 1 获取页面模型数据
        Map model = getModelByPageId(pageId);
        if (model == null) {
            ExceptionCast.cast(CmsCode.CMS_GENERATEHTML_DATAISNULL);
        }
        // 2 获取页面的模板
        String template = getTemplateByPageId(pageId);
        if (StringUtils.isEmpty(template)) {
            ExceptionCast.cast(CmsCode.CMS_GENERATEHTML_TEMPLATEISNULL);
        }
        // 3 生成静态化页面
        String html = generateHtml(template, model);
        if (StringUtils.isEmpty(html)) {
            ExceptionCast.cast(CmsCode.CMS_GENERATEHTML_HTMLISNULL);
        }
        return html;
    }

    // 生成静态化页面
    private String generateHtml(String template, Map model) {
        try {
            //生成配置类
            Configuration configuration = new Configuration(Configuration.getVersion());
            //模板加载器
            StringTemplateLoader stringTemplateLoader = new StringTemplateLoader();
            stringTemplateLoader.putTemplate("template", template);
            //配置模板加载器
            configuration.setTemplateLoader(stringTemplateLoader);
            //获取模板
            Template configurationTemplate = configuration.getTemplate("template");
            String html = FreeMarkerTemplateUtils.processTemplateIntoString(configurationTemplate, model);
            return html;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // 获取页面的模板
    private String getTemplateByPageId(String pageId) {
        CmsPage cmsPage = findById(pageId);
        if (cmsPage == null) {
            ExceptionCast.cast(CmsCode.CMS_PAGE_NOT_FOUND);
        }
        if (StringUtils.isBlank(cmsPage.getTemplateId())) {
            ExceptionCast.cast(CmsCode.CMS_GENERATEHTML_TEMPLATEISNULL);
        }
        Optional<CmsTemplate> templateOptional = cmsTemplateRepository.findById(cmsPage.getTemplateId());
        if (!templateOptional.isPresent()) {
            ExceptionCast.cast(CmsCode.CMS_GENERATEHTML_TEMPLATEISNULL);
        }

        CmsTemplate cmsTemplate = templateOptional.get();
        String templateFileId = cmsTemplate.getTemplateFileId();
        //根据id查询文件
        GridFSFile gridFSFile = gridFsTemplate.findOne(Query.query(Criteria.where("_id").is(templateFileId)));
        //打开下载流对象
        GridFSDownloadStream gridFSDownloadStream = gridFSBucket.openDownloadStream(gridFSFile.getObjectId());
        //创建gridFsResource，用于获取流对象
        GridFsResource gridFsResource = new GridFsResource(gridFSFile, gridFSDownloadStream);
        try {
            String template = IOUtils.toString(gridFsResource.getInputStream(), "utf-8");
            return template;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    // 获取页面模型数据
    private Map getModelByPageId(String pageId) {
        CmsPage cmsPage = findById(pageId);
        if (cmsPage == null) {
            ExceptionCast.cast(CmsCode.CMS_PAGE_NOT_FOUND);
        }
        if (StringUtils.isBlank(cmsPage.getDataUrl())) {
            ExceptionCast.cast(CmsCode.CMS_GENERATEHTML_DATAURLISNULL);
        }
        ResponseEntity<Map> forEntity = restTemplate.getForEntity(cmsPage.getDataUrl(), Map.class);
        Map body = forEntity.getBody();
        return body;
    }

    public ResponseResult postPage(String pageId) {
        // 1 执行页面静态化
        String pageHtml = getPageHtml(pageId);
        // 2 将静态化页面存储到GridFs
        CmsPage cmsPage = saveHtml(pageId, pageHtml);
        // 3 向MQ发送消息
        sendPostPage(cmsPage);
        return new ResponseResult(CommonCode.SUCCESS);
    }

    private void sendPostPage(CmsPage cmsPage) {
        Map<String, String> map = new HashMap<>();
        map.put("pageId", cmsPage.getPageId());
        String msg = JSON.toJSONString(map);
        rabbitTemplate.convertAndSend(RabbitmqConfig.EX_ROUTING_CMS_POSTPAGE, cmsPage.getSiteId(), msg);
    }


    private CmsPage saveHtml(String pageId, String pageHtml) {
        CmsPage cmsPage = findById(pageId);
        if (cmsPage == null) {
            ExceptionCast.cast(CmsCode.CMS_PAGE_NOT_FOUND);
        }
        InputStream inputStream = null;
        ObjectId objectId = null;
        try {
            inputStream = IOUtils.toInputStream(pageHtml, "utf-8");
            objectId = gridFsTemplate.store(inputStream, cmsPage.getPageName());
        } catch (IOException e) {
            ExceptionCast.cast(CmsCode.CMS_GENERATEHTML_SAVEHTMLERROR);
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // 静态化页面保存后设置文件存储id, 并更新页面信息
        cmsPage.setHtmlFileId(objectId.toHexString());
        cmsPageRepository.save(cmsPage);
        return cmsPage;
    }

    public CmsPageResult save(CmsPage cmsPage) {
        CmsPage page = cmsPageRepository.findByPageNameAndPageWebPathAndSiteId(
                cmsPage.getPageName(), cmsPage.getPageWebPath(), cmsPage.getSiteId());
        if (page != null) {
            return updatePage(page.getPageId(), cmsPage);
        }
        return addPage(cmsPage);
    }

    public CmsPostPageResult postPageQuick(CmsPage cmsPage) {
        // 添加页面
        CmsPageResult cmsPageResult = save(cmsPage);
        if (!cmsPageResult.isSuccess()) {
            ExceptionCast.cast(CommonCode.FAIL);
        }
        CmsPage page = cmsPageResult.getCmsPage();
        //发布页面
        ResponseResult responseResult = postPage(page.getPageId());
        if (!responseResult.isSuccess()) {
            ExceptionCast.cast(CommonCode.FAIL);
        }
        //得到页面的url
        //页面url=站点域名+站点webpath+页面webpath+页面名称
        //查询站点信息
        CmsSite cmsSite = findCmsSiteById(page.getSiteId());
        //站点域名
        String siteDomain = cmsSite.getSiteDomain();
        //站点web路径
        String siteWebPath = cmsSite.getSiteWebPath() == "/" ? "" : cmsSite.getSiteWebPath();
        //页面web路径
        String pageWebPath = page.getPageWebPath();
        //页面名称
        String pageName = page.getPageName();
        //页面的web访问地址
        String pageUrl = siteDomain + siteWebPath + pageWebPath + pageName;
        return new CmsPostPageResult(CommonCode.SUCCESS, pageUrl);
    }

    private CmsSite findCmsSiteById(String siteId) {
        Optional<CmsSite> cmsSiteOptional = cmsSiteRepository.findById(siteId);
        if (!cmsSiteOptional.isPresent()) {
            return null;
        }
        return cmsSiteOptional.get();
    }
}
