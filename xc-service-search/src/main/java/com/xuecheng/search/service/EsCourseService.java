package com.xuecheng.search.service;

import com.xuecheng.framework.domain.course.CoursePub;
import com.xuecheng.framework.domain.course.TeachplanMediaPub;
import com.xuecheng.framework.domain.search.CourseSearchParam;
import com.xuecheng.framework.exception.ExceptionCast;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.QueryResponseResult;
import com.xuecheng.framework.model.response.QueryResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Transactional
public class EsCourseService {

    @Value("${xuecheng.course.index}")
    private String esIndex;

    @Value("${xuecheng.course.type}")
    private String esType;

    @Value("${xuecheng.course.source_field}")
    private String sourceField;

    @Value("${xuecheng.media.index}")
    private String mediaIndex;

    @Value("${xuecheng.media.type}")
    private String mediaType;

    @Value("${xuecheng.media.source_field}")
    private String mediaSourceField;

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    public QueryResponseResult<CoursePub> list(int page, int size, CourseSearchParam courseSearchParam) {
        //设置索引
        SearchRequest searchRequest = new SearchRequest(esIndex);
        //设置类型
        searchRequest.types(esType);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        //source源字段过虑
        String[] sourceFields = sourceField.split(",");
        searchSourceBuilder.fetchSource(sourceFields, new String[]{});
        //关键字
        String keyword = courseSearchParam.getKeyword();
        if (StringUtils.isNotBlank(keyword)) {
            //匹配关键字
            MultiMatchQueryBuilder multiMatchQueryBuilder = QueryBuilders
                    .multiMatchQuery(keyword, "name", "teachplan", "description");
            //设置匹配占比
            multiMatchQueryBuilder.minimumShouldMatch("70%");
            //提升另个字段的Boost值
            multiMatchQueryBuilder.field("name", 10);
            boolQueryBuilder.must(multiMatchQueryBuilder);
        }

        //过滤
        //一级分类
        String mt = courseSearchParam.getMt();
        if (StringUtils.isNotBlank(mt)) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("mt", mt));
        }
        //二级分类
        String st = courseSearchParam.getSt();
        if (StringUtils.isNotBlank(st)) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("st", st));
        }
        //难度等级
        String grade = courseSearchParam.getGrade();
        if (StringUtils.isNotBlank(grade)) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("grade", grade));
        }

        //分页
        page = page <= 0 ? 1 : page;
        size = size <= 0 ? 20 : size;
        int start = (page - 1) * size;
        searchSourceBuilder.from(start);
        searchSourceBuilder.size(size);
        //布尔查询
        searchSourceBuilder.query(boolQueryBuilder);

        //高亮设置
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.preTags("<font class='eslight'>");
        highlightBuilder.postTags("</font>");
        //设置高亮字段
        highlightBuilder.fields().add(new HighlightBuilder.Field("name"));
        searchSourceBuilder.highlighter(highlightBuilder);

        //请求搜索
        searchRequest.source(searchSourceBuilder);
        SearchResponse searchResponse = null;
        try {
            searchResponse = restHighLevelClient.search(searchRequest);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("xuecheng search error..{}", e.getMessage());
            ExceptionCast.cast(CommonCode.FAIL);
        }
        //结果集处理
        SearchHits hits = searchResponse.getHits();
        SearchHit[] searchHits = hits.getHits();
        //记录总数
        long totalHits = hits.getTotalHits();
        //分页结果
        QueryResult<CoursePub> queryResult = new QueryResult<>();
        queryResult.setTotal(totalHits);
        //分页数据列表
        List<CoursePub> coursePubList = new ArrayList<>();
        for (SearchHit searchHit : searchHits) {
            CoursePub coursePub = new CoursePub();
            //取出source
            Map<String, Object> sourceAsMap = searchHit.getSourceAsMap();
            //取出id
            String id = (String) sourceAsMap.get("id");
            //取出名称
            String name = (String) sourceAsMap.get("name");

            //取出高亮字段内容
            Map<String, HighlightField> highlightFields = searchHit.getHighlightFields();
            if (highlightFields != null) {
                HighlightField nameField = highlightFields.get("name");
                if (nameField != null) {
                    Text[] fragments = nameField.getFragments();
                    StringBuffer stringBuffer = new StringBuffer();
                    for (Text fragment : fragments) {
                        stringBuffer.append(fragment.toString());
                    }
                    name = stringBuffer.toString();
                }
            }

            //图片
            String pic = (String) sourceAsMap.get("pic");
            //收费规则，对应数据字典
            String charge = (String) sourceAsMap.get("charge");
            //价格
            Float price = null;
            if (sourceAsMap.get("price") != null) {
                price = Float.parseFloat(sourceAsMap.get("price").toString());
            }
            Float price_old = null;
            if (sourceAsMap.get("price_old") != null) {
                price_old = Float.parseFloat(sourceAsMap.get("price_old").toString());
            }
            coursePub.setId(id);
            coursePub.setName(name);
            coursePub.setPic(pic);
            coursePub.setCharge(charge);
            coursePub.setPrice(price);
            coursePub.setPrice_old(price_old);
            coursePubList.add(coursePub);
        }
        queryResult.setList(coursePubList);
        return new QueryResponseResult<>(CommonCode.SUCCESS, queryResult);
    }

    public Map<String, CoursePub> getall(String id) {
        //设置索引
        SearchRequest searchRequest = new SearchRequest(esIndex);
        //设置类型
        searchRequest.types(esType);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //查询条件，根据课程id查询
        searchSourceBuilder.query(QueryBuilders.termsQuery("id", id));
        //请求搜索
        searchRequest.source(searchSourceBuilder);
        SearchResponse searchResponse = null;
        try {
            //执行搜索
            searchResponse = restHighLevelClient.search(searchRequest);
        } catch (Exception e) {
            log.error("xuecheng search error..{}", e.getMessage());
            ExceptionCast.cast(CommonCode.FAIL);
        }
        //获取搜索结果
        SearchHits hits = searchResponse.getHits();
        SearchHit[] searchHits = hits.getHits();
        Map<String, CoursePub> map = new HashMap<>();
        for (SearchHit searchHit : searchHits) {
            CoursePub coursePub = new CoursePub();
            //取出source
            Map<String, Object> sourceAsMap = searchHit.getSourceAsMap();
            String courseId = (String) sourceAsMap.get("id");
            String name = (String) sourceAsMap.get("name");
            String grade = (String) sourceAsMap.get("grade");
            String charge = (String) sourceAsMap.get("charge");
            String pic = (String) sourceAsMap.get("pic");
            String description = (String) sourceAsMap.get("description");
            String teachplan = (String) sourceAsMap.get("teachplan");
            coursePub.setId(courseId);
            coursePub.setName(name);
            coursePub.setGrade(grade);
            coursePub.setCharge(charge);
            coursePub.setPic(pic);
            coursePub.setDescription(description);
            coursePub.setTeachplan(teachplan);
            map.put(courseId, coursePub);
        }
        return map;
    }

    //根据课程计划查询媒资信息
    public List<TeachplanMediaPub> getmedia(String[] teachplanIds) {
        //设置索引
        SearchRequest searchRequest = new SearchRequest(mediaIndex);
        //设置类型
        searchRequest.types(mediaType);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //source源字段过虑
        String[] sourceFields = mediaSourceField.split(",");
        searchSourceBuilder.fetchSource(sourceFields, new String[]{});
        //查询条件，根据课程计划id查询(可传入多个id)
        searchSourceBuilder.query(QueryBuilders.termsQuery("teachplan_id", teachplanIds));
        //请求搜索
        searchRequest.source(searchSourceBuilder);
        SearchResponse searchResponse = null;
        try {
            //执行搜索
            searchResponse = restHighLevelClient.search(searchRequest);
        } catch (Exception e) {
            log.error("xuecheng search error..{}", e.getMessage());
            ExceptionCast.cast(CommonCode.FAIL);
        }
        //获取搜索结果
        SearchHits hits = searchResponse.getHits();
        SearchHit[] searchHits = hits.getHits();
        //数据列表
        List<TeachplanMediaPub> teachplanMediaPubList = new ArrayList<>();
        for (SearchHit searchHit : searchHits) {
            TeachplanMediaPub teachplanMediaPub = new TeachplanMediaPub();
            Map<String, Object> sourceAsMap = searchHit.getSourceAsMap();
            //取出课程计划媒资信息
            String courseid = (String) sourceAsMap.get("courseid");
            String media_id = (String) sourceAsMap.get("media_id");
            String media_url = (String) sourceAsMap.get("media_url");
            String teachplan_id = (String) sourceAsMap.get("teachplan_id");
            String media_fileoriginalname = (String) sourceAsMap.get("media_fileoriginalname");
            teachplanMediaPub.setCourseId(courseid);
            teachplanMediaPub.setMediaId(media_id);
            teachplanMediaPub.setMediaUrl(media_url);
            teachplanMediaPub.setTeachplanId(teachplan_id);
            teachplanMediaPub.setMediaFileOriginalName(media_fileoriginalname);
            teachplanMediaPubList.add(teachplanMediaPub);
        }
        return teachplanMediaPubList;
    }
}
