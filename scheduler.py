"""
David Carlson & Bryce Martin
ECE 4800 Senior Design Project

This File represents interactions of the Pi and Sprinkler Relays (GPIO)
It will server as the timer program

Tested in Python3.7 and 3.4(RPi)
"""
from time import sleep
from datetime import datetime
from sys import platform
from collections import deque
import datalocker
import busio
import digitalio
import board


# TODO During initial setup from android app -- ask for time+timezone+dst! (or base everything off utc?)
# datetime.now(timezone(hours=-7))
# TODO Look into the possible ways of setting the RPi's time in linux, rather than python?
# Perhaps we set the system with with python? -- or do we poll internet servers for time?
# TODO using temp sensor values recorded throughout a day, alter/change water duration based on daytime temperatures
# TODO use moisture values/samples to dictate watering, instead of timmer stuff


        # will will also need to collect sensor values from the connected wind and temp/humid sensors (put this on a thread?)
relays = [digitalio.DigitalInOut(board.D26),  # Sector 1   # TODO Do this I/O's correlate to the sectors of the system?
          digitalio.DigitalInOut(board.D19),  # Sector 2
          digitalio.DigitalInOut(board.D13),  # Sector 3
          digitalio.DigitalInOut(board.D06)]  # Sector 4


def log_data(payload):
    with open("log.csv", 'a') as file_out:
        file_out.write("{}/{}/{}, {}:{}:{}, {}, {}, {}, {}, {}, {}, {}".format(
            str(payload['Year']), str(payload['Month']), str(payload['Day']),
            str(payload['Hour']), str(payload['Minute']), str(payload['Second']),
            str(payload['Sector']), str(payload['Moisture']), str(payload['Sunlight']),
            str(payload['Battery']), str(payload['Temperature']), str(payload['Humidity']),
            str(payload['Wind'])
        ))
    file_out.close()



# TODO, determine all of these threshold values  --  use all of them?
wind_limit = 6  # m/s
wind_hystersis = 8
humid_limit = 45  # RH
humid_hystersis = 50
light_limit = 10
light_hystersis = 15
temp_ceil = 42  # C
temp_floor = 6  # C
moist_target = datalocker.Thresholds["Moisture"][2] + 10  # TODO Do we want to hit a target moisture level
moist_floor = datalocker.Thresholds["Moisture"][2]

"""
User can set fallback times/durations
User can set default watering duration -- Moisture trigger --> water for X mins (15min)
Stretch goal: User edits wind limits
User can adjust humidity threshold
User can adjust light settings

"""

