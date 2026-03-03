package stirling.software.common.service;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class MobileScannerServiceTest {

    @Test
    void uploadRequiresExistingSession() throws IOException {
        MobileScannerService service = new MobileScannerService();
        MockMultipartFile file =
                new MockMultipartFile("files", "scan.png", "image/png", "hello".getBytes());

        assertThrows(
                IllegalArgumentException.class,
                () -> service.uploadFiles("missing-session", List.of(file)));
    }

    @Test
    void uploadRejectsUnsupportedExtensions() throws IOException {
        MobileScannerService service = new MobileScannerService();
        service.createSession("test-session-1");

        MockMultipartFile file =
                new MockMultipartFile("files", "scan.svg", "image/svg+xml", "hello".getBytes());

        assertThrows(
                IllegalArgumentException.class,
                () -> service.uploadFiles("test-session-1", List.of(file)));
    }
}
