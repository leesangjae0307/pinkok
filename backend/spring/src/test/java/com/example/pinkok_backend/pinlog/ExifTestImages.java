package com.example.pinkok_backend.pinlog;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * 테스트용: 폰 카메라처럼 "픽셀은 안 돌리고 EXIF 회전 표시만 붙인" JPEG 를 만든다.
 * 왼쪽 절반 빨강 · 오른쪽 절반 파랑이라, 돌린 뒤 어느 쪽이 위로 갔는지로 회전이 맞는지 알 수 있다.
 */
public final class ExifTestImages {

    private ExifTestImages() {
    }

    /** 왼쪽 빨강·오른쪽 파랑 JPEG 에 EXIF Orientation 을 붙인다 */
    public static byte[] leftRedRightBlue(int width, int height, int orientation) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.RED);
        g.fillRect(0, 0, width / 2, height);
        g.setColor(Color.BLUE);
        g.fillRect(width / 2, 0, width - width / 2, height);
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", out);
        return withOrientation(out.toByteArray(), orientation);
    }

    /** JPEG 의 SOI 바로 뒤에 Orientation 하나만 든 EXIF(APP1) 조각을 끼워 넣는다 (빅엔디언 TIFF) */
    public static byte[] withOrientation(byte[] jpeg, int orientation) {
        byte[] tiff = {
                'M', 'M', 0, 42, 0, 0, 0, 8,          // 빅엔디언, 첫 IFD 는 8바이트 뒤
                0, 1,                                  // 항목 1개
                0x01, 0x12, 0, 3, 0, 0, 0, 1,          // 태그 0x0112(Orientation), 형식 SHORT, 개수 1
                0, (byte) orientation, 0, 0,           // 값
                0, 0, 0, 0                             // 다음 IFD 없음
        };
        byte[] exifHeader = {'E', 'x', 'i', 'f', 0, 0};
        int length = 2 + exifHeader.length + tiff.length;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(jpeg, 0, 2);                         // SOI
        out.write(0xFF);
        out.write(0xE1);
        out.write((length >> 8) & 0xFF);
        out.write(length & 0xFF);
        out.write(exifHeader, 0, exifHeader.length);
        out.write(tiff, 0, tiff.length);
        out.write(jpeg, 2, jpeg.length - 2);
        return out.toByteArray();
    }

    /** 빨강에 가까운지 (JPEG 압축 오차 감안) */
    public static boolean isRed(int rgb) {
        return ((rgb >> 16) & 0xFF) > 180 && ((rgb >> 8) & 0xFF) < 90 && (rgb & 0xFF) < 90;
    }

    public static boolean isBlue(int rgb) {
        return ((rgb >> 16) & 0xFF) < 90 && ((rgb >> 8) & 0xFF) < 90 && (rgb & 0xFF) > 180;
    }
}
