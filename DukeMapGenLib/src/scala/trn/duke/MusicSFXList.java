package trn.duke;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * TODO - useful:  http://infosuite.duke4.net/index.php?page=references_sound_list
 */
public class MusicSFXList {

    public static<T> List<T> concat(List<T>... lists){
        // TODO - make a MultiList data structure
        List<T> result = new ArrayList<>();
        for(List<T> list : lists){
            result.addAll(list);
        }
        return result;
    }
    public static<T> List<T> concat(List<List<T>> lists){
        List<T> result = new ArrayList<>();
        for(List<T> list: lists){
            result.addAll(list);
        }
        return result;
    }

    public static class DUKE_VOCALS { // TODO - rename to DUKE_QUOTES ?
        public static List<Integer> ALL = Arrays.asList(new Integer[]{
                29, 33, 37, 56, 67, 72, 78, 107, 177, 180, 189, 190, 192, 193, 195, 196, 197, 199, 201, 202, 203,
                206, 207, 208, 214, 216, 218, 221, 222, 223, 224, 225, 226, 227, 228, 229, 235, 236, 237, 238, 239,
                240, 250, 251, 252, 263, 264, 265, 266, 267, 268, 271, 273, 278, 284, 285, 289, 290, 294, 302,
                269 // 269 is "suck it down" which i guess is only for fat commander death?
        }) ;
    }
    public static class DUKE_NOISES {
        public static List<Integer> ALL = Arrays.asList(new Integer[]{
                25, 27, 28, 36, 38, 40, 41, 42, 43, 48, 68, 200, 209, 211, 215, 245, 270, 274, 275, 276, 280, 304, 305,
                306, 307
        });
    }

    public static class DOOR_SOUNDS {
        public static List<Integer> ALL = Arrays.asList(new Integer[]{
                74, 165, 166, 167, 256, 257, 258, 259, 260, 262, 286, 287
        });
    }

    public static class ENEMIES {
        public static class Slimer {
            public static List<Integer> ALL = Arrays.asList(new Integer[]{
               283, 26, 163, 34, 149
            });
        }
        public static class Octabrain {
            public static List<Integer> ALL = Arrays.asList(new Integer[]{
                    141, 140, 142, 160, 159, 143, 144
            });
        }
        public static class Trooper {
            public static List<Integer> ALL = Arrays.asList(new Integer[]{
                    111, 110, 204, 112, 113, 61, 114
            });
        }
        public static class Pigcop {
            public static List<Integer> ALL = Arrays.asList(new Integer[]{
                    121, 120, 205, 253, 122, 123, 124, 198
            });
        }
        public static class PigcopRecon {
            public static List<Integer> ALL = Arrays.asList(new Integer[]{
                    126, 125, 127, 128, 129
            });
        }
        public static class Enforcer { // the lizard with the chaingun
            public static List<Integer> ALL = Arrays.asList(new Integer[]{
                    117, 115, 116, 64, 118, 119
            });
        }
        public static class SentryDrone {
            public static List<Integer> ALL = Arrays.asList(new Integer[]{
                    131, 130, 255, 132, 157, 133 // Note:  I dont hear anything for 130 (supposed to be drone roaming)
            });
        }
        public static class AssaultCommander { // Fat commander
            public static List<Integer> ALL = Arrays.asList(new Integer[]{
                    136, 135, 137, 155, 138, 139
            });
        }
        public static class BossEp1 {
            public static List<Integer> ALL = Arrays.asList(new Integer[]{
                    97, 156, 96, 154, 98, 99, 100, 181, 230
            });
        }
        public static class BossEp2 {
            public static List<Integer> ALL = Arrays.asList(new Integer[]{
                    102, 101, 103, 104, 105
            });
        }
        public static class BossEp3 {
            public static List<Integer> ALL = Arrays.asList(new Integer[]{
                    151, 150, 153, 108
            });
        }

        public static List<Integer> ALL = concat(Slimer.ALL, Octabrain.ALL, Trooper.ALL, Pigcop.ALL, PigcopRecon.ALL, Enforcer.ALL, SentryDrone.ALL, AssaultCommander.ALL, BossEp1.ALL, BossEp2.ALL, BossEp3.ALL);
    }

    public static class WEAPON_SOUNDS {
        public static List<Integer> ALL = Arrays.asList(new Integer[]{
                0, 1, 2, 3, 4, 5, 109, 169, 6, 7, 9, 13, 14, 11, 246, 12, 15, 16, 17, 10, 303, 219
        });
    }
    public static class INVENTORY {
        public static List<Integer> ALL = Arrays.asList(new Integer[]{
                49, 50, 51, 39, 106, 210, 217, 213
        });
    }

    public static class MISC {
        public static List<Integer> ALL = Arrays.asList(new Integer[]{
                158, 183, 241, 242, 164, 254, 291, 292, 293,
                288, 295, 296, 297, 298, 299, 300, 301, 30 // "end of episode sounds" ?
        });
    }

    public static class MISC_INTERACTIVE {
        public static List<Integer> ALL = Arrays.asList(new Integer[]{
                8, 18, 19, 20, 22, 35, 71, 73, 70, 76, 79, 83, 148, 174, 212, 243, 248, 261, 272
        });
    }

    public static class AMBIENCE {
        public static List<Integer> ALL = Arrays.asList(new Integer[]{
                21, 69, 75, 81, 82, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 95, 168, 170, 171, 172, 173, 175, 178, 179,
                184, 185, 186, 187, 188, 194, 220, 231, 232, 233, 234, 244, 247, 249, 277, 279, 281, 282, 308
        });

    }

    public static List<List<Integer>> ALL_LISTS = new ArrayList<List<Integer>>(){{
        add(DUKE_VOCALS.ALL);
        add(DUKE_NOISES.ALL);
        add(DOOR_SOUNDS.ALL);
        add(ENEMIES.ALL);
        add(WEAPON_SOUNDS.ALL);
        add(INVENTORY.ALL);
        add(MISC.ALL);
        add(MISC_INTERACTIVE.ALL);
        add(AMBIENCE.ALL);
    }};

    public static List<Integer> ALL = concat(ALL_LISTS);

    // TODO - unit test that ensures none of these overlap

    public static int SECRET_LEVEL = 183;
}
