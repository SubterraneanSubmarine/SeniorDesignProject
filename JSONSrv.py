# David Carlson & Bryce Martin
# ECE 4800 Senior Design Project

# This file contains the server for fetching/pushing data

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
# import ssl

import Jarvis


# Jarvis.TimerTriggering.get("DayOfWeek")[index] -> [True|False] | starttime | endtime
# Jarvis.TimerTriggering.get("DayOfWeek")[index] = newvalue

# Jarvise.SensorStats[index].get("Zone | Moisture | Tempurature | Power") = newvalue

# Jarvis.MainController.get("Wind | Rain | Tempurature") = newvalue


AvailablePaths = [
    "/TimerControl/State/",
    "/TimerControl/DaysZonesTimes/",
    "/TimerControl/Thresholds/",
    "/Xbee3/Dump/"
]


class PiSrv(BaseHTTPRequestHandler):
    def set_header(self):
        self.send_response(200)
        self.send_header("Content-type", "application/json")
        self.end_headers()

    # PythonHTTPServer function name
    def do_HEAD(self):
        self.set_header()
        

    # GET replys with data
    # PythonHTTPServer function name
    def do_GET(self):
        requestPath = self.path
        if requestPath in AvailablePaths:
            self.set_header()

            if requestPath == AvailablePaths[0]:  # "/TimerControl/State/"
                self.wfile.write(json.dumps(Jarvis.SystemEnabled).encode("utf-8"))

            if requestPath == AvailablePaths[1]: # "/TimerControl/DaysZonesTimes/"
                self.wfile.write(json.dumps(Jarvis.TimerTriggering).encode("utf-8"))

            if requestPath == AvailablePaths[2]: # "/TimerControl/Thresholds/"
                self.wfile.write(json.dumps(Jarvis.Thresholds).encode("utf-8"))

            if requestPath == AvailablePaths[3]: # "/Xbee3/Dump/"
                self.wfile.write(json.dumps(Jarvis.SensorStats).encode("utf-8"))
#TODO Get data from GPIO \ stored data

        else:
            self.send_error(400, "Unexpected Path")



    # POST echoes the message adding a JSON field
    # PythonHTTPServer function name
    def do_POST(self):
 
        # refuse to receive non-json content
        if self.headers.get_content_type() != 'application/json':
            self.send_error(400, "Expected \'application/json\' header")
            return

        requestPath = self.path
        if requestPath in AvailablePaths:
            # read the message content and convert it into a python dictionary
            contentlength = int(self.headers.get("content-length"))
            if contentlength:
                
                serializedBodyData = self.rfile.read(contentlength).decode("utf-8")
                bodyData = json.loads(serializedBodyData) # Creates Dict data type

                if requestPath == AvailablePaths[0]:  # "/TimerControl/State/"
                    if "State" in bodyData:
                        state = bodyData.get("State")
                        if state in ["True", "False"]:
                            if state == "True":
                                Jarvis.SystemEnabled = True
                            elif state == "False":
                                Jarvis.SystemEnabled = False
                            self.set_header()
                            # Send reply
                            self.wfile.write(json.dumps(
                                Jarvis.SystemEnabled).encode("utf-8"))
                        else:
                            self.send_error(
                                400, "Expected \'True or False\' value")
                    else:
                        self.send_error(400, "Expected \'State\' key")

                if requestPath == AvailablePaths[1]:  # "/TimerControl/DaysZonesTimes/"
                    if bodyData.keys() <= Jarvis.TimerTriggering.keys():
                        for key in bodyData.keys():
                            if len(bodyData[key]) == 3:
                                if bool == type(bodyData[key][0]) and int == type(bodyData[key][1]) and int == type(bodyData[key][2]):
                                    Jarvis.TimerTriggering[key] = bodyData[key]
                                else:
                                    self.send_error(
                                        400, "Expected value type error")
                            else:
                                self.send_error(400, "Expected 3 values")
                        # Send reply
                        self.set_header()
                        self.wfile.write(json.dumps(
                            Jarvis.TimerTriggering).encode("utf-8"))
                    else:
                        self.send_error(400, "Expected Week\\Day key")

                if requestPath == AvailablePaths[2]: # "/TimerControl/Thresholds/"
                    if bodyData.keys() <= Jarvis.Thresholds.keys():
                        for key in bodyData.keys():
                            if len(bodyData[key]) == 2:
                                if int == type(bodyData[key][1]):
                                    Jarvis.Thresholds[key][1] = bodyData[key][1]
                                else:
                                    self.send_error(
                                        400, "Expected value type error")
                            else:
                                self.send_error(400, "Expected 2 values")
                        self.set_header()
                        self.wfile.write(json.dumps(
                            Jarvis.Thresholds).encode("utf-8"))
                    else:
                        self.send_error(400, "Expected Threshold keys")

                if requestPath == AvailablePaths[3]: # "/Xbee3/Dump/"
                    self.send_error(400, "Post Not Available")
            else:
                self.send_error(400, "No Content")
#TODO Process the JSON payload      # message["received"] = "ok"
#TODO JSON for TempDisable (?)
            # Get data from GPIO \ stored data
            # Push data from message to GPIO \ store it for later pushing(?)

            
        else:
            self.send_error(400, "Unexpected Path")


# From Python documentaion: https://docs.python.org/3/library/http.server.html
def run(server_class=HTTPServer, handler_class=PiSrv, port=8008):
    # listen on any IP-Address\Interface but only on the given port
    server_address = ('', port)
    RPiSrv = server_class(server_address, handler_class)
    #RPiSrv.socket = ssl.wrap_socket(RPiSrv.socket, keyfile="key.pem", certfile="cert.pem", server_side=True)
    print('Starting RPiSrv on port ', port)
    try:
        RPiSrv.serve_forever()
    except KeyboardInterrupt:
        pass
    RPiSrv.shutdown()
    RPiSrv.server_close()


