package cecike.core.memory

import chisel3._
import chisel3.util._
import cecike.core.common.Constants._
import cecike.core.common._
import cecike.utils._

class AddressInfo extends Bundle {
  val address = UInt(xLen.W)
  val size = UInt(MemorySize.memSzWidth.W)
}

class StoreInfo extends Bundle {
  val addressInfo = new AddressInfo
  val data = UInt(xLen.W)
}

class MemoryReadPort extends Bundle {
  val addressInfo = EnqIO(new AddressInfo)
  val data = DeqIO(UInt(xLen.W))
}

class MemoryWritePort extends Bundle {
  val storeInfo = EnqIO(new StoreInfo)
}