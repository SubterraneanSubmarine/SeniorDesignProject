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

# Query AI until it reports success
while xbee.atcmd("AI") != 0:
    time.sleep(0.1)

print("Network Established")
operating_network = ["OI", "OP", "CH"]
print("Operating network parameters:")
for cmd in operating_network:
    print("{}: {}".format(cmd, xbee.atcmd(cmd)))

# TODO Get/Read and send values about sensors connected to the coordinator to the RPi
# And keep the formatting the same (if possible?)
'''
Please format any messages using the following style
(because, I've dissected the payload using this formatting)

# This yields a payload like this:  'payload': b'{'Iteration': 0, 'Value': 345, 'Zone': 2}'
message = ("{'WindMeter': " + str(WindValue)
         + ", 'ADCRead': " + str(pin30.read())
         + ", 'Zone': " + str(zone) + "}")  # Multiline to read easier?
'''
temp = {}
while True:
    temp = xbee.receive()
    if temp:
        # for key, value in temp.items():
        print(temp)
        temp.clear()

