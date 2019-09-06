# David Carlson & Bryce Martin
# ECE 4800 Senior Design Project

# This File represents intractions of the Pi and (Xbee and Relays)
# This will also save/load stored data.

# Tested in Python3.7 and 3.4(RPi)

from sys import platform
if platform == "linux":
    import gpiozero
    import pyserial


# Initial States of system

SystemEnabled = False

# "Day": Active[bool], StartTime[int], EndTime[int]  -> (military time)
# "Monday": [True, 0, 230] -> 0 == 12:00am, 230 == 2:30am, ... 2314 == 11:14pm
#TODO reset these to default values
TimerTriggering = {
    "Sunday": [False, 0, 100],
    "Monday": [False, 100, 230],
    "Tuesday": [False, 245, 1000],
    "Wednesday": [False, 1245, 1300],
    "Thursday": [False, 1610, 1645],
    "Friday": [False, 2345, 50],
    "Saturday": [False, 1700, 1800]
}


#TODO Talk with Bryce: Should we use classes/objects instead of complex varibles?  (Are they really that much differnet?)

# List of Dictionaries
# [ {"key": value, ...}, {...}, ...]
SensorStats = [
    #TODO get number of sensors from Xbee, and build this list up
    # this is throw-away temp code
    {"Zone": 0,
     "Moisture": 37,
     "Temperature": 28,
     "Power Level": 88},
    {"Zone": 1,
     "Moisture": 70,
     "Temperature": 24,
     "Power Level": 75},
    {"Zone": 2,
     "Moisture": 20,
     "Temperature": 30,
     "Power Level": 96}
]

#TODO OR do we merge the MainController into the list of Sensors?
MainController = {
    "Wind": 5,
    "Rain": 1,
    "Temperature": 29
}
