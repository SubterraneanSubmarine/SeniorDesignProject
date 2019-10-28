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



SensorStats = [None] * 4

# TODO Do we want to use this?
# Threshold values to prevent system from running
# "Name": [AvValue, CurrentSensorValue, TurnOffLimit]
Thresholds = {
    "Moisture": [123, 0, 33],  #TODO What is the MAX Value? How does that translate to moisture in soil
    "Wind": [321, 0, 10],  # TODO What is the MAX Value?
    "Humidity": [231, 0, 45]  # RH: Reletive Humidity ==> 45 is good
}
# wind_limit = 10  # m/s
# humid_limit = 45  # RH

def getMoistureFloor():
    return Thresholds["Moisture"][2]
def getWindLimit():
    return Thresholds["Wind"][2]
def getHumidLimit():
    return Thresholds["Humidity"][2]
def getWaterDuration():
    return 15  # watering_duration_minutes

# watering_duration_minutes = 15

# "Day": Active[bool], StartTime[int]  -> (military time)
# "Monday": [True, 230] -> 230 == 2:30am
TimerTriggering = {
    "Sunday": [False, 300],
    "Monday": [False, 300],
    "Tuesday": [False, 300],
    "Wednesday": [False, 300],
    "Thursday": [False, 300],
    "Friday": [False, 300],
    "Saturday": [False, 300],
    "Water Duration": 1200  # 1200 seconds == 20min
}

# Array of Dict.
SensorStats = [None] * 4


def set_new():
    with lock:
        ProgramRunning = True


def get_new():
    if ProgramRunning:
        with lock:
            ProgramRunning = False
        return True
    return False