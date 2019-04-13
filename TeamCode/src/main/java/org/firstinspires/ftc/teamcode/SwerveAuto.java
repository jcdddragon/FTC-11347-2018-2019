// ***********************************************************************
// SwerveAuto
// ***********************************************************************
// The autonomous mode for swerve operations


//
// ****** IMPORTANT NOTE FOR STATE CHANGES ******
// The state machine used here has the ability to automatically add a wait
// before the next state starts operation. This is very useful for robot
// operations because the robot sometimes needs a delay before it starts
// any status checks in the following state. It allows the robot to start
// and action and wait before taking the next action.
//


// TODO - Add new log file just for autonomous state results? Or flag at END of each state...

package org.firstinspires.ftc.teamcode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.Gamepad;

import org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer;
import org.firstinspires.ftc.teamcode.vision.MasterVision;
import org.firstinspires.ftc.teamcode.vision.SampleRandomizedPositions;

// ***********************************************************************
// Definitions from Qualcomm code for OpMode recognition
// ***********************************************************************
// **** DO NOT ENABLE - Started from the crater and depot auto code derived classes now ****
// ****   @Autonomous(name="Swerve: 1-Auto 0.6", group="Swerve")
// ***********************************************************************
// 11/9/2018 - Swerve drive is now capable of auto correcting. The wheels actively adjust while moving
public class SwerveAuto extends SwerveCore {

    //private static Boolean useLightFlicker = Boolean.TRUE;

    // State machine for where we are on the autonomous journey

    // NEW states and initial working code done at GRITS meeting 10/13/2018

    enum autoStates {
        SWERVE_INIT,
        SWERVE_START,
        SWERVE_ALIGN,
        SWERVE_DROP,
        SWERVE_DELAY,
        SWERVE_TURN_TO_PARTICLE,
        SWERVE_SLIDE,
        SWERVE_GRAB,
        SWERVE_PARTICLE_TO_DEPOT,
        SWERVE_SCORE,
        SWERVE_TURN,
        SWERVE_GM_BACK,
        SWERVE_HIT_PARTICLE,
        SWERVE_GM_EXTEND,
        SWERVE_TO_WALL,
        SWERVE_AVOID_PARTICLE,
        SWERVE_TO_DEPOT,
        SWERVE_PLACE_MARKER,
        SWERVE_TO_CRATER,
        SWERVE_CRATER_PARTICLES,
        SWERVE_PARTICLE_TO_LANDER,
        SWERVE_RETRACT,
        SWERVE_MARKER,
        SWERVE_LAST_MOVE,
        SWERVE_DONE,

        SWERVE_AUTO_TESTING_TURN_BACK,
        SWERVE_AUTO_TESTING_CLIMB
    }

    enum particlePosition {
        partLeft,
        partCenter,
        partRight,
        partUnknown
    }

    private autoStates revTankState;
    private double stateStartTime;
    private double stateWaitTime;
    // report of our time wait
    private String checkReport;
    //    private String loopSenseStatus;
    private Boolean autoDriveWait;
    private Boolean autoDriveStop;
    private double parDist;
    private double parAng;
    private double wallDist;
    // debug options to run a few states
    // -- enabled/controlled in SwerveAutoTEST init/start
    boolean debugActive = Boolean.FALSE;
    long debugStates = 1;
    autoStates debugStartState = autoStates.SWERVE_DONE;

    private particlePosition parPosition;

    // Sensor data for robot positioning
//    public boolean skipDrop;
//    private float robotTurn[];
//    private float robotMove[];
//    private float lastSenseTime;
//
    // Robot orientation data
//    private float robotOrientation[];
//    private float robotSpeed[];
//    private float robotPosition[];
    // for Vuforia detection
    private MasterVision vision;
    private SampleRandomizedPositions goldPosition;

    // variables for auto actions


    // ***********************************************************************
    // SwerveAuto
    // ***********************************************************************
    // Constructs the class.
    // The system calls this member when the class is instantiated.
    SwerveAuto() {
        // Initialize base classes.
        // All via self-construction.

        // Initialize class members.
        // All via self-construction.
    }


