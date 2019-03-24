package com.xuecheng.framework.domain.learning.response;

import com.xuecheng.framework.model.response.ResultCode;
import io.swagger.annotations.ApiModelProperty;
import lombok.ToString;

@ToString
public enum  LearningCode implements ResultCode {

    LEARNING_GETMEDIA_ERROR(false, 23001, "获取学习地址失败！");

    //操作代码
    @ApiModelProperty(value = "操作是否成功", example = "true", required = true)
    boolean success;
    //操作代码
    @ApiModelProperty(value = "操作代码", example = "22001", required = true)
    int code;
    //提示信息
    @ApiModelProperty(value = "操作提示", example = "操作过于频繁！", required = true)
    String message;

    private LearningCode(boolean success, int code, String message){
        this.success = success;
        this.code = code;
        this.message = message;
    }

    @Override
    public boolean success() {
        return false;
    }

    @Override
    public int code() {
        return 0;
    }

    @Override
    public String message() {
        return null;
    }
}
