#include "loadelf.h"
#include "elf64.h"
#include "defer.h"

#include <cstdio>
#include <cstdlib>
#include <cstring>

#define CHECK_HEADER(__type, __class) bool check_header(__type header) { \
    auto ident = header.e_ident; \
    if (ident[EI_MAG0] != 0x7f || ident[EI_MAG1] != 'E' || ident[EI_MAG2] != 'L' || ident[EI_MAG3] != 'F') {  \
        puts("Magic number doesn't match."); \
        return false; \
    } \
    if (ident[EI_CLASS] != __class) { \
        puts("File class mismatch."); \
        return false; \
    } \
    if (ident[EI_DATA] != ELFDATA2LSB) { \
        puts("We only accept little endian file."); \
        return false; \
    } \
    if (header.e_type != ET_EXEC) { \
        puts("We need a executable ELF file."); \
        return false; \
    } \
    if (header.e_machine != EM_RISCV) { \
        puts("We need a RISC-V file."); \
        return false; \
    } \
    if (header.e_entry != ENTRY_POINT) { \
        printf("Require entry point as 0x%016llx", ENTRY_POINT); \
    } \
    return true; \
}

CHECK_HEADER(Elf64_Ehdr, ELFCLASS64)
CHECK_HEADER(Elf32_Ehdr, ELFCLASS32)

#define LOAD_ELF(__class, __ehdr, __phdr) bool load_elf_##__class(FILE *fp, const char *file_path, char *memory, uint64_t length) { \
    memset(memory, 0, length); \
    __ehdr header; \
    fseek(fp, 0, SEEK_SET); \
    if (fread(&header, sizeof(__ehdr), 1, fp) != 1) { \
        printf("Read elf header of %s failed. \n", file_path); \
        return false; \
    } \
    if (!check_header(header)) { \
        printf("ELF %s header not match our requirements.", file_path); \
        return false; \
    } else { \
        printf("ELF File Summary:\n"); \
        printf("\tPath: %s\n", file_path); \
        printf("\tEntry point: 0x%08x\n", header.e_entry); \
        printf("\tProgram header offset: 0x%x\n", header.e_phoff); \
        printf("\tProgram header count: 0x%x\n", header.e_phnum); \
        printf("\tProgram header entry size: 0x%x\n", header.e_phentsize); \
    } \
    auto *program_headers = new __phdr[header.e_phnum]; \
    defer(delete[](program_headers)); \
    fseek(fp, header.e_phoff, SEEK_SET); \
    if (fread(program_headers, sizeof(__phdr), header.e_phnum, fp) != header.e_phnum) { \
        printf("Read program header of %s failed. \n", file_path); \
        return false; \
    } \
    for (uint16_t i = 0; i < header.e_phnum; i ++) { \
        auto ph = program_headers[i]; \
        if (ph.p_type == PT_LOAD) { \
            printf("LOAD offset %x(%x) to address %x(%x).\n", \
                   ph.p_offset, ph.p_filesz, ph.p_vaddr, ph.p_memsz); \
            if (ph.p_vaddr + ph.p_memsz > (MEM_OFFSET + length) || ph.p_vaddr < MEM_OFFSET) { \
                puts("Simulated memory isn't big enough."); \
                return false; \
            } else { \
                auto start_index = ph.p_vaddr - MEM_OFFSET; \
                fseek(fp, ph.p_offset, SEEK_SET); \
                if (fread(&(memory[start_index]), sizeof(char), ph.p_filesz, fp) != ph.p_filesz) { \
                    printf("Read program header %d of %s failed. \n", i, file_path); \
                    return false; \
                } \
            } \
        } \
    } \
    puts("Load Done."); \
    return true; \
}

LOAD_ELF(64, Elf64_Ehdr, Elf64_Phdr)
LOAD_ELF(32, Elf32_Ehdr, Elf32_Phdr)

bool load_elf(const char *file_path, char *memory, uint64_t length) {
    FILE *fp = fopen(file_path, "rb");
    if (fp == nullptr) {
        printf("Open file %s failed. \n", file_path);
        return false;
    }
    defer(fclose(fp));

    unsigned char elf_class;
    fseek(fp, EI_CLASS, SEEK_SET);
    if (fread(&elf_class, sizeof(char), 1, fp) != 1) {
        printf("Read elf class of %s failed. \n", file_path);
        return false;
    }
    if (elf_class == ELFCLASS32) {
        puts("Load 32 bit elf file.");
        return load_elf_32(fp, file_path, memory, length);
    } else if (elf_class == ELFCLASS64) {
        puts("Load 64 bit elf file.");
        return load_elf_64(fp, file_path, memory, length);
    }
}