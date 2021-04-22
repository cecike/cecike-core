#include "simulator.h"

void Simulator::reset() {
    c->reset = 1;
    step(1);
    c->reset = 0;
}

void Simulator::step(uint64_t cycles) {
    cycle = 0;
    for (uint64_t i = 0; i < cycles * 10; i ++) {
        if ((i % 10) == 1) {
            cycle += 1;
            printf("Step %llu --\n", cycle);
            c->clock = 1;
        }

        if ((i % 10) == 6) {
            c->clock = 0;
        }

        c->eval();

        if ((i % 10) == 9 && halt) {
            return;
        }
    }
    return;
}

void Simulator::check_bus(uint8_t iREn, long long iRAddr, long long iRSize, long long* iRdata, uint8_t* iRValid, uint8_t dREn, long long dRAddr, long long dRSize, long long* dRdata, uint8_t* dRValid, long long dWAddr, long long dWdata, long long dWSize, uint8_t dWEn) {
    *iRValid = 0;
    *dRValid = 0;

    if (iREn == 1) {
        printf("Reading %016lx <- %016llx\n", iRAddr,
               memory.read(iRAddr, sz_double));
        *iRValid = 1;
        *iRdata = memory.read(iRAddr, sz_double);
    }

    if (dREn == 1) {
        *dRValid = 1;
        *dRdata = memory.read(dRAddr, dRSize);
    }

    if (dWEn == 1) {
        printf("Write: %lx %d %lx\n", dWAddr,
               dWSize,
               dWdata);
        if ((dWAddr & 0xFFFFFFFFLL) == 0xFFFF0000LL) {
            printf("%c", (char)dWdata);
        } else if ((dWAddr & 0xFFFFFFFFLL) == 0xFFFF0010LL) {
            if (dWdata != 0) {
                printf("Test failed with error %lu.\n", dWdata);
            } else {
                puts("Test passed.");
            }

            halt = true;
        } else {
            memory.write(dWAddr,
                         dWSize,
                         dWdata);
        }
    }
}

void Simulator::bus_init(const char *elf_path) {
    memory.init(elf_path);
}
