import { useEffect, useMemo, useRef, useState } from "react";
import "./App.css";

const API_URL = "http://localhost:8080/api/v1/ollama/chat";

export default function App() {
  const [systemMessage, setSystemMessage] = useState("請用繁體中文回答。");
  const [input, setInput] = useState("");
  const [messages, setMessages] = useState([{ role: "assistant", content: "嗨！你想聊什麼？" }]);
  const [streaming, setStreaming] = useState(false);

  const [memoryId, setMemoryId] = useState(() => makeUUID());

  const [typewriter, setTypewriter] = useState(true);

  const abortRef = useRef(null);
  const bottomRef = useRef(null);

  const charQueueRef = useRef([]);
  const typingTimerRef = useRef(null);

  const canSend = useMemo(() => input.trim().length > 0 && !streaming, [input, streaming]);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages, streaming]);

  async function sendMessage() {
    const userText = input.trim();
    if (!userText || streaming) return;

    setInput("");
    setMessages((prev) => [...prev, { role: "user", content: userText }]);
    setMessages((prev) => [...prev, { role: "assistant", content: "" }]);
    setStreaming(true);

    abortRef.current?.abort();
    const controller = new AbortController();
    abortRef.current = controller;

    charQueueRef.current = [];
    stopTypingLoop();

    try {
      const res = await fetch(API_URL, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "Accept": "text/event-stream",
        },
        body: JSON.stringify({
          memoryId,
          systemMessage,
          userMessage: userText,
        }),
        signal: controller.signal,
      });

      if (!res.ok) {
        const err = await safeReadText(res);
        throw new Error(`HTTP ${res.status}\n${err}`);
      }
      if (!res.body) throw new Error("res.body is null (no stream)");

      const reader = res.body.getReader();
      const decoder = new TextDecoder("utf-8");

      let buffer = "";

      while (true) {
        const { value, done } = await reader.read();
        if (done) break;

        buffer += decoder.decode(value, { stream: true });

        const events = buffer.split("\n\n");
        buffer = events.pop() ?? "";

        for (const evt of events) {
          const data = parseSseData(evt);
          if (data === "") continue;

          if (typewriter) {
            enqueueTyping(data);
          } else {
            appendAssistant(data);
          }
        }
      }

      // flush
      const last = parseSseData(buffer);
      if (last) {
        if (typewriter) enqueueTyping(last);
        else appendAssistant(last);
      }
    } catch (e) {
      const msg = e?.name === "AbortError" ? "（已停止）" : (e?.message ?? String(e));
      appendAssistant(`\n\n[錯誤] ${msg}`);
    } finally {
      setStreaming(false);
      if (typewriter) startTypingLoop();
    }
  }

  function stopStreaming() {
    abortRef.current?.abort();
  }

  function appendAssistant(text) {
    setMessages((prev) => {
      const next = [...prev];
      for (let i = next.length - 1; i >= 0; i--) {
        if (next[i].role === "assistant") {
          next[i] = { ...next[i], content: (next[i].content ?? "") + text };
          break;
        }
      }
      return next;
    });
  }

  function enqueueTyping(text) {
    for (const ch of text) charQueueRef.current.push(ch);
    startTypingLoop();
  }

  function startTypingLoop() {
    if (typingTimerRef.current) return;

    typingTimerRef.current = setInterval(() => {
      const n = 2;

      if (charQueueRef.current.length === 0) {
        stopTypingLoop();
        return;
      }

      const out = charQueueRef.current.splice(0, n).join("");
      appendAssistant(out);
    }, 15); 
  }

  function stopTypingLoop() {
    if (typingTimerRef.current) {
      clearInterval(typingTimerRef.current);
      typingTimerRef.current = null;
    }
  }

  function onKeyDown(e) {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      sendMessage();
    }
  }

  return (
    <div className="page">
      <aside className="sidebar">
        <div className="brand">Agent Chat</div>

        <div className="label">Memory ID（同一對話固定）</div>
        <div className="memory">{memoryId}</div>

        <div className="label">System Message</div>
        <textarea
          className="system"
          value={systemMessage}
          onChange={(e) => setSystemMessage(e.target.value)}
        />

        <div className="row">
          <label className="toggle">
            <input
              type="checkbox"
              checked={typewriter}
              onChange={(e) => setTypewriter(e.target.checked)}
              disabled={streaming}
            />
            打字機效果
          </label>
        </div>

        <div className="small">
          API：<code>{API_URL}</code>
        </div>

        <button
          className="btn secondary"
          onClick={() => {
            stopStreaming();
            setMessages([{ role: "assistant", content: "嗨！你想聊什麼？" }]);
            setMemoryId(makeUUID());
          }}
        >
          新對話 / 清空
        </button>
      </aside>

      <main className="main">
        <div className="chat">
          {messages.map((m, idx) => (
            <div key={idx} className={`msg ${m.role}`}>
              <div className="avatar">{m.role === "user" ? "你" : "AI"}</div>
              <div className="bubble">
                {m.content || (m.role === "assistant" && streaming ? "…" : "")}
              </div>
            </div>
          ))}
          <div ref={bottomRef} />
        </div>

        <div className="composer">
          <textarea
            className="input"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={onKeyDown}
            placeholder="輸入訊息（Enter 送出、Shift+Enter 換行）"
            disabled={streaming}
          />
          <div className="actions">
            <button className="btn" onClick={sendMessage} disabled={!canSend}>
              送出
            </button>
            <button className="btn secondary" onClick={stopStreaming} disabled={!streaming}>
              停止
            </button>
          </div>
        </div>
      </main>
    </div>
  );
}

function parseSseData(rawEventBlock) {
  const lines = rawEventBlock.split("\n");
  const dataLines = [];

  for (const line of lines) {
    if (line.startsWith("data:")) {
      dataLines.push(line.slice(5));
    }
  }

  return dataLines.join("\n");
}

async function safeReadText(res) {
  try {
    return await res.text();
  } catch {
    return "";
  }
}

function makeUUID() {
  if (crypto?.randomUUID) return crypto.randomUUID();
  return "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0;
    const v = c === "x" ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
}
