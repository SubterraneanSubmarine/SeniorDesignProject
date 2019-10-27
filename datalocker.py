"""
David Carlson & Bryce Martin
ECE 4800 Senior Design Project

This File represents the globally accesed variables/objects/data
interactions of the Pi and (Xbee and Relays)
This will also save/load stored data.

Tested in Python3.7 and 3.4(RPi)
"""

import threading

# TODO Outline what threads are modifying what data -- perhaps create separate mutex locks?
lock = threading.Lock()  # Mutex for threads to grab when changing values here

ProgramRunning = True
SystemEnabled = False
NewSensorData = False

SensorStats = [None] * 4

# "Day": Active[bool], StartTime[int]-> (military time)
# "Monday": [True, 0, 230] -> 0 == 12:00am, 230 == 2:30am, ... 2314 == 11:14pm
# TODO reset these to default values
timer_triggering = {
    "Sunday": [False, 0],
    "Monday": [False, 0],
    "Tuesday": [False, 0],
    "Wednesday": [False, 0],
    "Thursday": [False, 0],
    "Friday": [False, 0],
    "Saturday": [False, 0]
}

thresholds = {
    "Dry": 2000,  # The range for the soil is about 0.8 V to 2.6 V
    "Wind max": 10,  # Anemometer range -> 0 to 32.4 m/s (0.4 V to 2 V)
    "Humidity max": 50,
    "Temperature min": 8,
    "Prohibited time start": 1100,
    "Prohibited time end": 1800,
    "Water Duration": 900
}


def set_new():
    with lock:
        NewSensorData = True


def get_new():
    if ProgramRunning:
        with lock:
            NewSensorData = False
        return True
    return False
