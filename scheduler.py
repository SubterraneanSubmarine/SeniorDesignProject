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
if platform == "linux":
    import busio
    import digitalio
    import board


# TODO During initial setup from android app -- ask for time+timezone+dst! (or base everything off utc?)
# datetime.now(timezone(hours=-7))
# TODO Look into the possible ways of setting the RPi's time in linux, rather than python?
# Perhaps we set the system with with python? -- or do we poll internet servers for time?
# TODO using temp sensor values recorded throughout a day, alter/change water duration based on daytime temperatures
# TODO use moisture values/samples to dictate watering, instead of timmer stuff



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


temp_floor = 6  # C
start_time = 0
wateringQue = []


relays = [digitalio.DigitalInOut(board.D26),  # Sector 1
          digitalio.DigitalInOut(board.D19),  # Sector 2
          digitalio.DigitalInOut(board.D13),  # Sector 3
          digitalio.DigitalInOut(board.D06)]  # Sector 4

watering_queue = []
days_of_week = [
    "Monday",
    "Tuesday",
    "Wednesday",
    "Thursday",
    "Friday",
    "Saturday",
    "Sunday"
]
light_avg = [0] * len(datalocker.SensorStats)
last_seen = [0] * len(datalocker.SensorStats)
mia = [False] * len(datalocker.SensorStats)


def sprinkler_runner(DEBUG_MODE=False):
    if DEBUG_MODE:
        print("# TODO")  # TODO
        if platform == "win32":
            print("Sprinkler runner closing")
            return 0

    # Run thread as long as an interrupt isn't sent
    for relay in relays:
        relay.direction = digitalio.Direction.OUTPUT
        relay.value = False

    while datalocker.ProgramRunning:
        # If the sprinkler system is enabled/on
        if datalocker.SystemEnabled:
            # Update the watering_queue and find missing sensors when a new Xbee packet is available
            if datalocker.get_new():
                for sensor in datalocker.SensorStats:
                    if sensor is None:
                        continue

                    # Update last seen array
                    last_seen[sensor['Sector']] = sensor['Timestamp']

                    # Update light value arrays for every active sector
                    if not mia[sensor['Sector']]:
                        light_avg.append(sensor['Sunlight'])

                    # Check for readings that are a day or older
                    if datetime.timestamp(datetime.now()) - last_seen[sensor['Sector']] > 86400:
                        print("Sector {}'s sensor has gone MIA!".format(sensor['Sector']))  # Inform the user
                        mia[sensor['Sector']] = True
                        # Note that this will execute every time new data is put into the sensorstats array
                    else:
                        mia[sensor['Sector']] = False

                        # If reading is less than user set threshold  # TODO -- How do we want to handle the threshold value?
                        if (sensor["Moisture"] < datalocker.thresholds["Dry"]
                            # AND not already in the wateringQue
                                and not next((for item in watering_queue if item["Sector"] == sensor["Sector"]), False)):
                            # then add the sensor to the wateringQue
                            watering_queue.append(sensor)

                light_floor = sum(light_avg) / len(light_avg)
                light_floor = light_limit - 500 # I've set the number to 500 as the standard deviation will always flag one sector

                # Iterate through array once more to find outliers in light readings
                for sensor in datalocker.SensorStats:
                    if sensor['Sunlight'] < light_floor:
                        print("Sector {}'s light is low compared to peers; It may need to be moved")

            # Manage the watering queue by evaluating the current weather conditions
            # If conditions are met start watering, however fallback schedules are checked first
            if not start_time:
                current_time = int(datetime.now().strftime("%H%M"))
                day = days_of_week[datetime.today().weekday()]
                iterator = 0
                while iterator < len(mia):
                    if mia[iterator] and datalocker.timer_triggering[day][0] and current_time == datalocker.timer_triggering[day][1]:
                        relays[iterator].value = True
                        start_time = datetime.timestamp(datetime.now())

                if (len(watering_queue) > 0
                    and watering_queue[0]["Wind"] < datalocker.thresholds["Wind max"]
                    and datalocker.thresholds["Prohibited time end"] <= current_time <= datalocker.thresholds["Prohibited time start"]
                    and watering_queue[0]["Humidity"] < datalocker.thresholds["Humidity max"]
                    and watering_queue[0]["Temperature"] > datalocker.thresholds["Temperature min"]
                    ):  # Then we have passed all tests

                    relays[watering_queue[0]["Sector"]].value = True
                    start_time = datetime.timestamp(datetime.now())

            if (datetime.timestamp(datetime.now()) - start_time) > (datalocker.thresholds["Water Duration"]):
                relays[watering_queue[0]["Sector"]].value = False
                watering_queue.pop(0)
                start_time = 0
