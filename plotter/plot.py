#!/usr/bin/env python3

import serial
import re
import numpy as np
import matplotlib.pyplot as plt

numMeasurements = 100

X = np.linspace(-numMeasurements,0,numMeasurements)
rollMeasurements = np.zeros(numMeasurements)
speedMeasurements = np.zeros(numMeasurements)
setpoints = np.empty(numMeasurements)
setpoints.fill(180)

plt.subplot(211)
rollGraph = plt.plot(X,rollMeasurements)[0]
setpointGraph = plt.plot(X,setpoints,color='r')[0]
plt.ylim(140, 220);
plt.ylabel('Roll (degrees)')

plt.subplot(212)
speedGraph = plt.plot(X,speedMeasurements,color='g')[0]
plt.ylim(-300, 300);
plt.ylabel('Motor Speed')
plt.xlabel('Iterations')

ser = serial.Serial('/dev/ttyUSB0', 9600)
prog = re.compile(r'yaw:.(-?[\d.]+).pitch: (-?[\d.]+).roll: (-?[\d.]+).setpoint: (-?[\d.]+).forwardDir: (-?[\d.]+).Motor 1: (-?[\d.]+).Motor 2: (-?[\d.]+)')

while True:
	s = ser.readline().decode('ISO-8859-1')
	g = prog.match(s)
	if g != None:
		roll = float(g.group(3))
		setpoint = float(g.group(4))
		motorSpeed = float(g.group(6))
		print('Roll: {}, Setpoint: {}, motorSpeed: {}'.format(roll, setpoint, motorSpeed))
		
		rollMeasurements = np.roll(rollMeasurements, -1)
		rollMeasurements[-1] = roll
		
		speedMeasurements = np.roll(speedMeasurements, -1)
		speedMeasurements[-1] = motorSpeed
		
		rollGraph.set_ydata(rollMeasurements)
		speedGraph.set_ydata(speedMeasurements)
		plt.draw()
		plt.pause(0.01)


ser.close()
