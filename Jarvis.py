# David Carlson & Bryce Martin
# ECE 4800 Senior Design Project

# This File represents the globally accesed variables/objects/data
# intractions of the Pi and (Xbee and Relays)
# This will also save/load stored data.

# Tested in Python3.7 and 3.4(RPi)

import threading
lock = threading.Lock()

# Initial States of system

SystemEnabled = False
NewSensorData = False


# Threshold values to prevent system from running
# "Name": [CurrentSensorValue, TurnOffLimit]
Thresholds = {
    "Moisture": [0, 33], #TODO what is the max value of our sensor? How does that translate to moisture in soil
    "Wind": [0, 5], #TODO The anemometer will return a small voltage range: What will be less-than-ideal wind?
    "Temperature": [0, 32], #TODO What unit/standard should we use here? Celcius?
    "Rain": [0, 5] #TODO say... mm (milimeters)?
}


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


# List of Dictionaries
# [ {"key": value, ...}, {...}, ...]
SensorStats = [
#TODO get number of sensors from Xbee, and build this list up
# this is throw-away temp code
    {"MainController": 0,
    "Wind": 5,
    "Rain": 1,
    "Temperature": 29},
    {"Zone": 0,
     "Moisture": 37,
     "LightLevel": 28,
     "PowerLevel": 88},
    {"Zone": 1,
     "Moisture": 70,
     "LightLevel": 24,
     "PowerLevel": 75},
    {"Zone": 2,
     "Moisture": 20,
     "LightLevel": 30,
     "PowerLevel": 96}
]

#TODO OR do we merge the MainController into the list of Sensors?
MainController = {
    "Wind": 5,
    "Rain": 1,
    "Temperature": 29
}
