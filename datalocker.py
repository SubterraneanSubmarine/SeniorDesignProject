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


def set_new():
    with lock:
        NewSensorData = True


def get_new():
    if ProgramRunning:
        with lock:
            NewSensorData = False
        return True
    return False


# TODO Do we want to use this?
# Threshold values to prevent system from running
# "Name": [AvValue, CurrentSensorValue, TurnOffLimit]
Thresholds = {
    "Moisture": [avMoisture, 0, 33], #TODO what is the max value of our sensor? How does that translate to moisture in soil
    "Wind": [avWind, 0, 5], #TODO The anemometer will return a small voltage range: What will be less-than-ideal wind?
    "Temperature": [avTemp,0, 32] #TODO What unit/standard should we use here? Celcius?
}


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

def SensorCount():
    return len(SensorStats)

HISTORY_LENGTH = 5
NodeLastSeen_Light = [None] * HISTORY_LENGTH
NodeLastSeen_Time = [None] * HISTORY_LENGTH
NodeHealthStatus = [None] * HISTORY_LENGTH