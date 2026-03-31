package com.demo.auth.enums;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * 文件类型枚举
 */
@Getter
public enum FileType {
    AVATAR("1", "用户头像"),
    WORKFLOW("2", "工作流程图"),
    NOTIFICATION("3", "通知图片"),
    COMMON("4", "通用图片"),
    BANNER("5", "横幅广告"),
    PRODUCT("6", "产品图片");

    private final String code;
    private final String description;

    FileType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据 code 获取枚举
     */
    public static FileType fromCode(String code) {
        for (FileType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid file type code: " + code);
    }

    /**
     * 获取所有 code 列表
     */
    public static List<String> getAllCodes() {
        List<String> codes = new ArrayList<>();
        for (FileType type : values()) {
            codes.add(type.code);
        }
        return codes;
    }
}