    // ***********************************************************************
    // getStateName
    // ***********************************************************************
    // Return the name of the current state


    private String getStateName(autoStates myState) {
        // Set the name for the state we are in
        switch (myState) {

            case SWERVE_INIT:
                return "INITIALIZING";
            case SWERVE_START:
                return "START";
            case SWERVE_ALIGN:
                return "ALIGN";
            case SWERVE_DROP:
                return "DROP";
            case SWERVE_DELAY:
                return "DELAY";
            case SWERVE_TURN_TO_PARTICLE:
                return "ALIGN ON GROUND";
            case SWERVE_SLIDE:
                return "SLIDE";
            case SWERVE_GRAB:
                return "PULL BACK";
            case SWERVE_AVOID_PARTICLE:
                return "CENTER";
            case SWERVE_TURN:
                return "TURN TO PARTICLES";
            case SWERVE_TO_WALL:
                return "TO WALL";
            case SWERVE_TO_DEPOT:
                return "TO DEPOT";
            case SWERVE_PLACE_MARKER:
                return "PLACE MARKER";
            case SWERVE_TO_CRATER:
                return "TO CRATER";
            case SWERVE_CRATER_PARTICLES:
                return "CRATER PARTICLES";
            case SWERVE_PARTICLE_TO_LANDER:
                return "PARTICLE TO LANDER";
            case SWERVE_RETRACT:
                return "SCORE BALLS";
            case SWERVE_MARKER:
                return "ARM TO CRATER";
            case SWERVE_LAST_MOVE:
                return "LAST MOVE";
            case SWERVE_DONE:
                return "DONE";

            default:
                return "UNKNOWN! ID = " + revTankState;
        }
    }


    // ***********************************************************************
    // getCurStateName
    // ***********************************************************************
    // Return the name of the current state
    private String getCurStateName() {
        return getStateName(revTankState);
    }


    // ***********************************************************************
    // Init
    // ***********************************************************************
    // Performs any actions that are necessary when the OpMode is enabled.
    // The system calls this member once when the OpMode is enabled.
    @Override
    public void init() {


        double initWheelAngle;
        double initWheelPower;

        swerveDebug(500, "SwerveAuto::init", "START");
        initWheelAngle = .3;
        initWheelPower = 0.02;
        // Run initialization of other parts of the class
        // Note that the class will connect to all of our motors and servos

        super.init();

        swerveDebug(500, "SwerveAuto::init", "Back from super.init");

        //Tensor Flow Initialization
        VuforiaLocalizer.Parameters parameters = new VuforiaLocalizer.Parameters();
//        sets which camera to use
        parameters.cameraDirection = VuforiaLocalizer.CameraDirection.BACK;
        parameters.vuforiaLicenseKey = "AXALhZf/////AAABmeL06CuSFUvSihBEZtVB9MllwYAol1njgG9CAEcNIyohat03TdAACXdYBpbS6M0BCHZAnGChIMBGm0BP2MHKV7IHPsfti2ZwLEf0bZgd/oNwpq+h/YnIhrm4qARe/3sKUsJZo4tlHK+FkFU10vWg0uBHqgfSf1zW/lJbyVhh+h4u8/3y6B6tXG+3yb9zQZECGgJyqifA5sQNyqCP/Wy0O1AY9hgCnbCHeOMChhpaKiGpXM4PNPsDbKo59yEb6QSF8KNciYUQmR7vviirGKFj4TetMNHrgKVPYCQGzmWdKvmCB5sikQ6lelNGHU9Je6sKMScefU0s8Vn5WyToDfddPoNejyrmLkq9jH3ccZ/7Q+gA";

//        infer tells tensor flow which side  it doesnt have in relation to the robot
//        vision = new MasterVision(parameters, hardwareMap, false, MasterVision.TFLiteAlgorithm.INFER_RIGHT);
//        vision.init();// enables the camera overlay
//        vision.enable();// enables the tracking algorithms

        swerveDebug(500, "SwerveAuto::init", "TensorFlow Ready");


        // orient to the field now and save our angle for use in teleOp
        ourSwerve.setFieldOrientation();

        // set initial pushoff delay
        moveTimePushoff = 400;
        autoDriveWait = Boolean.FALSE;
        // force the marker drop servo to hold tight
        gameMarkDrop.setPosition(0);

        // cause all the wheels to turn to the initialization position - 45 degrees
        swerveLeftFront.updateWheel(initWheelPower, -initWheelAngle);
        swerveRightFront.updateWheel(initWheelPower, initWheelAngle);
        swerveLeftRear.updateWheel(initWheelPower, initWheelAngle);
        swerveRightRear.updateWheel(initWheelPower, -initWheelAngle);

        // wait for the wheels to turn
        swerveSleep(500);

        // stop power to the wheels - servos stay locked
        swerveLeftFront.updateWheel(0, -initWheelAngle);
        swerveRightFront.updateWheel(0, initWheelAngle);
        swerveLeftRear.updateWheel(0, initWheelAngle);
        swerveRightRear.updateWheel(0, -initWheelAngle);

        swerveDebug(500, "SwerveAuto::init", "Swerve wheels in init positions");

        // Robot and autonomous settings are read in from files in the core class init()
        // Report the autonomous settings
        // ***** now done in crater & depot *****
        // ***** showAutonomousGoals();

        swerveDebug(500, "SwerveAuto::init", "DONE");
    }


