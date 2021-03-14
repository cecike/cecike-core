package cecike

import chisel3.internal.sourceinfo.SourceInfo
import chisel3._

abstract class CecikeModule(implicit moduleCompileOptions: CompileOptions) extends Module {
  val moduleName = this.name
  val prefix = ":[" + moduleName + "]: "

  object log {
    def apply(fmt: String, data: Bits*)(implicit sourceInfo: SourceInfo, compileOptions: CompileOptions): Unit = {
      printf(prefix + fmt + "\n", data: _*)(sourceInfo, compileOptions)
    }

    def apply(pable: Printable)(implicit sourceInfo: SourceInfo, compileOptions: CompileOptions): Unit = {
      printf(Printables(List(PString(prefix), pable, PString("\n"))))(sourceInfo, compileOptions)
    }

    def apply(cond: Bool, fmt: String, data: Bits*)(implicit sourceInfo: SourceInfo, compileOptions: CompileOptions): Unit = {
      when (cond) {
        apply(fmt, data: _*)(sourceInfo, compileOptions)
      }(sourceInfo, compileOptions)
    }

    def apply(cond: Bool, pable: Printable)(implicit sourceInfo: SourceInfo, compileOptions: CompileOptions): Unit = {
      when (cond) {
        apply(pable)(sourceInfo, compileOptions)
      }(sourceInfo, compileOptions)
    }
  }
}
