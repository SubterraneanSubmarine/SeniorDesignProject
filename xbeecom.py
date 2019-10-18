"""
David Carlson & Bryce Martin
ECE 4800 Senior Design Project

This File represents intractions of the Pi and Xbee3 coordinator (UART)
Tested in Python3.7(Windows) and 3.4(RPi)
"""
import json
import re
import datalocker
from datetime import datetime
from time import sleep
import serial
import busio
import digitalio
import board
import adafruit_dht
import adafruit_mcp3xxx.mcp3008 as mcp
from adafruit_mcp3xxx.analog_in import AnalogIn

# The Xbee (under MicroPython API) forwards a byte object/string that
# needs to be formated before we can manipulate it as a data object
# We are 'JSON'ifying the data


def convert_to_dict(bytes_in):
    # Here is what the Xbee will send:
    # {'profile': 49413, 'dest_ep': 232,
    #   'broadcast': False, 'sender_nwk': 38204,
    #   'source_ep': 232, 'payload': b'{Iteration: 0, ...more Key:Value pairs here..., ADCRead: 169, Zone: 2}',
    # We are interested in manipulating/saving the payload value(s)
    #   'sender_eui64': b'\x00\x13\xa2\x00A\x99O\xcc',        ### NOTE This is the MAC Addr: A = 41, O = 4F
    #   'cluster': 17}
    temp = str(bytes_in, "utf-8")  # Convert byte data into a string
    split_list = re.split("('payload': )|('sender_eui64': )", temp)  # Isolating the payload string before we attempt JSON'ifying it

    # If we have a bad payload/message we should not continue
    if len(split_list) < 7:
        raise Exception("Split list failed")

    # Extract the payload element/portion from the xbee message
    # split_list ==> [ "{'profile': 49413, 'dest_ep': 232, 'broadcast': False, 'sender_nwk': 38204, 'source_ep': 232, "
    #               , 'payload':
    #               , None
    #               , "b'{'Iteration': 0, 'Value': 345, 'Zone': 2}',}'"
    #               , None
    #               , "'sender_eui64':"
    #               , "b'\x00\x13\xa2\x00A\x99O\xcc', 'cluster': 17}"]

    payload = "{" + split_list[1] + split_list[3][2:]     # ==> {'payload': {'Iteration': 0, 'Value': 345, 'Zone': 2}',
    payload = payload[:-3] + "}"                        # ==> {'payload': {'Iteration': 0, 'Value': 345, 'Zone': 2}}
    temp = split_list[0] + split_list[5] + split_list[6]   # ==> {'profile': 49413, 'dest_ep': 232, 'broadcast': False, 'sender_nwk': 38204, 'source_ep': 232, 'sender_eui64': b'\x00\x13\xa2\x00A\x99O\xcc', 'cluster': 17}

    # Get our modified strings ready for JSON-ification
    payload = payload.replace("\'", "\"").replace("b\"", "\"").replace("\\", "\\\\").replace("False", "false").replace("True", "true")
    temp = temp.replace("\'", "\"").replace("b\"", "\"").replace("\\", "\\\\").replace("False", "false").replace("True", "true")

    # And convert to JSON / Dictionary objects
    payload = json.loads(payload)
    temp = json.loads(temp)
    temp.update(payload)  # Merge dictionaries -- get the payload into the object

    returndict = {}
    for keys in payload["payload"].keys():
        returndict[keys] = payload["payload"][keys]

    return returndict  #temp  # Returns a nested Dictionary Object


