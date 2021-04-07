#include "sim_memory.h"
#include "loadelf.h"
#include "elf64.h"

static uint64_t size_array[4] = { 1, 2, 4, 8 };

SimMemory::SimMemory() {
    memory = new char[MEM_SIZE];
}

SimMemory::~SimMemory() {
    delete[] memory;
}

bool SimMemory::init(const char *path) {
    return load_elf(path, memory, MEM_SIZE);
}

uint64_t SimMemory::read(uint64_t addr, int size) {
    auto offset = addr_to_offset(addr);
    med_mem result;
    result.data_double = 0;
    for (int i = 0; i < size_array[size]; i ++) {
        result.data_bytes[i] = memory[offset + i];
    }
    return result.data_double;
}

void SimMemory::write(uint64_t addr, int size, uint64_t data) {
    auto offset = addr_to_offset(addr);
    med_mem result;
    result.data_double = data;
    for (int i = 0; i < size_array[size]; i ++) {
        memory[offset + i] = result.data_bytes[i];
    }
}

uint64_t SimMemory::addr_to_offset(uint64_t addr) {
    return (addr - MEM_OFFSET) & MEM_MASK;
}
