# David Carlson & Bryce Martin
# ECE 4800 Senior Design Project

# This is the entry point for the RPi based
# JSONServer, SprinklerControl, and Xbee3 interface

# Tested in Python3.7 and 3.4(RPi)

import JSONSrv


#TODO Question: Do I thread this?
#   One Thread hosts the webserver (and does it's thing)
#   2nd Thread monitors and runs the Sprinkler system+timer
#   3rd Thread does Xbee data collection/config

#TODO First Run:
#   Set all variables default
#   check for devies(?)
#       Check for Xbee coordinator + nodes
#   Plan this part out more



if __name__ == '__main__':
    #TODO Parse cmdline args.
    #TODO Create a help\useage output
    JSONSrv.run()
    print("Server Stopped.\n")