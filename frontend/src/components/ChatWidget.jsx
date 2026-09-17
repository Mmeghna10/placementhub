import { useState, useRef, useEffect } from 'react';
import { MessageCircle, X, Send } from 'lucide-react';
import { sendChatMessage } from '../api/chatApi';

export default function ChatWidget() {
  const [open, setOpen] = useState(false);
  const [messages, setMessages] = useState([
    { role: 'assistant', content: "Hi! I'm your PlacementHub guide. Ask me anything." },
  ]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const bottomRef = useRef(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, open]);

  const handleSend = async (e) => {
    e.preventDefault();
    if (!input.trim() || loading) return;

    const userMessage = { role: 'user', content: input.trim() };
    const updated = [...messages, userMessage];
    setMessages(updated);
    setInput('');
    setLoading(true);

    try {
      const conversation = updated.slice(1); // drop the canned greeting
      const res = await sendChatMessage(conversation);
      setMessages((prev) => [...prev, { role: 'assistant', content: res.data.reply }]);
    } catch (err) {
      setMessages((prev) => [
        ...prev,
        { role: 'assistant', content: "Sorry, I couldn't reach the assistant just now. Try again in a moment." },
      ]);
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      <button
        onClick={() => setOpen(!open)}
        className="fixed bottom-6 right-6 w-14 h-14 rounded-full bg-ink text-paper flex items-center justify-center shadow-lg hover:bg-ink/90 transition z-50"
        aria-label="Open guide chat"
      >
        {open ? <X size={22} /> : <MessageCircle size={22} />}
      </button>

      {open && (
        <div className="fixed bottom-24 right-6 w-[340px] max-w-[calc(100vw-3rem)] h-[460px] bg-white border border-ink/10 rounded-lg shadow-xl flex flex-col z-50">
          <div className="bg-ink px-4 py-3 rounded-t-lg">
            <p className="font-display text-sm font-semibold text-paper">PlacementHub Guide</p>
          </div>

          <div className="flex-1 overflow-y-auto px-4 py-3 space-y-3">
            {messages.map((m, i) => (
              <div
                key={i}
                className={`max-w-[85%] px-3 py-2 rounded-lg text-sm leading-relaxed ${
                  m.role === 'user'
                    ? 'ml-auto bg-teal text-paper'
                    : 'bg-paper text-ink border border-ink/10'
                }`}
              >
                {m.content}
              </div>
            ))}
            {loading && (
              <div className="bg-paper text-slate border border-ink/10 max-w-[60%] px-3 py-2 rounded-lg text-sm font-mono">
                …
              </div>
            )}
            <div ref={bottomRef} />
          </div>

          <form onSubmit={handleSend} className="border-t border-ink/10 p-3 flex gap-2">
            <input
              type="text"
              value={input}
              onChange={(e) => setInput(e.target.value)}
              placeholder="Ask a question…"
              className="flex-1 px-3 py-2 text-sm bg-paper border border-ink/15 rounded-md focus:outline-none focus:ring-2 focus:ring-teal/40"
            />
            <button
              type="submit"
              disabled={loading}
              className="w-9 h-9 flex items-center justify-center bg-ink text-paper rounded-md hover:bg-ink/90 transition disabled:opacity-50"
            >
              <Send size={16} />
            </button>
          </form>
        </div>
      )}
    </>
  );
}