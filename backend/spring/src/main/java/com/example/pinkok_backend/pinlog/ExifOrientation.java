package com.example.pinkok_backend.pinlog;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * JPEG 사진의 EXIF 회전 정보.
 *
 * <p>폰은 세로로 찍은 사진의 픽셀을 실제로 돌리지 않고, "보여줄 때 90도 돌려라"는 표시(EXIF Orientation)만 붙인다.
 * 앱·브라우저는 이 표시를 보고 돌려서 보여주지만, 자바 이미지 읽기(ImageIO)와 서버 FFmpeg 4.4 는 무시한다.
 * 그래서 서버가 만드는 썸네일과 PinLog 영상 속 사진이 눕혀져 나왔다. 이 클래스가 그 표시를 읽고 반영한다.
 *
 * <p>값의 뜻 (EXIF 규격): 1 그대로 · 2 좌우 반전 · 3 180도 · 4 상하 반전 · 5 대각선 반전 ·
 * 6 시계방향 90도 · 7 반대 대각선 반전 · 8 반시계방향 90도
 */
public final class ExifOrientation {

    private ExifOrientation() {
    }

    /** 회전 정보를 찾을 때 파일 앞부분만 읽는다 (EXIF 는 항상 앞쪽에 있다) */
    private static final int HEAD_BYTES = 256 * 1024;

    /** JPEG 의 회전 값 (1~8). JPEG 가 아니거나 정보가 없으면 1 */
    public static int read(byte[] jpeg) {
        try {
            return parse(jpeg);
        } catch (RuntimeException e) {
            return 1; // 깨진 EXIF 는 "회전 없음"으로 본다
        }
    }

    public static int read(Path file) {
        try (InputStream in = Files.newInputStream(file)) {
            return read(in.readNBytes(HEAD_BYTES));
        } catch (IOException e) {
            return 1;
        }
    }

    /** 90도 단위로 돌아가서 가로·세로가 바뀌는지 (5~8) */
    public static boolean swapsSides(int orientation) {
        return orientation >= 5 && orientation <= 8;
    }

