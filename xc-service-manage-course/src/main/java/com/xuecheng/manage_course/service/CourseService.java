package com.xuecheng.manage_course.service;

import com.alibaba.fastjson.JSON;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.xuecheng.framework.domain.cms.CmsPage;
import com.xuecheng.framework.domain.cms.response.CmsPageResult;
import com.xuecheng.framework.domain.cms.response.CmsPostPageResult;
import com.xuecheng.framework.domain.course.*;
import com.xuecheng.framework.domain.course.ext.CourseInfo;
import com.xuecheng.framework.domain.course.ext.CourseView;
import com.xuecheng.framework.domain.course.ext.TeachplanNode;
import com.xuecheng.framework.domain.course.request.CourseListRequest;
import com.xuecheng.framework.domain.course.response.AddCourseResult;
import com.xuecheng.framework.domain.course.response.CourseCode;
import com.xuecheng.framework.domain.course.response.CoursePublishResult;
import com.xuecheng.framework.exception.ExceptionCast;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.QueryResponseResult;
import com.xuecheng.framework.model.response.QueryResult;
import com.xuecheng.framework.model.response.ResponseResult;
import com.xuecheng.manage_course.client.CmsPageClient;
import com.xuecheng.manage_course.config.CoursePublishProperties;
import com.xuecheng.manage_course.dao.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@EnableConfigurationProperties(CoursePublishProperties.class)
@Service
@Transactional
public class CourseService {

    @Autowired
    private TeachplanMapper teachplanMapper;

    @Autowired
    private CourseBaseRepository courseBaseRepository;

    @Autowired
    private TeachplanRepository teachplanRepository;

    @Autowired
    private CourseMapper courseMapper;

    @Autowired
    private CourseMarketRepository courseMarketRepository;

    @Autowired
    private CoursePicRepository coursePicRepository;

    @Autowired
    private CmsPageClient cmsPageClient;

    @Autowired
    private CoursePublishProperties coursePublishProperties;

    @Autowired
    private CoursePubRepository coursePubRepository;

    @Autowired
    private TeachplanMediaRepository teachplanMediaRepository;

    @Autowired
    private TeachplanMediaPubRepository teachplanMediaPubRepository;

    public TeachplanNode findTeachplanList(String courseId) {
        return teachplanMapper.selectList(courseId);
    }

    public ResponseResult addTeachplan(Teachplan teachplan) {
        String parentid = teachplan.getParentid();
        //如果父结点为空则获取根结点
        if (StringUtils.isBlank(parentid)) {
            String courseid = teachplan.getCourseid();
            parentid = getTeachplanRoot(courseid);
        }
        Optional<Teachplan> teachplanOptional = teachplanRepository.findById(parentid);
        if (!teachplanOptional.isPresent()) {
            ExceptionCast.cast(CommonCode.INVALID_PARAM);
        }
        //取出父结点信息
        Teachplan teachplanParent = teachplanOptional.get();
        //父结点级别
        String grade = teachplanParent.getGrade();
        //设置父结点
        teachplan.setParentid(parentid);
        //子结点的级别，根据父结点来判断
        if ("1".equals(grade)) {
            teachplan.setGrade("2");
        } else if ("2".equals(grade)) {
            teachplan.setGrade("3");
        }
        //设置课程id
        teachplan.setCourseid(teachplanParent.getCourseid());
        teachplanRepository.save(teachplan);
        return new ResponseResult(CommonCode.SUCCESS);
    }

    //获取课程根结点, 如果没有则添加根结点
    public String getTeachplanRoot(String courseId) {
        //校验课程id
        Optional<CourseBase> courseBaseOptional = courseBaseRepository.findById(courseId);
        if (!courseBaseOptional.isPresent()) {
            ExceptionCast.cast(CourseCode.COURSE_NOT_FOUND);
        }
        CourseBase courseBase = courseBaseOptional.get();
        //取出课程计划根结点
        List<Teachplan> teachplanList = teachplanRepository.findByParentidAndCourseid("0", courseId);
        if (CollectionUtils.isEmpty(teachplanList)) {
            //新增一个根结点
            Teachplan teachplan = new Teachplan();
            teachplan.setPname(courseBase.getName());
            teachplan.setParentid("0");
            teachplan.setGrade("1");
            teachplan.setCourseid(courseId);
            teachplan.setStatus("0");
            Teachplan save = teachplanRepository.save(teachplan);
            return save.getId();
        }
        Teachplan teachplan = teachplanList.get(0);
        return teachplan.getId();
    }

