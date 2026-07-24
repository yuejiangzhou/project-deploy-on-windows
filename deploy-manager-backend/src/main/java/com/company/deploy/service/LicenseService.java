package com.company.deploy.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.deploy.common.PageResult;
import com.company.deploy.config.MinioConfig;
import com.company.deploy.entity.LicenseRecord;
import com.company.deploy.mapper.LicenseRecordMapper;
import com.company.deploy.mapper.ProjectMapper;
import io.minio.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LicenseService {

    private final LicenseRecordMapper licenseRecordMapper;
    private final ProjectMapper projectMapper;
    private final LicenseGenerator licenseGenerator;
    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    @Transactional
    public LicenseRecord generate(Long projectId, String customerName, Integer trialDays, String type) {
        if (projectMapper.selectById(projectId) == null) {
            throw new RuntimeException("项目不存在");
        }

        String licenseId = UUID.randomUUID().toString().replaceAll("-", "");
        LocalDate issueDate = LocalDate.now();
        LocalDate expireDate = issueDate.plusDays(trialDays);

        try {
            // 1. Generate .lic file content
            byte[] licBytes = licenseGenerator.generateLicenseFile(type, issueDate, expireDate, customerName);

            // 2. Generate AES-encrypted timestamp
            byte[] timestampBytes = licenseGenerator.generateTimestampFile();

            // 3. Upload to MinIO
            String bucket = minioConfig.getBucketPackages();
            ensureBucketExists(bucket);

            String licObjectKey = "licenses/" + licenseId + "/license.lic";
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(licObjectKey)
                    .stream(new ByteArrayInputStream(licBytes), licBytes.length, -1)
                    .contentType("application/octet-stream")
                    .build());

            String timestampObjectKey = "licenses/" + licenseId + "/timestamp.dat";
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(timestampObjectKey)
                    .stream(new ByteArrayInputStream(timestampBytes), timestampBytes.length, -1)
                    .contentType("application/octet-stream")
                    .build());

            // 4. Parse data for DB record
            String dataJson = licenseGenerator.buildDataJson(type, issueDate, expireDate, customerName);
            String signature = licenseGenerator.signData(dataJson);

            // 5. Insert DB record
            LicenseRecord record = new LicenseRecord();
            record.setProjectId(projectId);
            record.setLicenseId(licenseId);
            record.setType(type);
            record.setCustomerName(customerName);
            record.setIssueDate(issueDate);
            record.setExpireDate(expireDate);
            record.setTrialDays(trialDays);
            record.setDataJson(dataJson);
            record.setSignature(signature);
            record.setMinioBucket(bucket);
            record.setMinioLicObjectKey(licObjectKey);
            record.setMinioTimestampObjectKey(timestampObjectKey);
            record.setStatus("ACTIVE");
            record.setCreatedAt(LocalDateTime.now());
            record.setUpdatedAt(LocalDateTime.now());
            licenseRecordMapper.insert(record);

            log.info("License generated: licenseId={}, projectId={}, type={}, expireDate={}", licenseId, projectId, type, expireDate);
            return record;

        } catch (Exception e) {
            log.error("Failed to generate license for projectId={}", projectId, e);
            throw new RuntimeException("License生成失败: " + e.getMessage(), e);
        }
    }

    @Transactional
    public LicenseRecord renew(Long licenseId, Integer trialDays) {
        LicenseRecord oldRecord = licenseRecordMapper.selectById(licenseId);
        if (oldRecord == null) {
            throw new RuntimeException("License记录不存在");
        }

        // Set old license as RENEWED
        oldRecord.setStatus("RENEWED");
        oldRecord.setStatusNote("已续期为 License #(new)");
        oldRecord.setUpdatedAt(LocalDateTime.now());
        licenseRecordMapper.updateById(oldRecord);

        // Generate new license with parentId
        String newLicenseId = UUID.randomUUID().toString().replaceAll("-", "");
        LocalDate issueDate = LocalDate.now();
        LocalDate expireDate = issueDate.plusDays(trialDays);

        try {
            byte[] licBytes = licenseGenerator.generateLicenseFile(oldRecord.getType(), issueDate, expireDate, oldRecord.getCustomerName());
            byte[] timestampBytes = licenseGenerator.generateTimestampFile();

            String bucket = minioConfig.getBucketPackages();
            String licObjectKey = "licenses/" + newLicenseId + "/license.lic";
            String timestampObjectKey = "licenses/" + newLicenseId + "/timestamp.dat";

            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(licObjectKey)
                    .stream(new ByteArrayInputStream(licBytes), licBytes.length, -1)
                    .contentType("application/octet-stream")
                    .build());
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(timestampObjectKey)
                    .stream(new ByteArrayInputStream(timestampBytes), timestampBytes.length, -1)
                    .contentType("application/octet-stream")
                    .build());

            String dataJson = licenseGenerator.buildDataJson(oldRecord.getType(), issueDate, expireDate, oldRecord.getCustomerName());
            String signature = licenseGenerator.signData(dataJson);

            LicenseRecord newRecord = new LicenseRecord();
            newRecord.setProjectId(oldRecord.getProjectId());
            newRecord.setLicenseId(newLicenseId);
            newRecord.setType(oldRecord.getType());
            newRecord.setCustomerName(oldRecord.getCustomerName());
            newRecord.setIssueDate(issueDate);
            newRecord.setExpireDate(expireDate);
            newRecord.setTrialDays(trialDays);
            newRecord.setDataJson(dataJson);
            newRecord.setSignature(signature);
            newRecord.setMinioBucket(bucket);
            newRecord.setMinioLicObjectKey(licObjectKey);
            newRecord.setMinioTimestampObjectKey(timestampObjectKey);
            newRecord.setParentId(oldRecord.getId());
            newRecord.setStatus("ACTIVE");
            newRecord.setCreatedAt(LocalDateTime.now());
            newRecord.setUpdatedAt(LocalDateTime.now());
            licenseRecordMapper.insert(newRecord);

            // Update old record's note
            oldRecord.setStatusNote("已续期为 License #" + newRecord.getId());
            oldRecord.setUpdatedAt(LocalDateTime.now());
            licenseRecordMapper.updateById(oldRecord);

            log.info("License renewed: oldId={}, newId={}, newLicenseId={}", licenseId, newRecord.getId(), newLicenseId);
            return newRecord;

        } catch (Exception e) {
            log.error("Failed to renew license id={}", licenseId, e);
            throw new RuntimeException("License续期失败: " + e.getMessage(), e);
        }
    }

    public PageResult<LicenseRecord> list(int pageNum, int pageSize, Long projectId, String status) {
        LambdaQueryWrapper<LicenseRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LicenseRecord::getDeleted, false);
        if (projectId != null) {
            wrapper.eq(LicenseRecord::getProjectId, projectId);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(LicenseRecord::getStatus, status);
        }
        wrapper.orderByDesc(LicenseRecord::getCreatedAt);

        Page<LicenseRecord> page = licenseRecordMapper.selectPage(Page.of(pageNum, pageSize), wrapper);
        return PageResult.of(page.getRecords(), page.getTotal(), pageNum, pageSize);
    }

    public LicenseRecord getById(Long id) {
        LicenseRecord record = licenseRecordMapper.selectById(id);
        if (record == null) {
            throw new RuntimeException("License记录不存在");
        }
        return record;
    }

    public InputStream download(Long id) throws Exception {
        LicenseRecord record = getById(id);
        return minioClient.getObject(GetObjectArgs.builder()
                .bucket(record.getMinioBucket())
                .object(record.getMinioLicObjectKey())
                .build());
    }

    /**
     * Download timestamp.dat from MinIO.
     */
    public InputStream downloadTimestamp(Long id) throws Exception {
        LicenseRecord record = getById(id);
        return minioClient.getObject(GetObjectArgs.builder()
                .bucket(record.getMinioBucket())
                .object(record.getMinioTimestampObjectKey())
                .build());
    }

    private void ensureBucketExists(String bucket) throws Exception {
        boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!found) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            log.info("Bucket created: {}", bucket);
        }
    }
}
