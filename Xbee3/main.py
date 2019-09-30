'''
Bryce Martin and David Carlson
ECE 4800 - Senior Project

Code pulled from Digi manual for XBee3 quick setup/start
This file defines how a XBEE NODE behaves
'''


import xbee, time
from machine import Pin, ADC
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


# Here would be code that reads the dip switches and assigns the Zone
dip_pin_read_0 = 0
dip_pin_read_1 = 1  # read the 'MSB' 1st
zone = 0                                           # zone == [00...0000] == 0
zone |= dip_pin_read_1 << 1 | dip_pin_read_0 << 0  # zone == [00...0010] == 2

# Here would be code that reads from the Moisture Sensor (or whatever)
pin30 = ADC("D1")  # Pin 30 set as ADC
pin30.read()  # reads an ADC Value: 0 -- 4095 (3.3V / 4095)
# pin4 = Pin("D4", Pin.IN, value=0)  # Pin 23 == DIO4, value == pull down, direction == in
# pin4.value()  # return 0 or 1 for high/low input value
increment = 0


'''
Please format any messages using the following style
(because, I've dissected the payload using this formatting)
'''
# This yields a payload like this:  'payload': b'{'Iteration': 0, 'Value': 345, 'Zone': 2}'
message = ("{'Iteration': " + str(increment)
         + ", 'ADCRead': " + str(pin30.read())
         + ", 'Zone': " + str(zone) + "}")  # Multiline to read easier?



# xbee.transmit(xbee.ADDR_COORDINATOR, message)
while True:
    # xbee.transmit(xbee.ADDR_COORDINATOR, "Iteration: " + str(increment)
    #                                + ", \"ADC Read: \"" + str(pin30.read())
    #               )
    xbee.transmit(xbee.ADDR_COORDINATOR, message)
    message = ("{'Iteration': " + str(increment)
               + ", 'ADCRead': " + str(pin30.read())
               + ", 'Zone': " + str(zone) + "}")  # Multiline to read easier?
    increment = increment + 1
    time.sleep(120)