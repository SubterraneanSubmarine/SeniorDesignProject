# David Carlson & Bryce Martin
# ECE 4800 Senior Design Project

# This File represents intractions of the Pi and Sprinkler Relays (GPIO)
# It will server as the timer program

# Tested in Python3.7 and 3.4(RPi)
# 


from sys import platform
if platform == "linux":
    import gpiozero

from datetime import datetime
import time
import Jarvis

def SprinklerRunner():
    # print(datetime.now().time())
    # print(time.time())
    print("#TODO TIMER")
