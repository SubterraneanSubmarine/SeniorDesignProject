'''
David Carlson & Bryce Martin
ECE 4800 Senior Design Project

This File represents intractions of the Pi and Sprinkler Relays (GPIO)
It will server as the timer program

Tested in Python3.7 and 3.4(RPi)
'''



from datetime import datetime  # TODO import these --> , timedelta, timezone
import time
from sys import platform
if platform == "linux":
    import gpiozero

import Jarvis

# TODO During initial setup from android app -- ask for time+timezone+dst! (or base everything off utc?)
# datetime.now(timezone(hours=-7))
# TODO Look into the possible ways of seeting the RPi's time in linux, rather than python?
# Perhaps we set the system with with python? -- or do we poll internet servers for time?
# TODO using temp sensor values recorded through out a day, alter/change water duration based on daytime temperatures
# TODO use moisture values/samples to dictate watering, instead of timmer stuff

def SprinklerRunner():
    while Jarvis.ProgramRunning:
        if Jarvis.SystemEnabled:
            if (Jarvis.TimerTriggering.get(datetime.now().strftime("%A"))[0]
                and int(datetime.now().strftime("%H%M")) >= Jarvis.TimerTriggering.get(datetime.now().strftime("%A"))[1]
                    and int(datetime.now().strftime("%H%M")) < Jarvis.TimerTriggering.get(datetime.now().strftime("%A"))[2]):
                # If [today is enabled] AND the time is between [start] and [end]: Turn on sprinklers
                print("Enable the GPIO for the correct zone!!!!")
