'''
David Carlson & Bryce Martin
ECE 4800 Senior Design Project

This File represents intractions of the Pi and Xbee3 coordinator (UART)

Tested in Python3.7(Windows) and 3.4(RPi)
'''

from sys import platform
if platform == "linux":
    import serial

import Jarvis


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
                port.read(8)  # ready 8 bits (?) # TODO double check on this
            # print("#TODO XBEE")
