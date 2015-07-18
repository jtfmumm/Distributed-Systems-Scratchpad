package dsscratch.clocks

trait TimeStamp

case class EmptyTimeStamp extends TimeStamp {
  override def toString: String = "<<Empty>>"
}

case class Count(value: Int) extends TimeStamp {
  def inc(): Count = Count(value + 1)

  def >(other: Count) = value > other.value
  def >=(other: Count) = value >= other.value
  def <(other: Count) = value < other.value
  def <=(other: Count) = value <= other.value
  override def toString: String = "<<" + value + ">>"
}