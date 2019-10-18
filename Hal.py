'''
David Carlson & Bryce Martin
ECE 4800 Senior Design Project

This File represents interactions of the Pi and Xbee3 coordinator (UART)

Tested in Python3.7(Windows) and 3.4(RPi)
'''

from sys import platform
if platform == "linux":  # Once all the testing is done for this project, it will only run on the RPi -- so this could be deleted
    import serial
    import gpiozero
    # https://pypi.org/project/smbus2/
    from smbus2 import SMBus
    bus = SMBus(1)  # TODO get the correct address/handle for the SPI bus
from collections import deque
import json
import re
import Jarvis


# The Xbee (under MicroPython API) forwards a byte object/string that
# needs to be formated before we can manipulate it as a data object
# We are 'JSON'ifying the data
def ConvertToDict(bytes_in):
    # Here is what the Xbee will send:
    # {'profile': 49413, 'dest_ep': 232, 
    #   'broadcast': False, 'sender_nwk': 38204, 
    #   'source_ep': 232, 'payload': b'{Iteration: 0, ...more Key:Value pairs here..., ADCRead: 169, Zone: 2}',  ### We are interested in manipulating/saving the payload value(s)
    #   'sender_eui64': b'\x00\x13\xa2\x00A\x99O\xcc',        ### NOTE This is the MAC Addr: A = 41, O = 4F
    #   'cluster': 17}

    temp = str(bytes_in, "utf-8")  # Convert byte data into a string
    splitList = re.split("('payload': )|('sender_eui64': )", temp)  # Isolating the payload string before we attempt JSON'ifying it

    # If we have a bad payload/message we should not continue
    if len(splitList) < 7:
        raise Exception("splitlist failed")

    # Extract the payload element/portion from the xbee message
    # splitList ==> [ "{'profile': 49413, 'dest_ep': 232, 'broadcast': False, 'sender_nwk': 38204, 'source_ep': 232, "
    #               , 'payload':
    #               , None
    #               , "b'{'Iteration': 0, 'Value': 345, 'Zone': 2}',}'"
    #               , None
    #               , "'sender_eui64':"
    #               , "b'\x00\x13\xa2\x00A\x99O\xcc', 'cluster': 17}"]
    payload = "{" + splitList[1] + splitList[3][2:]     # ==> {'payload': {'Iteration': 0, 'Value': 345, 'Zone': 2}',
    payload = payload[:-3] + "}"                        # ==> {'payload': {'Iteration': 0, 'Value': 345, 'Zone': 2}}


    temp = splitList[0] + splitList[5] + splitList[6]   # ==> {'profile': 49413, 'dest_ep': 232, 'broadcast': False, 'sender_nwk': 38204, 'source_ep': 232, 'sender_eui64': b'\x00\x13\xa2\x00A\x99O\xcc', 'cluster': 17}
    
    # Get our modified strings ready for JSON-ifying
    payload = payload.replace("\'", "\"").replace("b\"", "\"").replace("\\", "\\\\").replace("False", "false").replace("True", "true")
    temp = temp.replace("\'", "\"").replace("b\"", "\"").replace("\\", "\\\\").replace("False", "false").replace("True", "true")
    
    # And convert to JSON / Dictionary objects
    payload = json.loads(payload)
    temp = json.loads(temp)
    temp.update(payload)  # Merge dictionaries -- get the payload into the object
    return payload #temp  # Returns a nested Dictionary Object

