#ifndef ELFLOADER_ELF64_H
#define ELFLOADER_ELF64_H

#ifdef __cplusplus
#include <cstdint>
#include <cstdio>
#elif
#include <stdint.h>
#include <stdio.h>
#endif

#ifdef __cplusplus
extern "C" {
#endif

typedef uint64_t Elf64_Addr;
typedef uint64_t Elf64_Off;
typedef uint16_t Elf64_Half;
typedef uint32_t Elf64_Word;
typedef int32_t Elf64_Sword;
typedef uint64_t Elf64_Xword;
typedef int64_t Elf64_Sxword;

typedef uint32_t Elf32_Addr;
typedef uint32_t Elf32_Off;
typedef uint16_t Elf32_Half;
typedef uint32_t Elf32_Word;

#define EI_NIDENT 16

// ELF file header
typedef struct {
    unsigned char       e_ident[EI_NIDENT];    // ELF indent
    Elf64_Half          e_type;         // Object file type
    Elf64_Half          e_machine;      // Machine type
    Elf64_Word          e_version;      // Object file version
    Elf64_Addr          e_entry;        // Entry point address
    Elf64_Off           e_phoff;        // Program header offset
    Elf64_Off           e_shoff;        // Section header offset
    Elf64_Word          e_flags;        // Processor-specific flags
    Elf64_Half          e_ehsize;       // ELF header size
    Elf64_Half          e_phentsize;    // Size of program header entry
    Elf64_Half          e_phnum;        // Number of program header entries
    Elf64_Half          e_shentsize;    // Size of section header entry
    Elf64_Half          e_shnum;        // Number of section header entries
    Elf64_Half          e_shstrndx;     // Section name string table index
} Elf64_Ehdr;

typedef struct {
    unsigned char       e_ident[EI_NIDENT];
    Elf32_Half          e_type;
    Elf32_Half          e_machine;
    Elf32_Word          e_version;
    Elf32_Addr          e_entry;
    Elf32_Off           e_phoff;
    Elf32_Off           e_shoff;
    Elf32_Word          e_flags;
    Elf32_Half          e_ehsize;
    Elf32_Half          e_phentsize;
    Elf32_Half          e_phnum;
    Elf32_Half          e_shentsize;
    Elf32_Half          e_shnum;
    Elf32_Half          e_shstrndx;
} Elf32_Ehdr;

// Elf ident
#define EI_MAG0         0
#define EI_MAG1         1
#define EI_MAG2         2
#define EI_MAG3         3
#define EI_CLASS        4
#define EI_DATA         5
#define EI_VERSION      6
#define EI_OSABI        7
#define EI_ABIVERSION   8
#define EI_PAD          9
#define EI_NIDENT       16

// Object File Classes, e_ident[EI_CLASS]
#define ELFCLASS32      1
#define ELFCLASS64      2

// Data Encodings, e_ident[EI_DATA]
#define ELFDATA2LSB     1
#define ELFDATA2MSB     2

// Object File Types, e_type
#define ET_EXEC         2

// Machine type
#define EM_RISCV        0xf3

// Entry point
#define ENTRY_POINT     0x80000000ULL
#define MEM_OFFSET      0x7ffff000ULL

// Program header table
typedef struct {
    Elf64_Word      p_type;     // Type of segment
    Elf64_Word      p_flags;    // Segment attributes
    Elf64_Off       p_offset;   // Offset in file
    Elf64_Addr      p_vaddr;    // Virtual address in memory
    Elf64_Addr      p_paddr;    // Reserved
    Elf64_Xword     p_filesz;   // Size of segment in file
    Elf64_Xword     p_memsz;    // Size of segment in memory
    Elf64_Xword     p_align;    // Alignment of segment
} Elf64_Phdr;

typedef struct {
    Elf32_Word      p_type;
    Elf32_Off       p_offset;
    Elf32_Addr      p_vaddr;
    Elf32_Addr      p_paddr;
    Elf32_Word      p_filesz;
    Elf32_Word      p_memsz;
    Elf32_Word      p_flags;
    Elf32_Word      p_align;
} Elf32_Phdr;

// Segment Types, p_type
#define PT_NULL     0
#define PT_LOAD     1
#define PT_DYNAMIC  2
#define PT_INTERP   3
#define PT_NOTE     4

#ifdef __cplusplus
}
#endif

#endif //ELFLOADER_ELF64_H
