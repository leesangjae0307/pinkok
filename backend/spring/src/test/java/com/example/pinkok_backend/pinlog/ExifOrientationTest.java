package com.example.pinkok_backend.pinlog;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

import static com.example.pinkok_backend.pinlog.ExifTestImages.isBlue;
import static com.example.pinkok_backend.pinlog.ExifTestImages.isRed;
import static com.example.pinkok_backend.pinlog.ExifTestImages.leftRedRightBlue;
import static org.assertj.core.api.Assertions.assertThat;

/** EXIF 회전 값 읽기 · 이미지 돌리기 · EXIF 지우기 */
class ExifOrientationTest {

    @Test
    @DisplayName("JPEG 의 EXIF 회전 값을 읽는다 (없거나 JPEG 가 아니면 1)")
    void readOrientation() throws Exception {
        for (int o = 1; o <= 8; o++) {
            assertThat(ExifOrientation.read(leftRedRightBlue(40, 20, o))).isEqualTo(o);
        }
        byte[] plain = java.util.Arrays.copyOf(leftRedRightBlue(40, 20, 1), 10);
        assertThat(ExifOrientation.read(plain)).isEqualTo(1);
        assertThat(ExifOrientation.read(new byte[]{'R', 'I', 'F', 'F', 0, 0})).isEqualTo(1);
        assertThat(ExifOrientation.read((byte[]) null)).isEqualTo(1);
    }

    @Test
    @DisplayName("6(시계 90도): 가로 사진이 세로가 되고, 왼쪽(빨강)이 위로 간다")
    void rotateClockwise() throws Exception {
        BufferedImage source = ImageIO.read(new ByteArrayInputStream(leftRedRightBlue(40, 20, 1)));
        BufferedImage rotated = ExifOrientation.apply(source, 6);
        assertThat(rotated.getWidth()).isEqualTo(20);
        assertThat(rotated.getHeight()).isEqualTo(40);
        assertThat(isRed(rotated.getRGB(10, 5))).isTrue();
        assertThat(isBlue(rotated.getRGB(10, 34))).isTrue();
    }

    @Test
    @DisplayName("8(반시계 90도): 왼쪽(빨강)이 아래로, 3(180도): 왼쪽이 오른쪽으로")
    void otherRotations() throws Exception {
        BufferedImage source = ImageIO.read(new ByteArrayInputStream(leftRedRightBlue(40, 20, 1)));

        BufferedImage ccw = ExifOrientation.apply(source, 8);
        assertThat(isBlue(ccw.getRGB(10, 5))).isTrue();
        assertThat(isRed(ccw.getRGB(10, 34))).isTrue();

        BufferedImage upsideDown = ExifOrientation.apply(source, 3);
        assertThat(upsideDown.getWidth()).isEqualTo(40);
        assertThat(isBlue(upsideDown.getRGB(5, 10))).isTrue();
        assertThat(isRed(upsideDown.getRGB(34, 10))).isTrue();

        assertThat(ExifOrientation.apply(source, 1)).isSameAs(source);
    }

    @Test
    @DisplayName("EXIF 를 지우면 회전 값이 1이 되고, 이미지는 그대로 읽힌다")
    void stripExif() throws Exception {
        byte[] withExif = leftRedRightBlue(40, 20, 6);
        byte[] stripped = ExifOrientation.stripExif(withExif);
        assertThat(ExifOrientation.read(stripped)).isEqualTo(1);
        assertThat(stripped.length).isLessThan(withExif.length);
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(stripped));
        assertThat(image.getWidth()).isEqualTo(40);
    }
}
