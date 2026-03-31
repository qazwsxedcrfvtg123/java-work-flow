package com.demo.auth.aspects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * @description:
 * @Creator: 阿昇
 * @CreateTime: 2026-03-20 16:31
 */
@Aspect
@Component
public class BaseResultAspect {
    public final static Logger logger = LogManager.getLogger(BaseResultAspect.class);
    public static final String METHOD_GET = "GET";
}
