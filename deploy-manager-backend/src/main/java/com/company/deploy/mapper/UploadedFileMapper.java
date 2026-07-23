package com.company.deploy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.deploy.entity.UploadedFile;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UploadedFileMapper extends BaseMapper<UploadedFile> {
}
