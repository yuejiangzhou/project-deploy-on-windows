package com.company.deploy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.deploy.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {
}
