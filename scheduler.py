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

wateringQue = []
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
            if len(wateringQue) > 0:
                for inQue in wateringQue:
                    current_time = int(datetime.now().strftime("%H%M"))
                    # TODO This about duration/fallback time -- doing it this way will result in erroneous durations...
                    # fallback_endTime = current_time + (datalocker.TimerTriggering.get(datetime.now().strftime("%A"))[2] - datalocker.TimerTriggering.get(datetime.now().strftime("%A"))[1])
                    if (inQue["Wind"] < wind_limit
                            # Short Circuit when it is Very Hot! # TODO: perhaps don't check this here, but after passing all the other tests (To decide how long to water the lawn)
                            and ((inQue["Temperature"] > temp_ceil and current_time in range(700, 2100)) 
                                    or ((current_time <= 700 or current_time >= 2100)  # Between 9:00pm and 7:00am
                                            and inQue["Humidity"] < humid_limit 
                                            and inQue["Sunlight"] < light_limit  #This feels redundant to the time check...
                                            and inQue["Temperature"] > temp_floor
                                        )
                                )
                        ):  # Then we have passed all tests, pop d'a que, start a'h wadderin'
                        # Do a watering cycle
                        while (datalocker.ProgramRunning and datalocker.SystemEnabled
                                # TODO Read the latest sensor data? OR Run for a User input time?  --  Xbee's will send updates every ______ 10? min
                                and datalocker.SensorStats[inQue["Sector"]]["Wind"] < wind_hystersis
                                and datalocker.SensorStats[inQue["Sector"]]["Moisture"] < moist_target
                                ):
                            relays[inQue["Sector"]].value = True
                        # Water cycle is complete, turn off sector
                        relays[inQue["Sector"]].value = False
                        # Remove sector from watering que
                        wateringQue.pop(inQue)

                    else:  # failed tests 
                        print("TODO")
                        # TODO check HealthChecks -- May going into fallback mode
                        """
                        # ___fallback mode___

                        # TODO This requires more thinking -- what if we are not within the correct time, do we go to the next que item? wait? Will all the looping eventually put us within the User specified watering timeframe?
                        

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