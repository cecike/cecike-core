package cecike.core.memory.tlb

import chisel3._
import chisel3.util._
import cecike.core.common.Constants._
import cecike.core.common._
import cecike.utils._

// there may be some exception/access level bit here
class TLBQueryResult extends Bundle {
  val valid = Bool()
  val physicalAddress = UInt(xLen.W)
}

// the tlb is expected to return result in
// 0 or n cycles - and it's a blocking operation
class TLBQueryPort extends Bundle {
  val virtualAddress = EnqIO(UInt(xLen.W))
  val queryResult = Flipped(Valid(new TLBQueryResult))
}

class TranslationLookasideBufferIO extends Bundle {
  val query = Flipped(new TLBQueryPort)
}

// Dummy module - will implement in the future.
class TranslationLookasideBuffer extends Module {
  val io = IO(new TranslationLookasideBufferIO)

  io.query.virtualAddress.ready := true.B

  io.query.queryResult.valid := io.query.virtualAddress.valid
  io.query.queryResult.bits.valid := io.query.virtualAddress.valid
  io.query.queryResult.bits.physicalAddress := io.query.virtualAddress.bits
}