def talk_to_xbee():
    serial_set = False

    spi = busio.SPI(clock=board.SCK, MISO=board.MISO, MOSI=board.MOSI)
    cs = digitalio.DigitalInOut(board.D25)
    mcp = MCP.MCP3008(spi, cs)
    anemometer = AnalogIn(mcp, MCP.P0)

    dht = adafruit_dht.DHT22(board.D5)

    speed_average = [0, 0, 0, 0, 0]
    temp_average = [0, 0, 0, 0, 0]
    humidity_average = [0, 0, 0, 0, 0]
    anemometer_voffset = 0;
    pointer = 0

    while not serial_set and datalocker.ProgramRunning:
        try:
            # The Miniuart (ttyS0) is used on the Raspberry Pi
            # The full UART is connected to the onboard Bluetooth device
            # Default settings: 8bits, no parity, 1 stopbit, baud 9600
            # Timeout can be set as Serial("/dev/ttyS0", 115200, timeout = 5) in seconds
            # Timeout will stop the readline() function, no timeout implies the function will run until data is received
            port = serial.Serial("/dev/ttyS0", 115200)
            serial_set = True
        except serial.SerialException:
            print("Serial connection failed, trying again...")

    while pointer < 5:
        try:
            temp_average[pointer] = dht.temperature
            humidity_average[pointer] = dht.humidity
            pointer = pointer + 1
        except RuntimeError as e:
            sleep(10)
    pointer = 0

    # Possibly average this value
    anemometer_voffset = anemometer.voltage

    while pointer < 5:
        try:
            speed_average[pointer] = abs((anemometer.voltage - anemometer_voffset) * 20.25)
            pointer = pointer + 1
        except RuntimeError as e:
            sleep(10)
    pointer = 0

    while datalocker.ProgramRunning:

        if port.inWaiting() > 0:
            temp = convert_to_dict(port.readline())
            temp['Second'] = datetime.now().second
            temp['Minute'] = datetime.now().minute
            temp['Hour'] = datetime.now().hour
            temp['Day'] = datetime.now().day
            temp['Month'] = datetime.now().month
            temp['Year'] = datetime.now().year
            temp['Temperature'] = sum(temp_average)/len(temp_average)
            temp['Humidity'] = sum(humidity_average)/len(humidity_average)
            temp['Wind'] = sum(speed_average)/len(speed_average)

            datalocker.SensorStats[temp.get('Sector')] = temp
            datalocker.set_new()

            # TODO Do XbeeHealthCheck!
                        # rotate through list of Xbee's. Update its 'last seen'
                        # if a Xbee misses an update 6 times, User needs Alert!
                        # if a nodes light level -- reletive to the others is low, for 4 samples, User needs Alert!

                    # We now have within SensorState an object like so...
                    # {"\\x00\\x13\\xa2\\x00A\\x99O\\xcc": {"Iteration": 30796, "Sunlight": 2079, "Battery": 3348, "Moisture": 3239, "Sector": 0}
                    #   , "\\x00\\x11\\xa3\\x05A\\x94O\\xdd": {"Iter... , ... : 4} }

        if datetime.now().minute / 2 == 0:
            try:
                speed_average[pointer] = abs((anemometer.voltage - anemometer_voffset) * 20.25)
                pointer = (pointer + 1) % 5
            except RuntimeError as e:
                print("Wind speed retrieve failed")

            try:
                temp_average[pointer] = dht.temperature
                humidity_average[pointer] = dht.humidity
                pointer = (pointer + 1) % 5
            except RuntimeError as e:
                print("Temp/Humidity")

    port.close()


NodeLastSeen = {}
def XbeeHealthCheck():
    # TODO Update lastSeen
    return 0


if __name__ == '__main__':
    temporary = convert_to_dict(RecieveStrings[0])
    print("Payload: ", temporary["payload"])
    print("Payload->Value: ", temporary["payload"]["Value"])

