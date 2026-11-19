package promiseproof_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    private FileStorageService fileStorageService;

    @BeforeEach
    void setUp() {
        fileStorageService = new FileStorageService(tempDir.toString());
    }

    @Test
    void rejectsUploadsWhoseContentIsNotADocument() {

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "notes.pdf",
                "application/pdf",
                "This is definitely not a PDF document.".getBytes()
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> fileStorageService.storeFile(file)
        );

        assertEquals(400, exception.getStatusCode().value());
    }

    @Test
    void acceptsUploadsWhoseContentMatchesThePdfMagicBytes() {

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "report.pdf",
                "application/pdf",
                "%PDF-1.4\n%âãÏÓ test content".getBytes()
        );

        String storedName = fileStorageService.storeFile(file);

        assertTrue(storedName.endsWith(".pdf"));

        fileStorageService.deleteFile(storedName);
    }

    @Test
    void acceptsJpgAndJpegFileNamesForTheSameJpegContent() {

        byte[] jpegContent = {
                (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
                0x00, 0x10, 0x4A, 0x46, 0x49, 0x46
        };

        MockMultipartFile jpg = new MockMultipartFile(
                "file",
                "photo.jpg",
                "image/jpeg",
                jpegContent
        );

        MockMultipartFile jpeg = new MockMultipartFile(
                "file",
                "photo.jpeg",
                "image/jpg",
                jpegContent
        );

        String storedJpg = fileStorageService.storeFile(jpg);
        String storedJpeg = fileStorageService.storeFile(jpeg);

        assertTrue(storedJpg.endsWith(".jpg"));
        assertTrue(storedJpeg.endsWith(".jpg"));

        fileStorageService.deleteFile(storedJpg);
        fileStorageService.deleteFile(storedJpeg);
    }

    @Test
    void acceptsJpegWhenTheBrowserSendsAGenericContentType() {

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "scan.JPEG",
                "application/octet-stream",
                new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00 }
        );

        String storedName = fileStorageService.storeFile(file);

        assertTrue(storedName.endsWith(".jpg"));

        fileStorageService.deleteFile(storedName);
    }

    @Test
    void rejectsPngContentDeclaredAsJpg() {

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "fake.jpg",
                "image/jpeg",
                new byte[] {
                        (byte) 0x89, 0x50, 0x4E, 0x47,
                        0x0D, 0x0A, 0x1A, 0x0A
                }
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> fileStorageService.storeFile(file)
        );

        assertEquals(400, exception.getStatusCode().value());
    }

    @Test
    void rejectsUploadsWithUnsupportedExtensions() {

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "notes.txt",
                "text/plain",
                "hello world".getBytes()
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> fileStorageService.storeFile(file)
        );

        assertEquals(400, exception.getStatusCode().value());
    }
}
