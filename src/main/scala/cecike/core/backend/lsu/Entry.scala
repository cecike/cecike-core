package cecike.core.backend.lsu

import chisel3._
import chisel3.util._
import cecike.core.common.Constants._
import cecike.core.common._
import cecike.utils._

class OpInfo extends Bundle {
  val pc = UInt(xLen.W)
  val branchTag = UInt(branchTagWidth.W)
  val rdInfo = UndirectionalValid(UInt(physicalRegisterAddressWidth.W))
  val robIndex = UInt(robAddressWidth.W)
}

class Address extends Bundle {
  val address = UInt(xLen.W)
  val length = UInt(3.W)
}

class AGUInfo extends Bundle {
  val opInfo = new OpInfo
  val address = new Address
}

class Status extends Bundle {
  val allocated = Bool()
  val valid = Bool()
  val done = Bool()
  val exception = Bool() // Reserved for future use
}

abstract class CommonLSUQueueEntry extends Bundle {
  val aguInfo = new AGUInfo
  val status = new Status

  def pending(): Bool = {
    status.allocated && status.valid && !status.done
  }
}

class StoreQueueEntry extends CommonLSUQueueEntry {
  val data = UInt(xLen.W)

  def matched(addr: UInt): Bool = {
    require(addr.getWidth == xLen)
    pending() && addr(xLen - 1, 3) === aguInfo.address.address(xLen - 1, 3)
  }
}

class LoadQueueEntry extends CommonLSUQueueEntry {
  // No more data need here.
}