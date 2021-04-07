#ifndef ELFLOADER_LOADELF_H
#define ELFLOADER_LOADELF_H

#include <cstdint>

bool load_elf(const char *file_path, char *memory, uint64_t length);

#endif //ELFLOADER_LOADELF_H