    public QueryResponseResult findCourseList(int page, int size, CourseListRequest courseListRequest, String companyId) {
        courseListRequest = courseListRequest == null ? new CourseListRequest() : courseListRequest;
        //企业id,将companyId传给dao
        courseListRequest.setCompanyId(companyId);
        page = page <= 0 ? 1 : page;
        size = size <= 0 ? 20 : size;
        //设置分页参数
        PageHelper.startPage(page, size);
        //分页查询
        Page<CourseInfo> courseInfoPage = courseMapper.findCourseListPage(courseListRequest);
        List<CourseInfo> courseInfoList = courseInfoPage.getResult();
        long total = courseInfoPage.getTotal();
        //查询结果集
        QueryResult<CourseInfo> queryResult = new QueryResult<>();
        queryResult.setList(courseInfoList);
        queryResult.setTotal(total);
        return new QueryResponseResult(CommonCode.SUCCESS, queryResult);
    }

    public AddCourseResult addCourseBase(CourseBase courseBase) {
        // TODO 需要根据moongodb的数据字典查, 暂时设死
        courseBase.setStatus("202001");
        CourseBase save = courseBaseRepository.save(courseBase);
        return new AddCourseResult(CommonCode.SUCCESS, save.getId());
    }

    public CourseBase getCourseBaseById(String courseId) {
        Optional<CourseBase> courseBaseOptional = courseBaseRepository.findById(courseId);
        if (!courseBaseOptional.isPresent()) {
            ExceptionCast.cast(CourseCode.COURSE_NOT_FOUND);
        }
        return courseBaseOptional.get();
    }

    public ResponseResult updateCourseBase(String id, CourseBase courseBase) {
        CourseBase one = getCourseBaseById(id);
        one.setName(courseBase.getName());
        one.setUsers(courseBase.getUsers());
        one.setMt(courseBase.getMt());
        one.setSt(courseBase.getSt());
        one.setGrade(courseBase.getGrade());
        one.setStudymodel(courseBase.getStudymodel());
        one.setDescription(courseBase.getDescription());
        courseBaseRepository.save(one);
        return new ResponseResult(CommonCode.SUCCESS);
    }

    public CourseMarket getCourseMarketById(String courseId) {
        Optional<CourseMarket> courseMarketOptional = courseMarketRepository.findById(courseId);
        if (!courseMarketOptional.isPresent()) {
            return null;
        }
        return courseMarketOptional.get();
    }

    public ResponseResult updateCourseMarket(String id, CourseMarket courseMarket) {
        CourseMarket one = getCourseMarketById(id);
        if (one == null) {
            courseMarket.setId(id);
            courseMarketRepository.save(courseMarket);
            return new ResponseResult(CommonCode.SUCCESS);
        }
        one.setCharge(courseMarket.getCharge());
        one.setValid(courseMarket.getValid());
        one.setQq(courseMarket.getQq());
        one.setPrice(courseMarket.getPrice());
        one.setStartTime(courseMarket.getStartTime());
        one.setEndTime(courseMarket.getEndTime());
        courseMarketRepository.save(one);
        return new ResponseResult(CommonCode.SUCCESS);
    }

    public ResponseResult addCoursePic(String courseId, String pic) {
        CoursePic coursePic = new CoursePic();
        coursePic.setCourseid(courseId);
        coursePic.setPic(pic);
        CoursePic save = coursePicRepository.save(coursePic);
        if (save == null) {
            ExceptionCast.cast(CommonCode.FAIL);
        }
        return new ResponseResult(CommonCode.SUCCESS);
    }

    public CoursePic findCoursePic(String courseId) {
        Optional<CoursePic> coursePicOptional = coursePicRepository.findById(courseId);
        if (!coursePicOptional.isPresent()) {
            ExceptionCast.cast(CourseCode.COURSE_PIC_NOT_FOUND);
        }
        return coursePicOptional.get();
    }

