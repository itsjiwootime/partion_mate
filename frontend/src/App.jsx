import { Routes, Route } from 'react-router-dom';
import Layout from './components/Layout';
import AuthLayout from './components/AuthLayout';
import Home from './pages/Home';
import PartyList from './pages/PartyList';
import CreateParty from './pages/CreateParty';
import Login from './pages/Login';
import Signup from './pages/Signup';
import Profile from './pages/Profile';
import PartyDetail from './pages/PartyDetail';
import JoinParty from './pages/JoinParty';
import MyParties from './pages/MyParties';
import Notifications from './pages/Notifications';
import Chat from './pages/Chat';

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
        <Route path="/notifications" element={<Notifications />} />
        <Route path="/chat" element={<Chat />} />
        <Route path="/chat/:partyId" element={<Chat />} />
        <Route path="/me" element={<Profile />} />
      </Route>
      <Route element={<AuthLayout />}>
        <Route path="/login" element={<Login />} />
        <Route path="/signup" element={<Signup />} />
      </Route>
    </Routes>
  );
}

export default App;
