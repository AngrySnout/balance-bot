#include <SoftwareSerial.h>
#include <AFMotor.h>
#include <Wire.h>
#include <Kalman.h>

#define OUTPUT_SERIAL_READABLE
#define OUTPUT_BLUETOOTH_BINARY

// Pins
const int bluetoothTx = A0;
const int bluetoothRx = A3;
const int motor1Num = 1;
const int motor2Num = 3;

// Variables
bool writeMotorSpeeds = true;
double motorSpeedMultiplier = 0.4;
double balanceKp = 50;
double balanceKi = 1.65;
double balanceKd = 3.57;
double movementSpeed = 10.0;
double rotationSpeed = 10.0;

// Constants
// Rotation constants go unused for now
const double rotationKp = 50;
const double rotationKi = 300;
const double rotationKd = 4;

// Internal variables
int forwardDir = 0;
int rotationDir = 0;

extern int speed;
extern double curAngle;

void setup()
{
    // Setup Serial
    Serial.begin(9600);
    
    // Setup Bluetooth
    setupBluetooth();
    
    // Setup MPU
    setupMPU();
}

void loop()
{
    // Get input from Bluetooth
    getInput();
    
    // Update setpoint
    updateSetpoint(movementSpeed * forwardDir, rotationSpeed * rotationDir);
    
    // Run every 25 milliseconds
    static long lastRun = 0;
    if ((uint16_t)((uint16_t)millis() - lastRun) > 25) {
        // Update MPU
        updateMPU();

        // Update motors
        updateMotors(writeMotorSpeeds, speed, curAngle, motorSpeedMultiplier);
        
        // Print debug information
        printDebug();
        
        lastRun = millis();
    }
}

void printDebug()
{
    extern SoftwareSerial bluetooth;
    extern int motor1Speed;
    extern int motor2Speed;
    extern int16_t gyroX, gyroY, gyroZ;
    extern double setpoint;
    extern double ySetpoint;

    // Write values to Serial/Bluetooth every 200 milliseconds
    static long lastRun = 0;
    if ((uint16_t)((uint16_t)millis() - lastRun) > 200) {
        #ifdef OUTPUT_BLUETOOTH_BINARY
        bluetooth.write('s');
        
        int16_t yaw = (gyroY + 180) * 10;
        int16_t roll = curAngle * 10;
        bluetooth.write((int8_t)(yaw >> 8)); bluetooth.write((int8_t)(yaw & 0xFF));
        bluetooth.write((int8_t)(roll >> 8)); bluetooth.write((int8_t)(roll & 0xFF));
        
        int16_t motor1SpeedN = motor1Speed + 255;
        int16_t motor2SpeedN = motor2Speed + 255;
        bluetooth.write((int8_t)(motor1SpeedN >> 8)); bluetooth.write((int8_t)(motor1SpeedN & 0xFF));
        bluetooth.write((int8_t)(motor2SpeedN >> 8)); bluetooth.write((int8_t)(motor2SpeedN & 0xFF));
        
        int16_t yawSetpointN = (ySetpoint + 180) * 10;
        int16_t rollSetpointN = (setpoint + 180) * 10;
        bluetooth.write((int8_t)(yawSetpointN >> 8)); bluetooth.write((int8_t)(yawSetpointN & 0xFF));
        bluetooth.write((int8_t)(rollSetpointN >> 8)); bluetooth.write((int8_t)(rollSetpointN & 0xFF));
        
        bluetooth.write('\n');
        #endif
        
        #ifdef OUTPUT_SERIAL_READABLE
        Serial.print("yaw: ");
        Serial.print(gyroY);
        Serial.print("\tpitch: ");
        Serial.print(gyroZ);
        Serial.print("\troll: ");
        Serial.print(curAngle);
        Serial.print("\tsetpoint: ");
        Serial.print(setpoint);
        Serial.print("\tforwardDir: ");
        Serial.print(forwardDir);
        Serial.print("\tMotor 1: ");
        Serial.print(motor1Speed);
        Serial.print("\tMotor 2: ");
        Serial.print(motor2Speed);
        Serial.print("\n");
        #endif

        lastRun = millis();
    }
}
