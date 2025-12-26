import { Outlet, useLocation } from 'react-router-dom';
import Header from './Header';
import BottomNav from './BottomNav';

const pageMeta = {
  '/': { title: 'Partition Mate', subtitle: '대용량도 함께 사면 부담 0%' },
  '/parties': { title: '파티 목록', subtitle: '모집 중인 파티를 확인해요' },
  '/parties/create': { title: '파티 생성', subtitle: '새로운 소분 모임을 만들어보세요' },
  '/chat': { title: '채팅', subtitle: '나눔을 위한 대화방' },
  '/me': { title: '내 정보', subtitle: '즐겨찾는 지점과 프로필' },
  branch: { title: '지점 파티', subtitle: '선택한 지점의 파티 목록' },
};

function Layout() {
  const { pathname } = useLocation();
  const isBranchPage = pathname.startsWith('/branch');
  const current = isBranchPage ? pageMeta.branch : pageMeta[pathname] ?? pageMeta['/'];

  return (
    <div className="min-h-screen bg-gradient-to-b from-mint-50 via-clean-white to-clean-white text-ink">
      <div className="mx-auto flex min-h-screen max-w-4xl flex-col px-4 pb-24 pt-6 sm:px-6 md:px-8">
        <Header title={current.title} subtitle={current.subtitle} />
        <main className="mt-6 flex-1 pb-6">
          <Outlet />
        </main>
      </div>
      <BottomNav />
    </div>
  );
}

export default Layout;