    public ResponseResult deleteCoursePic(String courseId) {
        long count = coursePicRepository.deleteByCourseid(courseId);
        if (count != 1) {
            ExceptionCast.cast(CommonCode.FAIL);
        }
        return new ResponseResult(CommonCode.SUCCESS);
    }

    public CourseView courseview(String id) {
        CourseView courseView = new CourseView();
        //查询课程基本信息
        Optional<CourseBase> courseBaseOptional = courseBaseRepository.findById(id);
        if (!courseBaseOptional.isPresent()) {
            ExceptionCast.cast(CourseCode.COURSE_NOT_FOUND);
        }
        courseView.setCourseBase(courseBaseOptional.get());
        //查询课程营销信息
        Optional<CourseMarket> courseMarketOptional = courseMarketRepository.findById(id);
        if (!courseMarketOptional.isPresent()) {
            ExceptionCast.cast(CourseCode.COURSE_NOT_FOUND);
        }
        courseView.setCourseMarket(courseMarketOptional.get());
        //查询课程图片信息
        Optional<CoursePic> coursePicOptional = coursePicRepository.findById(id);
        if (!coursePicOptional.isPresent()) {
            ExceptionCast.cast(CourseCode.COURSE_PIC_NOT_FOUND);
        }
        courseView.setCoursePic(coursePicOptional.get());
        //查询课程计划信息
        TeachplanNode teachplanNode = teachplanMapper.selectList(id);
        courseView.setTeachplanNode(teachplanNode);
        return courseView;
    }

    public CoursePublishResult preview(String id) {
        CourseBase courseBase = getCourseBaseById(id);

        CmsPage cmsPage = new CmsPage();
        cmsPage.setSiteId(coursePublishProperties.getSiteId());
        cmsPage.setPageName(id + ".html");
        cmsPage.setPageAliase(courseBase.getName());
        cmsPage.setPageWebPath(coursePublishProperties.getPageWebPath());
        cmsPage.setPagePhysicalPath(coursePublishProperties.getPagePhysicalPath());
        cmsPage.setTemplateId(coursePublishProperties.getTemplateId());
        cmsPage.setDataUrl(coursePublishProperties.getDataUrl() + id);
        //远程请求cms保存页面信息
        CmsPageResult cmsPageResult = cmsPageClient.save(cmsPage);
        if (!cmsPageResult.isSuccess()) {
            ExceptionCast.cast(CommonCode.FAIL);
        }
        //页面id
        String pageId = cmsPageResult.getCmsPage().getPageId();
        //页面预览url
        String previewUrl = coursePublishProperties.getPreviewUrl() + pageId;
        return new CoursePublishResult(CommonCode.SUCCESS, previewUrl);
    }

    public CoursePublishResult publish(String id) {
        CourseBase courseBase = getCourseBaseById(id);
        // 准备页面信息
        CmsPage cmsPage = new CmsPage();
        cmsPage.setSiteId(coursePublishProperties.getSiteId());
        cmsPage.setPageName(id + ".html");
        cmsPage.setPageAliase(courseBase.getName());
        cmsPage.setPageWebPath(coursePublishProperties.getPageWebPath());
        cmsPage.setPagePhysicalPath(coursePublishProperties.getPagePhysicalPath());
        cmsPage.setTemplateId(coursePublishProperties.getTemplateId());
        cmsPage.setDataUrl(coursePublishProperties.getDataUrl() + id);
        //发布页面
        CmsPostPageResult cmsPostPageResult = cmsPageClient.postPageQuick(cmsPage);
        if (!cmsPostPageResult.isSuccess()) {
            ExceptionCast.cast(CommonCode.FAIL);
        }
        //更新课程发布状态
        CourseBase course = saveCoursePubState(id);

        //课程索引...
        //创建课程索引信息
        CoursePub coursePub = createCoursePub(id);
        //向数据库保存课程索引信息
        saveCoursePub(coursePub);
        //保存课程计划媒资信息到待索引表
        saveTeachplanMediaPub(id);

        //课程缓存... TODO

        //页面url
        String pageUrl = cmsPostPageResult.getPageUrl();
        return new CoursePublishResult(CommonCode.SUCCESS, pageUrl);
    }

