extern int forwardDir;
extern int rotationDir;
extern bool writeMotorSpeeds;
extern double motor1SpeedMultiplier;
extern double motor2SpeedMultiplier;
extern double balanceKp;
extern double balanceKi;
extern double balanceKd;
extern double movementSpeed;
extern double rotationSpeed;

SoftwareSerial bluetooth(bluetoothTx, bluetoothRx);

void setupBluetooth()
{
    bluetooth.begin(9600);
}

int getByte()
{
    while (!bluetooth.available());
    unsigned char input = (unsigned char)bluetooth.read();
}

void getInput()
{
    if (bluetooth.available())
    {
        char in = (char)bluetooth.read();
        
        switch (in)
        {
            case 'F':
            case 'b':
                forwardDir += 1;
                break;
            case 'B':
            case 'f':
                forwardDir -= 1;
                break;
            case 'L':
            case 'r':
                rotationDir += 1;
                break;
            case 'R':
            case 'l':
                rotationDir -= 1;
                break;
            case 'x':
                setSetpoint((double)getByte() / 2.5 - 20);
                break;
            case 'z':
                writeMotorSpeeds = getByte();
                if (!writeMotorSpeeds) setMotorSpeeds(0, 0);
                break;
            case 'm':
                motorSpeedMultiplier = getByte() / 100.0;
                break;
            case 'h':
                movementSpeed = (double) getByte() / 5.0;
                break;
            case 'g':
                rotationSpeed = (double) getByte() / 5.0;
                break;
            case 'p':
                balanceKp = (getByte() * 50.0) / 255.0;
                break;
            case 'i':
                balanceKi = (getByte() * 5.0) / 255.0;
                break;
            case 'd':
                balanceKd = (getByte() * 5.0) / 255.0;
                break;
        }
    }
}
