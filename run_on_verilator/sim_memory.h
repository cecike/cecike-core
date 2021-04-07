#ifndef ELFLOADER_SIM_MEMORY_H
#define ELFLOADER_SIM_MEMORY_H

#include <cstdint>

// 16 M simulated memory
const uint64_t MEM_SIZE =  16 * 1024 * 1024;
const uint64_t MEM_MASK = (MEM_SIZE - 1);

typedef enum { sz_byte, sz_half, sz_word, sz_double } mem_size;
typedef union {
    uint64_t    data_double;
    char        data_bytes[8];
} med_mem;

class SimMemory {
public:
    SimMemory();
    ~SimMemory();
    bool init(const char *path);
    uint64_t read(uint64_t addr, int size);
    void write(uint64_t addr, int size, uint64_t data);
private:
    uint64_t addr_to_offset(uint64_t addr);
    char *memory;
};


#endif //ELFLOADER_SIM_MEMORY_H
