# YRepeat Explanation

See also [XRepeat Explanation](XRepeat.md)

This is a work in progress.  In contract to xrepeat, the yrepeat value seems to have no relationship
to the actual height of the wall.  Yrepeat seems to be a simple inverse scaling factor; changing it
makes the texture on the wall universally taller or shorter no matter where it is.

The formula I have so far:
```
                   2048
tex_height_px *  ---------  =  painted_height_z
                  yrepeat
```

- tex_height_px is the raw height of the texture
- 2048 is a factor (that contains the conversion to z coordinates)
- yrepeat is the walls yrepeat value
- painted_height_z is the actual height, in bit-shifted z coordinates, 
of a single repetition of the texture as painted on a wall.

For example, a texture with a height of 256, and the default scaling of yrepeat=8 will perfectly
match a room that is 256 * (2048/8) = 65536 z-units high.

Note: yrepeat can be set to zero; looks super ugly.