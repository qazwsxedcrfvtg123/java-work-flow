package com.demo.auth.enums;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/*
*family-1,baby-2,professtional-3,wedding-4,other-5
* * */
@Getter
public enum PhotosType {
    FAMILY("1","家庭"),
    BABY("2","寶寶"),
    PROFESSIONAL("3","專業"),
    WEDDING("4","婚禮"),
    OTHER("5","其他");

    private final String code;
    private final String description;

    PhotosType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static PhotosType fromCode(String code) {
        for (PhotosType value : PhotosType.values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }

    public static List<String> getAllCodes() {
        List<String> codes = new ArrayList<>();
        for (PhotosType type : values()) {
            codes.add(type.code);
        }
        return codes;
    }
}
