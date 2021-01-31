package cecike.utils

import chisel3._
import chisel3.util._

class RingBufferManagerIO(val depth: Int, val n: Int, val m: Int) extends Bundle {
  val req = Flipped(Valid(Vec(n, Bool())))
  val resp = Valid(Vec(n, UInt(log2Ceil(depth).W)))
  val deallocate = Input(Vec(m, Bool()))
  val valid = Output(UInt(depth.W))
}

class RingBufferManager(depth: Int, n: Int, m: Int) extends Module {
  require(isPow2(depth))
  val io = IO(new RingBufferManagerIO(depth, n, m))

  val addrWidth = log2Ceil(depth)

  val valid = RegInit(0.U(depth.W))
  val head = RegInit(0.U(addrWidth.W))
  val tail = RegInit(0.U(addrWidth.W))
  val entryNum = RegInit(0.U((addrWidth + 1).W))

  val headOffset = PopCount(io.deallocate)
  val tailOffset = PopCount(io.req.bits)

  val nextHead = Wire(UInt(addrWidth.W))
  val nextTail = Wire(UInt(addrWidth.W))
  val nextValid = Wire(UInt((depth << 1).W))

  val nextEntryNumBoth = entryNum + tailOffset - headOffset
  val nextEntryNumDeallocateOnly = entryNum - headOffset
  val reqValid = !nextEntryNumBoth(addrWidth) || !(nextEntryNumBoth(addrWidth - 1, 0).orR())
  val nextEntryNum = Mux(io.req.valid && reqValid, nextEntryNumBoth, nextEntryNumDeallocateOnly)

  nextHead := head + headOffset
  nextTail := tail + tailOffset
  nextValid := (~((~(0.U(depth.W))) << nextEntryNum)).asUInt()(depth - 1, 0) << nextHead

  head := nextHead
  entryNum := nextEntryNum
  valid := nextValid((depth << 1) - 1, depth) | nextValid(depth - 1, 0)

  when (io.req.valid && reqValid) {
    tail := nextTail
  }

  io.valid := valid

  val offset = Wire(Vec(n + 1, UInt(addrWidth.W)))
  offset(0) := 0.U

  io.resp.valid := reqValid
  for (i <- 0 until n) {
    io.resp.bits(i) := tail + offset(i)
    offset(i + 1) := offset(i) + io.req.bits(i).asUInt
  }
}
