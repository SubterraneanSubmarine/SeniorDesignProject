"""
David Carlson & Bryce Martin
ECE 4800 Senior Design Project

This File represents interactions of the Pi and Sprinkler Relays (GPIO)
It will server as the timer program

Tested in Python3.7 and 3.4(RPi)
"""
from time import sleep
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
relays = [digitalio.DigitalInOut(board.D26), digitalio.DigitalInOut(board.D19),
          digitalio.DigitalInOut(board.D13), digitalio.DigitalInOut(board.D06)]

queue = []


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

                someval = []
                # TODO interate through Nodes, added low moisture nodes to que
                # TODO Do lots of checks on que -- if tests pass, start watering and remove from que
                
                # Manage the watering queue by evaluating the current weather conditions

                # If conditions are met start watering


                '''
                while wateringQue is not empty AND program is running:
                    if wind < fast:
                        while wind < hystersis value AND program is running AND water duration is not finished
                        if temp > veryHigh:
                            Water the lawn to cool it down   : Minaie wants a paper/reference for this
                        if light is dark and humidity high:
                            don't water == fungus among us
                        if 

                    
                    # Rotate through the list of Xbee nodes, and check which have a low moisture reading.
                        # When low, prep to do a watering of that sector
                        # If node is reported LowHealth, check default watering schedule

                    if Jarvis.avMoisture > 55555555:  #cumulative yard, or per-sector?
                        if CurrentWind > 5:
                            
                        
                '''
                


                # If [today is enabled] AND the CURRENT_TIME is between [start] and [end]: Turn on sprinklers
                if (Jarvis.TimerTriggering.get(datetime.now().strftime("%A"))[0]
                    and int(datetime.now().strftime("%H%M")) >= Jarvis.TimerTriggering.get(datetime.now().strftime("%A"))[1]
                        and int(datetime.now().strftime("%H%M")) < Jarvis.TimerTriggering.get(datetime.now().strftime("%A"))[2]):
                    # Then do this:
                    print("Enable the GPIO for the correct zone!!!!")

