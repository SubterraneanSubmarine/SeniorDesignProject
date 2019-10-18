'''
David Carlson & Bryce Martin
ECE 4800 Senior Design Project

This File represents the globally accesed variables/objects/data
interactions of the Pi and (Xbee and Relays)
This will also save/load stored data.

Tested in Python3.7 and 3.4(RPi)
'''

import threading

# TODO Outline what threads are modifying what data -- perhaps create separate mutex locks?
lock = threading.Lock()  # Mutex for threads to grab when changing values here

ProgramRunning = True

# Initial States of system

SystemEnabled = False
NewSensorData = False

avMoisture = 0
avWind = 0
avTemp = 0
avRain = 0
avHumid = 0


"""
David Carlson & Bryce Martin
ECE 4800 Senior Design Project

This File represents the globally accesed variables/objects/data
intractions of the Pi and (Xbee and Relays)
This will also save/load stored data.

Tested in Python3.7 and 3.4(RPi)
"""

import threading

# TODO Outline what threads are modifying what data -- perhaps create separate mutex locks?
lock = threading.Lock()  # Mutex for threads to grab when changing values here
ProgramRunning = True

# Initial States of system
SystemEnabled = False
NewSensorData = False


# Threshold values to prevent system from running
# "Name": [AvValue, CurrentSensorValue, TurnOffLimit]
Thresholds = {
    "Moisture": [avMoisture, 0, 33], #TODO what is the max value of our sensor? How does that translate to moisture in soil
    "Wind": [avWind, 0, 5], #TODO The anemometer will return a small voltage range: What will be less-than-ideal wind?
    "Temperature": [avTemp,0, 32], #TODO What unit/standard should we use here? Celcius?
    "Rain": [avRain, 0, 5] #TODO say... mm (milimeters)?
}


# "Day": Active[bool], StartTime[int], EndTime[int]  -> (military time)
# "Monday": [True, 0, 230] -> 0 == 12:00am, 230 == 2:30am, ... 2314 == 11:14pm
# TODO reset these to default values
TimerTriggering = {
    "Sunday": [False, 0, 100],
    "Monday": [False, 100, 230],
    "Tuesday": [False, 245, 1000],
    "Wednesday": [False, 1245, 1300],
    "Thursday": [False, 1610, 1645],
    "Friday": [False, 2345, 50],
    "Saturday": [False, 1700, 1800]
}


def set_new():
    with lock:
        ProgramRunning = True


def get_new():
    if ProgramRunning:
        with lock:
            ProgramRunning = False
        return True
    return False


SensorStats = [None] * 4
# TODO Ensure that the Main Coordinator is picked up in the SensorStats




# SensorStats = {}
# SensorStats = {'xbee1': {'Moisture': 5, 
#                             'Sunlight': 8, 
#                             'Battery': 99, 
#                             'Sector': 9, 
#                             'Iteration': 33333}, 
#                 'xbee2': {'Moisture': 5, 
#                             'Sunlight': 8, 
#                             'Battery': 99, 
#                             'Sector': 9, 
#                             'Iteration': 33333}, 
#                 'xbee3': {'Moisture': 5, 
#                             'Sunlight': 8, 
#                             'Battery': 99, 
#                             'Sector': 9, 
#                             'Iteration': 33333}}