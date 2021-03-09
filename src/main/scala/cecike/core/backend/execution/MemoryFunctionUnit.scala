package cecike.core.backend.execution

import cecike.core.backend.lsu.OpInfo
import chisel3._
import chisel3.util._
import cecike.core.common.Constants._
import cecike.core.common._
import cecike.utils._

class MemoryFunctionUnit extends FunctionUnit(false, false, false, true) {
  val microOpIn = io.microOpIn

  io.fuType := FunctionUnitType.fuTypeCode(hasLSU = true)
  // Stage 1 - read register and select src
  io.rsRead(0).addr := io.microOpIn.bits.rs1Info.addr
  io.rsRead(1).addr := io.microOpIn.bits.rs2Info.addr

  val rs1 = RegInit(0.U(64.W))
  val rs2 = RegInit(0.U(64.W))

  val stage2MicroOp = Reg(UndirectionalValid(new IssueMicroOp))

  when (reset.asBool || io.flush) {
    stage2MicroOp.valid := false.B
  } otherwise {
    when (!io.loadStoreInfo.aguInfo.valid || io.loadStoreInfo.aguInfo.ready) {
      stage2MicroOp := microOpIn
      rs1 := io.rsRead(0).data
      rs2 := io.rsRead(1).data
    }
  }

  val op = stage2MicroOp.bits
  val opValid = stage2MicroOp.valid

  val length = stage2MicroOp.bits.fuOp(1, 0)
  val signExtension = stage2MicroOp.bits.fuOp(2)
  val load = !stage2MicroOp.bits.fuOp(3)

  val address = rs1 + stage2MicroOp.bits.immediate

  val aguInfo = io.loadStoreInfo.aguInfo
  aguInfo.valid := opValid && !io.flush
  aguInfo.bits.data := rs2
  aguInfo.bits.load := load
  aguInfo.bits.signExtension := signExtension

  aguInfo.bits.address.address := address
  aguInfo.bits.address.size := length

  aguInfo.bits.opInfo := OpInfo(stage2MicroOp.bits)

  io.microOpIn.ready := aguInfo.ready

  io.readyRd := io.loadStoreInfo.readyRd
  io.readyROB := io.loadStoreInfo.readyROB
  io.rdWrite := io.loadStoreInfo.rdWrite
}