    //保存课程计划媒资信息
    private void saveTeachplanMediaPub(String courseId) {
        //查询课程媒资信息
        List<TeachplanMedia> teachplanMediaList = teachplanMediaRepository.findByCourseId(courseId);
        if (CollectionUtils.isEmpty(teachplanMediaList)) {
            return;
        }
        //将课程计划媒资信息存储待索引表
        teachplanMediaPubRepository.deleteByCourseId(courseId);
        List<TeachplanMediaPub> teachplanMediaPubList = new ArrayList<>();
        for (TeachplanMedia teachplanMedia : teachplanMediaList) {
            TeachplanMediaPub teachplanMediaPub = new TeachplanMediaPub();
            BeanUtils.copyProperties(teachplanMedia, teachplanMediaPub);
            teachplanMediaPub.setTimestamp(new Date());
            teachplanMediaPubList.add(teachplanMediaPub);
        }
        teachplanMediaPubRepository.saveAll(teachplanMediaPubList);
    }

    private CoursePub saveCoursePub(CoursePub coursePub) {
        CoursePub save = coursePubRepository.save(coursePub);
        if (save == null) {
            ExceptionCast.cast(CourseCode.COURSE_PUBLISH_VIEWERROR);
        }
        return save;
    }

    //创建coursePub对象
    private CoursePub createCoursePub(String id) {
        CoursePub coursePub = new CoursePub();
        coursePub.setId(id);

        //基础信息
        Optional<CourseBase> courseBaseOptional = courseBaseRepository.findById(id);
        if (courseBaseOptional.isPresent()) {
            CourseBase courseBase = courseBaseOptional.get();
            BeanUtils.copyProperties(courseBase, coursePub);
        }
        //查询课程图片
        Optional<CoursePic> coursePicOptional = coursePicRepository.findById(id);
        if (coursePicOptional.isPresent()) {
            CoursePic coursePic = coursePicOptional.get();
            BeanUtils.copyProperties(coursePic, coursePub);
        }
        //课程营销信息
        Optional<CourseMarket> courseMarketOptional = courseMarketRepository.findById(id);
        if (courseMarketOptional.isPresent()) {
            CourseMarket courseMarket = courseMarketOptional.get();
            BeanUtils.copyProperties(courseMarket, coursePub);
        }
        //课程计划
        TeachplanNode teachplanNode = teachplanMapper.selectList(id);
        if (teachplanNode != null) {
            //将课程计划转成json
            String teachplanString = JSON.toJSONString(teachplanNode);
            coursePub.setTeachplan(teachplanString);
        }

        Date date = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String pubTime = dateFormat.format(date);
        //更新时间戳为最新时间
        coursePub.setTimestamp(date);
        //发布时间
        coursePub.setPubTime(pubTime);
        return coursePub;
    }

    //更新课程发布状态
    private CourseBase saveCoursePubState(String courseId) {
        CourseBase courseBase = getCourseBaseById(courseId);
        courseBase.setStatus("202002");
        CourseBase save = courseBaseRepository.save(courseBase);
        if (save == null) {
            ExceptionCast.cast(CommonCode.FAIL);
        }
        return save;
    }

    //保存媒资信息
    public ResponseResult savemedia(TeachplanMedia teachplanMedia) {
        if (teachplanMedia == null || StringUtils.isBlank(teachplanMedia.getTeachplanId())) {
            ExceptionCast.cast(CommonCode.INVALID_PARAM);
        }
        //查询课程计划
        Optional<Teachplan> teachplanOptional = teachplanRepository.findById(teachplanMedia.getTeachplanId());
        if (!teachplanOptional.isPresent()) {
            ExceptionCast.cast(CourseCode.COURSE_MEDIA_TEACHPLAN_ISNULL);
        }
        Teachplan teachplan = teachplanOptional.get();
        //只允许为叶子结点课程计划选择视频
        String grade = teachplan.getGrade();
        if (!StringUtils.equals(grade, "3")) {
            ExceptionCast.cast(CourseCode.COURSE_MEDIA_TEACHPLAN_GRADEERROR);
        }
        //保存媒资信息与课程计划信息
        TeachplanMedia save = teachplanMediaRepository.save(teachplanMedia);
        if (save == null) {
            ExceptionCast.cast(CommonCode.FAIL);
        }
        return new ResponseResult(CommonCode.SUCCESS);
    }
}
