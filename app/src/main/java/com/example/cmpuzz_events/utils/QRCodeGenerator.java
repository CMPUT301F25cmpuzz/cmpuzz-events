package com.example.cmpuzz_events.utils;

import android.graphics.Bitmap;
import android.graphics.Color;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

/**
 * Utility class for generating QR codes
 */
public class QRCodeGenerator {

    /**
     * Generate a QR code bitmap from a URL
     * 
     * @param url The URL to encode in the QR code
     * @param width Width of the QR code in pixels
     * @param height Height of the QR code in pixels
     * @return Bitmap of the QR code, or null if generation fails
     */
    public static Bitmap generateQRCode(String url, int width, int height) {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        try {
            BitMatrix bitMatrix = qrCodeWriter.encode(url, BarcodeFormat.QR_CODE, width, height);
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            
            return bitmap;
        } catch (WriterException e) {
            e.printStackTrace();
            return null;
        }
    }
}
