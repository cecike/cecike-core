#ifndef ELFLOADER_SIMULATOR_H
#define ELFLOADER_SIMULATOR_H

#include <cstdint>
#include <VSimpleSimCore.h>
#include "sim_memory.h"

class Simulator {
public:
    Simulator(const char *elf_path) : cycle(0), halt(false), has_error(false), c(new VSimpleSimCore) {
        bus_init(elf_path);
    }

    void reset(uint64_t cycles = 1);

    void step(uint64_t cycles = 1);

    void bus_init(const char *elf_path);

    void check_bus();

    std::shared_ptr<VSimpleSimCore> c;
private:
    uint64_t cycle;
    SimMemory memory;
    bool halt;
    bool has_error;
};


#endif //ELFLOADER_SIMULATOR_H
