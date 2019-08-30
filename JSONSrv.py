# David Carlson & Bryce Martin
# ECE 4800 Senior Design Project

# Timer and HostController for fetching/pushing data to Xbee3 Network



# Builtin Webserver of python being used to serve/recieve JSON Payloads
# Inspiration for this code sourced from GitHub user: Nitaku
# Git gist: https://gist.github.com/nitaku/10d0662536f37a087e1b


# Additional class\library information for PythonHTTPServer:
# https://docs.python.org/3/library/http.server.html
#### From Python Documentaion: "http.server is not recommended for production. It only implements basic security checks." ####
from http.server import BaseHTTPRequestHandler, HTTPServer
import socketserver
import json
import cgi


class PiSrv(BaseHTTPRequestHandler):
    def set_header(self):
        self.send_response(200)
        self.send_header("Content-type", "application/json")
        self.end_headers()


    # PythonHTTPServer function name
    def do_HEAD(self):
        self.set_header()
        

    # GET sends back a Hello world message
    # PythonHTTPServer function name
    def do_GET(self):
        self.set_header()
        self.wfile.write(json.dumps({"hello": "world", "received": "ok"}).encode("utf-8"))
 



    # POST echoes the message adding a JSON field
    # PythonHTTPServer function name
    def do_POST(self):
        ctype, pdict = cgi.parse_header(self.headers.get_content_type())
        
        # refuse to receive non-json content
        if ctype != 'application/json':
            self.send_response(400)
            self.end_headers()
            self.wfile.write("Error: Expected \'application/json\' header.".encode("utf-8"))
            return
            
        # read the message and convert it into a python dictionary        
        length = int(self.headers.get("content-length"))
        print("hummmm:"+json.loads(json.dumps(self.rfile.read(length).decode('utf-8'))))
        message = json.loads(self.rfile.read(length))
        
        # add a property to the object, just to mess with data
        message["received"] = "ok"
        
        # send the message back
        self.set_header()
        self.wfile.write(json.dumps(message).encode("utf-8"))


# From Python documentaion: https://docs.python.org/3/library/http.server.html
def run(server_class=HTTPServer, handler_class=PiSrv, port=8008):
    server_address = ('', port)
    httpd = server_class(server_address, handler_class)
    print('Starting httpd on port ', port)
    httpd.serve_forever()

if __name__=='__main__':
    print('hello')
    server_class=HTTPServer
    run()