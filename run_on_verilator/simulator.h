#ifndef ELFLOADER_SIMULATOR_H
#define ELFLOADER_SIMULATOR_H

#include <cstdint>
#include <VSimpleSimCore.h>
#include "sim_memory.h"

class Simulator {
public:
    Simulator() : cycle(0), halt(false), has_error(false), c(new VSimpleSimCore) {
    }

    void reset(uint64_t cycles = 1);

    void step(uint64_t cycles = 1);

    void bus_init(const char *elf_path);

    void check_bus(uint8_t iREn, long long iRAddr, long long iRSize, long long* iRdata, uint8_t* iRValid, uint8_t dREn, long long dRAddr, long long dRSize, long long* dRdata, uint8_t* dRValid, long long dWAddr, long long dWdata, long long dWSize, uint8_t dWEn);

    std::shared_ptr<VSimpleSimCore> c;
private:
    uint64_t cycle;
    SimMemory memory;
    bool halt;
    bool has_error;
};


#endif //ELFLOADER_SIMULATOR_H
