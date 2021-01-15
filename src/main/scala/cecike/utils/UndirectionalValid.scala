package cecike.utils

import chisel3._

class UndirectionalValid[+T <: Data](gen: T) extends Bundle {
  val valid = Bool()
  val bits  = gen

  def fire(): Bool = valid

  override def cloneType: this.type = UndirectionalValid(gen).asInstanceOf[this.type]
}

object UndirectionalValid {
  def apply[T <: Data](gen: T): UndirectionalValid[T] = new UndirectionalValid(gen)
}