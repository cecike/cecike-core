#include <iostream>
#include "loadelf.h"
#include "simulator.h"

int main(int argc, char **argv) {
    if (argc != 2) {
        printf("Argument mismatch.\n");
        exit(1);
    } else {
        printf("Loading elf %s to memory ...\n", argv[1]);
    }
    Simulator s(argv[1]);
    s.reset(10);
    s.step(100);
    return 0;
}
