#!/usr/bin/env python3
import itertools
import json
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

counter = itertools.count(1)


class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        request_id = next(counter)
        time.sleep((request_id % 20) * 0.002)
        status = 503 if request_id % 10 == 0 else 200
        body = json.dumps({"requestId": request_id, "status": status}).encode()
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def log_message(self, *_):
        pass


if __name__ == "__main__":
    print("Demo server: http://127.0.0.1:18082/test")
    ThreadingHTTPServer(("127.0.0.1", 18082), Handler).serve_forever()
