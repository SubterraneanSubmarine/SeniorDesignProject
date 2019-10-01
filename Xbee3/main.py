"""
Bryce Martin and David Carlson

ECE 4800 - Senior Project

Network setup pulled from Digi manual for XBee3 quick setup/start

This code runs on the Xbee3 acting as a router sending the data it
collects to the coordinator. Pins that collect data are labeled.
"""

import xbee
import time
from machine import Pin, ADC

# SLEEP_DURATION = 500
SLEEP_DURATION = 10

SELF = xbee.XBee()
# Set the identifying string of the radio
xbee.atcmd("NI", "Sensor Probe")

# Configure some basic network settings
# "CE must be 0 before SM can be set to a value greater than 0 to change the device to an end device"
network_settings = {"ID": 0xABCD, "EE": 0, "SM": 6, "AV": 2}
# "CE": 0

for command, value in network_settings.items():
    xbee.atcmd(command, value)
xbee.atcmd("AC")  # Apply changes
time.sleep(1)

# Query AI until it reports success
print("Connecting to network, please wait...")
while xbee.atcmd("AI") != 0:
    time.sleep_ms(100)
print("Connected to Network\n")

operating_network = ["OI", "OP", "CH"]
print("Operating network parameters:")
for cmd in operating_network:
    print("{}: {}".format(cmd, xbee.atcmd(cmd)))

# Pin Setup
tilt_switch = Pin("D8", Pin.IN, Pin.PULL_DOWN)
sw_bit_0 = Pin("P0", Pin.IN, Pin.PULL_DOWN)
sw_bit_1 = Pin("P1", Pin.IN, Pin.PULL_DOWN)
# Sleep disable pin logic should be flipped to maintain consistency
sleep_disable = Pin("P2", Pin.IN, Pin.PULL_UP)
moisture_sensor_power = Pin("P5", Pin.OUT)
moisture_probe = ADC("D3")
light_sensor = ADC("D2")

iteration = 0

while True:
    # Aggregate data
    if tilt_switch.value():

        # Evaluate current iteration number
        iteration += 1

        # Evaluate dip switch positions
        zone = 0x3 & (sw_bit_1.value() << 1) | (sw_bit_0.value())

        # Read data from moisture probe
        moisture_sensor_power.on()
        time.sleep_ms(100)
        reading = moisture_probe.read()
        # take a moisture measurement here
        # Average data here
        moisture_sensor_power.off()

        # Evaluate ambient light in area
        ambiance = light_sensor.read()

        # Evaluate voltage of power supply rail
        battery = xbee.atcmd("%V")

        try:
            print("Iteration: " + str(iteration) +
                  "\nSector: " + str(zone) +
                  "\nMoisture: " + str(reading) +
                  "\nSunlight: " + str(ambiance) +
                  "\nBattery: " + str(battery) +
                  "\n\n")
            xbee.transmit(xbee.ADDR_COORDINATOR,
                          "{'Iteration': " + str(iteration) +
                          ", 'Sector': " + str(zone) +
                          ", 'Moisture': " + str(reading) +
                          ", 'Sunlight': " + str(ambiance) +
                          ", 'Battery': " + str(battery) + "}"
                          )
        except Exception as err:
            print(err)

    # Deep sleep
    # In order for the device to sleep it must be an end node
    # We need to have a way to stop the device from sleeping as it
    # makes reconnecting to the device almost impossible
    if sleep_disable.value():
        time.sleep(SLEEP_DURATION)
    else:
        SELF.sleep_now(SLEEP_DURATION, pin_wake=False)
