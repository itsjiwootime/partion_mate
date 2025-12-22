import { Home, ListChecks, User, FolderKanban } from 'lucide-react';
import { NavLink, useLocation } from 'react-router-dom';

const navItems = [
  {
    to: '/',
    label: '홈',
    icon: Home,
    match: (path) => path === '/',
  },
  {
    to: '/parties',
    label: '파티목록',
    icon: ListChecks,
    match: (path) => path.startsWith('/parties') || path.startsWith('/branch'),
  },
  { to: '/my-parties', label: '내파티', icon: FolderKanban, match: (path) => path.startsWith('/my-parties') },
  { to: '/me', label: '내정보', icon: User, match: (path) => path.startsWith('/me') },
];

function BottomNav() {
  const { pathname } = useLocation();

  return (
    <nav className="fixed bottom-0 left-0 right-0 z-20 border-t border-mint-100 bg-white/90 backdrop-blur-md md:static md:mx-auto md:mt-10 md:max-w-4xl md:rounded-2xl md:border">
      <div className="grid grid-cols-4">
        {navItems.map(({ to, label, icon: Icon, match }) => {
          const isActive = match(pathname);
          return (
            <NavLink
              key={to}
              to={to}
              className={[
                'flex flex-col items-center gap-1 py-3 text-xs font-semibold transition',
                isActive ? 'text-mint-700' : 'text-ink/60 hover:text-ink',
              ].join(' ')}
              end={to === '/'}
            >
              <>
                <span
                  className={[
                    'flex h-10 w-10 items-center justify-center rounded-full',
                    isActive ? 'bg-mint-500/15 text-mint-700 shadow-inner' : 'bg-clean-white',
                  ].join(' ')}
                >
                  <Icon size={22} />
                </span>
                {label}
              </>
            </NavLink>
          );
        })}
      </div>
    </nav>
  );
}

export default BottomNav;