    // ***********************************************************************
    // start
    // ***********************************************************************
    // Do first actions when the start command is given.
    // Called once when the OpMode is started.
    @Override
    public void start() {
        swerveDebug(500, "SwerveAuto::start", "START");

        // Call the super/base class start method.
        super.start();

        // turn off tensorFlow, get particle position

//        vision.disable();
//        goldPosition = vision.getTfLite().getLastKnownSampleOrder();

//        if ( goldPosition == SampleRandomizedPositions.RIGHT) {
//            parPosition = particlePosition.partRight;
//        } else if ( goldPosition == SampleRandomizedPositions.LEFT ) {
//            parPosition = particlePosition.partLeft;
//        } else {
//            parPosition = particlePosition.partCenter;
//        }

        // nothing sensed yet
//        loopSenseStatus = "No sensing yet";

        // Start in the initial robot state
        revTankState = autoStates.SWERVE_INIT;
        setState(autoStates.SWERVE_START, 0);

        swerveDebug(500, "SwerveAuto::start", "DONE");
    }


    // ***********************************************************************
    // loop
    // ***********************************************************************
    // State machine for autonomous robot control
    // Called continuously while OpMode is running
    //
    // Note that since this is autonomous and we do not care about reading
    // joysticks.
    @Override
    public void loop() {
        // Normal logging of loop start

        swerveDebug(500, "SwerveAuto::loop", "START, state is " +
                getCurStateName() + "'");

//        display the target

        swerveDebug(50, "SwerveAuto::loop", "Sensing status: " +
                goldPosition + "'");

        // check for auto drive

        if (autoDriveWait) {
            if (ourSwerve.autoDriveCheck(autoDriveStop)) {
                autoDriveWait = Boolean.FALSE;

                // auto finished in time, so no more waiting
                stateWaitTime = 0.0;
            }
        }

        // if we are waiting, move on

        if (!checkStateReady()) {
            swerveDebug(500, "SwerveAuto::loop", "Waiting for steady state (" +
                    checkReport + ")");

            loopEndReporting();
            return;
        }

        // Move based on the current state

        switch (revTankState) {

//            SHOULD NEVER HAPPEN - INIT while we are running....
            case SWERVE_INIT:
                // INIT is only used to have some state before we set START in start()
                swerveDebug(1, "SwerveAuto::loop *ERROR*", "== loop with INIT state");
                setState(autoStates.SWERVE_START, 0);

                break;

//            First state
            case SWERVE_START:
                // jump to debug if active
                if (debugActive) {
                    setState(debugStartState, 0);
                } else {


                    // start the drop


                    setState(autoStates.SWERVE_ALIGN, 0);
                }
                break;

//            align the wheels before the drop
            case SWERVE_ALIGN:
                ourSwerve.autoDrive(0.2, 85, 0.0, .01);
                autoDriveWait = Boolean.TRUE;
                autoDriveStop = Boolean.TRUE;
                setState(autoStates.SWERVE_DROP, 250);
                break;
//            Drop down from the lander
            case SWERVE_DROP:
                // motor has 383.6 ticks per rev
                climber.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
                climber.setMode(DcMotor.RunMode.RUN_TO_POSITION);
                climber.setTargetPosition(6750);
                if (climber.getCurrentPosition() < 5750) {
                    climber.setPower(1);
                } else {
                    climber.setPower(.6);
                }
                setState(autoStates.SWERVE_DELAY, 2400);
                break;

            case SWERVE_DELAY:
//                                customizable delay adjusts for teammates
                swerveSleep(0);
                setState(autoStates.SWERVE_SLIDE, 700);
                break;

            case SWERVE_SLIDE:
                ourSwerve.autoDrive(.6, -15, 0, 7);
                autoDriveStop = Boolean.TRUE;
                autoDriveWait = Boolean.TRUE;
                if (debugActive) {
                    setState(autoStates.SWERVE_AUTO_TESTING_TURN_BACK, 3000);
                }
                setState(autoStates.SWERVE_HIT_PARTICLE, 3000);
                break;

//            turn towards the particle
//            TODO add triangle math
            case SWERVE_HIT_PARTICLE:
                ourSwerve.autoDrive(.4, -100, 0, 27);
                autoDriveWait = Boolean.TRUE;
                autoDriveStop = Boolean.TRUE;
                setState(autoStates.SWERVE_TURN, 5000);
                break;

            case SWERVE_TURN:
                // gets vSlide out of the way
                vSlide.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
                vSlide.setMode(DcMotor.RunMode.RUN_TO_POSITION);
                vSlide.setTargetPosition(1000);
                vSlide.setPower(1);

                orientRobot(-86);
                if (crater) {
                    setState(autoStates.SWERVE_TURN_TO_PARTICLE, 1700);
                }
                setState(autoStates.SWERVE_GM_EXTEND, 1700);
                break;

            case SWERVE_GM_EXTEND:
                hSlide.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
                hSlide.setMode(DcMotor.RunMode.RUN_TO_POSITION);
                hSlide.setTargetPosition(1900);
                hSlide.setPower(.6);
                setState(autoStates.SWERVE_MARKER, 2000);
                break;

            case SWERVE_MARKER:
                wristL.setPosition(1);
                wristR.setPosition(1);
                intakeL.setPower(-1);
                intakeR.setPower(-1);
                setState(autoStates.SWERVE_GM_BACK, 1000);
                break;

            case SWERVE_GM_BACK:
                wristL.setPosition(0);
                wristR.setPosition(0);
                intakeL.setPower(0);
                intakeR.setPower(0);
                hSlide.setTargetPosition(900);
                hSlide.setPower(-1);
                if (crater) {
                    setState(autoStates.SWERVE_TURN_TO_PARTICLE, 1500);
                } else {
                    setState(autoStates.SWERVE_TURN_TO_PARTICLE, 1500);
                }
                break;

            case SWERVE_TURN_TO_PARTICLE:
                setState(autoStates.SWERVE_GRAB, 1500);
                break;

            case SWERVE_GRAB:
                wristL.setPosition(1);
                wristR.setPosition(1);
                intakeR.setPower(1);
                intakeL.setPower(1);
                setState(autoStates.SWERVE_RETRACT, 2000);
                break;

            case SWERVE_RETRACT:
                hSlide.setTargetPosition(300);
                hSlide.setPower(-.6);
                setState(autoStates.SWERVE_TO_WALL, 5000);
                break;

//            case SWERVE_SCORE:
//                wristR.setPosition(-1);
//                wristL.setPosition(-1);
//                vSlide.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
//                vSlide.setMode(DcMotor.RunMode.RUN_TO_POSITION);
//                vSlide.setTargetPosition(1);
//
//                vSlide.setPower(1);
//                if(vSlide.getTargetPosition() == vSlide.getCurrentPosition()) {
//                    dump.setPosition(1);
//                }
//                break;

//            Move to the wall
            case SWERVE_TO_WALL:
                if (crater) {
                    ourSwerve.autoDrive(.4, -170, 45, 110);
                    autoDriveWait = Boolean.TRUE;
                    autoDriveStop = Boolean.TRUE;
                    setState(autoStates.SWERVE_TO_DEPOT, 3000);
                } else {
                    ourSwerve.autoDrive(.4, -170, 135, 110);
                    autoDriveWait = Boolean.TRUE;
                    autoDriveStop = Boolean.TRUE;
                    setState(autoStates.SWERVE_TO_CRATER, 3000);
                }
                break;

            // Move to the crater
            case SWERVE_TO_CRATER:
                if (crater) {
                    ourSwerve.autoDrive(0.8, 43, 45.0, 30);
                    autoDriveWait = Boolean.TRUE;
                    autoDriveStop = Boolean.TRUE;
                    setState(autoStates.SWERVE_LAST_MOVE, 5000);
                } else {
                    ourSwerve.autoDrive(0.4, 135, 135, 66);
                    autoDriveWait = Boolean.TRUE;
                    autoDriveStop = Boolean.TRUE;
                    setState(autoStates.SWERVE_LAST_MOVE, 5000);
                }
                break;

//            grab two balls from the crater
            case SWERVE_CRATER_PARTICLES:
                setState(autoStates.SWERVE_LAST_MOVE, 5000);
                break;

            // Waiting for final move to complete before done
            // We use this because waiting for this state still gives reporting of wait status

            case SWERVE_LAST_MOVE:
                // stop moving
                ourSwerve.stopRobot();

                setState(autoStates.SWERVE_DONE, 10);
                break;

            // All moves are done

            case SWERVE_DONE:
                // stop any movement
                ourSwerve.stopRobot();
                swerveDebug(500, "Climber Position", "The cllimber postition is " + climber.getCurrentPosition());


                break;


            // **** TEST cases **** //
            // make sure robot is calibrated properly: run a test auton
            case SWERVE_AUTO_TESTING_TURN_BACK:
                orientRobot(-90);
                setState(autoStates.SWERVE_LAST_MOVE, 10000);
                break;

            case SWERVE_AUTO_TESTING_CLIMB:
                climber.setTargetPosition(0);
                climber.setPower(-1);
                break;


            default:
                swerveDebug(1, "SwerveAuto::loop *ERROR*", "== unknown case state " +
                        getCurStateName() + " (" + revTankState + ")");
                setState(autoStates.SWERVE_DONE, 0);
                break;
        }

        // Report changes if not done

        if (debugActive || (revTankState != autoStates.SWERVE_DONE)) {
            loopEndReporting();
        }

        swerveDebug(500, "SwerveAuto::loop", "LOOP PASS DONE");
    }


