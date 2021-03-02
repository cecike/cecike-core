package cecike.core.common

import chisel3._
import cecike.core.common.Constants._
import cecike.utils._
import chisel3.util.BitPat

class ControlSignal extends Bundle {
  // TODO: Generate these signals in decode stage
  val instType = UInt(InstructionType.instTypeWidth.W)
  val fuType = UInt(FunctionUnitType.fuTypeWidth.W)
  val fuOp = UInt(functionUnitOpWidth.W)

  // TODO: Generate these signals in decode stage
  val rs1Valid = Bool()
  val rs2Valid = Bool()
  val rdValid = Bool()

  val immediate = UInt(xLen.W)
}

object CS {
  // May add exception here.
  def ok(iType: UInt, fType: UInt, fOp: UInt, rs1: Bool, rs2: Bool, rd: Bool): ControlSignal = {
    val cs = Wire(new ControlSignal)

    cs.instType := iType
    cs.fuType := fType
    cs.fuOp := fOp
    cs.rs1Valid := rs1
    cs.rs2Valid := rs2
    cs.rdValid := rd
    cs.immediate := DontCare

    cs
  }

  def err() = ok(InstructionType.IX,
    FunctionUnitType.FU_ALU, ALUOp.ADD, false.B, false.B, false.B)

  def immediate(i: UInt, iType: UInt): UInt = {
    val immediateTable = Array(
      InstructionType.I -> SignExtension(i(31, 20), xLen),
      InstructionType.S -> SignExtension(i(31, 25) ## i(11, 7), xLen),
      InstructionType.B -> SignExtension(i(31) ## i(7) ## i(30, 25) ## i(11, 8) ## false.B, xLen),
      InstructionType.U -> SignExtension(i(31, 12) ## 0.U(12.W), xLen),
      InstructionType.J -> SignExtension(i(31) ## i(19, 12) ## i(20) ## i(30, 21) ## false.B, xLen)
    )
    BinaryMuxLookUpDefault(iType, 0.U, immediateTable.map(p => (BitPat(p._1), p._2)))
  }
}

class MicroOp extends Bundle {
  val valid = Bool()
  val pc = UInt(xLen.W)
  val instruction = UInt(instructionLen.W)

  // TODO: Detailed design in branch prediction
  val branchPredictionInfo = new BranchPredictionInfo

  val controlSignal = new ControlSignal

  def instType(): UInt = controlSignal.instType
  def fuType(): UInt = controlSignal.fuType
  def fuOp(): UInt = controlSignal.fuOp
  def rs1Valid(): Bool = controlSignal.rs1Valid
  def rs2Valid(): Bool = controlSignal.rs2Valid
  def rdValid(): Bool = controlSignal.rdValid
  def immediate(): UInt = controlSignal.immediate
  def rs1(): UInt = Mux(valid && rs1Valid, instruction(19, 15), 0.U)
  def rs2(): UInt = Mux(valid && rs2Valid, instruction(24, 20), 0.U)
  def rd(): UInt = Mux(valid && rdValid, instruction(11, 7), 0.U)
  def opcode(): UInt = instruction(6, 0)
  def funct3(): UInt = instruction(14, 12)
  def funct7(): UInt = instruction(31, 25)

  val orderInfo = new OrderInfo

  val physicalRs1 = UInt(physicalRegisterAddressWidth.W)
  val physicalRs1Busy = Bool()
  val physicalRs2 = UInt(physicalRegisterAddressWidth.W)
  val physicalRs2Busy = Bool()
  val physicalRd = UInt(physicalRegisterAddressWidth.W)
  val oldPhysicalRd = UInt(physicalRegisterAddressWidth.W)

  val robIndex = UInt(robAddressWidth.W)
}

object MicroOp {
  def apply() = {
    val microOp = WireDefault(new MicroOp, DontCare)

    microOp.valid := false.B
    microOp.controlSignal.instType := InstructionType.IX
    microOp.controlSignal.fuType := FunctionUnitType.FU_ALU

    microOp
  }

  def apply(valid: Bool, pc: UInt, instruction: UInt, branchPredictionInfo: BranchPredictionInfo) = {
    val microOp = WireDefault(new MicroOp, DontCare)

    microOp.valid := valid
    microOp.pc := pc
    microOp.instruction := instruction
    microOp.branchPredictionInfo := branchPredictionInfo

    microOp
  }
}

class SourceRegisterInfo extends Bundle {
  val busy = Bool()
  val addr = UInt(physicalRegisterAddressWidth.W)
}

class IssueMicroOp extends Bundle {
  // Micro Op Info
  val pc = UInt(xLen.W)

  val branchPredictionInfo = new BranchPredictionInfo

  val instType = UInt(InstructionType.instTypeWidth.W)
  val fuType = UInt(FunctionUnitType.fuTypeWidth.W)
  val fuOp = UInt(functionUnitOpWidth.W)

  val rs1Info = new SourceRegisterInfo
  val rs2Info = new SourceRegisterInfo
  val immediate = UInt(xLen.W)

  val rdInfo = UndirectionalValid(UInt(physicalRegisterAddressWidth.W))

  val robIndex = UInt(robAddressWidth.W)
}

object IssueMicroOp {
  def apply(microOpIn: MicroOp) = {
    val microOp = Wire(new IssueMicroOp)
    microOp.pc := microOpIn.pc

    microOp.branchPredictionInfo := microOpIn.branchPredictionInfo

    microOp.instType := microOpIn.instType
    microOp.fuType := microOpIn.fuType
    microOp.fuOp := microOpIn.fuOp

    microOp.rs1Info.addr := microOpIn.physicalRs1
    microOp.rs1Info.busy := microOpIn.physicalRs1Busy

    microOp.rs2Info.addr := microOpIn.physicalRs2
    microOp.rs2Info.busy := microOpIn.physicalRs2Busy

    microOp.immediate := microOpIn.immediate

    microOp.rdInfo.bits := microOpIn.physicalRd
    microOp.rdInfo.valid := microOpIn.rdValid

    microOp.robIndex := microOpIn.robIndex

    microOp
  }
}

class ROBMicroOp extends Bundle {
  val valid = Bool()

  // Status
  // TODO: add exceptions
  val done = Bool()

  val orderInfo = new OrderInfo

  // To update map table
  val rdValid = Bool()
  val logicalRd = UInt(logicalRegisterAddressWidth.W)
  val physicalRd = UInt(physicalRegisterAddressWidth.W)
  val oldPhysicalRd = UInt(physicalRegisterAddressWidth.W)

  // To recover from ...
  val isBranchOp = Bool()
  val branchInfo = new BranchInfo

  def needFlush(): Bool = {
    valid && done && isBranchOp && branchInfo.valid && branchInfo.mispredicted
  }
}

object ROBMicroOp extends Bundle {
  def apply(microOpIn: MicroOp) = {
    val microOp = Wire(new ROBMicroOp)
    microOp.valid := microOpIn.valid

    microOp.done := false.B
    microOp.orderInfo := microOpIn.orderInfo

    microOp.rdValid := microOpIn.rdValid
    microOp.logicalRd := microOpIn.rd()
    microOp.physicalRd := microOpIn.physicalRd
    microOp.oldPhysicalRd := microOpIn.oldPhysicalRd
    microOp.isBranchOp := (microOpIn.fuType & FunctionUnitType.FU_BRU).orR()
    microOp.branchInfo := DontCare

    microOp
  }
}
