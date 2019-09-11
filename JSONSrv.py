'''
David Carlson & Bryce Martin
ECE 4800 Senior Design Project

This file contains the server for fetching/pushing data

Tested in Python3.7 and 3.4(RPi)

Builtin Webserver of python being used to serve/recieve JSON Payloads
Inspiration for this code sourced from GitHub user: Nitaku
Git gist: https://gist.github.com/nitaku/10d0662536f37a087e1b
'''


# Additional class\library information for PythonHTTPServer:
# https://docs.python.org/3/library/http.server.html
#### From Python Documentaion: "http.server is not recommended for production. It only implements basic security checks." ####
from http.server import BaseHTTPRequestHandler, HTTPServer
import socketserver
import json
# import ssl

import Jarvis


AvailablePaths = [
    "/TimerControl/State/",
    "/TimerControl/DaysZonesTimes/",
    "/TimerControl/Thresholds/",
    "/Xbee3/Dump/",
    "/DateTime/"  # TODO code in the datetime elements: We need to be able to set and read the date/time of RPi from android app
                  #TODO Consider adding in TempDisable
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
            #TODO Get data from GPIO \ stored data and return in Dump

        else:
            self.send_error(400, "Unexpected Path")


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

                serializedBodyData = self.rfile.read(
                    contentlength).decode("utf-8")
                bodyData = json.loads(serializedBodyData)  # Creates Dict data type

                # "/TimerControl/State/"
                if requestPath == AvailablePaths[0]:
                    if "State" in bodyData:
                        state = bodyData.get("State")
                        if state in ["True", "False"]:
                            if state == "True":
                                with Jarvis.lock:
                                    Jarvis.SystemEnabled = True
                            elif state == "False":
                                with Jarvis.lock:
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

                # "/TimerControl/DaysZonesTimes/"
                if requestPath == AvailablePaths[1]:
                    if bodyData.keys() <= Jarvis.TimerTriggering.keys():
                        for key in bodyData.keys():
                            if len(bodyData[key]) == 3:
                                if bool == type(bodyData[key][0]) and int == type(bodyData[key][1]) and int == type(bodyData[key][2]):
                                    # TODO Range-check the military integer values (?)
                                    with Jarvis.lock:
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

                # "/TimerControl/Thresholds/"
                if requestPath == AvailablePaths[2]:
                    if bodyData.keys() <= Jarvis.Thresholds.keys():
                        for key in bodyData.keys():
                            if len(bodyData[key]) == 2:
                                if int == type(bodyData[key][1]):
                                    with Jarvis.lock:
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

                # "/Xbee3/Dump/"
                if requestPath == AvailablePaths[3]:
                    self.send_error(400, "Post Not Available")

                #TODO TempDisable (?)
            else:
                self.send_error(400, "No Content")
        else:
            self.send_error(400, "Unexpected Path")


# From Python documentaion: https://docs.python.org/3/library/http.server.html
def run(server_class=HTTPServer, handler_class=PiSrv, port=8008):
    # listen on any IP-Address\Interface but only on the given port
    server_address = ('', port)
    RPiSrv = server_class(server_address, handler_class)
    RPiSrv.timeout = 0.5  # Do not block program/thread waiting for a request

    #RPiSrv.socket = ssl.wrap_socket(RPiSrv.socket, keyfile="key.pem", certfile="cert.pem", server_side=True)
    print(' ADD TO DEBUG OUTPUT: Starting RPiSrv on port ', port)

    while Jarvis.ProgramRunning:
        try:
            RPiSrv.handle_request()  # Wait 0.5 seconds for a request
        except:
            pass  # If there is a bad request, ignore it and start again
    RPiSrv.server_close()  # Close socket


