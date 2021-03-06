package cecike.core.backend.execution

import cecike.CecikeModule
import cecike.core.backend.lsu.{AGUInfo, LoadStoreInfo}
import cecike.core.backend.register.{RegisterFileReadPort, RegisterFileWritePort}
import chisel3._
import chisel3.util._
import cecike.core.common.Constants._
import cecike.core.common._

class FunctionUnitIO(val hasBRU: Boolean, val hasLSU: Boolean) extends Bundle {
  val flush = Input(Bool())
  val microOpIn = DeqIO(new IssueMicroOp)
  val rsRead = Vec(2, Flipped(new RegisterFileReadPort))

  val fuType = Output(UInt(FunctionUnitType.fuTypeWidth.W))
  val readyROB = Valid(UInt(robAddressWidth.W))
  val readyRd = Valid(UInt(physicalRegisterAddressWidth.W))
  val rdWrite = Flipped(new RegisterFileWritePort)
  val branchInfo = if (hasBRU) Output(new BranchInfo) else null
  val loadStoreInfo = if (hasLSU) new LoadStoreInfo else null
}

abstract class FunctionUnit(hasALU: Boolean, hasBRU: Boolean, hasMDU: Boolean, hasLSU: Boolean) extends CecikeModule {
  require(!(hasMDU && hasBRU))
  require(!(hasLSU && (hasALU || hasBRU || hasMDU)))
  val io = IO(new FunctionUnitIO(hasBRU, hasLSU))
}
