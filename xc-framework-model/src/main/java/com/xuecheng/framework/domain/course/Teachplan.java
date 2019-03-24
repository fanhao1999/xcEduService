package com.xuecheng.framework.domain.course;

import lombok.Data;
import lombok.ToString;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import java.io.Serializable;

/**
 * Created by admin on 2018/2/7.
 */
@Data
@ToString
@Entity
@Table(name="teachplan")
@GenericGenerator(name = "jpa-uuid", strategy = "uuid")
public class Teachplan implements Serializable {
    private static final long serialVersionUID = -916357110051689485L;
    @Id
    @GeneratedValue(generator = "jpa-uuid")
    @Column(length = 32)
    private String id;
    @NotEmpty(message = "课程计划名称不能为空")
    private String pname;
    private String parentid;
    private String grade;
    private String ptype;
    private String description;
    @NotEmpty(message = "课程id不能为空")
    private String courseid;
    @NotEmpty(message = "发布状态不能为空")
    private String status;
    private Integer orderby;
    private Double timelength;
    private String trylearn;

}
