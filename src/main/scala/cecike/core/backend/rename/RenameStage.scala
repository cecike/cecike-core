package cecike.core.backend.rename

import chisel3._
import chisel3.util._
import cecike.core.common.Constants._
import cecike.core.common.{BranchInfo, MicroOp}
import cecike.utils._

class RenameStageIO extends Bundle {
  val microOpIn = Flipped(DecoupledIO(Vec(decodeWidth, new MicroOp)))
  val microOpOut = DecoupledIO(Vec(decodeWidth, new MicroOp))

  // TODO: Inputs from ROB and compute units
  val flush = Input(Bool())
  val rdCommitPort = Vec(decodeWidth, new MapTableWritePort)
  val rdCommitDeallocateReqPort = Input(Vec(decodeWidth, Valid(UInt(physicalRegisterAddressWidth.W))))

  val backendWritePort = Input(Vec(issueWidth, Valid(UInt(physicalRegisterAddressWidth.W))))

  val branchInfo = Input(new BranchInfo)
  // Debug port
  val debug = Output(Vec(decodeWidth, new MicroOp))
}

class RenameStage extends Module {
  val io = IO(new RenameStageIO)

  // External stall
  val externalStall = io.microOpOut.valid && !io.microOpOut.ready

  // Rename stage 1 - read free list and
  // map table while generate order info.
  // Note that logical register index ZERO should
  // not allocate a physical register and use physical
  // register index ZERO instead.
  val freeList = Module(new FreeList)
  val mapTable = Module(new MapTable)
  val orderDetector = Module(new OrderDetector)

  // Connect signals from ROB
  freeList.io.deallocateReq := io.rdCommitDeallocateReqPort
  mapTable.io.rdCommitPort := io.rdCommitPort
  mapTable.io.flush := io.flush

  // Rs, Rd of input microOp
  val rs1s = io.microOpIn.bits.map(_.rs1())
  val rs2s = io.microOpIn.bits.map(_.rs2())
  val rds = io.microOpIn.bits.map(_.rd())
  val rdsValid = rds.map(_.orR && !io.flush && !externalStall && io.microOpIn.valid)
  val isBranch = io.microOpIn.bits.map { p =>
    p.isBranchInstruction && io.microOpIn.valid
  }

  // Connects input signals
  freeList.io.branchInfo := io.branchInfo
  for (i <- 0 until decodeWidth) {
    freeList.io.newBranch(i) := isBranch(i)
    freeList.io.allocateReq(i) := rdsValid(i)

    mapTable.io.rs1ReadPort(i).logicalAddr := rs1s(i)
    mapTable.io.rs2ReadPort(i).logicalAddr := rs2s(i)
    mapTable.io.rdReadPort(i).logicalAddr := rds(i)
    mapTable.io.rdWritePort(i).logicalAddr := rds(i)

    orderDetector.io.rs1(i) := rs1s(i)
    orderDetector.io.rs2(i) := rs2s(i)
    orderDetector.io.rd(i) := rds(i)
    orderDetector.io.valid(i) := rdsValid(i)
  }

  // Results from submodule
  val freeListResult = freeList.io.allocateResp.bits

  // Stall here
  io.microOpIn.ready := freeList.io.allocateResp.valid && !externalStall

  val physicalRs1 = mapTable.io.rs1ReadPort.map(_.physicalAddr)
  val physicalRs2 = mapTable.io.rs2ReadPort.map(_.physicalAddr)
  val physicalRd = mapTable.io.rdReadPort.map(_.physicalAddr)

  val orderInfo = orderDetector.io.info

  val writesToMapTable = Wire(Vec(decodeWidth, Bool()))
  for (i <- 0 until decodeWidth) {
    writesToMapTable(i) :=
      orderInfo(i).validWithMask((~(0.U(decodeWidth.W))).asUInt, i) &&
        freeList.io.allocateResp.valid
  }

  // Connects to map table
  for (i <- 0 until decodeWidth) {
    mapTable.io.rdWritePort(i).valid := writesToMapTable(i)
    mapTable.io.rdWritePort(i).physicalAddr := freeListResult(i)
  }

  // We will write our results here.
  val stage1MicroOp = Wire(Vec(decodeWidth, new MicroOp))
  stage1MicroOp := io.microOpIn.bits
  io.debug := stage1MicroOp

  for (i <- 0 until decodeWidth) {
    stage1MicroOp(i).physicalRs1 := physicalRs1(i)
    stage1MicroOp(i).physicalRs2 := physicalRs2(i)
    stage1MicroOp(i).oldPhysicalRd := physicalRd(i)
    stage1MicroOp(i).physicalRd := Mux(io.microOpIn.bits(i).rdValid, freeListResult(i), 0.U)
    stage1MicroOp(i).orderInfo := orderInfo(i)
  }

  // Partial result buffer
  val outputValid = RegInit(false.B)
  val microOpReg = Reg(Vec(decodeWidth, new MicroOp))

  val outputValidNext = Wire(Bool())
  val microOpRegNext = Wire(Vec(decodeWidth, new MicroOp))

  // Default value
  outputValidNext := io.microOpIn.fire()
  microOpRegNext := stage1MicroOp

  // Update register when no external stall
  when (!externalStall) {
    outputValid := outputValidNext
    microOpReg := microOpRegNext
  }

  val stage2MicroOp = Wire(Vec(decodeWidth, new MicroOp))
  stage2MicroOp := microOpReg

  // Rename stage 2 - read busy table and mask result by order info
  val busyTable = Module(new BusyTable)

  // Connect signals from backend and ROB
  busyTable.io.backendWritePort := io.backendWritePort
  busyTable.io.flush := io.flush

  // Physical rs, rd of input
  val stage2PhysicalRs1 = microOpReg.map(_.physicalRs1)
  val stage2PhysicalRs2 = microOpReg.map(_.physicalRs2)
  val stage2PhysicalRd = microOpReg.map(_.physicalRd)
  val stage2RdValid = stage2PhysicalRd.map(_.orR)

  // Connect to submodules
  for (i <- 0 until decodeWidth) {
    busyTable.io.rs1ReadPort(i).addr := stage2PhysicalRs1(i)
    busyTable.io.rs2ReadPort(i).addr := stage2PhysicalRs2(i)
    busyTable.io.rdWritePort(i).bits := stage2PhysicalRd(i)
    busyTable.io.rdWritePort(i).valid := stage2RdValid(i)
  }

  // Extract busy info
  val rs1Busy = busyTable.io.rs1ReadPort.map(_.busy)
  val rs2Busy = busyTable.io.rs2ReadPort.map(_.busy)

  // Write our results
  for (i <- 0 until decodeWidth) {
    val maskRs1 = microOpReg(i).orderInfo.rs1Info
    val maskRs2 = microOpReg(i).orderInfo.rs2Info

    stage2MicroOp(i).physicalRs1Busy := Mux(maskRs1.valid, true.B, rs1Busy(i))
    stage2MicroOp(i).physicalRs2Busy := Mux(maskRs2.valid, true.B, rs2Busy(i))

    when (maskRs1.valid) {
      stage2MicroOp(i).physicalRs1 := maskRs1.bits
    }

    when (maskRs2.valid) {
      stage2MicroOp(i).physicalRs2 := maskRs2.bits
    }
  }

  // Generate output
  io.microOpOut.valid := outputValid && !io.flush
  io.microOpOut.bits := stage2MicroOp
}
