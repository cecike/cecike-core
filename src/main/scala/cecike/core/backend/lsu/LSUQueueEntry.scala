package cecike.core.backend.lsu

import chisel3._
import chisel3.util._
import cecike.core.common.Constants._
import cecike.core.common._
import cecike.utils._

class OpInfo extends Bundle {
  val pc = UInt(xLen.W)
  val rdInfo = UndirectionalValid(UInt(physicalRegisterAddressWidth.W))
  val robIndex = UInt(robAddressWidth.W)
}

object OpInfo {
  def apply(microOpIn: IssueMicroOp) = {
    val opInfo = Wire(new OpInfo)

    opInfo.pc := microOpIn.pc
    opInfo.rdInfo := microOpIn.rdInfo
    opInfo.robIndex := microOpIn.robIndex

    opInfo
  }
}

class Address extends Bundle {
  val address = UInt(xLen.W)
  val length = UInt(MemorySize.memSzWidth.W)
}

class AGUInfo extends Bundle {
  val opInfo = new OpInfo
  val address = new Address
  val load = Bool()
  val data = UInt(xLen.W)
  val signExtension = Bool()
}

class Status extends Bundle {
  val exception = Bool() // Reserved for future use
}

class LSUEntry extends Bundle {
  val aguInfo = new AGUInfo
  val status = new Status
}
