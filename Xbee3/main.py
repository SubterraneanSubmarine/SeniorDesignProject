'''
Bryce Martin and David Carlson
ECE 4800 - Senior Project

Code pulled from Digi manual for XBee3 quick setup/start
This file defines how a XBEE NODE behaves
'''


import xbee, time
# Set the identifying string of the radio
xbee.atcmd("NI", "Router")
# Configure some basic network settings
network_settings = {"CE": 0, "ID": 0xABCD, "EE": 0}  # "command": value pairs
for command, value in network_settings.items():
    xbee.atcmd(command, value)

xbee.atcmd("AC")  # Apply changes
time.sleep(1)

# Query AI until it reports success
print("Connecting to network, please wait...")
while xbee.atcmd("AI") != 0:
    time.sleep(0.1)

print("Connected to Network")
operating_network = ["OI", "OP", "CH"]
print("Operating network parameters:")
for cmd in operating_network:
    print("{}: {}".format(cmd, xbee.atcmd(cmd)))

increment = 0
while True:
    xbee.transmit(xbee.ADDR_COORDINATOR, "Iteration: " + str(increment))
    increment = increment + 1
    time.sleep(15)