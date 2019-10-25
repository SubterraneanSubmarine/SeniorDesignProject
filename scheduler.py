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
wind_limit = 10  # m/s
humid_limit = 45  # RH
light_limit = 10
temp_floor = 6  # C
moist_target = datalocker.Thresholds["Moisture"][2] + 10  # TODO Do we want to hit a target moisture level
moist_floor = datalocker.Thresholds["Moisture"][2]
start_time = 0

wateringQueue = []


def sprinkler_runner():
    # Run thread as long as an interrupt isn't sent
    for relay in relays:
        relay.direction = digitalio.Direction.OUTPUT
        relay.value = False

    while datalocker.ProgramRunning:
        # If the sprinkler system is enabled/on
        if datalocker.SystemEnabled:
            # When the moisture of an area falls below the threshold add to watering queue
            if datalocker.get_new():
                for sensor in datalocker.SensorStats:
                    # If reading is less than user set threshold  # TODO -- How do we want to handle the threshold value?
                    if (sensor["Moisture"] < moist_floor
                        # AND not already in the wateringQue
                            and not next((inQue for inQue in wateringQue if inQue["Sector"] == sensor["Sector"]), False)):
                        # add the sensor to the wateringQue
                        wateringQue.append(sensor)
                
            # TODO Do lots of checks on que -- if tests pass, start watering and remove from que
            # Manage the watering queue by evaluating the current weather conditions
            # If conditions are met start watering
            if len(wateringQue) > 0 and not start_time:
                current_time = int(datetime.now().strftime("%H%M"))
                # TODO This about duration/fallback time -- doing it this way will result in erroneous durations...
                # fallback_endTime = current_time + (datalocker.TimerTriggering.get(datetime.now().strftime("%A"))[2] - datalocker.TimerTriggering.get(datetime.now().strftime("%A"))[1])
                if (wateringQue[0]["Wind"] < wind_limit
                    and current_time <= 700 or current_time >= 2100  # Between 9:00pm and 7:00am
                    and wateringQue[0]["Humidity"] < humid_limit
                    and wateringQue[0]["Temperature"] > temp_floor):
                    relays[wateringQue[0]["Sector"]].value = True
                    start_time = datetime.timestamp(datetime.now())

                else:  # failed tests
                    print("TODO")

            if datetime.timestamp(datetime.now()) - start_time > (datalocker.watering_duration_minutes * 60):
                relays[wateringQue[0]["Sector"]].value = False
                wateringQue.pop(0)
                start_time = 0
