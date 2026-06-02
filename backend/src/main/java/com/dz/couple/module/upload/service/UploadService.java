package com.dz.couple.module.upload.service;

import com.dz.couple.common.BusinessException;
import com.dz.couple.common.ErrorCode;
import com.dz.couple.config.AppProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

@Service
public class UploadService {
    private static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024;
    private static final long MAX_AUDIO_BYTES = 5L * 1024 * 1024;
    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyyMMdd");
    public static final String BIZ_PHOTO = "photo";
    public static final String BIZ_CHAT = "chat";
    private static final int MAX_IMAGE_PIXELS = 50_000_000;

    private final AppProperties appProperties;

    @Autowired
    public UploadService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public String saveImage(MultipartFile file) {
        return saveImage(file, null, null, null);
    }

    public String saveImage(MultipartFile file, Long coupleId, String biz, Long albumId) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择图片");
        }
        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "单张图片大小需≤10MB");
        }
        String ct = file.getContentType();
        if (ct == null || !ct.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅支持图片格式");
        }
        DetectedImage detected = detectAndValidateImage(file, ct);

        String dir = appProperties.getUploadDir();
        if (dir == null || dir.trim().isEmpty()) {
            dir = "uploads";
        }
        String day = LocalDate.now().format(DF);
        String ext = detected.ext;
        String name = UUID.randomUUID().toString().replace("-", "") + ext;
        String b = biz == null ? null : biz.trim().toLowerCase(Locale.ROOT);
        Path folder = Paths.get(dir).toAbsolutePath().normalize();
        String urlPrefix;
        if (BIZ_PHOTO.equals(b)) {
            if (coupleId == null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "coupleId不能为空");
            }
            String albumKey = albumId == null ? "uncategorized" : String.valueOf(albumId);
            folder = folder.resolve("photos").resolve(String.valueOf(coupleId)).resolve(safeSegment(albumKey)).resolve(day);
            urlPrefix = "/uploads/photos/" + coupleId + "/" + safeSegment(albumKey) + "/" + day + "/";
        } else if (BIZ_CHAT.equals(b)) {
            if (coupleId == null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "coupleId不能为空");
            }
            folder = folder.resolve("chat").resolve(String.valueOf(coupleId)).resolve(day);
            urlPrefix = "/uploads/chat/" + coupleId + "/" + day + "/";
        } else {
            folder = folder.resolve(day);
            urlPrefix = "/uploads/" + day + "/";
        }
        try {
            Files.createDirectories(folder);
            Path target = folder.resolve(name);
            file.transferTo(target.toFile());
            return urlPrefix + name;
        } catch (Exception e) {
            String msg = e.getClass().getSimpleName();
            if (e.getMessage() != null && !e.getMessage().trim().isEmpty()) {
                msg = msg + ": " + e.getMessage().trim();
            }
            if (msg.length() > 200) {
                msg = msg.substring(0, 200);
            }
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, msg);
        }
    }

    public String saveAudio(MultipartFile file, Long coupleId, String biz) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择音频");
        }
        if (file.getSize() > MAX_AUDIO_BYTES) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "单个音频大小需≤5MB");
        }
        String ct = file.getContentType();
        if (ct == null || !ct.toLowerCase(Locale.ROOT).startsWith("audio/")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅支持音频格式");
        }
        String ext = extractExt(file.getOriginalFilename());
        if (ext != null && !isAllowedAudioExt(ext)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅支持 mp3/m4a/wav/ogg 音频");
        }
        String dir = appProperties.getUploadDir();
        if (dir == null || dir.trim().isEmpty()) {
            dir = "uploads";
        }
        String day = LocalDate.now().format(DF);
        String name = UUID.randomUUID().toString().replace("-", "");
        String outExt = ext != null ? "." + ext : guessAudioExt(ct);
        String b = biz == null ? null : biz.trim().toLowerCase(Locale.ROOT);
        Path folder = Paths.get(dir).toAbsolutePath().normalize();
        String urlPrefix;
        if (BIZ_CHAT.equals(b)) {
            if (coupleId == null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "coupleId不能为空");
            }
            folder = folder.resolve("chat-audio").resolve(String.valueOf(coupleId)).resolve(day);
            urlPrefix = "/uploads/chat-audio/" + coupleId + "/" + day + "/";
        } else {
            folder = folder.resolve("audios").resolve(day);
            urlPrefix = "/uploads/audios/" + day + "/";
        }
        try {
            Files.createDirectories(folder);
            Path target = folder.resolve(name + outExt);
            file.transferTo(target.toFile());
            return urlPrefix + target.getFileName().toString();
        } catch (Exception e) {
            String msg = e.getClass().getSimpleName();
            if (e.getMessage() != null && !e.getMessage().trim().isEmpty()) {
                msg = msg + ": " + e.getMessage().trim();
            }
            if (msg.length() > 200) {
                msg = msg.substring(0, 200);
            }
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, msg);
        }
    }

    private DetectedImage detectAndValidateImage(MultipartFile file, String contentType) {
        String name = file.getOriginalFilename();
        String ext = extractExt(name);
        if (ext != null && !isAllowedExt(ext)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅支持 jpg/jpeg/png/gif/webp 图片");
        }

        ImageKind kind;
        byte[] head = new byte[32];
        int n = 0;
        try (InputStream in = file.getInputStream()) {
            n = in.read(head);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "读取文件失败");
        }
        if (n <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文件为空");
        }
        kind = detectKind(head, n);
        if (kind == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文件内容不是有效图片");
        }

        String ct = contentType == null ? "" : contentType.trim().toLowerCase(Locale.ROOT);
        if (ct.equals("image/svg+xml")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持SVG图片");
        }
        if (ct.equals("image/png") && kind != ImageKind.PNG) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文件类型与内容不匹配");
        }
        if ((ct.equals("image/jpeg") || ct.equals("image/jpg")) && kind != ImageKind.JPEG) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文件类型与内容不匹配");
        }
        if (ct.equals("image/gif") && kind != ImageKind.GIF) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文件类型与内容不匹配");
        }
        if (ct.equals("image/webp") && kind != ImageKind.WEBP) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文件类型与内容不匹配");
        }

        if (kind != ImageKind.WEBP) {
            try (InputStream in = file.getInputStream()) {
                BufferedImage img = ImageIO.read(in);
                if (img == null) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "图片解析失败");
                }
                int w = img.getWidth();
                int h = img.getHeight();
                if (w <= 0 || h <= 0) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "图片尺寸非法");
                }
                long pixels = (long) w * (long) h;
                if (pixels > MAX_IMAGE_PIXELS) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "图片分辨率过大");
                }
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "图片解析失败");
            }
        }

        DetectedImage out = new DetectedImage();
        out.kind = kind;
        out.ext = kind.ext;
        return out;
    }

    private ImageKind detectKind(byte[] head, int n) {
        if (n >= 3 && (head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xD8 && (head[2] & 0xFF) == 0xFF) {
            return ImageKind.JPEG;
        }
        byte[] png = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        if (startsWith(head, n, png)) {
            return ImageKind.PNG;
        }
        if (n >= 6) {
            String s = new String(head, 0, 6);
            if ("GIF87a".equals(s) || "GIF89a".equals(s)) {
                return ImageKind.GIF;
            }
        }
        if (n >= 12) {
            String riff = new String(head, 0, 4);
            String webp = new String(head, 8, 4);
            if ("RIFF".equals(riff) && "WEBP".equals(webp)) {
                return ImageKind.WEBP;
            }
        }
        return null;
    }

    private boolean startsWith(byte[] head, int n, byte[] sig) {
        if (n < sig.length) {
            return false;
        }
        for (int i = 0; i < sig.length; i++) {
            if (head[i] != sig[i]) {
                return false;
            }
        }
        return true;
    }

    private String extractExt(String originalName) {
        if (originalName == null) {
            return null;
        }
        String s = originalName.trim();
        int idx = s.lastIndexOf('.');
        if (idx < 0 || idx >= s.length() - 1) {
            return null;
        }
        String ext = s.substring(idx + 1).toLowerCase(Locale.ROOT);
        if (ext.length() > 10) {
            return null;
        }
        return ext;
    }

    private boolean isAllowedExt(String ext) {
        if (ext == null) {
            return false;
        }
        return ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png") || ext.equals("gif") || ext.equals("webp");
    }

    private boolean isAllowedAudioExt(String ext) {
        if (ext == null) {
            return false;
        }
        return ext.equals("mp3") || ext.equals("m4a") || ext.equals("wav") || ext.equals("ogg") || ext.equals("webm");
    }

    private String guessAudioExt(String contentType) {
        String ct = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        if (ct.contains("mpeg")) return ".mp3";
        if (ct.contains("wav")) return ".wav";
        if (ct.contains("ogg")) return ".ogg";
        if (ct.contains("webm")) return ".webm";
        if (ct.contains("mp4") || ct.contains("m4a")) return ".m4a";
        return ".mp3";
    }

    private String safeSegment(String s) {
        String t = String.valueOf(s == null ? "" : s).trim();
        if (t.isEmpty()) {
            return "unknown";
        }
        String out = t.replaceAll("[^0-9A-Za-z_-]", "_");
        if (out.isEmpty()) {
            return "unknown";
        }
        return out;
    }

    private static class DetectedImage {
        private ImageKind kind;
        private String ext;
    }

    private enum ImageKind {
        JPEG(".jpg"),
        PNG(".png"),
        GIF(".gif"),
        WEBP(".webp");

        private final String ext;

        ImageKind(String ext) {
            this.ext = ext;
        }
    }
}