wateringQue = []
def sprinkler_runner():
    # Run thread as long as an interrupt isn't sent
    for relay in relays:
        relay.direction = digitalio.Direction.OUTPUT
        relay.value = False

    # Initialize our history lists
    i = 0
    while i < datalocker.HISTORY_LENGTH:
        datalocker.NodeLastSeen_Light[i] = [None] * datalocker.HISTORY_LENGTH
        datalocker.NodeLastSeen_Time[i] = [None] * datalocker.HISTORY_LENGTH
        i = i + 1
    i = 0

    while datalocker.ProgramRunning:
        # If the sprinkler system is enabled/on
        if datalocker.SystemEnabled:
            # When the moisture of an area falls below the threshold add to watering queue
            current_time = int(datetime.timestamp(datetime.now()))
            if datalocker.get_new():
                number_of_sensors = datalocker.SensorCount()
                for sensor in datalocker.SensorStats:
                    if sensor is None: continue
                    # If reading is less than user set threshold  # TODO -- How do we want to handle the threshold value?
                    if (sensor["Moisture"] < moist_floor
                        # AND not already in the wateringQue
                        and not next((inQue for inQue in wateringQue if inQue["Sector"] == sensor["Sector"]), False)):
                        # then add the sensor to the wateringQue
                        wateringQue.append(sensor)
                    

                    # Do HealthCheck // compare light level of nodes to an average of each other. Record last update as well
                    

                    # If this is the first time we have seen this node, initialize the history
                    # The 'Sector' will be the index to the history list
                    if datalocker.NodeLastSeen_Light[sensor["Sector"]][datalocker.HISTORY_LENGTH - 1] is None:
                        while i < datalocker.HISTORY_LENGTH:
                            datalocker.NodeLastSeen_Light[sensor["Sector"]][i] = sensor["Light"]
                            datalocker.NodeLastSeen_Time[sensor["Sector"]][i] = current_time
                            i = i + 1
                        i = 0                       
                    else:
                        # We have seen this node before, Update our history
                        datalocker.NodeLastSeen_Light[sensor["Sector"]].pop(0)
                        datalocker.NodeLastSeen_Time[sensor["Sector"]].pop(0)
                        datalocker.NodeLastSeen_Light[sensor["Sector"]].append(sensor["Light"])
                        datalocker.NodeLastSeen_Time[sensor["Sector"]].append(current_time)

                        # Values have been updated. Ready for HealthCheck
                        # This loop we are in will allow us to compare each available sensor to its neighbors
                        avLight_of_otherNodes = 0
                        avTime_of_otherNodes = 0
                        avLight_of_currNode = 0
                        avTime_of_currNode = 0
                        count = 0
                        while i < number_of_sensors:
                            if i != sensor["Sector"]:
                                # Here we average the Light level of the neighbors together
                                avLight_of_otherNodes = avLight_of_otherNodes + sum(datalocker.NodeLastSeen_Light[i]) / datalocker.HISTORY_LENGTH

                                # Here is the   average difference   between check-in's by the neighbors
                                #   ((Newest Check-in) - (Oldest Check-in))   divided by...
                                #     ...((# of samples) - ( 1 ))             will equal the average time between updates
                                avTime_of_otherNodes = avTime_of_otherNodes + (datalocker.NodeLastSeen_Time[i][datalocker.HISTORY_LENGTH - 1] - datalocker.NodeLastSeen_Time[i][0]) / (datalocker.HISTORY_LENGTH - 1)
                                count = count + 1
                            else:
                                # Make an average for the sensor/node to check
                                avLight_of_currNode = sum(datalocker.NodeLastSeen_Light[i]) / datalocker.HISTORY_LENGTH
                                avTime_of_currNode = (datalocker.NodeLastSeen_Time[i][datalocker.HISTORY_LENGTH - 1] - datalocker.NodeLastSeen_Time[i][0]) / (datalocker.HISTORY_LENGTH - 1)
                            i = i + 1
                        i = 0
                        if count == 0: count = 1
                        # Divide the summation of neighbor nodes by their count
                        avLight_of_otherNodes = avLight_of_otherNodes / count
                        avTime_of_otherNodes = avTime_of_otherNodes / count


                        # If a Node has missed reporting for X days OR has low health
                        # seconds in a day: 86400
                        # in two days: 172800
                        # four days: 345600
                        # TODO: Instead of an averaged time difference between reports, do we just compare the more recent report to the current time?
                        # Do we want to make sure the Sector is added to the wateringQue?



                        # Next we check the differences and set our alert value: RED, YELLOW, GREEN
                        # IF node-light is less than neighbors AND has longer check-in time --> RED
                        if (avLight_of_currNode < avLight_of_otherNodes 
                            and avTime_of_currNode > avTime_of_otherNodes):

                            datalocker.NodeHealthStatus[sensor["Sector"]] = "Red"

                            # TODO: Keep this wateringQue addition here?
                            # if the Sector is NOT already in the wateringQue, we will add it
                            if not next((inQue for inQue in wateringQue if inQue["Sector"] == sensor["Sector"]), False):
                                wateringQue.append(sensor)

                        # IF it is NOT BOTH --> YELLOW
                        elif (avLight_of_currNode < avLight_of_otherNodes 
                              or avTime_of_currNode > avTime_of_otherNodes):
                            datalocker.NodeHealthStatus[sensor["Sector"]] = "Yellow"
                        # Otherwise we have a healthy node --> GREEN
                        else:
                            datalocker.NodeHealthStatus[sensor["Sector"]] = "Green"


                
            # TODO Do lots of checks on que -- if tests pass, start watering and remove from que
            # Manage the watering queue by evaluating the current weather conditions
            # If conditions are met start watering
            if len(wateringQue) > 0:
                # TODO This about duration/fallback time -- doing it this way will result in erroneous durations...
                # fallback_endTime = current_time + (datalocker.TimerTriggering.get(datetime.now().strftime("%A"))[2] - datalocker.TimerTriggering.get(datetime.now().strftime("%A"))[1])
                if (wateringQue[0]["Wind"] < wind_limit
                    and (current_time <= 700 or current_time >= 2100)  # Between 9:00pm and 7:00am
                    and wateringQue[0]["Humidity"] < humid_limit 
                    and wateringQue[0]["Temperature"] > temp_floor       
                    ):  # Then we have passed all tests, pop d'a que, start a'h wadderin'
                    
                    # Do a watering cycle
                    while (datalocker.ProgramRunning and datalocker.SystemEnabled
                            # TODO Read the latest sensor data? OR Run for a User input time?  --  Xbee's will send updates every ______ 10? min
                            and datalocker.SensorStats[wateringQue[0]["Sector"]]["Wind"] < wind_hystersis
                            and datalocker.SensorStats[wateringQue[0]["Sector"]]["Moisture"] < moist_target
                            ):
                        relays[inQue["Sector"]].value = True
                    # Water cycle is complete, turn off sector
                    relays[inQue["Sector"]].value = False
                    # Remove sector from watering que
                    wateringQue.pop(0)

                else:  # failed tests 
                    # if the Wind is down AND Today is Enabled AND we are at the user specified start time
                    if (wateringQue[0]["Wind"] < wind_limit
                        and datalocker.TimerTriggering.get(datetime.now().strftime("%A"))[0]
                        and int(datetime.now().strftime("%H%M")) >= datalocker.TimerTriggering.get(datetime.now().strftime("%A"))[1]):
                        # Start watering
                        

                    """
                    
                    

                    # If [today is enabled] AND the CURRENT_TIME is between [start] and [end]: Turn on sprinklers  --- ALSO: What is 'today'? 9:00pm to 7:00am?, or the imperical decree of the users watering desire?
                    if (datalocker.TimerTriggering.get(datetime.now().strftime("%A"))[0]
                        and int(datetime.now().strftime("%H%M")) >= datalocker.TimerTriggering.get(datetime.now().strftime("%A"))[1]
                            and int(datetime.now().strftime("%H%M")) < datalocker.TimerTriggering.get(datetime.now().strftime("%A"))[2]):
                        # Then do this:
                        # TODO Have user provide a fall-back watering duration
                        while (datalocker.ProgramRunning 
                                and datalocker.SystemEnabled
                                and ):
                        print("Enable the GPIO for the correct zone!!!!")
                    """