package com.company.deploy.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.deploy.common.PageResult;
import com.company.deploy.entity.OperationLog;
import com.company.deploy.mapper.OperationLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogService {

    private final OperationLogMapper operationLogMapper;

    @Async
    @Transactional
    public void log(String module, String action, String targetType, Long targetId,
                    String targetName, String detail, String status, String operator, String ip) {
        try {
            OperationLog logEntry = new OperationLog();
            logEntry.setModule(module);
            logEntry.setAction(action);
            logEntry.setTargetType(targetType);
            logEntry.setTargetId(targetId);
            logEntry.setTargetName(targetName);
            logEntry.setDetail(detail);
            logEntry.setStatus(status);
            logEntry.setOperator(operator != null ? operator : "system");
            logEntry.setIp(ip);
            logEntry.setCreatedAt(LocalDateTime.now());
            operationLogMapper.insert(logEntry);
        } catch (Exception e) {
            log.warn("Failed to save operation log: {}", e.getMessage());
        }
    }

    public void logSuccess(String module, String action, String targetType, Long targetId,
                           String targetName, String detail, String operator, String ip) {
        log(module, action, targetType, targetId, targetName, detail, "SUCCESS", operator, ip);
    }

    public void logFailure(String module, String action, String targetType, Long targetId,
                           String targetName, String detail, String operator, String ip) {
        log(module, action, targetType, targetId, targetName, detail, "FAILED", operator, ip);
    }

    public PageResult<OperationLog> getLogs(String module, String action, String status,
                                            Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        if (module != null && !module.isEmpty()) {
            wrapper.eq(OperationLog::getModule, module);
        }
        if (action != null && !action.isEmpty()) {
            wrapper.eq(OperationLog::getAction, action);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(OperationLog::getStatus, status);
        }
        wrapper.orderByDesc(OperationLog::getCreatedAt);

        Page<OperationLog> page = new Page<>(pageNum, pageSize);
        Page<OperationLog> result = operationLogMapper.selectPage(page, wrapper);

        return PageResult.of(result.getRecords(), result.getTotal(), pageNum, pageSize);
    }
}
