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
import datalocker
USE_PORT = 8008

# Possible http://raspberrypiserver:port/{AvailablePaths for interacting with the server}
AvailablePaths = [
    "/TimerControl/State/",
    "/TimerControl/DaysZonesTimes/",
    "/TimerControl/Thresholds/",
    "/Xbee3/Dump/",
    "/DateTime/"  # TODO code in the datetime elements: We need to be able to set and read the date/time of RPi from android app
                  #TODO Consider adding in TempDisable?
]

"""
Here, we define a class that takes a BaseHTTPRequestHandler
      With in this class, we define what the Handler/server will, and how, to respond.

The BaseHTTPRequestHandler has defined functions that we can expand upon
        The functions are the following
            do_HEAD
            do_GET
            do_POST
        They relate the the http/tcp request types of GET, POST, PUT, etc
"""

class PiSrv(BaseHTTPRequestHandler):
    # Header segment of an http/tcp packet -- informing a client what the data will be
    def set_header(self):
        self.send_response(200)
        self.send_header("Content-type", "application/json")
        self.end_headers()

    # PythonHTTPServer function name -- Define the header of our packet
    def do_HEAD(self):
        self.set_header()

    # PythonHTTPServer function name
    # GET simply responds to a request with data
    def do_GET(self):
        requestPath = self.path
        if requestPath in AvailablePaths:  # check if the requested 'page' is a valid path
            self.set_header()

            # We have a valid path requested, return the Data requested
            # In order to prevent the Android App from truncating any of our responses, 
            #       three tildes "~~~" will be appended to our transmitted message
            #       On the Android App side: The stream scanner will use a delimiter value of "~"
            #       Thus, ensuring* capture of our transmitted message
            if requestPath == AvailablePaths[0]:  # "/TimerControl/State/"
                self.wfile.write(json.dumps(datalocker.SystemEnabled).encode("utf-8") + "~~~".encode("utf-8"))

            if requestPath == AvailablePaths[1]: # "/TimerControl/DaysZonesTimes/"
                self.wfile.write(json.dumps(datalocker.TimerTriggering).encode("utf-8") + "~~~".encode("utf-8"))

            if requestPath == AvailablePaths[2]: # "/TimerControl/Thresholds/"
                self.wfile.write(json.dumps(datalocker.Thresholds).encode("utf-8") + "~~~".encode("utf-8"))

            if requestPath == AvailablePaths[3]: # "/Xbee3/Dump/"
                self.wfile.write(json.dumps(datalocker.SensorStats).encode("utf-8") + "~~~".encode("utf-8"))
            #TODO Get data from GPIO \ stored data and return in Dump

        else:
            self.send_error(400, "Unexpected Path")

            
    # PythonHTTPServer function name
    # POST will handle incoming data, and doing something with it
    def do_POST(self):
        # refuse to receive non-json content
        if self.headers.get_content_type() != 'application/json':
            self.send_error(400, "Expected \'application/json\' header")
            return

        requestPath = self.path
        if requestPath in AvailablePaths:  # Is the 'directory' requested one of the valid Paths?
            # read the message content and convert it into a python dictionary
            # Get the size of the packet body/payload (number of bytes)
            contentlength = int(self.headers.get("content-length"))
            if contentlength:  # if there is no data, Error!
                # Then, with the size of the payload, we read the bytes-data into an object
                serializedBodyData = self.rfile.read(
                    contentlength).decode("utf-8")
                bodyData = json.loads(serializedBodyData)  # Creates Dict data type
                # Now that we have the payload/body in an object, read the Path from the http request to determin how to handle the data
                # "/TimerControl/State/"
                if requestPath == AvailablePaths[0]:
                    if "State" in bodyData:
                        state = bodyData.get("State")
                        if state in ["True", "False"]:  # Check if the Value being sent is True or False
                            if state == "True":
                                with datalocker.lock:  # TODO Consider not using a lock -- only this thread touches this data
                                    datalocker.SystemEnabled = True
                            elif state == "False":
                                with datalocker.lock:
                                    datalocker.SystemEnabled = False
                            self.set_header()
                            # Send reply with current state of system
                            self.wfile.write(json.dumps(
                                datalocker.SystemEnabled).encode("utf-8"))
                        else:
                            self.send_error(
                                400, "Expected \'True or False\' value")
                    else:
                        self.send_error(400, "Expected \'State\' key")

                # "/TimerControl/DaysZonesTimes/"
                if requestPath == AvailablePaths[1]:
                    # Check to make sure there are no more than 7 days passed in
                    if bodyData.keys() <= datalocker.TimerTriggering.keys():
                        for key in bodyData.keys():
                            # Check to make sure we have the array of [bool, int, int] with our Key
                            if len(bodyData[key]) == 3:
                                if bool == type(bodyData[key][0]) and int == type(bodyData[key][1]) and int == type(bodyData[key][2]):
                                    # TODO Range-check the military integer values (?)
                                    with datalocker.lock:
                                        # We have the correct Data. Save it!
                                        datalocker.TimerTriggering[key] = bodyData[key]
                                else:
                                    self.send_error(
                                        400, "Expected value type error")
                            else:
                                self.send_error(400, "Expected 3 values")
                        # Send reply
                        self.set_header()
                        self.wfile.write(json.dumps(
                            datalocker.TimerTriggering).encode("utf-8"))
                    else:
                        self.send_error(400, "Expected Week\\Day key")

                # "/TimerControl/Thresholds/"
                if requestPath == AvailablePaths[2]:
                    # Ensure the incoming data is not bigger than the current data/object
                    if bodyData.keys() <= datalocker.Thresholds.keys():
                        for key in bodyData.keys():
                            if len(bodyData[key]) == 2:
                                if int == type(bodyData[key][1]):
                                    with datalocker.lock:
                                        datalocker.Thresholds[key][1] = bodyData[key][1]
                                else:
                                    self.send_error(
                                        400, "Expected value type error")
                            else:
                                self.send_error(400, "Expected 2 values")
                        self.set_header()
                        self.wfile.write(json.dumps(
                            datalocker.Thresholds).encode("utf-8"))
                    else:
                        self.send_error(400, "Expected Threshold keys")

                # "/Xbee3/Dump/"
                if requestPath == AvailablePaths[3]:  # Deny efforts to push data into the Xbee's
                    # TODO Do we want to try and post changes/data to the Xbee's?
                    self.send_error(400, "Post Not Available")
                #TODO TempDisable (?)
            else:
                self.send_error(400, "No Content")
        else:
            self.send_error(400, "Unexpected Path")

# From Python documentaion: https://docs.python.org/3/library/http.server.html
# Here, we pre-define several of our HTTPServer variables for the run function -- if run() is called without any arguments
def run(server_class=HTTPServer, handler_class=PiSrv, port=USE_PORT, DEBUG_MODE=False):
    # listen on any IP-Address\Interface but only on the given port
    server_address = ('', port)
    # Next, we call the server_call function (part of the python http.server library) with instructions on how to hand requests -- the PiSrv class dictates this
    RPiSrv = server_class(server_address, handler_class)
    RPiSrv.timeout = 0.5  # Do not block program/thread waiting for a request
    #RPiSrv.socket = ssl.wrap_socket(RPiSrv.socket, keyfile="key.pem", certfile="cert.pem", server_side=True)  # Oneday we could look into using SSL for the server
    print(' ADD TO DEBUG OUTPUT: Starting RPiSrv on port ', port)
    # Since this http.server is spawned on a thread, we will watch the ProgramRunning variable to know when to stop and shutdown the server.
    while datalocker.ProgramRunning:
        try:
            RPiSrv.handle_request()  # Wait 0.5 seconds for a request
        except:
            pass  # If there is a bad request, ignore it and start again
    RPiSrv.server_close()  # Close socket
