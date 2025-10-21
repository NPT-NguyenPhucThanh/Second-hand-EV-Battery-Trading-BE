package com.project.tradingev_batter.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

@Service
public class ChatFileService {

    @Autowired
    private ImageUploadService imageUploadService;

    private static final List<String> ALLOWED_FILE_TYPES = Arrays.asList(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", // DOCX
            "application/msword", // DOC
            "text/plain" // TXT
    );

    private static final List<String> ALLOWED_FILE_EXTENSIONS = Arrays.asList(
            ".pdf", ".docx", ".doc", ".txt"
    );

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    //Validate và upload file attachment cho chat
    public String uploadChatAttachment(MultipartFile file, Long chatroomId) throws Exception {
        // Validate file không null
        if (file == null || file.isEmpty()) {
            throw new Exception("File không được để trống");
        }

        // Validate file size (max 5MB)
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new Exception("File vượt quá kích thước cho phép (5MB). Size hiện tại: " +
                    (file.getSize() / (1024 * 1024)) + "MB");
        }

        // Validate file type
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_FILE_TYPES.contains(contentType)) {
            throw new Exception("Loại file không được hỗ trợ. Chỉ chấp nhận: PDF, DOCX, TXT");
        }

        // Validate file extension
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null ||
            ALLOWED_FILE_EXTENSIONS.stream().noneMatch(originalFilename.toLowerCase()::endsWith)) {
            throw new Exception("Định dạng file không hợp lệ. Chỉ chấp nhận: .pdf, .docx, .txt");
        }

        // Upload file lên Cloudinary
        String folderPath = "chat_attachments/chatroom_" + chatroomId;
        String fileUrl = imageUploadService.uploadImage(file, folderPath);

        return fileUrl;
    }

    //Kiểm tra xem file có phải là document không
    public boolean isDocument(String contentType) {
        return ALLOWED_FILE_TYPES.contains(contentType);
    }

    //Get file extension từ filename
    public String getFileExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            return filename.substring(lastDotIndex).toLowerCase();
        }
        return "";
    }
}

