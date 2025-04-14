# TODO

This site still seems active, so message here when this is ready
to share:  

https://forums.duke4.net/


Check out this recent blast radius mod:


https://www.moddb.com/mods/duke-nukem-3d-blast-radius/news/youre-not-supposed-to-be-here-level-lore-sunset-suicide-blast-radius-level-1brl1map


## How walls are defined

Walls are defind clockwise from the point of view of the Build editor (where Y is reversed)


So a box in build defined by \[P1, P2, P3, P4\] would look like this:

```
/\
|  -Y

    P1 ------> P2
    /\          |
    |           |
    |           |
    |          \/
    P4 <------ P3

 | +Y
\/


```