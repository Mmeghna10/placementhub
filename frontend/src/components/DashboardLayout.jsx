import Navbar from './Navbar';
import ChatWidget from './ChatWidget';

export default function DashboardLayout({ children }) {
  return (
    <div className="min-h-screen bg-paper bg-[radial-gradient(ellipse_80%_40%_at_50%_-10%,rgb(20_85_90/0.07),transparent)]">
      <Navbar />
      <main className="max-w-6xl mx-auto px-6 py-10 animate-fade-up">{children}</main>
      <ChatWidget />
    </div>
  );
}
