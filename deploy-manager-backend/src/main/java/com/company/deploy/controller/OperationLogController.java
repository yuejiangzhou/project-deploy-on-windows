package com.company.deploy.controller;

import com.company.deploy.common.PageResult;
import com.company.deploy.common.Result;
import com.company.deploy.entity.OperationLog;
import com.company.deploy.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/operation-logs")
@RequiredArgsConstructor
@Slf4j
public class OperationLogController {

    private final OperationLogService operationLogService;

    @GetMapping
    public Result<PageResult<OperationLog>> getLogs(
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(operationLogService.getLogs(module, action, status, pageNum, pageSize));
    }
}
