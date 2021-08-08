# XRepeat Explanation

See also [YRepeat Explanation](YRepeat.md)

This page describes the `xrepeat` field (a.k.a. `x-repeat`) of the walls in a Build map.  I don't know if this information is relevant to `yrepeat` also; I haven't tested that yet.

Also, it is likely that xrepeat can be defined more simply by describing how the build engine uses it to execute, however I want definitions that are complete on their own.

Short version:  `xrepeat` controls the horizontal scaling of a wall texture.

Long version: the `xrepeat` value is the inverse of the scale factor, though _also_ multipled by the wall length, as well as a constant.  The `xrepeat`values _does not_ actually tell you how many times a texture repeats -- for that you need to do a calculation involving the texture size.

Fun fact:  at normal scaling, a 64px texture fits perfectly on a 1024 wall (1024 = 1 of the largest grid units in the build editor).
## Terms

- world units:  the units used by the build engine for x,y map coordinates (z has a different scale)
- xrepeat: the build engine `xrepeat` value
- repeat:  the number of times a texture repeats on a wall
- tex_size_px:  the width of the 2d texture image, in pixels
- world_tex_size: the width of the texture in world units at default scaling
- wall size:  the length of the wall in world units
- scale: the scaling factor (e.g. 1 for normal scaling, 2 for twice as big, 0.5 for half as big)


## Definitons

```
world_tex_size = tex_width_px * 16
```

`xrepeat` is a dependent variable only on wall size
```
          wall size
xrepeat = -----------
          128 * scale
```

`repeat` depends on both wall size and tex_size_px 
```
         wall size
repeat = -------------------------
         tex_width_px * 16 * scale
```

Relationships to each other:
```
          repeat * tex_width_px
xrepeat = -------------
               8

          xrepeat * 8
repeat = ------------------
         tex_width_px
```

Scale:
```
        wall size
scale = ------------
        128 * xrepeat

        wall size
scale = --------------
        tex_width_px * 16 * repeat
```

## Formulas

How do I fit `n` repeats on a wall? You need a `f(repeats, tex_width_px)`:
```
          repeat * tex_width_px
xrepeat = ---------------------
                  8
```

