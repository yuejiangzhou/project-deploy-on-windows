package com.company.deploy.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.deploy.entity.UserEntity;
import com.company.deploy.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;

    public UserEntity findByUsername(String username) {
        LambdaQueryWrapper<UserEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserEntity::getUsername, username);
        return userMapper.selectOne(wrapper);
    }

    public UserEntity findById(Long id) {
        return userMapper.selectById(id);
    }

    public List<UserEntity> listAll() {
        LambdaQueryWrapper<UserEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(UserEntity::getCreatedAt);
        return userMapper.selectList(wrapper);
    }

    @Transactional
    public UserEntity createUser(String username, String encodedPassword, String displayName, String role) {
        UserEntity existing = findByUsername(username);
        if (existing != null) {
            throw new IllegalArgumentException("用户名已存在: " + username);
        }

        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setPassword(encodedPassword);
        user.setDisplayName(displayName != null ? displayName : username);
        user.setRole(role != null ? role : "DEVELOPER");
        user.setStatus("ACTIVE");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);
        return user;
    }

    @Transactional
    public UserEntity updateUser(Long id, String displayName, String role, String status) {
        UserEntity user = userMapper.selectById(id);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在: " + id);
        }

        if (displayName != null) {
            user.setDisplayName(displayName);
        }
        if (role != null) {
            user.setRole(role);
        }
        if (status != null) {
            user.setStatus(status);
        }
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        return user;
    }

    @Transactional
    public void updatePassword(Long id, String newEncodedPassword) {
        UserEntity user = userMapper.selectById(id);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在: " + id);
        }

        user.setPassword(newEncodedPassword);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    @Transactional
    public void updateLastLogin(Long id) {
        UserEntity user = userMapper.selectById(id);
        if (user != null) {
            user.setLastLogin(LocalDateTime.now());
            userMapper.updateById(user);
        }
    }
}
