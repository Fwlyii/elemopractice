package com.tju.elm_bk.service;

import com.tju.elm_bk.exception.APIException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageStorageServiceTest {

    @Test
    void storesPngUsingServerGeneratedFilename(@TempDir Path tempDir) throws Exception {
        byte[] png = new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 1, 2, 3};
        MockMultipartFile file = new MockMultipartFile(
                "avatar", "../../unsafe-name.jpg", "image/jpeg", png);

        String url = new ImageStorageService(tempDir.toString()).storeImage(file);

        assertTrue(url.matches("/uploads/[0-9a-f-]+\\.png"));
        Path stored = tempDir.resolve(url.substring("/uploads/".length()));
        assertTrue(Files.isRegularFile(stored));
        assertArrayEquals(png, Files.readAllBytes(stored));
        assertEquals(tempDir.toAbsolutePath().normalize(), stored.getParent());
    }

    @Test
    void rejectsContentWhoseDeclaredMimeTypeIsSpoofed(@TempDir Path tempDir) {
        MockMultipartFile fakeImage = new MockMultipartFile(
                "avatar", "avatar.png", "image/png", "not an image".getBytes());

        assertThrows(APIException.class,
                () -> new ImageStorageService(tempDir.toString()).storeImage(fakeImage));
    }
}
