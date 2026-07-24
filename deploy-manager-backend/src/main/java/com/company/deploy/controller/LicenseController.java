package com.company.deploy.controller;

import com.company.deploy.common.PageResult;
import com.company.deploy.common.Result;
import com.company.deploy.dto.LicenseGenerateRequest;
import com.company.deploy.dto.LicenseRenewRequest;
import com.company.deploy.entity.LicenseRecord;
import com.company.deploy.service.LicenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.InputStream;
import java.net.URLEncoder;

@RestController
@RequestMapping("/api/license")
@RequiredArgsConstructor
public class LicenseController {

    private final LicenseService licenseService;

    @PostMapping("/generate")
    public Result<LicenseRecord> generate(@Valid @RequestBody LicenseGenerateRequest request) {
        LicenseRecord record = licenseService.generate(
                request.getProjectId(),
                request.getCustomerName(),
                request.getTrialDays(),
                request.getType()
        );
        return Result.success(record);
    }

    @PostMapping("/{id}/renew")
    public Result<LicenseRecord> renew(@PathVariable Long id, @Valid @RequestBody LicenseRenewRequest request) {
        LicenseRecord record = licenseService.renew(id, request.getTrialDays());
        return Result.success(record);
    }

    @GetMapping("/list")
    public Result<PageResult<LicenseRecord>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) String status) {
        page = Math.max(1, page);
        size = Math.max(1, size);
        return Result.success(licenseService.list(page, size, projectId, status));
    }

    @GetMapping("/{id}")
    public Result<LicenseRecord> getById(@PathVariable Long id) {
        return Result.success(licenseService.getById(id));
    }

    @GetMapping("/{id}/download")
    public void download(@PathVariable Long id, HttpServletResponse response) throws Exception {
        LicenseRecord record = licenseService.getById(id);

        response.setContentType("application/octet-stream");
        String fileName = "license_" + record.getLicenseId() + ".lic";
        String encodedName = URLEncoder.encode(fileName, "UTF-8").replace("+", "%20");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + encodedName + "\"");

        try (InputStream is = licenseService.download(id)) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = is.read(buffer)) != -1) {
                response.getOutputStream().write(buffer, 0, len);
            }
            response.flushBuffer();
        }
    }
}
