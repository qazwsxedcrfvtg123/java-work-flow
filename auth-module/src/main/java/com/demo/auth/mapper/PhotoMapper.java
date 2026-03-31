package com.demo.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demo.auth.entity.FileEntity;
import com.demo.auth.enums.PhotosType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PhotoMapper extends BaseMapper<FileEntity> {

    public java.util.List<PhotosType> selectPhotosBytypes(@Param("types") List<String> type);
}
