'''
David Carlson & Bryce Martin
ECE 4800 Senior Design Project

This is the entry point for the RPi based
JSONServer, SprinklerControl, and Xbee3 interface

Tested in Python3.7 and 3.4(RPi)
'''
import threading
import signal
import sys
import os
import argparse
from datetime import datetime
from time import sleep
import subprocess
import JSONSrv
import scheduler
import xbeecom
import datalocker

# Here are some command-line options can be used for testing/help
parser = argparse.ArgumentParser(description="Smart Sprinkler Controller.", epilog="Senior Project by Bryce and David.")
parser.add_argument("--debug", help="Run Sprinker Controller in a debug mode.", action="store_true")
parser.add_argument("--debug_fake_data", help="Program will run using data contained within 'fakedata.py'. This argument defaults program into debug mode", action="store_true")
parser.add_argument("--port", help="Set port the JSONServer will listen on (must be greater than 1024).", )

# If a kill signal is sent to the program, inform the threads to stop execution
def signal_handler(sig, frame):
    print("Closing Program...")
    datalocker.ProgramRunning = False  # Signal to all running threads to wrap it up!

def printSensors():
    message = ""
    for sensor in datalocker.SensorStats:
        if sensor == None:
            continue
        message = message + "\n" + str(sensor)
    return message

def printWaterQue():
    message = ""
    if len(scheduler.watering_queue) > 0:
        for qued in scheduler.watering_queue:
            if qued == None:
                continue
            message = message + "\n" + str(qued)
    return message

USE_PORT = 8008
DEBUG_MODE = False
FAKE_DATA = False
if __name__ == '__main__':
    args = parser.parse_args()
    if args.debug:
        print("Running in Debug_Mode...")
        DEBUG_MODE = True
    if args.debug_fake_data:
        print("Running in Debug_Mode... with fake data")
        DEBUG_MODE = True
        FAKE_DATA = True
        import fakedata
        i = 0
        while i < len(datalocker.SensorStats):
            datalocker.SensorStats[i] = fakedata.SensorStats[i]
            i = i + 1
    if args.port:
        if int(args.port) < 1025:
            print("Error: Invalid port value")
            exit()
        USE_PORT = int(args.port)
    


    # Register our signal handerls for the program
    signal.signal(signal.SIGTERM, signal_handler)
    signal.signal(signal.SIGINT, signal_handler)
    
    if not DEBUG_MODE:
        print("Program Starting... (Ctrl + C to stop)")
        

    # Create our threads that will run the system. (Save handles/ID's to list/array)
    threads = []
    t1 = threading.Thread(target=scheduler.sprinkler_runner, kwargs={'DEBUG_MODE': DEBUG_MODE})
    threads.append(t1)
    t2 = threading.Thread(target=xbeecom.talk_to_xbee, kwargs={'DEBUG_MODE': DEBUG_MODE})
    threads.append(t2)
    t3 = threading.Thread(target=JSONSrv.run, kwargs={'port': USE_PORT, 'DEBUG_MODE': DEBUG_MODE})  #TODO Redirect stdout to null unless DEBUG
    threads.append(t3)

    # Start the threads
    for thread in threads:
        thread.start()

    # If a kill signal is sent to the program...
    #       Then the signal handler will change ProgramRunning to False
    userInputPrompt1 = "1: Set ProgramRunning-->True\n2: Set ProgramRunning-->False  (This will stop the program)\n3: Set SystemEnabled-->True\n4: Set SystemEnabled-->False\n5: Set timer_trigger...\n6: Set threshold...\n7: Set Date/Time...\n0: Clear Screen\n\nEnter Selection: "
    userInputPrompt2 = "1: Set ProgramRunning-->True\n2: Set ProgramRunning-->False  (This will stop the program)\n3: Set SystemEnabled-->True\n4: Set SystemEnabled-->False\n5: Set timer_trigger...\n6: Set threshold...\n7: Set Date/Time...\n8: Set sensor data...\n9: Signal NewData()\n0: Clear Screen\n\nEnter Selection: "
    debug_vals_set = True
    while datalocker.ProgramRunning:
        if DEBUG_MODE:
            os.system('clear')
            print("Date\\Time: ", datetime.now().strftime("%A %H%M"),"\tProgramRunning(", ("True" if datalocker.ProgramRunning else "False"), ")\tSystemEnabled(", ("True" if datalocker.SystemEnabled else "False"), ")\tJSONSrv Port: ", USE_PORT)
            print("-----------------------Current Thresholds-----------------------\n", datalocker.thresholds)
            print("-----------------------Current TimerTrigg-----------------------\n", datalocker.timer_triggering)
            print("-----------------------Current SensorStats-----------------------", printSensors())
            print("-----------------------Current WateringQue-----------------------", printWaterQue())
            print("-----------------------Node Health(s)-----------------------\n", datalocker.NodeHealthStatus)
            print("-----------------------Node LastSeen-----------------------\n", scheduler.last_seen)
            sys.stdout.flush()
            usr_input = input(userInputPrompt2 if FAKE_DATA else userInputPrompt1)
            try:
                usr_input = int(usr_input)
            except Exception as err:
                sys.stdout.flush()
            if usr_input == 1:
                datalocker.ProgramRunning = True
            elif usr_input == 2:
                datalocker.ProgramRunning = False
                DEBUG_MODE = False
            elif usr_input == 3:
                datalocker.SystemEnabled = True
            elif usr_input == 4:
                datalocker.SystemEnabled = False
            elif usr_input == 5:
                day = input("Enter a Day: ")
                boool = int(input("1(True) or 0(False): "))
                time = input("HHMM: ")
                datalocker.timer_triggering[day][0] = True if boool == 1 else False
                datalocker.timer_triggering[day][1] = int(time)
            elif usr_input == 6:
                a_key = input("Key: ")
                a_value = int(input("Value: "))
                datalocker.thresholds[a_key] = a_value
            elif usr_input == 7:
                set_time = input("Set RPi Time \"Wed, Oct 30 2019 14:14:00 MDT\": ")
                upDate = subprocess.Popen(["sudo", "date", "-s", set_time])
                upDate.communicate()
                upDate.wait()
            elif usr_input == 8 and FAKE_DATA:
                print("Enter new values for keys. Leave values blank for no change.")
                sector = input("Sector[0-3]: ")
                for item in datalocker.SensorStats[int(sector)]:
                    if item == "Sector": continue
                    default = datalocker.SensorStats[int(sector)][item]
                    usrinput = input("Set "+str(item)+" ("+str(default)+") to : ")
                    if usrinput:
                        datalocker.SensorStats[int(sector)][item] = usrinput
            elif usr_input == 9 and FAKE_DATA:
                datalocker.set_new()
            elif usr_input == 0:
                os.system('clear')
            else:
                print("Error. Try again.")

        else:
            sleep(5)
        
    # After a kill signal has changed the ProgramRunning variable to False
    #       We need to wait for all the threads to stop before letting main end
    for thread in threads:
        if thread.isAlive():
            thread.join()
            
    print("Program terminated.")