    // ***********************************************************************
    // stop
    // ***=-*******************************************************************
    // Performs any actions that are necessary when the OpMode is disabled.
    // The system calls this member once when the OpMode is disabled.

    @Override
    public void stop() {
        swerveDebug(500, "SwerveAuto::stop", "START");

//        completely shut down tensorFlow
        vision.shutdown();


        // Call the super/base class stop method
        super.stop();

        swerveDebug(500, "SwerveAuto::stop", "DONE");
    }


    // ***********************************************************************
    // setState
    // ***********************************************************************
    // Set a new state to run, with a delay before it activates
    private void setState(autoStates myTarget, double myDelay) {

        // when debugging, be ready to stop
        if (debugActive && (debugStates-- < 1)) {
            swerveDebug(500, "SwerveAuto::setState", "DEBUG LIMIT - end now");
            myTarget = autoStates.SWERVE_DONE;
        }

        // Report change of state
        if (myTarget != revTankState) {
            swerveDebug(500, "SwerveAuto::setState", "STATE CHANGE--from '" + getCurStateName() + "'  to '" + getStateName(myTarget));
        }
        // Record the starting time for the new state
        stateStartTime = getRuntime();
        // Recored the intended delay
        stateWaitTime = myDelay;
        // set the state...
        revTankState = myTarget;
        // Send telemetry data to the driver station.
        swerveLog("State", "Autonomous State: " + getCurStateName() +
                ", state time = " + swerveNumberFormat.format(getRuntime() - stateStartTime));
    }

