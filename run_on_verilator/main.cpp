#include <iostream>
#include "loadelf.h"
#include "simulator.h"

Simulator s;

extern "C" void sim_ram(uint8_t iREn, long long iRAddr, long long iRSize, long long* iRdata, uint8_t* iRValid, uint8_t dREn, long long dRAddr, long long dRSize, long long* dRdata, uint8_t* dRValid, long long dWAddr, long long dWdata, long long dWSize, uint8_t dWEn) {
    s.check_bus(iREn, iRAddr, iRSize, iRdata, iRValid, dREn, dRAddr, dRSize, dRdata, dRValid, dWAddr, dWdata, dWSize, dWEn);
}

int main(int argc, char **argv) {
    if (argc != 2) {
        printf("Argument mismatch.\n");
        exit(1);
    } else {
        printf("Loading elf %s to memory ...\n", argv[1]);
    }
    s.bus_init(argv[1]);
    s.reset();
    s.step(1000);
    return 0;
}
