import { Outlet } from 'react-router-dom';

function AuthLayout() {
  return (
    <div className="min-h-screen bg-gradient-to-b from-mint-50 via-clean-white to-clean-white text-ink">
      <div className="mx-auto flex min-h-screen max-w-lg flex-col justify-center px-4 py-10 sm:px-6">
        <div className="mb-6 text-center">
          <p className="text-xs font-semibold uppercase tracking-wide text-mint-700">Partition Mate</p>
          <h1 className="mt-2 text-2xl font-semibold text-ink">함께 사서 가볍게 나눠요</h1>
          <p className="mt-2 text-sm text-ink/60">가까운 지점에서 파티를 찾고 참여하세요.</p>
        </div>
        <Outlet />
      </div>
    </div>
  );
}

export default AuthLayout;
