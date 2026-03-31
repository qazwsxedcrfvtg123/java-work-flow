package com.demo.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demo.auth.entity.UserEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * User Mapper Interface (MyBatis Plus)
 * Works alongside JPA Repository
 */
@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {

    /**
     * Custom query example - Find users by role with custom SQL
     * This demonstrates using MyBatis for complex queries while JPA handles simple CRUD
     */
    @Select("SELECT * FROM auth_user WHERE role = #{role} AND enabled = #{enabled}")
    List<UserEntity> selectByRoleAndEnabled(@Param("role") String role, @Param("enabled") boolean enabled);

    /**
     * Custom query with dynamic SQL
     */
    List<UserEntity> selectUsersWithFilters(
        @Param("username") String username,
        @Param("email") String email,
        @Param("role") String role
    );

    /**
     * Example of a complex aggregation query
     */
    @Select("SELECT COUNT(*) FROM auth_user WHERE created_at BETWEEN #{startTime} AND #{endTime}")
    long countUsersByTimeRange(@Param("startTime") Long startTime, @Param("endTime") Long endTime);
}
