
package promiseproof_backend.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FileStorageService {

    private final Path uploadPath;

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of("pdf", "jpg", "jpeg", "png");

    // Magic-byte signatures used to verify the real content of an upload.
    // The browser-supplied content type is intentionally not trusted: browsers
    // use several aliases for JPEG (image/jpeg, image/jpg, image/pjpeg) and
    // sometimes send application/octet-stream even for valid images.
    private static final String PDF_PREFIX = "%PDF-";
    private static final int PDF_PREFIX_SCAN_BYTES = 1024;

    private static final int PNG_SIGNATURE_LENGTH = 8;
    private static final byte[] PNG_SIGNATURE = {
            (byte) 0x89, 0x50, 0x4E, 0x47, (byte) 0x0D, (byte) 0x0A, 0x1A, 0x0A
    };
    private static final int JPEG_SIGNATURE_LENGTH = 2;
    private static final byte[] JPEG_SIGNATURE = { (byte) 0xFF, (byte) 0xD8 };

    public FileStorageService(
            @Value("${promiseproof.upload-dir}") String uploadDirectory) {

        this.uploadPath = Paths.get(uploadDirectory)
                .toAbsolutePath()
                .normalize();

        try {
            Files.createDirectories(uploadPath);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Could not create evidence storage directory", e
            );
        }
    }

    public String storeFile(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Please select a file"
            );
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "File size must not exceed 10 MB"
            );
        }

        String originalName = file.getOriginalFilename();

        if (originalName == null || originalName.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid file name"
            );
        }

        String cleanName = Paths.get(originalName)
                .getFileName()
                .toString();

        int dotIndex = cleanName.lastIndexOf(".");

        if (dotIndex < 0 || dotIndex == cleanName.length() - 1) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "File extension is missing"
            );
        }

        String extension = normalizeExtension(
                cleanName.substring(dotIndex + 1).toLowerCase()
        );

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "This file type is not supported"
            );
        }

        String detectedExtension = detectExtension(file);

        if (detectedExtension == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The selected file could not be read as a supported document"
            );
        }

        if (!detectedExtension.equals(extension)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "File content does not match the file name"
            );
        }

        String storedName = UUID.randomUUID() + "." + extension;

        try {
            Path destination = uploadPath.resolve(storedName).normalize();

            if (!destination.startsWith(uploadPath)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid file path"
                );
            }

            Files.copy(file.getInputStream(), destination);

            return storedName;

        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not save uploaded file"
            );
        }
    }

    // Treat .jpg and .jpeg as the same format so a valid JPEG is never rejected
    // merely because the filename uses the other spelling.
    private String normalizeExtension(String extension) {
        return "jpeg".equals(extension) ? "jpg" : extension;
    }

    // Read the leading bytes of an upload and detect its real file type from
    // the magic-byte signature. Returns the allowed extension or null when the
    // content is missing or not a supported document.
    private String detectExtension(MultipartFile file) {

        try (InputStream inputStream = file.getInputStream()) {

            byte[] header = new byte[512];

            int bytesRead = inputStream.read(header);

            if (bytesRead <= 0) {
                return null;
            }

            byte[] content = new byte[bytesRead];
            System.arraycopy(header, 0, content, 0, bytesRead);

            if (startsWith(content, 0, PNG_SIGNATURE, PNG_SIGNATURE_LENGTH)) {
                return "png";
            }

            if (content.length >= JPEG_SIGNATURE_LENGTH &&
                    startsWith(content, 0, JPEG_SIGNATURE, JPEG_SIGNATURE_LENGTH)) {
                return "jpg";
            }

            String prefix = new String(
                    content,
                    0,
                    Math.min(bytesRead, PDF_PREFIX_SCAN_BYTES),
                    StandardCharsets.ISO_8859_1
            );

            if (prefix.toLowerCase().contains(PDF_PREFIX.toLowerCase())) {
                return "pdf";
            }

            return null;

        } catch (IOException e) {
            return null;
        }
    }

    private boolean startsWith(
            byte[] content,
            int offset,
            byte[] signature,
            int length) {

        if (content.length - offset < length) {
            return false;
        }

        for (int index = 0; index < length; index++) {

            if (content[offset + index] != signature[index]) {
                return false;
            }
        }

        return true;
    }

    public String sha256(String storedFileName) {

        Path filePath = uploadPath.resolve(storedFileName).normalize();

        if (!filePath.startsWith(uploadPath)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid file path"
            );
        }

        try (InputStream inputStream = Files.newInputStream(filePath)) {

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;

            while ((read = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }

            return HexFormat.of().formatHex(digest.digest());

        } catch (IOException | NoSuchAlgorithmException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not hash the stored evidence file"
            );
        }
    }

    public Resource loadFile(String storedFileName) {

        try {
            Path filePath = uploadPath.resolve(storedFileName)
                    .normalize();

            if (!filePath.startsWith(uploadPath)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid file path"
                );
            }

            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Evidence file not found"
                );
            }

            return resource;

        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not read evidence file"
            );
        }
    }

    public void deleteFile(String storedFileName) {

        try {
            Path filePath = uploadPath.resolve(storedFileName)
                    .normalize();

            if (filePath.startsWith(uploadPath)) {
                Files.deleteIfExists(filePath);
            }

        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not delete evidence file"
            );
        }
    }
}
