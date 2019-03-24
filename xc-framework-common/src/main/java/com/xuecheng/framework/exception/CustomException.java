package com.xuecheng.framework.exception;

import com.xuecheng.framework.model.response.ResultCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class CustomException extends RuntimeException {

    private ResultCode resultCode;
}