    /** 이미지를 회전 정보대로 돌린다. 1 이면 그대로 돌려준다 */
    public static BufferedImage apply(BufferedImage source, int orientation) {
        if (orientation < 2 || orientation > 8) {
            return source;
        }
        int w = source.getWidth();
        int h = source.getHeight();
        boolean swap = swapsSides(orientation);
        int type = source.getType() == BufferedImage.TYPE_CUSTOM ? BufferedImage.TYPE_INT_ARGB : source.getType();
        BufferedImage target = new BufferedImage(swap ? h : w, swap ? w : h, type);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int dx;
                int dy;
                switch (orientation) {
                    case 2 -> { dx = w - 1 - x; dy = y; }
                    case 3 -> { dx = w - 1 - x; dy = h - 1 - y; }
                    case 4 -> { dx = x; dy = h - 1 - y; }
                    case 5 -> { dx = y; dy = x; }
                    case 6 -> { dx = h - 1 - y; dy = x; }
                    case 7 -> { dx = h - 1 - y; dy = w - 1 - x; }
                    default -> { dx = y; dy = w - 1 - x; } // 8
                }
                target.setRGB(dx, dy, source.getRGB(x, y));
            }
        }
        return target;
    }

    /**
     * 회전 표시가 있는 JPEG 를 실제로 바로 세운 JPEG(EXIF 없음)로 새로 쓴다. PinLog 합성용.
     *
     * <p>FFmpeg 는 버전마다 EXIF 회전을 다르게 다룬다 (서버 4.4 는 무시, 최신 버전은 스스로 돌리고 결과 영상에 회전 표시까지 넘긴다).
     * 그래서 FFmpeg 에 맡기지 않고, 미리 바로 세운 사진을 넘겨서 어느 버전에서든 같은 결과가 나오게 한다.
     * 영상은 1280×720 이라 긴 변 {@code maxSide} 이하로 줄여서 읽는다 (메모리·시간 절약).
     *
     * @return 새로 쓴 파일. 회전 표시가 없거나(1) 읽지 못하면 원래 파일 그대로
     */
    public static Path upright(Path source, Path target, int maxSide) {
        int orientation = read(source);
        if (orientation == 1) {
            return source;
        }
        try (javax.imageio.stream.ImageInputStream in = javax.imageio.ImageIO.createImageInputStream(source.toFile())) {
            java.util.Iterator<javax.imageio.ImageReader> readers = javax.imageio.ImageIO.getImageReaders(in);
            if (!readers.hasNext()) {
                return source;
            }
            javax.imageio.ImageReader reader = readers.next();
            BufferedImage image;
            try {
                reader.setInput(in, true, true);
                int step = Math.max(1, Math.max(reader.getWidth(0), reader.getHeight(0)) / maxSide);
                javax.imageio.ImageReadParam param = reader.getDefaultReadParam();
                param.setSourceSubsampling(step, step, 0, 0);
                image = reader.read(0, param);
            } finally {
                reader.dispose();
            }
            BufferedImage rotated = apply(image, orientation);
            BufferedImage rgb = new BufferedImage(rotated.getWidth(), rotated.getHeight(), BufferedImage.TYPE_INT_RGB);
            rgb.getGraphics().drawImage(rotated, 0, 0, null);
            if (!javax.imageio.ImageIO.write(rgb, "jpg", target.toFile())) {
                return source;
            }
            return target;
        } catch (IOException | RuntimeException e) {
            return source; // 못 돌리면 원본으로 (눕혀질 수는 있어도 합성 자체는 된다)
        }
    }

    /**
     * JPEG 에서 EXIF(APP1) 조각을 지운다. 픽셀을 이미 바로 세운 JPEG(HEIC 를 변환한 것 등)에 회전 표시가 남아 있으면
     * 앱이 한 번 더 돌려버리므로 지운다.
     */
    public static byte[] stripExif(byte[] jpeg) {
        if (!isJpeg(jpeg)) {
            return jpeg;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream(jpeg.length);
        out.write(jpeg, 0, 2); // SOI
        int pos = 2;
        while (pos + 4 <= jpeg.length && (jpeg[pos] & 0xFF) == 0xFF) {
            int marker = jpeg[pos + 1] & 0xFF;
            if (marker == 0xDA) { // 영상 데이터 시작 — 나머지는 그대로
                break;
            }
            int length = u16(jpeg, pos + 2, false);
            int end = pos + 2 + length;
            if (length < 2 || end > jpeg.length) {
                return jpeg; // 구조가 이상하면 손대지 않는다
            }
            boolean exif = marker == 0xE1 && length >= 8 && startsWith(jpeg, pos + 4, "Exif");
            if (!exif) {
                out.write(jpeg, pos, end - pos);
            }
            pos = end;
        }
        out.write(jpeg, pos, jpeg.length - pos);
        return out.toByteArray();
    }

    // ------------------------------------------------------------------

    private static int parse(byte[] b) {
        if (!isJpeg(b)) {
            return 1;
        }
        int pos = 2;
        while (pos + 4 <= b.length && (b[pos] & 0xFF) == 0xFF) {
            int marker = b[pos + 1] & 0xFF;
            if (marker == 0xDA || marker == 0xD9) {
                break;
            }
            int length = u16(b, pos + 2, false);
            if (marker == 0xE1 && length >= 8 && startsWith(b, pos + 4, "Exif")) {
                return orientationInTiff(b, pos + 10, pos + 2 + length);
            }
            pos += 2 + length;
        }
        return 1;
    }

    /** TIFF 머리말부터 첫 번째 IFD 를 훑어 0x0112(Orientation) 값을 찾는다 */
    private static int orientationInTiff(byte[] b, int tiff, int limit) {
        if (tiff + 8 > limit) {
            return 1;
        }
        boolean little = b[tiff] == 'I' && b[tiff + 1] == 'I';
        int ifd = tiff + u32(b, tiff + 4, little);
        if (ifd + 2 > limit) {
            return 1;
        }
        int count = u16(b, ifd, little);
        for (int i = 0; i < count; i++) {
            int entry = ifd + 2 + i * 12;
            if (entry + 12 > limit) {
                break;
            }
            if (u16(b, entry, little) == 0x0112) {
                int value = u16(b, entry + 8, little);
                return value >= 1 && value <= 8 ? value : 1;
            }
        }
        return 1;
    }

    private static boolean isJpeg(byte[] b) {
        return b != null && b.length > 4 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8;
    }

    private static boolean startsWith(byte[] b, int offset, String text) {
        if (offset + text.length() > b.length) {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            if (b[offset + i] != text.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    private static int u16(byte[] b, int offset, boolean little) {
        int a = b[offset] & 0xFF;
        int c = b[offset + 1] & 0xFF;
        return little ? (c << 8) | a : (a << 8) | c;
    }

    private static int u32(byte[] b, int offset, boolean little) {
        int v0 = b[offset] & 0xFF;
        int v1 = b[offset + 1] & 0xFF;
        int v2 = b[offset + 2] & 0xFF;
        int v3 = b[offset + 3] & 0xFF;
        return little ? (v3 << 24) | (v2 << 16) | (v1 << 8) | v0 : (v0 << 24) | (v1 << 16) | (v2 << 8) | v3;
    }
}
