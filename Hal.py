'''
David Carlson & Bryce Martin
ECE 4800 Senior Design Project

This File represents intractions of the Pi and Xbee3 coordinator (UART)

Tested in Python3.7(Windows) and 3.4(RPi)
'''

from sys import platform
if platform == "linux":
    import serial
import json

import Jarvis


# The Xbee (under MicroPython API) forwards a byte object/string that
# needs to be formated before we can manipulate it as a data object
# We are 'JSON'ifying the data
def ConvertToDict(string_or_bytes):
    temp = str(string_or_bytes, "utf-8")
    # Here is what the Xbee will send:
    # {'profile': 49413, 'dest_ep': 232, 
    #     'broadcast': False, 'sender_nwk': 38204,
    #     'source_ep': 232, 'payload': b'Iteration: 12', 
    #     'sender_eui64': b'\x00\x13\xa2\x00A\x99O\xcc',    ### NOTE This is the MAC Addr: A = 41, O = 4F
    #     'cluster': 17}
    temp = temp.replace("\'", "\"").replace("b\"", "\"").replace("\\", "\\\\").replace("False", "false").replace("True", "true")
    temp = json.loads(temp)
    return temp

def TalkToXbee():
    if platform == "linux":
        try:
            # May want to change the pi to use ttyAMA0 instead of miniUART (ttyS0)
            port = serial.Serial("/dev/ttyS0")
            # Default settings: 8bits, no parity, 1 stopbit, baud 9600
        except:
            # Will the port fail opening? -- yes, when sudo piviledges are required
            # rather / instead: We need to config Pi port permissions...
            pass
        while Jarvis.ProgramRunning:
            if port.inWaiting() > 0:
                temp = ConvertToDict(port.readline())
                for key, value in temp.items():
                    print(key, " : ", value)
                # TODO Take this object, check if the reporting node already exists in our 'database.' Do stuff with it
