import React from 'react';
import ScreenLayout from '../../../components/layout/ScreenLayout';
import Header from '../../../components/layout/Header';
import EmptyState from '../../../components/feedback/EmptyState';

const DiaryScreen = () => {
  return (
    <ScreenLayout>
      <Header title="여행 다이어리" />
      <EmptyState
        icon="camera"
        title="아직 작성된 다이어리가 없어요"
        description="장소별 메모와 사진을 남겨보세요"
        actionLabel="다이어리 쓰기"
      />
    </ScreenLayout>
  );
};

export default DiaryScreen;
