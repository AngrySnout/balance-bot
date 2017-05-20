extern double balanceKp;
extern double balanceKi;
extern double balanceKd;

Kalman kalman;

int16_t accX, accY, accZ;
int16_t tempRaw;
int16_t gyroX, gyroY, gyroZ;

double accXangle;
double gyroXangle;
double kalAngleX;

unsigned long timer;
uint8_t i2cData[14];
double curAngle;

double setpoint = 5.0, originalSetpoint = 5.0;
double ySetpoint = 0.0, yOriginalSetpoint = 0.0;

double pTerm, iTerm, dTerm, integrated_error, last_error, error;
const double K = 1.9*1.12;
#define   GUARD_GAIN   10.0

int speed;

int getSpeed()
{
	return speed;
}

void setSetpoint(double newSetpoint)
{
	originalSetpoint = setpoint = newSetpoint;
}

void updateSetpoint(double forwardSpeed, double rotationSpeed)
{
    setpoint = originalSetpoint - forwardSpeed;
    ySetpoint = constrain(ySetpoint + yOriginalSetpoint - rotationSpeed, -180, 180);
}

// The following lines until the end of the file were taken from
// http://www.bajdi.com/building-a-self-balancing-bot/
void setupMPU() {
    Wire.begin();

    i2cData[0] = 7; // Set the sample rate to 1000Hz - 8kHz/(7+1) = 1000Hz
    i2cData[1] = 0x00; // Disable FSYNC and set 260 Hz Acc filtering, 256 Hz Gyro filtering, 8 KHz sampling
    i2cData[2] = 0x00; // Set Gyro Full Scale Range to ±250deg/s
    i2cData[3] = 0x00; // Set Accelerometer Full Scale Range to ±2g
    while(i2cWrite(0x19,i2cData,4,false)); // Write to all four registers at once
    while(i2cWrite(0x6B,0x01,true)); // PLL with X axis gyroscope reference and disable sleep mode 

    while(i2cRead(0x75,i2cData,1));
    if(i2cData[0] != 0x68) { // Read "WHO_AM_I" register
    Serial.print(F("Error reading sensor"));
        while(1);
    }

    delay(100); // Wait for sensor to stabilize

    /* Set kalman and gyro starting angle */
    while(i2cRead(0x3B,i2cData,6));
    accX = ((i2cData[0] << 8) | i2cData[1]);
    accY = ((i2cData[2] << 8) | i2cData[3]);
    accZ = ((i2cData[4] << 8) | i2cData[5]);
    accXangle = (atan2(accY,accZ)+PI)*RAD_TO_DEG;

    kalman.setAngle(accXangle); // Set starting angle
    gyroXangle = accXangle;
    timer = micros();
}

void updateMPU()
{
    while(i2cRead(0x3B,i2cData,14));
    accX = ((i2cData[0] << 8) | i2cData[1]);
    accY = ((i2cData[2] << 8) | i2cData[3]);
    accZ = ((i2cData[4] << 8) | i2cData[5]);
    tempRaw = ((i2cData[6] << 8) | i2cData[7]);  
    gyroX = ((i2cData[8] << 8) | i2cData[9]);
    gyroY = ((i2cData[10] << 8) | i2cData[11]);
    gyroZ = ((i2cData[12] << 8) | i2cData[13]);
    accXangle = (atan2(accY,accZ)+PI)*RAD_TO_DEG;
    accXangle += 180 - setpoint;
    if (accXangle >= 360) accXangle -= 360;
    double gyroXrate = (double)gyroX/131.0;
    curAngle = kalman.getAngle(accXangle, gyroXrate, (double)(micros()-timer)/1000000);
    timer = micros();
    
    // PID
    error = 180 - curAngle;  // 180 = level
    pTerm = balanceKp * error;
    integrated_error += error;
    iTerm = balanceKi * constrain(integrated_error, -GUARD_GAIN, GUARD_GAIN);
    dTerm = balanceKd * (error - last_error);
    last_error = error;
    speed = constrain(K*(pTerm + iTerm + dTerm), -255, 255);
}