    // ***********************************************************************
    // checkStateReady
    // ***********************************************************************
    // Is the target delay past?
    // Any other conditions to wait for before changing?
    private Boolean checkStateReady() {
        // Check current time and requested delay
        if (!checkStateElapsed(stateWaitTime)) {
            return Boolean.FALSE;
        }
        // Add waits for motor positions or anything else here...
        return Boolean.TRUE;
    }

    // ***********************************************************************
    // checkStateElapsed
    // ***********************************************************************
    // Is the target delay past?
    private Boolean checkStateElapsed(double myDelay) {
        double curTime;
        double curDelay;
        curTime = getRuntime();
        curDelay = (curTime - stateStartTime) * 1000;
        // Check current time and requested delay
        if (curDelay < myDelay) {
            checkReport = "== current (" +
                    swerveNumberFormat.format(curDelay) + "' less than '" +
                    swerveNumberFormat.format(myDelay);
            // ONLY show this for very high debug levels, or it will overflow the logs
            swerveDebug(5000, "SwerveAuto::checkStateElapsed", "**Delaying** " + checkReport);

            if (debugLevel < 5000) {
                telemetry.addData("CheckElapsed", checkReport);
            }
            return Boolean.FALSE;
        } else if (debugLevel < 5000) {
            telemetry.addData("CheckElapsed", "--DONE--");
        }
        // Add waits for motor positions or anything else here...
        return Boolean.TRUE;
    }

