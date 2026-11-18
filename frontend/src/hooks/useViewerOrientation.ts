/**
 * 전체화면 뷰어(사진·영상)가 열려 있는 동안만 가로 회전을 허용한다.
 *
 * 앱은 세로 고정이다(AndroidManifest). 뷰어를 열면 기기를 눕혔을 때 따라 돌고,
 * 닫으면(언마운트) 다시 세로로 돌려놓는다. 뷰어가 여럿 겹쳐도 마지막에 세로로 복구된다.
 */
import { useEffect } from 'react';
import Orientation from 'react-native-orientation-locker';

export function useViewerOrientation(enabled = true) {
  useEffect(() => {
    if (!enabled) {return undefined;}
    try {
      Orientation.unlockAllOrientations();
    } catch {
      // 네이티브 모듈을 못 쓰는 환경(테스트 등)에서는 세로로 그대로 둔다.
    }
    return () => {
      try {
        Orientation.lockToPortrait();
      } catch {
        // 위와 같다.
      }
    };
  }, [enabled]);
}
