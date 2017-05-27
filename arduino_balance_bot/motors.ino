AF_DCMotor motor1(motor1Num);
AF_DCMotor motor2(motor2Num);

int motor1Speed = 0;
int motor2Speed = 0;

void setMotorSpeed(AF_DCMotor motor, int speed)
{
    uint8_t direction = FORWARD;
    if (speed < 0)
    {
        direction = BACKWARD;
        speed = -speed;
    }
    if (speed != 0)
    {
        motor.setSpeed((uint8_t)speed);
        motor.run(direction);
    }
    else
    {
        motor.run(RELEASE);
    }
}

void setMotorSpeeds(int motor1Speed, int motor2Speed)
{
    setMotorSpeed(motor1, motor1Speed);
    setMotorSpeed(motor2, motor2Speed);
}

void updateMotors(bool writeMotorSpeeds, int speed, double curAngle, double motorSpeedMultiplier)
{
    motor1Speed = -speed * motorSpeedMultiplier;
    motor2Speed = -speed * motorSpeedMultiplier;
    
    if (writeMotorSpeeds && ((230 > curAngle && curAngle > 181) || (179 > curAngle && curAngle > 130))) setMotorSpeeds(motor1Speed, motor2Speed);
    else setMotorSpeeds(0, 0);
}
