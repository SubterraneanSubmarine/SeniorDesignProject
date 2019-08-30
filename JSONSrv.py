# David Carlson & Bryce Martin
# ECE 4800 Senior Design Project

# Timer and HostController for fetching/pushing data to Xbee3 Network

# Tested in Python3.7 and 3.4(RPi)

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
#TODO Outline/detail what information should be returned upon a get request
        # Get data from GPIO \ stored data
        self.wfile.write(json.dumps({"get": "request", "received": "ok"}).encode("utf-8"))
 



    # POST echoes the message adding a JSON field
    # PythonHTTPServer function name
    def do_POST(self):
        ctype, pdict = cgi.parse_header(self.headers.get_content_type())
        
        # refuse to receive non-json content
        if ctype != 'application/json':
            self.send_error(400, "Expected \'application/json\' header")
            return
            
        # read the message content and convert it into a python dictionary        
        body = self.rfile.read( int(self.headers.get("content-length")) ).decode("utf-8")
        message = json.loads(body)
        
#TODO Process the JSON payload      # message["received"] = "ok"
        # Get data from GPIO \ stored data
        # Push data from message to GPIO \ store it for later pushing(?)
        
        # Send reply
        self.set_header()
        self.wfile.write(json.dumps({"post": "request", "received": "ok"}).encode("utf-8"))



# From Python documentaion: https://docs.python.org/3/library/http.server.html
def run(server_class=HTTPServer, handler_class=PiSrv, port=8008):
    server_address = ('', port) #listen on any IP-Address\Interface but only on the given port
    RPiSrv = server_class(server_address, handler_class)
    print('Starting RPiSrv on port ', port)
    try:
        RPiSrv.serve_forever()
    except KeyboardInterrupt:
        pass
    RPiSrv.shutdown()
    RPiSrv.server_close()

if __name__=='__main__':
#TODO Parse cmdline args.
#TODO Create a help\useage output
    run()
    print("Server Stopped.\n")