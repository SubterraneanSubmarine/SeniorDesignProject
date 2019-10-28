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
import argparse
from time import sleep
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
    if args.port:
        if int(args.port) < 1025:
            print("Error: Invalid port value")
            exit()
        USE_PORT = int(args.port)
    


    # Register our signal handerls for the program
    signal.signal(signal.SIGTERM, signal_handler)
    signal.signal(signal.SIGINT, signal_handler)
    print("Program Starting... (Ctrl + C to stop)")

    if DEBUG_MODE:
        print("Type here to set/change variables of the system.")
        print("Available settings to change: number of nodes, current sensor/sample values, wind, temp, and humidity.")

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
    while datalocker.ProgramRunning:
        if DEBUG_MODE:
            # TODO print("We are in debug mode: Handle some user input")
            if args.debug_fake_data:
                print("---Initializing sensor values with passed in sensor file.---")
                # TODO Initialize variables using file
            # TODO get user input for variables -- change/update them as well if set by sensor file
        else:
            sleep(5)
        
    # After a kill signal has changed the ProgramRunning variable to False
    #       We need to wait for all the threads to stop before letting main end
    for thread in threads:
        if thread.isAlive():
            thread.join()
            
    print("Program terminated.")
