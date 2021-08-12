package trn.render

case class Texture(picnum: Int, widthPx: Int) {

  /**
    * Calculates the "xrepeat" value needed to make the texture repeat `repetitionCount` times before the wall ends.
    * @param repetitionCount how many times you want to make the texture repeat
    * @return xrepeat value
    */
  def xRepeatForNRepetitions(repetitionCount: Int): Int = {
    require(repetitionCount > 0)
    repetitionCount * widthPx / 8
  }

  /**
    * Calculates the "xrepeat" value needed to make the texture a certain size, where 1.0 is "no scaling"
    * @param scaleFactor
    * @param wallSize
    * @return
    */
  def xRepeatForScaleF(scaleFactor: Double, wallSize: Int): Int = {
    require(scaleFactor > 0 && wallSize > 0)
    (wallSize / (128 * scaleFactor)).toInt
  }
}
