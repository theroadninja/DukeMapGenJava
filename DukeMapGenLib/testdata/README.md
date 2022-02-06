

## Test Data

See [JavaTestUtils.java](../test/java/trn/JavaTestUtils.java) for java and
[TestUtils.scala](../test/scala/trn/prefab/TestUtils.scala) for scala.



## Notes on the Build Format:



COORDINATE SYSTEM
x+ goes to the right / east
y+ goes down / south




Sector with a hole in the middle (e.g. four walls but its not an island sector).  E.g.:
   +-----------+
   |           |
   |   +---+   |
   |   |   |   |
   |   +---+   |
   |           |
   +-----------+
   
   For this the sectors "firstwall" index points to the outer wall.  The inner walls are included in the level, but
   don't appear to be associated with any sector.
   
   UNKNOWN:  what if there are overlapping sectors ("sectors over sectors") -- how does it know which sector to draw
   the inner walls in?