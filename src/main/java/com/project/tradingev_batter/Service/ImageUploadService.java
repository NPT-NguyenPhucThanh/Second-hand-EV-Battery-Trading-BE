package com.project.tradingev_batter.Service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class ImageUploadService {

    private final Cloudinary cloudinary;

    @Autowired
    public ImageUploadService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    /**
     * Upload image và return URL
     * @param file MultipartFile từ request
     * @param folder Folder trên Cloudinary (EvTrading)
     * @return URL của image
     * @throws IOException Nếu upload fail
     */
    public String uploadImage(MultipartFile file, String folder) throws IOException {
        //Validate file
        if (file == null || file.isEmpty()) {
            throw new IOException("File is empty");
        }
        if (!file.getContentType().startsWith("image/")) {
            throw new IOException("Invalid file type: Must be image (jpg/png/gif)");
        }
        if (file.getSize() > 5 * 1024 * 1024) {  // 5MB limit
            throw new IOException("File too large: Max 5MB");
        }
        // Kiểm tra tên file tránh path traversal/malicious
        String originalFilename = file.getOriginalFilename();
        if (originalFilename.contains("..") || originalFilename.contains("/") || originalFilename.contains("\\")) {
            throw new IOException("Invalid filename");
        }

        //Optimize params (resize, quality auto)
        Map<String, Object> params = ObjectUtils.asMap(
                "folder", folder,
                "resource_type", "image",
                "overwrite", true,
                "invalidate", true,
                "transformation", new com.cloudinary.Transformation()  // Add import com.cloudinary.Transformation
                        .width(800)  // Resize max width 800px
                        .crop("scale")
                        .quality("auto:good")  // Auto quality
        );

        Map uploadResult = cloudinary.uploader().upload(file.getBytes(), params);
        return (String) uploadResult.get("secure_url");
    }

    //Delete image by public_id (từ URL extract)
    public void deleteImage(String publicId) throws IOException {
        cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
    }
}