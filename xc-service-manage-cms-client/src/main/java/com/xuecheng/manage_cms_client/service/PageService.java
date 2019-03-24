package com.xuecheng.manage_cms_client.service;

import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSDownloadStream;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.xuecheng.framework.domain.cms.CmsPage;
import com.xuecheng.framework.domain.cms.response.CmsCode;
import com.xuecheng.framework.exception.ExceptionCast;
import com.xuecheng.manage_cms_client.dao.CmsPageRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.util.Optional;

@Slf4j
@Service
@Transactional
public class PageService {

    @Autowired
    private CmsPageRepository cmsPageRepository;

    @Autowired
    private GridFsTemplate gridFsTemplate;

    @Autowired
    private GridFSBucket gridFSBucket;

    /**
     * 页面静态化发布完成后, 将该页面保存到服务器物理路径
     * @param pageId
     */
    public void savePageToServerPath(String pageId) {
        CmsPage cmsPage = findCmsPageById(pageId);
        if (cmsPage == null) {
            log.error("页面不存在, 页面id为:{}", pageId);
            return;
        }
        String htmlFileId = cmsPage.getHtmlFileId();
        // 获取文件的io流
        InputStream inputStream = findFileById(htmlFileId);
        if (inputStream == null) {
            log.error("下载文件异常, 异常文件id为:{}", htmlFileId);
            return;
        }
        // 得到文件存储的物理地址
        String pagePath = cmsPage.getPagePhysicalPath() + cmsPage.getPageName();
        // 将文件内容保存到服务器物理路径
        OutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(new File(pagePath));
            IOUtils.copy(inputStream, outputStream);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                outputStream.close();
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public InputStream findFileById(String fileId) {
        //根据id查询文件
        GridFSFile gridFSFile = gridFsTemplate.findOne(Query.query(Criteria.where("_id").is(fileId)));
        //打开下载流对象
        GridFSDownloadStream gridFSDownloadStream = gridFSBucket.openDownloadStream(gridFSFile.getObjectId());
        //创建gridFsResource，用于获取流对象
        GridFsResource gridFsResource = new GridFsResource(gridFSFile, gridFSDownloadStream);
        try {
            return gridFsResource.getInputStream();
        } catch (IOException e) {
            return null;
        }
    }

    public CmsPage findCmsPageById(String pageId) {
        Optional<CmsPage> cmsPageOptional = cmsPageRepository.findById(pageId);
        if (!cmsPageOptional.isPresent()) {
            return null;
        }
        return cmsPageOptional.get();
    }
}
