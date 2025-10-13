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
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/uploads")
@RequiredArgsConstructor
public class UploadController {

    private final UploadService uploadService;

    @Operation(summary = "Upload 1 ảnh lên Cloudinary",
            description = "Chọn 1 file ảnh (multipart/form-data), server đẩy lên Cloudinary và trả URL.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Upload thành công",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UploadResponse.class)))
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
        String url = uploadService.uploadImage(file);
        return new UploadResponse(url, "image", null);
    }

    @Operation(summary = "Upload nhiều ảnh lên Cloudinary",
            description = "Chọn nhiều file (multipart/form-data, key = files), server đẩy lên Cloudinary và trả danh sách URL.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Upload thành công",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = UploadResponse.class))))
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
        List<UploadResponse> result = new ArrayList<>();
        for (MultipartFile f : files) {
            String url = uploadService.uploadImage(f);
            result.add(new UploadResponse(url, "image", null));
        }
        return result;
    }
}
