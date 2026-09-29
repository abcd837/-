package com.smartprocurement.module.file.service;

import com.smartprocurement.common.exception.BusinessException;
import com.smartprocurement.module.file.dto.UploadedFileResponse;
import com.smartprocurement.module.file.entity.ProcurementAttachment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${procurement.upload-dir:uploads}")
    private String uploadDir;

    public UploadedFileResponse store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }

        String originalName = StringUtils.cleanPath(
                Objects.requireNonNull(file.getOriginalFilename(), "文件名不能为空"));
        String extension = getExtension(originalName);
        String storedName = UUID.randomUUID().toString().replace("-", "") + extension;

        try {
            Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(root);
            Path target = root.resolve(storedName).normalize();
            if (!target.startsWith(root)) {
                throw new BusinessException("非法文件路径");
            }
            file.transferTo(target.toFile());

            return new UploadedFileResponse(
                    originalName,
                    storedName,
                    target.toString(),
                    file.getSize(),
                    file.getContentType());
        } catch (IOException ex) {
            throw new BusinessException("文件保存失败");
        }
    }

    private String getExtension(String fileName) {
        int index = fileName.lastIndexOf('.');
        return index >= 0 ? fileName.substring(index) : "";
    }

    /** 删除附件对应的物理文件（事务提交后调用，避免回滚时文件已丢失） */
    public void deletePhysicalFile(ProcurementAttachment attachment) throws IOException {
        if (attachment == null || !StringUtils.hasText(attachment.getFilePath())) {
            return;
        }
        Path path = Paths.get(attachment.getFilePath());
        Files.deleteIfExists(path);
    }

    /** 读取附件物理文件为字节数组，供下载/预览接口返回 */
    public byte[] loadFileBytes(ProcurementAttachment attachment) throws IOException {
        if (attachment == null || !StringUtils.hasText(attachment.getFilePath())) {
            throw new BusinessException("附件文件路径为空");
        }
        Path path = Paths.get(attachment.getFilePath());
        if (!Files.exists(path)) {
            throw new BusinessException("附件物理文件不存在");
        }
        return Files.readAllBytes(path);
    }
}