def TalkToXbee():
    if platform == "linux":
        try:
            # May want to change the pi to use ttyAMA0 instead of miniUART (ttyS0)
            port = serial.Serial("/dev/ttyS0", 115200)
            # Default settings: 8bits, no parity, 1 stopbit, baud 9600
        except:
            # Will the port fail opening? -- yes, when sudo piviledges are required
            # rather / instead: We need to config Pi port permissions...
            pass
        while Jarvis.ProgramRunning:
            # Updated Local sensor variables
            localSensors()  # TODO This may need to be limited in the number of calls made to it?

            # Update Xbee Data + Nodes
            if port.inWaiting() > 0:
                try:
                    temp = ConvertToDict(port.readline())
                except:
                    print("We have failed to convert the payload...")

                # If the payload contains a sender_eui64...         # Change this to Zone!
                # if temp.get("sender_eui64") != None:
                if temp.get("Payload")["Zone"] != None:

                    # If the eui_64 does not exist in our database (dictionary)
                    # if Jarvis.SensorStats.get(temp["sender_eui64"]) == None:
                    if Jarvis.SensorStats.get(temp["Payload"]["Zone"]) == None:
                        # Then add it as a Key
                        # Jarvis.SensorStats[temp["sender_eui64"]] = {}
                        Jarvis.SensorStats[temp["Payload"]["Zone"]] = {}
                        # TODO And save a "Last seen"
                        
                    # The eui_64 is already in our database (dictionary): Update its Value with the new information from the Xbee
                    # Jarvis.SensorStats[temp["sender_eui64"]].update(temp["payload"])
                    Jarvis.SensorStats[temp["Payload"]["Zone"]].update(temp["Payload"])
                    with Jarvis.lock:
                        Jarvis.NewSensorData = True
                    
                    # TODO Do XbeeHealthCheck!
                        # rotate through list of Xbee's. Update its 'last seen'
                        # if a Xbee misses an update 6 times, User needs Alert!
                        # if a nodes light level -- reletive to the others is low, for 4 samples, User needs Alert!

                    # We now have within SensorState an object like so...
                    # {"\\x00\\x13\\xa2\\x00A\\x99O\\xcc": {"Iteration": 30796, "Sunlight": 2079, "Battery": 3348, "Moisture": 3239, "Sector": 0}
                    #   , "\\x00\\x11\\xa3\\x05A\\x94O\\xdd": {"Iter... , ... : 4} }


avWindRR = deque([0, 0, 0, 0, 0])
avHumidRR = deque([0, 0, 0, 0, 0])
avTempRR = deque([0, 0, 0, 0, 0])
avMoistRR = deque([0, 0, 0, 0, 0])
# TODO and the other values
def localSensors():
    # MCP 3008
    # SPI MISO, MOSI, CLK
    # Running average of temperature and wind(?) and humidity(?)
    # RoundRobbin moving average (do we want this time based?)  -- We could save a snapshot of what time it is, then only allow this to run if ~1Hr has passed.
    if Jarvis.NewSensorData:  # maybe? or time based, average every 20min?
        avWindRR.rotate(1)
        avHumidRR.rotate(1)
        avTempRR.rotate(1)
        avMoistRR.rotate(1)
        # avWindRR[0] = bus.read_byte_data(\#\#, \#)
        # avHumidRR[0] = bus.read_byte_data(\#\#, \#)
        # avTempRR[0] = bus.read_byte_data(\#\#, \#)
        # avMoistRR[0] = bus.read_byte_data(\#\#, \#)
        temp1 = 0
        temp2 = 0
        temp3 = 0
        temp4 = 0
        for val in zip(avWindRR, avHumidRR, avTempRR, avMoistRR):
            temp1 = temp1 + val[0]
            temp2 = temp2 + val[1]
            temp3 = temp3 + val[2]
            temp4 = temp4 + val[3]
        Jarvis.avWind = (temp1 / avWindRR.__len__())
        Jarvis.avTemp = (temp2 / avTempRR.__len__())
        Jarvis.avHumid = (temp3 / avHumidRR.__len__())
        Jarvis.avMoisture = (temp4 / avMoistRR.__len__())

NodeLastSeen = {}
def XbeeHealthCheck():
    # TODO Update lastSeen
    return 0



# TODO delete all this debug/troubleshooting help stuff -- below this line --
# {'profile': 49413, 'dest_ep': 232, 'broadcast': False, 'sender_nwk': 38204, 'source_ep': 232, 'payload': b'"Iteration": 0, "ADCRead": 169, "Zone": 2', 'sender_eui64': b'\x00\x13\xa2\x00A\x99O\xcc', 'cluster': 17}
RecieveStrings = [b"{'profile': 49413, 'dest_ep': 232, 'broadcast': False, 'sender_nwk': 38204, 'source_ep': 232, 'payload': b'{'Iteration': 0, 'Value': 345, 'Zone': 2}', 'sender_eui64': b'\\x00\\x13\\xa2\\x00A\\x99O\\xcc', 'cluster': 17}",
                  b"{'profile': 49413, 'dest_ep': 232, 'broadcast': False, 'sender_nwk': 38204, 'source_ep': 232, 'payload': b'{'Iteration': 55, 'Value': 555, 'Zone': 1}', 'sender_eui64': b'\\x00\\x13\\xa2\\x00F\\x99B\\xc3', 'cluster': 17}"]

if __name__=='__main__':
    temp = ConvertToDict(RecieveStrings[0])
    print("Payload: ", temp["payload"])
    print("Payload->Value: ", temp["payload"]["Zone"])
