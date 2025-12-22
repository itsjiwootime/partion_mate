import { Routes, Route } from 'react-router-dom';
import Layout from './components/Layout';
import Home from './pages/Home';
import PartyList from './pages/PartyList';
import CreateParty from './pages/CreateParty';
import Login from './pages/Login';
import Signup from './pages/Signup';
import Profile from './pages/Profile';
import PartyDetail from './pages/PartyDetail';
import JoinParty from './pages/JoinParty';
import MyParties from './pages/MyParties';

const Chat = () => (
  <div className="space-y-3">
    <h2 className="text-lg font-semibold text-ink">채팅</h2>
    <p className="text-sm text-ink/70">참여 중인 파티의 채팅이 여기에 표시됩니다.</p>
  </div>
);

function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<Home />} />
        <Route path="/parties" element={<PartyList />} />
        <Route path="/branch/:id" element={<PartyList />} />
        <Route path="/parties/:id" element={<PartyDetail />} />
        <Route path="/parties/:id/join" element={<JoinParty />} />
        <Route path="/parties/create" element={<CreateParty />} />
        <Route path="/my-parties" element={<MyParties />} />
        <Route path="/chat" element={<Chat />} />
        <Route path="/me" element={<Profile />} />
        <Route path="/login" element={<Login />} />
        <Route path="/signup" element={<Signup />} />
      </Route>
    </Routes>
  );
}

export default App;
