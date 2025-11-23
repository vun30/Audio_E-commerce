package org.example.audio_ecommerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.response.UploadResponse;
import org.example.audio_ecommerce.service.Impl.UploadService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/uploads")
@RequiredArgsConstructor
public class UploadController {

    private final UploadService uploadService;

    // Ảnh tối đa 5MB
    private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024; // 5MB
    // Video tối đa 20MB
    private static final long MAX_VIDEO_SIZE = 30L * 1024 * 1024; // 20MB

    private static final List<String> ALLOWED_IMAGE_TYPES = List.of(
            MediaType.IMAGE_JPEG_VALUE, // image/jpeg
            MediaType.IMAGE_PNG_VALUE,  // image/png
            "image/webp"
    );

    private static final String ALLOWED_VIDEO_TYPE = "video/mp4";

    // ============= HELPERS =============

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File ảnh không được để trống");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Định dạng ảnh không hợp lệ. Chỉ hỗ trợ: jpg, jpeg, png, webp"
            );
        }

        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Kích thước ảnh tối đa là 5MB"
            );
        }
    }

    private void validateVideoFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File video không được để trống");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_VIDEO_TYPE.equals(contentType)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Định dạng video không hợp lệ. Chỉ hỗ trợ MP4"
            );
        }

        if (file.getSize() > MAX_VIDEO_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Kích thước video tối đa là 20MB"
            );
        }
    }

    // ============= IMAGE =============

    @Operation(summary = "Upload 1 ảnh lên Cloudinary",
            description = "Chọn 1 file ảnh (multipart/form-data), server đẩy lên Cloudinary và trả URL.\n" +
                    "Hỗ trợ: jpg/jpeg/png/webp, tối đa 5MB.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Upload thành công",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UploadResponse.class))),
            @ApiResponse(responseCode = "400", description = "File không hợp lệ")
    })
    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UploadResponse uploadImage(
            @Parameter(description = "File ảnh cần upload",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(type = "string", format = "binary")
                    ))
            @RequestPart("file") MultipartFile file
    ) throws IOException {

        validateImageFile(file);
        String url = uploadService.uploadImage(file);
        return new UploadResponse(url, "image", null);
    }

    @Operation(summary = "Upload nhiều ảnh lên Cloudinary",
            description = "Chọn nhiều file (multipart/form-data, key = files), server đẩy lên Cloudinary và trả danh sách URL.\n" +
                    "Hỗ trợ: jpg/jpeg/png/webp, tối đa 5MB mỗi ảnh.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Upload thành công",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = UploadResponse.class)))),
            @ApiResponse(responseCode = "400", description = "File không hợp lệ")
    })
    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public List<UploadResponse> uploadImages(
            @Parameter(description = "Danh sách ảnh cần upload (cùng key = files)",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            array = @ArraySchema(schema = @Schema(type = "string", format = "binary"))
                    ))
            @RequestPart("files") List<MultipartFile> files
    ) throws IOException {

        if (files == null || files.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Danh sách ảnh không được rỗng");
        }

        List<UploadResponse> result = new ArrayList<>();
        for (MultipartFile f : files) {
            validateImageFile(f);
            String url = uploadService.uploadImage(f);
            result.add(new UploadResponse(url, "image", null));
        }
        return result;
    }

    // ============= VIDEO =============

    @Operation(summary = "Upload 1 video MP4 lên Cloudinary",
            description = "Chọn 1 file video định dạng MP4 (multipart/form-data), dung lượng tối đa 20MB.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Upload thành công",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UploadResponse.class))),
            @ApiResponse(responseCode = "400", description = "File không hợp lệ (định dạng hoặc kích thước)")
    })
    @PostMapping(value = "/video", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UploadResponse uploadVideo(
            @Parameter(description = "File video MP4 cần upload",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(type = "string", format = "binary")
                    ))
            @RequestPart("file") MultipartFile file
    ) throws IOException {

        validateVideoFile(file);
        String url = uploadService.uploadVideo(file);
        return new UploadResponse(url, "video", null);
    }
}
