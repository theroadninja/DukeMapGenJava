# Notes on using the Cycler Sprite

The cycler is typically paired with a GPSPEED.

Cycler
- lotag: pulse offset
- hitag: optional channel to match switch

GPSpeed
- lotag: pulse speed

## Speed

Above 256, the docs say it switches to a flashing light effect instead of pulsing.

Experiments with a stopwatch and a single computer suggest that a speed of `135`
is about one pulse per second.  Its not perfect, but I was too lazy to find out more,
or to look at the game code.

Some raw notes:
```
// 128 is just UNDER one pulse per second
// 132 is just a bit slower
// 134 gets slower after a while
// 135 is about one second
// 136 is a tiny bit faster
// 145 is a bit faster
// 160 definitely faster than one pulse per second
```


## Periodicity (pulse offsets)

The offsets are used to make a "beam" of light travel a path.
If you want that beam to go in a circle, then the offsets must be set just right.

You are building a circular arrangement of cyclers.  

Definitions:
- `CYCLER_COUNT` is the total number of cyclers in a circle
- "pulse offset" is the lotag of the cycler sprite, which can be though of as a delay before it starts pulsing
- "offset delta" is the _difference_ between the offsets of two adjacent cyclers.

To make light "beams" that don't look funny, the total offset delta must be a multiple of `2048`.
(this does not mean you have a cycler with lotag 2048, because the 2048 one is the same as the 0 one...)

Beam counts:
- 8 beams:  `offset delta = 16384 / CYCLER_COUNT`
- 4 beams:  `offset delta = 8192 / CYCLER_COUNT`
- 3 beams:  `offset delta = 6144 / CYCLER_COUNT`
- 2 beams:  `offset delta = 4096 / CYCLER_COUNT`
- 1 beams:  `offset delta = 2048 / CYCLER_COUNT`
- `1024` just gives a single beam that looks weird


## Troubleshooting

# Cyclers stuck "on"

Check the floor/ceiling tex.  Maybe they are not shaded!
This happened to me with one group.  Turns out I used a blue carpet for the floor which was not shaded.
The walls WERE shaded, but apparently because the floor/ceil were not, the cycler just made all of the walls
bright.


## See Also

- `CyclerMap.scala`