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

SLEEP_DURATION = 300 * 1000  # In seconds between soil samples
AVERAGE_SAMPLES = 3  # Number of soil samples to be taken before transmission
LOW_VOLT_THRESH = 2300  # Voltage floor before sleep duration time is increased.
LOW_LIGHT_THRESH = 4000  # Photoresistor ceiling before sleep duration time is increased.
SLEEP_MULTIPLIER = 2  # Sleep duration will be multiplied by this number if one of the above conditions are met


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
moisture_sensor_power = Pin("P5", Pin.OUT)
moisture_probe = ADC("D3")
light_sensor = ADC("D2")

# Unused Pins are set as outputs to reduce sleep current
Pin("D1", Pin.OUT)
Pin("D4", Pin.OUT)
Pin("D6", Pin.OUT)
Pin("D7", Pin.OUT)
Pin("D9", Pin.OUT)
tilt_power = Pin("P6", Pin.OUT)  # XCTU Sets this pin Pin.OUT & Pin.Pull_UP for DC Tilt_Switch_Power
Pin("P7", Pin.OUT)
Pin("P8", Pin.OUT)
Pin("P9", Pin.OUT)

iteration = 0  # Iteration is used to count number of samples before a packet is sent
light_average = 0  # A running total for the average light (Low numbers imply bright)
moisture_average = 0  # A running total for the average moisture (Low numbers imply dry)
battery = 0  # Battery readings are kept between iterations of the loop for evaluation
ambiance = 0  # Light readings are also kept between loop iterations for evaluation

while True:
    # Aggregate data
    tilt_power.on()
    time.sleep_ms(10)
    sleep_enable = Pin("P2", Pin.IN, Pin.PULL_DOWN)
    print("PreTilt"+str(tilt_switch.value()))
    if tilt_switch.value():
        print("PostTilt")
        sw_bit_0 = Pin("P0", Pin.IN, Pin.PULL_DOWN)
        sw_bit_1 = Pin("P1", Pin.IN, Pin.PULL_DOWN)

        # Evaluate current iteration number
        iteration += 1

        if iteration > AVERAGE_SAMPLES:
            moisture = moisture_average / AVERAGE_SAMPLES
            ambiance = light_average / AVERAGE_SAMPLES

            # Zero out iteration and running average variables
            iteration = 0
            moisture_average = 0
            light_average = 0

            # Evaluate voltage of power supply rail
            battery = xbee.atcmd("%V")
            # Evaluate dip switch positions
            zone = 0x3 & (sw_bit_1.value() << 1) | (sw_bit_0.value())

            try:
                print("Sector: " + str(zone) +
                      "\nMoisture: " + str(moisture) +
                      "\nSunlight: " + str(ambiance) +
                      "\nBattery: " + str(battery) +
                      "\n")
                xbee.transmit(xbee.ADDR_COORDINATOR,
                              (", 'Sector': " + str(zone) +
                               ", 'Moisture': " + str(moisture) +
                               ", 'Sunlight': " + str(ambiance) +
                               ", 'Battery': " + str(battery) + "}")
                              )
            except Exception as err:
                print(err)
        else:
            print("else")
            # Read data from moisture probe
            moisture_sensor_power.on()
            time.sleep_ms(100)
            moisture_average += moisture_probe.read()
            moisture_sensor_power.off()

            # Evaluate ambient light in area
            light_average += light_sensor.read()

        sw_bit_0 = Pin("P0", Pin.OUT)
        sw_bit_1 = Pin("P1", Pin.OUT)

    # Sleep duration evaluation
    sleep = SLEEP_DURATION
    if ambiance > LOW_LIGHT_THRESH or battery < LOW_VOLT_THRESH:
        sleep = SLEEP_DURATION * SLEEP_MULTIPLIER

    # Deep sleep or wait if sleep switch is closed
    if sleep_enable.value():
        sleep_enable = Pin("P2", Pin.OUT)
        print("PreDeep")
        xbee.XBee().sleep_now(sleep, pin_wake=False)
        while xbee.atcmd("AI") != 0:
            time.sleep_ms(100)
    else:
        sleep_enable = Pin("P2", Pin.OUT)
        print("PreNorm")
        time.sleep_ms(sleep)
        while xbee.atcmd("AI") != 0:
            time.sleep_ms(100)
