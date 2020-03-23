package org.firstinspires.ftc.teamcode.opmodes.autonomous

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.robotcore.hardware.DcMotor

import org.firstinspires.ftc.teamcode.hardware.HardwareSkybot
import org.firstinspires.ftc.teamcode.utilities.PIDController
import kotlin.math.*

abstract class AutonomousOpMode : LinearOpMode() {
    @JvmField var power: Double = 0.3
    @JvmField var countsPerRev: Double = 28.0
    @JvmField var wheelDiameter: Int = 4
    private var rotation = 0.0

    private var correction = 0.0
    companion object {
        private const val maxErrorRotate = 90.0

        private const val targetSpeedMaxRotate = 1.0
        private const val baseR = targetSpeedMaxRotate / maxErrorRotate

        private const val kdRotate = baseR * 20
        private const val kpRotate = baseR
        private const val kiRotate = baseR / 125

        private const val kdStraight = 0.0
        private const val kpStraight = 0.05
        private const val kiStraight = 0.0
    }

    private var pidRotate: PIDController = PIDController(kpRotate, kiRotate, kdRotate)
    private var pidDrive: PIDController = PIDController(kpStraight, kiStraight, kdStraight)

    @JvmField val robot: HardwareSkybot = HardwareSkybot()

    override fun runOpMode() {
        robot.init(hardwareMap)
        pidRotate = PIDController(kpRotate, kiRotate, kdRotate)
        pidDrive = PIDController(kpStraight, kiStraight, kdStraight)
        pidDrive.setpoint = 0.0
        pidDrive.setOutputRange(0.0, power)
        pidDrive.setInputRange(-90.0, 90.0)
        pidDrive.enable()
        robot.resetAngle()
    }

    fun inchToCounts (inches: Double) : Double {
        val revs = inches/(wheelDiameter*PI)
        return revs * countsPerRev
    }

    fun rotate(deg: Double) {
        var degrees = deg
        var rpower = power
        degrees = -degrees
        val turnTolerance = 0.1

        robot.resetAngle()
        robot.leftDrive.power = 0.0
        robot.rightDrive.power = 0.0
        robot.leftDriveFront.power = 0.0
        robot.rightDriveFront.power = 0.0

        if (abs(degrees) > 359) degrees = 359.0.withSign(degrees).toInt().toDouble()

        pidRotate.reset()
        pidRotate.setpoint = degrees
        pidRotate.setInputRange(0.0, degrees + 0.1)
        pidRotate.setOutputRange(0.0, targetSpeedMaxRotate / 4)
        pidRotate.setTolerance(turnTolerance)
        pidRotate.enable()
        telemetry.addLine("About to Rotate")
        telemetry.update()

        if (degrees < 0) {
            while (opModeIsActive() && robot.angle == 0.0) {
                robot.leftDrive.power = -rpower
                robot.rightDrive.power = rpower
                robot.leftDriveFront.power = rpower
                robot.rightDriveFront.power = -rpower
                telemetry.addLine("About to rotate right")
                telemetry.update()
                sleep(100)
            }
            do {
                telemetry.addLine("Rotating Right")
                telemetry.update()
                rpower = pidRotate.performPID(robot.angle) // power will be - on right turn.
                robot.leftDrive.power = -rpower
                robot.rightDrive.power = rpower
                robot.leftDriveFront.power = rpower
                robot.rightDriveFront.power = -rpower
            } while (opModeIsActive() && !pidRotate.onTarget())
        } else {   // left turn.
            do {
                rpower = pidRotate.performPID(robot.angle) // power will be + on left turn.
                robot.leftDrive.power = -rpower
                robot.rightDrive.power = rpower
                robot.leftDriveFront.power = rpower
                robot.rightDriveFront.power = -rpower
                telemetry.addLine("Updating")
                telemetry.addData("Degrees: ", degrees)
                telemetry.update()
            } while (opModeIsActive() && !pidRotate.onTarget())
        }

        // turn the motors off.
        robot.rightDrive.power = 0.0
        robot.leftDrive.power = 0.0
        robot.leftDriveFront.power = 0.0
        robot.rightDriveFront.power = 0.0

        rotation = robot.angle

        // wait for rotation to stop.
        sleep(500)

        // reset angle tracking on new heading.
        robot.resetAngle()

        telemetry.addLine("Done")
        telemetry.update()
    }

    fun pidDriveWithEncoders(counts: Double, power: Double) {
        robot.leftDrive.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
        robot.leftDrive.mode = DcMotor.RunMode.RUN_USING_ENCODER

        robot.leftDrive.targetPosition = counts.roundToInt()
        telemetry.addData("Target Count: ", counts)
        telemetry.update()

        val spower = if (counts > 0) power else -power

        robot.leftDrive.power = spower
        robot.rightDrive.power = spower
        robot.leftDriveFront.power = spower
        robot.rightDriveFront.power = spower

        while (abs(robot.leftDrive.currentPosition) < abs(counts) && opModeIsActive()) {
            correction = pidDrive.performPID(robot.angle)
            robot.leftDrive.power = spower - correction
            robot.rightDrive.power = spower + correction
            telemetry.addData("Current Count: ", robot.leftDrive.currentPosition)
            telemetry.update()
        }

        robot.leftDrive.power = 0.0
        robot.rightDrive.power = 0.0
        robot.rightDriveFront.power = 0.0
        robot.leftDriveFront.power = 0.0


        robot.leftDrive.mode = DcMotor.RunMode.RUN_WITHOUT_ENCODER
    }
}