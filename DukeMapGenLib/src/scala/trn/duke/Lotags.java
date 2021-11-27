package trn.duke;

public class Lotags {

    public static final class SE {
        public static final int ROTATE = 0;
        public static final int ROTATE_PIVOT = 1;
        public static final int QUAKE = 2;
        public static final int RANDOM_LIGHTS_SHOT = 3; // random lights after shot out
        public static final int RANDOM_LIGHTS = 4;
        // 5 is reserved
        public static final int SUBWAY = 6;
        public static final int TELEPORT = 7;
        public static final int UP_OPEN_DOOR_LIGHTS = 8;
        public static final int DOWN_OPEN_DOOR_LIGHTS = 9;
        public static final int DOOR_AUTO_CLOSE = 10;
        public static final int ROTATE_DOOR = 11;
        public static final int LIGHT_SWITCH = 12;
        public static final int EXPLOSIVE = 13;
        public static final int SUBWAY_CAR = 14;
        public static final int SLIDE_DOOR = 15;
        // 16 is reserved
        public static final int ELEVATOR = 17;
        // 18 is reserved
        public static final int SHOT_TOUCHPLATE_CEIL_DOWN = 19;
        public static final int BRIDGE = 20;
        public static final int DROP_FLOOR = 21;
        public static final int PRONG = 22;
        // 23 is reserved
        public static final int CONVEYOR = 24;
        public static final int PISTON = 25;
        // 26 is reserved
        public static final int CAMERA = 27; // for demos
        // 28 is reserved
        public static final int FLOATING_SECTOR = 29; // for waves
        public static final int TWO_WAY_TRAIN = 30;
        public static final int FLOOR_RISE = 31;
        public static final int CEIL_FALL = 32;
        public static final int QUAKE_JIBS = 33;
        // 34 is reserved
        // 35 is reserved
        public static final int SPAWN_SHOT = 36;

    }

    public static final class ST {
        public static final int WATER_ABOVE = 1;
        public static final int WATER_BELOW = 2;
        public static final int ELEVATOR = 15; // matches with SE 17  (maybe should call this ELEVATOR_TRANSPORT)

        public static final int CEILING_DOOR = 20;

        public static final int LIFT_DOWN = 16; // no transport; floor only moves, starts high
        public static final int LIFT_UP = 17; // no transport, floor only moves, starts low
        public static final int ELEVATOR_DOWN = 18;  // no transport; floor + ceiling move, starts high
        public static final int ELEVATOR_UP = 19;  // no transport; floor + ceiling move, starts low
    }
}
