package cecike.utils

import chisel3._

class UndirectionedValid[+T <: Data](gen: T) extends Bundle {
  val valid = Bool()
  val bits  = gen

  def fire(): Bool = valid

  override def cloneType: this.type = UndirectionedValid(gen).asInstanceOf[this.type]
}

object UndirectionedValid {
  def apply[T <: Data](gen: T): UndirectionedValid[T] = new UndirectionedValid(gen)
}