"""
# TODO delete all this debug/troubleshooting help stuff -- below this line --
# {'profile': 49413, 'dest_ep': 232, 'broadcast': False, 'sender_nwk': 38204, 'source_ep': 232, 'payload': b'"Iteration": 0, "ADCRead": 169, "Zone": 2', 'sender_eui64': b'\x00\x13\xa2\x00A\x99O\xcc', 'cluster': 17}
RecieveStrings = [b"{'profile': 49413, 'dest_ep': 232, 'broadcast': False, 'sender_nwk': 38204, 'source_ep': 232, 'payload': b'{'Iteration': 0, 'Value': 345, 'Zone': 2}', 'sender_eui64': b'\\x00\\x13\\xa2\\x00A\\x99O\\xcc', 'cluster': 17}",
                  b"{'profile': 49413, 'dest_ep': 232, 'broadcast': False, 'sender_nwk': 38204, 'source_ep': 232, 'payload': b'{'Iteration': 55, 'Value': 555, 'Zone': 1}', 'sender_eui64': b'\\x00\\x13\\xa2\\x00F\\x99B\\xc3', 'cluster': 17}"]



SAMPLE DATA
{'Iteration': 301, 'Sector': 1, 'Moisture': 0, 'Sunlight': 0, 'Battery': 2289, 'Second': 27, 'Minute': 40, 'Hour': 18, 'Day': 17, 'Month': 10, 'Year': 2019, 'Temperature': 25.2, 'Humidity': 35.5, 'Wind': 2.748697642481118}
{'Iteration': 302, 'Sector': 1, 'Moisture': 0, 'Sunlight': 0, 'Battery': 2288, 'Second': 37, 'Minute': 40, 'Hour': 18, 'Day': 17, 'Month': 10, 'Year': 2019, 'Temperature': 25.2, 'Humidity': 35.5, 'Wind': 2.748697642481118}
{'Iteration': 303, 'Sector': 1, 'Moisture': 0, 'Sunlight': 0, 'Battery': 2285, 'Second': 47, 'Minute': 40, 'Hour': 18, 'Day': 17, 'Month': 10, 'Year': 2019, 'Temperature': 25.2, 'Humidity': 35.5, 'Wind': 2.748697642481118}
{'Iteration': 304, 'Sector': 1, 'Moisture': 0, 'Sunlight': 0, 'Battery': 2284, 'Second': 57, 'Minute': 40, 'Hour': 18, 'Day': 17, 'Month': 10, 'Year': 2019, 'Temperature': 25.2, 'Humidity': 35.5, 'Wind': 2.748697642481118}
{'Iteration': 305, 'Sector': 1, 'Moisture': 0, 'Sunlight': 0, 'Battery': 2282, 'Second': 7, 'Minute': 41, 'Hour': 18, 'Day': 17, 'Month': 10, 'Year': 2019, 'Temperature': 25.2, 'Humidity': 35.5, 'Wind': 2.748697642481118}
{'Iteration': 306, 'Sector': 1, 'Moisture': 0, 'Sunlight': 0, 'Battery': 2282, 'Second': 17, 'Minute': 41, 'Hour': 18, 'Day': 17, 'Month': 10, 'Year': 2019, 'Temperature': 25.2, 'Humidity': 35.5, 'Wind': 2.748697642481118}
{'Iteration': 307, 'Sector': 1, 'Moisture': 0, 'Sunlight': 0, 'Battery': 2279, 'Second': 28, 'Minute': 41, 'Hour': 18, 'Day': 17, 'Month': 10, 'Year': 2019, 'Temperature': 25.2, 'Humidity': 35.5, 'Wind': 2.748697642481118}
{'Iteration': 308, 'Sector': 1, 'Moisture': 0, 'Sunlight': 0, 'Battery': 2279, 'Second': 38, 'Minute': 41, 'Hour': 18, 'Day': 17, 'Month': 10, 'Year': 2019, 'Temperature': 25.2, 'Humidity': 35.5, 'Wind': 2.748697642481118}
{'Iteration': 309, 'Sector': 1, 'Moisture': 0, 'Sunlight': 0, 'Battery': 2278, 'Second': 48, 'Minute': 41, 'Hour': 18, 'Day': 17, 'Month': 10, 'Year': 2019, 'Temperature': 25.2, 'Humidity': 35.5, 'Wind': 2.748697642481118}
{'Iteration': 310, 'Sector': 1, 'Moisture': 0, 'Sunlight': 0, 'Battery': 2274, 'Second': 58, 'Minute': 41, 'Hour': 18, 'Day': 17, 'Month': 10, 'Year': 2019, 'Temperature': 25.2, 'Humidity': 35.5, 'Wind': 2.748697642481118}
{'Iteration': 311, 'Sector': 1, 'Moisture': 0, 'Sunlight': 0, 'Battery': 2274, 'Second': 9, 'Minute': 42, 'Hour': 18, 'Day': 17, 'Month': 10, 'Year': 2019, 'Temperature': 25.2, 'Humidity': 35.5, 'Wind': 2.748697642481118}
"""
