/**
 * 런처 아이콘 생성.
 *
 * 원본(ICON_SRC)은 정사각형 로고 이미지다. 레거시 아이콘은 그대로 쓰고,
 * 적응형 아이콘은 런처가 가장자리를 잘라내므로 안전 영역 안에 줄여 넣는다.
 * 잘려도 하늘색이 이어 보이도록 배경을 원본 위쪽 하늘색으로 맞춘다.
 *
 * 로고를 바꾸면: node scripts/generate-icons.js
 */
const sharp = require('sharp');
const fs = require('fs');
const path = require('path');

const ICON_SRC = 'src/assets/images/app-icon.png';
const RES = 'android/app/src/main/res';
const SKY = { r: 0x5b, g: 0xb8, b: 0xfd, alpha: 1 }; // 원본 위쪽 하늘색

const LEGACY = { mdpi: 48, hdpi: 72, xhdpi: 96, xxhdpi: 144, xxxhdpi: 192 };
const ADAPTIVE = { mdpi: 108, hdpi: 162, xhdpi: 216, xxhdpi: 324, xxxhdpi: 432 };

/** 런처가 둥글게 깎아낼 때 쓸 원형 마스크 */
function circleMask(size) {
  const r = size / 2;
  return Buffer.from(
    `<svg width="${size}" height="${size}"><circle cx="${r}" cy="${r}" r="${r}" fill="#fff"/></svg>`,
  );
}

async function run() {
  for (const [dpi, size] of Object.entries(LEGACY)) {
    const square = await sharp(ICON_SRC).resize(size, size, { fit: 'cover' }).png().toBuffer();
    const dir = path.join(RES, `mipmap-${dpi}`);
    fs.writeFileSync(path.join(dir, 'ic_launcher.png'), square);

    const round = await sharp(square)
      .composite([{ input: circleMask(size), blend: 'dest-in' }])
      .png()
      .toBuffer();
    fs.writeFileSync(path.join(dir, 'ic_launcher_round.png'), round);
    console.log(`mipmap-${dpi}  ${size}x${size}  (일반 + 둥근)`);
  }

  // 적응형 전경: 108dp 중 가운데 72dp 만 확실히 보인다. 그 안에 다 들어가게 줄인다.
  for (const [dpi, size] of Object.entries(ADAPTIVE)) {
    const inner = Math.round(size * 0.62);
    const art = await sharp(ICON_SRC).resize(inner, inner, { fit: 'cover' }).png().toBuffer();
    const out = await sharp({
      create: { width: size, height: size, channels: 4, background: { r: 0, g: 0, b: 0, alpha: 0 } },
    })
      .composite([{ input: art, gravity: 'center' }])
      .png()
      .toBuffer();
    fs.writeFileSync(path.join(RES, `mipmap-${dpi}`, 'ic_launcher_foreground.png'), out);
    console.log(`mipmap-${dpi}  적응형 전경 ${size}x${size} (안쪽 ${inner})`);
  }

  // 스토어 등록용 512x512
  await sharp(ICON_SRC)
    .resize(512, 512, { fit: 'cover' })
    .flatten({ background: SKY })
    .png()
    .toFile('android/app/src/main/ic_launcher-playstore.png');
  console.log('스토어 아이콘 512x512 → android/app/src/main/ic_launcher-playstore.png');
}

run().catch((e) => { console.error(e); process.exit(1); });