    // ***********************************************************************
    // orientRobot
    // ***********************************************************************
    // turn the robot to a specific orientation
    private Boolean orientRobot(double newOrientationDegrees) {
        double newOrienation = newOrientationDegrees;
        double turnSpeed;
        // be sure we are not using automation
        ourSwerve.setSwerveMode(SwerveDrive.swerveModes.SWERVE_DRIVER);
        // check robot orientation
        ourSwerve.checkOrientation();
        // turn until within ~10 degrees
        while (Math.abs(newOrienation - ourSwerve.curHeading) > 1.0) {

            while (Math.abs(newOrienation - ourSwerve.curHeading) > 5.0) {
                // never take longer than 1.5 seconds
                if (checkStateElapsed(10000)) {
                    // stop the robot
                    ourSwerve.stopRobot();

                    return (Boolean.FALSE);
                }

                // turn faster if we need to turn more
                if (Math.abs(newOrienation - ourSwerve.curHeading) > 90.0) {
                    turnSpeed = 0.40;
                } else if (Math.abs(newOrienation - ourSwerve.curHeading) > 25.0) {
                    turnSpeed = 0.15;
                } else {
                    motorLeftFront.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
                    motorLeftRear.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
                    motorRightFront.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
                    motorRightRear.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
                    turnSpeed = 0.10;
                }
                if (newOrienation > ourSwerve.curHeading) {
                    turnSpeed = -turnSpeed;
                }

                // turn the robot
                ourSwerve.driveRobot(0.0, 0.0, turnSpeed, 0.0);
            }

            // stop the robot
            ourSwerve.stopRobot();

            motorLeftFront.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
            motorLeftRear.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
            motorRightFront.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
            motorRightRear.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);

            return (Boolean.TRUE);
        }
        return false;
    }
}
