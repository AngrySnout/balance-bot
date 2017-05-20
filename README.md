# Self Balancing Robot

This repository contains the source code for my self-balancing robot final year project.

## Subdirectories

### arduino_balance_bot

Contains the balancing source code for the Arduino Uno.  
In order to compile, you need to install the following libraries:

- [KalmanFilter](https://github.com/TKJElectronics/KalmanFilter)
- [Adafruit-Motor-Shield-library](https://github.com/adafruit/Adafruit-Motor-Shield-library)

### at_commands

Contains the at_commands application required for changing settings on the HC-05 Bluetooth module.

### plotter

Contains the Python 3 application for plotting bot input from the Arduino Uno over USB.  
You need the following packages in order to run it:

- pyserial
- numpy
- matplotlib

### remote_control

Contains the source code for the Android remote control application.

## License

Refer to each directory's LICENSE file.
