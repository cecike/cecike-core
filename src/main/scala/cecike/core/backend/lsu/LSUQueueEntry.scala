package cecike.core.backend.lsu

import chisel3._
import chisel3.util._
import cecike.core.common.Constants._
import cecike.core.common.{Constants, _}
import cecike.utils._

class OpInfo extends Bundle {
  val pc = UInt(xLen.W)
  val rdInfo = UndirectionalValid(UInt(physicalRegisterAddressWidth.W))
  val robIndex = UInt(robAddressWidth.W)
  val fuOp = UInt(functionUnitOpWidth.W)
}

object OpInfo {
  def apply(microOpIn: IssueMicroOp) = {
    val opInfo = Wire(new OpInfo)

    opInfo.pc := microOpIn.pc
    opInfo.rdInfo := microOpIn.rdInfo
    opInfo.robIndex := microOpIn.robIndex
    opInfo.fuOp := microOpIn.fuOp

    opInfo
  }
}

class AddressInfo extends Bundle {
  val address = UInt(xLen.W)
  val size = UInt(MemorySize.memSzWidth.W)
}

class AGUInfo extends Bundle {
  val opInfo = new OpInfo
  val address = new AddressInfo
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

object MemoryDataExtension {
  def apply(data: UInt, fuOp: UInt) : UInt = {
    require(data.getWidth == xLen)
    val resultTable = Array(
      LSUOp.LB -> SignExtension(data(7, 0)),
      LSUOp.LH -> SignExtension(data(15, 0)),
      LSUOp.LW -> SignExtension(data(31, 0))
    )
    MuxLookup(fuOp, data, resultTable)
  }
}