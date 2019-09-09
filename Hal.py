# David Carlson & Bryce Martin
# ECE 4800 Senior Design Project

# This File represents intractions of the Pi and Xbee3 coordinator (UART)

# Tested in Python3.7 and 3.4(RPi)

from sys import platform
if platform == "linux":
    import pyserial

import Jarvis


def TalkToXbee():
    print("#TODO XBEE")