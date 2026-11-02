#!/usr/bin/env python3
"""Local-only CHATENDER desktop bridge. API keys stay in this process, not the browser."""
import getpass
import json
import os
import threading
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen

HOST = "127.0.0.1"
PORT = 8765
ENDPOINT = "https://openrouter.ai/api/v1/chat/completions"
MAX_BODY = 96 * 1024
MAX_MESSAGES = 24
MAX_TEXT = 4000

ROLE_INSTRUCTIONS = {
    "general": "Answer accurately and concisely. State uncertainty when information is missing.",
    "coding": "Act as a careful senior software engineer. Explain assumptions, produce tested code, and prioritize security.",
    "diagnosis": "Diagnose device or software problems step by step. Warn before destructive actions. Never claim to have inspected a device unless data was supplied.",
    "memory": "Organize and retrieve only user-approved information provided in this conversation. Be clear about uncertainty and never invent memories."
}

class Handler(SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=os.path.dirname(os.path.abspath(__file__)), **kwargs)

    def log_message(self, fmt, *args):
        # Avoid logging chat content, request bodies, or credentials.
        print("%s - %s" % (self.address_string(), fmt % args))

    def send_json(self, status, payload):
        raw = json.dumps(payload).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(raw)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        self.wfile.write(raw)

    def do_GET(self):
        if self.path == "/api/status":
            self.send_json(200, {"ok": True, "provider": "openrouter"})
            return
        if self.path == "/":
            self.path = "/index.html"
        super().do_GET()

    def do_POST(self):
        if self.path != "/api/chat":
            self.send_json(404, {"error": "Not found"})
            return
        try:
            length = int(self.headers.get("Content-Length", "0"))
            if length <= 0 or length > MAX_BODY:
                self.send_json(413, {"error": "Request is empty or too large."})
                return
            data = json.loads(self.rfile.read(length).decode("utf-8"))
            role = data.get("role", "general")
            model = data.get("model", "openrouter/free")
            messages = data.get("messages", [])
            if role not in ROLE_INSTRUCTIONS:
                self.send_json(400, {"error": "Unknown assistant role."})
                return
            if not isinstance(model, str) or not model.strip() or len(model) > 160:
                self.send_json(400, {"error": "Invalid model preference."})
                return
            if not isinstance(messages, list) or not messages or len(messages) > MAX_MESSAGES:
                self.send_json(400, {"error": "Send between 1 and 24 conversation messages."})
                return
            safe_messages = [{"role": "system", "content": ROLE_INSTRUCTIONS[role]}]
            for item in messages:
                if not isinstance(item, dict) or item.get("role") not in ("user", "assistant"):
                    continue
                content = item.get("content", "")
                if not isinstance(content, str) or not content.strip():
                    continue
                safe_messages.append({"role": item["role"], "content": content[:MAX_TEXT]})
            if len(safe_messages) == 1:
                self.send_json(400, {"error": "No valid message content was supplied."})
                return

            request_data = json.dumps({
                "model": model.strip(),
                "messages": safe_messages,
                "temperature": 0.7,
                "max_tokens": 900
            }).encode("utf-8")
            req = Request(ENDPOINT, data=request_data, headers={
                "Authorization": "Bearer " + API_KEY,
                "Content-Type": "application/json",
                "HTTP-Referer": "http://127.0.0.1:%d" % PORT,
                "X-Title": "CHATENDER AI Desktop"
            }, method="POST")
            try:
                with urlopen(req, timeout=55) as response:
                    result = json.loads(response.read().decode("utf-8"))
            except HTTPError as exc:
                detail = exc.read(4096).decode("utf-8", "replace")
                try:
                    detail = json.loads(detail).get("error", {}).get("message", "Provider request failed.")
                except Exception:
                    detail = "Provider request failed."
                self.send_json(502, {"error": "OpenRouter error (%s): %s" % (exc.code, str(detail)[:500])})
                return
            except (URLError, TimeoutError) as exc:
                self.send_json(502, {"error": "Could not reach OpenRouter: %s" % str(exc)[:250]})
                return

            answer = result.get("choices", [{}])[0].get("message", {}).get("content", "")
            if not isinstance(answer, str) or not answer.strip():
                self.send_json(502, {"error": "The provider returned an empty answer."})
                return
            self.send_json(200, {"answer": answer})
        except (ValueError, UnicodeDecodeError):
            self.send_json(400, {"error": "Invalid request data."})
        except Exception as exc:
            self.send_json(500, {"error": "Local bridge error: %s" % str(exc)[:250]})

if __name__ == "__main__":
    API_KEY = os.environ.get("OPENROUTER_API_KEY", "").strip()
    if not API_KEY:
        API_KEY = getpass.getpass("Enter your OpenRouter API key (hidden, not saved): ").strip()
    if not API_KEY:
        raise SystemExit("No API key supplied. Nothing was started.")
    print("CHATENDER AI desktop bridge")
    print("Local address: http://%s:%d" % (HOST, PORT))
    print("API key stays in this running process. Press Ctrl+C to stop.")
    server = ThreadingHTTPServer((HOST, PORT), Handler)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nStopping CHATENDER AI.")
    finally:
        server.server_close()
