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


if platform == "linux":
    relays = [digitalio.DigitalInOut(board.D26),  # Sector 1
          digitalio.DigitalInOut(board.D19),  # Sector 2
          digitalio.DigitalInOut(board.D13),  # Sector 3
          digitalio.DigitalInOut(board.D6)]  # Sector 4
else:
    class fakeIo():
        def __init__(self, pin):
            self.pin = pin
            self.enabled = False
            self.direction = False
        def direction(self, dir):
            self.direction = dir
        def value(self, en):
            self.enable = en
    relays = [fakeIo(0), fakeIo(1), fakeIo(2), fakeIo(3)]





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

start_time_Global = 0
light_avg = [0] * len(datalocker.SensorStats)
last_seen = [0] * len(datalocker.SensorStats)


def sprinkler_runner(DEBUG_MODE=False):
    start_time = 0
    global start_time_Global
    start_time_Global = start_time
    if DEBUG_MODE:
        print("# TODO")  # TODO
        if platform == "win32":
            print("sprklrnnr closing")
            return 0

    # Run thread as long as an interrupt isn't sent
    for relay in relays:
        if platform == "linux":
            relay.direction = digitalio.Direction.OUTPUT
        else:
            relay.direction = True
        relay.value = False

    while datalocker.ProgramRunning:
        # If the sprinkler system is enabled/on
        if datalocker.SystemEnabled:
            # Update the watering_queue and find missing sensors when a new Xbee packet is available
            if datalocker.get_new():
                print("New Data!")
                for sensor in datalocker.SensorStats:
                    if sensor is None:
                        continue

                    # Update last seen array
                    last_seen[sensor['Sector']] = sensor['Timestamp']

                    # Update light value arrays for every active sector
                    if datalocker.NodeHealthStatus[sensor['Sector']] != "Red":
                        light_avg.append(sensor['Sunlight'])

                    # Check for readings that are a day or older
                    if datetime.timestamp(datetime.now()) - last_seen[sensor['Sector']] > 86400:
                        print("Sector {}'s sensor has gone MIA!".format(sensor['Sector']))  # Inform the user
                        datalocker.NodeHealthStatus[sensor["Sector"]] = "Red"
                        # Note that this will execute every time new data is put into the sensorstats array
                    else:
                        datalocker.NodeHealthStatus[sensor["Sector"]] = "Green"

                    # If reading is less than user set threshold  # TODO -- How do we want to handle the threshold value?
                    if (sensor["Moisture"] < datalocker.thresholds["Dry"]
                        # AND not already in the wateringQue
                            and not next((item for item in watering_queue if item["Sector"] == sensor["Sector"]), False)):
                        # then add the sensor to the wateringQue
                        watering_queue.append(sensor)

                light_floor = sum(light_avg) / len(light_avg)
                light_floor = light_floor - 500  # I've set the number to 500 as the standard deviation will always flag one sector  
                                                # (Quick/Dirty stdev:  (max-min) / 4) ==> 4096 / 4 = 1024
                light_avg.clear()

                # Iterate through array once more to find outliers in light readings
                for sensor in datalocker.SensorStats:
                    if sensor is None:
                        continue
                    if sensor['Sunlight'] < light_floor:
                        print("Sector {}'s light is low compared to peers; It may need to be moved")
                        datalocker.NodeHealthStatus["Sensor"] = "Yellow"

            # Manage the watering queue by evaluating the current weather conditions
            # If conditions are met start watering, however fallback schedules are checked first
            if not start_time:
                current_time = int(datetime.now().strftime("%H%M"))
                day = days_of_week[datetime.today().weekday()]
                iterator = 0
                while iterator < len(datalocker.NodeHealthStatus):
                    if datalocker.NodeHealthStatus[iterator] == "Red" and datalocker.timer_triggering[day][0] and current_time == datalocker.timer_triggering[day][1]:
                        relays[iterator].value = True
                        start_time = datetime.timestamp(datetime.now())
                        start_time_Global = start_time
                        watering_queue.append({'Sector': iterator})
                        print("MIA watering started")
                    iterator = iterator + 1

                if (len(watering_queue) > 0
                    and watering_queue[0]["Wind"] < datalocker.thresholds["Wind max"]
                    and (datalocker.thresholds["Prohibited time end"] <= current_time <= 2359
                         or current_time <= datalocker.thresholds["Prohibited time start"])
                    and watering_queue[0]["Humidity"] < datalocker.thresholds["Humidity max"]
                    and watering_queue[0]["Temperature"] > datalocker.thresholds["Temperature min"]
                    ):  # Then we have passed all tests

                    print("Sector: ", watering_queue[0]["Sector"], " watering has started.")
                    relays[watering_queue[0]["Sector"]].value = True
                    start_time = datetime.timestamp(datetime.now())
                    start_time_Global = start_time


            if (start_time and datetime.timestamp(datetime.now()) - start_time) > (datalocker.thresholds["Water Duration"]):
                relays[watering_queue[0]["Sector"]].value = False
                print("Sector: ", watering_queue[0]["Sector"], " watering has ended.")
                watering_queue.pop(0)
                start_time = 0
                start_time_Global = start_time


if __name__ == '__main__':
    datalocker.ProgramRunning = True
    datalocker.SystemEnabled = True
    while True:
        sprinkler_runner()