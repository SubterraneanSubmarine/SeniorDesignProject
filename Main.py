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
from time import sleep
import JSONSrv
import Edith
import Hal
import Jarvis


#TODO First Run:
#   Set all variables default
#   check for devies(?)
#       Check for Xbee coordinator + nodes
#   Plan this part out more
# TODO Consider using the python scheduling library?


# If a kill signal is sent to the program, inform the threads to stop execution
def signal_handler(sig, frame):
    print("Closing Program...")
    Jarvis.ProgramRunning = False  # Signal to all running threads to wrap it up!


if __name__ == '__main__':
    #TODO Parse cmdline args. {port, debug, etc}
    #TODO Create a help\useage output
    #TODO Check for dependances (pyserial, pygpio, etc)

    # Register our signal handerls for the program
    signal.signal(signal.SIGTERM, signal_handler)
    signal.signal(signal.SIGINT, signal_handler)

    print("Program Starting... (Ctrl + C to stop)")

    # Create our threads that will run the system. (Save handles/ID's to list/array)
    threads = []
    t1 = threading.Thread(target=Edith.SprinklerRunner)
    threads.append(t1)
    t2 = threading.Thread(target=Hal.TalkToXbee)
    threads.append(t2)
    t3 = threading.Thread(target=JSONSrv.run)  #TODO Redirect stdout to null unless DEBUG
    threads.append(t3)

    # Start the threads
    for thread in threads:
        thread.start()
    
    
    # If a kill signal is sent to the program...
    #       Then the signal handler will change ProgramRunning to False
    while Jarvis.ProgramRunning:
        sleep(5)

    # After a kill signal has changed the ProgramRunning variable to False
    #       We need to wait for all the threads to stop before letting main end
    for thread in threads:
        if thread.isAlive():
            thread.join()
    
    print("Program terminated.")
