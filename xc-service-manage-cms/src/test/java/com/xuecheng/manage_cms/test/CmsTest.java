package com.xuecheng.manage_cms.test;

import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSDownloadStream;
import com.mongodb.client.gridfs.model.GridFSFile;
import org.apache.commons.io.IOUtils;
import org.bson.types.ObjectId;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.util.Map;

@SpringBootTest
@RunWith(SpringRunner.class)
public class CmsTest {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private GridFsTemplate gridFsTemplate;

    @Autowired
    private GridFSBucket gridFSBucket;

    @Test
    public void restTemplate() {
        ResponseEntity<Map> forEntity = restTemplate
                .getForEntity("http://localhost:31001/cms/config/getmodel/5a791725dd573c3574ee333f", Map.class);
        System.out.println(forEntity);
    }

    @Test
    public void gridFsTemplate() throws FileNotFoundException {
        File file = new File(
                "H:/java黑马/Java实战《学成在线》微服务项目/day09 课程预览 Eureka Feign/资料/课程详情页面模板/course.ftl");
        InputStream inputStream = new FileInputStream(file);
        ObjectId objectId = gridFsTemplate.store(inputStream, "course.ftl");
        System.out.println(objectId);
    }

    @Test
    public void gridFSBucket() throws IOException {
        String fileId = "5c828f9352d9243a9011bf27";
        //根据id查询文件
        GridFSFile gridFSFile = gridFsTemplate.findOne(Query.query(Criteria.where("_id").is(fileId)));
        //打开下载流对象
        GridFSDownloadStream gridFSDownloadStream = gridFSBucket.openDownloadStream(gridFSFile.getObjectId());
        //创建gridFsResource，用于获取流对象
        GridFsResource gridFsResource = new GridFsResource(gridFSFile, gridFSDownloadStream);
        String s = IOUtils.toString(gridFsResource.getInputStream(), "utf-8");
        System.out.println(s);
    }
}
