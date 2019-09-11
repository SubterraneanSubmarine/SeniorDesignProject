'''
Bryce Martin and David Carlson
ECE 4800 - Senior Project

Code pulled from Digi manual for XBee3 quick setup/start
This file defines how a XBEE COORDINATOR behaves
'''


import xbee, time
# Set the identifying string of the radio
xbee.atcmd("NI", "Coordinator")
# Configure some basic network settings
network_settings = {"CE": 1, "ID": 0xABCD, "EE": 0, "NJ": 0xFF}
for command, value in network_settings.items():
    xbee.atcmd(command, value)

xbee.atcmd("AC") # Apply changes
time.sleep(1)

while xbee.atcmd("AI") != 0:
    time.sleep(0.1)

print("Network Established")
operating_network = ["OI", "OP", "CH"]
print("Operating network parameters:")
for cmd in operating_network:
    print("{}: {}".format(cmd, xbee.atcmd(cmd)))

while True:
    print(xbee.receive())  # This only prints the body/payload on the serial port!!!
    time.sleep(10)

