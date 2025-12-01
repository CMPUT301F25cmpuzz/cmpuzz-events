package com.example.cmpuzz_events.utils;

import android.graphics.Bitmap;
import android.graphics.Color;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class QRCodeGeneratorTest {

    @Mock
    private QRCodeWriter mockQRCodeWriter;

    @Mock
    private BitMatrix mockBitMatrix;

    @Mock
    private Bitmap mockBitmap;

    private MockedStatic<Bitmap> bitmapStaticMock;

    private static final String TEST_URL = "https://example.com/event/123";
    private static final int TEST_WIDTH = 10;
    private static final int TEST_HEIGHT = 10;

    @Before
    public void setUp() {
        bitmapStaticMock = Mockito.mockStatic(Bitmap.class);
    }

    @After
    public void tearDown() {
        if (bitmapStaticMock != null) {
            bitmapStaticMock.close();
        }
    }

    @Test
    public void generateQRCode_withValidUrl_returnsNonNullBitmap() throws WriterException {
        // Arrange
        bitmapStaticMock.when(() -> Bitmap.createBitmap(TEST_WIDTH, TEST_HEIGHT, Bitmap.Config.RGB_565))
                .thenReturn(mockBitmap);

        try (MockedConstruction<QRCodeWriter> mocked = Mockito.mockConstruction(QRCodeWriter.class,
                (mock, context) -> {
                    when(mock.encode(TEST_URL, BarcodeFormat.QR_CODE, TEST_WIDTH, TEST_HEIGHT))
                            .thenReturn(mockBitMatrix);
                })) {

            // Mock BitMatrix behavior
            when(mockBitMatrix.get(anyInt(), anyInt())).thenReturn(true, false);

            // Act
            Bitmap result = QRCodeGenerator.generateQRCode(TEST_URL, TEST_WIDTH, TEST_HEIGHT);

            // Assert
            assertNotNull(result);
            bitmapStaticMock.verify(() -> Bitmap.createBitmap(TEST_WIDTH, TEST_HEIGHT, Bitmap.Config.RGB_565));
        }
    }

    @Test
    public void generateQRCode_withEmptyUrl_returnsNonNullBitmap() throws WriterException {
        // Arrange
        String emptyUrl = "";
        bitmapStaticMock.when(() -> Bitmap.createBitmap(TEST_WIDTH, TEST_HEIGHT, Bitmap.Config.RGB_565))
                .thenReturn(mockBitmap);

        try (MockedConstruction<QRCodeWriter> mocked = Mockito.mockConstruction(QRCodeWriter.class,
                (mock, context) -> {
                    when(mock.encode(emptyUrl, BarcodeFormat.QR_CODE, TEST_WIDTH, TEST_HEIGHT))
                            .thenReturn(mockBitMatrix);
                })) {

            when(mockBitMatrix.get(anyInt(), anyInt())).thenReturn(false);

            // Act
            Bitmap result = QRCodeGenerator.generateQRCode(emptyUrl, TEST_WIDTH, TEST_HEIGHT);

            // Assert
            assertNotNull(result);
        }
    }

    @Test
    public void generateQRCode_withWriterException_returnsNull() throws WriterException {
        // Arrange
        try (MockedConstruction<QRCodeWriter> mocked = Mockito.mockConstruction(QRCodeWriter.class,
                (mock, context) -> {
                    when(mock.encode(TEST_URL, BarcodeFormat.QR_CODE, TEST_WIDTH, TEST_HEIGHT))
                            .thenThrow(new WriterException("Invalid QR code data"));
                })) {

            // Act
            Bitmap result = QRCodeGenerator.generateQRCode(TEST_URL, TEST_WIDTH, TEST_HEIGHT);

            // Assert
            assertNull(result);
        }
    }

    @Test
    public void generateQRCode_withDifferentDimensions_usesCorrectDimensions() throws WriterException {
        // Arrange
        int customWidth = 20;
        int customHeight = 20;
        
        bitmapStaticMock.when(() -> Bitmap.createBitmap(customWidth, customHeight, Bitmap.Config.RGB_565))
                .thenReturn(mockBitmap);

        try (MockedConstruction<QRCodeWriter> mocked = Mockito.mockConstruction(QRCodeWriter.class,
                (mock, context) -> {
                    when(mock.encode(TEST_URL, BarcodeFormat.QR_CODE, customWidth, customHeight))
                            .thenReturn(mockBitMatrix);
                })) {

            when(mockBitMatrix.get(anyInt(), anyInt())).thenReturn(true);

            // Act
            Bitmap result = QRCodeGenerator.generateQRCode(TEST_URL, customWidth, customHeight);

            // Assert
            assertNotNull(result);
            bitmapStaticMock.verify(() -> Bitmap.createBitmap(customWidth, customHeight, Bitmap.Config.RGB_565));
        }
    }

    @Test
    public void generateQRCode_setsPixelsCorrectly() throws WriterException {
        // Arrange
        bitmapStaticMock.when(() -> Bitmap.createBitmap(TEST_WIDTH, TEST_HEIGHT, Bitmap.Config.RGB_565))
                .thenReturn(mockBitmap);

        try (MockedConstruction<QRCodeWriter> mocked = Mockito.mockConstruction(QRCodeWriter.class,
                (mock, context) -> {
                    when(mock.encode(TEST_URL, BarcodeFormat.QR_CODE, TEST_WIDTH, TEST_HEIGHT))
                            .thenReturn(mockBitMatrix);
                })) {

            // Mock BitMatrix to return true for (0,0) and false for others
            when(mockBitMatrix.get(0, 0)).thenReturn(true);
            when(mockBitMatrix.get(anyInt(), anyInt())).thenReturn(false);

            // Act
            Bitmap result = QRCodeGenerator.generateQRCode(TEST_URL, TEST_WIDTH, TEST_HEIGHT);

            // Assert
            assertNotNull(result);
            // Verify that setPixel is called for specific coordinates
            verify(mockBitmap).setPixel(eq(0), eq(0), anyInt()); // Pixel at (0,0)
        }
    }

    @Test
    public void generateQRCode_withNullUrl_handlesGracefully() throws WriterException {
        // Arrange
        try (MockedConstruction<QRCodeWriter> mocked = Mockito.mockConstruction(QRCodeWriter.class,
                (mock, context) -> {
                    when(mock.encode(null, BarcodeFormat.QR_CODE, TEST_WIDTH, TEST_HEIGHT))
                            .thenThrow(new WriterException("Null content"));
                })) {

            // Act
            Bitmap result = QRCodeGenerator.generateQRCode(null, TEST_WIDTH, TEST_HEIGHT);

            // Assert
            assertNull(result);
        }
    }

    @Test
    public void generateQRCode_withLongUrl_returnsNonNullBitmap() throws WriterException {
        // Arrange
        String longUrl = "https://example.com/event/" + "a".repeat(1000);
        
        bitmapStaticMock.when(() -> Bitmap.createBitmap(TEST_WIDTH, TEST_HEIGHT, Bitmap.Config.RGB_565))
                .thenReturn(mockBitmap);

        try (MockedConstruction<QRCodeWriter> mocked = Mockito.mockConstruction(QRCodeWriter.class,
                (mock, context) -> {
                    when(mock.encode(longUrl, BarcodeFormat.QR_CODE, TEST_WIDTH, TEST_HEIGHT))
                            .thenReturn(mockBitMatrix);
                })) {

            when(mockBitMatrix.get(anyInt(), anyInt())).thenReturn(true);

            // Act
            Bitmap result = QRCodeGenerator.generateQRCode(longUrl, TEST_WIDTH, TEST_HEIGHT);

            // Assert
            assertNotNull(result);
        }
    }

    @Test
    public void generateQRCode_withSmallDimensions_returnsNonNullBitmap() throws WriterException {
        // Arrange
        int smallWidth = 5;
        int smallHeight = 5;
        
        bitmapStaticMock.when(() -> Bitmap.createBitmap(smallWidth, smallHeight, Bitmap.Config.RGB_565))
                .thenReturn(mockBitmap);

        try (MockedConstruction<QRCodeWriter> mocked = Mockito.mockConstruction(QRCodeWriter.class,
                (mock, context) -> {
                    when(mock.encode(TEST_URL, BarcodeFormat.QR_CODE, smallWidth, smallHeight))
                            .thenReturn(mockBitMatrix);
                })) {

            when(mockBitMatrix.get(anyInt(), anyInt())).thenReturn(true);

            // Act
            Bitmap result = QRCodeGenerator.generateQRCode(TEST_URL, smallWidth, smallHeight);

            // Assert
            assertNotNull(result);
        }
    }
}
